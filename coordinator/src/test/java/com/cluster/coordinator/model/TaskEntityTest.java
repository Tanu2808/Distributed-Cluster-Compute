package com.cluster.coordinator.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TaskEntityTest {

    @Test
    public void testTaskEntityState() {
        Task task = new Task();
        task.setId("task-1");
        task.setJobId("job-1");
        task.setTaskType("SUM_RANGE");
        task.setInput("{\"start\":1,\"end\":25}");
        task.setPartitionId(1);
        task.setRequiredCpu(1);
        task.setRequiredMemory(256L);
        task.setState(TaskState.UNASSIGNED);

        assertEquals("task-1", task.getId());
        assertEquals("job-1", task.getJobId());
        assertEquals("SUM_RANGE", task.getTaskType());
        assertEquals("{\"start\":1,\"end\":25}", task.getInput());
        assertEquals(1, task.getPartitionId());
        assertEquals(1, task.getRequiredCpu());
        assertEquals(256L, task.getRequiredMemory());
        assertEquals(TaskState.UNASSIGNED, task.getState());
    }
}
