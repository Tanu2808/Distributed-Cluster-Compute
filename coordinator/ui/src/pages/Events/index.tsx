import { useQuery } from "@tanstack/react-query";
import { Clock } from "lucide-react";
import { queryKeys } from "../../types/api";

export default function Events() {
  const { data: events, isLoading, error } = useQuery({
    queryKey: queryKeys.events,
    queryFn: async () => {
      const res = await fetch("/api/cluster/events");
      if (!res.ok) throw new Error("Failed to load events");
      return res.json() as Promise<any[]>;
    },
  });

  if (isLoading) return <div className="p-6 text-slate-400">Loading events...</div>;
  if (error) return <div className="p-6 text-red-400">Error: {(error as Error).message}</div>;

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
        <Clock size={24} className="text-cyan-400" /> Cluster Events
      </h1>
      <div className="bg-[#0c1526] border border-slate-800 rounded-lg overflow-hidden">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-900/50 text-slate-400 uppercase text-xs">
            <tr>
              <th className="px-4 py-3 border-b border-slate-800 font-medium w-48">Timestamp</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Type</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Worker ID</th>
              <th className="px-4 py-3 border-b border-slate-800 font-medium">Message</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/50">
            {events?.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-slate-500">
                  No events recorded.
                </td>
              </tr>
            ) : (
              events?.map((event) => (
                <tr key={event.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="px-4 py-3 text-xs text-slate-400">
                    {new Date(event.timestamp).toLocaleString()}
                  </td>
                  <td className="px-4 py-3 font-medium text-cyan-400">{event.eventType}</td>
                  <td className="px-4 py-3 font-mono text-xs text-slate-500 truncate max-w-xs">{event.workerId}</td>
                  <td className="px-4 py-3">{event.message}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
