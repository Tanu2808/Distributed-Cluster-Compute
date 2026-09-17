import React from 'react';

interface ConsoleCardProps {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  actions?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  bodyClassName?: string;
}

export const ConsoleCard: React.FC<ConsoleCardProps> = ({
  title,
  subtitle,
  actions,
  children,
  className = '',
  bodyClassName = 'p-4',
}) => {
  return (
    <div className={`bg-console-surface border border-console-border rounded-sm ${className}`}>
      {(title || actions) && (
        <div className="flex items-center justify-between px-4 py-2.5 border-b border-console-border bg-console-subtle">
          <div>
            {title && (
              <h2 className="text-xs font-semibold uppercase tracking-wider text-console-text">
                {title}
              </h2>
            )}
            {subtitle && (
              <p className="text-[11px] text-console-textMuted mt-0.5">
                {subtitle}
              </p>
            )}
          </div>
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      )}
      <div className={bodyClassName}>
        {children}
      </div>
    </div>
  );
};
