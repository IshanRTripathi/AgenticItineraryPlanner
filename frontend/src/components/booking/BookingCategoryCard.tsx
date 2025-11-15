/**
 * Booking Category Card Component
 * Collapsible card showing bookings by category
 */

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, Clock, DollarSign } from 'lucide-react';
import { cn } from '@/lib/utils';
import { BookingCategoryGroup, CategorizedBooking, getNodeIcon } from '@/utils/categorizeBookings';
import { BookingModal } from '@/components/booking/BookingModal';
import { buildEaseMyTripUrl } from '@/utils/easemytripUrlBuilder';

/**
 * Format time string to be more user-friendly
 * Converts various time formats to 12-hour format with AM/PM
 */
function formatTime(timeStr: string | number): string {
  if (!timeStr) return '';
  
  // If it's a number (timestamp), convert to Date and format
  if (typeof timeStr === 'number') {
    try {
      const date = new Date(timeStr);
      return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });
    } catch (e) {
      return '';
    }
  }
  
  // If already in 12-hour format with AM/PM, return as is
  if (/\d{1,2}:\d{2}\s*(AM|PM)/i.test(timeStr)) {
    return timeStr;
  }
  
  // Try to parse 24-hour format (HH:MM or H:MM)
  const match = timeStr.match(/(\d{1,2}):(\d{2})/);
  if (match) {
    let hours = parseInt(match[1], 10);
    const minutes = match[2];
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12 || 12; // Convert 0 to 12 for midnight
    return `${hours}:${minutes} ${ampm}`;
  }
  
  // If no match, return original
  return timeStr;
}

interface BookingCategoryCardProps {
  group: BookingCategoryGroup;
  defaultExpanded?: boolean;
  onBook?: (booking: CategorizedBooking) => void;
  onViewDetails?: (booking: CategorizedBooking) => void;
  onMarkBooked?: (booking: CategorizedBooking, confirmationCode: string) => void;
  itinerary?: any;
}

