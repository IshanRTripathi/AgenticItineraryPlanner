/**
 * Enhanced itinerary viewer with integrated chat interface
 * Combines the normalized itinerary viewer with chat functionality
 */

import React, { useState } from 'react';
import { useItinerary } from '../state/query/hooks';
import { TripData } from '../types/TripData';
import { NewChat } from './chat/NewChat';
import { NormalizedItineraryViewer } from './NormalizedItineraryViewer';
import './ItineraryWithChat.css';

interface ItineraryWithChatProps {
  itineraryId: string;
  className?: string;
}

export const ItineraryWithChat: React.FC<ItineraryWithChatProps> = ({
  itineraryId,
  className = '',
}) => {
  const { data: tripData, isLoading: loading, error: queryError, refetch } = useItinerary(itineraryId);
  const typedTripData = tripData as TripData | undefined;
  const [selectedDay, setSelectedDay] = useState<number>(1);
  const [selectedNodeId, setSelectedNodeId] = useState<string | undefined>();
  const [chatScope, setChatScope] = useState<'trip' | 'day'>('trip');
  const [activeTab, setActiveTab] = useState<'itinerary' | 'chat'>('itinerary');

  const error = queryError ? (queryError instanceof Error ? queryError.message : 'Failed to load itinerary') : null;

  const handleItineraryUpdate = async (updatedItineraryId: string) => {
    if (updatedItineraryId === itineraryId) {
      await refetch();
    }
  };

  const handleNodeSelect = (nodeId: string, day: number) => {
    setSelectedNodeId(nodeId);
    setSelectedDay(day);
    setChatScope('day');
    setActiveTab('chat');
  };

  const handleDaySelect = (day: number) => {
    setSelectedDay(day);
    setSelectedNodeId(undefined);
    setChatScope('day');
  };

  const handleTripScopeSelect = () => {
    setSelectedNodeId(undefined);
    setSelectedDay(undefined);
    setChatScope('trip');
  };

  if (loading) {
    return (
      <div className={`itinerary-with-chat ${className}`}>
        <div className="loading-container">
          <div className="loading-spinner">⏳</div>
          <p>Loading itinerary...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`itinerary-with-chat ${className}`}>
        <div className="error-container">
          <div className="error-icon">⚠️</div>
          <h3>Error Loading Itinerary</h3>
          <p>{error}</p>
          <button
            className="retry-button"
            onClick={() => refetch()}
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (!typedTripData) {
    return (
      <div className={`itinerary-with-chat ${className}`}>
        <div className="empty-container">
          <div className="empty-icon">📋</div>
          <h3>No Itinerary Found</h3>
          <p>The requested itinerary could not be found.</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`itinerary-with-chat ${className}`}>
      {/* Header with tabs */}
      <div className="itinerary-header">
        <div className="header-info">
          <h2 className="itinerary-title">{typedTripData.destination || typedTripData.endLocation?.name || 'Trip'}</h2>
          <div className="itinerary-meta">
            <span className="meta-item">
              📅 {typedTripData.dates?.start} - {typedTripData.dates?.end}
            </span>
            <span className="meta-item">
              📍 {typedTripData.itinerary?.days?.length || 0} days
            </span>
            <span className="meta-item">
              💰 {typedTripData.budget?.currency || 'USD'}
            </span>
          </div>
        </div>

        <div className="tab-navigation">
          <button
            className={`tab-button ${activeTab === 'itinerary' ? 'active' : ''}`}
            onClick={() => setActiveTab('itinerary')}
          >
            📋 Itinerary
          </button>
          <button
            className={`tab-button ${activeTab === 'chat' ? 'active' : ''}`}
            onClick={() => setActiveTab('chat')}
          >
            💬 Chat
          </button>
        </div>
      </div>

      {/* Context selector */}
      <div className="context-selector">
        <div className="context-info">
          <span className="context-label">Chat Context:</span>
          <div className="context-badges">
            <button
              className={`context-badge ${chatScope === 'trip' ? 'active' : ''}`}
              onClick={handleTripScopeSelect}
            >
              🌍 Entire Trip
            </button>
            {selectedDay && (
              <button
                className={`context-badge ${chatScope === 'day' ? 'active' : ''}`}
                onClick={() => setChatScope('day')}
              >
                📅 Day {selectedDay}
              </button>
            )}
            {selectedNodeId && (
              <span className="context-badge selected">
                🎯 Node Selected
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="main-content">
        {activeTab === 'itinerary' && (
          <div className="itinerary-tab">
            <NormalizedItineraryViewer
              itineraryId={itineraryId}
              tripData={typedTripData}
              onNodeSelect={handleNodeSelect}
              onDaySelect={handleDaySelect}
            />
          </div>
        )}

        {activeTab === 'chat' && (
          <div className="chat-tab">
            <NewChat />
          </div>
        )}
      </div>

      {/* Quick actions */}
      <div className="quick-actions">
        <button
          className="quick-action-button"
          onClick={() => setActiveTab('chat')}
          title="Open chat"
        >
          💬 Chat
        </button>
        <button
          className="quick-action-button"
          onClick={() => refetch()}
          title="Refresh itinerary"
        >
          🔄 Refresh
        </button>
        <button
          className="quick-action-button"
          onClick={() => {
            setSelectedNodeId(undefined);
            setSelectedDay(undefined);
            setChatScope('trip');
          }}
          title="Clear selection"
        >
          🎯 Clear
        </button>
      </div>
    </div>
  );
};
