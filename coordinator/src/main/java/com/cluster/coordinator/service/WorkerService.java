package com.cluster.coordinator.service;

import com.cluster.coordinator.dto.WorkerRegistrationRequest;
import com.cluster.coordinator.dto.WorkerResponseDto;
import com.cluster.coordinator.dto.WsMessageDto;
import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerResource;
import com.cluster.coordinator.model.WorkerState;
import com.cluster.coordinator.repository.WorkerHeartbeatRepository;
import com.cluster.coordinator.repository.WorkerRepository;
import com.cluster.coordinator.repository.WorkerResourceRepository;
import com.cluster.coordinator.websocket.ClusterWebSocketHandler;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing the lifecycle and state of cluster workers.
 * Handles worker registration, resource updates, and synchronization with the WebSocket layer.
 */
@Service
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final WorkerResourceRepository workerResourceRepository;
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    private final EventService eventService;
    private final ClusterWebSocketHandler webSocketHandler;

    public WorkerService(
            WorkerRepository workerRepository,
            WorkerResourceRepository workerResourceRepository,
            WorkerHeartbeatRepository workerHeartbeatRepository,
            EventService eventService,
            ClusterWebSocketHandler webSocketHandler) {
        this.workerRepository = workerRepository;
        this.workerResourceRepository = workerResourceRepository;
        this.workerHeartbeatRepository = workerHeartbeatRepository;
        this.eventService = eventService;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Registers a new worker in the database and initializes its state and resources.
     * Broadcasts the connection event to all active WebSocket clients.
     *
     * @param request The initial registration metadata from the worker node.
     * @return The persisted Worker entity.
     */
    @Transactional
    public Worker registerWorker(WorkerRegistrationRequest request) {
        Worker worker =
                workerRepository
                        .findById(request.getId())
                        .orElse(
                                new Worker(
                                        request.getId(),
                                        request.getName(),
                                        WorkerState.REGISTERING));

        worker.setName(request.getName());
        worker.setHostname(request.getHostname());
        worker.setIpAddress(request.getIpAddress());
        worker.setOperatingSystem(request.getOperatingSystem());
        worker.setArchitecture(request.getArchitecture());
        worker.setAgentVersion(request.getAgentVersion());
        worker.setState(WorkerState.ONLINE);
        worker = workerRepository.save(worker);

        WorkerResource resource =
                workerResourceRepository
                        .findByWorkerId(worker.getId())
                        .orElse(new WorkerResource());
        resource.setWorkerId(worker.getId());
        resource.setCpuCores(request.getCpuCores());
        resource.setMemoryRamMb(request.getMemoryRamMb());
        resource.setGpuCount(request.getGpuCount());
        resource.setStorageMb(request.getStorageMb());
        resource.setNetworkBps(request.getNetworkBps());
        workerResourceRepository.save(resource);

        eventService.recordEvent(
                "WORKER_REGISTERED", "Worker registered successfully", worker.getId());

        webSocketHandler.broadcast(
                new WsMessageDto<>("WORKER_CONNECTED", getWorkerDto(worker.getId()).orElse(null)));

        return worker;
    }

    /**
     * Updates the persistent resource metrics (e.g., total memory, disk space) for a worker.
     * Triggered when a worker reports a hardware topology change or initial resource scan.
     *
     * @param workerId The UUID of the worker.
     * @param resourceMsg The resource update payload containing metric values.
     */
    @Transactional
    public void updateWorkerResources(
            String workerId, com.cluster.shared.protocol.ResourceUpdateMessage resourceMsg) {
        workerResourceRepository
                .findByWorkerId(workerId)
                .ifPresent(
                        resource -> {
                            if (resourceMsg.getMemoryTotalBytes() != null) {
                                resource.setMemoryRamMb(
                                        resourceMsg.getMemoryTotalBytes() / (1024 * 1024));
                            }
                            if (resourceMsg.getDiskTotalBytes() != null) {
                                resource.setStorageMb(
                                        resourceMsg.getDiskTotalBytes() / (1024 * 1024));
                            }
                            workerResourceRepository.save(resource);
                        });
    }

    public List<Worker> getAllWorkers() {
        return workerRepository.findAll();
    }

    public List<WorkerResponseDto> getAllWorkerDtos() {
        return workerRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public Optional<Worker> getWorker(String workerId) {
        return workerRepository.findById(workerId);
    }

    public Optional<WorkerResponseDto> getWorkerDto(String workerId) {
        return workerRepository.findById(workerId).map(this::mapToDto);
    }

    private WorkerResponseDto mapToDto(Worker worker) {
        WorkerResponseDto dto = new WorkerResponseDto();
        dto.setId(worker.getId());
        dto.setName(worker.getName());
        dto.setHostname(worker.getHostname());
        dto.setIpAddress(worker.getIpAddress());
        dto.setOperatingSystem(worker.getOperatingSystem());
        dto.setArchitecture(worker.getArchitecture());
        dto.setAgentVersion(worker.getAgentVersion());
        dto.setState(worker.getState());
        dto.setLastHeartbeat(worker.getLastHeartbeat());
        dto.setConnectedSince(worker.getCreatedAt());

        workerResourceRepository
                .findByWorkerId(worker.getId())
                .ifPresent(
                        res -> {
                            dto.setCpuCores(res.getCpuCores());
                            dto.setMemoryRamMb(res.getMemoryRamMb());
                            dto.setGpuCount(res.getGpuCount());
                            dto.setStorageMb(res.getStorageMb());
                            dto.setNetworkBps(res.getNetworkBps());
                        });

        workerHeartbeatRepository
                .findTopByWorkerIdOrderByTimestampDesc(worker.getId())
                .ifPresent(
                        hb -> {
                            dto.setCpuUsagePercent(hb.getCpuUsagePercent());
                            dto.setMemoryUsagePercent(hb.getMemoryUsagePercent());
                            dto.setActiveTasks(hb.getActiveTasks());
                        });

        return dto;
    }

    @Transactional
    public void deleteWorker(String workerId) {
        if (workerRepository.existsById(workerId)) {
            workerResourceRepository.deleteByWorkerId(workerId);
            workerHeartbeatRepository.deleteByWorkerId(workerId);
            workerRepository.deleteById(workerId);
            eventService.recordEvent("WORKER_DELETED", "Worker deleted from coordinator", workerId);
            webSocketHandler.broadcast(new WsMessageDto<>("WORKER_DISCONNECTED", workerId));
        }
    }
}
