package com.cluster.worker.communication;

import com.cluster.worker.config.WorkerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(
    components = CoordinatorClient.class,
    properties = {
        "worker.coordinator.url=http://localhost:8080",
        "worker.coordinator.api-key=test-key",
        "worker.connection.timeout-ms=3000"
    }
)
@org.springframework.context.annotation.Import(WorkerConfig.class)
class CoordinatorClientTest {

    @Autowired
    private CoordinatorClient coordinatorClient;

    @Autowired
    private MockRestServiceServer server;



    @BeforeEach
    void setUp() {
        server.reset();
    }

    @Test
    void testRegisterWithApiKey() {
        server.expect(requestTo("http://localhost:8080/api/workers/register"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

        boolean result = coordinatorClient.register(new RegistrationPayload());

        assertTrue(result);
        server.verify();
    }
}
