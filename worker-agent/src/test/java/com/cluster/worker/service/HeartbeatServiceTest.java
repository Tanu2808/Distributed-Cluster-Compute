package com.cluster.worker.service;

import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatServiceTest {

    @Mock
    private WebSocketConnectionManager connectionManager;
    @Mock
    private WorkerIdentityGenerator identityGenerator;
    @Mock
    private SystemMetricsProvider metricsProvider;
    @Mock
    private WorkerLifecycleService lifecycleService;

    private HeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
        heartbeatService = new HeartbeatService(connectionManager, identityGenerator, metricsProvider, lifecycleService);
    }

    @Test
    void testSendHeartbeatWhenOnline() {
        when(lifecycleService.getState()).thenReturn(WorkerState.ONLINE);
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("worker-123");
        
        SystemMetrics metrics = new SystemMetrics();
        metrics.setCpuUsagePercent(50.0);
        metrics.setTotalMemoryMb(1024);
        metrics.setUsedMemoryMb(512);
        when(metricsProvider.collectMetrics()).thenReturn(metrics);

        heartbeatService.sendHeartbeat();

        verify(connectionManager, times(1)).sendMessage(eq("/app/worker.heartbeat"), any());
        verify(connectionManager, times(1)).sendMessage(eq("/app/worker.resource"), any());
    }

    @Test
    void testDoesNotSendHeartbeatWhenNotOnline() {
        when(lifecycleService.getState()).thenReturn(WorkerState.REGISTERING);
        
        heartbeatService.sendHeartbeat();
        
        verify(connectionManager, never()).sendMessage(anyString(), any());
    }
}
