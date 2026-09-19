// ─── WebSocket Message Types ─────────────────────────────────────────────────

/**
 * Events pushed by the Coordinator over WebSocket.
 * These align with the backend's WsMessageType enum.
 */
export type WsEventType =
  | "WORKER_CONNECTED"
  | "WORKER_DISCONNECTED"
  | "HEARTBEAT_UPDATE"
  | "RESOURCE_UPDATE"
  | "CLUSTER_UPDATE"
  | "CONFIGURATION_CHANGED";

/**
 * Envelope for every WebSocket message received from the Coordinator.
 * The payload shape depends on the event type.
 */
export interface WsMessage<T = unknown> {
  type: WsEventType;
  payload: T;
  timestamp: string; // ISO 8601
}

// ─── Connection Status ───────────────────────────────────────────────────────

/**
 * Lifecycle states of the WebSocket connection.
 * Maps to the state machine in websocketService.ts.
 */
export type ConnectionStatus =
  "CONNECTING" | "CONNECTED" | "DISCONNECTED" | "RECONNECTING" | "FAILED";

// ─── API Error ───────────────────────────────────────────────────────────────

/**
 * Normalized error thrown by the API client.
 * Wraps HTTP errors into a consistent shape for error handling in hooks.
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly statusText: string;

  constructor(status: number, statusText: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.statusText = statusText;
  }
}

// ─── React Query Key Factories ───────────────────────────────────────────────
// Centralized so invalidations are always consistent.

export const queryKeys = {
  cluster: ["cluster"] as const,
  resources: ["cluster", "resources"] as const,
  workers: ["workers"] as const,
  worker: (id: string) => ["workers", id] as const,
  events: ["events"] as const,
  settings: ["settings"] as const,
};
