import type { ClusterSummary, ClusterChartData, TimeSeriesPoint } from '../types';

export const mockClusterSummary: ClusterSummary = {
  status: 'HEALTHY',
  coordinatorStatus: 'RUNNING',
  totalWorkers: 3,
  connectedWorkers: 2,
  activeWorkers: 1,
  offlineWorkers: 1,

  totalCpuCores: 36,
  usedCpuCores: 23,
  availableCpuCores: 13,
  cpuUsagePercent: 63.9,

  totalMemoryGb: 112,
  usedMemoryGb: 38,
  availableMemoryGb: 74,
  memoryUsagePercent: 33.9,

  totalGpus: 2,
  usedGpus: 1,
  availableGpus: 1,

  totalStorageTb: 7,
  usedStorageTb: 3.8,
  availableStorageTb: 3.2,
  storageUsagePercent: 54.3,

  networkInboundMbps: 436,
  networkOutboundMbps: 291,
};

// Generate time-series mock data (last 30 points, ~30 min)
function generateSeries(base: number, variance: number, count = 30): TimeSeriesPoint[] {
  const now = Date.now();
  return Array.from({ length: count }, (_, i) => {
    const t = new Date(now - (count - 1 - i) * 60 * 1000);
    const v = Math.max(0, Math.min(100, base + (Math.random() - 0.5) * variance));
    return {
      time: t.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false }),
      value: parseFloat(v.toFixed(1)),
    };
  });
}

function generateAbsoluteSeries(base: number, variance: number, count = 30): TimeSeriesPoint[] {
  const now = Date.now();
  return Array.from({ length: count }, (_, i) => {
    const t = new Date(now - (count - 1 - i) * 60 * 1000);
    const v = Math.max(0, base + (Math.random() - 0.5) * variance);
    return {
      time: t.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false }),
      value: parseFloat(v.toFixed(1)),
    };
  });
}

export const mockChartData: ClusterChartData = {
  cpu: generateSeries(63, 30),
  memory: generateSeries(34, 12),
  gpu: generateSeries(38, 40),
  networkIn: generateAbsoluteSeries(436, 200),
  networkOut: generateAbsoluteSeries(291, 150),
};
