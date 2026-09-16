package com.cluster.worker.task;

import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.SystemMetrics;
import org.springframework.stereotype.Service;

@Service
public class ResourceAdmissionService {

    public boolean canAdmit(WorkerTask task, long allocatedMemoryMb, int allocatedCpuCores, SystemMetrics currentMetrics, WorkerConfig.Execution executionConfig) {
        
        long totalMemoryMb = currentMetrics.getTotalMemoryMb();
        int totalCpuCores = currentMetrics.getCpuCores();
        
        long reservedMemoryMb = executionConfig.getReservedMemoryMb();
        int reservedCpuCores = executionConfig.getReservedCpuCores();
        
        long requiredMemoryMb = task.getRequiredMemoryMb();
        int requiredCpuCores = task.getRequiredCpuCores();

        // Check if the task requests more than the machine physically has (minus reserved)
        if (requiredMemoryMb > (totalMemoryMb - reservedMemoryMb)) {
            return false;
        }
        
        if (requiredCpuCores > (totalCpuCores - reservedCpuCores)) {
            return false;
        }

        // Check if admitting the task would exceed available memory
        long availableMemoryMb = totalMemoryMb - reservedMemoryMb - allocatedMemoryMb;
        if (requiredMemoryMb > availableMemoryMb) {
            return false;
        }

        // Check if admitting the task would exceed available CPU
        int availableCpuCores = totalCpuCores - reservedCpuCores - allocatedCpuCores;
        if (requiredCpuCores > availableCpuCores) {
            return false;
        }

        return true;
    }
}
