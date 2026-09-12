package com.cluster.worker;

import com.cluster.worker.communication.WebSocketConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = WorkerApplication.class)
class WorkerApplicationTests {

    @MockBean
    private WebSocketConnectionManager webSocketConnectionManager;

    @Test
    void contextLoads() {
        // Just testing if application context loads successfully
    }
}
