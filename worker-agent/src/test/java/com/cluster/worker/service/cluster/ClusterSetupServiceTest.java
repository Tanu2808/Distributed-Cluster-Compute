package com.cluster.worker.service.cluster;

import com.cluster.worker.model.cluster.ClusterConfiguration;
import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.model.cluster.JoinCode;
import com.cluster.worker.persistence.WorkerConfiguration;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.service.WorkerStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClusterSetupServiceTest {

    @Mock
    private CoordinatorProvisioningService provisioningService;

    @Mock
    private CoordinatorConnectionResolver connectionResolver;

    @Mock
    private WorkerConfigurationStore configStore;

    @Mock
    private WorkerStateManager stateManager;

    private ClusterSetupService setupService;

    @BeforeEach
    void setUp() {
        setupService = new ClusterSetupService(provisioningService, connectionResolver, configStore, stateManager);
    }

    @Test
    void testJoinClusterReturnsUnavailable() {
        JoinCode code = new JoinCode("ABCD-1234-EFGH-5678");
        ClusterEnrollment enrollment = setupService.joinCluster(code);
        
        assertEquals(ClusterEnrollment.Status.FAILED, enrollment.getStatus());
        assertEquals("Coordinator enrollment unavailable", enrollment.getMessage());
    }

    @Test
    void testCreateClusterLocalSuccess() {
        when(provisioningService.provisionLocalCoordinator("test-cluster")).thenReturn(true);
        when(connectionResolver.resolveLocalCoordinator("test-cluster")).thenReturn(new com.cluster.worker.model.cluster.ClusterConnectionInfo("http://localhost:8080", null));
        when(configStore.getConfig()).thenReturn(new WorkerConfiguration("worker-1"));

        ClusterConfiguration config = setupService.createCluster("test-cluster", true);
        
        assertEquals("test-cluster", config.getClusterName());
        verify(provisioningService, times(1)).provisionLocalCoordinator("test-cluster");
        verify(configStore, times(1)).save();
        verify(stateManager, times(1)).transitionLifecycle(WorkerLifecycleState.CONFIGURED);
    }

    @Test
    void testCreateClusterLocalFails() {
        when(provisioningService.provisionLocalCoordinator("test-cluster")).thenReturn(false);
        
        assertThrows(RuntimeException.class, () -> {
            setupService.createCluster("test-cluster", true);
        });
    }

    @Test
    void testCreateClusterRemoteUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> {
            setupService.createCluster("test-cluster", false);
        });
    }
}
