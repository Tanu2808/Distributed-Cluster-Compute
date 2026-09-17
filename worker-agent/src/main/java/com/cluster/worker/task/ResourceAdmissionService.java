package com.cluster.worker.task;

import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.SystemMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResourceAdmissionService {

    private static final Logger log = LoggerFactory.getLogger(ResourceAdmissionService.class);

    private final ConcurrentHashMap<String, ResourceReservation> reservations = new ConcurrentHashMap<>();

    public static class ResourceReservation {
        private final long memoryMb;
        private final int cpuCores;

        public ResourceReservation(long memoryMb, int cpuCores) {
            this.memoryMb = memoryMb;
            this.cpuCores = cpuCores;
        }

        public long getMemoryMb() { return memoryMb; }
        public int getCpuCores() { return cpuCores; }
    }

    public synchronized boolean tryReserve(WorkerTask task, SystemMetrics currentMetrics, WorkerConfig.Execution executionConfig) {
        if (task == null || task.getTaskId() == null) {
            return false;
        }

        long allocatedMem = getAllocatedMemoryMb();
        int allocatedCpu = getAllocatedCpuCores();

        boolean admitted = canAdmit(task, allocatedMem, allocatedCpu, currentMetrics, executionConfig);
        if (admitted) {
            reservations.put(task.getTaskId(), new ResourceReservation(
                    Math.max(0, task.getRequiredMemoryMb()),
                    Math.max(0, task.getRequiredCpuCores())
            ));
            log.info("Reserved resources for task {}: {} MB RAM, {} CPU cores. Total allocated: {} MB, {} cores",
                    task.getTaskId(), task.getRequiredMemoryMb(), task.getRequiredCpuCores(),
                    getAllocatedMemoryMb(), getAllocatedCpuCores());
            return true;
        }

        return false;
    }

    public synchronized boolean release(String taskId) {
        if (taskId == null) {
            return false;
        }
        ResourceReservation removed = reservations.remove(taskId);
        if (removed != null) {
            log.info("Released resources for task {}: {} MB RAM, {} CPU cores. Total allocated: {} MB, {} cores",
                    taskId, removed.getMemoryMb(), removed.getCpuCores(),
                    getAllocatedMemoryMb(), getAllocatedCpuCores());
            return true;
        }
        return false;
    }

    public synchronized boolean release(WorkerTask task) {
        if (task != null && task.getTaskId() != null) {
            return release(task.getTaskId());
        }
        return false;
    }

    public synchronized long getAllocatedMemoryMb() {
        return reservations.values().stream().mapToLong(ResourceReservation::getMemoryMb).sum();
    }

    public synchronized int getAllocatedCpuCores() {
        return reservations.values().stream().mapToInt(ResourceReservation::getCpuCores).sum();
    }

    public synchronized boolean hasReservation(String taskId) {
        return taskId != null && reservations.containsKey(taskId);
    }

    public synchronized void clearReservations() {
        reservations.clear();
    }

    public boolean canAdmit(WorkerTask task, long allocatedMemoryMb, int allocatedCpuCores, SystemMetrics currentMetrics, WorkerConfig.Execution executionConfig) {
        if (task == null || currentMetrics == null || executionConfig == null) {
            return false;
        }

        long totalMemoryMb = currentMetrics.getTotalMemoryMb();
        int totalCpuCores = currentMetrics.getCpuCores();
        
        long reservedMemoryMb = executionConfig.getReservedMemoryMb();
        int reservedCpuCores = executionConfig.getReservedCpuCores();
        
        long requiredMemoryMb = Math.max(0, task.getRequiredMemoryMb());
        int requiredCpuCores = Math.max(0, task.getRequiredCpuCores());

        long usableMemoryMb = Math.max(0, totalMemoryMb - reservedMemoryMb);
        int usableCpuCores = Math.max(0, totalCpuCores - reservedCpuCores);

        // Check if the task requests more than the machine physically has (minus reserved)
        if (requiredMemoryMb > usableMemoryMb) {
            return false;
        }
        
        if (requiredCpuCores > usableCpuCores) {
            return false;
        }

        // Check if admitting the task would exceed available memory
        long availableMemoryMb = usableMemoryMb - allocatedMemoryMb;
        if (requiredMemoryMb > availableMemoryMb) {
            return false;
        }

        // Check if admitting the task would exceed available CPU
        int availableCpuCores = usableCpuCores - allocatedCpuCores;
        if (requiredCpuCores > availableCpuCores) {
            return false;
        }

        return true;
    }
}
