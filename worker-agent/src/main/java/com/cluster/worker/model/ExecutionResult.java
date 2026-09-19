package com.cluster.worker.model;

public class ExecutionResult {
    private String taskId;
    private boolean success;
    private String output;
    private String error;

    public ExecutionResult(String taskId, boolean success, String output, String error) {
        this.taskId = taskId;
        this.success = success;
        this.output = output;
        this.error = error;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
