# Frontend

The Frontend is a React 18 application built with Vite that provides a visual dashboard for the Distributed Cluster Compute project. It connects to the Coordinator's REST APIs and WebSocket endpoints to provide real-time visibility into the cluster.

## Key Architectural Files
- `src/state/clusterStore.ts`: Global Zustand store that manages the real-time connection state and latest WebSocket events.
- `src/services/apiClient.ts`: Centralized wrapper for making REST requests to the Coordinator.
- `src/services/websocketService.ts`: STOMP client implementation for connecting to `/ws/cluster` to receive live streaming data.
- `src/App.tsx`: The core layout provider outlining the sidebar, top navigation, and routing context.
- `src/pages/Dashboard/index.tsx`: The primary overview screen showing live nodes and aggregated resource consumption.

## Setup Instructions

Make sure you have Node.js 18+ installed.

```bash
npm install
npm run dev
```

The application will start on `http://localhost:5173`.
