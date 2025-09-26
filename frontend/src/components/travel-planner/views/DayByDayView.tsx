import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Button } from '../../ui/button';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '../../ui/collapsible';
import { ViewComponentProps, ErrorBoundary } from '../shared/types';
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

export function DayByDayView({ tripData, onDaySelect, isCollapsed = false }: ViewComponentProps) {
  const { t } = useTranslation();
  const [expandedDay, setExpandedDay] = useState<number | null>(1); // First day (day 1) expanded by default

  const handleDayToggle = (dayNumber: number, dayData: any) => {
    if (expandedDay === dayNumber) {
      setExpandedDay(null); // Collapse if already expanded
    } else {
      setExpandedDay(dayNumber); // Expand the selected day
      onDaySelect?.(dayNumber, dayData); // Notify parent component
    }
  };

  return (
    <ErrorBoundary>
      <div className="p-6">
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
                <div className="flex items-center justify-between p-4 hover:bg-gray-50 cursor-pointer transition-colors">
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
              <div className="grid gap-4">
                {(day.components || day.activities).map((component: any, compIndex: number) => (
                  <Card key={compIndex} className="overflow-hidden">
                    <div className="flex">
                      {/* Content Section */}
                      <div className="flex-1 p-6">
                        <div className="flex items-start justify-between mb-4">
                          <div className="flex-1">
                            <div className="flex items-center space-x-2 mb-2">
                              {getTypeIcon(component.type || component.category)}
                              <Badge variant="outline" className="text-xs">
                                {t(`mockData.categories.${component.type || component.category || 'activity'}`, component.type || component.category || 'activity')}
                              </Badge>
                              {(component.booking?.required || component.bookingRequired) && (
                                <Badge variant="default" className="text-xs bg-orange-100 text-orange-700 border-orange-300">
                                  {t('mockData.booking.required')}
                                </Badge>
                              )}
                            </div>
                            <h3 className="text-lg font-semibold mb-2">{component.name || t('dayByDay.unnamedActivity')}</h3>
                            <p className="text-gray-600 text-sm mb-4 line-clamp-2">
                              {component.description || t('dayByDay.noDescription')}
                            </p>
                          </div>
                        </div>

                        {/* Key Info Row - Money, Location, Distance */}
                        <div className="grid grid-cols-3 gap-4 mb-4 p-3 bg-gray-50 rounded-lg">
                          {/* Cost */}
                          <div className="flex items-center space-x-2 text-sm">
                            <DollarSign className="w-4 h-4 text-green-600" />
                            <div>
                              <p className="font-semibold text-green-700">
                                {component.cost?.currency || component.currency || 'EUR'} {component.cost?.pricePerPerson || component.estimatedCost || '0'}
                              </p>
                              <p className="text-gray-500 text-xs">{t('dayByDay.perPerson')}</p>
                            </div>
                          </div>

                          {/* Location */}
                          <div className="flex items-center space-x-2 text-sm">
                            <MapPin className="w-4 h-4 text-blue-600" />
                            <div>
                              <p className="font-semibold text-blue-700">{component.location?.name || t('dayByDay.unknownLocation')}</p>
                              <p className="text-gray-500 text-xs truncate">{component.location?.address || ''}</p>
                            </div>
                          </div>

                          {/* Duration */}
                          <div className="flex items-center space-x-2 text-sm">
                            <Clock className="w-4 h-4 text-purple-600" />
                            <div>
                              <p className="font-semibold text-purple-700">{formatDuration(component.timing?.duration || component.duration)}</p>
                              <p className="text-gray-500 text-xs">
                                {(() => {
                                  const startTime = formatTime(component.timing?.startTime || component.startTime);
                                  const endTime = formatTime(component.timing?.endTime || component.endTime);
                                  
                                  if (startTime === 'TBD' && endTime === 'TBD') {
                                    return 'Time TBD';
                                  } else if (startTime === 'TBD') {
                                    return `Until ${endTime}`;
                                  } else if (endTime === 'TBD') {
                                    return `From ${startTime}`;
                                  } else {
                                    return `${startTime} - ${endTime}`;
                                  }
                                })()}
                              </p>
                            </div>
                          </div>
                        </div>

                        {/* Additional Info */}
                        <div className="flex flex-wrap gap-2 mb-4">
                          {component.category && (
                            <Badge variant="secondary" className="text-xs">
                              {component.category}
                            </Badge>
                          )}
                          {/* Show tips as badges if available */}
                          {component.tips && (
                            <Badge variant="outline" className="text-xs bg-yellow-50 text-yellow-700 border-yellow-300">
                              💡 Has Tips
                            </Badge>
                          )}
                        </div>

                        {/* Action Buttons */}
                        <div className="flex items-center space-x-2">
                          {(component.booking?.required || component.bookingRequired) && (
                            <Button size="sm" variant="outline" className="bg-orange-50 text-orange-700 border-orange-300 hover:bg-orange-100">
                              <ExternalLink className="w-4 h-4 mr-2" />
                              {t('dayByDay.bookNow')}
                            </Button>
                          )}
                          <Button size="sm" variant="ghost" className="text-blue-600 hover:text-blue-800">
                            <MapPin className="w-4 h-4 mr-2" />
                            {t('dayByDay.viewDetails')}
                          </Button>
                        </div>

                        {/* Tips */}
                        {component.tips && (
                          <div className="mt-4 p-3 bg-blue-50 rounded-lg">
                            <p className="text-sm font-medium text-blue-800 mb-1">💡 {t('dayByDay.travelTip')}</p>
                            <p className="text-sm text-blue-700">
                              {typeof component.tips === 'string' 
                                ? component.tips 
                                : component.tips.insider?.[0] || component.tips.bestTimeToVisit || 'Travel tip available'
                              }
                            </p>
                          </div>
                        )}
                      </div>

                      {/* Image Section - Right Side - Only show when not collapsed */}
                      {!isCollapsed && (
                        <div className="w-64 h-64 flex-shrink-0 relative">
                          <img 
                            src={component.media?.images?.[0] || getPlaceholderImage(component.category || component.type, component.name)}
                            alt={component.name}
                            className="w-full h-full object-cover"
                            onError={(e) => {
                              e.currentTarget.src = getPlaceholderImage(component.category || component.type, component.name);
                            }}
                          />
                          {/* Image overlay with rating if available */}
                          {(component.details?.rating || component.rating) && (
                            <div className="absolute top-2 right-2 bg-white/90 backdrop-blur-sm rounded-full px-2 py-1 flex items-center space-x-1">
                              <Star className="w-3 h-3 text-yellow-500 fill-current" />
                              <span className="text-xs font-medium">{component.details?.rating || component.rating}</span>
                            </div>
                          )}
                          {/* Category overlay */}
                          <div className="absolute bottom-2 left-2 bg-black/70 backdrop-blur-sm rounded px-2 py-1">
                            <span className="text-xs text-white font-medium">
                              {component.category || component.type || 'Activity'}
                            </span>
                          </div>
                        </div>
                      )}
                    </div>
                  </Card>
                ))}
              </div>
            ) : (
              <Card className="p-8 text-center">
                <div className="text-gray-500">
                  <Calendar className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                  <p className="text-lg font-medium mb-2">No activities planned yet</p>
                  <p className="text-sm">Your itinerary will be populated once the planning is complete.</p>
                </div>
              </Card>
            )}
            
                  {/* Day Notes */}
                  {day.notes && (
                    <Card className="p-4 bg-blue-50 border-blue-200">
                      <div className="flex items-start space-x-2">
                        <Info className="w-5 h-5 text-blue-600 mt-0.5" />
                        <div>
                          <p className="text-sm font-medium text-blue-800 mb-1">Day Notes</p>
                          <p className="text-sm text-blue-700">{day.notes}</p>
                        </div>
                      </div>
                    </Card>
                  )}
                </div>
              </CollapsibleContent>
            </Collapsible>
          );
        }) || (
          <Card className="p-8 text-center">
            <div className="text-gray-500">
              <Calendar className="w-16 h-16 mx-auto mb-4 text-gray-300" />
              <p className="text-xl font-medium mb-2">No itinerary data available yet</p>
              <p className="text-sm">Your personalized itinerary will appear here once planning is complete.</p>
            </div>
          </Card>
        )}
        </div>
      </div>
    </ErrorBoundary>
  );
}
