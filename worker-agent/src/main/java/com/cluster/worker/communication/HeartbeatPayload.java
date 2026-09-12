package com.cluster.worker.communication;

import java.time.LocalDateTime;
import java.util.Map;

public class HeartbeatPayload {
    private LocalDateTime timestamp;
    private double cpuUsagePercent;
    private double memoryUsagePercent;
    private int activeTasks;
    private Map<String, Object> resourceInformation;

    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    public double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }
    public int getActiveTasks() { return activeTasks; }
    public void setActiveTasks(int activeTasks) { this.activeTasks = activeTasks; }
    public Map<String, Object> getResourceInformation() { return resourceInformation; }
    public void setResourceInformation(Map<String, Object> resourceInformation) { this.resourceInformation = resourceInformation; }
}
