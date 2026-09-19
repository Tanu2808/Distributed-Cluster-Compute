package com.cluster.worker.model;

import java.util.Map;

public class SystemMetrics {
    private int cpuCores;
    private double cpuUsagePercent;

    private long totalMemoryMb;
    private long usedMemoryMb;

    private long totalStorageMb;
    private long usedStorageMb;

    private long networkBytesSent;
    private long networkBytesReceived;

    private int gpuCount;
    private Map<String, Object> additionalInfo;

    // Getters and Setters
    public int getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(int cpuCores) {
        this.cpuCores = cpuCores;
    }

    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public long getTotalMemoryMb() {
        return totalMemoryMb;
    }

    public void setTotalMemoryMb(long totalMemoryMb) {
        this.totalMemoryMb = totalMemoryMb;
    }

    public long getUsedMemoryMb() {
        return usedMemoryMb;
    }

    public void setUsedMemoryMb(long usedMemoryMb) {
        this.usedMemoryMb = usedMemoryMb;
    }

    public long getTotalStorageMb() {
        return totalStorageMb;
    }

    public void setTotalStorageMb(long totalStorageMb) {
        this.totalStorageMb = totalStorageMb;
    }

    public long getUsedStorageMb() {
        return usedStorageMb;
    }

    public void setUsedStorageMb(long usedStorageMb) {
        this.usedStorageMb = usedStorageMb;
    }

    public long getNetworkBytesSent() {
        return networkBytesSent;
    }

    public void setNetworkBytesSent(long networkBytesSent) {
        this.networkBytesSent = networkBytesSent;
    }

    public long getNetworkBytesReceived() {
        return networkBytesReceived;
    }

    public void setNetworkBytesReceived(long networkBytesReceived) {
        this.networkBytesReceived = networkBytesReceived;
    }

    public int getGpuCount() {
        return gpuCount;
    }

    public void setGpuCount(int gpuCount) {
        this.gpuCount = gpuCount;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
