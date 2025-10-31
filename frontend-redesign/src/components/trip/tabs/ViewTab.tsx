/**
 * View Tab - Trip Overview
 * Task 25: Enhanced with statistics, weather, map, and quick actions
 */

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { TripMap } from '@/components/map/TripMap';
import { WeatherWidget } from '@/components/weather/WeatherWidget';
import { ExportOptionsModal, ExportOptions } from '@/components/export/ExportOptionsModal';
import { ShareModal } from '@/components/share/ShareModal';
import { exportService } from '@/services/exportService';
import { useToast } from '@/components/ui/use-toast';
import {
  Calendar,
  MapPin,
  DollarSign,
  CreditCard,
  Cloud,
  CloudRain,
  Sun,
  CloudSnow,
  Edit,
  Share2,
  Download,
  CalendarPlus,
} from 'lucide-react';

interface ViewTabProps {
  itinerary: any; // NormalizedItinerary type
}

// Mock weather data - TODO: Replace with real API
const MOCK_WEATHER = [
  { day: 'Mon', icon: Sun, high: 75, low: 62 },
  { day: 'Tue', icon: Cloud, high: 72, low: 60 },
  { day: 'Wed', icon: CloudRain, high: 68, low: 58 },
  { day: 'Thu', icon: Sun, high: 76, low: 63 },
  { day: 'Fri', icon: Cloud, high: 73, low: 61 },
  { day: 'Sat', icon: Sun, high: 78, low: 64 },
  { day: 'Sun', icon: CloudRain, high: 70, low: 59 },
];

