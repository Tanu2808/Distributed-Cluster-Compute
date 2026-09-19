# Coordinator Cluster Dashboard

## Purpose
The Coordinator Cluster Dashboard (Frontend) provides a cluster-wide visualization of the Distributed Compute platform. It gives administrators real-time visibility into logical cluster health, registered workers, aggregated hardware usage, and job statuses.

## Architecture
The application is built using React 18 and Vite. It serves as the visual presentation layer over the Coordinator node. 
- It communicates downstream to the **Coordinator REST API** for standard data querying.
- It connects to the **Coordinator STOMP WebSocket** (`/ws/coordinator`) for live streaming of cluster events and node availability.

## Routing and Views
- `/dashboard` (Dashboard): The primary overview screen showing live nodes and aggregated resource consumption.
- `/nodes` (Nodes): Detailed table of all registered Worker Agents.
- `/observability` (Observability): Streams incoming cluster events.
- `/settings` (Settings): Cluster-wide configuration (e.g. Join codes).

## State Management
Global state is managed via **Zustand**. 
- `clusterStore.ts` tracks real-time connection state and caches the latest WebSocket events, preventing unnecessary prop-drilling across the dashboard layout.

## Key Files
| File | Responsibility |
|------|----------------|
| `src/app/App.tsx` | The core layout provider outlining the sidebar, top navigation, and routing context. |
| `src/state/clusterStore.ts` | Global Zustand store for cluster data and STOMP events. |
| `src/services/websocketService.ts` | STOMP client implementation for connecting to the Coordinator. |
| `src/hooks/useWebSocket.ts` | React hook that wraps the STOMP client to automatically sync with component lifecycles. |
| `src/api/client.ts` | Centralized wrapper for making REST requests to the Coordinator. |

## Development
Make sure you have Node.js 18+ installed.

```bash
npm install
npm run dev
```

The application will start on `http://localhost:5173`.
