package com.cluster.worker.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.TaskAssignmentMessage;
import com.cluster.shared.protocol.TaskResultMessage;
import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.task.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private WorkerConfig config;
    private WorkerStateManager stateManager;
    private ResourceAdmissionService admissionService;
    private TaskHandlerRegistry handlerRegistry;
    private TaskService taskService;

    @Mock private SystemMetricsProvider metricsProvider;
    @Mock private WebSocketConnectionManager connectionManager;
    @Mock private WorkerConfigurationStore configStore;

    @BeforeEach
    void setUp() {
        config = new WorkerConfig();
        WorkerConfig.Execution execConfig = new WorkerConfig.Execution();
        execConfig.setMaxConcurrentTasks(2);
        execConfig.setQueueCapacity(2);
        execConfig.setDefaultTimeoutSeconds(10);
        execConfig.setReservedMemoryMb(256);
        execConfig.setReservedCpuCores(1);
        config.setExecution(execConfig);

        stateManager = new WorkerStateManager();
        admissionService = new ResourceAdmissionService();

        SystemMetrics metrics = new SystemMetrics();
        metrics.setTotalMemoryMb(4096);
        metrics.setCpuCores(4);
        lenient().when(metricsProvider.collectMetrics()).thenReturn(metrics);
        lenient().when(configStore.getWorkerId()).thenReturn("test-worker-1");

        // Register default COMPUTE and SUM_RANGE handlers
        handlerRegistry =
                new TaskHandlerRegistry(
                        List.of(new ComputeTaskHandler(), new SumRangeTaskHandler()));

        taskService =
                new TaskService(
                        config,
                        stateManager,
                        admissionService,
                        metricsProvider,
                        handlerRegistry,
                        connectionManager,
                        configStore);
    }

    @AfterEach
    void tearDown() {
        if (taskService != null) {
            taskService.shutdown();
        }
    }

    @Test
    void testTaskSubmissionAndSuccessfulCompletion() throws InterruptedException {
        Map<String, Object> input = new HashMap<>();
        input.put("operation", "add");
        input.put("a", 10.0);
        input.put("b", 25.0);

        TaskAssignmentMessage assignment =
                new TaskAssignmentMessage("task-success-1", "COMPUTE", input, 1, 256, 5);

        taskService.submitTask(assignment);

        // Await completion (max 3 seconds)
        WorkerTask task = null;
        for (int i = 0; i < 30; i++) {
            task = taskService.getTask("task-success-1").orElse(null);
            if (task != null && task.getState() == TaskState.COMPLETED) {
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(task);
        assertEquals(TaskState.COMPLETED, task.getState());
        assertNotNull(task.getResult());
        assertTrue(task.getResult() instanceof Map);
        assertEquals(35.0, ((Map<?, ?>) task.getResult()).get("result"));
        assertNotNull(task.getStartedAt());
        assertNotNull(task.getCompletedAt());
        assertNull(task.getErrorMessage());

        // Verify resources were released
        assertEquals(0, admissionService.getAllocatedMemoryMb());
        assertEquals(0, admissionService.getAllocatedCpuCores());

        // Verify result message sent
        verify(connectionManager, atLeastOnce()).sendMessage(eq("/app/worker.task.result"), any());
    }

    @Test
    void testDuplicateTaskIdRejected() {
        TaskAssignmentMessage first =
                new TaskAssignmentMessage(
                        "task-dup-1",
                        "COMPUTE",
                        Map.of("operation", "add", "a", 1, "b", 2),
                        1,
                        128,
                        5);
        TaskAssignmentMessage duplicate =
                new TaskAssignmentMessage(
                        "task-dup-1",
                        "COMPUTE",
                        Map.of("operation", "add", "a", 3, "b", 4),
                        1,
                        128,
                        5);

        taskService.submitTask(first);
        taskService.submitTask(duplicate);

        // One valid task in repository
        assertEquals(1, taskService.getAllTasks().size());
        verify(connectionManager, atLeastOnce()).sendMessage(eq("/app/worker.task.status"), any());
    }

    @Test
    void testInvalidTaskAssignmentRejected() {
        // Missing taskId
        TaskAssignmentMessage invalid1 =
                new TaskAssignmentMessage("", "COMPUTE", Map.of(), 1, 128, 5);
        taskService.submitTask(invalid1);
        assertTrue(taskService.getAllTasks().isEmpty());

        // Unsupported task type
        TaskAssignmentMessage invalid2 =
                new TaskAssignmentMessage(
                        "task-unsupported", "NON_EXISTENT_TYPE", Map.of(), 1, 128, 5);
        taskService.submitTask(invalid2);
        WorkerTask rejected = taskService.getTask("task-unsupported").orElse(null);
        // Either not saved or saved as REJECTED
        if (rejected != null) {
            assertEquals(TaskState.FAILED, rejected.getState());
        }

        // Negative resources
        TaskAssignmentMessage invalid3 =
                new TaskAssignmentMessage("task-neg", "COMPUTE", Map.of(), -1, -500, 5);
        taskService.submitTask(invalid3);
        assertNull(taskService.getTask("task-neg").orElse(null));
    }

    @Test
    void testQueueCapacityRespected() throws InterruptedException {
        WorkerConfig customConfig = new WorkerConfig();
        WorkerConfig.Execution execConfig = new WorkerConfig.Execution();
        execConfig.setMaxConcurrentTasks(1);
        execConfig.setQueueCapacity(2);
        execConfig.setDefaultTimeoutSeconds(10);
        execConfig.setReservedMemoryMb(256);
        execConfig.setReservedCpuCores(1);
        customConfig.setExecution(execConfig);

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch blockerLatch = new CountDownLatch(1);
        TaskHandler blockingHandler =
                new TaskHandler() {
                    @Override
                    public boolean supports(String taskType) {
                        return "BLOCKING".equals(taskType);
                    }

                    @Override
                    public Object execute(WorkerTask task) throws Exception {
                        taskStarted.countDown();
                        blockerLatch.await(5, TimeUnit.SECONDS);
                        return "done";
                    }
                };

        handlerRegistry = new TaskHandlerRegistry(List.of(blockingHandler));
        taskService =
                new TaskService(
                        customConfig,
                        stateManager,
                        admissionService,
                        metricsProvider,
                        handlerRegistry,
                        connectionManager,
                        configStore);

        // Task 0 starts running
        taskService.submitTask(
                new TaskAssignmentMessage("task-fill-0", "BLOCKING", Map.of(), 0, 100, 10));
        assertTrue(taskStarted.await(3, TimeUnit.SECONDS));

        // Tasks 1 and 2 fill queue capacity of 2
        taskService.submitTask(
                new TaskAssignmentMessage("task-fill-1", "BLOCKING", Map.of(), 0, 100, 10));
        taskService.submitTask(
                new TaskAssignmentMessage("task-fill-2", "BLOCKING", Map.of(), 0, 100, 10));

        // 4th task must be rejected due to full queue
        taskService.submitTask(
                new TaskAssignmentMessage("task-overflow", "BLOCKING", Map.of(), 0, 100, 10));

        WorkerTask overflowTask = taskService.getTask("task-overflow").orElse(null);
        assertNotNull(overflowTask);
        assertEquals(TaskState.FAILED, overflowTask.getState());
        assertTrue(overflowTask.getErrorMessage().contains("queue is full"));

        blockerLatch.countDown();
    }

    @Test
    void testTaskExecutionFailure() throws InterruptedException {
        // Fibonacci with invalid bounds (n = 2000 > 1000) throws IllegalArgumentException
        Map<String, Object> input = new HashMap<>();
        input.put("operation", "fibonacci");
        input.put("n", 2000);

        TaskAssignmentMessage assignment =
                new TaskAssignmentMessage("task-fail-1", "COMPUTE", input, 1, 128, 5);

        taskService.submitTask(assignment);

        WorkerTask task = null;
        for (int i = 0; i < 30; i++) {
            task = taskService.getTask("task-fail-1").orElse(null);
            if (task != null && task.getState() == TaskState.FAILED) {
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(task);
        assertEquals(TaskState.FAILED, task.getState());
        assertNotNull(task.getErrorMessage());
        assertNotNull(task.getCompletedAt());

        // Resources must be released
        assertEquals(0, admissionService.getAllocatedMemoryMb());
    }

    @Test
    void testTaskCancellationWhileRunning() throws InterruptedException {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskInterrupted = new CountDownLatch(1);

        TaskHandler slowHandler =
                new TaskHandler() {
                    @Override
                    public boolean supports(String taskType) {
                        return "SLOW".equals(taskType);
                    }

                    @Override
                    public Object execute(WorkerTask task) throws Exception {
                        taskStarted.countDown();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            taskInterrupted.countDown();
                            throw e;
                        }
                        return "completed-unexpectedly";
                    }
                };

        handlerRegistry = new TaskHandlerRegistry(List.of(slowHandler));
        taskService =
                new TaskService(
                        config,
                        stateManager,
                        admissionService,
                        metricsProvider,
                        handlerRegistry,
                        connectionManager,
                        configStore);

        taskService.submitTask(
                new TaskAssignmentMessage("task-cancel-run", "SLOW", Map.of(), 1, 200, 10));

        // Wait until task has actually started running
        assertTrue(taskStarted.await(3, TimeUnit.SECONDS));

        // Cancel it while running
        boolean cancelled = taskService.cancelTask("task-cancel-run");
        assertTrue(cancelled);

        // Verify thread received interruption
        assertTrue(taskInterrupted.await(3, TimeUnit.SECONDS));

        WorkerTask task = taskService.getTask("task-cancel-run").orElseThrow();
        assertEquals(TaskState.CANCELLED, task.getState());
        assertNotNull(task.getCompletedAt());

        // Verify resources released
        assertEquals(0, admissionService.getAllocatedMemoryMb());

        // Ensure state is never overwritten with COMPLETED
        Thread.sleep(200);
        assertEquals(TaskState.CANCELLED, task.getState());
    }

    @Test
    void testTaskCancellationWhileQueued() throws InterruptedException {
        CountDownLatch tasksRunning = new CountDownLatch(2);
        CountDownLatch blockWorker = new CountDownLatch(1);

        TaskHandler blockingHandler =
                new TaskHandler() {
                    @Override
                    public boolean supports(String taskType) {
                        return "BLOCK".equals(taskType);
                    }

                    @Override
                    public Object execute(WorkerTask task) throws Exception {
                        tasksRunning.countDown();
                        blockWorker.await(5, TimeUnit.SECONDS);
                        return "ok";
                    }
                };

        handlerRegistry = new TaskHandlerRegistry(List.of(blockingHandler));
        taskService =
                new TaskService(
                        config,
                        stateManager,
                        admissionService,
                        metricsProvider,
                        handlerRegistry,
                        connectionManager,
                        configStore);

        // Saturated execution pool (size 2)
        taskService.submitTask(new TaskAssignmentMessage("t1", "BLOCK", Map.of(), 0, 100, 10));
        taskService.submitTask(new TaskAssignmentMessage("t2", "BLOCK", Map.of(), 0, 100, 10));

        // Ensure both t1 and t2 are actively running
        assertTrue(tasksRunning.await(3, TimeUnit.SECONDS));

        // Third task will be in queue
        taskService.submitTask(
                new TaskAssignmentMessage("t3-queued", "BLOCK", Map.of(), 0, 100, 10));

        assertEquals(TaskState.ASSIGNED, taskService.getTask("t3-queued").orElseThrow().getState());

        // Cancel queued task
        boolean cancelled = taskService.cancelTask("t3-queued");
        assertTrue(cancelled);
        assertEquals(
                TaskState.CANCELLED, taskService.getTask("t3-queued").orElseThrow().getState());

        blockWorker.countDown();
    }

    @Test
    void testTerminalStateCannotBeCancelledAgain() throws InterruptedException {
        taskService.submitTask(
                new TaskAssignmentMessage(
                        "t-comp",
                        "COMPUTE",
                        Map.of("operation", "add", "a", 1, "b", 1),
                        1,
                        100,
                        5));

        for (int i = 0; i < 30; i++) {
            WorkerTask t = taskService.getTask("t-comp").orElse(null);
            if (t != null && t.getState() == TaskState.COMPLETED) break;
            Thread.sleep(100);
        }

        WorkerTask completed = taskService.getTask("t-comp").orElseThrow();
        assertEquals(TaskState.COMPLETED, completed.getState());

        // Trying to cancel an already COMPLETED task must return false
        boolean result = taskService.cancelTask("t-comp");
        assertFalse(result);
        assertEquals(TaskState.COMPLETED, completed.getState());
    }

    @Test
    void testTaskTimeoutFailsAndReleasesResources() throws InterruptedException {
        TaskHandler hangingHandler =
                new TaskHandler() {
                    @Override
                    public boolean supports(String taskType) {
                        return "HANG".equals(taskType);
                    }

                    @Override
                    public Object execute(WorkerTask task) throws Exception {
                        Thread.sleep(5000);
                        return "done";
                    }
                };

        handlerRegistry = new TaskHandlerRegistry(List.of(hangingHandler));
        taskService =
                new TaskService(
                        config,
                        stateManager,
                        admissionService,
                        metricsProvider,
                        handlerRegistry,
                        connectionManager,
                        configStore);

        // Timeout set to 1 second
        taskService.submitTask(
                new TaskAssignmentMessage("task-timeout", "HANG", Map.of(), 1, 300, 1));

        WorkerTask task = null;
        for (int i = 0; i < 40; i++) {
            task = taskService.getTask("task-timeout").orElse(null);
            if (task != null && task.getState() == TaskState.FAILED) {
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(task);
        assertEquals(TaskState.FAILED, task.getState());
        assertTrue(task.getErrorMessage().contains("timed out"));
        assertNotNull(task.getCompletedAt());

        // Resources must be released
        assertEquals(0, admissionService.getAllocatedMemoryMb());
    }

    @Test
    void testSumRangeLifecycleAndResultMetadataPropagation() throws InterruptedException {
        TaskAssignmentMessage assignment =
                TaskAssignmentMessage.builder()
                        .taskId("sum-range-task-1")
                        .taskType("SUM_RANGE")
                        .input(Map.of("start", 1, "end", 100))
                        .requiredCpuCores(1)
                        .requiredMemoryMb(128)
                        .timeoutSeconds(5)
                        .jobId("job-distributed-99")
                        .partitionId(2)
                        .totalPartitions(5)
                        .build();

        taskService.submitTask(assignment);

        WorkerTask task = null;
        for (int i = 0; i < 30; i++) {
            task = taskService.getTask("sum-range-task-1").orElse(null);
            if (task != null && task.getState() == TaskState.COMPLETED) {
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(task);
        assertEquals(TaskState.COMPLETED, task.getState());
        assertEquals(5050L, task.getResult());
        assertEquals("job-distributed-99", task.getJobId());
        assertEquals(2, task.getPartitionId());
        assertEquals(5, task.getTotalPartitions());
        assertFalse(admissionService.hasReservation("sum-range-task-1"));

        // Verify result message sent over WebSocket with proper metadata
        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(connectionManager, atLeastOnce())
                .sendMessage(eq("/app/worker.task.result"), captor.capture());

        TaskResultMessage resultMsg = (TaskResultMessage) captor.getValue().getPayload();
        assertEquals("sum-range-task-1", resultMsg.getTaskId());
        assertEquals("COMPLETED", resultMsg.getStatus());
        assertEquals(5050L, resultMsg.getResult());
        assertEquals("job-distributed-99", resultMsg.getJobId());
        assertEquals(2, resultMsg.getPartitionId());
        assertNull(resultMsg.getError());
    }

    @Test
    void testSumRange1ToMillionComputesCorrectly() throws InterruptedException {
        TaskAssignmentMessage assignment =
                TaskAssignmentMessage.builder()
                        .taskId("sum-range-million")
                        .taskType("SUM_RANGE")
                        .input(Map.of("start", 1, "end", 1_000_000))
                        .requiredCpuCores(1)
                        .requiredMemoryMb(128)
                        .timeoutSeconds(10)
                        .build();

        taskService.submitTask(assignment);

        WorkerTask task = null;
        for (int i = 0; i < 30; i++) {
            task = taskService.getTask("sum-range-million").orElse(null);
            if (task != null && task.getState() == TaskState.COMPLETED) {
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(task);
        assertEquals(TaskState.COMPLETED, task.getState());
        assertEquals(500000500000L, task.getResult());
        assertFalse(admissionService.hasReservation("sum-range-million"));
    }

    @Test
    void testSumRangeInvalidInputFailsAndReleasesResources() throws InterruptedException {
        // start > end (invalid input)
        TaskAssignmentMessage assignment =
                TaskAssignmentMessage.builder()
                        .taskId("sum-invalid-range")
                        .taskType("SUM_RANGE")
                        .input(Map.of("start", 100, "end", 10))
                        .requiredCpuCores(1)
                        .requiredMemoryMb(128)
                        .timeoutSeconds(5)
                        .jobId("job-err-1")
                        .partitionId(0)
                        .build();

        taskService.submitTask(assignment);

        WorkerTask task = null;
        for (int i = 0; i < 30; i++) {
            task = taskService.getTask("sum-invalid-range").orElse(null);
            if (task != null && task.getState() == TaskState.FAILED) {
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(task);
        assertEquals(TaskState.FAILED, task.getState());
        assertNotNull(task.getErrorMessage());
        assertTrue(task.getErrorMessage().contains("must be less than or equal to end"));
        assertFalse(admissionService.hasReservation("sum-invalid-range"));

        // Verify result message sent with FAILED status and error
        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(connectionManager, atLeastOnce())
                .sendMessage(eq("/app/worker.task.result"), captor.capture());

        TaskResultMessage resultMsg = (TaskResultMessage) captor.getValue().getPayload();
        assertEquals("sum-invalid-range", resultMsg.getTaskId());
        assertEquals("FAILED", resultMsg.getStatus());
        assertNull(resultMsg.getResult());
        assertTrue(resultMsg.getError().contains("must be less than or equal to end"));
        assertEquals("job-err-1", resultMsg.getJobId());
        assertEquals(0, resultMsg.getPartitionId());
    }

    @Test
    void testSumRangeMissingFieldFailsWithoutDefaulting() throws InterruptedException {
        // Missing start field completely
        TaskAssignmentMessage assignment =
                TaskAssignmentMessage.builder()
                        .taskId("sum-missing-field")
                        .taskType("SUM_RANGE")
                        .input(Map.of("end", 100))
                        .requiredCpuCores(1)
                        .requiredMemoryMb(128)
                        .timeoutSeconds(5)
                        .build();

        taskService.submitTask(assignment);

        WorkerTask task = null;
        for (int i = 0; i < 30; i++) {
            task = taskService.getTask("sum-missing-field").orElse(null);
            if (task != null && task.getState() == TaskState.FAILED) {
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(task);
        assertEquals(TaskState.FAILED, task.getState());
        assertTrue(task.getErrorMessage().contains("Missing required parameter: start"));
        assertFalse(admissionService.hasReservation("sum-missing-field"));
    }

    @Test
    void testSumRangeCancellationWhileRunning() throws InterruptedException {
        // Large range so it has work to do while we trigger cancel
        TaskAssignmentMessage assignment =
                TaskAssignmentMessage.builder()
                        .taskId("sum-cancel-task")
                        .taskType("SUM_RANGE")
                        .input(Map.of("start", 1, "end", 50_000_000))
                        .requiredCpuCores(1)
                        .requiredMemoryMb(128)
                        .timeoutSeconds(10)
                        .build();

        taskService.submitTask(assignment);

        // Cancel immediately or after short delay
        Thread.sleep(2);
        boolean cancelled = taskService.cancelTask("sum-cancel-task");
        assertTrue(cancelled);

        WorkerTask task = taskService.getTask("sum-cancel-task").orElse(null);
        assertNotNull(task);
        assertEquals(TaskState.CANCELLED, task.getState());
        assertFalse(admissionService.hasReservation("sum-cancel-task"));

        // Wait to verify terminal state CANCELLED is never overwritten
        Thread.sleep(200);
        assertEquals(TaskState.CANCELLED, task.getState());
    }
}
