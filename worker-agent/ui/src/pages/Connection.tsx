import { useState } from 'react';
import { RefreshCw, Copy, Check } from 'lucide-react';

import { useConnectionStatus } from '../hooks/useConnectionStatus';
import { useWorkerStatus } from '../hooks/useWorkerStatus';
import {
  ConsoleCard,
  KeyValueTable,
  StatusBadge,
  AlertBanner,
  Skeleton,
} from '../components';

function formatDateTime(isoString?: string | null): string {
  if (!isoString) return 'Not available';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return 'Not available';
    return d.toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return 'Not available';
  }
}

function getHeartbeatHealth(isoString?: string | null): { label: string; status: string } {
  if (!isoString) return { label: 'Not available', status: 'UNKNOWN' };
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return { label: 'Not available', status: 'UNKNOWN' };
    const diffMs = Date.now() - d.getTime();
    if (diffMs < 20000) {
      return { label: 'Healthy', status: 'ONLINE' };
    }
    return { label: 'Stale', status: 'BUSY' };
  } catch {
    return { label: 'Not available', status: 'UNKNOWN' };
  }
}

export default function Connection() {
  const { connection, loading, refreshing, error, refetch } = useConnectionStatus(4000);
  const { status } = useWorkerStatus(4000);

  const [copiedUrl, setCopiedUrl] = useState(false);

  const handleCopyUrl = (url?: string) => {
    if (!url) return;
    navigator.clipboard.writeText(url);
    setCopiedUrl(true);
    setTimeout(() => setCopiedUrl(false), 1800);
  };

  const heartbeatHealth = getHeartbeatHealth(connection?.lastSuccessfulHeartbeat);
  const connectionState = connection?.connectionState || 'UNKNOWN';

  // Initial Loading Skeleton
  if (loading && !connection) {
    return (
      <div className="space-y-4 max-w-7xl">
        <div className="flex items-center justify-between pb-2 border-b border-console-border">
          <div className="space-y-1">
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-4 w-64" />
          </div>
          <Skeleton className="h-7 w-20" />
        </div>
        <Skeleton className="h-28 w-full" />
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Skeleton className="h-56" />
          <Skeleton className="h-56" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4 max-w-7xl">
      {/* 1. Page Header */}
      <div className="flex items-start sm:items-center justify-between gap-4 pb-2.5 border-b border-console-border">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-base font-semibold tracking-tight text-console-text">
              Connection
            </h1>
            <StatusBadge status={connectionState} />
          </div>
          <p className="text-[11px] text-console-textDim mt-0.5">
            Coordinator connectivity and worker communication status
          </p>
        </div>

        <button
          type="button"
          onClick={() => refetch()}
          disabled={refreshing}
          className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-console-text hover:bg-slate-50 bg-white border border-console-border hover:border-slate-400 rounded-sm transition-colors disabled:opacity-50 shrink-0"
          title="Refresh connection status"
        >
          <RefreshCw className={`w-3.5 h-3.5 text-console-textDim ${refreshing ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Error Alert with Retry */}
      {error && (
        <AlertBanner
          type="error"
          message={
            <div className="flex items-center justify-between gap-4">
              <span>Unable to load connection information.</span>
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

      {/* 2. Connection Status Primary Panel */}
      <ConsoleCard title="Connection Status">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="p-3 bg-console-subtle border border-console-borderSubtle rounded-sm">
            <span className="text-[10px] font-medium uppercase tracking-wider text-console-textDim block mb-1">
              WebSocket Status
            </span>
            <StatusBadge status={connectionState} size="md" />
          </div>

          <div className="p-3 bg-console-subtle border border-console-borderSubtle rounded-sm">
            <span className="text-[10px] font-medium uppercase tracking-wider text-console-textDim block mb-1">
              Worker Lifecycle
            </span>
            <StatusBadge status={status?.lifecycleState || 'UNKNOWN'} size="md" />
          </div>

          <div className="p-3 bg-console-subtle border border-console-borderSubtle rounded-sm">
            <span className="text-[10px] font-medium uppercase tracking-wider text-console-textDim block mb-1">
              Execution State
            </span>
            <StatusBadge status={status?.executionState || 'IDLE'} size="md" />
          </div>

          <div className="p-3 bg-console-subtle border border-console-borderSubtle rounded-sm">
            <span className="text-[10px] font-medium uppercase tracking-wider text-console-textDim block mb-1">
              Coordinator Endpoint
            </span>
            <div className="font-mono text-xs text-console-text truncate mt-1" title={connection?.coordinatorUrl || undefined}>
              {connection?.coordinatorUrl || 'Not configured'}
            </div>
          </div>
        </div>
      </ConsoleCard>

      {/* 3. Two-Column Information Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Left Column: Coordinator Details */}
        <div className="space-y-4">
          <ConsoleCard title="Coordinator">
            <KeyValueTable
              columns={1}
              items={[
                {
                  label: 'Coordinator URL',
                  mono: true,
                  value: connection?.coordinatorUrl ? (
                    <button
                      type="button"
                      onClick={() => handleCopyUrl(connection.coordinatorUrl)}
                      className="inline-flex items-center gap-1.5 hover:text-console-accent text-console-text transition-colors text-left"
                      title="Click to copy URL"
                    >
                      <span className="truncate max-w-[200px] sm:max-w-[320px]">
                        {connection.coordinatorUrl}
                      </span>
                      {copiedUrl ? (
                        <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                      ) : (
                        <Copy className="w-3.5 h-3.5 text-console-textDim hover:text-console-text shrink-0" />
                      )}
                    </button>
                  ) : (
                    'Not available'
                  ),
                },
                {
                  label: 'Connection State',
                  value: <StatusBadge status={connectionState} />,
                },
                {
                  label: 'Connected Since',
                  mono: true,
                  value: formatDateTime(connection?.connectedSince),
                },
                {
                  label: 'Last Message Timestamp',
                  mono: true,
                  value: formatDateTime(connection?.lastMessageTimestamp),
                },
                {
                  label: 'Reconnect Count',
                  mono: true,
                  value: connection?.reconnectCount !== undefined ? String(connection.reconnectCount) : 'Not available',
                },
              ]}
            />
          </ConsoleCard>

          {/* Last Connection Error (Rendered only when error exists) */}
          {connection?.lastConnectionError && (
            <ConsoleCard title="Last Connection Error">
              <div className="space-y-2">
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-sm text-rose-800 font-mono text-[11px] break-words leading-relaxed">
                  {connection.lastConnectionError}
                </div>
                <div className="text-[11px] text-console-textDim">
                  Recorded during the last failed socket handshake or transport timeout.
                </div>
              </div>
            </ConsoleCard>
          )}
        </div>

        {/* Right Column: Communication Health & Worker State */}
        <div className="space-y-4">
          {/* Communication Health */}
          <ConsoleCard title="Communication Health">
            <KeyValueTable
              columns={1}
              items={[
                {
                  label: 'Heartbeat Health',
                  value: (
                    <StatusBadge
                      status={heartbeatHealth.status}
                      label={heartbeatHealth.label}
                    />
                  ),
                },
                {
                  label: 'Last Successful Heartbeat',
                  mono: true,
                  value: formatDateTime(connection?.lastSuccessfulHeartbeat),
                },
                {
                  label: 'Connection Restarts',
                  mono: true,
                  value: connection?.reconnectCount !== undefined ? `${connection.reconnectCount} reconnects` : 'Not available',
                },
              ]}
            />
          </ConsoleCard>

          {/* Worker State */}
          <ConsoleCard title="Worker State">
            <div className="space-y-3">
              <div className="flex items-center justify-between py-1 border-b border-console-borderSubtle text-xs">
                <span className="text-console-textDim font-medium">Lifecycle State</span>
                <StatusBadge status={status?.lifecycleState || 'UNKNOWN'} />
              </div>
              <div className="flex items-center justify-between py-1 border-b border-console-borderSubtle text-xs">
                <span className="text-console-textDim font-medium">Execution State</span>
                <StatusBadge status={status?.executionState || 'IDLE'} />
              </div>
              <div className="flex items-center justify-between py-1 border-b border-console-borderSubtle text-xs">
                <span className="text-console-textDim font-medium">Transport State</span>
                <StatusBadge status={connectionState} />
              </div>
            </div>
          </ConsoleCard>
        </div>
      </div>
    </div>
  );
}
