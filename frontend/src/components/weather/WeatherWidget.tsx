/**
 * Weather Widget Component
 * Displays weather forecast for trip locations
 * Task 14: Mobile-optimized with stacked layout and responsive icons
 */

import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Cloud,
  CloudRain,
  CloudSnow,
  Sun,
  Wind,
  Droplets,
  Loader2,
} from 'lucide-react';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { weatherService } from '@/services/weatherService';

interface WeatherWidgetProps {
  location: string;
  date: string;
}

const getWeatherIcon = (condition: string) => {
  const lower = condition.toLowerCase();
  if (lower.includes('rain')) return CloudRain;
  if (lower.includes('snow')) return CloudSnow;
  if (lower.includes('cloud')) return Cloud;
  return Sun;
};

export function WeatherWidget({ location, date }: WeatherWidgetProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const { data: weather, isLoading, error } = useQuery({
    queryKey: ['weather', location, date],
    queryFn: async () => {
      return await weatherService.getWeatherForCity(location);
    },
    enabled: !!location,
    staleTime: 30 * 60 * 1000, // 30 minutes
  });

  if (!import.meta.env.VITE_OPENWEATHER_API_KEY) {
    return (
      <Card>
        <CardHeader className="p-3 sm:p-6">
          <CardTitle className="text-sm sm:text-base">Weather Forecast</CardTitle>
        </CardHeader>
        <CardContent className="p-3 sm:p-6">
          <div className="text-center py-3 sm:py-4 text-muted-foreground text-xs sm:text-sm">
            <Cloud className="w-6 h-6 sm:w-8 sm:h-8 mx-auto mb-2 opacity-50" />
            <p>Weather API not configured</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (isLoading) {
    return (
      <Card>
        <CardHeader className="p-3 sm:p-6">
          <CardTitle className="text-sm sm:text-base">Weather Forecast</CardTitle>
        </CardHeader>
        <CardContent className="p-3 sm:p-6">
          <div className="flex items-center justify-center py-3 sm:py-4">
            <Loader2 className="w-5 h-5 sm:w-6 sm:h-6 animate-spin text-primary" />
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error || !weather) {
    return (
      <Card>
        <CardHeader className="p-3 sm:p-6">
          <CardTitle className="text-sm sm:text-base">Weather Forecast</CardTitle>
        </CardHeader>
        <CardContent className="p-3 sm:p-6">
          <div className="text-center py-3 sm:py-4 text-muted-foreground text-xs sm:text-sm">
            <Cloud className="w-6 h-6 sm:w-8 sm:h-8 mx-auto mb-2 opacity-50" />
            <p>Unable to load weather data</p>
            {error && <p className="text-xs mt-2">{String(error)}</p>}
          </div>
        </CardContent>
      </Card>
    );
  }

  const WeatherIcon = getWeatherIcon(weather.condition);

  return (
    <Card>
      <CardHeader className="p-3 sm:p-6">
        <CardTitle className="text-sm sm:text-base flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
          <span>Weather Forecast</span>
          <Badge variant="secondary" className="text-xs">{weather.city}</Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="p-3 sm:p-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          {/* Main weather display */}
          <div className="flex items-center gap-3 sm:gap-4">
            <WeatherIcon className="w-10 h-10 sm:w-12 sm:h-12 text-primary flex-shrink-0" />
            <div>
              <div className="text-2xl sm:text-3xl font-bold">{weather.temperature}°C</div>
              <div className="text-xs sm:text-sm text-muted-foreground">
                {weather.description}
              </div>
            </div>
          </div>

          {/* Weather details */}
          <div className="flex sm:flex-col gap-4 sm:gap-2 text-xs sm:text-sm w-full sm:w-auto">
            <div className="flex items-center gap-2 text-muted-foreground flex-1 sm:flex-initial">
              <Droplets className="w-4 h-4 flex-shrink-0" />
              <span>{weather.humidity}%</span>
              <span className="hidden sm:inline">humidity</span>
            </div>
            <div className="flex items-center gap-2 text-muted-foreground flex-1 sm:flex-initial">
              <Wind className="w-4 h-4 flex-shrink-0" />
              <span>{weather.windSpeed} km/h</span>
              <span className="hidden sm:inline">wind</span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
