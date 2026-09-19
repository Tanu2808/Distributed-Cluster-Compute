package com.cluster.shared.protocol;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterMessage {
    private String hostname;
    private String osName;
    private String osVersion;
    private int cpuCores;
    private String cpuInfo;
    private long memoryMb;
    private String gpuInfo;
    private String storageInfo;
    private String architecture;
    private String agentVersion;
    private Map<String, Object> tags;
}
