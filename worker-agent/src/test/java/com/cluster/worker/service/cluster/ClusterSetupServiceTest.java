package com.cluster.worker.service.cluster;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.model.cluster.ClusterConfiguration;
import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.model.cluster.JoinCode;
import com.cluster.worker.persistence.WorkerConfiguration;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.service.WorkerLifecycleService;
import com.cluster.worker.service.WorkerStateManager;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ClusterSetupServiceTest {

    @Mock private CoordinatorProvisioningService provisioningService;

    @Mock private CoordinatorConnectionResolver connectionResolver;

    @Mock private WorkerConfigurationStore configStore;

    @Mock private WorkerStateManager stateManager;

    @Mock private WorkerLifecycleService lifecycleService;

    private ClusterSetupService setupService;

    @BeforeEach
    void setUp() {
        setupService =
                new ClusterSetupService(
                        provisioningService,
                        connectionResolver,
                        configStore,
                        stateManager,
                        lifecycleService);
        ReflectionTestUtils.setField(
                setupService, "defaultCoordinatorUrl", "http://localhost:8080");
        lenient().when(configStore.getWorkerId()).thenReturn("worker-1");
    }

    @Test
    void testJoinClusterConnectionFailure_DoesNotTransition() {
        JoinCode code = new JoinCode("ABCD-1234-EFGH-5678");
        ClusterEnrollment enrollment = setupService.joinCluster(code);

        assertEquals(ClusterEnrollment.Status.FAILED, enrollment.getStatus());
        assertTrue(
                enrollment.getMessage().startsWith("Coordinator connection failed"),
                "Message was: " + enrollment.getMessage());

        // Ensure no state transitions or saves occurred on failure
        verify(configStore, never()).save();
        verify(stateManager, never()).transitionLifecycle(any());
        verify(lifecycleService, never()).initiateConnection();
    }

    @Test
    void testJoinClusterSuccess_TransitionsProperly() {
        JoinCode code = new JoinCode("ABCD-1234-EFGH-5678");
        when(configStore.getConfig()).thenReturn(new WorkerConfiguration("worker-1"));

        try (MockedConstruction<RestTemplate> mocked =
                mockConstruction(
                        RestTemplate.class,
                        (mock, context) -> {
                            when(mock.postForEntity(anyString(), any(), eq(Map.class)))
                                    .thenReturn(
                                            ResponseEntity.ok(
                                                    Map.of(
                                                            "clusterId",
                                                            "test-cluster-id",
                                                            "coordinatorUrl",
                                                            "http://localhost:8080")));
                        })) {

            ClusterEnrollment enrollment = setupService.joinCluster(code);

            assertEquals(ClusterEnrollment.Status.SUCCESS, enrollment.getStatus());

            // Order is critical: save config first, then state transitions, then initiate
            // connection
            InOrder inOrder = inOrder(configStore, stateManager, lifecycleService);
            inOrder.verify(configStore).save();
            inOrder.verify(stateManager)
                    .transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
            inOrder.verify(stateManager).transitionLifecycle(WorkerLifecycleState.CONFIGURED);
            inOrder.verify(lifecycleService).initiateConnection();
        }
    }

    @Test
    void testCreateClusterLocalSuccess_TransitionsProperly() {
        when(provisioningService.provisionLocalCoordinator("test-cluster")).thenReturn(true);
        when(connectionResolver.resolveLocalCoordinator("test-cluster"))
                .thenReturn(
                        new com.cluster.worker.model.cluster.ClusterConnectionInfo(
                                "http://localhost:8080", null));
        when(configStore.getConfig()).thenReturn(new WorkerConfiguration("worker-1"));

        ClusterConfiguration config = setupService.createCluster("test-cluster", true);

        assertEquals("test-cluster", config.getClusterName());

        verify(provisioningService, times(1)).provisionLocalCoordinator("test-cluster");

        InOrder inOrder = inOrder(configStore, stateManager, lifecycleService);
        inOrder.verify(configStore).save();
        inOrder.verify(stateManager)
                .transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
        inOrder.verify(stateManager).transitionLifecycle(WorkerLifecycleState.CONFIGURED);
        inOrder.verify(lifecycleService).initiateConnection();
    }

    @Test
    void testCreateClusterLocalFails_DoesNotTransition() {
        when(provisioningService.provisionLocalCoordinator("test-cluster")).thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> {
                    setupService.createCluster("test-cluster", true);
                });

        // Ensure no state transitions or saves occurred on failure
        verify(configStore, never()).save();
        verify(stateManager, never()).transitionLifecycle(any());
        verify(lifecycleService, never()).initiateConnection();
    }

    @Test
    void testCreateClusterRemoteUnsupported() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    setupService.createCluster("test-cluster", false);
                });

        verify(configStore, never()).save();
        verify(stateManager, never()).transitionLifecycle(any());
        verify(lifecycleService, never()).initiateConnection();
    }
}
