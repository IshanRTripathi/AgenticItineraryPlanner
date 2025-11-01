/**
 * Packing Category Card Component
 * Collapsible card for packing list categories
 */

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, Plus, Trash2 } from 'lucide-react';
import { Checkbox } from '@/components/ui/checkbox';
import { cn } from '@/lib/utils';

interface PackingItem {
  id: string;
  name: string;
  checked: boolean;
  custom: boolean;
  quantity?: number;
  priority?: boolean;
}

interface PackingCategoryCardProps {
  name: string;
  icon: string;
  items: PackingItem[];
  defaultExpanded?: boolean;
  onToggleItem: (itemId: string) => void;
  onAddItem: (itemName: string) => void;
  onDeleteItem: (itemId: string) => void;
  onMarkAllComplete: () => void;
  onExpandChange?: (expanded: boolean) => void;
}

export function PackingCategoryCard({
  name,
  icon,
  items,
  defaultExpanded = false,
  onToggleItem,
  onAddItem,
  onDeleteItem,
  onMarkAllComplete,
  onExpandChange,
}: PackingCategoryCardProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);

  const handleToggleExpand = () => {
    const newExpanded = !isExpanded;
    setIsExpanded(newExpanded);
    onExpandChange?.(newExpanded);
  };
  const [isAdding, setIsAdding] = useState(false);
  const [newItemName, setNewItemName] = useState('');

  const totalItems = items.length;
  const checkedItems = items.filter(i => i.checked).length;
  const progressPercentage = totalItems > 0 ? Math.round((checkedItems / totalItems) * 100) : 0;
  const isComplete = totalItems > 0 && checkedItems === totalItems;

  const handleAddItem = () => {
    if (newItemName.trim()) {
      onAddItem(newItemName.trim());
      setNewItemName('');
      setIsAdding(false);
    }
  };

  return (
    <div className={cn(
      "bg-white rounded-lg border border-gray-200 overflow-hidden flex flex-col",
      isExpanded ? "h-full" : "h-auto"
    )}>
      {/* Header */}
      <button
        onClick={handleToggleExpand}
        className="w-full flex items-center justify-between p-4 hover:bg-gray-50 transition-colors flex-shrink-0"
      >
        <div className="flex items-center gap-3 flex-1">
          {/* Icon */}
          <div className="text-2xl">{icon}</div>
          
          {/* Label and Progress */}
          <div className="text-left flex-1">
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold text-gray-900">{name}</span>
              <span className="text-xs text-muted-foreground">({totalItems})</span>
              {isComplete && (
                <span className="text-xs font-semibold text-primary">✓ Complete</span>
              )}
            </div>
            
            {/* Progress Bar */}
            <div className="mt-2">
              <div className="flex items-center gap-2">
                <div className="flex-1 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                  <motion.div
                    className={cn(
                      'h-full rounded-full',
                      progressPercentage < 30 ? 'bg-red-500' :
                      progressPercentage < 70 ? 'bg-amber-500' :
                      'bg-green-500'
                    )}
                    initial={{ width: 0 }}
                    animate={{ width: `${progressPercentage}%` }}
                    transition={{ duration: 0.5, ease: 'easeOut' }}
                  />
                </div>
                <span className="text-xs font-semibold text-gray-600 min-w-[3ch]">
                  {progressPercentage}%
                </span>
              </div>
              {!isExpanded && (
                <div className="text-xs text-muted-foreground mt-0.5">
                  {checkedItems} of {totalItems} packed
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Chevron */}
        <ChevronDown
          className={cn(
            'w-4 h-4 text-muted-foreground transition-transform ml-2',
            isExpanded && 'rotate-180'
          )}
        />
      </button>

      {/* Expanded Content */}
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="border-t border-gray-200 flex-1 flex flex-col overflow-hidden"
          >
            <div className="p-3 space-y-2 bg-gray-50/50 flex flex-col flex-1 overflow-hidden">
              {/* Quick Actions */}
              <div className="flex gap-2 pb-2 border-b border-gray-200 flex-shrink-0">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onMarkAllComplete();
                  }}
                  className="px-3 py-1 text-xs font-medium bg-primary/10 text-primary rounded-md hover:bg-primary/20 transition-colors"
                >
                  Mark All Complete
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsAdding(true);
                  }}
                  className="px-3 py-1 text-xs font-medium bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors flex items-center gap-1"
                >
                  <Plus className="w-3 h-3" />
                  Add Item
                </button>
              </div>

              {/* Add Item Form */}
              <AnimatePresence>
                {isAdding && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    className="overflow-hidden flex-shrink-0"
                  >
                    <div className="p-2 bg-white rounded-lg border border-gray-200">
                      <div className="flex gap-2">
                        <input
                          type="text"
                          value={newItemName}
                          onChange={(e) => setNewItemName(e.target.value)}
                          onKeyDown={(e) => e.key === 'Enter' && handleAddItem()}
                          placeholder="Item name..."
                          className="flex-1 px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                          autoFocus
                        />
                        <button
                          onClick={handleAddItem}
                          className="px-3 py-1.5 text-sm font-medium bg-primary text-white rounded-md hover:bg-primary/90"
                        >
                          Add
                        </button>
                        <button
                          onClick={() => {
                            setIsAdding(false);
                            setNewItemName('');
                          }}
                          className="px-3 py-1.5 text-sm font-medium bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Items List - Scrollable */}
              <div className="flex-1 overflow-y-auto space-y-2 min-h-0">
                {items.map((item) => (
                  <motion.div
                    key={item.id}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    className={cn(
                      'flex items-center gap-3 p-2 rounded-lg transition-all',
                      item.checked ? 'bg-gray-100' : 'bg-white hover:bg-gray-50'
                    )}
                  >
                    <div 
                      onClick={(e) => {
                        e.stopPropagation();
                        onToggleItem(item.id);
                      }}
                      className="cursor-pointer"
                    >
                      <Checkbox
                        checked={item.checked}
                        onCheckedChange={() => onToggleItem(item.id)}
                      />
                    </div>
                    <span
                      className={cn(
                        'flex-1 text-sm',
                        item.checked && 'line-through text-muted-foreground'
                      )}
                    >
                      {item.name}
                    </span>
                    {item.priority && !item.checked && (
                      <span className="text-xs font-semibold text-red-600">!</span>
                    )}
                    {item.custom && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onDeleteItem(item.id);
                        }}
                        className="p-1 hover:bg-red-50 rounded transition-colors"
                      >
                        <Trash2 className="w-3 h-3 text-red-600" />
                      </button>
                    )}
                  </motion.div>
                ))}

                {/* Empty State */}
                {items.length === 0 && (
                  <div className="text-center py-4 text-sm text-muted-foreground">
                    No items yet. Click "Add Item" to get started.
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
