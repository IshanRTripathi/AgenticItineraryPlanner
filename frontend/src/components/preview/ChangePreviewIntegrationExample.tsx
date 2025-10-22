/**
 * Change Preview Integration Example
 * Demonstrates how to integrate change preview into edit flows
 */

import React, { useState } from 'react';
import { ChangePreviewWrapper } from './ChangePreviewWrapper';
import { useChangePreview } from '../../hooks/useChangePreview';
import { Button } from '../ui/button';

/**
 * Example 1: Using ChangePreviewWrapper (Recommended)
 * This is the easiest way to add change preview to any edit operation
 */
export function EditWithPreviewWrapper() {
  const itineraryId = 'example-id';

  const handleEdit = (previewChange: (changeSet: any, diff?: any) => void) => {
    // When user makes an edit, create a change set
    const changeSet = {
      ops: [
        {
          op: 'update',
          path: '/days/0/nodes/0/name',
          value: 'Updated Activity Name',
        },
      ],
    };

    // Optionally, you can fetch the diff first
    // For now, just pass the changeSet
    previewChange(changeSet);
  };

  return (
    <ChangePreviewWrapper
      itineraryId={itineraryId}
      onChangeApplied={() => 
}

/**
 * Example 2: Using useChangePreview Hook (More Control)
 * Use this when you need more control over the preview flow
 */
export function EditWithPreviewHook() {
  const itineraryId = 'example-id';
  const [showPreview, setShowPreview] = useState(false);
  const [previewData, setPreviewData] = useState<any>(null);

  const { proposeChanges, applyChanges, isLoading } = useChangePreview({
    itineraryId,
    onSuccess: () => {
      
      setShowPreview(false);
    },
    onError: (error) => {
      
    },
  });

  const handleEdit = async () => {
    const changeSet = {
      ops: [
        {
          op: 'update',
          path: '/days/0/nodes/0/name',
          value: 'Updated Activity Name',
        },
      ],
    };

    // Propose changes to get preview
    const preview = await proposeChanges(changeSet);
    if (preview) {
      setPreviewData(preview);
      setShowPreview(true);
    }
  };

  const handleApply = async () => {
    if (previewData) {
      await applyChanges(previewData.changeSet);
    }
  };

  return (
    <div>
      <Button onClick={handleEdit} disabled={isLoading}>
        Edit Activity
      </Button>

      {showPreview && previewData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
          <div className="bg-white p-6 rounded-lg">
            <h3>Preview Changes</h3>
            <pre>{JSON.stringify(previewData.diff, null, 2)}</pre>
            <div className="flex gap-2 mt-4">
              <Button onClick={() => setShowPreview(false)}>Cancel</Button>
              <Button onClick={handleApply} disabled={isLoading}>
                Apply
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Example 3: Integration Pattern for DayByDayView
 * Shows how to wrap node edit operations
 */
export function DayByDayViewIntegrationExample() {
  const itineraryId = 'example-id';

  return (
    <ChangePreviewWrapper
      itineraryId={itineraryId}
      onChangeApplied={() => {
        // Refresh the itinerary data
        
      }}
    >
      {({ previewChange }) => (
        <div className="day-by-day-view">
          {/* Your existing day-by-day view components */}
          <div className="node-card">
            <h4>Activity Name</h4>
            <Button
              onClick={() => {
                // When user clicks edit, create change set and preview
                const changeSet = {
                  ops: [
                    {
                      op: 'update',
                      path: '/days/0/nodes/0/timing/startTime',
                      value: '10:00',
                    },
                  ],
                };
                previewChange(changeSet);
              }}
            >
              Edit Time
            </Button>
          </div>
        </div>
      )}
    </ChangePreviewWrapper>
  );
}

/**
 * Example 4: Integration Pattern for WorkflowBuilder
 * Shows how to wrap workflow node updates
 */
export function WorkflowBuilderIntegrationExample() {
  const itineraryId = 'example-id';

  return (
    <ChangePreviewWrapper
      itineraryId={itineraryId}
      onChangeApplied={() => {
        // Sync workflow with itinerary
        
      }}
    >
      {({ previewChange }) => (
        <div className="workflow-builder">
          {/* Your existing workflow builder */}
          <div className="workflow-node">
            <h4>Node Title</h4>
            <Button
              onClick={() => {
                // When node is updated, create change set and preview
                const changeSet = {
                  ops: [
                    {
                      op: 'update',
                      path: '/days/0/nodes/0/position',
                      value: { x: 100, y: 200 },
                    },
                  ],
                };
                previewChange(changeSet);
              }}
            >
              Update Position
            </Button>
          </div>
        </div>
      )}
    </ChangePreviewWrapper>
  );
}

