import React, { useState } from 'react';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { Separator } from '../ui/separator';
import { ScrollArea } from '../ui/scroll-area';
import { 
  Hotel, 
  Star, 
  MapPin, 
  Wifi, 
  Car, 
  Utensils, 
  Dumbbell, 
  Check,
  ExternalLink,
  Search,
  Calendar,
  Users
} from 'lucide-react';

interface HotelBookingSystemProps {
  destination: string;
  nights: number;
  guests: number;
  checkInDate: string;
  checkOutDate: string;
  onBookingUpdate: (booking: any) => void;
  existingBooking?: any;
}

interface Hotel {
  id: string;
  name: string;
  location: string;
  rating: number;
  price: number;
  currency: string;
  image: string;
  amenities: string[];
  distance: string;
  provider: string;
  available: boolean;
}

const mockHotels: Hotel[] = [
  {
    id: '1',
    name: 'Grand Hotel Barcelona',
    location: 'Eixample, Barcelona',
    rating: 4.5,
    price: 180,
    currency: 'EUR',
    image: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=400&h=300&fit=crop',
    amenities: ['wifi', 'parking', 'restaurant', 'gym'],
    distance: '0.5 km from city center',
    provider: 'Booking.com',
    available: true
  },
  {
    id: '2',
    name: 'Hotel Casa Fuster',
    location: 'Gràcia, Barcelona',
    rating: 4.3,
    price: 220,
    currency: 'EUR',
    image: 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=400&h=300&fit=crop',
    amenities: ['wifi', 'restaurant', 'spa'],
    distance: '1.2 km from city center',
    provider: 'Expedia',
    available: true
  },
  {
    id: '3',
    name: 'Hotel Omm',
    location: 'Eixample, Barcelona',
    rating: 4.7,
    price: 320,
    currency: 'EUR',
    image: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&h=300&fit=crop',
    amenities: ['wifi', 'parking', 'restaurant', 'gym', 'spa'],
    distance: '0.8 km from city center',
    provider: 'Hotels.com',
    available: true
  }
];

const providers = [
  { id: 'booking', name: 'Booking.com', icon: '🏨' },
  { id: 'expedia', name: 'Expedia', icon: '✈️' },
  { id: 'hotels', name: 'Hotels.com', icon: '🏨' },
  { id: 'airbnb', name: 'Airbnb', icon: '🏠' }
];

