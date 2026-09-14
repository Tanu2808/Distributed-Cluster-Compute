package com.cluster.worker.api;

import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.api.dto.WorkerSettingsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final WorkerConfig config;

    public SettingsController(WorkerConfig config) {
        this.config = config;
    }

    @GetMapping
    public ResponseEntity<WorkerSettingsResponse> getSettings() {
        return ResponseEntity.ok(new WorkerSettingsResponse(
            config.getName() != null ? config.getName() : "unnamed-worker",
            config.getCoordinator().getUrl(),
            config.getHeartbeat().getIntervalMs(),
            config.getMetrics().getIntervalMs()
        ));
    }
}
