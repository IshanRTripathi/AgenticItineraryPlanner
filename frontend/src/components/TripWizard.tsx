import React, { useState } from 'react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Slider } from './ui/slider';
import { Checkbox } from './ui/checkbox';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Badge } from './ui/badge';
import { Calendar } from './ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from './ui/popover';
import { Switch } from './ui/switch';
import { RadioGroup, RadioGroupItem } from './ui/radio-group';
import { ArrowLeft, ArrowRight, CalendarIcon, MapPin, Sparkles, Star, Clock, Users, Plus, Trash2 } from 'lucide-react';
import { TripData } from '../App';

// Date formatting utility
const formatDate = (date: Date | undefined) => {
  if (!date) return 'Pick a date';
  return date.toLocaleDateString('en-IN', { 
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });
};

interface TripWizardProps {
  onComplete: (tripData: Omit<TripData, 'id'>) => void;
  onBack: () => void;
}

interface Traveler {
  id: string;
  name: string;
  age?: string;
  preferences: {
    heritage_vs_nightlife: 'heritage' | 'nightlife' | 'both';
    nature_vs_city: 'nature' | 'city' | 'both';
    food_street_vs_fine: 'street' | 'fine' | 'both';
    museums_vs_shopping: 'museums' | 'shopping' | 'both';
    intensity: number; // 1-5
  };
  accessibility: {
    womenOnly: boolean;
    stepFree: boolean;
    lowSensory: boolean;
    visualAlerts: boolean;
    liveCaptions: boolean;
  };
  flexibleDates: {
    enabled: boolean;
    earliestDeparture?: Date;
    latestReturn?: Date;
    maxShift: number; // days
  };
}

interface Activity {
  name: string;
  time: string;
  duration: string;
  cost: number;
  rating: number;
}

interface WizardFormData {
  // Step 1 - Basics
  destination: string;
  startDate: Date | undefined;
  endDate: Date | undefined;
  travelers: number;
  budget: number;
  
  // Step 2 - Travelers
  travelersList: Traveler[];
  
  // Step 3 - Flights
  flightClass: 'economy' | 'premium' | 'business';
  flightCost: number;
  
  // Step 4 - Stay
  roomType: 'standard' | 'deluxe' | 'suite';
  stayCost: number;
  
  // Step 5 - Local Transport
  transportCost: number;
  
  // Step 6 - Activities
  activities: Activity[];
  activitiesCost: number;
  
  // Demo data toggle
  useDemoData: boolean;
}

const demoActivities: Activity[] = [
  { name: 'Breakfast Cafe', time: '08:30', duration: '45m', cost: 500, rating: 4.5 },
  { name: 'Heritage Walk', time: '10:00', duration: '3h 0m', cost: 800, rating: 4.6 },
  { name: 'Art Gallery', time: '14:00', duration: '1h 30m', cost: 300, rating: 4.2 },
  { name: 'Rooftop Dinner', time: '19:00', duration: '2h 0m', cost: 2500, rating: 4.7 }
];

