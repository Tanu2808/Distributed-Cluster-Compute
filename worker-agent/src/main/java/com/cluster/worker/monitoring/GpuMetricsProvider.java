package com.cluster.worker.monitoring;

public interface GpuMetricsProvider {
    int getGpuCount();
    // In a real scenario, this could return list of GPU names, VRAM, usage etc.
}
