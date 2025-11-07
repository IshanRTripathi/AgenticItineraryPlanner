import type { Coordinates } from '../types/dto';

// Google Maps type declarations
declare global {
  interface Window {
    google: any;
  }
}

export interface GeocodingResult {
  coordinates: Coordinates;
  formattedAddress: string;
  placeId?: string;
  types?: string[];
}

export interface GeocodingCacheEntry {
  coordinates: Coordinates;
  formattedAddress: string;
  placeId?: string;
  types?: string[];
  timestamp: number;
}

export class GeocodingService {
  private geocoder: any | null = null;
  private cache = new Map<string, GeocodingCacheEntry>();
  private readonly CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours

  constructor() {
    this.initializeGeocoder();
  }

  private initializeGeocoder(): void {
    if (typeof window !== 'undefined' && (window as any).google?.maps?.Geocoder) {
      this.geocoder = new (window as any).google.maps.Geocoder();
    } else {
      // Wait for Google Maps to load
      const checkGoogleMaps = () => {
        if ((window as any).google?.maps?.Geocoder) {
          this.geocoder = new (window as any).google.maps.Geocoder();
        } else {
          setTimeout(checkGoogleMaps, 100);
        }
      };
      checkGoogleMaps();
    }
  }

  /**
   * Geocode an address to coordinates
   */
  async geocodeAddress(address: string): Promise<GeocodingResult | null> {
    if (!address || address.trim() === '') {
      console.warn('[GeocodingService] Empty address provided');
      return null;
    }

    const normalizedAddress = address.trim().toLowerCase();
    
    // Check cache first
    const cached = this.getCachedResult(normalizedAddress);
    if (cached) {
      console.log('[GeocodingService] Using cached result for:', address);
      return {
        coordinates: cached.coordinates,
        formattedAddress: cached.formattedAddress,
        placeId: cached.placeId,
        types: cached.types,
      };
    }

    if (!this.geocoder) {
      console.error('[GeocodingService] Geocoder not initialized');
      return null;
    }

    try {
      console.log('[GeocodingService] Geocoding address:', address);
      
      const result = await new Promise<any[]>((resolve, reject) => {
        this.geocoder!.geocode({ address }, (results: any, status: any) => {
          if (status === 'OK' && results && results.length > 0) {
            resolve(results);
          } else {
            console.warn('[GeocodingService] Geocoding failed:', status, address);
            reject(new Error(`Geocoding failed: ${status}`));
          }
        });
      });

      const firstResult = result[0];
      const location = firstResult.geometry.location;
      const coordinates: Coordinates = {
        lat: location.lat(),
        lng: location.lng(),
      };

      const geocodingResult: GeocodingResult = {
        coordinates,
        formattedAddress: firstResult.formatted_address,
        placeId: firstResult.place_id,
        types: firstResult.types,
      };

      // Cache the result
      this.cacheResult(normalizedAddress, geocodingResult);

      console.log('[GeocodingService] Geocoding successful:', {
        address,
        coordinates,
        formattedAddress: geocodingResult.formattedAddress,
      });

      return geocodingResult;
    } catch (error) {
      console.error('[GeocodingService] Geocoding error:', error);
      return null;
    }
  }

  /**
   * Reverse geocode coordinates to address
   */
  async reverseGeocode(coordinates: Coordinates): Promise<GeocodingResult | null> {
    if (!this.geocoder) {
      console.error('[GeocodingService] Geocoder not initialized');
      return null;
    }

    const cacheKey = `reverse_${coordinates.lat}_${coordinates.lng}`;
    
    // Check cache first
    const cached = this.getCachedResult(cacheKey);
    if (cached) {
      console.log('[GeocodingService] Using cached reverse geocoding result');
      return {
        coordinates: cached.coordinates,
        formattedAddress: cached.formattedAddress,
        placeId: cached.placeId,
        types: cached.types,
      };
    }

    try {
      console.log('[GeocodingService] Reverse geocoding coordinates:', coordinates);
      
      const result = await new Promise<any[]>((resolve, reject) => {
        this.geocoder!.geocode({ location: coordinates }, (results: any, status: any) => {
          if (status === 'OK' && results && results.length > 0) {
            resolve(results);
          } else {
            console.warn('[GeocodingService] Reverse geocoding failed:', status);
            reject(new Error(`Reverse geocoding failed: ${status}`));
          }
        });
      });

      const firstResult = result[0];
      const geocodingResult: GeocodingResult = {
        coordinates,
        formattedAddress: firstResult.formatted_address,
        placeId: firstResult.place_id,
        types: firstResult.types,
      };

      // Cache the result
      this.cacheResult(cacheKey, geocodingResult);

      console.log('[GeocodingService] Reverse geocoding successful:', geocodingResult);
      return geocodingResult;
    } catch (error) {
      console.error('[GeocodingService] Reverse geocoding error:', error);
      return null;
    }
  }

