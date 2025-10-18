import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Bot, Sparkles, MapPin, Calendar, DollarSign, Play, Settings } from 'lucide-react';
import { AgentConfigModal } from './AgentConfigModal';
import { AgentErrorDisplay } from './AgentErrorDisplay';

interface Agent {
  id: string;
  name: string;
  description: string;
  icon: React.ReactNode;
  capabilities: string[];
  status: 'available' | 'running' | 'disabled';
}

interface AgentControlPanelProps {
  itineraryId: string;
  onAgentExecute?: (agentId: string) => void;
  onAgentConfigure?: (agentId: string) => void;
  className?: string;
}

const AVAILABLE_AGENTS: Agent[] = [
  {
    id: 'planner',
    name: 'Planner Agent',
    description: 'Creates and optimizes itinerary structure',
    icon: <Calendar className="w-6 h-6" />,
    capabilities: ['plan', 'create', 'optimize'],
    status: 'available',
  },
  {
    id: 'enrichment',
    name: 'Enrichment Agent',
    description: 'Adds details, photos, and reviews',
    icon: <Sparkles className="w-6 h-6" />,
    capabilities: ['enrich', 'photos', 'reviews'],
    status: 'available',
  },
  {
    id: 'booking',
    name: 'Booking Agent',
    description: 'Handles reservations and bookings',
    icon: <DollarSign className="w-6 h-6" />,
    capabilities: ['book', 'reserve', 'confirm'],
    status: 'available',
  },
  {
    id: 'location',
    name: 'Location Agent',
    description: 'Optimizes routes and locations',
    icon: <MapPin className="w-6 h-6" />,
    capabilities: ['route', 'optimize', 'navigate'],
    status: 'available',
  },
];

export function AgentControlPanel({
  itineraryId,
  onAgentExecute,
  onAgentConfigure,
  className = '',
}: AgentControlPanelProps) {
  const [runningAgents, setRunningAgents] = useState<Set<string>>(new Set());
  const [configModalOpen, setConfigModalOpen] = useState(false);
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null);
  const [agentErrors, setAgentErrors] = useState<Map<string, { message: string; timestamp: Date }>>(new Map());

  const handleExecute = async (agentId: string) => {
    setRunningAgents(prev => new Set(prev).add(agentId));
    
    try {
      // Import agentService dynamically to avoid circular deps
      const { agentService } = await import('../../services/agentService');
      
      // Check if agent can execute
      const { canExecute, lockedNodes } = await agentService.canExecuteAgent(itineraryId);
      
      if (!canExecute) {
        const proceed = window.confirm(
          `Warning: ${lockedNodes.length} nodes are locked and will not be modified.\n\n` +
          `Locked nodes: ${lockedNodes.join(', ')}\n\n` +
          `Do you want to proceed anyway?`
        );
        
        if (!proceed) {
          setRunningAgents(prev => {
            const next = new Set(prev);
            next.delete(agentId);
            return next;
          });
          return;
        }
      }
      
      // Execute agent
      const result = await agentService.executeAgent({
        itineraryId,
        agentType: agentId,
        respectLocks: true,
      });
      
      if (!result.success) {
        setAgentErrors(prev => new Map(prev).set(agentId, {
          message: result.message || 'Agent execution failed',
          timestamp: new Date()
        }));
      } else {
        // Clear any previous errors for this agent
        setAgentErrors(prev => {
          const next = new Map(prev);
          next.delete(agentId);
          return next;
        });
        onAgentExecute?.(agentId);
      }
    } catch (error: any) {
      console.error('Agent execution error:', error);
      setAgentErrors(prev => new Map(prev).set(agentId, {
        message: error.message || 'Failed to execute agent. Please try again.',
        timestamp: new Date()
      }));
    } finally {
      setRunningAgents(prev => {
        const next = new Set(prev);
        next.delete(agentId);
        return next;
      });
    }
  };

  const getStatusBadge = (agent: Agent) => {
    if (runningAgents.has(agent.id)) {
      return <Badge variant="default">Running</Badge>;
    }
    if (agent.status === 'disabled') {
      return <Badge variant="secondary">Disabled</Badge>;
    }
    return <Badge variant="outline">Available</Badge>;
  };

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Bot className="w-5 h-5" />
          <CardTitle>AI Agents</CardTitle>
        </div>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {AVAILABLE_AGENTS.map(agent => (
            <Card key={agent.id} className="overflow-hidden">
              <CardContent className="p-4">
                <div className="flex items-start gap-3">
                  <div className="p-2 bg-blue-100 rounded-lg">
                    {agent.icon}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-1">
                      <h4 className="font-semibold text-sm">{agent.name}</h4>
                      {getStatusBadge(agent)}
                    </div>
                    <p className="text-xs text-gray-600 mb-2">{agent.description}</p>
                    <div className="flex flex-wrap gap-1 mb-3">
                      {agent.capabilities.map(cap => (
                        <Badge key={cap} variant="secondary" className="text-xs">
                          {cap}
                        </Badge>
                      ))}
                    </div>
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        onClick={() => handleExecute(agent.id)}
                        disabled={agent.status === 'disabled' || runningAgents.has(agent.id)}
                        className="flex-1"
                      >
                        <Play className="w-3 h-3 mr-1" />
                        {runningAgents.has(agent.id) ? 'Running...' : 'Execute'}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setSelectedAgent(agent);
                          setConfigModalOpen(true);
                          onAgentConfigure?.(agent.id);
                        }}
                      >
                        <Settings className="w-3 h-3" />
                      </Button>
                    </div>
                    
                    {/* Error Display for this agent */}
                    {agentErrors.has(agent.id) && (
                      <div className="mt-2">
                        <AgentErrorDisplay
                          error={agentErrors.get(agent.id)!.message}
                          agentName={agent.name}
                          timestamp={agentErrors.get(agent.id)!.timestamp}
                          onDismiss={() => {
                            setAgentErrors(prev => {
                              const next = new Map(prev);
                              next.delete(agent.id);
                              return next;
                            });
                          }}
                          onRetry={() => handleExecute(agent.id)}
                        />
                      </div>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Agent Configuration Modal */}
        {selectedAgent && (
          <AgentConfigModal
            isOpen={configModalOpen}
            onClose={() => {
              setConfigModalOpen(false);
              setSelectedAgent(null);
            }}
            agent={{
              id: selectedAgent.id,
              name: selectedAgent.name,
              description: selectedAgent.description,
              capabilities: selectedAgent.capabilities,
            }}
            onExecute={(config) => {
              handleExecute(selectedAgent.id);
              setConfigModalOpen(false);
              setSelectedAgent(null);
            }}
          />
        )}
      </CardContent>
    </Card>
  );
}
