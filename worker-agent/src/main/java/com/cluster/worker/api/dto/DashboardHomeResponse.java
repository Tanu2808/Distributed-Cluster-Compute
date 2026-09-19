package com.cluster.worker.api.dto;

public record DashboardHomeResponse(
        String workerStatus,
        String coordinatorConnection,
        String clusterName,
        String workerHostname,
        double cpuUsagePercent,
        int cpuCores,
        long ramUsageMb,
        long ramTotalMb,
        long storageUsageMb,
        long storageTotalMb,
        int gpuCount,
        int activeTasks,
        int queuedTasks) {}
