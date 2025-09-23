import React, { useState, useEffect } from 'react';
import { Button } from '../ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Input } from '../ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { useItineraries } from '../../state/query/hooks';
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
  Star,
  Trash2
} from 'lucide-react';
import { TripData } from '../../types/TripData';
import { apiClient } from '../../services/apiClient';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '../ui/dropdown-menu';
import { useQueryClient } from '@tanstack/react-query';

interface TripDashboardProps {
  trips: TripData[];
  onCreateTrip: () => void;
  onViewTrip: (trip: TripData) => void;
  onBack: () => void;
}

export function TripDashboard({ trips, onCreateTrip, onViewTrip, onBack }: TripDashboardProps) {
  const qc = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [filterBy, setFilterBy] = useState('all');
  const [sortBy, setSortBy] = useState('date');

  // Fetch fresh data from API instead of using cached props
  const { data: freshTrips, isLoading, error } = useItineraries();
  
  // Use fresh data if available, fallback to props
  const allTrips = freshTrips || trips;

  const getStartDateString = (trip: TripData): string | undefined => {
    return trip.dates?.start || (trip as any)?.createdAt;
  };

  const getEndDateString = (trip: TripData): string | undefined => {
    return trip.dates?.end || (trip as any)?.updatedAt || (trip as any)?.createdAt;
  };

  const upcomingTrips = allTrips.filter(trip => {
    const startStr = getStartDateString(trip);
    return startStr ? new Date(startStr) > new Date() : false;
  });

  const pastTrips = allTrips.filter(trip => {
    const endStr = getEndDateString(trip);
    return endStr ? new Date(endStr) < new Date() : false;
  });

  const getDisplayDestination = (trip: TripData): string => {
    const byFields = trip.destination || trip.endLocation?.name;
    if (byFields) return byFields;
    const summary: string | undefined = (trip as any)?.summary;
    if (summary) {
      const m = summary.match(/trip to\s+([A-Za-z\s]+?)(,|\.|$)/i) || summary.match(/to\s+([A-Za-z\s]+?)(,|\.|$)/i);
      if (m && m[1]) return m[1].trim();
    }
    return 'Unknown';
  };

  const filteredTrips = allTrips.filter(trip => {
    const destination = getDisplayDestination(trip);
    const themes = trip.themes || [];
    const matchesSearch = destination.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         themes.some(theme => theme.toLowerCase().includes(searchQuery.toLowerCase()));
    
    if (filterBy === 'upcoming') return matchesSearch && upcomingTrips.includes(trip);
    if (filterBy === 'past') return matchesSearch && pastTrips.includes(trip);
    return matchesSearch;
  });

  const sortedTrips = filteredTrips.sort((a, b) => {
    if (sortBy === 'date') {
      const aStr = getStartDateString(a);
      const bStr = getStartDateString(b);
      const aTs = aStr ? new Date(aStr).getTime() : 0;
      const bTs = bStr ? new Date(bStr).getTime() : 0;
      return bTs - aTs;
    }
    if (sortBy === 'destination') {
      const aDest = getDisplayDestination(a);
      const bDest = getDisplayDestination(b);
      return aDest.localeCompare(bDest);
    }
    if (sortBy === 'budget') {
      const aVal = (a.budget?.total ?? 0);
      const bVal = (b.budget?.total ?? 0);
      return bVal - aVal;
    }
    return 0;
  });

  // Show loading state while fetching fresh data
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading trips...</p>
        </div>
      </div>
    );
  }

  // Show error state if API call failed
  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="max-w-md mx-auto">
            <p className="text-red-600 mb-4">Failed to load trips</p>
            <Button onClick={onBack}>Go Back</Button>
          </div>
        </div>
      </div>
    );
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  };

  const getTripStatus = (trip: TripData) => {
    const now = new Date();
    const startStr = getStartDateString(trip);
    const endStr = getEndDateString(trip);
    const start = startStr ? new Date(startStr) : null;
    const end = endStr ? new Date(endStr) : null;
    
    if (start && now < start) return { status: 'upcoming', color: 'bg-blue-500' };
    if (start && end && now >= start && now <= end) return { status: 'ongoing', color: 'bg-green-500' };
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

  const deleteTrip = async (trip: TripData) => {
    try {
      await apiClient.deleteItinerary(trip.id);
      // Refresh list
      qc.invalidateQueries({ queryKey: ['itineraries'] });
    } catch (e) {
      console.error('Failed to delete trip', e);
    }
  };

  function TripCardMenu({ trip }: { trip: TripData }) {
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="sm">
            <MoreVertical className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={() => onViewTrip(trip)}>
            <Eye className="h-3 w-3 mr-2" /> View
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => shareTrip(trip)}>
            <Share2 className="h-3 w-3 mr-2" /> Share
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => duplicateTrip(trip)}>
            <Copy className="h-3 w-3 mr-2" /> Duplicate
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => deleteTrip(trip)} className="text-red-600 focus:text-red-700">
            <Trash2 className="h-3 w-3 mr-2" /> Delete
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

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
                    {new Set(allTrips.map(trip => {
                      const dest = trip.destination || trip.endLocation?.name || 'Unknown';
                      return dest.split(',')[1]?.trim() || dest;
                    })).size}
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
                  <p className="text-2xl font-semibold">₹{(allTrips.reduce((sum, trip) => sum + trip.budget.total, 0) / 100000).toFixed(1)}L</p>
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
                              {`Trip to ${getDisplayDestination(trip)}`}
                            </CardTitle>
                            <CardDescription className="flex items-center gap-4 text-sm mt-1">
                              <span className="flex items-center gap-1">
                                <Calendar className="h-3 w-3" />
                                {getStartDateString(trip) && getEndDateString(trip)
                                  ? `${formatDate(getStartDateString(trip) as string)} – ${formatDate(getEndDateString(trip) as string)}`
                                  : (getStartDateString(trip) ? formatDate(getStartDateString(trip) as string) : '—')}
                              </span>
                              <span className="flex items-center gap-1">
                                <Users className="h-3 w-3" />
                                {trip.partySize || trip.travelers?.length || 1}
                              </span>
                            </CardDescription>
                          </div>
                          
                          <div className="flex items-center gap-2">
                            <div className={`w-2 h-2 rounded-full ${status.color}`} />
                            <div className="relative">
                              <TripCardMenu trip={trip} />
                            </div>
                          </div>
                        </div>
                      </CardHeader>
                      
                      <CardContent>
                        <div className="space-y-3">
                          <div className="flex flex-wrap gap-1">
                            {(trip.themes || []).slice(0, 3).map((theme) => (
                              <Badge key={theme} variant="secondary" className="text-xs">
                                {theme}
                              </Badge>
                            ))}
                            {(trip.themes || []).length > 3 && (
                              <Badge variant="secondary" className="text-xs">
                                +{(trip.themes || []).length - 3}
                              </Badge>
                            )}
                          </div>
                          
                          {typeof trip.budget?.total === 'number' && (
                          <div className="flex items-center justify-between text-sm">
                              <span className="text-gray-600">Budget</span>
                              <span className="font-medium">₹{trip.budget.total.toLocaleString('en-IN')}</span>
                            </div>
                          )}
                          
                          {trip.summary && (
                            <div className="text-sm text-gray-700">
                              {trip.summary}
                            </div>
                          )}

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
                            {/* Removed duplicate Copy button; duplicate is available in the menu */}
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


