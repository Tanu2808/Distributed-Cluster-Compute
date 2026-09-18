package com.cluster.worker.task;

public enum TaskState {
    UNASSIGNED,
    ASSIGNED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
