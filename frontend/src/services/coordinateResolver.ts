/**
 * Smart Coordinate Resolution Service
 * Implements hierarchical coordinate strategy to minimize API calls
 * and ensure map always renders with fallback coordinates
 */

import type { Coordinates, NodeLocation } from '@/types/dto';
import { geocodingService } from './geocodingService';

export interface CoordinateResolutionResult {
  coordinates: Coordinates;
  confidence: 'exact' | 'approximate' | 'city' | 'fallback';
  source: 'provided' | 'geocoded' | 'city-center' | 'default';
  cached: boolean;
}

interface CachedResult extends CoordinateResolutionResult {
  timestamp: number;
}

export interface CityCoordinates {
  name: string;
  coordinates: Coordinates;
  country?: string;
}

/**
 * Smart Coordinate Resolver
 * Resolves coordinates using a hierarchical fallback strategy
 */
export class SmartCoordinateResolver {
  private cityCache = new Map<string, Coordinates>();
  private locationCache = new Map<string, CachedResult>();
  private readonly CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours

  // Common city coordinates (fallback database)
  private readonly CITY_COORDINATES: Record<string, Coordinates> = {
    // India
    'delhi': { lat: 28.6139, lng: 77.2090 },
    'mumbai': { lat: 19.0760, lng: 72.8777 },
    'bangalore': { lat: 12.9716, lng: 77.5946 },
    'kolkata': { lat: 22.5726, lng: 88.3639 },
    'chennai': { lat: 13.0827, lng: 80.2707 },
    'hyderabad': { lat: 17.3850, lng: 78.4867 },
    'pune': { lat: 18.5204, lng: 73.8567 },
    'jaipur': { lat: 26.9124, lng: 75.7873 },
    'goa': { lat: 15.2993, lng: 74.1240 },

    // International
    'tokyo': { lat: 35.6762, lng: 139.6503 },
    'paris': { lat: 48.8566, lng: 2.3522 },
    'london': { lat: 51.5074, lng: -0.1278 },
    'new york': { lat: 40.7128, lng: -74.0060 },
    'dubai': { lat: 25.2048, lng: 55.2708 },
    'singapore': { lat: 1.3521, lng: 103.8198 },
    'bangkok': { lat: 13.7563, lng: 100.5018 },
    'sydney': { lat: -33.8688, lng: 151.2093 },
    'rome': { lat: 41.9028, lng: 12.4964 },
    'barcelona': { lat: 41.3851, lng: 2.1734 },
  };

  /**
   * Main resolution method - tries multiple strategies in order
   */
  async resolve(
    location: NodeLocation | undefined,
    cityName: string
  ): Promise<CoordinateResolutionResult> {
    console.log('[CoordinateResolver] ===== RESOLVING COORDINATES =====');
    console.log('[CoordinateResolver] Location:', location);
    console.log('[CoordinateResolver] City context:', cityName);

    // Strategy 1: Use provided coordinates if valid
    if (location?.coordinates && this.isValidCoordinates(location.coordinates)) {
      console.log('[CoordinateResolver] ✓ Strategy 1: Using provided coordinates');
      console.log('[CoordinateResolver]   Name:', location.name);
      console.log('[CoordinateResolver]   Coords:', location.coordinates);
      return {
        coordinates: location.coordinates,
        confidence: 'exact',
        source: 'provided',
        cached: false,
      };
    }

    // Strategy 2: Check if location is generic (skip API call)
    if (location?.name && this.isGenericLocation(location.name)) {
      console.log('[CoordinateResolver] ✓ Strategy 2: Generic location detected');
      console.log('[CoordinateResolver]   Using city center for:', location.name);
      const cityCoords = await this.getCityCoordinates(cityName);
      console.log('[CoordinateResolver]   City coords:', cityCoords);
      return {
        coordinates: cityCoords,
        confidence: 'city',
        source: 'city-center',
        cached: false,
      };
    }

    // Strategy 3: Check cache
    const cacheKey = this.getCacheKey(location?.name, cityName);
    const cached = this.locationCache.get(cacheKey);
    if (cached) {
      // Check if cache is still valid
      const isExpired = Date.now() - cached.timestamp > this.CACHE_EXPIRY_MS;
      if (!isExpired) {
        console.log('[CoordinateResolver] ✓ Strategy 3: Using cached result');
        console.log('[CoordinateResolver]   Cache key:', cacheKey);
        console.log('[CoordinateResolver]   Cached coords:', cached.coordinates);
        return { ...cached, cached: true };
      } else {
        // Remove expired entry
        this.locationCache.delete(cacheKey);
        console.log('[CoordinateResolver] Cache expired for:', location?.name);
      }
    }

    // Strategy 4: Geocode specific venue (only if it looks like a real place)
    if (location?.name && this.isSpecificVenue(location.name)) {
      try {
        const geocoded = await this.geocodeLocation(location.name, cityName);
        if (geocoded) {
          const result: CoordinateResolutionResult = {
            coordinates: geocoded,
            confidence: 'approximate',
            source: 'geocoded',
            cached: false,
          };
          // Cache with timestamp
          this.locationCache.set(cacheKey, { ...result, timestamp: Date.now() });
          console.log('[CoordinateResolver] Geocoded specific venue:', location.name);
          return result;
        }
      } catch (error) {
        console.warn('[CoordinateResolver] Geocoding failed:', error);
      }
    }

    // Strategy 5: Fallback to city center
    console.log('[CoordinateResolver] Falling back to city center for:', location?.name || 'unknown');
    const cityCoords = await this.getCityCoordinates(cityName);
    return {
      coordinates: cityCoords,
      confidence: 'city',
      source: 'city-center',
      cached: false,
    };
  }

