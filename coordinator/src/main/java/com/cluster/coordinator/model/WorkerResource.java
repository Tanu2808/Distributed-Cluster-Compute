package com.cluster.coordinator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "worker_resources")
public class WorkerResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", unique = true, nullable = false)
    private String workerId;

    private int cpuCores;
    
    private long memoryRamMb;
    
    private int gpuCount;
    
    private long storageMb;
    
    private long networkBps;

    public WorkerResource() {}

    public WorkerResource(String workerId, int cpuCores, long memoryRamMb, int gpuCount, long storageMb, long networkBps) {
        this.workerId = workerId;
        this.cpuCores = cpuCores;
        this.memoryRamMb = memoryRamMb;
        this.gpuCount = gpuCount;
        this.storageMb = storageMb;
        this.networkBps = networkBps;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public int getCpuCores() { return cpuCores; }
    public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
    public long getMemoryRamMb() { return memoryRamMb; }
    public void setMemoryRamMb(long memoryRamMb) { this.memoryRamMb = memoryRamMb; }
    public int getGpuCount() { return gpuCount; }
    public void setGpuCount(int gpuCount) { this.gpuCount = gpuCount; }
    public long getStorageMb() { return storageMb; }
    public void setStorageMb(long storageMb) { this.storageMb = storageMb; }
    public long getNetworkBps() { return networkBps; }
    public void setNetworkBps(long networkBps) { this.networkBps = networkBps; }
}
