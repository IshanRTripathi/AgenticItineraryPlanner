import React, { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '../ui/dialog';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Progress } from '../ui/progress';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '../ui/collapsible';
import { ChevronDown, ChevronRight, Clock, CheckCircle, AlertTriangle, XCircle, Play } from 'lucide-react';

interface AgentEvent {
  agent: 'places' | 'flights' | 'hotels' | 'restaurants' | 'transit' | 'itinerary';
  status: 'pending' | 'running' | 'done' | 'warning' | 'error';
  startedAt?: number;
  finishedAt?: number;
  note: string;
}

interface AgentOrchestratorProps {
  isOpen: boolean;
  onClose: () => void;
  onComplete: () => void;
}

const agentDefinitions = [
  {
    id: 'places' as const,
    name: 'Places Agent',
    description: 'Discovers city areas, heatmaps, opening hours, crowd windows',
    dependency: null
  },
  {
    id: 'itinerary' as const,
    name: 'Itinerary Agent (planner)',
    description: 'Drafts day slots from Places Agent output',
    dependency: 'places'
  },
  {
    id: 'flights' as const,
    name: 'Flights Agent',
    description: 'Fares & hold eligibility',
    dependency: null
  },
  {
    id: 'hotels' as const,
    name: 'Hotels Agent',
    description: 'Ratings & availability filtered by Places heatmaps',
    dependency: 'places'
  },
  {
    id: 'restaurants' as const,
    name: 'Restaurants Agent',
    description: 'Ratings, cost, cuisine near heatmap zones',
    dependency: null
  },
  {
    id: 'transit' as const,
    name: 'Transit Agent',
    description: 'Passes, transfers, travel times',
    dependency: null
  }
];

const demoTimeline: AgentEvent[] = [
  // Initial parallel agents (no dependencies)
  {
    agent: 'places',
    status: 'running',
    startedAt: 0,
    note: ''
  },
  {
    agent: 'flights',
    status: 'running',
    startedAt: 0,
    note: ''
  },
  {
    agent: 'restaurants',
    status: 'running',
    startedAt: 0,
    note: ''
  },
  {
    agent: 'transit',
    status: 'running',
    startedAt: 0,
    note: ''
  },
  
  // Early completions
  {
    agent: 'transit',
    status: 'done',
    startedAt: 0,
    finishedAt: 4,
    note: 'Metro + Suica pass; airport transfer 55m.'
  },
  {
    agent: 'restaurants',
    status: 'done',
    startedAt: 0,
    finishedAt: 5,
    note: 'Top 20 near hotspots, avg ★4.4, ₹₹.'
  },
  {
    agent: 'flights',
    status: 'done',
    startedAt: 0,
    finishedAt: 7,
    note: 'TYO fares from ₹29,800; 24h hold: Yes.'
  },
  
  // Places completes, enabling dependent agents
  {
    agent: 'places',
    status: 'done',
    startedAt: 0,
    finishedAt: 12,
    note: 'Found hotspots: Asakusa, Shibuya, Shinjuku, Odaiba; crowd windows + hours.'
  },
  {
    agent: 'hotels',
    status: 'running',
    startedAt: 12,
    note: ''
  },
  {
    agent: 'itinerary',
    status: 'running',
    startedAt: 12,
    note: ''
  },
  
  // Final completions
  {
    agent: 'hotels',
    status: 'done',
    startedAt: 12,
    finishedAt: 18,
    note: 'Hotels ≥★4.2 near Asakusa/Shinjuku; Standard Room ₹6,428/night.'
  },
  {
    agent: 'itinerary',
    status: 'done',
    startedAt: 12,
    finishedAt: 19,
    note: 'Drafted Day 2: Breakfast Cafe → Heritage Walk → Art Gallery → Rooftop Dinner.'
  }
];

