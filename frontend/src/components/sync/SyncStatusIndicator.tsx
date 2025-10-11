import React from 'react';
import { CheckCircle, RefreshCw, AlertCircle, Clock } from 'lucide-react';
import { Badge } from '../ui/badge';

interface SyncStatusIndicatorProps {
  status: 'synced' | 'syncing' | 'error' | 'pending';
  pendingCount?: number;
  lastSyncTime?: Date;
  onManualSync?: () => void;
  className?: string;
}

export function SyncStatusIndicator({
  status,
  pendingCount = 0,
  lastSyncTime,
  onManualSync,
  className = '',
}: SyncStatusIndicatorProps) {
  const getStatusConfig = () => {
    switch (status) {
      case 'synced':
        return {
          icon: <CheckCircle className="w-4 h-4" />,
          text: 'Synced',
          color: 'bg-green-100 text-green-700 border-green-200',
        };
      case 'syncing':
        return {
          icon: <RefreshCw className="w-4 h-4 animate-spin" />,
          text: 'Syncing',
          color: 'bg-blue-100 text-blue-700 border-blue-200',
        };
      case 'error':
        return {
          icon: <AlertCircle className="w-4 h-4" />,
          text: 'Sync Error',
          color: 'bg-red-100 text-red-700 border-red-200',
        };
      case 'pending':
        return {
          icon: <Clock className="w-4 h-4" />,
          text: 'Pending',
          color: 'bg-yellow-100 text-yellow-700 border-yellow-200',
        };
    }
  };

  const config = getStatusConfig();

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <Badge variant="outline" className={`${config.color} flex items-center gap-1`}>
        {config.icon}
        <span>{config.text}</span>
        {pendingCount > 0 && <span className="ml-1">({pendingCount})</span>}
      </Badge>
      {lastSyncTime && status === 'synced' && (
        <span className="text-xs text-gray-500">
          {new Date(lastSyncTime).toLocaleTimeString()}
        </span>
      )}
      {onManualSync && (
        <button
          onClick={onManualSync}
          className="text-xs text-blue-600 hover:text-blue-800 underline"
        >
          Sync Now
        </button>
      )}
    </div>
  );
}
