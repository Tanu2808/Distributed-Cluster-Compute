# Coordinator WebSocket Architecture

## Purpose
The `websocket` package manages the persistent STOMP communication channel between the Coordinator and the distributed Worker Agents. It handles connection lifecycles, authentication, and message broadcasting.

## Responsibilities
- Establishing and authenticating incoming WebSocket HTTP upgrades.
- Maintaining a thread-safe registry of active `WebSocketSession`s.
- Broadcasting cluster events to the Coordinator Dashboard (`frontend/`).

## Architecture / Flow
1. A Worker Agent attempts an HTTP upgrade to `/ws/coordinator`.
2. The `BasicAuthHandshakeInterceptor` extracts the `api-key` and validates it against the cluster configuration.
3. If valid, the STOMP session is established and managed by `ClusterWebSocketHandler`.
4. Incoming STOMP commands are routed by Spring's `@EnableWebSocketMessageBroker` to the `WorkerMessageController`.

## Key Files

| File | Responsibility |
|------|----------------|
| `ClusterWebSocketHandler.java` | Manages raw WebSocket connections and provides a `broadcast()` utility for pushing events to all clients. |
| `BasicAuthHandshakeInterceptor.java` | Validates Basic Auth credentials before allowing the WebSocket upgrade to complete. |
| `WebSocketConfig.java` / `ClusterWebSocketConfig.java` | Spring configuration enabling the STOMP broker and mapping the endpoint paths. |

## Integration
This package acts as the network transport layer for real-time events. It depends on `SecurityConfig` (or local cluster settings) for authentication validation and is used by `SchedulerService` to dispatch `TASK_ASSIGN` payloads downstream to workers.
