# Distributed Compute Cluster

> A distributed computing platform that combines the resources of multiple PCs into a single logical compute cluster.

## Overview

**Distributed Compute Cluster** is a distributed computing platform designed around a **Coordinator–Worker architecture**.

A central **Coordinator (Master)** manages a cluster of connected **Worker (Slave) PCs**. Each worker contributes its available computing resources such as CPU, memory, GPU, storage, and network capacity.

The long-term goal is to make multiple ordinary PCs behave like **one logical computing system**, allowing workloads to be distributed across machines.

For example:

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

                    Combined Cluster
                    12 CPU Cores
                    32 GB RAM
```

A future workload requiring **12 CPU cores** could therefore use resources from all three machines rather than being restricted to a single PC.

---

# Architecture

The platform consists of three primary layers:

```text
┌─────────────────────────────────────────────┐
│                  FRONTEND                   │
│                                             │
│ Dashboard │ Nodes │ Observability │ Settings│
└──────────────────────┬──────────────────────┘
                       │
                 REST / WebSocket
                       │
                       ▼
┌─────────────────────────────────────────────┐
│              COORDINATOR / MASTER           │
│                                             │
│ API Layer                                   │
│ Worker Manager                              │
│ Resource Manager                            │
│ Job Manager                                 │
│ Task Manager                                │
│ Metrics Aggregator                          │
│ Communication Manager                      │
│ Fault Manager                               │
└──────────────────────┬──────────────────────┘
                       │
                  WebSocket
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐
    │ Worker 1 │ │ Worker 2 │ │ Worker N │
    │   Agent  │ │   Agent  │ │   Agent  │
    └──────────┘ └──────────┘ └──────────┘
```

## Coordinator

The Coordinator is the **control plane** of the cluster.

Responsibilities include:

* Worker registration
* Worker authentication
* Worker health monitoring
* Heartbeat management
* Resource tracking
* Cluster resource aggregation
* Job management
* Task management
* Task scheduling
* Result management
* Fault detection
* Real-time event publishing
* Cluster observability

## Worker Agent

The Worker Agent runs on each participating PC.

Responsibilities include:

* Connecting to the Coordinator
* Registering the machine
* Reporting hardware capabilities
* Monitoring system resources
* Sending heartbeats
* Receiving tasks
* Executing tasks
* Reporting task status
* Returning results
* Reconnecting after connection failures

---

# Core Concept

The system maintains two views of the infrastructure.

### Physical View

Users can inspect individual machines:

```text
PC-01
├── 16 CPU cores
├── 32 GB RAM
└── GPU

PC-02
├── 8 CPU cores
├── 16 GB RAM
└── No GPU

PC-03
├── 32 CPU cores
├── 64 GB RAM
└── GPU
```

### Logical View

The Coordinator aggregates those resources:

```text
CLUSTER

56 CPU Cores
112 GB RAM
2 GPUs
```

This logical representation is the foundation for future distributed workloads.

---

# Features

## Dashboard

The main dashboard provides a cluster-wide overview.

* Cluster status
* Coordinator status
* Connected workers
* Active workers
* CPU utilization
* Memory utilization
* GPU utilization
* Storage
* Network utilization
* Recent cluster events
* Aggregated resources

---

## Nodes

The Nodes page provides detailed information about every worker.

Information includes:

* Worker ID
* Hostname
* IP address
* Operating system
* CPU
* Memory
* GPU
* Storage
* Network
* Current utilization
* Worker status
* Last heartbeat
* Agent version

Worker states include:

```text
REGISTERING
ONLINE
BUSY
HEARTBEAT_TIMEOUT
OFFLINE
UNHEALTHY
```

---

## Cluster Observability

The Cluster Observability page represents all connected machines as **one logical computer**.

Example:

```text
                 CLUSTER

        ┌──────────────────────┐
        │      56 CPU CORES    │
        │      112 GB RAM      │
        │      2 GPUs          │
        │      3 WORKERS       │
        └──────────────────────┘
