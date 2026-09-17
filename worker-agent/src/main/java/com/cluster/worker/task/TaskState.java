package com.cluster.worker.task;

public enum TaskState {
    RECEIVED,
    VALIDATING,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REJECTED || this == CANCELLED;
    }
}
