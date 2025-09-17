import React, { useState } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Card } from './ui/card';
import { Badge } from './ui/badge';
import { RadioGroup, RadioGroupItem } from './ui/radio-group';
import { Label } from './ui/label';
import { Calendar } from './ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from './ui/popover';
import { Slider } from './ui/slider';
import { Switch } from './ui/switch';
import { Separator } from './ui/separator';
import { Plus, Minus, X, CalendarIcon, MapPin, Users, DollarSign, Settings2 } from 'lucide-react';
import { format, addDays } from 'date-fns';
import { DateRange } from 'react-day-picker';
import { TripData, Traveler, TravelPreferences, TripSettings, TripLocation } from '../types/TripData';
import { apiClient, CreateItineraryRequest } from '../services/apiClient';
import exampleImage from 'figma:asset/3a9239d730c70fddfaa34557264375013dd112e1.png';

interface SimplifiedTripWizardProps {
  onComplete: (tripData: TripData) => void;
  onBack: () => void;
}

const PREFERENCE_CONTROLS = [
  { key: 'heritage' as keyof TravelPreferences, label: 'Heritage Sites', color: 'bg-amber-500', icon: '🏛️' },
  { key: 'nightlife' as keyof TravelPreferences, label: 'Nightlife', color: 'bg-purple-500', icon: '🌃' },
  { key: 'adventure' as keyof TravelPreferences, label: 'Adventure', color: 'bg-red-500', icon: '🏔️' },
  { key: 'relaxation' as keyof TravelPreferences, label: 'Relaxation', color: 'bg-blue-500', icon: '🧘' },
  { key: 'culture' as keyof TravelPreferences, label: 'Culture', color: 'bg-green-500', icon: '🎭' },
  { key: 'nature' as keyof TravelPreferences, label: 'Nature', color: 'bg-teal-500', icon: '🌿' },
  { key: 'shopping' as keyof TravelPreferences, label: 'Shopping', color: 'bg-pink-500', icon: '🛍️' },
  { key: 'cuisine' as keyof TravelPreferences, label: 'Cuisine', color: 'bg-orange-500', icon: '🍽️' },
];

