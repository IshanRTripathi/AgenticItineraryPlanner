/**
 * Premium Preferences Step
 * Step 3 using BudgetSlider and enhanced preference cards with collapsible sections
 */

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { BudgetSlider } from '@/components/premium/search/BudgetSlider';
import { fadeInUp } from '@/lib/animations/variants';
import { DollarSign, Zap, Mountain, UtensilsCrossed, Camera, ShoppingBag, Landmark, Heart, ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

interface PremiumPreferencesStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

const PACE_OPTIONS = [
  { id: 'relaxed', label: 'Relaxed', description: '2-3 activities per day', emoji: '🧘' },
  { id: 'moderate', label: 'Moderate', description: '4-5 activities per day', emoji: '🚶' },
  { id: 'fast', label: 'Fast-paced', description: '6+ activities per day', emoji: '🏃' },
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
  const [isPaceExpanded, setIsPaceExpanded] = useState(true);
  const [isInterestsExpanded, setIsInterestsExpanded] = useState(false);

  // Auto-collapse pace and expand interests when pace is selected
  useEffect(() => {
    if (pace && isPaceExpanded) {
      setTimeout(() => {
        setIsPaceExpanded(false);
        setIsInterestsExpanded(true);
      }, 300);
    }
  }, [pace, isPaceExpanded]);

  const handleBudgetChange = (value: number[]) => {
    const budgetRange: [number, number] = [value[0], value[1]];
    setBudget(budgetRange);
    onDataChange({ budgetRange });
  };

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
    <div className="space-y-6">
      {/* Compact Header */}
      <motion.div
        className="text-center"
        variants={fadeInUp}
        initial="initial"
        animate="animate"
      >
        <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-primary/10 mb-3">
          <Zap className="w-6 h-6 text-primary" />
        </div>
        <h2 className="text-2xl font-bold text-foreground mb-1">
          Customize your trip
        </h2>
        <p className="text-sm text-muted-foreground">
          Tell us about your preferences
        </p>
      </motion.div>

      {/* Two Column Layout: Budget on left, Pace + Interests on right */}
      <motion.div
        variants={fadeInUp}
        initial="initial"
        animate="animate"
        transition={{ delay: 0.1 }}
        className="grid grid-cols-1 lg:grid-cols-2 gap-6"
      >
        {/* Left Column: Budget */}
        <div>
          <div className="flex items-center gap-2 mb-4">
            <DollarSign className="w-4 h-4 text-muted-foreground" />
            <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              Budget Range
            </span>
          </div>
          <BudgetSlider
            value={budget}
            onChange={handleBudgetChange}
            min={0}
            max={5000}
            step={50}
          />
        </div>

        {/* Right Column: Travel Pace + Interests (Collapsible) */}
        <div className="space-y-4">
          {/* Travel Pace - Collapsible */}
          <div className="border-2 border-border rounded-lg overflow-hidden">
            <button
              onClick={() => setIsPaceExpanded(!isPaceExpanded)}
              className="w-full flex items-center justify-between p-4 bg-white hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-center gap-2">
                <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Travel Pace
                </span>
                {pace && !isPaceExpanded && (
                  <span className="text-xs font-semibold text-primary">
                    {PACE_OPTIONS.find(o => o.id === pace)?.emoji} {PACE_OPTIONS.find(o => o.id === pace)?.label}
                  </span>
                )}
              </div>
              <ChevronDown className={cn(
                'w-4 h-4 text-muted-foreground transition-transform',
                isPaceExpanded && 'rotate-180'
              )} />
            </button>
            
            <AnimatePresence>
              {isPaceExpanded && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.3 }}
                  className="overflow-hidden"
                >
                  <div className="p-4 pt-0 space-y-2">
                    {PACE_OPTIONS.map((option) => (
                      <motion.button
                        key={option.id}
                        onClick={() => handlePaceChange(option.id)}
                        className={cn(
                          'w-full p-3 rounded-lg border-2 transition-all text-left relative overflow-hidden flex items-center gap-3',
                          pace === option.id
                            ? 'border-primary bg-primary/5 shadow-md'
                            : 'border-border hover:border-primary/50'
                        )}
                        whileHover={{ scale: 1.01 }}
                        whileTap={{ scale: 0.99 }}
                      >
                        {pace === option.id && (
                          <motion.div
                            className="absolute top-2 right-2 w-5 h-5 rounded-full bg-primary flex items-center justify-center"
                            initial={{ scale: 0 }}
                            animate={{ scale: 1 }}
                            transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                          >
                            <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                          </motion.div>
                        )}
                        <div className="text-2xl">{option.emoji}</div>
                        <div className="flex-1">
                          <div className="font-semibold text-sm mb-0.5">{option.label}</div>
                          <div className="text-xs text-muted-foreground">{option.description}</div>
                        </div>
                      </motion.button>
                    ))}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* Interests - Collapsible */}
          <div className="border-2 border-border rounded-lg overflow-hidden">
            <button
              onClick={() => setIsInterestsExpanded(!isInterestsExpanded)}
              className="w-full flex items-center justify-between p-4 bg-white hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-center gap-2">
                <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Interests
                </span>
                {interests.length > 0 && !isInterestsExpanded && (
                  <span className="text-xs font-semibold text-primary">
                    {interests.length} selected
                  </span>
                )}
              </div>
              <ChevronDown className={cn(
                'w-4 h-4 text-muted-foreground transition-transform',
                isInterestsExpanded && 'rotate-180'
              )} />
            </button>
            
            <AnimatePresence>
              {isInterestsExpanded && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.3 }}
                  className="overflow-hidden"
                >
                  <div className="p-4 pt-0">
                    <div className="grid grid-cols-2 gap-2">
                      {INTERESTS.map((interest) => {
                        const Icon = interest.icon;
                        const isSelected = interests.includes(interest.id);
                        return (
                          <motion.button
                            key={interest.id}
                            onClick={() => toggleInterest(interest.id)}
                            className={cn(
                              'p-3 rounded-lg border-2 transition-all flex flex-col items-center gap-2 relative overflow-hidden',
                              isSelected
                                ? 'border-primary bg-primary/5 shadow-md'
                                : 'border-border hover:border-primary/50'
                            )}
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                          >
                            {isSelected && (
                              <motion.div
                                className="absolute top-1 right-1 w-4 h-4 rounded-full bg-primary flex items-center justify-center"
                                initial={{ scale: 0 }}
                                animate={{ scale: 1 }}
                                transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                              >
                                <svg className="w-2.5 h-2.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                              </motion.div>
                            )}
                            <Icon className={cn('w-5 h-5', isSelected ? 'text-primary' : interest.color)} />
                            <span className={cn('text-xs font-medium text-center', isSelected ? 'text-primary' : 'text-foreground')}>
                              {interest.label}
                            </span>
                          </motion.button>
                        );
                      })}
                    </div>
                    
                    {/* Summary */}
                    {interests.length > 0 && (
                      <motion.div
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="mt-3 text-center text-xs text-muted-foreground"
                      >
                        ✨ Perfect! We'll create an itinerary focused on {interests.length} interest{interests.length > 1 ? 's' : ''}
                      </motion.div>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
