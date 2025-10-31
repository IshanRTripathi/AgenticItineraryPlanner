/**
 * Booking Item Card Component
 * Individual bookable item card with status and actions
 */

import { motion } from 'framer-motion';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { MapPin, Clock, DollarSign, ExternalLink, Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import { listItem } from '@/utils/animations';
import { CategorizedBooking, getCategoryColor, getNodeIcon } from '@/utils/categorizeBookings';

interface BookingItemCardProps {
  booking: CategorizedBooking;
  onBook?: (booking: CategorizedBooking) => void;
  onViewDetails?: (booking: CategorizedBooking) => void;
}

export function BookingItemCard({ booking, onBook, onViewDetails }: BookingItemCardProps) {
  const isBooked = booking.status === 'booked';
  const isPending = booking.status === 'pending';
  
  return (
    <motion.div
      variants={listItem}
      className={cn(
        'p-3 rounded-lg border-l-4 bg-muted/50 hover:bg-muted transition-colors',
        getCategoryColor(booking.category)
      )}
    >
      <div className="flex items-start gap-3">
        {/* Icon */}
        <div className="text-xl flex-shrink-0">{getNodeIcon(booking.type)}</div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          {/* Title and Status */}
          <div className="flex items-start justify-between gap-2 mb-1">
            <h4 className="font-semibold text-sm">{booking.title}</h4>
            {isBooked && (
              <Badge variant="default" className="text-xs flex-shrink-0">
                <Check className="w-3 h-3 mr-1" />
                Booked
              </Badge>
            )}
            {isPending && (
              <Badge variant="secondary" className="text-xs flex-shrink-0">
                ⏳ Pending
              </Badge>
            )}
          </div>

          {/* Day and Location */}
          <p className="text-xs text-muted-foreground mb-2">
            Day {booking.dayNumber} • {booking.dayLocation}
          </p>

          {/* Details */}
          <div className="flex flex-wrap gap-3 text-xs text-muted-foreground mb-2">
            {booking.timing?.startTime && (
              <div className="flex items-center gap-1">
                <Clock className="w-3 h-3" />
                {booking.timing.startTime}
                {booking.timing.duration && ` (${booking.timing.duration})`}
              </div>
            )}
            {booking.location?.address && (
              <div className="flex items-center gap-1">
                <MapPin className="w-3 h-3" />
                <span className="truncate max-w-[200px]">{booking.location.address}</span>
              </div>
            )}
            {booking.cost?.amount && (
              <div className="flex items-center gap-1">
                <DollarSign className="w-3 h-3" />
                ${booking.cost.amount}
              </div>
            )}
          </div>

          {/* Booking Reference */}
          {booking.bookingRef && (
            <p className="text-xs text-muted-foreground mb-2">
              Confirmation: <span className="font-mono font-semibold">{booking.bookingRef}</span>
            </p>
          )}

          {/* Actions */}
          <div className="flex gap-2 mt-2">
            {!isBooked && onBook && (
              <Button
                size="sm"
                variant="outline"
                className="h-7 text-xs"
                onClick={() => onBook(booking)}
              >
                <ExternalLink className="w-3 h-3 mr-1" />
                Book Now
              </Button>
            )}
            {isBooked && onViewDetails && (
              <Button
                size="sm"
                variant="ghost"
                className="h-7 text-xs"
                onClick={() => onViewDetails(booking)}
              >
                View Details
              </Button>
            )}
          </div>
        </div>
      </div>
    </motion.div>
  );
}
