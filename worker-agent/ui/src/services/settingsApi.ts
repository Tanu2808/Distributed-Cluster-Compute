import { fetchApi } from "./apiClient";
import type { WorkerSettingsResponse, ClusterSettingsResponse } from "../types";

export const settingsApi = {
  getWorkerSettings: () => fetchApi<WorkerSettingsResponse>("/settings/worker"),
  getClusterSettings: () =>
    fetchApi<ClusterSettingsResponse>("/settings/cluster"),
  resetSettings: () =>
    fetchApi<{ message: string }>("/settings/reset", { method: "POST" }),
};
