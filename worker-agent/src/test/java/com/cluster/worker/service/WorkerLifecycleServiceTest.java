package com.cluster.worker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkerLifecycleServiceTest {

    @Mock private WebSocketConnectionManager connectionManager;

    @Mock private WorkerIdentityGenerator identityGenerator;

    @Mock private SystemMetricsProvider metricsProvider;

    @Mock private WorkerConfigurationStore configStore;

    @Mock private com.cluster.worker.communication.TaskMessageHandler taskMessageHandler;

    private WorkerStateManager stateManager;
    private WorkerLifecycleService service;

    @BeforeEach
    void setUp() {
        stateManager = new WorkerStateManager();
        service =
                new WorkerLifecycleService(
                        connectionManager,
                        identityGenerator,
                        metricsProvider,
                        stateManager,
                        configStore,
                        taskMessageHandler);
    }

    @Test
    void testStartGoesToSetupWhenNotConfigured() {
        when(configStore.isConfigured()).thenReturn(false);
        service.start();

        assertEquals(WorkerLifecycleState.SETUP_REQUIRED, stateManager.getLifecycleState());
        assertEquals(ConnectionState.DISCONNECTED, stateManager.getConnectionState());
        verify(connectionManager, times(0)).connect();
    }

    @Test
    void testStartRegistersWorkerWhenConfigured() {
        when(configStore.isConfigured()).thenReturn(true);
        service.start();

        assertEquals(WorkerLifecycleState.CONFIGURED, stateManager.getLifecycleState());
        assertEquals(ConnectionState.CONNECTING, stateManager.getConnectionState());
        verify(connectionManager, times(1)).connect();
    }

    @Test
    void testHandleDisconnection() {
        when(configStore.isConfigured()).thenReturn(true);
        service.start();
        // Assume connected
        stateManager.transitionConnection(ConnectionState.REGISTERING);
        stateManager.transitionConnection(ConnectionState.ONLINE);

        service.handleDisconnection();

        assertEquals(ConnectionState.RECONNECTING, stateManager.getConnectionState());
    }

    @Test
    void testShutdownTransitionsToStopping() {
        service.shutdown();
        assertEquals(WorkerLifecycleState.STOPPING, stateManager.getLifecycleState());
    }
}
