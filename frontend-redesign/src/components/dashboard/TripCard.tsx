/**
 * Trip Card Component
 * Displays individual trip summary
 */

import { Card, CardContent, CardFooter } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Calendar, Users, DollarSign, MapPin, MoreVertical } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Trip {
  id: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: 'upcoming' | 'completed';
  imageUrl: string;
  travelers: number;
  budget: string;
}

interface TripCardProps {
  trip: Trip;
}

export function TripCard({ trip }: TripCardProps) {
  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const getDuration = () => {
    const start = new Date(trip.startDate);
    const end = new Date(trip.endDate);
    const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return `${days} days`;
  };

  return (
    <Card className="overflow-hidden hover:shadow-elevation-3 transition-all duration-300 group">
      {/* Image */}
      <div className="relative h-48 overflow-hidden">
        <img
          src={trip.imageUrl}
          alt={trip.destination}
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
        
        {/* Status Badge */}
        <div className="absolute top-3 right-3">
          <span
            className={cn(
              'px-3 py-1 rounded-full text-xs font-semibold',
              trip.status === 'upcoming'
                ? 'bg-primary text-white'
                : 'bg-muted text-muted-foreground'
            )}
          >
            {trip.status === 'upcoming' ? 'Upcoming' : 'Completed'}
          </span>
        </div>

        {/* Destination */}
        <div className="absolute bottom-3 left-3 right-3">
          <h3 className="text-xl font-bold text-white flex items-center gap-2">
            <MapPin className="w-5 h-5" />
            {trip.destination}
          </h3>
        </div>
      </div>

      {/* Content */}
      <CardContent className="p-4 space-y-3">
        {/* Dates */}
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Calendar className="w-4 h-4" />
          <span>
            {formatDate(trip.startDate)} - {formatDate(trip.endDate)}
          </span>
        </div>

        {/* Details */}
        <div className="flex items-center gap-4 text-sm">
          <div className="flex items-center gap-1.5">
            <Users className="w-4 h-4 text-muted-foreground" />
            <span>{trip.travelers} traveler{trip.travelers > 1 ? 's' : ''}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <DollarSign className="w-4 h-4 text-muted-foreground" />
            <span className="capitalize">{trip.budget}</span>
          </div>
        </div>

        <div className="text-sm font-medium text-primary">
          {getDuration()}
        </div>
      </CardContent>

      {/* Footer */}
      <CardFooter className="p-4 pt-0 flex gap-2">
        <Button
          variant="outline"
          className="flex-1"
          onClick={() => window.location.href = `/trip/${trip.id}`}
        >
          View Details
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="px-2"
        >
          <MoreVertical className="w-4 h-4" />
        </Button>
      </CardFooter>
    </Card>
  );
}