  /**
   * Batch resolve multiple locations efficiently
   */
  async batchResolve(
    locations: Array<{ location: NodeLocation | undefined; cityName: string }>,
    onProgress?: (current: number, total: number) => void
  ): Promise<Map<string, CoordinateResolutionResult>> {
    const results = new Map<string, CoordinateResolutionResult>();

    // First pass: resolve from cache and provided coordinates
    const toGeocode: typeof locations = [];

    for (let i = 0; i < locations.length; i++) {
      const { location, cityName } = locations[i];
      const key = this.getCacheKey(location?.name, cityName);

      // Check provided coordinates
      if (location?.coordinates && this.isValidCoordinates(location.coordinates)) {
        results.set(key, {
          coordinates: location.coordinates,
          confidence: 'exact',
          source: 'provided',
          cached: false,
        });
        continue;
      }

      // Check cache
      const cached = this.locationCache.get(key);
      if (cached) {
        const isExpired = Date.now() - cached.timestamp > this.CACHE_EXPIRY_MS;
        if (!isExpired) {
          results.set(key, { ...cached, cached: true });
          continue;
        } else {
          this.locationCache.delete(key);
        }
      }

      // Check if generic
      if (location?.name && this.isGenericLocation(location.name)) {
        const cityCoords = await this.getCityCoordinates(cityName);
        results.set(key, {
          coordinates: cityCoords,
          confidence: 'city',
          source: 'city-center',
          cached: false,
        });
        continue;
      }

      // Needs geocoding
      if (location?.name && this.isSpecificVenue(location.name)) {
        toGeocode.push({ location, cityName });
      } else {
        // Fallback to city
        const cityCoords = await this.getCityCoordinates(cityName);
        results.set(key, {
          coordinates: cityCoords,
          confidence: 'city',
          source: 'city-center',
          cached: false,
        });
      }

      onProgress?.(i + 1, locations.length);
    }

    // Second pass: batch geocode remaining locations
    if (toGeocode.length > 0) {
      console.log('[CoordinateResolver] Batch geocoding', toGeocode.length, 'locations');

      // Process in small batches to avoid rate limiting
      const batchSize = 5;
      for (let i = 0; i < toGeocode.length; i += batchSize) {
        const batch = toGeocode.slice(i, i + batchSize);

        await Promise.all(
          batch.map(async ({ location, cityName }) => {
            const key = this.getCacheKey(location?.name, cityName);
            try {
              const coords = await this.geocodeLocation(location!.name!, cityName);
              if (coords) {
                const result: CoordinateResolutionResult = {
                  coordinates: coords,
                  confidence: 'approximate',
                  source: 'geocoded',
                  cached: false,
                };
                results.set(key, result);
                this.locationCache.set(key, { ...result, timestamp: Date.now() });
              } else {
                // Fallback to city
                const cityCoords = await this.getCityCoordinates(cityName);
                results.set(key, {
                  coordinates: cityCoords,
                  confidence: 'city',
                  source: 'city-center',
                  cached: false,
                });
              }
            } catch (error) {
              console.warn('[CoordinateResolver] Batch geocoding failed for:', location?.name);
              const cityCoords = await this.getCityCoordinates(cityName);
              results.set(key, {
                coordinates: cityCoords,
                confidence: 'city',
                source: 'city-center',
                cached: false,
              });
            }
          })
        );

        // Small delay between batches
        if (i + batchSize < toGeocode.length) {
          await new Promise(resolve => setTimeout(resolve, 200));
        }
      }
    }

    return results;
  }

