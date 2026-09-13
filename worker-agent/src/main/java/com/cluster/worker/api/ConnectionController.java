package com.cluster.worker.api;

import com.cluster.worker.communication.WebSocketConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/connection")
public class ConnectionController {

    private final WebSocketConnectionManager connectionManager;

    public ConnectionController(WebSocketConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {
        return ResponseEntity.ok(Map.of("connected", connectionManager.isConnected()));
    }
}
