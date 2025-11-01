/**
 * Weather Service
 * Integrates with OpenWeatherMap API for real weather data
 * Implements localStorage caching with 1-day TTL
 */

interface WeatherData {
  day: string;
  icon: string;
  high: number;
  low: number;
  description: string;
  date: string;
}

interface WeatherCache {
  data: WeatherData[];
  timestamp: number;
  destination: string;
}

// Cache configuration
const CACHE_KEY_PREFIX = 'weather_cache_';
const CACHE_TTL = 24 * 60 * 60 * 1000; // 1 day in milliseconds

interface WeatherApiResponse {
  list: Array<{
    dt: number;
    main: {
      temp_max: number;
      temp_min: number;
    };
    weather: Array<{
      main: string;
      description: string;
      icon: string;
    }>;
  }>;
}

/**
 * Map OpenWeatherMap icon codes to Lucide icon names
 */
function mapWeatherIcon(iconCode: string): string {
  const iconMap: Record<string, string> = {
    '01d': 'Sun',
    '01n': 'Moon',
    '02d': 'CloudSun',
    '02n': 'CloudMoon',
    '03d': 'Cloud',
    '03n': 'Cloud',
    '04d': 'Cloudy',
    '04n': 'Cloudy',
    '09d': 'CloudRain',
    '09n': 'CloudRain',
    '10d': 'CloudRain',
    '10n': 'CloudRain',
    '11d': 'CloudLightning',
    '11n': 'CloudLightning',
    '13d': 'CloudSnow',
    '13n': 'CloudSnow',
    '50d': 'CloudFog',
    '50n': 'CloudFog',
  };
  return iconMap[iconCode] || 'Cloud';
}

/**
 * Convert Kelvin to Fahrenheit
 */
function kelvinToFahrenheit(kelvin: number): number {
  return Math.round((kelvin - 273.15) * 9 / 5 + 32);
}

/**
 * Get day name from date
 */
function getDayName(date: Date): string {
  const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  return days[date.getDay()];
}

/**
 * Get cache key for a destination
 */
function getCacheKey(destination: string): string {
  return `${CACHE_KEY_PREFIX}${destination.toLowerCase().replace(/\s+/g, '_')}`;
}

/**
 * Get cached weather data if valid
 */
function getCachedWeather(destination: string, days: number): WeatherData[] | null {
  try {
    const cacheKey = getCacheKey(destination);
    const cached = localStorage.getItem(cacheKey);

    if (!cached) {
      return null;
    }

    const cacheData: WeatherCache = JSON.parse(cached);
    const now = Date.now();

    // Check if cache is still valid (within TTL)
    if (now - cacheData.timestamp > CACHE_TTL) {
      console.log('[WeatherService] Cache expired for', destination);
      localStorage.removeItem(cacheKey);
      return null;
    }

    // Check if destination matches
    if (cacheData.destination.toLowerCase() !== destination.toLowerCase()) {
      return null;
    }

    console.log('[WeatherService] Using cached weather for', destination);
    return cacheData.data.slice(0, days);
  } catch (error) {
    console.error('[WeatherService] Error reading cache:', error);
    return null;
  }
}

/**
 * Save weather data to cache
 */
function setCachedWeather(destination: string, data: WeatherData[]): void {
  try {
    const cacheKey = getCacheKey(destination);
    const cacheData: WeatherCache = {
      data,
      timestamp: Date.now(),
      destination,
    };

    localStorage.setItem(cacheKey, JSON.stringify(cacheData));
    console.log('[WeatherService] Cached weather for', destination);
  } catch (error) {
    console.error('[WeatherService] Error saving cache:', error);
  }
}

/**
 * Clear all weather cache
 */
export function clearWeatherCache(): void {
  try {
    const keys = Object.keys(localStorage);
    let cleared = 0;
    keys.forEach(key => {
      if (key.startsWith(CACHE_KEY_PREFIX)) {
        localStorage.removeItem(key);
        cleared++;
      }
    });
    console.log(`[WeatherService] Cleared ${cleared} weather cache entries`);
  } catch (error) {
    console.error('[WeatherService] Error clearing cache:', error);
  }
}

// Expose clearWeatherCache to window for debugging
if (typeof window !== 'undefined') {
  (window as any).clearWeatherCache = clearWeatherCache;
}

/**
 * Fetch weather forecast for a destination
 * Uses OpenWeatherMap 5-day forecast API with localStorage caching
 */
