import React, { useState } from 'react';
import { ValidationIssue, getSeverityColor } from '../../services/validationApi';
import './ValidationIssueCard.css';

interface ValidationIssueCardProps {
  issue: ValidationIssue;
  onDismiss?: (issueId: string, reason: string) => void;
  onApplyFix?: (issueId: string) => void;
}

export const ValidationIssueCard: React.FC<ValidationIssueCardProps> = ({
  issue,
  onDismiss,
  onApplyFix
}) => {
  const [showDismissDialog, setShowDismissDialog] = useState(false);
  const [dismissReason, setDismissReason] = useState('');
  
  const handleDismiss = () => {
    if (onDismiss) {
      onDismiss(issue.id, dismissReason || 'User dismissed');
      setShowDismissDialog(false);
      setDismissReason('');
    }
  };
  
  return (
    <div className={`validation-issue-card severity-${issue.severity.toLowerCase()}`}>
      <div className="issue-header">
        <span 
          className="severity-badge"
          style={{ backgroundColor: getSeverityColor(issue.severity) }}
        >
          {issue.severity}
        </span>
        {issue.dayNumber && (
          <span className="day-badge">Day {issue.dayNumber}</span>
        )}
      </div>
      
      <p className="issue-message">{issue.message}</p>
      
      {issue.recommendation && (
        <div className="issue-recommendation">
          <span className="rec-icon">💡</span>
          <span>{issue.recommendation}</span>
        </div>
      )}
      
      <div className="issue-actions">
        {onApplyFix && (
          <button 
            className="action-button fix-button"
            onClick={() => onApplyFix(issue.id)}
          >
            Apply Fix
          </button>
        )}
        {onDismiss && (
          <button 
            className="action-button dismiss-button"
            onClick={() => setShowDismissDialog(true)}
          >
            Dismiss
          </button>
        )}
      </div>
      
      {showDismissDialog && (
        <div className="dismiss-dialog">
          <p>Why are you dismissing this issue?</p>
          <input
            type="text"
            placeholder="Optional reason..."
            value={dismissReason}
            onChange={(e) => setDismissReason(e.target.value)}
          />
          <div className="dialog-actions">
            <button onClick={handleDismiss}>Confirm</button>
            <button onClick={() => setShowDismissDialog(false)}>Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
};
