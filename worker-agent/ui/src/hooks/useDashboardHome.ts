import { useState, useEffect } from 'react';
import { dashboardApi } from '../services/dashboardApi';
import type { DashboardHomeResponse } from '../types';

export function useDashboardHome(pollingIntervalMs = 5000) {
  const [data, setData] = useState<DashboardHomeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const fetchData = async () => {
      try {
        const res = await dashboardApi.getHome();
        if (mounted) {
          setData(res);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to fetch dashboard data');
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

  return { data, loading, error };
}
