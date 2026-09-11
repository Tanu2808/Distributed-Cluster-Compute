package com.cluster.coordinator.controller;

import com.cluster.coordinator.dto.ClusterResourcesResponse;
import com.cluster.coordinator.model.ClusterEvent;
import com.cluster.coordinator.service.ClusterService;
import com.cluster.coordinator.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ClusterService clusterService;
    private final EventService eventService;

    public ClusterController(ClusterService clusterService, EventService eventService) {
        this.clusterService = clusterService;
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getClusterStatus() {
        return ResponseEntity.ok(Map.of("status", "ONLINE", "version", "1.0.0"));
    }

    @GetMapping("/resources")
    public ResponseEntity<ClusterResourcesResponse> getClusterResources() {
        return ResponseEntity.ok(clusterService.getAggregateResources());
    }

    @GetMapping("/events")
    public ResponseEntity<List<ClusterEvent>> getClusterEvents() {
        return ResponseEntity.ok(eventService.getRecentEvents());
    }
}
