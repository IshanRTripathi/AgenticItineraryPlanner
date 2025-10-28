/**
 * Trip Detail Page
 * Shows complete itinerary with sidebar navigation
 * Task 24: Full sidebar implementation
 */

import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { TripSidebar } from '@/components/trip/TripSidebar';
import { MobileTabs } from '@/components/trip/MobileTabs';
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
import { Eye, Map, CreditCard, DollarSign, Package, FileText } from 'lucide-react';

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

  // Tab configuration
  const TABS = [
    { id: 'view', label: 'View', icon: Eye },
    { id: 'plan', label: 'Plan', icon: Map },
    { id: 'bookings', label: 'Bookings', icon: CreditCard },
    { id: 'budget', label: 'Budget', icon: DollarSign },
    { id: 'packing', label: 'Packing', icon: Package },
    { id: 'docs', label: 'Docs', icon: FileText },
  ];

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

  // If itinerary is still generating, show progress view
  if (itinerary.status === 'generating' || itinerary.status === 'planning') {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        <div className="max-w-2xl w-full">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold mb-2">Creating Your Perfect Itinerary</h1>
            <p className="text-muted-foreground">
              Our AI agents are working on your personalized travel plan...
            </p>
          </div>
          <TripDetailSkeleton />
          <div className="mt-8 text-center text-sm text-muted-foreground">
            <p>This usually takes 30-60 seconds</p>
            <p className="mt-2">The page will automatically update when ready</p>
          </div>
        </div>
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
    <div className="min-h-screen bg-background pb-20 md:pb-0">
      {/* Mobile: Horizontal Tabs */}
      <MobileTabs
        tabs={TABS}
        activeTab={activeTab}
        onTabChange={handleTabChange}
      />

      {/* Desktop: Sidebar + Content Layout */}
      <div className="flex h-screen overflow-hidden bg-muted">
        {/* Desktop Sidebar */}
        <div className="hidden lg:block">
          <TripSidebar
            tripId={id!}
            destination={destination}
            dateRange={formatDateRange(startDate, endDate)}
            travelerCount={travelers}
            activeTab={activeTab}
            onTabChange={handleTabChange}
          />
        </div>

        {/* Main Content */}
        <main className="flex-1 overflow-y-auto">
          <div className="p-4 md:p-8">
            {renderTabContent()}
          </div>
        </main>
      </div>

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
