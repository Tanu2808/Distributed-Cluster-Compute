package com.cluster.coordinator.service;

import com.cluster.coordinator.dto.ClusterResourcesResponse;
import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerResource;
import com.cluster.coordinator.model.WorkerState;
import com.cluster.coordinator.repository.WorkerRepository;
import com.cluster.coordinator.repository.WorkerResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClusterService {

    private final WorkerRepository workerRepository;
    private final WorkerResourceRepository workerResourceRepository;

    public ClusterService(WorkerRepository workerRepository, WorkerResourceRepository workerResourceRepository) {
        this.workerRepository = workerRepository;
        this.workerResourceRepository = workerResourceRepository;
    }

    @Transactional(readOnly = true)
    public ClusterResourcesResponse getAggregateResources() {
        List<Worker> activeWorkers = workerRepository.findAll().stream()
                .filter(w -> w.getState() == WorkerState.ONLINE || w.getState() == WorkerState.BUSY)
                .toList();

        long totalCpu = 0;
        long totalMemory = 0;
        long totalGpu = 0;
        long totalStorage = 0;
        long totalNetwork = 0;

        for (Worker worker : activeWorkers) {
            WorkerResource resource = workerResourceRepository.findByWorkerId(worker.getId()).orElse(null);
            if (resource != null) {
                totalCpu += resource.getCpuCores();
                totalMemory += resource.getMemoryRamMb();
                totalGpu += resource.getGpuCount();
                totalStorage += resource.getStorageMb();
                totalNetwork += resource.getNetworkBps();
            }
        }

        return new ClusterResourcesResponse(totalCpu, totalMemory, totalGpu, totalStorage, totalNetwork);
    }
}
