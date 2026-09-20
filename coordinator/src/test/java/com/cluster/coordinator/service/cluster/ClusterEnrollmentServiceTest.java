package com.cluster.coordinator.service.cluster;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cluster.coordinator.model.ClusterSettings;
import com.cluster.coordinator.repository.ClusterSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

public class ClusterEnrollmentServiceTest {

    private ClusterSettingsRepository settingsRepository;
    private com.cluster.coordinator.repository.WorkerRepository workerRepository;
    private ClusterEnrollmentService enrollmentService;

    private CoordinatorEndpointProvider endpointProvider;

    // Stored in DB in the formatted (hyphenated) form that generateNewCode() produces
    private static final String STORED_CODE = "AAAA-BBBB-CCCC-DDDD";

    @BeforeEach
    public void setup() {
        settingsRepository = mock(ClusterSettingsRepository.class);
        workerRepository = mock(com.cluster.coordinator.repository.WorkerRepository.class);
        endpointProvider = mock(CoordinatorEndpointProvider.class);
        when(endpointProvider.getBaseUrl()).thenReturn("http://localhost:8080");

        enrollmentService = new ClusterEnrollmentService(settingsRepository, workerRepository, endpointProvider);
    }

    // -------------------------------------------------------------------------
    // Join-code generation tests (unchanged behaviour)
    // -------------------------------------------------------------------------

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
        when(settingsRepository.findById("CLUSTER_JOIN_CODE"))
                .thenReturn(
                        Optional.of(
                                new ClusterSettings("CLUSTER_JOIN_CODE", "AAAA-BBBB-CCCC-DDDD")));

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

    // -------------------------------------------------------------------------
    // Enrollment normalization tests
    // -------------------------------------------------------------------------

    private void stubStoredCode(String code) {
        when(settingsRepository.findById("CLUSTER_JOIN_CODE"))
                .thenReturn(Optional.of(new ClusterSettings("CLUSTER_JOIN_CODE", code)));
        when(settingsRepository.findById("CLUSTER_ID"))
                .thenReturn(Optional.of(new ClusterSettings("CLUSTER_ID", "test-cluster-id")));
    }

    /** (a) Correct formatted code (exactly as stored and displayed) must succeed. */
    @Test
    public void testEnrollWorker_FormattedCode_Succeeds() {
        stubStoredCode(STORED_CODE);
        when(workerRepository.findById("worker-123")).thenReturn(Optional.empty());

        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker("AAAA-BBBB-CCCC-DDDD", "worker-123");

        assertTrue(result.isSuccess(), "Formatted code identical to stored value must succeed");
        assertEquals("test-cluster-id", result.getClusterId());
        assertEquals("http://localhost:8080", result.getCoordinatorUrl());
        assertNotNull(result.getRuntimeCredential());

        verify(workerRepository).save(any());
    }

    /**
     * (b) Correct unformatted code (hyphens stripped — what the Worker actually sends) must
     * succeed.
     */
    @Test
    public void testEnrollWorker_UnformattedCode_Succeeds() {
        stubStoredCode(STORED_CODE);
        when(workerRepository.findById("worker-123")).thenReturn(Optional.empty());

        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker("AAAABBBBCCCCDDDD", "worker-123");

        assertTrue(result.isSuccess(), "Bare (no-hyphen) form of the correct code must succeed");
        assertEquals("test-cluster-id", result.getClusterId());
        assertNotNull(result.getRuntimeCredential());
    }

    /** (c) Correct lowercase code must succeed (case-insensitive comparison). */
    @Test
    public void testEnrollWorker_LowercaseCode_Succeeds() {
        stubStoredCode(STORED_CODE);
        when(workerRepository.findById("worker-123")).thenReturn(Optional.empty());

        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker("aaaa-bbbb-cccc-dddd", "worker-123");

        assertTrue(result.isSuccess(), "Lowercase version of the correct code must succeed");
        assertNotNull(result.getRuntimeCredential());
    }

