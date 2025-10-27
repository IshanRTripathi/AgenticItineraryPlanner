/**
 * AI Agent Progress Component
 * Premium animated progress display with real-time updates
 * Design: Apple.com refinement + Emirates.com luxury
 */

import { useState, useEffect, useRef } from 'react';
import { Card } from '@/components/ui/card';
import { Sparkles, Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useStompWebSocket } from '@/hooks/useStompWebSocket';
import { useProgressWithStages } from '@/hooks/useSmoothProgress';
import { ProgressBar } from './ProgressBar';
import { api } from '@/services/api';
import type { AgentProgressEvent } from '@/types/dto';

const PROGRESS_STEPS = [
  { id: 1, label: 'Analyzing your preferences', duration: 2000 },
  { id: 2, label: 'Finding best destinations', duration: 2500 },
  { id: 3, label: 'Planning activities', duration: 3000 },
  { id: 4, label: 'Optimizing itinerary', duration: 2000 },
  { id: 5, label: 'Finalizing details', duration: 1500 },
];

const MOTIVATIONAL_MESSAGES = [
  'Creating your perfect adventure...',
  'Discovering hidden gems...',
  'Crafting unforgettable experiences...',
  'Planning the trip of a lifetime...',
  'Almost there...',
];

export function AgentProgress() {
  const [actualProgress, setActualProgress] = useState<number | undefined>(undefined);
  const [currentStep, setCurrentStep] = useState(0);
  const [isCompleted, setIsCompleted] = useState(false);
  const onCompleteCalledRef = useRef(false);
  
  // Use smooth progress with stages (swooshes to 70% quickly)
  const { progress, stage } = useProgressWithStages(!isCompleted, actualProgress);

  // Get executionId from URL params
  const urlParams = new URLSearchParams(window.location.search);
  const executionId = urlParams.get('executionId');
  const itinId = urlParams.get('itineraryId');

  // Connect to STOMP WebSocket for real-time updates
  const { isConnected } = useStompWebSocket(executionId, {
    onMessage: (event: AgentProgressEvent) => {
      console.log('[AgentProgress] Received event:', event);
      
      if (event.progress !== undefined) {
        setActualProgress(event.progress);
      }
      if (event.currentStep) {
        const stepIndex = PROGRESS_STEPS.findIndex(s => 
          s.label.toLowerCase().includes(event.currentStep!.toLowerCase())
        );
        if (stepIndex >= 0) {
          setCurrentStep(stepIndex);
        }
      }
      if (event.status === 'completed' && event.itineraryId) {
        setActualProgress(100);
        setIsCompleted(true);
        // Navigate to trip detail after a short delay
        if (!onCompleteCalledRef.current) {
          onCompleteCalledRef.current = true;
          setTimeout(() => {
            window.location.href = `/trip/${event.itineraryId}`;
          }, 2000);
        }
      }
    },
  });

  // Poll backend for itinerary status
  useEffect(() => {
    if (!itinId || isCompleted) return;

    const pollInterval = setInterval(async () => {
      try {
        const itinerary = await api.get<any>(`/itineraries/${itinId}/json`);
        
        // Check if itinerary is ready (has days with activities)
        if (itinerary && itinerary.days && itinerary.days.length > 0) {
          const hasActivities = itinerary.days.some((day: any) => 
            day.nodes && day.nodes.length > 0
          );
          
          if (hasActivities) {
            console.log('[AgentProgress] Itinerary is ready!');
            setActualProgress(100);
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
    }, 5000); // Poll every 5 seconds

    return () => clearInterval(pollInterval);
  }, [itinId, isCompleted]);

  // Auto-complete when smooth progress reaches 100%
  useEffect(() => {
    if (progress >= 100 && !isCompleted && !onCompleteCalledRef.current) {
      console.log('[AgentProgress] Progress reached 100%, checking itinerary...');
      setIsCompleted(true);
      
      if (itinId && !onCompleteCalledRef.current) {
        onCompleteCalledRef.current = true;
        setTimeout(() => {
          window.location.href = `/trip/${itinId}`;
        }, 1000);
      }
    }
  }, [progress, isCompleted, itinId]);

  return (
    <Card className="max-w-2xl w-full p-8 shadow-elevation-3 relative overflow-hidden">
      {/* Animated background particles */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {[...Array(20)].map((_, i) => (
          <div
            key={i}
            className="absolute w-1 h-1 bg-primary/20 rounded-full animate-float"
            style={{
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
              animationDelay: `${Math.random() * 3}s`,
              animationDuration: `${3 + Math.random() * 2}s`,
            }}
          />
        ))}
      </div>

      <div className="relative z-10">
        {/* Icon */}
        <div className="flex justify-center mb-6">
          <div className="relative">
            <div className="w-20 h-20 rounded-full bg-gradient-to-br from-primary to-primary-600 flex items-center justify-center animate-pulse-slow">
              <Sparkles className="w-10 h-10 text-white animate-spin-slow" />
            </div>
            <div className="absolute inset-0 rounded-full bg-primary/20 animate-ping" />
          </div>
        </div>

        {/* Heading */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-foreground mb-2">
            Creating Your Perfect Itinerary
          </h1>
          <div className="flex items-center justify-center gap-2 text-muted-foreground animate-fade-in">
            <span className="text-2xl">{stage.icon}</span>
            <p className={stage.color}>{stage.message}</p>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="mb-6">
          <ProgressBar progress={progress} />
          <div className="text-center mt-3 text-sm text-muted-foreground animate-fade-in">
            {Math.round(progress)}% complete
          </div>
        </div>

        {/* Steps */}
        <div className="space-y-3">
          {PROGRESS_STEPS.map((step, index) => {
            const isCompleted = index < currentStep;
            const isCurrent = index === currentStep;
            const isUpcoming = index > currentStep;

            return (
              <div
                key={step.id}
                className={cn(
                  'flex items-center gap-3 p-3 rounded-lg transition-all duration-300',
                  isCurrent && 'bg-primary/5 scale-105',
                  isCompleted && 'opacity-60'
                )}
              >
                <div
                  className={cn(
                    'w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 transition-all duration-300',
                    isCompleted && 'bg-success',
                    isCurrent && 'bg-primary animate-pulse',
                    isUpcoming && 'bg-muted'
                  )}
                >
                  {isCompleted ? (
                    <Check className="w-4 h-4 text-white" />
                  ) : (
                    <span className="text-xs font-semibold text-white">
                      {step.id}
                    </span>
                  )}
                </div>
                <span
                  className={cn(
                    'text-sm font-medium transition-colors',
                    isCurrent && 'text-primary font-semibold',
                    isCompleted && 'text-muted-foreground',
                    isUpcoming && 'text-muted-foreground'
                  )}
                >
                  {step.label}
                </span>
                {isCurrent && (
                  <div className="ml-auto">
                    <div className="flex gap-1">
                      <div className="w-1.5 h-1.5 bg-primary rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                      <div className="w-1.5 h-1.5 bg-primary rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                      <div className="w-1.5 h-1.5 bg-primary rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Footer message */}
        <div className="mt-8 text-center">
          <p className="text-xs text-muted-foreground">
            This usually takes 30-60 seconds. Please don't close this window.
          </p>
        </div>
      </div>
    </Card>
  );
}
