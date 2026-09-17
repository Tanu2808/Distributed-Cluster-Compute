package com.cluster.coordinator.service;

import com.cluster.coordinator.model.AssignmentState;
import com.cluster.coordinator.model.TaskAssignment;
import com.cluster.coordinator.model.WorkerResource;
import com.cluster.coordinator.repository.TaskAssignmentRepository;
import com.cluster.coordinator.repository.WorkerResourceRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceReservationService {

    private final WorkerResourceRepository workerResourceRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public ResourceReservationService(WorkerResourceRepository workerResourceRepository,
                                      TaskAssignmentRepository taskAssignmentRepository) {
        this.workerResourceRepository = workerResourceRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
    }

    public int getAvailableCpu(String workerId) {
        WorkerResource resource = workerResourceRepository.findByWorkerId(workerId).orElse(null);
        if (resource == null) {
            return 0;
        }
        return resource.getCpuCores() - getReservedCpu(workerId);
    }

    public long getAvailableMemory(String workerId) {
        WorkerResource resource = workerResourceRepository.findByWorkerId(workerId).orElse(null);
        if (resource == null) {
            return 0;
        }
        return resource.getMemoryRamMb() - getReservedMemory(workerId);
    }

    public int getReservedCpu(String workerId) {
        return getActiveAssignments(workerId).stream()
                .mapToInt(TaskAssignment::getAllocatedCpu)
                .sum();
    }

    public long getReservedMemory(String workerId) {
        return getActiveAssignments(workerId).stream()
                .mapToLong(TaskAssignment::getAllocatedMemory)
                .sum();
    }
    
    public int getTotalCpu(String workerId) {
        WorkerResource resource = workerResourceRepository.findByWorkerId(workerId).orElse(null);
        return resource != null ? resource.getCpuCores() : 0;
    }

    public void reserve(String workerId, String taskId, int cpu, long memory) {
        // Prevent duplicate TaskAssignments for the same Task
        if (taskAssignmentRepository.findByTaskId(taskId).isPresent()) {
            throw new IllegalStateException("Task " + taskId + " is already assigned/reserved.");
        }
        TaskAssignment assignment = new TaskAssignment();
        assignment.setId(UUID.randomUUID().toString());
        assignment.setTaskId(taskId);
        assignment.setWorkerId(workerId);
        assignment.setAllocatedCpu(cpu);
        assignment.setAllocatedMemory(memory);
        assignment.setState(AssignmentState.ALLOCATED);
        taskAssignmentRepository.save(assignment);
    }

    public void release(String workerId, String taskId) {
        taskAssignmentRepository.findByTaskId(taskId).ifPresent(assignment -> {
            if (assignment.getWorkerId().equals(workerId)) {
                // Do not delete, transition to CANCELLED if it is not already in a terminal state
                if (assignment.getState() == AssignmentState.ALLOCATED || assignment.getState() == AssignmentState.ACTIVE) {
                    assignment.setState(AssignmentState.CANCELLED);
                    taskAssignmentRepository.save(assignment);
                }
            }
        });
    }

    private List<TaskAssignment> getActiveAssignments(String workerId) {
        return taskAssignmentRepository.findByWorkerIdAndStateIn(workerId, 
                Arrays.asList(AssignmentState.ALLOCATED, AssignmentState.ACTIVE));
    }
}
