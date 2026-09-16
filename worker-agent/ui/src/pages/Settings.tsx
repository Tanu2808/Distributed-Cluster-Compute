import { useEffect, useState } from 'react';
import { settingsApi } from '../services/settingsApi';
import type { WorkerSettingsResponse, ClusterSettingsResponse } from '../types';

export default function Settings() {
  const [workerSettings, setWorkerSettings] = useState<WorkerSettingsResponse | null>(null);
  const [clusterSettings, setClusterSettings] = useState<ClusterSettingsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSettings = async () => {
      try {
        const [worker, cluster] = await Promise.all([
          settingsApi.getWorkerSettings(),
          settingsApi.getClusterSettings(),
        ]);
        setWorkerSettings(worker);
        setClusterSettings(cluster);
      } catch (err: any) {
        setError(err.message || 'Failed to load settings');
      } finally {
        setLoading(false);
      }
    };
    fetchSettings();
  }, []);

  const handleReset = async () => {
    if (!confirm('Are you sure you want to reset the cluster configuration? This will disconnect the worker and require setup again.')) {
      return;
    }
    
    try {
      await settingsApi.resetSettings();
      window.location.href = '/setup';
    } catch (err: any) {
      alert('Failed to reset configuration: ' + (err.message || 'Unknown error'));
    }
  };

  if (loading) return <div className="text-worker-muted">Loading settings...</div>;
  if (error) return <div className="text-red-400">Error: {error}</div>;

  return (
    <div className="space-y-6 max-w-2xl">
      <h1 className="text-2xl font-bold">Settings</h1>
      
      <div className="bg-worker-card border border-worker-border rounded-lg p-6 space-y-4">
        <h2 className="text-lg font-semibold border-b border-worker-border pb-2">Worker Agent Details</h2>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div className="text-worker-muted">Worker ID</div>
          <div className="font-mono">{workerSettings?.workerId}</div>
          <div className="text-worker-muted">Config Version</div>
          <div>{workerSettings?.version}</div>
        </div>
      </div>

      <div className="bg-worker-card border border-worker-border rounded-lg p-6 space-y-4">
        <h2 className="text-lg font-semibold border-b border-worker-border pb-2">Cluster Configuration</h2>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div className="text-worker-muted">Cluster Name</div>
          <div>{clusterSettings?.clusterName || 'N/A'}</div>
          <div className="text-worker-muted">Cluster ID</div>
          <div className="font-mono">{clusterSettings?.clusterId || 'N/A'}</div>
          <div className="text-worker-muted">Coordinator URL</div>
          <div className="font-mono">{clusterSettings?.coordinatorUrl || 'N/A'}</div>
          <div className="text-worker-muted">Status</div>
          <div>
            {clusterSettings?.isConfigured ? 
              <span className="text-green-400">Configured</span> : 
              <span className="text-yellow-400">Not Configured</span>
            }
          </div>
        </div>
      </div>

      <div className="bg-worker-card border border-worker-border rounded-lg p-6 space-y-4">
        <h2 className="text-lg font-semibold text-red-500 border-b border-worker-border pb-2">Danger Zone</h2>
        <p className="text-sm text-worker-muted">
          Resetting the configuration will clear the cluster connection details and return the worker to the initial setup state. The worker ID will be preserved.
        </p>
        <button 
          onClick={handleReset}
          className="px-4 py-2 bg-red-500/10 text-red-500 border border-red-500/30 rounded hover:bg-red-500/20 transition-colors"
        >
          Reset Configuration
        </button>
      </div>
    </div>
  )
}
