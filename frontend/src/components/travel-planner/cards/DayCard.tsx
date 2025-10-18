import React, { useState, useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Button } from '../../ui/button';
import { NormalizedNode } from '../../../types/NormalizedItinerary';
import {
  Clock,
  MapPin,
  Star,
  DollarSign,
  Phone,
  Globe,
  Calendar,
  Camera,
  Utensils,
  Bed,
  Navigation,
  ShoppingBag,
  Music,
  Info,
  Lock,
  Unlock
} from 'lucide-react';

interface DayCardProps {
  node: any; // Accept both NormalizedNode and Component formats
  dayNumber: number;
  nodeIndex: number;
  isSelected?: boolean;
  isProcessing?: boolean;
  hasActiveAgents?: boolean;
  onNodeUpdate?: (nodeId: string, updates: any) => Promise<void>;
  onAgentProcess?: (nodeId: string, agentId: string) => Promise<void>;
  onNodeSelect?: (nodeId: string) => void;
  onNodeLockToggle?: (nodeId: string, locked: boolean) => Promise<void>;
  onCardHover?: (node: any, dayNumber: number) => void;
  onCardLeave?: () => void;
  onCardClick?: (node: any, dayNumber: number) => void;
}

// Helper function to get type icon
const getTypeIcon = (type: string) => {
  switch (type) {
    case 'activity': return <Camera className="w-4 h-4" />;
    case 'dining': return <Utensils className="w-4 h-4" />;
    case 'accommodation': return <Bed className="w-4 h-4" />;
    case 'transport': return <Navigation className="w-4 h-4" />;
    case 'custom': return <ShoppingBag className="w-4 h-4" />;
    default: return <Info className="w-4 h-4" />;
  }
};

// Helper function to get placeholder image based on type
const getPlaceholderImage = (type: string, name: string) => {
  const typeMap: { [key: string]: string } = {
    'activity': 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400&h=300&fit=crop',
    'dining': 'https://images.unsplash.com/photo-1602273660127-a0000560a4c1?w=400&auto=format&fit=crop',
    'accommodation': 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=400&h=300&fit=crop',
    'transport': 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=400&h=300&fit=crop',
    'custom': 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400&h=300&fit=crop'
  };

  return typeMap[type] || typeMap['custom'];
};

// Helper function to format time
const formatTime = (timeString?: string) => {
  if (!timeString) return 'TBD';

  try {
    let date: Date;

    if (/^\d{1,2}:\d{2}$/.test(timeString)) {
      const [hours, minutes] = timeString.split(':');
      date = new Date();
      date.setHours(parseInt(hours, 10), parseInt(minutes, 10), 0, 0);
    } else {
      date = new Date(timeString);
    }

    if (isNaN(date.getTime())) {
      return timeString;
    }

    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
  } catch {
    return timeString;
  }
};

// Helper function to format duration
const formatDuration = (duration?: number) => {
  if (!duration && duration !== 0) return 'TBD';

  const hours = Math.floor(duration / 60);
  const mins = duration % 60;

  if (hours > 0) {
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  }
  return `${mins}m`;
};

