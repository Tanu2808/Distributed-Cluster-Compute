package com.cluster.worker.communication;

import com.cluster.worker.config.WorkerConfig;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class CoordinatorClient {

    private final RestTemplate restTemplate;
    private final String coordinatorUrl;

    public CoordinatorClient(WorkerConfig config, RestTemplateBuilder builder) {
        this.coordinatorUrl = config.getCoordinator().getUrl();
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(config.getConnection().getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(config.getConnection().getTimeoutMs()))
                .additionalInterceptors((request, body, execution) -> {
                    if (config.getCoordinator().getApiKey() != null) {
                        request.getHeaders().add("Authorization", "Bearer " + config.getCoordinator().getApiKey());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    public boolean register(Object registrationRequest) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    coordinatorUrl + "/api/workers/register",
                    registrationRequest,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }

    public boolean sendHeartbeat(String workerId, Object heartbeatRequest) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    coordinatorUrl + "/api/workers/" + workerId + "/heartbeat",
                    heartbeatRequest,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }

    public boolean deregister(String workerId) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    coordinatorUrl + "/api/workers/" + workerId + "/deregister",
                    null,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }
}
