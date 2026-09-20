import React, { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import {
  ArrowRight,
  Check,
  Copy,
  RefreshCw,
} from "lucide-react";
import { clusterApi } from "../services/clusterApi";
import { useSettings } from "../hooks/useSettings";
import { useWorkerStatus } from "../hooks/useWorkerStatus";
import {
  ConsoleCard,
  KeyValueTable,
  StatusBadge,
  AlertBanner,
  Skeleton,
} from "../components";

export default function Setup() {
  const navigate = useNavigate();
  const {
    clusterSettings,
    loading: settingsLoading,
    refetch: refetchSettings,
  } = useSettings();
  const { status, info } = useWorkerStatus(2000);

  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Form State
  const [coordinatorUrl, setCoordinatorUrl] = useState("");

  // Status & Validation State
  const [submitting, setSubmitting] = useState(false);
  const [setupPhase, setSetupPhase] = useState<"IDLE" | "CONNECTING" | "REGISTERING" | "SUCCESS">("IDLE");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{
    coordinatorUrl?: string;
  }>({});

  const isConfigured =
    clusterSettings?.isConfigured || status?.lifecycleState === "CONFIGURED";

  useEffect(() => {
    if (submitting) {
      if (status?.connectionState === "ONLINE") {
        setSetupPhase("SUCCESS");
        setSubmitting(false);
        refetchSettings().then(() => {
          setTimeout(() => navigate("/home"), 1200);
        });
      } else if (status?.connectionState === "CONNECTING") {
        setSetupPhase("CONNECTING");
      } else if (status?.connectionState === "REGISTERING") {
        setSetupPhase("REGISTERING");
      } else if (status?.connectionState === "DISCONNECTED") {
        if (setupPhase === "CONNECTING" || setupPhase === "REGISTERING") {
           setError("Connection failed. Unable to reach Coordinator or authentication failed.");
           setSubmitting(false);
           setSetupPhase("IDLE");
        }
      }
    }
  }, [status?.connectionState, submitting, setupPhase, navigate, refetchSettings]);

  const handleCopy = (text: string, key: string) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 1800);
  };

  const validate = (): boolean => {
    const errors: { coordinatorUrl?: string } = {};
    if (!coordinatorUrl.trim()) {
      errors.coordinatorUrl = "Coordinator URL is required.";
    } else {
      try {
        new URL(coordinatorUrl);
      } catch (e) {
        errors.coordinatorUrl = "Must be a valid URL (e.g., http://192.168.1.100:8080)";
      }
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!validate()) return;

    setSubmitting(true);
    setSetupPhase("CONNECTING");
    try {
      const response = await clusterApi.connectToCoordinator(coordinatorUrl.trim());
      if (response.status !== "SUCCESS") {
        setError(response.message || "Unable to complete worker setup.");
        setSubmitting(false);
        setSetupPhase("IDLE");
      }
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Unable to complete worker setup.",
      );
      setSubmitting(false);
      setSetupPhase("IDLE");
    }
  };

  if (settingsLoading && !clusterSettings) {
    return (
      <div className="space-y-4 max-w-3xl mx-auto">
        <div className="space-y-1 pb-2 border-b border-console-border">
          <Skeleton className="h-6 w-40" />
          <Skeleton className="h-4 w-72" />
        </div>
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isConfigured && !submitting && setupPhase !== "SUCCESS") {
    return (
      <div className="space-y-4 max-w-3xl mx-auto">
        <div className="flex items-start sm:items-center justify-between gap-4 pb-2.5 border-b border-console-border">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-base font-semibold tracking-tight text-console-text">
                Worker Setup
              </h1>
              <StatusBadge status="CONFIGURED" label="Worker configured" />
            </div>
            <p className="text-[11px] text-console-textDim mt-0.5">
              This worker node is already enrolled in a cluster
            </p>
          </div>
          <Link
            to="/home"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-white bg-console-accent hover:bg-console-accentHover rounded-sm transition-colors shadow-sm"
          >
            <span>Open Worker Overview</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <ConsoleCard title="Current Enrollment Configuration">
          <KeyValueTable
            columns={1}
            items={[
              {
                label: "Worker ID",
                mono: true,
                value: info?.workerId ? (
                  <button
                    type="button"
                    onClick={() => handleCopy(info.workerId, "workerId")}
                    className="inline-flex items-center gap-1.5 hover:text-console-accent text-console-text transition-colors text-left"
                    title="Click to copy Worker ID"
                  >
                    <span className="truncate max-w-[240px] sm:max-w-[400px]">
                      {info.workerId}
                    </span>
                    {copiedKey === "workerId" ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-console-textDim hover:text-console-text shrink-0" />
                    )}
                  </button>
                ) : (
                  "Unavailable"
                ),
              },
              {
                label: "Cluster Name",
                value: clusterSettings?.clusterName || "Not configured",
              },
              {
                label: "Coordinator URL",
                mono: true,
                value: clusterSettings?.coordinatorUrl || "Not configured",
              },
              {
                label: "Enrollment Status",
                value: <StatusBadge status="CONFIGURED" label="Configured" />,
              },
            ]}
          />
          <div className="mt-4 pt-3 border-t border-console-borderSubtle flex items-center justify-between text-xs text-console-textDim">
            <span>
              To modify cluster parameters or disconnect, visit settings.
            </span>
            <Link
              to="/settings"
              className="text-console-accent hover:text-console-accentHover font-medium inline-flex items-center gap-1"
            >
              <span>Manage in Settings</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </ConsoleCard>
      </div>
    );
  }

  return (
    <div className="space-y-4 max-w-2xl mx-auto">
      <div className="flex items-start sm:items-center justify-between gap-4 pb-2.5 border-b border-console-border">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-base font-semibold tracking-tight text-console-text">
              Worker Setup
            </h1>
            {setupPhase === "SUCCESS" ? (
                <StatusBadge status="ONLINE" label="Connected" />
            ) : (
                <StatusBadge status="SETUP_REQUIRED" label="Setup required" />
            )}
          </div>
          <p className="text-[11px] text-console-textDim mt-0.5">
            Connect this worker to an existing Coordinator
          </p>
        </div>
      </div>

      {setupPhase === "SUCCESS" && <AlertBanner type="success" message="Worker configured successfully. Redirecting..." />}

      {error && (
        <AlertBanner
          type="error"
          message={
            <div className="flex items-center justify-between gap-4">
              <span>{error}</span>
              <button
                type="button"
                onClick={() => setError(null)}
                className="underline hover:text-console-accent text-rose-800 shrink-0 font-sans font-medium"
              >
                Dismiss
              </button>
            </div>
          }
        />
      )}

      <ConsoleCard
        title="Connect Worker"
        subtitle="Connect this machine to an existing Coordinator"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label
              htmlFor="coordinatorUrl"
              className="block text-xs font-medium text-console-text"
            >
              Coordinator URL
            </label>
            <input
              id="coordinatorUrl"
              type="text"
              value={coordinatorUrl}
              onChange={(e) => {
                setCoordinatorUrl(e.target.value);
                if (fieldErrors.coordinatorUrl)
                  setFieldErrors({ ...fieldErrors, coordinatorUrl: undefined });
              }}
              disabled={submitting || setupPhase === "SUCCESS"}
              placeholder="https://coordinator.example.com"
              className={`w-full px-3 py-2 text-xs bg-white border rounded-sm text-console-text placeholder:text-console-textDim focus:outline-none transition-colors ${
                fieldErrors.coordinatorUrl
                  ? "border-rose-500 focus:border-rose-600"
                  : "border-console-border focus:border-console-accent"
              }`}
            />
            {fieldErrors.coordinatorUrl ? (
              <p className="text-[11px] text-rose-700 font-medium">
                {fieldErrors.coordinatorUrl}
              </p>
            ) : (
              <p className="text-[11px] text-console-textDim">
                Enter the Coordinator address provided by your cluster administrator.
              </p>
            )}
          </div>

          <div className="pt-3 border-t border-console-border flex items-center justify-between">
            <div className="text-[11px] font-medium text-console-textDim flex items-center gap-1.5">
                {setupPhase === "CONNECTING" && (
                    <><RefreshCw className="w-3.5 h-3.5 animate-spin" /> Connecting...</>
                )}
                {setupPhase === "REGISTERING" && (
                    <><RefreshCw className="w-3.5 h-3.5 animate-spin" /> Registering worker...</>
                )}
                {setupPhase === "SUCCESS" && (
                    <><Check className="w-3.5 h-3.5 text-emerald-600" /> Connected</>
                )}
                {error && (
                    <span className="text-rose-700">Connection failed</span>
                )}
            </div>
            
            <button
              type="submit"
              disabled={submitting || setupPhase === "SUCCESS"}
              className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-medium text-white bg-console-accent hover:bg-console-accentHover disabled:opacity-50 rounded-sm transition-colors"
            >
                <span>{error ? "Retry" : "Connect"}</span>
            </button>
          </div>
        </form>
      </ConsoleCard>
    </div>
  );
}
