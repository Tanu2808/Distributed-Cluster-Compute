package com.cluster.coordinator.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_assignments")
public class TaskAssignment {

    @Id
    private String id;
    
    private String taskId;
    
    private String workerId;
    
    private String status;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
