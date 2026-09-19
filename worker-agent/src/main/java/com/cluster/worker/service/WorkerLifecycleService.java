package com.cluster.worker.service;

import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.RegisterMessage;
import com.cluster.worker.communication.TaskMessageHandler;
import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import jakarta.annotation.PreDestroy;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Service;

@Service
public class WorkerLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(WorkerLifecycleService.class);

    private final WebSocketConnectionManager connectionManager;
    private final WorkerIdentityGenerator identityGenerator;
    private final SystemMetricsProvider metricsProvider;

    private final WorkerStateManager stateManager;
    private final WorkerConfigurationStore configStore;
    private final TaskMessageHandler taskMessageHandler;

    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> reconnectFuture;

    public WorkerLifecycleService(
            WebSocketConnectionManager connectionManager,
            WorkerIdentityGenerator identityGenerator,
            SystemMetricsProvider metricsProvider,
            WorkerStateManager stateManager,
            WorkerConfigurationStore configStore,
            TaskMessageHandler taskMessageHandler) {
        this.connectionManager = connectionManager;
        this.identityGenerator = identityGenerator;
        this.metricsProvider = metricsProvider;
        this.stateManager = stateManager;
        this.configStore = configStore;
        this.taskMessageHandler = taskMessageHandler;

        this.executorService =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread t = new Thread(r, "worker-lifecycle-scheduler");
                            t.setDaemon(true);
                            return t;
                        });

        this.connectionManager.setCallbacks(this::onConnected, this::onDisconnected);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        String workerId = identityGenerator.getOrCreateWorkerId();
        if (stateManager.getLifecycleState() == WorkerLifecycleState.STARTING) {
            stateManager.transitionLifecycle(WorkerLifecycleState.INITIALIZING);

            if (!configStore.isConfigured()) {
                stateManager.transitionLifecycle(WorkerLifecycleState.SETUP_REQUIRED);
                log.info(
                        "[Worker: {}] Worker is not configured. Entering SETUP_REQUIRED state.",
                        workerId);
                return;
            }

            stateManager.transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
            log.info("[Worker: {}] Worker is configured. Proceeding to connect...", workerId);
            stateManager.transitionLifecycle(WorkerLifecycleState.CONFIGURED);
            initiateConnection();
        } else if (stateManager.getConnectionState() == ConnectionState.DISCONNECTED) {
            initiateConnection();
        }
    }

    public synchronized void initiateConnection() {
        String workerId = identityGenerator.getOrCreateWorkerId();
        if (stateManager.getLifecycleState() == WorkerLifecycleState.STOPPING
                || stateManager.getLifecycleState() == WorkerLifecycleState.SETUP_REQUIRED) {
            return;
        }

        ConnectionState currentConn = stateManager.getConnectionState();
        if (currentConn == ConnectionState.ONLINE || currentConn == ConnectionState.CONNECTING) {
            return;
        }

        stateManager.transitionConnection(ConnectionState.CONNECTING);
        log.info("[Worker: {}] Attempting to connect to coordinator...", workerId);
        connectionManager.connect();
    }

    private synchronized void scheduleReconnect() {
        String workerId = identityGenerator.getOrCreateWorkerId();
        if (stateManager.getLifecycleState() == WorkerLifecycleState.STOPPING) {
            return;
        }

        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            reconnectFuture.cancel(false);
        }

        try {
            stateManager.transitionConnection(ConnectionState.RECONNECTING);
        } catch (Exception e) {
            log.debug("[Worker: {}] Reconnecting transition note: {}", workerId, e.getMessage());
        }

        int attempts = connectionManager.getReconnectCount();
        long delay = Math.min(60L, 5L * (1L << Math.min(attempts, 4))); // 5, 10, 20, 40, 60...
        log.info(
                "[Worker: {}] Reconnecting to coordinator in {} seconds (attempt #{})...",
                workerId,
                delay,
                attempts);

        reconnectFuture =
                executorService.schedule(
                        () -> {
                            try {
                                initiateConnection();
                            } catch (Exception e) {
                                log.error(
                                        "[Worker: {}] Error executing reconnect attempt",
                                        workerId,
                                        e);
                                onDisconnected();
                            }
                        },
                        delay,
                        TimeUnit.SECONDS);
    }

    private void onConnected(StompSession session) {
        String workerId = identityGenerator.getOrCreateWorkerId();
        log.info("[Worker: {}] WebSocket connected. Sending REGISTER message...", workerId);

        synchronized (this) {
            if (reconnectFuture != null) {
                reconnectFuture.cancel(false);
            }
        }
        connectionManager.resetReconnectCount();
        stateManager.transitionConnection(ConnectionState.REGISTERING);

        // Subscribe to control channel for REGISTER_ACK
        session.subscribe(
                "/topic/worker." + workerId + ".control",
                new StompFrameHandler() {
                    @Override
                    @NonNull
                    public Type getPayloadType(@NonNull StompHeaders headers) {
                        return MessageEnvelope.class;
                    }

                    @Override
                    public void handleFrame(
                            @NonNull StompHeaders headers, @Nullable Object payload) {
                        if (payload instanceof MessageEnvelope) {
                            MessageEnvelope<?> envelope = (MessageEnvelope<?>) payload;
                            if (envelope.getType() == MessageType.REGISTER_ACK) {
                                stateManager.transitionConnection(ConnectionState.ONLINE);
                                try {
                                    if (stateManager.getExecutionState()
                                            == com.cluster.worker.model.ExecutionState.OFFLINE) {
                                        stateManager.transitionExecution(
                                                com.cluster.worker.model.ExecutionState.IDLE);
                                    }
                                } catch (Exception e) {
                                    log.debug("Execution transition note: {}", e.getMessage());
                                }
                                log.info(
                                        "[Worker: {}] Worker registered successfully and is"
                                                + " ONLINE.",
                                        workerId);
                            }
                        }
                    }
                });

        // Subscribe to tasks
        session.subscribe("/topic/worker." + workerId + ".tasks", taskMessageHandler);

        RegisterMessage registerMsg = buildRegistrationMessage();

        MessageEnvelope<RegisterMessage> envelope =
                MessageEnvelope.<RegisterMessage>builder()
                        .type(MessageType.REGISTER)
                        .workerId(workerId)
                        .timestamp(Instant.now())
                        .payload(registerMsg)
                        .build();

        connectionManager.sendMessage("/app/worker.register", envelope);
    }

    private RegisterMessage buildRegistrationMessage() {
        RegisterMessage registerMsg = new RegisterMessage();

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            registerMsg.setHostname(localHost.getHostName());
        } catch (UnknownHostException e) {
            String envHostname = System.getenv("HOSTNAME");
            registerMsg.setHostname(envHostname != null ? envHostname : "unknown");
        }

        registerMsg.setOsName(System.getProperty("os.name"));
        registerMsg.setOsVersion(System.getProperty("os.version"));
        registerMsg.setArchitecture(System.getProperty("os.arch"));
        registerMsg.setAgentVersion("0.0.1-SNAPSHOT");

        SystemMetrics currentMetrics = metricsProvider.collectMetrics();
        registerMsg.setCpuCores(currentMetrics.getCpuCores());
        registerMsg.setMemoryMb(currentMetrics.getTotalMemoryMb());

        // Populate actual CPU info
        String cpuIdentifier = null;
        try {
            cpuIdentifier = metricsProvider.getCpuMetricsProvider().getProcessorIdentifier();
        } catch (Exception e) {
            log.debug("Processor identifier unavailable: {}", e.getMessage());
        }
        if (cpuIdentifier != null && !cpuIdentifier.trim().isEmpty()) {
            registerMsg.setCpuInfo(
                    cpuIdentifier.trim() + " (" + currentMetrics.getCpuCores() + " cores)");
        } else if (currentMetrics.getCpuCores() > 0) {
            registerMsg.setCpuInfo(currentMetrics.getCpuCores() + " Cores");
        } else {
            registerMsg.setCpuInfo("Unknown CPU");
        }

        // Populate actual GPU info or explicit Unavailable
        String gpuInfo = "Unavailable";
        try {
            gpuInfo = metricsProvider.getGpuMetricsProvider().getGpuInfo();
        } catch (Exception e) {
            log.debug("GPU info unavailable: {}", e.getMessage());
        }
        registerMsg.setGpuInfo(gpuInfo);

        // Populate storage info
        if (currentMetrics.getTotalStorageMb() > 0) {
            registerMsg.setStorageInfo(currentMetrics.getTotalStorageMb() + " MB Total Storage");
        } else {
            registerMsg.setStorageInfo("Unavailable");
        }

        registerMsg.setTags(new HashMap<>());
        return registerMsg;
    }

    private void onDisconnected() {
        String workerId = identityGenerator.getOrCreateWorkerId();
        if (stateManager.getLifecycleState() == WorkerLifecycleState.STOPPING) {
            return;
        }

        if (stateManager.getConnectionState() != ConnectionState.DISCONNECTED) {
            try {
                stateManager.transitionConnection(ConnectionState.DISCONNECTED);
            } catch (Exception e) {
                log.debug(
                        "[Worker: {}] Disconnect transition notice: {}", workerId, e.getMessage());
            }
        }

        scheduleReconnect();
    }

    public WorkerStateManager getStateManager() {
        return stateManager;
    }

    public void handleDisconnection() {
        onDisconnected();
    }

    @PreDestroy
    public void shutdown() {
        String workerId = identityGenerator.getOrCreateWorkerId();
        log.info("[Worker: {}] Shutting down worker agent lifecycle...", workerId);
        try {
            this.stateManager.transitionLifecycle(WorkerLifecycleState.STOPPING);
        } catch (Exception e) {
            log.debug("State transition exception during shutdown: {}", e.getMessage());
        }
        synchronized (this) {
            if (reconnectFuture != null) {
                reconnectFuture.cancel(true);
            }
        }
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                log.debug("Lifecycle executor service did not terminate immediately");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
