/**
 * Day Card Component
 * Collapsible horizontal card showing day summary
 * Enhanced with drag & drop support for reordering activities
 */

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
    MapPin,
    Clock,
    DollarSign,
    ChevronDown,
    ChevronUp,
    Lock,
    Save,
    X,
    Star,
    ExternalLink,
    Image as ImageIcon,
    Users,
    Loader2,
    CheckCircle2,
    AlertCircle,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { DndContext, closestCenter } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { SortableActivity } from './SortableActivity';
import { useDayActivitiesReorder } from '@/hooks/useDayActivitiesReorder';
import { motion, AnimatePresence } from 'framer-motion';
import { staggerChildren, listItem, expandCollapse } from '@/utils/animations';
import { getDayColor } from '@/constants/dayColors';

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

const getPhotoUrl = (photoReference?: string, maxWidth: number = 400): string | undefined => {
    if (!photoReference) return undefined;
    // Try runtime config first, then fall back to build-time env var
    const runtimeKey = (window as any).__APP_CONFIG__?.GOOGLE_MAPS_API_KEY;
    const buildTimeKey = import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY;
    const apiKey = (runtimeKey && !runtimeKey.startsWith('__')) ? runtimeKey : buildTimeKey;
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
const EnrichmentBadge = ({ status }: { status: 'pending' | 'enriching' | 'enriched' | 'failed' }) => {
    switch (status) {
        case 'enriching':
            return (
                <Badge variant="secondary" className="h-6 px-2 bg-blue-50 text-blue-700 border-blue-200">
                    <Loader2 className="w-3 h-3 mr-1 animate-spin" />
                    <span className="text-xs">Enriching...</span>
                </Badge>
            );
        case 'enriched':
            return (
                <Badge variant="secondary" className="h-6 px-2 bg-green-50 text-green-700 border-green-200">
                    <CheckCircle2 className="w-3 h-3 mr-1" />
                    <span className="text-xs">Enriched</span>
                </Badge>
            );
        case 'failed':
            return (
                <Badge variant="secondary" className="h-6 px-2 bg-amber-50 text-amber-700 border-amber-200">
                    <AlertCircle className="w-3 h-3 mr-1" />
                    <span className="text-xs">Limited data</span>
                </Badge>
            );
        case 'pending':
        default:
            return (
                <Badge variant="secondary" className="h-6 px-2 bg-gray-50 text-gray-600 border-gray-200">
                    <Clock className="w-3 h-3 mr-1" />
                    <span className="text-xs">Pending</span>
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
    // Use drag & drop hook if enabled and itineraryId is provided
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
            // Don't mutate props - the refetch will update the data
            console.log('[DayCard] Reorder successful, waiting for refetch');
        },
        onRefetchNeeded,
        autoSave: false, // Manual save mode
    });

    // Use activities from hook if drag & drop is enabled, otherwise use day.nodes
    const displayActivities = enableDragDrop && itineraryId ? activities : (day.nodes || []);
    const activityCount = displayActivities.length;
    const totalCost = displayActivities.reduce(
        (sum: number, node: any) => sum + (node.cost?.amount || 0),
        0
    );

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
                    <div className="flex items-center justify-between gap-4">
                        {/* Day Number Badge */}
                        <div className="flex items-center gap-3">
                            <div
                                className={cn(
                                    "flex items-center justify-center w-10 h-10 rounded-full font-bold text-sm transition-all",
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
                                    <h3 className="font-semibold text-base">Day {day.dayNumber}</h3>
                                    {dayStatus === 'current' && (
                                        <span
                                            className="text-xs px-2 py-0.5 rounded-full font-medium"
                                            style={{
                                                backgroundColor: `${dayColor.primary}20`,
                                                color: dayColor.primary
                                            }}
                                        >
                                            Today
                                        </span>
                                    )}
                                </div>
                                <p className="text-xs text-muted-foreground">{formatDate(day.date)}</p>
                            </div>
                        </div>

                        {/* Summary Stats */}
                        <div className="flex items-center gap-4 text-sm flex-wrap">
                            <div className="flex items-center gap-1 text-muted-foreground">
                                <MapPin className="w-4 h-4" />
                                <span className="hidden sm:inline">{day.location || 'Planning...'}</span>
                            </div>
                            {isGenerating && !hasActivities ? (
                                <div className="flex items-center gap-2 text-muted-foreground">
                                    <div className="w-3 h-3 border-2 border-muted-foreground border-t-transparent rounded-full animate-spin" />
                                    <span className="text-xs">Generating activities...</span>
                                </div>
                            ) : (
                                <>
                                    <div className="flex items-center gap-1 text-muted-foreground">
                                        <Clock className="w-4 h-4" />
                                        <span>{activityCount} activities</span>
                                    </div>
                                    {totalCost > 0 && (
                                        <div className="flex items-center gap-1 text-muted-foreground">
                                            <DollarSign className="w-4 h-4" />
                                            <span>${totalCost.toLocaleString()}</span>
                                        </div>
                                    )}
                                </>
                            )}
                        </div>

                        {/* Activity Preview Icons - Show in collapsed state */}
                        {!isExpanded && hasActivities && (
                            <div className="flex items-center gap-1 mt-2">
                                {displayActivities.slice(0, 6).map((activity: any, i: number) => (
                                    <motion.div
                                        key={activity.id}
                                        initial={{ scale: 0, opacity: 0 }}
                                        animate={{ scale: 1, opacity: 1 }}
                                        transition={{ delay: i * 0.05 }}
                                        className="w-8 h-8 rounded-full bg-muted flex items-center justify-center text-sm border-2 border-background shadow-sm"
                                        title={activity.title}
                                    >
                                        {getNodeIcon(activity.type)}
                                    </motion.div>
                                ))}
                                {displayActivities.length > 6 && (
                                    <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center text-xs text-muted-foreground border-2 border-background">
                                        +{displayActivities.length - 6}
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Expand/Collapse Icon */}
                        <div className="shrink-0 w-10 h-10 flex items-center justify-center rounded-md hover:bg-muted/80 transition-colors">
                            {isExpanded ? (
                                <ChevronUp className="w-5 h-5 text-muted-foreground" />
                            ) : (
                                <ChevronDown className="w-5 h-5 text-muted-foreground" />
                            )}
                        </div>
                    </div>

                    {/* Expanded View - Full Details */}
                    <AnimatePresence>
                        {isExpanded && (
                            <motion.div
                                className="mt-4 pt-4 border-t space-y-4"
                                onClick={(e) => e.stopPropagation()}
                                initial="collapsed"
                                animate="expanded"
                                exit="collapsed"
                                variants={expandCollapse}
                            >
                                {/* Day Summary Panel */}
                                {hasActivities && !showPlaceholder && (
                                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 p-3 bg-muted/30 rounded-lg">
                                        <div className="text-center">
                                            <div className="text-xs text-muted-foreground mb-1">Activities</div>
                                            <div className="text-lg font-semibold">{activityCount}</div>
                                        </div>
                                        <div className="text-center">
                                            <div className="text-xs text-muted-foreground mb-1">Budget</div>
                                            <div className="text-lg font-semibold">${totalCost.toLocaleString()}</div>
                                        </div>
                                        <div className="text-center">
                                            <div className="text-xs text-muted-foreground mb-1">Duration</div>
                                            <div className="text-lg font-semibold">
                                                {displayActivities.reduce((sum: number, a: any) => {
                                                    const duration = a.timing?.duration || '0h';
                                                    const hours = parseInt(duration) || 0;
                                                    return sum + hours;
                                                }, 0)}h
                                            </div>
                                        </div>
                                        <div className="text-center">
                                            <div className="text-xs text-muted-foreground mb-1">Status</div>
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
                                                    <span>AI agents are creating activities for this day...</span>
                                                </div>
                                                <p className="text-xs text-muted-foreground">
                                                    Activities will be enriched with photos and details
                                                </p>
                                            </div>
                                        </div>
                                    ) : enableDragDrop && itineraryId ? (
                                        /* Drag & Drop Enabled */
                                        <>
                                            {/* Save/Discard Bar */}
                                            <AnimatePresence>
                                                {hasUnsavedChanges && (
                                                    <motion.div
                                                        initial={{ opacity: 0, y: -10 }}
                                                        animate={{ opacity: 1, y: 0 }}
                                                        exit={{ opacity: 0, y: -10 }}
                                                        className="flex items-center justify-between gap-3 p-3 mb-3 bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800 rounded-lg"
                                                    >
                                                        <div className="flex items-center gap-2 text-sm text-amber-900 dark:text-amber-100">
                                                            <div className="w-2 h-2 bg-amber-500 rounded-full animate-pulse" />
                                                            <span className="font-medium">Unsaved changes</span>
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
                                                                Discard
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
                                                                        Saving...
                                                                    </>
                                                                ) : (
                                                                    <>
                                                                        <Save className="w-3 h-3 mr-1" />
                                                                        Save Changes
                                                                    </>
                                                                )}
                                                            </Button>
                                                        </div>
                                                    </motion.div>
                                                )}
                                            </AnimatePresence>

                                            <DndContext
                                                collisionDetection={closestCenter}
                                                onDragEnd={handleDragEnd}
                                            >
                                                <SortableContext
                                                    items={displayActivities.map((a: any) => a.id)}
                                                    strategy={verticalListSortingStrategy}
                                                >
                                                    <motion.div
                                                        variants={staggerChildren}
                                                        initial="initial"
                                                        animate="animate"
                                                    >
                                                        {displayActivities.map((node: any) => (
                                                            <SortableActivity
                                                                key={node.id}
                                                                id={node.id}
                                                                disabled={node.locked || isReordering}
                                                            >
                                                                <motion.div
                                                                    variants={listItem}
                                                                    whileHover={{ y: -2, boxShadow: "0 8px 16px rgba(0,0,0,0.08)" }}
                                                                    transition={{ duration: 0.2 }}
                                                                    className={cn(
                                                                        'group relative p-3 rounded-xl border-l-4 bg-white hover:bg-gray-50/50 transition-all cursor-pointer overflow-hidden',
                                                                        getNodeColor(node.type),
                                                                        isReordering && 'opacity-50 pointer-events-none',
                                                                        // Add shimmer effect for enriching activities
                                                                        isGenerating && getEnrichmentStatus(node) === 'enriching' && 'animate-pulse'
                                                                    )}
                                                                >
                                                                    {/* Subtle gradient overlay */}
                                                                    <div className="absolute inset-0 bg-gradient-to-r from-transparent to-gray-50/30 opacity-0 group-hover:opacity-100 transition-opacity" />

                                                                    <div className="relative flex items-start gap-3">
                                                                        {/* Photo or Icon */}
                                                                        {node.location?.photos?.[0] ? (
                                                                            <div className="flex-shrink-0 w-16 h-16 rounded-xl overflow-hidden shadow-sm group-hover:shadow-md transition-shadow">
                                                                                <img
                                                                                    src={getPhotoUrl(node.location.photos[0], 200) || ''}
                                                                                    alt={node.title}
                                                                                    className="w-full h-full object-cover"
                                                                                    onError={(e) => {
                                                                                        const target = e.target as HTMLImageElement;
                                                                                        target.style.display = 'none';
                                                                                        target.parentElement!.innerHTML = `<div class="w-full h-full bg-gradient-to-br from-gray-100 to-gray-50 flex items-center justify-center text-2xl">${getNodeIcon(node.type)}</div>`;
                                                                                    }}
                                                                                />
                                                                            </div>
                                                                        ) : (
                                                                            <div className="flex-shrink-0 w-16 h-16 rounded-xl bg-gradient-to-br from-gray-100 to-gray-50 flex items-center justify-center text-2xl shadow-sm group-hover:shadow-md transition-shadow">
                                                                                {getNodeIcon(node.type)}
                                                                            </div>
                                                                        )}

                                                                        {/* Content */}
                                                                        <div className="flex-1 min-w-0">
                                                                            {/* Header */}
                                                                            <div className="flex items-start justify-between gap-2 mb-1">
                                                                                <div className="flex-1 min-w-0">
                                                                                    <h4 className="font-semibold text-base text-gray-900 mb-1 line-clamp-1">
                                                                                        {node.title}
                                                                                    </h4>

                                                                                    {/* Rating, Reviews, Price Level */}
                                                                                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                                                                                        {node.location?.rating && (
                                                                                            <div className="flex items-center gap-1 text-xs font-medium text-amber-600">
                                                                                                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                                                                                                <span>{formatRating(node.location.rating)}</span>
                                                                                            </div>
                                                                                        )}
                                                                                        {node.location?.userRatingsTotal && (
                                                                                            <div className="flex items-center gap-1 text-xs text-gray-500">
                                                                                                <Users className="w-3 h-3" />
                                                                                                <span>{formatReviewCount(node.location.userRatingsTotal)}</span>
                                                                                            </div>
                                                                                        )}
                                                                                        {node.location?.priceLevel && (
                                                                                            <span className="text-xs font-medium text-green-600">
                                                                                                {getPriceLevelIndicator(node.location.priceLevel)}
                                                                                            </span>
                                                                                        )}
                                                                                    </div>

                                                                                    {node.location?.address && (
                                                                                        <p className="text-xs text-gray-600 flex items-center gap-1 line-clamp-1">
                                                                                            <MapPin className="w-3 h-3 flex-shrink-0" />
                                                                                            <span className="truncate">{node.location.address}</span>
                                                                                        </p>
                                                                                    )}
                                                                                </div>

                                                                                {/* Status badges */}
                                                                                <div className="flex items-center gap-1 flex-shrink-0 flex-wrap">
                                                                                    {/* Show enrichment status during generation */}
                                                                                    {isGenerating && (
                                                                                        <EnrichmentBadge status={getEnrichmentStatus(node)} />
                                                                                    )}
                                                                                    {node.locked && (
                                                                                        <Badge variant="secondary" className="h-6 px-2">
                                                                                            <Lock className="w-3 h-3" />
                                                                                        </Badge>
                                                                                    )}
                                                                                    {node.bookingRef && (
                                                                                        <Badge className="h-6 px-2 bg-green-100 text-green-700 hover:bg-green-100">
                                                                                            ✓ Booked
                                                                                        </Badge>
                                                                                    )}
                                                                                </div>
                                                                            </div>

                                                                            {/* Meta information */}
                                                                            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-600 mb-2">
                                                                                {node.timing?.startTime && (
                                                                                    <div className="flex items-center gap-1 font-medium">
                                                                                        <Clock className="w-3.5 h-3.5" />
                                                                                        <span>{formatTime(node.timing.startTime)}</span>
                                                                                        {node.timing.duration && (
                                                                                            <span className="text-gray-400">• {node.timing.duration}</span>
                                                                                        )}
                                                                                    </div>
                                                                                )}
                                                                                {node.cost?.amount && (
                                                                                    <div className="flex items-center gap-1 font-medium">
                                                                                        <DollarSign className="w-3.5 h-3.5" />
                                                                                        <span>₹{node.cost.amount.toLocaleString()}</span>
                                                                                    </div>
                                                                                )}
                                                                            </div>

                                                                            {/* Description */}
                                                                            {node.details?.description && (
                                                                                <p className="text-xs text-gray-600 leading-relaxed line-clamp-2 mb-3">
                                                                                    {node.details.description}
                                                                                </p>
                                                                            )}

                                                                            {/* Actions - Stack vertically on mobile */}
                                                                            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                                                                                {!node.bookingRef && (
                                                                                    <Button
                                                                                        size="sm"
                                                                                        variant="outline"
                                                                                        className="min-h-[44px] sm:h-8 text-xs px-3 hover:bg-primary hover:text-white hover:border-primary transition-colors touch-manipulation active:scale-95"
                                                                                    >
                                                                                        Book Now
                                                                                    </Button>
                                                                                )}
                                                                                {node.location?.placeId && (
                                                                                    <Button
                                                                                        size="sm"
                                                                                        variant="ghost"
                                                                                        className="min-h-[44px] sm:h-8 text-xs px-3 touch-manipulation active:scale-95"
                                                                                        onClick={() => window.open(getGoogleMapsUrl(node.location.placeId), '_blank')}
                                                                                    >
                                                                                        <ExternalLink className="w-3 h-3 mr-1" />
                                                                                        View on Map
                                                                                    </Button>
                                                                                )}
                                                                                {node.location?.photos && node.location.photos.length > 1 && (
                                                                                    <Badge variant="secondary" className="h-8 px-2 text-xs">
                                                                                        <ImageIcon className="w-3 h-3 mr-1" />
                                                                                        {node.location.photos.length} photos
                                                                                    </Badge>
                                                                                )}
                                                                            </div>
                                                                        </div>
                                                                    </div>
                                                                </motion.div>
                                                            </SortableActivity>
                                                        ))}
                                                    </motion.div>
                                                </SortableContext>
                                            </DndContext>
                                        </>
                                    ) : (
                                        /* Drag & Drop Disabled - Original View */
                                        <motion.div
                                            variants={staggerChildren}
                                            initial="initial"
                                            animate="animate"
                                        >
                                            {displayActivities.map((node: any, index: number) => (
                                                <motion.div
                                                    key={index}
                                                    variants={listItem}
                                                    whileHover={{ y: -2, boxShadow: "0 8px 16px rgba(0,0,0,0.08)" }}
                                                    transition={{ duration: 0.2 }}
                                                    className={cn(
                                                        'group relative p-4 rounded-xl border-l-4 bg-white hover:bg-gray-50/50 transition-all cursor-pointer overflow-hidden',
                                                        getNodeColor(node.type),
                                                        // Add shimmer effect for enriching activities
                                                        isGenerating && getEnrichmentStatus(node) === 'enriching' && 'animate-pulse'
                                                    )}
                                                >
                                                    {/* Subtle gradient overlay */}
                                                    <div className="absolute inset-0 bg-gradient-to-r from-transparent to-gray-50/30 opacity-0 group-hover:opacity-100 transition-opacity" />

                                                    <div className="relative flex items-start gap-4">
                                                        {/* Photo or Icon - Responsive sizing */}
                                                        {node.location?.photos?.[0] ? (
                                                            <div className="flex-shrink-0 w-12 sm:w-14 md:w-16 h-12 sm:h-14 md:h-16 rounded-xl overflow-hidden shadow-sm group-hover:shadow-md transition-shadow">
                                                                <img
                                                                    src={getPhotoUrl(node.location.photos[0], 200) || ''}
                                                                    alt={node.title}
                                                                    className="w-full h-full object-cover"
                                                                    onError={(e) => {
                                                                        const target = e.target as HTMLImageElement;
                                                                        target.style.display = 'none';
                                                                        target.parentElement!.innerHTML = `<div class="w-full h-full bg-gradient-to-br from-gray-100 to-gray-50 flex items-center justify-center text-xl sm:text-2xl">${getNodeIcon(node.type)}</div>`;
                                                                    }}
                                                                />
                                                            </div>
                                                        ) : (
                                                            <div className="flex-shrink-0 w-12 sm:w-14 md:w-16 h-12 sm:h-14 md:h-16 rounded-xl bg-gradient-to-br from-gray-100 to-gray-50 flex items-center justify-center text-xl sm:text-2xl shadow-sm group-hover:shadow-md transition-shadow">
                                                                {getNodeIcon(node.type)}
                                                            </div>
                                                        )}

                                                        {/* Content */}
                                                        <div className="flex-1 min-w-0">
                                                            {/* Header */}
                                                            <div className="flex items-start justify-between gap-2 mb-1">
                                                                <div className="flex-1 min-w-0">
                                                                    <h4 className="font-semibold text-base text-gray-900 mb-1 line-clamp-1">
                                                                        {node.title}
                                                                    </h4>

                                                                    {/* Rating, Reviews, Price Level - Stack on mobile */}
                                                                    <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-2 mb-1">
                                                                        {node.location?.rating && (
                                                                            <div className="flex items-center gap-1 text-xs font-medium text-amber-600">
                                                                                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                                                                                <span>{formatRating(node.location.rating)}</span>
                                                                            </div>
                                                                        )}
                                                                        {node.location?.userRatingsTotal && (
                                                                            <div className="flex items-center gap-1 text-xs text-gray-500">
                                                                                <Users className="w-3 h-3" />
                                                                                <span>{formatReviewCount(node.location.userRatingsTotal)}</span>
                                                                            </div>
                                                                        )}
                                                                        {node.location?.priceLevel && (
                                                                            <span className="text-xs font-medium text-green-600">
                                                                                {getPriceLevelIndicator(node.location.priceLevel)}
                                                                            </span>
                                                                        )}
                                                                    </div>

                                                                    {node.location?.address && (
                                                                        <p className="text-xs text-gray-600 flex items-center gap-1 line-clamp-1">
                                                                            <MapPin className="w-3 h-3 flex-shrink-0" />
                                                                            <span className="truncate">{node.location.address}</span>
                                                                        </p>
                                                                    )}
                                                                </div>

                                                                {/* Status badges */}
                                                                <div className="flex items-center gap-1 flex-shrink-0 flex-wrap">
                                                                    {/* Show enrichment status during generation */}
                                                                    {isGenerating && (
                                                                        <EnrichmentBadge status={getEnrichmentStatus(node)} />
                                                                    )}
                                                                    {node.locked && (
                                                                        <Badge variant="secondary" className="h-6 px-2">
                                                                            <Lock className="w-3 h-3" />
                                                                        </Badge>
                                                                    )}
                                                                    {node.bookingRef && (
                                                                        <Badge className="h-6 px-2 bg-green-100 text-green-700 hover:bg-green-100">
                                                                            ✓ Booked
                                                                        </Badge>
                                                                    )}
                                                                </div>
                                                            </div>

                                                            {/* Meta information */}
                                                            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-600 mb-2">
                                                                {node.timing?.startTime && (
                                                                    <div className="flex items-center gap-1 font-medium">
                                                                        <Clock className="w-3.5 h-3.5" />
                                                                        <span>{formatTime(node.timing.startTime)}</span>
                                                                        {node.timing.duration && (
                                                                            <span className="text-gray-400">• {node.timing.duration}</span>
                                                                        )}
                                                                    </div>
                                                                )}
                                                                {node.cost?.amount && (
                                                                    <div className="flex items-center gap-1 font-medium">
                                                                        <DollarSign className="w-3.5 h-3.5" />
                                                                        <span>₹{node.cost.amount.toLocaleString()}</span>
                                                                    </div>
                                                                )}
                                                            </div>

                                                            {/* Description */}
                                                            {node.details?.description && (
                                                                <p className="text-xs text-gray-600 leading-relaxed line-clamp-2 mb-3">
                                                                    {node.details.description}
                                                                </p>
                                                            )}

                                                            {/* Actions - Stack vertically on mobile */}
                                                            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                                                                {!node.bookingRef && (
                                                                    <Button
                                                                        size="sm"
                                                                        variant="outline"
                                                                        className="min-h-[44px] sm:h-8 text-xs px-3 hover:bg-primary hover:text-white hover:border-primary transition-colors touch-manipulation active:scale-95"
                                                                    >
                                                                        Book Now
                                                                    </Button>
                                                                )}
                                                                {node.location?.placeId && (
                                                                    <Button
                                                                        size="sm"
                                                                        variant="ghost"
                                                                        className="min-h-[44px] sm:h-8 text-xs px-3 touch-manipulation active:scale-95"
                                                                        onClick={() => window.open(getGoogleMapsUrl(node.location.placeId), '_blank')}
                                                                    >
                                                                        <ExternalLink className="w-3 h-3 mr-1" />
                                                                        View on Map
                                                                    </Button>
                                                                )}
                                                                {node.location?.photos && node.location.photos.length > 1 && (
                                                                    <Badge variant="secondary" className="h-8 px-2 text-xs">
                                                                        <ImageIcon className="w-3 h-3 mr-1" />
                                                                        {node.location.photos.length} photos
                                                                    </Badge>
                                                                )}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </motion.div>
                                            ))}
                                        </motion.div>
                                    )}
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </CardContent>
            </div>
        </Card>
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
