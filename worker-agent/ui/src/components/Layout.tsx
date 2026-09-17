import { useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useWorkerStatus } from '../hooks/useWorkerStatus';
import { ConsoleHeader } from './ConsoleHeader';
import { ConsoleSidebar } from './ConsoleSidebar';
import { BreadcrumbBar } from './BreadcrumbBar';

export default function Layout() {
  const { status, info } = useWorkerStatus(3000);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (status?.lifecycleState === 'SETUP_REQUIRED' && location.pathname !== '/setup') {
      navigate('/setup', { replace: true });
    }
  }, [status?.lifecycleState, location.pathname, navigate]);

  return (
    <div className="flex flex-col h-screen w-screen overflow-hidden bg-console-bg text-console-text font-sans antialiased">
      {/* Top Navigation Console Header */}
      <ConsoleHeader status={status} info={info} />

      {/* Main Console Layout */}
      <div className="flex flex-1 min-h-0 overflow-hidden">
        {/* Left Sub-Navigation Sidebar */}
        <ConsoleSidebar status={status} />

        {/* Primary Viewport Area */}
        <div className="flex-1 flex flex-col min-w-0 overflow-hidden bg-console-bg">
          <BreadcrumbBar />
          <main className="flex-1 overflow-y-auto p-4 md:p-6">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
