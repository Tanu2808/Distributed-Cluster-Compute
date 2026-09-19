package com.cluster.worker.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cluster.worker.persistence.WorkerConfiguration;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.service.WorkerStateManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SettingsController.class)
public class SettingsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private WorkerConfigurationStore configStore;

    @MockBean private WorkerStateManager stateManager;

    @Test
    public void testGetWorkerSettingsExcludesSecrets() throws Exception {
        WorkerConfiguration config = new WorkerConfiguration("test-worker-id");
        config.setVersion(1);
        config.setEnrollmentCredential("secret-token-do-not-leak");

        when(configStore.getConfig()).thenReturn(config);

        mockMvc.perform(get("/api/settings/worker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value("test-worker-id"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.enrollmentCredential").doesNotExist());
    }

    @Test
    public void testGetClusterSettingsExcludesSecrets() throws Exception {
        WorkerConfiguration config = new WorkerConfiguration("test-worker-id");
        config.setClusterId("test-cluster-id");
        config.setClusterName("My Cluster");
        config.setCoordinatorUrl("http://localhost:8080");
        config.setEnrollmentCredential("secret-token-do-not-leak");

        when(configStore.getConfig()).thenReturn(config);
        when(configStore.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/settings/cluster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clusterId").value("test-cluster-id"))
                .andExpect(jsonPath("$.clusterName").value("My Cluster"))
                .andExpect(jsonPath("$.coordinatorUrl").value("http://localhost:8080"))
                .andExpect(jsonPath("$.isConfigured").value(true))
                .andExpect(jsonPath("$.enrollmentCredential").doesNotExist());
    }

    @Test
    public void testResetConfiguration() throws Exception {
        mockMvc.perform(post("/api/settings/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Configuration reset successfully"));
    }
}
