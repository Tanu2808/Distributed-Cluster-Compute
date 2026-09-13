import { NavLink, Outlet } from 'react-router-dom'
import { Server, Settings, Home, HardDrive, List, Link } from 'lucide-react'

export default function Layout() {
  const navItems = [
    { to: '/home', icon: Home, label: 'Overview' },
    { to: '/setup', icon: Server, label: 'Setup' },
    { to: '/node', icon: HardDrive, label: 'Node Status' },
    { to: '/tasks', icon: List, label: 'Tasks' },
    { to: '/connection', icon: Link, label: 'Connection' },
    { to: '/settings', icon: Settings, label: 'Settings' },
  ]

  return (
    <div className="flex h-screen bg-worker-bg text-worker-text overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 bg-worker-card border-r border-worker-border flex flex-col">
        <div className="h-16 flex items-center px-6 border-b border-worker-border">
          <Server className="w-6 h-6 text-worker-primary mr-3" />
          <h1 className="font-bold text-lg tracking-tight">Worker Agent</h1>
        </div>
        
        <nav className="flex-1 py-6 px-3 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center px-3 py-2.5 rounded-md transition-colors ${
                  isActive 
                    ? 'bg-worker-primary/10 text-worker-primary' 
                    : 'text-worker-muted hover:bg-worker-border/50 hover:text-worker-text'
                }`
              }
            >
              <item.icon className="w-5 h-5 mr-3" />
              <span className="font-medium text-sm">{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-worker-border">
          <div className="flex items-center justify-between text-xs text-worker-muted">
            <span>Status:</span>
            <span className="flex items-center text-worker-primary">
              <span className="w-2 h-2 rounded-full bg-worker-primary mr-1.5 animate-pulse"></span>
              Online
            </span>
          </div>
          <div className="mt-2 text-[10px] text-center opacity-50">
            Local Worker Instance v0.1
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        <header className="h-16 bg-worker-card/50 backdrop-blur border-b border-worker-border flex items-center justify-end px-6">
           <div className="px-3 py-1 bg-worker-border rounded text-xs font-medium text-worker-muted">
             localhost:8081
           </div>
        </header>
        <div className="flex-1 overflow-auto p-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
