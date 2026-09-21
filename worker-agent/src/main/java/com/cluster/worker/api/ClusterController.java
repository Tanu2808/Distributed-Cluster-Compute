package com.cluster.worker.api;

import com.cluster.worker.model.cluster.ClusterEnrollment;
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

    @PostMapping("/connect")
    public ResponseEntity<ClusterEnrollment> connectToCoordinator(@RequestBody Map<String, String> payload) {
        String url = payload.get("coordinatorUrl");
        ClusterEnrollment result = setupService.connectToCoordinator(url);

        if (result.getStatus() == ClusterEnrollment.Status.FAILED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }
}
