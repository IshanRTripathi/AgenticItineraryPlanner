/**
 * Generation Progress Banner
 * Compact, refined progress indicator for trip page during generation
 * Now with real-time WebSocket updates!
 */

import { useEffect, useState } from 'react';
import { Sparkles, Check, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useStompWebSocket } from '@/hooks/useStompWebSocket';
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
      : 1; // Start at 1% to show something immediately
  
  console.log('[GenerationProgressBanner] Initial progress:', initialProgress, 'from URL:', urlProgress);
  
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
        
        if (progressValue !== undefined && progressValue !== null) {
          const backendProgress = Number(progressValue);
          if (!isNaN(backendProgress)) {
            // Only update if backend progress is higher than current, or if we haven't initialized yet
            setProgress(prev => {
              // If we have URL progress and backend sends lower, keep URL progress
              if (urlProgress && !hasInitialized) {
                setHasInitialized(true);
                return Math.max(prev, backendProgress);
              }
              return Math.max(prev, backendProgress);
            });
            console.log('[GenerationProgressBanner] Progress update from backend:', backendProgress, 'from event:', event);
          }
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
          console.log('[GenerationProgressBanner] Message from backend:', messageValue);
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
        return 'Enriching with photos & details...';
      
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

  return (
    <div className="sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 py-4">
        {/* Glass Morphism Card */}
        <div className="p-4 rounded-2xl bg-white/30 backdrop-blur-xl border border-white/40 shadow-2xl">
          <div className="flex items-center justify-between gap-4">
            {/* Left: Icon + Message */}
            <div className="flex items-center gap-3 min-w-0 flex-1">
              <div className="relative flex-shrink-0">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary to-blue-600 flex items-center justify-center shadow-lg">
                  {progress === 100 ? (
                    <Check className="w-5 h-5 text-white" />
                  ) : (
                    <Loader2 className="w-5 h-5 text-white animate-spin" />
                  )}
                </div>
                {progress < 100 && (
                  <div className="absolute inset-0 rounded-xl border-2 border-primary/30 animate-ping" />
                )}
              </div>
              
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-gray-900">
                    {progress === 100 ? 'Itinerary Complete!' : getPhaseMessage()}
                  </span>
                  {completedDays > 0 && progress < 100 && (
                    <span className="text-xs text-gray-600 px-2 py-0.5 rounded-full bg-white/40 backdrop-blur-sm border border-white/30">
                      {completedDays}/{totalDays} days ready
                    </span>
                  )}
                </div>
                <div className="text-xs text-gray-600 mt-0.5">
                  {progress === 100 
                    ? 'All activities are now available'
                    : 'Updates appear automatically as they\'re ready'
                  }
                </div>
              </div>
            </div>

            {/* Right: Progress */}
            <div className="flex items-center gap-3 flex-shrink-0">
              <div className="hidden sm:block w-32">
                <div className="h-2.5 bg-white/30 backdrop-blur-md rounded-full overflow-hidden border border-white/40">
                  <div 
                    className="h-full bg-gradient-to-r from-primary to-blue-600 rounded-full transition-all duration-500 ease-out relative overflow-hidden"
                    style={{ width: `${progress}%` }}
                  >
                    <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/40 to-transparent animate-shimmer" 
                         style={{ backgroundSize: '200% 100%' }} />
                  </div>
                </div>
              </div>
              <span className="text-sm font-bold text-gray-900 tabular-nums min-w-[3ch]">
                {Math.round(progress)}%
              </span>
            </div>
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
