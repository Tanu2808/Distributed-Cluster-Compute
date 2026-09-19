package com.cluster.coordinator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id private String id;

    private String jobId;

    private String taskType;

    @Column(columnDefinition = "TEXT")
    private String input;

    private Integer partitionId;

    private int requiredCpu;

    private long requiredMemory;

    @Enumerated(EnumType.STRING)
    private TaskState state;

    // Deprecated: preserved for backward compatibility
    private String command;

    // Deprecated: preserved for backward compatibility
    private String status;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(Integer partitionId) {
        this.partitionId = partitionId;
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

    @Deprecated
    public String getCommand() {
        return command;
    }

    @Deprecated
    public void setCommand(String command) {
        this.command = command;
    }

    @Deprecated
    public String getStatus() {
        return status;
    }

    @Deprecated
    public void setStatus(String status) {
        this.status = status;
    }
}
