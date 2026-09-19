package com.cluster.worker.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.ExecutionState;
import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import com.cluster.worker.service.WorkerLifecycleService;
import com.cluster.worker.service.WorkerStateManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkerController.class)
public class WorkerControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private WorkerLifecycleService lifecycleService;

    @MockBean private WorkerIdentityGenerator identityGenerator;

    @MockBean private WorkerStateManager stateManager;

    @Test
    public void testGetWorkerStatus() throws Exception {
        when(lifecycleService.getStateManager()).thenReturn(stateManager);
        when(stateManager.getLifecycleState()).thenReturn(WorkerLifecycleState.CONFIGURED);
        when(stateManager.getConnectionState()).thenReturn(ConnectionState.ONLINE);
        when(stateManager.getExecutionState()).thenReturn(ExecutionState.IDLE);

        mockMvc.perform(get("/api/worker/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleState").value("CONFIGURED"))
                .andExpect(jsonPath("$.connectionState").value("ONLINE"))
                .andExpect(jsonPath("$.executionState").value("IDLE"));
    }

    @Test
    public void testGetWorkerInfo() throws Exception {
        when(identityGenerator.getOrCreateWorkerId()).thenReturn("test-id-123");

        mockMvc.perform(get("/api/worker/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value("test-id-123"));
    }
}
