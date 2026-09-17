import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ArrowRight, Check, Copy, RefreshCw, KeyRound, ServerPlus } from 'lucide-react';
import { clusterApi } from '../services/clusterApi';
import { useSettings } from '../hooks/useSettings';
import { useWorkerStatus } from '../hooks/useWorkerStatus';
import {
  ConsoleCard,
  KeyValueTable,
  StatusBadge,
  AlertBanner,
  Skeleton,
} from '../components';

type SetupMode = 'JOIN' | 'CREATE';

export default function Setup() {
  const navigate = useNavigate();
  const { clusterSettings, loading: settingsLoading, refetch: refetchSettings } = useSettings();
  const { status, info } = useWorkerStatus(5000);

  const [mode, setMode] = useState<SetupMode>('JOIN');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Form State
  const [joinCode, setJoinCode] = useState('');
  const [clusterName, setClusterName] = useState('');
  const [isLocal, setIsLocal] = useState(true);

  // Status & Validation State
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ joinCode?: string; clusterName?: string }>({});

  const isConfigured = clusterSettings?.isConfigured || status?.lifecycleState === 'CONFIGURED';

  const handleCopy = (text: string, key: string) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 1800);
  };

  const validateJoin = (): boolean => {
    const errors: { joinCode?: string } = {};
    const cleanCode = joinCode.replace(/[\s-]/g, '');
    if (!joinCode.trim()) {
      errors.joinCode = 'Join code is required.';
    } else if (cleanCode.length !== 16 || !/^[a-zA-Z0-9]{16}$/.test(cleanCode)) {
      errors.joinCode = 'Must be 16 alphanumeric characters (XXXX-XXXX-XXXX-XXXX).';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const validateCreate = (): boolean => {
    const errors: { clusterName?: string } = {};
    if (!clusterName.trim()) {
      errors.clusterName = 'Cluster name is required.';
    } else if (clusterName.trim().length < 3) {
      errors.clusterName = 'Cluster name must be at least 3 characters.';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleJoinSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!validateJoin()) return;

    setSubmitting(true);
    try {
      const response = await clusterApi.joinCluster(joinCode.trim());
      if (response.status === 'SUCCESS') {
        setSuccess('Worker configured successfully.');
        await refetchSettings();
        setTimeout(() => navigate('/home'), 1200);
      } else {
        setError(response.message || 'Unable to complete worker setup.');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to complete worker setup.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!validateCreate()) return;

    setSubmitting(true);
    try {
      await clusterApi.createCluster(clusterName.trim(), isLocal);
      setSuccess('Worker configured successfully.');
      await refetchSettings();
      setTimeout(() => navigate('/home'), 1200);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to complete worker setup.');
    } finally {
      setSubmitting(false);
    }
  };

  // Initial Loading Skeleton
  if (settingsLoading && !clusterSettings) {
    return (
      <div className="space-y-4 max-w-3xl mx-auto">
        <div className="space-y-1 pb-2 border-b border-console-border">
          <Skeleton className="h-6 w-40" />
          <Skeleton className="h-4 w-72" />
        </div>
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  // 1. If Already Configured View
  if (isConfigured) {
    return (
      <div className="space-y-4 max-w-3xl mx-auto">
        {/* Header */}
        <div className="flex items-start sm:items-center justify-between gap-4 pb-2.5 border-b border-console-border">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-base font-semibold tracking-tight text-console-text">
                Worker Setup
              </h1>
              <StatusBadge status="CONFIGURED" label="Worker configured" />
            </div>
            <p className="text-[11px] text-console-textDim mt-0.5">
              This worker node is already enrolled in a cluster
            </p>
          </div>

          <Link
            to="/home"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-white bg-console-accent hover:bg-console-accentHover rounded-sm transition-colors shadow-sm"
          >
            <span>Open Worker Overview</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Configuration Summary Card */}
        <ConsoleCard title="Current Enrollment Configuration">
          <KeyValueTable
            columns={1}
            items={[
              {
                label: 'Worker ID',
                mono: true,
                value: info?.workerId ? (
                  <button
                    type="button"
                    onClick={() => handleCopy(info.workerId, 'workerId')}
                    className="inline-flex items-center gap-1.5 hover:text-console-accent text-console-text transition-colors text-left"
                    title="Click to copy Worker ID"
                  >
                    <span className="truncate max-w-[240px] sm:max-w-[400px]">
                      {info.workerId}
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
                label: 'Cluster Name',
                value: clusterSettings?.clusterName || 'Not configured',
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
                    <span className="truncate max-w-[240px] sm:max-w-[400px]">
                      {clusterSettings.clusterId}
                    </span>
                    {copiedKey === 'clusterId' ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-console-textDim hover:text-console-text shrink-0" />
                    )}
                  </button>
                ) : (
                  'Not configured'
                ),
              },
              {
                label: 'Coordinator URL',
                mono: true,
                value: clusterSettings?.coordinatorUrl || 'Not configured',
              },
              {
                label: 'Enrollment Status',
                value: <StatusBadge status="CONFIGURED" label="Configured" />,
              },
            ]}
          />

          <div className="mt-4 pt-3 border-t border-console-borderSubtle flex items-center justify-between text-xs text-console-textDim">
            <span>To modify cluster parameters or disconnect, visit settings.</span>
            <Link
              to="/settings"
              className="text-console-accent hover:text-console-accentHover font-medium inline-flex items-center gap-1"
            >
              <span>Manage in Settings</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </ConsoleCard>
      </div>
    );
  }

  // 2. Unconfigured Worker Setup View
  return (
    <div className="space-y-4 max-w-2xl mx-auto">
      {/* Page Header */}
      <div className="flex items-start sm:items-center justify-between gap-4 pb-2.5 border-b border-console-border">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-base font-semibold tracking-tight text-console-text">
              Worker Setup
            </h1>
            <StatusBadge status="SETUP_REQUIRED" label="Setup required" />
          </div>
          <p className="text-[11px] text-console-textDim mt-0.5">
            Configure this worker for cluster operation
          </p>
        </div>
      </div>

      {/* Notifications */}
      {success && (
        <AlertBanner type="success" message={success} />
      )}

      {error && (
        <AlertBanner
          type="error"
          message={
            <div className="flex items-center justify-between gap-4">
              <span>{error}</span>
              <button
                type="button"
                onClick={() => setError(null)}
                className="underline hover:text-console-accent text-rose-800 shrink-0 font-sans font-medium"
              >
                Dismiss
              </button>
            </div>
          }
        />
      )}

      {/* Mode Selector Segmented Control */}
      <div className="grid grid-cols-2 p-1 bg-slate-100 border border-console-border rounded-sm gap-1">
        <button
          type="button"
          onClick={() => {
            setMode('JOIN');
            setError(null);
            setFieldErrors({});
          }}
          disabled={submitting}
          className={`flex items-center justify-center gap-2 py-2 px-3 text-xs font-medium rounded-sm transition-colors ${
            mode === 'JOIN'
              ? 'bg-white text-console-accent border border-slate-300 shadow-sm font-semibold'
              : 'text-console-textMuted hover:text-console-text hover:bg-slate-200/60 border border-transparent'
          }`}
        >
          <KeyRound className="w-3.5 h-3.5" />
          <span>Join Cluster</span>
        </button>

        <button
          type="button"
          onClick={() => {
            setMode('CREATE');
            setError(null);
            setFieldErrors({});
          }}
          disabled={submitting}
          className={`flex items-center justify-center gap-2 py-2 px-3 text-xs font-medium rounded-sm transition-colors ${
            mode === 'CREATE'
              ? 'bg-white text-console-accent border border-slate-300 shadow-sm font-semibold'
              : 'text-console-textMuted hover:text-console-text hover:bg-slate-200/60 border border-transparent'
          }`}
        >
          <ServerPlus className="w-3.5 h-3.5" />
          <span>Create Cluster</span>
        </button>
      </div>

      {/* Configuration Form Panel */}
      {mode === 'JOIN' ? (
        <ConsoleCard
          title="Join Cluster"
          subtitle="Enroll this worker using a cluster join code"
        >
          <form onSubmit={handleJoinSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="joinCode" className="block text-xs font-medium text-console-text">
                Cluster Join Code
              </label>
              <input
                id="joinCode"
                type="text"
                value={joinCode}
                onChange={(e) => {
                  setJoinCode(e.target.value.toUpperCase());
                  if (fieldErrors.joinCode) setFieldErrors({ ...fieldErrors, joinCode: undefined });
                }}
                disabled={submitting}
                placeholder="XXXX-XXXX-XXXX-XXXX"
                className={`w-full px-3 py-2 text-xs font-mono bg-white border rounded-sm text-console-text placeholder:text-console-textDim focus:outline-none transition-colors ${
                  fieldErrors.joinCode
                    ? 'border-rose-500 focus:border-rose-600'
                    : 'border-console-border focus:border-console-accent'
                }`}
              />
              {fieldErrors.joinCode ? (
                <p className="text-[11px] text-rose-700 font-medium">
                  {fieldErrors.joinCode}
                </p>
              ) : (
                <p className="text-[11px] text-console-textDim">
                  16-character alphanumeric authorization code provided by the coordinator.
                </p>
              )}
            </div>

            <div className="pt-3 border-t border-console-border flex items-center justify-end">
              <button
                type="submit"
                disabled={submitting}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-medium text-white bg-console-accent hover:bg-console-accentHover disabled:opacity-50 rounded-sm transition-colors"
              >
                {submitting ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    <span>Connecting...</span>
                  </>
                ) : (
                  <span>Join Cluster</span>
                )}
              </button>
            </div>
          </form>
        </ConsoleCard>
      ) : (
        <ConsoleCard
          title="Create Cluster"
          subtitle="Initialize a new cluster with this node as the first member"
        >
          <form onSubmit={handleCreateSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="clusterName" className="block text-xs font-medium text-console-text">
                Cluster Name
              </label>
              <input
                id="clusterName"
                type="text"
                value={clusterName}
                onChange={(e) => {
                  setClusterName(e.target.value);
                  if (fieldErrors.clusterName) setFieldErrors({ ...fieldErrors, clusterName: undefined });
                }}
                disabled={submitting}
                placeholder="e.g. Production-Compute-Cluster"
                className={`w-full px-3 py-2 text-xs bg-white border rounded-sm text-console-text placeholder:text-console-textDim focus:outline-none transition-colors ${
                  fieldErrors.clusterName
                    ? 'border-rose-500 focus:border-rose-600'
                    : 'border-console-border focus:border-console-accent'
                }`}
              />
              {fieldErrors.clusterName && (
                <p className="text-[11px] text-rose-700 font-medium">
                  {fieldErrors.clusterName}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <span className="block text-xs font-medium text-console-text">
                Coordinator Deployment
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <label
                  className={`flex items-start gap-2.5 p-2.5 border rounded-sm cursor-pointer transition-colors ${
                    isLocal
                      ? 'bg-blue-50/60 border-console-accent'
                      : 'bg-white border-console-border hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="radio"
                    name="coordinatorDeployment"
                    checked={isLocal}
                    onChange={() => setIsLocal(true)}
                    disabled={submitting}
                    className="mt-0.5 text-console-accent focus:ring-console-accent"
                  />
                  <div className="space-y-0.5">
                    <span className="text-xs font-medium text-console-text block">Local Node</span>
                    <span className="text-[11px] text-console-textDim block">
                      Run coordinator locally on this computer
                    </span>
                  </div>
                </label>

                <label
                  className={`flex items-start gap-2.5 p-2.5 border rounded-sm cursor-pointer transition-colors ${
                    !isLocal
                      ? 'bg-blue-50/60 border-console-accent'
                      : 'bg-white border-console-border hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="radio"
                    name="coordinatorDeployment"
                    checked={!isLocal}
                    onChange={() => setIsLocal(false)}
                    disabled={submitting}
                    className="mt-0.5 text-console-accent focus:ring-console-accent"
                  />
                  <div className="space-y-0.5">
                    <span className="text-xs font-medium text-console-text block">Remote Server</span>
                    <span className="text-[11px] text-console-textDim block">
                      Connect to an existing standalone coordinator
                    </span>
                  </div>
                </label>
              </div>
            </div>

            <div className="pt-3 border-t border-console-border flex items-center justify-end">
              <button
                type="submit"
                disabled={submitting}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-medium text-white bg-console-accent hover:bg-console-accentHover disabled:opacity-50 rounded-sm transition-colors"
              >
                {submitting ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    <span>Creating...</span>
                  </>
                ) : (
                  <span>Create Cluster</span>
                )}
              </button>
            </div>
          </form>
        </ConsoleCard>
      )}
    </div>
  );
}
