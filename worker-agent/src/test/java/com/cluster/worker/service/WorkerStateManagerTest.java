package com.cluster.worker.service;

import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.ExecutionState;
import com.cluster.worker.model.WorkerLifecycleState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkerStateManagerTest {

    private WorkerStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new WorkerStateManager();
    }

    @Test
    void testValidLifecycleTransitions() {
        assertEquals(WorkerLifecycleState.STARTING, stateManager.getLifecycleState());
        
        stateManager.transitionLifecycle(WorkerLifecycleState.INITIALIZING);
        assertEquals(WorkerLifecycleState.INITIALIZING, stateManager.getLifecycleState());

        stateManager.transitionLifecycle(WorkerLifecycleState.SETUP_REQUIRED);
        assertEquals(WorkerLifecycleState.SETUP_REQUIRED, stateManager.getLifecycleState());

        stateManager.transitionLifecycle(WorkerLifecycleState.LOADING_CONFIGURATION);
        assertEquals(WorkerLifecycleState.LOADING_CONFIGURATION, stateManager.getLifecycleState());

        stateManager.transitionLifecycle(WorkerLifecycleState.CONFIGURED);
        assertEquals(WorkerLifecycleState.CONFIGURED, stateManager.getLifecycleState());
    }

    @Test
    void testInvalidLifecycleTransitionThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            stateManager.transitionLifecycle(WorkerLifecycleState.CONFIGURED);
        });
    }

    @Test
    void testValidConnectionTransitions() {
        assertEquals(ConnectionState.DISCONNECTED, stateManager.getConnectionState());
        
        stateManager.transitionConnection(ConnectionState.CONNECTING);
        assertEquals(ConnectionState.CONNECTING, stateManager.getConnectionState());

        stateManager.transitionConnection(ConnectionState.REGISTERING);
        assertEquals(ConnectionState.REGISTERING, stateManager.getConnectionState());

        stateManager.transitionConnection(ConnectionState.ONLINE);
        assertEquals(ConnectionState.ONLINE, stateManager.getConnectionState());

        stateManager.transitionConnection(ConnectionState.DISCONNECTED);
        assertEquals(ConnectionState.DISCONNECTED, stateManager.getConnectionState());
    }

    @Test
    void testInvalidConnectionTransitionThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            stateManager.transitionConnection(ConnectionState.ONLINE);
        });
    }

    @Test
    void testValidExecutionTransitions() {
        assertEquals(ExecutionState.OFFLINE, stateManager.getExecutionState());
        
        stateManager.transitionExecution(ExecutionState.IDLE);
        assertEquals(ExecutionState.IDLE, stateManager.getExecutionState());

        stateManager.transitionExecution(ExecutionState.BUSY);
        assertEquals(ExecutionState.BUSY, stateManager.getExecutionState());

        stateManager.transitionExecution(ExecutionState.IDLE);
        assertEquals(ExecutionState.IDLE, stateManager.getExecutionState());
        
        stateManager.transitionExecution(ExecutionState.OFFLINE);
        assertEquals(ExecutionState.OFFLINE, stateManager.getExecutionState());
    }

    @Test
    void testInvalidExecutionTransitionThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            stateManager.transitionExecution(ExecutionState.BUSY);
        });
    }
}
