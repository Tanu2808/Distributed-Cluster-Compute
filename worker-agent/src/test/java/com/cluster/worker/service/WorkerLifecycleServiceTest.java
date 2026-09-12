package com.cluster.worker.service;

import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.WorkerState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class WorkerLifecycleServiceTest {

    @Mock
    private WebSocketConnectionManager connectionManager;
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
        service = new WorkerLifecycleService(connectionManager, identityGenerator, metricsProvider, config);
    }

    @Test
    void testStartRegistersWorker() {
        service.start();
        
        assertEquals(WorkerState.REGISTERING, service.getState());
        verify(connectionManager, times(1)).connect();
    }

    @Test
    void testHandleDisconnection() {
        service.setState(WorkerState.ONLINE);

        service.handleDisconnection();

        assertEquals(WorkerState.DISCONNECTED, service.getState()); 
        // It transitions immediately to DISCONNECTED during retry, then after 5 secs REGISTERING
    }

    @Test
    void testGracefulShutdown() {
        service.shutdown();
        
        assertEquals(WorkerState.STOPPING, service.getState());
    }
}
