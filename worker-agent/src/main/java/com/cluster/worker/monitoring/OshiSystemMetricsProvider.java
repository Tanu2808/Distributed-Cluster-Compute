package com.cluster.worker.monitoring;

import com.cluster.worker.model.SystemMetrics;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class OshiSystemMetricsProvider implements SystemMetricsProvider {

    private final CpuMetricsProvider cpuMetricsProvider;
    private final MemoryMetricsProvider memoryMetricsProvider;
    private final GpuMetricsProvider gpuMetricsProvider;
    private final DiskMetricsProvider diskMetricsProvider;
    private final NetworkMetricsProvider networkMetricsProvider;

    public OshiSystemMetricsProvider(CpuMetricsProvider cpuMetricsProvider,
                                     MemoryMetricsProvider memoryMetricsProvider,
                                     GpuMetricsProvider gpuMetricsProvider,
                                     DiskMetricsProvider diskMetricsProvider,
                                     NetworkMetricsProvider networkMetricsProvider) {
        this.cpuMetricsProvider = cpuMetricsProvider;
        this.memoryMetricsProvider = memoryMetricsProvider;
        this.gpuMetricsProvider = gpuMetricsProvider;
        this.diskMetricsProvider = diskMetricsProvider;
        this.networkMetricsProvider = networkMetricsProvider;
    }

    @Override
    public SystemMetrics collectMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        
        metrics.setCpuCores(cpuMetricsProvider.getCoreCount());
        metrics.setCpuUsagePercent(cpuMetricsProvider.getCpuUsagePercent());
        
        metrics.setTotalMemoryMb(memoryMetricsProvider.getTotalMemoryBytes() / (1024 * 1024));
        metrics.setUsedMemoryMb(memoryMetricsProvider.getUsedMemoryBytes() / (1024 * 1024));
        
        metrics.setTotalStorageMb(diskMetricsProvider.getTotalDiskBytes() / (1024 * 1024));
        metrics.setUsedStorageMb(diskMetricsProvider.getUsedDiskBytes() / (1024 * 1024));
        
        metrics.setNetworkBytesSent(networkMetricsProvider.getBytesSent());
        metrics.setNetworkBytesReceived(networkMetricsProvider.getBytesReceived());
        
        metrics.setGpuCount(gpuMetricsProvider.getGpuCount());
        
        metrics.setAdditionalInfo(new HashMap<>());
        
        return metrics;
    }

    @Override
    public CpuMetricsProvider getCpuMetricsProvider() { return cpuMetricsProvider; }

    @Override
    public MemoryMetricsProvider getMemoryMetricsProvider() { return memoryMetricsProvider; }

    @Override
    public GpuMetricsProvider getGpuMetricsProvider() { return gpuMetricsProvider; }

    @Override
    public DiskMetricsProvider getDiskMetricsProvider() { return diskMetricsProvider; }

    @Override
    public NetworkMetricsProvider getNetworkMetricsProvider() { return networkMetricsProvider; }
}
