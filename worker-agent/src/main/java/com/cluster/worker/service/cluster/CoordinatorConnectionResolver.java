package com.cluster.worker.service.cluster;

import com.cluster.worker.model.cluster.ClusterConnectionInfo;
import com.cluster.worker.model.cluster.JoinCode;

public interface CoordinatorConnectionResolver {
    
    /**
     * Resolves the connection info for a local coordinator.
     */
    ClusterConnectionInfo resolveLocalCoordinator(String clusterName);

    /**
     * Resolves the connection info given an existing coordinator endpoint and a join code.
     */
    ClusterConnectionInfo resolveExistingCoordinator(String endpointUrl, JoinCode joinCode);
}
