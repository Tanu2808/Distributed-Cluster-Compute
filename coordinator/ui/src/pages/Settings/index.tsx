import { useState, useEffect } from "react";
import {
  Server,
  Users,
  Shield,
  Cpu,
  BarChart3,
  Check,
  RotateCcw,
  Loader2,
} from "lucide-react";
import { LoadingSkeleton } from "../../components/feedback/LoadingSkeleton";
import { ErrorBanner } from "../../components/feedback/ErrorBanner";
import { useSettings, useUpdateSettings } from "../../hooks/useSettings";
import type { ClusterSettings, RegistrationMode, LogLevel } from "../../types";

type SettingsTab = "coordinator" | "registration" | "resources" | "monitoring";

const tabs: { id: SettingsTab; label: string; icon: React.ReactNode }[] = [
  { id: "coordinator", label: "Coordinator", icon: <Server size={14} /> },
  {
    id: "registration",
    label: "Worker Registration",
    icon: <Users size={14} />,
  },
  { id: "resources", label: "Resource Config", icon: <Cpu size={14} /> },
  { id: "monitoring", label: "Monitoring", icon: <BarChart3 size={14} /> },
];

function SectionTitle({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <div className="flex items-start gap-3 mb-6 pb-5 border-b border-slate-800">
      <div className="w-9 h-9 rounded-lg bg-accent-500/10 border border-accent-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
        <span className="text-accent-400">{icon}</span>
      </div>
      <div>
        <h2 className="text-sm font-bold text-slate-200">{title}</h2>
        <p className="text-xs text-slate-600 mt-0.5">{description}</p>
      </div>
    </div>
  );
}

function FieldGroup({
  label,
  description,
  children,
}: {
  label: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-start gap-3 py-4 border-b border-slate-800/70 last:border-0">
      <div className="sm:w-56 flex-shrink-0">
        <label className="text-sm font-medium text-slate-300">{label}</label>
        {description && (
          <p className="text-xs text-slate-600 mt-0.5">{description}</p>
        )}
      </div>
      <div className="flex-1">{children}</div>
    </div>
  );
}

