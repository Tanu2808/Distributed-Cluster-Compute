package com.cluster.worker.monitoring;

import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

@Component
public class OshiMemoryMetricsProvider implements MemoryMetricsProvider {

    private final GlobalMemory memory;

    public OshiMemoryMetricsProvider(SystemInfo systemInfo) {
        this.memory = systemInfo.getHardware().getMemory();
    }

    @Override
    public long getTotalMemoryBytes() {
        return memory.getTotal();
    }

    @Override
    public long getAvailableMemoryBytes() {
        return memory.getAvailable();
    }

    @Override
    public long getUsedMemoryBytes() {
        return memory.getTotal() - memory.getAvailable();
    }
}
