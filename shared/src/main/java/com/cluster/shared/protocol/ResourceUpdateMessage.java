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
    private Double cpuUsagePercent;
    private Long memoryUsedBytes;
    private Long memoryTotalBytes;
    private Double gpuUsagePercent;
    private Long diskFreeBytes;
    private Long diskTotalBytes;
}
