/**
 * Packing Tab Component
 * AI-generated packing suggestions with categorized checklist
 */

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Checkbox } from '@/components/ui/checkbox';
import { Plus, Trash2, Sparkles } from 'lucide-react';

interface PackingTabProps {
  tripId?: string;
}

interface PackingItem {
  id: string;
  name: string;
  checked: boolean;
  custom: boolean;
}

interface PackingCategory {
  name: string;
  icon: string;
  items: PackingItem[];
}

// Mock AI-generated packing list
const initialPackingList: PackingCategory[] = [
  {
    name: 'Clothing',
    icon: '👕',
    items: [
      { id: '1', name: 'T-shirts (5)', checked: false, custom: false },
      { id: '2', name: 'Pants/Jeans (3)', checked: false, custom: false },
      { id: '3', name: 'Underwear (7)', checked: true, custom: false },
      { id: '4', name: 'Socks (7 pairs)', checked: true, custom: false },
      { id: '5', name: 'Jacket/Sweater', checked: false, custom: false },
      { id: '6', name: 'Sleepwear', checked: false, custom: false },
      { id: '7', name: 'Comfortable shoes', checked: false, custom: false },
      { id: '8', name: 'Sandals/Flip-flops', checked: false, custom: false },
    ],
  },
  {
    name: 'Toiletries',
    icon: '🧴',
    items: [
      { id: '9', name: 'Toothbrush & toothpaste', checked: true, custom: false },
      { id: '10', name: 'Shampoo & conditioner', checked: false, custom: false },
      { id: '11', name: 'Body wash/soap', checked: false, custom: false },
      { id: '12', name: 'Deodorant', checked: true, custom: false },
      { id: '13', name: 'Sunscreen (SPF 50+)', checked: false, custom: false },
      { id: '14', name: 'Moisturizer', checked: false, custom: false },
      { id: '15', name: 'Razor & shaving cream', checked: false, custom: false },
    ],
  },
  {
    name: 'Electronics',
    icon: '📱',
    items: [
      { id: '16', name: 'Phone & charger', checked: true, custom: false },
      { id: '17', name: 'Power bank', checked: false, custom: false },
      { id: '18', name: 'Camera', checked: false, custom: false },
      { id: '19', name: 'Laptop & charger', checked: false, custom: false },
      { id: '20', name: 'Headphones', checked: true, custom: false },
      { id: '21', name: 'Universal adapter', checked: false, custom: false },
    ],
  },
  {
    name: 'Documents',
    icon: '📄',
    items: [
      { id: '22', name: 'Passport', checked: false, custom: false },
      { id: '23', name: 'Visa (if required)', checked: false, custom: false },
      { id: '24', name: 'Travel insurance', checked: false, custom: false },
      { id: '25', name: 'Flight tickets', checked: false, custom: false },
      { id: '26', name: 'Hotel confirmations', checked: false, custom: false },
      { id: '27', name: 'Credit cards & cash', checked: false, custom: false },
    ],
  },
  {
    name: 'Health & Safety',
    icon: '💊',
    items: [
      { id: '28', name: 'Prescription medications', checked: false, custom: false },
      { id: '29', name: 'First aid kit', checked: false, custom: false },
      { id: '30', name: 'Hand sanitizer', checked: true, custom: false },
      { id: '31', name: 'Face masks', checked: false, custom: false },
      { id: '32', name: 'Insect repellent', checked: false, custom: false },
    ],
  },
  {
    name: 'Miscellaneous',
    icon: '🎒',
    items: [
      { id: '33', name: 'Backpack/Day bag', checked: false, custom: false },
      { id: '34', name: 'Reusable water bottle', checked: false, custom: false },
      { id: '35', name: 'Sunglasses', checked: false, custom: false },
      { id: '36', name: 'Travel pillow', checked: false, custom: false },
      { id: '37', name: 'Umbrella', checked: false, custom: false },
    ],
  },
];

export function PackingTab({ tripId }: PackingTabProps) {
  const [packingList, setPackingList] = useState<PackingCategory[]>(initialPackingList);
  const [newItemName, setNewItemName] = useState('');
  const [selectedCategory, setSelectedCategory] = useState(0);

  const totalItems = packingList.reduce((sum, cat) => sum + cat.items.length, 0);
  const checkedItems = packingList.reduce(
    (sum, cat) => sum + cat.items.filter((item) => item.checked).length,
    0
  );
  const progress = totalItems > 0 ? (checkedItems / totalItems) * 100 : 0;

  const toggleItem = (categoryIndex: number, itemId: string) => {
    setPackingList((prev) =>
      prev.map((cat, idx) =>
        idx === categoryIndex
          ? {
              ...cat,
              items: cat.items.map((item) =>
                item.id === itemId ? { ...item, checked: !item.checked } : item
              ),
            }
          : cat
      )
    );
  };

  const addCustomItem = () => {
    if (!newItemName.trim()) return;

    const newItem: PackingItem = {
      id: `custom-${Date.now()}`,
      name: newItemName,
      checked: false,
      custom: true,
    };

    setPackingList((prev) =>
      prev.map((cat, idx) =>
        idx === selectedCategory
          ? { ...cat, items: [...cat.items, newItem] }
          : cat
      )
    );

    setNewItemName('');
  };

  const deleteCustomItem = (categoryIndex: number, itemId: string) => {
    setPackingList((prev) =>
      prev.map((cat, idx) =>
        idx === categoryIndex
          ? {
              ...cat,
              items: cat.items.filter((item) => item.id !== itemId),
            }
          : cat
      )
    );
  };

  return (
    <div className="space-y-6 p-6">
      {/* Progress Overview */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-secondary" />
              AI-Generated Packing List
            </CardTitle>
            <Badge variant="secondary" className="text-lg px-4 py-1">
              {checkedItems}/{totalItems} packed
            </Badge>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            <div className="flex justify-between text-sm text-muted-foreground">
              <span>Packing Progress</span>
              <span>{progress.toFixed(0)}%</span>
            </div>
            <div className="w-full bg-muted rounded-full h-3">
              <div
                className="bg-primary rounded-full h-3 transition-all duration-300"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Add Custom Item */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Add Custom Item</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex gap-2">
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(Number(e.target.value))}
              className="px-3 py-2 border rounded-md"
            >
              {packingList.map((cat, idx) => (
                <option key={idx} value={idx}>
                  {cat.icon} {cat.name}
                </option>
              ))}
            </select>
            <Input
              placeholder="Item name..."
              value={newItemName}
              onChange={(e) => setNewItemName(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && addCustomItem()}
            />
            <Button onClick={addCustomItem} size="icon">
              <Plus className="h-4 w-4" />
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Packing Categories */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {packingList.map((category, categoryIndex) => (
          <Card key={categoryIndex}>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <span className="text-2xl">{category.icon}</span>
                {category.name}
                <Badge variant="outline" className="ml-auto">
                  {category.items.filter((i) => i.checked).length}/{category.items.length}
                </Badge>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {category.items.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center gap-3 p-2 rounded-md hover:bg-muted/50 transition-colors"
                  >
                    <Checkbox
                      checked={item.checked}
                      onCheckedChange={() => toggleItem(categoryIndex, item.id)}
                    />
                    <span
                      className={`flex-1 text-sm ${
                        item.checked ? 'line-through text-muted-foreground' : ''
                      }`}
                    >
                      {item.name}
                    </span>
                    {item.custom && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-6 w-6"
                        onClick={() => deleteCustomItem(categoryIndex, item.id)}
                      >
                        <Trash2 className="h-3 w-3 text-error" />
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
