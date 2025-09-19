import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogTrigger } from './ui/dialog';
import { Card, CardContent, CardHeader } from './ui/card';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Progress } from './ui/progress';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription, SheetTrigger } from './ui/sheet';
import { Separator } from './ui/separator';
import { Checkbox } from './ui/checkbox';
import { Textarea } from './ui/textarea';
import { ScrollArea } from './ui/scroll-area';
import { Avatar, AvatarFallback } from './ui/avatar';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from './ui/dropdown-menu';
import { PhotoSpotsPanel, MustTryFoodsPanel, CostEstimatorPanel } from './stippl/ToolsPanels';
import { WorkflowBuilder } from './WorkflowBuilder';
import { useItinerary } from '../state/query/hooks';
import { 
  MapPin, 
  Calendar, 
  Users, 
  Plus, 
  Minus,
  Share2,
  Video,
  Eye,
  EyeOff,
  Menu,
  Settings,
  Download,
  Mail,
  FileText,
  Car,
  Plane,
  Train,
  Ship,
  Bus,
  ExternalLink,
  ChevronRight,
  Search,
  Filter,
  MoreHorizontal,
  Globe,
  UserPlus,
  Send,
  Star,
  Clock,
  Route,
  Hotel,
  Camera,
  Utensils,
  Package,
  Calculator,
  X,
  Check,
  Workflow,
  Map,
  RotateCcw
} from 'lucide-react';
import { TripData, PopularDestination } from '../types/TripData';
import { getAllDestinations, calculateDistance } from '../data/destinations';

interface TravelPlannerProps {
  tripData: TripData;
  onSave: (updatedTrip: TripData) => void;
  onBack: () => void;
  onShare: () => void;
  onExportPDF: () => void;
}

interface Destination {
  id: string;
  name: string;
  nights: number;
  sleeping: boolean;
  discover: boolean;
  transport?: {
    distance: string;
    duration: string;
  };
  notes: string;
  lat?: number;
  lng?: number;
}

interface AgentStatus {
  id: string;
  kind: 'planner' | 'places' | 'route' | 'hotels' | 'flights' | 'activities' | 'bus' | 'train' | 'pt' | 'food' | 'photo' | 'packing' | 'cost';
  status: 'queued' | 'running' | 'succeeded' | 'failed';
  progress: number;
  message?: string;
  step?: string;
}

const CURRENCIES = ['EUR', 'USD', 'GBP', 'CAD', 'AUD', 'NZD', 'HKD', 'CHF', 'JPY', 'CNY', 'SGD', 'ZAR'];

// Use real packing data from itinerary if available
const getPackingCategories = (tripData: TripData) => {
  if (tripData.itinerary?.packingList && tripData.itinerary.packingList.length > 0) {
    return tripData.itinerary.packingList.map(category => ({
      name: category.category,
      items: category.items.map(item => item.name),
      completed: category.items.filter(item => item.essential).length,
      total: category.items.length
    }));
  }
  
  // Fallback to basic categories if no data available
  return [
    {
      name: 'Clothing',
      items: ['T-shirts', 'Jeans', 'Underwear', 'Socks', 'Jacket', 'Shoes', 'Sandals'],
      completed: 0,
      total: 7
    },
    {
      name: 'Essentials', 
      items: ['Medical Insurance Card', 'Medicaments', 'Notebook', 'Passport/ID', 'Phone Charger', 'Power Bank', 'Sunglasses'],
      completed: 0,
      total: 7
    },
    {
      name: 'Toiletries',
      items: ['Toothbrush', 'Toothpaste', 'Shampoo', 'Soap', 'Deodorant', 'Sunscreen', 'First Aid Kit'],
      completed: 0,
      total: 7
    }
  ];
};

