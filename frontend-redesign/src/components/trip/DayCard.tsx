/**
 * Day Card Component
 * Collapsible horizontal card showing day summary
 * Enhanced with drag & drop support for reordering activities
 */

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
    Calendar,
    MapPin,
    Clock,
    DollarSign,
    ChevronDown,
    ChevronUp,
    Lock,
    Save,
    X,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { DndContext, closestCenter } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { SortableActivity } from './SortableActivity';
import { useDayActivitiesReorder } from '@/hooks/useDayActivitiesReorder';
import { motion, AnimatePresence } from 'framer-motion';
import { staggerChildren, listItem, expandCollapse } from '@/utils/animations';

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

export function DayCard({ 
    day, 
    isExpanded, 
    onToggle, 
    itineraryId, 
    enableDragDrop = false,
    onRefetchNeeded,
    isGenerating = false 
}: DayCardProps) {
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
        onReorderSuccess: (newActivities) => {
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
        return new Date(date).toLocaleDateString('en-US', {
            weekday: 'short',
            month: 'short',
            day: 'numeric',
        });
    };

    return (
        <Card className="overflow-hidden">
            <button
                onClick={onToggle}
                className={cn(
                    'w-full text-left p-4 md:p-6',
                    'flex items-center justify-between',
                    'min-h-[60px] md:min-h-auto',
                    'active:bg-muted transition-colors',
                    'cursor-pointer hover:bg-muted/50'
                )}
            />
                <CardContent className="flex-1 p-0">
                {/* Collapsed View - Summary */}
                <div className="flex items-center justify-between gap-4">
                    {/* Day Number Badge */}
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-primary text-primary-foreground font-bold text-sm">
                            {day.dayNumber}
                        </div>
                        <div>
                            <h3 className="font-semibold text-base">Day {day.dayNumber}</h3>
                            <p className="text-xs text-muted-foreground">{formatDate(day.date)}</p>
                        </div>
                    </div>

                    {/* Summary Stats */}
                    <div className="flex items-center gap-4 text-sm">
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
                                        <span>${totalCost}</span>
                                    </div>
                                )}
                            </>
                        )}
                    </div>

                    {/* Expand/Collapse Icon */}
                    <Button variant="ghost" size="icon" className="shrink-0">
                        {isExpanded ? (
                            <ChevronUp className="w-5 h-5" />
                        ) : (
                            <ChevronDown className="w-5 h-5" />
                        )}
                    </Button>
                </div>

                {/* Expanded View - Full Details */}
                <AnimatePresence>
                {isExpanded && (
                    <motion.div 
                        className="mt-4 pt-4 border-t space-y-3" 
                        onClick={(e) => e.stopPropagation()}
                        initial="collapsed"
                        animate="expanded"
                        exit="collapsed"
                        variants={expandCollapse}
                    >
                        {showPlaceholder ? (
                            /* Placeholder for generating activities */
                            <div className="space-y-3">
                                {[1, 2, 3].map((i) => (
                                    <div
                                        key={i}
                                        className="p-3 rounded-lg border-l-4 border-l-gray-300 bg-muted/30 animate-pulse"
                                    >
                                        <div className="flex items-start gap-3">
                                            <div className="w-6 h-6 bg-muted rounded" />
                                            <div className="flex-1 space-y-2">
                                                <div className="h-4 bg-muted rounded w-3/4" />
                                                <div className="h-3 bg-muted rounded w-1/2" />
                                            </div>
                                        </div>
                                    </div>
                                ))}
                                <p className="text-center text-sm text-muted-foreground py-4">
                                    AI agents are creating activities for this day...
                                </p>
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
                                                className={cn(
                                                    'p-3 rounded-lg border-l-4 bg-muted/50 hover:bg-muted transition-colors',
                                                    getNodeColor(node.type),
                                                    isReordering && 'opacity-50 pointer-events-none'
                                                )}
                                            >
                                                <div className="flex items-start gap-3">
                                                    {/* Icon */}
                                                    <div className="text-xl">{getNodeIcon(node.type)}</div>

                                                    {/* Content */}
                                                    <div className="flex-1 min-w-0">
                                                        <div className="flex items-start justify-between gap-2 mb-1">
                                                            <h4 className="font-semibold text-sm">{node.title}</h4>
                                                            {node.locked && (
                                                                <Badge variant="secondary" className="shrink-0">
                                                                    <Lock className="w-3 h-3" />
                                                                </Badge>
                                                            )}
                                                        </div>

                                                        {node.location?.address && (
                                                            <p className="text-xs text-muted-foreground flex items-center gap-1 mb-1">
                                                                <MapPin className="w-3 h-3" />
                                                                {node.location.address}
                                                            </p>
                                                        )}

                                                        <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
                                                            {node.timing?.startTime && (
                                                                <div className="flex items-center gap-1">
                                                                    <Clock className="w-3 h-3" />
                                                                    {node.timing.startTime}
                                                                    {node.timing.duration && ` (${node.timing.duration})`}
                                                                </div>
                                                            )}
                                                            {node.cost?.amount && (
                                                                <div className="flex items-center gap-1">
                                                                    <DollarSign className="w-3 h-3" />
                                                                    ${node.cost.amount}
                                                                </div>
                                                            )}
                                                        </div>

                                                        {node.details?.description && (
                                                            <p className="text-xs text-muted-foreground mt-2 line-clamp-2">
                                                                {node.details.description}
                                                            </p>
                                                        )}

                                                        {/* Actions */}
                                                        <div className="flex gap-2 mt-2">
                                                            {!node.bookingRef && (
                                                                <Button size="sm" variant="outline" className="h-7 text-xs">
                                                                    Book Now
                                                                </Button>
                                                            )}
                                                            {node.bookingRef && (
                                                                <Badge variant="default" className="text-xs">Booked</Badge>
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
                                    className={cn(
                                        'p-3 rounded-lg border-l-4 bg-muted/50 hover:bg-muted transition-colors',
                                        getNodeColor(node.type)
                                    )}
                                >
                                    <div className="flex items-start gap-3">
                                        {/* Icon */}
                                        <div className="text-xl">{getNodeIcon(node.type)}</div>

                                        {/* Content */}
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-start justify-between gap-2 mb-1">
                                                <h4 className="font-semibold text-sm">{node.title}</h4>
                                                {node.locked && (
                                                    <Badge variant="secondary" className="shrink-0">
                                                        <Lock className="w-3 h-3" />
                                                    </Badge>
                                                )}
                                            </div>

                                            {node.location?.address && (
                                                <p className="text-xs text-muted-foreground flex items-center gap-1 mb-1">
                                                    <MapPin className="w-3 h-3" />
                                                    {node.location.address}
                                                </p>
                                            )}

                                            <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
                                                {node.timing?.startTime && (
                                                    <div className="flex items-center gap-1">
                                                        <Clock className="w-3 h-3" />
                                                        {node.timing.startTime}
                                                        {node.timing.duration && ` (${node.timing.duration})`}
                                                    </div>
                                                )}
                                                {node.cost?.amount && (
                                                    <div className="flex items-center gap-1">
                                                        <DollarSign className="w-3 h-3" />
                                                        ${node.cost.amount}
                                                    </div>
                                                )}
                                            </div>

                                            {node.details?.description && (
                                                <p className="text-xs text-muted-foreground mt-2 line-clamp-2">
                                                    {node.details.description}
                                                </p>
                                            )}

                                            {/* Actions */}
                                            <div className="flex gap-2 mt-2">
                                                {!node.bookingRef && (
                                                    <Button size="sm" variant="outline" className="h-7 text-xs">
                                                        Book Now
                                                    </Button>
                                                )}
                                                {node.bookingRef && (
                                                    <Badge variant="default" className="text-xs">Booked</Badge>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            ))}
                            </motion.div>
                        )}
                    </motion.div>
                )}
                </AnimatePresence>
            </CardContent>
        </Card>
    );
}