```

It provides:

* Aggregate CPU utilization
* Aggregate memory utilization
* Aggregate GPU utilization
* Aggregate storage
* Aggregate network traffic
* Resource availability
* Worker contribution
* Cluster health
* Real-time metrics

The page also provides visibility into how individual machines contribute to the logical cluster.

---

## Cluster Settings

Coordinator configuration includes:

* Cluster name
* Cluster ID
* Coordinator configuration
* Worker registration
* Worker heartbeat interval
* Worker timeout
* Resource limits
* Monitoring configuration
* Logging configuration

---

# Frontend UI

> **Status: Phase 2 Complete** — Application logic implemented with React Query, Zustand, and WebSocket integration.

The frontend is a React 18 + TypeScript + Vite application styled with Tailwind CSS v3.

## Current State

The frontend UI is fully wired up to a robust API and state management layer, ready to connect to the Spring Boot Coordinator.

- **Data Fetching:** Handled by `@tanstack/react-query` for automatic caching, deduplication, background refetching, and loading/error states.
- **Real-time Updates:** A singleton `WebSocketService` maintains a connection to the backend with automatic exponential backoff.
- **State Management:** A lightweight `zustand` store listens to WebSocket messages and seamlessly invalidates React Query caches, causing the UI to live-update without manual page refreshes.

The application includes a **Mock Mode** (`VITE_USE_MOCK_API=true`). When enabled, custom React Query hooks immediately return static data from `src/mock/` instead of calling the backend, allowing independent frontend development without needing a running server.

## Pages

| Page | Route | Description |
|---|---|---|
| Dashboard | `/` | Cluster status, aggregated resource cards (CPU/RAM/GPU/Storage), 4 utilization charts, worker table, events feed |
| Nodes | `/nodes` | Grid + list view of workers, search, status filter, click-to-expand detail drawer |
| Cluster Observability | `/observability` | "Logical Cluster" hero, worker→cluster contribution diagram, per-resource sections with charts |
| Cluster Settings | `/settings` | Tabbed settings: Coordinator, Worker Registration, Resource Config, Monitoring |

## Application Logic Architecture

```text
src/
├── api/             ← Typed REST API clients (GET/PUT)
├── hooks/           ← React Query hooks bridging UI and APIs (e.g., useCluster, useWorkers)
├── services/        ← WebSocket singleton managing live connections
├── state/           ← Zustand store for connection status and real-time event triggers
└── utils/           ← Config and QueryClient setup
```

The UI components do not directly call `fetch()` or `WebSocket` APIs. They simply consume data from the React Query hooks:

```tsx
export default function Dashboard() {
  const { data: cluster, isLoading, isError } = useClusterSummary();
  
  if (isLoading) return <LoadingSkeleton />;
  if (isError) return <ErrorBanner message="Failed to fetch cluster data" />;
  
  return <ClusterStatusBadge status={cluster.status} />;
}
```

## Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

Open the displayed URL (default: `http://localhost:5173`).

---

# Communication


The Coordinator and Worker Agents communicate using a persistent communication channel.

The primary communication mechanism is:

**WebSocket**

Communication follows a structured message protocol.

Example message types:

```text
REGISTER
REGISTER_ACK
HEARTBEAT
RESOURCE_UPDATE
TASK_ASSIGN
TASK_CANCEL
TASK_STATUS
TASK_RESULT
ERROR
PING
PONG
```

Example heartbeat:

```json
{
  "type": "HEARTBEAT",
  "workerId": "worker-01",
  "timestamp": "2026-09-11T12:00:00Z",
  "payload": {
    "cpuUsage": 42.3,
    "memoryUsage": 61.2
  }
}
```

---

# Resource Aggregation

Each Worker Agent reports its available resources to the Coordinator.

The Coordinator aggregates those resources into cluster-level metrics.

```text
Worker 1
4 CPU
8 GB RAM

Worker 2
8 CPU
16 GB RAM

Worker 3
16 CPU
32 GB RAM

        ↓

Coordinator

        ↓

Cluster

28 CPU
56 GB RAM
```

