/**
 * usePlacesAutocomplete Hook
 * Provides location autocomplete functionality with Google Places API
 */

import { useState, useEffect } from 'react';
import { useDebounce } from '../useDebounce';

export interface PlaceSuggestion {
  id: string;
  name: string;
  type: 'city' | 'airport' | 'landmark';
  country: string;
  code?: string; // For airports
  city?: string; // For landmarks
  coordinates?: {
    lat: number;
    lng: number;
  };
}

interface GroupedSuggestions {
  cities: PlaceSuggestion[];
  airports: PlaceSuggestion[];
  landmarks: PlaceSuggestion[];
}

export function usePlacesAutocomplete(query: string) {
  const [suggestions, setSuggestions] = useState<PlaceSuggestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [recentSearches, setRecentSearches] = useState<PlaceSuggestion[]>([]);
  
  const debouncedQuery = useDebounce(query, 300);

  // Load recent searches from localStorage
  useEffect(() => {
    const stored = localStorage.getItem('recentLocationSearches');
    if (stored) {
      try {
        setRecentSearches(JSON.parse(stored).slice(0, 5));
      } catch (e) {
        console.error('Failed to parse recent searches', e);
      }
    }
  }, []);

  // Helper function to determine place type
  const determineType = (types: string[]): PlaceSuggestion['type'] => {
    if (types.includes('airport')) return 'airport';
    if (types.includes('locality') || types.includes('administrative_area_level_1')) return 'city';
    if (types.includes('point_of_interest') || types.includes('establishment')) return 'landmark';
    return 'city'; // default
  };

  // Fetch suggestions from Google Places API
  useEffect(() => {
    if (!debouncedQuery || debouncedQuery.length < 2) {
      setSuggestions([]);
      return;
    }

    setLoading(true);

    // Check if Google Maps is loaded
    if (typeof window === 'undefined' || !(window as any).google?.maps?.places) {
      // Fallback to mock data if Google Maps is not available
      const mockSuggestions: PlaceSuggestion[] = [
        { id: '1', name: 'Paris', type: 'city' as const, country: 'France' },
        { id: '2', name: 'Paris Charles de Gaulle Airport', type: 'airport' as const, country: 'France', code: 'CDG' },
        { id: '3', name: 'Eiffel Tower', type: 'landmark' as const, country: 'France', city: 'Paris' },
      ].filter(s => s.name.toLowerCase().includes(debouncedQuery.toLowerCase()));
      
      setSuggestions(mockSuggestions);
      setLoading(false);
      return;
    }

    const googleMaps = (window as any).google.maps;
    const service = new googleMaps.places.AutocompleteService();
    
    service.getPlacePredictions(
      {
        input: debouncedQuery,
        types: ['(cities)', 'airport', 'establishment'],
      },
      (predictions: any, status: any) => {
        setLoading(false);
        
        if (status === googleMaps.places.PlacesServiceStatus.OK && predictions) {
          const parsed: PlaceSuggestion[] = predictions.map((prediction) => {
            const types = prediction.types || [];
            const type = determineType(types);
            
            return {
              id: prediction.place_id,
              name: prediction.structured_formatting.main_text,
              type,
              country: prediction.structured_formatting.secondary_text || '',
              code: type === 'airport' ? prediction.structured_formatting.main_text.match(/\(([A-Z]{3})\)/)?.[1] : undefined,
              city: type === 'landmark' ? prediction.structured_formatting.secondary_text.split(',')[0] : undefined,
            };
          });
          
          setSuggestions(parsed);
        } else {
          setSuggestions([]);
        }
      }
    );
  }, [debouncedQuery]);

  // Group suggestions by type
  const groupedSuggestions: GroupedSuggestions = {
    cities: suggestions.filter(s => s.type === 'city').slice(0, 5).sort((a, b) => a.name.localeCompare(b.name)),
    airports: suggestions.filter(s => s.type === 'airport').slice(0, 5).sort((a, b) => a.name.localeCompare(b.name)),
    landmarks: suggestions.filter(s => s.type === 'landmark').slice(0, 5).sort((a, b) => a.name.localeCompare(b.name)),
  };

  // Save to recent searches
  const saveToRecent = (suggestion: PlaceSuggestion) => {
    const updated = [suggestion, ...recentSearches.filter(s => s.id !== suggestion.id)].slice(0, 5);
    setRecentSearches(updated);
    localStorage.setItem('recentLocationSearches', JSON.stringify(updated));
  };

  return {
    suggestions,
    groupedSuggestions,
    recentSearches,
    loading,
    saveToRecent,
  };
}
