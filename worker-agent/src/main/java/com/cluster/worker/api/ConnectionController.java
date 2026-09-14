package com.cluster.worker.api;

import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.api.dto.ConnectionStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/connection")
public class ConnectionController {

    private final WebSocketConnectionManager connectionManager;

    public ConnectionController(WebSocketConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @GetMapping("/status")
    public ResponseEntity<ConnectionStatusResponse> getConnectionStatus() {
        return ResponseEntity.ok(new ConnectionStatusResponse(connectionManager.isConnected()));
    }
}
