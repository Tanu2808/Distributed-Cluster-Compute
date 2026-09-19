package com.cluster.worker.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.monitoring.CpuMetricsProvider;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.persistence.WorkerConfiguration;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.service.TaskService;
import com.cluster.worker.service.WorkerLifecycleService;
import com.cluster.worker.service.WorkerStateManager;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
public class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private WorkerLifecycleService lifecycleService;

    @MockBean private WorkerConfigurationStore configStore;

    @MockBean private SystemMetricsProvider metricsProvider;

    @MockBean private TaskService taskService;

    @Test
    public void testGetHomeDashboard() throws Exception {
        WorkerStateManager stateManager = mock(WorkerStateManager.class);
        when(stateManager.getLifecycleState()).thenReturn(WorkerLifecycleState.CONFIGURED);
        when(stateManager.getConnectionState()).thenReturn(ConnectionState.ONLINE);
        when(lifecycleService.getStateManager()).thenReturn(stateManager);

        when(configStore.isConfigured()).thenReturn(true);
        WorkerConfiguration config = new WorkerConfiguration();
        config.setClusterName("test-cluster");
        when(configStore.getConfig()).thenReturn(config);

        SystemMetrics metrics = new SystemMetrics();
        metrics.setCpuUsagePercent(50.5);
        metrics.setCpuCores(8);
        when(metricsProvider.collectMetrics()).thenReturn(metrics);

        when(taskService.getActiveTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/worker/dashboard/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerStatus").value("CONFIGURED"))
                .andExpect(jsonPath("$.coordinatorConnection").value("ONLINE"))
                .andExpect(jsonPath("$.clusterName").value("test-cluster"))
                .andExpect(jsonPath("$.cpuUsagePercent").value(50.5))
                .andExpect(jsonPath("$.cpuCores").value(8))
                .andExpect(jsonPath("$.activeTasks").value(0));
    }

    @Test
    public void testGetNodeDashboard() throws Exception {
        SystemMetrics metrics = new SystemMetrics();
        metrics.setCpuUsagePercent(12.3);
        metrics.setUsedMemoryMb(1024L);
        when(metricsProvider.collectMetrics()).thenReturn(metrics);

        CpuMetricsProvider cpuMetricsProvider = mock(CpuMetricsProvider.class);
        when(cpuMetricsProvider.getProcessorIdentifier()).thenReturn("Test CPU");
        when(metricsProvider.getCpuMetricsProvider()).thenReturn(cpuMetricsProvider);

        mockMvc.perform(get("/api/worker/dashboard/node"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpuModel").value("Test CPU"))
                .andExpect(jsonPath("$.cpuUsagePercent").value(12.3))
                .andExpect(jsonPath("$.memoryUsedMb").value(1024));
    }
}
