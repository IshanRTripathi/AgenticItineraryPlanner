import React, { useState } from 'react';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Input } from '../ui/input';
import { Checkbox } from '../ui/checkbox';
import { ScrollArea } from '../ui/scroll-area';
import { 
  MapPin, 
  Camera, 
  Utensils, 
  Calculator,
  Plus,
  Star,
  Clock,
  DollarSign,
  Users,
  Copy,
  Pin
} from 'lucide-react';

interface PhotoSpot {
  id: string;
  name: string;
  description: string;
  bestTime: string;
  difficulty: 'easy' | 'moderate' | 'challenging';
  lat?: number;
  lng?: number;
}

interface FoodItem {
  id: string;
  name: string;
  description: string;
  type: 'street food' | 'restaurant' | 'local dish' | 'dessert';
  price: 'low' | 'medium' | 'high';
  venues?: string[];
}

interface CostEstimate {
  category: string;
  amount: number;
  currency: string;
  breakdown?: { item: string; cost: number; }[];
}

const PHOTO_SPOTS: PhotoSpot[] = [
  {
    id: '1',
    name: 'Wat Arun at Sunrise',
    description: 'Temple of Dawn with stunning river views and traditional architecture',
    bestTime: 'Golden hour (6:30-7:30 AM)',
    difficulty: 'easy'
  },
  {
    id: '2', 
    name: 'Chatuchak Weekend Market',
    description: 'Vibrant market scenes with colorful stalls and street life',
    bestTime: 'Morning (9:00-11:00 AM)',
    difficulty: 'moderate'
  },
  {
    id: '3',
    name: 'Sky Bar Rooftop',
    description: 'Panoramic city skyline views from one of the world\'s highest rooftops',
    bestTime: 'Sunset (6:00-7:30 PM)',
    difficulty: 'easy'
  }
];

const MUST_TRY_FOODS: FoodItem[] = [
  {
    id: '1',
    name: 'Pad Thai',
    description: 'Thailand\'s signature stir-fried noodle dish with tamarind, fish sauce, and peanuts',
    type: 'local dish',
    price: 'low',
    venues: ['Jay Fai', 'Thip Samai', 'Street vendors on Khao San Road']
  },
  {
    id: '2',
    name: 'Tom Yum Goong',
    description: 'Spicy and sour soup with shrimp, mushrooms, and aromatic herbs',
    type: 'local dish', 
    price: 'medium',
    venues: ['Raan Jay Fai', 'Local restaurants', 'Hotel restaurants']
  },
  {
    id: '3',
    name: 'Mango Sticky Rice',
    description: 'Sweet dessert with ripe mango slices over coconut sticky rice',
    type: 'dessert',
    price: 'low',
    venues: ['Street vendors', 'Chatuchak Market', 'MBK Food Court']
  }
];

interface ToolsPanelsProps {
  tripData: any;
  onAddToItinerary: (item: any, type: string) => void;
}

