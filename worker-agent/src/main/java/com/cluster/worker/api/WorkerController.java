package com.cluster.worker.api;

import com.cluster.worker.registration.WorkerIdentityGenerator;
import com.cluster.worker.service.WorkerLifecycleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    private final WorkerLifecycleService lifecycleService;
    private final WorkerIdentityGenerator identityGenerator;

    public WorkerController(WorkerLifecycleService lifecycleService, WorkerIdentityGenerator identityGenerator) {
        this.lifecycleService = lifecycleService;
        this.identityGenerator = identityGenerator;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getWorkerStatus() {
        return ResponseEntity.ok(Map.of("status", lifecycleService.getState().name()));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getWorkerInfo() {
        return ResponseEntity.ok(Map.of("workerId", identityGenerator.getOrCreateWorkerId()));
    }
}
