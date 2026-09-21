import { useQuery } from "@tanstack/react-query";
import { List } from "lucide-react";

export default function Tasks() {
  const { data: tasks, isLoading, error } = useQuery({
    queryKey: ["tasks"],
    queryFn: async () => {
      const res = await fetch("/api/jobs/tasks");
      if (!res.ok) throw new Error("Failed to load tasks");
      return res.json() as Promise<any[]>;
    },
  });

  if (isLoading) return <div className="p-6 text-slate-400">Loading tasks...</div>;
  if (error) return <div className="p-6 text-red-400">Error: {(error as Error).message}</div>;

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
        <List size={24} className="text-cyan-400" /> Tasks
      </h1>
      <div className="bg-[#0c1526] border border-slate-800 rounded-lg overflow-hidden">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-900/50 text-slate-400 uppercase text-xs">
            <tr>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Task ID</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Job ID</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Type</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/50">
            {tasks?.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-slate-500">
                  No tasks active.
                </td>
              </tr>
            ) : (
              tasks?.map((task) => (
                <tr key={task.taskId} className="hover:bg-slate-800/30 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs truncate max-w-xs">{task.taskId}</td>
                  <td className="px-4 py-3 font-mono text-xs truncate max-w-xs">{task.jobId}</td>
                  <td className="px-4 py-3">{task.taskType}</td>
                  <td className="px-4 py-3">{task.state}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
