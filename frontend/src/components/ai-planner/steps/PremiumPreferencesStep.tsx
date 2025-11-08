/**
 * Premium Preferences Step
 * Step 3 using BudgetSlider and enhanced preference cards with collapsible sections
 */

import { useState } from 'react';
import { motion } from 'framer-motion';
import { fadeInUp } from '@/lib/animations/variants';
import { Mountain, UtensilsCrossed, Camera, ShoppingBag, Landmark, Heart, MessageSquare } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Textarea } from '@/components/ui/textarea';

interface PremiumPreferencesStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

const PACE_OPTIONS = [
  { id: 'relaxed', label: 'Relaxed', description: '2-3 activities per day', emoji: '🧘' },
  { id: 'moderate', label: 'Moderate', description: '4-5 activities per day', emoji: '🚶' },
  { id: 'fast', label: 'Fast-paced', description: '6+ activities per day', emoji: '🏃' },
];

const BUDGET_PRESETS = [
  { label: 'Budget', range: [300, 1000] as [number, number], emoji: '💰', description: ' ' },
  { label: 'Moderate', range: [1000, 2500] as [number, number], emoji: '💵', description: ' ' },
  { label: 'Luxury', range: [2500, 5000] as [number, number], emoji: '💎', description: ' ' },
];

const INTERESTS = [
  { id: 'culture', label: 'Culture & History', icon: Landmark, color: 'text-purple-500' },
  { id: 'adventure', label: 'Adventure', icon: Mountain, color: 'text-orange-500' },
  { id: 'food', label: 'Food & Dining', icon: UtensilsCrossed, color: 'text-red-500' },
  { id: 'photography', label: 'Photography', icon: Camera, color: 'text-blue-500' },
  { id: 'shopping', label: 'Shopping', icon: ShoppingBag, color: 'text-pink-500' },
  { id: 'relaxation', label: 'Relaxation', icon: Heart, color: 'text-green-500' },
];

export function PremiumPreferencesStep({ data, onDataChange }: PremiumPreferencesStepProps) {
  const [budget, setBudget] = useState<[number, number]>(
    data.budgetRange || [500, 2000]
  );
  const [pace, setPace] = useState(data.pace || '');
  const [interests, setInterests] = useState<string[]>(data.interests || []);
  const [customInstructions, setCustomInstructions] = useState(data.customInstructions || '');

  const handlePaceChange = (value: string) => {
    setPace(value);
    onDataChange({ pace: value });
  };

  const toggleInterest = (id: string) => {
    const newInterests = interests.includes(id)
      ? interests.filter((i) => i !== id)
      : [...interests, id];
    setInterests(newInterests);
    onDataChange({ interests: newInterests });
  };

  return (
    <div className="space-y-5 sm:space-y-6">

      {/* Compact Layout */}
      <motion.div
        variants={fadeInUp}
        initial="initial"
        animate="animate"
        className="space-y-4"
      >
        {/* Budget - 3 Cards */}
        <div>
          <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2 block">
            Budget Range
          </label>
          <div className="grid grid-cols-3 gap-2">
            {BUDGET_PRESETS.slice(0, 3).map((preset) => (
              <motion.button
                key={preset.label}
                onClick={() => {
                  setBudget(preset.range);
                  onDataChange({ budgetRange: preset.range });
                }}
                className={cn(
                  'p-2 sm:p-3 rounded-lg border-2 transition-all text-center relative touch-manipulation active:scale-95',
                  budget[0] === preset.range[0] && budget[1] === preset.range[1]
                    ? 'border-primary bg-primary/5 shadow-md'
                    : 'border-border hover:border-primary/50'
                )}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <div className="text-lg sm:text-xl mb-0.5 sm:mb-1">{preset.emoji}</div>
                <div className="text-[10px] sm:text-xs font-semibold">{preset.label}</div>
                <div className="text-[9px] sm:text-xs text-muted-foreground mt-0.5 hidden sm:block">{preset.description}</div>
              </motion.button>
            ))}
          </div>
        </div>

        {/* Travel Pace - 3 Cards */}
        <div>
          <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2 block">
            Travel Pace
          </label>
          <div className="grid grid-cols-3 gap-2">
            {PACE_OPTIONS.map((option) => (
              <motion.button
                key={option.id}
                onClick={() => handlePaceChange(option.id)}
                className={cn(
                  'p-2 sm:p-3 rounded-lg border-2 transition-all text-center relative touch-manipulation active:scale-95',
                  pace === option.id
                    ? 'border-primary bg-primary/5 shadow-md'
                    : 'border-border hover:border-primary/50'
                )}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <div className="text-lg sm:text-xl mb-0.5 sm:mb-1">{option.emoji}</div>
                <div className="text-[10px] sm:text-xs font-semibold">{option.label}</div>
                <div className="text-[9px] sm:text-xs text-muted-foreground mt-0.5">{option.description}</div>
              </motion.button>
            ))}
          </div>
        </div>

        {/* Interests - 2 cols on mobile, 3 on desktop */}
        <div>
          <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2 block">
            Interests
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {INTERESTS.map((interest) => {
              const Icon = interest.icon;
              const isSelected = interests.includes(interest.id);
              return (
                <motion.button
                  key={interest.id}
                  onClick={() => toggleInterest(interest.id)}
                  className={cn(
                    'p-2 sm:p-2.5 rounded-lg border-2 transition-all flex items-center justify-center gap-1 sm:gap-1.5 text-[10px] sm:text-xs touch-manipulation active:scale-95',
                    isSelected
                      ? 'border-primary bg-primary/5 shadow-md'
                      : 'border-border hover:border-primary/50'
                  )}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                >
                  <Icon className={cn('w-3 h-3 sm:w-3.5 sm:h-3.5 flex-shrink-0', isSelected ? 'text-primary' : interest.color)} />
                  <span className={cn('font-medium truncate', isSelected ? 'text-primary' : 'text-foreground')}>
                    {interest.label}
                  </span>
                </motion.button>
              );
            })}
          </div>
        </div>

        {/* Custom Instructions */}
        <div>
          <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2 flex items-center gap-1.5">
            <MessageSquare className="w-3.5 h-3.5" />
            Additional Instructions (Optional)
          </label>
          <Textarea
            value={customInstructions}
            onChange={(e) => {
              setCustomInstructions(e.target.value);
              onDataChange({ customInstructions: e.target.value });
            }}
            placeholder="Any special requests or preferences? e.g., 'I prefer vegetarian restaurants', 'Include kid-friendly activities', 'Avoid crowded places'..."
            className="min-h-[80px] sm:min-h-[100px] text-xs sm:text-sm resize-none"
          />
        </div>
      </motion.div>
    </div>
  );
}
