package com.cluster.worker.execution;

import com.cluster.worker.model.ExecutionResult;

public interface TaskExecutor {
    boolean canExecute(String taskType);

    ExecutionResult execute(String taskId, String taskPayload);
}
