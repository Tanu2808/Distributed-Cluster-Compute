package com.cluster.worker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.ExecutionState;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeartbeatServiceTest {

    @Mock private WebSocketConnectionManager connectionManager;
    @Mock private WorkerIdentityGenerator identityGenerator;
    @Mock private SystemMetricsProvider metricsProvider;
    @Mock private WorkerLifecycleService lifecycleService;
    @Mock private WorkerStateManager stateManager;
    @Mock private TaskService taskService;

    private HeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        heartbeatService =
                new HeartbeatService(
                        connectionManager,
                        identityGenerator,
                        metricsProvider,
                        lifecycleService,
                        taskService);
    }

    @Test
    void testSendHeartbeatWhenOnline() {
        when(lifecycleService.getStateManager()).thenReturn(stateManager);
        when(stateManager.getConnectionState()).thenReturn(ConnectionState.ONLINE);
        when(stateManager.getExecutionState()).thenReturn(ExecutionState.IDLE);
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
        when(lifecycleService.getStateManager()).thenReturn(stateManager);
        when(stateManager.getConnectionState()).thenReturn(ConnectionState.REGISTERING);

        heartbeatService.sendHeartbeat();

        verify(connectionManager, never()).sendMessage(anyString(), any());
    }
}
