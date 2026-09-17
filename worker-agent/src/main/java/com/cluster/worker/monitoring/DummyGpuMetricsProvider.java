package com.cluster.worker.monitoring;

import org.springframework.stereotype.Component;

@Component
public class DummyGpuMetricsProvider implements GpuMetricsProvider {

    @Override
    public int getGpuCount() {
        return 0;
    }

    @Override
    public String getGpuInfo() {
        return "Unavailable";
    }
}
