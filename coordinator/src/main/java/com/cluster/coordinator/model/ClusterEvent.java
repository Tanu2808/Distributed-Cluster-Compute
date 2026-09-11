package com.cluster.coordinator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cluster_events")
public class ClusterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;

    private String message;

    private String workerId;

    private LocalDateTime timestamp;

    public ClusterEvent() {}

    public ClusterEvent(String eventType, String message, String workerId) {
        this.eventType = eventType;
        this.message = message;
        this.workerId = workerId;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
