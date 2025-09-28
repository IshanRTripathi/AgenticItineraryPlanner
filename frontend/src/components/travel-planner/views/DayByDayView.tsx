import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Button } from '../../ui/button';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '../../ui/collapsible';
import { ViewComponentProps, ErrorBoundary } from '../shared/types';
import { AutoRefreshEmptyState } from '../../shared/AutoRefreshEmptyState';
import { useMapContext } from '../../../contexts/MapContext';
import { geocodingService, geocodingUtils } from '../../../services/geocodingService';
import { 
  Clock, 
  MapPin, 
  Star, 
  DollarSign, 
  Users, 
  Phone, 
  Globe, 
  Calendar,
  Navigation,
  Car,
  Plane,
  Train,
  Bus,
  Utensils,
  Bed,
  Camera,
  ShoppingBag,
  Music,
  ExternalLink,
  Info,
  ChevronDown,
  ChevronRight
} from 'lucide-react';

// Helper function to get type icon
const getTypeIcon = (type: string) => {
  switch (type) {
    case 'attraction': return <Camera className="w-4 h-4" />;
    case 'restaurant': return <Utensils className="w-4 h-4" />;
    case 'hotel': return <Bed className="w-4 h-4" />;
    case 'transport': return <Navigation className="w-4 h-4" />;
    case 'shopping': return <ShoppingBag className="w-4 h-4" />;
    case 'entertainment': return <Music className="w-4 h-4" />;
    default: return <Info className="w-4 h-4" />;
  }
};

// Helper function to get transport icon
const getTransportIcon = (mode: string) => {
  switch (mode) {
    case 'walking': return <Navigation className="w-4 h-4" />;
    case 'taxi': return <Car className="w-4 h-4" />;
    case 'flight': return <Plane className="w-4 h-4" />;
    case 'train': return <Train className="w-4 h-4" />;
    case 'bus': return <Bus className="w-4 h-4" />;
    default: return <Car className="w-4 h-4" />;
  }
};

// Helper function to format time
const formatTime = (timeString: string) => {
  if (!timeString) return 'TBD';
  
  try {
    // Handle different time formats
    let date: Date;
    
    // If it's already in HH:MM format, create a date with today's date
    if (/^\d{1,2}:\d{2}$/.test(timeString)) {
      const [hours, minutes] = timeString.split(':');
      date = new Date();
      date.setHours(parseInt(hours, 10), parseInt(minutes, 10), 0, 0);
    } else {
      // Try to parse as ISO string or other date format
      date = new Date(timeString);
    }
    
    // Check if the date is valid
    if (isNaN(date.getTime())) {
      return timeString; // Return original string if parsing fails
    }
    
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: true 
    });
  } catch {
    return timeString; // Return original string if any error occurs
  }
};

