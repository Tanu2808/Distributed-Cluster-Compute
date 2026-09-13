export interface WorkerStatusResponse {
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

export interface SettingsResponse {
  workerName: string;
  coordinatorUrl: string;
  heartbeatIntervalMs: number;
  metricsIntervalMs: number;
}
