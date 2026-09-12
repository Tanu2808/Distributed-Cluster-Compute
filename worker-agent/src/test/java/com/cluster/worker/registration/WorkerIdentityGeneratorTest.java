package com.cluster.worker.registration;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class WorkerIdentityGeneratorTest {

    @Test
    void testGetOrCreateWorkerId() {
        WorkerIdentityGenerator generator = new WorkerIdentityGenerator();
        
        // Clean up any existing file for this test
        File idFile = new File(".worker-id");
        if (idFile.exists()) {
            idFile.delete();
        }

        String id1 = generator.getOrCreateWorkerId();
        assertNotNull(id1);
        assertFalse(id1.isEmpty());

        // Should return the same ID
        String id2 = generator.getOrCreateWorkerId();
        assertEquals(id1, id2);

        // Clean up
        if (idFile.exists()) {
            idFile.delete();
        }
    }
}
