package com.cluster.worker.monitoring;

import org.springframework.stereotype.Component;

@Component
public class DummyGpuMetricsProvider implements GpuMetricsProvider {

    @Override
    public int getGpuCount() {
        return 0; // Hardware GPU detection is complex and beyond OSHI's basic capabilities without external libs
    }
}
