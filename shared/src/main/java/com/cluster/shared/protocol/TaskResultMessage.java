package com.cluster.shared.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultMessage {
    private String taskId;
    private String status;
    private Object result;
    private long executionDurationMs;
    private String error;

    // Distributed subtask metadata (Phase 8B)
    private String jobId;
    private Integer partitionId;

    /** Backward-compatible constructor for non-partitioned task results. */
    public TaskResultMessage(
            String taskId, String status, Object result, long executionDurationMs, String error) {
        this.taskId = taskId;
        this.status = status;
        this.result = result;
        this.executionDurationMs = executionDurationMs;
        this.error = error;
    }
}
