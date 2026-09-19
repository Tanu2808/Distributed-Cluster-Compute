package com.cluster.coordinator.controller;

import com.cluster.coordinator.service.WorkerService;
import com.cluster.coordinator.service.cluster.ClusterEnrollmentService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller responsible for cluster node enrollment and managing worker metadata.
 * Provides endpoints for joining the cluster, rotating join codes, and retrieving active workers.
 */
@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ClusterEnrollmentService enrollmentService;
    private final WorkerService workerService;

    public ClusterController(
            ClusterEnrollmentService enrollmentService, WorkerService workerService) {
        this.enrollmentService = enrollmentService;
        this.workerService = workerService;
    }

    /**
     * Retrieves the current join code required for new workers to authenticate during enrollment.
     * @return A map containing the active join code.
     */
    @GetMapping("/join-code")
    public ResponseEntity<Map<String, String>> getJoinCode() {
        return ResponseEntity.ok(Map.of("joinCode", enrollmentService.getJoinCode()));
    }

    /**
     * Rotates the cluster join code, invalidating the previous one.
     * Any new worker will require the newly generated code to join.
     * @return A map containing the newly generated join code.
     */
    @PostMapping("/join-code/rotate")
    public ResponseEntity<Map<String, String>> rotateJoinCode() {
        return ResponseEntity.ok(Map.of("joinCode", enrollmentService.rotateJoinCode()));
    }

    /**
     * Enrolls a new worker into the cluster using the current join code.
     * If successful, returns the cluster coordinates and runtime credentials for WebSocket connection.
     * 
     * @param payload Map containing the "joinCode" and "workerId".
     * @return Connection details if authorized, or 401 Unauthorized if the join code is invalid.
     */
    @PostMapping("/enroll")
    public ResponseEntity<Map<String, String>> enrollWorker(
            @RequestBody Map<String, String> payload) {
        String joinCode = payload.get("joinCode");
        String workerId = payload.get("workerId");

        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker(joinCode, workerId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(
                    Map.of(
                            "clusterId", result.getClusterId(),
                            "coordinatorUrl", result.getCoordinatorUrl(),
                            "runtimeCredential", result.getRuntimeCredential()));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Retrieves a summarized view of all registered cluster workers and their physical capabilities.
     * Used by the frontend dashboard to display active nodes.
     * 
     * @return List of summarized worker data objects.
     */
    @GetMapping("/workers")
    public ResponseEntity<List<Map<String, Object>>> getClusterWorkers() {
        List<Map<String, Object>> response =
                workerService.getAllWorkerDtos().stream()
                        .map(
                                worker ->
                                        Map.<String, Object>of(
                                                "workerId", worker.getId(),
                                                "state", worker.getState().name(),
                                                "activeTasks", worker.getActiveTasks(),
                                                "cpuCores", worker.getCpuCores(),
                                                "memoryRamMb", worker.getMemoryRamMb(),
                                                "storageMb", worker.getStorageMb()))
                        .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
