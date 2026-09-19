package com.cluster.worker.monitoring;

import com.cluster.worker.model.SystemMetrics;

public interface SystemMetricsProvider {
    SystemMetrics collectMetrics();

    // Provide individual components
    CpuMetricsProvider getCpuMetricsProvider();

    MemoryMetricsProvider getMemoryMetricsProvider();

    GpuMetricsProvider getGpuMetricsProvider();

    DiskMetricsProvider getDiskMetricsProvider();

    NetworkMetricsProvider getNetworkMetricsProvider();
}