export function ViewTab({ itinerary }: ViewTabProps) {
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const { toast } = useToast();
  
  const isGenerating = itinerary?.status === 'generating' || itinerary?.status === 'planning';

  // Extract just the city name from destination (e.g., "Sydney, New South Wales, Australia" -> "Sydney")
  // Use destination field first, then fallback to first day's location, then extract from summary
  const getDestinationCity = () => {
    if (itinerary.destination) {
      return itinerary.destination.split(',')[0].trim();
    }
    if (itinerary.days?.[0]?.location) {
      return itinerary.days[0].location.split(',')[0].trim();
    }
    // Last resort: extract from summary "Your personalized itinerary for Sydney, Australia"
    if (itinerary.summary) {
      const match = itinerary.summary.match(/for\s+([^,]+)/);
      if (match) return match[1].trim();
    }
    return 'Unknown';
  };
  
  const destination = getDestinationCity();
  // Handle nested structure: itinerary.itinerary.days or itinerary.days
  const days = itinerary?.itinerary?.days || itinerary?.days || [];
  const startDate = days[0]?.date || '';
  const endDate = days[days.length - 1]?.date || '';
  const dayCount = days.length;
  
  // Calculate statistics
  const activityCount = days.reduce((total: number, day: any) => {
    // TripData uses 'components', not 'nodes'
    return total + (day.components?.length || 0);
  }, 0);
  
  const totalBudget = 5000; // TODO: Get from itinerary metadata
  const bookingsCount = 0; // TODO: Get from bookings

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const getTripStatus = () => {
    const today = new Date();
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    if (today < start) return { label: 'Upcoming', variant: 'default' as const };
    if (today > end) return { label: 'Completed', variant: 'secondary' as const };
    return { label: 'Ongoing', variant: 'default' as const };
  };

  const status = getTripStatus();

  const handleExport = async (options: ExportOptions) => {
    setIsExporting(true);
    try {
      // For now, use the existing export service
      // TODO: Enhance to use options
      await exportService.exportToPDF(itinerary as any);
      toast({
        title: 'Export successful',
        description: 'Your itinerary is ready to print or save as PDF',
      });
      setIsExportModalOpen(false);
    } catch (error) {
      toast({
        title: 'Export failed',
        description: 'Could not export PDF. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Trip Header */}
      <div className="space-y-4">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-4xl font-bold text-foreground mb-2">{destination}</h1>
            <div className="flex items-center gap-4 text-muted-foreground">
              <div className="flex items-center gap-2">
                <Calendar className="w-5 h-5" />
                <span>{formatDate(startDate)}</span>
              </div>
              <span>→</span>
              <span>{formatDate(endDate)}</span>
            </div>
          </div>
          <Badge variant={status.variant} className="text-sm px-3 py-1">
            {status.label}
          </Badge>
        </div>

        {/* Destination Image */}
        <div className="relative h-[300px] rounded-xl overflow-hidden">
          <img
            src="https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200"
            alt={destination}
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Total Days
            </CardTitle>
            <Calendar className="w-5 h-5 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">{dayCount}</div>
            <p className="text-xs text-muted-foreground mt-1">
              {dayCount} days of adventure
            </p>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Activities
            </CardTitle>
            <MapPin className="w-5 h-5 text-primary" />
          </CardHeader>
          <CardContent>
            {isGenerating && activityCount === 0 ? (
              <>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                  <div className="text-2xl font-bold text-muted-foreground">...</div>
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  Generating activities
                </p>
              </>
            ) : (
              <>
                <div className="text-3xl font-bold">{activityCount}</div>
                <p className="text-xs text-muted-foreground mt-1">
                  Planned activities
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Budget
            </CardTitle>
            <DollarSign className="w-5 h-5 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">${totalBudget.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground mt-1">
              Estimated total
            </p>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Bookings
            </CardTitle>
            <CreditCard className="w-5 h-5 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">{bookingsCount}</div>
            <p className="text-xs text-muted-foreground mt-1">
              Confirmed bookings
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Weather Forecast */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Cloud className="w-5 h-5" />
            7-Day Weather Forecast
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4 overflow-x-auto pb-2">
            {MOCK_WEATHER.map((day, index) => {
              const Icon = day.icon;
              return (
                <div
                  key={index}
                  className="flex flex-col items-center min-w-[80px] p-3 rounded-lg hover:bg-muted transition-colors"
                >
                  <span className="text-sm font-medium text-muted-foreground mb-2">
                    {day.day}
                  </span>
                  <Icon className="w-8 h-8 text-primary mb-2" />
                  <div className="text-center">
                    <div className="text-lg font-bold">{day.high}°</div>
                    <div className="text-sm text-muted-foreground">{day.low}°</div>
                  </div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* Weather Forecast */}
      <WeatherWidget 
        location={destination} 
        date={startDate}
      />

      {/* Trip Map */}
      <TripMap itinerary={itinerary} />

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Button variant="outline" className="justify-start h-auto py-4">
              <Edit className="w-5 h-5 mr-3" />
              <div className="text-left">
                <div className="font-medium">Edit Trip</div>
                <div className="text-xs text-muted-foreground">
                  Modify dates, travelers, or preferences
                </div>
              </div>
            </Button>

            <Button 
              variant="outline" 
              className="justify-start h-auto py-4"
              onClick={() => setIsShareModalOpen(true)}
            >
              <Share2 className="w-5 h-5 mr-3" />
              <div className="text-left">
                <div className="font-medium">Share Trip</div>
                <div className="text-xs text-muted-foreground">
                  Share with friends and family
                </div>
              </div>
            </Button>

            <Button 
              variant="outline" 
              className="justify-start h-auto py-4"
              onClick={() => setIsExportModalOpen(true)}
            >
              <Download className="w-5 h-5 mr-3" />
              <div className="text-left">
                <div className="font-medium">Export PDF</div>
                <div className="text-xs text-muted-foreground">
                  Download printable itinerary
                </div>
              </div>
            </Button>

            <Button variant="outline" className="justify-start h-auto py-4">
              <CalendarPlus className="w-5 h-5 mr-3" />
              <div className="text-left">
                <div className="font-medium">Add to Calendar</div>
                <div className="text-xs text-muted-foreground">
                  Sync with Google Calendar
                </div>
              </div>
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Export Options Modal */}
      <ExportOptionsModal
        isOpen={isExportModalOpen}
        onClose={() => setIsExportModalOpen(false)}
        onExport={handleExport}
        isExporting={isExporting}
      />

      {/* Share Modal */}
      <ShareModal
        isOpen={isShareModalOpen}
        onClose={() => setIsShareModalOpen(false)}
        itineraryId={itinerary?.id || itinerary?.itineraryId || ''}
        itinerary={itinerary}
      />
    </div>
  );
}
