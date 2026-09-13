package com.cluster.worker.api;

import com.cluster.worker.registration.WorkerIdentityGenerator;
import com.cluster.worker.service.WorkerLifecycleService;
import com.cluster.worker.model.WorkerState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkerController.class)
public class WorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkerLifecycleService lifecycleService;

    @MockBean
    private WorkerIdentityGenerator identityGenerator;

    @Test
    public void testGetWorkerStatus() throws Exception {
        when(lifecycleService.getState()).thenReturn(WorkerState.ONLINE);

        mockMvc.perform(get("/api/worker/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    public void testGetWorkerInfo() throws Exception {
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("test-id-123");

        mockMvc.perform(get("/api/worker/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value("test-id-123"));
    }
}
