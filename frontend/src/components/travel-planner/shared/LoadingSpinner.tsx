/**
 * @deprecated Use LoadingState from components/shared/LoadingState.tsx instead
 * This file is kept for backward compatibility
 */

import React from 'react';
import { LoadingState, LoadingSpinner as NewLoadingSpinner } from '../../shared/LoadingState';
import { SkeletonLoader } from '../../loading/SkeletonLoader';

interface LoadingSpinnerProps {
  message?: string;
  size?: 'sm' | 'md' | 'lg';
  fullScreen?: boolean;
  className?: string;
}

/**
 * @deprecated Use LoadingState component instead
 */
export function LoadingSpinner({ 
  message = 'Loading...', 
  size = 'md',
  fullScreen = false,
  className = ''
}: LoadingSpinnerProps) {
  return (
    <LoadingState
      variant={fullScreen ? 'fullPage' : 'inline'}
      message={message}
      size={size}
      className={className}
    />
  );
}

/**
 * @deprecated Use SkeletonLoader with variant="card" instead
 */
export function SkeletonCard() {
  return <SkeletonLoader variant="card" count={1} />;
}

/**
 * @deprecated Use TableSkeleton from SkeletonLoader instead
 */
export function SkeletonTable() {
  return <SkeletonLoader variant="list" count={5} />;
}
