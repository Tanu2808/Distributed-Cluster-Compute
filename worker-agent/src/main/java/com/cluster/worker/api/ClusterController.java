package com.cluster.worker.api;

import com.cluster.worker.model.cluster.ClusterConfiguration;
import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.model.cluster.JoinCode;
import com.cluster.worker.service.cluster.ClusterSetupService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ClusterSetupService setupService;

    public ClusterController(ClusterSetupService setupService) {
        this.setupService = setupService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getClusterStatus() {
        return ResponseEntity.status(501).body(Map.of("error", "Not implemented yet"));
    }

    @PostMapping("/join")
    public ResponseEntity<ClusterEnrollment> joinCluster(@RequestBody Map<String, String> payload) {
        String codeStr = payload.get("joinCode");
        try {
            JoinCode joinCode = new JoinCode(codeStr);
            ClusterEnrollment result = setupService.joinCluster(joinCode);

            if (result.getStatus() == ClusterEnrollment.Status.FAILED) {
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(
                            new ClusterEnrollment(
                                    null, ClusterEnrollment.Status.FAILED, e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCluster(@RequestBody Map<String, Object> payload) {
        String clusterName = (String) payload.get("clusterName");
        Boolean isLocal = (Boolean) payload.get("isLocal");

        if (isLocal == null) {
            isLocal = true; // default
        }

        try {
            ClusterConfiguration config = setupService.createCluster(clusterName, isLocal);
            return ResponseEntity.ok(config);
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
