package com.cluster.worker.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerConfigurationStoreTest {

    @Mock
    private LocalStateStore stateStore;

    private WorkerConfigurationStore configStore;

    @BeforeEach
    void setUp() {
        configStore = new WorkerConfigurationStore(stateStore);
    }

    @Test
    void testInitMissingFileCreatesNewConfig() {
        when(stateStore.readState()).thenReturn(null);

        configStore.init();

        assertNotNull(configStore.getConfig());
        assertNotNull(configStore.getWorkerId());
        assertEquals(1, configStore.getConfig().getVersion());
        verify(stateStore, times(1)).writeState(anyString());
    }

    @Test
    void testInitMalformedFileRecoversAndCreatesNew() {
        when(stateStore.readState()).thenReturn("not-json");

        configStore.init();

        assertNotNull(configStore.getConfig());
        assertNotNull(configStore.getWorkerId());
        verify(stateStore, times(1)).writeState(anyString());
    }

    @Test
    void testInitValidFileLoadsConfig() {
        String validJson = "{\"version\":1,\"workerId\":\"test-id\",\"clusterId\":\"c-id\",\"coordinatorUrl\":\"http://local\"}";
        when(stateStore.readState()).thenReturn(validJson);

        configStore.init();

        WorkerConfiguration config = configStore.getConfig();
        assertNotNull(config);
        assertEquals("test-id", config.getWorkerId());
        assertEquals("c-id", config.getClusterId());
        assertEquals("http://local", config.getCoordinatorUrl());
        assertTrue(configStore.isConfigured());
        verify(stateStore, never()).writeState(anyString()); // doesn't save on load if valid
    }

    @Test
    void testResetConfigurationKeepsWorkerId() {
        String validJson = "{\"version\":1,\"workerId\":\"test-id\",\"clusterId\":\"c-id\",\"coordinatorUrl\":\"http://local\"}";
        when(stateStore.readState()).thenReturn(validJson);

        configStore.init();
        assertTrue(configStore.isConfigured());

        configStore.resetConfiguration();
        
        assertFalse(configStore.isConfigured());
        assertEquals("test-id", configStore.getWorkerId());
        assertNull(configStore.getConfig().getClusterId());
        verify(stateStore, times(1)).writeState(anyString());
    }
}
