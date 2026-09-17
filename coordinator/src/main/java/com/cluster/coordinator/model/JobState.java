package com.cluster.coordinator.model;

public enum JobState {
    SUBMITTED,
    PARTITIONING,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
