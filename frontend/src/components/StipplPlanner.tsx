import React, { useState } from 'react';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogTrigger } from './ui/dialog';
import { Card, CardContent, CardHeader } from './ui/card';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Progress } from './ui/progress';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from './ui/sheet';
import { Separator } from './ui/separator';
import { Checkbox } from './ui/checkbox';
import { Textarea } from './ui/textarea';
import { ScrollArea } from './ui/scroll-area';
import { Avatar, AvatarFallback } from './ui/avatar';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from './ui/dropdown-menu';
import { PhotoSpotsPanel, MustTryFoodsPanel, CostEstimatorPanel } from './stippl/ToolsPanels';
import { WorkflowBuilderPanel } from './WorkflowBuilderPanel';
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
  Map
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

const PACKING_CATEGORIES = [
  {
    name: 'Clothing',
    items: ['T-shirts', 'Jeans', 'Underwear', 'Socks', 'Jacket', 'Shoes', 'Sandals'],
    completed: 7,
    total: 27
  },
  {
    name: 'Essentials', 
    items: ['Medical Insurance Card', 'Medicaments', 'Notebook', 'Passport/ID', 'Phone Charger', 'Power Bank', 'Sunglasses'],
    completed: 0,
    total: 23
  },
  {
    name: 'Toiletries',
    items: ['Toothbrush', 'Toothpaste', 'Shampoo', 'Soap', 'Deodorant', 'Sunscreen', 'First Aid Kit'],
    completed: 0,
    total: 27
  }
];

