/**
 * Day Card Component
 * Collapsible horizontal card showing day summary
 */

import { useState } from 'react';
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
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface DayCardProps {
    day: any; // NormalizedDay type
    isExpanded: boolean;
    onToggle: () => void;
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

export function DayCard({ day, isExpanded, onToggle }: DayCardProps) {
    const activityCount = day.nodes?.length || 0;
    const totalCost = day.nodes?.reduce(
        (sum: number, node: any) => sum + (node.cost?.amount || 0),
        0
    );

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
            >
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
                            <span className="hidden sm:inline">{day.location}</span>
                        </div>
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
                {isExpanded && (
                    <div className="mt-4 pt-4 border-t space-y-3" onClick={(e) => e.stopPropagation()}>
                        {day.nodes?.map((node: any, index: number) => (
                            <div
                                key={index}
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
                            </div>
                        ))}
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
