/**
 * Weather Widget Component
 * Displays weather forecast for trip locations
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
        <CardHeader>
          <CardTitle className="text-base">Weather Forecast</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-4 text-muted-foreground text-sm">
            <Cloud className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p>Weather API not configured</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Weather Forecast</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center py-4">
            <Loader2 className="w-6 h-6 animate-spin text-primary" />
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error || !weather) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Weather Forecast</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-4 text-muted-foreground text-sm">
            <Cloud className="w-8 h-8 mx-auto mb-2 opacity-50" />
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
      <CardHeader>
        <CardTitle className="text-base flex items-center justify-between">
          <span>Weather Forecast</span>
          <Badge variant="secondary">{weather.city}</Badge>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <WeatherIcon className="w-12 h-12 text-primary" />
            <div>
              <div className="text-3xl font-bold">{weather.temperature}°C</div>
              <div className="text-sm text-muted-foreground">
                {weather.description}
              </div>
            </div>
          </div>

          <div className="space-y-2 text-sm">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Droplets className="w-4 h-4" />
              <span>{weather.humidity}% humidity</span>
            </div>
            <div className="flex items-center gap-2 text-muted-foreground">
              <Wind className="w-4 h-4" />
              <span>{weather.windSpeed} km/h wind</span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
