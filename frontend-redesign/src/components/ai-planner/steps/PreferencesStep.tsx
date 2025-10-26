/**
 * Preferences Step
 * Step 3 of trip wizard
 */

import { useState } from 'react';
import { Label } from '@/components/ui/label';
import { DollarSign, Zap, Sparkles, Mountain, UtensilsCrossed, Camera, ShoppingBag, Landmark } from 'lucide-react';
import { cn } from '@/lib/utils';

interface PreferencesStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

const BUDGET_TIERS = [
  { id: 'budget', label: 'Budget', icon: DollarSign, range: '$500-1500' },
  { id: 'moderate', label: 'Moderate', icon: Sparkles, range: '$1500-3000' },
  { id: 'luxury', label: 'Luxury', icon: Zap, range: '$3000+' },
];

const PACE_OPTIONS = [
  { id: 'relaxed', label: 'Relaxed', description: 'Take it easy' },
  { id: 'moderate', label: 'Moderate', description: 'Balanced pace' },
  { id: 'fast', label: 'Fast-paced', description: 'See it all' },
];

const INTERESTS = [
  { id: 'culture', label: 'Culture', icon: Landmark },
  { id: 'adventure', label: 'Adventure', icon: Mountain },
  { id: 'food', label: 'Food', icon: UtensilsCrossed },
  { id: 'photography', label: 'Photography', icon: Camera },
  { id: 'shopping', label: 'Shopping', icon: ShoppingBag },
];

export function PreferencesStep({ data, onDataChange }: PreferencesStepProps) {
  const [budget, setBudget] = useState(data.budget || 'moderate');
  const [pace, setPace] = useState(data.pace || 'moderate');
  const [interests, setInterests] = useState<string[]>(data.interests || []);

  const handleBudgetChange = (value: string) => {
    setBudget(value);
    onDataChange({ ...data, budget: value });
  };

  const handlePaceChange = (value: string) => {
    setPace(value);
    onDataChange({ ...data, pace: value });
  };

  const toggleInterest = (id: string) => {
    const newInterests = interests.includes(id)
      ? interests.filter((i) => i !== id)
      : [...interests, id];
    setInterests(newInterests);
    onDataChange({ ...data, interests: newInterests });
  };

  return (
    <div className="space-y-8">
      <div className="text-center mb-8">
        <h2 className="text-2xl font-bold text-foreground mb-2">
          Customize your trip
        </h2>
        <p className="text-muted-foreground">
          Tell us about your preferences
        </p>
      </div>

      {/* Budget Tier */}
      <div>
        <Label className="mb-4 block">Budget</Label>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {BUDGET_TIERS.map((tier) => {
            const Icon = tier.icon;
            return (
              <button
                key={tier.id}
                onClick={() => handleBudgetChange(tier.id)}
                className={cn(
                  'p-6 rounded-lg border-2 transition-all hover:shadow-md',
                  budget === tier.id
                    ? 'border-primary bg-primary/5'
                    : 'border-border hover:border-primary/50'
                )}
              >
                <Icon className="w-8 h-8 mx-auto mb-3 text-primary" />
                <div className="font-semibold mb-1">{tier.label}</div>
                <div className="text-sm text-muted-foreground">{tier.range}</div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Pace */}
      <div>
        <Label className="mb-4 block">Travel Pace</Label>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {PACE_OPTIONS.map((option) => (
            <button
              key={option.id}
              onClick={() => handlePaceChange(option.id)}
              className={cn(
                'p-4 rounded-lg border-2 transition-all hover:shadow-md text-left',
                pace === option.id
                  ? 'border-primary bg-primary/5'
                  : 'border-border hover:border-primary/50'
              )}
            >
              <div className="font-semibold mb-1">{option.label}</div>
              <div className="text-sm text-muted-foreground">{option.description}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Interests */}
      <div>
        <Label className="mb-4 block">Interests (select all that apply)</Label>
        <div className="flex flex-wrap gap-3">
          {INTERESTS.map((interest) => {
            const Icon = interest.icon;
            const isSelected = interests.includes(interest.id);
            return (
              <button
                key={interest.id}
                onClick={() => toggleInterest(interest.id)}
                className={cn(
                  'px-4 py-2 rounded-full border-2 transition-all flex items-center gap-2',
                  isSelected
                    ? 'border-primary bg-primary text-white'
                    : 'border-border hover:border-primary/50'
                )}
              >
                <Icon className="w-4 h-4" />
                {interest.label}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