// Helper function to format duration
const formatDuration = (duration: string | number) => {
  if (!duration && duration !== 0) return 'TBD';
  
  // If it's already a string (like "3h" or "2h 30m"), return as is
  if (typeof duration === 'string') {
    return duration;
  }
  
  // If it's a number (minutes), convert to hours and minutes
  if (typeof duration === 'number') {
    const hours = Math.floor(duration / 60);
    const mins = duration % 60;
    
    if (hours > 0) {
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${mins}m`;
  }
  
  return 'TBD';
};

// Helper function to get placeholder image based on category
const getPlaceholderImage = (category: string, name: string) => {
  const categoryMap: { [key: string]: string } = {
    'architecture': 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=400&h=300&fit=crop',
    'culture': 'https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?w=400&h=300&fit=crop',
    'art': 'https://images.unsplash.com/photo-1541961017774-22349e4a1262?w=400&h=300&fit=crop',
    'restaurant': 'https://images.unsplash.com/photo-1602273660127-a0000560a4c1?w=400&auto=format&fit=crop',
    'beach': 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&h=300&fit=crop',
    'park': 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=400&h=300&fit=crop',
    'history': 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    'attraction': 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400&h=300&fit=crop',
    'transport': 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=400&h=300&fit=crop',
    'hotel': 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=400&h=300&fit=crop'
  };
  
  // Try to find a matching category
  const lowerCategory = category?.toLowerCase() || '';
  for (const [key, url] of Object.entries(categoryMap)) {
    if (lowerCategory.includes(key)) {
      return url;
    }
  }
  
  // Default fallback
  return 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400&h=300&fit=crop';
};

export function DayByDayView({ tripData, onDaySelect, isCollapsed = false, onRefresh }: ViewComponentProps) {
  const { t } = useTranslation();
  const [expandedDay, setExpandedDay] = useState<number | null>(1); // First day (day 1) expanded by default
  
  // Map context for centering map on hovered cards
  const { centerOnDayComponent, setHoveredCard } = useMapContext();

  const handleDayToggle = (dayNumber: number, dayData: any) => {
    if (expandedDay === dayNumber) {
      setExpandedDay(null); // Collapse if already expanded
    } else {
      setExpandedDay(dayNumber); // Expand the selected day
      onDaySelect?.(dayNumber, dayData); // Notify parent component
    }
  };

  const handleCardHover = async (component: any, dayNumber: number) => {
    console.log('=== DAY BY DAY CARD HOVER ===');
    console.log('Component:', component);
    console.log('Day Number:', dayNumber);
    
    try {
      // Try to get coordinates for the component
      const coordinates = await geocodingUtils.getCoordinatesForComponent(component);
      
      if (coordinates) {
        console.log('Found coordinates for component:', coordinates);
        centerOnDayComponent(dayNumber, component.id, coordinates);
        setHoveredCard({ dayNumber, componentId: component.id });
      } else {
        console.log('No coordinates found for component, skipping map center');
        // Still set hovered card for visual feedback
        setHoveredCard({ dayNumber, componentId: component.id });
      }
    } catch (error) {
      console.error('Error handling card hover:', error);
      // Still set hovered card for visual feedback
      setHoveredCard({ dayNumber, componentId: component.id });
    }
  };

  const handleCardLeave = () => {
    console.log('=== DAY BY DAY CARD LEAVE ===');
    setHoveredCard(null);
  };

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1 overflow-y-auto p-4 md:p-6">
        <div className="space-y-4">
        {tripData.itinerary?.days?.map((day: any, index: number) => {
          const dayNumber = day.dayNumber || index + 1;
          const isExpanded = expandedDay === dayNumber;
          
          return (
            <Collapsible 
              key={day.id || index} 
              open={isExpanded}
              onOpenChange={() => handleDayToggle(dayNumber, day)}
              className="border rounded-lg"
            >
              {/* Day Header - Always Visible */}
              <CollapsibleTrigger asChild>
                <div className="flex items-center justify-between p-4 hover:bg-gray-50 cursor-pointer transition-colors min-h-[60px]">
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-semibold">
                      {dayNumber}
                    </div>
                    <div>
                      <h2 className="text-xl font-semibold">Day {dayNumber}</h2>
                      <p className="text-gray-600">{day.location || 'Unknown Location'}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex gap-2">
                      <Badge variant="outline">{day.theme || 'Explore'}</Badge>
                      <Badge variant="secondary">{(day.components || day.activities)?.length || 0} places</Badge>
                    </div>
                    {isExpanded ? (
                      <ChevronDown className="w-5 h-5 text-gray-500" />
                    ) : (
                      <ChevronRight className="w-5 h-5 text-gray-500" />
                    )}
                  </div>
                </div>
              </CollapsibleTrigger>

              {/* Day Content - Collapsible */}
              <CollapsibleContent>
                <div className="px-4 pb-4 space-y-4">

            {/* Places Grid */}
            {(day.components || day.activities) && (day.components || day.activities).length > 0 ? (
              <div className="grid gap-3 sm:gap-4">
                {(day.components || day.activities).map((component: any, compIndex: number) => (
                  <Card 
                    key={compIndex} 
                    className="overflow-hidden p-0 cursor-pointer transition-all duration-200 hover:shadow-lg hover:scale-[1.02]"
                    onMouseEnter={() => handleCardHover(component, dayNumber)}
                    onMouseLeave={handleCardLeave}
                  >
                    {/* Full Card Image with Gradient Overlay */}
                    <div className="relative h-64 sm:h-56 md:h-64 w-full">
                      <img 
                        src={component.media?.images?.[0] || getPlaceholderImage(component.category || component.type, component.name)}
                        alt={component.name}
                        className="w-full h-full object-cover brightness-75"
                        onError={(e) => {
                          e.currentTarget.src = getPlaceholderImage(component.category || component.type, component.name);
                        }}
                      />
                      
                      {/* Full Card Translucent Overlay */}
                      <div className="absolute inset-0 bg-black/50">
                        {/* Top Right - Rating */}
                        {(component.details?.rating || component.rating) && (
                          <div className="absolute top-2 right-2 sm:top-4 sm:right-4 bg-black/80 backdrop-blur-sm rounded-full px-2 py-1 sm:px-3 sm:py-1.5 flex items-center space-x-1 shadow-lg z-20">
                            <Star className="w-3 h-3 sm:w-4 sm:h-4 text-yellow-500 fill-current" />
                            <span className="text-xs sm:text-sm font-semibold text-white">{component.details?.rating || component.rating}</span>
                          </div>
                        )}
                        
                        {/* Top Left - Category Badge */}
                        <div className="absolute top-2 left-2 sm:top-4 sm:left-4 z-20">
                          <div className="flex items-center space-x-1 sm:space-x-2">
                            {getTypeIcon(component.type || component.category)}
                            <Badge variant="secondary" className="bg-black/80 text-white border-0 shadow-lg text-xs sm:text-sm px-2 py-1">
                              {t(`mockData.categories.${component.type || component.category || 'activity'}`, component.type || component.category || 'activity')}
                            </Badge>
                            {(component.booking?.required || component.bookingRequired) && (
                              <Badge variant="default" className="bg-orange-500 text-white border-0 shadow-lg text-xs px-2 py-1">
                                {t('mockData.booking.required')}
                              </Badge>
                            )}
                          </div>
                        </div>

                        {/* Content positioned over the translucent overlay with proper spacing */}
                        <div className="absolute bottom-0 left-0 right-0 p-3 pb-4 sm:p-4 sm:pb-6 z-10" style={{paddingTop: '50px'}}>
                          <h3 className="text-lg sm:text-xl font-bold text-white mb-3 sm:mb-4 drop-shadow-lg line-clamp-2 leading-tight">
                            {component.name || t('dayByDay.unnamedActivity')}
                          </h3>
                          
                          {/* Key Info Row - Full Width Horizontal */}
                          <div className="flex items-center justify-between gap-2 sm:gap-4">
                            {/* Cost */}
                            <div className="flex items-center space-x-1 sm:space-x-2 text-white flex-1 min-w-0">
                              <DollarSign className="w-3 h-3 sm:w-4 sm:h-4 text-white flex-shrink-0" />
                              <div className="min-w-0 flex-1">
                                <p className="font-semibold text-xs sm:text-sm text-white truncate">
                                  {component.cost?.currency || component.currency || 'EUR'} {typeof (component.cost?.pricePerPerson || component.estimatedCost) === 'object' ? '0' : (component.cost?.pricePerPerson || component.estimatedCost || '0')}
                                </p>
                                <p className="text-white/70 text-xs">{t('dayByDay.perPerson')}</p>
                              </div>
                            </div>

                            {/* Duration */}
                            <div className="flex items-center space-x-1 sm:space-x-2 text-white flex-1 min-w-0">
                              <Clock className="w-3 h-3 sm:w-4 sm:h-4 text-white flex-shrink-0" />
                              <div className="min-w-0 flex-1">
                                <p className="font-semibold text-xs sm:text-sm text-white truncate">{typeof (component.duration || component.estimatedDuration) === 'object' ? '2h' : (component.duration || component.estimatedDuration || '2h')}</p>
                                <p className="text-white/70 text-xs">{t('dayByDay.duration')}</p>
                              </div>
                            </div>
                            
                            {/* Location */}
                            <div className="flex items-center space-x-1 sm:space-x-2 text-white flex-1 min-w-0">
                              <MapPin className="w-3 h-3 sm:w-4 sm:h-4 text-white flex-shrink-0" />
                              <div className="min-w-0 flex-1">
                                <p className="font-semibold text-xs sm:text-sm text-white truncate">{typeof component.location?.name === 'object' ? t('dayByDay.unknownLocation') : (component.location?.name || t('dayByDay.unknownLocation'))}</p>
                                <p className="text-white/70 text-xs truncate">{typeof component.location?.address === 'object' ? '' : (component.location?.address || '')}</p>
                              </div>
                            </div>
                          </div>

                          {/* Opening Hours */}
                          {(component.details?.openingHours || component.openingHours) && (
                            <div className="mt-2 sm:mt-4 pt-2 sm:pt-4 border-t border-white/20">
                              <div className="flex items-center space-x-1 sm:space-x-2 text-white">
                                <Clock className="w-3 h-3 sm:w-4 sm:h-4 text-white" />
                                <div>
                                    <p className="font-semibold text-xs sm:text-sm text-white">Opening Hours</p>
                                  <p className="text-white/70 text-xs">
                                    {(() => {
                                      const hours = component.details?.openingHours || component.openingHours;
                                      if (typeof hours === 'object' || !hours || hours === 'dayByDay.openingHours') {
                                        return 'N/A';
                                      }
                                      return hours;
                                    })()}
                                  </p>
                                </div>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                    
                    {/* Additional Details Section - Only show if there's contact info */}
                    {(component.contact?.phone || component.contact?.website) && (
                      <div className="p-3 sm:p-6 space-y-2 sm:space-y-4">
                        {/* Contact Information */}
                        <div className="flex flex-wrap gap-2 sm:gap-3">
                          {component.contact?.phone && (
                            <Button variant="outline" size="sm" className="text-xs px-2 py-1 sm:px-3 sm:py-2">
                              <Phone className="w-3 h-3 mr-1" />
                              {typeof component.contact.phone === 'object' ? 'Phone' : component.contact.phone}
                            </Button>
                          )}
                          {component.contact?.website && (
                            <Button variant="outline" size="sm" className="text-xs px-2 py-1 sm:px-3 sm:py-2">
                              <Globe className="w-3 h-3 mr-1" />
                              {t('dayByDay.website')}
                            </Button>
                          )}
                        </div>
                      </div>
                    )}
                  </Card>
                ))}
              </div>
            ) : (
              <Card className="p-8 text-center">
                <div className="text-gray-500">
                  <Calendar className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                  <p className="text-xl font-medium mb-2">No activities planned for this day</p>
                  <p className="text-sm">Your personalized itinerary will appear here once planning is complete.</p>
                </div>
              </Card>
            )}
            
            {/* Transport Information */}
            {day.transport && (
              <Card className="p-4">
                <div className="flex items-center space-x-3">
                  {getTransportIcon(day.transport.mode)}
                  <div className="flex-1">
                    <h4 className="font-semibold text-sm">{day.transport.mode} Transport</h4>
                    <p className="text-gray-600 text-sm">
                      {day.transport.distance && `${day.transport.distance} • `}
                      {day.transport.duration && `${day.transport.duration}`}
                    </p>
                  </div>
                  {day.transport.cost && (
                    <div className="text-right">
                      <p className="font-semibold text-sm text-green-600">
                        {day.transport.currency || 'EUR'} {day.transport.cost}
                      </p>
                    </div>
                  )}
                </div>
              </Card>
            )}
                </div>
              </CollapsibleContent>
            </Collapsible>
          );
        }) || (
          <AutoRefreshEmptyState
            title="No itinerary data available yet"
            description="Your personalized itinerary will appear here once planning is complete."
            onRefresh={() => {
              console.log('DayByDayView: Manual refresh triggered');
              onRefresh?.();
            }}
            showRefreshButton={true}
            icon={<Calendar className="w-16 h-16 mx-auto mb-4 text-gray-300" />}
          />
        )}
        </div>
      </div>
    </div>
  );
}