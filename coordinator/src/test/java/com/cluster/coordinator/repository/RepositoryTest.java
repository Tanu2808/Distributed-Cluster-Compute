package com.cluster.coordinator.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.JobState;
import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.model.TaskState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RepositoryTest {

    @Autowired private JobRepository jobRepository;

    @Autowired private TaskRepository taskRepository;

    @Test
    public void testJobToTaskRelationshipAndQueries() {
        taskRepository.deleteAll();
        jobRepository.deleteAll();
        
        // Create Job
        Job job = new Job();
        job.setId("job-100");
        job.setName("Sum Job");
        job.setTaskType("SUM_RANGE");
        job.setInput("{\"start\":1,\"end\":100}");
        job.setRequestedCpu(4);
        job.setRequestedMemory(1024L);
        job.setTotalPartitions(4);
        job.setCompletedPartitions(0);
        job.setState(JobState.SUBMITTED);
        jobRepository.save(job);

        // Create Tasks
        for (int i = 0; i < 4; i++) {
            Task task = new Task();
            task.setId("task-" + i);
            task.setJobId(job.getId());
            task.setTaskType("SUM_RANGE");
            task.setInput("{\"start\":" + (i * 25 + 1) + ",\"end\":" + ((i + 1) * 25) + "}");
            task.setPartitionId(i);
            task.setRequiredCpu(1);
            task.setRequiredMemory(256L);
            task.setState(TaskState.UNASSIGNED);
            taskRepository.save(task);
        }

        // Test Job Query
        List<Job> submittedJobs = jobRepository.findByState(JobState.SUBMITTED);
        assertEquals(1, submittedJobs.size());
        assertEquals("job-100", submittedJobs.get(0).getId());

        // Test Task Query by JobId
        List<Task> jobTasks = taskRepository.findByJobId("job-100");
        assertEquals(4, jobTasks.size());

        // Test Task Query by JobId and PartitionId
        Optional<Task> specificTask = taskRepository.findByJobIdAndPartitionId("job-100", 2);
        assertTrue(specificTask.isPresent());
        assertEquals("task-2", specificTask.get().getId());
        assertEquals(2, specificTask.get().getPartitionId());

        // Test Task Query by State
        List<Task> unassignedTasks = taskRepository.findByState(TaskState.UNASSIGNED);
        assertEquals(4, unassignedTasks.size());
    }
}
