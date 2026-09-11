import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';
import type { TimeSeriesPoint } from '../../types';

interface AreaChartWidgetProps {
  data: TimeSeriesPoint[];
  label: string;
  color?: string;
  unit?: string;
  height?: number;
  showGrid?: boolean;
}

function CustomTooltip({ active, payload, label, unit }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-[#111d33] border border-slate-700 rounded-lg px-3 py-2 shadow-card text-xs">
      <p className="text-slate-400 mb-1">{label}</p>
      <p className="text-slate-100 font-semibold">{payload[0].value}{unit}</p>
    </div>
  );
}

export function AreaChartWidget({
  data, label, color = '#06b6d4', unit = '%', height = 120, showGrid = false
}: AreaChartWidgetProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data} margin={{ top: 4, right: 4, left: -28, bottom: 0 }}>
        <defs>
          <linearGradient id={`grad-${label}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor={color} stopOpacity={0.3} />
            <stop offset="95%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>
        {showGrid && (
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
        )}
        <XAxis
          dataKey="time"
          tick={{ fill: '#64748b', fontSize: 10 }}
          tickLine={false}
          axisLine={false}
          interval="preserveStartEnd"
        />
        <YAxis
          domain={[0, unit === '%' ? 100 : 'auto']}
          tick={{ fill: '#64748b', fontSize: 10 }}
          tickLine={false}
          axisLine={false}
          tickFormatter={(v) => `${v}${unit}`}
        />
        <Tooltip content={<CustomTooltip unit={unit} />} />
        <Area
          type="monotone"
          dataKey="value"
          stroke={color}
          strokeWidth={2}
          fill={`url(#grad-${label})`}
          dot={false}
          activeDot={{ r: 4, fill: color, stroke: '#0c1526', strokeWidth: 2 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
