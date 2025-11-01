/**
 * Provider Selection Modal
 * Allows users to choose a booking provider
 */

import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Star, ExternalLink } from 'lucide-react';
import { Provider, getProvidersByVertical } from '@/config/providers';

interface ProviderSelectionModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  vertical: 'flight' | 'hotel' | 'activity' | 'train' | 'bus';
  onSelectProvider: (provider: Provider) => void;
}

export function ProviderSelectionModal({
  open,
  onOpenChange,
  vertical,
  onSelectProvider,
}: ProviderSelectionModalProps) {
  const providers = getProvidersByVertical(vertical);

  const verticalLabels = {
    flight: 'Flight',
    hotel: 'Hotel',
    activity: 'Activity',
    train: 'Train',
    bus: 'Bus',
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Choose a {verticalLabels[vertical]} Provider</DialogTitle>
        </DialogHeader>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
          {providers.map((provider) => (
            <Card
              key={provider.id}
              className="p-4 hover:shadow-md transition-shadow cursor-pointer"
              onClick={() => onSelectProvider(provider)}
            >
              <div className="flex items-start gap-4">
                {/* Provider Logo */}
                <div className="w-16 h-16 rounded-lg border flex items-center justify-center bg-muted flex-shrink-0">
                  <img
                    src={provider.logo}
                    alt={provider.name}
                    className="w-12 h-12 object-contain"
                    onError={(e) => {
                      // Fallback to placeholder
                      e.currentTarget.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="%23002B5B" stroke-width="2"%3E%3Crect x="3" y="3" width="18" height="18" rx="2"/%3E%3Cpath d="M9 9h6v6H9z"/%3E%3C/svg%3E';
                    }}
                  />
                </div>

                {/* Provider Info */}
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-base mb-1">{provider.name}</h3>
                  
                  {/* Rating */}
                  {provider.rating && (
                    <div className="flex items-center gap-1 mb-2">
                      <Star className="h-4 w-4 fill-warning text-warning" />
                      <span className="text-sm font-medium">{provider.rating}</span>
                      <span className="text-xs text-muted-foreground">/5.0</span>
                    </div>
                  )}

                  {/* Price Range (Mock) */}
                  <div className="flex items-center gap-2 mb-3">
                    <Badge variant="outline" className="text-xs">
                      From $99
                    </Badge>
                    <Badge variant="secondary" className="text-xs">
                      Best Price
                    </Badge>
                  </div>

                  {/* Select Button */}
                  <Button
                    size="sm"
                    className="w-full"
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectProvider(provider);
                    }}
                  >
                    <ExternalLink className="h-3 w-3 mr-2" />
                    Select Provider
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>

        {providers.length === 0 && (
          <div className="text-center py-8 text-muted-foreground">
            <p>No providers available for {verticalLabels[vertical].toLowerCase()}s</p>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
