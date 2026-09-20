package com.cluster.worker.service.cluster;

import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.service.WorkerStateManager;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ClusterSetupService {

    private final WorkerConfigurationStore configStore;
    private final WorkerStateManager stateManager;
    private final com.cluster.worker.service.WorkerLifecycleService lifecycleService;

    public ClusterSetupService(
            WorkerConfigurationStore configStore,
            WorkerStateManager stateManager,
            @org.springframework.context.annotation.Lazy
                    com.cluster.worker.service.WorkerLifecycleService lifecycleService) {
        this.configStore = configStore;
        this.stateManager = stateManager;
        this.lifecycleService = lifecycleService;
    }

    /** Attempts to join an existing cluster using a coordinator URL which may contain a token. */
    public ClusterEnrollment connectToCoordinator(String url) {
        if (url == null || url.isBlank()) {
            return new ClusterEnrollment(
                    configStore.getWorkerId(),
                    ClusterEnrollment.Status.FAILED,
                    "Coordinator URL is required.");
        }

        try {
            URI uri = new URI(url);
            String baseUrl = uri.getScheme() + "://" + uri.getAuthority();
            String query = uri.getQuery();
            String token = null;

            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && pair[0].equalsIgnoreCase("token")) {
                        token = pair[1];
                        break;
                    }
                }
            }

            RestTemplate restTemplate = new RestTemplate();
            String enrollUrl = baseUrl + "/api/cluster/enroll";
            
            // If token is null, we can try without one, or use a dummy.
            // The existing coordinator expects a joinCode to not be null.
            String joinCodeToUse = token != null ? token : "";

            Map<String, String> requestBody =
                    Map.of(
                            "joinCode", joinCodeToUse,
                            "workerId", configStore.getWorkerId());
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(enrollUrl, requestBody, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String clusterId = (String) response.getBody().get("clusterId");
                String returnedCoordinatorUrl = (String) response.getBody().get("coordinatorUrl");
                String runtimeCredential = (String) response.getBody().get("runtimeCredential");

                // Persist Configuration
                var storedConfig = configStore.getConfig();
                storedConfig.setClusterName(clusterId);
                storedConfig.setClusterId(clusterId);
                storedConfig.setCoordinatorUrl(
                        returnedCoordinatorUrl != null
                                ? returnedCoordinatorUrl
                                : baseUrl);

                if (runtimeCredential != null) {
                    storedConfig.setEnrollmentCredential(runtimeCredential);
                } else {
                    storedConfig.setEnrollmentCredential(joinCodeToUse); // Fallback
                }
                configStore.save();

                // Transition state to kick off the connection flow
                stateManager.transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
                stateManager.transitionLifecycle(WorkerLifecycleState.CONFIGURED);

                // Invoke existing connection startup mechanism
                lifecycleService.initiateConnection();

                return new ClusterEnrollment(
                        configStore.getWorkerId(),
                        ClusterEnrollment.Status.SUCCESS,
                        "Successfully initiated connection to cluster");
            } else {
                return new ClusterEnrollment(
                        configStore.getWorkerId(),
                        ClusterEnrollment.Status.FAILED,
                        "Unauthorized or invalid token");
            }
        } catch (Exception e) {
            return new ClusterEnrollment(
                    configStore.getWorkerId(),
                    ClusterEnrollment.Status.FAILED,
                    "Coordinator connection failed: " + e.getMessage());
        }
    }
}
