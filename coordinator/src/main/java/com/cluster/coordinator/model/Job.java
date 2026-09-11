package com.cluster.coordinator.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    private String id;
    
    private String name;
    
    private int requiredCpu;
    
    private long requiredMemory;
    
    private String status;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRequiredCpu() { return requiredCpu; }
    public void setRequiredCpu(int requiredCpu) { this.requiredCpu = requiredCpu; }
    public long getRequiredMemory() { return requiredMemory; }
    public void setRequiredMemory(long requiredMemory) { this.requiredMemory = requiredMemory; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
