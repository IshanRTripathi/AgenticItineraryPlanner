/**
 * View Tab - Trip Overview
 * Task 25: Enhanced with statistics, weather, map, and quick actions
 */

import { useState, useEffect, useRef, useMemo } from 'react';
import { MobileViewTab } from './MobileViewTab';

import { motion, useInView, useSpring, useTransform } from 'framer-motion';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { WeatherWidget } from '@/components/weather/WeatherWidget';
import { ExportOptionsModal, ExportOptions } from '@/components/export/ExportOptionsModal';
import { ShareModal } from '@/components/share/ShareModal';
import { exportService } from '@/services/exportService';
import { fetchWeatherForecast } from '@/services/weatherService';
import { useToast } from '@/components/ui/use-toast';
import { useTranslation } from '@/i18n';
import { useCurrency } from '@/hooks/useCurrency';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import { useAuth } from '@/contexts/AuthContext';
import {
  Calendar,
  MapPin,
  IndianRupee,
  Coins,
  CreditCard,
  Cloud,
  Edit,
  Share2,
  Download,
  CalendarPlus,
  ChevronLeft,
  ChevronRight,
  DollarSign,
} from 'lucide-react';

/**
 * Destination Slideshow Component
 * Displays a carousel of photos from the itinerary with place names
 */
interface PhotoWithPlace {
  photoRef: string;
  placeName: string;
  placeType?: string;
}

