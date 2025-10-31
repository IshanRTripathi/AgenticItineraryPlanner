/**
 * Premium Preferences Step
 * Step 3 using BudgetSlider and enhanced preference cards with collapsible sections
 */

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { fadeInUp } from '@/lib/animations/variants';
import { Zap, Mountain, UtensilsCrossed, Camera, ShoppingBag, Landmark, Heart, ChevronDown } from 'lucide-react';
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

const BUDGET_PRESETS = [
  { label: 'Budget', range: [300, 1000] as [number, number], emoji: '💰', description: '$300-1K' },
  { label: 'Moderate', range: [1000, 2500] as [number, number], emoji: '💵', description: '$1K-2.5K' },
  { label: 'Luxury', range: [2500, 5000] as [number, number], emoji: '💎', description: '$2.5K-5K' },
  { label: 'Flexible', range: [0, 5000] as [number, number], emoji: '✨', description: 'Any' },
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
  const [isBudgetExpanded, setIsBudgetExpanded] = useState(true);
  const [isPaceExpanded, setIsPaceExpanded] = useState(false);
  const [isInterestsExpanded, setIsInterestsExpanded] = useState(false);

  // Track if sections were just selected to trigger auto-collapse/expand
  const [justSelectedBudget, setJustSelectedBudget] = useState(false);
  const [justSelectedPace, setJustSelectedPace] = useState(false);

  // Auto-collapse budget and expand pace when budget preset is selected
  useEffect(() => {
    if (justSelectedBudget && isBudgetExpanded) {
      setTimeout(() => {
        setIsBudgetExpanded(false);
        setIsPaceExpanded(true);
        setJustSelectedBudget(false);
      }, 300);
    }
  }, [justSelectedBudget, isBudgetExpanded]);

  // Auto-collapse pace and expand interests when pace is selected
  useEffect(() => {
    if (justSelectedPace && isPaceExpanded) {
      setTimeout(() => {
        setIsPaceExpanded(false);
        setIsInterestsExpanded(true);
        setJustSelectedPace(false);
      }, 300);
    }
  }, [justSelectedPace, isPaceExpanded]);

  const handlePaceChange = (value: string) => {
    setPace(value);
    onDataChange({ pace: value });
    setJustSelectedPace(true);
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

      {/* Consistent Collapsible Cards Layout */}
      <motion.div
        variants={fadeInUp}
        initial="initial"
        animate="animate"
        transition={{ delay: 0.1 }}
        className="space-y-4"
      >
        {/* Budget - Collapsible */}
        <div className="border-2 border-border rounded-lg overflow-hidden">
          <button
            onClick={() => setIsBudgetExpanded(!isBudgetExpanded)}
            className="w-full flex items-center justify-between p-4 bg-white hover:bg-gray-50 transition-colors"
          >
            <div className="flex items-center gap-2">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                Budget Range
              </span>
              {!isBudgetExpanded && (
                <span className="text-xs font-semibold text-primary">
                  ${budget[0].toLocaleString()} - ${budget[1].toLocaleString()}
                </span>
              )}
            </div>
            <ChevronDown className={cn(
              'w-4 h-4 text-muted-foreground transition-transform',
              isBudgetExpanded && 'rotate-180'
            )} />
          </button>

          <AnimatePresence>
            {isBudgetExpanded && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ duration: 0.3 }}
                className="overflow-hidden"
              >
                <div className="p-4 pt-0">
                  {/* Budget Presets */}
                  <div className="grid grid-cols-4 gap-2">
                    {BUDGET_PRESETS.map((preset) => (
                      <motion.button
                        key={preset.label}
                        onClick={() => {
                          setBudget(preset.range);
                          onDataChange({ budgetRange: preset.range });
                          setJustSelectedBudget(true);
                        }}
                        className={cn(
                          'p-3 rounded-lg border-2 transition-all text-center relative',
                          budget[0] === preset.range[0] && budget[1] === preset.range[1]
                            ? 'border-primary bg-primary/5 shadow-md'
                            : 'border-border hover:border-primary/50'
                        )}
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                      >
                        {budget[0] === preset.range[0] && budget[1] === preset.range[1] && (
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
                        <div className="text-2xl mb-1">{preset.emoji}</div>
                        <div className="text-xs font-semibold">{preset.label}</div>
                        <div className="text-xs text-muted-foreground mt-0.5">{preset.description}</div>
                      </motion.button>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

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
      </motion.div>
    </div>
  );
}
