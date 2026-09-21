import { apiGet, apiPut } from "./client";
import type { ClusterSettings, RegistrationMode, LogLevel } from "../types";


/**
 * Helper to parse boolean from string
 */
function parseBool(val: string | undefined, defaultVal: boolean): boolean {
  if (val === "true") return true;
  if (val === "false") return false;
  return defaultVal;
}

/**
 * GET /api/settings
 * Returns the current coordinator + worker cluster settings.
 * The backend returns a flat Record<string, string>. We map it to the nested ClusterSettings object.
 */
export async function fetchSettings(): Promise<ClusterSettings> {
  const map = await apiGet<Record<string, string>>("/api/settings");

  return {
    coordinator: {
      hostname: map["coordinator.hostname"] || "localhost",
      port: parseInt(map["coordinator.port"]) || 8080,
      clusterId: map["coordinator.clusterId"] || "cluster-01",
      clusterName: map["coordinator.clusterName"] || "Default Cluster",
    },
    workerRegistration: {
      registrationMode:
        (map["workerRegistration.registrationMode"] as RegistrationMode) ||
        "OPEN",
      requireAuthentication: parseBool(
        map["workerRegistration.requireAuthentication"],
        false,
      ),
      heartbeatIntervalSeconds:
        parseInt(map["workerRegistration.heartbeatIntervalSeconds"]) || 5,
      workerTimeoutSeconds:
        parseInt(map["workerRegistration.workerTimeoutSeconds"]) || 30,
    },
    resourceConfig: {
      maxCpuAllocationPercent:
        parseInt(map["resourceConfig.maxCpuAllocationPercent"]) || 100,
      maxMemoryAllocationPercent:
        parseInt(map["resourceConfig.maxMemoryAllocationPercent"]) || 100,
      gpuAllocationEnabled: parseBool(
        map["resourceConfig.gpuAllocationEnabled"],
        false,
      ),
      perWorkerCpuLimit:
        parseInt(map["resourceConfig.perWorkerCpuLimit"]) || 0,
      perWorkerMemoryLimitGb:
        parseInt(map["resourceConfig.perWorkerMemoryLimitGb"]) || 0,
    },
    monitoring: {
      metricsCollectionIntervalSeconds:
        parseInt(map["monitoring.metricsCollectionIntervalSeconds"]) || 10,
      eventRetentionDays:
        parseInt(map["monitoring.eventRetentionDays"]) || 7,
      logLevel: (map["monitoring.logLevel"] as LogLevel) || "INFO",
      enableMetricsExport: parseBool(
        map["monitoring.enableMetricsExport"],
        false,
      ),
    },
  };
}

/**
 * PUT /api/settings
 * Applies new cluster settings to the coordinator.
 * Converts the nested ClusterSettings into a flat Record<string, string> for the backend.
 */
export async function updateSettings(
  settings: ClusterSettings,
): Promise<ClusterSettings> {
  const flatMap: Record<string, string> = {
    "coordinator.hostname": settings.coordinator.hostname,
    "coordinator.port": String(settings.coordinator.port),
    "coordinator.clusterId": settings.coordinator.clusterId,
    "coordinator.clusterName": settings.coordinator.clusterName,

    "workerRegistration.registrationMode":
      settings.workerRegistration.registrationMode,
    "workerRegistration.requireAuthentication": String(
      settings.workerRegistration.requireAuthentication,
    ),
    "workerRegistration.heartbeatIntervalSeconds": String(
      settings.workerRegistration.heartbeatIntervalSeconds,
    ),
    "workerRegistration.workerTimeoutSeconds": String(
      settings.workerRegistration.workerTimeoutSeconds,
    ),

    "resourceConfig.maxCpuAllocationPercent": String(
      settings.resourceConfig.maxCpuAllocationPercent,
    ),
    "resourceConfig.maxMemoryAllocationPercent": String(
      settings.resourceConfig.maxMemoryAllocationPercent,
    ),
    "resourceConfig.gpuAllocationEnabled": String(
      settings.resourceConfig.gpuAllocationEnabled,
    ),
    "resourceConfig.perWorkerCpuLimit": String(
      settings.resourceConfig.perWorkerCpuLimit,
    ),
    "resourceConfig.perWorkerMemoryLimitGb": String(
      settings.resourceConfig.perWorkerMemoryLimitGb,
    ),

    "monitoring.metricsCollectionIntervalSeconds": String(
      settings.monitoring.metricsCollectionIntervalSeconds,
    ),
    "monitoring.eventRetentionDays": String(
      settings.monitoring.eventRetentionDays,
    ),
    "monitoring.logLevel": settings.monitoring.logLevel,
    "monitoring.enableMetricsExport": String(
      settings.monitoring.enableMetricsExport,
    ),
  };

  await apiPut("/api/settings", flatMap);

  // Return the original structured object to update the UI cache
  return settings;
}
