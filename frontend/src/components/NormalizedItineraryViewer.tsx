import React, { useState, useEffect } from 'react';
import { apiClient } from '../services/apiClient';
import { NormalizedDataTransformer } from '../services/normalizedDataTransformer';
import { NormalizedItinerary, ChangeSet } from '../types/NormalizedItinerary';
import { TripData } from '../types/TripData';

interface NormalizedItineraryViewerProps {
  itineraryId: string;
}

export const NormalizedItineraryViewer: React.FC<NormalizedItineraryViewerProps> = ({ itineraryId }) => {
  const [normalizedItinerary, setNormalizedItinerary] = useState<NormalizedItinerary | null>(null);
  const [tripData, setTripData] = useState<TripData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedDay, setSelectedDay] = useState<number>(1);

  useEffect(() => {
    loadItinerary();
  }, [itineraryId]);

  const loadItinerary = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const normalized = await apiClient.getItineraryJson(itineraryId);
      setNormalizedItinerary(normalized);
      
      const transformed = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalized);
      setTripData(transformed);
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load itinerary');
    } finally {
      setLoading(false);
    }
  };

  const handleProposeChange = async () => {
    if (!normalizedItinerary) return;
    
    try {
      const changeSet: ChangeSet = {
        scope: 'day',
        day: selectedDay,
        ops: [
          {
            op: 'move',
            id: normalizedItinerary.days[selectedDay - 1]?.nodes[0]?.id,
            startTime: new Date(Date.now() + 3600000).toISOString() // 1 hour later
          }
        ],
        preferences: {
          userFirst: true,
          autoApply: false,
          respectLocks: true
        }
      };
      
      const result = await apiClient.proposeChanges(itineraryId, changeSet);
      console.log('Propose result:', result);
      alert('Changes proposed successfully! Check console for details.');
      
    } catch (err) {
      console.error('Failed to propose changes:', err);
      alert('Failed to propose changes: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handleApplyChange = async () => {
    if (!normalizedItinerary) return;
    
    try {
      const changeSet: ChangeSet = {
        scope: 'day',
        day: selectedDay,
        ops: [
          {
            op: 'move',
            id: normalizedItinerary.days[selectedDay - 1]?.nodes[0]?.id,
            startTime: new Date(Date.now() + 3600000).toISOString() // 1 hour later
          }
        ],
        preferences: {
          userFirst: true,
          autoApply: false,
          respectLocks: true
        }
      };
      
      const result = await apiClient.applyChanges(itineraryId, {
        changeSet: changeSet
      });
      console.log('Apply result:', result);
      alert('Changes applied successfully! Check console for details.');
      
      // Reload itinerary to see changes
      await loadItinerary();
      
    } catch (err) {
      console.error('Failed to apply changes:', err);
      alert('Failed to apply changes: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handleUndoChange = async () => {
    if (!normalizedItinerary) return;
    
    try {
      const result = await apiClient.undoChanges(itineraryId, {
        toVersion: normalizedItinerary.version - 1
      });
      console.log('Undo result:', result);
      alert('Changes undone successfully! Check console for details.');
      
      // Reload itinerary to see changes
      await loadItinerary();
      
    } catch (err) {
      console.error('Failed to undo changes:', err);
      alert('Failed to undo changes: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handleMockBooking = async () => {
    if (!normalizedItinerary) return;
    
    const day = normalizedItinerary.days[selectedDay - 1];
    if (!day || day.nodes.length === 0) {
      alert('No nodes available for booking');
      return;
    }
    
    try {
      const result = await apiClient.mockBook({
        itineraryId: itineraryId,
        nodeId: day.nodes[0].id
      });
      console.log('Booking result:', result);
      alert(`Booking completed! Reference: ${result.bookingRef}`);
      
      // Reload itinerary to see changes
      await loadItinerary();
      
    } catch (err) {
      console.error('Failed to book:', err);
      alert('Failed to book: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  if (loading) {
    return (
      <div className="p-4">
        <div className="text-center">Loading itinerary...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4">
        <div className="text-red-600">Error: {error}</div>
        <button 
          onClick={loadItinerary}
          className="mt-2 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          Retry
        </button>
      </div>
    );
  }

  if (!normalizedItinerary || !tripData) {
    return (
      <div className="p-4">
        <div className="text-gray-600">No itinerary data available</div>
      </div>
    );
  }

  const currentDay = normalizedItinerary.days[selectedDay - 1];

  return (
    <div className="p-4 max-w-6xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold mb-2">{normalizedItinerary.summary}</h1>
        <div className="text-sm text-gray-600 mb-4">
          Version: {normalizedItinerary.version} | Currency: {normalizedItinerary.currency} | 
          Themes: {normalizedItinerary.themes.join(', ')}
        </div>
        
        {/* Action Buttons */}
        <div className="flex gap-2 mb-4">
          <button 
            onClick={handleProposeChange}
            className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
          >
            Propose Change
          </button>
          <button 
            onClick={handleApplyChange}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            Apply Change
          </button>
          <button 
            onClick={handleUndoChange}
            className="px-4 py-2 bg-orange-500 text-white rounded hover:bg-orange-600"
          >
            Undo Change
          </button>
          <button 
            onClick={handleMockBooking}
            className="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600"
          >
            Mock Booking
          </button>
        </div>
      </div>

      {/* Day Selector */}
      <div className="mb-4">
        <label className="block text-sm font-medium mb-2">Select Day:</label>
        <select 
          value={selectedDay} 
          onChange={(e) => setSelectedDay(parseInt(e.target.value))}
          className="border rounded px-3 py-2"
        >
          {normalizedItinerary.days.map((day, index) => (
            <option key={day.dayNumber} value={day.dayNumber}>
              Day {day.dayNumber} - {day.date}
            </option>
          ))}
        </select>
      </div>

      {/* Day Details */}
      {currentDay && (
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4">
            Day {currentDay.dayNumber} - {currentDay.location}
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="bg-gray-50 p-3 rounded">
              <div className="text-sm text-gray-600">Total Cost</div>
              <div className="font-semibold">{currentDay.totals?.cost || 0} {normalizedItinerary.currency}</div>
            </div>
            <div className="bg-gray-50 p-3 rounded">
              <div className="text-sm text-gray-600">Distance</div>
              <div className="font-semibold">{currentDay.totals?.distanceKm || 0} km</div>
            </div>
            <div className="bg-gray-50 p-3 rounded">
              <div className="text-sm text-gray-600">Duration</div>
              <div className="font-semibold">{currentDay.totals?.durationHr || 0} hours</div>
            </div>
          </div>

          {/* Nodes */}
          <div className="mb-6">
            <h3 className="text-lg font-medium mb-3">Activities</h3>
            <div className="space-y-3">
              {currentDay.nodes.map((node) => (
                <div key={node.id} className="border rounded p-4">
                  <div className="flex justify-between items-start mb-2">
                    <h4 className="font-medium">{node.title}</h4>
                    <div className="flex gap-2">
                      <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded">
                        {node.type}
                      </span>
                      {node.locked && (
                        <span className="px-2 py-1 bg-red-100 text-red-800 text-xs rounded">
                          Locked
                        </span>
                      )}
                      {node.bookingRef && (
                        <span className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded">
                          Booked: {node.bookingRef}
                        </span>
                      )}
                    </div>
                  </div>
                  
                  <div className="text-sm text-gray-600 mb-2">
                    {node.timing && (
                      <div>
                        Time: {new Date(node.timing.startTime).toLocaleTimeString()} - 
                        {new Date(node.timing.endTime).toLocaleTimeString()} 
                        ({node.timing.durationMin} min)
                      </div>
                    )}
                    {node.cost && (
                      <div>Cost: {node.cost.amount} {node.cost.currency} per {node.cost.per}</div>
                    )}
                    {node.location && (
                      <div>Location: {node.location.name}</div>
                    )}
                  </div>
                  
                  {node.labels && node.labels.length > 0 && (
                    <div className="flex gap-1 mb-2">
                      {node.labels.map((label, index) => (
                        <span key={index} className="px-2 py-1 bg-gray-100 text-gray-700 text-xs rounded">
                          {label}
                        </span>
                      ))}
                    </div>
                  )}
                  
                  {node.tips && (
                    <div className="text-sm">
                      {node.tips.bestTime && (
                        <div className="mb-1">
                          <strong>Best time:</strong> {node.tips.bestTime.join(', ')}
                        </div>
                      )}
                      {node.tips.warnings && node.tips.warnings.length > 0 && (
                        <div className="mb-1">
                          <strong>Warnings:</strong> {node.tips.warnings.join(', ')}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Edges (Transit) */}
          {currentDay.edges && currentDay.edges.length > 0 && (
            <div>
              <h3 className="text-lg font-medium mb-3">Transportation</h3>
              <div className="space-y-2">
                {currentDay.edges.map((edge, index) => (
                  <div key={index} className="border rounded p-3">
                    <div className="text-sm">
                      <strong>From:</strong> {edge.from} → <strong>To:</strong> {edge.to}
                    </div>
                    {edge.transitInfo && (
                      <div className="text-sm text-gray-600 mt-1">
                        Mode: {edge.transitInfo.mode}
                        {edge.transitInfo.durationMin && ` (${edge.transitInfo.durationMin} min)`}
                        {edge.transitInfo.provider && ` | Provider: ${edge.transitInfo.provider}`}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Agent Status */}
      <div className="mt-6 bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-medium mb-3">Agent Status</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {Object.entries(normalizedItinerary.agents).map(([agentName, status]) => (
            <div key={agentName} className="border rounded p-3">
              <div className="font-medium capitalize">{agentName} Agent</div>
              <div className="text-sm text-gray-600">
                Status: <span className="font-medium">{status.status}</span>
              </div>
              {status.lastRunAt && (
                <div className="text-sm text-gray-600">
                  Last run: {new Date(status.lastRunAt).toLocaleString()}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
