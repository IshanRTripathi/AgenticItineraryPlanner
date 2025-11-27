import React, { useState } from 'react';
import { Memory, calculateConfidenceWithDecay, formatMemoryLabel, formatMemoryValue, formatRelativeTime } from '../../services/memoryApi';
import { ConfidenceBar } from './ConfidenceBar';
import './PreferenceItem.css';

interface PreferenceItemProps {
  memory: Memory;
  onUpdate: (memoryId: string, updates: Partial<Memory>) => void;
  onDelete: (memoryId: string) => void;
}

export const PreferenceItem: React.FC<PreferenceItemProps> = ({ memory, onUpdate, onDelete }) => {
  const [isEditing, setIsEditing] = useState(false);
  
  const currentConfidence = calculateConfidenceWithDecay(memory);
  const hasRecentChange = memory.lastUpdated > Date.now() - 7 * 24 * 60 * 60 * 1000;
  
  const handleDelete = () => {
    if (window.confirm('Are you sure you want to remove this preference?')) {
      onDelete(memory.id);
    }
  };
  
  return (
    <div className="preference-item">
      <div className="preference-content">
        {/* Preference Label */}
        <div className="preference-label">
          {formatMemoryLabel(memory)}
          {hasRecentChange && (
            <span className="change-badge">Recently updated</span>
          )}
          {memory.isPersonal && (
            <span className="private-badge">🔒 Private</span>
          )}
        </div>
        
        {/* Preference Value */}
        <div className="preference-value">
          {formatMemoryValue(memory)}
        </div>
        
        {/* Confidence Indicator */}
        <div className="confidence-indicator">
          <ConfidenceBar value={currentConfidence} />
        </div>
        
        {/* Source & Last Updated */}
        <div className="preference-meta">
          <span className="source">
            {memory.source === 'LEARNED' ? '📚 Learned from trips' : '✍️ You told us'}
          </span>
          <span className="updated">
            Updated {formatRelativeTime(memory.lastUpdated)}
          </span>
        </div>
      </div>
      
      {/* Actions */}
      <div className="preference-actions">
        <button 
          className="action-button edit-button"
          onClick={() => setIsEditing(true)}
          title="Edit preference"
        >
          ✏️
        </button>
        <button 
          className="action-button delete-button"
          onClick={handleDelete}
          title="Remove preference"
        >
          🗑️
        </button>
      </div>
    </div>
  );
};
