import { apiGet } from "./client";
import type { Worker } from "../types";

/**
 * GET /api/workers
 * Returns the list of all registered workers.
 */
export function fetchWorkers(): Promise<Worker[]> {
  return apiGet<Worker[]>("/api/workers");
}

/**
 * GET /api/workers/{workerId}
 * Returns the full detail for a single worker including current utilization.
 */
export function fetchWorker(workerId: string): Promise<Worker> {
  return apiGet<Worker>(`/api/workers/${workerId}`);
}
