/**
 * Trip Detail Page
 * Shows complete itinerary with sidebar navigation
 * Task 24: Full sidebar implementation
 */

import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { TripSidebar } from '@/components/trip/TripSidebar';
import { BookingModal } from '@/components/booking/BookingModal';
import { BudgetTab } from '@/components/trip/tabs/BudgetTab';
import { PackingTab } from '@/components/trip/tabs/PackingTab';
import { DocsTab } from '@/components/trip/tabs/DocsTab';
import { ViewTab } from '@/components/trip/tabs/ViewTab';
import { PlanTab } from '@/components/trip/tabs/PlanTab';
import { BookingsTab } from '@/components/trip/tabs/BookingsTab';
import { TripDetailSkeleton } from '@/components/loading/TripDetailSkeleton';
import { ErrorDisplay } from '@/components/error/ErrorDisplay';
import { useItinerary } from '@/hooks/useItinerary';

export function TripDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: itinerary, isLoading, error, refetch } = useItinerary(id);
  
  // Get active tab from URL or default to 'view'
  const activeTab = searchParams.get('tab') || 'view';
  
  const [bookingModal, setBookingModal] = useState<{
    isOpen: boolean;
    type: 'flight' | 'hotel' | 'activity';
    name: string;
  }>({
    isOpen: false,
    type: 'flight',
    name: '',
  });

  // Handle tab change with URL update
  const handleTabChange = (tab: string) => {
    setSearchParams({ tab });
  };

  // Show loading skeleton while fetching data
  if (isLoading) {
    return <TripDetailSkeleton />;
  }

  // Show error state if fetch failed
  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <ErrorDisplay
          error={error as Error}
          onRetry={() => refetch()}
          onGoBack={() => window.history.back()}
        />
      </div>
    );
  }

  // Show error if no itinerary data
  if (!itinerary) {
    return (
      <div className="min-h-screen bg-background">
        <ErrorDisplay
          error={new Error('Itinerary not found')}
          onGoBack={() => window.history.back()}
        />
      </div>
    );
  }

  // Extract data from real itinerary
  const destination = itinerary.days[0]?.location || itinerary.summary || 'Unknown Destination';
  const startDate = itinerary.days[0]?.date || '';
  const endDate = itinerary.days[itinerary.days.length - 1]?.date || '';
  const travelers = 2; // TODO: Get from itinerary metadata when available

  const formatDateRange = (start: string, end: string) => {
    const startFormatted = new Date(start).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
    const endFormatted = new Date(end).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
    return `${startFormatted} - ${endFormatted}`;
  };

  const closeBookingModal = () => {
    setBookingModal({ ...bookingModal, isOpen: false });
  };

  // Render active tab content
  const renderTabContent = () => {
    switch (activeTab) {
      case 'view':
        return <ViewTab itinerary={itinerary} />;
      case 'plan':
        return <PlanTab itinerary={itinerary} />;
      case 'bookings':
        return <BookingsTab itinerary={itinerary} />;
      case 'budget':
        return <BudgetTab tripId={id!} />;
      case 'packing':
        return <PackingTab />;
      case 'docs':
        return <DocsTab />;
      default:
        return <ViewTab itinerary={itinerary} />;
    }
  };

  return (
    <div className="flex h-screen overflow-hidden bg-muted">
      {/* Sidebar */}
      <TripSidebar
        tripId={id!}
        destination={destination}
        dateRange={formatDateRange(startDate, endDate)}
        travelerCount={travelers}
        activeTab={activeTab}
        onTabChange={handleTabChange}
      />

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto">
        <div className="p-8">
          {renderTabContent()}
        </div>
      </main>

      {/* Booking Modal */}
      <BookingModal
        isOpen={bookingModal.isOpen}
        onClose={closeBookingModal}
        bookingType={bookingModal.type}
        itemName={bookingModal.name}
      />
    </div>
  );
}


export default TripDetailPage;
