import React, { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Input } from './ui/input';
import { Badge } from './ui/badge';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from './ui/sheet';
import { Alert, AlertDescription } from './ui/alert';
import { useItinerary } from '../state/query/hooks';
import { 
  ArrowLeft, 
  Save, 
  Search, 
  Plus, 
  Trash2, 
  GripVertical,
  Undo,
  Redo,
  Lock,
  Clock,
  AlertTriangle
} from 'lucide-react';
import { TripData } from '../types/TripData';

interface EditModeProps {
  tripData: TripData;
  onSave: (updatedTrip: Partial<TripData>) => void;
  onCancel: () => void;
}

interface EditAction {
  type: 'add' | 'remove' | 'reorder' | 'replace';
  dayIndex: number;
  activityIndex?: number;
  data?: unknown;
}

const suggestedActivities = [
  { id: 'new-1', title: 'Local Art Gallery', type: 'cultural', duration: 90, cost: 500 },
  { id: 'new-2', title: 'Street Food Tour', type: 'food', duration: 120, cost: 800 },
  { id: 'new-3', title: 'Rooftop Bar', type: 'nightlife', duration: 180, cost: 1500 },
  { id: 'new-4', title: 'Cooking Class', type: 'food', duration: 150, cost: 1200 },
  { id: 'new-5', title: 'City Walking Tour', type: 'cultural', duration: 180, cost: 600 },
];

