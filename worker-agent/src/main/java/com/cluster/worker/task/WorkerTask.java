package com.cluster.worker.task;

import java.time.Instant;
import java.util.Map;

public class WorkerTask {
    private String taskId;
    private String taskType;
    private Map<String, Object> input;
    
    // Resource requirements
    private int requiredCpuCores;
    private long requiredMemoryMb;
    private long timeoutSeconds;
    
    // State tracking
    private TaskState state;
    private String errorMessage;
    private Object result;
    
    // Metrics
    private Instant receivedAt;
    private Instant startedAt;
    private Instant completedAt;

    public WorkerTask() {
        this.state = TaskState.RECEIVED;
        this.receivedAt = Instant.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public int getRequiredCpuCores() {
        return requiredCpuCores;
    }

    public void setRequiredCpuCores(int requiredCpuCores) {
        this.requiredCpuCores = requiredCpuCores;
    }

    public long getRequiredMemoryMb() {
        return requiredMemoryMb;
    }

    public void setRequiredMemoryMb(long requiredMemoryMb) {
        this.requiredMemoryMb = requiredMemoryMb;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public synchronized TaskState getState() {
        return state;
    }

    public synchronized void setState(TaskState state) {
        if (this.state != null && this.state.isTerminal()) {
            return;
        }
        if (state == TaskState.RUNNING && this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        if (state != null && state.isTerminal() && this.completedAt == null) {
            this.completedAt = Instant.now();
        }
        this.state = state;
    }

    public synchronized boolean transitionState(TaskState newState) {
        if (this.state != null && this.state.isTerminal()) {
            return false;
        }
        setState(newState);
        return true;
    }

    public synchronized boolean isTerminal() {
        return state != null && state.isTerminal();
    }

    public synchronized boolean isCancelled() {
        return state == TaskState.CANCELLED;
    }

    public synchronized boolean isRunning() {
        return state == TaskState.RUNNING;
    }

    public synchronized String getErrorMessage() {
        return errorMessage;
    }

    public synchronized void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public synchronized Object getResult() {
        return result;
    }

    public synchronized void setResult(Object result) {
        this.result = result;
    }

    public synchronized Instant getReceivedAt() {
        return receivedAt;
    }

    public synchronized void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public synchronized Instant getStartedAt() {
        return startedAt;
    }

    public synchronized void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public synchronized Instant getCompletedAt() {
        return completedAt;
    }

    public synchronized void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