export function StipplPlanner({ tripData, onSave, onBack, onShare, onExportPDF }: TravelPlannerProps) {
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

  const [destinations, setDestinations] = useState<Destination[]>([
    {
      id: '1',
      name: tripData.endLocation.name,
      nights: 2,
      sleeping: false,
      discover: false,
      transport: { distance: '130 km', duration: '2h 40m' },
      notes: '',
      lat: 13.7563,
      lng: 100.5018
    },
    {
      id: '2', 
      name: 'Bank of Thailand Museum',
      nights: 1,
      sleeping: false,
      discover: false,
      notes: '',
      lat: 13.7539,
      lng: 100.5014
    }
  ]);

  const [agentStatuses, setAgentStatuses] = useState<AgentStatus[]>([
    { id: '1', kind: 'planner', status: 'succeeded', progress: 100 },
    { id: '2', kind: 'places', status: 'running', progress: 65, message: 'Finding attractions...' },
    { id: '3', kind: 'route', status: 'queued', progress: 0 },
    { id: '4', kind: 'hotels', status: 'queued', progress: 0 },
    { id: '5', kind: 'food', status: 'queued', progress: 0 },
    { id: '6', kind: 'photo', status: 'queued', progress: 0 },
  ]);

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
        <h1 className="text-xl font-semibold">Your trip to {tripData.endLocation.name}</h1>
        <span className="text-gray-500">
          {new Date(tripData.dates.start).toLocaleDateString('en-US', { day: 'numeric', month: 'long' })} – {' '}
          {new Date(tripData.dates.end).toLocaleDateString('en-US', { day: 'numeric', month: 'long' })}
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
                        {destination.name} — Bank of Thailand Museum
                        <div className="text-sm text-gray-500 font-normal">19 October 2025</div>
                      </SheetTitle>
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
      <div className="flex flex-1">
        <div className="flex-1">
          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <div className="border-b border-gray-200 px-6">
              <TabsList className="h-12">
                <TabsTrigger value="destinations">Destinations</TabsTrigger>
                <TabsTrigger value="day-by-day">Day by day</TabsTrigger>
              </TabsList>
            </div>
            
            <TabsContent value="destinations" className="m-0">
              {renderDestinationsTable()}
            </TabsContent>
            
            <TabsContent value="day-by-day" className="m-0 p-6">
              <div className="space-y-4">
                {destinations.map((destination, index) => (
                  <Card key={destination.id} className="p-4">
                    <div className="flex items-center justify-between mb-4">
                      <h3 className="font-semibold">Day {index + 1} - {destination.name}</h3>
                      <Badge variant="outline">{destination.nights} nights</Badge>
                    </div>
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
                  </Card>
                ))}
              </div>
            </TabsContent>
          </Tabs>
        </div>
        
        <div className="w-1/2 bg-gray-100 relative border-l border-gray-200">
          <div className="absolute inset-0 flex items-center justify-center text-gray-500">
            <div className="text-center">
              <Globe className="w-16 h-16 mx-auto mb-4 text-gray-300" />
              <p>Interactive Map</p>
              <p className="text-sm">Showing {destinations.length} destinations</p>
            </div>
          </div>
          
          <div className="absolute top-4 right-4 space-y-2">
            <Button size="sm" onClick={onShare}>Share trip</Button>
            <Button size="sm" variant="outline">
              <Video className="w-4 h-4 mr-2" />
              Create movie
            </Button>
          </div>
          
          <div className="absolute top-4 left-4">
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
      </div>
    );
  };

  const renderPackingView = () => (
    <div className="p-6">
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
          {PACKING_CATEGORIES.map((category, index) => (
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
            <h3 className="font-medium mb-4">{PACKING_CATEGORIES[activePackingCategory].name}</h3>
            <ScrollArea className="h-64">
              <div className="space-y-2">
                {PACKING_CATEGORIES[activePackingCategory].items.map((item, index) => (
                  <div key={item} className="flex items-center justify-between p-2 hover:bg-gray-50 rounded">
                    <div className="flex items-center space-x-3">
                      <Checkbox />
                      <span>{item}</span>
                    </div>
                    <Badge variant="secondary" className="text-xs">1x</Badge>
                  </div>
                ))}
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
    const [selectedCountry, setSelectedCountry] = useState('thailand');
    const destinationsData = getAllDestinations(selectedCountry === 'all' ? undefined : selectedCountry);
    
    return (
      <div className="flex h-full">
        <div className="w-1/3 border-r border-gray-200 p-6">
          <div className="mb-6">
            <Select value={selectedCountry} onValueChange={setSelectedCountry}>
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
              <h3 className="font-medium mb-3">Articles (30)</h3>
              <div className="space-y-2">
                <div className="p-3 border rounded-lg hover:bg-gray-50 cursor-pointer">
                  <p className="text-sm">Best temples in Bangkok</p>
                </div>
                <div className="p-3 border rounded-lg hover:bg-gray-50 cursor-pointer">
                  <p className="text-sm">Ultimate street food guide</p>
                </div>
                <div className="p-3 border rounded-lg hover:bg-gray-50 cursor-pointer">
                  <p className="text-sm">Hidden gems in Thailand</p>
                </div>
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

        <div className="flex-1 relative">
          {showMap ? (
            <div className="h-full bg-gray-100 relative">
              <div className="absolute inset-0 flex items-center justify-center text-gray-500">
                <div className="text-center">
                  <Globe className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                  <p>Interactive Map</p>
                  <p className="text-sm">Showing {destinationsData.length} destinations</p>
                  <p className="text-sm mt-2">Click destinations to view details</p>
                </div>
              </div>
              
              <div className="absolute top-4 right-4 space-y-2">
                <Button 
                  size="sm" 
                  variant={showMap ? "outline" : "default"}
                  onClick={() => setShowMap(!showMap)}
                >
                  {showMap ? (
                    <>
                      <Workflow className="w-4 h-4 mr-2" />
                      Workflow
                    </>
                  ) : (
                    <>
                      <Map className="w-4 h-4 mr-2" />
                      Map
                    </>
                  )}
                </Button>
                <Button size="sm" onClick={onShare}>Share trip</Button>
                <Button size="sm" variant="outline">
                  <Video className="w-4 h-4 mr-2" />
                  Create movie
                </Button>
              </div>

              <div className="absolute top-4 left-4">
                <div className="bg-white rounded-lg p-3 shadow-sm border">
                  <div className="flex items-center space-x-2 mb-2">
                    <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                    <span className="text-xs">Current Destinations</span>
                  </div>
                  <div className="flex items-center space-x-2 mb-2">
                    <div className="w-3 h-3 rounded-full bg-green-500"></div>
                    <span className="text-xs">Popular Places</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 rounded-full bg-orange-500"></div>
                    <span className="text-xs">Attractions</span>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="h-full">
              <WorkflowBuilderPanel 
                dayPlans={[]} 
                selectedDay={0} 
                onDaySelect={() => {}} 
                onComponentUpdate={() => {}}
                onComponentDelete={() => {}}
                onComponentAdd={() => {}}
                onSave={() => {}}
              />
            </div>
          )}
        </div>
      </div>
    );
  };
}