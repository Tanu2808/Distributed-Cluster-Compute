package com.cluster.worker.api.dto;
public record WorkerSettingsResponse(String workerName, String coordinatorUrl, long heartbeatIntervalMs, long metricsIntervalMs) {}
