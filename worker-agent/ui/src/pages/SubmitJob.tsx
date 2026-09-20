import React, { useState, useEffect } from "react";
import { Terminal, Send, RefreshCw } from "lucide-react";

interface JobStatus {
  id: string;
  taskType: string;
  state: string;
  completedPartitions: number;
  totalPartitions: number;
  finalResult: string | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
}

export default function SubmitJob() {
  const [jobType, setJobType] = useState<"SUM_RANGE" | "ML_INFERENCE">("SUM_RANGE");
  const [submitting, setSubmitting] = useState(false);
  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  const [jobStatus, setJobStatus] = useState<JobStatus | null>(null);

  // SUM_RANGE inputs
  const [startRange, setStartRange] = useState("1");
  const [endRange, setEndRange] = useState("100");
  const [partitions, setPartitions] = useState("4");

  // ML_INFERENCE inputs
  const [mlData, setMlData] = useState("[[1.0], [2.0], [3.0], [4.0]]");

  useEffect(() => {
    let interval: number;
    if (activeJobId) {
      interval = window.setInterval(async () => {
        try {
          const response = await fetch(`/api/jobs/${activeJobId}`);
          if (response.ok) {
            const data = await response.json();
            setJobStatus(data);
            if (["COMPLETED", "FAILED", "CANCELLED"].includes(data.state)) {
              window.clearInterval(interval);
            }
          }
        } catch (e) {
          console.error("Failed to fetch job status", e);
        }
      }, 1000);
    }
    return () => window.clearInterval(interval);
  }, [activeJobId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setActiveJobId(null);
    setJobStatus(null);

    let inputPayload = "";
    if (jobType === "SUM_RANGE") {
      inputPayload = JSON.stringify({
        start: parseInt(startRange),
        end: parseInt(endRange)
      });
    } else {
      try {
        const parsedData = JSON.parse(mlData);
        inputPayload = JSON.stringify({
          model: "linear_regression.onnx",
          data: parsedData
        });
      } catch (err) {
        alert("Invalid JSON for ML data array");
        setSubmitting(false);
        return;
      }
    }

    const jobReq = {
      taskType: jobType,
      input: inputPayload,
      targetPartitions: parseInt(partitions)
    };

    try {
      const response = await fetch("/api/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(jobReq)
      });
      if (response.ok) {
        const data = await response.json();
        setActiveJobId(data.id);
        setJobStatus(data);
      } else {
        alert("Failed to submit job");
      }
    } catch (e) {
      console.error(e);
      alert("Error submitting job");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-console-text flex items-center gap-2">
            <Send className="w-5 h-5 text-console-accent" />
            Submit Workload
          </h1>
          <p className="text-sm text-console-textMuted mt-1">
            Dispatch distributed jobs to the cluster
          </p>
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Job Configuration Form */}
        <div className="bg-console-card border border-console-border rounded shadow-sm p-4">
          <h2 className="text-sm font-semibold uppercase text-console-textDim mb-4">
            Job Configuration
          </h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-console-text mb-1">
                Workload Type
              </label>
              <select
                value={jobType}
                onChange={(e) => setJobType(e.target.value as any)}
                className="w-full bg-console-input bg-console-bg border border-console-input text-console-text text-sm rounded px-3 py-2"
              >
                <option value="SUM_RANGE">Sum Range (Compute Bound)</option>
                <option value="ML_INFERENCE">ML Inference (ONNX)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-console-text mb-1">
                Target Partitions (Tasks)
              </label>
              <input
                type="number"
                value={partitions}
                onChange={(e) => setPartitions(e.target.value)}
                min="1"
                className="w-full bg-console-input bg-console-bg border border-console-input text-console-text text-sm rounded px-3 py-2"
              />
            </div>

            {jobType === "SUM_RANGE" && (
              <div className="flex gap-4">
                <div className="flex-1">
                  <label className="block text-xs font-medium text-console-text mb-1">
                    Start Range
                  </label>
                  <input
                    type="number"
                    value={startRange}
                    onChange={(e) => setStartRange(e.target.value)}
                    className="w-full bg-console-input bg-console-bg border border-console-input text-console-text text-sm rounded px-3 py-2"
                  />
                </div>
                <div className="flex-1">
                  <label className="block text-xs font-medium text-console-text mb-1">
                    End Range
                  </label>
                  <input
                    type="number"
                    value={endRange}
                    onChange={(e) => setEndRange(e.target.value)}
                    className="w-full bg-console-input bg-console-bg border border-console-input text-console-text text-sm rounded px-3 py-2"
                  />
                </div>
              </div>
            )}

            {jobType === "ML_INFERENCE" && (
              <div>
                <label className="block text-xs font-medium text-console-text mb-1">
                  Data Array (JSON)
                </label>
                <textarea
                  value={mlData}
                  onChange={(e) => setMlData(e.target.value)}
                  rows={4}
                  className="w-full bg-console-input bg-console-bg border border-console-input text-console-text text-sm rounded px-3 py-2 font-mono"
                />
              </div>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded text-sm transition-colors flex justify-center items-center gap-2"
            >
              {submitting ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  Submitting...
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  Dispatch Job
                </>
              )}
            </button>
          </form>
        </div>

        {/* Job Status & Results */}
        <div className="bg-console-card border border-console-border rounded shadow-sm p-4 flex flex-col">
          <h2 className="text-sm font-semibold uppercase text-console-textDim mb-4 flex items-center justify-between">
            <span>Job Status</span>
            {activeJobId && <span className="font-mono text-xs text-blue-400">{activeJobId.substring(0, 8)}...</span>}
          </h2>

          {!jobStatus ? (
            <div className="flex-1 flex flex-col items-center justify-center text-console-textMuted text-sm">
              <Terminal className="w-12 h-12 mb-3 text-console-border" />
              <p>No active job.</p>
              <p>Submit a workload to see progress.</p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex justify-between items-center bg-console-bg p-3 rounded border border-console-borderSubtle">
                <span className="text-xs text-console-textMuted uppercase font-semibold">State</span>
                <span className={`text-sm font-mono font-bold ${
                  jobStatus.state === 'COMPLETED' ? 'text-green-500' :
                  jobStatus.state === 'FAILED' ? 'text-red-500' :
                  'text-amber-500'
                }`}>
                  {jobStatus.state}
                </span>
              </div>

              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-console-textDim">Progress</span>
                  <span className="text-console-text font-mono">
                    {jobStatus.completedPartitions} / {jobStatus.totalPartitions} Partitions
                  </span>
                </div>
                <div className="h-2 w-full bg-console-bg rounded overflow-hidden">
                  <div
                    className="h-full bg-blue-500 transition-all duration-500"
                    style={{ width: `${(jobStatus.completedPartitions / jobStatus.totalPartitions) * 100}%` }}
                  ></div>
                </div>
              </div>

              {jobStatus.state === 'COMPLETED' && jobStatus.finalResult && (
                <div className="mt-4 p-4 bg-green-50/10 border border-green-500/20 rounded">
                  <h3 className="text-xs font-semibold uppercase text-green-400 mb-2">Final Result</h3>
                  <div className="font-mono text-sm break-words overflow-auto max-h-32 text-green-300">
                    {jobStatus.finalResult}
                  </div>
                </div>
              )}

              {jobStatus.state === 'FAILED' && jobStatus.errorMessage && (
                <div className="mt-4 p-4 bg-red-50/10 border border-red-500/20 rounded">
                  <h3 className="text-xs font-semibold uppercase text-red-400 mb-2">Error</h3>
                  <div className="font-mono text-sm text-red-300">
                    {jobStatus.errorMessage}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
