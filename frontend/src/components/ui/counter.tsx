/**
 * Counter Component
 * Increment/decrement number input
 */

import * as React from 'react';
import { Button } from './button';
import { cn } from '@/lib/utils';
import { Minus, Plus } from 'lucide-react';

interface CounterProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  step?: number;
  label?: string;
  className?: string;
}

export function Counter({
  value,
  onChange,
  min = 0,
  max = 99,
  step = 1,
  label,
  className,
}: CounterProps) {
  const handleDecrement = () => {
    const newValue = value - step;
    if (newValue >= min) {
      onChange(newValue);
      // Haptic feedback if supported
      if ('vibrate' in navigator) {
        navigator.vibrate(10);
      }
    }
  };

  const handleIncrement = () => {
    const newValue = value + step;
    if (newValue <= max) {
      onChange(newValue);
      // Haptic feedback if supported
      if ('vibrate' in navigator) {
        navigator.vibrate(10);
      }
    }
  };

  const isMinDisabled = value <= min;
  const isMaxDisabled = value >= max;

  return (
    <div className={cn('flex items-center gap-3', className)}>
      {label && (
        <span className="text-sm font-medium flex-1">{label}</span>
      )}
      
      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={handleDecrement}
          disabled={isMinDisabled}
          className={cn(
            'h-8 w-8 rounded-full border-2',
            'transition-all duration-200',
            isMinDisabled && 'opacity-30 cursor-not-allowed'
          )}
        >
          <Minus className="h-4 w-4" />
        </Button>

        <div className="w-12 text-center">
          <span className="text-lg font-bold">{value}</span>
        </div>

        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={handleIncrement}
          disabled={isMaxDisabled}
          className={cn(
            'h-8 w-8 rounded-full border-2',
            'transition-all duration-200',
            isMaxDisabled && 'opacity-30 cursor-not-allowed'
          )}
        >
          <Plus className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
