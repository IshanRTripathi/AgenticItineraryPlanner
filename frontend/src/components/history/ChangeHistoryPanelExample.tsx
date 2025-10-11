/**
 * Example usage of ChangeHistoryPanel component
 */

import React from 'react';
import { ChangeHistoryPanel } from './ChangeHistoryPanel';

export function ChangeHistoryPanelExample() {
  return (
    <div className="p-6 space-y-6">
      <div>
        <h2 className="text-2xl font-bold mb-4">Change History Panel Examples</h2>
        <p className="text-gray-600 mb-6">
          View and manage itinerary revision history
        </p>
      </div>

      {/* Basic Usage */}
      <div>
        <h3 className="text-lg font-semibold mb-3">Basic Panel</h3>
        <ChangeHistoryPanel />
      </div>

      {/* Without Filters */}
      <div>
        <h3 className="text-lg font-semibold mb-3">Without Filters</h3>
        <ChangeHistoryPanel showFilters={false} />
      </div>

      {/* Custom Height */}
      <div>
        <h3 className="text-lg font-semibold mb-3">Custom Height</h3>
        <ChangeHistoryPanel maxHeight="400px" />
      </div>

      {/* With Callbacks */}
      <div>
        <h3 className="text-lg font-semibold mb-3">With Event Callbacks</h3>
        <ChangeHistoryPanel
          onRevisionSelect={(revision) => {
            console.log('Selected revision:', revision);
          }}
          onJumpToVersion={(version) => {
            console.log('Jumping to version:', version);
          }}
        />
      </div>

      {/* Features List */}
      <div className="bg-gray-50 p-6 rounded-lg">
        <h3 className="text-lg font-semibold mb-3">Features</h3>
        <ul className="space-y-2 text-sm text-gray-700">
          <li>✅ Chronological list of all changes</li>
          <li>✅ Search changes by description</li>
          <li>✅ Filter by user</li>
          <li>✅ View change count per revision</li>
          <li>✅ Restore to any previous version</li>
          <li>✅ Current version indicator</li>
          <li>✅ User attribution</li>
          <li>✅ Relative timestamps</li>
          <li>✅ Auto-refresh capability</li>
        </ul>
      </div>
    </div>
  );
}
