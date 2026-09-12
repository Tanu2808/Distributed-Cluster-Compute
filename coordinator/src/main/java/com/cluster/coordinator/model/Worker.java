package com.cluster.coordinator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workers")
public class Worker {

    @Id
    private String id;

    private String name;

    private String hostname;

    private String ipAddress;

    private String operatingSystem;

    private String architecture;

    private String agentVersion;

    @Enumerated(EnumType.STRING)
    private WorkerState state;

    private LocalDateTime lastHeartbeat;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    public Worker() {}

    public Worker(String id, String name, WorkerState state) {
        this.id = id;
        this.name = name;
        this.state = state;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }
    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String agentVersion) { this.agentVersion = agentVersion; }
    public WorkerState getState() { return state; }
    public void setState(WorkerState state) { this.state = state; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
