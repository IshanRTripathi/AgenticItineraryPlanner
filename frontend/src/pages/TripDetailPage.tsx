/**
 * Trip Detail Page
 * Shows complete itinerary with sidebar navigation
 * Task 24: Full sidebar implementation
 * Week 11: Integrated with UnifiedItineraryContext for real-time updates
 */

import React, { useState } from 'react';
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
import { ChatTab } from '@/components/trip/tabs/ChatTab';
import { TripDetailSkeleton } from '@/components/loading/TripDetailSkeleton';
import { ErrorDisplay } from '@/components/error/ErrorDisplay';
import { GenerationProgressBanner } from '@/components/trip/GenerationProgressBanner';
import { UnifiedItineraryProvider, useUnifiedItinerary } from '@/contexts/UnifiedItineraryContext';
import { Eye, Map, CreditCard, DollarSign, Package, FileText, MessageSquare } from 'lucide-react';

/**
 * Inner component that uses the UnifiedItineraryContext
 */
function TripDetailContent() {
  const { id } = useParams<{ id: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const { state, loadItinerary } = useUnifiedItinerary();
  const [wasConnected, setWasConnected] = React.useState(false);
  const [isRefreshing, setIsRefreshing] = React.useState(false);

  const { itinerary, loading, error, isConnected } = state;

  // Track when itinerary is being refreshed (not initial load)
  const prevItineraryRef = React.useRef(itinerary);
  React.useEffect(() => {
    if (loading && prevItineraryRef.current) {
      setIsRefreshing(true);
    } else if (!loading) {
      setIsRefreshing(false);
      prevItineraryRef.current = itinerary;
    }
  }, [loading, itinerary]);

  // Track if we were ever connected
  React.useEffect(() => {
    if (isConnected) {
      setWasConnected(true);
    }
  }, [isConnected]);

  // Track if we've loaded the itinerary at least once
  const [hasLoadedOnce, setHasLoadedOnce] = React.useState(false);
  React.useEffect(() => {
    if (itinerary && !hasLoadedOnce) {
      setHasLoadedOnce(true);
    }
  }, [itinerary, hasLoadedOnce]);

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
    { id: 'chat', label: 'Chat', icon: MessageSquare },
    { id: 'bookings', label: 'Bookings', icon: CreditCard },
    { id: 'budget', label: 'Budget', icon: DollarSign },
    { id: 'packing', label: 'Packing', icon: Package },
    { id: 'docs', label: 'Docs', icon: FileText },
  ];

  // Show loading skeleton only on initial load, not during refetch
  if (loading && !hasLoadedOnce) {
    return <TripDetailSkeleton />;
  }

  // Show error state if fetch failed
  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <ErrorDisplay
          error={new Error(error)}
          onRetry={() => loadItinerary(id!)}
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

  // Check if itinerary is still generating
  const isGenerating = itinerary.status === 'generating' || itinerary.status === 'planning';
  
  // If generating, we'll show the UI with a banner but still render the content
  // This allows for optimistic updates as data comes in via WebSocket

  // Extract data from real itinerary with safe access
  const days = (itinerary as any)?.days || [];
  const destination = days[0]?.location || (itinerary as any)?.summary || 'Unknown Destination';
  const startDate = days[0]?.date || '';
  const endDate = days[days.length - 1]?.date || '';
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

  // Render active tab content with refresh indicator
  const renderTabContent = () => {
    const content = (() => {
      switch (activeTab) {
        case 'view':
          return <ViewTab itinerary={itinerary} key={itinerary?.updatedAt || 'view'} />;
        case 'plan':
          return <PlanTab itinerary={itinerary} key={itinerary?.updatedAt || 'plan'} />;
        case 'chat':
          return <ChatTab />;
        case 'bookings':
          return <BookingsTab itinerary={itinerary} key={itinerary?.updatedAt || 'bookings'} />;
        case 'budget':
          return <BudgetTab tripId={id!} />;
        case 'packing':
          return <PackingTab />;
        case 'docs':
          return <DocsTab />;
        default:
          return <ViewTab itinerary={itinerary} key={itinerary?.updatedAt || 'view'} />;
      }
    })();

    return (
      <div className="relative">
        {isRefreshing && (
          <div className="absolute top-0 left-0 right-0 z-10 bg-blue-50 border-b border-blue-200 px-4 py-2 flex items-center gap-2 animate-fade-in">
            <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
            <span className="text-sm text-blue-700 font-medium">Updating itinerary...</span>
          </div>
        )}
        <div className={isRefreshing ? 'opacity-75 transition-opacity' : ''}>
          {content}
        </div>
      </div>
    );
  };

  // Calculate generation progress
  const completedDays = days.filter((day: any) => day.nodes && day.nodes.length > 0).length;
  const currentPhase = state.currentPhase || 'skeleton';

  return (
    <div className="min-h-screen bg-background pb-20 md:pb-0">
      {/* Premium Generation Progress Banner */}
      {isGenerating && (
        <GenerationProgressBanner
          itineraryStatus={itinerary.status}
          completedDays={completedDays}
          totalDays={days.length || 4}
          currentPhase={currentPhase}
          onComplete={() => loadItinerary(id!)}
        />
      )}

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
        providerUrl=""
      />

      {/* Connection Status Indicator - only show if we were previously connected */}
      {!isConnected && wasConnected && (
        <div className="fixed bottom-4 right-4 bg-yellow-500 text-white px-4 py-2 rounded-lg shadow-lg text-sm">
          Reconnecting to server...
        </div>
      )}
    </div>
  );
}

/**
 * Wrapper component that provides UnifiedItineraryContext
 */
export function TripDetailPage() {
  const { id } = useParams<{ id: string }>();

  if (!id) {
    return (
      <div className="min-h-screen bg-background">
        <ErrorDisplay
          error={new Error('Invalid trip ID')}
          onGoBack={() => window.history.back()}
        />
      </div>
    );
  }

  return (
    <UnifiedItineraryProvider itineraryId={id}>
      <TripDetailContent />
    </UnifiedItineraryProvider>
  );
}

export default TripDetailPage;
