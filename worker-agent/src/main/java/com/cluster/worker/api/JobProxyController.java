package com.cluster.worker.api;

import com.cluster.worker.persistence.WorkerConfiguration;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/jobs")
public class JobProxyController {

    private final WorkerConfigurationStore configStore;
    private final RestTemplate restTemplate;

    public JobProxyController(WorkerConfigurationStore configStore) {
        this.configStore = configStore;
        this.restTemplate = new RestTemplate();
    }

    private String getCoordinatorUrl() {
        WorkerConfiguration config = configStore.getConfig();
        if (config == null || config.getCoordinatorUrl() == null) {
            throw new IllegalStateException("Worker is not configured with a coordinator URL");
        }
        return config.getCoordinatorUrl().replaceFirst("/stomp$", "");
    }

    private HttpHeaders getHeaders() {
        WorkerConfiguration config = configStore.getConfig();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("worker", config.getEnrollmentCredential());
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @PostMapping
    public ResponseEntity<String> createJob(@RequestBody String body) {
        String url = getCoordinatorUrl() + "/api/jobs";
        HttpEntity<String> entity = new HttpEntity<>(body, getHeaders());
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    @GetMapping
    public ResponseEntity<String> getAllJobs() {
        String url = getCoordinatorUrl() + "/api/jobs";
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<String> getJob(@PathVariable String jobId) {
        String url = getCoordinatorUrl() + "/api/jobs/" + jobId;
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    @GetMapping("/{jobId}/tasks")
    public ResponseEntity<String> getJobTasks(@PathVariable String jobId) {
        String url = getCoordinatorUrl() + "/api/jobs/" + jobId + "/tasks";
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }
}
