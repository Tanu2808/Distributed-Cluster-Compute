import { fetchApi } from "./apiClient";

export interface ClusterEnrollmentResponse {
  workerId: string | null;
  status: string;
  message: string;
}

export interface ClusterConfigurationResponse {
  clusterName: string;
  connectionInfo: any;
}

export const clusterApi = {
  connectToCoordinator: async (coordinatorUrl: string): Promise<ClusterEnrollmentResponse> => {
    return await fetchApi<ClusterEnrollmentResponse>("/cluster/connect", {
      method: "POST",
      body: JSON.stringify({ coordinatorUrl }),
    });
  },
};
