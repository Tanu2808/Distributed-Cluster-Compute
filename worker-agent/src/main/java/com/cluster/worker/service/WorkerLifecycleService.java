package com.cluster.worker.service;

import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.RegisterMessage;
import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WorkerLifecycleService {

    private final WebSocketConnectionManager connectionManager;
    private final WorkerIdentityGenerator identityGenerator;
    private final SystemMetricsProvider metricsProvider;
    private final WorkerConfig config;
    private WorkerState state = WorkerState.STARTING;
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public WorkerLifecycleService(WebSocketConnectionManager connectionManager,
                                  WorkerIdentityGenerator identityGenerator,
                                  SystemMetricsProvider metricsProvider,
                                  WorkerConfig config) {
        this.connectionManager = connectionManager;
        this.identityGenerator = identityGenerator;
        this.metricsProvider = metricsProvider;
        this.config = config;
        
        this.connectionManager.setCallbacks(this::onConnected, this::onDisconnected);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (state == WorkerState.STARTING || state == WorkerState.DISCONNECTED) {
            connectWithBackoff();
        }
    }

    private void connectWithBackoff() {
        if (state == WorkerState.ONLINE) {
            return;
        }
        state = WorkerState.REGISTERING;
        System.out.println("Attempting to connect to coordinator...");
        connectionManager.connect();
    }

    private void onConnected(StompSession session) {
        System.out.println("WebSocket connected. Sending REGISTER message...");
        isReconnecting.set(false);
        
        String workerId = identityGenerator.getOrCreateWorkerId();
        
        // Subscribe to REGISTER_ACK
        session.subscribe("/topic/worker." + workerId + ".control", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageEnvelope.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof MessageEnvelope) {
                    MessageEnvelope<?> envelope = (MessageEnvelope<?>) payload;
                    if (envelope.getType() == MessageType.REGISTER_ACK) {
                        state = WorkerState.ONLINE;
                        System.out.println("Worker registered successfully and is ONLINE.");
                    }
                }
            }
        });
        
        @SuppressWarnings("unused")
        WorkerConfig ignoredConfig = config;
        
        RegisterMessage registerMsg = new RegisterMessage();
        
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            registerMsg.setHostname(localHost.getHostName());
        } catch (UnknownHostException e) {
            registerMsg.setHostname("unknown");
        }
        
        registerMsg.setOsName(System.getProperty("os.name"));
        registerMsg.setOsVersion(System.getProperty("os.version"));
        registerMsg.setTags(new HashMap<>()); // dummy for now

        MessageEnvelope<RegisterMessage> envelope = MessageEnvelope.<RegisterMessage>builder()
                .type(MessageType.REGISTER)
                .workerId(workerId)
                .timestamp(Instant.now())
                .payload(registerMsg)
                .build();
                
        connectionManager.sendMessage("/app/worker.register", envelope);
    }
    
    private void onDisconnected() {
        if (state == WorkerState.STOPPING) {
            return;
        }
        if (isReconnecting.compareAndSet(false, true)) {
            System.err.println("Disconnected from coordinator. Reconnecting in 5 seconds...");
            state = WorkerState.DISCONNECTED;
            executorService.schedule(this::connectWithBackoff, 5, TimeUnit.SECONDS);
        }
    }

    public WorkerState getState() {
        return state;
    }

    public void setState(WorkerState state) {
        this.state = state;
    }

    public void handleDisconnection() {
        onDisconnected();
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down worker agent...");
        this.state = WorkerState.STOPPING;
        executorService.shutdown();
    }
}
