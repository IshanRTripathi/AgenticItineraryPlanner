/**
 * Smart Packing Suggestions Component
 * AI-powered suggestions based on destination, weather, and activities
 */

import { motion } from 'framer-motion';
import { Sparkles, Plus } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Suggestion {
  id: string;
  name: string;
  reason: string;
  category: string;
  icon: string;
}

interface SmartSuggestionsProps {
  destination?: string;
  weather?: string;
  activities?: string[];
  onAddSuggestion: (suggestion: Suggestion) => void;
  addedSuggestionIds?: Set<string>;
}

// Generate smart suggestions based on trip context
const generateSuggestions = (
  destination?: string,
  weather?: string,
  activities?: string[]
): Suggestion[] => {
  const suggestions: Suggestion[] = [];

  // Weather-based suggestions
  if (weather?.toLowerCase().includes('rain')) {
    suggestions.push({
      id: 'sug-umbrella',
      name: 'Umbrella',
      reason: 'Rain expected',
      category: 'Miscellaneous',
      icon: '☔',
    });
    suggestions.push({
      id: 'sug-raincoat',
      name: 'Rain jacket',
      reason: 'Stay dry',
      category: 'Clothing',
      icon: '🧥',
    });
  }

  if (weather?.toLowerCase().includes('sun') || weather?.toLowerCase().includes('hot')) {
    suggestions.push({
      id: 'sug-sunscreen',
      name: 'Sunscreen SPF 50+',
      reason: 'Sunny weather',
      category: 'Toiletries',
      icon: '☀️',
    });
    suggestions.push({
      id: 'sug-hat',
      name: 'Sun hat',
      reason: 'Sun protection',
      category: 'Clothing',
      icon: '🧢',
    });
  }

  if (weather?.toLowerCase().includes('cold') || weather?.toLowerCase().includes('snow')) {
    suggestions.push({
      id: 'sug-gloves',
      name: 'Gloves',
      reason: 'Cold weather',
      category: 'Clothing',
      icon: '🧤',
    });
    suggestions.push({
      id: 'sug-scarf',
      name: 'Scarf',
      reason: 'Stay warm',
      category: 'Clothing',
      icon: '🧣',
    });
  }

  // Activity-based suggestions
  if (activities?.some(a => a.toLowerCase().includes('beach') || a.toLowerCase().includes('swim'))) {
    suggestions.push({
      id: 'sug-swimsuit',
      name: 'Swimsuit',
      reason: 'Beach activities',
      category: 'Clothing',
      icon: '🩱',
    });
    suggestions.push({
      id: 'sug-towel',
      name: 'Beach towel',
      reason: 'Swimming',
      category: 'Miscellaneous',
      icon: '🏖️',
    });
  }

  if (activities?.some(a => a.toLowerCase().includes('hik') || a.toLowerCase().includes('trek'))) {
    suggestions.push({
      id: 'sug-boots',
      name: 'Hiking boots',
      reason: 'Hiking planned',
      category: 'Clothing',
      icon: '🥾',
    });
    suggestions.push({
      id: 'sug-backpack',
      name: 'Hiking backpack',
      reason: 'Trail essentials',
      category: 'Miscellaneous',
      icon: '🎒',
    });
  }

  if (activities?.some(a => a.toLowerCase().includes('business') || a.toLowerCase().includes('work'))) {
    suggestions.push({
      id: 'sug-suit',
      name: 'Business suit',
      reason: 'Professional meetings',
      category: 'Clothing',
      icon: '👔',
    });
    suggestions.push({
      id: 'sug-laptop',
      name: 'Laptop',
      reason: 'Work trip',
      category: 'Electronics',
      icon: '💻',
    });
  }

  // Destination-based suggestions
  if (destination?.toLowerCase().includes('europe')) {
    suggestions.push({
      id: 'sug-adapter',
      name: 'EU power adapter',
      reason: 'European outlets',
      category: 'Electronics',
      icon: '🔌',
    });
  }

  return suggestions;
};

export function SmartSuggestions({
  destination,
  weather,
  activities,
  onAddSuggestion,
  addedSuggestionIds = new Set(),
}: SmartSuggestionsProps) {
  const allSuggestions = generateSuggestions(destination, weather, activities);
  
  // Filter out suggestions that have been added
  const suggestions = allSuggestions.filter(
    (suggestion) => !addedSuggestionIds.has(suggestion.id)
  );

  if (suggestions.length === 0) {
    return null;
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-gradient-to-br from-primary/5 to-purple-50 rounded-xl border border-primary/20 p-5"
    >
      <div className="flex items-center gap-2 mb-4">
        <Sparkles className="w-5 h-5 text-primary" />
        <h3 className="text-sm font-semibold text-gray-900">Smart Suggestions</h3>
        <span className="text-xs text-muted-foreground">
          Based on your trip details
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {suggestions.map((suggestion) => (
          <motion.div
            key={suggestion.id}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-lg border border-gray-200 p-3 hover:shadow-md transition-all group"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3 flex-1">
                <span className="text-2xl">{suggestion.icon}</span>
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-sm text-gray-900 truncate">
                    {suggestion.name}
                  </div>
                  <div className="text-xs text-muted-foreground mt-0.5">
                    {suggestion.reason}
                  </div>
                  <div className="text-xs text-primary/70 mt-1">
                    → {suggestion.category}
                  </div>
                </div>
              </div>
              <button
                onClick={() => onAddSuggestion(suggestion)}
                className={cn(
                  'p-1.5 rounded-md transition-all',
                  'bg-primary/10 text-primary',
                  'hover:bg-primary hover:text-white',
                  'opacity-0 group-hover:opacity-100'
                )}
                title="Add to list"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}
