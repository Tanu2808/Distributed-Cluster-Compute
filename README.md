# Distributed Compute Cluster

A distributed computing platform designed around a **Coordinator–Worker architecture**.

The long-term goal is to make multiple ordinary PCs behave like **one logical computing system**, allowing workloads to be distributed across machines.

## Architecture

The platform consists of three primary layers communicating over real-time persistent channels:

1. **Frontend (UI)**: React 18 + Vite dashboard to visualize the logical cluster.
2. **Coordinator (Master)**: The central Spring Boot control plane that manages the cluster.
3. **Worker Agent (Node)**: A lightweight daemon running on compute nodes to supply resources.

```text
                    LOGICAL COMPUTE CLUSTER

                    ┌──────────────────┐
                    │    Coordinator   │
                    │      Master      │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
          ┌────────┐     ┌────────┐     ┌────────┐
          │ PC-01  │     │ PC-02  │     │ PC-03  │
          │ 4 CPU  │     │ 4 CPU  │     │ 4 CPU  │
          │ 8 GB   │     │ 8 GB   │     │ 16 GB  │
          └────────┘     └────────┘     └────────┘
```

## Implementation Status (What is built till now)

- ✅ **UI / Frontend**: The React UI is fully wired up with React Query, Zustand, and TailwindCSS.
- ✅ **Maven Multi-Module Project**: The backend is organized into `shared`, `coordinator`, and `worker-agent`.
- ✅ **Internal Communication (WebSocket)**: Both Coordinator and Worker use persistent STOMP WebSockets for bidirectional communication.
- ✅ **Strict Type Safety**: Standardized protocol (`MessageEnvelope`, `HeartbeatMessage`, etc.) ensures type safety across the network.
- ✅ **Authentication**: WebSocket endpoints are secured via Basic Auth (`api-key`).
- ✅ **Backend-to-Frontend Push**: Real-time frontend WebSocket (`/ws/cluster`) broadcasts live cluster and worker updates.
- ⏳ **Task Orchestration**: Initial protocol models for executing distributed tasks are built, but the task engine itself is pending.

## How to Configure Connection

The Worker Agent connects to the Coordinator over WebSocket.
In `worker-agent/src/main/resources/application.yml`:
- `worker.coordinator.url`: e.g. `http://localhost:8080`
- `worker.coordinator.api-key`: `admin_secret` (must match Coordinator)
The WebSocket uses STOMP with Basic Auth headers containing this api-key.

## Protocol Contracts

The system uses STOMP WebSocket messages structured within a `MessageEnvelope`.

- **REGISTER**: Sent by worker to master. Contains hostname, OS, and actual CPU cores/RAM.
- **REGISTER_ACK**: Sent by master to worker. Acknowledges successful registration.
- **HEARTBEAT**: Sent by worker periodically. Contains active tasks and usage metrics.
- **RESOURCE_UPDATE**: Sent by worker to report hardware resource changes.
- **PING/PONG**: Handled natively at the STOMP transport level via `setHeartbeatValue` (10s interval) to separate transport health from application health.

## REST API

The Coordinator exposes the following REST APIs (Basic Auth: admin/admin_secret):
- `GET /api/workers` - Lists all registered workers with real-time status and metrics.
- `GET /api/workers/{id}` - Details for a specific worker.
- `GET /api/cluster/resources` - Aggregated total CPU cores and RAM across the cluster.
- `GET /api/cluster/events` - Recent cluster events.

## How to Start the Complete Project

### 1. Build the Backend
From the root directory, build all the Maven modules (`shared`, `coordinator`, `worker-agent`):
```bash
mvn clean install -DskipTests
```

### 2. Start the Coordinator
The Coordinator runs on port 8080 and must be started first.
```bash
cd coordinator
mvn spring-boot:run
```

### 3. Start the Worker Agent
The Worker Agent connects to the Coordinator on port 8080.
```bash
cd worker-agent
mvn spring-boot:run
```
*(To run multiple local workers, override the port: `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"`)*

### 4. Start the Frontend
The frontend runs on Vite's local dev server.
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## Modules Directory

For more detailed information on what each component does and what it is capable of, please refer to their respective README files:
- [Coordinator](./coordinator/README.md)
- [Worker Agent](./worker-agent/README.md)
- [Shared Protocol](./shared/README.md)
- [Frontend](./frontend/README.md)
