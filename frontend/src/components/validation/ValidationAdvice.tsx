import React, { useState } from 'react';
import { ValidationAdvice as ValidationAdviceType, getSeverityColor, getScoreColor } from '../../services/validationApi';
import { ValidationIssueCard } from './ValidationIssueCard';
import './ValidationAdvice.css';

interface ValidationAdviceProps {
  advice: ValidationAdviceType;
  onDismiss?: (issueId: string, reason: string) => void;
  onApplyFix?: (issueId: string) => void;
}

export const ValidationAdvice: React.FC<ValidationAdviceProps> = ({
  advice,
  onDismiss,
  onApplyFix
}) => {
  const [isExpanded, setIsExpanded] = useState(true);
  
  if (!advice || !advice.success) {
    return null;
  }
  
  const { summary, issuesByCategory, recommendations, metadata } = advice;
  const totalIssues = summary.totalErrors + summary.totalWarnings;
  
  if (totalIssues === 0 && recommendations.length === 0) {
    return (
      <div className="validation-advice success">
        <div className="advice-header">
          <span className="success-icon">✅</span>
          <h3>All Good!</h3>
        </div>
        <p>Your itinerary looks great. No issues found.</p>
      </div>
    );
  }
  
  return (
    <div className="validation-advice">
      <div className="advice-header" onClick={() => setIsExpanded(!isExpanded)}>
        <div className="header-left">
          <span className="advice-icon">💡</span>
          <h3>Suggestions</h3>
          <span className="issue-count">
            {totalIssues} issue{totalIssues !== 1 ? 's' : ''}
          </span>
        </div>
        <div className="header-right">
          <div className="score-badge" style={{ backgroundColor: getScoreColor(summary.score) }}>
            Score: {Math.round(summary.score)}
          </div>
          <button className="expand-button">
            {isExpanded ? '▼' : '▶'}
          </button>
        </div>
      </div>
      
      {isExpanded && (
        <div className="advice-content">
          {/* Summary */}
          <div className="advice-summary">
            {summary.totalErrors > 0 && (
              <div className="summary-item error">
                <span className="summary-icon">❌</span>
                <span>{summary.totalErrors} error{summary.totalErrors !== 1 ? 's' : ''}</span>
              </div>
            )}
            {summary.totalWarnings > 0 && (
              <div className="summary-item warning">
                <span className="summary-icon">⚠️</span>
                <span>{summary.totalWarnings} warning{summary.totalWarnings !== 1 ? 's' : ''}</span>
              </div>
            )}
            {summary.filteredIssues > 0 && (
              <div className="summary-item filtered">
                <span className="summary-icon">🔇</span>
                <span>{summary.filteredIssues} filtered (previously dismissed)</span>
              </div>
            )}
          </div>
          
          {/* Issues by Category */}
          {Object.entries(issuesByCategory).map(([category, issues]) => (
            <div key={category} className="category-section">
              <h4 className="category-title">{category}</h4>
              {issues.map(issue => (
                <ValidationIssueCard
                  key={issue.id}
                  issue={issue}
                  onDismiss={onDismiss}
                  onApplyFix={onApplyFix}
                />
              ))}
            </div>
          ))}
          
          {/* Recommendations */}
          {recommendations.length > 0 && (
            <div className="recommendations-section">
              <h4>Recommendations</h4>
              {recommendations.map((rec, index) => (
                <div key={index} className={`recommendation priority-${rec.priority.toLowerCase()}`}>
                  <div className="rec-header">
                    <span className="rec-priority">{rec.priority}</span>
                    <span className="rec-category">{rec.category}</span>
                  </div>
                  <p className="rec-action">{rec.action}</p>
                  <p className="rec-impact">{rec.impact}</p>
                </div>
              ))}
            </div>
          )}
          
          {/* Metadata */}
          {metadata && (
            <div className="advice-metadata">
              <span>Validated {new Date(metadata.validatedAt).toLocaleString()}</span>
              <span>•</span>
              <span>Took {metadata.validationDuration}ms</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
