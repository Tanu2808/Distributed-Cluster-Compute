import { fetchApi } from './apiClient';

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
    joinCluster: async (joinCode: string): Promise<ClusterEnrollmentResponse> => {
        return await fetchApi<ClusterEnrollmentResponse>('/cluster/join', {
            method: 'POST',
            body: JSON.stringify({ joinCode })
        });
    },

    createCluster: async (clusterName: string, isLocal: boolean): Promise<ClusterConfigurationResponse> => {
        return await fetchApi<ClusterConfigurationResponse>('/cluster/create', {
            method: 'POST',
            body: JSON.stringify({ clusterName, isLocal })
        });
    }
};
