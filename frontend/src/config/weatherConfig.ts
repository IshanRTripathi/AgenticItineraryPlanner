/**
 * Weather API Configuration
 * 
 * TODO: Move API key to Google Cloud Secret Manager
 * Secret name: OPENWEATHER_API
 */

export const WEATHER_CONFIG = {
  // OpenWeatherMap API configuration
  API_KEY: import.meta.env.VITE_OPENWEATHER_API_KEY || '',
  BASE_URL: 'https://api.openweathermap.org/data/2.5',
  
  // API limits and settings
  RATE_LIMIT: {
    FREE_TIER_CALLS_PER_DAY: 1000,
    CALLS_PER_MINUTE: 60
  },
  
  // Cache settings
  CACHE: {
    TTL_MINUTES: 10, // Cache weather data for 10 minutes
    MAX_CACHE_SIZE: 50 // Maximum number of cities to cache
  },
  
  // Fallback settings
  FALLBACK: {
    ENABLED: true,
    USE_MOCK_DATA: true
  }
};

/**
 * Get API key from environment variables
 * In production, this will be set by Google Cloud Secret Manager via cloudbuild.yaml
 */
export function getWeatherApiKey(): string {
  const apiKey = WEATHER_CONFIG.API_KEY;
  
  if (!apiKey) {
    console.warn('OpenWeatherMap API key not found in environment variables. Weather data will use fallback.');
  }
  
  return apiKey;
}

/**
 * Check if we're within API rate limits
 */
export function isWithinRateLimit(callCount: number): boolean {
  return callCount < WEATHER_CONFIG.RATE_LIMIT.CALLS_PER_MINUTE;
}
