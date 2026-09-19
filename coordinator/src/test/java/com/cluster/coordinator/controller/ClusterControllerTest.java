package com.cluster.coordinator.controller;

import com.cluster.coordinator.service.cluster.ClusterEnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ClusterControllerTest {

    private ClusterEnrollmentService enrollmentService;
    private com.cluster.coordinator.service.WorkerService workerService;
    private ClusterController clusterController;

    @BeforeEach
    public void setup() {
        enrollmentService = mock(ClusterEnrollmentService.class);
        workerService = mock(com.cluster.coordinator.service.WorkerService.class);
        clusterController = new ClusterController(enrollmentService, workerService);
    }

    @Test
    public void testGetJoinCode() {
        when(enrollmentService.getJoinCode()).thenReturn("AAAA-BBBB-CCCC-DDDD");

        ResponseEntity<Map<String, String>> response = clusterController.getJoinCode();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("AAAA-BBBB-CCCC-DDDD", response.getBody().get("joinCode"));
    }

    @Test
    public void testRotateJoinCode() {
        when(enrollmentService.rotateJoinCode()).thenReturn("1111-2222-3333-4444");

        ResponseEntity<Map<String, String>> response = clusterController.rotateJoinCode();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("1111-2222-3333-4444", response.getBody().get("joinCode"));
    }

    @Test
    public void testEnrollWorker_Success() {
        ClusterEnrollmentService.EnrollmentResult result = new ClusterEnrollmentService.EnrollmentResult(true, "test-cluster", "http://localhost:8080", "mocked-runtime-credential");
        when(enrollmentService.enrollWorker("VALID-CODE", "worker-123")).thenReturn(result);

        ResponseEntity<Map<String, String>> response = clusterController.enrollWorker(Map.of("joinCode", "VALID-CODE", "workerId", "worker-123"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test-cluster", response.getBody().get("clusterId"));
        assertEquals("http://localhost:8080", response.getBody().get("coordinatorUrl"));
        assertEquals("mocked-runtime-credential", response.getBody().get("runtimeCredential"));
    }

    @Test
    public void testEnrollWorker_Failure() {
        ClusterEnrollmentService.EnrollmentResult result = new ClusterEnrollmentService.EnrollmentResult(false, null, null, null);
        when(enrollmentService.enrollWorker("INVALID-CODE", "worker-123")).thenReturn(result);

        ResponseEntity<Map<String, String>> response = clusterController.enrollWorker(Map.of("joinCode", "INVALID-CODE", "workerId", "worker-123"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getBody());
    }
}
