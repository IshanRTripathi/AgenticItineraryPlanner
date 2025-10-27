/**
 * Search Results Page - Premium Design
 * Million-dollar UI with smooth animations and filters
 * Design: Apple.com refinement + Emirates.com luxury
 */

import { useState } from 'react';
import { motion } from 'framer-motion';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { BookingModal } from '@/components/booking/BookingModal';
import { 
  Plane, Star, Clock, DollarSign, Filter, 
  ArrowUpDown, Wifi, Coffee, Luggage, MapPin,
  Calendar, Users, SlidersHorizontal
} from 'lucide-react';

// Mock search results with enhanced data
const MOCK_RESULTS = [
  {
    id: '1',
    name: 'Air France',
    flightNumber: 'AF123',
    type: 'flight' as const,
    departure: '10:00 AM',
    arrival: '2:30 PM',
    duration: '4h 30m',
    price: 850,
    rating: 4.5,
    reviews: 1234,
    stops: 'Non-stop',
    aircraft: 'Boeing 777',
    amenities: ['wifi', 'meals', 'entertainment'],
    departureAirport: 'JFK',
    arrivalAirport: 'CDG',
    cabinClass: 'Economy',
  },
  {
    id: '2',
    name: 'Lufthansa',
    flightNumber: 'LH456',
    type: 'flight' as const,
    departure: '2:00 PM',
    arrival: '6:45 PM',
    duration: '4h 45m',
    price: 720,
    rating: 4.3,
    reviews: 892,
    stops: 'Non-stop',
    aircraft: 'Airbus A350',
    amenities: ['wifi', 'meals'],
    departureAirport: 'JFK',
    arrivalAirport: 'CDG',
    cabinClass: 'Economy',
  },
  {
    id: '3',
    name: 'British Airways',
    flightNumber: 'BA789',
    type: 'flight' as const,
    departure: '6:30 PM',
    arrival: '11:15 PM',
    duration: '4h 45m',
    price: 680,
    rating: 4.4,
    reviews: 1567,
    stops: '1 stop',
    aircraft: 'Boeing 787',
    amenities: ['wifi', 'meals', 'entertainment', 'extra-legroom'],
    departureAirport: 'JFK',
    arrivalAirport: 'CDG',
    cabinClass: 'Economy',
  },
  {
    id: '4',
    name: 'Delta Airlines',
    flightNumber: 'DL234',
    type: 'flight' as const,
    departure: '8:00 AM',
    arrival: '12:20 PM',
    duration: '4h 20m',
    price: 920,
    rating: 4.6,
    reviews: 2103,
    stops: 'Non-stop',
    aircraft: 'Airbus A330',
    amenities: ['wifi', 'meals', 'entertainment'],
    departureAirport: 'JFK',
    arrivalAirport: 'CDG',
    cabinClass: 'Economy',
  },
  {
    id: '5',
    name: 'United Airlines',
    flightNumber: 'UA567',
    type: 'flight' as const,
    departure: '11:30 AM',
    arrival: '4:00 PM',
    duration: '4h 30m',
    price: 795,
    rating: 4.2,
    reviews: 1456,
    stops: 'Non-stop',
    aircraft: 'Boeing 767',
    amenities: ['wifi', 'meals'],
    departureAirport: 'JFK',
    arrivalAirport: 'CDG',
    cabinClass: 'Economy',
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

  const [sortBy, setSortBy] = useState<'price' | 'duration' | 'rating'>('price');
  const [filters, setFilters] = useState({
    priceRange: [0, 1000],
    stops: ['non-stop', '1-stop'],
    airlines: [] as string[],
    amenities: [] as string[],
  });

  const openBookingModal = (type: 'flight' | 'hotel' | 'activity', name: string) => {
    setBookingModal({ isOpen: true, type, name });
  };

  const closeBookingModal = () => {
    setBookingModal({ ...bookingModal, isOpen: false });
  };

  // Sort results
  const sortedResults = [...MOCK_RESULTS].sort((a, b) => {
    if (sortBy === 'price') return a.price - b.price;
    if (sortBy === 'rating') return b.rating - a.rating;
    if (sortBy === 'duration') {
      const aDuration = parseInt(a.duration);
      const bDuration = parseInt(b.duration);
      return aDuration - bDuration;
    }
    return 0;
  });

  const getAmenityIcon = (amenity: string) => {
    switch (amenity) {
      case 'wifi': return <Wifi className="w-4 h-4" />;
      case 'meals': return <Coffee className="w-4 h-4" />;
      case 'entertainment': return <Star className="w-4 h-4" />;
      case 'extra-legroom': return <Luggage className="w-4 h-4" />;
      default: return null;
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-b from-background to-muted/20">
      <Header />
      
      <main className="flex-1 py-8">
        <div className="container">
          {/* Premium Header with Search Summary */}
          <motion.div 
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-8"
          >
            <Card className="bg-gradient-to-r from-primary/5 via-primary/3 to-transparent border-primary/20">
              <CardContent className="p-6">
                <div className="flex items-center justify-between flex-wrap gap-4">
                  <div>
                    <h1 className="text-3xl font-bold mb-3 bg-gradient-to-r from-primary to-primary-600 bg-clip-text text-transparent">
                      Flight Results
                    </h1>
                    <div className="flex items-center gap-4 text-sm text-muted-foreground flex-wrap">
                      <div className="flex items-center gap-2">
                        <MapPin className="w-4 h-4" />
                        <span className="font-medium">New York (JFK) → Paris (CDG)</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4" />
                        <span>Jun 15, 2025</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Users className="w-4 h-4" />
                        <span>2 passengers</span>
                      </div>
                    </div>
                  </div>
                  <Button variant="outline" size="lg">
                    <Filter className="w-4 h-4 mr-2" />
                    Modify Search
                  </Button>
                </div>
              </CardContent>
            </Card>
          </motion.div>

          {/* Sort Bar */}
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.1 }}
            className="mb-6 flex items-center justify-between"
          >
            <div className="text-sm text-muted-foreground">
              <span className="font-semibold text-foreground">{sortedResults.length}</span> flights found
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground">Sort by:</span>
              <div className="flex gap-2">
                {(['price', 'duration', 'rating'] as const).map((option) => (
                  <Button
                    key={option}
                    variant={sortBy === option ? 'primary' : 'outline'}
                    size="sm"
                    onClick={() => setSortBy(option)}
                    className="capitalize"
                  >
                    <ArrowUpDown className="w-3 h-3 mr-1" />
                    {option}
                  </Button>
                ))}
              </div>
            </div>
          </motion.div>

          {/* Main Content Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Premium Sidebar Filters */}
            <motion.div 
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
              className="lg:col-span-1"
            >
              <Card className="sticky top-24">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between mb-6">
                    <h3 className="font-bold text-lg flex items-center gap-2">
                      <SlidersHorizontal className="w-5 h-5" />
                      Filters
                    </h3>
                    <Button variant="ghost" size="sm">Clear</Button>
                  </div>
                  
                  <div className="space-y-6">
                    {/* Price Range */}
                    <div>
                      <label className="text-sm font-semibold mb-3 block">Price Range</label>
                      <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">$0</span>
                          <span className="text-muted-foreground">$1000+</span>
                        </div>
                        <div className="h-2 bg-muted rounded-full relative">
                          <div className="absolute h-full bg-primary rounded-full" style={{ width: '60%' }} />
                        </div>
                        <div className="text-center text-sm font-medium text-primary">
                          $0 - $600
                        </div>
                      </div>
                    </div>

                    {/* Stops */}
                    <div>
                      <label className="text-sm font-semibold mb-3 block">Stops</label>
                      <div className="space-y-2">
                        {['Non-stop', '1 stop', '2+ stops'].map((stop) => (
                          <label key={stop} className="flex items-center gap-3 p-2 rounded-lg hover:bg-muted/50 cursor-pointer transition-colors">
                            <input 
                              type="checkbox" 
                              defaultChecked={stop === 'Non-stop'}
                              className="w-4 h-4 rounded border-2 border-primary text-primary focus:ring-2 focus:ring-primary/20"
                            />
                            <span className="text-sm flex-1">{stop}</span>
                            <span className="text-xs text-muted-foreground">
                              {stop === 'Non-stop' ? '3' : stop === '1 stop' ? '1' : '1'}
                            </span>
                          </label>
                        ))}
                      </div>
                    </div>

                    {/* Airlines */}
                    <div>
                      <label className="text-sm font-semibold mb-3 block">Airlines</label>
                      <div className="space-y-2">
                        {['Air France', 'Lufthansa', 'British Airways', 'Delta', 'United'].map((airline) => (
                          <label key={airline} className="flex items-center gap-3 p-2 rounded-lg hover:bg-muted/50 cursor-pointer transition-colors">
                            <input 
                              type="checkbox"
                              className="w-4 h-4 rounded border-2 border-primary text-primary focus:ring-2 focus:ring-primary/20"
                            />
                            <span className="text-sm flex-1">{airline}</span>
                          </label>
                        ))}
                      </div>
                    </div>

                    {/* Amenities */}
                    <div>
                      <label className="text-sm font-semibold mb-3 block">Amenities</label>
                      <div className="flex flex-wrap gap-2">
                        {['WiFi', 'Meals', 'Entertainment', 'Extra Legroom'].map((amenity) => (
                          <Badge 
                            key={amenity}
                            variant="outline"
                            className="cursor-pointer hover:bg-primary hover:text-white transition-colors"
                          >
                            {amenity}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </motion.div>

            {/* Premium Results Cards */}
            <div className="lg:col-span-3 space-y-4">
              {sortedResults.map((result, index) => (
                <motion.div
                  key={result.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.1 * index }}
                >
                  <Card className="group hover:shadow-elevation-3 transition-all duration-300 hover:-translate-y-1 border-2 border-transparent hover:border-primary/20">
                    <CardContent className="p-6">
                      <div className="flex items-start justify-between gap-6">
                        {/* Flight Info */}
                        <div className="flex-1 space-y-4">
                          {/* Airline Header */}
                          <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary/10 to-primary/5 flex items-center justify-center group-hover:scale-110 transition-transform">
                              <Plane className="w-6 h-6 text-primary" />
                            </div>
                            <div className="flex-1">
                              <div className="font-bold text-lg">{result.name}</div>
                              <div className="text-sm text-muted-foreground flex items-center gap-2">
                                <span>{result.flightNumber}</span>
                                <span>•</span>
                                <span>{result.aircraft}</span>
                              </div>
                            </div>
                            {result.stops === 'Non-stop' && (
                              <Badge variant="success" className="text-xs">
                                Non-stop
                              </Badge>
                            )}
                          </div>

                          {/* Flight Times */}
                          <div className="grid grid-cols-3 gap-4">
                            <div>
                              <div className="text-xs text-muted-foreground mb-1">Departure</div>
                              <div className="text-2xl font-bold">{result.departure}</div>
                              <div className="text-sm text-muted-foreground">{result.departureAirport}</div>
                            </div>
                            <div className="flex flex-col items-center justify-center">
                              <div className="text-xs text-muted-foreground mb-2 flex items-center gap-1">
                                <Clock className="w-3 h-3" />
                                {result.duration}
                              </div>
                              <div className="w-full h-px bg-gradient-to-r from-transparent via-primary to-transparent relative">
                                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-2 h-2 rounded-full bg-primary" />
                              </div>
                              <div className="text-xs text-muted-foreground mt-2">{result.stops}</div>
                            </div>
                            <div className="text-right">
                              <div className="text-xs text-muted-foreground mb-1">Arrival</div>
                              <div className="text-2xl font-bold">{result.arrival}</div>
                              <div className="text-sm text-muted-foreground">{result.arrivalAirport}</div>
                            </div>
                          </div>

                          {/* Amenities & Rating */}
                          <div className="flex items-center justify-between pt-3 border-t">
                            <div className="flex items-center gap-3">
                              {result.amenities.slice(0, 4).map((amenity) => (
                                <div 
                                  key={amenity}
                                  className="flex items-center gap-1 text-muted-foreground hover:text-primary transition-colors"
                                  title={amenity}
                                >
                                  {getAmenityIcon(amenity)}
                                </div>
                              ))}
                            </div>
                            <div className="flex items-center gap-2">
                              <div className="flex items-center gap-1">
                                <Star className="w-4 h-4 fill-warning text-warning" />
                                <span className="font-semibold">{result.rating}</span>
                              </div>
                              <span className="text-xs text-muted-foreground">
                                ({result.reviews.toLocaleString()} reviews)
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* Price & Book */}
                        <div className="text-right space-y-3 min-w-[140px]">
                          <div>
                            <div className="text-xs text-muted-foreground mb-1">from</div>
                            <div className="text-3xl font-bold text-primary flex items-start justify-end">
                              <DollarSign className="w-5 h-5 mt-1" />
                              {result.price}
                            </div>
                            <div className="text-xs text-muted-foreground">per person</div>
                          </div>
                          <Button
                            size="lg"
                            className="w-full group-hover:scale-105 transition-transform"
                            onClick={() => openBookingModal(result.type, `${result.name} ${result.flightNumber}`)}
                          >
                            Book Now
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            className="w-full"
                          >
                            View Details
                          </Button>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </motion.div>
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


export default SearchResultsPage;
