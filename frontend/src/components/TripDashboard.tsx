import React, { useState } from 'react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { 
  ArrowLeft,
  Plus,
  Search,
  Filter,
  MapPin,
  Calendar,
  Users,
  Download,
  Share2,
  Eye,
  MoreVertical,
  Copy,
  Settings,
  Plane,
  Clock,
  Star
} from 'lucide-react';
import { TripData } from '../App';

interface TripDashboardProps {
  trips: TripData[];
  onCreateTrip: () => void;
  onViewTrip: (trip: TripData) => void;
  onBack: () => void;
}

export function TripDashboard({ trips, onCreateTrip, onViewTrip, onBack }: TripDashboardProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterBy, setFilterBy] = useState('all');
  const [sortBy, setSortBy] = useState('date');

  // Mock additional trips for demo
  const mockTrips: TripData[] = [
    {
      id: 'demo-1',
      destination: 'Tokyo, Japan',
      dates: { start: '2024-03-15T00:00:00Z', end: '2024-03-22T00:00:00Z' },
      budget: 150000,
      partySize: 2,
      themes: ['Culture', 'Food', 'Technology'],
      dietaryRestrictions: ['Vegetarian'],
      walkingTolerance: 4,
      pace: 3,
      stayType: 'Hotel',
      transport: 'Train',
      itinerary: {
        totalDays: 7,
        destination: 'Tokyo, Japan',
        weather: {
          forecast: [
            { temperature: { high: 22, low: 15 }, condition: 'sunny' },
            { temperature: { high: 24, low: 16 }, condition: 'partly-cloudy' },
            { temperature: { high: 20, low: 14 }, condition: 'rainy' }
          ]
        },
        days: [
          {
            day: 1,
            date: '2024-03-15',
            totalWalkingTime: 45,
            totalDuration: 480,
            activities: [
              {
                id: 'act-1',
                title: 'Senso-ji Temple',
                description: 'Visit Tokyo\'s oldest temple in Asakusa district',
                time: '09:00',
                duration: 90,
                type: 'cultural',
                walkingTime: 0,
                openingHours: '6:00 AM - 5:00 PM',
                dietSuitable: true
              },
              {
                id: 'act-2',
                title: 'Traditional Sushi Experience',
                description: 'Authentic sushi lunch at Jiro\'s apprentice restaurant',
                time: '12:30',
                duration: 120,
                type: 'food',
                walkingTime: 15,
                openingHours: '12:00 PM - 3:00 PM',
                dietSuitable: false
              }
            ]
          },
          {
            day: 2,
            date: '2024-03-16',
            totalWalkingTime: 60,
            totalDuration: 420,
            activities: [
              {
                id: 'act-3',
                title: 'Meiji Shrine',
                description: 'Peaceful shrine in the heart of Tokyo',
                time: '10:00',
                duration: 90,
                type: 'cultural',
                walkingTime: 20,
                openingHours: '9:00 AM - 4:30 PM',
                dietSuitable: true
              }
            ]
          }
        ]
      },
      bookingData: {
        bookingReference: 'BK87654321',
        status: 'confirmed'
      }
    },
    {
      id: 'demo-2',
      destination: 'Paris, France',
      dates: { start: '2024-01-10T00:00:00Z', end: '2024-01-17T00:00:00Z' },
      budget: 120000,
      partySize: 1,
      themes: ['Heritage', 'Art', 'Photography'],
      dietaryRestrictions: [],
      walkingTolerance: 5,
      pace: 2,
      stayType: 'Airbnb',
      transport: 'Walking',
      itinerary: {
        totalDays: 7,
        destination: 'Paris, France',
        weather: {
          forecast: [
            { temperature: { high: 8, low: 3 }, condition: 'cloudy' },
            { temperature: { high: 10, low: 4 }, condition: 'sunny' },
            { temperature: { high: 6, low: 1 }, condition: 'rainy' }
          ]
        },
        days: [
          {
            day: 1,
            date: '2024-01-10',
            totalWalkingTime: 30,
            totalDuration: 360,
            activities: [
              {
                id: 'act-4',
                title: 'Louvre Museum',
                description: 'World\'s largest art museum and historic monument',
                time: '10:00',
                duration: 180,
                type: 'cultural',
                walkingTime: 0,
                openingHours: '9:00 AM - 6:00 PM',
                dietSuitable: true
              },
              {
                id: 'act-5',
                title: 'Seine River Walk',
                description: 'Scenic walk along the famous river',
                time: '15:00',
                duration: 90,
                type: 'nature',
                walkingTime: 30,
                openingHours: 'All day',
                dietSuitable: true
              }
            ]
          },
          {
            day: 2,
            date: '2024-01-11',
            totalWalkingTime: 25,
            totalDuration: 300,
            activities: [
              {
                id: 'act-6',
                title: 'Eiffel Tower',
                description: 'Iconic iron lattice tower and symbol of Paris',
                time: '11:00',
                duration: 120,
                type: 'cultural',
                walkingTime: 25,
                openingHours: '9:30 AM - 11:45 PM',
                dietSuitable: true
              }
            ]
          }
        ]
      },
      bookingData: {
        bookingReference: 'BK12345678',
        status: 'completed'
      }
    }
  ];

  const allTrips = [...trips, ...mockTrips];

  const upcomingTrips = allTrips.filter(trip => 
    new Date(trip.dates.start) > new Date()
  );

  const pastTrips = allTrips.filter(trip => 
    new Date(trip.dates.end) < new Date()
  );

  const filteredTrips = allTrips.filter(trip => {
    const matchesSearch = trip.destination.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         trip.themes.some(theme => theme.toLowerCase().includes(searchQuery.toLowerCase()));
    
    if (filterBy === 'upcoming') return matchesSearch && upcomingTrips.includes(trip);
    if (filterBy === 'past') return matchesSearch && pastTrips.includes(trip);
    return matchesSearch;
  });

  const sortedTrips = filteredTrips.sort((a, b) => {
    if (sortBy === 'date') {
      return new Date(b.dates.start).getTime() - new Date(a.dates.start).getTime();
    }
    if (sortBy === 'destination') {
      return a.destination.localeCompare(b.destination);
    }
    if (sortBy === 'budget') {
      return b.budget - a.budget;
    }
    return 0;
  });

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  };

  const getTripStatus = (trip: TripData) => {
    const now = new Date();
    const start = new Date(trip.dates.start);
    const end = new Date(trip.dates.end);
    
    if (now < start) return { status: 'upcoming', color: 'bg-blue-500' };
    if (now >= start && now <= end) return { status: 'ongoing', color: 'bg-green-500' };
    return { status: 'completed', color: 'bg-gray-500' };
  };

  const duplicateTrip = (trip: TripData) => {
    // Would create a new trip based on existing one
    console.log('Duplicating trip:', trip.destination);
  };

  const shareTrip = (trip: TripData) => {
    const shareUrl = `https://tripai.com/share/${trip.id}`;
    navigator.clipboard.writeText(shareUrl);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b">
        <div className="max-w-6xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Button variant="ghost" onClick={onBack}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back
              </Button>
              <div>
                <h1 className="text-2xl">My Trips</h1>
                <p className="text-gray-600">Manage all your adventures</p>
              </div>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline">
                <Settings className="h-4 w-4 mr-2" />
                Settings
              </Button>
              <Button onClick={onCreateTrip}>
                <Plus className="h-4 w-4 mr-2" />
                New Trip
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 py-6">
        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-2">
                <Plane className="h-5 w-5 text-blue-600" />
                <div>
                  <p className="text-2xl font-semibold">{allTrips.length}</p>
                  <p className="text-sm text-gray-600">Total Trips</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-2">
                <Calendar className="h-5 w-5 text-green-600" />
                <div>
                  <p className="text-2xl font-semibold">{upcomingTrips.length}</p>
                  <p className="text-sm text-gray-600">Upcoming</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-2">
                <MapPin className="h-5 w-5 text-purple-600" />
                <div>
                  <p className="text-2xl font-semibold">
                    {new Set(allTrips.map(trip => trip.destination.split(',')[1]?.trim() || trip.destination)).size}
                  </p>
                  <p className="text-sm text-gray-600">Countries Visited</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-2">
                <Star className="h-5 w-5 text-yellow-600" />
                <div>
                  <p className="text-2xl font-semibold">₹{(allTrips.reduce((sum, trip) => sum + trip.budget, 0) / 100000).toFixed(1)}L</p>
                  <p className="text-sm text-gray-600">Total Spent</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Filters and Search */}
        <div className="flex flex-col md:flex-row gap-4 mb-6">
          <div className="flex-1">
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search destinations, themes..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
          
          <Select value={filterBy} onValueChange={setFilterBy}>
            <SelectTrigger className="w-40">
              <SelectValue placeholder="Filter" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Trips</SelectItem>
              <SelectItem value="upcoming">Upcoming</SelectItem>
              <SelectItem value="past">Past Trips</SelectItem>
            </SelectContent>
          </Select>
          
          <Select value={sortBy} onValueChange={setSortBy}>
            <SelectTrigger className="w-40">
              <SelectValue placeholder="Sort by" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="date">Date</SelectItem>
              <SelectItem value="destination">Destination</SelectItem>
              <SelectItem value="budget">Budget</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Trips Tabs */}
        <Tabs defaultValue="all" className="space-y-4">
          <TabsList>
            <TabsTrigger value="all">All Trips ({allTrips.length})</TabsTrigger>
            <TabsTrigger value="upcoming">Upcoming ({upcomingTrips.length})</TabsTrigger>
            <TabsTrigger value="past">Past ({pastTrips.length})</TabsTrigger>
          </TabsList>

          <TabsContent value="all" className="space-y-4">
            {sortedTrips.length === 0 ? (
              <Card>
                <CardContent className="p-8 text-center">
                  <MapPin className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                  <h3 className="text-lg font-medium mb-2">No trips found</h3>
                  <p className="text-gray-600 mb-4">
                    {searchQuery ? 'Try adjusting your search terms' : 'Start planning your first adventure!'}
                  </p>
                  <Button onClick={onCreateTrip}>
                    <Plus className="h-4 w-4 mr-2" />
                    Plan New Trip
                  </Button>
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {sortedTrips.map((trip) => {
                  const status = getTripStatus(trip);
                  return (
                    <Card key={trip.id} className="cursor-pointer hover:shadow-lg transition-shadow">
                      <CardHeader className="pb-3">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <CardTitle className="flex items-center gap-2 text-lg">
                              <MapPin className="h-4 w-4" />
                              {trip.destination}
                            </CardTitle>
                            <CardDescription className="flex items-center gap-4 text-sm mt-1">
                              <span className="flex items-center gap-1">
                                <Calendar className="h-3 w-3" />
                                {formatDate(trip.dates.start)}
                              </span>
                              <span className="flex items-center gap-1">
                                <Users className="h-3 w-3" />
                                {trip.partySize}
                              </span>
                            </CardDescription>
                          </div>
                          
                          <div className="flex items-center gap-2">
                            <div className={`w-2 h-2 rounded-full ${status.color}`} />
                            <Button variant="ghost" size="sm">
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      </CardHeader>
                      
                      <CardContent>
                        <div className="space-y-3">
                          <div className="flex flex-wrap gap-1">
                            {trip.themes.slice(0, 3).map((theme) => (
                              <Badge key={theme} variant="secondary" className="text-xs">
                                {theme}
                              </Badge>
                            ))}
                            {trip.themes.length > 3 && (
                              <Badge variant="secondary" className="text-xs">
                                +{trip.themes.length - 3}
                              </Badge>
                            )}
                          </div>
                          
                          <div className="flex items-center justify-between text-sm">
                            <span className="text-gray-600">Budget</span>
                            <span className="font-medium">₹{trip.budget.toLocaleString('en-IN')}</span>
                          </div>
                          
                          {trip.bookingData && (
                            <div className="flex items-center justify-between text-sm">
                              <span className="text-gray-600">Status</span>
                              <Badge variant="outline" className="text-xs capitalize">
                                {trip.bookingData.status}
                              </Badge>
                            </div>
                          )}
                          
                          <div className="flex gap-2 pt-2">
                            <Button size="sm" className="flex-1" onClick={() => onViewTrip(trip)}>
                              <Eye className="h-3 w-3 mr-1" />
                              View
                            </Button>
                            <Button size="sm" variant="outline" onClick={() => shareTrip(trip)}>
                              <Share2 className="h-3 w-3" />
                            </Button>
                            <Button size="sm" variant="outline" onClick={() => duplicateTrip(trip)}>
                              <Copy className="h-3 w-3" />
                            </Button>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            )}
          </TabsContent>

          <TabsContent value="upcoming">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {upcomingTrips.map((trip) => (
                <Card key={trip.id} className="cursor-pointer hover:shadow-lg transition-shadow">
                  <CardContent className="p-4">
                    <div className="flex items-center gap-3 mb-3">
                      <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                        <Plane className="h-5 w-5 text-blue-600" />
                      </div>
                      <div className="flex-1">
                        <h4 className="font-medium">{trip.destination}</h4>
                        <p className="text-sm text-gray-600">
                          {Math.ceil((new Date(trip.dates.start).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))} days to go
                        </p>
                      </div>
                    </div>
                    <Button size="sm" className="w-full" onClick={() => onViewTrip(trip)}>
                      View Itinerary
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </TabsContent>

          <TabsContent value="past">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {pastTrips.map((trip) => (
                <Card key={trip.id} className="cursor-pointer hover:shadow-lg transition-shadow">
                  <CardContent className="p-4">
                    <div className="flex items-center gap-3 mb-3">
                      <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
                        <Clock className="h-5 w-5 text-gray-600" />
                      </div>
                      <div className="flex-1">
                        <h4 className="font-medium">{trip.destination}</h4>
                        <p className="text-sm text-gray-600">
                          Completed {formatDate(trip.dates.end)}
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button size="sm" variant="outline" className="flex-1" onClick={() => onViewTrip(trip)}>
                        <Eye className="h-3 w-3 mr-1" />
                        View
                      </Button>
                      <Button size="sm" variant="outline" onClick={() => duplicateTrip(trip)}>
                        <Copy className="h-3 w-3 mr-1" />
                        Rebook
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}