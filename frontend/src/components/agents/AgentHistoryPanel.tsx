/**
 * Agent History Panel
 * Displays past agent executions with filtering
 */

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { History, CheckCircle, XCircle, Clock, ChevronRight } from 'lucide-react';

interface AgentExecution {
  id: string;
  agentId: string;
  agentName: string;
  status: 'success' | 'failed' | 'running';
  startTime: Date;
  endTime?: Date;
  duration?: number;
  changesCount: number;
}

interface AgentHistoryPanelProps {
  executions: AgentExecution[];
  onViewDetails: (executionId: string) => void;
  className?: string;
}

export function AgentHistoryPanel({
  executions,
  onViewDetails,
  className = '',
}: AgentHistoryPanelProps) {
  const [filterAgent, setFilterAgent] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const uniqueAgents = Array.from(new Set(executions.map(e => e.agentId)));

  const filteredExecutions = executions.filter(exec => {
    if (filterAgent !== 'all' && exec.agentId !== filterAgent) return false;
    if (filterStatus !== 'all' && exec.status !== filterStatus) return false;
    return true;
  });

  const totalPages = Math.ceil(filteredExecutions.length / itemsPerPage);
  const paginatedExecutions = filteredExecutions.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const getStatusConfig = (status: AgentExecution['status']) => {
    switch (status) {
      case 'success':
        return {
          icon: <CheckCircle className="w-4 h-4 text-green-500" />,
          badge: <Badge className="bg-green-100 text-green-700">Success</Badge>,
        };
      case 'failed':
        return {
          icon: <XCircle className="w-4 h-4 text-red-500" />,
          badge: <Badge className="bg-red-100 text-red-700">Failed</Badge>,
        };
      case 'running':
        return {
          icon: <Clock className="w-4 h-4 text-blue-500 animate-spin" />,
          badge: <Badge className="bg-blue-100 text-blue-700">Running</Badge>,
        };
    }
  };

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <History className="w-5 h-5" />
            <CardTitle>Agent Execution History</CardTitle>
          </div>
          <Badge variant="outline">{filteredExecutions.length} executions</Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Filters */}
        <div className="flex gap-4">
          <div className="flex-1">
            <Select value={filterAgent} onValueChange={setFilterAgent}>
              <SelectTrigger>
                <SelectValue placeholder="Filter by agent" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Agents</SelectItem>
                {uniqueAgents.map(agentId => (
                  <SelectItem key={agentId} value={agentId}>
                    {agentId}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex-1">
            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger>
                <SelectValue placeholder="Filter by status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="success">Success</SelectItem>
                <SelectItem value="failed">Failed</SelectItem>
                <SelectItem value="running">Running</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Execution List */}
        <div className="space-y-2">
          {paginatedExecutions.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              No executions found
            </div>
          ) : (
            paginatedExecutions.map(exec => {
              const config = getStatusConfig(exec.status);
              return (
                <div
                  key={exec.id}
                  className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50 cursor-pointer"
                  onClick={() => onViewDetails(exec.id)}
                >
                  <div className="flex items-center gap-3 flex-1">
                    {config.icon}
                    <div className="flex-1 min-w-0">
                      <h4 className="font-semibold text-sm">{exec.agentName}</h4>
                      <p className="text-xs text-gray-500">
                        {exec.startTime.toLocaleString()}
                        {exec.duration && ` • ${exec.duration}ms`}
                        {exec.changesCount > 0 && ` • ${exec.changesCount} changes`}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {config.badge}
                    <ChevronRight className="w-4 h-4 text-gray-400" />
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-4 border-t">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              disabled={currentPage === 1}
            >
              Previous
            </Button>
            <span className="text-sm text-gray-600">
              Page {currentPage} of {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
            >
              Next
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
