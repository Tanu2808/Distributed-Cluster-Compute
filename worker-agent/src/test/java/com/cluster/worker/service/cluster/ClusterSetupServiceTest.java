package com.cluster.worker.service.cluster;

import com.cluster.worker.model.cluster.ClusterConfiguration;
import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.model.cluster.JoinCode;
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

    private ClusterSetupService setupService;

    @BeforeEach
    void setUp() {
        setupService = new ClusterSetupService(provisioningService, connectionResolver);
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
        when(connectionResolver.resolveLocalCoordinator("test-cluster")).thenReturn(null);
        
        ClusterConfiguration config = setupService.createCluster("test-cluster", true);
        
        assertEquals("test-cluster", config.getClusterName());
        verify(provisioningService, times(1)).provisionLocalCoordinator("test-cluster");
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
