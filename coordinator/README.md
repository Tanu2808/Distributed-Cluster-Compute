# Coordinator

## Purpose
The Coordinator acts as the central control plane (Master) of the Distributed Compute Cluster. It orchestrates workers, aggregates their capabilities into a logical cluster view, and manages the execution lifecycle of distributed compute jobs.

## Responsibilities
- **Worker Registration**: Handling new node enrollment and API key validation.
- **Worker Lifecycle**: Tracking STOMP heartbeats to determine worker health and liveness.
- **Resource Aggregation**: Aggregating physical resource metrics (CPU, RAM) across the cluster.
- **Job Management**: Creating and tracking the overall state of distributed Jobs.
- **Task Partitioning**: Splitting logical Jobs into discrete executable Tasks.
- **Scheduling**: Assigning Tasks to available Workers based on physical resource constraints.
- **Task Status/Result Tracking**: Handling updates from Workers as tasks progress.
- **REST APIs**: Exposing cluster state to the Coordinator frontend.
- **WebSocket/STOMP**: Managing real-time persistent connections with active Worker Agents.
- **Security**: Validating Basic Authentication for WebSocket connections.
- **Persistence**: Recording cluster events, worker state, and job history.

## Architecture
The Coordinator is the central hub:
- It communicates downstream with **Worker Agents** over a persistent WebSocket (STOMP) connection using standardized messages from the **Shared** module.
- It communicates upstream with the **Coordinator Frontend** via REST APIs and a dedicated WebSocket channel for live event streaming.

## Job Execution Flow
1. A client submits a Job payload via the REST API (`JobController`).
2. The `JobService` initializes the Job entity.
3. The `JobPartitioner` evaluates the Job and splits it into multiple `Task` entities based on resource requirements.
4. The `SchedulerService` periodically matches unassigned Tasks to `ONLINE` Workers with sufficient CPU and RAM.
5. Once scheduled, a `TASK_ASSIGN` message is dispatched to the chosen Worker via STOMP.
6. The Worker executes the task and streams `TASK_STATUS` messages back to the Coordinator, updating the task and job state.

## REST API
Key endpoints provided by the Coordinator:
- `GET /api/workers` - Lists registered workers.
- `GET /api/workers/{id}` - Details for a specific worker.
- `GET /api/cluster/resources` - Aggregates CPU/RAM across the cluster.
- `POST /api/cluster/join-code/rotate` - Rotates join codes for new worker enrollment.
- `POST /api/jobs` - Submits a new job.
- `GET /api/jobs/{id}` - Checks job status.

## WebSocket
The Coordinator relies on Spring WebSocket (STOMP):
- **Connection Lifecycle**: Managed via `ClusterWebSocketHandler` to track session connects/disconnects.
- **Authentication**: `BasicAuthHandshakeInterceptor` enforces security during the initial HTTP upgrade.
- **Message Routing**: Typed messages like `REGISTER` and `HEARTBEAT` are routed to controllers.

## Key Packages
For more architectural details, refer to the package-level documentation:

| Package | Responsibility | Documentation |
|---------|----------------|---------------|
| `controller` | REST API boundaries and STOMP message controllers. | [Controller README](./src/main/java/com/cluster/coordinator/controller/README.md) |
| `service` | Core business logic, scheduling, and job lifecycle. | [Service README](./src/main/java/com/cluster/coordinator/service/README.md) |
| `service/partitioner` | Strategy for splitting Jobs into Tasks. | [Partitioner README](./src/main/java/com/cluster/coordinator/service/partitioner/README.md) |
| `websocket` | STOMP session handlers and authentication. | [WebSocket README](./src/main/java/com/cluster/coordinator/websocket/README.md) |
