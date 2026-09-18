package com.cluster.coordinator.dto;

import com.cluster.coordinator.model.JobState;

public class JobResponse {

    private String jobId;
    private String taskType;
    private JobState state;
    private int requestedCpu;
    private long requestedMemory;
    private int totalPartitions;
    private int completedPartitions;
    private String finalResult;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
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

    public int getTotalPartitions() {
        return totalPartitions;
    }

    public void setTotalPartitions(int totalPartitions) {
        this.totalPartitions = totalPartitions;
    }

    public int getCompletedPartitions() {
        return completedPartitions;
    }

    public void setCompletedPartitions(int completedPartitions) {
        this.completedPartitions = completedPartitions;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }
}
