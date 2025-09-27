import React, { useState } from 'react';
import { Button } from '../../ui/button';
import { Card } from '../../ui/card';
import { Progress } from '../../ui/progress';
import { ScrollArea } from '../../ui/scroll-area';
import { Input } from '../../ui/input';
import { Checkbox } from '../../ui/checkbox';
import { Badge } from '../../ui/badge';
import { UserPlus, Send } from 'lucide-react';
import { PackingListProps, PackingCategory } from '../shared/types';

// Use real packing data from itinerary if available
const getPackingCategories = (tripData: any): PackingCategory[] => {
  if (tripData.itinerary?.packingList && tripData.itinerary.packingList.length > 0) {
    return tripData.itinerary.packingList.map((category: any) => ({
      name: category.category,
      items: category.items.map((item: any) => item.name),
      completed: category.items.filter((item: any) => item.essential).length,
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

export function PackingListView({ tripData, onUpdate }: PackingListProps) {
  const [activePackingCategory, setActivePackingCategory] = useState(0);
  const [newPackingItem, setNewPackingItem] = useState('');

  const packingCategories = getPackingCategories(tripData);

  return (
    <div className="p-4 md:p-6 overflow-y-auto h-full">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <h2 className="text-xl md:text-2xl font-semibold">Packing list</h2>
        <div className="flex flex-wrap items-center gap-2 md:gap-3">
          <Button variant="outline" size="sm" className="min-h-[44px]">
            <UserPlus className="w-4 h-4 mr-2" />
            <span className="hidden sm:inline">Invite friends</span>
          </Button>
          <Button size="sm" className="min-h-[44px]">Add list</Button>
        </div>
      </div>

      <div className="mb-6">
        <Progress value={9} className="h-2" />
        <p className="text-sm text-gray-600 mt-1">9% complete</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 md:gap-6">
        <div className="space-y-3">
          {packingCategories.map((category, index) => (
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

        <div className="md:col-span-3">
          <Card className="p-4">
            <h3 className="font-medium mb-4">{packingCategories[activePackingCategory]?.name || 'Category'}</h3>
            <ScrollArea className="h-64 md:h-80">
              <div className="space-y-2">
                {packingCategories[activePackingCategory]?.items?.length > 0 ? (
                  packingCategories[activePackingCategory].items.map((item, index) => (
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
}
