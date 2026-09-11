package com.cluster.coordinator.dto;

public class ClusterResourcesResponse {

    private long totalCpuCores;
    private long totalMemoryRamMb;
    private long totalGpuCount;
    private long totalStorageMb;
    private long totalNetworkBps;

    public ClusterResourcesResponse(long totalCpuCores, long totalMemoryRamMb, long totalGpuCount, long totalStorageMb, long totalNetworkBps) {
        this.totalCpuCores = totalCpuCores;
        this.totalMemoryRamMb = totalMemoryRamMb;
        this.totalGpuCount = totalGpuCount;
        this.totalStorageMb = totalStorageMb;
        this.totalNetworkBps = totalNetworkBps;
    }

    // Getters and Setters
    public long getTotalCpuCores() { return totalCpuCores; }
    public void setTotalCpuCores(long totalCpuCores) { this.totalCpuCores = totalCpuCores; }
    public long getTotalMemoryRamMb() { return totalMemoryRamMb; }
    public void setTotalMemoryRamMb(long totalMemoryRamMb) { this.totalMemoryRamMb = totalMemoryRamMb; }
    public long getTotalGpuCount() { return totalGpuCount; }
    public void setTotalGpuCount(long totalGpuCount) { this.totalGpuCount = totalGpuCount; }
    public long getTotalStorageMb() { return totalStorageMb; }
    public void setTotalStorageMb(long totalStorageMb) { this.totalStorageMb = totalStorageMb; }
    public long getTotalNetworkBps() { return totalNetworkBps; }
    public void setTotalNetworkBps(long totalNetworkBps) { this.totalNetworkBps = totalNetworkBps; }
}
