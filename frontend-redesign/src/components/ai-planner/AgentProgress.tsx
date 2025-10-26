/**
 * AI Agent Progress Component
 * Premium animated progress display with real-time updates
 */

import { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Sparkles, Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useWebSocket } from '@/hooks/useWebSocket';
import { endpoints } from '@/services/api';
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
  const [progress, setProgress] = useState(0);
  const [currentStep, setCurrentStep] = useState(0);
  const [messageIndex, setMessageIndex] = useState(0);

  // Get executionId from URL params
  const urlParams = new URLSearchParams(window.location.search);
  const executionId = urlParams.get('executionId');
  const itinId = urlParams.get('itineraryId');

  // Connect to WebSocket for real-time updates
  const wsUrl = executionId ? endpoints.websocketUrl : null;
  const { isConnected } = useWebSocket(wsUrl, {
    onMessage: (event: AgentProgressEvent) => {
      if (event.progress !== undefined) {
        setProgress(event.progress);
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
        // Navigate to trip detail after a short delay
        setTimeout(() => {
          window.location.href = `/trip/${event.itineraryId}`;
        }, 2000);
      }
    },
  });

  // Fallback: Simulate progress if no WebSocket connection
  useEffect(() => {
    if (!executionId || isConnected) return;

    const totalDuration = PROGRESS_STEPS.reduce((sum, step) => sum + step.duration, 0);
    let elapsed = 0;

    const interval = setInterval(() => {
      elapsed += 100;
      const newProgress = Math.min((elapsed / totalDuration) * 100, 100);
      setProgress(newProgress);

      let cumulativeDuration = 0;
      for (let i = 0; i < PROGRESS_STEPS.length; i++) {
        cumulativeDuration += PROGRESS_STEPS[i].duration;
        if (elapsed < cumulativeDuration) {
          setCurrentStep(i);
          break;
        }
      }

      if (elapsed >= totalDuration) {
        clearInterval(interval);
        setCurrentStep(PROGRESS_STEPS.length);
        if (itinId) {
          setTimeout(() => {
            window.location.href = `/trip/${itinId}`;
          }, 1000);
        }
      }
    }, 100);

    return () => clearInterval(interval);
  }, [executionId, isConnected, itinId]);

  useEffect(() => {
    // Rotate motivational messages
    const messageInterval = setInterval(() => {
      setMessageIndex((prev) => (prev + 1) % MOTIVATIONAL_MESSAGES.length);
    }, 3000);

    return () => clearInterval(messageInterval);
  }, []);

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
          <p className="text-muted-foreground animate-fade-in">
            {MOTIVATIONAL_MESSAGES[messageIndex]}
          </p>
        </div>

        {/* Progress Bar */}
        <div className="mb-8">
          <div className="h-3 bg-muted rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-primary via-primary-600 to-secondary transition-all duration-300 ease-out relative"
              style={{ width: `${progress}%` }}
            >
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-shimmer" />
            </div>
          </div>
          <div className="text-center mt-2 text-sm font-semibold text-primary">
            {Math.round(progress)}%
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
