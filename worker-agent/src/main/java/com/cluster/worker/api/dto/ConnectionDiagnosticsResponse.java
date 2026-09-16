package com.cluster.worker.api.dto;

import java.time.Instant;

public record ConnectionDiagnosticsResponse(
    String connectionState,
    String coordinatorUrl,
    Instant connectedSince,
    Instant lastSuccessfulHeartbeat,
    Instant lastMessageTimestamp,
    int reconnectCount,
    String lastConnectionError
) {}
