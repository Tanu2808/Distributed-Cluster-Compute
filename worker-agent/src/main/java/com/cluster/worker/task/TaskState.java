package com.cluster.worker.task;

public enum TaskState {
    RECEIVED,
    VALIDATING,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED
}