    /** (d) Correct code with surrounding whitespace must succeed. */
    @Test
    public void testEnrollWorker_CodeWithSurroundingWhitespace_Succeeds() {
        stubStoredCode(STORED_CODE);
        when(workerRepository.findById("worker-123")).thenReturn(Optional.empty());

        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker("  AAAA-BBBB-CCCC-DDDD  ", "worker-123");

        assertTrue(result.isSuccess(), "Code with surrounding whitespace must succeed");
        assertNotNull(result.getRuntimeCredential());
    }

    /** (d-extra) Correct code with internal whitespace (spaces instead of hyphens) must succeed. */
    @Test
    public void testEnrollWorker_CodeWithInternalWhitespace_Succeeds() {
        stubStoredCode(STORED_CODE);
        when(workerRepository.findById("worker-123")).thenReturn(Optional.empty());

        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker("AAAA BBBB CCCC DDDD", "worker-123");

        assertTrue(result.isSuccess(), "Code with internal spaces instead of hyphens must succeed");
        assertNotNull(result.getRuntimeCredential());
    }

    /** (e) Incorrect code must fail (returns false, no cluster info). */
    @Test
    public void testEnrollWorker_WrongCode_Fails() {
        when(settingsRepository.findById("CLUSTER_JOIN_CODE"))
                .thenReturn(Optional.of(new ClusterSettings("CLUSTER_JOIN_CODE", STORED_CODE)));

        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker("ZZZZ-YYYY-XXXX-WWWW", "worker-123");

        assertFalse(result.isSuccess(), "Wrong code must not succeed");
        assertNull(result.getClusterId());
        assertNull(result.getCoordinatorUrl());
        assertNull(result.getRuntimeCredential());
        verify(workerRepository, never()).save(any());
    }

    /** (f) Null and blank inputs must fail safely without throwing. */
    @Test
    public void testEnrollWorker_NullOrBlankCode_FailsSafely() {
        assertFalse(
                enrollmentService.enrollWorker(null, "worker-123").isSuccess(),
                "null must return failure");
        assertFalse(
                enrollmentService.enrollWorker("", "worker-123").isSuccess(),
                "empty string must return failure");
        assertFalse(
                enrollmentService.enrollWorker("   ", "worker-123").isSuccess(),
                "blank string must return failure");

        assertFalse(
                enrollmentService.enrollWorker("AAAA-BBBB-CCCC-DDDD", null).isSuccess(),
                "null workerId must fail");
        assertFalse(
                enrollmentService.enrollWorker("AAAA-BBBB-CCCC-DDDD", "").isSuccess(),
                "empty workerId must fail");

        // No interaction with repository for null/blank inputs
        verify(settingsRepository, never()).findById(any());
        verify(workerRepository, never()).save(any());
    }

    /**
     * (g) Stored code in bare form must also be matched by formatted input (symmetric
     * normalization).
     */
    @Test
    public void testEnrollWorker_StoredInBareForm_FormattedInputSucceeds() {
        // If for any reason a bare code were stored, formatted input should still match.
        when(settingsRepository.findById("CLUSTER_JOIN_CODE"))
                .thenReturn(
                        Optional.of(new ClusterSettings("CLUSTER_JOIN_CODE", "AAAABBBBCCCCDDDD")));
        when(settingsRepository.findById("CLUSTER_ID"))
                .thenReturn(Optional.of(new ClusterSettings("CLUSTER_ID", "test-cluster-id")));
        when(workerRepository.findById("worker-123")).thenReturn(Optional.empty());

        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker("AAAA-BBBB-CCCC-DDDD", "worker-123");

        assertTrue(
                result.isSuccess(),
                "Normalization must be symmetric — formatted input matches bare stored value");
        assertNotNull(result.getRuntimeCredential());
    }

    // These URL resolution fallback tests were moved/obviated by CoordinatorEndpointProvider
}
