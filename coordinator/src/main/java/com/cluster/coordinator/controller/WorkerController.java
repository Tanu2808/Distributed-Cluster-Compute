package com.cluster.coordinator.controller;

import com.cluster.coordinator.dto.WorkerHeartbeatRequest;
import com.cluster.coordinator.dto.WorkerRegistrationRequest;
import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.service.HeartbeatService;
import com.cluster.coordinator.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    private final WorkerService workerService;
    private final HeartbeatService heartbeatService;

    public WorkerController(WorkerService workerService, HeartbeatService heartbeatService) {
        this.workerService = workerService;
        this.heartbeatService = heartbeatService;
    }

    @GetMapping
    public ResponseEntity<List<Worker>> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers());
    }

    @GetMapping("/{workerId}")
    public ResponseEntity<Worker> getWorker(@PathVariable String workerId) {
        return workerService.getWorker(workerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<Worker> registerWorker(@Valid @RequestBody WorkerRegistrationRequest request) {
        Worker worker = workerService.registerWorker(request);
        return ResponseEntity.ok(worker);
    }

    @PostMapping("/{workerId}/heartbeat")
    public ResponseEntity<Void> processHeartbeat(@PathVariable String workerId,
                                                 @Valid @RequestBody WorkerHeartbeatRequest request) {
        try {
            heartbeatService.processHeartbeat(workerId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{workerId}")
    public ResponseEntity<Void> deleteWorker(@PathVariable String workerId) {
        workerService.deleteWorker(workerId);
        return ResponseEntity.noContent().build();
    }
}
