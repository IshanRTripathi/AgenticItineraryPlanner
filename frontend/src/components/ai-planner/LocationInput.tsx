/**
 * Location Input with Nominatim Autocomplete
 * Uses free OpenStreetMap Nominatim API (no API key required)
 */

import { useState, useEffect, useRef } from 'react';
import { Input } from '@/components/ui/input';
import { MapPin } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface LocationInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

interface NominatimPlace {
  display_name: string;
  address?: {
    city?: string;
    town?: string;
    village?: string;
    state?: string;
    country?: string;
    country_code?: string;
  };
  name?: string;
}

export function LocationInput({ value, onChange, placeholder }: LocationInputProps) {
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [isFocused, setIsFocused] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const abortController = useRef<AbortController | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const formatPlaceLabel = (place: NominatimPlace): string => {
    const addr = place?.address || {};
    const locality = addr.city || addr.town || addr.village || place?.name;
    const state = addr.state;
    const country = addr.country || (addr.country_code ? addr.country_code.toUpperCase() : undefined);
    
    if (locality && state && country) return `${locality}, ${state}, ${country}`;
    if (locality && country) return `${locality}, ${country}`;
    if (state && country) return `${state}, ${country}`;
    return country || place?.display_name || '';
  };

  useEffect(() => {
    if (!isFocused || !value || value.length < 2) {
      setSuggestions([]);
      return;
    }

    const handle = setTimeout(async () => {
      try {
        if (abortController.current) abortController.current.abort();
        abortController.current = new AbortController();

        const res = await fetch(
          `https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&q=${encodeURIComponent(value)}&limit=5`,
          {
            signal: abortController.current.signal,
            headers: { 'Accept-Language': navigator.language || 'en' }
          }
        );

        const data: NominatimPlace[] = await res.json();
        const names = data.map((place) => formatPlaceLabel(place));
        const unique = Array.from(new Set(names)).slice(0, 5);
        setSuggestions(unique);
        setActiveIndex(-1);
      } catch (error) {
        // Ignore abort errors
        if (error instanceof Error && error.name !== 'AbortError') {
          console.error('Location search error:', error);
        }
      }
    }, 300);

    return () => clearTimeout(handle);
  }, [value, isFocused]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (suggestions.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex((prev) => (prev > 0 ? prev - 1 : -1));
    } else if (e.key === 'Enter' && activeIndex >= 0) {
      e.preventDefault();
      onChange(suggestions[activeIndex]);
      setSuggestions([]);
      setIsFocused(false);
      inputRef.current?.blur();
    } else if (e.key === 'Escape') {
      setSuggestions([]);
      setIsFocused(false);
    }
  };

  const handleSelectSuggestion = (suggestion: string) => {
    onChange(suggestion);
    setSuggestions([]);
    setIsFocused(false);
    inputRef.current?.blur();
  };

  return (
    <div className="relative">
      <div className="relative">
        <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <Input
          ref={inputRef}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setIsFocused(true)}
          onBlur={() => {
            // Delay to allow click on suggestions
            setTimeout(() => setIsFocused(false), 200);
          }}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          className="pl-10"
        />
      </div>

      <AnimatePresence>
        {isFocused && suggestions.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="absolute z-50 w-full mt-2 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden"
          >
            {suggestions.map((suggestion, index) => (
              <button
                key={index}
                onClick={() => handleSelectSuggestion(suggestion)}
                className={`w-full px-4 py-3 text-left text-sm hover:bg-gray-100 transition-colors flex items-center gap-2 ${
                  index === activeIndex ? 'bg-gray-100' : ''
                }`}
              >
                <MapPin className="w-4 h-4 text-gray-400 flex-shrink-0" />
                <span className="truncate">{suggestion}</span>
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
