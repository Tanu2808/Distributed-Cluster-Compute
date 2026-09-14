package com.cluster.worker.service;

import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.RegisterMessage;
import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerLifecycleState;
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
    private final WorkerStateManager stateManager;
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public WorkerLifecycleService(WebSocketConnectionManager connectionManager,
                                  WorkerIdentityGenerator identityGenerator,
                                  SystemMetricsProvider metricsProvider,
                                  WorkerConfig config,
                                  WorkerStateManager stateManager) {
        this.connectionManager = connectionManager;
        this.identityGenerator = identityGenerator;
        this.metricsProvider = metricsProvider;
        this.config = config;
        this.stateManager = stateManager;
        
        this.connectionManager.setCallbacks(this::onConnected, this::onDisconnected);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (stateManager.getLifecycleState() == WorkerLifecycleState.STARTING) {
            stateManager.transitionLifecycle(WorkerLifecycleState.INITIALIZING);
            
            if (!config.getCluster().isConfigured()) {
                stateManager.transitionLifecycle(WorkerLifecycleState.SETUP_REQUIRED);
                System.out.println("Worker is not configured. Entering SETUP_REQUIRED state.");
                return;
            }
            
            stateManager.transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
            System.out.println("Worker is configured. Proceeding to connect...");
            stateManager.transitionLifecycle(WorkerLifecycleState.CONFIGURED);
            connectWithBackoff();
        } else if (stateManager.getConnectionState() == ConnectionState.DISCONNECTED) {
            connectWithBackoff();
        }
    }

    private void connectWithBackoff() {
        if (stateManager.getConnectionState() == ConnectionState.ONLINE || stateManager.getConnectionState() == ConnectionState.CONNECTING) {
            return;
        }
        
        if (stateManager.getLifecycleState() == WorkerLifecycleState.SETUP_REQUIRED) {
            return; // don't connect if setup required
        }
        
        if (isReconnecting.get()) {
            stateManager.transitionConnection(ConnectionState.RECONNECTING);
        }
        
        stateManager.transitionConnection(ConnectionState.CONNECTING);
        System.out.println("Attempting to connect to coordinator...");
        connectionManager.connect();
    }

    private void onConnected(StompSession session) {
        System.out.println("WebSocket connected. Sending REGISTER message...");
        isReconnecting.set(false);
        stateManager.transitionConnection(ConnectionState.REGISTERING);
        
        String workerId = identityGenerator.getOrCreateWorkerId();
        
        // Subscribe to REGISTER_ACK
        session.subscribe("/topic/worker." + workerId + ".control", new StompFrameHandler() {
            @Override
            @org.springframework.lang.NonNull
            public Type getPayloadType(@org.springframework.lang.NonNull StompHeaders headers) {
                return MessageEnvelope.class;
            }

            @Override
            public void handleFrame(@org.springframework.lang.NonNull StompHeaders headers, @org.springframework.lang.Nullable Object payload) {
                if (payload instanceof MessageEnvelope) {
                    MessageEnvelope<?> envelope = (MessageEnvelope<?>) payload;
                    if (envelope.getType() == MessageType.REGISTER_ACK) {
                        stateManager.transitionConnection(ConnectionState.ONLINE);
                        System.out.println("Worker registered successfully and is ONLINE.");
                    }
                }
            }
        });
        
        RegisterMessage registerMsg = new RegisterMessage();
        
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            registerMsg.setHostname(localHost.getHostName());
        } catch (UnknownHostException e) {
            registerMsg.setHostname("unknown");
        }
        
        registerMsg.setOsName(System.getProperty("os.name"));
        registerMsg.setOsVersion(System.getProperty("os.version"));
        
        SystemMetrics currentMetrics = metricsProvider.collectMetrics();
        registerMsg.setCpuCores(currentMetrics.getCpuCores());
        registerMsg.setMemoryMb(currentMetrics.getTotalMemoryMb());
        
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
        if (stateManager.getLifecycleState() == WorkerLifecycleState.STOPPING) {
            return;
        }
        
        if (stateManager.getConnectionState() != ConnectionState.DISCONNECTED) {
            stateManager.transitionConnection(ConnectionState.DISCONNECTED);
        }
        
        if (isReconnecting.compareAndSet(false, true)) {
            System.err.println("Disconnected from coordinator. Reconnecting in 5 seconds...");
            executorService.schedule(this::connectWithBackoff, 5, TimeUnit.SECONDS);
        }
    }

    public WorkerStateManager getStateManager() {
        return stateManager;
    }

    public void handleDisconnection() {
        onDisconnected();
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down worker agent...");
        try {
            this.stateManager.transitionLifecycle(WorkerLifecycleState.STOPPING);
        } catch (Exception e) {
            // ignore
        }
        executorService.shutdown();
    }
}
