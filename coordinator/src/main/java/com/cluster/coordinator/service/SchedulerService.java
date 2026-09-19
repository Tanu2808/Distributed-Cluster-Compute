package com.cluster.coordinator.service;

import com.cluster.coordinator.model.*;
import com.cluster.coordinator.repository.JobRepository;
import com.cluster.coordinator.repository.TaskRepository;
import com.cluster.coordinator.repository.WorkerRepository;
import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.TaskAssignmentMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodic scheduling service responsible for evaluating unassigned Tasks and matching them
 * with eligible online Workers based on resource availability (CPU and Memory).
 * Utilizes {@link ResourceReservationService} to track allocations and dispatches assignments
 * via STOMP messages.
 */
@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final TaskRepository taskRepository;
    private final WorkerRepository workerRepository;
    private final JobRepository jobRepository;
    private final ResourceReservationService resourceReservationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public SchedulerService(
            TaskRepository taskRepository,
            WorkerRepository workerRepository,
            JobRepository jobRepository,
            ResourceReservationService resourceReservationService,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.jobRepository = jobRepository;
        this.resourceReservationService = resourceReservationService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${coordinator.scheduler.interval-ms:5000}")
    public void triggerScheduling() {
        scheduleTasks();
    }

    @Transactional
    public synchronized void scheduleTasks() {
        List<Task> unassignedTasks = taskRepository.findByState(TaskState.UNASSIGNED);
        if (unassignedTasks.isEmpty()) {
            return;
        }

        List<Worker> onlineWorkers = workerRepository.findByState(WorkerState.ONLINE);
        if (onlineWorkers.isEmpty()) {
            log.debug("No online workers available for scheduling");
            return;
        }

        for (Task task : unassignedTasks) {
            scheduleTask(task, onlineWorkers);
        }
    }

    private void scheduleTask(Task task, List<Worker> onlineWorkers) {
        Worker selectedWorker =
                onlineWorkers.stream()
                        .filter(worker -> isWorkerEligible(worker, task))
                        .min(workerComparator())
                        .orElse(null);

        if (selectedWorker != null) {
            log.info("Scheduling task {} to worker {}", task.getId(), selectedWorker.getId());

            // 1. reserve/create assignment
            resourceReservationService.reserve(
                    selectedWorker.getId(), task.getId(),
                    task.getRequiredCpu(), task.getRequiredMemory());

            // 2. persist scheduling state
            task.setState(TaskState.ASSIGNED);
            taskRepository.save(task);

            Job job = jobRepository.findById(task.getJobId()).orElseThrow();

            // 3. send TASK_ASSIGN
            try {
                Map<String, Object> inputMap =
                        objectMapper.readValue(
                                task.getInput(), new TypeReference<Map<String, Object>>() {});

                TaskAssignmentMessage msgPayload =
                        TaskAssignmentMessage.builder()
                                .taskId(task.getId())
                                .jobId(job.getId())
                                .taskType(task.getTaskType())
                                .input(inputMap)
                                .requiredCpuCores(task.getRequiredCpu())
                                .requiredMemoryMb(task.getRequiredMemory())
                                .partitionId(task.getPartitionId())
                                .totalPartitions(job.getTotalPartitions())
                                .build();

                MessageEnvelope<TaskAssignmentMessage> envelope =
                        MessageEnvelope.<TaskAssignmentMessage>builder()
                                .type(MessageType.TASK_ASSIGN)
                                .workerId(selectedWorker.getId())
                                .timestamp(Instant.now())
                                .payload(msgPayload)
                                .build();

                messagingTemplate.convertAndSend(
                        "/topic/worker." + selectedWorker.getId() + ".tasks", envelope);
                log.info(
                        "Successfully dispatched TASK_ASSIGN for task {} to worker {}",
                        task.getId(),
                        selectedWorker.getId());

                if (job.getState() == JobState.QUEUED) {
                    job.setState(JobState.RUNNING);
                    jobRepository.save(job);
                    log.info("Job {} transitioned to RUNNING", job.getId());
                }

            } catch (Exception e) {
                log.error(
                        "Failed to send TASK_ASSIGN for task {} to worker {}",
                        task.getId(),
                        selectedWorker.getId(),
                        e);
                // 4. if send fails, release reservation and return Task to UNASSIGNED
                resourceReservationService.release(selectedWorker.getId(), task.getId());
                task.setState(TaskState.UNASSIGNED);
                taskRepository.save(task);
            }

        } else {
            log.debug("No eligible worker found for task {}", task.getId());
        }
    }

    private boolean isWorkerEligible(Worker worker, Task task) {
        int availableCpu = resourceReservationService.getAvailableCpu(worker.getId());
        long availableMemory = resourceReservationService.getAvailableMemory(worker.getId());
        return availableCpu >= task.getRequiredCpu() && availableMemory >= task.getRequiredMemory();
    }

    private Comparator<Worker> workerComparator() {
        return (w1, w2) -> {
            double utilization1 = calculateUtilization(w1.getId());
            double utilization2 = calculateUtilization(w2.getId());

            int compare = Double.compare(utilization1, utilization2);
            if (compare == 0) {
                // Deterministic tie-breaker
                return w1.getId().compareTo(w2.getId());
            }
            return compare;
        };
    }

    private double calculateUtilization(String workerId) {
        int totalCpu = resourceReservationService.getTotalCpu(workerId);
        if (totalCpu == 0)
            return 1.0; // Assume full utilization if total is 0 to avoid division by zero
        int reservedCpu = resourceReservationService.getReservedCpu(workerId);
        return (double) reservedCpu / totalCpu;
    }
}
