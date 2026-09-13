package com.cluster.worker.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getClusterStatus() {
        // Placeholder for Phase 2 cluster integration
        return ResponseEntity.status(501).body(Map.of("error", "Not implemented yet"));
    }
}
