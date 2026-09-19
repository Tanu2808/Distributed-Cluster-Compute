import { useState, useEffect } from "react";
import { workerApi } from "../services/workerApi";
import type { WorkerStatusResponse, WorkerInfoResponse } from "../types";

export function useWorkerStatus(pollingIntervalMs = 5000) {
  const [status, setStatus] = useState<WorkerStatusResponse | null>(null);
  const [info, setInfo] = useState<WorkerInfoResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const fetchData = async () => {
      try {
        const [statusRes, infoRes] = await Promise.all([
          workerApi.getStatus(),
          workerApi.getInfo(),
        ]);
        if (mounted) {
          setStatus(statusRes);
          setInfo(infoRes);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(
            err instanceof Error
              ? err.message
              : "Failed to fetch worker status",
          );
          setStatus({ status: "UNKNOWN" });
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    fetchData();
    const intervalId = setInterval(fetchData, pollingIntervalMs);

    return () => {
      mounted = false;
      clearInterval(intervalId);
    };
  }, [pollingIntervalMs]);

  return { status, info, loading, error };
}
