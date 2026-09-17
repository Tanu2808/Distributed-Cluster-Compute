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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OshiSystemMetricsProvider.class);

    @Override
    public SystemMetrics collectMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        
        try {
            metrics.setCpuCores(cpuMetricsProvider.getCoreCount());
            metrics.setCpuUsagePercent(cpuMetricsProvider.getCpuUsagePercent());
        } catch (Exception e) {
            log.debug("Failed to collect CPU metrics: {}", e.getMessage());
            metrics.setCpuCores(Runtime.getRuntime().availableProcessors());
            metrics.setCpuUsagePercent(0.0);
        }
        
        try {
            metrics.setTotalMemoryMb(memoryMetricsProvider.getTotalMemoryBytes() / (1024 * 1024));
            metrics.setUsedMemoryMb(memoryMetricsProvider.getUsedMemoryBytes() / (1024 * 1024));
        } catch (Exception e) {
            log.debug("Failed to collect memory metrics: {}", e.getMessage());
            metrics.setTotalMemoryMb(Runtime.getRuntime().maxMemory() / (1024 * 1024));
            metrics.setUsedMemoryMb((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
        }
        
        try {
            metrics.setTotalStorageMb(diskMetricsProvider.getTotalDiskBytes() / (1024 * 1024));
            metrics.setUsedStorageMb(diskMetricsProvider.getUsedDiskBytes() / (1024 * 1024));
        } catch (Exception e) {
            log.debug("Failed to collect disk metrics: {}", e.getMessage());
            metrics.setTotalStorageMb(0);
            metrics.setUsedStorageMb(0);
        }
        
        try {
            metrics.setNetworkBytesSent(networkMetricsProvider.getBytesSent());
            metrics.setNetworkBytesReceived(networkMetricsProvider.getBytesReceived());
        } catch (Exception e) {
            log.debug("Failed to collect network metrics: {}", e.getMessage());
            metrics.setNetworkBytesSent(0);
            metrics.setNetworkBytesReceived(0);
        }
        
        try {
            metrics.setGpuCount(gpuMetricsProvider.getGpuCount());
        } catch (Exception e) {
            log.debug("Failed to collect GPU metrics: {}", e.getMessage());
            metrics.setGpuCount(0);
        }
        
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
