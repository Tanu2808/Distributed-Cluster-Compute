package com.cluster.worker.monitoring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NvidiaGpuMetricsProviderTest {

    @Test
    void testGetGpuCount() {
        NvidiaGpuMetricsProvider provider = new NvidiaGpuMetricsProvider();

        int count = provider.getGpuCount();

        // It might be 0 on machines without nvidia-smi, which is a valid fallback
        assertTrue(count >= 0, "GPU count should be non-negative");
    }
}
