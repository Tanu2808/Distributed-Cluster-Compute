import { fetchApi } from './apiClient';
import type { ConnectionStatusResponse } from '../types';

export const connectionApi = {
  getStatus: () => fetchApi<ConnectionStatusResponse>('/connection/status'),
};
