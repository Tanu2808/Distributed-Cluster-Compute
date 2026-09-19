package com.cluster.coordinator.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cluster.coordinator.exception.GlobalExceptionHandler;
import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.JobState;
import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.model.TaskState;
import com.cluster.coordinator.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class JobControllerTest {

    private MockMvc mockMvc;
    private JobService jobService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        jobService = Mockito.mock(JobService.class);
        objectMapper = new ObjectMapper();
        JobController jobController = new JobController(jobService, objectMapper);
        mockMvc =
                MockMvcBuilders.standaloneSetup(jobController)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    public void testCreateJobSuccess() throws Exception {
        Job mockJob = new Job();
        mockJob.setId("job-123");
        mockJob.setTaskType("SUM_RANGE");
        mockJob.setState(JobState.QUEUED);
        mockJob.setRequestedCpu(4);
        mockJob.setRequestedMemory(1024L);
        mockJob.setTotalPartitions(4);
        mockJob.setCompletedPartitions(0);

        when(jobService.createJob(anyString(), anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(mockJob);

        String requestBody =
                """
                {
                    "taskType": "SUM_RANGE",
                    "input": {
                        "start": 1,
                        "end": 100000
                    },
                    "requestedCpu": 4,
                    "requestedMemory": 1024
                }
                """;

        mockMvc.perform(
                        post("/api/jobs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.taskType").value("SUM_RANGE"))
                .andExpect(jsonPath("$.state").value("QUEUED"))
                .andExpect(jsonPath("$.totalPartitions").value(4));
    }

    @Test
    public void testCreateJobMissingTaskType() throws Exception {
        String requestBody =
                """
                {
                    "input": {
                        "start": 1,
                        "end": 100000
                    },
                    "requestedCpu": 4,
                    "requestedMemory": 1024
                }
                """;

        mockMvc.perform(
                        post("/api/jobs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.taskType").value("taskType is required"));
    }

    @Test
    public void testCreateJobInvalidCpu() throws Exception {
        String requestBody =
                """
                {
                    "taskType": "SUM_RANGE",
                    "input": {
                        "start": 1,
                        "end": 100000
                    },
                    "requestedCpu": 0,
                    "requestedMemory": 1024
                }
                """;

        mockMvc.perform(
                        post("/api/jobs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.requestedCpu").value("requestedCpu must be greater than 0"));
    }

    @Test
    public void testCreateJobUnsupportedTaskType() throws Exception {
        when(jobService.createJob(anyString(), anyString(), anyString(), anyInt(), anyLong()))
                .thenThrow(new IllegalArgumentException("Unsupported taskType: UNKNOWN"));

        String requestBody =
                """
                {
                    "taskType": "UNKNOWN",
                    "input": {
                        "start": 1,
                        "end": 100000
                    },
                    "requestedCpu": 1,
                    "requestedMemory": 1024
                }
                """;

        mockMvc.perform(
                        post("/api/jobs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported taskType: UNKNOWN"));
    }

    @Test
    public void testGetJobSuccess() throws Exception {
        Job mockJob = new Job();
        mockJob.setId("job-123");
        mockJob.setTaskType("SUM_RANGE");
        mockJob.setState(JobState.QUEUED);
        mockJob.setFinalResult("55");

        when(jobService.getJob("job-123")).thenReturn(Optional.of(mockJob));

        mockMvc.perform(get("/api/jobs/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.state").value("QUEUED"))
                .andExpect(jsonPath("$.finalResult").value("55"));
    }

    @Test
    public void testGetJobMissing() throws Exception {
        when(jobService.getJob("job-999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/job-999")).andExpect(status().isNotFound());
    }

    @Test
    public void testGetJobTasksSuccess() throws Exception {
        Job mockJob = new Job();
        mockJob.setId("job-123");

        Task mockTask = new Task();
        mockTask.setId("task-1");
        mockTask.setJobId("job-123");
        mockTask.setPartitionId(1);
        mockTask.setTaskType("SUM_RANGE");
        mockTask.setInput("{\"start\":1,\"end\":10}");
        mockTask.setState(TaskState.UNASSIGNED);

        when(jobService.getJob("job-123")).thenReturn(Optional.of(mockJob));
        when(jobService.getTasksForJob("job-123")).thenReturn(List.of(mockTask));

        mockMvc.perform(get("/api/jobs/job-123/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value("task-1"))
                .andExpect(jsonPath("$[0].partitionId").value(1))
                .andExpect(jsonPath("$[0].input.start").value(1));
    }

    @Test
    public void testGetTasksMissingJob() throws Exception {
        when(jobService.getJob("job-999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/job-999/tasks")).andExpect(status().isNotFound());
    }
}
