package com.cluster.worker.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileLocalStateStore implements LocalStateStore {

    private static final Logger log = LoggerFactory.getLogger(FileLocalStateStore.class);
    private static final String CONFIG_FILE = ".worker-config.json";

    private final Path configPath;

    public FileLocalStateStore() {
        this.configPath = Paths.get(CONFIG_FILE);
    }

    @Override
    public String readState() {
        if (!Files.exists(configPath)) {
            return null;
        }
        try {
            return Files.readString(configPath);
        } catch (IOException e) {
            log.error("Failed to read worker configuration file", e);
            throw new RuntimeException("Could not read configuration", e);
        }
    }

    @Override
    public void writeState(String json) {
        try {
            Files.writeString(configPath, json);
        } catch (IOException e) {
            log.error("Failed to write worker configuration file", e);
            throw new RuntimeException("Could not write configuration", e);
        }
    }

    @Override
    public void deleteState() {
        try {
            Files.deleteIfExists(configPath);
        } catch (IOException e) {
            log.error("Failed to delete worker configuration file", e);
            throw new RuntimeException("Could not delete configuration", e);
        }
    }
}
