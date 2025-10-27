/**
 * Bookings Tab - Provider Booking Interface
 * Task 31: Enhanced with provider sidebar + Real data integration
 */

import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { BookingCard } from '@/components/booking/BookingCard';
import { BookingModal } from '@/components/booking/BookingModal';
import { getProvidersByVertical } from '@/config/providers';
import { bookingService, Booking } from '@/services/bookingService';
import { useToast } from '@/components/ui/use-toast';
import {
  Plane,
  Hotel,
  Car,
  Train,
  Bus,
  Check,
  ExternalLink,
  Loader2,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface BookingsTabProps {
  itinerary: any; // NormalizedItinerary type
}

const PROVIDER_CATEGORIES = [
  {
    id: 'flights',
    label: 'Flights',
    icon: Plane,
    vertical: 'flight' as const,
  },
  {
    id: 'hotels',
    label: 'Hotels',
    icon: Hotel,
    vertical: 'hotel' as const,
  },
  {
    id: 'transport',
    label: 'Transport',
    icon: Car,
    vertical: 'activity' as const,
  },
];

export function BookingsTab({ itinerary }: BookingsTabProps) {
  const [selectedCategory, setSelectedCategory] = useState('flights');
  const [selectedProvider, setSelectedProvider] = useState<string | null>(null);
  const [bookingModal, setBookingModal] = useState({
    isOpen: false,
    type: 'flight' as 'flight' | 'hotel' | 'activity',
    name: '',
  });
  const [realBookings, setRealBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const { toast } = useToast();

  // Fetch real bookings from API
  useEffect(() => {
    const fetchBookings = async () => {
      if (!itinerary?.itineraryId) return;
      
      setIsLoading(true);
      try {
        const bookings = await bookingService.getBookings(itinerary.itineraryId);
        setRealBookings(bookings);
      } catch (error) {
        console.error('Failed to fetch bookings:', error);
        toast({
          title: 'Error',
          description: 'Failed to load bookings',
          variant: 'destructive',
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchBookings();
  }, [itinerary?.itineraryId, toast]);

  // Merge real bookings with itinerary nodes
  const bookings = [
    ...realBookings.map(b => ({
      id: b.id,
      title: b.bookingDetails?.name || 'Booking',
      type: b.type,
      bookingRef: b.confirmationNumber,
      timing: { startTime: b.bookingDetails?.date },
      location: { address: b.bookingDetails?.location },
      cost: { amount: b.cost.amount },
      status: b.status,
    })),
    ...itinerary.days.flatMap((day: any) =>
      day.nodes?.filter((node: any) => node.bookingRef) || []
    ),
  ];

  const currentCategory = PROVIDER_CATEGORIES.find(c => c.id === selectedCategory);
  const providers = currentCategory ? getProvidersByVertical(currentCategory.vertical) : [];

  const handleProviderClick = (providerId: string) => {
    setSelectedProvider(providerId);
    // Open booking modal
    setBookingModal({
      isOpen: true,
      type: currentCategory?.vertical || 'flight',
      name: providers.find(p => p.id === providerId)?.name || '',
    });
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
      {/* Provider Sidebar */}
      <div className="lg:col-span-1">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Booking Providers</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {PROVIDER_CATEGORIES.map((category) => {
              const Icon = category.icon;
              const isActive = selectedCategory === category.id;
              const categoryProviders = getProvidersByVertical(category.vertical);

              return (
                <div key={category.id} className="space-y-2">
                  {/* Category Header */}
                  <button
                    onClick={() => setSelectedCategory(category.id)}
                    className={cn(
                      'w-full flex items-center gap-2 px-3 py-2 rounded-lg font-medium transition-colors',
                      isActive
                        ? 'bg-primary text-primary-foreground'
                        : 'hover:bg-muted'
                    )}
                  >
                    <Icon className="w-4 h-4" />
                    {category.label}
                  </button>

                  {/* Provider Buttons */}
                  {isActive && (
                    <div className="space-y-1 pl-2">
                      {categoryProviders.map((provider) => (
                        <button
                          key={provider.id}
                          onClick={() => handleProviderClick(provider.id)}
                          className={cn(
                            'w-full flex items-center justify-between px-3 py-2 rounded-lg text-sm transition-colors',
                            selectedProvider === provider.id
                              ? 'bg-primary/10 text-primary font-medium'
                              : 'hover:bg-muted'
                          )}
                        >
                          <div className="flex items-center gap-2">
                            <div className="w-5 h-5 rounded bg-muted flex items-center justify-center text-xs">
                              {provider.name.charAt(0)}
                            </div>
                            <span>{provider.name}</span>
                          </div>
                          {selectedProvider === provider.id && (
                            <Check className="w-4 h-4" />
                          )}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </CardContent>
        </Card>
      </div>

      {/* Bookings Content */}
      <div className="lg:col-span-3 space-y-6">
        {/* Loading State */}
        {isLoading && (
          <Card>
            <CardContent className="py-12 text-center">
              <Loader2 className="w-8 h-8 mx-auto animate-spin text-primary mb-4" />
              <p className="text-sm text-muted-foreground">Loading bookings...</p>
            </CardContent>
          </Card>
        )}

        {/* Existing Bookings */}
        {!isLoading && bookings.length > 0 ? (
          <div className="space-y-4">
            <h3 className="text-xl font-semibold">Your Bookings</h3>
            {bookings.map((node: any, index: number) => (
              <BookingCard
                key={index}
                booking={{
                  id: node.id || `booking-${index}`,
                  type: node.type === 'hotel' ? 'hotel' : node.type === 'attraction' ? 'activity' : 'flight',
                  name: node.title,
                  date: node.timing?.startTime || 'TBD',
                  location: node.location?.address || '',
                  price: node.cost?.amount || 0,
                  status: 'confirmed',
                  confirmationCode: node.bookingRef || '',
                }}
              />
            ))}
          </div>
        ) : !isLoading ? (
          <Card>
            <CardContent className="py-12 text-center">
              <div className="space-y-4">
                <div className="w-16 h-16 mx-auto rounded-full bg-muted flex items-center justify-center">
                  <Hotel className="w-8 h-8 text-muted-foreground" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold mb-2">No Bookings Yet</h3>
                  <p className="text-sm text-muted-foreground mb-4">
                    Select a provider from the sidebar to start booking
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        ) : null}

        {/* Available Activities to Book */}
        <div className="space-y-4">
          <h3 className="text-xl font-semibold">Available to Book</h3>
          <div className="grid gap-4">
            {itinerary.days.flatMap((day: any) =>
              day.nodes
                ?.filter((node: any) => !node.bookingRef)
                .map((node: any, index: number) => (
                  <Card key={`${day.dayNumber}-${index}`} className="hover:shadow-md transition-shadow">
                    <CardContent className="p-4">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h4 className="font-semibold text-lg mb-1">{node.title}</h4>
                          <p className="text-sm text-muted-foreground mb-2">
                            Day {day.dayNumber} • {day.location}
                          </p>
                          {node.cost?.amount && (
                            <Badge variant="secondary">
                              ${node.cost.amount}
                            </Badge>
                          )}
                        </div>
                        <Button
                          size="sm"
                          onClick={() => {
                            setBookingModal({
                              isOpen: true,
                              type: node.type === 'hotel' ? 'hotel' : 'activity',
                              name: node.title,
                            });
                          }}
                        >
                          <ExternalLink className="w-4 h-4 mr-2" />
                          Book Now
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))
            )}
          </div>
        </div>
      </div>

      {/* Booking Modal */}
      <BookingModal
        isOpen={bookingModal.isOpen}
        onClose={() => setBookingModal({ ...bookingModal, isOpen: false })}
        bookingType={bookingModal.type}
        itemName={bookingModal.name}
      />
    </div>
  );
}
