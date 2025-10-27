/**
 * Trip List Component
 * Displays user's trips in a grid - Connected to Backend
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
      <div className="text-center py-16">
        <div className="w-24 h-24 mx-auto mb-6 rounded-full bg-destructive/10 flex items-center justify-center">
          <MapPin className="w-12 h-12 text-destructive" />
        </div>
        <h3 className="text-2xl font-semibold mb-2">Failed to load trips</h3>
        <p className="text-muted-foreground mb-6">{(queryError as Error).message}</p>
        <button
          onClick={() => refetch()}
          className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
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
      <div className="flex items-center justify-center py-16">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin text-primary mx-auto mb-4" />
          <p className="text-muted-foreground">Loading your trips...</p>
        </div>
      </div>
    );
  }

  // Empty State
  if (trips.length === 0) {
    return (
      <div className="text-center py-16">
        <div className="w-24 h-24 mx-auto mb-6 rounded-full bg-muted flex items-center justify-center">
          <MapPin className="w-12 h-12 text-muted-foreground" />
        </div>
        <h3 className="text-2xl font-semibold mb-2">No trips yet</h3>
        <p className="text-muted-foreground mb-6">
          Start planning your next adventure with AI
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* Filter Tabs */}
      <Tabs value={filter} onValueChange={(v) => setFilter(v as any)} className="mb-8">
        <TabsList>
          <TabsTrigger value="all" className="gap-2">
            <Calendar className="w-4 h-4" />
            All Trips ({trips.length})
          </TabsTrigger>
          <TabsTrigger value="upcoming" className="gap-2">
            <Clock className="w-4 h-4" />
            Upcoming ({upcomingCount})
          </TabsTrigger>
          <TabsTrigger value="completed" className="gap-2">
            <MapPin className="w-4 h-4" />
            Completed ({completedCount})
          </TabsTrigger>
        </TabsList>
      </Tabs>

      {/* Trip Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
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
