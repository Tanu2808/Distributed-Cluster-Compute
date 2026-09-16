package com.cluster.worker.service.cluster;


public interface CoordinatorProvisioningService {
    
    /**
     * Provisions a local coordinator instance if supported, or prepares the local environment.
     * @param clusterName Name of the cluster.
     * @return true if successful, false otherwise.
     */
    boolean provisionLocalCoordinator(String clusterName);
}
