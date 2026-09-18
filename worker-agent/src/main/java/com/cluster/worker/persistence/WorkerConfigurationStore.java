package com.cluster.worker.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class WorkerConfigurationStore {

    private static final Logger log = LoggerFactory.getLogger(WorkerConfigurationStore.class);
    private static final String LEGACY_ID_FILE = ".worker-id";

    private final LocalStateStore stateStore;
    private final ObjectMapper objectMapper;
    
    private WorkerConfiguration currentConfig;

    public WorkerConfigurationStore(LocalStateStore stateStore) {
        this.stateStore = stateStore;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        String json = stateStore.readState();
        if (json == null || json.isBlank()) {
            log.info("No worker configuration found. Initializing new configuration.");
            currentConfig = createNewConfiguration();
            save();
            return;
        }

        try {
            currentConfig = objectMapper.readValue(json, WorkerConfiguration.class);
        } catch (JsonProcessingException e) {
            log.warn("Worker configuration is malformed or corrupted. Re-initializing.", e);
            // We should ideally try to keep the old workerId if possible, but if it's completely malformed, generate new.
            // A more robust migration could attempt regex extraction, but for now we create new.
            currentConfig = createNewConfiguration();
            save();
        }
    }

    private WorkerConfiguration createNewConfiguration() {
        String workerId = null;
        
        // Attempt to migrate legacy ID if it exists
        Path legacyIdPath = Paths.get(LEGACY_ID_FILE);
        if (Files.exists(legacyIdPath)) {
            try {
                workerId = Files.readString(legacyIdPath).trim();
                log.info("Migrated legacy worker ID: {}", workerId);
            } catch (IOException e) {
                log.warn("Failed to read legacy worker ID", e);
            }
        }

        if (workerId == null || workerId.isEmpty()) {
            workerId = UUID.randomUUID().toString();
            log.info("Generated new worker ID: {}", workerId);
        }

        return new WorkerConfiguration(workerId);
    }

    public synchronized void save() {
        try {
            String json = objectMapper.writeValueAsString(currentConfig);
            stateStore.writeState(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize worker configuration", e);
            throw new RuntimeException("Serialization error", e);
        }
    }

    public synchronized WorkerConfiguration getConfig() {
        return currentConfig;
    }

    public synchronized String getWorkerId() {
        return currentConfig.getWorkerId();
    }

    public synchronized boolean isConfigured() {
        return currentConfig.getCoordinatorUrl() != null && !currentConfig.getCoordinatorUrl().isBlank() 
               && currentConfig.getClusterId() != null && !currentConfig.getClusterId().isBlank();
    }

    public synchronized void resetConfiguration() {
        String existingWorkerId = currentConfig.getWorkerId();
        currentConfig = new WorkerConfiguration(existingWorkerId);
        save();
        log.info("Worker configuration has been reset.");
    }
}
