package com.cluster.worker.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkerUiApiController {

    @GetMapping("/worker/status")
    public ResponseEntity<Map<String, String>> getWorkerStatus() {
        return ResponseEntity.ok(Map.of("status", "running", "message", "Worker is active"));
    }

    @GetMapping("/cluster/status")
    public ResponseEntity<Map<String, String>> getClusterStatus() {
        return ResponseEntity.status(501).body(Map.of("error", "Not implemented yet"));
    }

    @GetMapping("/tasks/active")
    public ResponseEntity<Map<String, Object>> getActiveTasks() {
        return ResponseEntity.ok(Map.of("tasks", new Object[]{}));
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getSettings() {
        return ResponseEntity.ok(Map.of("environment", "local-worker"));
    }
}
