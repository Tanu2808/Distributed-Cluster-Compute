package com.cluster.coordinator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class WorkerRegistrationRequest {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String hostname;

    @NotBlank
    private String ipAddress;

    @NotBlank
    private String operatingSystem;

    @NotBlank
    private String architecture;

    @NotBlank
    private String agentVersion;

    @Positive
    private int cpuCores;

    @Positive
    private long memoryRamMb;

    @PositiveOrZero
    private int gpuCount;

    @PositiveOrZero
    private long storageMb;

    @PositiveOrZero
    private long networkBps;

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

    public int getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(int cpuCores) {
        this.cpuCores = cpuCores;
    }

    public long getMemoryRamMb() {
        return memoryRamMb;
    }

    public void setMemoryRamMb(long memoryRamMb) {
        this.memoryRamMb = memoryRamMb;
    }

    public int getGpuCount() {
        return gpuCount;
    }

    public void setGpuCount(int gpuCount) {
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
}