function DestinationSlideshow({ days, destination, onPhotoChange }: { days: any[]; destination: string; onPhotoChange?: (photo: PhotoWithPlace) => void }) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [photos, setPhotos] = useState<PhotoWithPlace[]>([]);

  useEffect(() => {
    console.log('[DestinationSlideshow] Days received:', days?.length);
    console.log('[DestinationSlideshow] Destination:', destination);

    // Collect all photos with place names from all days - ONLY ATTRACTIONS
    const allPhotos: PhotoWithPlace[] = [];
    for (const day of days) {
      const nodes = day.nodes || [];
      console.log(`[DestinationSlideshow] Day ${day.dayNumber}: ${nodes.length} nodes`);

      for (const node of nodes) {
        // Filter: Only include attractions, skip hotels/accommodations
        const isAttraction = node.type === 'attraction' ||
          node.type === 'activity' ||
          node.type === 'sightseeing' ||
          (!node.type?.includes('hotel') && !node.type?.includes('accommodation'));

        console.log(`[DestinationSlideshow] Node "${node.title}":`, {
          type: node.type,
          isAttraction,
          hasLocation: !!node.location,
          hasPhotos: !!node.location?.photos,
          photoCount: node.location?.photos?.length || 0,
          photos: node.location?.photos
        });

        if (isAttraction && node.location?.photos && node.location.photos.length > 0) {
          // Add only first photo per location to show variety
          allPhotos.push({
            photoRef: node.location.photos[0],
            placeName: node.title || node.location?.name || 'Unknown Place',
            placeType: node.type
          });
        }
      }
    }

    console.log('[DestinationSlideshow] Total photos collected:', allPhotos.length);

    // Remove duplicates based on photoRef
    const uniquePhotos = allPhotos.filter((photo, index, self) =>
      index === self.findIndex((p) => p.photoRef === photo.photoRef)
    ).slice(0, 10);

    // If no photos, use fallback
    if (uniquePhotos.length === 0) {
      console.log('[DestinationSlideshow] No photos found, using fallback');
      const searchQuery = encodeURIComponent(destination);
      uniquePhotos.push({
        photoRef: `https://source.unsplash.com/1600x900/?${searchQuery},travel,landmark`,
        placeName: destination,
        placeType: 'destination'
      });
    }

    console.log('[DestinationSlideshow] Final photos to display:', uniquePhotos);
    setPhotos(uniquePhotos);
  }, [days, destination]);

  const nextSlide = () => {
    setCurrentIndex((prev) => {
      const newIndex = (prev + 1) % photos.length;
      if (onPhotoChange && photos[newIndex]) {
        onPhotoChange(photos[newIndex]);
      }
      return newIndex;
    });
  };

  const prevSlide = () => {
    setCurrentIndex((prev) => {
      const newIndex = (prev - 1 + photos.length) % photos.length;
      if (onPhotoChange && photos[newIndex]) {
        onPhotoChange(photos[newIndex]);
      }
      return newIndex;
    });
  };

  // Notify parent of initial photo
  useEffect(() => {
    if (onPhotoChange && photos[currentIndex]) {
      onPhotoChange(photos[currentIndex]);
    }
  }, [photos]);

  // Auto-advance slideshow every 5 seconds
  useEffect(() => {
    if (photos.length <= 1) return;

    const interval = setInterval(nextSlide, 5000);
    return () => clearInterval(interval);
  }, [photos.length]);

  const getPhotoUrl = (photoRef: string): string => {
    console.log('[DestinationSlideshow] Getting photo URL for:', photoRef?.substring(0, 50));

    // If it's already a full URL (Unsplash fallback), return as is
    if (photoRef?.startsWith('http')) {
      console.log('[DestinationSlideshow] Using full URL:', photoRef);
      return photoRef;
    }

    // Otherwise, it's a Google Maps photo reference
    const apiKey = import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY;
    console.log('[DestinationSlideshow] API Key available:', !!apiKey);

    if (!apiKey) {
      console.log('[DestinationSlideshow] No API key, using fallback');
      return 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=1200';
    }

    const url = `https://maps.googleapis.com/maps/api/place/photo?maxwidth=1200&photo_reference=${photoRef}&key=${apiKey}`;
    console.log('[DestinationSlideshow] Generated Google Maps URL:', url.substring(0, 100) + '...');
    return url;
  };

  const currentPhoto = photos[currentIndex];

  return (
    <div className="relative h-screen md:h-[400px] -mx-4 md:mx-0 md:rounded-2xl overflow-hidden group shadow-xl md:shadow-2xl">
      {/* Current Image with premium parallax transition */}
      <motion.img
        key={currentIndex}
        src={getPhotoUrl(currentPhoto?.photoRef)}
        alt={currentPhoto?.placeName || destination}
        className="w-full h-full object-cover object-center"
        initial={{ x: 60, scale: 1.02, opacity: 0 }}
        animate={{ x: 0, scale: 1, opacity: 1 }}
        exit={{ x: -60, scale: 0.98, opacity: 0 }}
        transition={{
          duration: 0.9,
          ease: [0.22, 1, 0.36, 1],
          opacity: { duration: 0.6 }
        }}
        onError={(e) => {
          const target = e.target as HTMLImageElement;
          target.src = 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=1600';
        }}
      />

      {/* Lighter gradient overlay */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/20 to-transparent" />

      {/* Place Name - Desktop only */}
      <motion.div
        key={`name-${currentIndex}`}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2, duration: 0.5 }}
        className="hidden md:block absolute bottom-4 left-4 z-20 max-w-[calc(100%-6rem)]"
      >
        <div className="bg-white/20 backdrop-blur-md rounded-lg px-4 py-2 border border-white/30 shadow-lg inline-block">
          <h3 className="text-lg font-bold text-white drop-shadow-lg truncate">
            {currentPhoto?.placeName?.split(',')[0] || currentPhoto?.placeName}
          </h3>
        </div>
      </motion.div>

      {/* Premium Navigation Buttons - Desktop only */}
      {photos.length > 1 && (
        <>
          <motion.button
            onClick={prevSlide}
            whileHover={{ scale: 1.1, x: -2 }}
            whileTap={{ scale: 0.95 }}
            className="hidden md:flex absolute left-6 top-1/2 -translate-y-1/2 w-14 h-14 rounded-full bg-white/25 backdrop-blur-xl hover:bg-white/35 transition-all opacity-0 group-hover:opacity-100 items-center justify-center shadow-[0_8px_32px_0_rgba(0,0,0,0.4)] border-2 border-white/40 z-30"
            aria-label="Previous photo"
          >
            <ChevronLeft className="w-7 h-7 text-white drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]" />
          </motion.button>
          <motion.button
            onClick={nextSlide}
            whileHover={{ scale: 1.1, x: 2 }}
            whileTap={{ scale: 0.95 }}
            className="hidden md:flex absolute right-6 top-1/2 -translate-y-1/2 w-14 h-14 rounded-full bg-white/25 backdrop-blur-xl hover:bg-white/35 transition-all opacity-0 group-hover:opacity-100 items-center justify-center shadow-[0_8px_32px_0_rgba(0,0,0,0.4)] border-2 border-white/40 z-30"
            aria-label="Next photo"
          >
            <ChevronRight className="w-7 h-7 text-white drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]" />
          </motion.button>
        </>
      )}
    </div>
  );
}

