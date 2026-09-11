package com.cluster.coordinator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class WorkerRegistrationRequest {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

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
