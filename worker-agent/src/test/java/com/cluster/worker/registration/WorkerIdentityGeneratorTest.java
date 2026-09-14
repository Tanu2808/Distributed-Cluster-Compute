package com.cluster.worker.registration;

import com.cluster.worker.persistence.WorkerConfigurationStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkerIdentityGeneratorTest {

    @Test
    void testGetOrCreateWorkerId() {
        WorkerConfigurationStore mockStore = mock(WorkerConfigurationStore.class);
        when(mockStore.getWorkerId()).thenReturn("mocked-worker-id");

        WorkerIdentityGenerator generator = new WorkerIdentityGenerator(mockStore);
        
        String id1 = generator.getOrCreateWorkerId();
        String id2 = generator.getOrCreateWorkerId();
        
        assertEquals("mocked-worker-id", id1);
        assertEquals(id1, id2);
    }
}
