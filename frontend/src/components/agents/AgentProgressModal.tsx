import React, {useEffect, useRef, useState} from 'react';
import {Card} from '../ui/card';
import {Progress} from '../ui/progress';
import {Badge} from '../ui/badge';
import {Button} from '../ui/button';
import {AlertCircle, CheckCircle2, Clock, Loader2, MapPin, RefreshCw, Route, Users, X, Zap} from 'lucide-react';
import {AGENT_TASKS, AgentTask, TripData} from '../../types/TripData';
import {AgentEvent, apiClient} from '../../services/apiClient';
import {useAppStore} from '../../state/hooks';
import {useItinerary} from '../../state/query/hooks';

interface AgentProgressModalProps {
  tripData: TripData;
  onComplete: () => void;
  onCancel?: () => void;
}

interface AgentProgress {
  task: AgentTask;
  status: 'pending' | 'running' | 'completed' | 'failed';
  progress: number;
  lastEmittedProgress: number; // Boundary for individual agent progress
  startTime?: number;
  endTime?: number;
  error?: string;
}

export function AgentProgressModal({ tripData, onComplete, onCancel }: AgentProgressModalProps) {
  const { setCurrentTrip } = useAppStore();
  // Make API calls immediately for SSE connection, but delay completion checking
  const { refetch: refetchItinerary } = useItinerary(tripData.id);
  const [agents, setAgents] = useState<AgentProgress[]>(
    AGENT_TASKS.map(task => ({
      task,
      status: 'pending',
      progress: 0,
      lastEmittedProgress: 0
    }))
  );
  
  const [currentAgentIndex, setCurrentAgentIndex] = useState(0);
  const [overallProgress, setOverallProgress] = useState(0);
  const [targetProgress, setTargetProgress] = useState(0);
  const [lastEmittedProgress, setLastEmittedProgress] = useState(0);
  const [startTime] = useState(Date.now());
  const [elapsedTime, setElapsedTime] = useState(0);
  const [hasCompleted, setHasCompleted] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [isRetrying, setIsRetrying] = useState(false);
  const [sseConnectionStatus, setSseConnectionStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'error'>('connecting');
  const completionTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const maxTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const progressAnimationRef = useRef<NodeJS.Timeout | null>(null);

  // Smooth progress animation - moves 2% per second towards target, but respects lastEmittedProgress as boundary
  useEffect(() => {
    if (progressAnimationRef.current) {
      clearInterval(progressAnimationRef.current);
    }

    // Only animate if target is higher than current AND we haven't exceeded the last emitted progress
    if (targetProgress > overallProgress && overallProgress < lastEmittedProgress) {
      
      progressAnimationRef.current = setInterval(() => {
        setOverallProgress(prev => {
          // Don't exceed the last emitted progress - this is our boundary
          const maxAllowedProgress = Math.min(targetProgress, lastEmittedProgress);
          const newProgress = Math.min(prev + 2, maxAllowedProgress);
          
          
          
          if (newProgress >= maxAllowedProgress) {
            
            if (progressAnimationRef.current) {
              clearInterval(progressAnimationRef.current);
              progressAnimationRef.current = null;
            }
          }
          return newProgress;
        });
      }, 1000); // Update every 1 second (2% per second)
    } else {
      
    }

    return () => {
      if (progressAnimationRef.current) {
        clearInterval(progressAnimationRef.current);
        progressAnimationRef.current = null;
      }
    };
  }, [targetProgress, overallProgress, lastEmittedProgress]);

  // Individual agent progress animation - moves 2% per second towards target, but respects lastEmittedProgress as boundary
  useEffect(() => {
    setAgents(prev => prev.map(agent => {
      // Only animate if agent is running and current progress is below the boundary
      if (agent.status === 'running' && agent.progress < agent.lastEmittedProgress) {
        const newProgress = Math.min(agent.progress + 2, agent.lastEmittedProgress);
        
        return { ...agent, progress: newProgress };
      }
      return agent;
    }));
  }, [elapsedTime]); // Trigger every second when elapsedTime updates

  // Update elapsed time every second
  useEffect(() => {
    const interval = setInterval(() => {
      setElapsedTime(Date.now() - startTime);
    }, 1000);

    return () => clearInterval(interval);
  }, [startTime]);

  const handleSSEEvent = (event: MessageEvent) => {
    try {
      
      
      // Handle connection messages
      if (event.data && typeof event.data === 'string' && event.data.includes('Connected to agent stream')) {
        
        setSseConnectionStatus('connected');
        return;
      }
      
      // Parse agent events
      const agentEvent: AgentEvent = JSON.parse(event.data);
      
      
      
      // Update agent status based on event
      setAgents(prev => prev.map(agent => {
        // Direct match by agent kind
        if (agent.task.id === agentEvent.kind) { // todo keep consistent status names across the repo managed by types
          const newStatus = agentEvent.status === 'completed' ? 'completed' : 
                           agentEvent.status === 'running' ? 'running' : 
                           agentEvent.status === 'failed' ? 'failed' : 'pending';
          
          
          
          return {
            ...agent,
            status: newStatus,
            progress: agentEvent.progress || agent.progress,
            lastEmittedProgress: agentEvent.progress !== null && agentEvent.progress !== undefined ? agentEvent.progress : 
                                newStatus === 'completed' ? 100 : 
                                newStatus === 'failed' ? 0 : agent.lastEmittedProgress,
            startTime: agentEvent.status === 'running' && !agent.startTime ? Date.now() : agent.startTime,
            endTime: agentEvent.status === 'completed' || agentEvent.status === 'failed' ? Date.now() : agent.endTime,
            error: agentEvent.status === 'failed' ? agentEvent.message || 'Agent failed' : undefined
          };
        }
        
        return agent;
      }));
      
      // Individual agent progress boundaries are now handled per-agent above
      
      // Update overall progress with smooth calculation for multiple agents
      setAgents(prev => {
        const completedCount = prev.filter(a => a.status === 'completed').length;
        const failedCount = prev.filter(a => a.status === 'failed').length;
        const runningAgent = prev.find(a => a.status === 'running');
        
        // Calculate progress: completed agents + current running agent progress
        let totalProgress = 0;
        if (completedCount === AGENT_TASKS.length) {
          totalProgress = 100;
        } else if (runningAgent) {
          // Base progress from completed agents + current agent progress
          const baseProgress = (completedCount / AGENT_TASKS.length) * 100;
          const currentAgentProgress = (runningAgent.progress / 100) * (100 / AGENT_TASKS.length);
          totalProgress = baseProgress + currentAgentProgress;
        } else {
          totalProgress = (completedCount / AGENT_TASKS.length) * 100;
        }
        
        // Set target progress - the animation will smoothly move towards it
        setTargetProgress(Math.round(totalProgress));
        
        // Update last emitted progress as the boundary
        setLastEmittedProgress(Math.round(totalProgress));
        
        
        
        
        // Check if any agent failed
        if (failedCount > 0 && !hasError) {
          setHasError(true);
          setErrorMessage('One or more agents failed to complete. You can retry or cancel the operation.');
        }
        
        return prev;
      });
      
      // Check if all agents are completed - but don't trigger completion here
      // Let the completion checker handle it to avoid race conditions
      if (agentEvent.status === 'completed') {
        
      }
      
      // Check if any agent failed
      if (agentEvent.status === 'failed') {
        
        setHasError(true);
        setErrorMessage(`Trip generation failed: ${agentEvent.message || 'Unknown error occurred'}`);
      }
      
    } catch (error) {
      
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
      
      setHasError(true);
      setErrorMessage('No trip data available. Please try again.');
      return;
    }

    

    // Set maximum timeout (5 minutes)
    const maxTimeout = setTimeout(() => {
      if (!hasCompleted && !hasError) {
        
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
            
            
            
            // Check if we have actual itinerary content (not just placeholder data)
            const hasActualContent = responseData.itinerary?.days && 
                                   responseData.itinerary.days.length > 0 && 
                                   responseData.destination !== 'Loading...';
            
            // Check if all agents are completed by looking at current agent states
            const allAgentsCompleted = agents.every(agent => agent.status === 'completed');
            
            // Only consider completed if all agents are done AND has actual content
            if (allAgentsCompleted && hasActualContent) {
              
              setHasCompleted(true);
              setTargetProgress(100); // Set target to 100% - animation will handle the smooth transition
              setLastEmittedProgress(100); // Set boundary to 100% for completion
              setOverallProgress(100); // Also set immediately to ensure 100% is shown
              setCurrentTrip(responseData);
              
              // Clean up timers and event source
              if (completionTimeoutRef.current) {
                clearTimeout(completionTimeoutRef.current);
                completionTimeoutRef.current = null;
              }
              if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
              }
              if (progressAnimationRef.current) {
                clearInterval(progressAnimationRef.current);
                progressAnimationRef.current = null;
              }
              
              onComplete();
              return;
            } else if (responseData.status === 'planning' && hasActualContent) {
              
              setCurrentTrip(responseData);
            } else if (responseData.status === 'planning' && !hasActualContent) {
              
            }
          }
        } catch (e) {
          
          // Don't set error on completion check failure, just continue
        }
        
        // Schedule next check only if not completed
        if (!hasCompleted && !hasError) {
          const timeout = setTimeout(checkCompletion, 2000);
          completionTimeoutRef.current = timeout;
        }
      }
    };

    // Connect to SSE stream for real-time agent updates FIRST
    const eventSource = apiClient.createAgentEventStream(tripData.id);
    eventSourceRef.current = eventSource;
    
    // Start checking after 30 seconds to allow backend processing time
    completionTimeoutRef.current = setTimeout(checkCompletion, 30000);
    
    eventSource.onopen = () => {
      
      setSseConnectionStatus('connected');
    };
    
    // Handle all SSE events
    eventSource.onmessage = (event) => {
      
      handleSSEEvent(event);
    };
    
    // Handle named events (like 'agent-event')
    eventSource.addEventListener('agent-event', (event) => {
      
      handleSSEEvent(event);
    });
    
    eventSource.addEventListener('connected', (event) => {
      
      setSseConnectionStatus('connected');
    });
    
    eventSource.onerror = (error) => {
      
      setSseConnectionStatus('error');
      // Don't immediately set error state, let the completion checker handle it
    };
    
    // Cleanup function to close SSE connection
    return () => {
      
      if (completionTimeoutRef.current) {
        clearTimeout(completionTimeoutRef.current);
      }
      if (maxTimeoutRef.current) {
        clearTimeout(maxTimeoutRef.current);
      }
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
      if (progressAnimationRef.current) {
        clearInterval(progressAnimationRef.current);
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
            <p>Elapsed time: {formatTime(elapsedTime)}</p>
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




