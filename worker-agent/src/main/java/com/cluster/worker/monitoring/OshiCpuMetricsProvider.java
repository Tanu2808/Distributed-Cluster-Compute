package com.cluster.worker.monitoring;

import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

@Component
public class OshiCpuMetricsProvider implements CpuMetricsProvider {

    private final CentralProcessor processor;

    public OshiCpuMetricsProvider(SystemInfo systemInfo) {
        this.processor = systemInfo.getHardware().getProcessor();
    }

    @Override
    public int getCoreCount() {
        return processor.getPhysicalProcessorCount();
    }

    @Override
    public int getLogicalProcessorCount() {
        return processor.getLogicalProcessorCount();
    }

    @Override
    public double getCpuUsagePercent() {
        // Simple usage estimation. OSHI usually requires waiting a bit between ticks.
        // A full implementation would store prev ticks. For simplicity, we just return system load
        // or a proxy.
        double[] load = processor.getSystemLoadAverage(1);
        if (load[0] < 0) {
            return 0.0;
        }
        return (load[0] / getLogicalProcessorCount()) * 100.0;
    }

    @Override
    public double[] getSystemLoadAverage() {
        return processor.getSystemLoadAverage(3);
    }

    @Override
    public String getProcessorIdentifier() {
        return processor.getProcessorIdentifier().getName();
    }
}
