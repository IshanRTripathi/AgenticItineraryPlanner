/**
 * Packing Tab Component
 * AI-generated packing suggestions with smart categorization
 */

import { useState } from 'react';
import { motion } from 'framer-motion';
import { PackingCategoryCard } from '@/components/packing/PackingCategoryCard';
import { SmartSuggestions } from '@/components/packing/SmartSuggestions';
import { PackingTemplates } from '@/components/packing/PackingTemplates';
import { staggerChildren, slideUp } from '@/utils/animations';
import { Package } from 'lucide-react';
import { cn } from '@/lib/utils';

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
      { id: '1', name: 'T-shirts', checked: false, custom: false },
      { id: '2', name: 'Pants/Jeans', checked: false, custom: false },
      { id: '3', name: 'Underwear', checked: true, custom: false },
      { id: '4', name: 'Socks', checked: true, custom: false },
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
      { id: '13', name: 'Sunscreen', checked: false, custom: false },
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
      { id: '23', name: 'Visa', checked: false, custom: false },
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

export function PackingTab() {
  const [packingList, setPackingList] = useState<PackingCategory[]>(initialPackingList);
  const [showTemplates, setShowTemplates] = useState(false);
  const [expandedCategories, setExpandedCategories] = useState<Record<number, boolean>>({
    0: true,
    1: true,
  });
  const [addedSuggestionIds, setAddedSuggestionIds] = useState<Set<string>>(new Set());

  // Mock trip context for smart suggestions
  const tripContext = {
    destination: 'Paris, France',
    weather: 'Sunny and warm',
    activities: ['sightseeing', 'museums', 'dining'],
  };

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

  const handleMarkAllComplete = (categoryIndex: number) => {
    setPackingList((prev) =>
      prev.map((cat, idx) =>
        idx === categoryIndex
          ? {
              ...cat,
              items: cat.items.map((item) => ({ ...item, checked: true })),
            }
          : cat
      )
    );
  };

  const handleAddSuggestion = (suggestion: any) => {
    const categoryIndex = packingList.findIndex(
      (cat) => cat.name === suggestion.category
    );

    if (categoryIndex === -1) return;

    const newItem: PackingItem = {
      id: `sug-${Date.now()}`,
      name: suggestion.name,
      checked: false,
      custom: true,
    };

    setPackingList((prev) =>
      prev.map((cat, idx) =>
        idx === categoryIndex
          ? { ...cat, items: [...cat.items, newItem] }
          : cat
      )
    );

    // Mark this suggestion as added
    setAddedSuggestionIds((prev) => new Set(prev).add(suggestion.id));
  };

  const handleSelectTemplate = (templateId: string) => {
    // In a real app, this would load template-specific items
    console.log('Selected template:', templateId);
    setShowTemplates(false);
    // You could add template-specific items here
  };

  return (
    <div className="space-y-6">
      {/* Templates Toggle - Moved to top */}
      {!showTemplates && (
        <motion.button
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          onClick={() => setShowTemplates(true)}
          className="w-full py-3 px-4 bg-white border-2 border-dashed border-gray-300 rounded-lg text-sm font-medium text-gray-600 hover:border-primary hover:text-primary hover:bg-primary/5 transition-all"
        >
          Browse Packing Templates
        </motion.button>
      )}

      {/* Templates */}
      {showTemplates && (
        <PackingTemplates onSelectTemplate={handleSelectTemplate} />
      )}

      {/* Header with Progress */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white rounded-xl shadow-sm border border-gray-200 p-6"
      >
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-2xl font-bold mb-1 flex items-center gap-2">
              <Package className="w-6 h-6 text-primary" />
              Packing List
            </h2>
            <p className="text-sm text-muted-foreground">
              AI-generated suggestions for your trip
            </p>
          </div>
          
          {/* Circular Progress */}
          <div className="relative w-20 h-20">
            <svg className="w-20 h-20 transform -rotate-90">
              <circle
                cx="40"
                cy="40"
                r="32"
                stroke="currentColor"
                strokeWidth="6"
                fill="none"
                className="text-gray-200"
              />
              <motion.circle
                cx="40"
                cy="40"
                r="32"
                stroke="currentColor"
                strokeWidth="6"
                fill="none"
                strokeLinecap="round"
                className={cn(
                  progress < 30 ? 'text-red-500' :
                  progress < 70 ? 'text-amber-500' :
                  'text-green-500'
                )}
                initial={{ strokeDasharray: '0 999' }}
                animate={{ strokeDasharray: `${progress * 2} 999` }}
                transition={{ duration: 1, ease: 'easeOut' }}
              />
            </svg>
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-lg font-bold text-gray-900">{progress.toFixed(0)}%</span>
            </div>
          </div>
        </div>

        {/* Summary Stats */}
        <div className="flex gap-6">
          <div>
            <div className="text-2xl font-bold text-gray-900">{totalItems}</div>
            <div className="text-xs text-muted-foreground">Total Items</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-primary">{checkedItems}</div>
            <div className="text-xs text-muted-foreground">Packed</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-gray-600">{totalItems - checkedItems}</div>
            <div className="text-xs text-muted-foreground">Remaining</div>
          </div>
        </div>
      </motion.div>

      {/* Smart Suggestions - Only show if there are suggestions */}
      <SmartSuggestions
        destination={tripContext.destination}
        weather={tripContext.weather}
        activities={tripContext.activities}
        onAddSuggestion={handleAddSuggestion}
        addedSuggestionIds={addedSuggestionIds}
      />

      {/* Category Cards - 2 Column Grid */}
      <motion.div
        variants={staggerChildren}
        initial="initial"
        animate="animate"
        className="grid grid-cols-1 lg:grid-cols-2 gap-4"
      >
        {packingList.map((category, categoryIndex) => (
          <motion.div 
            key={categoryIndex} 
            variants={slideUp} 
            className={expandedCategories[categoryIndex] ? "h-[400px]" : "h-auto"}
          >
            <PackingCategoryCard
              name={category.name}
              icon={category.icon}
              items={category.items}
              defaultExpanded={categoryIndex < 2}
              onToggleItem={(itemId) => toggleItem(categoryIndex, itemId)}
              onAddItem={(itemName) => {
                const newItem: PackingItem = {
                  id: `custom-${Date.now()}`,
                  name: itemName,
                  checked: false,
                  custom: true,
                };
                setPackingList((prev) =>
                  prev.map((cat, idx) =>
                    idx === categoryIndex
                      ? { ...cat, items: [...cat.items, newItem] }
                      : cat
                  )
                );
              }}
              onDeleteItem={(itemId) => deleteCustomItem(categoryIndex, itemId)}
              onMarkAllComplete={() => handleMarkAllComplete(categoryIndex)}
              onExpandChange={(expanded) => {
                setExpandedCategories(prev => ({
                  ...prev,
                  [categoryIndex]: expanded
                }));
              }}
            />
          </motion.div>
        ))}
      </motion.div>
    </div>
  );
}
