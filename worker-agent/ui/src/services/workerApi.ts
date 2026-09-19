import { fetchApi } from "./apiClient";
import type { WorkerStatusResponse, WorkerInfoResponse } from "../types";

export const workerApi = {
  getStatus: () => fetchApi<WorkerStatusResponse>("/worker/status"),
  getInfo: () => fetchApi<WorkerInfoResponse>("/worker/info"),
};
