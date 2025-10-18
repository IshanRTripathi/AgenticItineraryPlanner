import React, { useState } from 'react';
import { UndoRedoControls } from '../controls/UndoRedoControls';
import { PdfExportButton } from '../export/PdfExportButton';
import { ShareModal } from '../share/ShareModal';
import { Button } from '../ui/button';
import { Share2, Settings } from 'lucide-react';
import { useUnifiedItinerary } from '../../contexts/UnifiedItineraryContext';

export function MainToolbar() {
  const { state } = useUnifiedItinerary();
  const [showShareModal, setShowShareModal] = useState(false);

  if (!state.itinerary?.id) return null;

  return (
    <div className="flex items-center gap-2 p-2 border-b bg-white">
      <UndoRedoControls showVersionInfo={true} showStackDepth={false} />
      <div className="h-6 w-px bg-gray-300 mx-2" />
      <PdfExportButton itineraryId={state.itinerary.id} variant="outline" size="sm" />
      <Button variant="outline" size="sm" onClick={() => setShowShareModal(true)}>
        <Share2 className="w-4 h-4 mr-1" />
        Share
      </Button>
      <div className="flex-1" />
      <Button variant="ghost" size="sm">
        <Settings className="w-4 h-4" />
      </Button>
      <ShareModal
        itineraryId={state.itinerary.id}
        isOpen={showShareModal}
        onClose={() => setShowShareModal(false)}
      />
    </div>
  );
}
