import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Card } from '../ui/card';
import { Badge } from '../ui/badge';
import { Label } from '../ui/label';
import { Calendar } from '../ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '../ui/popover';
import { Slider } from '../ui/slider';
import { Switch } from '../ui/switch';
import { Separator } from '../ui/separator';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Plus, Minus, X, CalendarIcon, MapPin, Users, DollarSign, Settings2, ArrowLeft } from 'lucide-react';
import { format, addDays, addMonths, startOfMonth } from 'date-fns';
import { enUS, enGB } from 'date-fns/locale';
import { DateRange } from 'react-day-picker';
import { TripData, Traveler, TravelPreferences, TripSettings, TripLocation } from '../../types/TripData';
import { apiClient, CreateItineraryRequest } from '../../services/apiClient';
import { useCreateItinerary } from '../../state/query/hooks';
import { useAppStore } from '../../state/hooks';
import { UserProfileButton } from '../shared/UserProfileButton';
import { useFormSubmission } from '../../hooks/useFormSubmission';
import { GlobalNavigation } from '../shared/GlobalNavigation';
import { BreadcrumbNavigation } from '../shared/BreadcrumbNavigation';
import { AgentProgressModal } from '../agents/AgentProgressModal';

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
  const createItineraryMutation = useCreateItinerary();
  const { addTrip, setCurrentTrip } = useAppStore();
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedTripData, setGeneratedTripData] = useState<TripData | null>(null);
  
  // Form submission with debouncing
  const { isSubmitting, submit: submitForm, error: submissionError } = useFormSubmission({
    debounceMs: 2000, // 2 second debounce
    onError: (error) => {
      console.error('Form submission error:', error);
      alert(`Failed to create itinerary: ${error.message}`);
    }
  });
  
  const [startLocation, setStartLocation] = useState('Bengaluru, India');
  const [endLocation, setEndLocation] = useState('Bali, Indonesia');
  const [fromSuggestions, setFromSuggestions] = useState<string[]>([]);
  const [toSuggestions, setToSuggestions] = useState<string[]>([]);
  const [activeFromIndex, setActiveFromIndex] = useState<number>(-1);
  const [activeToIndex, setActiveToIndex] = useState<number>(-1);
  const fromAbort = useRef<AbortController | null>(null);
  const toAbort = useRef<AbortController | null>(null);
  const [fromFocused, setFromFocused] = useState<boolean>(false);
  const [toFocused, setToFocused] = useState<boolean>(false);
  const [isRoundTrip, setIsRoundTrip] = useState(true);
  const [dateRange, setDateRange] = useState<DateRange | undefined>(() => {
    const year = new Date().getFullYear();
    const from = new Date(year, 12, 25); // month is 0-indexed
    const to = new Date(year, 12, 28);
    return { from, to };
  });
  const [numberOfMonths, setNumberOfMonths] = useState(2);
  const [budget, setBudget] = useState(50000);
  const [currency, setCurrency] = useState('INR');
  const [flexibleDays, setFlexibleDays] = useState<number>(0);
  const [validationMessage, setValidationMessage] = useState<string>('');
  const userLocale = useMemo(() => (typeof navigator !== 'undefined' ? navigator.language : 'en-US'), []);
  const dayPickerLocale = useMemo(() => (userLocale?.toLowerCase()?.startsWith('en-gb') ? enGB : enUS), [userLocale]);
  
  // Track which preset is selected (if any) - moved from inside IIFE
  type PresetKey = 'budget' | 'mid' | 'luxury' | null;
  const [selectedPreset, setSelectedPreset] = useState<PresetKey>(null);

  // Handle window resize for calendar numberOfMonths
  useEffect(() => {
    const handleResize = () => {
      setNumberOfMonths(window.innerWidth < 768 ? 1 : 2);
    };
    
    // Set initial value
    handleResize();
    
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Initialize from URL if query params exist
  useEffect(() => {
    try {
      const params = new URLSearchParams(window.location.search);
      const fromParam = params.get('from');
      const toParam = params.get('to');
      if (fromParam && toParam) {
        const from = new Date(fromParam);
        const to = new Date(toParam);
        if (!isNaN(from.getTime()) && !isNaN(to.getTime())) {
          setDateRange({ from, to });
        }
      }
    } catch {}
  }, []);

  // Persist selection to URL
  useEffect(() => {
    if (!dateRange?.from || !dateRange?.to) return;
    try {
      const params = new URLSearchParams(window.location.search);
      params.set('from', dateRange.from.toISOString().slice(0,10));
      params.set('to', dateRange.to.toISOString().slice(0,10));
      const newUrl = `${window.location.pathname}?${params.toString()}`;
      window.history.replaceState({}, '', newUrl);
    } catch {}
  }, [dateRange?.from, dateRange?.to]);

  // Debounced place lookup (OpenStreetMap Nominatim)
  const formatPlaceLabel = (d: any): string => {
    const a = d?.address || {};
    const locality = a.city || a.town || a.village || a.municipality || a.suburb || a.county || d?.name;
    const state = a.state || a.region || a.state_district;
    const country = a.country || (a.country_code ? a.country_code.toUpperCase() : undefined);
    if (locality && state && country) return `${locality}, ${state}, ${country}`;
    if (locality && country) return `${locality}, ${country}`;
    if (state && country) return `${state}, ${country}`;
    return country || d?.display_name || '';
  };

  useEffect(() => {
    if (!fromFocused || !startLocation || startLocation.length < 2) {
      setFromSuggestions([]);
      return;
    }
    const handle = setTimeout(async () => {
      try {
        if (fromAbort.current) fromAbort.current.abort();
        fromAbort.current = new AbortController();
        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&q=${encodeURIComponent(startLocation)}&limit=5`, {
          signal: fromAbort.current.signal,
          headers: { 'Accept-Language': userLocale || 'en' }
        });
        const data = await res.json();
        const names: string[] = (data || []).map((d: any) => formatPlaceLabel(d));
        const unique = Array.from(new Set(names)).slice(0, 5);
        setFromSuggestions(unique);
        setActiveFromIndex(-1);
      } catch {}
    }, 300);
    return () => clearTimeout(handle);
  }, [startLocation, userLocale, fromFocused]);

  useEffect(() => {
    if (!toFocused || !endLocation || endLocation.length < 2) {
      setToSuggestions([]);
      return;
    }
    const handle = setTimeout(async () => {
      try {
        if (toAbort.current) toAbort.current.abort();
        toAbort.current = new AbortController();
        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&q=${encodeURIComponent(endLocation)}&limit=5`, {
          signal: toAbort.current.signal,
          headers: { 'Accept-Language': userLocale || 'en' }
        });
        const data = await res.json();
        const names: string[] = (data || []).map((d: any) => formatPlaceLabel(d));
        const unique = Array.from(new Set(names)).slice(0, 5);
        setToSuggestions(unique);
        setActiveToIndex(-1);
      } catch {}
    }, 300);
    return () => clearTimeout(handle);
  }, [endLocation, userLocale, toFocused]);
  
  const [travelers, setTravelers] = useState<Traveler[]>([
    {
      id: '1',
      name: 'Ishan',
      email: '',
      age: 27,
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
           travelers.every(t => t.name.trim()) &&
           budget > 0;
  };

  const handleSubmit = async () => {
    if (!isFormValid()) return;

    // Use the debounced form submission
    await submitForm(async () => {
      setIsGenerating(true);

      try {
        // Create the API request
        const createRequest: CreateItineraryRequest = {
          destination: endLocation,
          startDate: dateRange!.from!.toISOString().split('T')[0],
          endDate: dateRange!.to!.toISOString().split('T')[0],
          party: {
            adults: travelers[0] ? 1 : 0, // todo should be length of travellers array?
            children: 0,
            infants: 0,
            rooms: 1
          },
          budgetTier: budget <= 2000 ? 'economy' : budget <= 5000 ? 'mid-range' : 'luxury', // todo take input from ui component
          interests: Object.entries(preferences)
            .filter(([_, value]) => (value as number) > 50)
            .map(([key]) => key as string),
          constraints: Object.entries(settings)
            .filter(([_, value]) => value as boolean)
            .map(([key]) => key as string),
          language: 'en'
        };

        // Call the backend API via mutation
        const response = await createItineraryMutation.mutateAsync(createRequest);

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
            start: dateRange?.from?.toISOString() || new Date().toISOString(),
            end: dateRange?.to?.toISOString() || new Date().toISOString()
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
          status: 'planning', // todo make this centralised using enums or types at all places
          createdAt: response.createdAt,
          updatedAt: response.updatedAt,
          isPublic: response.isPublic,
          // Add computed properties expected by components
          destination: endLocation,
          partySize: travelers.length,
          dietaryRestrictions: travelers.flatMap(t => t.preferences?.dietaryRestrictions || []),
          walkingTolerance: 3, // Default moderate walking
          pace: 3, // Default moderate pace
          stayType: 'Hotel',
          transport: 'Public',
          themes: Object.entries(preferences)
            .filter(([_, value]) => (value as number) > 50)
            .map(([key]) => key.charAt(0).toUpperCase() + key.slice(1))
        };

        addTrip(tripData);
        setCurrentTrip(tripData);
        setGeneratedTripData(tripData);
        
        // If the itinerary is still generating, show progress modal
        if ((response.status as string) === 'planning') {
          // The itinerary is being generated asynchronously
          // Keep the progress modal open and don't set isGenerating to false
          console.log('Itinerary is being generated asynchronously, showing progress modal');
        } else {
          // The itinerary is complete
          console.log('Itinerary is complete, proceeding to next screen');
          setIsGenerating(false);
          onComplete(tripData);
        }

        return tripData;
      } catch (error) {
        setIsGenerating(false);
        throw error; // Re-throw to be handled by the form submission hook
      }
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      {/* Show progress modal if generating */}
      {isGenerating && generatedTripData ? (
        <AgentProgressModal
          tripData={generatedTripData}
          onComplete={() => {
            console.log('Itinerary generation completed!');
            setIsGenerating(false);
            onComplete(generatedTripData);
          }}
          onCancel={() => {
            setIsGenerating(false);
            onBack();
          }}
        />
      ) : (
        <>
          {/* Header - Consistent with My Trips */}
      <div className="bg-white border-b">
        <div className="max-w-6xl mx-auto px-3 sm:px-4 py-3 sm:py-4">
          <div className="flex items-center justify-between gap-3 sm:gap-4">
            {/* Left Section */}
            <div className="flex items-center gap-3 sm:gap-4">
              <Button variant="ghost" size="sm" onClick={onBack} className="p-2 sm:p-2">
                <ArrowLeft className="h-4 w-4 sm:mr-2" />
                <span className="hidden sm:inline">Back</span>
              </Button>
              <div className="min-w-0 flex-1">
                <h1 className="text-xl sm:text-2xl font-semibold truncate">Plan Your Trip</h1>
              </div>
            </div>
            
            {/* Right Section - User Profile Only */}
            <div className="flex items-center">
              <UserProfileButton />
            </div>
          </div>
        </div>
      </div>


      <div className="p-4">
        <div className="max-w-6xl mx-auto">
          {/* Description */}
          <div className="text-center mb-8">
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
                <div className="relative">
                  <Label>From</Label>
                  <Input 
                    placeholder="Bengaluru, India"
                    value={startLocation}
                    onChange={(e) => setStartLocation(e.target.value)}
                    onFocus={() => setFromFocused(true)}
                    onBlur={() => setTimeout(()=>{ setFromFocused(false); setFromSuggestions([]); }, 150)}
                    onKeyDown={(e) => {
                      if (fromSuggestions.length === 0) return;
                      if (e.key === 'ArrowDown') {
                        e.preventDefault();
                        setActiveFromIndex((i) => Math.min(i + 1, fromSuggestions.length - 1));
                      } else if (e.key === 'ArrowUp') {
                        e.preventDefault();
                        setActiveFromIndex((i) => Math.max(i - 1, 0));
                      } else if (e.key === 'Enter' && activeFromIndex >= 0) {
                        e.preventDefault();
                        setStartLocation(fromSuggestions[activeFromIndex]);
                        setFromSuggestions([]);
                      } else if (e.key === 'Escape') {
                        setFromSuggestions([]);
                      }
                    }}
                  />
                  {fromFocused && fromSuggestions.length > 0 && (
                    <div className="absolute z-50 mt-1 w-full bg-white border rounded-md shadow-lg max-h-60 overflow-auto">
                      {fromSuggestions.map((s, idx) => (
                        <button
                          type="button"
                          key={`${s}-${idx}`}
                          className={`w-full text-left px-3 py-2 text-sm hover:bg-gray-50 ${idx===activeFromIndex? 'bg-gray-100':''}`}
                          onMouseDown={(e) => {
                            e.preventDefault();
                            setStartLocation(s);
                            setFromSuggestions([]);
                          }}
                        >
                          {s}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                
                <div className="relative">
                  <Label>To</Label>
                  <Input 
                    placeholder="Bali, Indonesia"
                    value={endLocation}
                    onChange={(e) => setEndLocation(e.target.value)}
                    onFocus={() => setToFocused(true)}
                    onBlur={() => setTimeout(()=>{ setToFocused(false); setToSuggestions([]); }, 150)}
                    onKeyDown={(e) => {
                      if (toSuggestions.length === 0) return;
                      if (e.key === 'ArrowDown') {
                        e.preventDefault();
                        setActiveToIndex((i) => Math.min(i + 1, toSuggestions.length - 1));
                      } else if (e.key === 'ArrowUp') {
                        e.preventDefault();
                        setActiveToIndex((i) => Math.max(i - 1, 0));
                      } else if (e.key === 'Enter' && activeToIndex >= 0) {
                        e.preventDefault();
                        setEndLocation(toSuggestions[activeToIndex]);
                        setToSuggestions([]);
                      } else if (e.key === 'Escape') {
                        setToSuggestions([]);
                      }
                    }}
                  />
                  {toFocused && toSuggestions.length > 0 && (
                    <div className="absolute z-50 mt-1 w-full bg-white border rounded-md shadow-lg max-h-60 overflow-auto">
                      {toSuggestions.map((s, idx) => (
                        <button
                          type="button"
                          key={`${s}-${idx}`}
                          className={`w-full text-left px-3 py-2 text-sm hover:bg-gray-50 ${idx===activeToIndex? 'bg-gray-100':''}`}
                          onMouseDown={(e) => {
                            e.preventDefault();
                            setEndLocation(s);
                            setToSuggestions([]);
                          }}
                        >
                          {s}
                        </button>
                      ))}
                    </div>
                  )}
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
                    <PopoverContent className="w-auto p-0 sm:w-auto" align="start">
                      <div className="p-1 sm:p-3">
                      <Calendar
                        initialFocus
                        mode="range"
                        defaultMonth={dateRange?.from}
                        selected={dateRange}
                          onSelect={(range) => {
                            setValidationMessage('');
                            const today = new Date();
                            const clamp = (d: Date | undefined) => d && d < new Date(today.getFullYear(), today.getMonth(), today.getDate()) ? today : d;
                            const from = clamp(range?.from);
                            const to = clamp(range?.to);
                            const nextRange = { from: from, to: to } as any;
                            setDateRange(nextRange);
                            if (from && to) {
                              const nights = Math.max(0, Math.ceil((to.getTime() - from.getTime()) / (1000 * 60 * 60 * 24)));
                              if (nights < 2) setValidationMessage('Minimum trip length is 2 nights.');
                              else if (nights > 7) setValidationMessage('Maximum trip length is 7 nights.');
                            }
                          }}
                        numberOfMonths={numberOfMonths}
                          locale={dayPickerLocale}
                          classNames={{
                            day_outside: "invisible",
                            day_today: "bg-primary/10 text-primary",
                            // Ensure cells don't highlight when only an outside day is selected
                            cell:
                              "relative p-0 text-center text-sm focus-within:relative focus-within:z-20 [&:has([aria-selected]:not(.rdp-day_outside))]:bg-accent [&:has(>.day-range-end)]:rounded-r-md first:[&:has([aria-selected]:not(.rdp-day_outside))]:rounded-l-md last:[&:has([aria-selected]:not(.rdp-day_outside))]:rounded-r-md",
                          }}
                          disabled={{ before: new Date(new Date().getFullYear(), new Date().getMonth(), new Date().getDate()) }}
                        />

                        <div className="mt-2 sm:mt-3">
                          <div className="text-xs text-gray-600 mb-1 sm:mb-2">Quick durations</div>
                          <Select onValueChange={(value) => {
                            const nights = parseInt(value);
                            const base = dateRange?.from && dateRange.from >= new Date() ? dateRange.from : new Date();
                            const from = new Date(base.getFullYear(), base.getMonth(), base.getDate());
                            const to = addDays(from, nights);
                            setDateRange({ from, to });
                            setValidationMessage('');
                          }}>
                            <SelectTrigger className="h-8 text-xs sm:text-sm">
                              <SelectValue placeholder="Select duration" />
                            </SelectTrigger>
                            <SelectContent>
                              {[3,5,7,10,14].map((n) => (
                                <SelectItem key={n} value={n.toString()}>
                                  {n} nights
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>


                        <div className="mt-2 sm:mt-4">
                          <div className="flex items-center justify-between">
                            <Label className="text-sm">Flexible dates</Label>
                            <div className="flex items-center gap-1 sm:gap-2">
                              <Button size="sm" variant={flexibleDays===0? 'secondary':'outline'} className="text-xs sm:text-sm px-2 sm:px-3" onClick={()=>setFlexibleDays(0)}>±0</Button>
                              <Button size="sm" variant={flexibleDays===1? 'secondary':'outline'} className="text-xs sm:text-sm px-2 sm:px-3" onClick={()=>setFlexibleDays(1)}>±1</Button>
                              <Button size="sm" variant={flexibleDays===3? 'secondary':'outline'} className="text-xs sm:text-sm px-2 sm:px-3" onClick={()=>setFlexibleDays(3)}>±3</Button>
                              <Button size="sm" variant={flexibleDays===7? 'secondary':'outline'} className="text-xs sm:text-sm px-2 sm:px-3" onClick={()=>setFlexibleDays(7)}>±7</Button>
                            </div>
                          </div>
                          {flexibleDays>0 && (
                            <p className="mt-1 sm:mt-2 text-xs text-gray-600">We will try nearby dates within ±{flexibleDays} days.</p>
                          )}
                        </div>

                        {validationMessage && (
                          <div className="mt-2 sm:mt-3 text-sm text-red-600">{validationMessage}</div>
                        )}
                      </div>
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
              
              <div className="space-y-3">
                  <Label>Total Budget</Label>
                {(() => {
                  const ranges: Record<string, { min: number; max: number }>= {
                    USD: { min: 500, max: 10000 },
                    EUR: { min: 500, max: 9000 },
                    GBP: { min: 400, max: 8000 },
                    JPY: { min: 50000, max: 1500000 },
                    INR: { min: 30000, max: 300000 },
                  };
                  const { min, max } = ranges[currency] || ranges.INR;
                  const clamp = (v: number) => Math.min(max, Math.max(min, v));
                  const value = clamp(budget);
                  if (value !== budget) setBudget(value);
                  return (
                    <>
                      {/* Preset buttons - three vertical cards side by side */}
                      <div className="grid grid-cols-3 gap-3">
                        {(() => {
                          const presets: Record<string, { key: PresetKey; label: string; value: number }[]> = {
                            USD: [
                              { key: 'budget', label: '💸 Budget', value: 800 },
                              { key: 'mid', label: '💼 Mid-range', value: 2000 },
                              { key: 'luxury', label: '✨ Luxury', value: 5000 },
                            ],
                            EUR: [
                              { key: 'budget', label: '💸 Budget', value: 750 },
                              { key: 'mid', label: '💼 Mid-range', value: 1800 },
                              { key: 'luxury', label: '✨ Luxury', value: 4500 },
                            ],
                            GBP: [
                              { key: 'budget', label: '💸 Budget', value: 650 },
                              { key: 'mid', label: '💼 Mid-range', value: 1600 },
                              { key: 'luxury', label: '✨ Luxury', value: 4000 },
                            ],
                            JPY: [
                              { key: 'budget', label: '💸 Budget', value: 120000 },
                              { key: 'mid', label: '💼 Mid-range', value: 250000 },
                              { key: 'luxury', label: '✨ Luxury', value: 600000 },
                            ],
                            INR: [
                              { key: 'budget', label: '💸 Budget', value: 120000 },
                              { key: 'mid', label: '💼 Mid-range', value: 250000 },
                              { key: 'luxury', label: '✨ Luxury', value: 600000 },
                            ],
                          };
                          return (presets[currency] || presets.USD).map(p => (
                            <Button
                              key={p.key}
                              size="lg"
                              variant={selectedPreset === p.key ? 'default' : 'outline'}
                              className="w-full py-8 text-lg font-semibold rounded-xl shadow-sm hover:shadow md:text-xl"
                              style={{ aspectRatio: '2 / 3' }}
                              onClick={() => {
                                setBudget(clamp(p.value));
                                setSelectedPreset(p.key);
                              }}
                            >
                              {p.label}
                            </Button>
                          ));
                        })()}
                      </div>

                      <div className="flex flex-col sm:flex-row sm:items-center gap-3">
                    <select 
                      value={currency} 
                      onChange={(e) => setCurrency(e.target.value)}
                      className="px-3 py-2 border rounded-md"
                    >
                      <option value="INR">INR</option>
                      <option value="USD">USD</option>
                      <option value="EUR">EUR</option>
                      <option value="GBP">GBP</option>
                      <option value="JPY">JPY</option>
                    </select>
                        <Slider
                          value={[value]}
                          min={min}
                          max={max}
                          step={currency === 'JPY' ? 10000 : 50}
                          onValueChange={([v]) => {
                            setBudget(clamp(v));
                            // Unset preset when user drags slider
                            // Only clear if it actually deviates from a preset value
                            // to prevent flicker during initial selection
                            setSelectedPreset(null);
                          }}
                          className="w-full"
                          aria-label="Budget range"
                        />
                        <Input
                          type="number"
                          className="w-36"
                          value={value}
                          onChange={(e) => {
                            const n = Number(e.target.value);
                            if (Number.isFinite(n)) {
                              setBudget(clamp(n));
                              setSelectedPreset(null);
                            }
                          }}
                        />
                  </div>
                      <div className="flex justify-between text-xs text-gray-500">
                        <span>{currency} {min.toLocaleString()}</span>
                        <span>{currency} {max.toLocaleString()}</span>
                </div>
                <div className="text-xs text-gray-600">Note: Unrealistic budgets may lead to poor itinerary quality.</div>
                <div className="text-xs text-gray-600">Please select from above 3 options if unsure.</div>
                    </>
                  );
                })()}
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

            {/* Summary moved to Generating page */}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex justify-between items-center mt-8 pt-6 border-t">
          <Button variant="outline" onClick={onBack}>
            Back
          </Button>
          
          <Button 
            onClick={handleSubmit} 
            disabled={!isFormValid() || isSubmitting || isGenerating}
            size="lg"
            className="px-8"
          >
            {isSubmitting || isGenerating ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                {isGenerating ? 'Creating Itinerary...' : 'Processing...'}
              </>
            ) : (
              'Create My Itinerary'
            )}
          </Button>
        </div>
        
        {/* Show error message if submission failed */}
        {submissionError && (
          <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-md">
            <p className="text-sm text-red-600">
              {submissionError.message}
            </p>
          </div>
        )}
        </div>
      </div>
        </>
      )}
    </div>
  );
}



