import { useState, useEffect, useCallback } from 'react';
import { taskApi } from '../services/taskApi';
import type { WorkerTask } from '../types';

export function useTasks(pollingIntervalMs = 4000) {
  const [tasks, setTasks] = useState<WorkerTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const poll = async () => {
      try {
        const res = await taskApi.getAll();
        if (mounted) {
          setTasks(res);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to fetch tasks');
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
      const res = await taskApi.getAll();
      setTasks(res);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch tasks');
    } finally {
      setRefreshing(false);
      setLoading(false);
    }
  }, []);

  return {
    tasks,
    data: { tasks }, // For backward compatibility
    loading,
    refreshing,
    error,
    refetch,
  };
}
