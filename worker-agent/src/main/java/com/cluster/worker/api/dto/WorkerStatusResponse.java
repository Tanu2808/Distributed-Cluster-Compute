package com.cluster.worker.api.dto;

public class WorkerStatusResponse {
    private String lifecycleState;
    private String connectionState;
    private String executionState;

    // legacy status for backwards compat temporarily if needed, though we should transition UI.
    private String status;

    public WorkerStatusResponse(String lifecycleState, String connectionState, String executionState) {
        this.lifecycleState = lifecycleState;
        this.connectionState = connectionState;
        this.executionState = executionState;
        this.status = lifecycleState; // fallback
    }

    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }

    public String getConnectionState() { return connectionState; }
    public void setConnectionState(String connectionState) { this.connectionState = connectionState; }

    public String getExecutionState() { return executionState; }
    public void setExecutionState(String executionState) { this.executionState = executionState; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
