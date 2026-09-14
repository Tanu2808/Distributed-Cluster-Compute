package com.cluster.worker.api;

import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.api.dto.ConnectionDiagnosticsResponse;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.service.HeartbeatService;
import com.cluster.worker.service.WorkerStateManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worker/connection")
public class ConnectionController {

    private final WebSocketConnectionManager connectionManager;
    private final WorkerStateManager stateManager;
    private final WorkerConfigurationStore configStore;
    private final HeartbeatService heartbeatService;

    public ConnectionController(WebSocketConnectionManager connectionManager, 
                                WorkerStateManager stateManager,
                                WorkerConfigurationStore configStore,
                                HeartbeatService heartbeatService) {
        this.connectionManager = connectionManager;
        this.stateManager = stateManager;
        this.configStore = configStore;
        this.heartbeatService = heartbeatService;
    }

    @GetMapping
    public ResponseEntity<ConnectionDiagnosticsResponse> getConnectionDiagnostics() {
        String url = "";
        if (configStore.getConfig() != null && configStore.getConfig().getCoordinatorUrl() != null) {
            url = configStore.getConfig().getCoordinatorUrl();
            // In a real application, ensure URL has no secrets embedded (e.g. basic auth)
            // Assuming configStore.getConfig().getCoordinatorUrl() is just the base URL
        }

        ConnectionDiagnosticsResponse response = new ConnectionDiagnosticsResponse(
            stateManager.getConnectionState().name(),
            url,
            connectionManager.getConnectedSince(),
            heartbeatService.getLastSuccessfulHeartbeat(),
            connectionManager.getLastMessageTimestamp(),
            connectionManager.getReconnectCount(),
            connectionManager.getLastConnectionError()
        );

        return ResponseEntity.ok(response);
    }
}
