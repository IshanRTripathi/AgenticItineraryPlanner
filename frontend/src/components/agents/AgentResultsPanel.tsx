/**
 * Agent Results Panel
 * Displays agent execution results with before/after comparison
 */

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { CheckCircle, XCircle, Clock, Zap } from 'lucide-react';
import { DiffViewer, DiffSection } from '../diff/DiffViewer';
import { createObjectDiff } from '../../utils/diffUtils';

interface AgentResult {
  agentId: string;
  agentName: string;
  status: 'success' | 'failed' | 'partial';
  changes: any[];
  metrics: {
    executionTime: number;
    tokensUsed?: number;
    nodesModified: number;
    nodesAdded: number;
    nodesRemoved: number;
  };
  before: any;
  after: any;
  timestamp: Date;
}

interface AgentResultsPanelProps {
  result: AgentResult;
  onApply: () => void;
  onReject: () => void;
  className?: string;
}

export function AgentResultsPanel({
  result,
  onApply,
  onReject,
  className = '',
}: AgentResultsPanelProps) {
  const [showDiff, setShowDiff] = useState(true);

  const getStatusConfig = () => {
    switch (result.status) {
      case 'success':
        return {
          icon: <CheckCircle className="w-5 h-5 text-green-500" />,
          badge: <Badge className="bg-green-100 text-green-700">Success</Badge>,
          color: 'border-green-200',
        };
      case 'failed':
        return {
          icon: <XCircle className="w-5 h-5 text-red-500" />,
          badge: <Badge className="bg-red-100 text-red-700">Failed</Badge>,
          color: 'border-red-200',
        };
      case 'partial':
        return {
          icon: <CheckCircle className="w-5 h-5 text-amber-500" />,
          badge: <Badge className="bg-amber-100 text-amber-700">Partial</Badge>,
          color: 'border-amber-200',
        };
    }
  };

  const config = getStatusConfig();
  const diffSections: DiffSection[] = result.before && result.after
    ? [createObjectDiff('Agent Changes', result.before, result.after)]
    : [];

  return (
    <Card className={`${className} ${config.color} border-2`}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {config.icon}
            <div>
              <CardTitle>{result.agentName} Results</CardTitle>
              <p className="text-sm text-gray-500 mt-1">
                Executed at {result.timestamp.toLocaleString()}
              </p>
            </div>
          </div>
          {config.badge}
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Metrics */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="p-3 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-2 mb-1">
              <Clock className="w-4 h-4 text-gray-500" />
              <span className="text-xs text-gray-600">Time</span>
            </div>
            <p className="text-lg font-semibold">{result.metrics.executionTime}ms</p>
          </div>

          {result.metrics.tokensUsed && (
            <div className="p-3 bg-gray-50 rounded-lg">
              <div className="flex items-center gap-2 mb-1">
                <Zap className="w-4 h-4 text-gray-500" />
                <span className="text-xs text-gray-600">Tokens</span>
              </div>
              <p className="text-lg font-semibold">{result.metrics.tokensUsed.toLocaleString()}</p>
            </div>
          )}

          <div className="p-3 bg-green-50 rounded-lg">
            <span className="text-xs text-gray-600">Added</span>
            <p className="text-lg font-semibold text-green-700">{result.metrics.nodesAdded}</p>
          </div>

          <div className="p-3 bg-blue-50 rounded-lg">
            <span className="text-xs text-gray-600">Modified</span>
            <p className="text-lg font-semibold text-blue-700">{result.metrics.nodesModified}</p>
          </div>
        </div>

        {/* Changes Summary */}
        <div className="p-4 bg-gray-50 rounded-lg">
          <h4 className="font-semibold mb-2">Changes Summary</h4>
          <p className="text-sm text-gray-600">
            {result.changes.length} changes made to the itinerary
          </p>
        </div>

        {/* Diff Viewer Toggle */}
        {diffSections.length > 0 && (
          <div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowDiff(!showDiff)}
              className="mb-2"
            >
              {showDiff ? 'Hide' : 'Show'} Detailed Changes
            </Button>

            {showDiff && (
              <div className="border rounded-lg overflow-hidden">
                <DiffViewer sections={diffSections} viewMode="side-by-side" showUnchanged={false} />
              </div>
            )}
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-2 pt-4 border-t">
          <Button variant="outline" onClick={onReject}>
            Reject Changes
          </Button>
          <Button onClick={onApply} className="bg-blue-600 hover:bg-blue-700">
            Apply Changes
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
