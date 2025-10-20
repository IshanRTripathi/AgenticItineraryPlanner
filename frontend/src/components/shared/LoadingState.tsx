import React from 'react';
import { Card } from '../ui/card';
import { Loader2 } from 'lucide-react';
import { Progress } from '../ui/progress';

export type LoadingVariant = 'fullPage' | 'inline' | 'progress' | 'minimal';

export interface LoadingStateProps {
  variant?: LoadingVariant;
  message?: string;
  progress?: number; // 0-100 for progress variant
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  showCard?: boolean;
}

/**
 * Standardized loading state component
 * 
 * Variants:
 * - fullPage: Full screen loading with card
 * - inline: Inline loading for sections
 * - progress: Loading with progress bar
 * - minimal: Just spinner, no card
 */
export function LoadingState({
  variant = 'inline',
  message = 'Loading...',
  progress,
  size = 'md',
  className = '',
  showCard = true
}: LoadingStateProps) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12'
  };

  const textSizeClasses = {
    sm: 'text-xs',
    md: 'text-sm',
    lg: 'text-base'
  };

  const spinnerContent = (
    <div className={`flex flex-col items-center justify-center gap-3 ${className}`}>
      <Loader2 className={`${sizeClasses[size]} animate-spin text-blue-600`} />
      {message && (
        <p className={`text-gray-600 ${textSizeClasses[size]} text-center max-w-md`}>
          {message}
        </p>
      )}
      {variant === 'progress' && progress !== undefined && (
        <div className="w-full max-w-md space-y-2">
          <Progress value={progress} className="h-2" />
          <p className="text-xs text-gray-500 text-center">{Math.round(progress)}%</p>
        </div>
      )}
    </div>
  );

  // Full page variant
  if (variant === 'fullPage') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <Card className="p-8 max-w-md w-full">
          {spinnerContent}
        </Card>
      </div>
    );
  }

  // Minimal variant (no card)
  if (variant === 'minimal' || !showCard) {
    return <div className="p-4">{spinnerContent}</div>;
  }

  // Inline and progress variants (with card)
  return (
    <Card className="p-6 m-4">
      {spinnerContent}
    </Card>
  );
}

/**
 * Compact loading spinner for buttons and small spaces
 */
export function LoadingSpinner({ 
  size = 'sm', 
  className = '' 
}: { 
  size?: 'sm' | 'md' | 'lg'; 
  className?: string;
}) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8'
  };

  return (
    <Loader2 
      className={`${sizeClasses[size]} animate-spin text-current ${className}`} 
    />
  );
}

/**
 * Loading overlay for sections
 */
export function LoadingOverlay({ 
  message = 'Loading...', 
  className = '' 
}: { 
  message?: string; 
  className?: string;
}) {
  return (
    <div className={`absolute inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center z-10 ${className}`}>
      <div className="flex flex-col items-center gap-2">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
        <p className="text-sm text-gray-600">{message}</p>
      </div>
    </div>
  );
}
