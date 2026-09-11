import { useQuery } from '@tanstack/react-query';
import { config } from '../utils/config';
import { queryKeys } from '../types/api';
import { fetchWorkers, fetchWorker } from '../api/workersApi';
import { mockWorkers } from '../mock/workers';
import type { Worker } from '../types';

/**
 * Returns the full list of registered workers.
 *
 * Mock mode:  returns mockWorkers immediately.
 * Real mode:  fetches GET /api/workers.
 */
export function useWorkers() {
  return useQuery<Worker[], Error>({
    queryKey: queryKeys.workers,
    queryFn: config.USE_MOCK_API
      ? () => Promise.resolve(mockWorkers)
      : fetchWorkers,
    staleTime: config.USE_MOCK_API ? Infinity : undefined,
  });
}

/**
 * Returns the detail for a single worker by ID.
 *
 * Mock mode:  finds the worker from mockWorkers.
 * Real mode:  fetches GET /api/workers/{workerId}.
 *
 * The query is disabled when workerId is null/undefined.
 */
export function useWorker(workerId: string | null | undefined) {
  return useQuery<Worker, Error>({
    queryKey: queryKeys.worker(workerId ?? ''),
    queryFn: config.USE_MOCK_API
      ? () => {
          const w = mockWorkers.find(w => w.id === workerId);
          if (!w) throw new Error(`Worker ${workerId} not found`);
          return Promise.resolve(w);
        }
      : () => fetchWorker(workerId!),
    enabled: !!workerId,
    staleTime: config.USE_MOCK_API ? Infinity : undefined,
  });
}
