package com.cluster.worker.task;

public interface TaskHandler {
    
    /**
     * Returns true if this handler can process the given task type.
     */
    boolean supports(String taskType);

    /**
     * Executes the task. This method should run synchronously and return the result.
     * The engine will run this in a bounded execution thread pool.
     */
    Object execute(WorkerTask task) throws Exception;
}
