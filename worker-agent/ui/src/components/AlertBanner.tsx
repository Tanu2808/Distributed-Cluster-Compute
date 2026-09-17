import React from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from 'lucide-react';

interface AlertBannerProps {
  type?: 'error' | 'warning' | 'info' | 'success';
  message: React.ReactNode;
  onClose?: () => void;
  className?: string;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({
  type = 'info',
  message,
  onClose,
  className = '',
}) => {
  let containerStyles = 'bg-blue-50 border-blue-200 text-blue-900';
  let Icon = Info;
  let iconStyles = 'text-blue-600';

  if (type === 'error') {
    containerStyles = 'bg-rose-50 border-rose-200 text-rose-900';
    Icon = AlertCircle;
    iconStyles = 'text-rose-600';
  } else if (type === 'warning') {
    containerStyles = 'bg-amber-50 border-amber-200 text-amber-900';
    Icon = AlertTriangle;
    iconStyles = 'text-amber-600';
  } else if (type === 'success') {
    containerStyles = 'bg-emerald-50 border-emerald-200 text-emerald-900';
    Icon = CheckCircle2;
    iconStyles = 'text-emerald-600';
  }

  return (
    <div className={`flex items-start gap-2.5 p-3 rounded-sm border text-xs leading-relaxed ${containerStyles} ${className}`}>
      <Icon className={`w-4 h-4 shrink-0 mt-0.5 ${iconStyles}`} />
      <div className="flex-1 font-mono text-[11px]">
        {message}
      </div>
      {onClose && (
        <button
          onClick={onClose}
          className="text-slate-400 hover:text-slate-700 p-0.5 -mr-1 rounded shrink-0"
          aria-label="Dismiss alert"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  );
};