export function BookingCategoryCard({
  group,
  defaultExpanded = false,
  onBook,
  onViewDetails,
  onMarkBooked,
  itinerary,
}: BookingCategoryCardProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  const [editingBooking, setEditingBooking] = useState<string | null>(null);
  const [confirmationCode, setConfirmationCode] = useState('');
  const [bookingModalState, setBookingModalState] = useState<{
    isOpen: boolean;
    booking: CategorizedBooking | null;
  }>({ isOpen: false, booking: null });

  const handleBookNow = (booking: CategorizedBooking) => {
    setBookingModalState({ isOpen: true, booking });
  };

  const closeBookingModal = () => {
    setBookingModalState({ isOpen: false, booking: null });
  };
  
  const totalItems = group.items.length;
  const hasBookings = group.bookedCount > 0;
  const progressPercentage = totalItems > 0 ? Math.round((group.bookedCount / totalItems) * 100) : 0;

  // Category-specific colors - subtle and elegant
  const categoryStyles = {
    accommodation: {
      accent: 'border-l-blue-500',
      background: 'bg-blue-50/30',
      cardBorder: 'border-l-blue-400',
      icon: 'text-blue-600',
      progress: 'bg-blue-500',
    },
    transport: {
      accent: 'border-l-purple-500',
      background: 'bg-purple-50/30',
      cardBorder: 'border-l-purple-400',
      icon: 'text-purple-600',
      progress: 'bg-purple-500',
    },
    activity: {
      accent: 'border-l-green-500',
      background: 'bg-green-50/30',
      cardBorder: 'border-l-green-400',
      icon: 'text-green-600',
      progress: 'bg-green-500',
    },
    meal: {
      accent: 'border-l-orange-500',
      background: 'bg-orange-50/30',
      cardBorder: 'border-l-orange-400',
      icon: 'text-orange-600',
      progress: 'bg-orange-500',
    },
  };

  const styles = categoryStyles[group.category as keyof typeof categoryStyles] || {
    accent: 'border-l-gray-500',
    background: 'bg-gray-50/30',
    cardBorder: 'border-l-gray-400',
    icon: 'text-gray-600',
    progress: 'bg-gray-500',
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 overflow-hidden shadow-sm">
      {/* Header with subtle left accent */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className={cn(
          'w-full flex items-center justify-between p-4 hover:bg-gray-50 transition-colors border-l-4',
          styles.accent
        )}
      >
        <div className="flex items-center gap-3">
          {/* Icon with category color */}
          <div className={cn('text-2xl', styles.icon)}>{group.icon}</div>
          
          {/* Label and Stats */}
          <div className="text-left">
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold text-gray-900">
                {group.label}
              </span>
              <span className="text-xs text-muted-foreground">
                ({totalItems})
              </span>
            </div>
            
            {/* Progress Bar */}
            <div className="mt-2">
              <div className="flex items-center gap-2 mb-1">
                <div className="flex-1 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                  <motion.div
                    className={cn('h-full rounded-full', styles.progress)}
                    initial={{ width: 0 }}
                    animate={{ width: `${progressPercentage}%` }}
                    transition={{ duration: 0.5, ease: 'easeOut' }}
                  />
                </div>
                <span className="text-xs font-semibold text-gray-700 min-w-[3ch]">
                  {progressPercentage}%
                </span>
              </div>
              {!isExpanded && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span>{group.bookedCount} of {totalItems} booked</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Chevron */}
        <ChevronDown
          className={cn(
            'w-4 h-4 text-muted-foreground transition-transform',
            isExpanded && 'rotate-180'
          )}
        />
      </button>

      {/* Expanded Content */}
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="border-t border-gray-200"
          >
            <div className={cn('p-3 space-y-2', styles.background)}>
              {/* Items List - White Cards with colored left border */}
              {group.items.map((booking) => {
                return (
                <div key={booking.id} className="space-y-2">
                  <motion.div
                    className={cn(
                      'p-3 rounded-lg transition-all border bg-white border-l-4',
                      styles.cardBorder,
                      booking.status === 'booked'
                        ? 'shadow-md border-r border-t border-b border-gray-200'
                        : 'border-r border-t border-b border-gray-100 hover:shadow-sm'
                    )}
                  >
                    <div className="flex items-start gap-3">
                      {/* Icon */}
                      <div className="text-xl flex-shrink-0 mt-0.5">{getNodeIcon(booking.type)}</div>

                      {/* Content */}
                      <div className="flex-1 min-w-0">
                        {/* Title and Status */}
                        <div className="flex items-start justify-between gap-2 mb-1">
                          <h4 className="font-semibold text-sm text-gray-900 line-clamp-1">
                            {booking.title}
                          </h4>
                          {booking.status === 'booked' && (
                            <span className="flex-shrink-0 inline-flex items-center gap-1 px-2 py-0.5 bg-primary rounded-full text-xs font-semibold text-white">
                              <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                              </svg>
                            </span>
                          )}
                        </div>

                        {/* Day and Location */}
                        <p className="text-xs text-muted-foreground mb-2">
                          Day {booking.dayNumber} • {booking.dayLocation}
                        </p>

                        {/* Details Row */}
                        <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground mb-2">
                          {booking.timing?.startTime && (
                            <div className="flex items-center gap-1">
                              <Clock className="w-3 h-3" />
                              {formatTime(booking.timing.startTime)}
                            </div>
                          )}
                          {booking.cost?.amount && (
                            <div className="flex items-center gap-1">
                              <DollarSign className="w-3 h-3" />
                              ${booking.cost.amount}
                            </div>
                          )}
                          {booking.bookingRef && (
                            <div className="flex items-center gap-1 font-mono text-primary font-semibold">
                              {booking.bookingRef}
                            </div>
                          )}
                        </div>

                        {/* Actions */}
                        <div className="flex gap-2">
                          {booking.status !== 'booked' && (
                            <>
                              <button
                                onClick={() => handleBookNow(booking)}
                                className="px-3 py-1 text-xs font-medium bg-primary text-white rounded-md hover:bg-primary/90 transition-colors"
                              >
                                Book Now
                              </button>
                              <button
                                onClick={() => setEditingBooking(booking.id)}
                                className="px-3 py-1 text-xs font-medium bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors"
                              >
                                Mark as Booked
                              </button>
                            </>
                          )}
                          {booking.status === 'booked' && onViewDetails && (
                            <button
                              onClick={() => onViewDetails(booking)}
                              className="px-3 py-1 text-xs font-medium bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors"
                            >
                              View Details
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  </motion.div>

                  {/* Edit Booking Form */}
                  <AnimatePresence>
                    {editingBooking === booking.id && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="overflow-hidden"
                      >
                        <div className="p-3 bg-gray-50 rounded-lg border border-gray-200">
                          <label className="block text-xs font-medium text-gray-700 mb-2">
                            Confirmation Code
                          </label>
                          <div className="flex gap-2">
                            <input
                              type="text"
                              value={confirmationCode}
                              onChange={(e) => setConfirmationCode(e.target.value)}
                              placeholder="Enter confirmation code"
                              className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                              autoFocus
                            />
                            <button
                              onClick={() => {
                                if (confirmationCode.trim() && onMarkBooked) {
                                  onMarkBooked(booking, confirmationCode.trim());
                                  setEditingBooking(null);
                                  setConfirmationCode('');
                                }
                              }}
                              className="px-4 py-2 text-sm font-medium bg-primary text-white rounded-md hover:bg-primary/90 transition-colors"
                            >
                              Save
                            </button>
                            <button
                              onClick={() => {
                                setEditingBooking(null);
                                setConfirmationCode('');
                              }}
                              className="px-4 py-2 text-sm font-medium bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition-colors"
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              );
              })}

              {/* Empty State */}
              {group.items.length === 0 && (
                <div className="text-center py-6 text-sm text-muted-foreground">
                  No {group.label.toLowerCase()} items
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Booking Modal */}
      {bookingModalState.booking && (
        <BookingModal
          isOpen={bookingModalState.isOpen}
          onClose={closeBookingModal}
          bookingType={
            bookingModalState.booking.category === 'accommodation' ? 'hotel' :
            bookingModalState.booking.category === 'transport' ? 'flight' : 'activity'
          }
          itemName={bookingModalState.booking.title}
          providerUrl={buildEaseMyTripUrl(bookingModalState.booking, itinerary)}
          onMarkBooked={onBook ? (ref) => onBook(bookingModalState.booking!) : undefined}
        />
      )}
    </div>
  );
}