interface ViewTabProps {
  itinerary: any; // NormalizedItinerary type
}

// Helper function to get currency symbol
const getCurrencySymbol = (currency?: string): string => {
  if (!currency) return '$';
  switch (currency.toUpperCase()) {
    case 'INR':
      return '₹';
    case 'USD':
      return '$';
    case 'EUR':
      return '€';
    case 'GBP':
      return '£';
    case 'JPY':
      return '¥';
    case 'CNY':
      return '¥';
    case 'AUD':
      return 'A$';
    case 'CAD':
      return 'C$';
    default:
      return currency + ' ';
  }
};

export function ViewTab({ itinerary }: ViewTabProps) {
  const { t } = useTranslation();
  const { preferredCurrency, convert, getCurrencySymbol } = useCurrency();
  const { user, isAuthenticated } = useAuth();

  console.log('[ViewTab] 🎯 Hook Values:', {
    preferredCurrency,
    hasConvert: !!convert,
    hasGetSymbol: !!getCurrencySymbol
  });

  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [currentWeather, setCurrentWeather] = useState<{
    high: number;
    low: number;
    condition: string;
    icon: string;
  } | null>(null);
  const [isLoadingWeather, setIsLoadingWeather] = useState(false);
  const [currentPhoto, setCurrentPhoto] = useState<PhotoWithPlace | null>(null);
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
    const nodes = day.nodes || [];
    return total + nodes.length;
  }, 0);

  // Calculate total budget from all nodes across all days
  const totalBudget = days.reduce((total: number, day: any) => {
    const nodes = day.nodes || [];
    const dayTotal = nodes.reduce((daySum: number, node: any) => {
      // Support both field names for backward compatibility
      const cost = node.cost?.amountPerPerson || node.cost?.pricePerPerson || node.cost?.amount || 0;

      console.log('[ViewTab] Node cost:', {
        title: node.title || node.name,
        cost,
        costObject: node.cost,
        estimatedCost: node.estimatedCost,
        price: node.price
      });

      return daySum + (typeof cost === 'number' ? cost : 0);
    }, 0);
    return total + dayTotal;
  }, 0);

  // Get currency from itinerary or first node with cost
  const itineraryCurrency = itinerary?.currency ||
    days.flatMap((day: any) => day.nodes || [])
      .find((node: any) => node.cost?.currency)?.cost?.currency ||
    'USD';

  // Display currency: use preferred currency or itinerary currency
  const displayCurrency = useMemo(() => {
    const result = preferredCurrency || itineraryCurrency;
    console.log('[ViewTab] 💱 Display Currency Calculation:', {
      preferredCurrency,
      itineraryCurrency,
      displayCurrency: result,
      usingPreferred: !!preferredCurrency
    });
    return result;
  }, [preferredCurrency, itineraryCurrency]);

  // Convert total budget to display currency
  const convertedTotalBudget = useMemo(() => {
    console.log('[ViewTab] 🔄 CONVERSION STARTING:', {
      totalBudget,
      itineraryCurrency,
      displayCurrency,
      preferredCurrency,
      areEqual: itineraryCurrency === displayCurrency
    });

    const result = convert(totalBudget, itineraryCurrency, displayCurrency);

    console.log('[ViewTab] ✅ CONVERSION RESULT:', {
      input: `${totalBudget} ${itineraryCurrency}`,
      output: `${result} ${displayCurrency}`,
      changed: result !== totalBudget
    });

    return result;
  }, [totalBudget, itineraryCurrency, displayCurrency, preferredCurrency, convert]);

  console.log('[ViewTab] Budget calculation:', {
    daysCount: days.length,
    activityCount,
    totalBudget,
    itineraryCurrency,
    displayCurrency,
    convertedTotalBudget,
    preferredCurrency,
    firstDay: days[0],
    firstDayNodes: days[0]?.nodes || days[0]?.components
  });

  const bookingsCount = 0; // TODO: Get from bookings

  // Fetch current weather for destination
  useEffect(() => {
    async function loadWeather() {
      if (!destination || destination === 'Unknown') return;

      setIsLoadingWeather(true);
      try {
        const forecast = await fetchWeatherForecast(destination, 1);
        if (forecast.length > 0) {
          setCurrentWeather({
            high: forecast[0].high,
            low: forecast[0].low,
            condition: forecast[0].description,
            icon: forecast[0].icon,
          });
        }
      } catch (error) {
        console.error('[ViewTab] Failed to load weather:', error);
      } finally {
        setIsLoadingWeather(false);
      }
    }

    loadWeather();
  }, [destination]);

  // Fetch photos for background
  useEffect(() => {
    if (currentPhoto) return;

    const allPhotos: PhotoWithPlace[] = [];
    for (const day of days) {
      const nodes = day.nodes || [];
      for (const node of nodes) {
        const isAttraction = node.type === 'attraction' ||
          node.type === 'activity' ||
          node.type === 'sightseeing' ||
          (!node.type?.includes('hotel') && !node.type?.includes('accommodation'));

        if (isAttraction && node.location?.photos && node.location.photos.length > 0) {
          allPhotos.push({
            photoRef: node.location.photos[0],
            placeName: node.title || node.location?.name || 'Unknown Place',
            placeType: node.type
          });
        }
      }
    }

    if (allPhotos.length > 0) {
      // Pick a random photo or the first one
      setCurrentPhoto(allPhotos[0]);
    } else {
      // Fallback
      const searchQuery = encodeURIComponent(destination);
      setCurrentPhoto({
        photoRef: `https://source.unsplash.com/1600x900/?${searchQuery},travel,landmark`,
        placeName: destination,
        placeType: 'destination'
      });
    }
  }, [days, destination]);

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

    if (today < start) return { label: t('common.status.upcoming'), variant: 'default' as const };
    if (today > end) return { label: t('common.status.completed'), variant: 'secondary' as const };
    return { label: t('common.status.ongoing'), variant: 'default' as const };
  };

  const status = getTripStatus();

  // Count-up animation hook
  function useCountUp(end: number, duration: number = 2000) {
    const ref = useRef<HTMLDivElement>(null);
    const isInView = useInView(ref, { once: true });
    const motionValue = useSpring(0, { duration });
    const rounded = useTransform(motionValue, (latest) => Math.round(latest));

    useEffect(() => {
      if (isInView) {
        motionValue.set(end);
      }
    }, [isInView, end, motionValue]);

    return { ref, value: rounded };
  }



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
    <div className="md:space-y-8">
      {/* Mobile: Refined Magazine Layout */}
      {/* Mobile: Premium View */}
      <div className="md:hidden">
        <MobileViewTab itinerary={itinerary} />
      </div>

      {/* Desktop: Normal layout */}
      <div className="hidden md:block space-y-8">
        {/* Trip Header - Centered */}
        <div className="space-y-4">
          <div className="text-center space-y-2 pb-4 border-b">
            <h1 className="text-4xl font-bold text-foreground">{destination}</h1>
            <div className="flex items-center justify-center gap-2 text-muted-foreground text-base">
              <Calendar className="w-5 h-5" />
              <span>{formatDate(startDate)}</span>
              <span>→</span>
              <span>{formatDate(endDate)}</span>
            </div>
          </div>

          {/* Destination Image Slideshow */}
          <DestinationSlideshow days={days} destination={destination} />
        </div>

        {/* Currency Selector */}
        <div className="flex justify-end">
          <CurrencySelector variant="compact" showFlags={true} />
        </div>

        {/* Statistics Cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            title={t('components.viewTab.stats.totalDays')}
            value={dayCount}
            subtitle={t('components.viewTab.stats.days', { count: dayCount }, { count: dayCount })}
            icon={Calendar}
            delay={0}
          />

          {isGenerating && activityCount === 0 ? (
            <Card className="shadow-sm">
              <CardHeader className="flex flex-row items-center justify-between pb-1 md:pb-2 px-3 md:px-6 pt-3 md:pt-6">
                <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">
                  {t('components.viewTab.stats.activities')}
                </CardTitle>
                <MapPin className="w-4 h-4 md:w-5 md:h-5 text-primary" />
              </CardHeader>
              <CardContent className="px-3 md:px-6 pb-3 md:pb-6">
                <div className="flex items-center gap-1.5 md:gap-2">
                  <div className="w-4 h-4 md:w-6 md:h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                  <div className="text-xl md:text-2xl font-bold text-muted-foreground">...</div>
                </div>
                <p className="text-[10px] md:text-xs text-muted-foreground mt-0.5 md:mt-1">
                  {t('components.viewTab.stats.generating')}
                </p>
              </CardContent>
            </Card>
          ) : (
            <StatCard
              title={t('components.viewTab.stats.activities')}
              value={activityCount}
              subtitle={t('components.viewTab.stats.planned')}
              icon={MapPin}
              delay={0.1}
            />
          )}

          <StatCard
            title={t('components.viewTab.stats.budget')}
            value={Math.round(convertedTotalBudget)}
            subtitle={t('components.viewTab.stats.perPerson')}
            icon={Coins}
            prefix={getCurrencySymbol(displayCurrency)}
            delay={0.2}
          />

          {/* Weather Card */}
          {isLoadingWeather ? (
            <Card className="shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
              <CardHeader className="flex flex-row items-center justify-between pb-1 md:pb-2 px-3 md:px-6 pt-3 md:pt-6">
                <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">
                  {t('components.viewTab.stats.weather')}
                </CardTitle>
                <div className="w-7 h-7 md:w-10 md:h-10 rounded-full bg-primary/10 flex items-center justify-center">
                  <Cloud className="w-3.5 h-3.5 md:w-5 md:h-5 text-primary" />
                </div>
              </CardHeader>
              <CardContent className="px-3 md:px-6 pb-3 md:pb-6">
                <div className="flex items-center gap-1.5 md:gap-2">
                  <div className="w-4 h-4 md:w-6 md:h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                  <div className="text-xl md:text-2xl font-bold text-muted-foreground">...</div>
                </div>
                <p className="text-[10px] md:text-xs text-muted-foreground mt-0.5 md:mt-1">
                  {t('common.actions.loading')}
                </p>
              </CardContent>
            </Card>
          ) : currentWeather ? (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3, duration: 0.5 }}
            >
              <Card className="shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                <CardHeader className="flex flex-row items-center justify-between pb-1 md:pb-2 px-3 md:px-6 pt-3 md:pt-6">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">
                    {t('components.viewTab.stats.weather')}
                  </CardTitle>
                  <div className="w-7 h-7 md:w-10 md:h-10 rounded-full bg-primary/10 flex items-center justify-center">
                    <Cloud className="w-3.5 h-3.5 md:w-5 md:h-5 text-primary" />
                  </div>
                </CardHeader>
                <CardContent className="px-3 md:px-6 pb-3 md:pb-6">
                  <div className="flex items-baseline gap-0.5 md:gap-1">
                    <div className="text-xl md:text-3xl font-bold bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
                      {currentWeather.high}°
                    </div>
                    <div className="text-sm md:text-lg text-muted-foreground">
                      / {currentWeather.low}°
                    </div>
                  </div>
                  <p className="text-[10px] md:text-xs text-muted-foreground mt-0.5 md:mt-1 capitalize truncate">
                    {currentWeather.condition}
                  </p>
                </CardContent>
              </Card>
            </motion.div>
          ) : (
            <Card className="shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
              <CardHeader className="flex flex-row items-center justify-between pb-1 md:pb-2 px-3 md:px-6 pt-3 md:pt-6">
                <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">
                  {t('components.viewTab.stats.weather')}
                </CardTitle>
                <div className="w-7 h-7 md:w-10 md:h-10 rounded-full bg-primary/10 flex items-center justify-center">
                  <Cloud className="w-3.5 h-3.5 md:w-5 md:h-5 text-primary" />
                </div>
              </CardHeader>
              <CardContent className="px-3 md:px-6 pb-3 md:pb-6">
                <div className="text-xl md:text-2xl font-bold text-muted-foreground">--</div>
                <p className="text-[10px] md:text-xs text-muted-foreground mt-0.5 md:mt-1">
                  {t('components.viewTab.stats.unavailable')}
                </p>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-4 gap-4">
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="group relative overflow-hidden rounded-2xl bg-gradient-to-br from-blue-50 to-blue-100/50 dark:from-blue-950/30 dark:to-blue-900/20 p-6 text-left transition-all hover:shadow-lg border border-blue-200/50 dark:border-blue-800/50"
          >
            <div className="relative z-10">
              <div className="w-10 h-10 rounded-full bg-blue-500/10 dark:bg-blue-400/10 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                <Edit className="w-5 h-5 text-blue-600 dark:text-blue-400" />
              </div>
              <h3 className="font-semibold text-base text-blue-900 dark:text-blue-100 mb-1">
                {t('components.viewTab.quickActions.editTrip.title')}
              </h3>
              <p className="text-xs text-blue-700/70 dark:text-blue-300/70 line-clamp-2">
                {t('components.viewTab.quickActions.editTrip.description')}
              </p>
            </div>
            <div className="absolute inset-0 bg-gradient-to-br from-blue-400/0 to-blue-500/5 opacity-0 group-hover:opacity-100 transition-opacity" />
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => setIsShareModalOpen(true)}
            className="group relative overflow-hidden rounded-2xl bg-gradient-to-br from-purple-50 to-purple-100/50 dark:from-purple-950/30 dark:to-purple-900/20 p-6 text-left transition-all hover:shadow-lg border border-purple-200/50 dark:border-purple-800/50"
          >
            <div className="relative z-10">
              <div className="w-10 h-10 rounded-full bg-purple-500/10 dark:bg-purple-400/10 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                <Share2 className="w-5 h-5 text-purple-600 dark:text-purple-400" />
              </div>
              <h3 className="font-semibold text-base text-purple-900 dark:text-purple-100 mb-1">
                {t('components.viewTab.quickActions.shareTrip.title')}
              </h3>
              <p className="text-xs text-purple-700/70 dark:text-purple-300/70 line-clamp-2">
                {t('components.viewTab.quickActions.shareTrip.description')}
              </p>
            </div>
            <div className="absolute inset-0 bg-gradient-to-br from-purple-400/0 to-purple-500/5 opacity-0 group-hover:opacity-100 transition-opacity" />
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => setIsExportModalOpen(true)}
            className="group relative overflow-hidden rounded-2xl bg-gradient-to-br from-green-50 to-green-100/50 dark:from-green-950/30 dark:to-green-900/20 p-6 text-left transition-all hover:shadow-lg border border-green-200/50 dark:border-green-800/50"
          >
            <div className="relative z-10">
              <div className="w-10 h-10 rounded-full bg-green-500/10 dark:bg-green-400/10 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                <Download className="w-5 h-5 text-green-600 dark:text-green-400" />
              </div>
              <h3 className="font-semibold text-base text-green-900 dark:text-green-100 mb-1">
                {t('components.viewTab.quickActions.exportPdf.title')}
              </h3>
              <p className="text-xs text-green-700/70 dark:text-green-300/70 line-clamp-2">
                {t('components.viewTab.quickActions.exportPdf.description')}
              </p>
            </div>
            <div className="absolute inset-0 bg-gradient-to-br from-green-400/0 to-green-500/5 opacity-0 group-hover:opacity-100 transition-opacity" />
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="group relative overflow-hidden rounded-2xl bg-gradient-to-br from-orange-50 to-orange-100/50 dark:from-orange-950/30 dark:to-orange-900/20 p-6 text-left transition-all hover:shadow-lg border border-orange-200/50 dark:border-orange-800/50"
          >
            <div className="relative z-10">
              <div className="w-10 h-10 rounded-full bg-orange-500/10 dark:bg-orange-400/10 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                <CalendarPlus className="w-5 h-5 text-orange-600 dark:text-orange-400" />
              </div>
              <h3 className="font-semibold text-base text-orange-900 dark:text-orange-100 mb-1">
                {t('components.viewTab.quickActions.addToCalendar.title')}
              </h3>
              <p className="text-xs text-orange-700/70 dark:text-orange-300/70 line-clamp-2">
                {t('components.viewTab.quickActions.addToCalendar.description')}
              </p>
            </div>
            <div className="absolute inset-0 bg-gradient-to-br from-orange-400/0 to-orange-500/5 opacity-0 group-hover:opacity-100 transition-opacity" />
          </motion.button>
        </div>
      </div>

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

