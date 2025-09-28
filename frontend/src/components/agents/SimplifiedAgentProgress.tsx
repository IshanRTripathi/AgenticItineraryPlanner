import React, { useState, useEffect, useRef } from 'react';
import { TripData } from '../../types/TripData';
import { apiClient } from '../../services/apiClient';
import { Progress } from '../ui/progress';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

interface SimplifiedAgentProgressProps {
  tripData: TripData;
  onComplete: () => void;
  onCancel: () => void;
}

interface AgentStatus {
  kind: string;
  status: 'queued' | 'running' | 'completed' | 'failed';
  progress: number;
  message?: string;
}

export function SimplifiedAgentProgress({ tripData, onComplete, onCancel }: SimplifiedAgentProgressProps) {
  const [agents, setAgents] = useState<AgentStatus[]>([]);
  const [overallProgress, setOverallProgress] = useState(0);
  const [isCompleted, setIsCompleted] = useState(false);
  const onCompleteCalledRef = useRef(false);
  const [hasError, setHasError] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [currentMessage, setCurrentMessage] = useState<string>('Processing with AI models...');
  const [remainingTime, setRemainingTime] = useState<number>(120); // 2 minutes in seconds
  
  const eventSourceRef = useRef<EventSource | null>(null);
  const startTimeRef = useRef<number>(Date.now());
  const messageIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const countdownIntervalRef = useRef<NodeJS.Timeout | null>(null);
  
  // Rotating status messages
  const statusMessages = [
    'Processing with AI models...',
    'Personalizing for you...',
    'Fetching latest data...',
    'Optimizing daily itineraries...',
    'Analyzing preferences...',
    'Generating recommendations...'
  ];
  const [messageIndex, setMessageIndex] = useState(0);

  // Calculate overall progress whenever agents change
  useEffect(() => {
    // Don't recalculate progress if already completed
    if (isCompleted) {
      return;
    }
    
    if (agents.length === 0) {
      setOverallProgress(0);
      return;
    }
    
    const totalProgress = agents.reduce((sum, agent) => sum + agent.progress, 0);
    const averageProgress = totalProgress / agents.length;
    
    setOverallProgress(Math.round(averageProgress));
  }, [agents, isCompleted]);

  // Start intervals immediately on mount - only run once
  useEffect(() => {
    console.log('=== STARTING INTERVALS ON MOUNT ===');
    
    // Start rotating messages immediately, every 3 seconds
    console.log('Starting rotating messages interval');
    messageIntervalRef.current = setInterval(() => {
      setMessageIndex(prev => {
        const next = (prev + 1) % statusMessages.length;
        console.log('Rotating message from', prev, 'to', next, 'message:', statusMessages[next]);
        return next;
      });
    }, 3000);

    // Start countdown timer immediately
    console.log('Starting countdown timer interval');
    countdownIntervalRef.current = setInterval(() => {
      setRemainingTime(prev => {
        const next = prev <= 1 ? 0 : prev - 1;
        console.log('Countdown timer:', prev, '->', next);
        return next;
      });
    }, 1000);

    return () => {
      console.log('=== CLEANING UP INTERVALS ===');
      if (messageIntervalRef.current) {
        clearInterval(messageIntervalRef.current);
        messageIntervalRef.current = null;
      }
      if (countdownIntervalRef.current) {
        clearInterval(countdownIntervalRef.current);
        countdownIntervalRef.current = null;
      }
    };
  }, []); // Empty dependency array - only run on mount

  // Stop intervals when completed or has error
  useEffect(() => {
    if (isCompleted || hasError) {
      console.log('Stopping intervals due to completion or error');
      if (messageIntervalRef.current) {
        clearInterval(messageIntervalRef.current);
        messageIntervalRef.current = null;
      }
      if (countdownIntervalRef.current) {
        clearInterval(countdownIntervalRef.current);
        countdownIntervalRef.current = null;
      }
    }
  }, [isCompleted, hasError]);

  // Main SSE connection effect
  useEffect(() => {
    if (!tripData.id) return;

    // Prevent multiple connections for the same trip
    if (eventSourceRef.current) {
      console.log('SSE connection already exists, skipping new connection');
      return;
    }

    console.log('=== SIMPLIFIED AGENT PROGRESS INITIALIZATION ===');
    console.log('Trip ID:', tripData.id);

    // Connect to SSE stream
    const eventSource = apiClient.createAgentEventStream(tripData.id);
    eventSourceRef.current = eventSource;

    eventSource.onopen = () => {
      console.log('SSE connection opened');
      // Don't override the rotating message - let it continue
    };

    // Handle agent list event
    eventSource.addEventListener('agent-list', (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log('Received agent list:', data);
        
        if (data.agents && Array.isArray(data.agents)) {
          const initialAgents: AgentStatus[] = data.agents.map((agentKind: string) => ({
            kind: agentKind,
            status: 'queued',
            progress: 0
          }));
          
          setAgents(initialAgents);
          // Don't override the rotating message - let it continue
        }
      } catch (error) {
        console.error('Error parsing agent list event:', error);
      }
    });

    // Handle agent events
    eventSource.addEventListener('agent-event', (event) => {
      try {
        const agentEvent = JSON.parse(event.data);
        console.log('Received agent event:', agentEvent);

        setAgents(prev => prev.map(agent => {
          if (agent.kind === agentEvent.kind) {
            const updatedAgent = {
              ...agent,
              status: agentEvent.status,
              progress: agentEvent.progress || agent.progress,
              message: agentEvent.message
            };
            
            // Don't update current message for individual agent events - let rotating messages handle it
            // Only update for failures
            if (agentEvent.status === 'failed') {
              setCurrentMessage(`${agentEvent.kind} agent failed: ${agentEvent.message}`);
            }
            
            return updatedAgent;
          }
          return agent;
        }));
      } catch (error) {
        console.error('Error parsing agent event:', error);
      }
    });

    // Handle completion event
    eventSource.addEventListener('completion', (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log('Received completion event:', data);
        
        // Prevent multiple onComplete calls
        if (onCompleteCalledRef.current) {
          console.log('onComplete already called, skipping');
          return;
        }
        onCompleteCalledRef.current = true;
        
        // Stop all intervals
        if (messageIntervalRef.current) {
          clearInterval(messageIntervalRef.current);
          messageIntervalRef.current = null;
        }
        if (countdownIntervalRef.current) {
          clearInterval(countdownIntervalRef.current);
          countdownIntervalRef.current = null;
        }
        
        setIsCompleted(true);
        setOverallProgress(100); // Set to 100% and keep it there
        setCurrentMessage('All agents completed successfully!');
        
        // Clean up and call onComplete with a small delay to ensure backend has saved the data
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
          eventSourceRef.current = null;
        }
        
        // Small delay to ensure backend has fully saved the itinerary
        setTimeout(() => {
          onComplete();
        }, 500);
      } catch (error) {
        console.error('Error parsing completion event:', error);
      }
    });

    eventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
      setHasError(true);
      setErrorMessage('Connection lost. Please try again.');
    };

    // Cleanup function
    return () => {
      console.log('Cleaning up SSE connection');
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      if (messageIntervalRef.current) {
        clearInterval(messageIntervalRef.current);
        messageIntervalRef.current = null;
      }
      if (countdownIntervalRef.current) {
        clearInterval(countdownIntervalRef.current);
        countdownIntervalRef.current = null;
      }
    };
  }, [tripData.id, onComplete]);

  const getStatusIcon = () => {
    if (hasError) return <AlertCircle className="w-6 h-6 text-red-500" />;
    if (isCompleted) return <CheckCircle2 className="w-6 h-6 text-green-500" />;
    return <Loader2 className="w-6 h-6 text-blue-500 animate-spin" />;
  };

  const getStatusText = () => {
    if (hasError) return 'Error';
    if (isCompleted) return 'Completed';
    return 'Processing';
  };

  const getRemainingTime = () => {
    const minutes = Math.floor(remainingTime / 60);
    const seconds = remainingTime % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const getDisplayMessage = () => {
    if (isCompleted) return currentMessage;
    if (hasError) return currentMessage;
    const message = statusMessages[messageIndex];
    console.log('getDisplayMessage - messageIndex:', messageIndex, 'message:', message);
    return message;
  };

  if (hasError) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <Card className="w-full max-w-md mx-4">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-red-600">
              <AlertCircle className="w-5 h-5" />
              Error
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-gray-600">{errorMessage}</p>
            <div className="flex gap-2">
              <Button onClick={onCancel} variant="outline" className="flex-1">
                Cancel
              </Button>
              <Button onClick={() => window.location.reload()} className="flex-1">
                Retry
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <Card className="w-full max-w-md mx-4">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            {getStatusIcon()}
            {getStatusText()}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>Progress</span>
              <span>{overallProgress}%</span>
            </div>
            <Progress value={overallProgress} className="w-full" />
          </div>
          
          <div className="text-sm text-gray-600">
            {getDisplayMessage()}
          </div>
          
          <div className="flex justify-between text-xs text-gray-500">
            <span>Remaining: {getRemainingTime()}</span>
            <span>{agents.filter(a => a.status === 'completed').length}/{agents.length} agents</span>
          </div>
          
          {!isCompleted && (
            <Button onClick={onCancel} variant="outline" className="w-full">
              Cancel
            </Button>
          )}
        </CardContent>
      </Card>
    </div>
  );
}