function Toggle({
  checked,
  onChange,
  label,
}: {
  checked: boolean;
  onChange: (v: boolean) => void;
  label?: string;
}) {
  return (
    <div className="flex items-center gap-2.5">
      <button
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={`relative inline-flex w-10 h-5 rounded-full transition-colors duration-200 focus:outline-none ${
          checked ? "bg-accent-500" : "bg-slate-700"
        }`}
      >
        <span
          className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform duration-200 ${
            checked ? "translate-x-5" : "translate-x-0"
          }`}
        />
      </button>
      {label && <span className="text-sm text-slate-400">{label}</span>}
    </div>
  );
}

export default function Settings() {
  const [activeTab, setActiveTab] = useState<SettingsTab>("coordinator");

  const { data: serverSettings, isLoading, isError, refetch } = useSettings();
  const {
    mutate: updateSettings,
    isPending,
    isSuccess,
    isError: isMutationError,
  } = useUpdateSettings();

  const [settings, setSettings] = useState<ClusterSettings | null>(null);

  useEffect(() => {
    if (serverSettings) setSettings(serverSettings);
  }, [serverSettings]);

  function handleSave() {
    if (settings) updateSettings(settings);
  }

  function handleReset() {
    if (serverSettings) setSettings(serverSettings);
  }

  const updateCoord = (
    patch: Partial<NonNullable<typeof settings>["coordinator"]>,
  ) =>
    setSettings((s) =>
      s ? { ...s, coordinator: { ...s.coordinator, ...patch } } : null,
    );

  const updateReg = (
    patch: Partial<NonNullable<typeof settings>["workerRegistration"]>,
  ) =>
    setSettings((s) =>
      s
        ? { ...s, workerRegistration: { ...s.workerRegistration, ...patch } }
        : null,
    );

  const updateRes = (
    patch: Partial<NonNullable<typeof settings>["resourceConfig"]>,
  ) =>
    setSettings((s) =>
      s ? { ...s, resourceConfig: { ...s.resourceConfig, ...patch } } : null,
    );

  const updateMon = (
    patch: Partial<NonNullable<typeof settings>["monitoring"]>,
  ) =>
    setSettings((s) =>
      s ? { ...s, monitoring: { ...s.monitoring, ...patch } } : null,
    );

  if (isLoading || !settings) {
    return (
      <div className="p-6">
        <div className="card p-6 max-w-4xl">
          <LoadingSkeleton rows={8} />
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 animate-fade-in">
      <div className="max-w-4xl space-y-4">
        {isError && (
          <ErrorBanner
            message="Failed to load settings."
            onRetry={() => refetch()}
          />
        )}
        {isMutationError && <ErrorBanner message="Failed to save settings." />}

        {/* Tabs */}
        <div className="flex gap-1 bg-surface-900 border border-slate-800 rounded-xl p-1">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex-1 flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-xs font-medium transition-all duration-150 ${
                activeTab === tab.id
                  ? "bg-surface-700 text-slate-100 border border-slate-700"
                  : "text-slate-500 hover:text-slate-300"
              }`}
            >
              {tab.icon}
              <span className="hidden sm:inline">{tab.label}</span>
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="card p-6">
          {/* Coordinator */}
          {activeTab === "coordinator" && (
            <>
              <SectionTitle
                icon={<Server size={16} />}
                title="Coordinator Configuration"
                description="Core settings for the cluster coordinator node"
              />
              <FieldGroup
                label="Cluster Name"
                description="Human-readable name for this cluster"
              >
                <input
                  className="input-field"
                  value={settings.coordinator.clusterName}
                  onChange={(e) => updateCoord({ clusterName: e.target.value })}
                />
              </FieldGroup>
              <FieldGroup
                label="Cluster ID"
                description="Unique identifier (auto-generated)"
              >
                <input
                  className="input-field font-mono text-slate-500 cursor-not-allowed"
                  value={settings.coordinator.clusterId}
                  readOnly
                />
              </FieldGroup>
              <FieldGroup
                label="Hostname"
                description="Coordinator bind hostname or IP"
              >
                <input
                  className="input-field font-mono"
                  value={settings.coordinator.hostname}
                  onChange={(e) => updateCoord({ hostname: e.target.value })}
                />
              </FieldGroup>
              <FieldGroup
                label="Port"
                description="Coordinator API and WebSocket port"
              >
                <input
                  className="input-field w-32 font-mono"
                  type="number"
                  value={settings.coordinator.port}
                  onChange={(e) =>
                    updateCoord({ port: parseInt(e.target.value) || 8080 })
                  }
                />
              </FieldGroup>
            </>
          )}

          {/* Registration */}
          {activeTab === "registration" && (
            <>
              <SectionTitle
                icon={<Shield size={16} />}
                title="Worker Registration"
                description="Control how workers join the cluster"
              />
              <FieldGroup
                label="Registration Mode"
                description="How workers are allowed to register"
              >
                <select
                  className="select-field w-48"
                  value={settings.workerRegistration.registrationMode}
                  onChange={(e) =>
                    updateReg({
                      registrationMode: e.target.value as RegistrationMode,
                    })
                  }
                >
                  <option value="OPEN">Open (no auth)</option>
                  <option value="TOKEN">Token-based</option>
                  <option value="CERTIFICATE">Certificate</option>
                </select>
              </FieldGroup>
              <FieldGroup
                label="Require Authentication"
                description="Enforce auth before a worker can join"
              >
                <Toggle
                  checked={settings.workerRegistration.requireAuthentication}
                  onChange={(v) => updateReg({ requireAuthentication: v })}
                  label={
                    settings.workerRegistration.requireAuthentication
                      ? "Enabled"
                      : "Disabled"
                  }
                />
              </FieldGroup>
              <FieldGroup
                label="Heartbeat Interval"
                description="How often workers send a heartbeat (seconds)"
              >
                <div className="flex items-center gap-2">
                  <input
                    className="input-field w-24 font-mono"
                    type="number"
                    min={1}
                    max={120}
                    value={settings.workerRegistration.heartbeatIntervalSeconds}
                    onChange={(e) =>
                      updateReg({
                        heartbeatIntervalSeconds:
                          parseInt(e.target.value) || 10,
                      })
                    }
                  />
                  <span className="text-xs text-slate-500">seconds</span>
                </div>
              </FieldGroup>
              <FieldGroup
                label="Worker Timeout"
                description="Time before a silent worker is marked offline"
              >
                <div className="flex items-center gap-2">
                  <input
                    className="input-field w-24 font-mono"
                    type="number"
                    min={5}
                    max={600}
                    value={settings.workerRegistration.workerTimeoutSeconds}
                    onChange={(e) =>
                      updateReg({
                        workerTimeoutSeconds: parseInt(e.target.value) || 30,
                      })
                    }
                  />
                  <span className="text-xs text-slate-500">seconds</span>
                </div>
              </FieldGroup>
            </>
          )}

          {/* Resources */}
          {activeTab === "resources" && (
            <>
              <SectionTitle
                icon={<Cpu size={16} />}
                title="Resource Configuration"
                description="Control how cluster resources are allocated and limited"
              />
              <FieldGroup
                label="Max CPU Allocation"
                description="Maximum CPU % any task can consume per worker"
              >
                <div className="flex items-center gap-3">
                  <input
                    type="range"
                    min={10}
                    max={100}
                    value={settings.resourceConfig.maxCpuAllocationPercent}
                    onChange={(e) =>
                      updateRes({
                        maxCpuAllocationPercent: parseInt(e.target.value),
                      })
                    }
                    className="w-40 accent-cyan-400"
                  />
                  <span className="text-sm font-mono text-slate-300 w-10">
                    {settings.resourceConfig.maxCpuAllocationPercent}%
                  </span>
                </div>
              </FieldGroup>
              <FieldGroup
                label="Max Memory Allocation"
                description="Maximum memory % per worker for task execution"
              >
                <div className="flex items-center gap-3">
                  <input
                    type="range"
                    min={10}
                    max={100}
                    value={settings.resourceConfig.maxMemoryAllocationPercent}
                    onChange={(e) =>
                      updateRes({
                        maxMemoryAllocationPercent: parseInt(e.target.value),
                      })
                    }
                    className="w-40 accent-violet-400"
                  />
                  <span className="text-sm font-mono text-slate-300 w-10">
                    {settings.resourceConfig.maxMemoryAllocationPercent}%
                  </span>
                </div>
              </FieldGroup>
              <FieldGroup
                label="GPU Allocation"
                description="Allow tasks to request GPU resources"
              >
                <Toggle
                  checked={settings.resourceConfig.gpuAllocationEnabled}
                  onChange={(v) => updateRes({ gpuAllocationEnabled: v })}
                  label={
                    settings.resourceConfig.gpuAllocationEnabled
                      ? "Enabled"
                      : "Disabled"
                  }
                />
              </FieldGroup>
              <FieldGroup
                label="Per-Worker CPU Limit"
                description="Max cores per worker for task assignment (0 = unlimited)"
              >
                <div className="flex items-center gap-2">
                  <input
                    className="input-field w-24 font-mono"
                    type="number"
                    min={0}
                    value={settings.resourceConfig.perWorkerCpuLimit}
                    onChange={(e) =>
                      updateRes({
                        perWorkerCpuLimit: parseInt(e.target.value) || 0,
                      })
                    }
                  />
                  <span className="text-xs text-slate-500">
                    cores (0 = no limit)
                  </span>
                </div>
              </FieldGroup>
              <FieldGroup
                label="Per-Worker Memory Limit"
                description="Max GB per worker for task assignment (0 = unlimited)"
              >
                <div className="flex items-center gap-2">
                  <input
                    className="input-field w-24 font-mono"
                    type="number"
                    min={0}
                    value={settings.resourceConfig.perWorkerMemoryLimitGb}
                    onChange={(e) =>
                      updateRes({
                        perWorkerMemoryLimitGb: parseInt(e.target.value) || 0,
                      })
                    }
                  />
                  <span className="text-xs text-slate-500">
                    GB (0 = no limit)
                  </span>
                </div>
              </FieldGroup>
            </>
          )}

          {/* Monitoring */}
          {activeTab === "monitoring" && (
            <>
              <SectionTitle
                icon={<BarChart3 size={16} />}
                title="Monitoring & Logging"
                description="Configure metrics collection, event retention, and log verbosity"
              />
              <FieldGroup
                label="Metrics Interval"
                description="How often the coordinator collects metrics from workers"
              >
                <div className="flex items-center gap-2">
                  <input
                    className="input-field w-24 font-mono"
                    type="number"
                    min={1}
                    max={60}
                    value={settings.monitoring.metricsCollectionIntervalSeconds}
                    onChange={(e) =>
                      updateMon({
                        metricsCollectionIntervalSeconds:
                          parseInt(e.target.value) || 5,
                      })
                    }
                  />
                  <span className="text-xs text-slate-500">seconds</span>
                </div>
              </FieldGroup>
              <FieldGroup
                label="Event Retention"
                description="How long cluster events are stored"
              >
                <div className="flex items-center gap-2">
                  <input
                    className="input-field w-24 font-mono"
                    type="number"
                    min={1}
                    max={365}
                    value={settings.monitoring.eventRetentionDays}
                    onChange={(e) =>
                      updateMon({
                        eventRetentionDays: parseInt(e.target.value) || 30,
                      })
                    }
                  />
                  <span className="text-xs text-slate-500">days</span>
                </div>
              </FieldGroup>
              <FieldGroup
                label="Log Level"
                description="Verbosity of coordinator logs"
              >
                <select
                  className="select-field w-36"
                  value={settings.monitoring.logLevel}
                  onChange={(e) =>
                    updateMon({ logLevel: e.target.value as LogLevel })
                  }
                >
                  <option value="DEBUG">DEBUG</option>
                  <option value="INFO">INFO</option>
                  <option value="WARN">WARN</option>
                  <option value="ERROR">ERROR</option>
                </select>
              </FieldGroup>
              <FieldGroup
                label="Metrics Export"
                description="Export metrics to an external monitoring system"
              >
                <Toggle
                  checked={settings.monitoring.enableMetricsExport}
                  onChange={(v) => updateMon({ enableMetricsExport: v })}
                  label={
                    settings.monitoring.enableMetricsExport
                      ? "Enabled"
                      : "Disabled"
                  }
                />
              </FieldGroup>
            </>
          )}
        </div>

        {/* Action bar */}
        <div className="flex items-center justify-between mt-4">
          <button
            onClick={handleReset}
            className="btn-secondary flex items-center gap-2"
          >
            <RotateCcw size={14} />
            Reset to defaults
          </button>
          <button
            onClick={handleSave}
            disabled={isPending}
            className={`flex items-center gap-2 px-5 py-2 rounded-lg text-sm font-semibold transition-all duration-200 ${
              isSuccess
                ? "bg-green-600 text-white"
                : isPending
                  ? "bg-slate-700 text-slate-400 cursor-not-allowed"
                  : "bg-accent-500 hover:bg-accent-400 text-white"
            }`}
          >
            {isSuccess ? (
              <>
                <Check size={14} /> Saved
              </>
            ) : isPending ? (
              <>
                <Loader2 size={14} className="animate-spin" /> Saving…
              </>
            ) : (
              "Save Changes"
            )}
          </button>
        </div>

      </div>
    </div>
  );
}
