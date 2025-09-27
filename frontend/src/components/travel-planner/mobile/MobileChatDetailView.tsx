import React from 'react';
import { Button } from '../../ui/button';
import { ArrowLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { ChatInterface } from '../../ChatInterface';

interface MobileChatDetailViewProps {
  itineraryId: string;
  onItineraryUpdate: (updatedItinerary: any) => void;
  onBack: () => void;
}

export function MobileChatDetailView({
  itineraryId,
  onItineraryUpdate,
  onBack,
}: MobileChatDetailViewProps) {
  const { t } = useTranslation();

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between">
        <Button
          variant="ghost"
          size="sm"
          onClick={onBack}
          className="flex items-center space-x-2 min-h-[44px]"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back</span>
        </Button>
        <h1 className="text-lg font-semibold">AI Assistant</h1>
        <div className="w-16" /> {/* Spacer for centering */}
      </div>

      {/* Chat Interface */}
      <div className="flex-1 overflow-hidden">
        <ChatInterface
          itineraryId={itineraryId}
          onItineraryUpdate={onItineraryUpdate}
        />
      </div>
    </div>
  );
}
