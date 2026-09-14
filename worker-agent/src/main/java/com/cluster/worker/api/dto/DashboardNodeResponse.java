package com.cluster.worker.api.dto;

public record DashboardNodeResponse(
    String osName,
    String osVersion,
    String architecture,
    String cpuModel,
    double cpuUsagePercent,
    long memoryUsedMb,
    long memoryTotalMb,
    long diskUsedMb,
    long diskTotalMb,
    int gpuCount,
    long networkBytesSent,
    long networkBytesReceived,
    String agentVersion
) {}
