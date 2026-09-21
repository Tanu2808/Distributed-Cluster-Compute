import { useLocation } from "react-router-dom";
import { Clock } from "lucide-react";
import { useClusterSummary } from "../../hooks/useCluster";
import { ClusterStatusBadge } from "../status/ClusterStatusBadge";
import { WsStatusIndicator } from "../status/WsStatusIndicator";

const pageTitles: Record<string, string> = {
  "/": "Dashboard",
  "/nodes": "Nodes",
  "/observability": "Cluster Observability",
  "/settings": "Cluster Settings",
};

function LiveClock() {
  const now = new Date();
  return (
    <span className="font-mono text-xs text-slate-500">
      {now.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false,
      })}
    </span>
  );
}

export function TopBar() {
  const { pathname } = useLocation();
  const title = pageTitles[pathname] ?? "Cluster";
  const { data: cluster } = useClusterSummary();

  return (
    <header className="h-14 bg-[#0c1526]/80 backdrop-blur border-b border-slate-800 flex items-center justify-between px-6 flex-shrink-0">
      {/* Left: page title */}
      <div className="flex items-center gap-3">
        <h1 className="text-base font-semibold text-slate-100">{title}</h1>
      </div>

      {/* Right: WS indicator + cluster status + time */}
      <div className="flex items-center gap-4">
        <WsStatusIndicator />
        {cluster && <ClusterStatusBadge status={cluster.status} />}
        <div className="flex items-center gap-1.5 text-slate-500">
          <Clock size={12} />
          <LiveClock />
        </div>
      </div>
    </header>
  );
}
