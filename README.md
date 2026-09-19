# Distributed Cluster Compute

## Purpose
Distributed Cluster Compute is a platform designed around a **Coordinator–Worker architecture**. The long-term goal is to make multiple ordinary PCs behave like **one logical computing system**, allowing workloads to be distributed seamlessly across machines.

## Architecture
The platform operates as a distributed system with a central control plane and multiple compute nodes communicating over real-time persistent channels.

```text
                    LOGICAL COMPUTE CLUSTER

                    ┌──────────────────┐
                    │    Coordinator   │
                    │   (Control Plane)│
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
          ┌────────┐     ┌────────┐     ┌────────┐
          │ Worker │     │ Worker │     │ Worker │
          │(Node)  │     │(Node)  │     │(Node)  │
          │ 4 CPU  │     │ 4 CPU  │     │ 16 GB  │
          │ 8 GB   │     │ 8 GB   │     │        │
          └────────┘     └────────┘     └────────┘
```

The system features dual frontends targeting different scopes:
- **`frontend/` (Coordinator Dashboard)**: The cluster-wide UI showing aggregated resources, job status, and overall health.
- **`worker-agent/ui/` (Worker Agent Dashboard)**: A local node dashboard for monitoring individual worker connectivity, system metrics, and execution diagnostics.

## Core Modules
- **[Coordinator](./coordinator/README.md)**: The control plane orchestrating worker registration, resource aggregation, and job scheduling.
- **[Worker Agent](./worker-agent/README.md)**: A lightweight daemon running on compute nodes to supply physical hardware resources and execute tasks.
- **[Shared](./shared/README.md)**: A Java library containing cross-cutting domain models, STOMP message envelopes, and protocol types to guarantee strict type safety over network boundaries.
- **[Frontend](./frontend/README.md)**: A React/Vite dashboard connecting to the Coordinator's REST/STOMP interfaces.
- **[Worker Agent UI](./worker-agent/ui/README.md)**: An embedded React/Vite UI served locally by each Worker Agent.

## System Communication
- **REST APIs**: Used by the frontends for data querying and job submission.
- **WebSocket (STOMP)**: 
  - **Coordinator ↔ Worker**: Persistent real-time channel for `REGISTER`, `HEARTBEAT`, and task messages.
  - **Coordinator ↔ Frontend**: Live streaming of cluster events and node availability.
- **Shared Protocol**: All STOMP traffic is wrapped in a standardized `MessageEnvelope` defined in the shared module.

## Worker Lifecycle
Workers transition through deterministic states managed locally and tracked globally by the Coordinator:
1. `STARTING` / `INITIALIZING`
2. `SETUP_REQUIRED` (if missing credentials)
3. `CONFIGURED`
4. Connection States: `CONNECTING` → `REGISTERING` → `ONLINE`
5. Execution States: `OFFLINE` → `IDLE` ↔ `BUSY`

## Job / Task Execution
The task pipeline operates as follows:
1. **Job Creation**: A client submits a Job (e.g. via REST).
2. **Job Partitioning**: The `JobService` uses a `JobPartitioner` to split the workload into smaller, discrete `Task` entities.
3. **Scheduling**: The `SchedulerService` matches unassigned Tasks to `ONLINE` Workers with sufficient resources.
4. **Worker Assignment**: A `TASK_ASSIGN` STOMP message is dispatched.
5. **Worker Task Handler**: The Worker's `TaskMessageHandler` receives it and looks up the appropriate `TaskHandler` from the `TaskHandlerRegistry`.
6. **Task Execution**: The `TaskExecutor` runs the workload.
7. **Result/Status**: The Worker returns `TASK_STATUS` updates back to the Coordinator, updating the global Job state.

## Resource Management
The Worker Agent monitors underlying physical hardware using OSHI.
- **Metrics Collected**: CPU usage/limits, RAM availability, Disk usage, Network I/O.
- **Resource Aggregation**: The Coordinator tracks the total vs. reserved CPU/RAM across the logical cluster.
- **Scheduling Reservation**: The Coordinator's `ResourceReservationService` ensures tasks are only assigned to nodes with adequate unreserved CPU/RAM.

## Security
- **Coordinator ↔ Worker**: Secured via Basic Authentication during the WebSocket HTTP handshake (`api-key`).
- **REST API**: Standard basic authentication bounds endpoints serving the Dashboard.

## APIs
Important REST endpoints exposed by the Coordinator:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/workers` | Retrieve list of registered workers and statuses |
| `GET` | `/api/workers/{id}` | Detailed metrics for a specific worker |
| `GET` | `/api/cluster/resources` | Aggregated total CPU cores and RAM across the cluster |
| `POST`| `/api/cluster/join-code/rotate`| Rotate the active registration code for new workers |
| `POST`| `/api/jobs` | Submit a new distributed compute job |
| `GET` | `/api/jobs/{id}` | Poll the status of a Job |
| `GET` | `/api/jobs/{id}/tasks` | Retrieve partition-level Task statuses |

## Build and Run

### 1. Build Backend
From the root directory, build all Maven modules:
```bash
mvn clean install -DskipTests
```

### 2. Start Coordinator
```bash
cd coordinator
mvn spring-boot:run
```

### 3. Start Worker Agent
The Worker UI is embedded via Maven and will be served on port `8081` (default).
```bash
cd worker-agent
mvn spring-boot:run
```

### 4. Start Coordinator Frontend (Dev Mode)
```bash
cd frontend
npm install
npm run dev
```

### 5. Start Worker Agent UI (Dev Mode)
```bash
cd worker-agent/ui
npm install
npm run dev
```

## Documentation Hierarchy
- [Coordinator](./coordinator/README.md)
  - [Controller API](./coordinator/src/main/java/com/cluster/coordinator/controller/README.md)
  - [Service Layer](./coordinator/src/main/java/com/cluster/coordinator/service/README.md)
  - [Job Partitioning](./coordinator/src/main/java/com/cluster/coordinator/service/partitioner/README.md)
  - [WebSocket Handlers](./coordinator/src/main/java/com/cluster/coordinator/websocket/README.md)
- [Worker Agent](./worker-agent/README.md)
  - [Local API](./worker-agent/src/main/java/com/cluster/worker/api/README.md)
  - [Communication](./worker-agent/src/main/java/com/cluster/worker/communication/README.md)
  - [Execution Engine](./worker-agent/src/main/java/com/cluster/worker/execution/README.md)
  - [Hardware Monitoring](./worker-agent/src/main/java/com/cluster/worker/monitoring/README.md)
  - [Task Management](./worker-agent/src/main/java/com/cluster/worker/task/README.md)
- [Shared Protocol](./shared/README.md)
- [Cluster Dashboard (Frontend)](./frontend/README.md)
- [Worker Agent UI](./worker-agent/ui/README.md)
