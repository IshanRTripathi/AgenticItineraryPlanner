import React from 'react';
import { Card } from '../ui/card';

export type SkeletonVariant = 
  | 'text' 
  | 'card' 
  | 'avatar' 
  | 'button' 
  | 'dayCard' 
  | 'nodeCard' 
  | 'list';

interface SkeletonLoaderProps {
  variant?: SkeletonVariant;
  count?: number;
  className?: string;
}

/**
 * Standardized skeleton loader component
 * 
 * Variants:
 * - text: Single line of text
 * - card: Generic card skeleton
 * - avatar: Circular avatar
 * - button: Button-shaped skeleton
 * - dayCard: Day card skeleton (for itinerary days)
 * - nodeCard: Node card skeleton (for itinerary items)
 * - list: List of items skeleton
 */
export function SkeletonLoader({ 
  variant = 'text', 
  count = 1, 
  className = '' 
}: SkeletonLoaderProps) {
  const renderSkeleton = () => {
    switch (variant) {
      case 'dayCard':
        return <DayCardSkeleton className={className} />;
      case 'nodeCard':
        return <NodeCardSkeleton className={className} />;
      case 'list':
        return <ListSkeleton className={className} />;
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

/**
 * Day card skeleton for itinerary days
 */
export function DayCardSkeleton({ className = '' }: { className?: string }) {
  return (
    <Card className={`p-4 animate-pulse ${className}`}>
      <div className="space-y-3">
        {/* Day header */}
        <div className="flex items-center justify-between">
          <div className="h-6 bg-gray-300 rounded w-32"></div>
          <div className="h-4 bg-gray-200 rounded w-20"></div>
        </div>
        
        {/* Day description */}
        <div className="h-4 bg-gray-200 rounded w-full"></div>
        <div className="h-4 bg-gray-200 rounded w-3/4"></div>
        
        {/* Activities placeholder */}
        <div className="space-y-2 pt-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="flex items-center gap-3 p-2 bg-gray-100 rounded">
              <div className="w-8 h-8 bg-gray-300 rounded"></div>
              <div className="flex-1 space-y-1">
                <div className="h-3 bg-gray-300 rounded w-2/3"></div>
                <div className="h-2 bg-gray-200 rounded w-1/2"></div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Card>
  );
}

/**
 * Node card skeleton for itinerary items (activities, hotels, etc.)
 */
export function NodeCardSkeleton({ className = '' }: { className?: string }) {
  return (
    <Card className={`p-4 animate-pulse ${className}`}>
      <div className="flex gap-3">
        {/* Icon/Image placeholder */}
        <div className="w-12 h-12 bg-gray-300 rounded flex-shrink-0"></div>
        
        {/* Content */}
        <div className="flex-1 space-y-2">
          {/* Title */}
          <div className="h-4 bg-gray-300 rounded w-3/4"></div>
          
          {/* Subtitle/Time */}
          <div className="h-3 bg-gray-200 rounded w-1/2"></div>
          
          {/* Description */}
          <div className="h-3 bg-gray-200 rounded w-full"></div>
          <div className="h-3 bg-gray-200 rounded w-2/3"></div>
          
          {/* Tags/Badges */}
          <div className="flex gap-2 pt-1">
            <div className="h-5 bg-gray-200 rounded w-16"></div>
            <div className="h-5 bg-gray-200 rounded w-20"></div>
          </div>
        </div>
        
        {/* Action buttons */}
        <div className="flex flex-col gap-2">
          <div className="w-8 h-8 bg-gray-200 rounded"></div>
          <div className="w-8 h-8 bg-gray-200 rounded"></div>
        </div>
      </div>
    </Card>
  );
}

/**
 * List skeleton for generic lists
 */
export function ListSkeleton({ 
  count = 5, 
  className = '' 
}: { 
  count?: number; 
  className?: string;
}) {
  return (
    <div className={`space-y-3 animate-pulse ${className}`}>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="flex items-center gap-3 p-3 bg-gray-100 rounded">
          <div className="w-10 h-10 bg-gray-300 rounded"></div>
          <div className="flex-1 space-y-2">
            <div className="h-4 bg-gray-300 rounded w-3/4"></div>
            <div className="h-3 bg-gray-200 rounded w-1/2"></div>
          </div>
          <div className="w-20 h-8 bg-gray-200 rounded"></div>
        </div>
      ))}
    </div>
  );
}

/**
 * Table skeleton for data tables
 */
export function TableSkeleton({ 
  rows = 5, 
  columns = 4, 
  className = '' 
}: { 
  rows?: number; 
  columns?: number; 
  className?: string;
}) {
  return (
    <div className={`space-y-3 animate-pulse ${className}`}>
      {/* Header */}
      <div className="flex gap-4 pb-2 border-b">
        {Array.from({ length: columns }).map((_, i) => (
          <div key={i} className="h-4 bg-gray-300 rounded flex-1"></div>
        ))}
      </div>
      
      {/* Rows */}
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex gap-4">
          {Array.from({ length: columns }).map((_, j) => (
            <div key={j} className="h-4 bg-gray-200 rounded flex-1"></div>
          ))}
        </div>
      ))}
    </div>
  );
}
