package com.cluster.coordinator.service;

import com.cluster.coordinator.model.ClusterEvent;
import com.cluster.coordinator.repository.ClusterEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final ClusterEventRepository eventRepository;

    public EventService(ClusterEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void recordEvent(String eventType, String message, String workerId) {
        ClusterEvent event = new ClusterEvent(eventType, message, workerId);
        eventRepository.save(event);
        log.info("[EVENT: {}] Worker: {} - {}", eventType, workerId != null ? workerId : "SYSTEM", message);
    }

    public List<ClusterEvent> getRecentEvents() {
        return eventRepository.findTop100ByOrderByTimestampDesc();
    }
}
