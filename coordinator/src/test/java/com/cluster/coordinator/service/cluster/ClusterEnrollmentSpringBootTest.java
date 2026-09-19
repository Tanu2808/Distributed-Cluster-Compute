package com.cluster.coordinator.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"cluster.coordinator.advertised-url=http://192.168.1.200:8080"})
public class ClusterEnrollmentSpringBootTest {

    @Autowired private ClusterEnrollmentService enrollmentService;

    @Test
    public void testEnrollWorker_UsesConfiguredPropertyInSpringContext() {
        String joinCode = enrollmentService.getJoinCode();
        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker(joinCode, "spring-test-worker");

        assertTrue(result.isSuccess(), "Enrollment with valid join code must succeed");
        assertEquals("http://192.168.1.200:8080", result.getCoordinatorUrl());
    }
}
