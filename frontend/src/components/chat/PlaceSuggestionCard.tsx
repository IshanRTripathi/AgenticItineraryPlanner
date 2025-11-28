import React, { useState } from 'react';
import { Star, MapPin, DollarSign, ExternalLink, Image as ImageIcon, X, ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent } from '@/components/ui/dialog';

interface PlaceSuggestion {
  placeId: string;
  name: string;
  address: string;
  rating?: number;
  userRatingsTotal?: number;
  priceLevel?: number;
  photos?: Array<{ photoReference: string }>;
  openingHours?: string;
  estimatedCost?: number;
  estimatedDuration?: number;
  distanceKm?: number;
  website?: string;
  googleMapsUrl?: string;
  day?: number; // Day number this suggestion is for (1-indexed)
}

interface PlaceSuggestionCardProps {
  suggestion: PlaceSuggestion;
  onSelect: (suggestion: PlaceSuggestion) => void;
  index: number;
}

export function PlaceSuggestionCard({ suggestion, onSelect, index }: PlaceSuggestionCardProps) {
  const [selectedPhotoIndex, setSelectedPhotoIndex] = useState<number | null>(null);
  
  const hasPhotos = suggestion.photos && suggestion.photos.length > 0;
  
  const getPhotoUrl = (photo: any, maxWidth: number = 400) => {
    const apiKey = import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY || '';
    // Handle both camelCase and snake_case
    const photoRef = photo.photoReference || photo.photo_reference;
    if (!photoRef) return '';
    return `https://maps.googleapis.com/maps/api/place/photo?maxwidth=${maxWidth}&photo_reference=${photoRef}&key=${apiKey}`;
  };

  const formatPriceLevel = (level?: number) => {
    if (level === undefined || level === null) return 'Free';
    if (level === 0) return 'Free';
    return '$'.repeat(level);
  };

  const formatNumber = (num?: number) => {
    if (!num) return '0';
    if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
    return num.toString();
  };
  
  const formatDistance = (km?: number) => {
    if (!km) return null;
    if (km < 1) return `${(km * 1000).toFixed(0)}m`;
    return `${km.toFixed(1)}km`;
  };

  return (
    <>
      <div className="border border-gray-200 rounded-lg bg-white hover:shadow-lg transition-shadow">
        <div className="p-4 border-b border-gray-100">
          <div className="flex items-start gap-3">
            <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-base text-gray-900 mb-1 line-clamp-1">
              {suggestion.name}
            </h3>
            
            <div className="flex items-center gap-2 text-sm text-gray-600 mb-2">
              <MapPin className="h-3.5 w-3.5 flex-shrink-0" />
              <span className="line-clamp-1 flex-1">{suggestion.address}</span>
              {suggestion.distanceKm !== undefined && (
                <span className="text-xs font-medium text-primary bg-primary/10 px-2 py-0.5 rounded-full flex-shrink-0">
                  {formatDistance(suggestion.distanceKm)}
                </span>
              )}
            </div>
            
            {suggestion.rating && (
              <div className="flex items-center gap-1">
                <div className="flex items-center gap-1 bg-yellow-50 px-2 py-1 rounded">
                  <Star className="h-3.5 w-3.5 fill-yellow-400 text-yellow-400" />
                  <span className="font-semibold text-sm text-gray-900">
                    {suggestion.rating.toFixed(1)}
                  </span>
                </div>
                {suggestion.userRatingsTotal && (
                  <span className="text-xs text-gray-500">
                    ({formatNumber(suggestion.userRatingsTotal)} reviews)
                  </span>
                )}
              </div>
            )}
            </div>
          </div>
        </div>

        <div className="px-4 py-2.5 border-b border-gray-100 flex items-center gap-2">
        {(suggestion.estimatedCost !== undefined || suggestion.priceLevel !== undefined) && (
          <div className="flex items-center gap-1.5 text-sm">
            <DollarSign className="h-4 w-4 text-green-600" />
            <span className="font-semibold text-gray-900">
              {suggestion.estimatedCost 
                ? `₹${suggestion.estimatedCost.toFixed(0)}`
                : formatPriceLevel(suggestion.priceLevel)
              }
            </span>
          </div>
        )}
        
        <div className="flex-1" />
        
          <div className="flex gap-1.5">
            {hasPhotos && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setSelectedPhotoIndex(0)}
                className="text-xs h-7 px-2"
              >
                <ImageIcon className="h-3 w-3 mr-1" />
                Photos
              </Button>
            )}
            
            {suggestion.googleMapsUrl && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => window.open(suggestion.googleMapsUrl, '_blank')}
                className="text-xs h-7 px-2"
              >
                <ExternalLink className="h-3 w-3 mr-1" />
                Maps
              </Button>
            )}
          </div>
        </div>

        <div className="p-4">
          <Button
            onClick={() => onSelect(suggestion)}
            className="w-full"
            size="md"
          >
            Select This Place
          </Button>
        </div>
      </div>

      {/* Photo Modal */}
      {hasPhotos && selectedPhotoIndex !== null && (
        <Dialog open={true} onOpenChange={() => setSelectedPhotoIndex(null)}>
          <DialogContent className="max-w-4xl p-0 overflow-hidden max-h-[90vh]">
            <div className="relative bg-black flex flex-col">
              {/* Close Button */}
              <button
                onClick={() => setSelectedPhotoIndex(null)}
                className="absolute top-4 right-4 z-10 bg-black/50 hover:bg-black/70 text-white rounded-full p-2 transition-colors"
              >
                <X className="h-5 w-5" />
              </button>

              {/* Main Photo */}
              <div className="relative flex items-center justify-center" style={{ height: '60vh', minHeight: '300px' }}>
                <img
                  src={getPhotoUrl(suggestion.photos![selectedPhotoIndex], 1200)}
                  alt={`${suggestion.name} - Photo ${selectedPhotoIndex + 1}`}
                  className="max-w-full max-h-full object-contain"
                  onError={(e) => {
                    e.currentTarget.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300"%3E%3Crect fill="%23ddd" width="400" height="300"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3EImage not available%3C/text%3E%3C/svg%3E';
                  }}
                />

                {/* Navigation Arrows */}
                {suggestion.photos!.length > 1 && (
                  <>
                    <button
                      onClick={() => setSelectedPhotoIndex((selectedPhotoIndex - 1 + suggestion.photos!.length) % suggestion.photos!.length)}
                      className="absolute left-4 bg-black/50 hover:bg-black/70 text-white rounded-full p-3 transition-colors"
                    >
                      <ChevronLeft className="h-6 w-6" />
                    </button>
                    <button
                      onClick={() => setSelectedPhotoIndex((selectedPhotoIndex + 1) % suggestion.photos!.length)}
                      className="absolute right-4 bg-black/50 hover:bg-black/70 text-white rounded-full p-3 transition-colors"
                    >
                      <ChevronRight className="h-6 w-6" />
                    </button>
                  </>
                )}
              </div>

              {/* Photo Counter */}
              <div className="bg-black/80 text-white px-4 py-3 text-center text-sm">
                Photo {selectedPhotoIndex + 1} of {suggestion.photos!.length}
              </div>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </>
  );
}

export default PlaceSuggestionCard;