export function PhotoSpotsPanel({ tripData, onAddToItinerary }: ToolsPanelsProps) {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Photo Spots</h2>
        <div className="flex items-center space-x-2">
          <Button 
            variant={viewMode === 'grid' ? 'default' : 'outline'} 
            size="sm"
            onClick={() => setViewMode('grid')}
          >
            Grid
          </Button>
          <Button 
            variant={viewMode === 'list' ? 'default' : 'outline'} 
            size="sm"
            onClick={() => setViewMode('list')}
          >
            List
          </Button>
        </div>
      </div>

      <div className={viewMode === 'grid' ? 'grid md:grid-cols-2 lg:grid-cols-3 gap-4' : 'space-y-4'}>
        {PHOTO_SPOTS.map((spot) => (
          <Card key={spot.id} className="hover:shadow-md transition-shadow">
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-lg flex items-center gap-2">
                    <Camera className="w-5 h-5" />
                    {spot.name}
                  </CardTitle>
                  <Badge 
                    variant={spot.difficulty === 'easy' ? 'default' : spot.difficulty === 'moderate' ? 'secondary' : 'destructive'}
                    className="mt-2"
                  >
                    {spot.difficulty}
                  </Badge>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-600 mb-3">{spot.description}</p>
              <div className="flex items-center justify-between text-xs text-gray-500 mb-4">
                <div className="flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  <span>{spot.bestTime}</span>
                </div>
              </div>
              <div className="flex space-x-2">
                <Button 
                  size="sm" 
                  variant="outline" 
                  className="flex-1"
                  onClick={() => onAddToItinerary(spot, 'photo')}
                >
                  <Copy className="w-3 h-3 mr-1" />
                  Copy to Day
                </Button>
                <Button size="sm" variant="outline">
                  <Pin className="w-3 h-3 mr-1" />
                  Pin to Map
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

export function MustTryFoodsPanel({ tripData, onAddToItinerary }: ToolsPanelsProps) {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');

  const filteredFoods = selectedCategory === 'all' 
    ? MUST_TRY_FOODS 
    : MUST_TRY_FOODS.filter(food => food.type === selectedCategory);

  const categories = ['all', 'local dish', 'street food', 'restaurant', 'dessert'];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Must-Try Foods</h2>
        <div className="flex items-center space-x-2">
          {categories.map(category => (
            <Button
              key={category}
              variant={selectedCategory === category ? 'default' : 'outline'}
              size="sm"
              onClick={() => setSelectedCategory(category)}
              className="capitalize"
            >
              {category}
            </Button>
          ))}
        </div>
      </div>

      <div className="space-y-4">
        {filteredFoods.map((food) => (
          <Card key={food.id} className="hover:shadow-md transition-shadow">
            <CardContent className="p-4">
              <div className="flex items-start justify-between mb-3">
                <div className="flex-1">
                  <h3 className="font-semibold flex items-center gap-2">
                    <Utensils className="w-4 h-4" />
                    {food.name}
                  </h3>
                  <div className="flex items-center gap-2 mt-1">
                    <Badge variant="outline" className="text-xs capitalize">
                      {food.type}
                    </Badge>
                    <Badge 
                      variant={food.price === 'low' ? 'default' : food.price === 'medium' ? 'secondary' : 'destructive'}
                      className="text-xs"
                    >
                      {food.price} price
                    </Badge>
                  </div>
                </div>
                <Button 
                  size="sm"
                  onClick={() => onAddToItinerary(food, 'food')}
                >
                  <Plus className="w-3 h-3 mr-1" />
                  Add to Trip
                </Button>
              </div>
              
              <p className="text-sm text-gray-600 mb-3">{food.description}</p>
              
              {food.venues && (
                <div>
                  <p className="text-xs font-medium text-gray-500 mb-1">Where to try:</p>
                  <div className="flex flex-wrap gap-1">
                    {food.venues.map((venue, index) => (
                      <Badge key={index} variant="outline" className="text-xs">
                        {venue}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

export function CostEstimatorPanel({ tripData }: { tripData: any }) {
  const [budgetTier, setBudgetTier] = useState<'budget' | 'standard' | 'luxury'>('standard');
  const [partySize, setPartySize] = useState(tripData.partySize || 2);
  const [currency, setCurrency] = useState('USD');

  const costEstimates: CostEstimate[] = [
    {
      category: 'Accommodation',
      amount: budgetTier === 'budget' ? 25 : budgetTier === 'standard' ? 60 : 150,
      currency,
      breakdown: [
        { item: 'Hotel/Hostel per night', cost: budgetTier === 'budget' ? 25 : budgetTier === 'standard' ? 60 : 150 }
      ]
    },
    {
      category: 'Food & Dining',
      amount: budgetTier === 'budget' ? 15 : budgetTier === 'standard' ? 35 : 80,
      currency,
      breakdown: [
        { item: 'Meals per day', cost: budgetTier === 'budget' ? 15 : budgetTier === 'standard' ? 35 : 80 }
      ]
    },
    {
      category: 'Transportation',
      amount: budgetTier === 'budget' ? 8 : budgetTier === 'standard' ? 20 : 45,
      currency,
      breakdown: [
        { item: 'Local transport per day', cost: budgetTier === 'budget' ? 8 : budgetTier === 'standard' ? 20 : 45 }
      ]
    },
    {
      category: 'Activities & Attractions',
      amount: budgetTier === 'budget' ? 10 : budgetTier === 'standard' ? 25 : 60,
      currency,
      breakdown: [
        { item: 'Attractions per day', cost: budgetTier === 'budget' ? 10 : budgetTier === 'standard' ? 25 : 60 }
      ]
    }
  ];

  const totalDaily = costEstimates.reduce((sum, estimate) => sum + estimate.amount, 0);
  const tripDays = tripData.dates ? 
    Math.ceil((new Date(tripData.dates.end).getTime() - new Date(tripData.dates.start).getTime()) / (1000 * 3600 * 24)) : 7;
  const totalTrip = totalDaily * tripDays * partySize;

  return (
    <div className="p-6">
      <h2 className="text-2xl font-semibold mb-6">Cost Estimator</h2>
      
      <div className="grid md:grid-cols-2 gap-6">
        <div className="space-y-4">
          <Card className="p-4">
            <h3 className="font-medium mb-4">Trip Settings</h3>
            <div className="space-y-3">
              <div>
                <label className="text-sm font-medium mb-2 block">Budget Tier</label>
                <div className="flex space-x-2">
                  {['budget', 'standard', 'luxury'].map(tier => (
                    <Button
                      key={tier}
                      variant={budgetTier === tier ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => setBudgetTier(tier as any)}
                      className="capitalize"
                    >
                      {tier}
                    </Button>
                  ))}
                </div>
              </div>
              
              <div>
                <label className="text-sm font-medium mb-2 block">Party Size</label>
                <div className="flex items-center space-x-2">
                  <Button 
                    variant="outline" 
                    size="sm"
                    onClick={() => setPartySize(Math.max(1, partySize - 1))}
                  >
                    -
                  </Button>
                  <span className="w-8 text-center">{partySize}</span>
                  <Button 
                    variant="outline" 
                    size="sm"
                    onClick={() => setPartySize(partySize + 1)}
                  >
                    +
                  </Button>
                </div>
              </div>
              
              <div>
                <label className="text-sm font-medium mb-2 block">Trip Duration</label>
                <p className="text-sm text-gray-600">{tripDays} days</p>
              </div>
            </div>
          </Card>
        </div>

        <div className="space-y-4">
          <Card className="p-4">
            <h3 className="font-medium mb-4">Estimated Costs (per person/day)</h3>
            <div className="space-y-3">
              {costEstimates.map((estimate) => (
                <div key={estimate.category} className="flex items-center justify-between">
                  <span className="text-sm">{estimate.category}</span>
                  <span className="font-medium">${estimate.amount}</span>
                </div>
              ))}
              <div className="border-t pt-3 mt-3">
                <div className="flex items-center justify-between font-semibold">
                  <span>Daily Total</span>
                  <span>${totalDaily}</span>
                </div>
              </div>
            </div>
          </Card>

          <Card className="p-4 bg-blue-50">
            <h3 className="font-medium mb-2">Trip Total</h3>
            <div className="text-2xl font-bold text-blue-700">
              ${totalTrip.toLocaleString()}
            </div>
            <p className="text-sm text-blue-600 mt-1">
              {partySize} {partySize === 1 ? 'person' : 'people'} × {tripDays} days
            </p>
          </Card>
        </div>
      </div>
    </div>
  );
}