// Animated Statistics Card Component
interface StatCardProps {
  title: string;
  value: number;
  subtitle: string;
  icon: any;
  prefix?: string;
  suffix?: string;
  delay?: number;
}

function StatCard({ title, value, subtitle, icon: Icon, prefix = '', suffix = '', delay = 0 }: StatCardProps) {
  const ref = useRef<HTMLDivElement>(null);
  const isInView = useInView(ref, { once: true, margin: "-100px" });
  const motionValue = useSpring(0, { duration: 2000, bounce: 0 });
  const rounded = useTransform(motionValue, (latest) => Math.round(latest));
  const [displayValue, setDisplayValue] = useState(0);

  useEffect(() => {
    if (isInView) {
      motionValue.set(value);
    }
  }, [isInView, value, motionValue]);

  useEffect(() => {
    const unsubscribe = rounded.on('change', (latest) => {
      setDisplayValue(latest);
    });
    return unsubscribe;
  }, [rounded]);

  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 20 }}
      animate={isInView ? { opacity: 1, y: 0 } : { opacity: 0, y: 20 }}
      transition={{ delay, duration: 0.5 }}
    >
      <Card className="shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
        <CardHeader className="flex flex-row items-center justify-between pb-1 md:pb-2 px-3 md:px-6 pt-3 md:pt-6">
          <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">
            {title}
          </CardTitle>
          <div className="w-7 h-7 md:w-10 md:h-10 rounded-full bg-primary/10 flex items-center justify-center">
            <Icon className="w-3.5 h-3.5 md:w-5 md:h-5 text-primary" />
          </div>
        </CardHeader>
        <CardContent className="px-3 md:px-6 pb-3 md:pb-6">
          <div className="text-xl md:text-3xl font-bold bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
            {prefix}{displayValue.toLocaleString()}{suffix}
          </div>
          <p className="text-[10px] md:text-xs text-muted-foreground mt-0.5 md:mt-1 truncate">
            {subtitle}
          </p>
        </CardContent>
      </Card>
    </motion.div>
  );
}
