import React, { useState } from 'react';
import { Button } from '../../ui/button';
import { Card } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../ui/select';
import { ScrollArea } from '../../ui/scroll-area';
import { 
  MapPin, 
  Star, 
  Users, 
  Plus, 
  Clock 
} from 'lucide-react';
import { DiscoverViewProps } from '../shared/types';
import { getAllDestinations, calculateDistance } from '../../../data/destinations';

export function DiscoverView({ tripData, destinations, onAddToTrip }: DiscoverViewProps) {
  const [selectedDiscoverCountry, setSelectedDiscoverCountry] = useState('thailand');

  const selectedCountry = selectedDiscoverCountry;
  const destinationsData = getAllDestinations(selectedCountry === 'all' ? undefined : selectedCountry);
  
  return (
    <div className="flex h-full overflow-hidden">
      <div className="w-1/3 border-r border-gray-200 p-6 overflow-y-auto">
        <div className="mb-6">
          <Select value={selectedCountry} onValueChange={setSelectedDiscoverCountry}>
            <SelectTrigger>
              <SelectValue placeholder="All countries" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="thailand">Thailand</SelectItem>
              <SelectItem value="japan">Japan</SelectItem>
              <SelectItem value="usa">USA</SelectItem>
              <SelectItem value="all">All countries</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-6">
          <div>
            <h3 className="font-medium mb-3">Travel Articles</h3>
            <div className="space-y-2">
              {tripData.itinerary?.highlights?.map((highlight: any, index: number) => (
                <div key={index} className="p-3 border rounded-lg hover:bg-gray-50 cursor-pointer">
                  <p className="text-sm">Best {highlight.toLowerCase()} experiences</p>
                </div>
              )) || (
                <div className="p-3 border rounded-lg hover:bg-gray-50 cursor-pointer">
                  <p className="text-sm">Local travel guides and tips</p>
                </div>
              )}
            </div>
          </div>

          <div>
            <h3 className="font-medium mb-3">Destinations ({destinationsData.length})</h3>
            <h2 className="text-xl font-semibold mb-4">
              Destinations in {selectedCountry === 'all' ? 'All Countries' : selectedCountry.charAt(0).toUpperCase() + selectedCountry.slice(1)}
            </h2>
            
            <ScrollArea className="h-[600px]">
              <div className="space-y-3">
                {destinationsData.map((destination: any) => {
                  // Calculate distance from current location if available
                  const currentLocation = destinations[0];
                  const distance = currentLocation?.lat && currentLocation?.lng 
                    ? calculateDistance(
                        currentLocation.lat, 
                        currentLocation.lng, 
                        destination.coordinates.lat, 
                        destination.coordinates.lng
                      )
                    : null;

                  return (
                    <Card key={destination.id} className="p-3 hover:shadow-md transition-shadow cursor-pointer">
                      <div className="relative">
                        <div className="w-full h-24 bg-gray-200 rounded mb-3 overflow-hidden">
                          <img 
                            src={destination.image} 
                            alt={destination.name}
                            className="w-full h-full object-cover"
                            onError={(e) => {
                              e.currentTarget.src = 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=300&h=200&fit=crop';
                            }}
                          />
                        </div>
                        
                        {distance && (
                          <Badge className="absolute top-2 left-2 bg-blue-100 text-blue-700 text-xs">
                            {Math.round(distance)} km away
                          </Badge>
                        )}
                        
                        <h4 className="font-medium mb-2">{destination.name}</h4>
                        <p className="text-sm text-gray-600 mb-2 line-clamp-2">{destination.description}</p>
                        
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center space-x-2 text-xs text-gray-500">
                            <Star className="w-3 h-3 fill-yellow-400 text-yellow-400" />
                            <span>{destination.rating}</span>
                            <Users className="w-3 h-3" />
                            <span>{(destination.visitCount / 1000).toFixed(1)}k</span>
                          </div>
                          <Badge variant="outline" className="text-xs">
                            {destination.currency} {destination.averageCost}/day
                          </Badge>
                        </div>
                        
                        <div className="flex flex-wrap gap-1 mb-3">
                          {destination.tags.slice(0, 3).map((tag: string) => (
                            <Badge key={tag} variant="secondary" className="text-xs">
                              {tag}
                            </Badge>
                          ))}
                        </div>
                        
                        <div className="flex gap-2">
                          <Button size="sm" variant="outline" className="flex-1 text-xs">
                            <Plus className="w-3 h-3 mr-1" />
                            Add to Trip
                          </Button>
                          <Button size="sm" variant="ghost" className="px-2">
                            <Star className="w-3 h-3" />
                          </Button>
                        </div>
                      </div>
                    </Card>
                  );
                })}
              </div>
            </ScrollArea>
          </div>
        </div>
      </div>

      <div className="flex-1 p-6 overflow-y-auto">
        <div className="mb-6">
          <h2 className="text-2xl font-semibold mb-2">Suggested Places in {tripData.endLocation?.name || tripData.destination}</h2>
          <p className="text-gray-600">Discover amazing places to visit and add them to your itinerary</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[
            {
              id: '1',
              name: 'Sagrada Familia',
              type: 'attraction',
              description: 'Famous basilica designed by Antoni Gaudí, a masterpiece of Catalan modernism.',
              rating: 4.6,
              cost: 25,
              currency: 'EUR',
              duration: '2-3 hours',
              image: 'https://images.unsplash.com/photo-1539650116574-75c0c6d73c6e?w=300&h=200&fit=crop',
              tags: ['architecture', 'religious', 'unesco'],
              location: 'Eixample, Barcelona'
            },
            {
              id: '2',
              name: 'Park Güell',
              type: 'attraction',
              description: 'Whimsical park with colorful mosaics and unique architecture by Gaudí.',
              rating: 4.4,
              cost: 10,
              currency: 'EUR',
              duration: '2-4 hours',
              image: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=300&h=200&fit=crop',
              tags: ['park', 'architecture', 'nature'],
              location: 'Gràcia, Barcelona'
            },
            {
              id: '3',
              name: 'La Boqueria Market',
              type: 'market',
              description: 'Vibrant food market with fresh produce, local delicacies, and tapas bars.',
              rating: 4.3,
              cost: 15,
              currency: 'EUR',
              duration: '1-2 hours',
              image: 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=300&h=200&fit=crop',
              tags: ['food', 'market', 'local'],
              location: 'Ciutat Vella, Barcelona'
            },
            {
              id: '4',
              name: 'Gothic Quarter',
              type: 'neighborhood',
              description: 'Historic medieval quarter with narrow streets, ancient buildings, and charming squares.',
              rating: 4.5,
              cost: 0,
              currency: 'EUR',
              duration: '2-3 hours',
              image: 'https://images.unsplash.com/photo-1555993539-1732b0258235?w=300&h=200&fit=crop',
              tags: ['historic', 'walking', 'culture'],
              location: 'Ciutat Vella, Barcelona'
            },
            {
              id: '5',
              name: 'Casa Batlló',
              type: 'attraction',
              description: 'Colorful modernist building by Gaudí with organic shapes and vibrant colors.',
              rating: 4.4,
              cost: 35,
              currency: 'EUR',
              duration: '1-2 hours',
              image: 'https://images.unsplash.com/photo-1558618047-3c8c76ca7d13?w=300&h=200&fit=crop',
              tags: ['architecture', 'modernist', 'museum'],
              location: 'Eixample, Barcelona'
            },
            {
              id: '6',
              name: 'Barceloneta Beach',
              type: 'beach',
              description: 'Popular city beach with golden sand, beach bars, and water sports.',
              rating: 4.2,
              cost: 0,
              currency: 'EUR',
              duration: '2-4 hours',
              image: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=300&h=200&fit=crop',
              tags: ['beach', 'relaxation', 'water-sports'],
              location: 'Barceloneta, Barcelona'
            }
          ].map((place) => (
            <Card key={place.id} className="overflow-hidden hover:shadow-lg transition-shadow">
              <div className="relative">
                <div className="w-full h-48 bg-gray-200 overflow-hidden">
                  <img 
                    src={place.image} 
                    alt={place.name}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      e.currentTarget.src = 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=300&h=200&fit=crop';
                    }}
                  />
                </div>
                
                <div className="absolute top-3 right-3">
                  <Badge className="bg-white/90 text-gray-800">
                    {place.type}
                  </Badge>
                </div>
              </div>
              
              <div className="p-4">
                <h3 className="font-semibold mb-2">{place.name}</h3>
                <p className="text-sm text-gray-600 mb-3 line-clamp-2">{place.description}</p>
                
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-2 text-sm text-gray-500">
                    <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                    <span>{place.rating}</span>
                    <Clock className="w-4 h-4" />
                    <span>{place.duration}</span>
                  </div>
                  <Badge variant="outline" className="text-xs">
                    {place.currency} {place.cost}
                  </Badge>
                </div>
                
                <div className="flex flex-wrap gap-1 mb-4">
                  {place.tags.map((tag: string) => (
                    <Badge key={tag} variant="secondary" className="text-xs">
                      {tag}
                    </Badge>
                  ))}
                </div>
                
                <div className="space-y-2">
                  <div className="flex items-center text-sm text-gray-600">
                    <MapPin className="w-4 h-4 mr-1" />
                    <span>{place.location}</span>
                  </div>
                  
                  <div className="flex gap-2">
                    <Select onValueChange={(dayNumber) => {
                      console.log(`Adding ${place.name} to Day ${dayNumber}`);
                      onAddToTrip(place, parseInt(dayNumber));
                    }}>
                      <SelectTrigger className="flex-1 text-xs">
                        <SelectValue placeholder="Select day" />
                      </SelectTrigger>
                      <SelectContent>
                        {tripData.itinerary?.days?.map((day: any, index: number) => (
                          <SelectItem key={index} value={(day.dayNumber || index + 1).toString()}>
                            Day {day.dayNumber || index + 1}
                          </SelectItem>
                        )) || (
                          <SelectItem value="1">Day 1</SelectItem>
                        )}
                      </SelectContent>
                    </Select>
                    <Button size="sm" variant="outline" className="px-3">
                      <Plus className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
