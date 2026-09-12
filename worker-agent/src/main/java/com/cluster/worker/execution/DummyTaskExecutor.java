package com.cluster.worker.execution;

import com.cluster.worker.model.ExecutionResult;
import org.springframework.stereotype.Component;

@Component
public class DummyTaskExecutor implements TaskExecutor {

    @Override
    public boolean canExecute(String taskType) {
        return "DUMMY".equalsIgnoreCase(taskType);
    }

    @Override
    public ExecutionResult execute(String taskId, String taskPayload) {
        System.out.println("Executing dummy task " + taskId + " with payload: " + taskPayload);
        try {
            Thread.sleep(1000); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionResult(taskId, false, null, "Interrupted");
        }
        return new ExecutionResult(taskId, true, "Success", null);
    }
}
