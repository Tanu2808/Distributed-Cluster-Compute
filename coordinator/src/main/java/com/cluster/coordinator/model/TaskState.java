package com.cluster.coordinator.model;

public enum TaskState {
    UNASSIGNED,
    ASSIGNED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
