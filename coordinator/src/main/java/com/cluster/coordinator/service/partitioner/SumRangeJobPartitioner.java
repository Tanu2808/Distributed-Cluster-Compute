package com.cluster.coordinator.service.partitioner;

import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.model.TaskState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SumRangeJobPartitioner implements JobPartitioner {

    private static final String TYPE = "SUM_RANGE";
    private static final long MAX_RANGE_SPAN = 100_000_000L;
    private final ObjectMapper objectMapper;

    public SumRangeJobPartitioner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String taskType) {
        return TYPE.equalsIgnoreCase(taskType);
    }

    @Override
    public List<Task> partition(Job job, int targetPartitions) {
        long start;
        long end;
        
        try {
            JsonNode inputNode = objectMapper.readTree(job.getInput());
            if (!inputNode.has("start") || !inputNode.has("end")) {
                throw new IllegalArgumentException("SUM_RANGE job requires 'start' and 'end' input parameters");
            }
            start = inputNode.get("start").asLong();
            end = inputNode.get("end").asLong();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed JSON input for SUM_RANGE job", e);
        }

        if (start > end) {
            throw new IllegalArgumentException("Invalid range: start must be less than or equal to end");
        }

        long span = (end - start) + 1;

        // Ensure we don't violate the MAX_RANGE_SPAN safety rule
        long minPartitionsRequired = (span + MAX_RANGE_SPAN - 1) / MAX_RANGE_SPAN;
        int actualPartitions = Math.max(targetPartitions, (int) minPartitionsRequired);
        
        // Also ensure actualPartitions isn't greater than span
        actualPartitions = (int) Math.min(actualPartitions, span);
        if (actualPartitions <= 0) actualPartitions = 1;

        List<Task> tasks = new ArrayList<>(actualPartitions);
        long itemsPerPartition = span / actualPartitions;
        long remainderItems = span % actualPartitions;

        int baseCpuPerPartition = job.getRequestedCpu() / actualPartitions;
        int remainderCpu = job.getRequestedCpu() % actualPartitions;

        long baseMemoryPerPartition = job.getRequestedMemory() / actualPartitions;
        long remainderMemory = job.getRequestedMemory() % actualPartitions;

        long currentStart = start;
        for (int i = 1; i <= actualPartitions; i++) {
            long currentItems = itemsPerPartition + (i <= remainderItems ? 1 : 0);
            long currentEnd = currentStart + currentItems - 1;

            int currentCpu = baseCpuPerPartition + (i <= remainderCpu ? 1 : 0);
            long currentMemory = baseMemoryPerPartition + (i <= remainderMemory ? 1 : 0);

            Task task = new Task();
            task.setId(UUID.randomUUID().toString());
            task.setJobId(job.getId());
            task.setTaskType(TYPE);
            task.setPartitionId(i);
            
            String taskInput = String.format("{\"start\":%d,\"end\":%d}", currentStart, currentEnd);
            task.setInput(taskInput);
            
            task.setRequiredCpu(currentCpu);
            task.setRequiredMemory(currentMemory);
            task.setState(TaskState.UNASSIGNED);

            tasks.add(task);
            currentStart = currentEnd + 1;
        }

        return tasks;
    }
}
