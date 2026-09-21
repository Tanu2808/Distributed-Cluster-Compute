import { useMemo } from "react";
import { useWorkers } from "./useWorkers";
import type { ClusterSummary } from "../types";

/**
 * Returns the aggregated cluster summary.
 *
 * Mock mode:  returns mockClusterSummary immediately (isLoading=false).
 * Real mode:  fetches GET /api/cluster, manages loading/error/retry via React Query.
 *
 * Both modes return the same { data, isLoading, isError, error, refetch } shape.
 */
export function useClusterSummary() {

  const {
    data: workers = [],
    isLoading: workersLoading,
    isError: workersError,
    error: workersErr,
    refetch: refetchWorkers,
  } = useWorkers();

  const cluster = useMemo(() => {
    let totalCpuCores = 0;
    let usedCpuCores = 0;
    let totalMemoryGb = 0;
    let usedMemoryGb = 0;
    let totalGpus = 0;
    let totalStorageTb = 0;
    let usedStorageTb = 0;
    let activeWorkers = 0;
    let connectedWorkers = 0;
    let offlineWorkers = 0;

    for (const w of workers) {
      const isOnline = w.state === "ONLINE" || w.state === "BUSY";

      if (w.state === "BUSY") activeWorkers++;
      if (isOnline) connectedWorkers++;
      if (w.state === "OFFLINE" || w.state === "HEARTBEAT_TIMEOUT")
        offlineWorkers++;

      if (isOnline) {
        totalCpuCores += w.cpuCores || 0;
        totalMemoryGb += (w.memoryRamMb || 0) / 1024;
        totalGpus += w.gpuCount || 0;
        totalStorageTb += (w.storageMb || 0) / (1024 * 1024);

        usedCpuCores += ((w.cpuCores || 0) * (w.cpuUsagePercent || 0)) / 100;
        usedMemoryGb +=
          (((w.memoryRamMb || 0) / 1024) * (w.memoryUsagePercent || 0)) / 100;

        // Approximation for used storage (since we don't track used storage per worker yet)
        usedStorageTb += 0; // Removed mock value
      }
    }

    return {
      status: workers.length === 0 ? "NO WORKERS" : (offlineWorkers > 0 ? "DEGRADED" : "RUNNING"),
      coordinatorStatus: "RUNNING",
      totalWorkers: workers.length,
      connectedWorkers,
      activeWorkers,
      offlineWorkers,
      totalCpuCores: Math.round(totalCpuCores),
      usedCpuCores: Math.round(usedCpuCores),
      totalMemoryGb: Math.round(totalMemoryGb),
      usedMemoryGb: Math.round(usedMemoryGb),
      totalGpus,
      totalStorageTb,
      usedStorageTb,
      // Provide fallback defaults for other missing values to prevent UI crashes
      availableCpuCores: Math.round(totalCpuCores - usedCpuCores),
      cpuUsagePercent:
        totalCpuCores > 0 ? (usedCpuCores / totalCpuCores) * 100 : 0,
      availableMemoryGb: Math.round(totalMemoryGb - usedMemoryGb),
      memoryUsagePercent:
        totalMemoryGb > 0 ? (usedMemoryGb / totalMemoryGb) * 100 : 0,
      usedGpus: 0,
      availableGpus: totalGpus,
      availableStorageTb: totalStorageTb - usedStorageTb,
      storageUsagePercent:
        totalStorageTb > 0 ? (usedStorageTb / totalStorageTb) * 100 : 0,
      networkInboundMbps: 0,
      networkOutboundMbps: 0,
    } as ClusterSummary;
  }, [workers]);

  return {
    data: cluster,
    isLoading: workersLoading,
    isError: workersError,
    error: workersErr,
    refetch: () => {
      refetchWorkers();
    },
  };
}
