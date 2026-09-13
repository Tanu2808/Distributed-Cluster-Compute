import { fetchApi } from './apiClient';
import type { ActiveTasksResponse } from '../types';

export const taskApi = {
  getActive: () => fetchApi<ActiveTasksResponse>('/tasks/active'),
};
