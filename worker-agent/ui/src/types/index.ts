export interface WorkerStatusResponse {
  lifecycleState?: string;
  connectionState?: string;
  executionState?: string;
  status: string;
}

export interface WorkerInfoResponse {
  workerId: string;
}

export interface ConnectionStatusResponse {
  connected: boolean;
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
