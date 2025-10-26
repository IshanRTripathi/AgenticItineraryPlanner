/**
 * Trip Detail Page
 * Shows complete itinerary with day-by-day breakdown
 */

import { useState } from 'react';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { BookingModal } from '@/components/booking/BookingModal';
import { BookingCard } from '@/components/booking/BookingCard';
import { 
  Calendar, MapPin, Users, DollarSign, Clock, 
  Plane, Hotel, UtensilsCrossed, Camera, ArrowLeft 
} from 'lucide-react';

// Mock data - replace with API call
const MOCK_TRIP = {
  id: '1',
  destination: 'Paris, France',
  startDate: '2025-06-15',
  endDate: '2025-06-22',
  status: 'upcoming',
  imageUrl: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200',
  travelers: 2,
  budget: 'moderate',
  days: [
    {
      day: 1,
      date: '2025-06-15',
      title: 'Arrival & Eiffel Tower',
      activities: [
        { time: '10:00 AM', title: 'Arrive at Charles de Gaulle Airport', icon: Plane },
        { time: '12:00 PM', title: 'Check-in at Hotel Le Marais', icon: Hotel },
        { time: '3:00 PM', title: 'Visit Eiffel Tower', icon: Camera },
        { time: '7:00 PM', title: 'Dinner at Le Jules Verne', icon: UtensilsCrossed },
      ],
    },
    {
      day: 2,
      date: '2025-06-16',
      title: 'Louvre & Notre-Dame',
      activities: [
        { time: '9:00 AM', title: 'Breakfast at hotel', icon: UtensilsCrossed },
        { time: '10:00 AM', title: 'Louvre Museum Tour', icon: Camera },
        { time: '2:00 PM', title: 'Lunch at Café Marly', icon: UtensilsCrossed },
        { time: '4:00 PM', title: 'Notre-Dame Cathedral', icon: Camera },
      ],
    },
  ],
};

// Mock bookings data
const MOCK_BOOKINGS = [
  {
    id: '1',
    type: 'flight' as const,
    name: 'Air France AF123',
    date: '2025-06-15',
    location: 'Paris CDG',
    price: 850,
    status: 'confirmed' as const,
    confirmationCode: 'AF123XYZ',
  },
  {
    id: '2',
    type: 'hotel' as const,
    name: 'Hotel Le Marais',
    date: '2025-06-15',
    location: 'Paris, France',
    price: 1200,
    status: 'confirmed' as const,
    confirmationCode: 'HLM456ABC',
  },
];

export function TripDetailPage() {
  const [activeTab, setActiveTab] = useState('itinerary');
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

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  };

  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      
      <main className="flex-1">
        {/* Hero Section */}
        <div className="relative h-96 overflow-hidden">
          <img
            src={MOCK_TRIP.imageUrl}
            alt={MOCK_TRIP.destination}
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />
          
          <div className="absolute inset-0 flex items-end">
            <div className="container pb-8">
              <Button
                variant="ghost"
                className="text-white hover:bg-white/20 mb-4"
                onClick={() => window.history.back()}
              >
                <ArrowLeft className="w-4 h-4 mr-2" />
                Back to Trips
              </Button>
              
              <h1 className="text-5xl font-bold text-white mb-4 flex items-center gap-3">
                <MapPin className="w-10 h-10" />
                {MOCK_TRIP.destination}
              </h1>
              
              <div className="flex flex-wrap gap-6 text-white/90">
                <div className="flex items-center gap-2">
                  <Calendar className="w-5 h-5" />
                  <span>{formatDate(MOCK_TRIP.startDate)}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Users className="w-5 h-5" />
                  <span>{MOCK_TRIP.travelers} travelers</span>
                </div>
                <div className="flex items-center gap-2">
                  <DollarSign className="w-5 h-5" />
                  <span className="capitalize">{MOCK_TRIP.budget} budget</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="container py-12">
          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="mb-8">
              <TabsTrigger value="itinerary">Day-by-Day Itinerary</TabsTrigger>
              <TabsTrigger value="bookings">Bookings</TabsTrigger>
              <TabsTrigger value="documents">Documents</TabsTrigger>
            </TabsList>

            <TabsContent value="itinerary">
              <div className="space-y-6">
                {MOCK_TRIP.days.map((day) => (
                  <Card key={day.day}>
                    <CardHeader>
                      <CardTitle className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-full bg-primary text-white flex items-center justify-center font-bold">
                          {day.day}
                        </div>
                        <div>
                          <div className="text-xl">{day.title}</div>
                          <div className="text-sm font-normal text-muted-foreground">
                            {formatDate(day.date)}
                          </div>
                        </div>
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        {day.activities.map((activity, idx) => {
                          const Icon = activity.icon;
                          return (
                            <div
                              key={idx}
                              className="flex items-start gap-4 p-4 rounded-lg hover:bg-muted/50 transition-colors"
                            >
                              <div className="flex-shrink-0">
                                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                                  <Icon className="w-5 h-5 text-primary" />
                                </div>
                              </div>
                              <div className="flex-1">
                                <div className="flex items-center gap-2 mb-1">
                                  <Clock className="w-4 h-4 text-muted-foreground" />
                                  <span className="text-sm font-semibold text-muted-foreground">
                                    {activity.time}
                                  </span>
                                </div>
                                <div className="font-medium">{activity.title}</div>
                              </div>
                              <Button 
                                variant="outline" 
                                size="sm"
                                onClick={() => openBookingModal(
                                  activity.icon === Plane ? 'flight' : 
                                  activity.icon === Hotel ? 'hotel' : 'activity',
                                  activity.title
                                )}
                              >
                                Book Now
                              </Button>
                            </div>
                          );
                        })}
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </TabsContent>

            <TabsContent value="bookings">
              {MOCK_BOOKINGS.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {MOCK_BOOKINGS.map((booking) => (
                    <BookingCard key={booking.id} booking={booking} />
                  ))}
                </div>
              ) : (
                <Card>
                  <CardContent className="py-12 text-center">
                    <p className="text-muted-foreground">No bookings yet</p>
                  </CardContent>
                </Card>
              )}
            </TabsContent>

            <TabsContent value="documents">
              <Card>
                <CardContent className="py-12 text-center">
                  <p className="text-muted-foreground">No documents uploaded</p>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
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
