package com.cluster.coordinator.dto;

import java.util.Map;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateJobRequest {

    @NotBlank(message = "taskType is required")
    private String taskType;

    @NotNull(message = "input is required")
    private Map<String, Object> input;

    @Min(value = 1, message = "requestedCpu must be greater than 0")
    private int requestedCpu;

    @Min(value = 0, message = "requestedMemory must not be negative")
    private long requestedMemory;

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

    public int getRequestedCpu() {
        return requestedCpu;
    }

    public void setRequestedCpu(int requestedCpu) {
        this.requestedCpu = requestedCpu;
    }

    public long getRequestedMemory() {
        return requestedMemory;
    }

    public void setRequestedMemory(long requestedMemory) {
        this.requestedMemory = requestedMemory;
    }
}
