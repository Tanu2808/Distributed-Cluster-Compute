package com.cluster.coordinator.service.partitioner;

import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.Task;

import java.util.List;

/**
 * Strategy interface for partitioning logical jobs into executable tasks.
 */
public interface JobPartitioner {
    
    /**
     * @param taskType the type of the task
     * @return true if this partitioner supports the given task type
     */
    boolean supports(String taskType);
    
    /**
     * Partitions the logical job into a list of tasks.
     * @param job the job to partition
     * @param targetPartitions the requested number of partitions
     * @return the list of generated tasks
     */
    List<Task> partition(Job job, int targetPartitions);
}
