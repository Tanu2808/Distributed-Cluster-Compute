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

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
