package com.cluster.coordinator.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JobEntityTest {

    @Test
    public void testJobEntityState() {
        Job job = new Job();
        job.setId("job-1");
        job.setName("Test Job");
        job.setTaskType("SUM_RANGE");
        job.setInput("{\"start\":1,\"end\":100}");
        job.setRequestedCpu(4);
        job.setRequestedMemory(1024L);
        job.setTotalPartitions(4);
        job.setCompletedPartitions(0);
        job.setState(JobState.SUBMITTED);

        assertEquals("job-1", job.getId());
        assertEquals("Test Job", job.getName());
        assertEquals("SUM_RANGE", job.getTaskType());
        assertEquals("{\"start\":1,\"end\":100}", job.getInput());
        assertEquals(4, job.getRequestedCpu());
        assertEquals(1024L, job.getRequestedMemory());
        assertEquals(4, job.getTotalPartitions());
        assertEquals(0, job.getCompletedPartitions());
        assertEquals(JobState.SUBMITTED, job.getState());
    }
}
