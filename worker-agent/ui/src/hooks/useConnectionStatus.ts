import { useState, useEffect, useCallback } from "react";
import { connectionApi } from "../services/connectionApi";
import type { ConnectionDiagnosticsResponse } from "../types";

export function useConnectionStatus(pollingIntervalMs = 5000) {
  const [connection, setConnection] =
    useState<ConnectionDiagnosticsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const poll = async () => {
      try {
        const res = await connectionApi.getStatus();
        if (mounted) {
          setConnection(res);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(
            err instanceof Error
              ? err.message
              : "Failed to fetch connection status",
          );
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    poll();
    const intervalId = setInterval(poll, pollingIntervalMs);

    return () => {
      mounted = false;
      clearInterval(intervalId);
    };
  }, [pollingIntervalMs]);

  const refetch = useCallback(async () => {
    setRefreshing(true);
    try {
      const res = await connectionApi.getStatus();
      setConnection(res);
      setError(null);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to fetch connection status",
      );
    } finally {
      setRefreshing(false);
      setLoading(false);
    }
  }, []);

  return { connection, loading, refreshing, error, refetch };
}
