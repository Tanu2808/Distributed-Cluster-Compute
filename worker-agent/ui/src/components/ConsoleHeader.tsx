import React, { useState } from 'react';
import { Server, Copy, Check } from 'lucide-react';
import { StatusBadge } from './StatusBadge';
import type { WorkerStatusResponse, WorkerInfoResponse } from '../types';

interface ConsoleHeaderProps {
  status: WorkerStatusResponse | null;
  info: WorkerInfoResponse | null;
}

export const ConsoleHeader: React.FC<ConsoleHeaderProps> = ({ status, info }) => {
  const [copied, setCopied] = useState(false);

  const handleCopyId = () => {
    if (!info?.workerId) return;
    navigator.clipboard.writeText(info.workerId);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  const getStatusString = () => {
    if (status?.lifecycleState === 'SETUP_REQUIRED') return 'SETUP_REQUIRED';
    if (status?.connectionState) return status.connectionState;
    return status?.status || 'UNKNOWN';
  };

  return (
    <header className="h-11 bg-console-topbar border-b border-console-border px-4 flex items-center justify-between select-none shrink-0 z-20">
      {/* Brand & Console Identity */}
      <div className="flex items-center gap-2.5">
        <div className="w-6 h-6 rounded-sm bg-console-accent flex items-center justify-center text-white shrink-0 shadow-sm">
          <Server className="w-3.5 h-3.5" />
        </div>
        <div className="flex items-baseline gap-1.5">
          <span className="font-semibold text-xs text-console-text tracking-tight">
            Distributed Cluster Compute
          </span>
          <span className="text-console-textDim text-xs font-light">/</span>
          <span className="text-xs text-console-textMuted font-medium">
            Worker Console
          </span>
        </div>
      </div>

      {/* Instance Metadata & Global Status */}
      <div className="flex items-center gap-3">
        {info?.workerId && (
          <button
            onClick={handleCopyId}
            className="flex items-center gap-1.5 px-2 py-0.5 bg-console-card border border-console-border hover:border-slate-400 rounded-sm text-xs font-mono text-console-text hover:text-console-accent transition-colors"
            title="Click to copy Worker ID"
          >
            <span className="text-console-textDim text-[10px]">ID:</span>
            <span className="max-w-[120px] md:max-w-[200px] truncate">{info.workerId}</span>
            {copied ? (
              <Check className="w-3 h-3 text-emerald-600 shrink-0" />
            ) : (
              <Copy className="w-3 h-3 text-console-textDim shrink-0" />
            )}
          </button>
        )}

        <StatusBadge status={getStatusString()} />
      </div>
    </header>
  );
};
