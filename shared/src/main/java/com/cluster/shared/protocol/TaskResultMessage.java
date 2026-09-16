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
}
