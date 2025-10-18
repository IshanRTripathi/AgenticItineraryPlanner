/**
 * Lock State Debug Panel
 * Shows all lock states for debugging
 */

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Lock, Unlock, RefreshCw, Trash2 } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface LockStateDebugPanelProps {
  itineraryId: string;
  className?: string;
}

export function LockStateDebugPanel({ itineraryId, className = '' }: LockStateDebugPanelProps) {
  const [lockStates, setLockStates] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadLockStates = async () => {
    setLoading(true);
    setError(null);
    try {
      const states = await apiClient.getLockStates(itineraryId);
      setLockStates(states);
      console.log('Lock states loaded:', states);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load lock states');
      console.error('Failed to load lock states:', err);
    } finally {
      setLoading(false);
    }
  };

  const unlockAll = async () => {
    if (!window.confirm('Unlock all nodes? This cannot be undone.')) return;

    setLoading(true);
    try {
      const lockedNodes = Object.entries(lockStates)
        .filter(([_, isLocked]) => isLocked)
        .map(([nodeId]) => nodeId);

      for (const nodeId of lockedNodes) {
        await apiClient.toggleNodeLock(itineraryId, nodeId, false);
      }

      await loadLockStates();
      alert(`Unlocked ${lockedNodes.length} nodes`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to unlock all');
      console.error('Failed to unlock all:', err);
    } finally {
      setLoading(false);
    }
  };

  const toggleNode = async (nodeId: string, currentState: boolean) => {
    try {
      await apiClient.toggleNodeLock(itineraryId, nodeId, !currentState);
      await loadLockStates();
    } catch (err) {
      console.error('Failed to toggle node:', err);
      alert('Failed to toggle lock state');
    }
  };

  useEffect(() => {
    loadLockStates();
  }, [itineraryId]);

  const lockedCount = Object.values(lockStates).filter(Boolean).length;
  const totalCount = Object.keys(lockStates).length;

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Lock className="w-5 h-5" />
            <CardTitle>Lock State Debug</CardTitle>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant={lockedCount > 0 ? 'default' : 'secondary'}>
              {lockedCount}/{totalCount} locked
            </Badge>
            <Button size="sm" variant="outline" onClick={loadLockStates} disabled={loading}>
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </Button>
            <Button
              size="sm"
              variant="destructive"
              onClick={unlockAll}
              disabled={loading || lockedCount === 0}
            >
              <Trash2 className="w-4 h-4 mr-1" />
              Unlock All
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
            {error}
          </div>
        )}

        {loading && Object.keys(lockStates).length === 0 ? (
          <div className="text-center py-8 text-gray-500">Loading lock states...</div>
        ) : totalCount === 0 ? (
          <div className="text-center py-8 text-gray-500">No nodes found</div>
        ) : (
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {Object.entries(lockStates).map(([nodeId, isLocked]) => (
              <div
                key={nodeId}
                className="flex items-center justify-between p-3 border rounded-md hover:bg-gray-50"
              >
                <div className="flex items-center gap-3">
                  {isLocked ? (
                    <Lock className="w-4 h-4 text-red-500" />
                  ) : (
                    <Unlock className="w-4 h-4 text-gray-400" />
                  )}
                  <code className="text-sm font-mono">{nodeId}</code>
                </div>
                <Button
                  size="sm"
                  variant={isLocked ? 'default' : 'outline'}
                  onClick={() => toggleNode(nodeId, isLocked)}
                  className={isLocked ? 'bg-red-500 hover:bg-red-600' : ''}
                >
                  {isLocked ? 'Locked' : 'Unlocked'}
                </Button>
              </div>
            ))}
          </div>
        )}

        <div className="mt-4 p-3 bg-gray-50 rounded-md">
          <h4 className="text-sm font-semibold mb-2">Console Output</h4>
          <pre className="text-xs font-mono text-gray-600 overflow-x-auto">
            {JSON.stringify(lockStates, null, 2)}
          </pre>
        </div>
      </CardContent>
    </Card>
  );
}
