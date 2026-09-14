package com.cluster.worker.api;

import com.cluster.worker.config.WorkerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
public class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkerConfig config;

    @Test
    public void testGetSettings() throws Exception {
        WorkerConfig.Coordinator coordConfig = new WorkerConfig.Coordinator();
        coordConfig.setUrl("http://localhost:8080");

        WorkerConfig.Heartbeat hbConfig = new WorkerConfig.Heartbeat();
        hbConfig.setIntervalMs(10000);

        WorkerConfig.Metrics metricConfig = new WorkerConfig.Metrics();
        metricConfig.setIntervalMs(15000);

        when(config.getName()).thenReturn("test-worker");
        when(config.getCoordinator()).thenReturn(coordConfig);
        when(config.getHeartbeat()).thenReturn(hbConfig);
        when(config.getMetrics()).thenReturn(metricConfig);

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerName").value("test-worker"))
                .andExpect(jsonPath("$.coordinatorUrl").value("http://localhost:8080"))
                .andExpect(jsonPath("$.heartbeatIntervalMs").value(10000))
                .andExpect(jsonPath("$.metricsIntervalMs").value(15000));
    }
}
