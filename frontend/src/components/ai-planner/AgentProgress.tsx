/**
 * AI Agent Progress - Real-Time WebSocket Updates
 * Shows actual generation progress with glass morphism design
 * Stays on page until user chooses to view or generation completes
 */

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Plane, Check, Loader2, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useStompWebSocket } from '@/hooks/useStompWebSocket';

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
        const step = data.step ?? message.step ?? message.agentKind;
        const statusMessage = data.status ?? data.message ?? message.message ?? message.status;
        
        console.log('[AgentProgress] Agent progress update:', {
          progress,
          step,
          message: statusMessage,
          rawData: data
        });
        
        setState((prev) => ({
          ...prev,
          overallProgress: typeof progress === 'number' ? progress : prev.overallProgress,
          currentPhase: step || prev.currentPhase,
          message: statusMessage || prev.message,
        }));
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
          
          // Enable partial view button after first day
          if (dayNumber === 1) {
            console.log('[AgentProgress] Enabling partial view button');
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
          message: phaseMessage || `Moving to ${toPhase} phase`,
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

  // Handle manual redirect
  const handleViewPartial = () => {
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

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl">
        {/* Logo */}
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-white to-blue-100 border-2 border-primary/20 flex items-center justify-center shadow-lg mb-3">
            <Plane className="w-8 h-8 text-primary" />
          </div>
          <h1 className="text-3xl font-bold">
            <span className="text-gray-900">Easy</span>
            <span className="text-primary">Trip</span>
          </h1>
        </div>

        {/* Main Progress Card - Glass Morphism */}
        <div className="p-8 rounded-3xl bg-white/40 backdrop-blur-xl border border-white/60 shadow-2xl">
          {/* Header */}
          <div className="text-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              {state.isComplete ? 'Itinerary Complete!' : 'Generating Your Itinerary'}
            </h2>
            <p className="text-sm text-muted-foreground">
              {state.message}
            </p>
          </div>

          {/* Connection Status */}
          {!isConnected && !state.hasError && (
            <div className="mb-6 p-4 rounded-xl bg-yellow-50/80 backdrop-blur-sm border border-yellow-200/50">
              <div className="flex items-center gap-2 text-yellow-800">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span className="text-sm">Connecting to generation service...</span>
              </div>
            </div>
          )}

          {/* Error State */}
          {state.hasError && (
            <div className="mb-6 p-4 rounded-xl bg-red-50/80 backdrop-blur-sm border border-red-200/50">
              <div className="flex items-center gap-2 text-red-800">
                <AlertCircle className="w-4 h-4" />
                <span className="text-sm">{state.errorMessage}</span>
              </div>
            </div>
          )}

          {/* Overall Progress Bar */}
          <div className="mb-8">
            <div className="flex items-center justify-between text-sm text-muted-foreground mb-3">
              <span className="font-semibold">Overall Progress</span>
              <span className="font-semibold tabular-nums">{Math.round(state.overallProgress)}%</span>
            </div>
            
            <div className="h-3 bg-gray-200/50 rounded-full overflow-hidden backdrop-blur-sm">
              <motion.div
                className="h-full bg-gradient-to-r from-primary to-blue-600 rounded-full relative overflow-hidden"
                initial={{ width: 0 }}
                animate={{ width: `${state.overallProgress}%` }}
                transition={{ duration: 0.5, ease: "easeOut" }}
              >
                {/* Shimmer effect */}
                <div 
                  className="absolute inset-0 bg-gradient-to-r from-transparent via-white/40 to-transparent"
                  style={{
                    animation: 'shimmer 2s infinite',
                    backgroundSize: '200% 100%'
                  }}
                />
              </motion.div>
            </div>
          </div>

          {/* Current Phase */}
          <div className="mb-6 p-4 rounded-xl bg-primary/5 backdrop-blur-sm border border-primary/10">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
                <Loader2 className="w-5 h-5 text-primary animate-spin" />
              </div>
              <div>
                <p className="text-sm font-semibold text-gray-900">{state.currentPhase}</p>
                <p className="text-xs text-muted-foreground">{state.message}</p>
              </div>
            </div>
          </div>

          {/* Days Progress */}
          {state.daysCompleted.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-900 mb-3">Days Generated</h3>
              <div className="space-y-2">
                {state.daysCompleted.map((day) => (
                  <motion.div
                    key={day.dayNumber}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="flex items-center justify-between p-3 rounded-lg bg-green-50/80 backdrop-blur-sm border border-green-200/50"
                  >
                    <div className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-green-600" />
                      <span className="text-sm font-medium text-gray-900">Day {day.dayNumber}</span>
                    </div>
                    <span className="text-xs text-muted-foreground">{day.activities} activities</span>
                  </motion.div>
                ))}
              </div>
            </div>
          )}

          {/* Action Button */}
          <div className="flex flex-col gap-3">
            {canViewPartial && !state.isComplete && (
              <Button
                onClick={handleViewPartial}
                className="w-full h-12 bg-gradient-to-r from-primary to-blue-600 hover:from-primary/90 hover:to-blue-600/90 text-white shadow-md"
              >
                View Partially Generated Itinerary
              </Button>
            )}
            
            {state.isComplete && (
              <Button
                onClick={() => window.location.href = `/trip/${itineraryId}`}
                className="w-full h-12 bg-gradient-to-r from-green-600 to-green-700 hover:from-green-700 hover:to-green-800 text-white shadow-md"
              >
                View Complete Itinerary
              </Button>
            )}
            
            {!canViewPartial && !state.isComplete && (
              <div className="text-center text-sm text-muted-foreground">
                Button will appear once first day is generated
              </div>
            )}
          </div>
        </div>

        {/* Info Text */}
        <p className="text-xs text-center text-muted-foreground mt-4">
          {state.isComplete 
            ? 'Redirecting automatically in 2 seconds...'
            : 'Generation typically takes 45-60 seconds'
          }
        </p>

        {/* Debug Panel - Remove in production */}
        {debugMessages.length > 0 && (
          <details className="mt-4 p-4 rounded-xl bg-gray-100/80 backdrop-blur-sm border border-gray-300/50">
            <summary className="text-xs font-semibold text-gray-700 cursor-pointer">
              Debug: WebSocket Messages ({debugMessages.length})
            </summary>
            <div className="mt-2 space-y-2 max-h-60 overflow-y-auto">
              {debugMessages.map((msg, idx) => (
                <div key={idx} className="text-xs font-mono bg-white p-2 rounded border border-gray-200">
                  <div className="text-gray-500">{msg.time}</div>
                  <pre className="text-gray-800 whitespace-pre-wrap break-all">
                    {JSON.stringify(msg.data, null, 2)}
                  </pre>
                </div>
              ))}
            </div>
          </details>
        )}
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