export function SimplifiedTripWizard({ onComplete, onBack }: SimplifiedTripWizardProps) {
  const [startLocation, setStartLocation] = useState('');
  const [endLocation, setEndLocation] = useState('');
  const [isRoundTrip, setIsRoundTrip] = useState(true);
  const [dateRange, setDateRange] = useState<DateRange | undefined>();
  const [budget, setBudget] = useState(5000);
  const [currency, setCurrency] = useState('USD');
  
  const [travelers, setTravelers] = useState<Traveler[]>([
    {
      id: '1',
      name: '',
      email: '',
      age: 25,
      preferences: {
        dietaryRestrictions: [],
        mobilityNeeds: [],
        interests: []
      }
    }
  ]);

  const [preferences, setPreferences] = useState<TravelPreferences>({
    heritage: 30,
    nightlife: 20,
    adventure: 40,
    relaxation: 60,
    culture: 50,
    nature: 70,
    shopping: 10,
    cuisine: 80,
    photography: 40,
    spiritual: 20
  });

  const [settings, setSettings] = useState<TripSettings>({
    womenFriendly: false,
    petFriendly: false,
    veganOnly: false,
    wheelchairAccessible: false,
    budgetFriendly: true,
    luxuryOnly: false,
    familyFriendly: false,
    soloTravelSafe: false
  });

  const addTraveler = () => {
    const newTraveler: Traveler = {
      id: Date.now().toString(),
      name: '',
      email: '',
      age: 25,
      preferences: {
        dietaryRestrictions: [],
        mobilityNeeds: [],
        interests: []
      }
    };
    setTravelers([...travelers, newTraveler]);
  };

  const removeTraveler = (id: string) => {
    setTravelers(travelers.filter(t => t.id !== id));
  };

  const updateTraveler = (id: string, updates: Partial<Traveler>) => {
    setTravelers(travelers.map(t => 
      t.id === id ? { ...t, ...updates } : t
    ));
  };

  const updatePreference = (key: keyof TravelPreferences, value: number) => {
    setPreferences(prev => ({ ...prev, [key]: value }));
  };

  const updateSetting = (key: keyof TripSettings, value: boolean) => {
    setSettings(prev => ({ ...prev, [key]: value }));
  };

  const isFormValid = () => {
    return startLocation.trim() && 
           endLocation.trim() && 
           dateRange?.from && 
           dateRange?.to && 
           travelers.every(t => t.name.trim() && t.email.trim()) &&
           budget > 0;
  };

  const handleSubmit = async () => {
    if (!isFormValid()) return;

    try {
      // Create the API request
      const createRequest: CreateItineraryRequest = {
        destination: endLocation,
        startDate: dateRange!.from!.toISOString().split('T')[0],
        endDate: dateRange!.to!.toISOString().split('T')[0],
        party: {
          adults: travelers[0] ? 1 : 0,
          children: 0,
          infants: 0,
          rooms: 1
        },
        budgetTier: budget <= 2000 ? 'economy' : budget <= 5000 ? 'mid-range' : 'luxury',
        interests: Object.entries(preferences)
          .filter(([_, value]) => value > 50)
          .map(([key, _]) => key),
        constraints: Object.entries(settings)
          .filter(([_, value]) => value)
          .map(([key, _]) => key),
        language: 'en'
      };

      // Call the backend API
      const response = await apiClient.createItinerary(createRequest);

      // Convert API response to TripData format for frontend
      const startLocationData: TripLocation = {
        id: '1',
        name: startLocation,
        country: 'Unknown',
        city: startLocation,
        coordinates: { lat: 0, lng: 0 },
        timezone: 'UTC',
        currency: currency,
        exchangeRate: 1
      };

      const endLocationData: TripLocation = {
        id: '2',
        name: endLocation,
        country: 'Unknown', 
        city: endLocation,
        coordinates: { lat: 0, lng: 0 },
        timezone: 'UTC',
        currency: currency,
        exchangeRate: 1
      };

      const tripData: TripData = {
        id: response.id,
        startLocation: startLocationData,
        endLocation: endLocationData,
        isRoundTrip,
        dates: {
          start: response.startDate,
          end: response.endDate
        },
        travelers,
        leadTraveler: travelers[0],
        budget: {
          total: budget,
          currency,
          breakdown: {
            accommodation: Math.round(budget * 0.4),
            food: Math.round(budget * 0.25),
            transport: Math.round(budget * 0.15),
            activities: Math.round(budget * 0.15),
            shopping: Math.round(budget * 0.03),
            emergency: Math.round(budget * 0.02)
          }
        },
        preferences,
        settings,
        status: response.status,
        createdAt: response.createdAt,
        updatedAt: response.updatedAt,
        isPublic: response.isPublic
      };

      onComplete(tripData);
    } catch (error) {
      console.error('Failed to create itinerary:', error);
      // Handle error - could show a toast notification
      alert('Failed to create itinerary. Please try again.');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold mb-2">Plan Your Perfect Trip</h1>
          <p className="text-gray-600">Tell us about your travel preferences and we'll create an amazing itinerary</p>
        </div>

        <div className="grid lg:grid-cols-2 gap-8">
          {/* Left Side - Trip Details & Travelers */}
          <div className="space-y-6">
            {/* Basic Trip Info */}
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <MapPin className="w-5 h-5 text-blue-600" />
                <h2 className="text-xl font-semibold">Trip Details</h2>
              </div>
              
              <div className="space-y-4">
                <div>
                  <Label>From</Label>
                  <Input 
                    placeholder="New York, NY" 
                    value={startLocation}
                    onChange={(e) => setStartLocation(e.target.value)}
                  />
                </div>
                
                <div>
                  <Label>To</Label>
                  <Input 
                    placeholder="Paris, France" 
                    value={endLocation}
                    onChange={(e) => setEndLocation(e.target.value)}
                  />
                </div>

                <div className="flex items-center space-x-2">
                  <Switch 
                    id="round-trip" 
                    checked={isRoundTrip}
                    onCheckedChange={setIsRoundTrip}
                  />
                  <Label htmlFor="round-trip">Round trip</Label>
                </div>

                <div>
                  <Label>Travel Dates</Label>
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button variant="outline" className="w-full justify-start text-left font-normal">
                        <CalendarIcon className="mr-2 h-4 w-4" />
                        {dateRange?.from ? (
                          dateRange.to ? (
                            <>
                              {format(dateRange.from, "LLL dd, y")} -{" "}
                              {format(dateRange.to, "LLL dd, y")}
                            </>
                          ) : (
                            format(dateRange.from, "LLL dd, y")
                          )
                        ) : (
                          <span>Pick your travel dates</span>
                        )}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <Calendar
                        initialFocus
                        mode="range"
                        defaultMonth={dateRange?.from}
                        selected={dateRange}
                        onSelect={setDateRange}
                        numberOfMonths={2}
                      />
                    </PopoverContent>
                  </Popover>
                </div>
              </div>
            </Card>

            {/* Budget */}
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <DollarSign className="w-5 h-5 text-green-600" />
                <h2 className="text-xl font-semibold">Budget</h2>
              </div>
              
              <div className="space-y-4">
                <div>
                  <Label>Total Budget</Label>
                  <div className="flex gap-2">
                    <Input 
                      type="number" 
                      value={budget}
                      onChange={(e) => setBudget(Number(e.target.value))}
                      className="flex-1"
                    />
                    <select 
                      value={currency} 
                      onChange={(e) => setCurrency(e.target.value)}
                      className="px-3 py-2 border rounded-md"
                    >
                      <option value="USD">USD</option>
                      <option value="EUR">EUR</option>
                      <option value="GBP">GBP</option>
                      <option value="JPY">JPY</option>
                    </select>
                  </div>
                </div>
                
                <div className="grid grid-cols-3 gap-2 text-sm">
                  <div className="text-center p-2 bg-blue-50 rounded">
                    <div className="font-medium">{currency} {Math.round(budget * 0.4)}</div>
                    <div className="text-gray-600">Hotels</div>
                  </div>
                  <div className="text-center p-2 bg-green-50 rounded">
                    <div className="font-medium">{currency} {Math.round(budget * 0.25)}</div>
                    <div className="text-gray-600">Food</div>
                  </div>
                  <div className="text-center p-2 bg-purple-50 rounded">
                    <div className="font-medium">{currency} {Math.round(budget * 0.35)}</div>
                    <div className="text-gray-600">Other</div>
                  </div>
                </div>
              </div>
            </Card>

            {/* Travelers */}
            <Card className="p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Users className="w-5 h-5 text-purple-600" />
                  <h2 className="text-xl font-semibold">Travelers</h2>
                </div>
                <Button onClick={addTraveler} size="sm" variant="outline">
                  <Plus className="w-4 h-4 mr-1" />
                  Add Traveler
                </Button>
              </div>
              
              <div className="space-y-4">
                {travelers.map((traveler, index) => (
                  <div key={traveler.id} className="p-4 border rounded-lg relative">
                    {travelers.length > 1 && (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="absolute top-2 right-2 h-6 w-6 p-0"
                        onClick={() => removeTraveler(traveler.id)}
                      >
                        <X className="w-4 h-4" />
                      </Button>
                    )}
                    
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <Label>Name</Label>
                        <Input 
                          placeholder="Full name"
                          value={traveler.name}
                          onChange={(e) => updateTraveler(traveler.id, { name: e.target.value })}
                        />
                      </div>
                      <div>
                        <Label>Age</Label>
                        <Input 
                          type="number"
                          value={traveler.age}
                          onChange={(e) => updateTraveler(traveler.id, { age: Number(e.target.value) })}
                        />
                      </div>
                      <div className="col-span-2">
                        <Label>Email</Label>
                        <Input 
                          type="email"
                          placeholder="email@example.com"
                          value={traveler.email}
                          onChange={(e) => updateTraveler(traveler.id, { email: e.target.value })}
                        />
                      </div>
                    </div>
                    
                    {index === 0 && (
                      <Badge variant="secondary" className="mt-2">Lead Traveler</Badge>
                    )}
                  </div>
                ))}
              </div>
            </Card>
          </div>

          {/* Right Side - Preferences (Music Mixer Style) */}
          <div className="space-y-6">
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-6">
                <Settings2 className="w-5 h-5 text-indigo-600" />
                <h2 className="text-xl font-semibold">Trip Preferences</h2>
              </div>

              {/* Preference Sliders */}
              <div className="space-y-6 mb-8">
                <h3 className="font-medium text-gray-700">How much do you want to experience?</h3>
                
                <div className="grid grid-cols-2 gap-6">
                  {PREFERENCE_CONTROLS.map((control) => (
                    <div key={control.key} className="space-y-3">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="text-lg">{control.icon}</span>
                          <Label className="text-sm font-medium">{control.label}</Label>
                        </div>
                        <Badge variant="outline" className="text-xs">
                          {preferences[control.key]}%
                        </Badge>
                      </div>
                      
                      <div className="relative">
                        <Slider
                          value={[preferences[control.key]]}
                          onValueChange={([value]) => updatePreference(control.key, value)}
                          max={100}
                          step={10}
                          className="w-full"
                        />
                        <div className="flex justify-between text-xs text-gray-400 mt-1">
                          <span>None</span>
                          <span>Lots</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <Separator className="my-6" />

              {/* Settings Toggles */}
              <div className="space-y-4">
                <h3 className="font-medium text-gray-700">Special Requirements</h3>
                
                <div className="grid grid-cols-2 gap-4">
                  {Object.entries(settings).map(([key, value]) => (
                    <div key={key} className="flex items-center justify-between">
                      <Label className="text-sm">
                        {key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}
                      </Label>
                      <Switch
                        checked={value}
                        onCheckedChange={(checked) => updateSetting(key as keyof TripSettings, checked)}
                      />
                    </div>
                  ))}
                </div>
              </div>
            </Card>

            {/* Preview */}
            <Card className="p-6 bg-gradient-to-r from-blue-50 to-purple-50">
              <h3 className="font-semibold mb-3">Trip Summary</h3>
              <div className="space-y-2 text-sm">
                <p><span className="font-medium">Route:</span> {startLocation || '...'} → {endLocation || '...'}</p>
                <p><span className="font-medium">Duration:</span> {
                  dateRange?.from && dateRange?.to 
                    ? `${Math.ceil((dateRange.to.getTime() - dateRange.from.getTime()) / (1000 * 60 * 60 * 24))} days`
                    : '...'
                }</p>
                <p><span className="font-medium">Travelers:</span> {travelers.length}</p>
                <p><span className="font-medium">Budget:</span> {currency} {budget.toLocaleString()}</p>
                <p><span className="font-medium">Top Interests:</span> {
                  Object.entries(preferences)
                    .sort(([,a], [,b]) => b - a)
                    .slice(0, 3)
                    .map(([key]) => key)
                    .join(', ')
                }</p>
              </div>
            </Card>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex justify-between items-center mt-8 pt-6 border-t">
          <Button variant="outline" onClick={onBack}>
            Back
          </Button>
          
          <Button 
            onClick={handleSubmit} 
            disabled={!isFormValid()}
            size="lg"
            className="px-8"
          >
            Create My Itinerary
          </Button>
        </div>
      </div>
    </div>
  );
}