package com.cluster.coordinator.controller;

import com.cluster.coordinator.service.cluster.ClusterEnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ClusterEnrollmentService enrollmentService;

    public ClusterController(ClusterEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
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
        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker(joinCode);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "clusterId", result.getClusterId(),
                    "coordinatorUrl", result.getCoordinatorUrl()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
