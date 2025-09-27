import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { Progress } from '../../ui/progress';
import { Badge } from '../../ui/badge';
import { Share2, FileText, RefreshCw, MapPin, Cloud, Train, Plane, Car, Navigation, AlertTriangle, CheckCircle, Clock } from 'lucide-react';
import { AgentStatus } from '../shared/types';
import { weatherService, WeatherData } from '../../../services/weatherService';
import { extractCitiesFromItinerary, getTotalActivities, getTransportModes } from '../../../utils/itineraryUtils';

interface TripOverviewViewProps {
  tripData: any;
  agentStatuses: AgentStatus[];
  onShare: () => void;
  onExportPDF: () => void;
  destinations?: Array<{ id: string; name: string; nights: number; sleeping: boolean; notes: string }>;
}

// Using WeatherData from weatherService instead of local interface

interface TransportDelay {
  id: string;
  type: 'flight' | 'train' | 'bus' | 'car';
  route: string;
  scheduledTime: string;
  actualTime: string;
  delay: number; // in minutes
  status: 'on-time' | 'delayed' | 'cancelled';
  reason?: string;
}

interface TripAnalytics {
  uniqueCities: number;
  totalDistance: number;
  transportModes: { [key: string]: number };
  averageCostPerDay: number;
  totalActivities: number;
}

