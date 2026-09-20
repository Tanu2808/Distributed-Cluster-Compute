package com.cluster.worker.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.service.cluster.ClusterSetupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClusterController.class)
@SuppressWarnings("null")
public class ClusterControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ClusterSetupService setupService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testConnectToCoordinator_Unavailable() throws Exception {
        when(setupService.connectToCoordinator(any()))
                .thenReturn(
                        new ClusterEnrollment(
                                null,
                                ClusterEnrollment.Status.FAILED,
                                "Coordinator enrollment unavailable"));

        mockMvc.perform(
                        post("/api/cluster/connect")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of("coordinatorUrl", "http://localhost:8080?token=ABCD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Coordinator enrollment unavailable"));
    }
}
