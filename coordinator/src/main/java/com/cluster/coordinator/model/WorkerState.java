package com.cluster.coordinator.model;

public enum WorkerState {
    REGISTERING,
    ONLINE,
    BUSY,
    HEARTBEAT_TIMEOUT,
    OFFLINE,
    UNHEALTHY
}
