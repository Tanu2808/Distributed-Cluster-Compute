import { apiGet, apiPut } from './client';
import type { ClusterSettings } from '../types';

/**
 * GET /api/settings
 * Returns the current coordinator + worker cluster settings.
 */
export function fetchSettings(): Promise<ClusterSettings> {
  return apiGet<ClusterSettings>('/api/settings');
}

/**
 * PUT /api/settings
 * Applies new cluster settings to the coordinator.
 * Returns the persisted settings as confirmed by the backend.
 */
export function updateSettings(settings: ClusterSettings): Promise<ClusterSettings> {
  return apiPut<ClusterSettings>('/api/settings', settings);
}
