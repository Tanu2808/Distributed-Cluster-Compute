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

    // Distributed subtask metadata (Phase 8B)
    private String jobId;
    private Integer partitionId;
    private Integer totalPartitions;

    /**
     * Backward-compatible constructor for non-partitioned task submissions.
     */
    public TaskAssignmentMessage(String taskId, String taskType, Map<String, Object> input,
                                 int requiredCpuCores, long requiredMemoryMb, long timeoutSeconds) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.input = input;
        this.requiredCpuCores = requiredCpuCores;
        this.requiredMemoryMb = requiredMemoryMb;
        this.timeoutSeconds = timeoutSeconds;
    }
}
