package com.cluster.worker.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SumRangeTaskHandlerTest {

    private SumRangeTaskHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SumRangeTaskHandler();
    }

    @Test
    void testSupports() {
        assertTrue(handler.supports("SUM_RANGE"));
        assertTrue(handler.supports("sum_range"));
        assertTrue(handler.supports("Sum_Range"));
        assertFalse(handler.supports("COMPUTE"));
        assertFalse(handler.supports(null));
    }

    @Test
    void testSumRange1To10() throws Exception {
        WorkerTask task = createTask(1, 10);
        Object result = handler.execute(task);
        assertNotNull(result);
        assertEquals(55L, result);
    }

    @Test
    void testSumRange1To100() throws Exception {
        WorkerTask task = createTask(1, 100);
        Object result = handler.execute(task);
        assertNotNull(result);
        assertEquals(5050L, result);
    }

    @Test
    void testSumRange1To1000000() throws Exception {
        WorkerTask task = createTask(1, 1_000_000);
        Object result = handler.execute(task);
        assertNotNull(result);
        assertEquals(500000500000L, result);
    }

    @Test
    void testSumRangeSingleElement() throws Exception {
        WorkerTask task = createTask(42, 42);
        Object result = handler.execute(task);
        assertEquals(42L, result);
    }

    @Test
    void testSumRangeNegativeNumbers() throws Exception {
        WorkerTask task = createTask(-10, -1);
        Object result = handler.execute(task);
        assertEquals(-55L, result);

        WorkerTask zeroCrossingTask = createTask(-5, 5);
        assertEquals(0L, handler.execute(zeroCrossingTask));
    }

    @Test
    void testSumRangeStringInputs() throws Exception {
        WorkerTask task = new WorkerTask();
        task.setTaskId("str-task");
        task.setInput(Map.of("start", "1", "end", "10"));

        Object result = handler.execute(task);
        assertEquals(55L, result);
    }

    @Test
    void testSumRangeBigIntegerAndBigDecimalInputs() throws Exception {
        WorkerTask task = new WorkerTask();
        task.setTaskId("big-num-task");
        task.setInput(Map.of(
                "start", new BigInteger("1"),
                "end", new BigDecimal("100")
        ));

        Object result = handler.execute(task);
        assertEquals(5050L, result);
    }

    @Test
    void testLargeSupportedRange() throws Exception {
        // 1 to 10,000,000 = 10,000,000 * 10,000,001 / 2 = 50,000,005,000,000
        WorkerTask task = createTask(1, 10_000_000);
        Object result = handler.execute(task);
        assertEquals(50000005000000L, result);
    }

    @Test
    void testLargeOffsetRange() throws Exception {
        // 1,000,000 to 1,001,000
        WorkerTask task = createTask(1_000_000L, 1_001_000L);
        Object result = handler.execute(task);
        // Formula: (1001000 - 1000000 + 1) * (1000000 + 1001000) / 2 = 1001 * 2001000 / 2 = 1,001,500,500
        assertEquals(1001500500L, result);
    }

    @Test
    void testMissingStartThrowsException() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("t-missing-start");
        task.setInput(Map.of("end", 100));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(task));
        assertTrue(ex.getMessage().contains("Missing required parameter: start"));
    }

    @Test
    void testMissingEndThrowsException() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("t-missing-end");
        task.setInput(Map.of("start", 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(task));
        assertTrue(ex.getMessage().contains("Missing required parameter: end"));
    }

    @Test
    void testNullStartThrowsException() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("t-null-start");
        Map<String, Object> input = new HashMap<>();
        input.put("start", null);
        input.put("end", 100);
        task.setInput(input);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(task));
        assertTrue(ex.getMessage().contains("Missing required parameter: start"));
    }

    @Test
    void testNullEndThrowsException() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("t-null-end");
        Map<String, Object> input = new HashMap<>();
        input.put("start", 1);
        input.put("end", null);
        task.setInput(input);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(task));
        assertTrue(ex.getMessage().contains("Missing required parameter: end"));
    }

    @Test
    void testNullInputThrowsException() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("t-null-input");
        task.setInput(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(task));
        assertTrue(ex.getMessage().contains("SUM_RANGE task requires input parameters"));
    }

    @Test
    void testStartGreaterThanEndThrowsException() {
        WorkerTask task = createTask(100, 10);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(task));
        assertTrue(ex.getMessage().contains("must be less than or equal to end"));
    }

    @Test
    void testNonNumericInputThrowsException() {
        WorkerTask task1 = new WorkerTask();
        task1.setTaskId("t-non-num-1");
        task1.setInput(Map.of("start", "not-a-number", "end", 10));
        assertThrows(IllegalArgumentException.class, () -> handler.execute(task1));

        WorkerTask task2 = new WorkerTask();
        task2.setTaskId("t-non-num-2");
        task2.setInput(Map.of("start", true, "end", 10));
        assertThrows(IllegalArgumentException.class, () -> handler.execute(task2));
    }

    @Test
    void testFloatingPointInputThrowsException() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("t-float");
        task.setInput(Map.of("start", 1.5, "end", 10));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(task));
        assertTrue(ex.getMessage().contains("must be an integer"));
    }

    @Test
    void testRangeSpanExceedingLimitThrowsException() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("t-exceed-span");
        // Safe limit is 100,000,000 span
        task.setInput(Map.of("start", 1L, "end", 100_000_005L));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(task));
        assertTrue(ex.getMessage().contains("exceeds maximum supported limit"));
    }

    @Test
    void testValuesOutsideSafeLimitsThrowsException() {
        WorkerTask taskStartTooLow = new WorkerTask();
        taskStartTooLow.setInput(Map.of("start", -2_000_000_000_000L, "end", 0));
        assertThrows(IllegalArgumentException.class, () -> handler.execute(taskStartTooLow));

        WorkerTask taskEndTooHigh = new WorkerTask();
        taskEndTooHigh.setInput(Map.of("start", 0, "end", 2_000_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> handler.execute(taskEndTooHigh));
    }

    @Test
    void testPreCancelledTaskThrowsInterruptedException() {
        WorkerTask task = createTask(1, 100);
        task.setState(TaskState.CANCELLED);

        assertThrows(InterruptedException.class, () -> handler.execute(task));
    }

    @Test
    void testInterruptedThreadThrowsInterruptedException() {
        WorkerTask task = createTask(1, 100);
        Thread.currentThread().interrupt();

        try {
            assertThrows(InterruptedException.class, () -> handler.execute(task));
        } finally {
            // Clear interrupted status
            Thread.interrupted();
        }
    }

    @Test
    void testCancellationDuringExecution() throws Exception {
        WorkerTask task = createTask(1, 10_000_000);
        AtomicBoolean cancelledDuringRun = new AtomicBoolean(false);

        Thread workerThread = new Thread(() -> {
            try {
                handler.execute(task);
            } catch (InterruptedException e) {
                cancelledDuringRun.set(true);
            } catch (Exception ignored) {
            }
        });

        workerThread.start();
        // Allow thread to start running summation loop
        Thread.sleep(2);
        task.setState(TaskState.CANCELLED);
        workerThread.interrupt();
        workerThread.join(2000);

        assertTrue(cancelledDuringRun.get(), "Handler should cooperatively catch cancellation/interrupt during loop");
    }

    private WorkerTask createTask(long start, long end) {
        WorkerTask task = new WorkerTask();
        task.setTaskId("sum-task-" + start + "-" + end);
        task.setTaskType("SUM_RANGE");
        task.setInput(Map.of("start", start, "end", end));
        task.setState(TaskState.RUNNING);
        return task;
    }
}
