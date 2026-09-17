package com.cluster.worker.task;

import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.SystemMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResourceAdmissionServiceTest {

    private ResourceAdmissionService admissionService;
    private WorkerConfig.Execution executionConfig;
    private SystemMetrics metrics;

    @BeforeEach
    void setUp() {
        admissionService = new ResourceAdmissionService();

        executionConfig = new WorkerConfig.Execution();
        executionConfig.setReservedMemoryMb(512);
        executionConfig.setReservedCpuCores(1);

        metrics = new SystemMetrics();
        metrics.setTotalMemoryMb(4096);
        metrics.setCpuCores(4);
    }

    @Test
    void testAdmitValidTask() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("task-1");
        task.setRequiredMemoryMb(1024);
        task.setRequiredCpuCores(2);

        boolean reserved = admissionService.tryReserve(task, metrics, executionConfig);
        assertTrue(reserved);
        assertEquals(1024, admissionService.getAllocatedMemoryMb());
        assertEquals(2, admissionService.getAllocatedCpuCores());
        assertTrue(admissionService.hasReservation("task-1"));
    }

    @Test
    void testRejectWhenTaskExceedsPhysicalUsableMemory() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("task-huge");
        task.setRequiredMemoryMb(4000); // 4096 - 512 = 3584 usable
        task.setRequiredCpuCores(1);

        boolean reserved = admissionService.tryReserve(task, metrics, executionConfig);
        assertFalse(reserved);
        assertEquals(0, admissionService.getAllocatedMemoryMb());
        assertFalse(admissionService.hasReservation("task-huge"));
    }

    @Test
    void testRejectWhenTaskExceedsPhysicalUsableCpu() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("task-high-cpu");
        task.setRequiredMemoryMb(512);
        task.setRequiredCpuCores(4); // 4 - 1 = 3 usable

        boolean reserved = admissionService.tryReserve(task, metrics, executionConfig);
        assertFalse(reserved);
        assertEquals(0, admissionService.getAllocatedCpuCores());
    }

    @Test
    void testRejectWhenCumulativeAllocationExceedsCapacity() {
        WorkerTask task1 = new WorkerTask();
        task1.setTaskId("task-1");
        task1.setRequiredMemoryMb(2000);
        task1.setRequiredCpuCores(2);
        assertTrue(admissionService.tryReserve(task1, metrics, executionConfig));

        WorkerTask task2 = new WorkerTask();
        task2.setTaskId("task-2");
        task2.setRequiredMemoryMb(2000); // 2000 + 2000 = 4000 > 3584
        task2.setRequiredCpuCores(1);
        assertFalse(admissionService.tryReserve(task2, metrics, executionConfig));
    }

    @Test
    void testReleaseIsIdempotent() {
        WorkerTask task = new WorkerTask();
        task.setTaskId("task-1");
        task.setRequiredMemoryMb(1024);
        task.setRequiredCpuCores(1);

        assertTrue(admissionService.tryReserve(task, metrics, executionConfig));
        assertEquals(1024, admissionService.getAllocatedMemoryMb());

        // First release succeeds
        assertTrue(admissionService.release("task-1"));
        assertEquals(0, admissionService.getAllocatedMemoryMb());

        // Second release returns false and does not go negative
        assertFalse(admissionService.release("task-1"));
        assertEquals(0, admissionService.getAllocatedMemoryMb());
        assertEquals(0, admissionService.getAllocatedCpuCores());
    }

    @Test
    void testConcurrentReservations() throws InterruptedException {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        // Usable memory: 4096 - 512 = 3584 MB. Each task asks for 500 MB. Max admitted: 7 tasks.
        for (int i = 0; i < threads; i++) {
            final String id = "task-" + i;
            pool.submit(() -> {
                WorkerTask task = new WorkerTask();
                task.setTaskId(id);
                task.setRequiredMemoryMb(500);
                task.setRequiredCpuCores(0);
                if (admissionService.tryReserve(task, metrics, executionConfig)) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(7, successCount.get());
        assertEquals(3500, admissionService.getAllocatedMemoryMb());
    }
}
