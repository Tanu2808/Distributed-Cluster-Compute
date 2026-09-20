import { useState } from "react";
import {
  LayoutDashboard,
  Server,
  Activity,
  Settings,
  ChevronLeft,
  ChevronRight,
  Cpu,
  FileText,
  Clock,
  Link,
  List
} from "lucide-react";
import { NavItem } from "../navigation/NavItem";

const navItems = [
  { to: "/dashboard", icon: LayoutDashboard, label: "Overview" },
  { to: "/workers", icon: Server, label: "Workers" },
  { to: "/jobs", icon: FileText, label: "Jobs" },
  { to: "/tasks", icon: List, label: "Tasks" },
  { to: "/observability", icon: Activity, label: "Observability" },
  { to: "/events", icon: Clock, label: "Events" },
  { to: "/enrollment", icon: Link, label: "Enrollment" },
  { to: "/settings", icon: Settings, label: "Settings" },
];

export function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <aside
      className={`flex-shrink-0 flex flex-col bg-[#0c1526] border-r border-slate-800 transition-all duration-300 ${
        collapsed ? "w-16" : "w-60"
      }`}
    >
      {/* Logo */}
      <div
        className={`flex items-center gap-3 px-4 py-5 border-b border-slate-800 ${collapsed ? "justify-center px-2" : ""}`}
      >
        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500 to-violet-600 flex items-center justify-center flex-shrink-0 shadow-glow-cyan">
          <Cpu size={16} className="text-white" />
        </div>
        {!collapsed && (
          <div className="min-w-0">
            <p className="text-sm font-bold text-slate-100 leading-tight">
              Cluster
            </p>
            <p className="text-[10px] text-slate-500 uppercase tracking-widest">
              Compute
            </p>
          </div>
        )}
      </div>

      {/* Nav */}
      <nav className="flex-1 px-2 py-4 space-y-1">
        {!collapsed && <p className="section-title px-3 mb-3">Navigation</p>}
        {navItems.map((item) => (
          <NavItem key={item.to} {...item} collapsed={collapsed} />
        ))}
      </nav>

      {/* Collapse toggle */}
      <div className="border-t border-slate-800 p-2">
        <button
          onClick={() => setCollapsed((c) => !c)}
          className="w-full flex items-center justify-center gap-2 py-2 rounded-lg text-slate-500 hover:text-slate-300 hover:bg-slate-800 transition-all duration-150 text-xs"
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? (
            <ChevronRight size={16} />
          ) : (
            <>
              <ChevronLeft size={16} />
              <span>Collapse</span>
            </>
          )}
        </button>
      </div>
    </aside>
  );
}
