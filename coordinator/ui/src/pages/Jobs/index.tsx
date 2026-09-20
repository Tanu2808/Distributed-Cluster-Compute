import { useQuery } from "@tanstack/react-query";
import { FileText } from "lucide-react";

export default function Jobs() {
  const { data: jobs, isLoading, error } = useQuery({
    queryKey: ["jobs"],
    queryFn: async () => {
      const res = await fetch("/api/jobs");
      if (!res.ok) throw new Error("Failed to load jobs");
      return res.json() as Promise<any[]>;
    },
  });

  if (isLoading) return <div className="p-6 text-slate-400">Loading jobs...</div>;
  if (error) return <div className="p-6 text-red-400">Error: {(error as Error).message}</div>;

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
        <FileText size={24} className="text-cyan-400" /> Jobs
      </h1>
      <div className="bg-[#0c1526] border border-slate-800 rounded-lg overflow-hidden">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-900/50 text-slate-400 uppercase text-xs">
            <tr>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Job ID</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Task Type</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">State</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Progress</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/50">
            {jobs?.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-slate-500">
                  No jobs have been submitted.
                </td>
              </tr>
            ) : (
              jobs?.map((job) => (
                <tr key={job.jobId} className="hover:bg-slate-800/30 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs">{job.jobId}</td>
                  <td className="px-4 py-3">{job.taskType}</td>
                  <td className="px-4 py-3">{job.state}</td>
                  <td className="px-4 py-3">
                    {job.completedPartitions} / {job.totalPartitions}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
