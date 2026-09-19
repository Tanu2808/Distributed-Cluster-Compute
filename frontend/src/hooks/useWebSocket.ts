import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { wsService } from "../services/websocketService";
import { useClusterStore } from "../state/clusterStore";
import { queryKeys } from "../types/api";
import { config } from "../utils/config";
import type { ConnectionStatus, WsMessage } from "../types/api";

/**
 * Initializes the WebSocket connection at the app root level.
 *
 * - Connects on mount, disconnects on unmount.
 * - Syncs connection status into the Zustand store.
 * - On data-changing events (RESOURCE_UPDATE, CLUSTER_UPDATE, HEARTBEAT_UPDATE),
 *   invalidates the relevant React Query caches so UI re-renders automatically.
 *
 * Mount this hook ONCE in App.tsx or a top-level component.
 * In mock mode this hook is a no-op.
 */
export function useWebSocket(): void {
  const queryClient = useQueryClient();
  const setConnectionStatus = useClusterStore((s) => s.setConnectionStatus);
  const setLastWsEvent = useClusterStore((s) => s.setLastWsEvent);

  useEffect(() => {
    if (config.USE_MOCK_API) return;

    // Connect on mount
    wsService.connect();

    // Sync status into Zustand store
    const unsubStatus = wsService.onStatusChange((status: ConnectionStatus) => {
      setConnectionStatus(status);
    });

    // React to incoming messages
    const unsubMessages = wsService.onMessage((message: WsMessage) => {
      setLastWsEvent(message);

      switch (message.type) {
        case "CLUSTER_UPDATE":
          queryClient.invalidateQueries({ queryKey: queryKeys.cluster });
          break;

        case "RESOURCE_UPDATE":
          queryClient.invalidateQueries({ queryKey: queryKeys.cluster });
          queryClient.invalidateQueries({ queryKey: queryKeys.workers });
          break;

        case "HEARTBEAT_UPDATE":
          queryClient.invalidateQueries({ queryKey: queryKeys.workers });
          break;

        case "WORKER_CONNECTED":
        case "WORKER_DISCONNECTED":
          queryClient.invalidateQueries({ queryKey: queryKeys.workers });
          queryClient.invalidateQueries({ queryKey: queryKeys.cluster });
          queryClient.invalidateQueries({ queryKey: queryKeys.events });
          break;

        case "CONFIGURATION_CHANGED":
          queryClient.invalidateQueries({ queryKey: queryKeys.settings });
          queryClient.invalidateQueries({ queryKey: queryKeys.events });
          break;

        default:
          break;
      }
    });

    return () => {
      unsubStatus();
      unsubMessages();
      wsService.disconnect();
    };
  }, [queryClient, setConnectionStatus, setLastWsEvent]);
}

/**
 * Returns the current WebSocket connection status from the Zustand store.
 * Components use this to render the connection indicator in the TopBar.
 */
export function useConnectionStatus(): ConnectionStatus {
  return useClusterStore((s) => s.connectionStatus);
}
