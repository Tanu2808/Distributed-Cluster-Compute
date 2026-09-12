package com.cluster.worker.communication;

public class RegistrationPayload {
    private String id;
    private String name;
    private String hostname;
    private String ipAddress;
    private String operatingSystem;
    private String architecture;
    private String agentVersion;
    private int cpuCores;
    private long memoryRamMb;
    private int gpuCount;
    private long storageMb;
    private long networkBps;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }
    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String agentVersion) { this.agentVersion = agentVersion; }
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
