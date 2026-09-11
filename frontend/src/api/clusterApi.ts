import { apiGet } from './client';
import type { ClusterSummary } from '../types';

/**
 * GET /api/cluster
 * Returns the full cluster summary including aggregated resource counts
 * and coordinator/worker status.
 */
export function fetchClusterSummary(): Promise<ClusterSummary> {
  return apiGet<ClusterSummary>('/api/cluster');
}

/**
 * GET /api/cluster/resources
 * Returns aggregated resource metrics (CPU, memory, GPU, storage, network).
 * Subset of ClusterSummary — useful for high-frequency resource polling.
 */
export function fetchClusterResources(): Promise<ClusterSummary> {
  return apiGet<ClusterSummary>('/api/cluster/resources');
}
