package com.cluster.worker.monitoring;

public interface NetworkMetricsProvider {
    long getBytesSent();
    long getBytesReceived();
    long getSpeedBps();
}