export function HotelBookingSystem({ 
  destination, 
  nights, 
  guests, 
  checkInDate, 
  checkOutDate, 
  onBookingUpdate,
  existingBooking 
}: HotelBookingSystemProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedProvider, setSelectedProvider] = useState('all');
  const [selectedHotel, setSelectedHotel] = useState<Hotel | null>(null);
  const [bookingDetails, setBookingDetails] = useState({
    rooms: 1,
    guests: guests,
    checkIn: checkInDate,
    checkOut: checkOutDate
  });

  const filteredHotels = mockHotels.filter(hotel => {
    const matchesSearch = hotel.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         hotel.location.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesProvider = selectedProvider === 'all' || hotel.provider === selectedProvider;
    return matchesSearch && matchesProvider && hotel.available;
  });

  const handleBookHotel = (hotel: Hotel) => {
    const booking = {
      id: Date.now().toString(),
      hotelName: hotel.name,
      location: hotel.location,
      checkIn: bookingDetails.checkIn,
      checkOut: bookingDetails.checkOut,
      nights: nights,
      guests: bookingDetails.guests,
      rooms: bookingDetails.rooms,
      price: hotel.price * nights,
      currency: hotel.currency,
      status: 'booked' as const,
      bookingReference: `BK${Date.now()}`,
      provider: hotel.provider,
      rating: hotel.rating,
      amenities: hotel.amenities,
      image: hotel.image
    };
    
    onBookingUpdate(booking);
    setSelectedHotel(null);
  };

  const getAmenityIcon = (amenity: string) => {
    switch (amenity) {
      case 'wifi': return <Wifi className="w-4 h-4" />;
      case 'parking': return <Car className="w-4 h-4" />;
      case 'restaurant': return <Utensils className="w-4 h-4" />;
      case 'gym': return <Dumbbell className="w-4 h-4" />;
      default: return <Check className="w-4 h-4" />;
    }
  };

  if (existingBooking) {
    return (
      <div className="space-y-4">
        <Card className="border-green-200 bg-green-50">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-green-800 flex items-center gap-2">
                <Check className="w-5 h-5" />
                Hotel Booked
              </CardTitle>
              <Badge className="bg-green-100 text-green-800">Confirmed</Badge>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex items-start gap-4">
                <img 
                  src={existingBooking.image} 
                  alt={existingBooking.hotelName}
                  className="w-20 h-20 rounded-lg object-cover"
                />
                <div className="flex-1">
                  <h3 className="font-semibold text-lg">{existingBooking.hotelName}</h3>
                  <p className="text-gray-600 flex items-center gap-1">
                    <MapPin className="w-4 h-4" />
                    {existingBooking.location}
                  </p>
                  <div className="flex items-center gap-2 mt-2">
                    <div className="flex items-center gap-1">
                      <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                      <span className="text-sm">{existingBooking.rating}</span>
                    </div>
                    <Badge variant="outline">{existingBooking.provider}</Badge>
                  </div>
                </div>
              </div>
              
              <Separator />
              
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-gray-600">Check-in:</span>
                  <p className="font-medium">{new Date(existingBooking.checkIn).toLocaleDateString()}</p>
                </div>
                <div>
                  <span className="text-gray-600">Check-out:</span>
                  <p className="font-medium">{new Date(existingBooking.checkOut).toLocaleDateString()}</p>
                </div>
                <div>
                  <span className="text-gray-600">Guests:</span>
                  <p className="font-medium">{existingBooking.guests} guests, {existingBooking.rooms} room(s)</p>
                </div>
                <div>
                  <span className="text-gray-600">Total:</span>
                  <p className="font-medium text-green-600">{existingBooking.currency} {existingBooking.price}</p>
                </div>
              </div>
              
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-600">Booking Reference:</span>
                <code className="text-sm bg-gray-100 px-2 py-1 rounded">{existingBooking.bookingReference}</code>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Search and Filters */}
      <div className="space-y-4">
        <div className="flex items-center gap-4">
          <div className="flex-1">
            <Label htmlFor="search">Search hotels</Label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                id="search"
                placeholder="Search by hotel name or location..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
          <div>
            <Label htmlFor="provider">Provider</Label>
            <Select value={selectedProvider} onValueChange={setSelectedProvider}>
              <SelectTrigger className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Providers</SelectItem>
                {providers.map(provider => (
                  <SelectItem key={provider.id} value={provider.id}>
                    {provider.icon} {provider.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Booking Details */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <Calendar className="w-5 h-5" />
              Booking Details
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="checkin">Check-in</Label>
                <Input
                  id="checkin"
                  type="date"
                  value={bookingDetails.checkIn}
                  onChange={(e) => setBookingDetails(prev => ({ ...prev, checkIn: e.target.value }))}
                />
              </div>
              <div>
                <Label htmlFor="checkout">Check-out</Label>
                <Input
                  id="checkout"
                  type="date"
                  value={bookingDetails.checkOut}
                  onChange={(e) => setBookingDetails(prev => ({ ...prev, checkOut: e.target.value }))}
                />
              </div>
              <div>
                <Label htmlFor="guests">Guests</Label>
                <Select 
                  value={bookingDetails.guests.toString()} 
                  onValueChange={(value) => setBookingDetails(prev => ({ ...prev, guests: parseInt(value) }))}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[1, 2, 3, 4, 5, 6].map(num => (
                      <SelectItem key={num} value={num.toString()}>
                        <Users className="w-4 h-4 mr-2 inline" />
                        {num} {num === 1 ? 'Guest' : 'Guests'}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="rooms">Rooms</Label>
                <Select 
                  value={bookingDetails.rooms.toString()} 
                  onValueChange={(value) => setBookingDetails(prev => ({ ...prev, rooms: parseInt(value) }))}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[1, 2, 3, 4].map(num => (
                      <SelectItem key={num} value={num.toString()}>
                        {num} {num === 1 ? 'Room' : 'Rooms'}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Hotel Results */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">
            Available Hotels in {destination}
          </h3>
          <Badge variant="outline">{filteredHotels.length} hotels found</Badge>
        </div>

        <ScrollArea className="h-96">
          <div className="space-y-4">
            {filteredHotels.map((hotel) => (
              <Card key={hotel.id} className="hover:shadow-md transition-shadow">
                <CardContent className="p-4">
                  <div className="flex gap-4">
                    <img 
                      src={hotel.image} 
                      alt={hotel.name}
                      className="w-24 h-24 rounded-lg object-cover"
                    />
                    <div className="flex-1">
                      <div className="flex items-start justify-between">
                        <div>
                          <h4 className="font-semibold text-lg">{hotel.name}</h4>
                          <p className="text-gray-600 flex items-center gap-1">
                            <MapPin className="w-4 h-4" />
                            {hotel.location}
                          </p>
                          <p className="text-sm text-gray-500">{hotel.distance}</p>
                        </div>
                        <div className="text-right">
                          <div className="text-2xl font-bold text-green-600">
                            {hotel.currency} {hotel.price}
                          </div>
                          <div className="text-sm text-gray-500">per night</div>
                          <div className="text-lg font-semibold text-green-700">
                            {hotel.currency} {hotel.price * nights}
                          </div>
                          <div className="text-sm text-gray-500">for {nights} nights</div>
                        </div>
                      </div>
                      
                      <div className="flex items-center gap-2 mt-2">
                        <div className="flex items-center gap-1">
                          <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                          <span className="text-sm font-medium">{hotel.rating}</span>
                        </div>
                        <Badge variant="outline">{hotel.provider}</Badge>
                      </div>
                      
                      <div className="flex items-center gap-2 mt-2">
                        {hotel.amenities.map(amenity => (
                          <div key={amenity} className="flex items-center gap-1 text-gray-600">
                            {getAmenityIcon(amenity)}
                            <span className="text-xs capitalize">{amenity}</span>
                          </div>
                        ))}
                      </div>
                      
                      <div className="flex gap-2 mt-4">
                        <Button 
                          variant="outline" 
                          size="sm"
                          onClick={() => window.open(`https://${hotel.provider}.com`, '_blank')}
                        >
                          <ExternalLink className="w-4 h-4 mr-2" />
                          View on {hotel.provider}
                        </Button>
                        <Button 
                          size="sm"
                          onClick={() => handleBookHotel(hotel)}
                        >
                          <Hotel className="w-4 h-4 mr-2" />
                          Book Now
                        </Button>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </ScrollArea>
      </div>
    </div>
  );
}



