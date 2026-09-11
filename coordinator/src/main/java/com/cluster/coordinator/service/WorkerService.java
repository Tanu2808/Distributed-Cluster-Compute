package com.cluster.coordinator.service;

import com.cluster.coordinator.dto.WorkerRegistrationRequest;
import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerResource;
import com.cluster.coordinator.model.WorkerState;
import com.cluster.coordinator.repository.WorkerHeartbeatRepository;
import com.cluster.coordinator.repository.WorkerRepository;
import com.cluster.coordinator.repository.WorkerResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final WorkerResourceRepository workerResourceRepository;
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    private final EventService eventService;

    public WorkerService(WorkerRepository workerRepository,
                         WorkerResourceRepository workerResourceRepository,
                         WorkerHeartbeatRepository workerHeartbeatRepository,
                         EventService eventService) {
        this.workerRepository = workerRepository;
        this.workerResourceRepository = workerResourceRepository;
        this.workerHeartbeatRepository = workerHeartbeatRepository;
        this.eventService = eventService;
    }

    @Transactional
    public Worker registerWorker(WorkerRegistrationRequest request) {
        Worker worker = workerRepository.findById(request.getId())
                .orElse(new Worker(request.getId(), request.getName(), WorkerState.REGISTERING));
        
        worker.setName(request.getName());
        worker.setState(WorkerState.ONLINE);
        worker = workerRepository.save(worker);

        WorkerResource resource = workerResourceRepository.findByWorkerId(worker.getId())
                .orElse(new WorkerResource());
        resource.setWorkerId(worker.getId());
        resource.setCpuCores(request.getCpuCores());
        resource.setMemoryRamMb(request.getMemoryRamMb());
        resource.setGpuCount(request.getGpuCount());
        resource.setStorageMb(request.getStorageMb());
        resource.setNetworkBps(request.getNetworkBps());
        workerResourceRepository.save(resource);

        eventService.recordEvent("WORKER_REGISTERED", "Worker registered successfully", worker.getId());
        
        return worker;
    }

    public List<Worker> getAllWorkers() {
        return workerRepository.findAll();
    }

    public Optional<Worker> getWorker(String workerId) {
        return workerRepository.findById(workerId);
    }

    @Transactional
    public void deleteWorker(String workerId) {
        if (workerRepository.existsById(workerId)) {
            workerResourceRepository.deleteByWorkerId(workerId);
            workerHeartbeatRepository.deleteByWorkerId(workerId);
            workerRepository.deleteById(workerId);
            eventService.recordEvent("WORKER_DELETED", "Worker deleted from coordinator", workerId);
        }
    }
}