export function EditMode({ tripData, onSave, onCancel }: EditModeProps) {
  // Fetch fresh data from API instead of using cached props
  const { data: freshTripData, isLoading, error } = useItinerary(tripData.id);

  // Use fresh data if available, fallback to props
  const currentTripData = freshTripData || tripData;

  const [editedItinerary, setEditedItinerary] = useState(currentTripData.itinerary || { days: [] });
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDay, setSelectedDay] = useState(0);
  const [undoStack, setUndoStack] = useState<EditAction[]>([]);
  const [redoStack, setRedoStack] = useState<EditAction[]>([]);
  const [conflicts, setConflicts] = useState<string[]>([]);
  const [lockedActivities, setLockedActivities] = useState<Set<string>>(new Set());

  // Update editedItinerary when fresh data arrives
  useEffect(() => {
    if (currentTripData.itinerary) {
      setEditedItinerary(currentTripData.itinerary);
    }
  }, [currentTripData]);

  // Show loading state while fetching fresh data
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading edit mode...</p>
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
            <p className="text-red-600 mb-4">Failed to load edit data</p>
            <Button onClick={onCancel}>Go Back</Button>
          </div>
        </div>
      </div>
    );
  }

  // If no itinerary data, show empty state
  if (!currentTripData.itinerary || !currentTripData.itinerary.days || currentTripData.itinerary.days.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="max-w-md mx-auto">
            <Clock className="h-12 w-12 mx-auto mb-4 text-gray-400" />
            <h2 className="text-xl font-medium mb-2">No Itinerary to Edit</h2>
            <p className="text-gray-600 mb-4">
              This trip doesn't have a generated itinerary yet. Please wait for the agents to complete processing.
            </p>
            <Button variant="outline" onClick={onCancel}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              Go Back
            </Button>
          </div>
        </div>
      </div>
    );
  }

  const checkConflicts = (dayData: unknown) => {
    const newConflicts: string[] = [];
    const dayDataTyped = dayData as { activities?: unknown[]; components?: unknown[] };
    const activities = dayDataTyped.activities || dayDataTyped.components || [];
    
    for (let i = 0; i < activities.length - 1; i++) {
      const current = activities[i] as { timing?: { startTime?: string; duration?: number }; time?: string; duration?: number; name?: string; title?: string };
      const next = activities[i + 1] as { timing?: { startTime?: string }; time?: string; name?: string; title?: string };
      
      const currentTime = current.timing?.startTime || current.time || '09:00';
      const currentDuration = current.timing?.duration || current.duration || 60;
      const nextTime = next.timing?.startTime || next.time || '10:00';
      
      const currentEndTime = new Date(`2000-01-01T${currentTime}`);
      currentEndTime.setMinutes(currentEndTime.getMinutes() + currentDuration);
      
      const nextStartTime = new Date(`2000-01-01T${nextTime}`);
      
      if (currentEndTime > nextStartTime) {
        newConflicts.push(`Time overlap between ${current.name || current.title || 'Activity'} and ${next.name || next.title || 'Activity'}`);
      }
    }
    
    setConflicts(newConflicts);
  };

  const addActivity = (dayIndex: number, activity: unknown) => {
    const newItinerary = { ...editedItinerary };
    const timeSlot = getNextAvailableTime(newItinerary.days[dayIndex]);
    const activityTyped = activity as { type?: string; title: string; duration?: number; cost?: number };
    
    const newActivity = {
      id: `${dayIndex}-${Date.now()}`,
      type: activityTyped.type || 'attraction',
      name: activityTyped.title,
      description: `A ${activityTyped.type} activity in ${tripData.destination}`,
      location: {
        name: activityTyped.title,
        address: 'Address not available',
        coordinates: { lat: 28.6139 + Math.random() * 0.1, lng: 77.2090 + Math.random() * 0.1 }
      },
      timing: {
        startTime: timeSlot,
        endTime: '10:00',
        duration: activityTyped.duration || 60,
        suggestedDuration: activityTyped.duration || 60
      },
      cost: {
        pricePerPerson: activityTyped.cost || 0,
        currency: 'EUR',
        priceRange: 'mid-range' as const,
        includesWhat: [],
        additionalCosts: []
      },
      travel: {
        distanceFromPrevious: Math.floor(Math.random() * 20) + 5,
        travelTimeFromPrevious: Math.floor(Math.random() * 20) + 5,
        transportMode: 'walking' as const,
        transportCost: 0
      },
      details: {
        rating: 4.0,
        reviewCount: 0,
        category: activityTyped.type,
        tags: [activityTyped.type],
        openingHours: {
          monday: { open: '09:00', close: '18:00' },
          tuesday: { open: '09:00', close: '18:00' },
          wednesday: { open: '09:00', close: '18:00' },
          thursday: { open: '09:00', close: '18:00' },
          friday: { open: '09:00', close: '18:00' },
          saturday: { open: '09:00', close: '18:00' },
          sunday: { open: '09:00', close: '18:00' }
        },
        contact: {},
        accessibility: {
          wheelchairAccessible: false,
          elevatorAccess: false,
          restrooms: false,
          parking: false
        },
        amenities: []
      },
      booking: {
        required: false,
        bookingUrl: undefined,
        phone: undefined,
        notes: undefined
      },
      media: {
        images: [],
        videos: [],
        virtualTour: undefined
      },
      tips: {
        bestTimeToVisit: 'Morning',
        whatToBring: [],
        insider: [],
        warnings: []
      },
      alternatives: [],
      userNotes: undefined,
      isCustom: true,
      addedByUser: true,
      priority: 'recommended' as const
    };
    
    if (!newItinerary.days[dayIndex].activities) {
      newItinerary.days[dayIndex].activities = [];
    }
    newItinerary.days[dayIndex].activities.push(newActivity);
    newItinerary.days[dayIndex].activities.sort((a: unknown, b: unknown) => 
      ((a as any).timing?.startTime || (a as any).time || '09:00').localeCompare((b as any).timing?.startTime || (b as any).time || '09:00')
    );
    
    setEditedItinerary(newItinerary);
    checkConflicts(newItinerary?.days?.[dayIndex]);
  };

  const removeActivity = (dayIndex: number, activityIndex: number) => {
    const activities = editedItinerary?.days?.[dayIndex]?.activities || editedItinerary?.days?.[dayIndex]?.components || [];
    const activity = activities[activityIndex];
    if (lockedActivities.has(activity?.id)) return;
    
    const newItinerary = { ...editedItinerary };
    if (newItinerary.days[dayIndex].activities) {
      newItinerary.days[dayIndex].activities.splice(activityIndex, 1);
    } else if (newItinerary.days[dayIndex].components) {
      newItinerary.days[dayIndex].components.splice(activityIndex, 1);
    }
    setEditedItinerary(newItinerary);
    checkConflicts(newItinerary?.days?.[dayIndex]);
  };

  const getNextAvailableTime = (dayData: unknown) => {
    const dayDataTyped = dayData as { activities?: unknown[]; components?: unknown[] };
    const activities = dayDataTyped.activities || dayDataTyped.components || [];
    if (activities.length === 0) return '09:00';
    
    const lastActivity = activities[activities.length - 1] as { timing?: { startTime?: string; duration?: number }; time?: string; duration?: number };
    const lastTime = lastActivity.timing?.startTime || lastActivity.time || '09:00';
    const lastDuration = lastActivity.timing?.duration || lastActivity.duration || 60;
    
    const lastEndTime = new Date(`2000-01-01T${lastTime}`);
    lastEndTime.setMinutes(lastEndTime.getMinutes() + lastDuration + 30); // 30 min buffer
    
    return lastEndTime.toTimeString().slice(0, 5);
  };

  const toggleLock = (activityId: string) => {
    const newLocked = new Set(lockedActivities);
    if (newLocked.has(activityId)) {
      newLocked.delete(activityId);
    } else {
      newLocked.add(activityId);
    }
    setLockedActivities(newLocked);
  };

  const filteredSuggestions = suggestedActivities.filter(activity =>
    activity.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    activity.type.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const undo = () => {
    if (undoStack.length > 0) {
      // Implementation for undo functionality
      console.log('Undo action');
    }
  };

  const redo = () => {
    if (redoStack.length > 0) {
      // Implementation for redo functionality
      console.log('Redo action');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b">
        <div className="max-w-6xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Button variant="ghost" onClick={onCancel}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                Cancel
              </Button>
              <div>
                <h1 className="text-2xl">Edit Itinerary</h1>
                <p className="text-gray-600">{tripData.destination}</p>
              </div>
            </div>
            
            <div className="flex gap-2">
              <Button variant="outline" onClick={undo} disabled={undoStack.length === 0}>
                <Undo className="h-4 w-4" />
              </Button>
              <Button variant="outline" onClick={redo} disabled={redoStack.length === 0}>
                <Redo className="h-4 w-4" />
              </Button>
              <Button onClick={() => onSave({ itinerary: editedItinerary })}>
                <Save className="h-4 w-4 mr-2" />
                Save Changes
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Conflicts Alert */}
      {conflicts.length > 0 && (
        <div className="max-w-6xl mx-auto px-4 pt-4">
          <Alert className="border-orange-200 bg-orange-50">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              <strong>Conflicts detected:</strong>
              <ul className="mt-1 ml-4 list-disc">
                {conflicts.map((conflict, index) => (
                  <li key={index} className="text-sm">{conflict}</li>
                ))}
              </ul>
            </AlertDescription>
          </Alert>
        </div>
      )}

      <div className="max-w-6xl mx-auto px-4 py-6">
        <div className="grid lg:grid-cols-4 gap-6">
          {/* Search & Add Panel */}
          <div className="lg:col-span-1">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Search className="h-5 w-5" />
                  Add Activities
                </CardTitle>
                <CardDescription>
                  Search and add new activities to your itinerary
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input
                  placeholder="Search activities..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {filteredSuggestions.map((activity) => (
                    <div key={activity.id} className="p-3 border rounded-lg hover:bg-gray-50">
                      <div className="flex items-start justify-between mb-2">
                        <h4 className="font-medium text-sm">{activity.title}</h4>
                        <Sheet>
                          <SheetTrigger asChild>
                            <Button size="sm" variant="outline">
                              <Plus className="h-3 w-3" />
                            </Button>
                          </SheetTrigger>
                          <SheetContent>
                            <SheetHeader>
                              <SheetTitle>Add to Day</SheetTitle>
                              <SheetDescription>
                                Select which day to add "{activity.title}" to
                              </SheetDescription>
                            </SheetHeader>
                            <div className="mt-6 space-y-3">
                              {editedItinerary?.days?.map((day, index: number) => (
                                <Button
                                  key={index}
                                  variant="outline"
                                  className="w-full justify-start"
                                  onClick={() => addActivity(index, activity)}
                                >
                                  Day {day.dayNumber || index + 1} - {new Date(day.date).toLocaleDateString()}
                                </Button>
                              ))}
                            </div>
                          </SheetContent>
                        </Sheet>
                      </div>
                      <div className="flex items-center gap-2 text-xs text-gray-500">
                        <Badge variant="outline" className="text-xs">
                          {activity.type}
                        </Badge>
                        <span>{activity.duration} min</span>
                        <span>₹{activity.cost}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Days Selector */}
          <div className="lg:col-span-3">
            <div className="flex gap-2 mb-4 overflow-x-auto">
              {editedItinerary?.days?.map((day, index: number) => (
                <Button
                  key={index}
                  variant={selectedDay === index ? "default" : "outline"}
                  onClick={() => setSelectedDay(index)}
                  className="whitespace-nowrap"
                >
                  Day {day.dayNumber || index + 1}
                </Button>
              ))}
            </div>

            {/* Selected Day Activities */}
            <Card>
              <CardHeader>
                <CardTitle>
                  Day {editedItinerary?.days?.[selectedDay]?.dayNumber || selectedDay + 1} Activities
                </CardTitle>
                <CardDescription>
                  Drag to reorder, click to edit, or remove activities
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {(editedItinerary?.days?.[selectedDay]?.activities || editedItinerary?.days?.[selectedDay]?.components || []).map((activity, index: number) => (
                    <div
                      key={activity.id}
                      className={`p-4 border rounded-lg ${lockedActivities.has(activity.id) ? 'bg-blue-50 border-blue-200' : 'bg-white'}`}
                    >
                      <div className="flex items-start gap-3">
                        <div className="flex flex-col items-center mt-1">
                          <GripVertical className="h-4 w-4 text-gray-400 cursor-move" />
                          <span className="text-xs mt-1 font-medium">
                            {activity.timing?.startTime || activity.time || '09:00'}
                          </span>
                        </div>
                        
                        <div className="flex-1">
                          <div className="flex items-start justify-between mb-2">
                            <h4 className="font-medium">{activity.name || activity.title}</h4>
                            <div className="flex gap-1">
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => toggleLock(activity.id)}
                                className={lockedActivities.has(activity.id) ? 'text-blue-600' : ''}
                              >
                                <Lock className="h-3 w-3" />
                              </Button>
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => removeActivity(selectedDay, index)}
                                disabled={lockedActivities.has(activity.id)}
                              >
                                <Trash2 className="h-3 w-3" />
                              </Button>
                            </div>
                          </div>
                          
                          <p className="text-sm text-gray-600 mb-2">
                            {activity.description}
                          </p>
                          
                          <div className="flex items-center gap-2">
                            <Badge variant="outline" className="text-xs">
                              {activity.timing?.duration || activity.duration || 60} min
                            </Badge>
                            <Badge variant="outline" className="text-xs">
                              {activity.type}
                            </Badge>
                            {activity.details?.accessibility?.wheelchairAccessible && (
                              <Badge variant="outline" className="text-xs bg-green-50">
                                Accessible ✓
                              </Badge>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                  
                  {(!editedItinerary?.days?.[selectedDay]?.activities && !editedItinerary?.days?.[selectedDay]?.components) || 
                   ((editedItinerary.days[selectedDay].activities || editedItinerary.days[selectedDay].components || []).length === 0) && (
                    <div className="text-center py-8 text-gray-500">
                      <Clock className="h-8 w-8 mx-auto mb-2" />
                      <p>No activities planned for this day</p>
                      <p className="text-sm">Add some activities from the sidebar</p>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}