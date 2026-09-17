package com.cluster.coordinator.service;

import com.cluster.coordinator.model.*;
import com.cluster.coordinator.repository.*;
import com.cluster.shared.protocol.TaskResultMessage;
import com.cluster.shared.protocol.TaskStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

@Service
public class TaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskResultRepository taskResultRepository;
    private final JobRepository jobRepository;
    private final ResourceReservationService resourceReservationService;
    private final ObjectMapper objectMapper;

    public TaskExecutionService(TaskRepository taskRepository,
                                TaskAssignmentRepository taskAssignmentRepository,
                                TaskResultRepository taskResultRepository,
                                JobRepository jobRepository,
                                ResourceReservationService resourceReservationService,
                                ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskResultRepository = taskResultRepository;
        this.jobRepository = jobRepository;
        this.resourceReservationService = resourceReservationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processTaskStatus(String workerId, TaskStatusMessage msg) {
        log.info("Processing task status for task {}: {}", msg.getTaskId(), msg.getState());
        
        Task task = taskRepository.findById(msg.getTaskId()).orElse(null);
        if (task == null) {
            log.warn("Received status for unknown task {}", msg.getTaskId());
            return;
        }

        // Validate Identity
        if (msg.getJobId() != null && !msg.getJobId().equals(task.getJobId())) {
            log.warn("Job ID mismatch for task {}. Expected {}, got {}", task.getId(), task.getJobId(), msg.getJobId());
            return;
        }
        if (msg.getPartitionId() != null && !msg.getPartitionId().equals(task.getPartitionId())) {
            log.warn("Partition ID mismatch for task {}. Expected {}, got {}", task.getId(), task.getPartitionId(), msg.getPartitionId());
            return;
        }

        TaskAssignment assignment = taskAssignmentRepository.findByTaskId(task.getId()).orElse(null);
        if (assignment == null) {
            log.warn("Received status for task {} without assignment", task.getId());
            return;
        }
        if (workerId != null && !workerId.equals(assignment.getWorkerId())) {
            log.warn("Worker ID mismatch for task {}. Assigned to {}, but message from {}", task.getId(), assignment.getWorkerId(), workerId);
            return;
        }

        TaskState newState;
        try {
            newState = TaskState.valueOf(msg.getState());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown task state: {}", msg.getState());
            return;
        }

        // Validate Transitions (Idempotency and anti-backwards)
        if (isTerminalState(task.getState())) {
            log.debug("Task {} is already terminal ({}). Ignoring status update to {}", task.getId(), task.getState(), newState);
            return; // Ignore duplicate/backward transitions from terminal
        }
        
        if (task.getState() == TaskState.RUNNING && newState == TaskState.ASSIGNED) {
            log.warn("Invalid backward transition from RUNNING to ASSIGNED for task {}", task.getId());
            return;
        }

        // Apply transition
        task.setState(newState);
        taskRepository.save(task);

        if (newState == TaskState.RUNNING) {
            assignment.setState(AssignmentState.ACTIVE);
            taskAssignmentRepository.save(assignment);
        } else if (newState == TaskState.COMPLETED) {
            assignment.setState(AssignmentState.COMPLETED);
            taskAssignmentRepository.save(assignment);
            resourceReservationService.release(workerId, task.getId());
            // Progress is managed in processTaskResult typically, but if status comes without result?
            // Usually we rely on TASK_RESULT for completion logic, but to be robust, we don't duplicate increment.
            // Wait, prompt says: "Job.completedPartitions += 1. This increment must happen exactly once."
            // We should rely on TASK_RESULT for logical completion if it provides output. But if TASK_STATUS is COMPLETED and no RESULT? 
            // In Phase 9B, Worker Agent sends TASK_RESULT before or after TASK_STATUS (usually result then status).
            // We will let `processTaskResult` handle the increment, or handle it here if `processTaskResult` wasn't called yet?
            // To ensure it happens exactly once, we can increment ONLY in processTaskResult. 
            // The prompt says: "When a Task successfully completes: Job.completedPartitions += 1... Only then Job = COMPLETED".
            // It also says: "Do NOT mark the Job completed based merely on a successful TASK_ASSIGN or TASK_STATUS RUNNING message".
            // Let's do completion logic fully in processTaskResult. But if STATUS comes first, we can just update states.
        } else if (newState == TaskState.FAILED) {
            assignment.setState(AssignmentState.FAILED);
            taskAssignmentRepository.save(assignment);
            resourceReservationService.release(workerId, task.getId());
            markJobFailed(task.getJobId());
        } else if (newState == TaskState.CANCELLED) {
            assignment.setState(AssignmentState.CANCELLED);
            taskAssignmentRepository.save(assignment);
            resourceReservationService.release(workerId, task.getId());
            // Prompt: "Do not automatically mark the entire Job CANCELLED unless the existing project semantics explicitly require it."
        }
    }

    @Transactional
    public void processTaskResult(String workerId, TaskResultMessage msg) {
        log.info("Processing task result for task {}: {}", msg.getTaskId(), msg.getStatus());

        Task task = taskRepository.findById(msg.getTaskId()).orElse(null);
        if (task == null) {
            log.warn("Received result for unknown task {}", msg.getTaskId());
            return;
        }

        // Validate Identity
        if (msg.getJobId() != null && !msg.getJobId().equals(task.getJobId())) {
            log.warn("Job ID mismatch for task result {}. Expected {}, got {}", task.getId(), task.getJobId(), msg.getJobId());
            return;
        }
        if (msg.getPartitionId() != null && !msg.getPartitionId().equals(task.getPartitionId())) {
            log.warn("Partition ID mismatch for task result {}. Expected {}, got {}", task.getId(), task.getPartitionId(), msg.getPartitionId());
            return;
        }

        TaskAssignment assignment = taskAssignmentRepository.findByTaskId(task.getId()).orElse(null);
        if (assignment == null) {
            log.warn("Received result for task {} without assignment", task.getId());
            return;
        }
        if (workerId != null && !workerId.equals(assignment.getWorkerId())) {
            log.warn("Worker ID mismatch for task result {}. Assigned to {}, but message from {}", task.getId(), assignment.getWorkerId(), workerId);
            return;
        }

        // Check Idempotency
        if (taskResultRepository.findByTaskId(task.getId()).isPresent()) {
            log.debug("Duplicate result received for task {}. Ignoring.", task.getId());
            return;
        }

        // Persist Result
        TaskResult tr = new TaskResult();
        tr.setId(UUID.randomUUID().toString());
        tr.setTaskId(task.getId());
        tr.setJobId(task.getJobId());
        tr.setPartitionId(task.getPartitionId());
        tr.setWorkerId(workerId);
        tr.setStatus(msg.getStatus());
        tr.setError(msg.getError());
        tr.setExecutionDurationMs(msg.getExecutionDurationMs());
        
        try {
            tr.setOutput(objectMapper.writeValueAsString(msg.getResult()));
        } catch (Exception e) {
            log.error("Failed to serialize task result output", e);
            tr.setOutput(String.valueOf(msg.getResult()));
        }
        
        taskResultRepository.save(tr);

        // Update states and resources if not already handled by status
        if (!isTerminalState(task.getState())) {
            TaskState resultState = parseState(msg.getStatus(), TaskState.COMPLETED);
            task.setState(resultState);
            taskRepository.save(task);

            if (resultState == TaskState.COMPLETED) {
                assignment.setState(AssignmentState.COMPLETED);
            } else if (resultState == TaskState.FAILED) {
                assignment.setState(AssignmentState.FAILED);
            } else if (resultState == TaskState.CANCELLED) {
                assignment.setState(AssignmentState.CANCELLED);
            }
            taskAssignmentRepository.save(assignment);

            resourceReservationService.release(workerId, task.getId());
        }

        // Job Progress (Only increment on SUCCESS and EXACTLY ONCE per task)
        // Since we checked idempotency for duplicate results above, we know this is the first time we process this result.
        if ("COMPLETED".equalsIgnoreCase(msg.getStatus())) {
            Job job = jobRepository.findById(task.getJobId()).orElse(null);
            if (job != null && job.getState() != JobState.COMPLETED && job.getState() != JobState.FAILED) {
                job.setCompletedPartitions(job.getCompletedPartitions() + 1);
                
                if (job.getCompletedPartitions() == job.getTotalPartitions()) {
                    job.setState(JobState.COMPLETED);
                    log.info("Job {} fully completed", job.getId());
                }
                jobRepository.save(job);
            }
        } else if ("FAILED".equalsIgnoreCase(msg.getStatus())) {
            markJobFailed(task.getJobId());
        }
    }

    private void markJobFailed(String jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job != null && job.getState() != JobState.FAILED && job.getState() != JobState.COMPLETED) {
            job.setState(JobState.FAILED);
            jobRepository.save(job);
            log.info("Job {} failed due to task failure", job.getId());
        }
    }

    private boolean isTerminalState(TaskState state) {
        return state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.CANCELLED;
    }
    
    private TaskState parseState(String status, TaskState defaultState) {
        try {
            return TaskState.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            return defaultState;
        }
    }
}
