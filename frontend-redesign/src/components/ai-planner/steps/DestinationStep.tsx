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
      <div className="text-center mb-8">
        <h2 className="text-2xl font-bold text-foreground mb-2">
          Where do you want to go?
        </h2>
        <p className="text-muted-foreground">
          Choose your dream destination
        </p>
      </div>

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
            className="pl-10 h-14 text-lg"
          />
        </div>
      </div>

      {/* Popular Destinations */}
      <div>
        <Label className="mb-3 block">Popular Destinations</Label>
        <div className="flex flex-wrap gap-2">
          {POPULAR_DESTINATIONS.map((dest) => (
            <button
              key={dest}
              onClick={() => handleDestinationChange(dest)}
              className="px-4 py-2 rounded-full border border-border hover:border-primary hover:bg-primary/5 transition-colors text-sm"
            >
              {dest}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
