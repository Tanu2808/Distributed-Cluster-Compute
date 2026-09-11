package com.cluster.coordinator.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;

@Entity
@Table(name = "task_results")
public class TaskResult {

    @Id
    private String id;
    
    private String taskId;
    
    @Lob
    private String output;
    
    private int exitCode;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }
}
