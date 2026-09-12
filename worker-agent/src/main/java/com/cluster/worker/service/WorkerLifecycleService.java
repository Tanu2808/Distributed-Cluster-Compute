package com.cluster.worker.service;

import com.cluster.worker.communication.CoordinatorClient;
import com.cluster.worker.communication.RegistrationPayload;
import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.SystemMetrics;
import com.cluster.worker.model.WorkerState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.registration.WorkerIdentityGenerator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WorkerLifecycleService {

    private final CoordinatorClient coordinatorClient;
    private final WorkerIdentityGenerator identityGenerator;
    private final SystemMetricsProvider metricsProvider;
    private final WorkerConfig config;
    private WorkerState state = WorkerState.STARTING;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public WorkerLifecycleService(CoordinatorClient coordinatorClient,
                                  WorkerIdentityGenerator identityGenerator,
                                  SystemMetricsProvider metricsProvider,
                                  WorkerConfig config) {
        this.coordinatorClient = coordinatorClient;
        this.identityGenerator = identityGenerator;
        this.metricsProvider = metricsProvider;
        this.config = config;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (state == WorkerState.STARTING || state == WorkerState.DISCONNECTED) {
            registerWithBackoff();
        }
    }

    private void registerWithBackoff() {
        state = WorkerState.REGISTERING;
        SystemMetrics metrics = metricsProvider.collectMetrics();
        RegistrationPayload payload = createRegistrationPayload(metrics);

        boolean registered = coordinatorClient.register(payload);
        if (registered) {
            state = WorkerState.ONLINE;
            System.out.println("Worker registered successfully with ID: " + payload.getId());
        } else {
            System.out.println("Failed to register with coordinator. Retrying in 5 seconds...");
            executorService.schedule(this::registerWithBackoff, 5, TimeUnit.SECONDS);
        }
    }

    private RegistrationPayload createRegistrationPayload(SystemMetrics metrics) {
        RegistrationPayload payload = new RegistrationPayload();
        payload.setId(identityGenerator.getOrCreateWorkerId());
        payload.setName(config.getName());
        payload.setCpuCores(metrics.getCpuCores());
        payload.setMemoryRamMb(metrics.getTotalMemoryMb());
        payload.setGpuCount(metrics.getGpuCount());
        payload.setStorageMb(metrics.getTotalStorageMb());
        payload.setNetworkBps(metrics.getNetworkBytesSent() + metrics.getNetworkBytesReceived()); // Simplification
        
        payload.setOperatingSystem(System.getProperty("os.name"));
        payload.setArchitecture(System.getProperty("os.arch"));
        payload.setAgentVersion("1.0.0");
        
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            payload.setHostname(localHost.getHostName());
            payload.setIpAddress(localHost.getHostAddress());
        } catch (UnknownHostException e) {
            payload.setHostname("unknown");
            payload.setIpAddress("unknown");
        }
        return payload;
    }

    public WorkerState getState() {
        return state;
    }

    public void setState(WorkerState state) {
        this.state = state;
    }

    public void handleDisconnection() {
        if (state != WorkerState.REGISTERING && state != WorkerState.DISCONNECTED && state != WorkerState.STOPPING) {
            System.err.println("Handling disconnection... transitioning to DISCONNECTED state.");
            state = WorkerState.DISCONNECTED;
            registerWithBackoff();
        }
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down worker agent...");
        this.state = WorkerState.STOPPING;
        try {
            coordinatorClient.deregister(identityGenerator.getOrCreateWorkerId());
        } catch (Exception e) {
            System.err.println("Failed to deregister gracefully: " + e.getMessage());
        }
        executorService.shutdown();
    }
}
