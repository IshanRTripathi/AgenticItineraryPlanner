import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { ScrollArea } from '../ui/scroll-area';
import { Button } from '../ui/button';
import { AgentProgressBar } from './AgentProgressBar';
import { X } from 'lucide-react';
import { useSseConnection } from '../../hooks/useSseConnection';

interface AgentExecutionProgressProps {
  itineraryId: string;
  agentType: string;
  executionId: string;
  onCancel?: () => void;
  onComplete?: (result: any) => void;
}

interface AgentEvent {
  status: 'queued' | 'running' | 'completed' | 'failed';
  progress: number;
  currentStep: string;
  logs: string[];
  result?: any;
  error?: string;
}

export const AgentExecutionProgress: React.FC<AgentExecutionProgressProps> = ({
  itineraryId,
  agentType,
  executionId,
  onCancel,
  onComplete
}) => {
  const [agentState, setAgentState] = useState<AgentEvent>({
    status: 'queued',
    progress: 0,
    currentStep: 'Initializing...',
    logs: []
  });

  const { isConnected } = useSseConnection(itineraryId);

  useEffect(() => {
    if (!isConnected) return;

    // Listen for agent events via SSE
    const handleAgentEvent = (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        if (data.executionId === executionId) {
          setAgentState(prev => ({
            ...prev,
            status: data.status || prev.status,
            progress: data.progress || prev.progress,
            currentStep: data.message || prev.currentStep,
            logs: data.logs || prev.logs,
            result: data.result,
            error: data.error
          }));

          if (data.status === 'completed' && onComplete && data.result) {
            onComplete(data.result);
          }
        }
      } catch (error) {
        
      }
    };

    // Subscribe to agent events
    window.addEventListener('agent_progress', handleAgentEvent as any);

    return () => {
      window.removeEventListener('agent_progress', handleAgentEvent as any);
    };
  }, [isConnected, executionId, onComplete]);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">
            {agentType} Agent Execution
          </CardTitle>
          {onCancel && agentState.status === 'running' && (
            <Button variant="ghost" size="sm" onClick={onCancel}>
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <AgentProgressBar
          status={agentState.status}
          progress={agentState.progress}
          currentStep={agentState.currentStep}
        />

        {agentState.logs.length > 0 && (
          <div>
            <h4 className="text-sm font-medium mb-2">Execution Log</h4>
            <ScrollArea className="h-32 border rounded p-2">
              <div className="space-y-1">
                {agentState.logs.map((log, index) => (
                  <p key={index} className="text-xs text-gray-600 font-mono">
                    {log}
                  </p>
                ))}
              </div>
            </ScrollArea>
          </div>
        )}

        {agentState.error && (
          <div className="bg-red-50 border border-red-200 rounded p-3">
            <p className="text-sm text-red-800">{agentState.error}</p>
          </div>
        )}

        {agentState.status === 'completed' && agentState.result && (
          <div className="bg-green-50 border border-green-200 rounded p-3">
            <p className="text-sm text-green-800">
              Agent execution completed successfully
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

