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
}
