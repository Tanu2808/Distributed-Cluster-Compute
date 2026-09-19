package com.cluster.coordinator.controller;

import com.cluster.coordinator.service.cluster.ClusterEnrollmentService;
import com.cluster.coordinator.service.WorkerService;
import com.cluster.coordinator.dto.WorkerResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ClusterEnrollmentService enrollmentService;
    private final WorkerService workerService;

    public ClusterController(ClusterEnrollmentService enrollmentService, WorkerService workerService) {
        this.enrollmentService = enrollmentService;
        this.workerService = workerService;
    }

    @GetMapping("/join-code")
    public ResponseEntity<Map<String, String>> getJoinCode() {
        return ResponseEntity.ok(Map.of("joinCode", enrollmentService.getJoinCode()));
    }

    @PostMapping("/join-code/rotate")
    public ResponseEntity<Map<String, String>> rotateJoinCode() {
        return ResponseEntity.ok(Map.of("joinCode", enrollmentService.rotateJoinCode()));
    }

    @PostMapping("/enroll")
    public ResponseEntity<Map<String, String>> enrollWorker(@RequestBody Map<String, String> payload) {
        String joinCode = payload.get("joinCode");
        String workerId = payload.get("workerId");
        
        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker(joinCode, workerId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "clusterId", result.getClusterId(),
                    "coordinatorUrl", result.getCoordinatorUrl(),
                    "runtimeCredential", result.getRuntimeCredential()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/workers")
    public ResponseEntity<List<Map<String, Object>>> getClusterWorkers() {
        List<Map<String, Object>> response = workerService.getAllWorkerDtos().stream().map(worker -> Map.<String, Object>of(
            "workerId", worker.getId(),
            "state", worker.getState().name(),
            "activeTasks", worker.getActiveTasks(),
            "cpuCores", worker.getCpuCores(),
            "memoryRamMb", worker.getMemoryRamMb(),
            "storageMb", worker.getStorageMb()
        )).collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
}
