package com.cluster.worker;

import com.cluster.worker.communication.CoordinatorClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class WorkerApplicationTests {

    @MockBean
    private CoordinatorClient coordinatorClient;

    @Test
    void contextLoads() {
        // Just testing if application context loads successfully
    }
}
