import { useState, useEffect } from 'react';
import { taskApi } from '../services/taskApi';
import type { ActiveTasksResponse } from '../types';

export function useTasks(pollingIntervalMs = 5000) {
  const [data, setData] = useState<ActiveTasksResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const fetchTasks = async () => {
      try {
        const res = await taskApi.getActive();
        if (mounted) {
          setData(res);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to fetch tasks');
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    fetchTasks();
    const intervalId = setInterval(fetchTasks, pollingIntervalMs);

    return () => {
      mounted = false;
      clearInterval(intervalId);
    };
  }, [pollingIntervalMs]);

  return { data, loading, error };
}
