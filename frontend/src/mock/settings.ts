import type { ClusterSettings } from '../types';

export const mockSettings: ClusterSettings = {
  coordinator: {
    hostname: 'coordinator.local',
    port: 8080,
    clusterId: 'cluster-7f4a2b1d',
    clusterName: 'HomeCluster-Dev',
  },
  workerRegistration: {
    registrationMode: 'TOKEN',
    requireAuthentication: true,
    heartbeatIntervalSeconds: 10,
    workerTimeoutSeconds: 30,
  },
  resourceConfig: {
    maxCpuAllocationPercent: 90,
    maxMemoryAllocationPercent: 85,
    gpuAllocationEnabled: true,
    perWorkerCpuLimit: 0,   // 0 = no limit
    perWorkerMemoryLimitGb: 0,
  },
  monitoring: {
    metricsCollectionIntervalSeconds: 5,
    eventRetentionDays: 30,
    logLevel: 'INFO',
    enableMetricsExport: false,
  },
};
