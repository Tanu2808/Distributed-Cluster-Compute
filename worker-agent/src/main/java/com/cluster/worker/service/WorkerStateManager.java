package com.cluster.worker.service;

import com.cluster.worker.model.ConnectionState;
import com.cluster.worker.model.ExecutionState;
import com.cluster.worker.model.WorkerLifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WorkerStateManager {
    private static final Logger log = LoggerFactory.getLogger(WorkerStateManager.class);

    private WorkerLifecycleState lifecycleState = WorkerLifecycleState.STARTING;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private ExecutionState executionState = ExecutionState.OFFLINE;

    public synchronized void transitionLifecycle(WorkerLifecycleState newState) {
        if (!isValidLifecycleTransition(this.lifecycleState, newState)) {
            log.warn("Invalid lifecycle transition from {} to {}", this.lifecycleState, newState);
            throw new IllegalStateException("Invalid lifecycle transition from " + this.lifecycleState + " to " + newState);
        }
        log.info("Lifecycle state transition: {} -> {}", this.lifecycleState, newState);
        this.lifecycleState = newState;
    }

    public synchronized void transitionConnection(ConnectionState newState) {
        if (!isValidConnectionTransition(this.connectionState, newState)) {
            log.warn("Invalid connection transition from {} to {}", this.connectionState, newState);
            throw new IllegalStateException("Invalid connection transition from " + this.connectionState + " to " + newState);
        }
        log.info("Connection state transition: {} -> {}", this.connectionState, newState);
        this.connectionState = newState;
    }

    public synchronized void transitionExecution(ExecutionState newState) {
        if (!isValidExecutionTransition(this.executionState, newState)) {
            log.warn("Invalid execution transition from {} to {}", this.executionState, newState);
            throw new IllegalStateException("Invalid execution transition from " + this.executionState + " to " + newState);
        }
        log.info("Execution state transition: {} -> {}", this.executionState, newState);
        this.executionState = newState;
    }

    private boolean isValidLifecycleTransition(WorkerLifecycleState current, WorkerLifecycleState next) {
        if (next == WorkerLifecycleState.STOPPING) return true;
        
        switch (current) {
            case STARTING:
                return next == WorkerLifecycleState.INITIALIZING;
            case INITIALIZING:
                return next == WorkerLifecycleState.SETUP_REQUIRED || next == WorkerLifecycleState.LOADING_CONFIGURATION;
            case LOADING_CONFIGURATION:
                return next == WorkerLifecycleState.CONFIGURED;
            case SETUP_REQUIRED:
                return next == WorkerLifecycleState.LOADING_CONFIGURATION;
            case CONFIGURED:
                return false;
            case STOPPING:
                return false;
            default:
                return false;
        }
    }

    private boolean isValidConnectionTransition(ConnectionState current, ConnectionState next) {
        switch (current) {
            case DISCONNECTED:
                return next == ConnectionState.CONNECTING;
            case CONNECTING:
                return next == ConnectionState.REGISTERING || next == ConnectionState.DISCONNECTED;
            case REGISTERING:
                return next == ConnectionState.ONLINE || next == ConnectionState.DISCONNECTED;
            case ONLINE:
                return next == ConnectionState.DISCONNECTED;
            case RECONNECTING:
                return next == ConnectionState.CONNECTING || next == ConnectionState.DISCONNECTED;
            default:
                return false;
        }
    }

    private boolean isValidExecutionTransition(ExecutionState current, ExecutionState next) {
        switch (current) {
            case OFFLINE:
                return next == ExecutionState.IDLE;
            case IDLE:
                return next == ExecutionState.BUSY || next == ExecutionState.OFFLINE;
            case BUSY:
                return next == ExecutionState.IDLE || next == ExecutionState.OFFLINE;
            default:
                return false;
        }
    }

    public synchronized WorkerLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public synchronized ConnectionState getConnectionState() {
        return connectionState;
    }

    public synchronized ExecutionState getExecutionState() {
        return executionState;
    }
}
