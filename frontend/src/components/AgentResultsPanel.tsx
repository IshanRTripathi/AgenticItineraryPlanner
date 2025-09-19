import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { Separator } from './ui/separator';
import { 
  Plane, 
  Hotel, 
  Utensils, 
  MapPin, 
  Bus, 
  Star, 
  Clock, 
  DollarSign,
  Users,
  CheckCircle,
  ChevronDown,
  ChevronUp,
  ExternalLink,
  Info
} from 'lucide-react';
import { 
  FlightOption, 
  HotelOption, 
  RestaurantOption, 
  PlaceOption, 
  TransportOption 
} from '../types/TripData';

interface AgentResultsPanelProps {
  agentResults?: {
    flights?: FlightOption[];
    hotels?: HotelOption[];
    restaurants?: RestaurantOption[];
    places?: PlaceOption[];
    transport?: TransportOption[];
  };
  onSelectOption?: (type: string, optionId: string) => void;
  selectedOptions?: {
    flights?: string;
    hotels?: string;
    restaurants?: string;
    places?: string;
    transport?: string;
  };
}

export function AgentResultsPanel({ 
  agentResults, 
  onSelectOption, 
  selectedOptions = {} 
}: AgentResultsPanelProps) {
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(['flights', 'hotels']));

  const toggleSection = (section: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(section)) {
      newExpanded.delete(section);
    } else {
      newExpanded.add(section);
    }
    setExpandedSections(newExpanded);
  };

  const getCategoryColor = (category: string) => {
    switch (category) {
      case 'cost-effective': return 'bg-green-100 text-green-800';
      case 'fastest': return 'bg-blue-100 text-blue-800';
      case 'optimal': return 'bg-purple-100 text-purple-800';
      case 'budget': return 'bg-green-100 text-green-800';
      case 'mid-range': return 'bg-yellow-100 text-yellow-800';
      case 'luxury': return 'bg-red-100 text-red-800';
      case 'local': return 'bg-orange-100 text-orange-800';
      case 'dietary-friendly': return 'bg-teal-100 text-teal-800';
      case 'must-visit': return 'bg-red-100 text-red-800';
      case 'hidden-gem': return 'bg-indigo-100 text-indigo-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const renderFlightOption = (flight: FlightOption) => (
    <Card key={flight.id} className={`cursor-pointer transition-all hover:shadow-md ${
      selectedOptions.flights === flight.id ? 'ring-2 ring-blue-500' : ''
    }`} onClick={() => onSelectOption?.('flights', flight.id)}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Plane className="h-5 w-5 text-blue-600" />
            <span className="font-semibold">{flight.airline}</span>
            <span className="text-sm text-gray-500">{flight.flightNumber}</span>
          </div>
          <Badge className={getCategoryColor(flight.category)}>
            {flight.category?.replace('-', ' ') || 'Unknown'}
          </Badge>
        </div>
        
        <div className="grid grid-cols-2 gap-4 mb-3">
          <div>
            <div className="text-sm text-gray-500">Departure</div>
            <div className="font-medium">{flight.departure.time}</div>
            <div className="text-sm text-gray-600">{flight.departure.airport}</div>
          </div>
          <div>
            <div className="text-sm text-gray-500">Arrival</div>
            <div className="font-medium">{flight.arrival.time}</div>
            <div className="text-sm text-gray-600">{flight.arrival.airport}</div>
          </div>
        </div>
        
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4 text-sm text-gray-600">
            <span className="flex items-center gap-1">
              <Clock className="h-4 w-4" />
              {flight.duration}
            </span>
            <span>{flight.stops === 0 ? 'Direct' : `${flight.stops} stop${flight.stops > 1 ? 's' : ''}`}</span>
          </div>
          <div className="text-right">
            <div className="text-lg font-bold text-green-600">
              ${flight.price}
            </div>
            <div className="text-xs text-gray-500">{flight.currency}</div>
          </div>
        </div>
        
        {flight.features && flight.features.length > 0 && (
          <div className="mt-3 pt-3 border-t">
            <div className="flex flex-wrap gap-1">
              {flight.features.map((feature, index) => (
                <Badge key={index} variant="outline" className="text-xs">
                  {feature}
                </Badge>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );

  const renderHotelOption = (hotel: HotelOption) => (
    <Card key={hotel.id} className={`cursor-pointer transition-all hover:shadow-md ${
      selectedOptions.hotels === hotel.id ? 'ring-2 ring-blue-500' : ''
    }`} onClick={() => onSelectOption?.('hotels', hotel.id)}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Hotel className="h-5 w-5 text-purple-600" />
            <span className="font-semibold">{hotel.name}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1">
              <Star className="h-4 w-4 text-yellow-500 fill-current" />
              <span className="text-sm">{hotel.rating}</span>
            </div>
            <Badge className={getCategoryColor(hotel.category)}>
              {hotel.category.replace('-', ' ')}
            </Badge>
          </div>
        </div>
        
        <div className="mb-3">
          <div className="text-sm text-gray-600 mb-1">{hotel.location.address}</div>
          <div className="text-xs text-gray-500">
            {hotel.location.distanceToAttractions}km from attractions
          </div>
        </div>
        
        <div className="flex items-center justify-between mb-3">
          <div className="text-sm text-gray-600">
            {hotel.roomType}
          </div>
          <div className="text-right">
            <div className="text-lg font-bold text-green-600">
              ${hotel.price}
            </div>
            <div className="text-xs text-gray-500">per night</div>
          </div>
        </div>
        
        {hotel.amenities && hotel.amenities.length > 0 && (
          <div className="pt-3 border-t">
            <div className="flex flex-wrap gap-1">
              {hotel.amenities.slice(0, 3).map((amenity, index) => (
                <Badge key={index} variant="outline" className="text-xs">
                  {amenity}
                </Badge>
              ))}
              {hotel.amenities.length > 3 && (
                <Badge variant="outline" className="text-xs">
                  +{hotel.amenities.length - 3} more
                </Badge>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );

  const renderRestaurantOption = (restaurant: RestaurantOption) => (
    <Card key={restaurant.id} className={`cursor-pointer transition-all hover:shadow-md ${
      selectedOptions.restaurants === restaurant.id ? 'ring-2 ring-blue-500' : ''
    }`} onClick={() => onSelectOption?.('restaurants', restaurant.id)}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Utensils className="h-5 w-5 text-orange-600" />
            <span className="font-semibold">{restaurant.name}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1">
              <Star className="h-4 w-4 text-yellow-500 fill-current" />
              <span className="text-sm">{restaurant.rating}</span>
            </div>
            <Badge className={getCategoryColor(restaurant.category)}>
              {restaurant.category.replace('-', ' ')}
            </Badge>
          </div>
        </div>
        
        <div className="mb-3">
          <div className="text-sm text-gray-600 mb-1">{restaurant.cuisine} • {restaurant.priceRange}</div>
          <div className="text-xs text-gray-500">{restaurant.openingHours}</div>
        </div>
        
        {restaurant.dietaryOptions && restaurant.dietaryOptions.length > 0 && (
          <div className="pt-3 border-t">
            <div className="flex flex-wrap gap-1">
              {restaurant.dietaryOptions.map((option, index) => (
                <Badge key={index} variant="outline" className="text-xs bg-green-50">
                  {option}
                </Badge>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );

  const renderPlaceOption = (place: PlaceOption) => (
    <Card key={place.id} className={`cursor-pointer transition-all hover:shadow-md ${
      selectedOptions.places === place.id ? 'ring-2 ring-blue-500' : ''
    }`} onClick={() => onSelectOption?.('places', place.id)}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <MapPin className="h-5 w-5 text-red-600" />
            <span className="font-semibold">{place.name}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1">
              <Star className="h-4 w-4 text-yellow-500 fill-current" />
              <span className="text-sm">{place.rating}</span>
            </div>
            <Badge className={getCategoryColor(place.category)}>
              {place.category.replace('-', ' ')}
            </Badge>
          </div>
        </div>
        
        <div className="mb-3">
          <div className="text-sm text-gray-600 mb-1 capitalize">{place.type}</div>
          <div className="text-xs text-gray-500">{place.bestTimeToVisit}</div>
        </div>
        
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4 text-sm text-gray-600">
            <span className="flex items-center gap-1">
              <Clock className="h-4 w-4" />
              {place.duration} min
            </span>
            <span className="flex items-center gap-1">
              <Users className="h-4 w-4" />
              {place.crowdLevel} crowd
            </span>
          </div>
          <div className="text-right">
            <div className="text-sm font-semibold">
              {place.entryFee > 0 ? `€${place.entryFee}` : 'Free'}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  const renderTransportOption = (transport: TransportOption) => (
    <Card key={transport.id} className={`cursor-pointer transition-all hover:shadow-md ${
      selectedOptions.transport === transport.id ? 'ring-2 ring-blue-500' : ''
    }`} onClick={() => onSelectOption?.('transport', transport.id)}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Bus className="h-5 w-5 text-green-600" />
            <span className="font-semibold">{transport.name}</span>
          </div>
          <Badge className={getCategoryColor(transport.type)}>
            {transport.type.replace('-', ' ')}
          </Badge>
        </div>
        
        <div className="mb-3">
          <div className="text-sm text-gray-600">
            {transport.route.from} → {transport.route.to}
          </div>
          {transport.schedule && (
            <div className="text-xs text-gray-500">{transport.schedule}</div>
          )}
        </div>
        
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4 text-sm text-gray-600">
            <span className="flex items-center gap-1">
              <Clock className="h-4 w-4" />
              {transport.duration} min
            </span>
            <span className="flex items-center gap-1">
              <Users className="h-4 w-4" />
              {transport.convenience} convenience
            </span>
          </div>
          <div className="text-right">
            <div className="text-sm font-semibold">
              €{transport.cost}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  if (!agentResults) {
    return (
      <Card>
        <CardContent className="p-6 text-center">
          <Info className="h-8 w-8 mx-auto mb-2 text-gray-400" />
          <p className="text-gray-600">No agent results available yet</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 mb-4">
        <h3 className="text-lg font-semibold">Agent Results</h3>
        <Badge variant="outline" className="text-xs">
          {agentResults ? Object.values(agentResults).flat().length : 0} options found
        </Badge>
      </div>

      {/* Flights */}
      {agentResults.flights && agentResults.flights.length > 0 && (
        <Card>
          <CardHeader 
            className="cursor-pointer" 
            onClick={() => toggleSection('flights')}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Plane className="h-5 w-5 text-blue-600" />
                <CardTitle className="text-base">Flight Options</CardTitle>
                <Badge variant="outline">{agentResults.flights.length} found</Badge>
              </div>
              {expandedSections.has('flights') ? 
                <ChevronUp className="h-4 w-4" /> : 
                <ChevronDown className="h-4 w-4" />
              }
            </div>
            <CardDescription>
              Compare flight options by price, duration, and convenience
            </CardDescription>
          </CardHeader>
          {expandedSections.has('flights') && (
            <CardContent>
              <div className="grid gap-3">
                {agentResults.flights.map(renderFlightOption)}
              </div>
            </CardContent>
          )}
        </Card>
      )}

      {/* Hotels */}
      {agentResults.hotels && agentResults.hotels.length > 0 && (
        <Card>
          <CardHeader 
            className="cursor-pointer" 
            onClick={() => toggleSection('hotels')}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Hotel className="h-5 w-5 text-purple-600" />
                <CardTitle className="text-base">Hotel Options</CardTitle>
                <Badge variant="outline">{agentResults.hotels.length} found</Badge>
              </div>
              {expandedSections.has('hotels') ? 
                <ChevronUp className="h-4 w-4" /> : 
                <ChevronDown className="h-4 w-4" />
              }
            </div>
            <CardDescription>
              Choose from budget to luxury accommodations near attractions
            </CardDescription>
          </CardHeader>
          {expandedSections.has('hotels') && (
            <CardContent>
              <div className="grid gap-3">
                {agentResults.hotels.map(renderHotelOption)}
              </div>
            </CardContent>
          )}
        </Card>
      )}

      {/* Restaurants */}
      {agentResults.restaurants && agentResults.restaurants.length > 0 && (
        <Card>
          <CardHeader 
            className="cursor-pointer" 
            onClick={() => toggleSection('restaurants')}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Utensils className="h-5 w-5 text-orange-600" />
                <CardTitle className="text-base">Restaurant Options</CardTitle>
                <Badge variant="outline">{agentResults.restaurants.length} found</Badge>
              </div>
              {expandedSections.has('restaurants') ? 
                <ChevronUp className="h-4 w-4" /> : 
                <ChevronDown className="h-4 w-4" />
              }
            </div>
            <CardDescription>
              Local cuisine and dietary-friendly options
            </CardDescription>
          </CardHeader>
          {expandedSections.has('restaurants') && (
            <CardContent>
              <div className="grid gap-3">
                {agentResults.restaurants.map(renderRestaurantOption)}
              </div>
            </CardContent>
          )}
        </Card>
      )}

      {/* Places */}
      {agentResults.places && agentResults.places.length > 0 && (
        <Card>
          <CardHeader 
            className="cursor-pointer" 
            onClick={() => toggleSection('places')}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <MapPin className="h-5 w-5 text-red-600" />
                <CardTitle className="text-base">Attractions</CardTitle>
                <Badge variant="outline">{agentResults.places.length} found</Badge>
              </div>
              {expandedSections.has('places') ? 
                <ChevronUp className="h-4 w-4" /> : 
                <ChevronDown className="h-4 w-4" />
              }
            </div>
            <CardDescription>
              Must-visit attractions and hidden gems
            </CardDescription>
          </CardHeader>
          {expandedSections.has('places') && (
            <CardContent>
              <div className="grid gap-3">
                {agentResults.places.map(renderPlaceOption)}
              </div>
            </CardContent>
          )}
        </Card>
      )}

      {/* Transport */}
      {agentResults.transport && agentResults.transport.length > 0 && (
        <Card>
          <CardHeader 
            className="cursor-pointer" 
            onClick={() => toggleSection('transport')}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Bus className="h-5 w-5 text-green-600" />
                <CardTitle className="text-base">Transportation</CardTitle>
                <Badge variant="outline">{agentResults.transport.length} found</Badge>
              </div>
              {expandedSections.has('transport') ? 
                <ChevronUp className="h-4 w-4" /> : 
                <ChevronDown className="h-4 w-4" />
              }
            </div>
            <CardDescription>
              Public and private transport options
            </CardDescription>
          </CardHeader>
          {expandedSections.has('transport') && (
            <CardContent>
              <div className="grid gap-3">
                {agentResults.transport.map(renderTransportOption)}
              </div>
            </CardContent>
          )}
        </Card>
      )}
    </div>
  );
}
