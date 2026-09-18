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

    // Stored in DB in the formatted (hyphenated) form that generateNewCode() produces
    private static final String STORED_CODE = "AAAA-BBBB-CCCC-DDDD";

    @BeforeEach
    public void setup() {
        settingsRepository = mock(ClusterSettingsRepository.class);
        enrollmentService = new ClusterEnrollmentService(settingsRepository);
        ReflectionTestUtils.setField(enrollmentService, "serverPort", "8080");
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

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("AAAA-BBBB-CCCC-DDDD");

        assertTrue(result.isSuccess(), "Formatted code identical to stored value must succeed");
        assertEquals("test-cluster-id", result.getClusterId());
        assertEquals("http://localhost:8080", result.getCoordinatorUrl());
    }

    /** (b) Correct unformatted code (hyphens stripped — what the Worker actually sends) must succeed. */
    @Test
    public void testEnrollWorker_UnformattedCode_Succeeds() {
        stubStoredCode(STORED_CODE);

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("AAAABBBBCCCCDDDD");

        assertTrue(result.isSuccess(), "Bare (no-hyphen) form of the correct code must succeed");
        assertEquals("test-cluster-id", result.getClusterId());
    }

    /** (c) Correct lowercase code must succeed (case-insensitive comparison). */
    @Test
    public void testEnrollWorker_LowercaseCode_Succeeds() {
        stubStoredCode(STORED_CODE);

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("aaaa-bbbb-cccc-dddd");

        assertTrue(result.isSuccess(), "Lowercase version of the correct code must succeed");
    }

    /** (d) Correct code with surrounding whitespace must succeed. */
    @Test
    public void testEnrollWorker_CodeWithSurroundingWhitespace_Succeeds() {
        stubStoredCode(STORED_CODE);

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("  AAAA-BBBB-CCCC-DDDD  ");

        assertTrue(result.isSuccess(), "Code with surrounding whitespace must succeed");
    }

    /** (d-extra) Correct code with internal whitespace (spaces instead of hyphens) must succeed. */
    @Test
    public void testEnrollWorker_CodeWithInternalWhitespace_Succeeds() {
        stubStoredCode(STORED_CODE);

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("AAAA BBBB CCCC DDDD");

        assertTrue(result.isSuccess(), "Code with internal spaces instead of hyphens must succeed");
    }

    /** (e) Incorrect code must fail (returns false, no cluster info). */
    @Test
    public void testEnrollWorker_WrongCode_Fails() {
        when(settingsRepository.findById("CLUSTER_JOIN_CODE"))
                .thenReturn(Optional.of(new ClusterSettings("CLUSTER_JOIN_CODE", STORED_CODE)));

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("ZZZZ-YYYY-XXXX-WWWW");

        assertFalse(result.isSuccess(), "Wrong code must not succeed");
        assertNull(result.getClusterId());
        assertNull(result.getCoordinatorUrl());
    }

    /** (f) Null and blank inputs must fail safely without throwing. */
    @Test
    public void testEnrollWorker_NullOrBlankCode_FailsSafely() {
        assertFalse(enrollmentService.enrollWorker(null).isSuccess(), "null must return failure");
        assertFalse(enrollmentService.enrollWorker("").isSuccess(), "empty string must return failure");
        assertFalse(enrollmentService.enrollWorker("   ").isSuccess(), "blank string must return failure");

        // No interaction with repository for null/blank inputs
        verify(settingsRepository, never()).findById(any());
    }

    /** (g) Stored code in bare form must also be matched by formatted input (symmetric normalization). */
    @Test
    public void testEnrollWorker_StoredInBareForm_FormattedInputSucceeds() {
        // If for any reason a bare code were stored, formatted input should still match.
        when(settingsRepository.findById("CLUSTER_JOIN_CODE"))
                .thenReturn(Optional.of(new ClusterSettings("CLUSTER_JOIN_CODE", "AAAABBBBCCCCDDDD")));
        when(settingsRepository.findById("CLUSTER_ID"))
                .thenReturn(Optional.of(new ClusterSettings("CLUSTER_ID", "test-cluster-id")));

        ClusterEnrollmentService.EnrollmentResult result = enrollmentService.enrollWorker("AAAA-BBBB-CCCC-DDDD");

        assertTrue(result.isSuccess(), "Normalization must be symmetric — formatted input matches bare stored value");
    }
}

