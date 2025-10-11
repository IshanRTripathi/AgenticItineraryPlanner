import React from 'react';

interface SkeletonLoaderProps {
  variant?: 'text' | 'card' | 'avatar' | 'button';
  count?: number;
  className?: string;
}

export function SkeletonLoader({ variant = 'text', count = 1, className = '' }: SkeletonLoaderProps) {
  const renderSkeleton = () => {
    switch (variant) {
      case 'card':
        return (
          <div className={`bg-gray-200 rounded-lg p-4 animate-pulse ${className}`}>
            <div className="h-4 bg-gray-300 rounded w-3/4 mb-2"></div>
            <div className="h-3 bg-gray-300 rounded w-1/2"></div>
          </div>
        );
      case 'avatar':
        return <div className={`w-10 h-10 bg-gray-300 rounded-full animate-pulse ${className}`}></div>;
      case 'button':
        return <div className={`h-10 bg-gray-300 rounded animate-pulse ${className}`}></div>;
      default:
        return <div className={`h-4 bg-gray-300 rounded animate-pulse ${className}`}></div>;
    }
  };

  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="mb-2">
          {renderSkeleton()}
        </div>
      ))}
    </>
  );
}
