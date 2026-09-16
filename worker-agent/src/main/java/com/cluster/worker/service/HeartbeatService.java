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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HeartbeatService {

    private final WebSocketConnectionManager connectionManager;
    private final WorkerIdentityGenerator identityGenerator;
    private final SystemMetricsProvider metricsProvider;
    private final WorkerLifecycleService lifecycleService;
    private final TaskService taskService;
    private Instant lastSuccessfulHeartbeat;

    public HeartbeatService(WebSocketConnectionManager connectionManager,
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

    @Scheduled(fixedDelayString = "${worker.heartbeat.interval-ms:5000}")
    public void sendHeartbeat() {
        if (lifecycleService.getStateManager().getConnectionState() != ConnectionState.ONLINE) {
            // Don't send heartbeats if not registered/online
            return;
        }

        String workerId = identityGenerator.getOrCreateWorkerId();
        
        HeartbeatMessage heartbeatMsg = new HeartbeatMessage();
        
        // Map states back to the string values expected by coordinator for now.
        String status = lifecycleService.getStateManager().getExecutionState() == ExecutionState.BUSY ? "BUSY" : "ONLINE";
        heartbeatMsg.setStatus(status);
        heartbeatMsg.setRunningTasks(taskService.getActiveTasks().size());

        MessageEnvelope<HeartbeatMessage> hbEnvelope = MessageEnvelope.<HeartbeatMessage>builder()
                .type(MessageType.HEARTBEAT)
                .workerId(workerId)
                .timestamp(Instant.now())
                .payload(heartbeatMsg)
                .build();

        connectionManager.sendMessage("/app/worker.heartbeat", hbEnvelope);
        
        // Also send resource update
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
        // GPU not currently populated by real metrics, keep null instead of 0.0
        
        long usedStorage = metrics.getUsedStorageMb();
        long totalStorage = metrics.getTotalStorageMb();
        
        if (totalStorage > 0) {
            resourceMsg.setDiskTotalBytes(totalStorage * 1024L * 1024L);
            if (usedStorage > 0) {
                resourceMsg.setDiskFreeBytes((totalStorage - usedStorage) * 1024L * 1024L);
            }
        }
        
        MessageEnvelope<ResourceUpdateMessage> resEnvelope = MessageEnvelope.<ResourceUpdateMessage>builder()
                .type(MessageType.RESOURCE_UPDATE)
                .workerId(workerId)
                .timestamp(Instant.now())
                .payload(resourceMsg)
                .build();

        connectionManager.sendMessage("/app/worker.resource", resEnvelope);
        
        lastSuccessfulHeartbeat = Instant.now();
    }
}
