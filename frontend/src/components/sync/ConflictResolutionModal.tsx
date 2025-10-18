/**
 * Conflict Resolution Modal
 * Handles conflicts between workflow and itinerary
 */

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { AlertTriangle, GitMerge, ArrowLeft, ArrowRight, X } from 'lucide-react';
import { DiffViewer, DiffSection } from '../diff/DiffViewer';
import { createObjectDiff } from '../../utils/diffUtils';

interface ConflictData {
  nodeId: string;
  workflowVersion: any;
  itineraryVersion: any;
  conflictType: 'modified' | 'deleted' | 'added';
  timestamp: Date;
}

interface ConflictResolutionModalProps {
  conflicts: ConflictData[];
  isOpen: boolean;
  onClose: () => void;
  onResolve: (resolution: ConflictResolution[]) => void;
}

export interface ConflictResolution {
  nodeId: string;
  action: 'keep-workflow' | 'keep-itinerary' | 'merge';
  mergedData?: any;
}

export function ConflictResolutionModal({
  conflicts,
  isOpen,
  onClose,
  onResolve,
}: ConflictResolutionModalProps) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [resolutions, setResolutions] = useState<Map<string, ConflictResolution>>(new Map());

  if (!isOpen || conflicts.length === 0) return null;

  const currentConflict = conflicts[currentIndex];
  const hasResolution = resolutions.has(currentConflict.nodeId);
  const currentResolution = resolutions.get(currentConflict.nodeId);

  const handleResolve = (action: ConflictResolution['action'], mergedData?: any) => {
    const newResolutions = new Map(resolutions);
    newResolutions.set(currentConflict.nodeId, {
      nodeId: currentConflict.nodeId,
      action,
      mergedData,
    });
    setResolutions(newResolutions);

    // Move to next conflict if available
    if (currentIndex < conflicts.length - 1) {
      setCurrentIndex(currentIndex + 1);
    }
  };

  const handleApplyAll = () => {
    const resolutionArray = Array.from(resolutions.values());
    if (resolutionArray.length < conflicts.length) {
      if (!window.confirm(`Only ${resolutionArray.length}/${conflicts.length} conflicts resolved. Continue anyway?`)) {
        return;
      }
    }
    onResolve(resolutionArray);
  };

  const diffSections: DiffSection[] = [
    createObjectDiff('Workflow vs Itinerary', currentConflict.workflowVersion, currentConflict.itineraryVersion),
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <Card className="w-full max-w-6xl max-h-[90vh] overflow-hidden flex flex-col">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <AlertTriangle className="w-6 h-6 text-amber-500" />
              <div>
                <CardTitle>Resolve Sync Conflicts</CardTitle>
                <p className="text-sm text-gray-500 mt-1">
                  Conflict {currentIndex + 1} of {conflicts.length}
                </p>
              </div>
            </div>
            <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-full">
              <X className="w-5 h-5" />
            </button>
          </div>
        </CardHeader>

        <CardContent className="flex-1 overflow-y-auto">
          {/* Conflict Info */}
          <div className="mb-4 p-4 bg-amber-50 border border-amber-200 rounded-lg">
            <div className="flex items-center justify-between mb-2">
              <div>
                <h4 className="font-semibold">Node: {currentConflict.nodeId}</h4>
                <p className="text-sm text-gray-600">
                  Conflict detected at {currentConflict.timestamp.toLocaleString()}
                </p>
              </div>
              <Badge variant="outline" className="bg-amber-100">
                {currentConflict.conflictType}
              </Badge>
            </div>
            {hasResolution && (
              <div className="mt-2 p-2 bg-green-50 border border-green-200 rounded">
                <p className="text-sm text-green-700">
                  ✓ Resolved: {currentResolution?.action.replace('-', ' ')}
                </p>
              </div>
            )}
          </div>

          {/* Diff Viewer */}
          <div className="mb-4">
            <DiffViewer sections={diffSections} viewMode="side-by-side" showUnchanged={false} />
          </div>

          {/* Resolution Options */}
          <div className="grid grid-cols-3 gap-4">
            <Card
              className={`cursor-pointer transition-all ${
                currentResolution?.action === 'keep-workflow'
                  ? 'border-blue-500 border-2 bg-blue-50'
                  : 'hover:border-gray-400'
              }`}
              onClick={() => handleResolve('keep-workflow')}
            >
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <ArrowLeft className="w-5 h-5 text-blue-600" />
                  <h4 className="font-semibold">Keep Workflow</h4>
                </div>
                <p className="text-sm text-gray-600">
                  Use the workflow version and discard itinerary changes
                </p>
              </CardContent>
            </Card>

            <Card
              className={`cursor-pointer transition-all ${
                currentResolution?.action === 'keep-itinerary'
                  ? 'border-green-500 border-2 bg-green-50'
                  : 'hover:border-gray-400'
              }`}
              onClick={() => handleResolve('keep-itinerary')}
            >
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <ArrowRight className="w-5 h-5 text-green-600" />
                  <h4 className="font-semibold">Keep Itinerary</h4>
                </div>
                <p className="text-sm text-gray-600">
                  Use the itinerary version and discard workflow changes
                </p>
              </CardContent>
            </Card>

            <Card
              className={`cursor-pointer transition-all ${
                currentResolution?.action === 'merge'
                  ? 'border-purple-500 border-2 bg-purple-50'
                  : 'hover:border-gray-400'
              }`}
              onClick={() => {
                // Simple merge: combine both versions
                const merged = {
                  ...currentConflict.itineraryVersion,
                  ...currentConflict.workflowVersion,
                };
                handleResolve('merge', merged);
              }}
            >
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <GitMerge className="w-5 h-5 text-purple-600" />
                  <h4 className="font-semibold">Merge Both</h4>
                </div>
                <p className="text-sm text-gray-600">
                  Combine changes from both versions (workflow takes precedence)
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Navigation */}
          <div className="mt-6 flex items-center justify-between">
            <Button
              variant="outline"
              onClick={() => setCurrentIndex(Math.max(0, currentIndex - 1))}
              disabled={currentIndex === 0}
            >
              Previous
            </Button>

            <div className="text-sm text-gray-600">
              {resolutions.size} of {conflicts.length} resolved
            </div>

            <Button
              variant="outline"
              onClick={() => setCurrentIndex(Math.min(conflicts.length - 1, currentIndex + 1))}
              disabled={currentIndex === conflicts.length - 1}
            >
              Next
            </Button>
          </div>
        </CardContent>

        {/* Actions */}
        <div className="border-t p-4 flex justify-end gap-2 bg-gray-50">
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={handleApplyAll}
            disabled={resolutions.size === 0}
            className="bg-blue-600 hover:bg-blue-700"
          >
            Apply Resolutions ({resolutions.size})
          </Button>
        </div>
      </Card>
    </div>
  );
}