export function AgentOrchestrator({ isOpen, onClose, onComplete }: AgentOrchestratorProps) {
  const [currentTime, setCurrentTime] = useState(0);
  const [isRunning, setIsRunning] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const [showDetails, setShowDetails] = useState(false);
  const [currentEvents, setCurrentEvents] = useState<AgentEvent[]>([]);

  useEffect(() => {
    if (!isRunning) return;

    const interval = setInterval(() => {
      setCurrentTime(prev => {
        const nextTime = prev + 1;
        
        // Update events for current time
        const eventsAtTime = demoTimeline.filter(event => 
          (event.startedAt === nextTime) || (event.finishedAt === nextTime)
        );
        
        if (eventsAtTime.length > 0) {
          setCurrentEvents(prev => {
            const updated = [...prev];
            eventsAtTime.forEach(event => {
              const existingIndex = updated.findIndex(e => e.agent === event.agent);
              if (existingIndex >= 0) {
                updated[existingIndex] = event;
              } else {
                updated.push(event);
              }
            });
            return updated;
          });
        }
        
        // Check if complete
        if (nextTime >= 19) {
          setIsRunning(false);
          setIsComplete(true);
        }
        
        return nextTime;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isRunning]);

  const startOrchestration = () => {
    setCurrentTime(0);
    setCurrentEvents([]);
    setIsRunning(true);
    setIsComplete(false);
  };

  const getAgentStatus = (agentId: string) => {
    const latestEvent = currentEvents
      .filter(e => e.agent === agentId)
      .sort((a, b) => (b.finishedAt || b.startedAt || 0) - (a.finishedAt || a.startedAt || 0))[0];
    
    if (!latestEvent) {
      const agent = agentDefinitions.find(a => a.id === agentId);
      if (agent?.dependency) {
        const dependencyDone = currentEvents.some(e => e.agent === agent.dependency && e.status === 'done');
        return dependencyDone ? 'pending' : 'waiting';
      }
      return 'pending';
    }
    
    return latestEvent.status;
  };

  const getAgentNote = (agentId: string) => {
    const latestEvent = currentEvents
      .filter(e => e.agent === agentId && e.note)
      .sort((a, b) => (b.finishedAt || b.startedAt || 0) - (a.finishedAt || a.startedAt || 0))[0];
    
    return latestEvent?.note || '';
  };

  const getAgentTiming = (agentId: string) => {
    const events = currentEvents.filter(e => e.agent === agentId);
    const startEvent = events.find(e => e.startedAt !== undefined);
    const endEvent = events.find(e => e.finishedAt !== undefined);
    
    if (startEvent && endEvent) {
      return `00:${String(startEvent.startedAt).padStart(2, '0')}→00:${String(endEvent.finishedAt).padStart(2, '0')}`;
    } else if (startEvent) {
      return `00:${String(startEvent.startedAt).padStart(2, '0')}→running`;
    }
    
    return '';
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'running':
        return <Play className="h-4 w-4 text-blue-500 animate-pulse" />;
      case 'done':
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'warning':
        return <AlertTriangle className="h-4 w-4 text-yellow-500" />;
      case 'error':
        return <XCircle className="h-4 w-4 text-red-500" />;
      case 'waiting':
        return <Clock className="h-4 w-4 text-gray-400" />;
      default:
        return <Clock className="h-4 w-4 text-gray-400" />;
    }
  };

  const getStatusBadge = (status: string) => {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      pending: 'outline',
      waiting: 'outline',
      running: 'default',
      done: 'secondary',
      warning: 'destructive',
      error: 'destructive'
    };
    
    return (
      <Badge variant={variants[status] || 'outline'} className="min-w-16 justify-center">
        {status === 'waiting' ? 'Waiting for Places' : status.charAt(0).toUpperCase() + status.slice(1)}
      </Badge>
    );
  };

  const formatTime = (seconds: number) => {
    return `00:${String(seconds).padStart(2, '0')}`;
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Building your plan — multi-agent run</DialogTitle>
          <DialogDescription>
            AI agents working in parallel to create your personalized itinerary
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Progress & Timer */}
          <div className="flex items-center justify-between">
            <div className="text-sm text-gray-600">
              Time: {formatTime(currentTime)} / 00:19
            </div>
            <div className="flex gap-2">
              {!isRunning && !isComplete && (
                <Button onClick={startOrchestration}>
                  Start Orchestration
                </Button>
              )}
              <Button variant="outline" onClick={() => setShowDetails(!showDetails)}>
                {showDetails ? 'Hide' : 'Show'} details
              </Button>
            </div>
          </div>

          <Progress value={(currentTime / 19) * 100} className="w-full" />

          {/* Agent Status Table */}
          <div className="border rounded-lg overflow-hidden">
            <div className="grid grid-cols-12 gap-4 p-4 bg-gray-50 text-sm">
              <div className="col-span-3">Agent</div>
              <div className="col-span-2">Status</div>
              <div className="col-span-2">Started</div>
              <div className="col-span-2">Finished</div>
              <div className="col-span-3">Notes</div>
            </div>
            
            {agentDefinitions.map((agent) => {
              const status = getAgentStatus(agent.id);
              const note = getAgentNote(agent.id);
              const timing = getAgentTiming(agent.id);
              
              return (
                <div key={agent.id} className="grid grid-cols-12 gap-4 p-4 border-t text-sm">
                  <div className="col-span-3 flex items-center gap-2">
                    {getStatusIcon(status)}
                    <div>
                      <div>{agent.name}</div>
                      {agent.dependency && (
                        <div className="text-xs text-gray-500">
                          Depends on: {agentDefinitions.find(a => a.id === agent.dependency)?.name}
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="col-span-2 flex items-center">
                    {getStatusBadge(status)}
                  </div>
                  <div className="col-span-2 flex items-center">
                    {timing.split('→')[0] || '—'}
                  </div>
                  <div className="col-span-2 flex items-center">
                    {timing.includes('→') ? timing.split('→')[1] || '—' : '—'}
                  </div>
                  <div className="col-span-3 flex items-center text-xs text-gray-600">
                    {note || agent.description}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Detailed Logs */}
          <Collapsible open={showDetails} onOpenChange={setShowDetails}>
            <CollapsibleTrigger asChild>
              <Button variant="outline" className="w-full">
                <ChevronDown className="h-4 w-4 mr-2" />
                Agent Logs (All / Warnings / Errors)
              </Button>
            </CollapsibleTrigger>
            <CollapsibleContent className="space-y-2 mt-4">
              <div className="border rounded-lg p-4 max-h-60 overflow-y-auto bg-gray-50">
                {currentEvents.length === 0 ? (
                  <div className="text-sm text-gray-500">No events yet. Start orchestration to see logs.</div>
                ) : (
                  currentEvents.map((event, index) => (
                    <div key={index} className="text-sm mb-2 p-2 bg-white rounded border">
                      <div className="flex items-center gap-2 mb-1">
                        <Badge variant="outline" className="text-xs">
                          {formatTime(event.finishedAt || event.startedAt || 0)}
                        </Badge>
                        <span className="font-medium">{event.agent}</span>
                        {getStatusBadge(event.status)}
                      </div>
                      {event.note && (
                        <div className="text-gray-600 pl-4">{event.note}</div>
                      )}
                    </div>
                  ))
                )}
              </div>
            </CollapsibleContent>
          </Collapsible>

          {/* Footer Actions */}
          <div className="flex justify-between">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button
              onClick={() => {
                onComplete();
                onClose();
              }}
              disabled={!isComplete}
            >
              Continue when complete
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}



