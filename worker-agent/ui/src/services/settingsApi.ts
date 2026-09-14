import { fetchApi } from './apiClient';
import type { SettingsResponse } from '../types';

export const settingsApi = {
  getSettings: () => fetchApi<SettingsResponse>('/settings'),
};
