/**
 * Day Card Component
 * Collapsible horizontal card showing day summary
 * Enhanced with drag & drop support for reordering activities
 */

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import {
    MapPin,
    Clock,
    DollarSign,
    ChevronDown,
    Lock,
    Save,
    X,
    Star,
    Image as ImageIcon,
    Users,
    Loader2,
    CheckCircle2,
    AlertCircle,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { 
    DndContext, 
    closestCenter, 
    PointerSensor, 
    TouchSensor, 
    useSensor, 
    useSensors 
} from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { SortableActivity } from './SortableActivity';
import { useDayActivitiesReorder } from '@/hooks/useDayActivitiesReorder';
import { motion } from 'framer-motion';
import { getDayColor } from '@/constants/dayColors';
import { useState, useEffect } from 'react';
import { buildHotelUrl, buildActivityUrl, buildBusUrl, buildTrainUrl } from '@/utils/easemytripUrlBuilder';
import { BookingModal } from '@/components/booking/BookingModal';
import { useTranslation } from '@/i18n';

interface DayCardProps {
    day: any; // NormalizedDay type
    isExpanded: boolean;
    onToggle: () => void;
    itineraryId?: string; // Required for drag & drop persistence
    enableDragDrop?: boolean; // Enable/disable drag & drop
    onRefetchNeeded?: () => void; // Callback to trigger refetch after reorder
    isGenerating?: boolean; // Whether this day is still being generated
}

const getNodeIcon = (type: string) => {
    switch (type) {
        case 'attraction':
            return '🏛️';
        case 'meal':
            return '🍽️';
        case 'hotel':
        case 'accommodation':
            return '🏨';
        case 'transit':
        case 'transport':
            return '🚗';
        default:
            return '📍';
    }
};

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

// Helper component for currency icon (replaces DollarSign with appropriate icon)
const CurrencyIcon = ({ currency, className }: { currency?: string; className?: string }) => {
    // For now, we'll use DollarSign for all currencies since lucide-react doesn't have currency-specific icons
    // The actual symbol is shown in the text
    return <DollarSign className={className} />;
};

const getPriceLevelIndicator = (priceLevel?: number) => {
    if (!priceLevel) return null;
    return '₹'.repeat(priceLevel);
};

const formatRating = (rating?: number) => {
    if (!rating) return null;
    return rating.toFixed(1);
};

const formatReviewCount = (count?: number) => {
    if (!count) return null;
    if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`;
    if (count >= 1000) return `${(count / 1000).toFixed(1)}K`;
    return count.toString();
};

const getGoogleMapsUrl = (placeId?: string): string | undefined => {
    if (!placeId) return undefined;
    return `https://www.google.com/maps/place/?q=place_id:${placeId}`;
};

// Check if activity type should show booking button
const shouldShowBookingButton = (type: string): boolean => {
    // Hide booking for attractions and restaurants (meal)
    const nonBookableTypes = ['attraction', 'meal'];
    return !nonBookableTypes.includes(type.toLowerCase());
};

// Build booking URL based on activity type
const buildBookingUrl = (node: any, day: any): string => {
    const type = node.type?.toLowerCase();
    const destination = node.location?.name || day.location || '';
    const date = day.date || '';
    
    // Accommodation - needs destination and dates
    if (type === 'hotel' || type === 'accommodation') {
        return buildHotelUrl({
            destination,
            checkIn: date,
            checkOut: date, // Will be calculated properly
            rooms: 1,
            adults: 2
        });
    }
    
    // Transport types
    if (type === 'transit' || type === 'transport') {
        // Check description for specific transport type
        const description = node.details?.description?.toLowerCase() || '';
        
        if (description.includes('train') || description.includes('railway')) {
            return buildTrainUrl({ origin: '', destination, date });
        }
        if (description.includes('bus')) {
            return buildBusUrl({ origin: '', destination, date });
        }
        // Default to activities for generic transport
        return buildActivityUrl({ destination, date });
    }
    
    // Train
    if (type === 'train' || type === 'railway') {
        return buildTrainUrl({ origin: '', destination, date });
    }
    
    // Bus
    if (type === 'bus') {
        return buildBusUrl({ origin: '', destination, date });
    }
    
    // Cab/Taxi/Car - redirect to cabs page
    if (type === 'cab' || type === 'taxi' || type === 'car') {
        return 'https://www.easemytrip.com/cabs/';
    }
    
    // For all other activities (attractions, dining, etc.) - redirect to activities page
    return buildActivityUrl({ destination, date });
};

const getPhotoUrl = (photoReference?: string, maxWidth: number = 400): string | undefined => {
    if (!photoReference) return undefined;
    // Use build-time environment variable
    const apiKey = import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY;
    if (!apiKey) return undefined;
    return `https://maps.googleapis.com/maps/api/place/photo?maxwidth=${maxWidth}&photo_reference=${photoReference}&key=${apiKey}`;
};

const formatTime = (timestamp?: string | number): string => {
    if (!timestamp) return '';
    try {
        const date = new Date(timestamp);
        // Format as "8:00 AM" or "2:30 PM"
        return date.toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    } catch (e) {
        return typeof timestamp === 'string' ? timestamp : ''; // Return original if parsing fails
    }
};

const getNodeColor = (type: string) => {
    switch (type) {
        case 'attraction':
            return 'border-l-blue-500';
        case 'meal':
            return 'border-l-orange-500';
        case 'hotel':
        case 'accommodation':
            return 'border-l-purple-500';
        case 'transit':
        case 'transport':
            return 'border-l-green-500';
        default:
            return 'border-l-gray-500';
    }
};

// Determine enrichment status from node data
const getEnrichmentStatus = (node: any): 'pending' | 'enriching' | 'enriched' | 'failed' => {
    // If explicitly set, use it
    if (node.enrichmentStatus) {
        return node.enrichmentStatus;
    }
    
    // Otherwise, infer from data presence
    const hasPhotos = node.location?.photos && node.location.photos.length > 0;
    const hasRating = node.location?.rating !== undefined;
    const hasPlaceId = node.location?.placeId !== undefined;
    
    // If has rich data, consider enriched
    if (hasPhotos || hasRating || hasPlaceId) {
        return 'enriched';
    }
    
    // If has basic location but no rich data, might be pending
    if (node.location?.name) {
        return 'pending';
    }
    
    return 'pending';
};

// Enrichment status badge component
const EnrichmentBadge = ({ status, t }: { status: 'pending' | 'enriching' | 'enriched' | 'failed'; t: any }) => {
    switch (status) {
        case 'enriching':
            return (
                <Badge variant="secondary" className="h-6 px-2 bg-blue-50 text-blue-700 border-blue-200">
                    <Loader2 className="w-3 h-3 mr-1 animate-spin" />
                    <span className="text-xs">{t('components.dayCard.enrichment.enriching')}</span>
                </Badge>
            );
        case 'enriched':
            return (
                <Badge variant="secondary" className="h-6 px-2 bg-green-50 text-green-700 border-green-200">
                    <CheckCircle2 className="w-3 h-3 mr-1" />
                    <span className="text-xs">{t('components.dayCard.enrichment.enriched')}</span>
                </Badge>
            );
        case 'failed':
            return (
                <Badge variant="secondary" className="h-6 px-2 bg-amber-50 text-amber-700 border-amber-200">
                    <AlertCircle className="w-3 h-3 mr-1" />
                    <span className="text-xs">{t('components.dayCard.enrichment.limitedData')}</span>
                </Badge>
            );
        case 'pending':
        default:
            return (
                <Badge variant="secondary" className="h-6 px-2 bg-gray-50 text-gray-600 border-gray-200">
                    <Clock className="w-3 h-3 mr-1" />
                    <span className="text-xs">{t('components.dayCard.enrichment.pending')}</span>
                </Badge>
            );
    }
};

export function DayCard({
    day,
    isExpanded,
    onToggle,
    itineraryId,
    enableDragDrop = false,
    onRefetchNeeded,
    isGenerating = false
}: DayCardProps) {
    const { t } = useTranslation();
    // Photo viewer state - now supports gallery with description
    const [selectedPhoto, setSelectedPhoto] = useState<{ photos: string[]; title: string; description?: string; currentIndex: number } | null>(null);
    // Booking modal state
    const [bookingModal, setBookingModal] = useState<{ isOpen: boolean; url: string; itemName: string } | null>(null);
    
    // State to track if we're on mobile
    const [isMobile, setIsMobile] = useState(false);
    
    useEffect(() => {
        const checkMobile = () => setIsMobile(window.innerWidth < 640);
        checkMobile();
        window.addEventListener('resize', checkMobile);
        return () => window.removeEventListener('resize', checkMobile);
    }, []);
    
    // Get hasUnsavedChanges early to use in sensor configuration
    const {
        activities,
        isReordering,
        hasUnsavedChanges,
        handleDragEnd,
        saveReorder,
        discardChanges
    } = useDayActivitiesReorder({
        itineraryId: itineraryId || '',
        dayNumber: day.dayNumber,
        initialActivities: day.nodes || [],
        onReorderSuccess: () => {
            console.log('[DayCard] Reorder successful, waiting for refetch');
        },
        onRefetchNeeded,
        autoSave: false,
    });
    
    // Configure sensors - always call both to maintain hook count
    // On mobile: require longer press initially, shorter once in edit mode
    const touchDelay = isMobile ? (hasUnsavedChanges ? 250 : 500) : 250;
    
    const sensors = useSensors(
        useSensor(PointerSensor, {
            activationConstraint: {
                distance: 8, // Require 8px movement before drag starts
            },
        }),
        useSensor(TouchSensor, {
            activationConstraint: {
                delay: touchDelay, // Longer delay on mobile without changes
                tolerance: 5,
            },
        })
    );
    
    // Get day color from shared palette (matches map markers)
    const dayColor = getDayColor(day.dayNumber);
    
    // Debug: Log activity data structure (only in development)
    if (import.meta.env.DEV && day.nodes?.length > 0) {
        console.log(`[DayCard Day ${day.dayNumber}] 🔍 FULL FIRST ACTIVITY:`, day.nodes[0]);
        console.log(`[DayCard Day ${day.dayNumber}] 🔍 LOCATION OBJECT:`, day.nodes[0].location);
        console.log(`[DayCard Day ${day.dayNumber}] Activity data check:`, {
            activityCount: day.nodes.length,
            firstActivity: {
                title: day.nodes[0].title,
                hasLocation: !!day.nodes[0].location,
                locationKeys: day.nodes[0].location ? Object.keys(day.nodes[0].location) : [],
                hasPhotos: !!day.nodes[0].location?.photos,
                photoCount: day.nodes[0].location?.photos?.length || 0,
                photosValue: day.nodes[0].location?.photos,
                hasRating: !!day.nodes[0].location?.rating,
                rating: day.nodes[0].location?.rating,
                hasReviews: !!day.nodes[0].location?.userRatingsTotal,
                reviews: day.nodes[0].location?.userRatingsTotal,
                hasPriceLevel: !!day.nodes[0].location?.priceLevel,
                priceLevel: day.nodes[0].location?.priceLevel,
                hasPlaceId: !!day.nodes[0].location?.placeId,
                placeId: day.nodes[0].location?.placeId
            },
            apiKeySet: !!import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY
        });
    }

    // Determine day status for visual styling
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const dayDate = new Date(day.date);
    dayDate.setHours(0, 0, 0, 0);

    const dayStatus = dayDate < today ? 'past' : dayDate.getTime() === today.getTime() ? 'current' : 'future';

    // Always use activities from drag & drop hook
    const displayActivities = activities;
    const activityCount = displayActivities.length;
    const totalCost = displayActivities.reduce(
        (sum: number, node: any) => sum + (node.cost?.amountPerPerson || node.cost?.pricePerPerson || node.cost?.amount || 0),
        0
    );
    // Get currency from first activity with cost, fallback to USD
    const dayCurrency = displayActivities.find((node: any) => node.cost?.currency)?.cost?.currency || 'USD';

    // Show placeholder activities if generating and no activities yet
    const hasActivities = activityCount > 0;
    const showPlaceholder = isGenerating && !hasActivities;

    const formatDate = (date: string) => {
        // Readable format: "Monday, Jan 15" or "Friday, Dec 3"
        const dateObj = new Date(date);
        const weekday = dateObj.toLocaleDateString('en-US', { weekday: 'long' });
        const monthDay = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
        return `${weekday}, ${monthDay}`;
    };

    return (
        <>
        <Card
            className={cn(
                "overflow-hidden transition-all duration-300 border-l-4",
                // Only apply past day opacity if NOT generating
                !isGenerating && dayStatus === 'past' && 'opacity-60',
                dayStatus === 'current' && 'ring-2 ring-offset-2'
            )}
            style={{
                borderLeftColor: dayColor.primary,
                ...(dayStatus === 'current' && { '--tw-ring-color': dayColor.primary } as any)
            }}
        >
            <div
                onClick={onToggle}
                className={cn(
                    'w-full text-left p-4',
                    'cursor-pointer hover:bg-muted/50 active:bg-muted transition-colors'
                )}
            >
                <CardContent className="flex-1 p-0">
                    {/* Collapsed View - Summary */}
                    <div className="flex items-center justify-between gap-3">
                        {/* Left Section: Day Badge & Title */}
                        <div className="flex items-center gap-3">
                            <div
                                className={cn(
                                    "flex items-center justify-center w-10 h-10 rounded-full font-bold text-sm transition-all flex-shrink-0",
                                    dayStatus === 'current' ? 'shadow-lg scale-110 text-white' :
                                        dayStatus === 'past' ? 'bg-muted text-muted-foreground' :
                                            'text-white'
                                )}
                                style={{
                                    backgroundColor: dayStatus === 'past' ? undefined : dayColor.primary
                                }}
                            >
                                {day.dayNumber}
                            </div>
                            <div>
                                <div className="flex items-center gap-2">
                                    <h3 className="font-semibold text-base sm:text-lg">{t('components.dayCard.day', { number: day.dayNumber })}</h3>
                                    {dayStatus === 'current' && (
                                        <span
                                            className="text-xs px-2 py-0.5 rounded-full font-medium"
                                            style={{
                                                backgroundColor: `${dayColor.primary}20`,
                                                color: dayColor.primary
                                            }}
                                        >
                                            {t('components.dayCard.today')}
                                        </span>
                                    )}
                                </div>
                                <p className="text-xs sm:text-sm text-muted-foreground">{formatDate(day.date)}</p>
                            </div>
                        </div>

                        {/* Right Section: Stats (hidden when expanded) & Toggle */}
                        <div className="flex items-center gap-3 flex-shrink-0">
                            {/* Summary Stats - Only show when collapsed */}
                            {!isExpanded && (
                                <div className="hidden md:flex items-center gap-4 text-sm text-muted-foreground">
                                    <div className="flex items-center gap-1.5">
                                        <MapPin className="w-4 h-4" />
                                        <span className="max-w-[120px] truncate">{day.location || t('components.dayCard.planning')}</span>
                                    </div>
                                    {isGenerating && !hasActivities ? (
                                        <div className="flex items-center gap-1.5">
                                            <div className="w-3 h-3 border-2 border-muted-foreground border-t-transparent rounded-full animate-spin" />
                                            <span className="text-xs">{t('components.dayCard.generatingActivities')}</span>
                                        </div>
                                    ) : (
                                        <>
                                            <div className="flex items-center gap-1.5">
                                                <Clock className="w-4 h-4" />
                                                <span>{activityCount}</span>
                                            </div>
                                            {totalCost > 0 && (
                                                <div className="flex items-center gap-1.5">
                                                    <CurrencyIcon currency={dayCurrency} className="w-4 h-4" />
                                                    <span>{getCurrencySymbol(dayCurrency)}{totalCost.toLocaleString()}</span>
                                                </div>
                                            )}
                                        </>
                                    )}
                                </div>
                            )}

                            {/* Expand/Collapse Icon */}
                            <motion.div 
                                className="w-9 h-9 flex items-center justify-center rounded-md hover:bg-muted/80 transition-colors"
                                animate={{ rotate: isExpanded ? 180 : 0 }}
                                transition={{ duration: 0.3, ease: [0.04, 0.62, 0.23, 0.98] }}
                            >
                                <ChevronDown className="w-5 h-5 text-muted-foreground" />
                            </motion.div>
                        </div>
                    </div>

                    {/* Expanded View - Full Details */}
                    {isExpanded && (
                        <div
                            className="mt-4 pt-4 border-t space-y-4"
                            onClick={(e) => e.stopPropagation()}
                        >
                                {/* Location Header */}
                                {day.location && (
                                    <div className="flex items-center gap-2 text-muted-foreground">
                                        <MapPin className="w-4 h-4" />
                                        <span className="text-sm font-medium">{day.location}</span>
                                    </div>
                                )}

                                {/* Day Summary Panel */}
                                {hasActivities && !showPlaceholder && (
                                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 p-3 bg-muted/30 rounded-lg">
                                        <div className="text-center">
                                            <div className="text-xs text-muted-foreground mb-1">{t('components.dayCard.summary.activities')}</div>
                                            <div className="text-lg font-semibold">{activityCount}</div>
                                        </div>
                                        <div className="text-center">
                                            <div className="text-xs text-muted-foreground mb-1">{t('components.dayCard.summary.budget')}</div>
                                            <div className="text-lg font-semibold">{getCurrencySymbol(dayCurrency)}{totalCost.toLocaleString()}</div>
                                        </div>
                                        <div className="text-center">
                                            <div className="text-xs text-muted-foreground mb-1">{t('components.dayCard.summary.duration')}</div>
                                            <div className="text-lg font-semibold">
                                                {displayActivities.reduce((sum: number, a: any) => {
                                                    const duration = a.timing?.duration || '0h';
                                                    const hours = parseInt(duration) || 0;
                                                    return sum + hours;
                                                }, 0)}h
                                            </div>
                                        </div>
                                        <div className="text-center">
                                            <div className="text-xs text-muted-foreground mb-1">{t('components.dayCard.summary.status')}</div>
                                            <div className="text-lg font-semibold">
                                                {displayActivities.filter((a: any) => a.bookingRef).length}/{activityCount}
                                            </div>
                                        </div>
                                    </div>
                                )}

                                <div className="space-y-3">
                                    {showPlaceholder ? (
                                        /* Placeholder for generating activities */
                                        <div className="space-y-3">
                                            {[1, 2, 3].map((i) => (
                                                <div
                                                    key={i}
                                                    className="p-2.5 rounded-lg border-l-4 border-l-gray-300 bg-muted/30 animate-pulse relative overflow-hidden"
                                                >
                                                    {/* Shimmer effect */}
                                                    <div className="absolute inset-0 -translate-x-full animate-[shimmer_2s_infinite] bg-gradient-to-r from-transparent via-white/60 to-transparent" />
                                                    <div className="flex items-start gap-3 relative">
                                                        <div className="w-6 h-6 bg-muted rounded" />
                                                        <div className="flex-1 space-y-2">
                                                            <div className="h-4 bg-muted rounded w-3/4" />
                                                            <div className="h-3 bg-muted rounded w-1/2" />
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                            <div className="text-center py-4">
                                                <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground mb-2">
                                                    <Loader2 className="w-4 h-4 animate-spin" />
                                                    <span>{t('components.dayCard.generating.message')}</span>
                                                </div>
                                                <p className="text-xs text-muted-foreground">
                                                    {t('components.dayCard.generating.enrichment')}
                                                </p>
                                            </div>
                                        </div>
                                    ) : (
                                        /* Activity List with Drag & Drop */
                                        <>
                                            {/* Save/Discard Bar - Floating on Mobile, Inline on Desktop */}
                                            {hasUnsavedChanges && (
                                                <>
                                                    {/* Desktop: Inline bar */}
                                                    <div className="hidden sm:flex items-center justify-between gap-3 p-3 mb-3 bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800 rounded-lg transition-all duration-200">
                                                        <div className="flex items-center gap-2 text-sm text-amber-900 dark:text-amber-100">
                                                            <div className="w-2 h-2 bg-amber-500 rounded-full animate-pulse" />
                                                            <span className="font-medium">{t('components.dayCard.unsavedChanges')}</span>
                                                        </div>
                                                        <div className="flex items-center gap-2">
                                                            <Button
                                                                size="sm"
                                                                variant="ghost"
                                                                onClick={discardChanges}
                                                                disabled={isReordering}
                                                                className="h-8 text-xs"
                                                            >
                                                                <X className="w-3 h-3 mr-1" />
                                                                {t('components.dayCard.discard')}
                                                            </Button>
                                                            <Button
                                                                size="sm"
                                                                onClick={saveReorder}
                                                                disabled={isReordering}
                                                                className="h-8 text-xs"
                                                            >
                                                                {isReordering ? (
                                                                    <>
                                                                        <div className="w-3 h-3 mr-1 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                                                        {t('components.dayCard.saving')}
                                                                    </>
                                                                ) : (
                                                                    <>
                                                                        <Save className="w-3 h-3 mr-1" />
                                                                        {t('components.dayCard.saveChanges')}
                                                                    </>
                                                                )}
                                                            </Button>
                                                        </div>
                                                    </div>
                                                    
                                                    {/* Mobile: Floating icon buttons */}
                                                    <div className="sm:hidden fixed bottom-24 right-4 z-50 flex flex-col gap-2">
                                                        <Button
                                                            size="icon"
                                                            variant="outline"
                                                            onClick={discardChanges}
                                                            disabled={isReordering}
                                                            className="h-4 w-4 rounded-full bg-white dark:bg-gray-900 border-2 border-gray-300 dark:border-gray-700 shadow-lg hover:shadow-xl active:scale-90 transition-all"
                                                            title={t('components.dayCard.discard')}
                                                        >
                                                            <X className="w-3 h-3" />
                                                        </Button>
                                                        <Button
                                                            size="icon"
                                                            onClick={saveReorder}
                                                            disabled={isReordering}
                                                            className="h-4 w-4 rounded-full bg-amber-500 hover:bg-amber-600 shadow-lg hover:shadow-xl active:scale-90 transition-all"
                                                            title={t('components.dayCard.saveChanges')}
                                                        >
                                                            {isReordering ? (
                                                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                                            ) : (
                                                                <Save className="w-3 h-3" />
                                                            )}
                                                        </Button>
                                                    </div>
                                                </>
                                            )}

                                            <DndContext
                                                sensors={sensors}
                                                collisionDetection={closestCenter}
                                                onDragEnd={handleDragEnd}
                                            >
                                                <SortableContext
                                                    items={displayActivities.map((a: any) => a.id)}
                                                    strategy={verticalListSortingStrategy}
                                                >
                                                    <div className="space-y-3">
                                                        {displayActivities.map((node: any) => (
                                                            <SortableActivity
                                                                key={node.id}
                                                                id={node.id}
                                                                disabled={node.locked || isReordering}
                                                            >
                                                                <div
                                                                    className={cn(
                                                                        'group relative p-3 sm:p-4 rounded-xl border-l-4 bg-white hover:bg-gray-50 transition-all duration-200 cursor-pointer overflow-hidden shadow-sm hover:shadow-md',
                                                                        getNodeColor(node.type),
                                                                        isReordering && 'opacity-50 pointer-events-none',
                                                                        // Add shimmer effect for enriching activities
                                                                        isGenerating && getEnrichmentStatus(node) === 'enriching' && 'animate-pulse'
                                                                    )}
                                                                >
                                                                    {/* Subtle gradient overlay */}
                                                                    <div className="absolute inset-0 bg-gradient-to-r from-transparent to-gray-50/30 opacity-0 group-hover:opacity-100 transition-opacity" />

                                                                    <div className="relative">
                                                                        {/* Mobile: Vertical layout, Desktop: Horizontal layout */}
                                                                        <div className="flex sm:flex-row flex-col sm:items-start gap-3">
                                                                            {/* Photo or Icon */}
                                                                            <div className="flex sm:flex-col gap-3 items-start">
                                                                                {node.location?.photos?.[0] ? (
                                                                                    <button
                                                                                        onClick={(e) => {
                                                                                            e.stopPropagation();
                                                                                            setSelectedPhoto({
                                                                                                photos: node.location.photos,
                                                                                                title: node.title,
                                                                                                description: node.details?.description,
                                                                                                currentIndex: 0
                                                                                            });
                                                                                        }}
                                                                                        className="flex-shrink-0 w-20 h-20 sm:w-16 sm:h-16 rounded-lg overflow-hidden shadow-sm hover:shadow-lg transition-all cursor-pointer relative group/photo"
                                                                                    >
                                                                                        <img
                                                                                            src={getPhotoUrl(node.location.photos[0], 200) || ''}
                                                                                            alt={node.title}
                                                                                            className="w-full h-full object-cover group-hover/photo:scale-110 transition-transform duration-300"
                                                                                            onError={(e) => {
                                                                                                const target = e.target as HTMLImageElement;
                                                                                                target.style.display = 'none';
                                                                                                target.parentElement!.innerHTML = `<div class="w-full h-full bg-gradient-to-br from-gray-100 to-gray-50 flex items-center justify-center text-xl">${getNodeIcon(node.type)}</div>`;
                                                                                            }}
                                                                                        />
                                                                                        <div className="absolute inset-0 bg-black/0 group-hover/photo:bg-black/20 transition-colors flex items-center justify-center">
                                                                                            <ImageIcon className="w-4 h-4 text-white opacity-0 group-hover/photo:opacity-100 transition-opacity" />
                                                                                        </div>
                                                                                    </button>
                                                                                ) : (
                                                                                    <div className="flex-shrink-0 w-20 h-20 sm:w-16 sm:h-16 rounded-lg bg-gradient-to-br from-gray-100 to-gray-50 flex items-center justify-center text-2xl shadow-sm group-hover:shadow-md transition-shadow">
                                                                                        {getNodeIcon(node.type)}
                                                                                    </div>
                                                                                )}
                                                                                
                                                                                {/* Mobile: Rating & Meta next to image */}
                                                                                <div className="sm:hidden flex-1 flex flex-col justify-center gap-2.5 min-w-0 py-1">
                                                                                    {/* Rating & Reviews */}
                                                                                    {node.location?.rating && (
                                                                                        <div className="flex items-center gap-2">
                                                                                            <div className="w-4 flex justify-center">
                                                                                                <Star className="w-2 h-2 fill-amber-500 text-amber-500 flex-shrink-0" />
                                                                                            </div>
                                                                                            <span className="text-xs font-bold text-amber-900 leading-none">{formatRating(node.location.rating)}</span>
                                                                                            {node.location?.userRatingsTotal && (
                                                                                                <span className="text-xs text-gray-500 leading-none">
                                                                                                    ({formatReviewCount(node.location.userRatingsTotal)})
                                                                                                </span>
                                                                                            )}
                                                                                        </div>
                                                                                    )}
                                                                                    
                                                                                    {/* Time */}
                                                                                    {node.timing?.startTime && (
                                                                                        <div className="flex items-center gap-2">
                                                                                            <div className="w-4 flex justify-center">
                                                                                                <Clock className="w-2 h-2 text-blue-500 flex-shrink-0" />
                                                                                            </div>
                                                                                            <span className="text-xs font-semibold text-gray-700 leading-none">{formatTime(node.timing.startTime)}</span>
                                                                                        </div>
                                                                                    )}
                                                                                    
                                                                                    {/* Cost & Price Level */}
                                                                                    {(node.cost?.amount || node.location?.priceLevel) && (
                                                                                        <div className="flex items-center gap-2">
                                                                                            <div className="w-4 flex justify-center">
                                                                                                <CurrencyIcon currency={node.cost.currency} className="w-2 h-2 text-emerald-600 flex-shrink-0" />
                                                                                            </div>
                                                                                            {node.cost?.amount ? (
                                                                                                <span className="text-xs font-bold text-emerald-700 leading-none">
                                                                                                    {getCurrencySymbol(node.cost.currency)}{node.cost.amount.toLocaleString()}
                                                                                                </span>
                                                                                            ) : node.location?.priceLevel ? (
                                                                                                <span className="text-xs font-bold text-emerald-600 leading-none">
                                                                                                    {getPriceLevelIndicator(node.location.priceLevel)}
                                                                                                </span>
                                                                                            ) : null}
                                                                                        </div>
                                                                                    )}
                                                                                </div>
                                                                            </div>

                                                                            {/* Content */}
                                                                            <div className="flex-1 min-w-0">
                                                                                {/* Title */}
                                                                                <div className="flex items-start justify-between gap-2 mb-2">
                                                                                    <h4 className="font-bold text-base sm:text-lg text-gray-900 leading-tight flex-1">
                                                                                        {node.title}
                                                                                    </h4>
                                                                                    {node.bookingRef && (
                                                                                        <Badge className="px-2 py-0.5 text-xs bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-50 flex-shrink-0">
                                                                                            <CheckCircle2 className="w-3 h-3 mr-1" />
                                                                                            {t('components.dayCard.booked')}
                                                                                        </Badge>
                                                                                    )}
                                                                                </div>

                                                                                {/* Desktop: Rating & Reviews */}
                                                                                <div className="hidden sm:flex flex-wrap items-center gap-2 mb-2">
                                                                                    {node.location?.rating && (
                                                                                        <div className="flex items-center gap-1 px-2 py-0.5 bg-amber-50 rounded-md">
                                                                                            <Star className="w-3 h-3 fill-amber-500 text-amber-500" />
                                                                                            <span className="text-xs font-semibold text-amber-900">{formatRating(node.location.rating)}</span>
                                                                                        </div>
                                                                                    )}
                                                                                    {node.location?.userRatingsTotal && (
                                                                                        <span className="text-xs text-gray-500">
                                                                                            {t('components.dayCard.reviews', { count: formatReviewCount(node.location.userRatingsTotal) || '0' })}
                                                                                        </span>
                                                                                    )}
                                                                                    {node.location?.priceLevel && (
                                                                                        <span className="text-xs font-semibold text-emerald-600">
                                                                                            {getPriceLevelIndicator(node.location.priceLevel)}
                                                                                        </span>
                                                                                    )}
                                                                                </div>

                                                                                {/* Address */}
                                                                                {node.location?.address && (
                                                                                    <p className="text-xs text-gray-600 flex items-start gap-1 line-clamp-1 mb-2">
                                                                                        <MapPin className="w-3 h-3 flex-shrink-0 text-gray-400 mt-0.5" />
                                                                                        <span className="truncate">{node.location.address}</span>
                                                                                    </p>
                                                                                )}

                                                                                {/* Desktop: Meta information */}
                                                                                <div className="hidden sm:flex flex-wrap items-center gap-x-3 gap-y-1 text-xs sm:text-sm text-gray-600 mb-2">
                                                                                    {node.timing?.startTime && (
                                                                                        <div className="flex items-center gap-1">
                                                                                            <Clock className="w-3 h-3 text-gray-400" />
                                                                                            <span className="font-medium">{formatTime(node.timing.startTime)}</span>
                                                                                        </div>
                                                                                    )}
                                                                                    {node.cost?.amount && (
                                                                                        <div className="flex items-center gap-1">
                                                                                            <CurrencyIcon currency={node.cost.currency} className="w-3 h-3 text-gray-400" />
                                                                                            <span className="font-semibold">
                                                                                                {getCurrencySymbol(node.cost.currency)}{node.cost.amount.toLocaleString()}
                                                                                            </span>
                                                                                        </div>
                                                                                    )}
                                                                                </div>

                                                                                {/* Description */}
                                                                                {node.details?.description && (
                                                                                    <p className="text-sm text-gray-600 leading-relaxed line-clamp-2 mb-3">
                                                                                        {node.details.description}
                                                                                    </p>
                                                                                )}

                                                                                {/* Actions */}
                                                                                <div className="flex items-center gap-2">
                                                                                {!node.bookingRef && shouldShowBookingButton(node.type) && (
                                                                                    <Button
                                                                                        size="sm"
                                                                                        className="h-9 text-sm px-4 font-semibold shadow-sm hover:shadow-md transition-all touch-manipulation active:scale-95"
                                                                                        onClick={(e) => {
                                                                                            e.stopPropagation();
                                                                                            setBookingModal({
                                                                                                isOpen: true,
                                                                                                url: buildBookingUrl(node, day),
                                                                                                itemName: node.title
                                                                                            });
                                                                                        }}
                                                                                    >
                                                                                        {t('components.dayCard.bookNow')}
                                                                                    </Button>
                                                                                )}
                                                                                {node.location?.placeId && (
                                                                                    <Button
                                                                                        size="sm"
                                                                                        variant="outline"
                                                                                        className="h-9 sm:w-auto w-9 p-0 sm:px-3 shadow-sm hover:shadow-md transition-all touch-manipulation active:scale-95"
                                                                                        onClick={(e) => {
                                                                                            e.stopPropagation();
                                                                                            window.open(getGoogleMapsUrl(node.location.placeId), '_blank');
                                                                                        }}
                                                                                        title={t('components.dayCard.viewOnMap')}
                                                                                    >
                                                                                        <MapPin className="w-4 h-4 sm:mr-2" />
                                                                                        <span className="hidden sm:inline">{t('components.dayCard.viewOnMap')}</span>
                                                                                    </Button>
                                                                                )}
                                                                                {node.location?.photos && node.location.photos.length > 1 && (
                                                                                    <Button
                                                                                        size="sm"
                                                                                        variant="outline"
                                                                                        className="h-9 sm:w-auto w-9 p-0 sm:px-3 shadow-sm hover:shadow-md transition-all touch-manipulation active:scale-95"
                                                                                        onClick={(e) => {
                                                                                            e.stopPropagation();
                                                                                            setSelectedPhoto({
                                                                                                photos: node.location.photos,
                                                                                                title: node.title,
                                                                                                currentIndex: 0
                                                                                            });
                                                                                        }}
                                                                                        title={t('components.dayCard.photos', { count: node.location.photos.length })}
                                                                                    >
                                                                                        <ImageIcon className="w-4 h-4 sm:mr-2" />
                                                                                        <span className="hidden sm:inline">{t('components.dayCard.photos', { count: node.location.photos.length })}</span>
                                                                                    </Button>
                                                                                )}
                                                                                </div>
                                                                            </div>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            </SortableActivity>
                                                        ))}
                                                    </div>
                                                </SortableContext>
                                            </DndContext>
                                        </>
                                    )}
                                </div>
                            </div>
                        )}
                </CardContent>
            </div>
        </Card>

        {/* Booking Modal */}
        {bookingModal?.isOpen && (
            <BookingModal
                isOpen={bookingModal.isOpen}
                onClose={() => setBookingModal(null)}
                bookingType="activity"
                itemName={bookingModal.itemName}
                providerUrl={bookingModal.url}
            />
        )}

        {/* Photo Gallery Modal - Proper Modal with Fixed Bounds */}
        <Dialog open={!!selectedPhoto} onOpenChange={() => setSelectedPhoto(null)}>
            <DialogContent className="max-w-4xl p-0 overflow-hidden max-h-[90vh]">
                {selectedPhoto && (
                    <div className="relative bg-black flex flex-col">
                        {/* Main Photo Container - Swipeable */}
                        <div 
                            className="relative flex items-center justify-center overflow-hidden touch-pan-y"
                            style={{ height: '60vh', minHeight: '300px', maxHeight: '400px' }}
                            onTouchStart={(e) => {
                                const touch = e.touches[0];
                                (e.currentTarget as any).touchStartX = touch.clientX;
                            }}
                            onTouchEnd={(e) => {
                                const touch = e.changedTouches[0];
                                const startX = (e.currentTarget as any).touchStartX;
                                const diff = touch.clientX - startX;
                                
                                // Swipe threshold: 50px
                                if (Math.abs(diff) > 50 && selectedPhoto.photos.length > 1) {
                                    if (diff > 0) {
                                        // Swipe right - previous photo
                                        setSelectedPhoto({
                                            ...selectedPhoto,
                                            currentIndex: (selectedPhoto.currentIndex - 1 + selectedPhoto.photos.length) % selectedPhoto.photos.length
                                        });
                                    } else {
                                        // Swipe left - next photo
                                        setSelectedPhoto({
                                            ...selectedPhoto,
                                            currentIndex: (selectedPhoto.currentIndex + 1) % selectedPhoto.photos.length
                                        });
                                    }
                                }
                            }}
                        >
                            <img
                                src={getPhotoUrl(selectedPhoto.photos[selectedPhoto.currentIndex], 1200) || ''}
                                alt={`${selectedPhoto.title} - Photo ${selectedPhoto.currentIndex + 1}`}
                                className="w-full h-full object-contain select-none"
                                draggable={false}
                                onError={(e) => {
                                    const target = e.target as HTMLImageElement;
                                    target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300"%3E%3Crect fill="%23f0f0f0" width="400" height="300"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3EImage not available%3C/text%3E%3C/svg%3E';
                                }}
                            />
                            
                            {/* Navigation Arrows - Desktop only */}
                            {selectedPhoto.photos.length > 1 && (
                                <>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setSelectedPhoto({
                                                ...selectedPhoto,
                                                currentIndex: (selectedPhoto.currentIndex - 1 + selectedPhoto.photos.length) % selectedPhoto.photos.length
                                            });
                                        }}
                                        className="hidden sm:flex absolute left-4 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full bg-black/60 hover:bg-black/80 backdrop-blur-sm items-center justify-center text-white transition-all active:scale-95"
                                        aria-label="Previous photo"
                                    >
                                        <ChevronDown className="w-7 h-7 rotate-90" />
                                    </button>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setSelectedPhoto({
                                                ...selectedPhoto,
                                                currentIndex: (selectedPhoto.currentIndex + 1) % selectedPhoto.photos.length
                                            });
                                        }}
                                        className="hidden sm:flex absolute right-4 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full bg-black/60 hover:bg-black/80 backdrop-blur-sm items-center justify-center text-white transition-all active:scale-95"
                                        aria-label="Next photo"
                                    >
                                        <ChevronDown className="w-7 h-7 -rotate-90" />
                                    </button>
                                </>
                            )}
                        </div>
                        
                        {/* Bottom Section - Info, Description and Thumbnails */}
                        <div className="bg-black p-4 max-h-[30vh] overflow-y-auto">
                            {/* Title and Counter */}
                            <div className="flex items-start justify-between gap-3 mb-2">
                                <h3 className="text-white font-semibold text-base flex-1">
                                    {selectedPhoto.title}
                                </h3>
                                {selectedPhoto.photos.length > 1 && (
                                    <p className="text-white/80 text-sm font-medium whitespace-nowrap">
                                        {selectedPhoto.currentIndex + 1} / {selectedPhoto.photos.length}
                                    </p>
                                )}
                            </div>
                            
                            {/* Description */}
                            {selectedPhoto.description && (
                                <p className="text-white/80 text-sm leading-relaxed mb-3">
                                    {selectedPhoto.description}
                                </p>
                            )}
                            
                            {/* Thumbnail Strip - Inside Modal */}
                            {selectedPhoto.photos.length > 1 && (
                                <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
                                    {selectedPhoto.photos.map((photo, index) => (
                                        <button
                                            key={index}
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setSelectedPhoto({ ...selectedPhoto, currentIndex: index });
                                            }}
                                            className={cn(
                                                "w-16 h-16 rounded-md overflow-hidden flex-shrink-0 transition-all active:scale-95",
                                                index === selectedPhoto.currentIndex 
                                                    ? "ring-2 ring-white" 
                                                    : "opacity-50 hover:opacity-75"
                                            )}
                                        >
                                            <img
                                                src={getPhotoUrl(photo, 200) || ''}
                                                alt={`Thumbnail ${index + 1}`}
                                                className="w-full h-full object-cover"
                                            />
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </DialogContent>
        </Dialog>
        </>
    );
}

// Add shimmer animation for loading states
const shimmerKeyframes = `
@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
`;

// Inject shimmer animation styles
if (typeof document !== 'undefined' && !document.getElementById('daycard-shimmer-styles')) {
    const style = document.createElement('style');
    style.id = 'daycard-shimmer-styles';
    style.textContent = shimmerKeyframes;
    document.head.appendChild(style);
}
