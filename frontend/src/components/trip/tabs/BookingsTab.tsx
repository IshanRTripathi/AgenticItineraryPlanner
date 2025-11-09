/**
 * Bookings Tab - Intelligent Booking Interface
 * Enhanced with smart categorization and EaseMyTrip integration
 */

import { useState } from 'react';
import { motion } from 'framer-motion';
import { BookingModal } from '@/components/booking/BookingModal';
import { BookingCategoryCard } from '@/components/booking/BookingCategoryCard';
import { categorizeBookings, CategorizedBooking } from '@/utils/categorizeBookings';
import { buildEaseMyTripUrl } from '@/utils/easemytripUrlBuilder';
import { slideUp, staggerChildren } from '@/utils/animations';
import { useToast } from '@/components/ui/use-toast';
import { useTranslation } from '@/i18n';

interface BookingsTabProps {
  itinerary: any; // NormalizedItinerary type
}

export function BookingsTab({ itinerary }: BookingsTabProps) {
  const { t } = useTranslation();
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

      {/* Category Cards */}
      {categoryGroups.length > 0 ? (
        <motion.div
          variants={staggerChildren}
          initial="initial"
          animate="animate"
          className="space-y-4"
        >
          {categoryGroups.map((group, index) => (
            <motion.div key={group.category} variants={slideUp}>
              <BookingCategoryCard
                group={group}
                defaultExpanded={index === 0}
                onBook={handleBook}
                onViewDetails={handleViewDetails}
                onMarkBooked={handleMarkBooked}
                itinerary={itinerary}
              />
            </motion.div>
          ))}
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