Important distinction:

> Aggregated resources represent the cluster's logical compute capacity. They do not imply that physically separate RAM, disks, or CPUs become a single hardware device.

---

# Future Distributed Computing

The current foundation is designed for a more advanced scheduling system.

A future job could request:

```text
CPU: 12 cores
RAM: 20 GB
```

If no individual worker can satisfy the request:

```text
PC-01 → 4 CPU
PC-02 → 4 CPU
PC-03 → 4 CPU
```

the Coordinator could divide the workload:

```text
                 JOB
                  │
             12 CPU
                  │
          ┌───────┼───────┐
          ▼       ▼       ▼
        PC-01   PC-02   PC-03
         4 CPU   4 CPU   4 CPU
```

This capability will be implemented through:

* Job decomposition
* Task graphs
* Resource-aware scheduling
* Parallel execution
* Distributed intermediate data
* Task synchronization
* Partial-result aggregation
* Fault recovery

These advanced capabilities are intentionally separated from the initial cluster-management foundation.

---

# Technology Stack

## Backend

* Java
* Spring Boot
* Spring Web
* Spring WebSocket
* Spring Data JPA
* Spring Actuator
* Bean Validation
* PostgreSQL

## Frontend

* React 18
* TypeScript 5
* Vite 8
* Tailwind CSS v3
* Recharts (charts)
* React Router v6 (routing)
* Lucide React (icons)
* Inter (Google Fonts)

## Communication

* REST API
* WebSocket

## Database

* PostgreSQL

## Build

* Maven

---

# Project Structure

```text
distributed-compute-cluster/
│
├── frontend/                   ← React + TypeScript + Vite (Phase 1 complete)
│   └── src/
│       ├── app/                ← React Router entry
│       ├── types/              ← Shared TypeScript interfaces
│       ├── mock/               ← Mock data layer (replaced by API in Phase 2)
│       ├── components/         ← Reusable UI components
│       │   ├── cards/
│       │   ├── charts/
│       │   ├── tables/
│       │   ├── status/
│       │   ├── navigation/
│       │   ├── layout/
│       │   └── modals/
│       └── pages/
│           ├── Dashboard/
│           ├── Nodes/
│           ├── Observability/
│           └── Settings/
│
├── coordinator/                ← Phase 3 (Spring Boot, planned)
│   ├── src/
│   └── pom.xml
│
├── worker-agent/               ← Phase 4 (Java agent, planned)
│   ├── src/
│   └── pom.xml
│
├── shared/
│   ├── protocol/
│   ├── dto/
│   └── common/
│
├── docker/
│
├── docs/
│
├── .gitignore
├── README.md
└── pom.xml
```

---

# Development Roadmap

The project will be developed incrementally.

### Phase 1 — UI ✅ Complete

* [x] Project scaffold — React 18 + TypeScript + Vite + Tailwind CSS v3
* [x] Design system — dark theme, color palette, typography, animations
* [x] Persistent sidebar (collapsible) + top navigation bar
* [x] Shared component library — cards, charts, tables, status badges, modal
* [x] TypeScript type definitions for all domain models
* [x] Realistic mock data layer — workers, cluster summary, events, settings
* [x] Dashboard — cluster status, aggregated resources, utilization charts, worker table, events feed
* [x] Nodes page — grid/list view, search, filter, worker cards, detail slide-over drawer
* [x] Cluster Observability — logical cluster hero, worker→cluster SVG diagram, per-resource sections
* [x] Cluster Settings — tabbed sections, toggles, sliders, dropdowns, save/reset
* [x] Production build verified (`npm run build` exits 0)

### Phase 2 — Frontend Logic

* [x] API service layer
* [x] Application state
* [x] Backend integration
* [x] Real-time updates
* [x] Error handling

### Phase 3 — Coordinator

* [ ] Spring Boot Coordinator
* [ ] Worker management
* [ ] REST APIs
* [ ] Database persistence
* [ ] Resource management
* [ ] Cluster state

### Phase 4 — Worker Agent

