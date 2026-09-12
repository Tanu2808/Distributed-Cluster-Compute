package com.cluster.shared.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUpdateMessage {
    private double cpuUsagePercent;
    private long memoryUsedBytes;
    private long memoryTotalBytes;
    private double gpuUsagePercent;
    private long diskFreeBytes;
    private long diskTotalBytes;
}
