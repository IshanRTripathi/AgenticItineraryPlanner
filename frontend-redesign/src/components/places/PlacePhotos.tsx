/**
 * Place Photos Component
 * Displays photos for a place using Google Places API
 */

import { useQuery } from '@tanstack/react-query';
import { Loader2, Image as ImageIcon } from 'lucide-react';
import { useState } from 'react';
import { Dialog, DialogContent } from '@/components/ui/dialog';

interface PlacePhotosProps {
  placeId: string;
  maxPhotos?: number;
}

interface Photo {
  url: string;
  attribution: string;
}

export function PlacePhotos({ placeId, maxPhotos = 6 }: PlacePhotosProps) {
  const [selectedPhoto, setSelectedPhoto] = useState<Photo | null>(null);

  const { data: photos, isLoading } = useQuery<Photo[]>({
    queryKey: ['place-photos', placeId],
    queryFn: async () => {
      // In a real implementation, this would call your backend
      // which would use Google Places API
      const response = await fetch(
        `/api/v1/places/${placeId}/photos?maxResults=${maxPhotos}`
      );
      
      if (!response.ok) {
        throw new Error('Failed to fetch photos');
      }

      return response.json();
    },
    enabled: !!placeId,
    staleTime: 60 * 60 * 1000, // 1 hour
  });

  if (isLoading) {
    return (
      <div className="grid grid-cols-3 gap-2">
        {[...Array(3)].map((_, i) => (
          <div
            key={i}
            className="w-full h-32 bg-muted rounded animate-pulse flex items-center justify-center"
          >
            <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
          </div>
        ))}
      </div>
    );
  }

  if (!photos || photos.length === 0) {
    return (
      <div className="grid grid-cols-3 gap-2">
        <div className="w-full h-32 bg-muted rounded flex items-center justify-center col-span-3">
          <div className="text-center text-muted-foreground">
            <ImageIcon className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">No photos available</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="grid grid-cols-3 gap-2">
        {photos.slice(0, maxPhotos).map((photo, i) => (
          <button
            key={i}
            onClick={() => setSelectedPhoto(photo)}
            className="relative w-full h-32 overflow-hidden rounded group cursor-pointer"
          >
            <img
              src={photo.url}
              alt={photo.attribution}
              className="w-full h-full object-cover transition-transform group-hover:scale-110"
              loading="lazy"
            />
            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors" />
          </button>
        ))}
      </div>

      {/* Photo Lightbox */}
      <Dialog open={!!selectedPhoto} onOpenChange={() => setSelectedPhoto(null)}>
        <DialogContent className="max-w-4xl">
          {selectedPhoto && (
            <div className="space-y-4">
              <img
                src={selectedPhoto.url}
                alt={selectedPhoto.attribution}
                className="w-full h-auto rounded-lg"
              />
              <p className="text-xs text-muted-foreground">
                {selectedPhoto.attribution}
              </p>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
