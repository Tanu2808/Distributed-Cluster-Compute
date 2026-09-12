package com.cluster.worker.monitoring;

import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

import java.util.List;

@Component
public class OshiNetworkMetricsProvider implements NetworkMetricsProvider {

    private final List<NetworkIF> networkInterfaces;

    public OshiNetworkMetricsProvider(SystemInfo systemInfo) {
        this.networkInterfaces = systemInfo.getHardware().getNetworkIFs();
    }

    @Override
    public long getBytesSent() {
        long total = 0;
        for (NetworkIF net : networkInterfaces) {
            net.updateAttributes();
            total += net.getBytesSent();
        }
        return total;
    }

    @Override
    public long getBytesReceived() {
        long total = 0;
        for (NetworkIF net : networkInterfaces) {
            net.updateAttributes();
            total += net.getBytesRecv();
        }
        return total;
    }

    @Override
    public long getSpeedBps() {
        long total = 0;
        for (NetworkIF net : networkInterfaces) {
            total += net.getSpeed(); // Can be 0 if unknown
        }
        return total;
    }
}
