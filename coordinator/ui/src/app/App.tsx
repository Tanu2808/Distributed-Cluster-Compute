import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "../utils/queryClient";
import { AppLayout } from "../components/layout/AppLayout";
import { useWebSocket } from "../hooks/useWebSocket";
import Dashboard from "../pages/Dashboard";
import Workers from "../pages/Workers";
import Jobs from "../pages/Jobs";
import Tasks from "../pages/Tasks";
import Observability from "../pages/Observability";
import Events from "../pages/Events";
import Enrollment from "../pages/Enrollment";
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
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/workers" element={<Workers />} />
        <Route path="/jobs" element={<Jobs />} />
        <Route path="/tasks" element={<Tasks />} />
        <Route path="/observability" element={<Observability />} />
        <Route path="/events" element={<Events />} />
        <Route path="/enrollment" element={<Enrollment />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
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
