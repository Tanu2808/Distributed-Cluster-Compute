package com.cluster.coordinator.service.partitioner;

import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.model.TaskState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MlInferenceJobPartitioner implements JobPartitioner {

    private static final String TYPE = "ML_INFERENCE";
    private final ObjectMapper objectMapper;

    public MlInferenceJobPartitioner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String taskType) {
        return TYPE.equalsIgnoreCase(taskType);
    }

    @Override
    public List<Task> partition(Job job, int targetPartitions) {
        String modelName;
        ArrayNode dataArray;

        try {
            JsonNode inputNode = objectMapper.readTree(job.getInput());
            if (!inputNode.has("model") || !inputNode.has("data")) {
                throw new IllegalArgumentException(
                        "ML_INFERENCE job requires 'model' and 'data' input parameters");
            }
            modelName = inputNode.get("model").asText();
            JsonNode dataNode = inputNode.get("data");
            if (!dataNode.isArray()) {
                throw new IllegalArgumentException("'data' must be an array of records");
            }
            dataArray = (ArrayNode) dataNode;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed JSON input for ML_INFERENCE job", e);
        }

        int totalRecords = dataArray.size();
        if (totalRecords == 0) {
            throw new IllegalArgumentException("Data array cannot be empty");
        }

        // Adjust partitions based on data size
        int actualPartitions = Math.min(targetPartitions, totalRecords);
        if (actualPartitions <= 0) actualPartitions = 1;

        List<Task> tasks = new ArrayList<>(actualPartitions);
        int recordsPerPartition = totalRecords / actualPartitions;
        int remainderRecords = totalRecords % actualPartitions;

        int baseCpuPerPartition = job.getRequestedCpu() / actualPartitions;
        int remainderCpu = job.getRequestedCpu() % actualPartitions;

        long baseMemoryPerPartition = job.getRequestedMemory() / actualPartitions;
        long remainderMemory = job.getRequestedMemory() % actualPartitions;

        int currentStart = 0;
        for (int i = 1; i <= actualPartitions; i++) {
            int currentRecords = recordsPerPartition + (i <= remainderRecords ? 1 : 0);
            int currentEnd = currentStart + currentRecords;

            int currentCpu = baseCpuPerPartition + (i <= remainderCpu ? 1 : 0);
            long currentMemory = baseMemoryPerPartition + (i <= remainderMemory ? 1 : 0);

            Task task = new Task();
            task.setId(UUID.randomUUID().toString());
            task.setJobId(job.getId());
            task.setTaskType(TYPE);
            task.setPartitionId(i);

            // Construct task input
            ObjectNode taskInputNode = objectMapper.createObjectNode();
            taskInputNode.put("model", modelName);
            ArrayNode taskDataArray = objectMapper.createArrayNode();
            for (int j = currentStart; j < currentEnd; j++) {
                taskDataArray.add(dataArray.get(j));
            }
            taskInputNode.set("data", taskDataArray);
            
            try {
                task.setInput(objectMapper.writeValueAsString(taskInputNode));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize task input", e);
            }

            task.setRequiredCpu(currentCpu);
            task.setRequiredMemory(currentMemory);
            task.setState(TaskState.UNASSIGNED);

            tasks.add(task);
            currentStart = currentEnd;
        }

        return tasks;
    }
}
