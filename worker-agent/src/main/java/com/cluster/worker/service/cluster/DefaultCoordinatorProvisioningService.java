package com.cluster.worker.service.cluster;

import org.springframework.stereotype.Service;

@Service
public class DefaultCoordinatorProvisioningService implements CoordinatorProvisioningService {
    @Override
    public boolean provisionLocalCoordinator(String clusterName) {
        // Just a dummy placeholder for now
        System.out.println("Provisioning local coordinator for cluster: " + clusterName);
        return true;
    }
}
