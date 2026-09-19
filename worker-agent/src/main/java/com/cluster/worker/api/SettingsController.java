package com.cluster.worker.api;

import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.persistence.WorkerConfiguration;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.service.WorkerStateManager;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final WorkerConfigurationStore configStore;
    private final WorkerStateManager stateManager;

    public SettingsController(
            WorkerConfigurationStore configStore, WorkerStateManager stateManager) {
        this.configStore = configStore;
        this.stateManager = stateManager;
    }

    @GetMapping("/worker")
    public ResponseEntity<Map<String, Object>> getWorkerSettings() {
        WorkerConfiguration config = configStore.getConfig();
        return ResponseEntity.ok(
                Map.of(
                        "workerId", config.getWorkerId(),
                        "version", config.getVersion()));
    }

    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> getClusterSettings() {
        WorkerConfiguration config = configStore.getConfig();
        return ResponseEntity.ok(
                Map.of(
                        "clusterId", config.getClusterId() != null ? config.getClusterId() : "",
                        "clusterName",
                                config.getClusterName() != null ? config.getClusterName() : "",
                        "coordinatorUrl",
                                config.getCoordinatorUrl() != null
                                        ? config.getCoordinatorUrl()
                                        : "",
                        "isConfigured", configStore.isConfigured()));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetConfiguration() {
        configStore.resetConfiguration();
        stateManager.transitionLifecycle(WorkerLifecycleState.SETUP_REQUIRED);
        return ResponseEntity.ok(Map.of("message", "Configuration reset successfully"));
    }
}