  /**
   * Batch geocode multiple addresses
   */
  async batchGeocode(addresses: string[]): Promise<Map<string, GeocodingResult | null>> {
    console.log('[GeocodingService] Batch geocoding addresses:', addresses.length);
    
    const results = new Map<string, GeocodingResult | null>();
    
    // Process in batches to avoid rate limiting
    const batchSize = 5;
    for (let i = 0; i < addresses.length; i += batchSize) {
      const batch = addresses.slice(i, i + batchSize);
      const batchPromises = batch.map(async (address) => {
        const result = await this.geocodeAddress(address);
        return { address, result };
      });
      
      const batchResults = await Promise.all(batchPromises);
      batchResults.forEach(({ address, result }) => {
        results.set(address, result);
      });
      
      // Add delay between batches to respect rate limits
      if (i + batchSize < addresses.length) {
        await new Promise(resolve => setTimeout(resolve, 100));
      }
    }
    
    console.log('[GeocodingService] Batch geocoding completed:', results.size);
    return results;
  }

  /**
   * Get cached result if available and not expired
   */
  private getCachedResult(key: string): GeocodingCacheEntry | null {
    const cached = this.cache.get(key);
    if (cached && Date.now() - cached.timestamp < this.CACHE_EXPIRY_MS) {
      return cached;
    }
    
    // Remove expired entry
    if (cached) {
      this.cache.delete(key);
    }
    
    return null;
  }

  /**
   * Cache a geocoding result
   */
  private cacheResult(key: string, result: GeocodingResult): void {
    const cacheEntry: GeocodingCacheEntry = {
      ...result,
      timestamp: Date.now(),
    };
    
    this.cache.set(key, cacheEntry);
    
    // Limit cache size
    if (this.cache.size > 1000) {
      const oldestKey = this.cache.keys().next().value as string;
      this.cache.delete(oldestKey);
    }
  }

  /**
   * Clear the cache
   */
  clearCache(): void {
    this.cache.clear();
    console.log('[GeocodingService] Cache cleared');
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): { size: number; entries: string[] } {
    return {
      size: this.cache.size,
      entries: Array.from(this.cache.keys()),
    };
  }

  /**
   * Check if geocoder is ready
   */
  isReady(): boolean {
    return this.geocoder !== null;
  }
}

// Create singleton instance
export const geocodingService = new GeocodingService();

// Utility functions for common use cases
export const geocodingUtils = {
  /**
   * Extract coordinates from a component's location data
   */
  extractCoordinates(component: any): Coordinates | null {
    const coords = component?.location?.coordinates;

    // Try multiple possible paths for coordinates
    const paths = [
      () => component?.location?.coordinates,
      () => component?.coordinates,
      () => component?.position,
      () => component?.data?.location?.coordinates,
    ];

    for (const getCoords of paths) {
      try {
        const coords = getCoords();
        
        if (coords?.lat != null && coords?.lng != null) {
          const lat = typeof coords.lat === 'function' ? coords.lat() : coords.lat;
          const lng = typeof coords.lng === 'function' ? coords.lng() : coords.lng;
          
          if (typeof lat === 'number' && typeof lng === 'number' && 
              !isNaN(lat) && !isNaN(lng)) {
            return { lat, lng };
          }
        }
      } catch (e) {
        // Continue to next path
      }
    }
    
    return null;
  },

  /**
   * Get address from component location data
   */
  getAddress(component: any): string | null {
    return component?.location?.address || component?.location?.name || null;
  },

  /**
   * Get the best search query for geocoding a component
   */
  getSearchQuery(component: any): string {
    if (component?.location?.address && component.location.address.trim()) {
      return component.location.address;
    }
    
    if (component?.name && component?.location?.name) {
      return `${component.name}, ${component.location.name}`;
    }
    
    if (component?.name) {
      return component.name;
    }
    
    if (component?.location?.name) {
      return component.location.name;
    }
    
    return '';
  },

  /**
   * Check if component has valid coordinates
   */
  hasValidCoordinates(component: any): boolean {
    const coords = this.extractCoordinates(component);
    return coords !== null && 
           typeof coords.lat === 'number' && 
           typeof coords.lng === 'number' &&
           !isNaN(coords.lat) && 
           !isNaN(coords.lng) &&
           coords.lat >= -90 && coords.lat <= 90 &&
           coords.lng >= -180 && coords.lng <= 180;
  },

  /**
   * Get coordinates for a component, geocoding if necessary
   */
  async getCoordinatesForComponent(component: any): Promise<Coordinates | null> {
    // Skip transport/transit nodes entirely
    if (component?.type === 'transport' || component?.type === 'transit') {
      return null;
    }
    
    // First try to get existing coordinates
    const existingCoords = this.extractCoordinates(component);
    if (existingCoords) {
      return existingCoords;
    }

    // Try to geocode using the best search query
    const searchQuery = this.getSearchQuery(component);
    if (searchQuery) {
      const result = await geocodingService.geocodeAddress(searchQuery);
      if (result?.coordinates) {
        return result.coordinates;
      }
    }

    return null;
  },
};
