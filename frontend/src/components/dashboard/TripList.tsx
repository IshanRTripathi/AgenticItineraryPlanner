/**
 * Trip List Component
 * Displays user's trips in a grid - Connected to Backend
 * Task 12: Mobile-optimized with responsive grid and stacked actions
 * 
 * Backend Integration:
 * - Fetches trips from GET /api/v1/itineraries
 * - Displays loading skeleton during fetch
 * - Shows empty state if no trips
 * - Handles errors gracefully
 * - Supports delete with confirmation
 */

import { useState } from 'react';
import { TripCard } from './TripCard';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Calendar, MapPin, Clock, Loader2 } from 'lucide-react';
import { apiClient } from '@/services/apiClient';
import { useToast } from '@/components/ui/use-toast';
import { useItineraries } from '@/hooks/useItinerary';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import type { ItineraryListItem } from '@/types/dto';

interface Trip {
  id: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: 'upcoming' | 'completed';
  imageUrl?: string;
  travelers: number;
  budget: string;
}

export function TripList() {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const [filter, setFilter] = useState<'all' | 'upcoming' | 'completed'>('all');
  const { toast } = useToast();
  
  // Use React Query hook for data fetching
  const { data: itineraries, isLoading: loading, error: queryError, refetch } = useItineraries();
  
  // Transform backend data to Trip format
  const trips: Trip[] = (itineraries || []).map((itinerary: any) => {
    const endDate = new Date(itinerary.endDate);
    const now = new Date();
    
    return {
      id: itinerary.id,
      destination: itinerary.destination,
      startDate: itinerary.startDate,
      endDate: itinerary.endDate,
      status: endDate < now ? 'completed' : 'upcoming',
      imageUrl: itinerary.imageUrl,
      travelers: itinerary.travelers || 2,
      budget: itinerary.budget || 'moderate',
    };
  });

  const handleDelete = async (tripId: string) => {
    try {
      await apiClient.delete(`/itineraries/${tripId}`);
      refetch(); // Refetch trips after deletion
      toast({
        title: 'Trip deleted',
        description: 'Your trip has been successfully deleted',
      });
    } catch (err: any) {
      console.error('Failed to delete trip:', err);
      toast({
        title: 'Error deleting trip',
        description: err.message || 'Please try again',
        variant: 'destructive',
      });
    }
  };

  // Show error state
  if (queryError) {
    return (
      <div className="text-center py-12 sm:py-16 px-4">
        <div className="w-20 h-20 sm:w-24 sm:h-24 mx-auto mb-4 sm:mb-6 rounded-full bg-destructive/10 flex items-center justify-center">
          <MapPin className="w-10 h-10 sm:w-12 sm:h-12 text-destructive" />
        </div>
        <h3 className="text-xl sm:text-2xl font-semibold mb-2">Failed to load trips</h3>
        <p className="text-sm sm:text-base text-muted-foreground mb-4 sm:mb-6">{(queryError as Error).message}</p>
        <button
          onClick={() => refetch()}
          className="px-6 py-3 min-h-[44px] bg-primary text-white rounded-lg hover:bg-primary/90 touch-manipulation active:scale-95 transition-transform"
        >
          Try Again
        </button>
      </div>
    );
  }

  const filteredTrips = trips.filter(trip => {
    if (filter === 'all') return true;
    return trip.status === filter;
  });

  const upcomingCount = trips.filter(t => t.status === 'upcoming').length;
  const completedCount = trips.filter(t => t.status === 'completed').length;

  // Loading State
  if (loading) {
    return (
      <div className="flex items-center justify-center py-12 sm:py-16">
        <div className="text-center">
          <Loader2 className="w-10 h-10 sm:w-12 sm:h-12 animate-spin text-primary mx-auto mb-3 sm:mb-4" />
          <p className="text-sm sm:text-base text-muted-foreground">Loading your trips...</p>
        </div>
      </div>
    );
  }

  // Empty State
  if (trips.length === 0) {
    return (
      <div className="text-center py-12 sm:py-16 px-4">
        <div className="w-20 h-20 sm:w-24 sm:h-24 mx-auto mb-4 sm:mb-6 rounded-full bg-muted flex items-center justify-center">
          <MapPin className="w-10 h-10 sm:w-12 sm:h-12 text-muted-foreground" />
        </div>
        <h3 className="text-xl sm:text-2xl font-semibold mb-2">No trips yet</h3>
        <p className="text-sm sm:text-base text-muted-foreground mb-4 sm:mb-6">
          Start planning your next adventure with AI
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* Filter Tabs */}
      <Tabs value={filter} onValueChange={(v) => setFilter(v as any)} className="mb-4 sm:mb-6 md:mb-8">
        <TabsList className="w-full sm:w-auto grid grid-cols-3 sm:inline-grid">
          <TabsTrigger value="all" className="gap-1 sm:gap-2 min-h-[44px] text-xs sm:text-sm">
            <Calendar className="w-3 h-3 sm:w-4 sm:h-4" />
            <span className="hidden sm:inline">All Trips</span>
            <span className="sm:hidden">All</span>
            <span>({trips.length})</span>
          </TabsTrigger>
          <TabsTrigger value="upcoming" className="gap-1 sm:gap-2 min-h-[44px] text-xs sm:text-sm">
            <Clock className="w-3 h-3 sm:w-4 sm:h-4" />
            <span className="hidden sm:inline">Upcoming</span>
            <span className="sm:hidden">Soon</span>
            <span>({upcomingCount})</span>
          </TabsTrigger>
          <TabsTrigger value="completed" className="gap-1 sm:gap-2 min-h-[44px] text-xs sm:text-sm">
            <MapPin className="w-3 h-3 sm:w-4 sm:h-4" />
            <span className="hidden sm:inline">Completed</span>
            <span className="sm:hidden">Done</span>
            <span>({completedCount})</span>
          </TabsTrigger>
        </TabsList>
      </Tabs>

      {/* Trip Grid - Responsive */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-5 md:gap-6">
        {filteredTrips.map((trip) => (
          <TripCard 
            key={trip.id} 
            trip={trip}
            onDelete={handleDelete}
          />
        ))}
      </div>
    </div>
  );
}
