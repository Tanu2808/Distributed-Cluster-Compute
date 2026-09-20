import { useQuery } from "@tanstack/react-query";

import { queryKeys } from "../types/api";
import { fetchWorkers, fetchWorker } from "../api/workersApi";
import type { Worker } from "../types";

/**
 * Returns the full list of registered workers.
 *
 * Mock mode:  returns mockWorkers immediately.
 * Real mode:  fetches GET /api/workers.
 */
export function useWorkers() {
  return useQuery<Worker[], Error>({
    queryKey: queryKeys.workers,
    queryFn: fetchWorkers,
  });
}

/**
 * Returns the detail for a single worker by ID.
 *
 * Mock mode:  finds the worker from mockWorkers.
 * Real mode:  fetches GET /api/workers/{workerId}.
 */
export function useWorker(workerId: string | null | undefined) {
  return useQuery<Worker, Error>({
    queryKey: queryKeys.worker(workerId ?? ""),
    queryFn: () => fetchWorker(workerId!),
    enabled: !!workerId,
  });
}
