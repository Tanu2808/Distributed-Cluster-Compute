package com.cluster.worker.model.cluster;

public class ClusterEnrollment {
    public enum Status {
        PENDING,
        SUCCESS,
        FAILED
    }

    private String workerId;
    private Status status;
    private String message;

    public ClusterEnrollment() {}

    public ClusterEnrollment(String workerId, Status status, String message) {
        this.workerId = workerId;
        this.status = status;
        this.message = message;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
