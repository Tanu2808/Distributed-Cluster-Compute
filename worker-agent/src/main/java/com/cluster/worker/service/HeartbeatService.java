package com.cluster.worker.service;

import com.cluster.shared.protocol.HeartbeatMessage;
import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.ResourceUpdateMessage;
import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.ExecutionState;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class HeartbeatService {

    private final WebSocketConnectionManager connectionManager;
    private final WorkerIdentityGenerator identityGenerator;
    private final SystemMetricsProvider metricsProvider;
    private final WorkerLifecycleService lifecycleService;
    private final TaskService taskService;
    private Instant lastSuccessfulHeartbeat;

    public HeartbeatService(
            WebSocketConnectionManager connectionManager,
            WorkerIdentityGenerator identityGenerator,
            SystemMetricsProvider metricsProvider,
            WorkerLifecycleService lifecycleService,
            TaskService taskService) {
        this.connectionManager = connectionManager;
        this.identityGenerator = identityGenerator;
        this.metricsProvider = metricsProvider;
        this.lifecycleService = lifecycleService;
        this.taskService = taskService;
    }

    public Instant getLastSuccessfulHeartbeat() {
        return lastSuccessfulHeartbeat;
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(HeartbeatService.class);

    @Scheduled(fixedDelayString = "${worker.heartbeat.interval-ms:5000}")
    public void sendHeartbeat() {
        if (lifecycleService.getStateManager().getLifecycleState()
                == com.cluster.worker.model.WorkerLifecycleState.STOPPING) {
            return;
        }

        if (lifecycleService.getStateManager().getConnectionState() != ConnectionState.ONLINE) {
            // Don't send heartbeats if not registered/online
            return;
        }

        try {
            String workerId = identityGenerator.getOrCreateWorkerId();

            HeartbeatMessage heartbeatMsg = new HeartbeatMessage();
            String status =
                    lifecycleService.getStateManager().getExecutionState() == ExecutionState.BUSY
                            ? "BUSY"
                            : "ONLINE";
            heartbeatMsg.setStatus(status);
            heartbeatMsg.setRunningTasks(taskService.getActiveTasks().size());

            MessageEnvelope<HeartbeatMessage> hbEnvelope =
                    MessageEnvelope.<HeartbeatMessage>builder()
                            .type(MessageType.HEARTBEAT)
                            .workerId(workerId)
                            .timestamp(Instant.now())
                            .payload(heartbeatMsg)
                            .build();

            connectionManager.sendMessage("/app/worker.heartbeat", hbEnvelope);

            // Send resource update safely
            try {
                SystemMetrics metrics = metricsProvider.collectMetrics();
                ResourceUpdateMessage resourceMsg = new ResourceUpdateMessage();
                if (metrics.getCpuUsagePercent() > 0) {
                    resourceMsg.setCpuUsagePercent(metrics.getCpuUsagePercent());
                }
                if (metrics.getUsedMemoryMb() > 0) {
                    resourceMsg.setMemoryUsedBytes(metrics.getUsedMemoryMb() * 1024L * 1024L);
                }
                if (metrics.getTotalMemoryMb() > 0) {
                    resourceMsg.setMemoryTotalBytes(metrics.getTotalMemoryMb() * 1024L * 1024L);
                }

                long usedStorage = metrics.getUsedStorageMb();
                long totalStorage = metrics.getTotalStorageMb();

                if (totalStorage > 0) {
                    resourceMsg.setDiskTotalBytes(totalStorage * 1024L * 1024L);
                    if (usedStorage > 0) {
                        resourceMsg.setDiskFreeBytes(
                                Math.max(0, totalStorage - usedStorage) * 1024L * 1024L);
                    }
                }

                MessageEnvelope<ResourceUpdateMessage> resEnvelope =
                        MessageEnvelope.<ResourceUpdateMessage>builder()
                                .type(MessageType.RESOURCE_UPDATE)
                                .workerId(workerId)
                                .timestamp(Instant.now())
                                .payload(resourceMsg)
                                .build();

                connectionManager.sendMessage("/app/worker.resource", resEnvelope);
            } catch (Exception e) {
                log.warn("Failed to collect or dispatch resource update: {}", e.getMessage());
            }

            lastSuccessfulHeartbeat = Instant.now();
        } catch (Exception e) {
            log.warn("Failed to send heartbeat: {}", e.getMessage());
        }
    }
}
