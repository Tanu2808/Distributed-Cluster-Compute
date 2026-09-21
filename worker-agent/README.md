# Worker Agent Overview

## Purpose
The Worker Agent is a lightweight daemon that runs on participating compute nodes (Slaves) within the Distributed Compute Cluster. It supplies computational resources to the cluster, actively monitors local hardware metrics, and executes assigned tasks provided by the Coordinator.

## Architecture
The Worker Agent operates as a client to the central control plane:
Coordinator
↓ (STOMP / WebSocket)
Worker Agent
↓ (Local Dispatch)
Task Execution / Hardware Monitoring

## Worker Lifecycle
The Worker Agent state machine (`WorkerStateManager`) tracks transition across three domains:
- **Lifecycle State**: `STARTING` → `INITIALIZING` → `CONFIGURED`
- **Connection State**: `DISCONNECTED` → `CONNECTING` → `REGISTERING` → `ONLINE`
- **Execution State**: `OFFLINE` → `IDLE` ↔ `BUSY`

## Coordinator Communication
Communication relies on a persistent STOMP WebSocket connection to the Coordinator.
- **Connection Establishment**: Connects using a generated Enrollment Link URL containing the Coordinator address and registration token.
- **Enrollment/Registration**: Connects and enrolls to Coordinator, receiving a runtime credential to finalize the registration, and sends a `REGISTER` message.
- **Heartbeat & Resource Updates**: Periodically broadcasts liveness and OSHI hardware metrics.
- **Reconnect Behavior**: Implements exponential backoff via `WebSocketConnectionManager` when disconnected.

## Task Execution
The Worker Agent handles tasks asynchronously:
1. **Message Reception**: `TaskMessageHandler` receives a `TASK_ASSIGN` payload via STOMP.
2. **Validation**: The Worker validates resource constraints locally.
3. **Handler Selection**: Uses the `TaskHandlerRegistry` to locate the appropriate logic for the task type.
4. **Execution**: The `TaskExecutor` invokes the handler inside a managed thread.
5. **Result/Status**: `TASK_STATUS` updates (Running, Completed, Failed) are streamed back to the Coordinator, along with the final `TASK_RESULT`.

## Resource Monitoring
Hardware monitoring is abstracted through `SystemMetricsProvider`. The primary implementation uses **OSHI** (`OshiSystemMetricsProvider`) to collect:
- CPU core counts and current utilization.
- Total memory and available memory.
- Storage capacity.
- Network I/O.

## REST API
The Worker Agent exposes local HTTP endpoints for diagnostics:
- `GET /api/worker/status` - Basic liveness.
- `GET /api/worker/info` - Hardware capabilities and limits.

## Local UI
The Worker Agent embeds its own local dashboard:
- Located in `worker-agent/ui/`.
- Served natively by Spring Boot.
- Communicates directly with the local Worker Agent's REST API.
- Provides visual diagnostics for connection latency, local metrics, and active tasks.

## Configuration
Important configurable properties in `application.yml`:
- `cluster.worker.id`: Static ID of this node.

## Key Packages
For deep dives into the implementation:

| Package | Responsibility | Documentation |
|---------|----------------|---------------|
| `api` | Local HTTP API boundaries. | [API README](./src/main/java/com/cluster/worker/api/README.md) |
| `communication` | WebSocket lifecycle and STOMP parsing. | [Communication README](./src/main/java/com/cluster/worker/communication/README.md) |
| `execution` | Core thread pool and task runner logic. | [Execution README](./src/main/java/com/cluster/worker/execution/README.md) |
| `monitoring` | OSHI integration and metric polling. | [Monitoring README](./src/main/java/com/cluster/worker/monitoring/README.md) |
| `task` | Handlers, task registry, and task state. | [Task README](./src/main/java/com/cluster/worker/task/README.md) |
