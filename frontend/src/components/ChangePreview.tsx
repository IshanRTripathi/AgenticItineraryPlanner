/**
 * Change preview component for showing proposed changes before applying
 */

import React from 'react';
import { ItineraryDiff, DiffItem } from '../types/ChatTypes';
import './ChangePreview.css';

interface ChangePreviewProps {
  changeSet: any;
  diff: ItineraryDiff;
  onApply: () => void;
  onCancel: () => void;
}

export const ChangePreview: React.FC<ChangePreviewProps> = ({
  changeSet,
  diff,
  onApply,
  onCancel,
}) => {
  const renderDiffItem = (item: DiffItem, type: 'added' | 'removed' | 'updated') => {
    const typeConfig = {
      added: { icon: '➕', color: 'green', label: 'Added' },
      removed: { icon: '➖', color: 'red', label: 'Removed' },
      updated: { icon: '🔄', color: 'blue', label: 'Updated' },
    };

    const config = typeConfig[type];

    return (
      <div key={`${item.nodeId}-${type}`} className={`diff-item diff-${type}`}>
        <div className="diff-item-header">
          <span className={`diff-icon diff-${type}`}>
            {config.icon}
          </span>
          <span className="diff-label">{config.label}</span>
          <span className="diff-node-id">{item.nodeId}</span>
          <span className="diff-day">Day {item.day}</span>
        </div>
        
        {item.fields && item.fields.length > 0 && (
          <div className="diff-fields">
            <span className="diff-fields-label">Fields:</span>
            <span className="diff-fields-list">
              {item.fields.join(', ')}
            </span>
          </div>
        )}
      </div>
    );
  };

  const getTotalChanges = () => {
    return diff.added.length + diff.removed.length + diff.updated.length;
  };

  const getChangeSummary = () => {
    const changes = [];
    if (diff.added.length > 0) changes.push(`${diff.added.length} added`);
    if (diff.removed.length > 0) changes.push(`${diff.removed.length} removed`);
    if (diff.updated.length > 0) changes.push(`${diff.updated.length} updated`);
    return changes.join(', ');
  };

  return (
    <div className="change-preview">
      <div className="preview-header">
        <h4>📋 Proposed Changes</h4>
        <div className="preview-summary">
          <span className="change-count">{getTotalChanges()} changes</span>
          <span className="change-details">({getChangeSummary()})</span>
        </div>
      </div>

      <div className="preview-content">
        {diff.added.length > 0 && (
          <div className="diff-section">
            <h5 className="diff-section-title">Added Items</h5>
            <div className="diff-items">
              {diff.added.map(item => renderDiffItem(item, 'added'))}
            </div>
          </div>
        )}

        {diff.removed.length > 0 && (
          <div className="diff-section">
            <h5 className="diff-section-title">Removed Items</h5>
            <div className="diff-items">
              {diff.removed.map(item => renderDiffItem(item, 'removed'))}
            </div>
          </div>
        )}

        {diff.updated.length > 0 && (
          <div className="diff-section">
            <h5 className="diff-section-title">Updated Items</h5>
            <div className="diff-items">
              {diff.updated.map(item => renderDiffItem(item, 'updated'))}
            </div>
          </div>
        )}

        {getTotalChanges() === 0 && (
          <div className="no-changes">
            <span className="no-changes-icon">✅</span>
            <span className="no-changes-text">No changes to apply</span>
          </div>
        )}
      </div>

      <div className="preview-actions">
        <button 
          className="cancel-button"
          onClick={onCancel}
        >
          Cancel
        </button>
        <button 
          className="apply-button"
          onClick={onApply}
          disabled={getTotalChanges() === 0}
        >
          Apply Changes
        </button>
      </div>
    </div>
  );
};