  /**
   * Get city coordinates with fallback
   */
  private async getCityCoordinates(cityName: string): Promise<Coordinates> {
    // Normalize: lowercase, trim, and remove country suffix
    const normalized = cityName.toLowerCase().trim().split(',')[0].trim();

    // Check cache
    if (this.cityCache.has(normalized)) {
      return this.cityCache.get(normalized)!;
    }

    // Check hardcoded database
    if (this.CITY_COORDINATES[normalized]) {
      const coords = this.CITY_COORDINATES[normalized];
      this.cityCache.set(normalized, coords);
      return coords;
    }

    // Try geocoding the city (use original cityName for better results)
    try {
      console.log('[CoordinateResolver] Geocoding city:', cityName);
      const result = await geocodingService.geocodeAddress(cityName);
      if (result?.coordinates) {
        this.cityCache.set(normalized, result.coordinates);
        console.log('[CoordinateResolver] City geocoded successfully:', cityName, result.coordinates);
        return result.coordinates;
      }
    } catch (error) {
      console.warn('[CoordinateResolver] City geocoding failed:', cityName, error);
    }

    // Ultimate fallback: return default coordinates (center of India)
    // This ensures the map always renders even if geocoding fails
    console.warn('[CoordinateResolver] Failed to resolve coordinates for:', cityName, '- using default fallback');
    const defaultCoords = { lat: 20.5937, lng: 78.9629 }; // Center of India
    return defaultCoords;
  }

  /**
   * Geocode a specific location
   */
  private async geocodeLocation(
    locationName: string,
    cityName: string
  ): Promise<Coordinates | null> {
    try {
      // Construct search query
      const query = `${locationName}, ${cityName}`;
      const result = await geocodingService.geocodeAddress(query);
      return result?.coordinates || null;
    } catch (error) {
      console.error('[CoordinateResolver] Geocoding error:', error);
      return null;
    }
  }

  /**
   * Check if location name is generic (should skip API call)
   */
  private isGenericLocation(name: string): boolean {
    const genericPatterns = [
      /breakfast/i,
      /lunch/i,
      /dinner/i,
      /hotel/i,
      /accommodation/i,
      /lodging/i,
      /\bspot\b/i,
      /\barea\b/i,
      /\bzone\b/i,
      /morning activity/i,
      /afternoon activity/i,
      /evening activity/i,
      /free time/i,
      /rest/i,
      /check.?in/i,
      /check.?out/i,
    ];

    return genericPatterns.some(pattern => pattern.test(name));
  }

  /**
   * Check if location name looks like a specific venue
   */
  private isSpecificVenue(name: string): boolean {
    // Must be longer than generic terms
    if (name.length < 5) return false;

    // Must not be generic
    if (this.isGenericLocation(name)) return false;

    // Should contain specific indicators
    const specificIndicators = [
      /\bmuseum\b/i,
      /\btemple\b/i,
      /\bfort\b/i,
      /\bpalace\b/i,
      /\bpark\b/i,
      /\bmarket\b/i,
      /\bmall\b/i,
      /\brestaurant\b/i,
      /\bcafe\b/i,
      /\bbeach\b/i,
      /\btower\b/i,
      /\bgate\b/i,
      /\bsquare\b/i,
      /\bstation\b/i,
      /\bairport\b/i,
    ];

    return specificIndicators.some(pattern => pattern.test(name)) || name.length > 15;
  }

  /**
   * Validate coordinates
   */
  private isValidCoordinates(coords: Coordinates): boolean {
    return (
      coords &&
      typeof coords.lat === 'number' &&
      typeof coords.lng === 'number' &&
      !isNaN(coords.lat) &&
      !isNaN(coords.lng) &&
      coords.lat >= -90 &&
      coords.lat <= 90 &&
      coords.lng >= -180 &&
      coords.lng <= 180
    );
  }

  /**
   * Generate cache key
   */
  private getCacheKey(locationName: string | undefined, cityName: string): string {
    return `${locationName || 'unknown'}_${cityName}`.toLowerCase();
  }

  /**
   * Clear all caches
   */
  clearCache(): void {
    this.cityCache.clear();
    this.locationCache.clear();
    console.log('[CoordinateResolver] Cache cleared');
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): { cities: number; locations: number } {
    return {
      cities: this.cityCache.size,
      locations: this.locationCache.size,
    };
  }
}

// Export singleton instance
export const coordinateResolver = new SmartCoordinateResolver();
