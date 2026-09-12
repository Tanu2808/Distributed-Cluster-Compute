package com.cluster.worker.service;

import com.cluster.worker.communication.CoordinatorClient;
import com.cluster.worker.communication.RegistrationPayload;
import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerLifecycleServiceTest {

    @Mock
    private CoordinatorClient coordinatorClient;
    @Mock
    private WorkerIdentityGenerator identityGenerator;
    @Mock
    private SystemMetricsProvider metricsProvider;

    private WorkerConfig config;
    private WorkerLifecycleService service;

    @BeforeEach
    void setUp() {
        config = new WorkerConfig();
        config.setName("test-worker");
        service = new WorkerLifecycleService(coordinatorClient, identityGenerator, metricsProvider, config);
    }

    @Test
    void testSuccessfulRegistration() {
        when(metricsProvider.collectMetrics()).thenReturn(new SystemMetrics());
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("worker-123");
        when(coordinatorClient.register(any(RegistrationPayload.class))).thenReturn(true);

        service.start();

        assertEquals(WorkerState.ONLINE, service.getState());
        
        ArgumentCaptor<RegistrationPayload> captor = ArgumentCaptor.forClass(RegistrationPayload.class);
        verify(coordinatorClient).register(captor.capture());
        assertEquals("worker-123", captor.getValue().getId());
        assertEquals("test-worker", captor.getValue().getName());
    }

    @Test
    void testFailedRegistrationRetries() {
        when(metricsProvider.collectMetrics()).thenReturn(new SystemMetrics());
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("worker-123");
        when(coordinatorClient.register(any(RegistrationPayload.class))).thenReturn(false);

        service.start();

        assertEquals(WorkerState.REGISTERING, service.getState());
        verify(coordinatorClient, times(1)).register(any(RegistrationPayload.class));
    }

    @Test
    void testHandleDisconnection() {
        service.setState(WorkerState.ONLINE);
        when(metricsProvider.collectMetrics()).thenReturn(new SystemMetrics());
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("worker-123");
        when(coordinatorClient.register(any(RegistrationPayload.class))).thenReturn(false);

        service.handleDisconnection();

        assertEquals(WorkerState.REGISTERING, service.getState()); // It transitions immediately to REGISTERING during retry
    }

    @Test
    void testGracefulShutdown() {
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("worker-123");
        when(coordinatorClient.deregister("worker-123")).thenReturn(true);
        
        service.shutdown();
        
        assertEquals(WorkerState.STOPPING, service.getState());
        verify(coordinatorClient).deregister("worker-123");
    }
}
