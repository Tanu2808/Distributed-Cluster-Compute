package com.cluster.worker.model;

public enum WorkerLifecycleState {
    STARTING,
    INITIALIZING,
    SETUP_REQUIRED,
    LOADING_CONFIGURATION,
    CONFIGURED,
    STOPPING
}
