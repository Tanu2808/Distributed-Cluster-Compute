import { fetchApi } from './apiClient';
import type { DashboardHomeResponse, DashboardNodeResponse } from '../types';

export const dashboardApi = {
  getHome: () => fetchApi<DashboardHomeResponse>('/worker/dashboard/home'),
  getNode: () => fetchApi<DashboardNodeResponse>('/worker/dashboard/node'),
};
