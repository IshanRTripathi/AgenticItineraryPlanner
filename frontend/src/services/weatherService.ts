import { getWeatherApiKey } from '../config/weatherConfig';

export interface WeatherData {
  city: string;
  temperature: number;
  condition: string;
  humidity: number;
  windSpeed: number;
  lastUpdated: string;
  description: string;
  icon: string;
}

export interface OpenWeatherResponse {
  name: string;
  main: {
    temp: number;
    humidity: number;
  };
  weather: Array<{
    main: string;
    description: string;
    icon: string;
  }>;
  wind: {
    speed: number;
  };
  dt: number;
}

class WeatherService {
  private apiKey: string;
  private baseUrl = 'https://api.openweathermap.org/data/2.5';

  constructor() {
    // Get API key from configuration
    this.apiKey = getWeatherApiKey();
  }

  /**
   * Fetch weather data for a single city
   */
  async getWeatherForCity(cityName: string): Promise<WeatherData> {
    try {
      const response = await fetch(
        `${this.baseUrl}/weather?q=${encodeURIComponent(cityName)}&appid=${this.apiKey}&units=metric`
      );

      if (!response.ok) {
        throw new Error(`Weather API error: ${response.status} ${response.statusText}`);
      }

      const data: OpenWeatherResponse = await response.json();
      
      return this.transformWeatherData(data);
    } catch (error) {
      console.error(`Failed to fetch weather for ${cityName}:`, error);
      // Return fallback data
      return this.getFallbackWeatherData(cityName);
    }
  }

  /**
   * Fetch weather data for multiple cities
   */
  async getWeatherForCities(cityNames: string[]): Promise<WeatherData[]> {
    const weatherPromises = cityNames.map(city => this.getWeatherForCity(city));
    
    try {
      const results = await Promise.allSettled(weatherPromises);
      
      return results.map((result, index) => {
        if (result.status === 'fulfilled') {
          return result.value;
        } else {
          console.error(`Failed to fetch weather for ${cityNames[index]}:`, result.reason);
          return this.getFallbackWeatherData(cityNames[index]);
        }
      });
    } catch (error) {
      console.error('Error fetching weather for multiple cities:', error);
      return cityNames.map(city => this.getFallbackWeatherData(city));
    }
  }

  /**
   * Transform OpenWeather API response to our WeatherData format
   */
  private transformWeatherData(data: OpenWeatherResponse): WeatherData {
    const weather = data.weather[0];
    
    return {
      city: data.name,
      temperature: Math.round(data.main.temp),
      condition: this.mapWeatherCondition(weather.main),
      humidity: data.main.humidity,
      windSpeed: Math.round(data.wind.speed * 3.6), // Convert m/s to km/h
      lastUpdated: new Date(data.dt * 1000).toLocaleTimeString(),
      description: weather.description,
      icon: weather.icon
    };
  }

  /**
   * Map OpenWeather condition codes to our condition strings
   */
  private mapWeatherCondition(condition: string): string {
    const conditionMap: { [key: string]: string } = {
      'Clear': 'sunny',
      'Clouds': 'cloudy',
      'Rain': 'rainy',
      'Drizzle': 'rainy',
      'Thunderstorm': 'rainy',
      'Snow': 'snowy',
      'Mist': 'cloudy',
      'Fog': 'cloudy',
      'Haze': 'cloudy',
      'Dust': 'cloudy',
      'Sand': 'cloudy',
      'Ash': 'cloudy',
      'Squall': 'rainy',
      'Tornado': 'rainy'
    };

    return conditionMap[condition] || 'cloudy';
  }

  /**
   * Get fallback weather data when API fails
   */
  private getFallbackWeatherData(cityName: string): WeatherData {
    const conditions = ['sunny', 'partly-cloudy', 'cloudy', 'rainy'];
    const randomCondition = conditions[Math.floor(Math.random() * conditions.length)];
    
    return {
      city: cityName,
      temperature: Math.round(15 + Math.random() * 20),
      condition: randomCondition,
      humidity: Math.round(40 + Math.random() * 40),
      windSpeed: Math.round(5 + Math.random() * 15),
      lastUpdated: new Date().toLocaleTimeString(),
      description: 'Weather data unavailable',
      icon: '01d' // Default sunny icon
    };
  }

  /**
   * Get weather icon emoji based on condition
   */
  getWeatherIcon(condition: string): string {
    const iconMap: { [key: string]: string } = {
      'sunny': '☀️',
      'partly-cloudy': '⛅',
      'cloudy': '☁️',
      'rainy': '🌧️',
      'snowy': '❄️',
      'stormy': '⛈️'
    };

    return iconMap[condition] || '☁️';
  }

  /**
   * Get weather icon emoji based on OpenWeather icon code
   */
  getWeatherIconFromCode(iconCode: string): string {
    const iconMap: { [key: string]: string } = {
      '01d': '☀️', '01n': '🌙', // clear sky
      '02d': '⛅', '02n': '☁️', // few clouds
      '03d': '☁️', '03n': '☁️', // scattered clouds
      '04d': '☁️', '04n': '☁️', // broken clouds
      '09d': '🌧️', '09n': '🌧️', // shower rain
      '10d': '🌦️', '10n': '🌧️', // rain
      '11d': '⛈️', '11n': '⛈️', // thunderstorm
      '13d': '❄️', '13n': '❄️', // snow
      '50d': '🌫️', '50n': '🌫️'  // mist
    };

    return iconMap[iconCode] || '☁️';
  }
}

// Export singleton instance
export const weatherService = new WeatherService();
