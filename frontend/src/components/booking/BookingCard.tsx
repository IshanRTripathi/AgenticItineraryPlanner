/**
 * Booking Card Component
 * Displays individual booking details
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BookingStatusBadge } from './BookingStatusBadge';
import { Plane, Hotel, Camera, Calendar, MapPin, DollarSign } from 'lucide-react';

type BookingType = 'flight' | 'hotel' | 'activity';
type BookingStatus = 'confirmed' | 'pending' | 'cancelled' | 'failed';

interface Booking {
  id: string;
  type: BookingType;
  name: string;
  date: string;
  location: string;
  price: number;
  status: BookingStatus;
  confirmationCode?: string;
}

interface BookingCardProps {
  booking: Booking;
}

const typeConfig = {
  flight: { icon: Plane, label: 'Flight' },
  hotel: { icon: Hotel, label: 'Hotel' },
  activity: { icon: Camera, label: 'Activity' },
};

export function BookingCard({ booking }: BookingCardProps) {
  const config = typeConfig[booking.type];
  const Icon = config.icon;

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <CardTitle className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
              <Icon className="w-5 h-5 text-primary" />
            </div>
            <div>
              <div className="text-lg">{booking.name}</div>
              <div className="text-sm font-normal text-muted-foreground">
                {config.label}
              </div>
            </div>
          </CardTitle>
          <BookingStatusBadge status={booking.status} />
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {/* Details */}
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4 text-muted-foreground" />
              <span>{formatDate(booking.date)}</span>
            </div>
            <div className="flex items-center gap-2">
              <MapPin className="w-4 h-4 text-muted-foreground" />
              <span>{booking.location}</span>
            </div>
          </div>

          {/* Price */}
          <div className="flex items-center gap-2 text-lg font-semibold text-primary">
            <DollarSign className="w-5 h-5" />
            {booking.price.toLocaleString()}
          </div>

          {/* Confirmation Code */}
          {booking.confirmationCode && (
            <div className="bg-muted/50 rounded-lg p-3">
              <div className="text-xs text-muted-foreground mb-1">Confirmation Code</div>
              <div className="font-mono font-semibold">{booking.confirmationCode}</div>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-2 pt-2">
            <Button variant="outline" size="sm" className="flex-1">
              View Details
            </Button>
            {booking.status === 'confirmed' && (
              <Button variant="ghost" size="sm">
                Cancel
              </Button>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
