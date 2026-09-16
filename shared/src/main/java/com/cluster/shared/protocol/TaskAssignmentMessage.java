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
public class TaskAssignmentMessage {
    private String taskId;
    private String taskType;
    private Map<String, Object> input;
    private int requiredCpuCores;
    private long requiredMemoryMb;
    private long timeoutSeconds;
}
