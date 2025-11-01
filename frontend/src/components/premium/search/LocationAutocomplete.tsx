/**
 * LocationAutocomplete Component
 * Advanced location search with grouped suggestions and keyboard navigation
 */

import { useState, ReactNode } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { MapPin, Plane, Landmark, Clock } from 'lucide-react';
import { usePlacesAutocomplete, PlaceSuggestion } from '@/hooks/data/usePlacesAutocomplete';
import { useKeyboardNav } from '@/hooks/interactions/useKeyboardNav';

interface LocationAutocompleteProps {
  value: string;
  onChange: (value: string) => void;
  onSelect?: (suggestion: PlaceSuggestion) => void;
  placeholder?: string;
}

// SuggestionItem sub-component
function SuggestionItem({
  icon,
  primary,
  secondary,
  onClick,
}: {
  icon: ReactNode;
  primary: ReactNode;
  secondary: string;
  onClick: () => void;
}) {
  return (
    <motion.button
      onClick={onClick}
      className="w-full flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-gray-50 transition-colors text-left"
      whileHover={{ x: 4 }}
      transition={{ duration: 0.15 }}
    >
      <div className="text-gray-400">{icon}</div>
      <div className="flex-1 min-w-0">
        <div className="text-sm font-medium text-gray-900 truncate">{primary}</div>
        <div className="text-xs text-gray-500 truncate">{secondary}</div>
      </div>
    </motion.button>
  );
}

export function LocationAutocomplete({
  value,
  onChange,
  onSelect,
  placeholder = 'Search location',
}: LocationAutocompleteProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  
  const { groupedSuggestions, recentSearches, loading, saveToRecent } = usePlacesAutocomplete(value);

  // Flatten all suggestions for keyboard navigation
  const allSuggestions = [
    ...(!value ? recentSearches : []),
    ...groupedSuggestions.cities,
    ...groupedSuggestions.airports,
    ...groupedSuggestions.landmarks,
  ];

  // Helper function to highlight matching text
  const highlightMatch = (text: string, query: string): ReactNode => {
    if (!query) return text;
    
    const parts = text.split(new RegExp(`(${query})`, 'gi'));
    return parts.map((part, i) =>
      part.toLowerCase() === query.toLowerCase() ? (
        <strong key={i} className="font-semibold text-primary-600">
          {part}
        </strong>
      ) : (
        part
      )
    );
  };

  const handleSelect = (suggestion: PlaceSuggestion) => {
    onChange(suggestion.name);
    saveToRecent(suggestion);
    onSelect?.(suggestion);
    setIsOpen(false);
    setSelectedIndex(-1);
  };

  // Keyboard navigation
  useKeyboardNav({
    onArrowUp: () => {
      setSelectedIndex((prev) => (prev <= 0 ? allSuggestions.length - 1 : prev - 1));
    },
    onArrowDown: () => {
      setSelectedIndex((prev) => (prev >= allSuggestions.length - 1 ? 0 : prev + 1));
    },
    onEnter: () => {
      if (selectedIndex >= 0 && allSuggestions[selectedIndex]) {
        handleSelect(allSuggestions[selectedIndex]);
      }
    },
    onEscape: () => {
      setIsOpen(false);
      setSelectedIndex(-1);
    },
    enabled: isOpen,
  });

  return (
    <div className="relative">
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => setIsOpen(true)}
        onBlur={() => setTimeout(() => setIsOpen(false), 200)}
        placeholder={placeholder}
        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
      />

      <AnimatePresence>
        {isOpen && (value || recentSearches.length > 0) && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="absolute z-50 w-full mt-2 bg-white rounded-xl shadow-2xl border border-gray-200 max-h-96 overflow-y-auto"
          >
            {/* Recent Searches */}
            {!value && recentSearches.length > 0 && (
              <div className="p-2">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Recent Searches
                </div>
                {recentSearches.map((search, index) => (
                  <SuggestionItem
                    key={`recent-${search.id}`}
                    icon={<Clock className="w-4 h-4" />}
                    primary={search.name}
                    secondary={search.country}
                    onClick={() => handleSelect(search)}
                  />
                ))}
              </div>
            )}

            {/* Cities */}
            {groupedSuggestions.cities.length > 0 && (
              <div className="p-2">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Cities
                </div>
                {groupedSuggestions.cities.map((suggestion) => (
                  <SuggestionItem
                    key={suggestion.id}
                    icon={<MapPin className="w-4 h-4" />}
                    primary={highlightMatch(suggestion.name, value)}
                    secondary={suggestion.country}
                    onClick={() => handleSelect(suggestion)}
                  />
                ))}
              </div>
            )}

            {/* Airports */}
            {groupedSuggestions.airports.length > 0 && (
              <div className="p-2">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Airports
                </div>
                {groupedSuggestions.airports.map((suggestion) => (
                  <SuggestionItem
                    key={suggestion.id}
                    icon={<Plane className="w-4 h-4" />}
                    primary={highlightMatch(suggestion.name, value)}
                    secondary={`${suggestion.code ? `${suggestion.code} · ` : ''}${suggestion.country}`}
                    onClick={() => handleSelect(suggestion)}
                  />
                ))}
              </div>
            )}

            {/* Landmarks */}
            {groupedSuggestions.landmarks.length > 0 && (
              <div className="p-2">
                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Landmarks
                </div>
                {groupedSuggestions.landmarks.map((suggestion) => (
                  <SuggestionItem
                    key={suggestion.id}
                    icon={<Landmark className="w-4 h-4" />}
                    primary={highlightMatch(suggestion.name, value)}
                    secondary={suggestion.city || suggestion.country}
                    onClick={() => handleSelect(suggestion)}
                  />
                ))}
              </div>
            )}

            {/* No Results / Did You Mean */}
            {value && allSuggestions.length === 0 && !loading && (
              <div className="p-4 text-center text-gray-500">
                <p className="text-sm">No results found</p>
                <p className="text-xs mt-1">
                  Did you mean:{' '}
                  <span className="text-primary-600 cursor-pointer hover:underline">
                    Paris
                  </span>
                  ?
                </p>
              </div>
            )}

            {/* Loading State */}
            {loading && (
              <div className="p-4 text-center text-gray-500">
                <p className="text-sm">Searching...</p>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
