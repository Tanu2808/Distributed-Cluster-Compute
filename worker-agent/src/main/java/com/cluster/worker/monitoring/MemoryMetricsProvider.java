package com.cluster.worker.monitoring;

public interface MemoryMetricsProvider {
    long getTotalMemoryBytes();
    long getAvailableMemoryBytes();
    long getUsedMemoryBytes();
}
