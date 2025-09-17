import React, { useState, useEffect } from 'react';
import { Card } from './ui/card';
import { Progress } from './ui/progress';
import { Badge } from './ui/badge';
import { CheckCircle2, Loader2, Clock, Zap } from 'lucide-react';
import { AGENT_TASKS, AgentTask } from '../types/TripData';
import { TripData } from '../types/TripData';
import { apiClient, AgentEvent } from '../services/apiClient';

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

  useEffect(() => {
    if (!tripData.id) return;

    // Connect to SSE stream for real-time agent updates
    const eventSource = apiClient.createAgentEventStream(tripData.id);
    
    eventSource.onmessage = (event) => {
      try {
        const agentEvent: AgentEvent = JSON.parse(event.data);
        
        // Update agent status based on event
        setAgents(prev => prev.map(agent => {
          const matchingTask = AGENT_TASKS.find(task => 
            task.id.includes(agentEvent.kind) || agentEvent.kind.includes(task.id)
          );
          
          if (matchingTask?.id === agent.task.id) {
            const newStatus = agentEvent.status === 'succeeded' ? 'completed' : 
                             agentEvent.status === 'running' ? 'running' : 
                             agentEvent.status === 'failed' ? 'completed' : 'pending';
            
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
        const completedCount = agents.filter(a => a.status === 'completed').length;
        const runningAgent = agents.find(a => a.status === 'running');
        const runningProgress = runningAgent ? (runningAgent.progress / 100) : 0;
        
        const totalProgress = ((completedCount + runningProgress) / AGENT_TASKS.length) * 100;
        setOverallProgress(Math.round(totalProgress));
        
        // Check if all agents are completed
        if (agentEvent.kind === 'orchestrator' && agentEvent.status === 'succeeded') {
          setTimeout(() => onComplete(), 1000);
        }
        
      } catch (error) {
        console.error('Error parsing agent event:', error);
      }
    };
    
    eventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
    };
    
    // Cleanup on unmount
    return () => {
      eventSource.close();
    };
  }, [tripData.id, onComplete, agents]);

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

  const elapsedTime = formatTime(Date.now() - startTime);
  const completedCount = agents.filter(a => a.status === 'completed').length;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <Card className="w-full max-w-2xl mx-4 p-8">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <Zap className="w-8 h-8 text-white" />
          </div>
          <h2 className="text-2xl font-semibold mb-2">Creating Your Perfect Itinerary</h2>
          <p className="text-gray-600">Our AI agents are working together to plan your amazing trip</p>
        </div>

        {/* Overall Progress */}
        <div className="mb-8">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm font-medium">Overall Progress</span>
            <div className="flex items-center gap-4 text-sm text-gray-500">
              <span>{completedCount}/{AGENT_TASKS.length} completed</span>
              <span>{elapsedTime} elapsed</span>
            </div>
          </div>
          <Progress value={overallProgress} className="h-3" />
          <div className="text-center mt-1">
            <span className="text-lg font-semibold text-blue-600">{Math.round(overallProgress)}%</span>
          </div>
        </div>

        {/* Agent Status List */}
        <div className="space-y-3 max-h-80 overflow-y-auto">
          {agents.map((agent, index) => (
            <div
              key={agent.task.id}
              className={`flex items-center p-4 rounded-lg border transition-all ${
                agent.status === 'running' 
                  ? 'bg-blue-50 border-blue-200 shadow-md' 
                  : agent.status === 'completed'
                  ? 'bg-green-50 border-green-200'
                  : 'bg-gray-50 border-gray-200'
              }`}
            >
              <div className="flex items-center gap-3 flex-1">
                <span className="text-2xl">{agent.task.icon}</span>
                {getAgentIcon(agent)}
                
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <h3 className="font-medium">{agent.task.name}</h3>
                    {agent.status === 'running' && (
                      <Badge variant="secondary" className="animate-pulse">
                        Running
                      </Badge>
                    )}
                    {agent.status === 'completed' && (
                      <Badge variant="default" className="bg-green-500">
                        Done
                      </Badge>
                    )}
                  </div>
                  <p className="text-sm text-gray-600">{agent.task.description}</p>
                  
                  {agent.status === 'running' && (
                    <div className="mt-2">
                      <Progress value={agent.progress} className="h-1" />
                    </div>
                  )}
                </div>

                {agent.endTime && agent.startTime && (
                  <div className="text-xs text-gray-500 ml-2">
                    {formatTime(agent.endTime - agent.startTime)}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Trip Info Footer */}
        <div className="mt-6 pt-6 border-t">
          <div className="flex justify-between text-sm text-gray-500">
            <span>
              {tripData.startLocation.name} → {tripData.endLocation.name}
            </span>
            <span>
              {tripData.travelers.length} traveler{tripData.travelers.length > 1 ? 's' : ''}
            </span>
            <span>
              {tripData.budget.currency} {tripData.budget.total.toLocaleString()}
            </span>
          </div>
        </div>
      </Card>
    </div>
  );
}