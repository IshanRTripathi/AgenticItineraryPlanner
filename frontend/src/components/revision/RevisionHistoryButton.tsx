import React, { useState } from 'react';
import { Button } from '../ui/button';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from '../ui/sheet';
import { History } from 'lucide-react';
import { RevisionTimeline } from './RevisionTimeline';

interface RevisionHistoryButtonProps {
  itineraryId: string;
  onVersionSelect?: (version: number) => void;
}

export const RevisionHistoryButton: React.FC<RevisionHistoryButtonProps> = ({
  itineraryId,
  onVersionSelect
}) => {
  const [isOpen, setIsOpen] = useState(false);

  const handleVersionSelect = (version: number) => {
    onVersionSelect?.(version);
    setIsOpen(false);
  };

  return (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetTrigger asChild>
        <Button variant="outline" size="sm">
          <History className="h-4 w-4 mr-2" />
          History
        </Button>
      </SheetTrigger>
      <SheetContent side="right" className="w-full sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Version History</SheetTitle>
          <SheetDescription>
            View and restore previous versions of your itinerary
          </SheetDescription>
        </SheetHeader>
        <div className="mt-6">
          <RevisionTimeline
            itineraryId={itineraryId}
            onVersionSelect={handleVersionSelect}
          />
        </div>
      </SheetContent>
    </Sheet>
  );
};
