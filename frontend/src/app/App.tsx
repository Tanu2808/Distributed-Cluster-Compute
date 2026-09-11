import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from '../components/layout/AppLayout';
import Dashboard from '../pages/Dashboard';
import Nodes from '../pages/Nodes';
import Observability from '../pages/Observability';
import Settings from '../pages/Settings';

export default function App() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/"              element={<Dashboard />} />
          <Route path="/nodes"         element={<Nodes />} />
          <Route path="/observability" element={<Observability />} />
          <Route path="/settings"      element={<Settings />} />
          <Route path="*"              element={<Navigate to="/" replace />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}
