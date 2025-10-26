/**
 * Search Results Page
 * Displays search results for flights, hotels, etc.
 */

import { useState } from 'react';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BookingModal } from '@/components/booking/BookingModal';
import { Plane, Star, Clock, DollarSign } from 'lucide-react';

// Mock search results
const MOCK_RESULTS = [
  {
    id: '1',
    name: 'Air France AF123',
    type: 'flight' as const,
    departure: '10:00 AM',
    arrival: '2:30 PM',
    duration: '4h 30m',
    price: 850,
    rating: 4.5,
    stops: 'Non-stop',
  },
  {
    id: '2',
    name: 'Lufthansa LH456',
    type: 'flight' as const,
    departure: '2:00 PM',
    arrival: '6:45 PM',
    duration: '4h 45m',
    price: 720,
    rating: 4.3,
    stops: 'Non-stop',
  },
  {
    id: '3',
    name: 'British Airways BA789',
    type: 'flight' as const,
    departure: '6:30 PM',
    arrival: '11:15 PM',
    duration: '4h 45m',
    price: 680,
    rating: 4.4,
    stops: '1 stop',
  },
];

export function SearchResultsPage() {
  const [bookingModal, setBookingModal] = useState<{
    isOpen: boolean;
    type: 'flight' | 'hotel' | 'activity';
    name: string;
  }>({
    isOpen: false,
    type: 'flight',
    name: '',
  });

  const openBookingModal = (type: 'flight' | 'hotel' | 'activity', name: string) => {
    setBookingModal({ isOpen: true, type, name });
  };

  const closeBookingModal = () => {
    setBookingModal({ ...bookingModal, isOpen: false });
  };

  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      
      <main className="flex-1 bg-muted/30 py-8">
        <div className="container">
          {/* Header */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold mb-2">Flight Results</h1>
            <p className="text-muted-foreground">
              New York (JFK) → Paris (CDG) • Jun 15, 2025 • 2 passengers
            </p>
          </div>

          {/* Filters */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Sidebar Filters */}
            <div className="lg:col-span-1">
              <Card>
                <CardContent className="p-6">
                  <h3 className="font-semibold mb-4">Filters</h3>
                  <div className="space-y-4">
                    <div>
                      <label className="text-sm font-medium mb-2 block">Price Range</label>
                      <div className="text-sm text-muted-foreground">$500 - $1000</div>
                    </div>
                    <div>
                      <label className="text-sm font-medium mb-2 block">Stops</label>
                      <div className="space-y-2">
                        <label className="flex items-center gap-2 text-sm">
                          <input type="checkbox" defaultChecked />
                          Non-stop
                        </label>
                        <label className="flex items-center gap-2 text-sm">
                          <input type="checkbox" />
                          1 stop
                        </label>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Results */}
            <div className="lg:col-span-3 space-y-4">
              {MOCK_RESULTS.map((result) => (
                <Card key={result.id} className="hover:shadow-elevation-3 transition-shadow">
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between gap-6">
                      {/* Flight Info */}
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-4">
                          <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                            <Plane className="w-5 h-5 text-primary" />
                          </div>
                          <div>
                            <div className="font-semibold">{result.name}</div>
                            <div className="text-sm text-muted-foreground">{result.stops}</div>
                          </div>
                        </div>

                        <div className="grid grid-cols-3 gap-4 text-sm">
                          <div>
                            <div className="text-muted-foreground mb-1">Departure</div>
                            <div className="font-semibold">{result.departure}</div>
                          </div>
                          <div>
                            <div className="text-muted-foreground mb-1 flex items-center gap-1">
                              <Clock className="w-3 h-3" />
                              Duration
                            </div>
                            <div className="font-semibold">{result.duration}</div>
                          </div>
                          <div>
                            <div className="text-muted-foreground mb-1">Arrival</div>
                            <div className="font-semibold">{result.arrival}</div>
                          </div>
                        </div>

                        <div className="flex items-center gap-1 mt-3">
                          <Star className="w-4 h-4 fill-warning text-warning" />
                          <span className="text-sm font-medium">{result.rating}</span>
                        </div>
                      </div>

                      {/* Price & Book */}
                      <div className="text-right">
                        <div className="flex items-center justify-end gap-1 text-2xl font-bold text-primary mb-2">
                          <DollarSign className="w-6 h-6" />
                          {result.price}
                        </div>
                        <div className="text-xs text-muted-foreground mb-4">per person</div>
                        <Button
                          size="lg"
                          onClick={() => openBookingModal(result.type, result.name)}
                        >
                          Book Now
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </div>
      </main>

      <Footer />

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
