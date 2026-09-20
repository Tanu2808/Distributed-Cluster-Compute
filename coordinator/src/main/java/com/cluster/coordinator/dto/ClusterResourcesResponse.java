package com.cluster.coordinator.dto;

public class ClusterResourcesResponse {
    private long totalCpu;
    private long totalMemory;
    private long totalGpu;
    private long totalStorage;
    private long totalNetwork;

    public ClusterResourcesResponse(long totalCpu, long totalMemory, long totalGpu, long totalStorage, long totalNetwork) {
        this.totalCpu = totalCpu;
        this.totalMemory = totalMemory;
        this.totalGpu = totalGpu;
        this.totalStorage = totalStorage;
        this.totalNetwork = totalNetwork;
    }

    public long getTotalCpu() { return totalCpu; }
    public void setTotalCpu(long totalCpu) { this.totalCpu = totalCpu; }
    public long getTotalMemory() { return totalMemory; }
    public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }
    public long getTotalGpu() { return totalGpu; }
    public void setTotalGpu(long totalGpu) { this.totalGpu = totalGpu; }
    public long getTotalStorage() { return totalStorage; }
    public void setTotalStorage(long totalStorage) { this.totalStorage = totalStorage; }
    public long getTotalNetwork() { return totalNetwork; }
    public void setTotalNetwork(long totalNetwork) { this.totalNetwork = totalNetwork; }
}
