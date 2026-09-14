package com.cluster.worker.api;

import com.cluster.worker.communication.WebSocketConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectionController.class)
public class ConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebSocketConnectionManager connectionManager;

    @Test
    public void testGetConnectionStatus() throws Exception {
        when(connectionManager.isConnected()).thenReturn(true);

        mockMvc.perform(get("/api/connection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }
}
