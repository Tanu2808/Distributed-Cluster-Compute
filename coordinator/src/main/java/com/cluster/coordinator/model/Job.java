package com.cluster.coordinator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jobs")
public class Job {

    @Id private String id;

    private String name;

    private String taskType;

    @Column(columnDefinition = "TEXT")
    private String input;

    private int requestedCpu;

    private long requestedMemory;

    private int totalPartitions;

    private int completedPartitions;

    @Column(columnDefinition = "TEXT")
    private String finalResult;

    @Enumerated(EnumType.STRING)
    private JobState state;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }
}
