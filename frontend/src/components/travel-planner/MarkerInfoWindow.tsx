import React from 'react';
import type { MarkerInfoWindowProps } from '../../types/MapTypes';

export const MarkerInfoWindow: React.FC<MarkerInfoWindowProps> = ({
  marker,
  onClose,
}) => {
  const statusColors = {
    planned: 'text-blue-600 bg-blue-50',
    in_progress: 'text-amber-600 bg-amber-50',
    completed: 'text-green-600 bg-green-50',
    skipped: 'text-gray-600 bg-gray-50',
    cancelled: 'text-red-600 bg-red-50',
  };

  const statusLabels = {
    planned: 'Planned',
    in_progress: 'In Progress',
    completed: 'Completed',
    skipped: 'Skipped',
    cancelled: 'Cancelled',
  };

  const typeLabels = {
    attraction: 'Attraction',
    meal: 'Meal',
    accommodation: 'Accommodation',
    transport: 'Transport',
  };

  const typeIcons = {
    attraction: '🎯',
    meal: '🍽️',
    accommodation: '🏨',
    transport: '🚗',
  };

  const renderStars = (rating: number) => {
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 >= 0.5;
    const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

    return (
      <div className="flex items-center">
        {Array.from({ length: fullStars }, (_, i) => (
          <span key={i} className="text-yellow-400">★</span>
        ))}
        {hasHalfStar && <span className="text-yellow-400">☆</span>}
        {Array.from({ length: emptyStars }, (_, i) => (
          <span key={i} className="text-gray-300">☆</span>
        ))}
        <span className="ml-1 text-sm text-gray-600">({rating.toFixed(1)})</span>
      </div>
    );
  };

  return (
    <div className="p-4 max-w-xs bg-white rounded-lg shadow-lg">
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center">
          <span className="text-lg mr-2">{typeIcons[marker.type]}</span>
          <h3 className="font-semibold text-gray-900 text-sm leading-tight">
            {marker.title}
          </h3>
        </div>
        {marker.locked && (
          <span className="text-red-500 text-xs bg-red-50 px-2 py-1 rounded">
            🔒 Locked
          </span>
        )}
      </div>

      {/* Content */}
      <div className="space-y-2 text-sm">
        {/* Type */}
        <div className="flex items-center">
          <span className="font-medium text-gray-700 w-12">Type:</span>
          <span className="text-gray-600">{typeLabels[marker.type]}</span>
        </div>

        {/* Status */}
        <div className="flex items-center">
          <span className="font-medium text-gray-700 w-12">Status:</span>
          <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColors[marker.status]}`}>
            {statusLabels[marker.status]}
          </span>
        </div>

        {/* Rating */}
        {marker.rating && (
          <div className="flex items-center">
            <span className="font-medium text-gray-700 w-12">Rating:</span>
            {renderStars(marker.rating)}
          </div>
        )}

        {/* Coordinates (for debugging) */}
        <div className="flex items-center text-xs text-gray-500">
          <span className="font-medium w-12">Location:</span>
          <span>
            {marker.position.lat.toFixed(4)}, {marker.position.lng.toFixed(4)}
          </span>
        </div>
      </div>

      {/* Actions */}
      <div className="mt-4 flex gap-2">
        {marker.googleMapsUri && (
          <a
            href={marker.googleMapsUri}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 inline-flex items-center justify-center px-3 py-2 text-xs font-medium text-blue-600 bg-blue-50 rounded-md hover:bg-blue-100 transition-colors"
          >
            <span className="mr-1">🗺️</span>
            View in Maps
          </a>
        )}
        
        {onClose && (
          <button
            onClick={onClose}
            className="px-3 py-2 text-xs font-medium text-gray-600 bg-gray-50 rounded-md hover:bg-gray-100 transition-colors"
          >
            Close
          </button>
        )}
      </div>
    </div>
  );
};

export default MarkerInfoWindow;
