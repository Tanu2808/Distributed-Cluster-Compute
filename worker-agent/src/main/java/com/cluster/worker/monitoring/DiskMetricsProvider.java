package com.cluster.worker.monitoring;

public interface DiskMetricsProvider {
    long getTotalDiskBytes();
    long getFreeDiskBytes();
    long getUsedDiskBytes();
}
