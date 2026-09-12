package com.cluster.worker.service;

import com.cluster.worker.communication.CoordinatorClient;
import com.cluster.worker.communication.HeartbeatPayload;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HeartbeatService {

    private final CoordinatorClient coordinatorClient;
    private final WorkerIdentityGenerator identityGenerator;
    private final SystemMetricsProvider metricsProvider;
    private final WorkerLifecycleService lifecycleService;

    public HeartbeatService(CoordinatorClient coordinatorClient,
                            WorkerIdentityGenerator identityGenerator,
                            SystemMetricsProvider metricsProvider,
                            WorkerLifecycleService lifecycleService) {
        this.coordinatorClient = coordinatorClient;
        this.identityGenerator = identityGenerator;
        this.metricsProvider = metricsProvider;
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(fixedDelayString = "${worker.heartbeat.interval-ms:5000}")
    public void sendHeartbeat() {
        if (lifecycleService.getState() != WorkerState.ONLINE && lifecycleService.getState() != WorkerState.BUSY) {
            // Don't send heartbeats if not registered/online
            return;
        }

        SystemMetrics metrics = metricsProvider.collectMetrics();
        HeartbeatPayload payload = new HeartbeatPayload();
        payload.setTimestamp(LocalDateTime.now());
        payload.setCpuUsagePercent(metrics.getCpuUsagePercent());
        
        double memoryUsage = 0.0;
        if (metrics.getTotalMemoryMb() > 0) {
            memoryUsage = ((double) metrics.getUsedMemoryMb() / metrics.getTotalMemoryMb()) * 100.0;
        }
        payload.setMemoryUsagePercent(memoryUsage);
        payload.setActiveTasks(0); // Dummy for now
        payload.setResourceInformation(metrics.getAdditionalInfo());

        boolean success = coordinatorClient.sendHeartbeat(identityGenerator.getOrCreateWorkerId(), payload);
        if (!success) {
            System.err.println("Heartbeat failed, coordinator might be down.");
            lifecycleService.handleDisconnection();
        }
    }
}
