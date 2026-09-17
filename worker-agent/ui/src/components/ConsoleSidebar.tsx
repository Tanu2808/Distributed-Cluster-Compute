import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, ListCheck, Cpu, Network, Settings, Sliders } from 'lucide-react';
import type { WorkerStatusResponse } from '../types';

interface ConsoleSidebarProps {
  status: WorkerStatusResponse | null;
}

export const ConsoleSidebar: React.FC<ConsoleSidebarProps> = ({ status }) => {
  const isSetupRequired = status?.lifecycleState === 'SETUP_REQUIRED';

  const navItems = [
    { to: '/home', icon: LayoutDashboard, label: 'Overview' },
    { to: '/tasks', icon: ListCheck, label: 'Tasks' },
    { to: '/node', icon: Cpu, label: 'Node Hardware' },
    { to: '/connection', icon: Network, label: 'Connection' },
    { to: '/settings', icon: Settings, label: 'Settings' },
    { to: '/setup', icon: Sliders, label: 'Setup', badge: isSetupRequired ? 'Required' : undefined },
  ];

  return (
    <aside className="w-52 bg-console-sidebar border-r border-console-border flex flex-col justify-between shrink-0 select-none z-10">
      <div>
        {/* Navigation Group Header */}
        <div className="px-3 pt-3 pb-1.5">
          <span className="text-[10px] font-semibold uppercase tracking-wider text-console-textDim font-mono">
            Navigation
          </span>
        </div>

        {/* Navigation Links */}
        <nav className="space-y-0.5 px-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center justify-between px-2.5 py-1.5 text-xs rounded-sm transition-colors ${
                  isActive
                    ? 'bg-blue-50/80 text-console-accent font-semibold border-l-2 border-console-accent pl-2'
                    : 'text-console-textMuted hover:text-console-text hover:bg-console-hover'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <div className="flex items-center gap-2.5 truncate">
                    <item.icon className={`w-3.5 h-3.5 shrink-0 ${isActive ? 'text-console-accent' : 'text-console-textDim'}`} />
                    <span className="truncate">{item.label}</span>
                  </div>
                  {item.badge && (
                    <span className="text-[9px] font-mono px-1 py-0.2 bg-amber-50 text-amber-800 border border-amber-300 rounded-sm">
                      {item.badge}
                    </span>
                  )}
                </>
              )}
            </NavLink>
          ))}
        </nav>
      </div>

      {/* Sidebar Footer Metadata */}
      <div className="p-3 border-t border-console-borderSubtle bg-console-subtle text-[11px] font-mono space-y-1">
        <div className="flex items-center justify-between text-console-textDim">
          <span>Lifecycle:</span>
          <span className="text-console-text font-medium truncate ml-1">
            {status?.lifecycleState || 'UNKNOWN'}
          </span>
        </div>
        <div className="flex items-center justify-between text-console-textDim">
          <span>Execution:</span>
          <span className="text-console-text font-medium truncate ml-1">
            {status?.executionState || 'IDLE'}
          </span>
        </div>
      </div>
    </aside>
  );
};
