/**
 * BudgetSlider Component
 * Interactive budget range selector with real-time feedback
 */

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import * as Slider from '@radix-ui/react-slider';
import { useDebounce } from '@/hooks/useDebounce';

interface BudgetSliderProps {
  value: number[];
  onChange: (value: number[]) => void;
  min?: number;
  max?: number;
  step?: number;
  resultsCount?: number;
}

export function BudgetSlider({
  value,
  onChange,
  min = 0,
  max = 5000,
  step = 10,
  resultsCount,
}: BudgetSliderProps) {
  const [localValue, setLocalValue] = useState(value);
  const debouncedValue = useDebounce(localValue, 300);

  // Update parent with debounced value
  useEffect(() => {
    if (JSON.stringify(debouncedValue) !== JSON.stringify(value)) {
      onChange(debouncedValue);
    }
  }, [debouncedValue]);

  // Sync with external changes
  useEffect(() => {
    if (JSON.stringify(value) !== JSON.stringify(localValue)) {
      setLocalValue(value);
    }
  }, [value]);

  const presets = [
    { label: 'Budget', range: [0, Math.floor(max * 0.3)] },
    { label: 'Mid-range', range: [Math.floor(max * 0.3), Math.floor(max * 0.7)] },
    { label: 'Luxury', range: [Math.floor(max * 0.7), max] },
    { label: 'Any', range: [0, max] },
  ];

  const getGradientColor = (): string => {
    const midpoint = (localValue[0] + localValue[1]) / 2;
    const percentage = midpoint / max;
    
    if (percentage < 0.4) return 'from-green-400 to-green-500';
    if (percentage < 0.7) return 'from-yellow-400 to-yellow-500';
    return 'from-orange-400 to-orange-500';
  };

  const formatCurrency = (amount: number): string => {
    return `$${amount.toLocaleString()}`;
  };

  return (
    <div className="w-full p-6 bg-white rounded-xl border border-gray-200">
      {/* Preset Buttons */}
      <div className="flex gap-2 mb-6">
        {presets.map((preset) => (
          <motion.button
            key={preset.label}
            onClick={() => setLocalValue(preset.range)}
            className="px-3 py-1 text-xs font-medium text-gray-700 bg-gray-100 rounded-full hover:bg-gray-200 transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            {preset.label}
          </motion.button>
        ))}
      </div>

      {/* Slider */}
      <Slider.Root
        className="relative flex items-center select-none touch-none w-full h-5"
        value={localValue}
        onValueChange={(newValue) => {
          setLocalValue(newValue);
        }}
        min={min}
        max={max}
        step={step}
        minStepsBetweenThumbs={1}
      >
        <Slider.Track className="bg-gray-200 relative grow rounded-full h-2">
          <Slider.Range 
            className={`absolute bg-gradient-to-r ${getGradientColor()} h-full rounded-full`}
          />
        </Slider.Track>
        
        <Slider.Thumb
          className="block w-5 h-5 bg-white border-2 border-primary-500 rounded-full shadow-lg hover:scale-110 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 transition-transform"
          aria-label="Minimum budget"
        />
        
        <Slider.Thumb
          className="block w-5 h-5 bg-white border-2 border-primary-500 rounded-full shadow-lg hover:scale-110 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 transition-transform"
          aria-label="Maximum budget"
        />
      </Slider.Root>

      {/* Value Display */}
      <div className="flex justify-between mt-4">
        <div className="text-sm font-semibold text-gray-900">
          {formatCurrency(localValue[0])}
        </div>
        <div className="text-sm font-semibold text-gray-900">
          {formatCurrency(localValue[1])}
        </div>
      </div>
    </div>
  );
}
