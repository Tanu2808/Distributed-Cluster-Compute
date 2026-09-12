package com.cluster.worker.model;

public enum WorkerState {
    STARTING,
    REGISTERING,
    ONLINE,
    BUSY,
    DISCONNECTED,
    STOPPING
}