* [ ] Worker Agent
* [ ] Hardware detection
* [ ] Resource monitoring
* [ ] Worker registration
* [ ] Heartbeat system

### Phase 5 — Communication

* [ ] WebSocket communication
* [ ] Registration protocol
* [ ] Heartbeat protocol
* [ ] Resource updates
* [ ] Reconnection
* [ ] Connection failure detection

### Phase 6 — Observability

* [ ] Cluster metrics aggregation
* [ ] Real-time metrics
* [ ] Cluster health
* [ ] Worker contribution
* [ ] Historical metrics

### Phase 7 — Distributed Execution

* [ ] Job submission
* [ ] Task model
* [ ] Task execution
* [ ] Resource-aware scheduling
* [ ] Parallel execution
* [ ] Multi-worker jobs

### Phase 8 — Advanced Distributed Computing

* [ ] Task decomposition
* [ ] Distributed task graphs
* [ ] Cross-worker computation
* [ ] Intermediate data management
* [ ] Partial-result aggregation
* [ ] Dynamic resource allocation

### Phase 9 — Fault Tolerance

* [ ] Worker failure detection
* [ ] Task retry
* [ ] Task reassignment
* [ ] Job recovery
* [ ] Coordinator recovery

---

# Getting Started

## Prerequisites

Install:

* Java LTS
* Maven
* Node.js
* npm
* PostgreSQL

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

Verify Node:

```bash
node --version
```

---

# Running the Coordinator

Navigate to the Coordinator:

```bash
cd coordinator
```

Run:

```bash
mvn spring-boot:run
```

The Coordinator will start and expose its API.

---

# Running a Worker

Navigate to the Worker Agent:

```bash
cd worker-agent
```

Configure the Coordinator address:

```properties
coordinator.url=ws://localhost:8080
```

Then run:

```bash
mvn spring-boot:run
```

The Worker Agent will:

1. Connect to the Coordinator
2. Register itself
3. Report hardware information
4. Begin sending heartbeats
5. Begin reporting resource utilization

---

# Running the Frontend

Navigate to the frontend:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start development server:

```bash
npm run dev
```

Open the displayed local development URL in a browser.

---

# Local Development Architecture

For initial development, everything can run on one machine:

```text
┌──────────────────────────────────────────────┐
│              DEVELOPMENT PC                  │
│                                              │
│  Frontend                                    │
│      │                                       │
│      ▼                                       │
│  Coordinator                                 │
│      │                                       │
│      ├──────── Worker Agent 1                │
│      ├──────── Worker Agent 2                │
│      └──────── Worker Agent 3                │
│                                              │
│  PostgreSQL                                  │
└──────────────────────────────────────────────┘
```

Multiple Worker Agents can initially be simulated on one machine.

Later, they can run on separate physical PCs:

```text
             Network
                │
        ┌───────┴───────┐
        │               │
        ▼               ▼
 Coordinator        Worker PCs
                    ├── PC 01
                    ├── PC 02
                    ├── PC 03
                    └── PC N
```

---

# Design Principles

The project follows these principles:

### Separation of Control and Execution

The Coordinator manages the cluster.

Workers perform computation.

```text
Coordinator = Control Plane
Worker      = Compute Plane
```

### Resource Awareness

The Coordinator continuously maintains an understanding of available cluster resources.

### Real-Time State

Cluster state should be updated continuously rather than relying on manual refreshes.

### Fault Tolerance

Workers are expected to disconnect unexpectedly. The architecture must account for this.

### Extensibility

The initial cluster-management system must support future distributed execution without requiring a complete architectural rewrite.

### Security

Workers must be authenticated before joining the cluster, and communication should be protected against unauthorized access.

---

# Project Status

**Current Stage:** Initial architecture and development

The project is currently focused on establishing the:

* Coordinator
* Worker Agent
* Communication layer
* Resource aggregation
* UI
* Cluster observability

Advanced distributed task execution will be built on top of this foundation.

---

# License

License information will be added when the project license is finalized.
