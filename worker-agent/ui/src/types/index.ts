export interface WorkerStatusResponse {
  lifecycleState?: string;
  connectionState?: string;
  executionState?: string;
  status: string;
}

export interface WorkerInfoResponse {
  workerId: string;
}

export interface ConnectionDiagnosticsResponse {
  connectionState: string;
  coordinatorUrl: string;
  connectedSince: string | null;
  lastSuccessfulHeartbeat: string | null;
  lastMessageTimestamp: string | null;
  reconnectCount: number;
  lastConnectionError: string | null;
}

export interface ActiveTasksResponse {
  tasks: unknown[];
}

export interface WorkerSettingsResponse {
  workerId: string;
  version: number;
}

export interface ClusterEnrollmentResponse {
  workerId: string;
  status: 'PENDING' | 'REGISTERED' | 'FAILED';
  message?: string;
}

export interface DashboardHomeResponse {
  workerStatus: string;
  coordinatorConnection: string;
  clusterName: string;
  workerHostname: string;
  cpuUsagePercent: number;
  cpuCores: number;
  ramUsageMb: number;
  ramTotalMb: number;
  storageUsageMb: number;
  storageTotalMb: number;
  gpuCount: number;
  activeTasks: number;
  queuedTasks: number;
}

export interface DashboardNodeResponse {
  osName: string;
  osVersion: string;
  architecture: string;
  cpuModel: string;
  cpuUsagePercent: number;
  memoryUsedMb: number;
  memoryTotalMb: number;
  diskUsedMb: number;
  diskTotalMb: number;
  gpuCount: number;
  networkBytesSent: number;
  networkBytesReceived: number;
  agentVersion: string;
}

export interface ClusterSettingsResponse {
  clusterId: string;
  clusterName: string;
  coordinatorUrl: string;
  isConfigured: boolean;
}
