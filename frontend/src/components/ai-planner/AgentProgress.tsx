/**
 * AI Agent Progress Component
 * Premium animated progress display with real-time updates
 * Design: Linear.app + Stripe.com + Apple.com refinement
 */

import { useState, useEffect, useRef } from 'react';
import { Card } from '@/components/ui/card';
import { Sparkles, Check, Zap, MapPin, Utensils, Image, DollarSign } from 'lucide-react';
import { useStompWebSocket } from '@/hooks/useStompWebSocket';
import { api } from '@/services/api';
import type { AgentProgressEvent } from '@/types/dto';

// Expected timeline based on actual backend logs (122 seconds total)
const EXPECTED_TIMELINE = {
  skeleton: 49,      // 0-49s: Day generation
  population: 24,    // 49-73s: Agent population
  enrichment: 43,    // 73-116s: Enrichment
  cost: 1,          // 116-117s: Cost estimation
  finalization: 5,   // 117-122s: Finalization
  total: 122
};

const PHASE_ICONS: Record<string, any> = {
  skeleton: MapPin,
  population: Utensils,
  enrichment: Image,
  cost_estimation: DollarSign,
  finalization: Check,
};

export function AgentProgress() {
  const [smoothProgress, setSmoothProgress] = useState(0);
  const [isCompleted, setIsCompleted] = useState(false);
  const [currentMessage, setCurrentMessage] = useState('Initializing your journey...');
  const [completedDays, setCompletedDays] = useState<number[]>([]);
  const [currentPhase, setCurrentPhase] = useState('skeleton');
  const [itineraryData, setItineraryData] = useState<any | null>(null);
  const [dayActivities, setDayActivities] = useState<Map<number, number>>(new Map());
  const [startTime] = useState(Date.now());
  const [loadError, setLoadError] = useState<string | null>(null);
  const [agentCompletions, setAgentCompletions] = useState<string[]>([]);
  const onCompleteCalledRef = useRef(false);
  const progressIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Get itineraryId from URL params
  // CRITICAL: Backend publishes to /topic/itinerary/{itineraryId}
  // executionId is NOT used for WebSocket - it's just a temporary tracking ID
  const urlParams = new URLSearchParams(window.location.search);
  const itinId = urlParams.get('itineraryId');

  // Always use itineraryId for WebSocket subscription
  const wsSubscriptionId = itinId;

  // Smooth progress animation that completes in 122 seconds
  useEffect(() => {
    if (isCompleted) {
      setSmoothProgress(100);
      return;
    }

    const targetTime = EXPECTED_TIMELINE.total * 1000; // 122 seconds
    const updateInterval = 100; // Update every 100ms for smooth animation

    progressIntervalRef.current = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const calculatedProgress = Math.min((elapsed / targetTime) * 100, 99);
      setSmoothProgress(calculatedProgress);
    }, updateInterval);

    return () => {
      if (progressIntervalRef.current) {
        clearInterval(progressIntervalRef.current);
      }
    };
  }, [startTime, isCompleted]);

  // Load itinerary data with retry logic
  const loadItineraryData = async (id: string, retryCount = 0) => {
    try {
      console.log('[AgentProgress] Loading itinerary data:', id);
      const data = await api.get<any>(`/itineraries/${id}/json`);
      setItineraryData(data);
      setLoadError(null); // Clear any previous errors

      // Count activities per day
      const activityMap = new Map<number, number>();
      data.days?.forEach((day: any) => {
        activityMap.set(day.dayNumber, day.nodes?.length || 0);
      });
      setDayActivities(activityMap);

      console.log('[AgentProgress] Itinerary data loaded:', {
        days: data.days?.length,
        activities: activityMap
      });
    } catch (error) {
      console.error('[AgentProgress] Failed to load itinerary:', error);

      // Retry up to 3 times with exponential backoff
      if (retryCount < 3) {
        const delay = Math.pow(2, retryCount) * 1000; // 1s, 2s, 4s
        console.log(`[AgentProgress] Retrying in ${delay}ms...`);
        setTimeout(() => loadItineraryData(id, retryCount + 1), delay);
      } else {
        setLoadError('Failed to load itinerary data. Please refresh the page.');
      }
    }
  };

  // Connect to STOMP WebSocket for real-time updates
  // CRITICAL: Use itineraryId, not executionId - backend publishes to /topic/itinerary/{itineraryId}
  const { isConnected } = useStompWebSocket(wsSubscriptionId, {
    onMessage: (event: AgentProgressEvent) => {
      console.log('[AgentProgress] Received event:', event);

      // Backend sends 'updateType', not 'type' - prioritize updateType
      const eventType = (event as any).updateType || (event as any).type;
      const eventData = (event as any).data || event;

      // Handle generation_complete event
      if (eventType === 'generation_complete' || event.status === 'completed') {
        console.log('[AgentProgress] Generation complete!');
        setSmoothProgress(100);
        setIsCompleted(true);
        setCurrentMessage('🎉 Your perfect itinerary is ready!');

        if (!onCompleteCalledRef.current) {
          onCompleteCalledRef.current = true;
          const targetId = event.itineraryId || itinId;
          setTimeout(() => {
            window.location.href = `/trip/${targetId}`;
          }, 2500);
        }
        return;
      }

      // Handle day_completed event
      if (eventType === 'day_completed') {
        const dayNum = eventData.dayNumber || (event as any).dayNumber;
        if (dayNum) {
          setCompletedDays(prev => [...new Set([...prev, dayNum])]);
          const msg = `🎉 Day ${dayNum} completed with ${eventData.day?.nodes?.length || 0} activities`;
          setCurrentMessage(msg);

          // Set progress milestone based on day completion
          const dayProgress = (dayNum / 4) * 40; // Days take 40% of total time
          setSmoothProgress(Math.max(smoothProgress, dayProgress));

          // CRITICAL: Load itinerary to get actual day data
          if (itinId) {
            loadItineraryData(itinId);
          }

          // Redirect to trip page after first day completes
          if (dayNum === 1 && !onCompleteCalledRef.current) {
            onCompleteCalledRef.current = true;
            console.log('[AgentProgress] First day complete, redirecting to trip page');
            setTimeout(() => {
              window.location.href = `/trip/${itinId}`;
            }, 2000); // 2 second delay to show the completion message
          }
        }
      }

      // Handle phase_transition event
      if (eventType === 'phase_transition') {
        const toPhase = eventData.toPhase || (event as any).toPhase;
        if (toPhase) {
          const phaseLower = toPhase.toLowerCase();
          setCurrentPhase(phaseLower);

          // Set progress milestone and message based on phase
          let msg = '';
          let progressMilestone = smoothProgress;

          if (phaseLower === 'population') {
            msg = '✨ Enriching with restaurants, attractions & transport';
            progressMilestone = 45;
          } else if (phaseLower === 'enrichment') {
            msg = '📸 Adding photos, reviews & location data';
            progressMilestone = 65;
          } else if (phaseLower === 'cost_estimation') {
            msg = '💰 Calculating budget estimates';
            progressMilestone = 90;
          } else if (phaseLower === 'finalization') {
            msg = '🎯 Finalizing your perfect itinerary';
            progressMilestone = 95;
          } else {
            msg = eventData.message || `Moving to ${toPhase} phase`;
          }

          setCurrentMessage(msg);
          setSmoothProgress(Math.max(smoothProgress, progressMilestone));

          // Load itinerary when phases change
          if (itinId) {
            loadItineraryData(itinId);
          }
        }
      }

      // Handle agent_progress event (from publishAgentProgress)
      if (eventType === 'agent_progress' || (event as any).agentId) {
        if (event.progress !== undefined) {
          setSmoothProgress(Math.max(smoothProgress, event.progress));
        }
        if (event.message || event.status) {
          setCurrentMessage(event.message || event.status || '');
        }
      }

      // Handle agent_complete event
      if (eventType === 'agent_complete') {
        const agentName = eventData.agentName || (event as any).agentName;
        const itemsProcessed = eventData.itemsProcessed || (event as any).itemsProcessed;
        if (agentName) {
          setAgentCompletions(prev => [...prev, agentName]);
          const msg = `✅ ${agentName} completed (${itemsProcessed} items)`;
          setCurrentMessage(msg);

          // Load itinerary to get updated data
          if (itinId) {
            loadItineraryData(itinId);
          }
        }
      }

      // Handle warning event
      if (eventType === 'warning') {
        console.warn('[AgentProgress] Warning:', eventData);
        const msg = eventData.message || 'Processing...';
        setCurrentMessage(`⚠️ ${msg}`);
      }

      // Handle error event
      if (eventType === 'error') {
        console.error('[AgentProgress] Error:', eventData);
        const msg = eventData.message || 'An error occurred';
        setCurrentMessage(`❌ ${msg}`);
        // Don't stop progress, let it continue
      }

      // Handle generic progress updates
      if (event.progress !== undefined && !eventType) {
        setSmoothProgress(Math.max(smoothProgress, event.progress));
      }

      // Handle generic message updates
      if (event.message && !eventType) {
        setCurrentMessage(event.message);
      }
    },
  });

  // Poll backend for itinerary status and load data (DISABLED - using WebSocket only)
  useEffect(() => {
    if (!itinId || isCompleted) return;

    // DISABLED: Polling conflicts with WebSocket, causing connection storms
    // WebSocket provides real-time updates, no need for polling
    return;

    const pollInterval = setInterval(async () => {
      try {
        const itinerary = await api.get<any>(`/itineraries/${itinId}/json`);

        // Update itinerary data
        const prevData = itineraryData;
        setItineraryData(itinerary);

        // Update activity counts
        const activityMap = new Map<number, number>();
        itinerary.days?.forEach((day: any) => {
          activityMap.set(day.dayNumber, day.nodes?.length || 0);
        });
        setDayActivities(activityMap);

        // Detect if activities were added (agents completed)
        if (prevData && itinerary.days) {
          const prevTotalActivities = prevData.days?.reduce((sum: number, day: any) =>
            sum + (day.nodes?.length || 0), 0) || 0;
          const currentTotalActivities = itinerary.days.reduce((sum: number, day: any) =>
            sum + (day.nodes?.length || 0), 0);

          if (currentTotalActivities > prevTotalActivities) {
            const added = currentTotalActivities - prevTotalActivities;
            setCurrentMessage(`✨ ${added} new activities added!`);
          }
        }

        // Check if itinerary is ready (has days with activities)
        if (itinerary && itinerary.days && itinerary.days.length > 0) {
          const hasActivities = itinerary.days.some((day: any) =>
            day.nodes && day.nodes.length > 0
          );

          if (hasActivities && itinerary.status === 'completed') {
            console.log('[AgentProgress] Itinerary is ready!');
            setSmoothProgress(100);
            setIsCompleted(true);

            if (!onCompleteCalledRef.current) {
              onCompleteCalledRef.current = true;
              setTimeout(() => {
                window.location.href = `/trip/${itinId}`;
              }, 1000);
            }
          }
        }
      } catch (error) {
        console.log('[AgentProgress] Polling error (itinerary not ready yet):', error);
      }
    }, 3000); // Poll every 3 seconds for more responsive updates

    return () => clearInterval(pollInterval);
  }, [itinId, isCompleted, itineraryData]);

  // Auto-complete when smooth progress reaches 100%
  useEffect(() => {
    if (smoothProgress >= 100 && !isCompleted && !onCompleteCalledRef.current) {
      console.log('[AgentProgress] Progress reached 100%, checking itinerary...');
      setIsCompleted(true);

      if (itinId && !onCompleteCalledRef.current) {
        onCompleteCalledRef.current = true;
        setTimeout(() => {
          window.location.href = `/trip/${itinId}`;
        }, 1000);
      }
    }
  }, [smoothProgress, isCompleted, itinId]);

  const PhaseIcon = PHASE_ICONS[currentPhase] || Sparkles;
  const timeElapsed = Math.floor((Date.now() - startTime) / 1000);
  const timeRemaining = Math.max(0, EXPECTED_TIMELINE.total - timeElapsed);

  return (
    <div className="min-h-screen w-full bg-gradient-to-br from-slate-50 via-white to-blue-50 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Animated gradient orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary/10 rounded-full blur-3xl animate-pulse"
          style={{ animationDuration: '4s' }} />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl animate-pulse"
          style={{ animationDuration: '6s', animationDelay: '1s' }} />
        <div className="absolute top-1/2 right-1/3 w-64 h-64 bg-purple-500/10 rounded-full blur-3xl animate-pulse"
          style={{ animationDuration: '5s', animationDelay: '2s' }} />
      </div>

      <Card className="max-w-3xl w-full p-10 shadow-2xl border-0 bg-white/90 backdrop-blur-xl relative overflow-hidden">
        {/* Shimmer effect */}
        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent animate-shimmer"
          style={{ backgroundSize: '200% 100%' }} />

        <div className="relative z-10">
          {/* Icon with phase indicator */}
          <div className="flex justify-center mb-8">
            <div className="relative">
              <div className="w-24 h-24 rounded-full bg-gradient-to-br from-primary via-primary-600 to-blue-600 flex items-center justify-center shadow-2xl">
                <PhaseIcon className="w-12 h-12 text-white" style={{
                  animation: isCompleted ? 'none' : 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite'
                }} />
              </div>
              {!isCompleted && (
                <>
                  <div className="absolute inset-0 rounded-full bg-primary/30 animate-ping" />
                  <div className="absolute -inset-2 rounded-full border-2 border-primary/20 animate-spin-slow" />
                </>
              )}
              {isCompleted && (
                <div className="absolute -inset-4 rounded-full border-4 border-green-500/50 animate-pulse" />
              )}
            </div>
          </div>

          {/* Heading */}
          <div className="text-center mb-10">
            <h1 className="text-4xl font-bold bg-gradient-to-r from-gray-900 via-gray-800 to-gray-900 bg-clip-text text-transparent mb-3">
              {isCompleted ? '🎉 Your Itinerary is Ready!' : 'Crafting Your Perfect Journey'}
            </h1>
            <p className="text-lg text-gray-600 animate-fade-in">
              {currentMessage}
            </p>
            {!isCompleted && timeRemaining > 0 && (
              <p className="text-sm text-gray-400 mt-2">
                About {timeRemaining}s remaining
              </p>
            )}
          </div>

          {/* Premium Progress Bar */}
          <div className="mb-10">
            <div className="relative h-3 bg-gray-100 rounded-full overflow-hidden shadow-inner">
              <div
                className="absolute inset-y-0 left-0 bg-gradient-to-r from-primary via-blue-500 to-purple-500 rounded-full transition-all duration-500 ease-out shadow-lg"
                style={{
                  width: `${smoothProgress}%`,
                  boxShadow: '0 0 20px rgba(59, 130, 246, 0.5)'
                }}
              >
                <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/30 to-white/0 animate-shimmer"
                  style={{ backgroundSize: '200% 100%' }} />
              </div>
            </div>
            <div className="flex items-center justify-between mt-4">
              <span className="text-2xl font-bold bg-gradient-to-r from-primary to-blue-600 bg-clip-text text-transparent">
                {Math.round(smoothProgress)}%
              </span>
              <span className="text-sm text-gray-500 font-medium">
                {timeElapsed}s elapsed
              </span>
            </div>
          </div>

          {/* Completed Days - Premium Cards */}
          {completedDays.length > 0 && (
            <div className="mb-8 animate-fade-in">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center">
                  <Check className="w-4 h-4 text-green-600" />
                </div>
                <span className="text-sm font-semibold text-gray-700">Days Completed</span>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                {completedDays.sort((a, b) => a - b).map((day, index) => {
                  const activityCount = dayActivities.get(day) || 0;
                  return (
                    <div
                      key={day}
                      className="group relative p-4 bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl border border-green-200/50 hover:border-green-300 transition-all duration-300 hover:shadow-lg hover:-translate-y-1"
                      style={{
                        animation: 'fadeInUp 0.5s ease-out',
                        animationDelay: `${index * 100}ms`,
                        animationFillMode: 'both'
                      }}
                    >
                      <div className="text-2xl font-bold text-green-600 mb-1">Day {day}</div>
                      {activityCount > 0 && (
                        <div className="text-xs text-green-700 font-medium">
                          {activityCount} activities
                        </div>
                      )}
                      <div className="absolute top-2 right-2 w-6 h-6 rounded-full bg-green-500 flex items-center justify-center">
                        <Check className="w-3 h-3 text-white" />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Activity Preview - Premium */}
          {itineraryData && itineraryData.days && itineraryData.days.length > 0 && (
            <div className="mb-8 p-6 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-2xl border border-blue-200/50 animate-fade-in">
              <h3 className="text-sm font-bold text-gray-700 mb-4 flex items-center gap-2">
                <MapPin className="w-4 h-4 text-blue-600" />
                Itinerary Preview
              </h3>
              <div className="space-y-3 max-h-64 overflow-y-auto custom-scrollbar">
                {itineraryData.days.slice(0, 4).map((day: any, index: number) => (
                  <div
                    key={day.dayNumber}
                    className="flex items-center gap-3 p-3 bg-white/80 backdrop-blur rounded-xl hover:bg-white transition-all duration-200 hover:shadow-md"
                    style={{
                      animation: 'fadeInLeft 0.5s ease-out',
                      animationDelay: `${index * 100}ms`,
                      animationFillMode: 'both'
                    }}
                  >
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center flex-shrink-0 shadow-lg">
                      <span className="text-white font-bold text-sm">{day.dayNumber}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-gray-800 truncate text-sm">
                        {day.location || 'Planning...'}
                      </p>
                      <p className="text-xs text-gray-500">
                        {day.nodes?.length || 0} activities • {day.date || 'TBD'}
                      </p>
                    </div>
                    <Check className="w-5 h-5 text-green-500 flex-shrink-0" />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Agent Completions */}
          {agentCompletions.length > 0 && (
            <div className="mb-8 animate-fade-in">
              <div className="flex items-center gap-2 mb-3">
                <Zap className="w-4 h-4 text-amber-500" />
                <span className="text-sm font-semibold text-gray-700">Agents Completed</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {agentCompletions.map((agent, index) => (
                  <div
                    key={`${agent}-${index}`}
                    className="px-3 py-1.5 bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200/50 rounded-full text-xs font-medium text-amber-700 flex items-center gap-1.5"
                    style={{
                      animation: 'fadeInUp 0.5s ease-out',
                      animationDelay: `${index * 100}ms`,
                      animationFillMode: 'both'
                    }}
                  >
                    <Check className="w-3 h-3" />
                    {agent}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Error Message */}
          {loadError && (
            <div className="mb-6 p-4 bg-red-50 rounded-xl border border-red-200 animate-fade-in">
              <p className="text-sm text-red-700 font-medium">{loadError}</p>
              <button
                onClick={() => window.location.reload()}
                className="mt-2 text-xs text-red-600 underline hover:no-underline font-medium"
              >
                Refresh Page
              </button>
            </div>
          )}

          {/* Footer message */}
          <div className="mt-10 text-center space-y-3">
            <p className="text-sm text-gray-600 font-medium">
              {isCompleted
                ? '✨ Redirecting to your itinerary...'
                : '⏱️ Usually takes about 2 minutes'
              }
            </p>
            {!isConnected && !isCompleted && (
              <div className="inline-flex items-center gap-2 px-4 py-2 bg-yellow-50 border border-yellow-200 rounded-full">
                <span className="w-2 h-2 bg-yellow-500 rounded-full animate-pulse" />
                <span className="text-xs text-yellow-700 font-medium">
                  Using backup connection
                </span>
              </div>
            )}
            {!isCompleted && (
              <p className="text-xs text-gray-400">
                Please keep this window open
              </p>
            )}
          </div>
        </div>
      </Card>
    </div>
  );
}
