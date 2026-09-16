package com.cluster.coordinator.service;

import com.cluster.coordinator.dto.WorkerRegistrationRequest;
import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerState;
import com.cluster.coordinator.repository.WorkerHeartbeatRepository;
import com.cluster.coordinator.repository.WorkerRepository;
import com.cluster.coordinator.repository.WorkerResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkerResourceRepository workerResourceRepository;

    @Mock
    private WorkerHeartbeatRepository workerHeartbeatRepository;

    @Mock
    private EventService eventService;

    @Mock
    private com.cluster.coordinator.websocket.ClusterWebSocketHandler webSocketHandler;

    @InjectMocks
    private WorkerService workerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerWorker_NewWorker_SuccessfullyRegistered() {
        WorkerRegistrationRequest request = new WorkerRegistrationRequest();
        request.setId("worker-1");
        request.setName("Test Worker");
        request.setCpuCores(4);
        
        when(workerRepository.findById("worker-1")).thenReturn(Optional.empty());
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArguments()[0]);
        when(workerResourceRepository.findByWorkerId("worker-1")).thenReturn(Optional.empty());

        Worker worker = workerService.registerWorker(request);

        assertEquals("worker-1", worker.getId());
        assertEquals(WorkerState.ONLINE, worker.getState());
        
        verify(workerRepository, times(1)).save(any(Worker.class));
        verify(workerResourceRepository, times(1)).save(any());
        verify(eventService, times(1)).recordEvent(eq("WORKER_REGISTERED"), anyString(), eq("worker-1"));
    }
}
