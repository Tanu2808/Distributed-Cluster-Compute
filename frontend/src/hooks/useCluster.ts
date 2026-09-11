import { useQuery } from '@tanstack/react-query';
import { config } from '../utils/config';
import { queryKeys } from '../types/api';
import { fetchClusterSummary } from '../api/clusterApi';
import { mockClusterSummary } from '../mock/cluster';
import type { ClusterSummary } from '../types';

/**
 * Returns the aggregated cluster summary.
 *
 * Mock mode:  returns mockClusterSummary immediately (isLoading=false).
 * Real mode:  fetches GET /api/cluster, manages loading/error/retry via React Query.
 *
 * Both modes return the same { data, isLoading, isError, error, refetch } shape.
 */
export function useClusterSummary() {
  return useQuery<ClusterSummary, Error>({
    queryKey: queryKeys.cluster,
    queryFn: config.USE_MOCK_API
      ? () => Promise.resolve(mockClusterSummary)
      : fetchClusterSummary,
    // In mock mode keep data fresh indefinitely — no need to refetch
    staleTime: config.USE_MOCK_API ? Infinity : undefined,
  });
}
