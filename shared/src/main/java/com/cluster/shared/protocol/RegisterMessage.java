package com.cluster.shared.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterMessage {
    private String hostname;
    private String osName;
    private String osVersion;
    private int cpuCores;
    private long memoryMb;
    private Map<String, Object> tags;
}
