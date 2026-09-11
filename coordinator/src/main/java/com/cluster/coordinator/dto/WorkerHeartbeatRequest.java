package com.cluster.coordinator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public class WorkerHeartbeatRequest {

    @Min(0)
    @Max(100)
    private double cpuUsagePercent;

    @Min(0)
    @Max(100)
    private double memoryUsagePercent;

    @PositiveOrZero
    private int activeTasks;

    // Getters and Setters
    public double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    public double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }
    public int getActiveTasks() { return activeTasks; }
    public void setActiveTasks(int activeTasks) { this.activeTasks = activeTasks; }
}
