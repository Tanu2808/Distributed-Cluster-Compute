import { fetchApi } from './apiClient';
import type { ActiveTasksResponse, WorkerTask } from '../types';

export const taskApi = {
  getActive: () => fetchApi<ActiveTasksResponse>('/tasks/active'),
  getAll: () => fetchApi<WorkerTask[]>('/tasks'),
  getTask: (taskId: string) => fetchApi<WorkerTask>(`/tasks/${taskId}`),
  cancelTask: (taskId: string) => fetchApi<void>(`/tasks/${taskId}/cancel`, { method: 'POST' }),
};
