package com.cluster.coordinator.dto;

import com.cluster.coordinator.model.WorkerState;
import java.time.LocalDateTime;

public class WorkerResponseDto {
    private String id;
    private String name;
    private String hostname;
    private String ipAddress;
    private String operatingSystem;
    private String architecture;
    private String agentVersion;
    private WorkerState state;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime connectedSince;

    // From WorkerResource
    private long cpuCores;
    private long memoryRamMb;
    private long gpuCount;
    private long storageMb;
    private long networkBps;

    // From latest WorkerHeartbeat
    private double cpuUsagePercent;
    private double memoryUsagePercent;
    private int activeTasks;

    public WorkerResponseDto() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public WorkerState getState() {
        return state;
    }

    public void setState(WorkerState state) {
        this.state = state;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public LocalDateTime getConnectedSince() {
        return connectedSince;
    }

    public void setConnectedSince(LocalDateTime connectedSince) {
        this.connectedSince = connectedSince;
    }

    public long getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(long cpuCores) {
        this.cpuCores = cpuCores;
    }

    public long getMemoryRamMb() {
        return memoryRamMb;
    }

    public void setMemoryRamMb(long memoryRamMb) {
        this.memoryRamMb = memoryRamMb;
    }

    public long getGpuCount() {
        return gpuCount;
    }

    public void setGpuCount(long gpuCount) {
        this.gpuCount = gpuCount;
    }

    public long getStorageMb() {
        return storageMb;
    }

    public void setStorageMb(long storageMb) {
        this.storageMb = storageMb;
    }

    public long getNetworkBps() {
        return networkBps;
    }

    public void setNetworkBps(long networkBps) {
        this.networkBps = networkBps;
    }

    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public double getMemoryUsagePercent() {
        return memoryUsagePercent;
    }

    public void setMemoryUsagePercent(double memoryUsagePercent) {
        this.memoryUsagePercent = memoryUsagePercent;
    }

    public int getActiveTasks() {
        return activeTasks;
    }

    public void setActiveTasks(int activeTasks) {
        this.activeTasks = activeTasks;
    }
}
