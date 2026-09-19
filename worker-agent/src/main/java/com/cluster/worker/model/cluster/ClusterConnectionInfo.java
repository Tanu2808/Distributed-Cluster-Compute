package com.cluster.worker.model.cluster;

public class ClusterConnectionInfo {
    private String url;
    private JoinCode joinCode;

    public ClusterConnectionInfo() {}

    public ClusterConnectionInfo(String url, JoinCode joinCode) {
        this.url = url;
        this.joinCode = joinCode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JoinCode getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(JoinCode joinCode) {
        this.joinCode = joinCode;
    }
}
