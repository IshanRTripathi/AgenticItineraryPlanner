/**
 * Node Lock Toggle Component
 * Provides lock/unlock functionality for itinerary nodes
 */

import React, { useState } from 'react';
import { Button } from '../ui/button';
import { Lock, Unlock, Loader2 } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '../ui/tooltip';
import { apiClient } from '../../services/apiClient';

interface NodeLockToggleProps {
  nodeId: string;
  itineraryId: string;
  isLocked: boolean;
  onLockChange?: (locked: boolean) => void;
  onError?: (error: Error) => void;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'icon' | 'button';
  className?: string;
  disabled?: boolean;
}

export function NodeLockToggle({
  nodeId,
  itineraryId,
  isLocked,
  onLockChange,
  onError,
  size = 'sm',
  variant = 'icon',
  className = '',
  disabled = false,
}: NodeLockToggleProps) {
  const [loading, setLoading] = useState(false);
  const [localLocked, setLocalLocked] = useState(isLocked);

  const handleToggle = async () => {
    if (loading || disabled) return;

    const newLockState = !localLocked;
    setLoading(true);

    try {
      const result = await apiClient.toggleNodeLock(itineraryId, nodeId, newLockState);
      
      if (result.success) {
        setLocalLocked(newLockState);
        onLockChange?.(newLockState);
      } else {
        throw new Error(result.message || 'Failed to toggle lock');
      }
    } catch (error) {
      
      onError?.(error as Error);
      
      // Show error feedback
      alert(`Failed to ${newLockState ? 'lock' : 'unlock'} node. Please try again.`);
    } finally {
      setLoading(false);
    }
  };

  const getIcon = () => {
    if (loading) return <Loader2 className="w-4 h-4 animate-spin" />;
    return localLocked ? <Lock className="w-4 h-4" /> : <Unlock className="w-4 h-4" />;
  };

  const getTooltipText = () => {
    if (disabled) return 'Lock toggle disabled';
    if (loading) return localLocked ? 'Unlocking...' : 'Locking...';
    return localLocked ? 'Unlock node (allow AI changes)' : 'Lock node (prevent AI changes)';
  };

  const getButtonSize = () => {
    switch (size) {
      case 'sm': return 'sm';
      case 'lg': return 'lg';
      default: return 'default';
    }
  };

  if (variant === 'icon') {
    return (
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant={localLocked ? 'default' : 'outline'}
              size={getButtonSize()}
              onClick={handleToggle}
              disabled={disabled || loading}
              className={`${className} ${localLocked ? 'bg-red-500 hover:bg-red-600' : ''}`}
            >
              {getIcon()}
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>{getTooltipText()}</p>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    );
  }

  // Button variant with text
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            variant={localLocked ? 'default' : 'outline'}
            size={getButtonSize()}
            onClick={handleToggle}
            disabled={disabled || loading}
            className={`${className} ${localLocked ? 'bg-red-500 hover:bg-red-600' : ''} flex items-center gap-2`}
          >
            {getIcon()}
            <span>{localLocked ? 'Locked' : 'Unlocked'}</span>
          </Button>
        </TooltipTrigger>
        <TooltipContent>
          <p>{getTooltipText()}</p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}

