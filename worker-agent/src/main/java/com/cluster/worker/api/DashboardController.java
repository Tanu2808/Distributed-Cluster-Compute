package com.cluster.worker.api;

import com.cluster.worker.api.dto.DashboardHomeResponse;
import com.cluster.worker.api.dto.DashboardNodeResponse;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.service.TaskService;
import com.cluster.worker.service.WorkerLifecycleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
@RequestMapping("/api/worker/dashboard")
public class DashboardController {

    private final WorkerLifecycleService lifecycleService;
    private final WorkerConfigurationStore configStore;
    private final SystemMetricsProvider metricsProvider;
    private final TaskService taskService;

    public DashboardController(WorkerLifecycleService lifecycleService,
                               WorkerConfigurationStore configStore,
                               SystemMetricsProvider metricsProvider,
                               TaskService taskService) {
        this.lifecycleService = lifecycleService;
        this.configStore = configStore;
        this.metricsProvider = metricsProvider;
        this.taskService = taskService;
    }

    @GetMapping("/home")
    public ResponseEntity<DashboardHomeResponse> getHomeDashboard() {
        SystemMetrics metrics = metricsProvider.collectMetrics();
        
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "Unknown";
        }
        
        String clusterName = configStore.isConfigured() ? configStore.getConfig().getClusterName() : "Not Configured";

        DashboardHomeResponse response = new DashboardHomeResponse(
                lifecycleService.getStateManager().getLifecycleState().name(),
                lifecycleService.getStateManager().getConnectionState().name(),
                clusterName,
                hostname,
                metrics.getCpuUsagePercent(),
                metrics.getCpuCores(),
                metrics.getUsedMemoryMb(),
                metrics.getTotalMemoryMb(),
                metrics.getUsedStorageMb(),
                metrics.getTotalStorageMb(),
                metrics.getGpuCount(),
                taskService.getActiveTasks().size(),
                taskService.getQueuedTasks().size()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/node")
    public ResponseEntity<DashboardNodeResponse> getNodeDashboard() {
        SystemMetrics metrics = metricsProvider.collectMetrics();
        
        DashboardNodeResponse response = new DashboardNodeResponse(
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                metricsProvider.getCpuMetricsProvider().getProcessorIdentifier(),
                metrics.getCpuUsagePercent(),
                metrics.getUsedMemoryMb(),
                metrics.getTotalMemoryMb(),
                metrics.getUsedStorageMb(),
                metrics.getTotalStorageMb(),
                metrics.getGpuCount(),
                metrics.getNetworkBytesSent(),
                metrics.getNetworkBytesReceived(),
                "0.0.1-SNAPSHOT"
        );

        return ResponseEntity.ok(response);
    }
}
