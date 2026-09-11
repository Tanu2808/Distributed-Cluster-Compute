import { config } from '../utils/config';
import type { WsMessage, ConnectionStatus } from '../types/api';

// ─── Listener Types ───────────────────────────────────────────────────────────

type MessageListener = (message: WsMessage) => void;
type StatusListener = (status: ConnectionStatus) => void;

// ─── WebSocket Service ────────────────────────────────────────────────────────

/**
 * Singleton WebSocket service for the cluster application.
 *
 * Responsibilities:
 *  - Maintains one WebSocket connection to the Coordinator
 *  - Automatically reconnects with exponential backoff on disconnect
 *  - Emits typed events to registered listeners
 *  - Is a no-op when USE_MOCK_API is true
 *
 * Usage:
 *   wsService.connect();
 *   wsService.on('message', handler);
 *   wsService.onStatusChange(handler);
 *   wsService.disconnect();
 */
class WebSocketService {
  private ws: WebSocket | null = null;
  private status: ConnectionStatus = 'DISCONNECTED';
  private reconnectAttempts = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly MAX_RETRIES = 5;
  private readonly BASE_DELAY_MS = 1_000;
  private readonly MAX_DELAY_MS = 30_000;

  private messageListeners = new Set<MessageListener>();
  private statusListeners = new Set<StatusListener>();

  // ── Public API ────────────────────────────────────────────────────────────

  /** Start the WebSocket connection. Safe to call multiple times. */
  connect(): void {
    if (config.USE_MOCK_API) return;
    if (this.ws?.readyState === WebSocket.OPEN) return;
    this._openConnection();
  }

  /** Permanently close the connection and stop reconnecting. */
  disconnect(): void {
    this._clearReconnectTimer();
    this.reconnectAttempts = 0;
    if (this.ws) {
      // Remove handlers before closing to prevent triggering reconnect
      this.ws.onclose = null;
      this.ws.onerror = null;
      this.ws.close();
      this.ws = null;
    }
    this._setStatus('DISCONNECTED');
  }

  /** Returns the current connection status. */
  getStatus(): ConnectionStatus {
    return this.status;
  }

  /** Register a listener for all incoming WebSocket messages. */
  onMessage(listener: MessageListener): () => void {
    this.messageListeners.add(listener);
    return () => this.messageListeners.delete(listener);
  }

  /** Register a listener for connection status changes. */
  onStatusChange(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }

  // ── Private Helpers ───────────────────────────────────────────────────────

  private _openConnection(): void {
    this._setStatus(this.reconnectAttempts === 0 ? 'CONNECTING' : 'RECONNECTING');

    try {
      this.ws = new WebSocket(config.WS_URL);
    } catch (err) {
      console.error('[WS] Failed to create WebSocket:', err);
      this._scheduleReconnect();
      return;
    }

    this.ws.onopen = () => {
      console.info('[WS] Connected to', config.WS_URL);
      this.reconnectAttempts = 0;
      this._setStatus('CONNECTED');
    };

    this.ws.onmessage = (event: MessageEvent) => {
      try {
        const message = JSON.parse(event.data) as WsMessage;
        this._emit(message);
      } catch (err) {
        console.warn('[WS] Failed to parse message:', event.data, err);
      }
    };

    this.ws.onclose = (event: CloseEvent) => {
      console.warn(`[WS] Closed (code=${event.code}, clean=${event.wasClean})`);
      this.ws = null;
      this._scheduleReconnect();
    };

    this.ws.onerror = (event: Event) => {
      console.error('[WS] Error:', event);
      // onclose will fire after onerror; reconnect is handled there
    };
  }

  private _scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.MAX_RETRIES) {
      console.error('[WS] Max reconnect attempts reached. Giving up.');
      this._setStatus('FAILED');
      return;
    }

    const delay = Math.min(
      this.BASE_DELAY_MS * 2 ** this.reconnectAttempts,
      this.MAX_DELAY_MS,
    );
    this.reconnectAttempts++;
    console.info(`[WS] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.MAX_RETRIES})`);
    this._setStatus('RECONNECTING');

    this.reconnectTimer = setTimeout(() => {
      this._openConnection();
    }, delay);
  }

  private _clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  private _setStatus(status: ConnectionStatus): void {
    if (this.status === status) return;
    this.status = status;
    this.statusListeners.forEach(fn => fn(status));
  }

  private _emit(message: WsMessage): void {
    this.messageListeners.forEach(fn => fn(message));
  }
}

// ─── Singleton Export ─────────────────────────────────────────────────────────

/** App-wide singleton WebSocket service instance. */
export const wsService = new WebSocketService();
