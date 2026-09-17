import { useState, useEffect, useCallback } from 'react';
import { settingsApi } from '../services/settingsApi';
import type { WorkerSettingsResponse, ClusterSettingsResponse } from '../types';

export function useSettings() {
  const [workerSettings, setWorkerSettings] = useState<WorkerSettingsResponse | null>(null);
  const [clusterSettings, setClusterSettings] = useState<ClusterSettingsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const poll = async () => {
      try {
        const [worker, cluster] = await Promise.all([
          settingsApi.getWorkerSettings(),
          settingsApi.getClusterSettings(),
        ]);
        if (mounted) {
          setWorkerSettings(worker);
          setClusterSettings(cluster);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Unable to load settings.');
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    poll();
  }, []);

  const refetch = useCallback(async () => {
    setRefreshing(true);
    try {
      const [worker, cluster] = await Promise.all([
        settingsApi.getWorkerSettings(),
        settingsApi.getClusterSettings(),
      ]);
      setWorkerSettings(worker);
      setClusterSettings(cluster);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load settings.');
    } finally {
      setRefreshing(false);
      setLoading(false);
    }
  }, []);

  return {
    workerSettings,
    clusterSettings,
    loading,
    refreshing,
    error,
    refetch,
  };
}
