import { apiGet } from './client';
import type { ClusterEvent } from '../types';

/**
 * GET /api/cluster/events
 * Returns recent cluster events (worker connections, heartbeat loss, etc.)
 * ordered from oldest to newest.
 */
export function fetchEvents(): Promise<ClusterEvent[]> {
  return apiGet<ClusterEvent[]>('/api/cluster/events');
}
