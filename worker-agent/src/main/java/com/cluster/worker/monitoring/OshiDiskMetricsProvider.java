package com.cluster.worker.monitoring;

import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

@Component
public class OshiDiskMetricsProvider implements DiskMetricsProvider {

    private final OperatingSystem os;

    public OshiDiskMetricsProvider(SystemInfo systemInfo) {
        this.os = systemInfo.getOperatingSystem();
    }

    @Override
    public long getTotalDiskBytes() {
        long total = 0;
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            total += fs.getTotalSpace();
        }
        return total;
    }

    @Override
    public long getFreeDiskBytes() {
        long free = 0;
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            free += fs.getUsableSpace();
        }
        return free;
    }

    @Override
    public long getUsedDiskBytes() {
        return getTotalDiskBytes() - getFreeDiskBytes();
    }
}
