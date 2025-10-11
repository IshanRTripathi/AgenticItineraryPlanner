/**
 * Change Preview Wrapper
 * Wraps edit operations with change preview functionality
 */

import React, { useState, useCallback } from 'react';
import { usePreviewSettings } from '../../contexts/PreviewSettingsContext';
import { ChangePreview } from '../ChangePreview';
import { DiffViewer, DiffSection } from '../diff/DiffViewer';
import { convertItineraryDiffToSections, createObjectDiff } from '../../utils/diffUtils';
import { apiClient } from '../../services/apiClient';
import { ItineraryDiff } from '../../types/ChatTypes';

interface ChangePreviewWrapperProps {
  itineraryId: string;
  onChangeApplied?: () => void;
  onChangeCancelled?: () => void;
  children: (props: {
    previewChange: (changeSet: any, diff?: ItineraryDiff) => void;
    isPreviewOpen: boolean;
  }) => React.ReactNode;
}

export function ChangePreviewWrapper({
  itineraryId,
  onChangeApplied,
  onChangeCancelled,
  children,
}: ChangePreviewWrapperProps) {
  const { settings } = usePreviewSettings();
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [pendingChange, setPendingChange] = useState<{
    changeSet: any;
    diff?: ItineraryDiff;
  } | null>(null);
  const [isApplying, setIsApplying] = useState(false);

  const previewChange = useCallback((changeSet: any, diff?: ItineraryDiff) => {
    setPendingChange({ changeSet, diff });
    setIsPreviewOpen(true);
  }, []);

  const handleApply = useCallback(async () => {
    if (!pendingChange) return;

    setIsApplying(true);
    try {
      await apiClient.applyChanges(itineraryId, pendingChange.changeSet);
      setIsPreviewOpen(false);
      setPendingChange(null);
      onChangeApplied?.();
    } catch (error) {
      console.error('Failed to apply changes:', error);
      // Error handling will be done by the parent component
    } finally {
      setIsApplying(false);
    }
  }, [itineraryId, pendingChange, onChangeApplied]);

  const handleCancel = useCallback(() => {
    setIsPreviewOpen(false);
    setPendingChange(null);
    onChangeCancelled?.();
  }, [onChangeCancelled]);

  // Convert diff to sections for advanced viewer
  const diffSections: DiffSection[] = pendingChange?.diff
    ? convertItineraryDiffToSections(pendingChange.diff)
    : [];

  return (
    <>
      {children({ previewChange, isPreviewOpen })}

      {isPreviewOpen && pendingChange && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
          <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col">
            {settings.useAdvancedDiff && diffSections.length > 0 ? (
              <DiffViewer
                sections={diffSections}
                viewMode={settings.defaultViewMode}
                showUnchanged={settings.showUnchanged}
                onClose={handleCancel}
              />
            ) : pendingChange.diff ? (
              <ChangePreview
                changeSet={pendingChange.changeSet}
                diff={pendingChange.diff}
                onApply={handleApply}
                onCancel={handleCancel}
              />
            ) : (
              <div className="p-6">
                <h3 className="text-lg font-semibold mb-4">No preview available</h3>
                <p className="text-gray-600 mb-4">
                  The change cannot be previewed. Would you like to apply it anyway?
                </p>
                <div className="flex justify-end gap-2">
                  <button
                    onClick={handleCancel}
                    className="px-4 py-2 border rounded-md hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleApply}
                    disabled={isApplying}
                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                  >
                    {isApplying ? 'Applying...' : 'Apply Anyway'}
                  </button>
                </div>
              </div>
            )}

            {(settings.useAdvancedDiff || !pendingChange.diff) && (
              <div className="border-t p-4 flex justify-end gap-2 bg-gray-50">
                <button
                  onClick={handleCancel}
                  disabled={isApplying}
                  className="px-4 py-2 border rounded-md hover:bg-gray-50 disabled:opacity-50"
                >
                  Cancel
                </button>
                <button
                  onClick={handleApply}
                  disabled={isApplying}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                >
                  {isApplying ? 'Applying...' : 'Apply Changes'}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
