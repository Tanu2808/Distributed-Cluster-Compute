// ─── Worker / Node Types ───────────────────────────────────────────────────

export type WorkerStatus = 'ONLINE' | 'BUSY' | 'OFFLINE' | 'UNHEALTHY' | 'REGISTERING';

export interface CpuInfo {
  model: string;
  cores: number;
  threads: number;
  usagePercent: number;
  availableCores: number;
}

export interface MemoryInfo {
  totalGb: number;
  usedGb: number;
  availableGb: number;
  usagePercent: number;
}

export interface GpuInfo {
  model: string;
  vramGb: number;
  usagePercent: number;
}

export interface StorageInfo {
  totalTb: number;
  usedTb: number;
  availableTb: number;
  usagePercent: number;
}

export interface NetworkInfo {
  inboundMbps: number;
  outboundMbps: number;
  latencyMs: number;
}

export interface Worker {
  id: string;
  hostname: string;
  ipAddress: string;
  status: WorkerStatus;
  cpu: CpuInfo;
  memory: MemoryInfo;
  gpu: GpuInfo | null;
  storage: StorageInfo;
  network: NetworkInfo;
  lastHeartbeat: string; // ISO timestamp
  os: string;
  architecture: string;
  agentVersion: string;
  connectedSince: string; // ISO timestamp
}

// ─── Cluster / Aggregated Types ─────────────────────────────────────────────

export type ClusterStatus = 'HEALTHY' | 'DEGRADED' | 'CRITICAL' | 'OFFLINE';

export interface ClusterSummary {
  status: ClusterStatus;
  coordinatorStatus: 'RUNNING' | 'STOPPED' | 'DEGRADED';
  totalWorkers: number;
  connectedWorkers: number;
  activeWorkers: number;
  offlineWorkers: number;

  // Aggregated resources
  totalCpuCores: number;
  usedCpuCores: number;
  availableCpuCores: number;
  cpuUsagePercent: number;

  totalMemoryGb: number;
  usedMemoryGb: number;
  availableMemoryGb: number;
  memoryUsagePercent: number;

  totalGpus: number;
  usedGpus: number;
  availableGpus: number;

  totalStorageTb: number;
  usedStorageTb: number;
  availableStorageTb: number;
  storageUsagePercent: number;

  networkInboundMbps: number;
  networkOutboundMbps: number;
}

// ─── Chart / Time Series ────────────────────────────────────────────────────

export interface TimeSeriesPoint {
  time: string;
  value: number;
}

export interface ClusterChartData {
  cpu: TimeSeriesPoint[];
  memory: TimeSeriesPoint[];
  gpu: TimeSeriesPoint[];
  networkIn: TimeSeriesPoint[];
  networkOut: TimeSeriesPoint[];
}

// ─── Events ─────────────────────────────────────────────────────────────────

export type EventType =
  | 'WORKER_CONNECTED'
  | 'WORKER_DISCONNECTED'
  | 'RESOURCE_CHANGED'
  | 'COORDINATOR_STARTED'
  | 'HEARTBEAT_LOST'
  | 'CONFIG_CHANGED'
  | 'WORKER_UNHEALTHY'
  | 'WORKER_RECOVERED';

export type EventSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';

export interface ClusterEvent {
  id: string;
  type: EventType;
  severity: EventSeverity;
  message: string;
  workerId?: string;
  timestamp: string; // ISO
}

// ─── Settings ────────────────────────────────────────────────────────────────

export interface CoordinatorSettings {
  hostname: string;
  port: number;
  clusterId: string;
  clusterName: string;
}

export type RegistrationMode = 'OPEN' | 'TOKEN' | 'CERTIFICATE';
export type LogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

export interface WorkerRegistrationSettings {
  registrationMode: RegistrationMode;
  requireAuthentication: boolean;
  heartbeatIntervalSeconds: number;
  workerTimeoutSeconds: number;
}

export interface ResourceConfigSettings {
  maxCpuAllocationPercent: number;
  maxMemoryAllocationPercent: number;
  gpuAllocationEnabled: boolean;
  perWorkerCpuLimit: number;
  perWorkerMemoryLimitGb: number;
}

export interface MonitoringSettings {
  metricsCollectionIntervalSeconds: number;
  eventRetentionDays: number;
  logLevel: LogLevel;
  enableMetricsExport: boolean;
}

export interface ClusterSettings {
  coordinator: CoordinatorSettings;
  workerRegistration: WorkerRegistrationSettings;
  resourceConfig: ResourceConfigSettings;
  monitoring: MonitoringSettings;
}
