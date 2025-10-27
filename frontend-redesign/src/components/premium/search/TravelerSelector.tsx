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
    <div className="flex items-center justify-between py-3 border-b border-gray-100 last:border-b-0">
      <div>
        <div className="font-medium text-sm text-gray-900">{label}</div>
        <div className="text-xs text-gray-500">{description}</div>
      </div>
      
      <div className="flex items-center gap-2">
        {/* Decrement Button */}
        <motion.button
          onClick={onDecrement}
          disabled={!canDecrement}
          className={`
            w-7 h-7 rounded-full border-2 flex items-center justify-center transition-colors
            ${canDecrement 
              ? 'border-primary-500 text-primary-500 hover:bg-primary-50' 
              : 'border-gray-200 text-gray-300 cursor-not-allowed'
            }
          `}
          whileHover={canDecrement ? { scale: 1.1 } : {}}
          whileTap={canDecrement ? { scale: 0.9 } : {}}
        >
          <Minus className="w-3 h-3" />
        </motion.button>

        {/* Count Display with Animation */}
        <motion.div
          key={count}
          initial={{ scale: 1.2 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.2 }}
          className="w-7 text-center font-semibold text-sm text-gray-900"
        >
          {count}
        </motion.div>

        {/* Increment Button */}
        <motion.button
          onClick={onIncrement}
          disabled={!canIncrement}
          className={`
            w-7 h-7 rounded-full border-2 flex items-center justify-center transition-colors
            ${canIncrement 
              ? 'border-primary-500 text-primary-500 hover:bg-primary-50' 
              : 'border-gray-200 text-gray-300 cursor-not-allowed'
            }
          `}
          whileHover={canIncrement ? { scale: 1.1 } : {}}
          whileTap={canIncrement ? { scale: 0.9 } : {}}
        >
          <Plus className="w-3 h-3" />
        </motion.button>
      </div>
    </div>
  );
}

export function TravelerSelector({ value, onChange }: TravelerSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);

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

  const getSummaryText = (): string => {
    const parts: string[] = [];
    
    if (value.adults > 0) {
      parts.push(`${value.adults} adult${value.adults !== 1 ? 's' : ''}`);
    }
    if (value.children > 0) {
      parts.push(`${value.children} child${value.children !== 1 ? 'ren' : ''}`);
    }
    if (value.infants > 0) {
      parts.push(`${value.infants} infant${value.infants !== 1 ? 's' : ''}`);
    }
    
    return parts.length > 0 ? parts.join(', ') : 'Add travelers';
  };

  const totalTravelers = value.adults + value.children + value.infants;
  const isAtMaxLimit = totalTravelers >= 27; // 9 per category

  return (
    <div className="relative">
      {/* Trigger Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-3 px-4 py-2.5 border-2 border-gray-300 rounded-lg hover:border-primary-400 transition-colors w-full text-left bg-white"
      >
        <Users className="w-4 h-4 text-gray-400" />
        <span className="flex-1 text-sm font-medium text-gray-900">
          {getSummaryText()}
        </span>
        <svg className={`w-4 h-4 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Dropdown Modal */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -10, scale: 0.95 }}
            transition={{ duration: 0.2 }}
            className="absolute z-50 mt-2 w-full bg-white rounded-xl shadow-2xl border border-gray-200 p-3"
          >
            {/* Traveler Rows */}
            <TravelerRow
              label="Adults"
              description="18+ years"
              count={value.adults}
              onIncrement={() => updateCount('adults', 1)}
              onDecrement={() => updateCount('adults', -1)}
              minCount={1}
              maxCount={9}
            />
            
            <TravelerRow
              label="Children"
              description="2-17 years"
              count={value.children}
              onIncrement={() => updateCount('children', 1)}
              onDecrement={() => updateCount('children', -1)}
              minCount={0}
              maxCount={9}
            />
            
            <TravelerRow
              label="Infants"
              description="0-2 years"
              count={value.infants}
              onIncrement={() => updateCount('infants', 1)}
              onDecrement={() => updateCount('infants', -1)}
              minCount={0}
              maxCount={9}
            />

            {/* Maximum Limit Message */}
            {isAtMaxLimit && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="text-xs text-gray-500 mt-2"
              >
                Maximum travelers reached
              </motion.div>
            )}

            {/* Apply Button */}
            <motion.button
              onClick={() => setIsOpen(false)}
              className="w-full mt-3 px-4 py-2 bg-primary-500 text-white rounded-lg font-semibold text-sm"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              Done
            </motion.button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
