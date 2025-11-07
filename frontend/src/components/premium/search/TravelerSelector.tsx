/**
 * TravelerSelector Component
 * Dynamic traveler counter with animated increments
 */

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, Minus, Users } from 'lucide-react';

export interface TravelerCounts {
  adults: number;
  children: number;
  infants: number;
}

interface TravelerSelectorProps {
  value: TravelerCounts;
  onChange: (counts: TravelerCounts) => void;
}

interface TravelerRowProps {
  label: string;
  description: string;
  count: number;
  onIncrement: () => void;
  onDecrement: () => void;
  minCount: number;
  maxCount: number;
}

function TravelerRow({
  label,
  description,
  count,
  onIncrement,
  onDecrement,
  minCount,
  maxCount,
}: TravelerRowProps) {
  const canDecrement = count > minCount;
  const canIncrement = count < maxCount;

  return (
    <div className="flex items-center justify-between">
      <div className="flex-1">
        <div className="font-semibold text-sm text-gray-900">{label}</div>
        <div className="text-xs text-gray-500 mt-0.5">{description}</div>
      </div>
      
      <div className="flex items-center gap-3">
        {/* Decrement Button */}
        <motion.button
          onClick={onDecrement}
          disabled={!canDecrement}
          className={`
            w-9 h-9 rounded-full flex items-center justify-center transition-all shadow-sm
            ${canDecrement 
              ? 'bg-white border-2 border-gray-300 text-gray-700 hover:border-primary hover:text-primary hover:shadow-md' 
              : 'bg-gray-100 border-2 border-gray-200 text-gray-300 cursor-not-allowed'
            }
          `}
          whileHover={canDecrement ? { scale: 1.1 } : {}}
          whileTap={canDecrement ? { scale: 0.9 } : {}}
        >
          <Minus className="w-4 h-4" />
        </motion.button>

        {/* Count Display with Animation */}
        <motion.div
          key={count}
          initial={{ scale: 1.3, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: 'spring', stiffness: 400, damping: 25 }}
          className="min-w-[2rem] text-center font-bold text-lg text-gray-900"
        >
          {count}
        </motion.div>

        {/* Increment Button */}
        <motion.button
          onClick={onIncrement}
          disabled={!canIncrement}
          className={`
            w-9 h-9 rounded-full flex items-center justify-center transition-all shadow-sm
            ${canIncrement 
              ? 'bg-white border-2 border-gray-300 text-gray-700 hover:border-primary hover:text-primary hover:shadow-md' 
              : 'bg-gray-100 border-2 border-gray-200 text-gray-300 cursor-not-allowed'
            }
          `}
          whileHover={canIncrement ? { scale: 1.1 } : {}}
          whileTap={canIncrement ? { scale: 0.9 } : {}}
        >
          <Plus className="w-4 h-4" />
        </motion.button>
      </div>
    </div>
  );
}

export function TravelerSelector({ value, onChange }: TravelerSelectorProps) {
  const updateCount = (type: keyof TravelerCounts, delta: number) => {
    const newCounts = { ...value };
    const newValue = newCounts[type] + delta;
    
    // Enforce limits
    if (type === 'adults') {
      newCounts[type] = Math.max(1, Math.min(9, newValue));
    } else {
      newCounts[type] = Math.max(0, Math.min(9, newValue));
    }
    
    onChange(newCounts);
  };

  const totalTravelers = value.adults + value.children + value.infants;

  return (
    <div className="bg-white rounded-xl shadow-lg border border-gray-200 p-4">
      {/* Header with total */}
      <div className="flex items-center justify-between mb-4 pb-3 border-b border-gray-200">
        <div className="flex items-center gap-2">
          <Users className="w-5 h-5 text-gray-600" />
          <span className="font-semibold text-sm text-gray-900">Travelers</span>
        </div>
        <div className="px-2.5 py-1 bg-primary/10 rounded-full">
          <span className="text-xs font-bold text-primary">
            {totalTravelers} {totalTravelers === 1 ? 'person' : 'people'}
          </span>
        </div>
      </div>

      {/* Traveler Rows - Always visible */}
      <div className="space-y-4">
        <TravelerRow
          label="Adults"
          description="Age 18+"
          count={value.adults}
          onIncrement={() => updateCount('adults', 1)}
          onDecrement={() => updateCount('adults', -1)}
          minCount={1}
          maxCount={9}
        />
        
        <TravelerRow
          label="Children"
          description="Age 2-17"
          count={value.children}
          onIncrement={() => updateCount('children', 1)}
          onDecrement={() => updateCount('children', -1)}
          minCount={0}
          maxCount={9}
        />
        
        <TravelerRow
          label="Infants"
          description="Under 2"
          count={value.infants}
          onIncrement={() => updateCount('infants', 1)}
          onDecrement={() => updateCount('infants', -1)}
          minCount={0}
          maxCount={9}
        />
      </div>
    </div>
  );
}
