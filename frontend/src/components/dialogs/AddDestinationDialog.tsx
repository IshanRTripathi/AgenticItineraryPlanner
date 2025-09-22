import React, { useState } from 'react';
import { Button } from '../ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '../ui/dialog';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import { ScrollArea } from '../ui/scroll-area';
import { Separator } from '../ui/separator';
import { 
  Search, 
  MapPin, 
  Star, 
  Plus,
  Calendar,
  Users,
  Clock
} from 'lucide-react';

interface AddDestinationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAddDestination: (destinationData: {
    name: string;
    dayNumber: number;
    isNewDay: boolean;
    destination: any;
  }) => void;
  existingDays: number[];
  currentTripData: any;
}

interface Destination {
  id: string;
  name: string;
  country: string;
  coordinates: {
    lat: number;
    lng: number;
  };
  description: string;
  rating: number;
  visitCount: number;
  averageCost: number;
  currency: string;
  tags: string[];
  image: string;
}

const mockDestinations: Destination[] = [
  {
    id: '1',
    name: 'Sagrada Familia',
    country: 'Spain',
    coordinates: { lat: 41.4036, lng: 2.1744 },
    description: 'Famous basilica designed by Antoni Gaudí, a masterpiece of Catalan modernism.',
    rating: 4.6,
    visitCount: 4500000,
    averageCost: 25,
    currency: 'EUR',
    tags: ['architecture', 'religious', 'unesco', 'landmark'],
    image: 'https://images.unsplash.com/photo-1539650116574-75c0c6d73c6e?w=300&h=200&fit=crop'
  },
  {
    id: '2',
    name: 'Park Güell',
    country: 'Spain',
    coordinates: { lat: 41.4145, lng: 2.1527 },
    description: 'Whimsical park with colorful mosaics and unique architecture by Gaudí.',
    rating: 4.4,
    visitCount: 3200000,
    averageCost: 10,
    currency: 'EUR',
    tags: ['park', 'architecture', 'nature', 'garden'],
    image: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=300&h=200&fit=crop'
  },
  {
    id: '3',
    name: 'La Boqueria Market',
    country: 'Spain',
    coordinates: { lat: 41.3819, lng: 2.1715 },
    description: 'Vibrant food market with fresh produce, local delicacies, and tapas bars.',
    rating: 4.3,
    visitCount: 2800000,
    averageCost: 15,
    currency: 'EUR',
    tags: ['food', 'market', 'local', 'shopping'],
    image: 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=300&h=200&fit=crop'
  },
  {
    id: '4',
    name: 'Gothic Quarter',
    country: 'Spain',
    coordinates: { lat: 41.3833, lng: 2.1767 },
    description: 'Historic medieval quarter with narrow streets, ancient buildings, and charming squares.',
    rating: 4.5,
    visitCount: 3800000,
    averageCost: 0,
    currency: 'EUR',
    tags: ['historic', 'walking', 'culture', 'architecture'],
    image: 'https://images.unsplash.com/photo-1555993539-1732b0258235?w=300&h=200&fit=crop'
  },
  {
    id: '5',
    name: 'Casa Batlló',
    country: 'Spain',
    coordinates: { lat: 41.3917, lng: 2.1649 },
    description: 'Colorful modernist building by Gaudí with organic shapes and vibrant colors.',
    rating: 4.4,
    visitCount: 2100000,
    averageCost: 35,
    currency: 'EUR',
    tags: ['architecture', 'modernist', 'museum', 'art'],
    image: 'https://images.unsplash.com/photo-1558618047-3c8c76ca7d13?w=300&h=200&fit=crop'
  },
  {
    id: '6',
    name: 'Barceloneta Beach',
    country: 'Spain',
    coordinates: { lat: 41.3784, lng: 2.1925 },
    description: 'Popular city beach with golden sand, beach bars, and water sports.',
    rating: 4.2,
    visitCount: 5200000,
    averageCost: 0,
    currency: 'EUR',
    tags: ['beach', 'relaxation', 'water-sports', 'outdoor'],
    image: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=300&h=200&fit=crop'
  },
  {
    id: '7',
    name: 'Montjuïc Hill',
    country: 'Spain',
    coordinates: { lat: 41.3633, lng: 2.1647 },
    description: 'Historic hill with panoramic city views, museums, and the Magic Fountain.',
    rating: 4.5,
    visitCount: 2900000,
    averageCost: 0,
    currency: 'EUR',
    tags: ['viewpoint', 'historic', 'museum', 'outdoor'],
    image: 'https://images.unsplash.com/photo-1571115764595-644a1f56a55c?w=300&h=200&fit=crop'
  },
  {
    id: '8',
    name: 'Casa Milà (La Pedrera)',
    country: 'Spain',
    coordinates: { lat: 41.3954, lng: 2.1619 },
    description: 'Another Gaudí masterpiece with undulating stone facade and unique rooftop.',
    rating: 4.3,
    visitCount: 1800000,
    averageCost: 28,
    currency: 'EUR',
    tags: ['architecture', 'modernist', 'museum', 'rooftop'],
    image: 'https://images.unsplash.com/photo-1558618047-3c8c76ca7d13?w=300&h=200&fit=crop'
  }
];

