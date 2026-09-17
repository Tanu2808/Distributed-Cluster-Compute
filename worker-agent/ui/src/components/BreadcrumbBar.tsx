import React from 'react';
import { useLocation } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';

const ROUTE_LABELS: Record<string, string> = {
  '/home': 'Overview',
  '/tasks': 'Tasks',
  '/node': 'Node Hardware',
  '/connection': 'Connection Diagnostics',
  '/settings': 'Settings',
  '/setup': 'Cluster Setup',
};

export const BreadcrumbBar: React.FC = () => {
  const location = useLocation();
  const pageLabel = ROUTE_LABELS[location.pathname] || 'Console';

  return (
    <div className="h-8 bg-console-topbar border-b border-console-border px-4 flex items-center text-[11px] text-console-textDim shrink-0 select-none">
      <span>Worker Agent</span>
      <ChevronRight className="w-3 h-3 mx-1.5 text-console-textDim shrink-0" />
      <span className="text-console-text font-medium">{pageLabel}</span>
    </div>
  );
};
