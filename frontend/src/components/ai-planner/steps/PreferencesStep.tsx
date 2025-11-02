/**
 * Preferences Step
 * Step 3 of trip wizard
 */

import { useState } from 'react';
import { Label } from '@/components/ui/label';
import { DollarSign, Zap, Sparkles, Mountain, UtensilsCrossed, Camera, ShoppingBag, Landmark, MessageSquare } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Textarea } from '@/components/ui/textarea';

interface PreferencesStepProps {
  data: any;
  onDataChange: (data: any) => void;
}

const BUDGET_TIERS = [
  { id: 'economy', label: 'Economy', icon: DollarSign, range: '$500-1500' },
  { id: 'mid-range', label: 'Mid-Range', icon: Sparkles, range: '$1500-3000' },
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
  { id: 'nature', label: 'Nature', icon: Mountain },
];

export function PreferencesStep({ data, onDataChange }: PreferencesStepProps) {
  const [budget, setBudget] = useState(data.budget || 'mid-range');
  const [pace, setPace] = useState(data.pace || 'moderate');
  const [interests, setInterests] = useState<string[]>(data.interests || []);
  const [customInstructions, setCustomInstructions] = useState(data.customInstructions || '');

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
    <div className="space-y-6">
      {/* Budget Tier */}
      <div>
        <Label className="mb-3 block">Budget</Label>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {BUDGET_TIERS.map((tier) => {
            const Icon = tier.icon;
            return (
              <button
                key={tier.id}
                onClick={() => handleBudgetChange(tier.id)}
                className={cn(
                  'p-4 rounded-lg border-2 transition-all hover:shadow-md',
                  budget === tier.id
                    ? 'border-primary bg-primary/5'
                    : 'border-border hover:border-primary/50'
                )}
              >
                <Icon className="w-6 h-6 mx-auto mb-2 text-primary" />
                <div className="font-semibold text-sm mb-1">{tier.label}</div>
                <div className="text-xs text-muted-foreground">{tier.range}</div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Pace */}
      <div>
        <Label className="mb-3 block">Travel Pace</Label>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {PACE_OPTIONS.map((option) => (
            <button
              key={option.id}
              onClick={() => handlePaceChange(option.id)}
              className={cn(
                'p-4 rounded-lg border-2 transition-all hover:shadow-md',
                pace === option.id
                  ? 'border-primary bg-primary/5'
                  : 'border-border hover:border-primary/50'
              )}
            >
              <div className="font-semibold text-sm mb-1">{option.label}</div>
              <div className="text-xs text-muted-foreground">{option.description}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Interests */}
      <div>
        <Label className="mb-3 block">Interests</Label>
        <div className="grid grid-cols-3 gap-2">
          {INTERESTS.map((interest) => {
            const Icon = interest.icon;
            const isSelected = interests.includes(interest.id);
            return (
              <button
                key={interest.id}
                onClick={() => toggleInterest(interest.id)}
                className={cn(
                  'px-3 py-2 rounded-lg border-2 transition-all flex items-center justify-center gap-1.5 text-sm',
                  isSelected
                    ? 'border-primary bg-primary text-white'
                    : 'border-border hover:border-primary/50'
                )}
              >
                <Icon className="w-3.5 h-3.5" />
                {interest.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Custom Instructions */}
      <div>
        <Label className="mb-2 flex items-center gap-1.5">
          <MessageSquare className="w-4 h-4" />
          Additional Instructions (Optional)
        </Label>
        <Textarea
          value={customInstructions}
          onChange={(e) => {
            setCustomInstructions(e.target.value);
            onDataChange({ ...data, customInstructions: e.target.value });
          }}
          placeholder="Any special requests or preferences? e.g., 'I prefer vegetarian restaurants', 'Include kid-friendly activities', 'Avoid crowded places'..."
          className="min-h-[80px] text-sm resize-none"
        />
      </div>
    </div>
  );
}
