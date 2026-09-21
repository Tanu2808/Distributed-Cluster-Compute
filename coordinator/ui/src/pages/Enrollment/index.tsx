import { useQuery } from "@tanstack/react-query";
import { Copy } from "lucide-react";
import { queryKeys } from "../../types/api";

export default function Enrollment() {
  const { data, isLoading, error } = useQuery({
    queryKey: queryKeys.events, // reusing a querykey, but better add enrollment
    queryFn: async () => {
      const res = await fetch("/api/cluster/enrollment-link");
      if (!res.ok) throw new Error("Failed to load");
      return res.json() as Promise<{ enrollmentLink: string }>;
    },
  });

  const handleCopy = () => {
    if (data?.enrollmentLink) {
      navigator.clipboard.writeText(data.enrollmentLink);
    }
  };

  if (isLoading) {
    return <div className="p-6 text-slate-400">Loading enrollment link...</div>;
  }

  if (error) {
    return (
      <div className="p-6 text-red-400">
        Error loading enrollment link: {(error as Error).message}
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-100">Add Worker</h1>
      </div>

      <div className="bg-[#0c1526] border border-slate-800 rounded-lg p-6">
        <h2 className="text-lg font-medium text-slate-200 mb-2">Worker Enrollment Link</h2>
        <p className="text-slate-400 mb-6">
          Use this link when connecting a new Worker Agent. This URL contains the coordinator address and the required registration token.
        </p>

        <div className="flex items-center gap-3 bg-slate-900/50 p-3 rounded-lg border border-slate-700">
          <code className="text-emerald-400 flex-1 break-all select-all">
            {data?.enrollmentLink}
          </code>
          <button
            onClick={handleCopy}
            className="flex items-center gap-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-md transition-colors"
          >
            <Copy size={16} />
            <span>Copy</span>
          </button>
        </div>
      </div>
    </div>
  );
}
