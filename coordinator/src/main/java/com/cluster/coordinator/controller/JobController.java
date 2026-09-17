package com.cluster.coordinator.controller;

import com.cluster.coordinator.dto.CreateJobRequest;
import com.cluster.coordinator.dto.JobResponse;
import com.cluster.coordinator.dto.TaskResponse;
import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.service.JobService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public JobController(JobService jobService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        String inputJson;
        try {
            inputJson = objectMapper.writeValueAsString(request.getInput());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid input JSON format", e);
        }

        Job job = jobService.createJob(
                "Job-" + System.currentTimeMillis(), // Or any auto-generated name since name wasn't in CreateJobRequest
                request.getTaskType(),
                inputJson,
                request.getRequestedCpu(),
                request.getRequestedMemory()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToJobResponse(job));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJob(@PathVariable String jobId) {
        return jobService.getJob(jobId)
                .map(this::mapToJobResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/tasks")
    public ResponseEntity<List<TaskResponse>> getJobTasks(@PathVariable String jobId) {
        if (jobService.getJob(jobId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<TaskResponse> tasks = jobService.getTasksForJob(jobId).stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(tasks);
    }

    private JobResponse mapToJobResponse(Job job) {
        JobResponse response = new JobResponse();
        response.setJobId(job.getId());
        response.setTaskType(job.getTaskType());
        response.setState(job.getState());
        response.setRequestedCpu(job.getRequestedCpu());
        response.setRequestedMemory(job.getRequestedMemory());
        response.setTotalPartitions(job.getTotalPartitions());
        response.setCompletedPartitions(job.getCompletedPartitions());
        return response;
    }

    private TaskResponse mapToTaskResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getId());
        response.setJobId(task.getJobId());
        response.setPartitionId(task.getPartitionId());
        response.setTaskType(task.getTaskType());
        response.setRequiredCpu(task.getRequiredCpu());
        response.setRequiredMemory(task.getRequiredMemory());
        response.setState(task.getState());
        try {
            response.setInput(objectMapper.readValue(task.getInput(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {}));
        } catch (Exception e) {
            // Ignore mapping error and leave input null if we can't parse it
        }
        return response;
    }
}
