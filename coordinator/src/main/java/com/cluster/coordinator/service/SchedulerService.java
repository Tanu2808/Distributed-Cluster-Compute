package com.cluster.coordinator.service;

import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.model.TaskState;
import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerState;
import com.cluster.coordinator.repository.TaskRepository;
import com.cluster.coordinator.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final TaskRepository taskRepository;
    private final WorkerRepository workerRepository;
    private final ResourceReservationService resourceReservationService;

    public SchedulerService(TaskRepository taskRepository,
                            WorkerRepository workerRepository,
                            ResourceReservationService resourceReservationService) {
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.resourceReservationService = resourceReservationService;
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
        Worker selectedWorker = onlineWorkers.stream()
                .filter(worker -> isWorkerEligible(worker, task))
                .min(workerComparator())
                .orElse(null);

        if (selectedWorker != null) {
            log.info("Scheduling task {} to worker {}", task.getId(), selectedWorker.getId());
            resourceReservationService.reserve(selectedWorker.getId(), task.getId(), 
                                               task.getRequiredCpu(), task.getRequiredMemory());
            
            task.setState(TaskState.ASSIGNED);
            taskRepository.save(task);
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
        if (totalCpu == 0) return 1.0; // Assume full utilization if total is 0 to avoid division by zero
        int reservedCpu = resourceReservationService.getReservedCpu(workerId);
        return (double) reservedCpu / totalCpu;
    }
}
