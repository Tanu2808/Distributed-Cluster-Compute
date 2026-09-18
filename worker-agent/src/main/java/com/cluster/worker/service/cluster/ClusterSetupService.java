package com.cluster.worker.service.cluster;

import com.cluster.worker.model.cluster.ClusterConfiguration;
import com.cluster.worker.model.cluster.ClusterConnectionInfo;
import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.model.cluster.JoinCode;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.cluster.worker.model.WorkerLifecycleState;
import com.cluster.worker.service.WorkerStateManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Service
public class ClusterSetupService {

    private final CoordinatorProvisioningService provisioningService;
    private final CoordinatorConnectionResolver connectionResolver;
    private final WorkerConfigurationStore configStore;
    private final WorkerStateManager stateManager;
    private final com.cluster.worker.service.WorkerLifecycleService lifecycleService;

    public ClusterSetupService(CoordinatorProvisioningService provisioningService,
                               CoordinatorConnectionResolver connectionResolver,
                               WorkerConfigurationStore configStore,
                               WorkerStateManager stateManager,
                               @org.springframework.context.annotation.Lazy com.cluster.worker.service.WorkerLifecycleService lifecycleService) {
        this.provisioningService = provisioningService;
        this.connectionResolver = connectionResolver;
        this.configStore = configStore;
        this.stateManager = stateManager;
        this.lifecycleService = lifecycleService;
    }

    @Value("${worker.coordinator.url:http://localhost:8080}")
    private String defaultCoordinatorUrl;

    /**
     * Attempts to join an existing cluster using a join code.
     */
    public ClusterEnrollment joinCluster(JoinCode joinCode) {
        RestTemplate restTemplate = new RestTemplate();
        String enrollUrl = defaultCoordinatorUrl + "/api/cluster/enroll";

        try {
            Map<String, String> requestBody = Map.of(
                    "joinCode", joinCode.getCode(),
                    "workerId", configStore.getWorkerId()
            );
            ResponseEntity<Map> response = restTemplate.postForEntity(enrollUrl, requestBody, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String clusterId = (String) response.getBody().get("clusterId");
                String returnedCoordinatorUrl = (String) response.getBody().get("coordinatorUrl");
                String runtimeCredential = (String) response.getBody().get("runtimeCredential");

                // Persist Configuration
                var storedConfig = configStore.getConfig();
                storedConfig.setClusterName(clusterId);
                storedConfig.setClusterId(clusterId);
                storedConfig.setCoordinatorUrl(returnedCoordinatorUrl != null ? returnedCoordinatorUrl : defaultCoordinatorUrl);
                
                if (runtimeCredential != null) {
                    storedConfig.setEnrollmentCredential(runtimeCredential);
                } else {
                    storedConfig.setEnrollmentCredential(joinCode.getCode()); // Fallback
                }
                configStore.save();
                
                // Transition state to kick off the connection flow
                stateManager.transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
                stateManager.transitionLifecycle(WorkerLifecycleState.CONFIGURED);
                
                // Invoke existing connection startup mechanism
                lifecycleService.initiateConnection();
                
                return new ClusterEnrollment(configStore.getWorkerId(), ClusterEnrollment.Status.SUCCESS, "Successfully joined cluster");
            } else {
                return new ClusterEnrollment(configStore.getWorkerId(), ClusterEnrollment.Status.FAILED, "Invalid join code or unauthorized");
            }
        } catch (Exception e) {
            return new ClusterEnrollment(configStore.getWorkerId(), ClusterEnrollment.Status.FAILED, "Coordinator connection failed: " + e.getMessage());
        }
    }

    /**
     * Attempts to create a new cluster.
     */
    public ClusterConfiguration createCluster(String clusterName, boolean isLocal) {
        if (clusterName == null || clusterName.isBlank()) {
            throw new IllegalArgumentException("Cluster name is required");
        }
        
        ClusterConnectionInfo connectionInfo;
        
        if (isLocal) {
            boolean provisioned = provisioningService.provisionLocalCoordinator(clusterName);
            if (!provisioned) {
                throw new RuntimeException("Failed to provision local coordinator");
            }
            connectionInfo = connectionResolver.resolveLocalCoordinator(clusterName);
        } else {
            // For existing server, we'd typically take an endpoint. For now, it's a placeholder.
            throw new UnsupportedOperationException("Creating a cluster on an existing remote coordinator is not fully supported yet.");
        }
        
        // Persist Configuration
        var storedConfig = configStore.getConfig();
        storedConfig.setClusterName(clusterName);
        storedConfig.setClusterId("local-cluster-id"); // In a real scenario, this comes back from the coordinator
        storedConfig.setCoordinatorUrl(connectionInfo.getUrl());
        if (connectionInfo.getJoinCode() != null) {
            storedConfig.setEnrollmentCredential(connectionInfo.getJoinCode().getCode());
        }
        configStore.save();
        
        // Transition state to kick off the connection flow
        stateManager.transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
        stateManager.transitionLifecycle(WorkerLifecycleState.CONFIGURED);
        
        // Invoke existing connection startup mechanism
        lifecycleService.initiateConnection();
        
        return new ClusterConfiguration(clusterName, connectionInfo);
    }
}
