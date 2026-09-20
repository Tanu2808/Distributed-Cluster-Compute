import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";

import { queryKeys } from "../types/api";
import { fetchEvents } from "../api/eventsApi";
import { wsService } from "../services/websocketService";
import type { ClusterEvent } from "../types";
import type { WsMessage } from "../types/api";

/**
 * Returns the cluster event list.
 *
 * Mock mode:  returns mockEvents immediately.
 * Real mode:  fetches GET /api/cluster/events.
 *             Additionally subscribes to live WebSocket events and appends
 *             them to the cached list without a full refetch.
 */
export function useClusterEvents() {
  const queryClient = useQueryClient();

  // Subscribe to incoming WS messages and append new events to the cache
  useEffect(() => {
    const unsubscribe = wsService.onMessage((message: WsMessage) => {
      // Map WS event types that represent cluster events into ClusterEvent shape
      const eventTypes = [
        "WORKER_CONNECTED",
        "WORKER_DISCONNECTED",
        "HEARTBEAT_UPDATE",
        "CLUSTER_UPDATE",
        "CONFIGURATION_CHANGED",
      ];

      if (!eventTypes.includes(message.type)) return;

      // Append the live event to the existing cache without invalidating
      queryClient.setQueryData<ClusterEvent[]>(queryKeys.events, (old = []) => {
        const liveEvent: ClusterEvent = {
          id: `ws-${Date.now()}`,
          type: message.type as ClusterEvent["type"],
          severity: "INFO",
          message: `[Live] ${message.type}`,
          timestamp: message.timestamp,
        };
        return [...old, liveEvent].slice(-100); // keep last 100
      });
    });

    return unsubscribe;
  }, [queryClient]);

  return useQuery<ClusterEvent[], Error>({
    queryKey: queryKeys.events,
    queryFn: fetchEvents,
  });
}
