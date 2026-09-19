import { useState, useEffect, useCallback } from "react";
import { dashboardApi } from "../services/dashboardApi";
import type { DashboardHomeResponse } from "../types";

export function useDashboardHome(pollingIntervalMs = 5000) {
  const [data, setData] = useState<DashboardHomeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const poll = async () => {
      try {
        const res = await dashboardApi.getHome();
        if (mounted) {
          setData(res);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(
            err instanceof Error
              ? err.message
              : "Failed to fetch dashboard data",
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
      const res = await dashboardApi.getHome();
      setData(res);
      setError(null);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to fetch dashboard data",
      );
    } finally {
      setRefreshing(false);
      setLoading(false);
    }
  }, []);

  return { data, loading, refreshing, error, refetch };
}
