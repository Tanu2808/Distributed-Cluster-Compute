package com.cluster.shared.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusMessage {
    private String taskId;
    private String state; // e.g., RUNNING, FAILED, COMPLETED
    private String message;

    // Distributed subtask metadata (Phase 8B/9B)
    private String jobId;
    private Integer partitionId;

    /** Backward-compatible constructor for non-partitioned task status. */
    public TaskStatusMessage(String taskId, String state, String message) {
        this.taskId = taskId;
        this.state = state;
        this.message = message;
    }
}
