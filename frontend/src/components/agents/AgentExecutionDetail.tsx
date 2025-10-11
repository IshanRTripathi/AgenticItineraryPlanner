/**
 * Agent Execution Detail View
 * Shows detailed information about a specific agent execution
 */

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { ScrollArea } from '../ui/scroll-area';
import { CheckCircle, XCircle, Clock, RefreshCw, FileText, GitCommit, Activity } from 'lucide-react';
import { DiffViewer, DiffSection } from '../diff/DiffViewer';
import { createObjectDiff } from '../../utils/diffUtils';

interface ExecutionDetail {
  id: string;
  agentId: string;
  agentName: string;
  status: 'success' | 'failed' | 'running';
  startTime: Date;
  endTime?: Date;
  duration?: number;
  logs: LogEntry[];
  changes: any[];
  timeline: TimelineEvent[];
  before: any;
  after: any;
  parameters?: Record<string, any>;
}

interface LogEntry {
  timestamp: Date;
  level: 'info' | 'warn' | 'error';
  message: string;
}

interface TimelineEvent {
  timestamp: Date;
  event: string;
  details?: string;
}

interface AgentExecutionDetailProps {
  execution: ExecutionDetail;
  onReExecute?: () => void;
  onClose: () => void;
  className?: string;
}

export function AgentExecutionDetail({
  execution,
  onReExecute,
  onClose,
  className = '',
}: AgentExecutionDetailProps) {
  const [activeTab, setActiveTab] = useState('overview');

  const getStatusConfig = () => {
    switch (execution.status) {
      case 'success':
        return {
          icon: <CheckCircle className="w-5 h-5 text-green-500" />,
          badge: <Badge className="bg-green-100 text-green-700">Success</Badge>,
        };
      case 'failed':
        return {
          icon: <XCircle className="w-5 h-5 text-red-500" />,
          badge: <Badge className="bg-red-100 text-red-700">Failed</Badge>,
        };
      case 'running':
        return {
          icon: <Clock className="w-4 h-4 text-blue-500 animate-spin" />,
          badge: <Badge className="bg-blue-100 text-blue-700">Running</Badge>,
        };
    }
  };

  const config = getStatusConfig();
  const diffSections: DiffSection[] = execution.before && execution.after
    ? [createObjectDiff('Execution Changes', execution.before, execution.after)]
    : [];

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {config.icon}
            <div>
              <CardTitle>{execution.agentName} Execution</CardTitle>
              <p className="text-sm text-gray-500 mt-1">
                ID: {execution.id}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {config.badge}
            <Button variant="outline" size="sm" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="overview">
              <Activity className="w-4 h-4 mr-2" />
              Overview
            </TabsTrigger>
            <TabsTrigger value="logs">
              <FileText className="w-4 h-4 mr-2" />
              Logs
            </TabsTrigger>
            <TabsTrigger value="changes">
              <GitCommit className="w-4 h-4 mr-2" />
              Changes
            </TabsTrigger>
            <TabsTrigger value="timeline">
              <Clock className="w-4 h-4 mr-2" />
              Timeline
            </TabsTrigger>
          </TabsList>

          <TabsContent value="overview" className="space-y-4">
            {/* Execution Info */}
            <div className="grid grid-cols-2 gap-4">
              <div className="p-3 bg-gray-50 rounded-lg">
                <span className="text-xs text-gray-600">Start Time</span>
                <p className="text-sm font-semibold">{execution.startTime.toLocaleString()}</p>
              </div>
              {execution.endTime && (
                <div className="p-3 bg-gray-50 rounded-lg">
                  <span className="text-xs text-gray-600">End Time</span>
                  <p className="text-sm font-semibold">{execution.endTime.toLocaleString()}</p>
                </div>
              )}
              {execution.duration && (
                <div className="p-3 bg-gray-50 rounded-lg">
                  <span className="text-xs text-gray-600">Duration</span>
                  <p className="text-sm font-semibold">{execution.duration}ms</p>
                </div>
              )}
              <div className="p-3 bg-gray-50 rounded-lg">
                <span className="text-xs text-gray-600">Changes Made</span>
                <p className="text-sm font-semibold">{execution.changes.length}</p>
              </div>
            </div>

            {/* Parameters */}
            {execution.parameters && Object.keys(execution.parameters).length > 0 && (
              <div>
                <h4 className="font-semibold mb-2">Parameters</h4>
                <div className="p-3 bg-gray-50 rounded-lg">
                  <pre className="text-xs font-mono overflow-x-auto">
                    {JSON.stringify(execution.parameters, null, 2)}
                  </pre>
                </div>
              </div>
            )}

            {/* Re-execute */}
            {onReExecute && execution.status !== 'running' && (
              <Button onClick={onReExecute} className="w-full">
                <RefreshCw className="w-4 h-4 mr-2" />
                Re-execute with Same Parameters
              </Button>
            )}
          </TabsContent>

          <TabsContent value="logs">
            <ScrollArea className="h-96">
              <div className="space-y-2">
                {execution.logs.map((log, index) => (
                  <div
                    key={index}
                    className={`p-2 rounded text-sm font-mono ${
                      log.level === 'error'
                        ? 'bg-red-50 text-red-800'
                        : log.level === 'warn'
                        ? 'bg-amber-50 text-amber-800'
                        : 'bg-gray-50 text-gray-800'
                    }`}
                  >
                    <span className="text-xs text-gray-500">
                      {log.timestamp.toLocaleTimeString()}
                    </span>
                    {' '}
                    <span className="font-semibold">[{log.level.toUpperCase()}]</span>
                    {' '}
                    {log.message}
                  </div>
                ))}
              </div>
            </ScrollArea>
          </TabsContent>

          <TabsContent value="changes">
            {diffSections.length > 0 ? (
              <DiffViewer sections={diffSections} viewMode="side-by-side" showUnchanged={false} />
            ) : (
              <div className="text-center py-8 text-gray-500">
                No changes to display
              </div>
            )}
          </TabsContent>

          <TabsContent value="timeline">
            <ScrollArea className="h-96">
              <div className="space-y-3">
                {execution.timeline.map((event, index) => (
                  <div key={index} className="flex gap-3">
                    <div className="flex flex-col items-center">
                      <div className="w-2 h-2 bg-blue-500 rounded-full" />
                      {index < execution.timeline.length - 1 && (
                        <div className="w-0.5 h-full bg-gray-300 my-1" />
                      )}
                    </div>
                    <div className="flex-1 pb-4">
                      <p className="text-sm font-semibold">{event.event}</p>
                      <p className="text-xs text-gray-500">
                        {event.timestamp.toLocaleTimeString()}
                      </p>
                      {event.details && (
                        <p className="text-xs text-gray-600 mt-1">{event.details}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </ScrollArea>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
