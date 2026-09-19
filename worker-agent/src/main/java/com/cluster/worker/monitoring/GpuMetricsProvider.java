package com.cluster.worker.monitoring;

public interface GpuMetricsProvider {
    int getGpuCount();

    default String getGpuInfo() {
        int count = getGpuCount();
        return count > 0 ? count + " GPU(s) detected" : "Unavailable";
    }
}
