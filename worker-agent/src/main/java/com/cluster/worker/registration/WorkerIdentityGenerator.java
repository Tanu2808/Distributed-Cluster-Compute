package com.cluster.worker.registration;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class WorkerIdentityGenerator {

    private static final String ID_FILE = ".worker-id";
    private String currentWorkerId;

    public synchronized String getOrCreateWorkerId() {
        if (currentWorkerId != null) {
            return currentWorkerId;
        }

        Path idPath = Paths.get(ID_FILE);
        if (Files.exists(idPath)) {
            try {
                currentWorkerId = Files.readString(idPath).trim();
                return currentWorkerId;
            } catch (IOException e) {
                // Ignore and generate new
            }
        }

        currentWorkerId = UUID.randomUUID().toString();
        try {
            Files.writeString(idPath, currentWorkerId);
        } catch (IOException e) {
            // Can't persist, but we have one in memory
        }
        return currentWorkerId;
    }
}
