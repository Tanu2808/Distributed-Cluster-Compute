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

export interface ClusterSettingsResponse {
  clusterId: string;
  clusterName: string;
  coordinatorUrl: string;
  isConfigured: boolean;
}
