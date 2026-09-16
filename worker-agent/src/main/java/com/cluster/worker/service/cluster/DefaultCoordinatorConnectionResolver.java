package com.cluster.worker.service.cluster;

import com.cluster.worker.model.cluster.ClusterConnectionInfo;
import com.cluster.worker.model.cluster.JoinCode;
import org.springframework.stereotype.Service;

@Service
public class DefaultCoordinatorConnectionResolver implements CoordinatorConnectionResolver {
    @Override
    public ClusterConnectionInfo resolveLocalCoordinator(String clusterName) {
        return new ClusterConnectionInfo("http://localhost:8080", null);
    }

    @Override
    public ClusterConnectionInfo resolveExistingCoordinator(String endpointUrl, JoinCode joinCode) {
        return new ClusterConnectionInfo(endpointUrl, joinCode);
    }
}
