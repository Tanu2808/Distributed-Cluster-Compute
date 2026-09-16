package com.cluster.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "worker")
public class WorkerConfig {

    private String name;
    private Coordinator coordinator = new Coordinator();
    private Heartbeat heartbeat = new Heartbeat();
    private Metrics metrics = new Metrics();
    private Connection connection = new Connection();
    private Cluster cluster = new Cluster();
    private Execution execution = new Execution();

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Coordinator getCoordinator() { return coordinator; }
    public void setCoordinator(Coordinator coordinator) { this.coordinator = coordinator; }
    public Heartbeat getHeartbeat() { return heartbeat; }
    public void setHeartbeat(Heartbeat heartbeat) { this.heartbeat = heartbeat; }
    public Metrics getMetrics() { return metrics; }
    public void setMetrics(Metrics metrics) { this.metrics = metrics; }
    public Connection getConnection() { return connection; }
    public void setConnection(Connection connection) { this.connection = connection; }
    public Cluster getCluster() { return cluster; }
    public void setCluster(Cluster cluster) { this.cluster = cluster; }
    public Execution getExecution() { return execution; }
    public void setExecution(Execution execution) { this.execution = execution; }

    public static class Coordinator {
        private String url;
        private String apiKey;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class Heartbeat {
        private long intervalMs;
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    }

    public static class Metrics {
        private long intervalMs;
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    }

    public static class Connection {
        private long timeoutMs;
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class Cluster {
        // cluster config is now managed by WorkerConfigurationStore
    }

    public static class Execution {
        private int maxConcurrentTasks = 4;
        private int queueCapacity = 100;
        private long reservedMemoryMb = 512;
        private int reservedCpuCores = 1;
        private long defaultTimeoutSeconds = 300;

        public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
        public void setMaxConcurrentTasks(int maxConcurrentTasks) { this.maxConcurrentTasks = maxConcurrentTasks; }
        
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        
        public long getReservedMemoryMb() { return reservedMemoryMb; }
        public void setReservedMemoryMb(long reservedMemoryMb) { this.reservedMemoryMb = reservedMemoryMb; }
        
        public int getReservedCpuCores() { return reservedCpuCores; }
        public void setReservedCpuCores(int reservedCpuCores) { this.reservedCpuCores = reservedCpuCores; }
        
        public long getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
        public void setDefaultTimeoutSeconds(long defaultTimeoutSeconds) { this.defaultTimeoutSeconds = defaultTimeoutSeconds; }
    }
}
