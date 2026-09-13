package com.cluster.worker.api;

import com.cluster.worker.registration.WorkerIdentityGenerator;
import com.cluster.worker.service.WorkerLifecycleService;
import com.cluster.worker.api.dto.WorkerStatusResponse;
import com.cluster.worker.api.dto.WorkerInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<WorkerStatusResponse> getWorkerStatus() {
        return ResponseEntity.ok(new WorkerStatusResponse(lifecycleService.getState().name()));
    }

    @GetMapping("/info")
    public ResponseEntity<WorkerInfoResponse> getWorkerInfo() {
        return ResponseEntity.ok(new WorkerInfoResponse(identityGenerator.getOrCreateWorkerId()));
    }
}
