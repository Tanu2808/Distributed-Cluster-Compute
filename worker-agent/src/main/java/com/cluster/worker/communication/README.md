# Worker Agent Communication

## Purpose
The `communication` package manages the resilient STOMP WebSocket connection to the upstream Coordinator. It guarantees that the worker maintains its connection, recovers from network partitions, and processes incoming cluster messages reliably.

## Responsibilities
- Connecting to the Coordinator's `/ws/coordinator` endpoint using Basic Authentication.
- Maintaining connection state and triggering reconnection logic with exponential backoff on failure.
- Routing incoming STOMP payloads (like `TASK_ASSIGN`) to the appropriate local handlers.
- Broadcasting `HEARTBEAT` and `RESOURCE_UPDATE` messages upstream.

## Architecture / Flow
When the Worker Agent boots and validates its configuration, it triggers `WebSocketConnectionManager.connect()`. The manager negotiates the WebSocket handshake using the node's API key. Once connected, `TaskMessageHandler` listens on the node's specific STOMP destination (`/topic/worker.{id}.tasks`) to receive instructions.

## Key Files

| File | Responsibility |
|------|----------------|
| `WebSocketConnectionManager.java` | Thread-safe manager handling the STOMP session lifecycle, retry loops, and connection tracking. |
| `TaskMessageHandler.java` | Spring `@Controller` utilizing `@MessageMapping` to intercept incoming tasks and pass them to the `TaskService`. |

## Integration
The `WebSocketConnectionManager` is heavily relied upon by `HeartbeatService` and `WorkerLifecycleService` to broadcast updates. `TaskMessageHandler` bridges the network layer to the `task` package for execution.
