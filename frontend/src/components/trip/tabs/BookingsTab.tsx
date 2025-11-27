/**
 * Bookings Tab - Intelligent Booking Interface
 * Enhanced with smart categorization and EaseMyTrip integration
 */

import { useState, useMemo, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { BookingModal } from '@/components/booking/BookingModal';
import { BookingCategoryCard } from '@/components/booking/BookingCategoryCard';
import { categorizeBookings, CategorizedBooking } from '@/utils/categorizeBookings';
import { buildEaseMyTripUrl } from '@/utils/easemytripUrlBuilder';
import { slideUp, staggerChildren } from '@/utils/animations';
import { useToast } from '@/components/ui/use-toast';
import { useTranslation } from '@/i18n';
import { useSearchParams } from 'react-router-dom';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

interface BookingsTabProps {
  itinerary: any; // NormalizedItinerary type
}

export function BookingsTab({ itinerary }: BookingsTabProps) {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  
  // Scroll to top when component mounts
  useEffect(() => {
    // Find the scrollable container (main element on desktop, window on mobile)
    const mainElement = document.querySelector('main');
    const scrollContainer = mainElement || window;
    
    if (scrollContainer === window) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      (scrollContainer as Element).scrollTo({ top: 0, behavior: 'smooth' });
    }
  }, []);
  const [bookingModal, setBookingModal] = useState({
    isOpen: false,
    type: 'flight' as 'flight' | 'hotel' | 'activity',
    name: '',
    url: '',
    booking: null as CategorizedBooking | null,
  });
  const { toast } = useToast();

  // Categorize all bookings
  const categoryGroups = categorizeBookings(itinerary);
  
  // Calculate summary stats
  const totalItems = categoryGroups.reduce((sum, g) => sum + g.items.length, 0);
  const totalBooked = categoryGroups.reduce((sum, g) => sum + g.bookedCount, 0);
  const totalAvailable = categoryGroups.reduce((sum, g) => sum + g.availableCount, 0);

  const handleBook = (booking: CategorizedBooking) => {
    // Build EaseMyTrip URL based on booking type and location
    const easemytripUrl = buildEaseMyTripUrl(booking, itinerary);
    
    console.log('[BookingsTab] Opening booking for:', {
      title: booking.title,
      category: booking.category,
      location: booking.dayLocation,
      url: easemytripUrl,
    });

    setBookingModal({
      isOpen: true,
      type: booking.category === 'accommodation' ? 'hotel' : 
            booking.category === 'transport' ? 'flight' : 'activity',
      name: booking.title,
      url: easemytripUrl,
      booking,
    });
  };

  const handleModalMarkBooked = (bookingRef: string) => {
    if (bookingModal.booking) {
      handleMarkBooked(bookingModal.booking, bookingRef);
    }
  };

  const handleViewDetails = (booking: CategorizedBooking) => {
    toast({
      title: 'Booking Details',
      description: `Viewing details for ${booking.title}`,
    });
  };

  const handleMarkBooked = (booking: CategorizedBooking, confirmationCode: string) => {
    // TODO: Update backend with booking status
    toast({
      title: 'Booking Marked',
      description: `${booking.title} marked as booked with code ${confirmationCode}`,
    });
    // Trigger refetch or update local state
  };

  // Create hotel booking group from location-based stays
  const hotelBookingGroup = useMemo(() => {
    const days = itinerary?.days || [];
    if (days.length === 0) return null;

    const locationGroups = days.reduce((acc: any, day: any) => {
      const location = day.location || 'Unknown';
      if (!acc[location]) {
        acc[location] = {
          location,
          startDay: day.dayNumber,
          endDay: day.dayNumber,
          startDate: day.date,
          endDate: day.date,
          days: [],
        };
      }
      acc[location].endDay = day.dayNumber;
      acc[location].endDate = day.date;
      acc[location].days.push(day);
      return acc;
    }, {});

    const hotelStays = Object.values(locationGroups).filter((group: any) => group.days.length > 0);
    
    if (hotelStays.length === 0) return null;

    // Create booking items for each location
    const items: CategorizedBooking[] = hotelStays.map((stay: any) => ({
      id: `hotel-${stay.location}`,
      title: `Hotel in ${stay.location}`,
      type: 'hotel',
      category: 'accommodation' as const,
      dayNumber: stay.startDay,
      dayLocation: stay.location,
      timing: {
        startTime: stay.startDate,
        endTime: stay.endDate,
      },
      location: {
        address: stay.location,
      },
      status: 'available' as const,
      details: {
        nights: stay.days.length,
        checkIn: stay.startDate,
        checkOut: stay.endDate,
        days: stay.days,
      },
    }));

    return {
      category: 'accommodation' as const,
      label: 'Hotels',
      icon: '🏨',
      items,
      bookedCount: 0,
      pendingCount: 0,
      availableCount: items.length,
    };
  }, [itinerary]);

  // Merge hotel group with other categories - hotels at the top
  const allCategoryGroups = useMemo(() => {
    const groups = [...categoryGroups];
    if (hotelBookingGroup) {
      // Insert hotel group at the beginning
      groups.unshift(hotelBookingGroup);
    }
    return groups;
  }, [categoryGroups, hotelBookingGroup]);

  const handleBookHotel = (location: string, checkIn: string, checkOut: string) => {
    const hotelUrl = `https://www.easemytrip.com/hotels/?city=${encodeURIComponent(location)}&cin=${checkIn}&cout=${checkOut}&r=1&a=2&c=0`;
    
    setBookingModal({
      isOpen: true,
      type: 'hotel',
      name: `Hotel in ${location}`,
      url: hotelUrl,
      booking: null,
    });
  };

  return (
    <div className="space-y-6">
      {/* Header with Summary */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white rounded-xl shadow-sm border border-gray-200 p-6"
      >
        <h2 className="text-2xl font-bold mb-2">{t('components.bookingsTab.title')}</h2>
        <p className="text-sm text-muted-foreground mb-4">
          {t('components.bookingsTab.subtitle')}
        </p>
        
        {/* Summary Stats */}
        <div className="flex gap-6">
          <div>
            <div className="text-2xl font-bold text-gray-900">{totalItems}</div>
            <div className="text-xs text-muted-foreground">{t('components.bookingsTab.stats.totalItems')}</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-primary">{totalBooked}</div>
            <div className="text-xs text-muted-foreground">{t('components.bookingsTab.stats.booked')}</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-gray-600">{totalAvailable}</div>
            <div className="text-xs text-muted-foreground">{t('components.bookingsTab.stats.available')}</div>
          </div>
        </div>
      </motion.div>

      {/* Category Cards (including Hotels) */}
      {allCategoryGroups.length > 0 ? (
        <motion.div
          variants={staggerChildren}
          initial="initial"
          animate="animate"
          className="space-y-4"
        >
          {allCategoryGroups.map((group, index) => {
            const expandParam = searchParams.get('expand');
            // Don't auto-expand any cards by default
            const shouldExpand = expandParam === group.category;
            
            // Custom rendering for Hotels category
            if (group.category === 'accommodation' && group.label === 'Hotels') {
              return (
                <motion.div key={`${group.category}-${index}`} variants={slideUp}>
                  <HotelBookingCard
                    group={group}
                    defaultExpanded={shouldExpand}
                    onBookHotel={handleBookHotel}
                  />
                </motion.div>
              );
            }
            
            // Custom handler for other bookings
            const handleBooking = (booking: CategorizedBooking) => {
              if (booking.type === 'hotel' && booking.details) {
                handleBookHotel(
                  booking.dayLocation,
                  booking.details.checkIn,
                  booking.details.checkOut
                );
              } else {
                handleBook(booking);
              }
            };
            
            return (
              <motion.div key={`${group.category}-${index}`} variants={slideUp}>
                <BookingCategoryCard
                  group={group}
                  defaultExpanded={shouldExpand}
                  onBook={handleBooking}
                  onViewDetails={handleViewDetails}
                  onMarkBooked={handleMarkBooked}
                  itinerary={itinerary}
                />
              </motion.div>
            );
          })}
        </motion.div>
      ) : (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center"
        >
          <div className="w-16 h-16 mx-auto rounded-full bg-muted flex items-center justify-center mb-4">
            <span className="text-3xl">📋</span>
          </div>
          <h3 className="text-lg font-semibold mb-2">{t('components.bookingsTab.empty.title')}</h3>
          <p className="text-sm text-muted-foreground">
            {t('components.bookingsTab.empty.description')}
          </p>
        </motion.div>
      )}

      {/* Booking Modal */}
      <BookingModal
        isOpen={bookingModal.isOpen}
        onClose={() => setBookingModal({ ...bookingModal, isOpen: false, booking: null, url: '' })}
        bookingType={bookingModal.type}
        itemName={bookingModal.name}
        providerUrl={bookingModal.url}
        onMarkBooked={handleModalMarkBooked}
      />
    </div>
  );
}

/**
 * Custom Hotel Booking Card Component
 * Shows individual cards for each night with + buttons to add more hotels
 */
interface HotelBookingCardProps {
  group: any;
  defaultExpanded?: boolean;
  onBookHotel: (location: string, checkIn: string, checkOut: string) => void;
}

function HotelBookingCard({ group, defaultExpanded = false, onBookHotel }: HotelBookingCardProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  
  // State to manage hotel stay splits (initially grouped by location)
  const [hotelStays, setHotelStays] = useState<any[]>(() => {
    // Group consecutive nights by location
    const stays: any[] = [];
    group.items.forEach((item: any) => {
      if (item.details?.days) {
        stays.push({
          id: item.id,
          location: item.dayLocation,
          startDay: item.details.days[0].dayNumber,
          endDay: item.details.days[item.details.days.length - 1].dayNumber,
          nights: item.details.days.length,
          checkIn: item.details.checkIn,
          checkOut: item.details.checkOut,
          days: item.details.days,
        });
      }
    });
    return stays.sort((a, b) => a.startDay - b.startDay);
  });

  // Split a stay into two separate stays
  const splitStay = (stayId: string, splitAfterDay: number) => {
    setHotelStays(prev => {
      const newStays: any[] = [];
      prev.forEach(stay => {
        if (stay.id === stayId && stay.nights > 1) {
          const splitIndex = stay.days.findIndex((d: any) => d.dayNumber === splitAfterDay);
          if (splitIndex >= 0 && splitIndex < stay.days.length - 1) {
            // First part
            const firstDays = stay.days.slice(0, splitIndex + 1);
            newStays.push({
              id: `${stay.id}-1`,
              location: stay.location,
              startDay: firstDays[0].dayNumber,
              endDay: firstDays[firstDays.length - 1].dayNumber,
              nights: firstDays.length,
              checkIn: firstDays[0].date,
              checkOut: firstDays[firstDays.length - 1].date,
              days: firstDays,
            });
            // Second part
            const secondDays = stay.days.slice(splitIndex + 1);
            newStays.push({
              id: `${stay.id}-2`,
              location: stay.location,
              startDay: secondDays[0].dayNumber,
              endDay: secondDays[secondDays.length - 1].dayNumber,
              nights: secondDays.length,
              checkIn: secondDays[0].date,
              checkOut: secondDays[secondDays.length - 1].date,
              days: secondDays,
            });
          } else {
            newStays.push(stay);
          }
        } else {
          newStays.push(stay);
        }
      });
      return newStays;
    });
  };

  // Check if a stay can be removed (only if there are multiple stays in the same location)
  const canRemoveStay = (stay: any) => {
    const staysInSameLocation = hotelStays.filter(s => s.location === stay.location);
    return staysInSameLocation.length > 1;
  };

  // Remove a stay and intelligently merge with adjacent stays in same location
  const removeStay = (stayId: string) => {
    setHotelStays(prev => {
      const stayIndex = prev.findIndex(s => s.id === stayId);
      if (stayIndex === -1) return prev;
      
      const removedStay = prev[stayIndex];
      const newStays = prev.filter(s => s.id !== stayId);
      
      // Try to merge with adjacent stays in the same location
      const prevStay = stayIndex > 0 ? prev[stayIndex - 1] : null;
      const nextStay = stayIndex < prev.length - 1 ? prev[stayIndex + 1] : null;
      
      // Check if we can merge with previous stay
      if (prevStay && prevStay.location === removedStay.location && prevStay.endDay === removedStay.startDay - 1) {
        // Merge with previous
        const mergedDays = [...prevStay.days, ...removedStay.days];
        const mergedStay = {
          ...prevStay,
          endDay: removedStay.endDay,
          nights: mergedDays.length,
          checkOut: removedStay.checkOut,
          days: mergedDays,
        };
        
        // Check if we can also merge with next stay
        if (nextStay && nextStay.location === removedStay.location && nextStay.startDay === removedStay.endDay + 1) {
          // Triple merge: prev + removed + next
          const tripleMergedDays = [...mergedDays, ...nextStay.days];
          return newStays
            .filter(s => s.id !== prevStay.id && s.id !== nextStay.id)
            .concat({
              ...prevStay,
              endDay: nextStay.endDay,
              nights: tripleMergedDays.length,
              checkOut: nextStay.checkOut,
              days: tripleMergedDays,
            })
            .sort((a, b) => a.startDay - b.startDay);
        }
        
        // Just merge with previous
        return newStays
          .filter(s => s.id !== prevStay.id)
          .concat(mergedStay)
          .sort((a, b) => a.startDay - b.startDay);
      }
      
      // Check if we can merge with next stay only
      if (nextStay && nextStay.location === removedStay.location && nextStay.startDay === removedStay.endDay + 1) {
        const mergedDays = [...removedStay.days, ...nextStay.days];
        const mergedStay = {
          ...removedStay,
          id: nextStay.id,
          endDay: nextStay.endDay,
          nights: mergedDays.length,
          checkOut: nextStay.checkOut,
          days: mergedDays,
        };
        
        return newStays
          .filter(s => s.id !== nextStay.id)
          .concat(mergedStay)
          .sort((a, b) => a.startDay - b.startDay);
      }
      
      // No merge possible, just remove
      return newStays;
    });
  };

  const totalItems = hotelStays.reduce((sum, stay) => sum + stay.nights, 0);
  const bookedCount = 0; // TODO: Track booked hotels
  const progressPercentage = totalItems > 0 ? Math.round((bookedCount / totalItems) * 100) : 0;

  return (
    <div className="bg-white rounded-lg border border-gray-200 overflow-hidden shadow-sm">
      {/* Header */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className={cn(
          'w-full flex items-center justify-between p-4 hover:bg-gray-50 transition-colors border-l-4 border-l-blue-500'
        )}
      >
        <div className="flex items-center gap-3">
          <div className="text-2xl text-blue-600">{group.icon}</div>
          
          <div className="text-left">
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold text-gray-900">{group.label}</span>
              <span className="text-xs text-muted-foreground">({totalItems} {totalItems === 1 ? 'night' : 'nights'})</span>
            </div>
            
            {/* Progress Bar */}
            <div className="mt-2">
              <div className="flex items-center gap-2 mb-1">
                <div className="flex-1 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                  <motion.div
                    className="h-full rounded-full bg-blue-500"
                    initial={{ width: 0 }}
                    animate={{ width: `${progressPercentage}%` }}
                    transition={{ duration: 0.5, ease: 'easeOut' }}
                  />
                </div>
                <span className="text-xs font-semibold text-gray-700 min-w-[3ch]">{progressPercentage}%</span>
              </div>
              {!isExpanded && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span>{bookedCount} of {totalItems} booked</span>
                </div>
              )}
            </div>
          </div>
        </div>

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
            <div className="p-3 space-y-2 bg-blue-50/30">
              {/* Hotel Stay Cards (grouped by consecutive nights) */}
              {hotelStays.map((stay, index) => (
                <div key={stay.id}>
                  {/* Hotel Stay Card */}
                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05 }}
                    className="p-3 rounded-lg transition-all border bg-white border-l-4 border-l-blue-400 border-r border-t border-b border-gray-100 hover:shadow-sm"
                  >
                    <div className="flex flex-col sm:flex-row items-start gap-3">
                      <div className="text-xl flex-shrink-0 mt-0.5">🏨</div>
                      
                      <div className="flex-1 min-w-0 w-full">
                        <div className="flex flex-col sm:flex-row items-start justify-between gap-2 mb-1">
                          <div className="flex-1">
                            <h4 className="font-semibold text-sm text-gray-900">
                              {stay.nights === 1 
                                ? `Night ${stay.startDay}` 
                                : `Night ${stay.startDay}-${stay.endDay}`}
                            </h4>
                            <p className="text-xs text-muted-foreground">{stay.location}</p>
                          </div>
                          <div className="flex items-center gap-2 w-full sm:w-auto justify-between sm:justify-end">
                            <div className="text-xs text-muted-foreground text-left sm:text-right">
                              {new Date(stay.checkIn).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                              {stay.nights > 1 && ` - ${new Date(stay.checkOut).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`}
                            </div>
                            {canRemoveStay(stay) && (
                              <button
                                onClick={() => removeStay(stay.id)}
                                className="w-5 h-5 flex-shrink-0 rounded-full bg-red-100 hover:bg-red-200 text-red-600 flex items-center justify-center transition-colors touch-manipulation active:scale-95"
                                title="Remove this stay"
                              >
                                <span className="text-xs">×</span>
                              </button>
                            )}
                          </div>
                        </div>

                        <p className="text-xs text-muted-foreground mb-2">
                          {stay.nights} {stay.nights === 1 ? 'night' : 'nights'}
                        </p>

                        <div className="flex flex-col sm:flex-row gap-2">
                          <button
                            onClick={() => onBookHotel(stay.location, stay.checkIn, stay.checkOut)}
                            className="w-full sm:w-auto px-3 py-1.5 sm:py-1 text-xs font-medium bg-primary text-white rounded-md hover:bg-primary/90 transition-colors touch-manipulation active:scale-95"
                          >
                            Book Now
                          </button>
                          <button
                            className="w-full sm:w-auto px-3 py-1.5 sm:py-1 text-xs font-medium bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors touch-manipulation active:scale-95"
                          >
                            Mark as Booked
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* Split buttons for multi-night stays */}
                    {stay.nights > 1 && (
                      <div className="mt-2 pt-2 border-t border-gray-100">
                        <div className="flex items-center gap-1 flex-wrap">
                          <span className="text-xs text-muted-foreground">Split after:</span>
                          {stay.days.slice(0, -1).map((day: any) => (
                            <button
                              key={day.dayNumber}
                              onClick={() => splitStay(stay.id, day.dayNumber)}
                              className="text-xs px-2 py-0.5 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded transition-colors touch-manipulation active:scale-95"
                            >
                              Night {day.dayNumber}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </motion.div>
                </div>
              ))}

              {/* Empty State */}
              {hotelStays.length === 0 && (
                <div className="text-center py-6 text-sm text-muted-foreground">
                  No hotel bookings yet
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
