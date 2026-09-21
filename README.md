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
- **`coordinator/ui/` (Coordinator Dashboard)**: The cluster-wide UI showing aggregated resources, job status, and overall health.
- **`worker-agent/ui/` (Worker Agent Dashboard)**: A local node dashboard for monitoring individual worker connectivity, system metrics, and execution diagnostics.

## Core Modules
- **[Coordinator](./coordinator/README.md)**: The control plane orchestrating worker registration, resource aggregation, and job scheduling.
- **[Worker Agent](./worker-agent/README.md)**: A lightweight daemon running on compute nodes to supply physical hardware resources and execute tasks.
- **[Shared](./shared/README.md)**: A Java library containing cross-cutting domain models, STOMP message envelopes, and protocol types to guarantee strict type safety over network boundaries.
- **[Coordinator UI](./coordinator/ui/README.md)**: An embedded React/Vite dashboard connecting to the Coordinator's REST/STOMP interfaces.
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

## Operations Guide & Multi-PC Demo Runbook

This guide explains how to run a genuine multi-machine LAN demonstration of the Distributed Cluster Compute platform. The platform is designed to discover your local LAN IPv4 address automatically and assign dynamic available ports, removing any hardcoded `localhost` or `8080` restrictions.

### 1. Build the Entire Project
From the root directory, build all Maven modules (this compiles both Java and Vite/React frontends):
```bash
mvn clean install -DskipTests
```

### 2. Prepare the Coordinator (PC A)
This machine will host the Control Plane and expose the web UI to the LAN.

**On PC A (e.g., Windows):**
1. **Firewall Configuration (Windows):** You must allow Java to accept inbound connections.
   - Open **Windows Defender Firewall with Advanced Security**.
   - Create a **New Inbound Rule** -> **Program** -> Browse to your `java.exe` and `javaw.exe` (inside your JDK `bin/` folder).
   - Alternatively, create a **Port Rule** for the specific port (but since the port is dynamically assigned by default, allowing the Java executable is easier for testing).
   - Ensure the rule applies to **Private** networks.
2. **Start Coordinator:**
   ```bash
   cd coordinator
   mvn spring-boot:run
   ```
3. **Verify Startup:** Look at the startup logs. You should see messages indicating the `detected LAN IP` and the dynamically allocated HTTP and WebSocket endpoints (e.g., `192.168.1.x:54321`).
4. **Open UI:** Open the Coordinator UI in your browser using the URL printed in the logs. Navigate to the **Enrollment** page and copy the Worker Enrollment Link.

### 3. Prepare the Worker (PC B)
This machine will provide computing power to the cluster.

**On PC B (e.g., Linux/Arch or another Windows PC):**
1. **Start Worker Agent:**
   ```bash
   cd worker-agent
   mvn spring-boot:run
   ```
2. **Connect to Worker UI:** Open the Worker UI on PC B (usually `http://localhost:8081` or the dynamic port printed in the Worker's logs).
3. **Enroll:** The Worker will initially be in a `SETUP_REQUIRED` state. Paste the Enrollment Link (copied from PC A) into the setup form. The link will look like:
   `http://<PC-A-LAN-IP>:<ACTUAL-PORT>?token=<TOKEN>`
4. **Verify Connection:** The Worker will persist the exact base URL of the Coordinator, derive the STOMP WebSocket URL (`ws://.../ws/coordinator`), and transition to `ONLINE`. Check PC A's Coordinator UI; the worker should now appear in the Dashboard.

### 4. Job Submission & Validation
1. **Submit `SUM_RANGE` Job:** From the Coordinator UI, submit a `SUM_RANGE` job. You will see the job partition into tasks, route to the Worker over WebSockets, compute, and aggregate the result back on the Coordinator.
2. **Submit `ML_INFERENCE` Job:** Submit an ML job. The Worker will dynamically load the ONNX model from its packaged JAR classpath and execute the inference task without requiring external file paths.
3. **Scale Up:** Repeat Step 3 on additional physical or virtual machines on the same LAN to build a larger cluster. Submit a sufficiently large workload to observe distributed scheduling across multiple nodes.
4. **Resilience Test:** Restart the Coordinator or Worker. The Worker's connection manager will gracefully disconnect, begin exponential backoff retries, and reconnect once the Coordinator is available again.

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
- [Cluster Dashboard (Coordinator UI)](./coordinator/ui/README.md)
- [Worker Agent UI](./worker-agent/ui/README.md)
