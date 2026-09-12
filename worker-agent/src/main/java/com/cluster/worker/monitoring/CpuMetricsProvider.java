package com.cluster.worker.monitoring;

public interface CpuMetricsProvider {
    int getCoreCount();
    int getLogicalProcessorCount();
    double getCpuUsagePercent();
    double[] getSystemLoadAverage();
}
