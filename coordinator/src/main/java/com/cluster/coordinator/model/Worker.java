package com.cluster.coordinator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workers")
public class Worker {

    @Id
    private String id;

    private String name;

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
    public WorkerState getState() { return state; }
    public void setState(WorkerState state) { this.state = state; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