export function AddDestinationDialog({ 
  open, 
  onOpenChange, 
  onAddDestination, 
  existingDays,
  currentTripData 
}: AddDestinationDialogProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDay, setSelectedDay] = useState<string>('');
  const [selectedDestination, setSelectedDestination] = useState<Destination | null>(null);

  const filteredDestinations = mockDestinations.filter(destination =>
    destination.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    destination.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    destination.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const handleAddDestination = () => {
    if (!selectedDestination || !selectedDay) return;

    const isNewDay = selectedDay === 'new';
    const dayNumber = isNewDay ? Math.max(...existingDays, 0) + 1 : parseInt(selectedDay);

    onAddDestination({
      name: selectedDestination.name,
      dayNumber,
      isNewDay,
      destination: selectedDestination
    });

    // Reset form
    setSearchQuery('');
    setSelectedDay('');
    setSelectedDestination(null);
    onOpenChange(false);
  };

  const maxDay = Math.max(...existingDays, 0);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>Add New Destination</DialogTitle>
          <DialogDescription>
            Search for a destination and select which day to add it to your itinerary.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Search */}
          <div className="space-y-2">
            <Label htmlFor="search">Search destinations</Label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                id="search"
                placeholder="Search by name, description, or tags..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>

          {/* Day Selection */}
          <div className="space-y-2">
            <Label htmlFor="day">Select Day</Label>
            <Select value={selectedDay} onValueChange={setSelectedDay}>
              <SelectTrigger>
                <SelectValue placeholder="Choose which day to add this destination" />
              </SelectTrigger>
              <SelectContent>
                {existingDays.map(day => (
                  <SelectItem key={day} value={day.toString()}>
                    Day {day}
                  </SelectItem>
                ))}
                <SelectItem value="new">
                  <Plus className="w-4 h-4 mr-2 inline" />
                  Create New Day ({maxDay + 1})
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Destination Results */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">Search Results</h3>
              <Badge variant="outline">{filteredDestinations.length} destinations found</Badge>
            </div>

            <ScrollArea className="h-96">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {filteredDestinations.map((destination) => (
                  <Card 
                    key={destination.id} 
                    className={`cursor-pointer transition-all hover:shadow-md ${
                      selectedDestination?.id === destination.id 
                        ? 'ring-2 ring-blue-500 bg-blue-50' 
                        : 'hover:bg-gray-50'
                    }`}
                    onClick={() => setSelectedDestination(destination)}
                  >
                    <CardContent className="p-4">
                      <div className="space-y-3">
                        <div className="flex gap-3">
                          <img 
                            src={destination.image} 
                            alt={destination.name}
                            className="w-16 h-16 rounded-lg object-cover"
                          />
                          <div className="flex-1 min-w-0">
                            <h4 className="font-semibold text-lg truncate">{destination.name}</h4>
                            <p className="text-sm text-gray-600 flex items-center gap-1">
                              <MapPin className="w-3 h-3" />
                              {destination.country}
                            </p>
                            <div className="flex items-center gap-2 mt-1">
                              <div className="flex items-center gap-1">
                                <Star className="w-3 h-3 fill-yellow-400 text-yellow-400" />
                                <span className="text-sm font-medium">{destination.rating}</span>
                              </div>
                              <Badge variant="outline" className="text-xs">
                                {destination.currency} {destination.averageCost}/day
                              </Badge>
                            </div>
                          </div>
                        </div>
                        
                        <p className="text-sm text-gray-600 line-clamp-2">
                          {destination.description}
                        </p>
                        
                        <div className="flex flex-wrap gap-1">
                          {destination.tags.slice(0, 3).map(tag => (
                            <Badge key={tag} variant="secondary" className="text-xs">
                              {tag}
                            </Badge>
                          ))}
                          {destination.tags.length > 3 && (
                            <Badge variant="secondary" className="text-xs">
                              +{destination.tags.length - 3} more
                            </Badge>
                          )}
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </ScrollArea>
          </div>

          {/* Selected Destination Summary */}
          {selectedDestination && (
            <Card className="border-blue-200 bg-blue-50">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <img 
                      src={selectedDestination.image} 
                      alt={selectedDestination.name}
                      className="w-12 h-12 rounded-lg object-cover"
                    />
                    <div>
                      <h4 className="font-semibold">{selectedDestination.name}</h4>
                      <p className="text-sm text-gray-600">
                        {selectedDay === 'new' 
                          ? `Will be added to new Day ${maxDay + 1}`
                          : `Will be added to Day ${selectedDay}`
                        }
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="flex items-center gap-1">
                      <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                      <span className="text-sm font-medium">{selectedDestination.rating}</span>
                    </div>
                    <Badge variant="outline">
                      {selectedDestination.currency} {selectedDestination.averageCost}/day
                    </Badge>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          <Separator />

          {/* Actions */}
          <div className="flex justify-end gap-3">
            <Button variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button 
              onClick={handleAddDestination}
              disabled={!selectedDestination || !selectedDay}
            >
              <Plus className="w-4 h-4 mr-2" />
              Add Destination
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}



