package com.cluster.worker.api;

import com.cluster.worker.config.WorkerConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final WorkerConfig config;

    public SettingsController(WorkerConfig config) {
        this.config = config;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(Map.of(
            "workerName", config.getName() != null ? config.getName() : "unnamed-worker",
            "coordinatorUrl", config.getCoordinator().getUrl(),
            "heartbeatIntervalMs", config.getHeartbeat().getIntervalMs(),
            "metricsIntervalMs", config.getMetrics().getIntervalMs()
        ));
    }
}
