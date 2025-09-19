import React, { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { 
  ArrowLeft, 
  Edit, 
  MapPin, 
  Clock, 
  Footprints, 
  Sun,
  Utensils,
  Calendar,
  Users,
  DollarSign,
  Navigation,
  GitBranch,
  Zap
} from 'lucide-react';
import { TripData } from '../types/TripData';
import { AgentResultsPanel } from './AgentResultsPanel';
import { useItinerary } from '../state/query/hooks';

interface ItineraryOverviewProps {
  tripData: TripData;
  onEdit: () => void;
  onWorkflowEdit?: () => void;
  onProceedToCost: () => void;
  onBack: () => void;
  onOpenPlanner?: () => void;
}

export function ItineraryOverview({ tripData, onEdit, onWorkflowEdit, onProceedToCost, onBack, onOpenPlanner }: ItineraryOverviewProps) {
  const [selectedDay, setSelectedDay] = useState(1);
  const [selectedOptions, setSelectedOptions] = useState<{[key: string]: string}>({});
  
  // Fetch fresh data from API instead of using cached props
  const { data: freshTripData, isLoading, error } = useItinerary(tripData.id);
  
  // Use fresh data if available, fallback to props
  const currentTripData = freshTripData || tripData;
  const itinerary = currentTripData.itinerary;
  
  const handleSelectOption = (type: string, optionId: string) => {
    setSelectedOptions(prev => ({
      ...prev,
      [type]: optionId
    }));
  };
  
  // Show loading state while fetching fresh data
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading itinerary...</p>
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
            <p className="text-red-600 mb-4">Failed to load itinerary data</p>
            <Button onClick={onBack}>Go Back</Button>
          </div>
        </div>
      </div>
    );
  }

  if (!itinerary || !itinerary.days || itinerary.days.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="max-w-md mx-auto">
            <MapPin className="h-12 w-12 mx-auto mb-4 text-gray-400" />
            <h2 className="text-xl font-medium mb-2">No Itinerary Available</h2>
            <p className="text-gray-600 mb-4">
              This trip doesn't have a generated itinerary yet. The agents are still processing your request.
            </p>
            <div className="flex gap-2 justify-center">
              <Button variant="outline" onClick={onBack}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                Go Back
              </Button>
              <Button onClick={onEdit}>
                <Edit className="h-4 w-4 mr-2" />
                Edit Plan
              </Button>
              {onOpenPlanner && (
                <Button variant="outline" onClick={onOpenPlanner}>
                  <MapPin className="h-4 w-4 mr-2" />
                  Stippl Planner
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'food': return <Utensils className="h-4 w-4" />;
      case 'cultural': return <MapPin className="h-4 w-4" />;
      case 'adventure': return <Navigation className="h-4 w-4" />;
      case 'nature': return <Sun className="h-4 w-4" />;
      case 'shopping': return <DollarSign className="h-4 w-4" />;
      case 'nightlife': return <Users className="h-4 w-4" />;
      default: return <MapPin className="h-4 w-4" />;
    }
  };

  const getPaceIndicator = (walkingTime: number) => {
    if (walkingTime < 20) return { color: 'bg-green-500', label: 'Easy' };
    if (walkingTime < 40) return { color: 'bg-yellow-500', label: 'Moderate' };
    return { color: 'bg-red-500', label: 'Active' };
  };

  const formatTime = (time: string) => {
    return new Date(`2000-01-01T${time}`).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  };

  const selectedDayData = itinerary?.days?.find((day) => day.dayNumber === selectedDay);

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
                <h1 className="text-2xl">{tripData.destination || tripData.endLocation?.name || 'Trip'}</h1>
                <p className="text-gray-600">
                  {new Date(tripData.dates.start).toLocaleDateString()} - {new Date(tripData.dates.end).toLocaleDateString()}
                </p>
              </div>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" onClick={onEdit}>
                <Edit className="h-4 w-4 mr-2" />
                Edit Plan
              </Button>
              {onWorkflowEdit && (
                <Button variant="outline" onClick={onWorkflowEdit}>
                  <GitBranch className="h-4 w-4 mr-2" />
                  Workflow Builder
                </Button>
              )}
              {onOpenPlanner && (
                <Button variant="outline" onClick={onOpenPlanner}>
                  <MapPin className="h-4 w-4 mr-2" />
                  Stippl Planner
                </Button>
              )}
              <Button onClick={onProceedToCost}>
                View Costs
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-6">
        <div className="grid lg:grid-cols-4 gap-6">
          {/* Map Section */}
          <div className="lg:col-span-1">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MapPin className="h-5 w-5" />
                  Map View
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="aspect-square bg-gray-200 rounded-lg flex items-center justify-center">
                  <div className="text-center text-gray-500">
                    <MapPin className="h-8 w-8 mx-auto mb-2" />
                    <p className="text-sm">Interactive Map</p>
                    <p className="text-xs">Day {selectedDay} Activities</p>
                  </div>
                </div>
                
                {/* Day Summary */}
                {selectedDayData && (
                  <div className="mt-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm">Walking Time</span>
                      <div className="flex items-center gap-2">
                        <Footprints className="h-4 w-4" />
                        <span className="text-sm">{selectedDayData.totalDistance || 0} min</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm">Total Duration</span>
                      <div className="flex items-center gap-2">
                        <Clock className="h-4 w-4" />
                        <span className="text-sm">{Math.round((selectedDayData.totalDuration || 0) / 60)} hours</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm">Pace</span>
                      <Badge variant="outline" className={`${getPaceIndicator(selectedDayData.totalDistance || 0).color} text-white`}>
                        {getPaceIndicator(selectedDayData.totalDistance || 0).label}
                      </Badge>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Weather Widget */}
            <Card className="mt-4">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Sun className="h-5 w-5" />
                  Weather Forecast
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {itinerary.days?.slice(0, 3).map((day, index: number) => (
                    <div key={index} className="flex items-center justify-between py-2">
                      <span className="text-sm">
                        Day {index + 1}
                      </span>
                      <div className="flex items-center gap-2">
                        <span className="text-sm">{Math.round(day.weather?.temperature?.high || 25)}°C</span>
                        <div className="w-6 h-6 bg-yellow-200 rounded-full flex items-center justify-center">
                          ☀️
                        </div>
                      </div>
                    </div>
                  )) || (
                    <div className="text-sm text-gray-500 py-2">
                      Weather forecast not available
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Itinerary Section */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle>Daily Itinerary</CardTitle>
                <CardDescription>
                  Your personalized {itinerary?.totalDays || 0}-day adventure
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Tabs value={selectedDay.toString()} onValueChange={(value) => setSelectedDay(parseInt(value))}>
                  <TabsList className="grid w-full grid-cols-4 lg:grid-cols-6">
                    {itinerary?.days?.map((day) => (
                      <TabsTrigger key={day.dayNumber} value={day.dayNumber.toString()}>
                        Day {day.dayNumber}
                      </TabsTrigger>
                    ))}
                  </TabsList>

                  {itinerary?.days?.map((day) => (
                    <TabsContent key={day.dayNumber} value={day.dayNumber.toString()} className="mt-6">
                      <div className="space-y-4">
                        <div className="flex items-center gap-2 mb-4">
                          <Calendar className="h-4 w-4" />
                          <span className="font-medium">
                            {new Date(day.date).toLocaleDateString('en-US', { 
                              weekday: 'long', 
                              month: 'long', 
                              day: 'numeric' 
                            })}
                          </span>
                        </div>

                        {(day.components || day.activities || []).map((activity, index: number) => (
                          <div key={activity.id} className="relative">
                            {/* Timeline connector */}
                            {index < (day.components || day.activities || []).length - 1 && (
                              <div className="absolute left-6 top-12 w-0.5 h-16 bg-gray-200" />
                            )}
                            
                            <div className="flex gap-4">
                              <div className="flex flex-col items-center">
                                <div className="w-12 h-12 bg-indigo-100 rounded-full flex items-center justify-center">
                                  {getActivityIcon(activity.type)}
                                </div>
                                <span className="text-xs mt-1 font-medium">
                                  {formatTime(activity.timing?.startTime || activity.time || '09:00')}
                                </span>
                              </div>
                              
                              <div className="flex-1 pb-6">
                                <Card>
                                  <CardContent className="p-4">
                                    <div className="flex items-start justify-between mb-2">
                                      <h4 className="font-medium">{activity.name}</h4>
                                      <div className="flex gap-1">
                                        <Badge variant="outline" className="text-xs">
                                          {activity.timing?.duration || activity.duration || 60} min
                                        </Badge>
                                        {activity.details?.accessibility?.wheelchairAccessible && (
                                          <Badge variant="outline" className="text-xs bg-green-50">
                                            Accessible ✓
                                          </Badge>
                                        )}
                                      </div>
                                    </div>
                                    
                                    <p className="text-sm text-gray-600 mb-3">
                                      {activity.description}
                                    </p>
                                    
                                    <div className="flex items-center justify-between text-xs text-gray-500">
                                      <span>📍 {activity.travel?.travelTimeFromPrevious || activity.walkingTime || 0} min walk</span>
                                      <span>🕒 {activity.details?.openingHours?.monday?.open || activity.openingHours || '09:00'} - {activity.details?.openingHours?.monday?.close || '18:00'}</span>
                                    </div>
                                  </CardContent>
                                </Card>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </TabsContent>
                  ))}
                </Tabs>
              </CardContent>
            </Card>
          </div>

          {/* Agent Results Section */}
          <div className="lg:col-span-1">
            <AgentResultsPanel 
              agentResults={tripData.agentResults}
              onSelectOption={handleSelectOption}
              selectedOptions={selectedOptions}
            />
          </div>
        </div>
      </div>
    </div>
  );
}