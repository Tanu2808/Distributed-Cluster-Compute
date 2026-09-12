package com.cluster.worker.service;

import com.cluster.worker.communication.CoordinatorClient;
import com.cluster.worker.communication.HeartbeatPayload;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatServiceTest {

    @Mock
    private CoordinatorClient coordinatorClient;
    @Mock
    private WorkerIdentityGenerator identityGenerator;
    @Mock
    private SystemMetricsProvider metricsProvider;
    @Mock
    private WorkerLifecycleService lifecycleService;

    @InjectMocks
    private HeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testSendHeartbeatWhenOnline() {
        when(lifecycleService.getState()).thenReturn(WorkerState.ONLINE);
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("worker-123");
        
        SystemMetrics metrics = new SystemMetrics();
        metrics.setCpuUsagePercent(50.0);
        metrics.setTotalMemoryMb(1024);
        metrics.setUsedMemoryMb(512);
        metrics.setAdditionalInfo(new HashMap<>());
        when(metricsProvider.collectMetrics()).thenReturn(metrics);

        when(coordinatorClient.sendHeartbeat(eq("worker-123"), any(HeartbeatPayload.class))).thenReturn(true);

        heartbeatService.sendHeartbeat();

        verify(coordinatorClient).sendHeartbeat(eq("worker-123"), any(HeartbeatPayload.class));
        verify(lifecycleService, never()).handleDisconnection();
    }

    @Test
    void testSkipHeartbeatWhenNotOnline() {
        when(lifecycleService.getState()).thenReturn(WorkerState.REGISTERING);
        
        heartbeatService.sendHeartbeat();
        
        verify(coordinatorClient, never()).sendHeartbeat(anyString(), any());
    }

    @Test
    void testHandleDisconnectionOnHeartbeatFailure() {
        when(lifecycleService.getState()).thenReturn(WorkerState.ONLINE);
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("worker-123");
        when(metricsProvider.collectMetrics()).thenReturn(new SystemMetrics());
        when(coordinatorClient.sendHeartbeat(eq("worker-123"), any())).thenReturn(false);

        heartbeatService.sendHeartbeat();

        verify(lifecycleService).handleDisconnection();
    }
}
