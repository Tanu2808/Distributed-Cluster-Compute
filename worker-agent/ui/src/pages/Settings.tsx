import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { RefreshCw, Copy, Check, RotateCcw, AlertTriangle } from 'lucide-react';
import { useSettings } from '../hooks/useSettings';
import { settingsApi } from '../services/settingsApi';
import {
  ConsoleCard,
  KeyValueTable,
  StatusBadge,
  AlertBanner,
  ConfirmModal,
  Skeleton,
} from '../components';

export default function Settings() {
  const { workerSettings, clusterSettings, loading, refreshing, error, refetch } = useSettings();
  const navigate = useNavigate();

  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [showResetModal, setShowResetModal] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const handleCopy = (text: string, key: string) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 1800);
  };

  const handleConfirmReset = async () => {
    setResetting(true);
    try {
      await settingsApi.resetSettings();
      setNotification({
        type: 'success',
        message: 'Settings reset to default values. Cluster configuration cleared.',
      });
      await refetch();
      setTimeout(() => {
        navigate('/setup');
      }, 1200);
    } catch (err) {
      setNotification({
        type: 'error',
        message: err instanceof Error ? err.message : 'Failed to reset settings.',
      });
    } finally {
      setResetting(false);
      setShowResetModal(false);
    }
  };

  // Initial Loading Skeleton
  if (loading && !workerSettings && !clusterSettings) {
    return (
      <div className="space-y-4 max-w-7xl">
        <div className="flex items-center justify-between pb-2 border-b border-console-border">
          <div className="space-y-1">
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-4 w-60" />
          </div>
          <div className="flex items-center gap-2">
            <Skeleton className="h-7 w-20" />
            <Skeleton className="h-7 w-32" />
          </div>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Skeleton className="h-48" />
          <Skeleton className="h-48" />
        </div>
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-4 max-w-7xl">
      {/* 1. Page Header */}
      <div className="flex items-start sm:items-center justify-between gap-4 pb-2.5 border-b border-console-border">
        <div>
          <h1 className="text-base font-semibold tracking-tight text-console-text">
            Settings
          </h1>
          <p className="text-[11px] text-console-textDim mt-0.5">
            Worker and cluster configuration
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => refetch()}
            disabled={refreshing}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-console-text hover:bg-slate-50 bg-white border border-console-border hover:border-slate-400 rounded-sm transition-colors disabled:opacity-50"
            title="Refresh settings"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-console-textDim ${refreshing ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>

          <button
            type="button"
            onClick={() => setShowResetModal(true)}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-rose-700 hover:bg-rose-50 bg-white border border-rose-300 rounded-sm transition-colors"
            title="Reset worker settings to default"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Reset to Defaults</span>
          </button>
        </div>
      </div>

      {/* Notifications / Feedback */}
      {notification && (
        <AlertBanner
          type={notification.type}
          message={notification.message}
          onClose={() => setNotification(null)}
        />
      )}

      {error && (
        <AlertBanner
          type="error"
          message={
            <div className="flex items-center justify-between gap-4">
              <span>{error}</span>
              <button
                onClick={() => refetch()}
                className="underline hover:text-console-accent text-rose-800 shrink-0 font-sans font-medium"
              >
                Retry
              </button>
            </div>
          }
        />
      )}

      {/* 2. Configuration Two-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Worker Settings Panel */}
        <ConsoleCard title="Worker Settings">
          <KeyValueTable
            columns={1}
            items={[
              {
                label: 'Worker ID',
                mono: true,
                value: workerSettings?.workerId ? (
                  <button
                    type="button"
                    onClick={() => handleCopy(workerSettings.workerId, 'workerId')}
                    className="inline-flex items-center gap-1.5 hover:text-console-accent text-console-text transition-colors text-left"
                    title="Click to copy Worker ID"
                  >
                    <span className="truncate max-w-[200px] sm:max-w-[320px]">
                      {workerSettings.workerId}
                    </span>
                    {copiedKey === 'workerId' ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-console-textDim hover:text-console-text shrink-0" />
                    )}
                  </button>
                ) : (
                  'Unavailable'
                ),
              },
              {
                label: 'Config Version',
                mono: true,
                value: workerSettings?.version !== undefined ? `v${workerSettings.version}` : 'Unavailable',
              },
              {
                label: 'Registration',
                value: <StatusBadge status={clusterSettings?.isConfigured ? 'CONFIGURED' : 'SETUP_REQUIRED'} />,
              },
            ]}
          />
        </ConsoleCard>

        {/* Cluster Settings Panel */}
        <ConsoleCard title="Cluster Settings">
          <KeyValueTable
            columns={1}
            items={[
              {
                label: 'Cluster Name',
                value: clusterSettings?.clusterName || <span className="text-console-textDim">Not configured</span>,
              },
              {
                label: 'Cluster ID',
                mono: true,
                value: clusterSettings?.clusterId ? (
                  <button
                    type="button"
                    onClick={() => handleCopy(clusterSettings.clusterId, 'clusterId')}
                    className="inline-flex items-center gap-1.5 hover:text-console-accent text-console-text transition-colors text-left"
                    title="Click to copy Cluster ID"
                  >
                    <span className="truncate max-w-[200px] sm:max-w-[320px]">
                      {clusterSettings.clusterId}
                    </span>
                    {copiedKey === 'clusterId' ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-console-textDim hover:text-console-text shrink-0" />
                    )}
                  </button>
                ) : (
                  <span className="text-console-textDim">Not configured</span>
                ),
              },
              {
                label: 'Coordinator URL',
                mono: true,
                value: clusterSettings?.coordinatorUrl ? (
                  <button
                    type="button"
                    onClick={() => handleCopy(clusterSettings.coordinatorUrl, 'coordinatorUrl')}
                    className="inline-flex items-center gap-1.5 hover:text-console-accent text-console-text transition-colors text-left"
                    title="Click to copy Coordinator URL"
                  >
                    <span className="truncate max-w-[200px] sm:max-w-[320px]">
                      {clusterSettings.coordinatorUrl}
                    </span>
                    {copiedKey === 'coordinatorUrl' ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-console-textDim hover:text-console-text shrink-0" />
                    )}
                  </button>
                ) : (
                  <span className="text-console-textDim">Not configured</span>
                ),
              },
              {
                label: 'Enrollment Status',
                value: (
                  <StatusBadge
                    status={clusterSettings?.isConfigured ? 'CONFIGURED' : 'SETUP_REQUIRED'}
                    label={clusterSettings?.isConfigured ? 'Configured' : 'Setup Required'}
                  />
                ),
              },
            ]}
          />
        </ConsoleCard>
      </div>

      {/* 3. Reset Configuration Section */}
      <ConsoleCard title="Reset Configuration">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-xs font-semibold text-rose-700">
              <AlertTriangle className="w-4 h-4 shrink-0 text-rose-600" />
              <span>Reset Cluster Configuration</span>
            </div>
            <p className="text-xs text-console-textDim leading-relaxed max-w-2xl">
              Clears saved cluster connection parameters and transitions this worker back to initial setup. The unique Worker ID is preserved.
            </p>
          </div>

          <button
            type="button"
            onClick={() => setShowResetModal(true)}
            className="px-3 py-1.5 text-xs font-medium text-rose-700 hover:bg-rose-50 bg-white border border-rose-300 rounded-sm transition-colors shrink-0"
          >
            Reset Configuration
          </button>
        </div>
      </ConsoleCard>

      {/* 4. Reset Confirmation Modal */}
      <ConfirmModal
        isOpen={showResetModal}
        title="Reset Settings"
        message="Reset worker settings to their default values? This will clear the cluster connection and require enrollment setup again."
        confirmLabel={resetting ? 'Resetting...' : 'Reset to Defaults'}
        variant="danger"
        onConfirm={handleConfirmReset}
        onCancel={() => setShowResetModal(false)}
      />
    </div>
  );
}
