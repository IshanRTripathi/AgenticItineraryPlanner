import React, { useState, useEffect, useRef } from 'react';
import { Card } from './ui/card';
import { Progress } from './ui/progress';
import { Badge } from './ui/badge';
import { CheckCircle2, Loader2, Clock, Zap } from 'lucide-react';
import { AGENT_TASKS, AgentTask } from '../types/TripData';
import { TripData } from '../types/TripData';
import { apiClient, AgentEvent } from '../services/apiClient';
import { useAppStore } from '../state/hooks';
import { useItinerary } from '../state/query/hooks';

interface AgentProgressModalProps {
  tripData: TripData;
  onComplete: () => void;
}

interface AgentProgress {
  task: AgentTask;
  status: 'pending' | 'running' | 'completed';
  progress: number;
  startTime?: number;
  endTime?: number;
}

export function AgentProgressModal({ tripData, onComplete }: AgentProgressModalProps) {
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
  const completionTimeoutRef = useRef<NodeJS.Timeout | null>(null);

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
                           agentEvent.status === 'failed' ? 'completed' : 'pending';
          
          console.log(`Updating agent ${agent.task.name} from ${agent.status} to ${newStatus}`);
          
          return {
            ...agent,
            status: newStatus,
            progress: agentEvent.progress || agent.progress,
            startTime: agentEvent.status === 'running' && !agent.startTime ? Date.now() : agent.startTime,
            endTime: agentEvent.status === 'succeeded' ? Date.now() : agent.endTime
          };
        }
        
        return agent;
      }));
      
      // Update overall progress
      setAgents(prev => {
        const completedCount = prev.filter(a => a.status === 'completed').length;
        const runningAgent = prev.find(a => a.status === 'running');
        const runningProgress = runningAgent ? (runningAgent.progress / 100) : 0;
        
        const totalProgress = ((completedCount + runningProgress) / AGENT_TASKS.length) * 100;
        setOverallProgress(Math.round(totalProgress));
        
        console.log(`Progress update: ${completedCount}/${AGENT_TASKS.length} completed, ${Math.round(totalProgress)}% total`);
        
        return prev;
      });
      
      // Check if planner agent is completed
      if (agentEvent.kind === 'planner' && agentEvent.status === 'succeeded') {
        console.log('Planner agent completed, refetching itinerary...');
        if (!hasCompleted) {
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
      
    } catch (error) {
      console.error('Error processing SSE event:', error);
    }
  };

  useEffect(() => {
    if (!tripData.id) {
      console.error('No trip data ID available for SSE connection');
      return;
    }

    console.log('Starting SSE connection for itinerary:', tripData.id);

    // Simple completion checker - check every 2 seconds if itinerary is completed
    const checkCompletion = async () => {
      if (!hasCompleted) {
        try {
          const result = await refetchItinerary();
          if (result.data) {
            const responseData = result.data as TripData;
            console.log('Checking completion status:', responseData.status);
            if (responseData.status === 'completed' || (responseData.itinerary?.days && responseData.itinerary.days.length > 0)) {
              console.log('Itinerary is completed, proceeding to next screen');
              setHasCompleted(true);
              setCurrentTrip(responseData);
              onComplete();
              return;
            }
          }
        } catch (e) {
          console.warn('Completion check failed:', e);
        }
        
        // Schedule next check
        const timeout = setTimeout(checkCompletion, 2000);
        completionTimeoutRef.current = timeout;
      }
    };

    // Start checking after 5 seconds
    const initialTimeout = setTimeout(checkCompletion, 5000);
    completionTimeoutRef.current = initialTimeout;

    // Connect to SSE stream for real-time agent updates
    const eventSource = apiClient.createAgentEventStream(tripData.id);
    
    eventSource.onopen = () => {
      console.log('SSE connection opened for itinerary:', tripData.id);
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
    });
    
    eventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
    };
    
    // Cleanup function to close SSE connection
    return () => {
      console.log('Closing SSE connection for itinerary:', tripData.id);
      if (completionTimeoutRef.current) {
        clearTimeout(completionTimeoutRef.current);
      }
      eventSource.close();
    };
  }, [tripData.id, onComplete, refetchItinerary, setCurrentTrip, hasCompleted]);

  const formatTime = (ms: number) => {
    return `${(ms / 1000).toFixed(1)}s`;
  };

  const getAgentIcon = (agent: AgentProgress) => {
    switch (agent.status) {
      case 'completed':
        return <CheckCircle2 className="w-5 h-5 text-green-500" />;
      case 'running':
        return <Loader2 className="w-5 h-5 text-blue-500 animate-spin" />;
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
      default:
        return <Badge variant="secondary">Pending</Badge>;
    }
  };

  const getElapsedTime = () => {
    return Date.now() - startTime;
  };

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
                </div>
              </div>
            ))}
          </div>

          {/* Footer */}
          <div className="text-center text-sm text-gray-500">
            <p>Elapsed time: {formatTime(getElapsedTime())}</p>
            <p className="mt-1">This usually takes 30-60 seconds</p>
          </div>
        </div>
      </Card>
    </div>
  );
}