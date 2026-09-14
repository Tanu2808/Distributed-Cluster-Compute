package com.cluster.worker.persistence;

public class WorkerConfiguration {
    private int version;
    private String workerId;
    private String clusterId;
    private String clusterName;
    private String coordinatorUrl;
    private String enrollmentCredential;

    public WorkerConfiguration() {
        this.version = 1;
    }

    public WorkerConfiguration(String workerId) {
        this.version = 1;
        this.workerId = workerId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getCoordinatorUrl() {
        return coordinatorUrl;
    }

    public void setCoordinatorUrl(String coordinatorUrl) {
        this.coordinatorUrl = coordinatorUrl;
    }

    public String getEnrollmentCredential() {
        return enrollmentCredential;
    }

    public void setEnrollmentCredential(String enrollmentCredential) {
        this.enrollmentCredential = enrollmentCredential;
    }
}
