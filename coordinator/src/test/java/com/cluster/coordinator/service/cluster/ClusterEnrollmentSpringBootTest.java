package com.cluster.coordinator.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"cluster.coordinator.advertised-host=192.168.1.200"})
public class ClusterEnrollmentSpringBootTest {

    @Autowired private ClusterEnrollmentService enrollmentService;
    @Autowired private CoordinatorEndpointProvider endpointProvider;

    @Test
    public void testEnrollWorker_UsesConfiguredPropertyInSpringContext() {
        String joinCode = enrollmentService.getJoinCode();
        ClusterEnrollmentService.EnrollmentResult result =
                enrollmentService.enrollWorker(joinCode, "spring-test-worker");

        assertTrue(result.isSuccess(), "Enrollment with valid join code must succeed");
        assertEquals("http://192.168.1.200:" + endpointProvider.getActualPort(), result.getCoordinatorUrl());
    }
}
