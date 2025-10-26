/**
 * Trip List Component
 * Displays user's trips in a grid
 */

import { useState } from 'react';
import { TripCard } from './TripCard';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Calendar, MapPin, Clock } from 'lucide-react';

// Mock data - replace with API call
const MOCK_TRIPS = [
  {
    id: '1',
    destination: 'Paris, France',
    startDate: '2025-06-15',
    endDate: '2025-06-22',
    status: 'upcoming' as const,
    imageUrl: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800',
    travelers: 2,
    budget: 'moderate',
  },
  {
    id: '2',
    destination: 'Tokyo, Japan',
    startDate: '2025-08-10',
    endDate: '2025-08-20',
    status: 'upcoming' as const,
    imageUrl: 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800',
    travelers: 1,
    budget: 'luxury',
  },
  {
    id: '3',
    destination: 'Bali, Indonesia',
    startDate: '2024-12-01',
    endDate: '2024-12-10',
    status: 'completed' as const,
    imageUrl: 'https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800',
    travelers: 2,
    budget: 'budget',
  },
];

export function TripList() {
  const [filter, setFilter] = useState<'all' | 'upcoming' | 'completed'>('all');

  const filteredTrips = MOCK_TRIPS.filter(trip => {
    if (filter === 'all') return true;
    return trip.status === filter;
  });

  const upcomingCount = MOCK_TRIPS.filter(t => t.status === 'upcoming').length;
  const completedCount = MOCK_TRIPS.filter(t => t.status === 'completed').length;

  if (MOCK_TRIPS.length === 0) {
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
            All Trips ({MOCK_TRIPS.length})
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
          <TripCard key={trip.id} trip={trip} />
        ))}
      </div>
    </div>
  );
}
