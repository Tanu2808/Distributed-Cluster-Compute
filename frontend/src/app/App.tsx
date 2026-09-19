import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "../utils/queryClient";
import { AppLayout } from "../components/layout/AppLayout";
import { useWebSocket } from "../hooks/useWebSocket";
import Dashboard from "../pages/Dashboard";
import Nodes from "../pages/Nodes";
import Observability from "../pages/Observability";
import Settings from "../pages/Settings";

/**
 * Inner component so we can use hooks that require QueryClientProvider context.
 * Initializes the WebSocket connection once at the app root.
 */
function AppInner() {
  // Connects WS on mount, syncs status to Zustand, invalidates React Query caches
  useWebSocket();

  return (
    <AppLayout>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/nodes" element={<Nodes />} />
        <Route path="/observability" element={<Observability />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppLayout>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppInner />
      </BrowserRouter>
    </QueryClientProvider>
  );
}
