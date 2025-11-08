/**
 * Generation Progress Banner
 * Compact, refined progress indicator for trip page during generation
 * Now with real-time WebSocket updates!
 * Mobile: Minimal square box in top right corner to avoid covering navigation
 */

import { useEffect, useState } from 'react';
import { Sparkles, Check, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useStompWebSocket } from '@/hooks/useStompWebSocket';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import type { AgentProgressEvent } from '@/types/dto';

interface GenerationProgressBannerProps {
  itineraryId: string;
  itineraryStatus?: string;
  completedDays?: number;
  totalDays?: number;
  currentPhase?: string;
  onComplete?: () => void;
}

export function GenerationProgressBanner({
  itineraryId,
  itineraryStatus,
  completedDays: initialCompletedDays = 0,
  totalDays = 4, // Default fallback, should be calculated from itinerary date range
  currentPhase: initialPhase = 'skeleton',
  onComplete
}: GenerationProgressBannerProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');
  
  // Check URL for initial progress (from AgentProgress redirect)
  const urlParams = new URLSearchParams(window.location.search);
  const urlProgress = urlParams.get('progress');
  
  // Log the total days to verify it's coming from itinerary data
  console.log('[GenerationProgressBanner] Initialized with totalDays:', totalDays, 'from itinerary data');
  
  // Initialize with URL progress if available, otherwise use props-based progress
  const initialProgress = urlProgress 
    ? Math.min(parseInt(urlProgress), 99)
    : initialCompletedDays > 0 
      ? Math.min((initialCompletedDays / totalDays) * 100, 99)
      : 0; // Start at 0% and wait for WebSocket updates
  
  console.log('[GenerationProgressBanner] Initial progress:', initialProgress, 'from URL:', urlProgress, 'completedDays:', initialCompletedDays);
  
  const [progress, setProgress] = useState(initialProgress);
  const [isVisible, setIsVisible] = useState(true);
  const [hasInitialized, setHasInitialized] = useState(false);
  const [currentMessage, setCurrentMessage] = useState(
    urlProgress 
      ? 'Building your itinerary...'
      : initialCompletedDays > 0 
        ? `${initialCompletedDays} of ${totalDays} days ready`
      : 'Connecting to generation service...'
  );
  const [completedDays, setCompletedDays] = useState(initialCompletedDays);
  const [currentPhase, setCurrentPhase] = useState(initialPhase);

  // Connect to WebSocket for real-time updates
  const { isConnected } = useStompWebSocket(itineraryId, {
    onMessage: (event: AgentProgressEvent) => {
      console.log('[GenerationProgressBanner] WebSocket event:', event);

      const eventType = (event as any).updateType || (event as any).type;
      const eventData = (event as any).data || event;

      // Handle generation_complete event
      if (eventType === 'generation_complete' || event.status === 'completed') {
        console.log('[GenerationProgressBanner] Generation complete!');
        setProgress(100);
        setCurrentMessage('🎉 Itinerary complete!');
        setTimeout(() => {
          setIsVisible(false);
          onComplete?.();
        }, 3000);
        return;
      }

      // Handle day_completed event
      if (eventType === 'day_completed') {
        const dayNum = eventData.dayNumber || (event as any).dayNumber;
        if (dayNum) {
          setCompletedDays(dayNum);
          const msg = `✅ Day ${dayNum} of ${totalDays} completed`;
          setCurrentMessage(msg);
          console.log('[GenerationProgressBanner] Day completed:', { dayNum, totalDays });
        }
      }

      // Handle phase_transition event
      if (eventType === 'phase_transition') {
        const toPhase = eventData.toPhase || (event as any).toPhase;
        const message = eventData.message || (event as any).message;
        
        if (toPhase) {
          const phaseLower = toPhase.toLowerCase();
          setCurrentPhase(phaseLower);
          
          // Use backend message if provided, otherwise use default
          const msg = message || getDefaultPhaseMessage(phaseLower);
          setCurrentMessage(msg);
          console.log('[GenerationProgressBanner] Phase transition:', { toPhase, message: msg });
        }
      }

      // Handle agent_progress event - PRIORITY: Use backend progress %
      if (eventType === 'agent_progress' || (event as any).agentId) {
        // Backend sends progress percentage - check multiple locations
        const progressValue = event.progress 
          || eventData.progress 
          || (event as any).data?.progress;
        
        console.log('🟡 BANNER RECEIVE - agent_progress:', {
          'Raw Event': event,
          'Event Data': eventData,
          'Progress Value': progressValue,
          'event.progress': event.progress,
          'eventData.progress': eventData.progress,
          '(event as any).data?.progress': (event as any).data?.progress
        });
        
        if (progressValue !== undefined && progressValue !== null) {
          const backendProgress = Number(progressValue);
          if (!isNaN(backendProgress)) {
            // Only update if backend progress is higher than current, or if we haven't initialized yet
            setProgress(prev => {
              const newProgress = urlProgress && !hasInitialized 
                ? Math.max(prev, backendProgress)
                : Math.max(prev, backendProgress);
              
              console.log('🟡 BANNER Progress update:', {
                'Previous': prev,
                'Backend': backendProgress,
                'New': newProgress
              });
              
              if (urlProgress && !hasInitialized) {
                setHasInitialized(true);
              }
              return newProgress;
            });
          } else {
            console.warn('🟡 BANNER Progress is NaN:', progressValue);
          }
        } else {
          console.warn('🟡 BANNER No progress value found in event');
        }
        
        // Backend sends status/message - check multiple locations
        const messageValue = event.message 
          || event.status 
          || eventData.message 
          || eventData.status
          || (event as any).data?.message
          || (event as any).data?.status;
        
        if (messageValue) {
          setCurrentMessage(messageValue);
          console.log('🟡 BANNER Message update:', messageValue);
        }
      }

      // Handle enrichment_update event for individual activities
      if (eventType === 'enrichment_update') {
        const nodeId = eventData.nodeId || (event as any).nodeId;
        const enrichmentStatus = eventData.enrichmentStatus || (event as any).enrichmentStatus;
        
        if (nodeId && enrichmentStatus) {
          console.log('[GenerationProgressBanner] Enrichment update:', { nodeId, enrichmentStatus });
          // The itinerary will be updated via the main WebSocket handler
          // This just updates the progress message
          if (enrichmentStatus === 'enriching') {
            setCurrentMessage('📸 Enriching activity with photos...');
          } else if (enrichmentStatus === 'enriched') {
            setCurrentMessage('✅ Activity enriched with details');
          }
        }
      }

      // Handle agent_complete event
      if (eventType === 'agent_complete') {
        const agentName = eventData.agentName || (event as any).agentName;
        const itemsProcessed = eventData.itemsProcessed || (event as any).itemsProcessed;
        
        if (agentName) {
          const msg = itemsProcessed 
            ? `✅ ${agentName} completed (${itemsProcessed} items)`
            : `✅ ${agentName} completed`;
          setCurrentMessage(msg);
          console.log('[GenerationProgressBanner] Agent completed:', { agentName, itemsProcessed });
        }
      }

      // Handle warning event
      if (eventType === 'warning') {
        const msg = eventData.message || (event as any).message || 'Processing...';
        setCurrentMessage(`⚠️ ${msg}`);
        console.warn('[GenerationProgressBanner] Warning:', msg);
      }

      // Handle error event
      if (eventType === 'error') {
        const msg = eventData.message || (event as any).message || 'An error occurred';
        setCurrentMessage(`❌ ${msg}`);
        console.error('[GenerationProgressBanner] Error:', msg);
      }
    },
  });

  // Helper function for default phase messages
  const getDefaultPhaseMessage = (phase: string): string => {
    const phaseLower = phase?.toLowerCase() || '';
    
    switch (phaseLower) {
      case 'skeleton':
      case 'skeleton_generation':
      case 'day_generation':
        return 'Creating day structure...';
      
      case 'population':
      case 'agent_population':
      case 'populating':
        return 'Adding activities and places...';
      
      case 'enrichment':
      case 'enriching':
      case 'place_enrichment':
      case 'google_maps_enrichment':
        return '📸 Enriching with photos & reviews...';
      
      case 'cost_estimation':
      case 'cost':
      case 'budget':
        return 'Calculating budget estimates...';
      
      case 'finalization':
      case 'finalizing':
      case 'final':
        return 'Finalizing your itinerary...';
      
      case 'planning':
      case 'initialization':
      case 'init':
        return 'Planning your trip...';
      
      case 'validation':
      case 'validating':
        return 'Validating itinerary...';
      
      case 'optimization':
      case 'optimizing':
        return 'Optimizing route and timing...';
      
      default:
        // Clean up phase name for display
        const cleanPhase = phase
          ?.replace(/_/g, ' ')
          ?.replace(/([A-Z])/g, ' $1')
          ?.trim()
          ?.toLowerCase();
        return cleanPhase ? `Processing ${cleanPhase}...` : 'Generating itinerary...';
    }
  };

  // Update message when WebSocket connects (only if not completed)
  useEffect(() => {
    if (isConnected && progress <= 1 && itineraryStatus !== 'completed' && itineraryStatus !== 'ready') {
      setCurrentMessage('Generation started...');
    }
  }, [isConnected, progress, itineraryStatus]);

  // Debug logging
  useEffect(() => {
    console.log('[GenerationProgressBanner] State:', {
      itineraryId,
      itineraryStatus,
      completedDays,
      totalDays,
      currentPhase,
      progress,
      currentMessage,
      isConnected
    });
  }, [itineraryId, itineraryStatus, completedDays, totalDays, currentPhase, progress, currentMessage, isConnected]);

  // Hide banner when complete
  useEffect(() => {
    if (itineraryStatus === 'completed' || itineraryStatus === 'ready') {
      setProgress(100);
      setTimeout(() => {
        setIsVisible(false);
        onComplete?.();
      }, 3000);
    }
  }, [itineraryStatus, onComplete]);

  /**
   * VISIBILITY LOGIC - When should the banner show?
   * 
   * Data Sources:
   * 1. itineraryStatus (from backend API) - Source of truth
   *    - 'generating' or 'planning' = Generation in progress → SHOW BANNER
   *    - 'completed' or 'ready' = Generation done → HIDE BANNER
   *    - 'draft' or other = Not started → HIDE BANNER
   * 
   * 2. WebSocket events - Only active during generation
   *    - Provides real-time progress updates
   *    - Not available for completed itineraries
   * 
   * 3. Days array - Populated during generation
   *    - Empty at start, filled progressively
   *    - Fully populated when complete
   * 
   * Decision: Show banner ONLY when status indicates active generation
   */
  
  const isGenerating = itineraryStatus === 'generating' || itineraryStatus === 'planning';
  const isCompleted = itineraryStatus === 'completed' || itineraryStatus === 'ready';
  
  // Debug: Log visibility decision
  console.log('[GenerationProgressBanner] Visibility check:', {
    isVisible,
    itineraryStatus,
    isGenerating,
    isCompleted,
    willShow: isVisible && isGenerating && !isCompleted
  });
  
  // Rule 1: Hide if manually set to invisible (after completion animation)
  if (!isVisible) {
    console.log('[GenerationProgressBanner] Hidden - isVisible=false');
    return null;
  }
  
  // Rule 2: Hide if generation is complete
  if (isCompleted) {
    console.log('[GenerationProgressBanner] Hidden - generation completed');
    return null;
  }

  // Rule 3: Show ONLY if actively generating
  if (!isGenerating) {
    console.log('[GenerationProgressBanner] Hidden - not generating. Status:', itineraryStatus);
    return null;
  }
  
  console.log('[GenerationProgressBanner] ✅ Rendering banner!');

  // Use currentMessage from WebSocket or fallback to calculated message
  const getPhaseMessage = () => {
    if (currentMessage && currentMessage !== 'Generating itinerary...') {
      return currentMessage;
    }

    if (completedDays < totalDays) {
      return `Creating day ${completedDays + 1} of ${totalDays}`;
    }
    
    switch (currentPhase) {
      case 'population':
        return 'Adding activity details';
      case 'enrichment':
        return 'Enriching with photos & reviews';
      case 'cost_estimation':
        return 'Calculating costs';
      case 'finalization':
        return 'Finalizing itinerary';
      default:
        return 'Generating itinerary';
    }
  };

  // Mobile: Ultra-minimal progress indicator
  if (isMobile) {
    return (
      <div className="fixed top-[4.5rem] right-2 z-50">
        {/* Compact Progress Circle */}
        <div className="relative w-10 h-10 rounded-full bg-white shadow-lg border border-gray-200">
          {/* Circular Progress Ring */}
          <svg className="w-full h-full transform -rotate-90" viewBox="0 0 40 40">
            {/* Background circle */}
            <circle
              cx="20"
              cy="20"
              r="17"
              stroke="currentColor"
              strokeWidth="2.5"
              fill="none"
              className="text-gray-200"
            />
            {/* Progress circle */}
            <circle
              cx="20"
              cy="20"
              r="17"
              stroke="currentColor"
              strokeWidth="2.5"
              fill="none"
              strokeLinecap="round"
              className="text-primary transition-all duration-500"
              strokeDasharray={`${(progress / 100) * 106.8} 106.8`}
            />
          </svg>
          
          {/* Center Content */}
          <div className="absolute inset-0 flex items-center justify-center">
            {progress === 100 ? (
              <Check className="w-4 h-4 text-primary" />
            ) : (
              <span className="text-[10px] font-bold text-primary">{Math.round(progress)}</span>
            )}
          </div>
        </div>
      </div>
    );
  }

  // Desktop: Full banner
  return (
    <div className="fixed top-4 right-4 left-4 sm:left-auto z-50 max-w-md mx-auto sm:mx-0">
      {/* Compact Glass Morphism Card */}
      <div className="p-3 rounded-xl bg-white/10 backdrop-blur-xl border border-white/40 shadow-2xl">
        <div className="flex items-center gap-3">
          {/* Icon */}
          <div className="relative flex-shrink-0">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-blue-600 flex items-center justify-center shadow-lg">
              {progress === 100 ? (
                <Check className="w-4 h-4 text-white" />
              ) : (
                <Loader2 className="w-4 h-4 text-white animate-spin" />
              )}
            </div>
            {progress < 100 && (
              <div className="absolute inset-0 rounded-lg border-2 border-primary/30 animate-ping" />
            )}
          </div>
          
          {/* Message & Progress */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between gap-2 mb-1">
              <span className="text-xs font-semibold text-gray-900 truncate">
                {progress === 100 ? 'Complete!' : getPhaseMessage()}
              </span>
              <span className="text-xs font-bold text-gray-900 tabular-nums">
                {Math.round(progress)}%
              </span>
            </div>
            
            {/* Progress Bar */}
            <div className="h-1.5 bg-gray-200/30 backdrop-blur-md rounded-full overflow-hidden border border-white/40">
              <div 
                className="h-full bg-gradient-to-r from-primary to-blue-600 rounded-full transition-all duration-500 ease-out relative overflow-hidden"
                style={{ width: `${progress}%` }}
              >
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/40 to-transparent animate-shimmer" 
                     style={{ backgroundSize: '200% 100%' }} />
              </div>
            </div>
            
            {/* Days Badge */}
            {completedDays > 0 && progress < 100 && (
              <div className="mt-1">
                <span className="text-xs text-gray-700 px-2 py-0.5 rounded-full bg-primary/10 backdrop-blur-sm border border-primary/20 inline-block">
                  {completedDays}/{totalDays} days ready
                </span>
              </div>
            )}
          </div>
        </div>
      </div>
      
      {/* Add shimmer animation */}
      <style>{`
        @keyframes shimmer {
          0% { background-position: -200% 0; }
          100% { background-position: 200% 0; }
        }
        .animate-shimmer {
          animation: shimmer 2s infinite;
        }
      `}</style>
    </div>
  );
}
