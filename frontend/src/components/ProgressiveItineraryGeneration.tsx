/**
 * Progressive Itinerary Generation Component
 * Displays real-time progress of itinerary generation using SSE
 */

import React, { useState, useEffect, useCallback } from 'react';
import { useSseConnection } from '../hooks/useSseConnection';
import './ProgressiveItineraryGeneration.css';

interface ProgressiveItineraryGenerationProps {
  itineraryId: string;
  executionId?: string;
  onComplete?: (itineraryId: string) => void;
  onError?: (error: string) => void;
  className?: string;
}

interface GenerationProgress {
  progress: number;
  phase: string;
  message: string;
  agentType?: string;
  dayNumber?: number;
  nodeId?: string;
}

export const ProgressiveItineraryGeneration: React.FC<ProgressiveItineraryGenerationProps> = ({
  itineraryId,
  executionId,
  onComplete,
  onError,
  className = ''
}) => {
  const [progress, setProgress] = useState<GenerationProgress>({
    progress: 0,
    phase: 'initializing',
    message: 'Starting itinerary generation...'
  });
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Handle SSE events
  const handleAgentEvent = useCallback((event: any) => {
    
    
    if (event.status === 'failed') {
      const errorMessage = event.message || 'Generation failed';
      setError(errorMessage);
      onError?.(errorMessage);
      return;
    }

    if (event.status === 'completed' && event.step === 'complete') {
      setIsComplete(true);
      onComplete?.(itineraryId);
      return;
    }

    // Update progress based on event
    const newProgress: GenerationProgress = {
      progress: event.progress || 0,
      phase: event.step || 'processing',
      message: event.message || 'Processing...',
      agentType: event.agentKind,
      dayNumber: event.dayNumber,
      nodeId: event.nodeId
    };

    setProgress(newProgress);
  }, [itineraryId, onComplete, onError]);

  const handleChangeEvent = useCallback((event: any) => {
    
    
    if (event.type === 'agent_completed' && event.data?.status === 'completed') {
      setIsComplete(true);
      onComplete?.(itineraryId);
    }
  }, [itineraryId, onComplete]);

  // Connect to SSE
  const { isConnected, connect, disconnect } = useSseConnection(itineraryId, {
    executionId,
    onAgentEvent: handleAgentEvent,
    onChangeEvent: handleChangeEvent
  });

  // Connect on mount
  useEffect(() => {
    if (itineraryId) {
      connect(itineraryId);
    }

    return () => {
      disconnect();
    };
  }, [itineraryId, connect, disconnect]);

  // Get phase display info
  const getPhaseInfo = (phase: string) => {
    switch (phase) {
      case 'skeleton_generation':
        return {
          title: 'Creating Structure',
          description: 'Building the day-by-day framework',
          icon: '🏗️'
        };
      case 'population':
        return {
          title: 'Adding Details',
          description: 'Populating activities, meals, and transport',
          icon: '🎯'
        };
      case 'enrichment':
        return {
          title: 'Enriching Content',
          description: 'Adding detailed information and validation',
          icon: '✨'
        };
      case 'finalization':
        return {
          title: 'Finalizing',
          description: 'Completing the itinerary',
          icon: '🎉'
        };
      default:
        return {
          title: 'Processing',
          description: 'Working on your itinerary',
          icon: '⚙️'
        };
    }
  };

  const phaseInfo = getPhaseInfo(progress.phase);

  if (error) {
    return (
      <div className={`progressive-generation error ${className}`}>
        <div className="error-container">
          <div className="error-icon">❌</div>
          <h3>Generation Failed</h3>
          <p>{error}</p>
          <button 
            className="retry-button"
            onClick={() => {
              setError(null);
              setProgress({ progress: 0, phase: 'initializing', message: 'Restarting...' });
              connect(itineraryId);
            }}
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (isComplete) {
    return (
      <div className={`progressive-generation complete ${className}`}>
        <div className="complete-container">
          <div className="complete-icon">🎉</div>
          <h3>Itinerary Complete!</h3>
          <p>Your personalized itinerary is ready to explore.</p>
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: '100%' }}></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`progressive-generation ${className}`}>
      <div className="generation-container">
        <div className="header">
          <div className="phase-icon">{phaseInfo.icon}</div>
          <div className="phase-info">
            <h3>{phaseInfo.title}</h3>
            <p>{phaseInfo.description}</p>
          </div>
        </div>

        <div className="progress-section">
          <div className="progress-bar">
            <div 
              className="progress-fill" 
              style={{ width: `${progress.progress}%` }}
            ></div>
          </div>
          <div className="progress-text">
            <span className="progress-percentage">{progress.progress}%</span>
            <span className="progress-message">{progress.message}</span>
          </div>
        </div>

        <div className="details">
          {progress.agentType && (
            <div className="detail-item">
              <span className="detail-label">Agent:</span>
              <span className="detail-value">{progress.agentType}</span>
            </div>
          )}
          {progress.dayNumber && (
            <div className="detail-item">
              <span className="detail-label">Day:</span>
              <span className="detail-value">{progress.dayNumber}</span>
            </div>
          )}
          <div className="detail-item">
            <span className="detail-label">Status:</span>
            <span className={`detail-value connection-status ${isConnected ? 'connected' : 'disconnected'}`}>
              {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
            </span>
          </div>
        </div>

        <div className="loading-indicator">
          <div className="spinner"></div>
          <span>Generating your perfect itinerary...</span>
        </div>
      </div>
    </div>
  );
};

export default ProgressiveItineraryGeneration;



