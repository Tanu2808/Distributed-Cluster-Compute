package com.cluster.coordinator.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cluster.coordinator.model.*;
import com.cluster.coordinator.repository.*;
import com.cluster.shared.protocol.MessageEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class SchedulerServiceTest {

    @Autowired private SchedulerService schedulerService;

    @Autowired private ResourceReservationService resourceReservationService;

    @Autowired private WorkerRepository workerRepository;

    @Autowired private WorkerResourceRepository workerResourceRepository;

    @Autowired private TaskRepository taskRepository;

    @Autowired private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired private JobRepository jobRepository;

    @MockitoBean private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    public void setup() {
        taskAssignmentRepository.deleteAll();
        taskRepository.deleteAll();
        jobRepository.deleteAll();
        workerResourceRepository.deleteAll();
        workerRepository.deleteAll();
    }

    private Worker createWorker(String id, WorkerState state, int cpu, long memory) {
        Worker w = new Worker(id, "Worker-" + id, state);
        workerRepository.save(w);
        WorkerResource wr = new WorkerResource(id, cpu, memory, 0, 100000, 1000);
        workerResourceRepository.save(wr);
        return w;
    }

    private Task createTask(String id, String jobId, int cpu, long memory, TaskState state) {
        if (!jobRepository.existsById(jobId)) {
            Job j = new Job();
            j.setId(jobId);
            j.setState(JobState.QUEUED);
            j.setTotalPartitions(3);
            jobRepository.save(j);
        }
        Task t = new Task();
        t.setId(id);
        t.setJobId(jobId);
        t.setRequiredCpu(cpu);
        t.setRequiredMemory(memory);
        t.setState(state);
        t.setTaskType("SUM_RANGE");
        t.setInput("{\"start\":1,\"end\":100}");
        t.setPartitionId(1);
        return taskRepository.save(t);
    }

    @Test
    public void testA_OneOnlineWorkerOneTask_Assigned() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        Task t = taskRepository.findById("t1").orElseThrow();
        assertEquals(TaskState.ASSIGNED, t.getState());

        TaskAssignment assignment = taskAssignmentRepository.findByTaskId("t1").orElseThrow();
        assertEquals("w1", assignment.getWorkerId());
        assertEquals(2, assignment.getAllocatedCpu());
        verify(messagingTemplate)
                .convertAndSend(eq("/topic/worker.w1.tasks"), any(MessageEnvelope.class));
    }

    @Test
    public void testB_OfflineWorker_TaskRemainsUnassigned() {
        createWorker("w1", WorkerState.OFFLINE, 4, 8000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        Task t = taskRepository.findById("t1").orElseThrow();
        assertEquals(TaskState.UNASSIGNED, t.getState());
    }

    @Test
    public void testC_InsufficientCpu_TaskRemainsUnassigned() {
        createWorker("w1", WorkerState.ONLINE, 2, 8000);
        createTask("t1", "j1", 4, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        Task t = taskRepository.findById("t1").orElseThrow();
        assertEquals(TaskState.UNASSIGNED, t.getState());
    }

    @Test
    public void testD_InsufficientMemory_TaskRemainsUnassigned() {
        createWorker("w1", WorkerState.ONLINE, 4, 1000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        Task t = taskRepository.findById("t1").orElseThrow();
        assertEquals(TaskState.UNASSIGNED, t.getState());
    }

    @Test
    public void testE_MultipleEligibleWorkers_DeterministicSelection() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000); // utilization 0
        createWorker("w2", WorkerState.ONLINE, 4, 8000); // utilization 0

        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        TaskAssignment assignment = taskAssignmentRepository.findByTaskId("t1").orElseThrow();
        // Since utilization is equal, w1 vs w2 string comparison tie-breaker means w1 is chosen
        assertEquals("w1", assignment.getWorkerId());
    }

    @Test
    public void testF_ResourceReservationUpdated() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        assertEquals(2, resourceReservationService.getAvailableCpu("w1"));
        assertEquals(6000, resourceReservationService.getAvailableMemory("w1"));
    }

    @Test
    public void testG_CannotOvercommitCpu() {
        createWorker("w1", WorkerState.ONLINE, 2, 8000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);
        createTask("t2", "j1", 2, 2000, TaskState.UNASSIGNED); // This task will fail to schedule

        schedulerService.scheduleTasks();

        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t1").orElseThrow().getState());
        assertEquals(TaskState.UNASSIGNED, taskRepository.findById("t2").orElseThrow().getState());
    }

    @Test
    public void testI_DuplicateSchedulingAttempt_NoDuplicateAssignment() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();
        schedulerService.scheduleTasks(); // Second call should do nothing

        assertEquals(1, taskAssignmentRepository.count());
    }

    @Test
    public void testJ_MultipleTasks_WorkerCapacityConsumedCorrectly() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createWorker("w2", WorkerState.ONLINE, 4, 8000);

        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);
        createTask("t2", "j1", 2, 2000, TaskState.UNASSIGNED);
        createTask("t3", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        // After t1 is assigned to w1 (util: 2/4), w2 has util 0/4.
        // t2 should go to w2.
        // After t2, both w1 and w2 have util 2/4.
        // t3 should go to w1 due to tie breaker.

        assertEquals(3, taskAssignmentRepository.count());
        assertEquals(2, taskAssignmentRepository.findByWorkerId("w1").size());
        assertEquals(1, taskAssignmentRepository.findByWorkerId("w2").size());
    }

    @Test
    public void testK_L_WorkerCapacityReleases() {
        createWorker("w1", WorkerState.ONLINE, 2, 8000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();
        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t1").orElseThrow().getState());

        createTask("t2", "j1", 2, 2000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        // t2 remains unassigned
        assertEquals(TaskState.UNASSIGNED, taskRepository.findById("t2").orElseThrow().getState());

        // Release t1
        resourceReservationService.release("w1", "t1");

        schedulerService.scheduleTasks();
        // Now t2 should be assigned
        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t2").orElseThrow().getState());
    }

    @Test
    public void testDistributedResourceTest() {
        // Core Project Concept Demonstration (Instruction 17)
        createWorker("A", WorkerState.ONLINE, 4, 16000);
        createWorker("B", WorkerState.ONLINE, 4, 16000);
        createWorker("C", WorkerState.ONLINE, 4, 16000);

        Job j = new Job();
        j.setId("job1");
        jobRepository.save(j);

        createTask("t1", "job1", 4, 4000, TaskState.UNASSIGNED);
        createTask("t2", "job1", 4, 4000, TaskState.UNASSIGNED);
        createTask("t3", "job1", 4, 4000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        // Total requested CPU is 12. No individual worker has 12 CPU.
        // But aggregate we can fulfill it by distributing the tasks.
        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t1").orElseThrow().getState());
        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t2").orElseThrow().getState());
        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t3").orElseThrow().getState());

        assertEquals(1, taskAssignmentRepository.findByWorkerId("A").size());
        assertEquals(1, taskAssignmentRepository.findByWorkerId("B").size());
        assertEquals(1, taskAssignmentRepository.findByWorkerId("C").size());

        assertEquals(0, resourceReservationService.getAvailableCpu("A"));
        assertEquals(0, resourceReservationService.getAvailableCpu("B"));
        assertEquals(0, resourceReservationService.getAvailableCpu("C"));

        verify(messagingTemplate, times(3)).convertAndSend(anyString(), any(MessageEnvelope.class));
    }

    @Test
    public void testSendFailure_ReleasesReservation() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        doThrow(new RuntimeException("Simulated STOMP failure"))
                .when(messagingTemplate)
                .convertAndSend(anyString(), any(MessageEnvelope.class));

        schedulerService.scheduleTasks();

        Task t = taskRepository.findById("t1").orElseThrow();
        assertEquals(TaskState.UNASSIGNED, t.getState()); // Must revert to UNASSIGNED
        assertEquals(4, resourceReservationService.getAvailableCpu("w1")); // Reservation released

        Job j = jobRepository.findById("j1").orElseThrow();
        assertEquals(JobState.QUEUED, j.getState()); // Should remain QUEUED
    }

    @Test
    public void testJobTransitionsToRunningOnSuccess() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createTask("t1", "j1", 2, 2000, TaskState.UNASSIGNED);

        schedulerService.scheduleTasks();

        Job j = jobRepository.findById("j1").orElseThrow();
        assertEquals(JobState.RUNNING, j.getState()); // Should transition to RUNNING
    }
}
