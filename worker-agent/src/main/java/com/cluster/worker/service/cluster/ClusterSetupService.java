package com.cluster.worker.service.cluster;

import com.cluster.worker.model.cluster.ClusterConfiguration;
import com.cluster.worker.model.cluster.ClusterConnectionInfo;
import com.cluster.worker.model.cluster.ClusterEnrollment;
import com.cluster.worker.model.cluster.JoinCode;
import org.springframework.stereotype.Service;

@Service
public class ClusterSetupService {

    private final CoordinatorProvisioningService provisioningService;
    private final CoordinatorConnectionResolver connectionResolver;

    public ClusterSetupService(CoordinatorProvisioningService provisioningService,
                               CoordinatorConnectionResolver connectionResolver) {
        this.provisioningService = provisioningService;
        this.connectionResolver = connectionResolver;
    }

    /**
     * Attempts to join an existing cluster using a join code.
     * Note: "Coordinator enrollment unavailable" is returned because the coordinator API doesn't exist yet.
     */
    public ClusterEnrollment joinCluster(JoinCode joinCode) {
        // Implementation for phase 3: we do not fake success.
        // We simulate a failure since the API isn't there yet.
        return new ClusterEnrollment(null, ClusterEnrollment.Status.FAILED, "Coordinator enrollment unavailable");
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
        
        return new ClusterConfiguration(clusterName, connectionInfo);
    }
}
