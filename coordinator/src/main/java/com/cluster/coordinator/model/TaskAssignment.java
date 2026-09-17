package com.cluster.coordinator.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "task_assignments")
public class TaskAssignment {

    @Id
    private String id;
    
    private String taskId;
    
    private String workerId;
    
    @Enumerated(EnumType.STRING)
    private AssignmentState state;
    
    private int allocatedCpu;
    
    private long allocatedMemory;
    
    // Deprecated: preserved for backward compatibility
    private String status;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public AssignmentState getState() { return state; }
    public void setState(AssignmentState state) { this.state = state; }
    public int getAllocatedCpu() { return allocatedCpu; }
    public void setAllocatedCpu(int allocatedCpu) { this.allocatedCpu = allocatedCpu; }
    public long getAllocatedMemory() { return allocatedMemory; }
    public void setAllocatedMemory(long allocatedMemory) { this.allocatedMemory = allocatedMemory; }
    
    @Deprecated
    public String getStatus() { return status; }
    @Deprecated
    public void setStatus(String status) { this.status = status; }
}
