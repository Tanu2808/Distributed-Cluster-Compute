import React from 'react';

export interface KeyValueItem {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
}

interface KeyValueTableProps {
  items: KeyValueItem[];
  columns?: 1 | 2 | 3;
  className?: string;
}

export const KeyValueTable: React.FC<KeyValueTableProps> = ({
  items,
  columns = 2,
  className = '',
}) => {
  const colClass = columns === 1
    ? 'grid-cols-1'
    : columns === 3
      ? 'grid-cols-1 md:grid-cols-3'
      : 'grid-cols-1 md:grid-cols-2';

  return (
    <div className={`grid ${colClass} gap-x-6 gap-y-2.5 text-xs ${className}`}>
      {items.map((item, idx) => (
        <div key={idx} className="flex items-baseline justify-between gap-4 py-1 border-b border-console-borderSubtle">
          <span className="text-console-textMuted font-medium shrink-0">
            {item.label}
          </span>
          <span className={`text-console-text text-right truncate ${item.mono ? 'font-mono text-[11px]' : ''}`}>
            {item.value ?? <span className="text-console-textDim">N/A</span>}
          </span>
        </div>
      ))}
    </div>
  );
};
