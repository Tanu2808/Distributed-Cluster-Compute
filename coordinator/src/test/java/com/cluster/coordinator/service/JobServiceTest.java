package com.cluster.coordinator.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.JobState;
import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.repository.JobRepository;
import com.cluster.coordinator.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JobServiceTest {

    @Autowired private JobRepository jobRepository;

    @Autowired private TaskRepository taskRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private JobService jobService;

    @BeforeEach
    public void setup() {
        jobRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    public void testValidSumRangeJob() { // A, F, H, I, J, M
        Job job =
                jobService.createJob(
                        "Test Job", "SUM_RANGE", "{\"start\":1,\"end\":12000000}", 3, 3072L);

        assertEquals(JobState.QUEUED, job.getState());
        assertEquals(3, job.getTotalPartitions());
        assertEquals(0, job.getCompletedPartitions());

        List<Task> tasks = jobService.getTasksForJob(job.getId());
        assertEquals(3, tasks.size());

        // F. Exact partition division & H. Partition IDs
        assertEquals(1, tasks.get(0).getPartitionId());
        assertEquals(2, tasks.get(1).getPartitionId());
        assertEquals(3, tasks.get(2).getPartitionId());

        assertTrue(tasks.get(0).getInput().contains("\"start\":1,\"end\":4000000"));
        assertTrue(tasks.get(1).getInput().contains("\"start\":4000001,\"end\":8000000"));
        assertTrue(tasks.get(2).getInput().contains("\"start\":8000001,\"end\":12000000"));

        // I, J. Resource distribution
        int totalCpu = 0;
        long totalMemory = 0;
        for (Task task : tasks) {
            totalCpu += task.getRequiredCpu();
            totalMemory += task.getRequiredMemory();
        }
        assertEquals(3, totalCpu);
        assertEquals(3072L, totalMemory);
    }

    @Test
    public void testInvalidTaskType() { // B
        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            jobService.createJob("Test Job", "UNKNOWN_TYPE", "{}", 1, 1024L);
                        });
        assertTrue(exception.getMessage().contains("Unsupported taskType"));
    }

    @Test
    public void testMissingInput() { // C
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    jobService.createJob("Test Job", "SUM_RANGE", null, 1, 1024L);
                });
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    jobService.createJob("Test Job", "SUM_RANGE", "", 1, 1024L);
                });
    }

    @Test
    public void testMalformedJson() { // D
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    jobService.createJob("Test Job", "SUM_RANGE", "{start:1}", 1, 1024L);
                });
    }

    @Test
    public void testStartGreaterThanEnd() { // E
        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            jobService.createJob(
                                    "Test Job", "SUM_RANGE", "{\"start\":100,\"end\":1}", 1, 1024L);
                        });
        assertTrue(exception.getMessage().contains("start must be less than or equal to end"));
    }

    @Test
    public void testUnevenDivision() { // G
        Job job =
                jobService.createJob(
                        "Uneven Job", "SUM_RANGE", "{\"start\":1,\"end\":10}", 3, 1024L);
        List<Task> tasks = jobService.getTasksForJob(job.getId());
        assertEquals(3, tasks.size());

        assertTrue(tasks.get(0).getInput().contains("\"start\":1,\"end\":4"));
        assertTrue(tasks.get(1).getInput().contains("\"start\":5,\"end\":7"));
        assertTrue(tasks.get(2).getInput().contains("\"start\":8,\"end\":10"));

        int totalCpu = tasks.stream().mapToInt(Task::getRequiredCpu).sum();
        long totalMemory = tasks.stream().mapToLong(Task::getRequiredMemory).sum();

        assertEquals(3, totalCpu);
        assertEquals(1024L, totalMemory);
    }

    @Test
    public void testLargeWorkload() { // K
        // Range spans 250,000,000 elements. Max per task is 100,000,000.
        // Even if we request 1 CPU (target partition = 1), it must create 3 partitions.
        Job job =
                jobService.createJob(
                        "Large Job", "SUM_RANGE", "{\"start\":1,\"end\":250000000}", 1, 1024L);

        List<Task> tasks = jobService.getTasksForJob(job.getId());
        assertEquals(3, tasks.size());
        assertEquals(3, job.getTotalPartitions());

        // Sum of CPUs must still equal requested (1).
        int totalCpu = tasks.stream().mapToInt(Task::getRequiredCpu).sum();
        assertEquals(1, totalCpu);

        // Ensure no task exceeds 100M
        assertTrue(tasks.get(0).getInput().contains("\"start\":1,\"end\":83333334"));
        assertTrue(tasks.get(1).getInput().contains("\"start\":83333335,\"end\":166666667"));
        assertTrue(tasks.get(2).getInput().contains("\"start\":166666668,\"end\":250000000"));
    }

    @Test
    public void testTransactionalJobCreationFailure() { // L
        long initialJobCount = jobRepository.count();
        long initialTaskCount = taskRepository.count();

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    jobService.createJob(
                            "Fail Job", "SUM_RANGE", "{\"start\":100,\"end\":1}", 1, 1024L);
                });

        assertEquals(initialJobCount, jobRepository.count());
        assertEquals(initialTaskCount, taskRepository.count());
    }
}
