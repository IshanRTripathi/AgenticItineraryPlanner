/**
 * Destination Selection Step
 * Step 1 of trip wizard
 */

import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { MapPin } from 'lucide-react';

interface DestinationStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

const POPULAR_DESTINATIONS = [
  'Paris, France',
  'Tokyo, Japan',
  'Bali, Indonesia',
  'Dubai, UAE',
  'New York, USA',
  'London, UK',
  'Barcelona, Spain',
  'Maldives',
];

export function DestinationStep({ data, onDataChange }: DestinationStepProps) {
  const [destination, setDestination] = useState(data.destination || '');

  const handleDestinationChange = (value: string) => {
    setDestination(value);
    onDataChange({ destination: value });
  };

  return (
    <div className="space-y-6">
      {/* Destination Input */}
      <div>
        <Label htmlFor="destination">Destination</Label>
        <div className="relative mt-2">
          <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <Input
            id="destination"
            value={destination}
            onChange={(e) => handleDestinationChange(e.target.value)}
            placeholder="Enter city or country"
            className="pl-10 h-12"
          />
        </div>
      </div>

      {/* Popular Destinations */}
      <div>
        <Label className="mb-3 block">Popular Destinations</Label>
        <div className="grid grid-cols-4 gap-2">
          {POPULAR_DESTINATIONS.slice(0, 4).map((dest) => (
            <button
              key={dest}
              onClick={() => handleDestinationChange(dest)}
              className="px-3 py-2 rounded-lg border border-border hover:border-primary hover:bg-primary/5 transition-colors text-sm"
            >
              {dest}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
