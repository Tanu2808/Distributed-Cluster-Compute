export type WorkerLifecycleState =
  | 'STARTING'
  | 'INITIALIZING'
  | 'SETUP_REQUIRED'
  | 'LOADING_CONFIGURATION'
  | 'CONFIGURED'
  | 'STOPPING';

export type ConnectionState =
  | 'DISCONNECTED'
  | 'CONNECTING'
  | 'REGISTERING'
  | 'ONLINE'
  | 'RECONNECTING';

export type ExecutionState =
  | 'OFFLINE'
  | 'IDLE'
  | 'BUSY';

export type TaskState =
  | 'RECEIVED'
  | 'VALIDATING'
  | 'QUEUED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'REJECTED'
  | 'CANCELLED';

export interface WorkerStatusResponse {
  lifecycleState?: WorkerLifecycleState | string;
  connectionState?: ConnectionState | string;
  executionState?: ExecutionState | string;
  status: string;
}

export interface WorkerInfoResponse {
  workerId: string;
}

export interface ConnectionDiagnosticsResponse {
  connectionState: ConnectionState | string;
  coordinatorUrl: string;
  connectedSince: string | null;
  lastSuccessfulHeartbeat: string | null;
  lastMessageTimestamp: string | null;
  reconnectCount: number;
  lastConnectionError: string | null;
}

export interface WorkerTask {
  taskId: string;
  taskType: string;
  input?: Record<string, unknown>;
  requiredCpuCores: number;
  requiredMemoryMb: number;
  timeoutSeconds: number;
  state: TaskState;
  errorMessage?: string | null;
  result?: unknown;
  receivedAt?: string;
  startedAt?: string;
  completedAt?: string;
}

export interface ActiveTasksResponse {
  tasks: WorkerTask[];
}

export interface WorkerSettingsResponse {
  workerId: string;
  version: number;
}

export interface ClusterEnrollmentResponse {
  workerId: string | null;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | string;
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