export async function fetchWeatherForecast(
  destination: string,
  days: number = 7
): Promise<WeatherData[]> {
  try {
    // Check cache first
    const cachedData = getCachedWeather(destination, days);
    if (cachedData) {
      return cachedData;
    }

    // Note: In production, API key should be in environment variables
    const API_KEY = import.meta.env.VITE_OPENWEATHER_API_KEY || 'demo';

    if (API_KEY === 'demo') {
      console.warn('[WeatherService] No API key configured, using fallback data');
      return getFallbackWeather(days);
    }

    // OpenWeatherMap 5-day forecast returns 40 data points (5 days * 8 intervals per day)
    // Use units=imperial to get Fahrenheit directly
    const response = await fetch(
      `https://api.openweathermap.org/data/2.5/forecast?q=${encodeURIComponent(destination)}&appid=${API_KEY}&units=imperial`
    );

    if (!response.ok) {
      throw new Error(`Weather API error: ${response.status}`);
    }

    const data: WeatherApiResponse = await response.json();

    // Group by day and get daily high/low
    const dailyData = new Map<string, WeatherData>();

    data.list.forEach((item) => {
      const date = new Date(item.dt * 1000);
      const dateKey = date.toISOString().split('T')[0];

      if (!dailyData.has(dateKey)) {
        dailyData.set(dateKey, {
          day: getDayName(date),
          icon: mapWeatherIcon(item.weather[0].icon),
          high: Math.round(item.main.temp_max),
          low: Math.round(item.main.temp_min),
          description: item.weather[0].description,
          date: dateKey,
        });
      } else {
        const existing = dailyData.get(dateKey)!;
        existing.high = Math.max(existing.high, Math.round(item.main.temp_max));
        existing.low = Math.min(existing.low, Math.round(item.main.temp_min));
      }
    });

    const result = Array.from(dailyData.values());

    // OpenWeatherMap 5-day forecast only returns ~5 days
    // If we need more days, repeat the first few days with updated day names
    let finalResult: WeatherData[];
    if (result.length < days && result.length > 0) {
      console.log(`[WeatherService] Got ${result.length} days from API, padding to ${days} days`);

      const missingDays = days - result.length;
      const lastDate = new Date(result[result.length - 1].date);

      // Repeat the first few days of real data for the missing days
      const paddedDays: WeatherData[] = [];
      for (let i = 0; i < missingDays; i++) {
        // Cycle through the first 3 days of real data
        const sourceDay = result[i % Math.min(3, result.length)];
        const nextDate = new Date(lastDate);
        nextDate.setDate(lastDate.getDate() + i + 1);

        paddedDays.push({
          ...sourceDay,
          day: getDayName(nextDate),
          date: nextDate.toISOString().split('T')[0],
        });
      }

      finalResult = [...result, ...paddedDays];
    } else if (result.length === 0) {
      // No API data, use fallback
      finalResult = getFallbackWeather(days);
    } else {
      finalResult = result.slice(0, days);
    }

    console.log(`[WeatherService] Returning ${finalResult.length} days of weather for ${destination}`);

    // Cache the result
    setCachedWeather(destination, finalResult);

    return finalResult;
  } catch (error) {
    console.error('[WeatherService] Failed to fetch weather:', error);
    return getFallbackWeather(days);
  }
}

/**
 * Fallback weather data when API is unavailable
 */
function getFallbackWeather(days: number): WeatherData[] {
  const fallbackData: WeatherData[] = [
    { day: 'Mon', icon: 'Sun', high: 75, low: 62, description: 'Sunny', date: '' },
    { day: 'Tue', icon: 'Cloud', high: 72, low: 60, description: 'Partly Cloudy', date: '' },
    { day: 'Wed', icon: 'CloudRain', high: 68, low: 58, description: 'Light Rain', date: '' },
    { day: 'Thu', icon: 'Sun', high: 76, low: 63, description: 'Sunny', date: '' },
    { day: 'Fri', icon: 'Cloud', high: 73, low: 61, description: 'Cloudy', date: '' },
    { day: 'Sat', icon: 'Sun', high: 78, low: 64, description: 'Clear', date: '' },
    { day: 'Sun', icon: 'CloudRain', high: 70, low: 59, description: 'Showers', date: '' },
  ];

  return fallbackData.slice(0, days);
}

/**
 * Get weather icon component name
 */
export function getWeatherIconName(iconName: string): string {
  return iconName;
}

/**
 * Weather service object for compatibility with WeatherWidget
 */
export const weatherService = {
  async getWeatherForCity(city: string) {
    try {
      // Check cache for current weather (1 hour TTL)
      const currentWeatherCacheKey = `${CACHE_KEY_PREFIX}current_${city.toLowerCase().replace(/\s+/g, '_')}`;
      const cached = localStorage.getItem(currentWeatherCacheKey);

      if (cached) {
        const cacheData = JSON.parse(cached);
        const now = Date.now();

        // Use shorter TTL for current weather (1 hour)
        if (now - cacheData.timestamp < 60 * 60 * 1000) {
          console.log('[WeatherService] Using cached current weather for', city);
          return cacheData.data;
        }
      }

      const API_KEY = import.meta.env.VITE_OPENWEATHER_API_KEY || 'demo';

      if (API_KEY === 'demo') {
        throw new Error('Weather API key not configured');
      }

      const response = await fetch(
        `https://api.openweathermap.org/data/2.5/weather?q=${encodeURIComponent(city)}&appid=${API_KEY}&units=metric`
      );

      if (!response.ok) {
        throw new Error(`Weather API error: ${response.status}`);
      }

      const data = await response.json();

      const weatherData = {
        city: data.name,
        temperature: Math.round(data.main.temp),
        condition: data.weather[0].main,
        description: data.weather[0].description,
        humidity: data.main.humidity,
        windSpeed: Math.round(data.wind.speed * 3.6), // Convert m/s to km/h
      };

      // Cache the result
      localStorage.setItem(currentWeatherCacheKey, JSON.stringify({
        data: weatherData,
        timestamp: Date.now(),
      }));
      console.log('[WeatherService] Cached current weather for', city);

      return weatherData;
    } catch (error) {
      console.error('[WeatherService] Failed to fetch current weather:', error);
      throw error;
    }
  },
};
