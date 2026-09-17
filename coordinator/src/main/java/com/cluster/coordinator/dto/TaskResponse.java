package com.cluster.coordinator.dto;

import com.cluster.coordinator.model.TaskState;
import java.util.Map;

public class TaskResponse {

    private String taskId;
    private String jobId;
    private Integer partitionId;
    private String taskType;
    private Map<String, Object> input;
    private int requiredCpu;
    private long requiredMemory;
    private TaskState state;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(Integer partitionId) {
        this.partitionId = partitionId;
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

    public int getRequiredCpu() {
        return requiredCpu;
    }

    public void setRequiredCpu(int requiredCpu) {
        this.requiredCpu = requiredCpu;
    }

    public long getRequiredMemory() {
        return requiredMemory;
    }

    public void setRequiredMemory(long requiredMemory) {
        this.requiredMemory = requiredMemory;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }
}
