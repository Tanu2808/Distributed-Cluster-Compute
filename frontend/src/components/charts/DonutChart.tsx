import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

interface DonutChartProps {
  value: number;      // 0-100
  color?: string;
  size?: number;
  label?: string;
  sublabel?: string;
}

function CustomTooltip({ active, payload }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-[#111d33] border border-slate-700 rounded-lg px-3 py-2 text-xs shadow-card">
      <p className="text-slate-100 font-semibold">{payload[0].value}%</p>
    </div>
  );
}

export function DonutChart({ value, color = '#06b6d4', size = 120, label, sublabel }: DonutChartProps) {
  const clamped = Math.max(0, Math.min(100, value));
  const data = [
    { name: 'used', value: clamped },
    { name: 'free', value: 100 - clamped },
  ];

  return (
    <div className="relative" style={{ width: size, height: size }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            innerRadius={size * 0.33}
            outerRadius={size * 0.46}
            startAngle={90}
            endAngle={-270}
            dataKey="value"
            strokeWidth={0}
          >
            <Cell fill={color} />
            <Cell fill="rgba(255,255,255,0.04)" />
          </Pie>
          <Tooltip content={<CustomTooltip />} />
        </PieChart>
      </ResponsiveContainer>
      <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
        {label && <span className="text-base font-bold text-slate-100">{label}</span>}
        {sublabel && <span className="text-[10px] text-slate-500 mt-0.5">{sublabel}</span>}
      </div>
    </div>
  );
}
