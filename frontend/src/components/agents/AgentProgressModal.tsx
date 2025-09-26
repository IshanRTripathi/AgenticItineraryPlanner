import React, { useState, useEffect, useRef } from 'react';
import { Card } from '../ui/card';
import { Progress } from '../ui/progress';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { CheckCircle2, Loader2, Clock, Zap, MapPin, Route, Users, AlertCircle, RefreshCw, X } from 'lucide-react';
import { AGENT_TASKS, AgentTask } from '../../types/TripData';
import { TripData } from '../../types/TripData';
import { apiClient, AgentEvent } from '../../services/apiClient';
import { useAppStore } from '../../state/hooks';
import { useItinerary } from '../../state/query/hooks';

interface AgentProgressModalProps {
  tripData: TripData;
  onComplete: () => void;
  onCancel?: () => void;
}

interface AgentProgress {
  task: AgentTask;
  status: 'pending' | 'running' | 'completed' | 'failed';
  progress: number;
  startTime?: number;
  endTime?: number;
  error?: string;
}

export function AgentProgressModal({ tripData, onComplete, onCancel }: AgentProgressModalProps) {
  const { setCurrentTrip } = useAppStore();
  const { refetch: refetchItinerary } = useItinerary(tripData.id);
  const [agents, setAgents] = useState<AgentProgress[]>(
    AGENT_TASKS.map(task => ({
      task,
      status: 'pending',
      progress: 0
    }))
  );
  
  const [currentAgentIndex, setCurrentAgentIndex] = useState(0);
  const [overallProgress, setOverallProgress] = useState(0);
  const [startTime] = useState(Date.now());
  const [hasCompleted, setHasCompleted] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [isRetrying, setIsRetrying] = useState(false);
  const [sseConnectionStatus, setSseConnectionStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'error'>('connecting');
  const completionTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const maxTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  const handleSSEEvent = (event: MessageEvent) => {
    try {
      console.log('Processing SSE event:', {
        type: event.type,
        data: event.data,
        dataType: typeof event.data
      });
      
      // Handle connection messages
      if (event.data && typeof event.data === 'string' && event.data.includes('Connected to agent stream')) {
        console.log('SSE connection confirmed:', event.data);
        setSseConnectionStatus('connected');
        return;
      }
      
      // Parse agent events
      const agentEvent: AgentEvent = JSON.parse(event.data);
      console.log('Parsed agent event:', agentEvent);
      console.log('Available agent tasks:', AGENT_TASKS.map(t => t.id));
      
      // Update agent status based on event
      setAgents(prev => prev.map(agent => {
        // Direct match by agent kind
        if (agent.task.id === agentEvent.kind) {
          const newStatus = agentEvent.status === 'succeeded' ? 'completed' : 
                           agentEvent.status === 'running' ? 'running' : 
                           agentEvent.status === 'failed' ? 'failed' : 'pending';
          
          console.log(`Updating agent ${agent.task.name} from ${agent.status} to ${newStatus}`);
          
          return {
            ...agent,
            status: newStatus,
            progress: agentEvent.progress || agent.progress,
            startTime: agentEvent.status === 'running' && !agent.startTime ? Date.now() : agent.startTime,
            endTime: agentEvent.status === 'succeeded' || agentEvent.status === 'failed' ? Date.now() : agent.endTime,
            error: agentEvent.status === 'failed' ? agentEvent.message || 'Agent failed' : undefined
          };
        }
        
        return agent;
      }));
      
      // Update overall progress
      setAgents(prev => {
        const completedCount = prev.filter(a => a.status === 'completed').length;
        const failedCount = prev.filter(a => a.status === 'failed').length;
        const runningAgent = prev.find(a => a.status === 'running');
        const runningProgress = runningAgent ? (runningAgent.progress / 100) : 0;
        
        const totalProgress = ((completedCount + runningProgress) / AGENT_TASKS.length) * 100;
        setOverallProgress(Math.round(totalProgress));
        
        console.log(`Progress update: ${completedCount}/${AGENT_TASKS.length} completed, ${failedCount} failed, ${Math.round(totalProgress)}% total`);
        
        // Check if any agent failed
        if (failedCount > 0 && !hasError) {
          setHasError(true);
          setErrorMessage('One or more agents failed to complete. You can retry or cancel the operation.');
        }
        
        return prev;
      });
      
      // Check if planner agent is completed
      if (agentEvent.kind === 'planner' && agentEvent.status === 'succeeded') {
        console.log('Planner agent completed, refetching itinerary...');
        if (!hasCompleted && !hasError) {
          setHasCompleted(true);
          setTimeout(async () => {
            console.log('Refetching itinerary before completion');
            try {
              const result = await refetchItinerary();
              if (result.data) {
                const responseData = result.data as TripData;
                setCurrentTrip(responseData);
              }
            } catch (e) {
              console.warn('Itinerary refetch failed, proceeding anyway', e);
            }
            console.log('Calling onComplete()');
            onComplete();
          }, 1000);
        }
      }
      
      // Check if planner agent failed
      if (agentEvent.kind === 'planner' && agentEvent.status === 'failed') {
        console.log('Planner agent failed');
        setHasError(true);
        setErrorMessage(`Trip generation failed: ${agentEvent.message || 'Unknown error occurred'}`);
      }
      
    } catch (error) {
      console.error('Error processing SSE event:', error);
      setSseConnectionStatus('error');
    }
  };

  // Retry function
  const handleRetry = () => {
    setIsRetrying(true);
    setHasError(false);
    setErrorMessage('');
    setOverallProgress(0);
    setAgents(AGENT_TASKS.map(task => ({
      task,
      status: 'pending',
      progress: 0
    })));
    
    // Restart the process
    setTimeout(() => {
      setIsRetrying(false);
      // The useEffect will handle restarting the connection
    }, 1000);
  };

  // Cancel function
  const handleCancel = () => {
    if (onCancel) {
      onCancel();
    } else {
      // Default behavior - go back to dashboard
      window.history.back();
    }
  };

  useEffect(() => {
    if (!tripData.id) {
      console.error('No trip data ID available for SSE connection');
      setHasError(true);
      setErrorMessage('No trip data available. Please try again.');
      return;
    }

    console.log('Starting SSE connection for itinerary:', tripData.id);

    // Set maximum timeout (5 minutes)
    const maxTimeout = setTimeout(() => {
      if (!hasCompleted && !hasError) {
        console.log('Maximum timeout reached');
        setHasError(true);
        setErrorMessage('Trip generation is taking longer than expected. You can retry or cancel the operation.');
      }
    }, 5 * 60 * 1000); // 5 minutes
    maxTimeoutRef.current = maxTimeout;

    // Simple completion checker - check every 2 seconds if itinerary is completed
    const checkCompletion = async () => {
      if (!hasCompleted && !hasError) {
        try {
          const result = await refetchItinerary();
          if (result.data) {
            const responseData = result.data as TripData;
            console.log('Checking completion status:', responseData.status);
            // Only consider completed if status is explicitly 'completed' AND has actual content
            if (responseData.status === 'completed' && responseData.itinerary?.days && responseData.itinerary.days.length > 0) {
              console.log('Itinerary is completed, proceeding to next screen');
              setHasCompleted(true);
              setCurrentTrip(responseData);
              onComplete();
              return;
            }
          }
        } catch (e) {
          console.warn('Completion check failed:', e);
          // Don't set error on completion check failure, just continue
        }
        
        // Schedule next check
        const timeout = setTimeout(checkCompletion, 2000);
        completionTimeoutRef.current = timeout;
      }
    };

    // Connect to SSE stream for real-time agent updates FIRST
    const eventSource = apiClient.createAgentEventStream(tripData.id);
    eventSourceRef.current = eventSource;
    
    // Start checking after 2 seconds (reduced from 5 seconds)
    const initialTimeout = setTimeout(checkCompletion, 2000);
    completionTimeoutRef.current = initialTimeout;
    
    eventSource.onopen = () => {
      console.log('SSE connection opened for itinerary:', tripData.id);
      setSseConnectionStatus('connected');
    };
    
    // Handle all SSE events
    eventSource.onmessage = (event) => {
      console.log('SSE message received:', event);
      handleSSEEvent(event);
    };
    
    // Handle named events (like 'agent-event')
    eventSource.addEventListener('agent-event', (event) => {
      console.log('SSE agent-event received:', event);
      handleSSEEvent(event);
    });
    
    eventSource.addEventListener('connected', (event) => {
      console.log('SSE connected event:', event.data);
      setSseConnectionStatus('connected');
    });
    
    eventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
      setSseConnectionStatus('error');
      // Don't immediately set error state, let the completion checker handle it
    };
    
    // Cleanup function to close SSE connection
    return () => {
      console.log('Closing SSE connection for itinerary:', tripData.id);
      if (completionTimeoutRef.current) {
        clearTimeout(completionTimeoutRef.current);
      }
      if (maxTimeoutRef.current) {
        clearTimeout(maxTimeoutRef.current);
      }
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, [tripData.id, onComplete, refetchItinerary, setCurrentTrip, hasCompleted, hasError, isRetrying]);

  const formatTime = (ms: number) => {
    return `${(ms / 1000).toFixed(1)}s`;
  };

  const getAgentIcon = (agent: AgentProgress) => {
    switch (agent.status) {
      case 'completed':
        return <CheckCircle2 className="w-5 h-5 text-green-500" />;
      case 'running':
        return <Loader2 className="w-5 h-5 text-blue-500 animate-spin" />;
      case 'failed':
        return <AlertCircle className="w-5 h-5 text-red-500" />;
      default:
        return <Clock className="w-5 h-5 text-gray-400" />;
    }
  };

  const getStatusBadge = (agent: AgentProgress) => {
    switch (agent.status) {
      case 'completed':
        return <Badge variant="default" className="bg-green-100 text-green-800">Completed</Badge>;
      case 'running':
        return <Badge variant="default" className="bg-blue-100 text-blue-800">Running</Badge>;
      case 'failed':
        return <Badge variant="destructive" className="bg-red-100 text-red-800">Failed</Badge>;
      default:
        return <Badge variant="secondary">Pending</Badge>;
    }
  };

  const getElapsedTime = () => {
    return Date.now() - startTime;
  };

  // Show error state
  if (hasError) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <Card className="w-full max-w-2xl mx-4 p-6">
          <div className="space-y-6">
            {/* Error Header */}
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-2">
                <AlertCircle className="w-6 h-6 text-red-500" />
                <h2 className="text-2xl font-bold text-red-600">Trip Generation Failed</h2>
              </div>
              <p className="text-gray-600">
                {errorMessage || 'Something went wrong while creating your itinerary'}
              </p>
            </div>

            {/* Error Details */}
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <h3 className="font-medium text-red-800 mb-2">What happened?</h3>
              <p className="text-sm text-red-700">
                The AI agents encountered an issue while generating your trip. This could be due to:
              </p>
              <ul className="text-sm text-red-700 mt-2 list-disc list-inside">
                <li>Network connectivity issues</li>
                <li>Server temporarily unavailable</li>
                <li>Invalid trip parameters</li>
                <li>API service limitations</li>
              </ul>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-3 justify-center">
              <Button
                onClick={handleRetry}
                disabled={isRetrying}
                className="flex items-center gap-2"
              >
                <RefreshCw className={`w-4 h-4 ${isRetrying ? 'animate-spin' : ''}`} />
                {isRetrying ? 'Retrying...' : 'Try Again'}
              </Button>
              <Button
                onClick={handleCancel}
                variant="outline"
                className="flex items-center gap-2"
              >
                <X className="w-4 h-4" />
                Cancel
              </Button>
            </div>

            {/* Connection Status */}
            <div className="text-center text-sm text-gray-500">
              <p>Connection Status: 
                <span className={`ml-1 ${
                  sseConnectionStatus === 'connected' ? 'text-green-600' :
                  sseConnectionStatus === 'error' ? 'text-red-600' :
                  'text-yellow-600'
                }`}>
                  {sseConnectionStatus}
                </span>
              </p>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <Card className="w-full max-w-2xl mx-4 p-6">
        <div className="space-y-6">
          {/* Header */}
          <div className="text-center">
            <div className="flex items-center justify-center gap-2 mb-2">
              <Zap className="w-6 h-6 text-blue-500" />
              <h2 className="text-2xl font-bold">Creating Your Itinerary</h2>
            </div>
            <p className="text-gray-600">
              Our AI agents are working hard to create your perfect trip to {tripData.destination}
            </p>
          </div>

          {/* Overall Progress */}
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>Overall Progress</span>
              <span>{overallProgress}%</span>
            </div>
            <Progress value={overallProgress} className="h-2" />
          </div>

          {/* Trip Summary */}
          <div className="grid grid-cols-2 gap-4 pt-4 border-t text-left">
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-gray-400" />
              <span className="text-sm">{tripData.startLocation?.name} → {tripData.endLocation?.name || tripData.destination}</span>
            </div>
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-gray-400" />
              <span className="text-sm">
                {Math.ceil((new Date(tripData.dates.end).getTime() - new Date(tripData.dates.start).getTime()) / (1000 * 60 * 60 * 24))} days
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Users className="h-4 w-4 text-gray-400" />
              <span className="text-sm">{tripData.travelers?.length || 1} traveler{(tripData.travelers?.length || 1) > 1 ? 's' : ''}</span>
            </div>
            <div className="flex items-center gap-2">
              <Route className="h-4 w-4 text-gray-400" />
              <span className="text-sm">{tripData.budget?.currency || 'USD'} {Number(tripData.budget?.total || 0).toLocaleString()}</span>
            </div>
          </div>

          {/* Manual Continue Button - Fallback */}
          {overallProgress >= 90 && !hasCompleted && (
            <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-sm text-yellow-800 mb-2">
                It looks like your itinerary is ready! If the page doesn't automatically continue, click below:
              </p>
              <button
                onClick={() => {
                  setHasCompleted(true);
                  onComplete();
                }}
                className="px-4 py-2 bg-yellow-600 text-white rounded hover:bg-yellow-700 transition-colors"
              >
                Continue to Itinerary
              </button>
            </div>
          )}

          {/* Cancel Button */}
          {!hasCompleted && !hasError && (
            <div className="flex justify-center">
              <Button
                onClick={handleCancel}
                variant="outline"
                className="flex items-center gap-2"
              >
                <X className="w-4 h-4" />
                Cancel Generation
              </Button>
            </div>
          )}

          {/* Agent Status List */}
          <div className="space-y-3">
            {agents.map((agent, index) => (
              <div
                key={agent.task.id}
                className={`flex items-center gap-4 p-3 rounded-lg border transition-all ${
                  agent.status === 'running' 
                    ? 'border-blue-200 bg-blue-50' 
                    : agent.status === 'completed'
                    ? 'border-green-200 bg-green-50'
                    : agent.status === 'failed'
                    ? 'border-red-200 bg-red-50'
                    : 'border-gray-200 bg-gray-50'
                }`}
              >
                <div className="flex-shrink-0">
                  {getAgentIcon(agent)}
                </div>
                
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <h3 className="font-medium text-gray-900">{agent.task.name}</h3>
                    {getStatusBadge(agent)}
                  </div>
                  <p className="text-sm text-gray-600 mt-1">{agent.task.description}</p>
                  
                  {agent.status === 'running' && (
                    <div className="mt-2">
                      <div className="flex justify-between text-xs text-gray-500 mb-1">
                        <span>Progress</span>
                        <span>{agent.progress}%</span>
                      </div>
                      <Progress value={agent.progress} className="h-1" />
                    </div>
                  )}
                  
                  {agent.status === 'completed' && agent.endTime && agent.startTime && (
                    <p className="text-xs text-gray-500 mt-1">
                      Completed in {formatTime(agent.endTime - agent.startTime)}
                    </p>
                  )}

                  {agent.status === 'failed' && agent.error && (
                    <div className="mt-2 p-2 bg-red-100 border border-red-200 rounded text-xs text-red-700">
                      <strong>Error:</strong> {agent.error}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Footer */}
          <div className="text-center text-sm text-gray-500">
            <p>Elapsed time: {formatTime(getElapsedTime())}</p>
            <p className="mt-1">This usually takes 30-60 seconds</p>
            <p className="mt-1">Connection: 
              <span className={`ml-1 ${
                sseConnectionStatus === 'connected' ? 'text-green-600' :
                sseConnectionStatus === 'error' ? 'text-red-600' :
                'text-yellow-600'
              }`}>
                {sseConnectionStatus}
              </span>
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}



