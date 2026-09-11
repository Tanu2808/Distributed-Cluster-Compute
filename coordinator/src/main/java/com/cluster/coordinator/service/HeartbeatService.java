package com.cluster.coordinator.service;

import com.cluster.coordinator.dto.WorkerHeartbeatRequest;
import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerHeartbeat;
import com.cluster.coordinator.model.WorkerState;
import com.cluster.coordinator.repository.WorkerHeartbeatRepository;
import com.cluster.coordinator.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);
    private final WorkerRepository workerRepository;
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    private final EventService eventService;

    @Value("${cluster.heartbeat.timeout-seconds:30}")
    private int heartbeatTimeoutSeconds;

    public HeartbeatService(WorkerRepository workerRepository,
                            WorkerHeartbeatRepository workerHeartbeatRepository,
                            EventService eventService) {
        this.workerRepository = workerRepository;
        this.workerHeartbeatRepository = workerHeartbeatRepository;
        this.eventService = eventService;
    }

    @Transactional
    public void processHeartbeat(String workerId, WorkerHeartbeatRequest request) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found: " + workerId));

        worker.setLastHeartbeat(LocalDateTime.now());
        if (worker.getState() == WorkerState.HEARTBEAT_TIMEOUT || worker.getState() == WorkerState.OFFLINE) {
            worker.setState(WorkerState.ONLINE);
            eventService.recordEvent("WORKER_RECONNECTED", "Worker reconnected and is online", workerId);
        } else if (worker.getState() == WorkerState.REGISTERING) {
            worker.setState(WorkerState.ONLINE);
        }
        workerRepository.save(worker);

        WorkerHeartbeat heartbeat = new WorkerHeartbeat(workerId, request.getCpuUsagePercent(),
                request.getMemoryUsagePercent(), request.getActiveTasks());
        workerHeartbeatRepository.save(heartbeat);
    }

    @Scheduled(fixedRateString = "${cluster.heartbeat.check-interval-ms:10000}")
    @Transactional
    public void checkHeartbeatTimeouts() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
        List<Worker> workers = workerRepository.findByLastHeartbeatBefore(cutoff);

        for (Worker worker : workers) {
            if (worker.getState() == WorkerState.ONLINE || worker.getState() == WorkerState.BUSY) {
                worker.setState(WorkerState.HEARTBEAT_TIMEOUT);
                workerRepository.save(worker);
                eventService.recordEvent("WORKER_TIMEOUT", "Worker heartbeat timed out", worker.getId());
                log.warn("Worker heartbeat timed out: {}", worker.getId());
            }
        }
    }
}
