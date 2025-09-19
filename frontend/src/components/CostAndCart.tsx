import React, { useState } from 'react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Checkbox } from './ui/checkbox';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Separator } from './ui/separator';
import { Switch } from './ui/switch';
import { 
  ArrowLeft, 
  CreditCard, 
  Plane, 
  Building, 
  Car,
  MapPin,
  Users,
  Info,
  Percent
} from 'lucide-react';
import { TripData } from '../types/TripData';

interface CostAndCartProps {
  tripData: TripData;
  onCheckout: () => void;
  onBack: () => void;
}

interface CartItem {
  id: string;
  type: 'flight' | 'hotel' | 'transport' | 'activity';
  title: string;
  description: string;
  basePrice: number;
  quantity: number;
  selected: boolean;
  options?: Array<{
    id: string;
    title: string;
    description: string;
    price: number;
    selected: boolean;
  }>;
}

export function CostAndCart({ tripData, onCheckout, onBack }: CostAndCartProps) {
  const [promoCode, setPromoCode] = useState('');
  const [promoApplied, setPromoApplied] = useState(false);
  const [holdPrices, setHoldPrices] = useState(false);

  // Generate mock cart items based on trip data
  const generateCartItems = (): CartItem[] => {
    const items: CartItem[] = [];
    
    // Flights
    items.push({
      id: 'flight-1',
      type: 'flight',
      title: `Flights to ${tripData.destination}`,
      description: 'Round-trip flights for all travelers',
      basePrice: Math.floor(tripData.budget * 0.4 / (tripData.partySize || tripData.travelers.length)),
      quantity: tripData.partySize || tripData.travelers.length,
      selected: true,
      options: [
        {
          id: 'economy',
          title: 'Economy Class',
          description: 'Standard seating, meals included',
          price: Math.floor(tripData.budget * 0.4 / (tripData.partySize || tripData.travelers.length)),
          selected: true
        },
        {
          id: 'premium',
          title: 'Premium Economy',
          description: 'Extra legroom, priority boarding',
          price: Math.floor(tripData.budget * 0.6 / (tripData.partySize || tripData.travelers.length)),
          selected: false
        }
      ]
    });

    // Accommodation
    const nights = Math.ceil((new Date(tripData.dates.end).getTime() - new Date(tripData.dates.start).getTime()) / (1000 * 60 * 60 * 24));
    items.push({
      id: 'hotel-1',
      type: 'hotel',
      title: `${tripData.stayType || 'Hotel'} Accommodation`,
      description: `${nights} nights in ${tripData.destination}`,
      basePrice: Math.floor(tripData.budget * 0.3 / nights),
      quantity: nights,
      selected: true,
      options: [
        {
          id: 'standard',
          title: 'Standard Room',
          description: 'Comfortable room with basic amenities',
          price: Math.floor(tripData.budget * 0.3 / nights),
          selected: true
        },
        {
          id: 'deluxe',
          title: 'Deluxe Room',
          description: 'Spacious room with premium amenities',
          price: Math.floor(tripData.budget * 0.45 / nights),
          selected: false
        }
      ]
    });

    // Local Transport
    if (tripData.transport && tripData.transport !== 'Walking') {
      items.push({
        id: 'transport-1',
        type: 'transport',
        title: 'Local Transportation',
        description: `${tripData.transport} passes and transfers`,
        basePrice: Math.floor(tripData.budget * 0.1),
        quantity: 1,
        selected: true
      });
    }

    // Activities (from itinerary)
    if (tripData.itinerary?.days) {
      const activities = tripData.itinerary.days.flatMap((day: any) => day.activities);
      const paidActivities = activities.filter((activity: any) => activity.type !== 'food');
      
      if (paidActivities.length > 0) {
        items.push({
          id: 'activities-1',
          type: 'activity',
          title: 'Activities & Experiences',
          description: `${paidActivities.length} curated experiences`,
          basePrice: Math.floor(tripData.budget * 0.2),
          quantity: 1,
          selected: true
        });
      }
    }

    return items;
  };

  const [cartItems, setCartItems] = useState<CartItem[]>(generateCartItems());

  const toggleItemSelection = (itemId: string) => {
    setCartItems(items =>
      items.map(item =>
        item.id === itemId ? { ...item, selected: !item.selected } : item
      )
    );
  };

  const selectOption = (itemId: string, optionId: string) => {
    setCartItems(items =>
      items.map(item =>
        item.id === itemId && item.options
          ? {
              ...item,
              options: item.options.map(option => ({
                ...option,
                selected: option.id === optionId
              })),
              basePrice: item.options.find(opt => opt.id === optionId)?.price || item.basePrice
            }
          : item
      )
    );
  };

  const applyPromoCode = () => {
    if (promoCode.toLowerCase() === 'save10') {
      setPromoApplied(true);
    }
  };

  const calculateSubtotal = () => {
    return cartItems
      .filter(item => item.selected)
      .reduce((sum, item) => sum + (item.basePrice * item.quantity), 0);
  };

  const calculateTaxes = () => {
    return Math.floor(calculateSubtotal() * 0.18); // 18% GST
  };

  const calculateDiscount = () => {
    return promoApplied ? Math.floor(calculateSubtotal() * 0.1) : 0;
  };

  const calculateTotal = () => {
    return calculateSubtotal() + calculateTaxes() - calculateDiscount();
  };

  const getItemIcon = (type: string) => {
    switch (type) {
      case 'flight': return <Plane className="h-5 w-5" />;
      case 'hotel': return <Building className="h-5 w-5" />;
      case 'transport': return <Car className="h-5 w-5" />;
      case 'activity': return <MapPin className="h-5 w-5" />;
      default: return <MapPin className="h-5 w-5" />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b">
        <div className="max-w-6xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Button variant="ghost" onClick={onBack}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Itinerary
              </Button>
              <div>
                <h1 className="text-2xl">Review & Book</h1>
                <p className="text-gray-600">{tripData.destination} Trip</p>
              </div>
            </div>
            
            <Badge variant="outline" className="flex items-center gap-2">
              <Users className="h-3 w-3" />
              {tripData.partySize || tripData.travelers.length} {(tripData.partySize || tripData.travelers.length) === 1 ? 'traveler' : 'travelers'}
            </Badge>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 py-6">
        <div className="grid lg:grid-cols-3 gap-6">
          {/* Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            {cartItems.map((item) => (
              <Card key={item.id}>
                <CardContent className="p-6">
                  <div className="flex items-start gap-4">
                    <div className="flex items-center gap-3">
                      <Checkbox
                        checked={item.selected}
                        onCheckedChange={() => toggleItemSelection(item.id)}
                      />
                      <div className="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center">
                        {getItemIcon(item.type)}
                      </div>
                    </div>
                    
                    <div className="flex-1">
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <h3 className="font-medium">{item.title}</h3>
                          <p className="text-sm text-gray-600">{item.description}</p>
                        </div>
                        <div className="text-right">
                          <p className="font-medium">₹{(item.basePrice * item.quantity).toLocaleString()}</p>
                          {item.quantity > 1 && (
                            <p className="text-xs text-gray-500">₹{item.basePrice.toLocaleString()} × {item.quantity}</p>
                          )}
                        </div>
                      </div>

                      {/* Options */}
                      {item.options && (
                        <div className="mt-4 space-y-2">
                          {item.options.map((option) => (
                            <div
                              key={option.id}
                              className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                                option.selected ? 'bg-indigo-50 border-indigo-200' : 'hover:bg-gray-50'
                              }`}
                              onClick={() => selectOption(item.id, option.id)}
                            >
                              <div className="flex items-center justify-between">
                                <div>
                                  <p className="font-medium text-sm">{option.title}</p>
                                  <p className="text-xs text-gray-600">{option.description}</p>
                                </div>
                                <div className="text-right">
                                  <p className="text-sm font-medium">₹{option.price.toLocaleString()}</p>
                                  {option.price !== item.basePrice && (
                                    <p className="text-xs text-gray-500">
                                      +₹{(option.price - item.basePrice).toLocaleString()}
                                    </p>
                                  )}
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Hold Prices Note */}
                      {item.type === 'flight' && (
                        <div className="mt-3 flex items-center gap-2 text-xs text-blue-600">
                          <Info className="h-3 w-3" />
                          <span>Prices held for 24 hours (where supported)</span>
                        </div>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}

            {/* Price Hold Option */}
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <Label htmlFor="hold-prices" className="font-medium">
                      Hold prices (24 hours)
                    </Label>
                    <p className="text-sm text-gray-600">
                      Lock current prices while you finalize your booking
                    </p>
                  </div>
                  <Switch
                    id="hold-prices"
                    checked={holdPrices}
                    onCheckedChange={setHoldPrices}
                  />
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Summary */}
          <div className="lg:col-span-1">
            <Card className="sticky top-4">
              <CardHeader>
                <CardTitle>Booking Summary</CardTitle>
                <CardDescription>
                  Review your selections and total cost
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* Promo Code */}
                <div className="space-y-2">
                  <Label htmlFor="promo">Promo Code</Label>
                  <div className="flex gap-2">
                    <Input
                      id="promo"
                      placeholder="Enter code"
                      value={promoCode}
                      onChange={(e) => setPromoCode(e.target.value)}
                      disabled={promoApplied}
                    />
                    <Button 
                      variant="outline" 
                      onClick={applyPromoCode}
                      disabled={promoApplied || !promoCode}
                    >
                      Apply
                    </Button>
                  </div>
                  {promoApplied && (
                    <p className="text-sm text-green-600 flex items-center gap-1">
                      <Percent className="h-3 w-3" />
                      10% discount applied!
                    </p>
                  )}
                </div>

                <Separator />

                {/* Cost Breakdown */}
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span>Subtotal</span>
                    <span>₹{calculateSubtotal().toLocaleString()}</span>
                  </div>
                  
                  <div className="flex justify-between">
                    <span>Taxes & Fees</span>
                    <span>₹{calculateTaxes().toLocaleString()}</span>
                  </div>
                  
                  {promoApplied && (
                    <div className="flex justify-between text-green-600">
                      <span>Discount (10%)</span>
                      <span>-₹{calculateDiscount().toLocaleString()}</span>
                    </div>
                  )}
                  
                  <Separator />
                  
                  <div className="flex justify-between text-lg font-medium">
                    <span>Total</span>
                    <span>₹{calculateTotal().toLocaleString()}</span>
                  </div>
                  
                  <div className="text-xs text-gray-500">
                    Per person: ₹{Math.floor(calculateTotal() / (tripData.partySize || tripData.travelers.length)).toLocaleString()}
                  </div>
                </div>

                {/* Budget Comparison */}
                <div className="p-3 bg-gray-50 rounded-lg">
                  <div className="flex justify-between text-sm mb-1">
                    <span>Budget Used</span>
                    <span>{Math.round((calculateTotal() / tripData.budget) * 100)}%</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div 
                      className={`h-2 rounded-full transition-all ${
                        calculateTotal() <= tripData.budget ? 'bg-green-500' : 'bg-red-500'
                      }`}
                      style={{ width: `${Math.min((calculateTotal() / tripData.budget) * 100, 100)}%` }}
                    />
                  </div>
                  <p className="text-xs text-gray-600 mt-1">
                    {calculateTotal() <= tripData.budget 
                      ? `₹${(tripData.budget - calculateTotal()).toLocaleString()} remaining`
                      : `₹${(calculateTotal() - tripData.budget).toLocaleString()} over budget`
                    }
                  </p>
                </div>

                <Button 
                  className="w-full" 
                  size="lg"
                  onClick={onCheckout}
                  disabled={cartItems.filter(item => item.selected).length === 0}
                >
                  <CreditCard className="h-4 w-4 mr-2" />
                  Proceed to Payment
                </Button>

                <p className="text-xs text-gray-500 text-center">
                  You won't be charged until you complete your booking
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}