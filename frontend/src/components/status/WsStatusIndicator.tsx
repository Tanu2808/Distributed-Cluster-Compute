import { useConnectionStatus } from '../../hooks/useWebSocket';
import { config } from '../../utils/config';
import type { ConnectionStatus } from '../../types/api';

const statusConfig: Record<ConnectionStatus, { dot: string; label: string; pulse: boolean }> = {
  CONNECTING:   { dot: 'bg-amber-400',  label: 'Connecting…',  pulse: true  },
  CONNECTED:    { dot: 'bg-green-400',  label: 'Live',         pulse: true  },
  DISCONNECTED: { dot: 'bg-slate-500',  label: 'Disconnected', pulse: false },
  RECONNECTING: { dot: 'bg-amber-400',  label: 'Reconnecting', pulse: true  },
  FAILED:       { dot: 'bg-red-500',    label: 'WS Failed',    pulse: false },
};

/**
 * WebSocket connection status indicator shown in the TopBar.
 * Hidden entirely in mock mode (no backend connection).
 */
export function WsStatusIndicator() {
  const status = useConnectionStatus();

  // In mock mode there is no real WebSocket — nothing to show
  if (config.USE_MOCK_API) return null;

  const { dot, label, pulse } = statusConfig[status];

  return (
    <div className="flex items-center gap-1.5 text-xs text-slate-500">
      <span className={`w-1.5 h-1.5 rounded-full ${dot} ${pulse ? 'animate-pulse' : ''}`} />
      <span className="hidden sm:inline">{label}</span>
    </div>
  );
}
