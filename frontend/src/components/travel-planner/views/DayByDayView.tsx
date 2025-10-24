import React, { useState, useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Button } from '../../ui/button';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '../../ui/collapsible';
import { ViewComponentProps, ErrorBoundary } from '../shared/types';
import { AutoRefreshEmptyState } from '../../shared/AutoRefreshEmptyState';
import { useMapState } from '../../../hooks/useMapState';
import { useUnifiedItinerary } from '../../../contexts/UnifiedItineraryContext';
import { geocodingService, geocodingUtils } from '../../../services/geocodingService';
import { itineraryApi } from '../../../services/api';
import { NormalizedNode } from '../../../types/NormalizedItinerary';
import { DayCard } from '../cards/DayCard';
import { workflowSyncService } from '../../../services/workflowSyncService';
import { SyncStatusIndicator } from '../../sync/SyncStatusIndicator';
import { logger } from '../../../utils/logger';
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

interface DayByDayViewProps extends ViewComponentProps {
  mapState?: ReturnType<typeof useMapState>;
}

export function DayByDayView({ tripData, onDaySelect, isCollapsed = false, onRefresh, mapState }: DayByDayViewProps) {
  const { t } = useTranslation();
  const [expandedDay, setExpandedDay] = useState<number | null>(1);
  const [processingNodes, setProcessingNodes] = useState<Set<string>>(new Set());
  const [syncStatus, setSyncStatus] = useState<{
    syncing: boolean;
    lastSync: number | null;
    error: string | null;
  }>({
    syncing: false,
    lastSync: null,
    error: null
  });
  
  // Use with optional chaining since map might not be available
  const centerOnDayComponent = mapState?.centerOnDayComponent || (() => {
    console.log('[DayByDayView] Map not available');
  });
  const setHoveredCard = mapState?.setHoveredCard || (() => {});
  
  // Use UnifiedItinerary context (must be wrapped with UnifiedItineraryProvider)
  const {
    state: contextState,
    processWithAgents,
    setSelectedDay,
    setSelectedNodes,
    updateNode
  } = useUnifiedItinerary();

  const handleDayToggle = (dayNumber: number, dayData: any) => {
    if (expandedDay === dayNumber) {
      setExpandedDay(null); // Collapse if already expanded
    } else {
      setExpandedDay(dayNumber); // Expand the selected day
      setSelectedDay(dayNumber - 1); // Update unified context (0-indexed)
      onDaySelect?.(dayNumber, dayData); // Notify parent component
    }
  };



  // Handle agent processing for a specific node
  const handleAgentProcess = useCallback(async (nodeId: string, agentId: string) => {
    setProcessingNodes(prev => new Set(prev).add(nodeId));
    
    try {
      await processWithAgents(nodeId, [agentId]);
    } catch (error) {
      logger.error('Agent processing failed', {
        component: 'DayByDayView',
        action: 'process_with_agent',
        nodeId,
        agentId
      }, error as Error);
    } finally {
      setProcessingNodes(prev => {
        const newSet = new Set(prev);
        newSet.delete(nodeId);
        return newSet;
      });
    }
  }, [processWithAgents]);

  // Handle node selection
  const handleNodeSelect = useCallback((nodeId: string) => {
    setSelectedNodes([nodeId]);
  }, [setSelectedNodes]);

  // Simple logging for manually locked items
  useEffect(() => {
    if (tripData?.itinerary?.days) {
      const lockedItems: string[] = [];
      tripData.itinerary.days.forEach((day, dayIndex) => {
        const components = day.components || [];
        components.forEach((component: any) => {
          if (component.locked === true) {
            lockedItems.push(`"${component.name}" (Day ${dayIndex + 1})`);
          }
        });
      });
      
      if (lockedItems.length > 0) {
        logger.debug('Manually locked items detected', {
          component: 'DayByDayView',
          action: 'check_locked_items',
          lockedCount: lockedItems.length,
          lockedItems
        });
      }
    }
  }, [tripData]);

  const handleNodeLockToggle = useCallback(async (nodeId: string, locked: boolean) => {
    if (!contextState.itinerary?.id) return;
    
    try {
      setSyncStatus(prev => ({ ...prev, syncing: true, error: null }));
      
      const result = await itineraryApi.toggleNodeLock(contextState.itinerary.id, nodeId, locked);
      if (!result.success) {
        throw new Error(`Failed to ${locked ? 'lock' : 'unlock'} node`);
      }
      
      if (contextState.itinerary?.itinerary?.days) {
        const updatedItinerary = { ...contextState.itinerary };
        if (updatedItinerary.itinerary) {
          updatedItinerary.itinerary.days.forEach(day => {
          day.components.forEach(component => {
            if (component.id === nodeId) {
              component.locked = locked;
            }
          });
        });
        }
      }
      
      setSyncStatus({ syncing: false, lastSync: Date.now(), error: null });
      setTimeout(() => onRefresh?.(), 500);
      
    } catch (error) {
      logger.error('Failed to toggle lock', {
        component: 'DayByDayView',
        action: 'toggle_lock',
        nodeId
      }, error as Error);
      setSyncStatus(prev => ({ ...prev, syncing: false, error: 'Failed to sync lock state' }));
      throw error;
    }
  }, [contextState.itinerary, onRefresh]);

  const handleComponentUpdate = useCallback(async (componentId: string, updates: any) => {
    if (!contextState.itinerary?.id) return;
    
    try {
      setSyncStatus(prev => ({ ...prev, syncing: true, error: null }));
      
      await workflowSyncService.syncNodeData(contextState.itinerary.id, componentId, updates);
      
      setSyncStatus({ syncing: false, lastSync: Date.now(), error: null });
      
      setTimeout(() => onRefresh?.(), 300);
    } catch (error) {
      logger.error('Failed to sync component update', {
        component: 'DayByDayView',
        action: 'sync_component_update'
      }, error as Error);
      setSyncStatus(prev => ({ ...prev, syncing: false, error: 'Failed to sync changes' }));
    }
  }, [contextState.itinerary, onRefresh]);

  const handleCardHover = async (component: any, dayNumber: number) => {
    // Keep hover for visual feedback but don't trigger map
    setHoveredCard({ dayNumber, componentId: component.id });
  };

  const handleCardLeave = () => {
    setHoveredCard(null);
  };

  const handleCardClick = async (component: any, dayNumber: number) => {
    try {
      console.log('[DayByDayView] Card clicked:', {
        componentId: component.id,
        componentName: component.name,
        hasLocation: !!component.location,
        locationName: component.location?.name,
        locationAddress: component.location?.address
      });

      let coordinates = await geocodingUtils.getCoordinatesForComponent(component);
      
      console.log('[DayByDayView] Coordinates result:', {
        coordinates,
        hasCoordinates: !!coordinates
      });

      if (coordinates) {
        console.log('[DayByDayView] ✅ Centering map on component:', {
          dayNumber,
          componentId: component.id,
          coordinates
        });
        centerOnDayComponent(dayNumber, component.id, coordinates);
      } else {
        console.warn('[DayByDayView] ⚠️ No coordinates available - locations need enrichment', {
          componentId: component.id,
          componentName: component.name,
          locationName: component.location?.name,
          locationAddress: component.location?.address,
          suggestion: 'Run enrichment agent to populate coordinates'
        });
        
        // Show a user-friendly message
        alert(`This location hasn't been enriched with coordinates yet.\n\nLocation: ${component.name}\n\nPlease run the Enrichment agent to add map coordinates to all locations.`);
      }
      setHoveredCard({ dayNumber, componentId: component.id });
    } catch (error) {
      logger.error('Error handling card click', {
        component: 'DayByDayView',
        action: 'handle_card_click',
        dayNumber,
        componentId: component.id
      }, error as Error);
      setHoveredCard({ dayNumber, componentId: component.id });
    }
  };

  return (
    <div className="h-full flex flex-col">


      <div className="border-b px-4 py-2">
        <SyncStatusIndicator
          status={syncStatus.syncing || contextState.syncStatus === 'syncing' ? 'syncing' : syncStatus.error ? 'error' : 'synced'}
          lastSyncTime={syncStatus.lastSync ? new Date(syncStatus.lastSync) : undefined}
          onManualSync={() => setSyncStatus({ syncing: false, lastSync: null, error: null })}
        />
      </div>
      
      <div className="flex-1 overflow-y-auto p-4 md:p-6">
        <div className="space-y-4">
        {contextState.itinerary?.itinerary?.days?.map((day: any, index: number) => {
          const dayNumber = day.dayNumber || index + 1;
          const isExpanded = expandedDay === dayNumber;
          const isUpdating = contextState.syncStatus === 'syncing';
          const hasActiveAgents = contextState.activeAgents.length > 0;
          
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
                      <Badge variant="secondary">{day.components?.length || 0} places</Badge>
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
            {day.components && day.components.length > 0 ? (
              <div className="grid gap-3 sm:gap-4">
                {day.components.map((component: any, compIndex: number) => {
                  const nodeId = component.id || `${dayNumber}-${compIndex}`;
                  const isProcessing = processingNodes.has(nodeId);
                  const isSelected = contextState.selectedNodeIds.includes(nodeId);
                  
                  // Simple lock state verification
                  if (component.locked === true) {
                    logger.debug('Locked component detected', {
                      component: 'DayByDayView',
                      action: 'render_locked_component',
                      nodeId,
                      componentName: component.name
                    });
                  }
                  
                  return (
                    <DayCard
                      key={compIndex}
                      node={component}
                      dayNumber={dayNumber}
                      nodeIndex={compIndex}
                      isSelected={isSelected}
                      isProcessing={isProcessing}
                      hasActiveAgents={hasActiveAgents}
                      onNodeUpdate={handleComponentUpdate}
                      onAgentProcess={handleAgentProcess}
                      onNodeSelect={handleNodeSelect}
                      onNodeLockToggle={handleNodeLockToggle}
                      onCardHover={handleCardHover}
                      onCardLeave={handleCardLeave}
                      onCardClick={handleCardClick}
                    />
                  );
                })}
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
            title={contextState.loading ? "Loading itinerary..." : "No itinerary data available yet"}
            description={
              contextState.error 
                ? `Error: ${contextState.error}` 
                : "Your personalized itinerary will appear here once planning is complete."
            }
            onRefresh={() => {
              logger.info('Manual refresh triggered', {
                component: 'DayByDayView',
                action: 'manual_refresh'
              });
              onRefresh?.();
            }}
            showRefreshButton={!contextState.loading}
            icon={<Calendar className="w-16 h-16 mx-auto mb-4 text-gray-300" />}
          />
        )}
        </div>
      </div>
    </div>
  );
}