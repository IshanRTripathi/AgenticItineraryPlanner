import React, { useState } from 'react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Input } from './ui/input';
import { Badge } from './ui/badge';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from './ui/sheet';
import { Alert, AlertDescription } from './ui/alert';
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
import { TripData } from '../App';

interface EditModeProps {
  tripData: TripData;
  onSave: (updatedTrip: Partial<TripData>) => void;
  onCancel: () => void;
}

interface EditAction {
  type: 'add' | 'remove' | 'reorder' | 'replace';
  dayIndex: number;
  activityIndex?: number;
  data?: any;
}

const suggestedActivities = [
  { id: 'new-1', title: 'Local Art Gallery', type: 'cultural', duration: 90, cost: 500 },
  { id: 'new-2', title: 'Street Food Tour', type: 'food', duration: 120, cost: 800 },
  { id: 'new-3', title: 'Rooftop Bar', type: 'nightlife', duration: 180, cost: 1500 },
  { id: 'new-4', title: 'Cooking Class', type: 'food', duration: 150, cost: 1200 },
  { id: 'new-5', title: 'City Walking Tour', type: 'cultural', duration: 180, cost: 600 },
];

export function EditMode({ tripData, onSave, onCancel }: EditModeProps) {
  const [editedItinerary, setEditedItinerary] = useState(tripData.itinerary);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDay, setSelectedDay] = useState(0);
  const [undoStack, setUndoStack] = useState<EditAction[]>([]);
  const [redoStack, setRedoStack] = useState<EditAction[]>([]);
  const [conflicts, setConflicts] = useState<string[]>([]);
  const [lockedActivities, setLockedActivities] = useState<Set<string>>(new Set());

  const checkConflicts = (dayData: any) => {
    const newConflicts: string[] = [];
    
    for (let i = 0; i < dayData.activities.length - 1; i++) {
      const current = dayData.activities[i];
      const next = dayData.activities[i + 1];
      
      const currentEndTime = new Date(`2000-01-01T${current.time}`);
      currentEndTime.setMinutes(currentEndTime.getMinutes() + current.duration);
      
      const nextStartTime = new Date(`2000-01-01T${next.time}`);
      
      if (currentEndTime > nextStartTime) {
        newConflicts.push(`Time overlap between ${current.title} and ${next.title}`);
      }
    }
    
    setConflicts(newConflicts);
  };

  const addActivity = (dayIndex: number, activity: any) => {
    const newItinerary = { ...editedItinerary };
    const timeSlot = getNextAvailableTime(newItinerary.days[dayIndex]);
    
    const newActivity = {
      ...activity,
      id: `${dayIndex}-${Date.now()}`,
      time: timeSlot,
      location: { lat: 28.6139 + Math.random() * 0.1, lng: 77.2090 + Math.random() * 0.1 },
      openingHours: '09:00 - 18:00',
      dietSuitable: true,
      walkingTime: Math.floor(Math.random() * 20) + 5
    };
    
    newItinerary.days[dayIndex].activities.push(newActivity);
    newItinerary.days[dayIndex].activities.sort((a: any, b: any) => a.time.localeCompare(b.time));
    
    setEditedItinerary(newItinerary);
    checkConflicts(newItinerary.days[dayIndex]);
  };

  const removeActivity = (dayIndex: number, activityIndex: number) => {
    const activity = editedItinerary.days[dayIndex].activities[activityIndex];
    if (lockedActivities.has(activity.id)) return;
    
    const newItinerary = { ...editedItinerary };
    newItinerary.days[dayIndex].activities.splice(activityIndex, 1);
    setEditedItinerary(newItinerary);
    checkConflicts(newItinerary.days[dayIndex]);
  };

  const getNextAvailableTime = (dayData: any) => {
    if (dayData.activities.length === 0) return '09:00';
    
    const lastActivity = dayData.activities[dayData.activities.length - 1];
    const lastEndTime = new Date(`2000-01-01T${lastActivity.time}`);
    lastEndTime.setMinutes(lastEndTime.getMinutes() + lastActivity.duration + 30); // 30 min buffer
    
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
                              {editedItinerary.days.map((day: any, index: number) => (
                                <Button
                                  key={index}
                                  variant="outline"
                                  className="w-full justify-start"
                                  onClick={() => addActivity(index, activity)}
                                >
                                  Day {day.day} - {new Date(day.date).toLocaleDateString()}
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
              {editedItinerary.days.map((day: any, index: number) => (
                <Button
                  key={index}
                  variant={selectedDay === index ? "default" : "outline"}
                  onClick={() => setSelectedDay(index)}
                  className="whitespace-nowrap"
                >
                  Day {day.day}
                </Button>
              ))}
            </div>

            {/* Selected Day Activities */}
            <Card>
              <CardHeader>
                <CardTitle>
                  Day {editedItinerary.days[selectedDay].day} Activities
                </CardTitle>
                <CardDescription>
                  Drag to reorder, click to edit, or remove activities
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {editedItinerary.days[selectedDay].activities.map((activity: any, index: number) => (
                    <div
                      key={activity.id}
                      className={`p-4 border rounded-lg ${lockedActivities.has(activity.id) ? 'bg-blue-50 border-blue-200' : 'bg-white'}`}
                    >
                      <div className="flex items-start gap-3">
                        <div className="flex flex-col items-center mt-1">
                          <GripVertical className="h-4 w-4 text-gray-400 cursor-move" />
                          <span className="text-xs mt-1 font-medium">
                            {activity.time}
                          </span>
                        </div>
                        
                        <div className="flex-1">
                          <div className="flex items-start justify-between mb-2">
                            <h4 className="font-medium">{activity.title}</h4>
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
                              {activity.duration} min
                            </Badge>
                            <Badge variant="outline" className="text-xs">
                              {activity.type}
                            </Badge>
                            {activity.dietSuitable && (
                              <Badge variant="outline" className="text-xs bg-green-50">
                                Diet ✓
                              </Badge>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                  
                  {editedItinerary.days[selectedDay].activities.length === 0 && (
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