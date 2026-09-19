package com.cluster.coordinator.service;

import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.JobState;
import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.repository.JobRepository;
import com.cluster.coordinator.repository.TaskRepository;
import com.cluster.coordinator.service.partitioner.JobPartitioner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final TaskRepository taskRepository;
    private final List<JobPartitioner> partitioners;
    private final ObjectMapper objectMapper;

    // Use requestedCpu as the default partition count strategy for now
    private static final int DEFAULT_PARTITIONS_PER_CPU = 1;

    public JobService(
            JobRepository jobRepository,
            TaskRepository taskRepository,
            List<JobPartitioner> partitioners,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
        this.partitioners = partitioners;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Job createJob(
            String name, String taskType, String input, int requestedCpu, long requestedMemory) {
        // 1. Validate request
        if (!StringUtils.hasText(taskType)) {
            throw new IllegalArgumentException("Job taskType cannot be null or empty");
        }
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException("Job input cannot be null or empty");
        }
        if (requestedCpu <= 0) {
            throw new IllegalArgumentException("Requested CPU must be greater than 0");
        }
        if (requestedMemory <= 0) {
            throw new IllegalArgumentException("Requested memory must be greater than 0");
        }

        try {
            objectMapper.readTree(input);
        } catch (Exception e) {
            throw new IllegalArgumentException("Job input must be valid JSON", e);
        }

        JobPartitioner partitioner =
                partitioners.stream()
                        .filter(p -> p.supports(taskType))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unsupported taskType: " + taskType));

        // 2. Create Job
        Job job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setName(StringUtils.hasText(name) ? name : "Job-" + job.getId().substring(0, 8));
        job.setTaskType(taskType);
        job.setInput(input);
        job.setRequestedCpu(requestedCpu);
        job.setRequestedMemory(requestedMemory);

        // 3. Assign initial state SUBMITTED
        job.setState(JobState.SUBMITTED);

        // 4. Persist Job
        job = jobRepository.save(job);

        // 5. Transition to PARTITIONING
        job.setState(JobState.PARTITIONING);
        job = jobRepository.save(job);

        // 6. Determine a deterministic partition count
        int targetPartitions = Math.max(1, requestedCpu * DEFAULT_PARTITIONS_PER_CPU);

        // 7. Call the appropriate JobPartitioner
        List<Task> tasks = partitioner.partition(job, targetPartitions);

        // 8. Persist generated Tasks
        taskRepository.saveAll(tasks);

        // 9. Set totalPartitions
        job.setTotalPartitions(tasks.size());

        // 10. Set completedPartitions = 0
        job.setCompletedPartitions(0);

        // 11. Transition Job to QUEUED
        job.setState(JobState.QUEUED);

        // 12. Persist final Job state
        job = jobRepository.save(job);

        log.info(
                "Created Job {} (type={}) with {} partitions",
                job.getId(),
                taskType,
                job.getTotalPartitions());
        return job;
    }

    public Optional<Job> getJob(String jobId) {
        return jobRepository.findById(jobId);
    }

    public List<Task> getTasksForJob(String jobId) {
        return taskRepository.findByJobId(jobId);
    }
}
