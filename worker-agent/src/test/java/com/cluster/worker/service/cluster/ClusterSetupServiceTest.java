package com.cluster.worker.service.cluster;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.model.cluster.ClusterEnrollment;
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

    @Mock private WorkerConfigurationStore configStore;

    @Mock private WorkerStateManager stateManager;

    @Mock private WorkerLifecycleService lifecycleService;

    private ClusterSetupService setupService;

    @BeforeEach
    void setUp() {
        setupService =
                new ClusterSetupService(
                        configStore,
                        stateManager,
                        lifecycleService);
        lenient().when(configStore.getWorkerId()).thenReturn("worker-1");
    }

    @Test
    void testConnectToCoordinatorFailure_DoesNotTransition() {
        ClusterEnrollment enrollment = setupService.connectToCoordinator("http://localhost:8080");

        assertEquals(ClusterEnrollment.Status.FAILED, enrollment.getStatus());
        assertTrue(
                enrollment.getMessage().startsWith("Coordinator connection failed"),
                "Message was: " + enrollment.getMessage());

        verify(configStore, never()).save();
        verify(stateManager, never()).transitionLifecycle(any());
        verify(lifecycleService, never()).initiateConnection();
    }

    @Test
    void testConnectToCoordinatorSuccess_TransitionsProperly() {
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

            ClusterEnrollment enrollment = setupService.connectToCoordinator("http://localhost:8080?token=ABCD");

            assertEquals(ClusterEnrollment.Status.SUCCESS, enrollment.getStatus());

            InOrder inOrder = inOrder(configStore, stateManager, lifecycleService);
            inOrder.verify(configStore).save();
            inOrder.verify(stateManager)
                    .transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
            inOrder.verify(stateManager).transitionLifecycle(WorkerLifecycleState.CONFIGURED);
            inOrder.verify(lifecycleService).initiateConnection();
        }
    }
}