export function TripWizard({ onComplete, onBack }: TripWizardProps) {
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState<WizardFormData>({
    destination: 'Tokyo, Japan',
    startDate: new Date(2024, 2, 15), // March 15, 2024
    endDate: new Date(2024, 2, 22), // March 22, 2024
    travelers: 2,
    budget: 150000,
    travelersList: [
      {
        id: '1',
        name: 'Traveler 1',
        preferences: {
          heritage_vs_nightlife: 'both',
          nature_vs_city: 'both',
          food_street_vs_fine: 'both',
          museums_vs_shopping: 'both',
          intensity: 3
        },
        accessibility: {
          womenOnly: false,
          stepFree: false,
          lowSensory: false,
          visualAlerts: false,
          liveCaptions: false
        },
        flexibleDates: {
          enabled: false,
          maxShift: 1
        }
      },
      {
        id: '2',
        name: 'Traveler 2',
        preferences: {
          heritage_vs_nightlife: 'both',
          nature_vs_city: 'both',
          food_street_vs_fine: 'both',
          museums_vs_shopping: 'both',
          intensity: 3
        },
        accessibility: {
          womenOnly: false,
          stepFree: false,
          lowSensory: false,
          visualAlerts: false,
          liveCaptions: false
        },
        flexibleDates: {
          enabled: false,
          maxShift: 1
        }
      }
    ],
    flightClass: 'economy',
    flightCost: 60000,
    roomType: 'standard',
    stayCost: 44996,
    transportCost: 15000,
    activities: demoActivities,
    activitiesCost: 30000,
    useDemoData: true
  });

  const totalSteps = 7;

  const updateFormData = (updates: Partial<WizardFormData>) => {
    setFormData(prev => ({ ...prev, ...updates }));
  };

  const toggleDemoData = (enabled: boolean) => {
    if (enabled) {
      // Fill with demo data
      updateFormData({
        destination: 'Tokyo, Japan',
        startDate: new Date(2024, 2, 15),
        endDate: new Date(2024, 2, 22),
        travelers: 2,
        budget: 150000,
        flightClass: 'economy',
        flightCost: 60000,
        roomType: 'standard',
        stayCost: 44996,
        transportCost: 15000,
        activities: demoActivities,
        activitiesCost: 30000,
        useDemoData: true
      });
    } else {
      // Clear data
      updateFormData({
        destination: '',
        startDate: undefined,
        endDate: undefined,
        travelers: 1,
        budget: 50000,
        travelersList: [{
          id: '1',
          name: 'Traveler 1',
          preferences: {
            heritage_vs_nightlife: 'both',
            nature_vs_city: 'both',
            food_street_vs_fine: 'both',
            museums_vs_shopping: 'both',
            intensity: 3
          },
          accessibility: {
            womenOnly: false,
            stepFree: false,
            lowSensory: false,
            visualAlerts: false,
            liveCaptions: false
          },
          flexibleDates: {
            enabled: false,
            maxShift: 1
          }
        }],
        flightClass: 'economy',
        flightCost: 0,
        roomType: 'standard',
        stayCost: 0,
        transportCost: 0,
        activities: [],
        activitiesCost: 0,
        useDemoData: false
      });
    }
  };

  const canProceed = () => {
    switch (currentStep) {
      case 1:
        return formData.destination && formData.startDate && formData.endDate && formData.travelers > 0 && formData.budget > 0;
      case 2:
        return formData.travelersList.length === formData.travelers;
      case 3:
        return formData.flightClass && formData.flightCost > 0;
      case 4:
        return formData.roomType && formData.stayCost > 0;
      case 5:
        return formData.transportCost >= 0;
      case 6:
        return true; // Activities are optional
      case 7:
        return true; // Review step
      default:
        return false;
    }
  };

  const calculateTotals = () => {
    const subtotal = formData.flightCost + formData.stayCost + formData.transportCost + formData.activitiesCost;
    const taxesAndFees = Math.round(subtotal * 0.18); // 18% taxes
    const total = subtotal + taxesAndFees;
    const budgetUsed = Math.round((total / formData.budget) * 100);
    const overBudget = total - formData.budget;
    
    return { subtotal, taxesAndFees, total, budgetUsed, overBudget };
  };

  const handleNext = () => {
    if (currentStep < totalSteps) {
      setCurrentStep(prev => prev + 1);
    } else {
      const totals = calculateTotals();
      onComplete({
        destination: formData.destination,
        dates: {
          start: formData.startDate!.toISOString(),
          end: formData.endDate!.toISOString()
        },
        budget: formData.budget,
        partySize: formData.travelers,
        themes: [], // Will be derived from traveler preferences
        dietaryRestrictions: [],
        walkingTolerance: 3,
        pace: 3,
        stayType: formData.roomType,
        transport: 'Mix',
        itinerary: {
          activities: formData.activities,
          costs: totals,
          travelers: formData.travelersList
        }
      });
    }
  };

  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <div className="space-y-6">
            {/* Demo Data Toggle */}
            <div className="flex items-center justify-between p-4 bg-blue-50 rounded-lg">
              <div>
                <Label>Use Demo Data</Label>
                <p className="text-sm text-gray-600">Prefill with Tokyo demo values</p>
              </div>
              <Switch
                checked={formData.useDemoData}
                onCheckedChange={toggleDemoData}
              />
            </div>

            <div>
              <Label htmlFor="destination">Destination</Label>
              <div className="relative mt-2">
                <MapPin className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                <Input
                  id="destination"
                  placeholder="Enter city or country"
                  value={formData.destination}
                  onChange={(e) => updateFormData({ destination: e.target.value })}
                  className="pl-10"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Start Date</Label>
                <Popover>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      className="w-full justify-start text-left mt-2"
                    >
                      <CalendarIcon className="mr-2 h-4 w-4" />
                      {formatDate(formData.startDate)}
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0">
                    <Calendar
                      mode="single"
                      selected={formData.startDate}
                      onSelect={(date) => updateFormData({ startDate: date })}
                      disabled={(date) => date < new Date()}
                      initialFocus
                    />
                  </PopoverContent>
                </Popover>
              </div>

              <div>
                <Label>End Date</Label>
                <Popover>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      className="w-full justify-start text-left mt-2"
                    >
                      <CalendarIcon className="mr-2 h-4 w-4" />
                      {formatDate(formData.endDate)}
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0">
                    <Calendar
                      mode="single"
                      selected={formData.endDate}
                      onSelect={(date) => updateFormData({ endDate: date })}
                      disabled={(date) => date < new Date() || (formData.startDate && date <= formData.startDate)}
                      initialFocus
                    />
                  </PopoverContent>
                </Popover>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Travelers</Label>
                <div className="mt-2">
                  <Slider
                    value={[formData.travelers]}
                    onValueChange={([value]) => {
                      updateFormData({ travelers: value });
                      // Adjust travelers list
                      const newList = [...formData.travelersList];
                      while (newList.length < value) {
                        newList.push({
                          id: (newList.length + 1).toString(),
                          name: `Traveler ${newList.length + 1}`,
                          preferences: {
                            heritage_vs_nightlife: 'both',
                            nature_vs_city: 'both',
                            food_street_vs_fine: 'both',
                            museums_vs_shopping: 'both',
                            intensity: 3
                          },
                          accessibility: {
                            womenOnly: false,
                            stepFree: false,
                            lowSensory: false,
                            visualAlerts: false,
                            liveCaptions: false
                          },
                          flexibleDates: {
                            enabled: false,
                            maxShift: 1
                          }
                        });
                      }
                      while (newList.length > value) {
                        newList.pop();
                      }
                      updateFormData({ travelersList: newList });
                    }}
                    max={10}
                    min={1}
                    step={1}
                    className="w-full"
                  />
                  <div className="text-center mt-2">
                    <span className="text-2xl">{formData.travelers}</span>
                  </div>
                </div>
              </div>

              <div>
                <Label>Budget (trip total)</Label>
                <div className="mt-2">
                  <Slider
                    value={[formData.budget]}
                    onValueChange={([value]) => updateFormData({ budget: value })}
                    max={500000}
                    min={25000}
                    step={5000}
                    className="w-full"
                  />
                  <div className="text-center mt-2">
                    <span className="text-2xl">₹{formData.budget.toLocaleString('en-IN')}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );

      case 2:
        return (
          <div className="space-y-6">
            {formData.travelersList.map((traveler, index) => (
              <Card key={traveler.id} className="p-4">
                <div className="space-y-4">
                  <div>
                    <Label>Name</Label>
                    <Input
                      value={traveler.name}
                      onChange={(e) => {
                        const newList = [...formData.travelersList];
                        newList[index].name = e.target.value;
                        updateFormData({ travelersList: newList });
                      }}
                      className="mt-1"
                    />
                  </div>

                  <div>
                    <Label>Quick preferences (choose one or both)</Label>
                    <div className="grid grid-cols-2 gap-4 mt-2">
                      <div>
                        <Label className="text-sm text-gray-600">Heritage vs Nightlife</Label>
                        <div className="flex gap-2 mt-1">
                          <Button
                            variant={traveler.preferences.heritage_vs_nightlife === 'heritage' ? 'default' : 'outline'}
                            size="sm"
                            onClick={() => {
                              const newList = [...formData.travelersList];
                              newList[index].preferences.heritage_vs_nightlife = 'heritage';
                              updateFormData({ travelersList: newList });
                            }}
                          >
                            Heritage
                          </Button>
                          <Button
                            variant={traveler.preferences.heritage_vs_nightlife === 'nightlife' ? 'default' : 'outline'}
                            size="sm"
                            onClick={() => {
                              const newList = [...formData.travelersList];
                              newList[index].preferences.heritage_vs_nightlife = 'nightlife';
                              updateFormData({ travelersList: newList });
                            }}
                          >
                            Nightlife
                          </Button>
                        </div>
                      </div>

                      <div>
                        <Label className="text-sm text-gray-600">Nature vs City</Label>
                        <div className="flex gap-2 mt-1">
                          <Button
                            variant={traveler.preferences.nature_vs_city === 'nature' ? 'default' : 'outline'}
                            size="sm"
                            onClick={() => {
                              const newList = [...formData.travelersList];
                              newList[index].preferences.nature_vs_city = 'nature';
                              updateFormData({ travelersList: newList });
                            }}
                          >
                            Nature
                          </Button>
                          <Button
                            variant={traveler.preferences.nature_vs_city === 'city' ? 'default' : 'outline'}
                            size="sm"
                            onClick={() => {
                              const newList = [...formData.travelersList];
                              newList[index].preferences.nature_vs_city = 'city';
                              updateFormData({ travelersList: newList });
                            }}
                          >
                            City
                          </Button>
                        </div>
                      </div>

                      <div>
                        <Label className="text-sm text-gray-600">Food-street vs Fine-dining</Label>
                        <div className="flex gap-2 mt-1">
                          <Button
                            variant={traveler.preferences.food_street_vs_fine === 'street' ? 'default' : 'outline'}
                            size="sm"
                            onClick={() => {
                              const newList = [...formData.travelersList];
                              newList[index].preferences.food_street_vs_fine = 'street';
                              updateFormData({ travelersList: newList });
                            }}
                          >
                            Food-street
                          </Button>
                          <Button
                            variant={traveler.preferences.food_street_vs_fine === 'fine' ? 'default' : 'outline'}
                            size="sm"
                            onClick={() => {
                              const newList = [...formData.travelersList];
                              newList[index].preferences.food_street_vs_fine = 'fine';
                              updateFormData({ travelersList: newList });
                            }}
                          >
                            Fine-dining
                          </Button>
                        </div>
                      </div>

                      <div>
                        <Label className="text-sm text-gray-600">Museums vs Shopping</Label>
                        <div className="flex gap-2 mt-1">
                          <Button
                            variant={traveler.preferences.museums_vs_shopping === 'museums' ? 'default' : 'outline'}
                            size="sm"
                            onClick={() => {
                              const newList = [...formData.travelersList];
                              newList[index].preferences.museums_vs_shopping = 'museums';
                              updateFormData({ travelersList: newList });
                            }}
                          >
                            Museums
                          </Button>
                          <Button
                            variant={traveler.preferences.museums_vs_shopping === 'shopping' ? 'default' : 'outline'}
                            size="sm"
                            onClick={() => {
                              const newList = [...formData.travelersList];
                              newList[index].preferences.museums_vs_shopping = 'shopping';
                              updateFormData({ travelersList: newList });
                            }}
                          >
                            Shopping
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div>
                    <Label>Intensity</Label>
                    <div className="mt-2">
                      <Slider
                        value={[traveler.preferences.intensity]}
                        onValueChange={([value]) => {
                          const newList = [...formData.travelersList];
                          newList[index].preferences.intensity = value;
                          updateFormData({ travelersList: newList });
                        }}
                        max={5}
                        min={1}
                        step={1}
                        className="w-full"
                      />
                      <div className="flex justify-between text-sm text-gray-500 mt-1">
                        <span>Relaxed</span>
                        <span>Balanced</span>
                        <span>Packed</span>
                      </div>
                    </div>
                  </div>

                  <div>
                    <Label>Accessibility (all OFF by default for demo)</Label>
                    <div className="grid grid-cols-2 gap-4 mt-2">
                      {Object.entries(traveler.accessibility).map(([key, value]) => (
                        <div key={key} className="flex items-center space-x-2">
                          <Switch
                            checked={value}
                            onCheckedChange={(checked) => {
                              const newList = [...formData.travelersList];
                              (newList[index].accessibility as any)[key] = checked;
                              updateFormData({ travelersList: newList });
                            }}
                          />
                          <Label className="text-sm capitalize">
                            {key.replace(/([A-Z])/g, ' $1').toLowerCase()}
                          </Label>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div>
                    <div className="flex items-center space-x-2 mb-2">
                      <Switch
                        checked={traveler.flexibleDates.enabled}
                        onCheckedChange={(checked) => {
                          const newList = [...formData.travelersList];
                          newList[index].flexibleDates.enabled = checked;
                          updateFormData({ travelersList: newList });
                        }}
                      />
                      <Label>Flexible dates</Label>
                    </div>
                    {traveler.flexibleDates.enabled && (
                      <div className="grid grid-cols-3 gap-2 text-sm text-gray-600">
                        <div>Earliest departure: 14 Mar 2024</div>
                        <div>Latest return: 23 Mar 2024</div>
                        <div>Max shift: ±1 day</div>
                      </div>
                    )}
                  </div>
                </div>
              </Card>
            ))}

            <div className="flex gap-2">
              <Button variant="outline" size="sm">Save travelers</Button>
              <Button variant="outline" size="sm">Apply to all</Button>
            </div>
          </div>
        );

      case 3:
        return (
          <div className="space-y-6">
            <div>
              <Label>From</Label>
              <Input placeholder="Select departure airport" className="mt-2" />
            </div>
            <div>
              <Label>To</Label>
              <Input value="Tokyo (TYO)" readOnly className="mt-2" />
            </div>
            <div>
              <Label>Cabin Class</Label>
              <div className="grid grid-cols-3 gap-4 mt-4">
                <Card 
                  className={`cursor-pointer p-4 ${formData.flightClass === 'economy' ? 'ring-2 ring-blue-500' : ''}`}
                  onClick={() => updateFormData({ flightClass: 'economy', flightCost: 60000 })}
                >
                  <div className="text-center">
                    <h3>Economy</h3>
                    <p className="text-2xl">₹30,000</p>
                    <p className="text-sm text-gray-600">per traveler</p>
                  </div>
                </Card>
                <Card 
                  className={`cursor-pointer p-4 ${formData.flightClass === 'premium' ? 'ring-2 ring-blue-500' : ''}`}
                  onClick={() => updateFormData({ flightClass: 'premium', flightCost: 90000 })}
                >
                  <div className="text-center">
                    <h3>Premium Economy</h3>
                    <p className="text-2xl">₹45,000</p>
                    <p className="text-sm text-gray-600">per traveler</p>
                  </div>
                </Card>
                <Card 
                  className={`cursor-pointer p-4 ${formData.flightClass === 'business' ? 'ring-2 ring-blue-500' : ''}`}
                  onClick={() => updateFormData({ flightClass: 'business', flightCost: 140000 })}
                >
                  <div className="text-center">
                    <h3>Business</h3>
                    <p className="text-2xl">₹70,000</p>
                    <p className="text-sm text-gray-600">per traveler</p>
                  </div>
                </Card>
              </div>
              <div className="mt-4 text-center">
                <p className="text-lg">Total flights: <span className="text-2xl">₹{formData.flightCost.toLocaleString('en-IN')}</span></p>
              </div>
              <div className="flex items-center space-x-2 mt-4">
                <Switch />
                <Label className="text-sm">Hold for 24h (can be turned on later)</Label>
              </div>
            </div>
          </div>
        );

      case 4:
        const nights = formData.startDate && formData.endDate 
          ? Math.ceil((formData.endDate.getTime() - formData.startDate.getTime()) / (1000 * 60 * 60 * 24))
          : 7;
        
        return (
          <div className="space-y-6">
            <div>
              <Label>Nights: {nights}</Label>
            </div>
            <div>
              <Label>Room Type</Label>
              <div className="grid grid-cols-3 gap-4 mt-4">
                <Card 
                  className={`cursor-pointer p-4 ${formData.roomType === 'standard' ? 'ring-2 ring-blue-500' : ''}`}
                  onClick={() => updateFormData({ roomType: 'standard', stayCost: 44996 })}
                >
                  <div className="text-center">
                    <h3>Standard Room</h3>
                    <p className="text-2xl">₹6,428</p>
                    <p className="text-sm text-gray-600">per night</p>
                  </div>
                </Card>
                <Card 
                  className={`cursor-pointer p-4 ${formData.roomType === 'deluxe' ? 'ring-2 ring-blue-500' : ''}`}
                  onClick={() => updateFormData({ roomType: 'deluxe', stayCost: 67210 })}
                >
                  <div className="text-center">
                    <h3>Deluxe</h3>
                    <p className="text-2xl">₹9,642</p>
                    <p className="text-sm text-gray-600">per night</p>
                    <p className="text-xs text-green-600">+₹3,214</p>
                  </div>
                </Card>
                <Card 
                  className={`cursor-pointer p-4 ${formData.roomType === 'suite' ? 'ring-2 ring-blue-500' : ''}`}
                  onClick={() => updateFormData({ roomType: 'suite', stayCost: 89992 })}
                >
                  <div className="text-center">
                    <h3>Suite</h3>
                    <p className="text-2xl">₹12,856</p>
                    <p className="text-sm text-gray-600">per night</p>
                    <p className="text-xs text-green-600">+₹6,428</p>
                  </div>
                </Card>
              </div>
              <div className="mt-4 text-center">
                <p className="text-lg">Total stay: <span className="text-2xl">₹{formData.stayCost.toLocaleString('en-IN')}</span></p>
              </div>
            </div>
          </div>
        );

      case 5:
        return (
          <div className="space-y-6">
            <div>
              <Label>Local Transportation</Label>
              <div className="mt-4 p-4 border rounded-lg">
                <div className="flex justify-between items-center">
                  <div>
                    <h3>Transit pass + transfers</h3>
                    <p className="text-sm text-gray-600">Metro + Suica pass, airport transfers</p>
                  </div>
                  <div className="text-2xl">₹{formData.transportCost.toLocaleString('en-IN')}</div>
                </div>
              </div>
            </div>
          </div>
        );

      case 6:
        return (
          <div className="space-y-6">
            <div className="flex justify-between items-center">
              <Label>Activities (Day 2 demo items)</Label>
              <Button variant="outline" size="sm">
                Edit later in Workflow Builder
              </Button>
            </div>
            
            <div className="space-y-3">
              {formData.activities.map((activity, index) => (
                <Card key={index} className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="text-sm text-gray-500">{activity.time}</div>
                      <div>
                        <h3 className="font-medium">{activity.name}</h3>
                        <div className="flex items-center gap-2 text-sm text-gray-600">
                          <Clock className="h-3 w-3" />
                          <span>{activity.duration}</span>
                          <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
                          <span>{activity.rating}</span>
                        </div>
                      </div>
                    </div>
                    <div className="text-lg">₹{activity.cost.toLocaleString('en-IN')}</div>
                  </div>
                </Card>
              ))}
            </div>
            
            <div className="mt-4 text-center">
              <p className="text-lg">Activities subtotal: <span className="text-2xl">₹{formData.activitiesCost.toLocaleString('en-IN')}</span></p>
            </div>
          </div>
        );

      case 7:
        const totals = calculateTotals();
        return (
          <div className="space-y-6">
            <div className="space-y-4">
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span className="text-xl">₹{totals.subtotal.toLocaleString('en-IN')}</span>
              </div>
              <div className="flex justify-between">
                <span>Taxes & Fees</span>
                <span className="text-xl">₹{totals.taxesAndFees.toLocaleString('en-IN')}</span>
              </div>
              <div className="border-t pt-2">
                <div className="flex justify-between items-center">
                  <span className="text-lg">Total</span>
                  <span className="text-2xl">₹{totals.total.toLocaleString('en-IN')}</span>
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span>Budget used</span>
                  <span className={totals.budgetUsed > 100 ? 'text-red-600' : 'text-green-600'}>
                    {totals.budgetUsed}% {totals.overBudget > 0 && `(₹${totals.overBudget.toLocaleString('en-IN')} over budget)`}
                  </span>
                </div>
              </div>
              <div className="text-sm text-gray-600">
                <p>Per person: ₹{Math.round(totals.total / formData.travelers).toLocaleString('en-IN')} ({formData.travelers} travelers)</p>
                <p>Budget set in planning: ₹{formData.budget.toLocaleString('en-IN')}</p>
              </div>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-2xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <Button variant="ghost" onClick={onBack}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back
          </Button>
          <div className="text-sm text-gray-500">
            Step {currentStep} of {totalSteps}
          </div>
        </div>

        {/* Progress Bar */}
        <div className="w-full bg-gray-200 rounded-full h-2 mb-8">
          <div 
            className="bg-indigo-600 h-2 rounded-full transition-all duration-300"
            style={{ width: `${(currentStep / totalSteps) * 100}%` }}
          />
        </div>

        {/* Content */}
        <Card>
          <CardHeader>
            <CardTitle>
              {currentStep === 1 && 'Basics'}
              {currentStep === 2 && 'Add travelers'}
              {currentStep === 3 && 'Flights'}
              {currentStep === 4 && 'Stay'}
              {currentStep === 5 && 'Local Transport'}
              {currentStep === 6 && 'Activities'}
              {currentStep === 7 && 'Review budget'}
            </CardTitle>
            <CardDescription>
              {currentStep === 1 && 'Destination, dates, travelers, and budget'}
              {currentStep === 2 && 'Configure preferences for each traveler'}
              {currentStep === 3 && 'Select flight options'}
              {currentStep === 4 && 'Choose accommodation type'}
              {currentStep === 5 && 'Local transportation needs'}
              {currentStep === 6 && 'Curated activities and experiences'}
              {currentStep === 7 && 'Final cost breakdown and budget review'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {renderStep()}
          </CardContent>
        </Card>

        {/* Navigation */}
        <div className="flex justify-between mt-8">
          <Button 
            variant="outline"
            onClick={() => setCurrentStep(prev => prev - 1)}
            disabled={currentStep === 1}
          >
            Previous
          </Button>
          
          <Button 
            onClick={handleNext}
            disabled={!canProceed()}
          >
            {currentStep === totalSteps ? 'Proceed to Book' : 'Continue'}
            <ArrowRight className="h-4 w-4 ml-2" />
          </Button>
        </div>
      </div>
    </div>
  );
}