export function StipplPlanner({ tripData, onSave, onBack, onShare, onExportPDF }: TravelPlannerProps) {
  console.log('=== STIPPL PLANNER COMPONENT RENDER ===');
  console.log('Trip Data Props:', tripData);
  console.log('=======================================');
  
  const [activeView, setActiveView] = useState('plan');
  const [activeTab, setActiveTab] = useState('destinations');
  const [currency, setCurrency] = useState('EUR');
  const [totalCost, setTotalCost] = useState(0);
  const [showNotes, setShowNotes] = useState(false);
  const [showMap, setShowMap] = useState(true);
  const [selectedDestination, setSelectedDestination] = useState<Destination | null>(null);
  const [transportMode, setTransportMode] = useState<'drive' | 'flights' | 'train' | 'bus' | 'ferry'>('drive');
  const [searchQuery, setSearchQuery] = useState('');
  const [newDestination, setNewDestination] = useState('');
  const [showAgentDock, setShowAgentDock] = useState(false);
  const [activePackingCategory, setActivePackingCategory] = useState(0);
  const [newPackingItem, setNewPackingItem] = useState('');
  const [selectedDiscoverCountry, setSelectedDiscoverCountry] = useState('thailand');
  const [showWorkflowBuilder, setShowWorkflowBuilder] = useState(false);
  const [leftPanelWidth, setLeftPanelWidth] = useState(50); // Percentage
  const [isResizing, setIsResizing] = useState(false);
  const resizeRef = useRef<HTMLDivElement>(null);

  // Resize handlers
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    setIsResizing(true);
  }, []);

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isResizing || !resizeRef.current) return;
    
    const container = resizeRef.current.parentElement;
    if (!container) return;
    
    const containerRect = container.getBoundingClientRect();
    const newWidth = ((e.clientX - containerRect.left) / containerRect.width) * 100;
    
    // Constrain between 20% and 80%
    const constrainedWidth = Math.min(Math.max(newWidth, 20), 80);
    setLeftPanelWidth(constrainedWidth);
  }, [isResizing]);

  const handleMouseUp = useCallback(() => {
    setIsResizing(false);
  }, []);

  useEffect(() => {
    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    } else {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isResizing, handleMouseMove, handleMouseUp]);

  // Fetch fresh data from API instead of using cached props
  const { data: freshTripData, isLoading, error } = useItinerary(tripData.id);
  
  // Use fresh data if available, fallback to props
  const currentTripData = freshTripData || tripData;

  // Log data fetching status
  console.log('=== STIPPL PLANNER DATA FETCH ===');
  console.log('Trip ID:', tripData.id);
  console.log('Is Loading:', isLoading);
  console.log('Error:', error);
  console.log('Fresh Trip Data:', freshTripData);
  console.log('Current Trip Data:', currentTripData);
  console.log('Has Itinerary:', !!currentTripData.itinerary);
  console.log('Days Count:', currentTripData.itinerary?.days?.length || 0);
  console.log('================================');

  const [destinations, setDestinations] = useState<Destination[]>([]);

  // Update destinations when fresh data arrives
  useEffect(() => {
    console.log('=== DESTINATIONS UPDATE EFFECT ===');
    console.log('Current Trip Data:', currentTripData);
    console.log('Has Itinerary:', !!currentTripData.itinerary);
    console.log('Days:', currentTripData.itinerary?.days);
    console.log('Days Length:', currentTripData.itinerary?.days?.length);
    
    if (currentTripData.itinerary?.days && currentTripData.itinerary.days.length > 0) {
      console.log('Processing itinerary days for destinations...');
      const newDestinations = currentTripData.itinerary.days.map((day, index) => {
        console.log(`Processing day ${index}:`, day);
        return {
          id: day.id || index.toString(),
          name: day.location,
          nights: 1,
          sleeping: !!day.accommodation,
          discover: day.components && day.components.length > 0,
          transport: day.components?.find(c => c.type === 'transport') ? {
            distance: `${day.totalDistance || 0} km`,
            duration: `${Math.round((day.totalDuration || 0) / 60)}h ${(day.totalDuration || 0) % 60}m`
          } : undefined,
          notes: day.notes || '',
          lat: day.components?.[0]?.location?.coordinates?.lat || 0,
          lng: day.components?.[0]?.location?.coordinates?.lng || 0
        };
      });
      console.log('New Destinations:', newDestinations);
      setDestinations(newDestinations);
    } else {
      console.log('No itinerary days found, using fallback destination');
      // Fallback to basic destination
      const fallbackDestination = {
        id: '1',
        name: currentTripData.endLocation?.name || currentTripData.destination || 'Unknown Destination',
        nights: 2,
        sleeping: false,
        discover: false,
        transport: { distance: '130 km', duration: '2h 40m' },
        notes: '',
        lat: 13.7563,
        lng: 100.5018
      };
      console.log('Fallback Destination:', fallbackDestination);
      setDestinations([fallbackDestination]);
    }
    console.log('==================================');
  }, [currentTripData]);

  // Use real agent progress from trip data if available
  const [agentStatuses, setAgentStatuses] = useState<AgentStatus[]>([]);

  // Update agent statuses when fresh data arrives
  useEffect(() => {
    console.log('=== AGENT STATUSES UPDATE EFFECT ===');
    console.log('Current Trip Data Agent Progress:', currentTripData.agentProgress);
    
    if (currentTripData.agentProgress) {
      console.log('Processing agent progress data...');
      const newAgentStatuses = Object.entries(currentTripData.agentProgress).map(([agentId, progress]) => {
        console.log(`Processing agent ${agentId}:`, progress);
        return {
          id: agentId,
          kind: agentId as any,
          status: progress.status as any,
          progress: progress.progress,
          message: progress.message
        };
      });
      console.log('New Agent Statuses:', newAgentStatuses);
      setAgentStatuses(newAgentStatuses);
    } else {
      console.log('No agent progress data found, using fallback statuses');
      // Fallback to basic status if no agent data available
      const fallbackStatuses = [
        { id: '1', kind: 'planner', status: 'succeeded', progress: 100 },
        { id: '2', kind: 'places', status: 'completed', progress: 100 },
        { id: '3', kind: 'route', status: 'completed', progress: 100 },
        { id: '4', kind: 'hotels', status: 'completed', progress: 100 },
        { id: '5', kind: 'food', status: 'completed', progress: 100 },
        { id: '6', kind: 'photo', status: 'completed', progress: 100 },
      ];
      console.log('Fallback Agent Statuses:', fallbackStatuses);
      setAgentStatuses(fallbackStatuses);
    }
    console.log('====================================');
  }, [currentTripData]);

  // Show loading state while fetching fresh data
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading planner...</p>
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
            <p className="text-red-600 mb-4">Failed to load planner data</p>
            <Button onClick={onBack}>Go Back</Button>
          </div>
        </div>
      </div>
    );
  }

  const totalNights = destinations.reduce((sum, dest) => sum + dest.nights, 0);
  const maxNights = 10;

  const updateDestination = (id: string, updates: Partial<Destination>) => {
    setDestinations(prev => prev.map(dest => 
      dest.id === id ? { ...dest, ...updates } : dest
    ));
  };

  const addDestination = () => {
    if (newDestination.trim()) {
      const newDest: Destination = {
        id: Date.now().toString(),
        name: newDestination,
        nights: 1,
        sleeping: false,
        discover: false,
        notes: ''
      };
      setDestinations(prev => [...prev, newDest]);
      setNewDestination('');
    }
  };

  const removeDestination = (id: string) => {
    setDestinations(prev => prev.filter(dest => dest.id !== id));
  };

  const renderSidebar = () => (
    <div className="w-64 bg-white border-r border-gray-200 flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 bg-gradient-to-r from-teal-400 to-teal-600 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">T</span>
          </div>
          <span className="text-xl font-semibold">TravelPlanner</span>
        </div>
      </div>
      
      <nav className="flex-1 p-4">
        <div className="space-y-2">
          <Button 
            variant={activeView === 'view' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => setActiveView('view')}
          >
            <Eye className="w-4 h-4 mr-2" />
            View
          </Button>
          <Button 
            variant={activeView === 'plan' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => setActiveView('plan')}
          >
            <MapPin className="w-4 h-4 mr-2" />
            Plan
          </Button>
          <Button 
            variant={activeView === 'budget' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => setActiveView('budget')}
          >
            <Calculator className="w-4 h-4 mr-2" />
            Budget
          </Button>
          <Button 
            variant={activeView === 'packing' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => setActiveView('packing')}
          >
            <Package className="w-4 h-4 mr-2" />
            Packing
          </Button>
          <Button 
            variant={activeView === 'collection' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => setActiveView('collection')}
          >
            <FileText className="w-4 h-4 mr-2" />
            Collection
          </Button>
          <Button 
            variant={activeView === 'docs' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => setActiveView('docs')}
          >
            <FileText className="w-4 h-4 mr-2" />
            Docs
          </Button>
          <Button 
            variant={activeView === 'discover' ? 'default' : 'ghost'} 
            className="w-full justify-start"
            onClick={() => setActiveView('discover')}
          >
            <Search className="w-4 h-4 mr-2" />
            Discover
          </Button>
        </div>
      </nav>
    </div>
  );

  const renderTopNav = () => (
    <div className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
      <div className="flex items-center space-x-4">
        <h1 className="text-xl font-semibold">Your trip to {currentTripData.endLocation?.name || currentTripData.destination || 'Unknown'}</h1>
        <span className="text-gray-500">
          {currentTripData.dates?.start ? new Date(currentTripData.dates.start).toLocaleDateString('en-US', { day: 'numeric', month: 'long' }) : 'TBD'} – {' '}
          {currentTripData.dates?.end ? new Date(currentTripData.dates.end).toLocaleDateString('en-US', { day: 'numeric', month: 'long' }) : 'TBD'}
        </span>
      </div>
      
      <div className="flex items-center space-x-3">
        <Button variant="outline" size="sm" onClick={onShare}>
          <Share2 className="w-4 h-4 mr-2" />
          Save
        </Button>
        <Button variant="outline" size="sm" onClick={onShare}>
          <Share2 className="w-4 h-4 mr-2" />
          Share
        </Button>
        <Button variant="outline" size="sm" onClick={onExportPDF}>
          <Download className="w-4 h-4 mr-2" />
          PDF
        </Button>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="rounded-full">
              <Avatar className="w-8 h-8">
                <AvatarFallback>U</AvatarFallback>
              </Avatar>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={onBack}>
              Back to Overview
            </DropdownMenuItem>
            <DropdownMenuItem>Settings</DropdownMenuItem>
            <DropdownMenuItem>Help</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );

  const renderDestinationsTable = () => (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-4">
          <Select value={currency} onValueChange={setCurrency}>
            <SelectTrigger className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {CURRENCIES.map(curr => (
                <SelectItem key={curr} value={curr}>
                  Cost in {curr} ({curr === 'EUR' ? '€' : curr === 'USD' ? '$' : '£'})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Badge variant="secondary">
            {totalNights}/{maxNights} Nights planned
          </Badge>
          <Button variant="ghost" size="sm" onClick={() => setShowNotes(!showNotes)}>
            <Menu className="w-4 h-4 mr-2" />
            Show notes
          </Button>
        </div>
      </div>

      <div className="border rounded-lg">
        <div className={`grid ${showNotes ? 'grid-cols-7' : 'grid-cols-6'} gap-4 p-4 bg-gray-50 border-b font-medium text-sm`}>
          <div>Destination</div>
          <div>Nights</div>
          <div>Sleeping</div>
          <div>Discover</div>
          <div>Transport</div>
          {showNotes && <div>Notes</div>}
        </div>
        
        {destinations.map((destination) => (
          <div key={destination.id} className={`grid ${showNotes ? 'grid-cols-7' : 'grid-cols-6'} gap-4 p-4 border-b items-center`}>
            <div className="font-medium">{destination.name}</div>
            <div className="flex items-center space-x-2">
              <Button 
                variant="outline" 
                size="sm" 
                className="w-8 h-8 p-0"
                onClick={() => updateDestination(destination.id, { nights: Math.max(0, destination.nights - 1) })}
              >
                <Minus className="w-3 h-3" />
              </Button>
              <span className="w-6 text-center">{destination.nights}</span>
              <Button 
                variant="outline" 
                size="sm" 
                className="w-8 h-8 p-0"
                onClick={() => updateDestination(destination.id, { nights: destination.nights + 1 })}
              >
                <Plus className="w-3 h-3" />
              </Button>
            </div>
            <div>
              <Button 
                variant={destination.sleeping ? "default" : "outline"} 
                size="sm" 
                className="w-8 h-8 p-0"
                onClick={() => updateDestination(destination.id, { sleeping: !destination.sleeping })}
              >
                <Plus className="w-3 h-3" />
              </Button>
            </div>
            <div>
              <Button 
                variant={destination.discover ? "default" : "outline"} 
                size="sm" 
                className="w-8 h-8 p-0 bg-amber-100 border-amber-300 text-amber-700 hover:bg-amber-200"
                onClick={() => updateDestination(destination.id, { discover: !destination.discover })}
              >
                <Plus className="w-3 h-3" />
              </Button>
            </div>
            <div>
              {destination.transport && (
                <Sheet>
                  <SheetTrigger asChild>
                    <Button variant="outline" size="sm" className="bg-pink-100 border-pink-300 text-pink-700 hover:bg-pink-200">
                      {destination.transport.distance}
                      <Plus className="w-3 h-3 ml-1" />
                    </Button>
                  </SheetTrigger>
                  <SheetContent side="left" className="w-[400px]">
                    <SheetHeader>
                      <SheetTitle>
                        {destination.name} — Transportation Details
                        <div className="text-sm text-gray-500 font-normal">
                          {currentTripData.dates?.start ? new Date(currentTripData.dates.start).toLocaleDateString('en-US', { day: 'numeric', month: 'long', year: 'numeric' }) : 'TBD'}
                        </div>
                      </SheetTitle>
                      <SheetDescription>
                        Plan your transportation and route details for this destination.
                      </SheetDescription>
                    </SheetHeader>
                    <div className="mt-6">
                      <Tabs value={transportMode} onValueChange={(value) => setTransportMode(value as any)}>
                        <TabsList className="grid grid-cols-5 w-full">
                          <TabsTrigger value="drive">Drive</TabsTrigger>
                          <TabsTrigger value="flights">Flights</TabsTrigger>
                          <TabsTrigger value="train">Train</TabsTrigger>
                          <TabsTrigger value="bus">Bus</TabsTrigger>
                          <TabsTrigger value="ferry">Ferry</TabsTrigger>
                        </TabsList>
                        
                        <TabsContent value="drive" className="mt-6">
                          <div className="space-y-4">
                            <Button variant="outline" className="w-full justify-start">
                              <ExternalLink className="w-4 h-4 mr-2" />
                              Rent your car on rentalcars.com
                            </Button>
                            <div className="space-y-2">
                              <div className="flex items-center justify-between p-3 border rounded-lg">
                                <span>2h 40m — 161 km</span>
                                <Button size="sm">Add to trip</Button>
                              </div>
                              <div className="flex items-center justify-between p-3 border rounded-lg">
                                <span>2h 45m — 171 km</span>
                                <Button size="sm">Add to trip</Button>
                              </div>
                            </div>
                          </div>
                        </TabsContent>
                        
                        <TabsContent value="flights" className="mt-6">
                          <div className="space-y-4">
                            <div className="flex items-center justify-between p-3 border rounded-lg">
                              <div className="flex items-center space-x-2">
                                <Plus className="w-4 h-4" />
                                <span>Add your flight</span>
                              </div>
                            </div>
                            <Button variant="outline" className="w-full justify-start">
                              <ExternalLink className="w-4 h-4 mr-2" />
                              Find flights on Skyscanner
                            </Button>
                            <div className="text-center py-8 text-gray-500">
                              <Plane className="w-12 h-12 mx-auto mb-2 text-gray-300" />
                              <p className="font-medium">No Plane tickets found</p>
                              <p className="text-sm">Use the + above to add transport to your trip</p>
                            </div>
                          </div>
                        </TabsContent>
                      </Tabs>
                    </div>
                  </SheetContent>
                </Sheet>
              )}
            </div>
            {showNotes && (
              <div>
                <Input 
                  placeholder="Add notes.." 
                  value={destination.notes}
                  onChange={(e) => updateDestination(destination.id, { notes: e.target.value })}
                  className="text-sm"
                />
              </div>
            )}
          </div>
        ))}
        
        <div className="p-4">
          <div className="flex items-center space-x-2">
            <Input 
              placeholder="Add new destination…" 
              value={newDestination}
              onChange={(e) => setNewDestination(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && addDestination()}
              className="flex-1"
            />
            <Button onClick={addDestination}>Add</Button>
          </div>
        </div>
      </div>

      <div className="flex justify-center space-x-4 mt-6">
        <Button variant="outline">
          <Search className="w-4 h-4 mr-2" />
          Discover
        </Button>
        <Button variant="outline">
          <FileText className="w-4 h-4 mr-2" />
          Collection
        </Button>
      </div>
    </div>
  );

  const renderPlanView = () => {
    console.log('=== RENDER PLAN VIEW ===');
    console.log('Destinations Length:', destinations.length);
    console.log('Destinations:', destinations);
    console.log('Current Trip Data Itinerary:', currentTripData.itinerary);
    console.log('Current Trip Data Days:', currentTripData.itinerary?.days);
    console.log('========================');
    
    if (destinations.length === 0) {
      return (
        <div className="p-6 space-y-6">
          <Card className="p-6">
            <div className="flex items-center space-x-3 mb-4">
              <Search className="w-5 h-5 text-gray-400" />
              <Input 
                placeholder="Search any place in the world.." 
                className="flex-1 border-none text-lg"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </Card>
          
          <Card className="p-6">
            <h3 className="font-semibold mb-2">Collect all your online research in one place</h3>
            <p className="text-gray-600 text-sm mb-4">
              Save your links from Instagram, Pinterest, blogs and more to your trip for easy reference.
            </p>
            <Button>Add a link</Button>
          </Card>
        </div>
      );
    }

    return (
      <div className="flex flex-1" ref={resizeRef}>
        <div style={{ width: `${leftPanelWidth}%` }} className="flex-shrink-0 flex flex-col">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="flex flex-col h-full">
            <div className="border-b border-gray-200 px-6 flex-shrink-0">
              <TabsList className="h-12">
                <TabsTrigger value="destinations">Destinations</TabsTrigger>
                <TabsTrigger value="day-by-day">Day by day</TabsTrigger>
              </TabsList>
            </div>
            
            <div className="flex-1 overflow-hidden">
              <TabsContent value="destinations" className="m-0 h-full overflow-y-auto">
                {renderDestinationsTable()}
              </TabsContent>
              
              <TabsContent value="day-by-day" className="m-0 h-full overflow-y-auto p-6">
                <div className="space-y-4">
                  {currentTripData.itinerary?.days?.map((day, index) => (
                    <Card key={day.id || index} className="p-4">
                      <div className="flex items-center justify-between mb-4">
                        <h3 className="font-semibold">Day {day.dayNumber || index + 1} - {day.location || 'Unknown Location'}</h3>
                        <div className="flex gap-2">
                          <Badge variant="outline">{day.theme || 'Explore'}</Badge>
                          <Badge variant="secondary">{day.components?.length || 0} activities</Badge>
                        </div>
                      </div>
                      
                      {day.components && day.components.length > 0 ? (
                        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
                          {day.components.map((component, compIndex) => (
                            <div key={compIndex} className="space-y-2 p-3 border rounded-lg">
                              <div className="flex items-center gap-2">
                                <Badge variant="outline" className="text-xs">
                                  {component.type || 'activity'}
                                </Badge>
                                <span className="text-sm font-medium">{component.name || 'Unnamed Activity'}</span>
                              </div>
                              <p className="text-sm text-gray-600 line-clamp-2">
                                {component.description || 'No description available'}
                              </p>
                              <div className="flex items-center justify-between text-xs text-gray-500">
                                <span>
                                  {component.timing?.startTime || 'TBD'} - {component.timing?.endTime || 'TBD'}
                                </span>
                                <span>
                                  {component.cost?.currency || 'USD'} {component.cost?.pricePerPerson || '0'}
                                </span>
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <div className="grid md:grid-cols-3 gap-4">
                          <div className="space-y-2">
                            <h4 className="text-sm font-medium text-gray-600">Morning</h4>
                            <p className="text-sm text-gray-500">Arrive and check-in</p>
                          </div>
                          <div className="space-y-2">
                            <h4 className="text-sm font-medium text-gray-600">Afternoon</h4>
                            <p className="text-sm text-gray-500">Explore local attractions</p>
                          </div>
                          <div className="space-y-2">
                            <h4 className="text-sm font-medium text-gray-600">Evening</h4>
                            <p className="text-sm text-gray-500">Dinner and rest</p>
                          </div>
                        </div>
                      )}
                      
                      {day.notes && (
                        <div className="mt-4 p-3 bg-blue-50 rounded-lg">
                          <p className="text-sm text-blue-800">{day.notes}</p>
                        </div>
                      )}
                    </Card>
                  )) || (
                    <div className="text-center py-8 text-gray-500">
                      <p>No itinerary data available yet.</p>
                    </div>
                  )}
                </div>
              </TabsContent>
            </div>
          </Tabs>
        </div>
        
        {/* Resize Handle */}
        <div 
          className={`w-1 bg-gray-300 hover:bg-blue-400 cursor-col-resize flex-shrink-0 relative group ${isResizing ? 'bg-blue-500' : ''}`}
          onMouseDown={handleMouseDown}
          title="Drag to resize panels"
        >
          <div className="absolute inset-y-0 -left-1 -right-1 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
            <div className="w-3 h-8 bg-blue-400 rounded-sm flex items-center justify-center">
              <div className="w-0.5 h-4 bg-white rounded"></div>
            </div>
          </div>
        </div>
        
        <div style={{ width: `${100 - leftPanelWidth}%` }} className="bg-gray-100 relative border-l border-gray-200 flex-shrink-0 flex flex-col overflow-hidden">
          <div className="absolute top-4 left-4 z-10">
            <div className="flex space-x-2">
              <Button 
                size="sm" 
                variant={!showWorkflowBuilder ? "default" : "outline"}
                onClick={() => setShowWorkflowBuilder(false)}
              >
                <Globe className="w-4 h-4 mr-2" />
                Map
              </Button>
              <Button 
                size="sm" 
                variant={showWorkflowBuilder ? "default" : "outline"}
                onClick={() => setShowWorkflowBuilder(true)}
              >
                <Workflow className="w-4 h-4 mr-2" />
                Workflow
              </Button>
            </div>
          </div>
          
          <div className="absolute top-4 right-4 space-y-2 z-10">
            <Button size="sm" onClick={onShare}>Share trip</Button>
            <Button size="sm" variant="outline">
              <Video className="w-4 h-4 mr-2" />
              Create movie
            </Button>
            <Button 
              size="sm" 
              variant="outline" 
              onClick={() => setLeftPanelWidth(50)}
              title="Reset panel sizes to 50/50"
            >
              <RotateCcw className="w-4 h-4 mr-2" />
              Reset Layout
            </Button>
          </div>
          
          <div className="flex-1 overflow-hidden">
            {!showWorkflowBuilder ? (
              <div className="h-full flex items-center justify-center text-gray-500">
              <div className="text-center">
                <Globe className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                <p>Interactive Map</p>
                <p className="text-sm">Showing {destinations.length} destinations</p>
              </div>
              <div className="absolute top-20 left-4">
                <div className="bg-white rounded-lg p-3 shadow-sm border">
                  <div className="flex items-center space-x-2 mb-2">
                    <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                    <span className="text-xs">Destinations</span>
                  </div>
                  <div className="flex items-center space-x-2 mb-2">
                    <div className="w-3 h-3 rounded-full bg-green-500"></div>
                    <span className="text-xs">Attractions</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 rounded-full bg-orange-500"></div>
                    <span className="text-xs">Transport</span>
                  </div>
                </div>
              </div>
            </div>
            ) : (
              <div className="h-full overflow-hidden">
                <WorkflowBuilder 
                  tripData={currentTripData}
                  onSave={(updatedItinerary) => {
                    console.log('Workflow saved:', updatedItinerary);
                    // TODO: Implement save functionality
                  }}
                  onCancel={() => {
                    console.log('Workflow cancelled');
                    setShowWorkflowBuilder(false);
                  }}
                />
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  const renderPackingView = () => (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Packing list</h2>
        <div className="flex items-center space-x-3">
          <Button variant="outline" size="sm">
            <UserPlus className="w-4 h-4 mr-2" />
            Invite friends
          </Button>
          <Button size="sm">Add list</Button>
        </div>
      </div>

      <div className="mb-6">
        <Progress value={9} className="h-2" />
        <p className="text-sm text-gray-600 mt-1">9% complete</p>
      </div>

      <div className="grid grid-cols-4 gap-6">
        <div className="space-y-3">
          {getPackingCategories(currentTripData).map((category, index) => (
            <Card 
              key={category.name}
              className={`p-4 cursor-pointer transition-colors ${
                activePackingCategory === index ? 'ring-2 ring-pink-500' : ''
              }`}
              onClick={() => setActivePackingCategory(index)}
            >
              <h3 className="font-medium">{category.name}</h3>
              <p className="text-sm text-gray-600">
                {category.completed}/{category.total}
              </p>
            </Card>
          ))}
        </div>

        <div className="col-span-3">
          <Card className="p-4">
            <h3 className="font-medium mb-4">{getPackingCategories(currentTripData)[activePackingCategory]?.name || 'Category'}</h3>
            <ScrollArea className="h-64">
              <div className="space-y-2">
                {getPackingCategories(currentTripData)[activePackingCategory]?.items?.length > 0 ? (
                  getPackingCategories(currentTripData)[activePackingCategory].items.map((item, index) => (
                    <div key={item} className="flex items-center justify-between p-2 hover:bg-gray-50 rounded">
                      <div className="flex items-center space-x-3">
                        <Checkbox />
                        <span>{item}</span>
                      </div>
                    <Badge variant="secondary" className="text-xs">1x</Badge>
                  </div>
                ))
                ) : (
                  <div className="text-center py-8 text-gray-500">
                    <p>No items in this category</p>
                  </div>
                )}
              </div>
            </ScrollArea>
            
            <div className="flex items-center space-x-2 mt-4 pt-4 border-t">
              <Input 
                placeholder="Add item..." 
                value={newPackingItem}
                onChange={(e) => setNewPackingItem(e.target.value)}
                className="flex-1"
              />
              <Button size="sm">
                <Send className="w-4 h-4" />
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );

  const renderDiscoverView = () => {
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
                {currentTripData.itinerary?.highlights?.map((highlight, index) => (
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
                  {destinationsData.map((destination) => {
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
                            {destination.tags.slice(0, 3).map(tag => (
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
            <h2 className="text-2xl font-semibold mb-2">Suggested Places in {currentTripData.endLocation?.name || currentTripData.destination}</h2>
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
                    {place.tags.map(tag => (
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
                        // TODO: Implement add to itinerary functionality
                      }}>
                        <SelectTrigger className="flex-1 text-xs">
                          <SelectValue placeholder="Select day" />
                        </SelectTrigger>
                        <SelectContent>
                          {currentTripData.itinerary?.days?.map((day, index) => (
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
  };

  const renderBudgetView = () => (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Budget & Costs</h2>
        <div className="flex items-center space-x-3">
          <Select value={currency} onValueChange={setCurrency}>
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {CURRENCIES.map(curr => (
                <SelectItem key={curr} value={curr}>{curr}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Total Budget</h3>
          <div className="text-3xl font-bold text-green-600">
            {currency} {currentTripData.budget?.total || 1200}
          </div>
          <p className="text-sm text-gray-600 mt-2">Estimated total cost</p>
        </Card>
        
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Accommodation</h3>
          <div className="text-2xl font-bold">
            {currency} {Math.round((currentTripData.budget?.total || 1200) * 0.4)}
          </div>
          <p className="text-sm text-gray-600 mt-2">40% of total budget</p>
        </Card>
        
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Activities</h3>
          <div className="text-2xl font-bold">
            {currency} {Math.round((currentTripData.budget?.total || 1200) * 0.3)}
          </div>
          <p className="text-sm text-gray-600 mt-2">30% of total budget</p>
        </Card>
      </div>
    </div>
  );

  const renderCollectionView = () => (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Collection</h2>
        <Button>Add to Collection</Button>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Card className="p-4">
          <div className="aspect-video bg-gray-200 rounded-lg mb-4"></div>
          <h3 className="font-semibold">Saved Places</h3>
          <p className="text-sm text-gray-600">{currentTripData.itinerary?.days?.length || 0} destinations planned</p>
        </Card>
        
        <Card className="p-4">
          <div className="aspect-video bg-gray-200 rounded-lg mb-4"></div>
          <h3 className="font-semibold">Photo Spots</h3>
          <p className="text-sm text-gray-600">{currentTripData.itinerary?.days?.reduce((total, day) => total + (day.components?.filter(c => c.type === 'attraction' || c.type === 'activity')?.length || 0), 0) || 0} attractions</p>
        </Card>
        
        <Card className="p-4">
          <div className="aspect-video bg-gray-200 rounded-lg mb-4"></div>
          <h3 className="font-semibold">Must-Try Foods</h3>
          <p className="text-sm text-gray-600">{currentTripData.itinerary?.days?.reduce((total, day) => total + (day.components?.filter(c => c.type === 'restaurant')?.length || 0), 0) || 0} meals planned</p>
        </Card>
      </div>
    </div>
  );

  const renderDocsView = () => (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Documents & Info</h2>
        <Button>Add Document</Button>
      </div>
      
      <div className="space-y-4">
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold">Travel Insurance</h3>
              <p className="text-sm text-gray-600">Policy: {currentTripData.id}-TI</p>
            </div>
            <Button variant="outline" size="sm">View</Button>
          </div>
        </Card>
        
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold">Flight Tickets</h3>
              <p className="text-sm text-gray-600">Booking: {currentTripData.id}-FL</p>
            </div>
            <Button variant="outline" size="sm">View</Button>
          </div>
        </Card>
        
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold">Hotel Reservations</h3>
              <p className="text-sm text-gray-600">{currentTripData.itinerary?.days?.length || 0} reservations</p>
            </div>
            <Button variant="outline" size="sm">View</Button>
          </div>
        </Card>
      </div>
    </div>
  );

  const renderView = () => (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Trip Overview</h2>
        <div className="flex items-center space-x-3">
          <Button variant="outline" onClick={onShare}>
            <Share2 className="w-4 h-4 mr-2" />
            Share
          </Button>
          <Button variant="outline" onClick={onExportPDF}>
            <FileText className="w-4 h-4 mr-2" />
            Export PDF
          </Button>
        </div>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Trip Summary</h3>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Destination:</span>
              <span className="font-medium">{currentTripData.destination}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Duration:</span>
              <span className="font-medium">{currentTripData.itinerary?.days?.length || 0} days</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Travelers:</span>
              <span className="font-medium">{currentTripData.travelers?.length || 1} people</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Budget:</span>
              <span className="font-medium">{currentTripData.budget?.currency} {currentTripData.budget?.total}</span>
            </div>
          </div>
        </Card>
        
        <Card className="p-6">
          <h3 className="font-semibold mb-4">Agent Progress</h3>
          <div className="space-y-3">
            {agentStatuses.map((agent) => (
              <div key={agent.id} className="flex items-center justify-between">
                <span className="text-sm capitalize">{agent.kind}</span>
                <div className="flex items-center space-x-2">
                  <Progress value={agent.progress} className="w-20" />
                  <span className="text-xs text-gray-600">{agent.progress}%</span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );

  // Main component return
  return (
    <div className="h-screen flex flex-col bg-gray-50">
      {renderTopNav()}
      <div className="flex flex-1 overflow-hidden">
        {renderSidebar()}
        {activeView === 'plan' && renderPlanView()}
        {activeView === 'packing' && renderPackingView()}
        {activeView === 'budget' && renderBudgetView()}
        {activeView === 'collection' && renderCollectionView()}
        {activeView === 'docs' && renderDocsView()}
        {activeView === 'view' && renderView()}
        {activeView === 'discover' && renderDiscoverView()}
      </div>
    </div>
  );
}