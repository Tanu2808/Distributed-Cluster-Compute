import { useState, useEffect } from 'react';
import { connectionApi } from '../services/connectionApi';
import type { ConnectionStatusResponse } from '../types';

export function useConnectionStatus(pollingIntervalMs = 5000) {
  const [connection, setConnection] = useState<ConnectionStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const fetchStatus = async () => {
      try {
        const res = await connectionApi.getStatus();
        if (mounted) {
          setConnection(res);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to fetch connection status');
          setConnection({ connected: false });
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    fetchStatus();
    const intervalId = setInterval(fetchStatus, pollingIntervalMs);

    return () => {
      mounted = false;
      clearInterval(intervalId);
    };
  }, [pollingIntervalMs]);

  return { connection, loading, error };
}
