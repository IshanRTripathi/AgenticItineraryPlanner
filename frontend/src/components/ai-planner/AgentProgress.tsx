/**
 * AI Agent Progress - Real-Time WebSocket Updates
 * Shows actual generation progress with glass morphism design
 * Mobile: Stacked compact cards with glassmorphism
 * Desktop: Side-by-side layout
 * Stays on page until user chooses to view or generation completes
 */

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Plane, Check, Loader2, AlertCircle, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useStompWebSocket } from '@/hooks/useStompWebSocket';
import { InteractiveGlobe } from '@/components/homepage/InteractiveGlobe';
import { useMediaQuery } from '@/hooks/useMediaQuery';

interface DayStatus {
  dayNumber: number;
  status: 'pending' | 'generating' | 'completed';
  activities: number;
}

interface ProgressState {
  overallProgress: number;
  currentPhase: string;
  message: string;
  daysCompleted: DayStatus[];
  totalDays: number;
  isComplete: boolean;
  hasError: boolean;
  errorMessage: string;
}

export function AgentProgress() {
  const urlParams = new URLSearchParams(window.location.search);
  const itineraryId = urlParams.get('itineraryId');
  const isMobile = useMediaQuery('(max-width: 1023px)');

  const [state, setState] = useState<ProgressState>({
    overallProgress: 0,
    currentPhase: 'Initializing',
    message: 'Starting generation...',
    daysCompleted: [],
    totalDays: 0,
    isComplete: false,
    hasError: false,
    errorMessage: '',
  });

  const [canViewPartial, setCanViewPartial] = useState(false);
  const [debugMessages, setDebugMessages] = useState<any[]>([]);

  // Real-time WebSocket connection
  const { isConnected } = useStompWebSocket(itineraryId, {
    onMessage: (message) => {
      console.log('[AgentProgress] Raw WebSocket message:', JSON.stringify(message, null, 2));

      // Add to debug messages (keep last 10)
      setDebugMessages(prev => [...prev.slice(-9), { time: new Date().toLocaleTimeString(), data: message }]);

      // Agent progress updates
      if (message.updateType === 'agent_progress') {
        // Data is nested in message.data
        const data = (message as any).data || message;
        const progress = data.progress ?? message.progress;
        const step = data.step ?? message.step ?? message.agentKind ?? data.agentId;
        const statusMessage = data.status ?? data.message ?? message.message ?? message.status;

        console.log('🟢 WEBSOCKET RECEIVE - agent_progress:', {
          'Raw Message': message,
          'Extracted Data': data,
          'Progress Value': progress,
          'Progress Type': typeof progress,
          'Step/Phase': step,
          'Status Message': statusMessage,
          'data.progress': data.progress,
          'message.progress': message.progress,
          'Will Update To': typeof progress === 'number' ? progress : 'SKIPPED (not a number)'
        });

        setState((prev) => {
          const newProgress = typeof progress === 'number' ? progress : prev.overallProgress;
          console.log('🟢 State updated - new progress:', newProgress, 'previous:', prev.overallProgress);
          
          // Enable button if progress > 30% (fallback if day_completed event doesn't fire)
          if (newProgress > 30 && !canViewPartial) {
            console.log('[AgentProgress] Enabling partial view button based on progress:', newProgress);
            setCanViewPartial(true);
          }
          
          return {
            ...prev,
            overallProgress: newProgress,
            currentPhase: step || prev.currentPhase,
            message: statusMessage || prev.message,
          };
        });
      }

      // Day completed - check both updateType and day field
      if (message.updateType === 'day_completed' || (message.day && message.day.dayNumber)) {
        // Data is nested in message.data
        const data = (message as any).data || message;
        const dayData = data.day || message.day || data;
        const dayNumber = data.dayNumber ?? dayData.dayNumber ?? message.dayNumber;
        const activities = dayData.nodes?.length ?? data.activities ?? message.activities ?? 0;
        const progress = data.progress ?? message.progress ?? 0;
        const dayMessage = data.message ?? message.message;

        console.log('[AgentProgress] Day completed:', {
          dayNumber,
          activities,
          progress,
          rawData: data,
          dayData
        });

        if (dayNumber) {
          setState((prev) => {
            // Update or add day status
            const existingDayIndex = prev.daysCompleted.findIndex(d => d.dayNumber === dayNumber);
            let updatedDays = [...prev.daysCompleted];

            if (existingDayIndex >= 0) {
              updatedDays[existingDayIndex] = { dayNumber, status: 'completed', activities };
            } else {
              updatedDays.push({ dayNumber, status: 'completed', activities });
            }

            // Sort by day number
            updatedDays.sort((a, b) => a.dayNumber - b.dayNumber);

            // Calculate total days if not set
            const totalDays = prev.totalDays || data.totalDays || message.totalDays || updatedDays.length;

            // Update overall progress if provided
            const newProgress = progress > 0 ? progress : prev.overallProgress;

            return {
              ...prev,
              daysCompleted: updatedDays,
              totalDays,
              overallProgress: newProgress,
              currentPhase: 'Skeleton Generation',
              message: dayMessage || `Day ${dayNumber} completed with ${activities} activities`,
            };
          });

          // Enable partial view button after any day is completed
          if (dayNumber >= 1) {
            console.log('[AgentProgress] Enabling partial view button for day', dayNumber);
            setCanViewPartial(true);
          }
        } else {
          console.warn('[AgentProgress] Day completed event missing dayNumber:', message);
        }
      }

      // Phase transition
      if (message.updateType === 'phase_transition') {
        // Data is nested in message.data
        const data = (message as any).data || message;
        const fromPhase = data.fromPhase ?? message.fromPhase;
        const toPhase = data.toPhase ?? message.toPhase;
        const progress = data.progress ?? message.progress;
        const phaseMessage = data.message ?? message.message;

        console.log('[AgentProgress] Phase transition:', {
          from: fromPhase,
          to: toPhase,
          progress,
          message: phaseMessage
        });

        setState((prev) => ({
          ...prev,
          overallProgress: typeof progress === 'number' ? progress : prev.overallProgress,
          currentPhase: toPhase || prev.currentPhase,
          message: phaseMessage || (toPhase ? `Moving to ${toPhase} phase` : 'Moving to next phase'),
        }));
      }

      // Generation complete
      if (message.updateType === 'generation_complete' || message.type === 'generation_complete') {
        const data = (message as any).data || message;
        const completionMessage = data.message ?? message.message ?? 'Itinerary generation complete!';

        console.log('[AgentProgress] Generation complete!');

        setState((prev) => ({
          ...prev,
          overallProgress: 100,
          isComplete: true,
          currentPhase: 'Complete',
          message: completionMessage,
        }));

        // Auto-redirect after 2 seconds when complete
        setTimeout(() => {
          window.location.href = `/trip/${itineraryId}`;
        }, 2000);
      }
    },
    onError: (err) => {
      console.error('[AgentProgress] WebSocket error:', err);
      setState((prev) => ({
        ...prev,
        hasError: true,
        errorMessage: 'Connection error. Please refresh the page.',
      }));
    },
    onConnect: () => {
      console.log('[AgentProgress] WebSocket connected successfully');
    },
    onDisconnect: () => {
      console.log('[AgentProgress] WebSocket disconnected');
    },
  });

  // Enable button when progress reaches 30% or any day is completed
  useEffect(() => {
    if (!canViewPartial && (state.overallProgress > 30 || state.daysCompleted.length > 0)) {
      console.log('[AgentProgress] Enabling button - progress:', state.overallProgress, 'days:', state.daysCompleted.length);
      setCanViewPartial(true);
    }
  }, [state.overallProgress, state.daysCompleted.length, canViewPartial]);

  // Handle manual redirect
  const handleViewPartial = () => {
    console.log('[AgentProgress] Redirecting to trip page with generating=true');
    window.location.href = `/trip/${itineraryId}?generating=true`;
  };

  // Error state
  if (!itineraryId) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50 flex items-center justify-center p-4">
        <div className="text-center">
          <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Missing Itinerary ID</h2>
          <p className="text-muted-foreground">Please start from the planner page.</p>
        </div>
      </div>
    );
  }

  // Mobile View - Ultra compact, everything visible without scrolling
  if (isMobile) {
    return (
      <div className="h-screen relative overflow-hidden flex flex-col p-4 pb-safe">
        {/* Globe Background */}
        <div className="absolute z-0 opacity-10 flex items-center justify-center">
            <InteractiveGlobe />
        </div>

        <div className="relative z-10 flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
          {/* Single Unified Card with Glassmorphism */}
          <div className="bg-white/20 backdrop-blur-2xl border border-white/30 rounded-3xl p-5 shadow-2xl">
            
            {/* Header with Progress */}
            <div className="text-center mb-4">
              <h2 className="text-xl font-bold text-gray-900 mb-1">
                {state.isComplete ? '🎉 Complete!' : 'Creating Your Trip'}
              </h2>
              <p className="text-xs text-muted-foreground">
                {state.isComplete ? 'Redirecting...' : 'Typically takes 45-60 seconds'}
              </p>
            </div>

            {/* Connection/Error Status */}
            {!isConnected && !state.hasError && (
              <div className="mb-3 p-2 rounded-lg bg-yellow-500/10 border border-yellow-500/30">
                <div className="flex items-center gap-2 text-yellow-700 text-xs">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Connecting...</span>
                </div>
              </div>
            )}

            {state.hasError && (
              <div className="mb-3 p-2 rounded-lg bg-red-500/10 border border-red-500/30">
                <div className="flex items-center gap-2 text-red-700 text-xs">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{state.errorMessage}</span>
                </div>
              </div>
            )}

            {/* Progress Circle - Smaller */}
            <div className="flex items-center justify-center mb-4">
              <div className="relative w-24 h-24">
                <svg className="transform -rotate-90 w-24 h-24">
                  <circle
                    cx="48"
                    cy="48"
                    r="42"
                    stroke="currentColor"
                    strokeWidth="5"
                    fill="none"
                    className="text-gray-200"
                  />
                  <motion.circle
                    cx="48"
                    cy="48"
                    r="42"
                    stroke="currentColor"
                    strokeWidth="5"
                    fill="none"
                    strokeLinecap="round"
                    className="text-primary"
                    initial={{ strokeDashoffset: 264 }}
                    animate={{ strokeDashoffset: 264 - (264 * state.overallProgress) / 100 }}
                    transition={{ duration: 0.5, ease: "easeOut" }}
                    style={{ strokeDasharray: 264 }}
                  />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-2xl font-bold text-gray-900">{Math.round(state.overallProgress)}%</span>
                </div>
              </div>
            </div>

            {/* Current Phase - Inline */}
            <div className="p-3 rounded-xl bg-primary/10 backdrop-blur-md border border-primary/20 mb-4">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-primary/20 flex items-center justify-center flex-shrink-0">
                  {state.isComplete ? (
                    <Check className="w-4 h-4 text-primary" />
                  ) : (
                    <Loader2 className="w-4 h-4 text-primary animate-spin" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-bold text-gray-900 truncate">{state.currentPhase}</p>
                  <p className="text-[10px] text-muted-foreground truncate">{state.message}</p>
                </div>
              </div>
            </div>

            {/* Days Generated - Compact Grid */}
            {state.daysCompleted.length > 0 && (
              <div className="mb-4">
                <p className="text-xs font-semibold text-gray-900 mb-2">Days Ready:</p>
                <div className="grid grid-cols-4 gap-1.5">
                  {state.daysCompleted.map((day) => (
                    <motion.div
                      key={day.dayNumber}
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      className="aspect-square rounded-lg bg-green-500/10 backdrop-blur-sm border border-green-500/30 flex flex-col items-center justify-center p-1"
                    >
                      <Check className="w-3.5 h-3.5 text-green-600 mb-0.5" />
                      <span className="text-[10px] font-bold text-green-700">Day {day.dayNumber}</span>
                    </motion.div>
                  ))}
                </div>
              </div>
            )}

            {state.daysCompleted.length === 0 && (
              <div className="text-center py-4 mb-4">
                <Sparkles className="w-8 h-8 text-white opacity-70 mx-auto mb-1.5" />
                <p className="text-xs text-muted-foreground text-white">Waiting for first day...</p>
              </div>
            )}

            {/* Action Buttons - Always visible */}
            <div className="space-y-2">
              {canViewPartial && !state.isComplete && (
                <Button
                  onClick={handleViewPartial}
                  className="w-full h-11 bg-gradient-to-r from-primary to-primary/90 hover:from-primary/90 hover:to-primary/80 text-white shadow-lg font-semibold touch-manipulation active:scale-95 text-sm"
                >
                  View Partial Itinerary
                </Button>
              )}

              {state.isComplete && (
                <Button
                  onClick={() => window.location.href = `/trip/${itineraryId}`}
                  className="w-full h-11 bg-gradient-to-r from-green-600 to-green-700 hover:from-green-700 hover:to-green-800 text-white shadow-lg font-semibold touch-manipulation active:scale-95 text-sm"
                >
                  View Complete Itinerary
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Desktop View - Original side-by-side layout
  return (
    <div className="min-h-screen flex flex-col lg:flex-row relative overflow-hidden">
      {/* Globe Background - Behind everything - Fullscreen and more transparent */}
      <div className="absolute z-0 opacity-0 flex items-center justify-center pointer-events-none">
          <InteractiveGlobe />
      </div>


      {/* Cards Container - Centered with equal widths */}
      <div className="flex-1 flex items-center justify-center p-4 lg:p-8 relative z-10">
        <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Left Card - Agent Status & Progress */}
          <div className="p-8 rounded-3xl bg-white/20 backdrop-blur-xl border border-white/30 shadow-2xl h-[700px] flex flex-col">
            <div className="text-center mb-6">
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                {state.isComplete ? '🎉 Complete!' : 'Generating...'}
              </h2>
              <p className="text-sm text-gray-600">
                {state.message}
              </p>
            </div>

            {/* Connection/Error Status */}
            {!isConnected && !state.hasError && (
              <div className="mb-4 p-3 rounded-xl bg-yellow-500/10 backdrop-blur-md border border-yellow-500/20">
                <div className="flex items-center gap-2 text-yellow-700 text-sm">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Connecting...</span>
                </div>
              </div>
            )}

            {state.hasError && (
              <div className="mb-4 p-3 rounded-xl bg-red-500/10 backdrop-blur-md border border-red-500/20">
                <div className="flex items-center gap-2 text-red-700 text-sm">
                  <AlertCircle className="w-4 h-4" />
                  <span>{state.errorMessage}</span>
                </div>
              </div>
            )}

            {/* Progress Circle */}
            <div className="flex items-center justify-center mb-6 flex-1">
              <div className="relative w-32 h-32">
                <svg className="transform -rotate-90 w-32 h-32">
                  <circle
                    cx="64"
                    cy="64"
                    r="56"
                    stroke="currentColor"
                    strokeWidth="8"
                    fill="none"
                    className="text-gray-200/50"
                  />
                  <motion.circle
                    cx="64"
                    cy="64"
                    r="56"
                    stroke="currentColor"
                    strokeWidth="8"
                    fill="none"
                    strokeLinecap="round"
                    className="text-primary"
                    initial={{ strokeDashoffset: 352 }}
                    animate={{ strokeDashoffset: 352 - (352 * state.overallProgress) / 100 }}
                    transition={{ duration: 0.5, ease: "easeOut" }}
                    style={{ strokeDasharray: 352 }}
                  />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-3xl font-bold text-gray-900">{Math.round(state.overallProgress)}%</span>
                </div>
              </div>
            </div>

            {/* Current Phase */}
            <div className="p-5 rounded-2xl bg-white/40 backdrop-blur-md border border-white/50 shadow-lg">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary to-blue-600 flex items-center justify-center flex-shrink-0 shadow-md">
                  {state.isComplete ? (
                    <Check className="w-6 h-6 text-white" />
                  ) : (
                    <Loader2 className="w-6 h-6 text-white animate-spin" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-base font-bold text-gray-900 truncate">{state.currentPhase}</p>
                  <p className="text-sm text-gray-600 truncate mt-0.5">{state.message}</p>
                </div>
              </div>
            </div>

            {/* Info Text */}
            <p className="text-xs text-center text-gray-600 mt-4">
              {state.isComplete
                ? 'Redirecting automatically...'
                : 'Generation typically takes 45-60 seconds'
              }
            </p>
          </div>

          {/* Right Card - Days Generated */}
          <div className="p-8 rounded-3xl bg-white/20 backdrop-blur-xl border border-white/30 shadow-2xl h-[700px] flex flex-col">
            <h3 className="text-xl font-bold text-gray-900 mb-4">Days Generated</h3>

            {state.daysCompleted.length === 0 ? (
              <div className="text-center py-12 flex-1 flex flex-col items-center justify-center">
                <Sparkles className="w-12 h-12 text-white mx-auto mb-3 opacity-70" />
                <p className="text-sm text-white">Waiting for first day...</p>
              </div>
            ) : (
              <div className="space-y-3 flex-1 overflow-y-auto pr-2">
                {state.daysCompleted.map((day) => (
                  <motion.div
                    key={day.dayNumber}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3 }}
                    className="p-5 rounded-2xl bg-white/40 backdrop-blur-md border border-white/50 shadow-lg hover:shadow-xl hover:bg-white/50 transition-all"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-green-500 to-emerald-600 flex items-center justify-center shadow-md">
                          <Check className="w-6 h-6 text-white" />
                        </div>
                        <div>
                          <p className="text-base font-bold text-gray-900">Day {day.dayNumber}</p>
                          <p className="text-sm text-gray-600 mt-0.5">{day.activities} activities</p>
                        </div>
                      </div>
                      <div className="px-4 py-1.5 rounded-xl bg-green-500/20 backdrop-blur-sm border border-green-500/40 shadow-sm">
                        <span className="text-sm font-semibold text-green-700">Ready</span>
                      </div>
                    </div>
                  </motion.div>
                ))}
              </div>
            )}

            {/* Action Buttons */}
            <div className="mt-6 space-y-3">
              {canViewPartial && !state.isComplete && (
                <Button
                  onClick={handleViewPartial}
                  className="w-full h-12 bg-gradient-to-r from-primary to-blue-600 hover:from-primary/90 hover:to-blue-600/90 text-white shadow-lg"
                >
                  View Partial Itinerary
                </Button>
              )}

              {state.isComplete && (
                <Button
                  onClick={() => window.location.href = `/trip/${itineraryId}`}
                  className="w-full h-12 bg-gradient-to-r from-green-600 to-green-700 hover:from-green-700 hover:to-green-800 text-white shadow-lg"
                >
                  View Complete Itinerary
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Add shimmer keyframes */}
      <style>{`
        @keyframes shimmer {
          0% { background-position: -200% 0; }
          100% { background-position: 200% 0; }
        }
      `}</style>
    </div>
  );
}