export function TripOverviewView({ tripData, agentStatuses, onShare, onExportPDF, destinations }: TripOverviewViewProps) {
  const { t } = useTranslation();
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [weatherData, setWeatherData] = useState<WeatherData[]>([]);
  const [transportDelays, setTransportDelays] = useState<TransportDelay[]>([]);
  const [tripAnalytics, setTripAnalytics] = useState<TripAnalytics | null>(null);
  const [weatherLoading, setWeatherLoading] = useState(false);
  const [weatherError, setWeatherError] = useState<string | null>(null);

  // Real weather data fetching
  const fetchWeatherData = async (): Promise<void> => {
    setWeatherLoading(true);
    setWeatherError(null);
    
    try {
      // Get unique city names from destinations
      let cities: string[] = [];
      
      if (destinations && destinations.length > 0) {
        // Use destinations directly - much simpler and more accurate
        // Extract unique city names to avoid duplicate API calls
        const uniqueCities = new Set(destinations.map(dest => dest.name));
        cities = Array.from(uniqueCities);
        console.log('Using destinations for weather:', destinations.map(dest => dest.name));
        console.log('Unique cities after deduplication:', cities);
      } else {
        // Fallback to extracting from itinerary data if no destinations available
        cities = extractCitiesFromItinerary(tripData);
        console.log('Fallback: extracting cities from itinerary:', cities);
      }
      
      if (cities.length === 0) {
        console.warn('No cities found in destinations or itinerary data');
        setWeatherData([]);
        return;
      }

      console.log('Fetching weather for unique cities:', cities);
      
      // Fetch weather data for all cities
      const weatherResults = await weatherService.getWeatherForCities(cities);
      setWeatherData(weatherResults);
      
      console.log('Weather data fetched successfully:', weatherResults);
    } catch (error) {
      console.error('Failed to fetch weather data:', error);
      setWeatherError('Failed to load weather data');
      // Set empty array on error
      setWeatherData([]);
    } finally {
      setWeatherLoading(false);
    }
  };

  const generateMockDelays = (): TransportDelay[] => {
    return [
      {
        id: '1',
        type: 'flight',
        route: 'Barcelona → Madrid',
        scheduledTime: '14:30',
        actualTime: '15:15',
        delay: 45,
        status: 'delayed',
        reason: 'Weather conditions'
      },
      {
        id: '2',
        type: 'train',
        route: 'Madrid → Seville',
        scheduledTime: '09:00',
        actualTime: '09:00',
        delay: 0,
        status: 'on-time'
      },
      {
        id: '3',
        type: 'bus',
        route: 'Seville → Valencia',
        scheduledTime: '16:00',
        actualTime: '16:20',
        delay: 20,
        status: 'delayed',
        reason: 'Traffic congestion'
      }
    ];
  };

  const generateTripAnalytics = (): TripAnalytics => {
    const days = tripData.itinerary?.days || [];
    const uniqueCities = new Set(days.map((day: any) => day.location)).size;
    
    return {
      uniqueCities,
      totalDistance: Math.round(800 + Math.random() * 400), // km - TODO: Calculate from real data
      transportModes: getTransportModes(tripData),
      averageCostPerDay: Math.round(120 + Math.random() * 80), // TODO: Calculate from real data
      totalActivities: getTotalActivities(tripData)
    };
  };

  const refreshData = async () => {
    setIsRefreshing(true);
    
    try {
      // Fetch real weather data
      await fetchWeatherData();
      
      // Simulate other API calls with delays
      await new Promise(resolve => setTimeout(resolve, 500));
      setTransportDelays(generateMockDelays());
      
      await new Promise(resolve => setTimeout(resolve, 500));
      setTripAnalytics(generateTripAnalytics());
    } catch (error) {
      console.error('Error refreshing data:', error);
    } finally {
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    // Load initial data
    refreshData();
  }, []);

  // Refetch weather data when trip data changes
  useEffect(() => {
    if (tripData?.itinerary?.days) {
      fetchWeatherData();
    }
  }, [tripData?.itinerary?.days]);

  const getWeatherIcon = (weatherData: WeatherData) => {
    // Use the icon from OpenWeather API if available, otherwise fall back to condition
    if (weatherData.icon) {
      return weatherService.getWeatherIconFromCode(weatherData.icon);
    }
    return weatherService.getWeatherIcon(weatherData.condition);
  };

  const getTransportIcon = (type: string) => {
    switch (type) {
      case 'flight': return <Plane className="w-4 h-4" />;
      case 'train': return <Train className="w-4 h-4" />;
      case 'bus': return <Car className="w-4 h-4" />;
      case 'car': return <Car className="w-4 h-4" />;
      default: return <Navigation className="w-4 h-4" />;
    }
  };

  const getDelayStatusColor = (status: string) => {
    switch (status) {
      case 'on-time': return 'bg-green-100 text-green-800 border-green-200';
      case 'delayed': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'cancelled': return 'bg-red-100 text-red-800 border-red-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getTranslatedStatus = (status: string) => {
    switch (status) {
      case 'on-time':
        return t('realTime.onTime');
      case 'delayed':
        return t('realTime.delayed');
      case 'cancelled':
        return t('realTime.cancelled');
      default:
        return status;
    }
  };

  return (
    <div className="p-4 md:p-6 overflow-y-auto h-full">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <h2 className="text-xl md:text-2xl font-semibold">Real-Time Trip Overview</h2>
        <div className="flex flex-wrap items-center gap-2 md:gap-3">
          <Button 
            variant="outline" 
            onClick={refreshData}
            disabled={isRefreshing}
            className="flex items-center space-x-2 min-h-[44px]"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span className="hidden sm:inline">{t('realTime.refreshData')}</span>
          </Button>
          <Button variant="outline" onClick={onShare} className="min-h-[44px]">
            <Share2 className="w-4 h-4 mr-2" />
            Share
          </Button>
          <Button variant="outline" onClick={onExportPDF}>
            <FileText className="w-4 h-4 mr-2" />
            Export PDF
          </Button>
        </div>
      </div>

      {/* Trip Analytics */}
      {tripAnalytics && (
        <div className="grid grid-cols-2 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-4 gap-2 sm:gap-4 mb-6">
          <Card>
            <CardContent className="p-2 sm:p-4">
              <div className="flex items-center space-x-1 sm:space-x-2">
                <MapPin className="w-4 h-4 sm:w-5 sm:h-5 text-blue-600" />
                <div>
                  <p className="text-xs sm:text-sm text-gray-600">{t('realTime.cities')}</p>
                  <p className="text-lg sm:text-2xl font-bold">{tripAnalytics.uniqueCities}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-2 sm:p-4">
              <div className="flex items-center space-x-1 sm:space-x-2">
                <Navigation className="w-4 h-4 sm:w-5 sm:h-5 text-green-600" />
                <div>
                  <p className="text-xs sm:text-sm text-gray-600">{t('realTime.distance')}</p>
                  <p className="text-lg sm:text-2xl font-bold">{tripAnalytics.totalDistance}km</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-2 sm:p-4">
              <div className="flex items-center space-x-1 sm:space-x-2">
                <Clock className="w-4 h-4 sm:w-5 sm:h-5 text-purple-600" />
                <div>
                  <p className="text-xs sm:text-sm text-gray-600">{t('realTime.activities')}</p>
                  <p className="text-lg sm:text-2xl font-bold">{tripAnalytics.totalActivities}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-2 sm:p-4">
              <div className="flex items-center space-x-1 sm:space-x-2">
                <span className="text-sm sm:text-lg">💰</span>
                <div>
                  <p className="text-xs sm:text-sm text-gray-600">{t('realTime.avgPerDay')}</p>
                  <p className="text-lg sm:text-2xl font-bold">€{tripAnalytics.averageCostPerDay}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Weather Status */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Cloud className="w-5 h-5" />
              <span>{t('realTime.weatherStatus')}</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            {weatherLoading ? (
              <div className="flex items-center justify-center p-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                <span className="ml-2 text-gray-600">Loading weather data...</span>
              </div>
            ) : weatherError ? (
              <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-red-600 text-sm">{weatherError}</p>
                <Button 
                  variant="outline" 
                  size="sm" 
                  onClick={fetchWeatherData}
                  className="mt-2"
                >
                  <RefreshCw className="w-4 h-4 mr-2" />
                  Retry
                </Button>
              </div>
            ) : weatherData.length === 0 ? (
              <div className="p-4 bg-gray-50 rounded-lg text-center">
                <p className="text-gray-600 text-sm">No weather data available</p>
                <p className="text-xs text-gray-500 mt-1">Cities will be detected from your itinerary</p>
              </div>
            ) : (
              <div className="space-y-4">
                {weatherData.map((weather, index) => (
                  <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <span className="text-2xl">{getWeatherIcon(weather)}</span>
                      <div>
                        <p className="font-medium">{weather.city}</p>
                        <p className="text-sm text-gray-600 capitalize">{weather.description}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-xl font-bold">{weather.temperature}°C</p>
                      <p className="text-xs text-gray-500">{t('realTime.updated')}: {weather.lastUpdated}</p>
                      <div className="flex items-center space-x-2 text-xs text-gray-500">
                        <span>💧 {weather.humidity}%</span>
                        <span>💨 {weather.windSpeed} km/h</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Transport Delays */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <AlertTriangle className="w-5 h-5" />
              <span>{t('realTime.transportStatus')}</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {transportDelays.map((delay) => (
                <div key={delay.id} className="p-3 border rounded-lg">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      {getTransportIcon(delay.type)}
                      <span className="font-medium">{delay.route}</span>
                    </div>
                    <Badge className={getDelayStatusColor(delay.status)}>
                      {delay.status === 'on-time' ? <CheckCircle className="w-3 h-3 mr-1" /> : 
                       delay.status === 'delayed' ? <Clock className="w-3 h-3 mr-1" /> : 
                       <AlertTriangle className="w-3 h-3 mr-1" />}
                      {getTranslatedStatus(delay.status)}
                    </Badge>
                  </div>
                  <div className="flex justify-between text-sm text-gray-600">
                    <span>{t('realTime.scheduled')}: {delay.scheduledTime}</span>
                    <span>{t('realTime.actual')}: {delay.actualTime}</span>
                    {delay.delay > 0 && <span className="text-yellow-600">+{delay.delay}min</span>}
                  </div>
                  {delay.reason && (
                    <p className="text-xs text-gray-500 mt-1">{t('realTime.reason')}: {delay.reason}</p>
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Transport Modes Summary */}
      {tripAnalytics && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Navigation className="w-5 h-5" />
              <span>{t('realTime.transportModes')}</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {Object.entries(tripAnalytics.transportModes).map(([mode, count]) => (
                <div key={mode} className="text-center p-3 bg-gray-50 rounded-lg">
                  <div className="flex justify-center mb-2">
                    {mode === 'flight' && <Plane className="w-6 h-6 text-blue-600" />}
                    {mode === 'train' && <Train className="w-6 h-6 text-green-600" />}
                    {mode === 'bus' && <Car className="w-6 h-6 text-orange-600" />}
                    {mode === 'walking' && <Navigation className="w-6 h-6 text-purple-600" />}
                  </div>
                  <p className="text-2xl font-bold">{count}</p>
                  <p className="text-sm text-gray-600 capitalize">{t(`mockData.transportation.${mode}`, mode)}</p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Original Trip Summary */}
      <Card>
        <CardHeader>
          <CardTitle>{t('realTime.tripSummary')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Destination:</span>
                <span className="font-medium">{tripData.destination}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Duration:</span>
                <span className="font-medium">{tripData.itinerary?.days?.length || 0} days</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Travelers:</span>
                <span className="font-medium">{tripData.travelers?.length || 1} people</span>
              </div>
            </div>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Budget:</span>
                <span className="font-medium">{tripData.budget?.currency} {tripData.budget?.total}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">{t('realTime.status')}:</span>
                <Badge variant="outline" className="bg-green-100 text-green-800 border-green-200">
                  <CheckCircle className="w-3 h-3 mr-1" />
                  {t('realTime.active')}
                </Badge>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
