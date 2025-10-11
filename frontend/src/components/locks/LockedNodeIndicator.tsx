/**
 * Locked Node Indicator Component
 * Visual indicator for locked nodes
 */

import React from 'react';
import { Lock } from 'lucide-react';
import { Badge } from '../ui/badge';

interface LockedNodeIndicatorProps {
  isLocked: boolean;
  variant?: 'badge' | 'icon' | 'overlay';
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  showText?: boolean;
}

export function LockedNodeIndicator({
  isLocked,
  variant = 'badge',
  size = 'md',
  className = '',
  showText = true,
}: LockedNodeIndicatorProps) {
  if (!isLocked) return null;

  const getIconSize = () => {
    switch (size) {
      case 'sm': return 'w-3 h-3';
      case 'lg': return 'w-6 h-6';
      default: return 'w-4 h-4';
    }
  };

  if (variant === 'badge') {
    return (
      <Badge variant="destructive" className={`flex items-center gap-1 ${className}`}>
        <Lock className={getIconSize()} />
        {showText && <span>Locked</span>}
      </Badge>
    );
  }

  if (variant === 'icon') {
    return (
      <div className={`flex items-center gap-1 text-red-600 ${className}`}>
        <Lock className={getIconSize()} />
        {showText && <span className="text-sm font-medium">Locked</span>}
      </div>
    );
  }

  // Overlay variant
  return (
    <div className={`absolute top-2 right-2 bg-red-500 text-white rounded-full p-2 shadow-lg ${className}`}>
      <Lock className={getIconSize()} />
    </div>
  );
}