export function DayCard({
  node,
  dayNumber,
  nodeIndex,
  isSelected = false,
  isProcessing = false,
  hasActiveAgents = false,
  onNodeUpdate,
  onAgentProcess,
  onNodeSelect,
  onNodeLockToggle,
  onCardHover,
  onCardLeave,
  onCardClick
}: DayCardProps) {
  const { t } = useTranslation();
  const [showProgress, setShowProgress] = useState(false);
  
  // Local state for immediate UI feedback
  const [localLocked, setLocalLocked] = useState<boolean | null>(null);
  
  // Handle both NormalizedNode and Component formats
  const title = node.title || node.name || 'Unnamed Activity';
  const description = node.description || '';
  const location = node.location;
  const cost = node.cost;
  const effectiveLocked = localLocked !== null ? localLocked : (node.locked === true);

  const handleAgentClick = useCallback(async (agentId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!onAgentProcess) return;

    setShowProgress(true);
    try {
      await onAgentProcess(node.id, agentId);
    } finally {
      setShowProgress(false);
    }
  }, [node.id, onAgentProcess]);

  const handleCardClick = useCallback(() => {
    onNodeSelect?.(node.id);
    onCardClick?.(node, dayNumber);
  }, [node.id, dayNumber, onNodeSelect, onCardClick]);

  const handleMouseEnter = useCallback(() => {
    onCardHover?.(node, dayNumber);
  }, [node, dayNumber, onCardHover]);

  const handleMouseLeave = useCallback(() => {
    onCardLeave?.();
  }, [onCardLeave]);

  const handleLockToggle = useCallback(async (e: React.MouseEvent) => {
    e.stopPropagation();
    const willSetTo = !effectiveLocked;

    if (!onNodeLockToggle) return;

    // Immediate UI feedback
    setLocalLocked(willSetTo);
    setShowProgress(true);
    
    try {
      await onNodeLockToggle(node.id, willSetTo);
      // Reset local state after successful toggle
      setTimeout(() => setLocalLocked(null), 1000);
    } catch (error) {
      console.error(`Failed to ${willSetTo ? 'lock' : 'unlock'} "${title}":`, error);
      // Revert on error
      setLocalLocked(effectiveLocked);
    } finally {
      setShowProgress(false);
    }
  }, [node.id, title, effectiveLocked, onNodeLockToggle]);

  // Reset local state when node.locked changes (from refresh/reload)
  useEffect(() => {
    setLocalLocked(null);
  }, [node.locked]);

  return (
    <Card
      className={`overflow-hidden p-0 cursor-pointer transition-all duration-200 hover:shadow-lg hover:scale-[1.02] ${isSelected ? 'ring-2 ring-blue-500' : ''
        } ${isProcessing ? 'opacity-75' : ''}`}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onClick={handleCardClick}
    >
      {/* Card Image with Gradient Overlay */}
      <div className="relative h-64 sm:h-56 md:h-64 w-full">
        <img
          src={getPlaceholderImage(node.type, title)}
          alt={title}
          className="w-full h-full object-cover brightness-75"
          onError={(e) => {
            e.currentTarget.src = getPlaceholderImage(node.type, title);
          }}
        />

        {/* Translucent Overlay */}
        <div className="absolute inset-0 bg-black/50">
          {/* Top Right - Rating */}
          {node.details?.rating && (
            <div className="absolute top-2 right-2 sm:top-4 sm:right-4 bg-black/80 backdrop-blur-sm rounded-full px-2 py-1 sm:px-3 sm:py-1.5 flex items-center space-x-1 shadow-lg z-20">
              <Star className="w-3 h-3 sm:w-4 sm:h-4 text-yellow-500 fill-current" />
              <span className="text-xs sm:text-sm font-semibold text-white">
                {node.details.rating}
              </span>
            </div>
          )}

          {/* Top Left - Type Badge */}
          <div className="absolute top-2 left-2 sm:top-4 sm:left-4 z-20">
            <div className="flex items-center space-x-1 sm:space-x-2">
              {getTypeIcon(node.type)}
              <Badge variant="secondary" className="bg-black/80 text-white border-0 shadow-lg text-xs sm:text-sm px-2 py-1">
                {t(`types.${node.type}`, node.type)}
              </Badge>
              {effectiveLocked && (
                <Badge variant="destructive" className="bg-red-600 text-white border-0 shadow-lg text-xs px-2 py-1">
                  <Lock className="w-3 h-3 mr-1" />
                  Locked
                </Badge>
              )}
              {node.status === 'planned' && !effectiveLocked && (
                <Badge variant="default" className="bg-orange-500 text-white border-0 shadow-lg text-xs px-2 py-1">
                  {t('booking.required')}
                </Badge>
              )}
            </div>
          </div>

          {/* Processing Indicator */}
          {(isProcessing || showProgress) && (
            <div className="absolute inset-0 bg-black/30 flex items-center justify-center z-30">
              <div className="bg-white/90 rounded-lg p-4 flex items-center space-x-2">
                <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                <span className="text-sm font-medium">Processing...</span>
              </div>
            </div>
          )}

          {/* Content positioned over the overlay */}
          <div className="absolute bottom-0 left-0 right-0 p-3 pb-4 sm:p-4 sm:pb-6 z-10" style={{ paddingTop: '50px' }}>
            <h3 className="text-lg sm:text-xl font-bold text-white mb-3 sm:mb-4 drop-shadow-lg line-clamp-2 leading-tight">
              {title}
            </h3>

            {/* Key Info Row */}
            <div className="flex items-center justify-between gap-2 sm:gap-4">
              {/* Cost */}
              <div className="flex items-center space-x-1 sm:space-x-2 text-white flex-1 min-w-0">
                <DollarSign className="w-3 h-3 sm:w-4 sm:h-4 text-white flex-shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-xs sm:text-sm text-white truncate">
                    {node.cost?.currency || 'EUR'} {node.cost?.amount || '0'}
                  </p>
                  <p className="text-white/70 text-xs">{t('cost.perPerson')}</p>
                </div>
              </div>

              {/* Duration */}
              <div className="flex items-center space-x-1 sm:space-x-2 text-white flex-1 min-w-0">
                <Clock className="w-3 h-3 sm:w-4 sm:h-4 text-white flex-shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-xs sm:text-sm text-white truncate">
                    {formatDuration(node.timing?.durationMin)}
                  </p>
                  <p className="text-white/70 text-xs">{t('duration.label')}</p>
                </div>
              </div>

              {/* Location */}
              <div className="flex items-center space-x-1 sm:space-x-2 text-white flex-1 min-w-0">
                <MapPin className="w-3 h-3 sm:w-4 sm:h-4 text-white flex-shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-xs sm:text-sm text-white truncate">
                    {node.location?.name || t('location.unknown')}
                  </p>
                  <p className="text-white/70 text-xs truncate">
                    {node.location?.address || ''}
                  </p>
                </div>
              </div>
            </div>

            {/* Timing Information */}
            {node.timing && (
              <div className="mt-2 sm:mt-4 pt-2 sm:pt-4 border-t border-white/20">
                <div className="flex items-center space-x-1 sm:space-x-2 text-white">
                  <Clock className="w-3 h-3 sm:w-4 sm:h-4 text-white" />
                  <div>
                    <p className="font-semibold text-xs sm:text-sm text-white">
                      {node.timing.startTime && node.timing.endTime
                        ? `${formatTime(new Date(node.timing.startTime).toTimeString())} - ${formatTime(new Date(node.timing.endTime).toTimeString())}`
                        : node.timing.startTime ? formatTime(new Date(node.timing.startTime).toTimeString()) : 'Time TBD'
                      }
                    </p>
                    <p className="text-white/70 text-xs">Scheduled time</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Agent Processing and Contact Section */}
      <div className="p-3 sm:p-6 space-y-2 sm:space-y-4">
        {/* Agent Processing Buttons */}
        <div className="flex flex-wrap gap-2 sm:gap-3">
          <Button
            variant="outline"
            size="sm"
            className="text-xs px-2 py-1 sm:px-3 sm:py-2"
            onClick={(e) => handleAgentClick('enrichment', e)}
            disabled={isProcessing || hasActiveAgents || showProgress}
          >
            <Camera className="w-3 h-3 mr-1" />
            {isProcessing || showProgress ? 'Processing...' : 'Enrich'}
          </Button>

          <Button
            variant="outline"
            size="sm"
            className="text-xs px-2 py-1 sm:px-3 sm:py-2"
            onClick={(e) => handleAgentClick('booking', e)}
            disabled={isProcessing || hasActiveAgents || showProgress}
          >
            <Calendar className="w-3 h-3 mr-1" />
            {isProcessing || showProgress ? 'Processing...' : 'Book'}
          </Button>

          {/* Lock/Unlock Button */}
          <Button
            variant={effectiveLocked ? "destructive" : "outline"}
            size="sm"
            className="text-xs px-2 py-1 sm:px-3 sm:py-2"
            onClick={handleLockToggle}
            disabled={isProcessing || hasActiveAgents || showProgress}
          >
            {effectiveLocked ? (
              <>
                <Unlock className="w-3 h-3 mr-1" />
                Unlock
              </>
            ) : (
              <>
                <Lock className="w-3 h-3 mr-1" />
                Lock
              </>
            )}
          </Button>

          {/* Edit Button */}
          <Button
            variant="outline"
            size="sm"
            className="text-xs px-2 py-1 sm:px-3 sm:py-2"
            onClick={(e) => {
              e.stopPropagation();
              // Could open an edit modal here
            }}
            disabled={isProcessing || hasActiveAgents || effectiveLocked}
          >
            <Info className="w-3 h-3 mr-1" />
            Edit
          </Button>
        </div>

        {/* Contact Information - if available */}
        {/* This would be populated from enrichment agent data */}
        <div className="flex flex-wrap gap-2 sm:gap-3 pt-2 border-t">
          <Button variant="outline" size="sm" className="text-xs px-2 py-1 sm:px-3 sm:py-2" disabled>
            <Phone className="w-3 h-3 mr-1" />
            Contact
          </Button>
          <Button variant="outline" size="sm" className="text-xs px-2 py-1 sm:px-3 sm:py-2" disabled>
            <Globe className="w-3 h-3 mr-1" />
            Website
          </Button>
        </div>
      </div>
    </Card>
  );
}