import { create } from "zustand";
import type { ConnectionStatus, WsMessage } from "../types/api";

// ─── Store Shape ──────────────────────────────────────────────────────────────

interface ClusterStoreState {
  /** Current WebSocket connection status. */
  connectionStatus: ConnectionStatus;

  /** The most recently received WebSocket message (null until first message). */
  lastWsEvent: WsMessage | null;

  // ── Actions ────────────────────────────────────────────────────────────────

  setConnectionStatus: (status: ConnectionStatus) => void;
  setLastWsEvent: (message: WsMessage) => void;
}

// ─── Store ────────────────────────────────────────────────────────────────────

/**
 * Zustand store for real-time cluster state driven by WebSocket events.
 *
 * This store holds:
 *  - Connection status (shown in the TopBar indicator)
 *  - The last WS message (used by hooks to decide when to invalidate React Query cache)
 *
 * Pages read data from React Query caches, not directly from this store.
 * This store is the signal that triggers React Query invalidations.
 */
export const useClusterStore = create<ClusterStoreState>((set) => ({
  connectionStatus: "DISCONNECTED",
  lastWsEvent: null,

  setConnectionStatus: (status) => set({ connectionStatus: status }),
  setLastWsEvent: (message) => set({ lastWsEvent: message }),
}));
