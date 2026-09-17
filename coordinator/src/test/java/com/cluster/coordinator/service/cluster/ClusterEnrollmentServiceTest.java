package com.cluster.coordinator.service.cluster;

import com.cluster.coordinator.model.ClusterSettings;
import com.cluster.coordinator.repository.ClusterSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClusterEnrollmentServiceTest {

    private ClusterSettingsRepository settingsRepository;
    private ClusterEnrollmentService enrollmentService;

    @BeforeEach
    public void setup() {
        settingsRepository = mock(ClusterSettingsRepository.class);
        enrollmentService = new ClusterEnrollmentService(settingsRepository);
        ReflectionTestUtils.setField(enrollmentService, "serverPort", "8080");
    }

    @Test
    public void testGetJoinCode_GeneratesWhenNoneExists() {
        when(settingsRepository.findById("CLUSTER_JOIN_CODE")).thenReturn(Optional.empty());

        String code = enrollmentService.getJoinCode();
        assertNotNull(code);
        assertEquals(19, code.length()); // XXXX-XXXX-XXXX-XXXX
        assertTrue(code.matches("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}"));

        verify(settingsRepository).save(any(ClusterSettings.class));
    }

    @Test
    public void testGetJoinCode_ReturnsExisting() {
        when(settingsRepository.findById("CLUSTER_JOIN_CODE")).thenReturn(Optional.of(new ClusterSettings("CLUSTER_JOIN_CODE", "AAAA-BBBB-CCCC-DDDD")));

        String code = enrollmentService.getJoinCode();
        assertEquals("AAAA-BBBB-CCCC-DDDD", code);

        verify(settingsRepository, never()).save(any());
    }

    @Test
    public void testRotateJoinCode() {
        String newCode = enrollmentService.rotateJoinCode();
        assertNotNull(newCode);

        ArgumentCaptor<ClusterSettings> captor = ArgumentCaptor.forClass(ClusterSettings.class);
        verify(settingsRepository).save(captor.capture());

        assertEquals(newCode, captor.getValue().getValue());
    }

    @Test
    public void testEnrollWorker_ValidCode() {
        when(settingsRepository.findById("CLUSTER_JOIN_CODE")).thenReturn(Optional.of(new ClusterSettings("CLUSTER_JOIN_CODE", "AAAA-BBBB-CCCC-DDDD")));
        when(settingsRepository.findById("CLUSTER_ID")).thenReturn(Optional.of(new ClusterSettings("CLUSTER_ID", "test-cluster-id")));

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("AAAA-BBBB-CCCC-DDDD");

        assertTrue(result.isSuccess());
        assertEquals("test-cluster-id", result.getClusterId());
        assertEquals("http://localhost:8080", result.getCoordinatorUrl());
    }

    @Test
    public void testEnrollWorker_InvalidCode() {
        when(settingsRepository.findById("CLUSTER_JOIN_CODE")).thenReturn(Optional.of(new ClusterSettings("CLUSTER_JOIN_CODE", "AAAA-BBBB-CCCC-DDDD")));

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("ZZZZ-YYYY-XXXX-WWWW");

        assertFalse(result.isSuccess());
        assertNull(result.getClusterId());
        assertNull(result.getCoordinatorUrl());
    }

    @Test
    public void testEnrollWorker_NullOrBlankCode() {
        assertFalse(enrollmentService.enrollWorker(null).isSuccess());
        assertFalse(enrollmentService.enrollWorker("   ").isSuccess());
    }
}
