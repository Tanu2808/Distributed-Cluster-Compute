package com.cluster.worker.api;

import com.cluster.worker.model.cluster.ClusterConfiguration;
import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.service.cluster.ClusterSetupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClusterController.class)
@SuppressWarnings("null")
public class ClusterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClusterSetupService setupService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testJoinCluster_Unavailable() throws Exception {
        when(setupService.joinCluster(any())).thenReturn(
            new ClusterEnrollment(null, ClusterEnrollment.Status.FAILED, "Coordinator enrollment unavailable")
        );

        mockMvc.perform(post("/api/cluster/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("joinCode", "ABCD-1234-EFGH-5678"))))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Coordinator enrollment unavailable"));
    }

    @Test
    public void testJoinCluster_InvalidCode() throws Exception {
        mockMvc.perform(post("/api/cluster/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("joinCode", "invalid"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    public void testCreateCluster_Local() throws Exception {
        when(setupService.createCluster("my-cluster", true)).thenReturn(
            new ClusterConfiguration("my-cluster", null)
        );

        mockMvc.perform(post("/api/cluster/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("clusterName", "my-cluster", "isLocal", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clusterName").value("my-cluster"));
    }
}
