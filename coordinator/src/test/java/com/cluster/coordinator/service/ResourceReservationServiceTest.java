package com.cluster.coordinator.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cluster.coordinator.model.AssignmentState;
import com.cluster.coordinator.model.TaskAssignment;
import com.cluster.coordinator.model.WorkerResource;
import com.cluster.coordinator.repository.TaskAssignmentRepository;
import com.cluster.coordinator.repository.WorkerResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class ResourceReservationServiceTest {

    @Autowired private ResourceReservationService resourceReservationService;

    @Autowired private WorkerResourceRepository workerResourceRepository;

    @Autowired private TaskAssignmentRepository taskAssignmentRepository;

    @BeforeEach
    public void setup() {
        taskAssignmentRepository.deleteAll();
        workerResourceRepository.deleteAll();
    }

    @Test
    public void testReserveAndRelease() {
        // Setup worker resources
        WorkerResource wr = new WorkerResource("worker-1", 4, 8000, 0, 100000, 1000);
        workerResourceRepository.save(wr);

        // Initial available resources
        assertEquals(4, resourceReservationService.getAvailableCpu("worker-1"));
        assertEquals(8000, resourceReservationService.getAvailableMemory("worker-1"));
        assertEquals(0, resourceReservationService.getReservedCpu("worker-1"));

        // Reserve
        resourceReservationService.reserve("worker-1", "task-1", 2, 2000);

        // Verify reserved resources
        assertEquals(2, resourceReservationService.getAvailableCpu("worker-1"));
        assertEquals(6000, resourceReservationService.getAvailableMemory("worker-1"));
        assertEquals(2, resourceReservationService.getReservedCpu("worker-1"));
        assertEquals(2000, resourceReservationService.getReservedMemory("worker-1"));

        // Release
        resourceReservationService.release("worker-1", "task-1");

        // Verify resources are returned (since assignment is CANCELLED)
        assertEquals(4, resourceReservationService.getAvailableCpu("worker-1"));
        assertEquals(8000, resourceReservationService.getAvailableMemory("worker-1"));

        // Verify TaskAssignment state is CANCELLED not deleted
        TaskAssignment assignment = taskAssignmentRepository.findByTaskId("task-1").orElseThrow();
        assertEquals(AssignmentState.CANCELLED, assignment.getState());
    }

    @Test
    public void testDuplicateReservationFails() {
        WorkerResource wr = new WorkerResource("worker-1", 4, 8000, 0, 100000, 1000);
        workerResourceRepository.save(wr);

        resourceReservationService.reserve("worker-1", "task-1", 2, 2000);

        assertThrows(
                IllegalStateException.class,
                () -> {
                    resourceReservationService.reserve("worker-1", "task-1", 1, 1000);
                });
    }
}
