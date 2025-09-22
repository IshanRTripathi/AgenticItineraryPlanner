/**
 * Disambiguation panel for when multiple nodes match a request
 */

import React from 'react';
import { NodeCandidate } from '../types/ChatTypes';
import './DisambiguationPanel.css';

interface DisambiguationPanelProps {
  candidates: NodeCandidate[];
  onSelect: (candidate: NodeCandidate) => void;
  onCancel: () => void;
}

export const DisambiguationPanel: React.FC<DisambiguationPanelProps> = ({
  candidates,
  onSelect,
  onCancel,
}) => {
  const getTypeIcon = (type: string) => {
    const iconMap: Record<string, string> = {
      'attraction': '🏛️',
      'meal': '🍽️',
      'hotel': '🏨',
      'accommodation': '🏨',
      'transit': '🚗',
      'transport': '🚗',
      'activity': '🎯',
    };
    return iconMap[type] || '📍';
  };

  const getConfidenceColor = (confidence?: number) => {
    if (!confidence) return 'neutral';
    if (confidence >= 0.8) return 'high';
    if (confidence >= 0.6) return 'medium';
    return 'low';
  };

  return (
    <div className="disambiguation-panel">
      <div className="disambiguation-header">
        <h4>🤔 Which one did you mean?</h4>
        <p>I found multiple options. Please select the one you're referring to:</p>
      </div>
      
      <div className="candidates-list">
        {candidates.map((candidate, index) => (
          <div
            key={candidate.id}
            className={`candidate-item ${getConfidenceColor(candidate.confidence)}`}
            onClick={() => onSelect(candidate)}
          >
            <div className="candidate-icon">
              {getTypeIcon(candidate.type)}
            </div>
            
            <div className="candidate-info">
              <div className="candidate-title">
                {candidate.title}
              </div>
              <div className="candidate-details">
                <span className="candidate-type">{candidate.type}</span>
                <span className="candidate-day">Day {candidate.day}</span>
                {candidate.location && (
                  <span className="candidate-location">📍 {candidate.location}</span>
                )}
              </div>
            </div>
            
            {candidate.confidence && (
              <div className="confidence-indicator">
                <div className={`confidence-bar ${getConfidenceColor(candidate.confidence)}`}>
                  <div 
                    className="confidence-fill"
                    style={{ width: `${candidate.confidence * 100}%` }}
                  />
                </div>
                <span className="confidence-text">
                  {Math.round(candidate.confidence * 100)}%
                </span>
              </div>
            )}
            
            <div className="select-indicator">
              →
            </div>
          </div>
        ))}
      </div>
      
      <div className="disambiguation-actions">
        <button 
          className="cancel-button"
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </div>
  );
};
