package com.cluster.coordinator.service;

import com.cluster.coordinator.model.*;
import com.cluster.coordinator.repository.*;
import com.cluster.shared.protocol.TaskResultMessage;
import com.cluster.shared.protocol.TaskStatusMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TaskExecutionServiceTest {

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private TaskResultRepository taskResultRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerResourceRepository workerResourceRepository;

    @Autowired
    private ResourceReservationService resourceReservationService;

    @Autowired
    private SchedulerService schedulerService;

    @BeforeEach
    public void setup() {
        taskResultRepository.deleteAll();
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

    private Job createJob(String id, int partitions) {
        Job j = new Job();
        j.setId(id);
        j.setTaskType("SUM_RANGE");
        j.setState(JobState.QUEUED);
        j.setTotalPartitions(partitions);
        j.setCompletedPartitions(0);
        return jobRepository.save(j);
    }

    private Task createTask(String id, String jobId, int partitionId, int cpu, long memory, TaskState state) {
        Task t = new Task();
        t.setId(id);
        t.setJobId(jobId);
        t.setPartitionId(partitionId);
        t.setRequiredCpu(cpu);
        t.setRequiredMemory(memory);
        t.setState(state);
        t.setTaskType("SUM_RANGE");
        t.setInput("{}");
        return taskRepository.save(t);
    }

    @Test
    public void testA_AssignedToRunning() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        Job j1 = createJob("j1", 1);
        Task t1 = createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        
        schedulerService.scheduleTasks(); // assigns to w1
        
        TaskStatusMessage msg = new TaskStatusMessage("t1", "RUNNING", "Started");
        msg.setJobId("j1");
        msg.setPartitionId(1);
        
        taskExecutionService.processTaskStatus("w1", msg);
        
        Task t = taskRepository.findById("t1").orElseThrow();
        assertEquals(TaskState.RUNNING, t.getState());
        
        TaskAssignment assignment = taskAssignmentRepository.findByTaskId("t1").orElseThrow();
        assertEquals(AssignmentState.ACTIVE, assignment.getState());
        
        // Resources should still be reserved
        assertEquals(2, resourceReservationService.getAvailableCpu("w1"));
    }

    @Test
    public void testB_RunningToCompleted_ReleasesResources() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createJob("j1", 1);
        createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        
        schedulerService.scheduleTasks();
        
        TaskStatusMessage msg = new TaskStatusMessage("t1", "COMPLETED", "Done");
        msg.setJobId("j1");
        msg.setPartitionId(1);
        
        taskExecutionService.processTaskStatus("w1", msg);
        
        Task t = taskRepository.findById("t1").orElseThrow();
        assertEquals(TaskState.COMPLETED, t.getState());
        
        TaskAssignment assignment = taskAssignmentRepository.findByTaskId("t1").orElseThrow();
        assertEquals(AssignmentState.COMPLETED, assignment.getState());
        
        // Resources should be released
        assertEquals(4, resourceReservationService.getAvailableCpu("w1"));
    }

    @Test
    public void testC_RunningToFailed() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createJob("j1", 1);
        createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        TaskStatusMessage msg = new TaskStatusMessage("t1", "FAILED", "Error");
        msg.setJobId("j1");
        msg.setPartitionId(1);
        
        taskExecutionService.processTaskStatus("w1", msg);
        
        assertEquals(TaskState.FAILED, taskRepository.findById("t1").orElseThrow().getState());
        assertEquals(AssignmentState.FAILED, taskAssignmentRepository.findByTaskId("t1").orElseThrow().getState());
        assertEquals(4, resourceReservationService.getAvailableCpu("w1"));
        assertEquals(JobState.FAILED, jobRepository.findById("j1").orElseThrow().getState());
    }

    @Test
    public void testE_InvalidBackwardsTransitionRejected() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createJob("j1", 1);
        createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        TaskStatusMessage msgComplete = new TaskStatusMessage("t1", "COMPLETED", "Done");
        msgComplete.setJobId("j1");
        msgComplete.setPartitionId(1);
        taskExecutionService.processTaskStatus("w1", msgComplete);
        
        TaskStatusMessage msgRunning = new TaskStatusMessage("t1", "RUNNING", "Started");
        msgRunning.setJobId("j1");
        msgRunning.setPartitionId(1);
        taskExecutionService.processTaskStatus("w1", msgRunning); // Should be ignored
        
        assertEquals(TaskState.COMPLETED, taskRepository.findById("t1").orElseThrow().getState());
    }

    @Test
    public void testF_J_K_SuccessfulResultPersistedAndJobCompleted() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createJob("j1", 1);
        createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        TaskResultMessage resultMsg = TaskResultMessage.builder()
                .taskId("t1")
                .jobId("j1")
                .partitionId(1)
                .status("COMPLETED")
                .result("5050")
                .executionDurationMs(100)
                .build();
                
        taskExecutionService.processTaskResult("w1", resultMsg);
        
        TaskResult tr = taskResultRepository.findByTaskId("t1").orElseThrow();
        assertEquals("t1", tr.getTaskId());
        assertEquals("\"5050\"", tr.getOutput());
        
        assertEquals(TaskState.COMPLETED, taskRepository.findById("t1").orElseThrow().getState());
        assertEquals(AssignmentState.COMPLETED, taskAssignmentRepository.findByTaskId("t1").orElseThrow().getState());
        assertEquals(4, resourceReservationService.getAvailableCpu("w1")); // Released
        
        Job j = jobRepository.findById("j1").orElseThrow();
        assertEquals(1, j.getCompletedPartitions());
        assertEquals(JobState.COMPLETED, j.getState());
    }

    @Test
    public void testM_N_O_WrongIdentityRejected() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createWorker("w2", WorkerState.ONLINE, 4, 8000);
        createJob("j1", 1);
        createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks(); // assigned to w1
        
        // Wrong Job ID
        TaskStatusMessage msg1 = new TaskStatusMessage("t1", "COMPLETED", "");
        msg1.setJobId("j2");
        msg1.setPartitionId(1);
        taskExecutionService.processTaskStatus("w1", msg1);
        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t1").orElseThrow().getState()); // rejected
        
        // Wrong Partition ID
        TaskStatusMessage msg2 = new TaskStatusMessage("t1", "COMPLETED", "");
        msg2.setJobId("j1");
        msg2.setPartitionId(2);
        taskExecutionService.processTaskStatus("w1", msg2);
        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t1").orElseThrow().getState()); // rejected
        
        // Wrong Worker ID
        TaskStatusMessage msg3 = new TaskStatusMessage("t1", "COMPLETED", "");
        msg3.setJobId("j1");
        msg3.setPartitionId(1);
        taskExecutionService.processTaskStatus("w2", msg3);
        assertEquals(TaskState.ASSIGNED, taskRepository.findById("t1").orElseThrow().getState()); // rejected
    }

    @Test
    public void testP_DuplicateTaskResult_HandledIdempotently() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createJob("j1", 2); // 2 partitions
        createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        TaskResultMessage resultMsg = TaskResultMessage.builder()
                .taskId("t1")
                .jobId("j1")
                .partitionId(1)
                .status("COMPLETED")
                .result("5050")
                .build();
                
        taskExecutionService.processTaskResult("w1", resultMsg);
        
        Job j = jobRepository.findById("j1").orElseThrow();
        assertEquals(1, j.getCompletedPartitions());
        
        // Duplicate
        taskExecutionService.processTaskResult("w1", resultMsg);
        
        Job jAfter = jobRepository.findById("j1").orElseThrow();
        assertEquals(1, jAfter.getCompletedPartitions()); // still 1
        assertEquals(1, taskResultRepository.count());
    }

    @Test
    public void testP2_DuplicateTaskResult_DoesNotDoubleCountFinalResult() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        createJob("j1", 1);
        createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        TaskResultMessage resultMsg = TaskResultMessage.builder()
                .taskId("t1")
                .jobId("j1")
                .partitionId(1)
                .status("COMPLETED")
                .result(10L)
                .build();
                
        taskExecutionService.processTaskResult("w1", resultMsg);
        
        Job j = jobRepository.findById("j1").orElseThrow();
        assertEquals("10", j.getFinalResult());
        
        // Simulate a duplicate message arriving after job is completed
        taskExecutionService.processTaskResult("w1", resultMsg);
        
        Job jAfter = jobRepository.findById("j1").orElseThrow();
        assertEquals("10", jAfter.getFinalResult()); // Should still be 10, not 20
    }

    @Test
    public void testP3_ExistingFinalResult_NotOverwritten() {
        createWorker("w1", WorkerState.ONLINE, 4, 8000);
        Job j = createJob("j1", 1);
        // Pre-set a final result to simulate existing completed job or concurrent update
        j.setFinalResult("999");
        jobRepository.save(j);
        
        createTask("t1", "j1", 1, 2, 2000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        TaskResultMessage resultMsg = TaskResultMessage.builder()
                .taskId("t1")
                .jobId("j1")
                .partitionId(1)
                .status("COMPLETED")
                .result(10L)
                .build();
                
        taskExecutionService.processTaskResult("w1", resultMsg);
        
        Job jAfter = jobRepository.findById("j1").orElseThrow();
        assertEquals("999", jAfter.getFinalResult()); // Preserved
    }

    @Test
    public void testU_MultiPartitionJob_CompletesProperly() {
        createWorker("A", WorkerState.ONLINE, 4, 16000);
        createWorker("B", WorkerState.ONLINE, 4, 16000);
        createWorker("C", WorkerState.ONLINE, 4, 16000);
        
        createJob("job1", 3);
        createTask("t1", "job1", 1, 4, 4000, TaskState.UNASSIGNED);
        createTask("t2", "job1", 2, 4, 4000, TaskState.UNASSIGNED);
        createTask("t3", "job1", 3, 4, 4000, TaskState.UNASSIGNED);
        
        schedulerService.scheduleTasks(); // Dispatch 3 tasks
        
        // w1 = A, w2 = B, w3 = C depending on tie breaker
        String w1 = taskAssignmentRepository.findByTaskId("t1").orElseThrow().getWorkerId();
        String w2 = taskAssignmentRepository.findByTaskId("t2").orElseThrow().getWorkerId();
        String w3 = taskAssignmentRepository.findByTaskId("t3").orElseThrow().getWorkerId();
        
        TaskResultMessage msg1 = TaskResultMessage.builder().taskId("t1").jobId("job1").partitionId(1).status("COMPLETED").result(100L).build();
        taskExecutionService.processTaskResult(w1, msg1);
        assertEquals(1, jobRepository.findById("job1").orElseThrow().getCompletedPartitions());
        assertEquals(JobState.RUNNING, jobRepository.findById("job1").orElseThrow().getState()); // Wait, it's RUNNING because dispatch transitioned it
        
        TaskResultMessage msg2 = TaskResultMessage.builder().taskId("t2").jobId("job1").partitionId(2).status("COMPLETED").result(200L).build();
        taskExecutionService.processTaskResult(w2, msg2);
        assertEquals(2, jobRepository.findById("job1").orElseThrow().getCompletedPartitions());
        
        TaskResultMessage msg3 = TaskResultMessage.builder().taskId("t3").jobId("job1").partitionId(3).status("COMPLETED").result(300L).build();
        taskExecutionService.processTaskResult(w3, msg3);
        assertEquals(3, jobRepository.findById("job1").orElseThrow().getCompletedPartitions());
        
        assertEquals(JobState.COMPLETED, jobRepository.findById("job1").orElseThrow().getState());
        assertEquals(4, resourceReservationService.getAvailableCpu("A"));
        assertEquals(4, resourceReservationService.getAvailableCpu("B"));
        assertEquals(4, resourceReservationService.getAvailableCpu("C"));
        
        Job completedJob = jobRepository.findById("job1").orElseThrow();
        assertEquals("600", completedJob.getFinalResult());
    }

    @Test
    public void testV_SinglePartitionAggregation() {
        createWorker("w1", WorkerState.ONLINE, 4, 16000);
        createJob("j1", 1);
        createTask("t1", "j1", 1, 1, 1000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        TaskResultMessage msg1 = TaskResultMessage.builder().taskId("t1").jobId("j1").partitionId(1).status("COMPLETED").result(55L).build();
        taskExecutionService.processTaskResult("w1", msg1);
        
        Job completedJob = jobRepository.findById("j1").orElseThrow();
        assertEquals("55", completedJob.getFinalResult());
        assertEquals(JobState.COMPLETED, completedJob.getState());
    }

    @Test
    public void testW_FailedPartitionPreventsAggregation() {
        createWorker("w1", WorkerState.ONLINE, 4, 16000);
        createJob("j1", 2);
        createTask("t1", "j1", 1, 1, 1000, TaskState.UNASSIGNED);
        createTask("t2", "j1", 2, 1, 1000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        TaskResultMessage msg1 = TaskResultMessage.builder().taskId("t1").jobId("j1").partitionId(1).status("COMPLETED").result(10L).build();
        taskExecutionService.processTaskResult("w1", msg1);
        
        TaskResultMessage msg2 = TaskResultMessage.builder().taskId("t2").jobId("j1").partitionId(2).status("FAILED").error("Failed").build();
        taskExecutionService.processTaskResult("w1", msg2);
        
        Job failedJob = jobRepository.findById("j1").orElseThrow();
        assertEquals(JobState.FAILED, failedJob.getState());
        assertNull(failedJob.getFinalResult());
    }

    @Test
    public void testX_LargeDeterministicSumRange() {
        createWorker("w1", WorkerState.ONLINE, 4, 16000);
        createJob("j1", 1);
        createTask("t1", "j1", 1, 1, 1000, TaskState.UNASSIGNED);
        schedulerService.scheduleTasks();
        
        long largeValue = 500000000500000000L; // Example of a very large sum
        TaskResultMessage msg1 = TaskResultMessage.builder().taskId("t1").jobId("j1").partitionId(1).status("COMPLETED").result(largeValue).build();
        taskExecutionService.processTaskResult("w1", msg1);
        
        Job completedJob = jobRepository.findById("j1").orElseThrow();
        assertEquals("500000000500000000", completedJob.getFinalResult());
    }

    @Test
    public void testY_ResultsFromAnotherJobNotIncluded() {
        createWorker("w1", WorkerState.ONLINE, 4, 16000);
        createJob("j1", 1);
        createJob("j2", 1); // Another job
        
        createTask("t1", "j1", 1, 1, 1000, TaskState.UNASSIGNED);
        createTask("t2", "j2", 1, 1, 1000, TaskState.UNASSIGNED); // Task for another job
        
        schedulerService.scheduleTasks();
        
        // Complete task for j2 first
        TaskResultMessage msg2 = TaskResultMessage.builder().taskId("t2").jobId("j2").partitionId(1).status("COMPLETED").result(100L).build();
        taskExecutionService.processTaskResult("w1", msg2);
        
        // Complete task for j1
        TaskResultMessage msg1 = TaskResultMessage.builder().taskId("t1").jobId("j1").partitionId(1).status("COMPLETED").result(50L).build();
        taskExecutionService.processTaskResult("w1", msg1);
        
        Job completedJob1 = jobRepository.findById("j1").orElseThrow();
        assertEquals("50", completedJob1.getFinalResult()); // Should only be 50, not 150
        
        Job completedJob2 = jobRepository.findById("j2").orElseThrow();
        assertEquals("100", completedJob2.getFinalResult());
    }
}
