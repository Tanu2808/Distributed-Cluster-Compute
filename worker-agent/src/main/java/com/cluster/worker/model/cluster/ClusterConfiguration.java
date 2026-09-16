package com.cluster.worker.model.cluster;

public class ClusterConfiguration {
    private String clusterName;
    private ClusterConnectionInfo connectionInfo;

    public ClusterConfiguration() {
    }

    public ClusterConfiguration(String clusterName, ClusterConnectionInfo connectionInfo) {
        this.clusterName = clusterName;
        this.connectionInfo = connectionInfo;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public ClusterConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public void setConnectionInfo(ClusterConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }
}
