import React from 'react';
import './ConfidenceBar.css';

interface ConfidenceBarProps {
  value: number; // 0-1
  showLabel?: boolean;
}

export const ConfidenceBar: React.FC<ConfidenceBarProps> = ({ value, showLabel = true }) => {
  const percentage = Math.round(value * 100);
  
  const getColor = () => {
    if (value > 0.7) return 'green';
    if (value > 0.4) return 'yellow';
    return 'red';
  };
  
  return (
    <div className="confidence-bar-container">
      <div className="confidence-bar">
        <div
          className={`confidence-fill confidence-${getColor()}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
      {showLabel && (
        <span className="confidence-text">{percentage}% confidence</span>
      )}
    </div>
  );
};
