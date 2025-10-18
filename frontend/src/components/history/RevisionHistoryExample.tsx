/**
 * Integrated example showing ChangeHistoryPanel and RevisionDetailView working together
 */

import React, { useState } from 'react';
import { ChangeHistoryPanel } from './ChangeHistoryPanel';
import { RevisionDetailView } from './RevisionDetailView';
import { useUnifiedItinerary } from '../../contexts/UnifiedItineraryContext';

export function RevisionHistoryExample() {
  const { state } = useUnifiedItinerary();
  const [selectedRevisionId, setSelectedRevisionId] = useState<string | null>(null);

  if (!state.itinerary?.id) {
    return (
      <div className="p-6 text-center text-gray-500">
        <p>No itinerary loaded. Please create or load an itinerary first.</p>
      </div>
    );
  }

  return (
    <div className="p-6">
      <h2 className="text-2xl font-bold mb-6">Revision History System</h2>
      
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* History Panel */}
        <div>
          <h3 className="text-lg font-semibold mb-3">Change History</h3>
          <ChangeHistoryPanel
            maxHeight="600px"
            onRevisionSelect={(revision) => {
              setSelectedRevisionId(revision.id);
            }}
          />
        </div>

        {/* Detail View */}
        <div>
          <h3 className="text-lg font-semibold mb-3">Revision Details</h3>
          {selectedRevisionId ? (
            <RevisionDetailView
              revisionId={selectedRevisionId}
              itineraryId={state.itinerary.id}
              onClose={() => setSelectedRevisionId(null)}
              onRestore={() => {
                console.log('Revision restored');
                setSelectedRevisionId(null);
              }}
            />
          ) : (
            <div className="border rounded-lg p-12 text-center text-gray-500">
              <p>Select a revision from the history to view details</p>
            </div>
          )}
        </div>
      </div>

      {/* Features List */}
      <div className="mt-8 bg-gray-50 p-6 rounded-lg">
        <h3 className="text-lg font-semibold mb-3">Complete Revision System Features</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <h4 className="font-medium mb-2">History Panel</h4>
            <ul className="space-y-1 text-sm text-gray-700">
              <li>✅ Chronological list</li>
              <li>✅ Search & filter</li>
              <li>✅ User attribution</li>
              <li>✅ Quick restore</li>
              <li>✅ Current version indicator</li>
            </ul>
          </div>
          <div>
            <h4 className="font-medium mb-2">Detail View</h4>
            <ul className="space-y-1 text-sm text-gray-700">
              <li>✅ Full change details</li>
              <li>✅ Side-by-side diff</li>
              <li>✅ Metadata display</li>
              <li>✅ Restore confirmation</li>
              <li>✅ Export & share (coming soon)</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
