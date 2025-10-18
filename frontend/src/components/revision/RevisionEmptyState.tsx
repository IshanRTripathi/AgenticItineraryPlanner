import React from 'react';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { History, FileText, HelpCircle } from 'lucide-react';

interface RevisionEmptyStateProps {
  onLearnMore?: () => void;
}

export const RevisionEmptyState: React.FC<RevisionEmptyStateProps> = ({
  onLearnMore
}) => {
  return (
    <Card>
      <CardContent className="flex flex-col items-center justify-center py-12 px-6 text-center">
        <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
          <History className="h-8 w-8 text-gray-400" />
        </div>
        
        <h3 className="text-lg font-semibold mb-2">No Version History Yet</h3>
        
        <p className="text-sm text-gray-600 mb-6 max-w-md">
          As you make changes to your itinerary, we'll automatically save versions here. 
          You'll be able to view, compare, and restore previous versions at any time.
        </p>

        <div className="space-y-4 w-full max-w-sm">
          <div className="flex items-start gap-3 text-left p-3 bg-gray-50 rounded-lg">
            <FileText className="h-5 w-5 text-gray-400 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="text-sm font-medium mb-1">How it works</h4>
              <p className="text-xs text-gray-600">
                Every time you save changes, we create a new version. You can always go back to any previous version.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3 text-left p-3 bg-gray-50 rounded-lg">
            <HelpCircle className="h-5 w-5 text-gray-400 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="text-sm font-medium mb-1">Why use versions?</h4>
              <p className="text-xs text-gray-600">
                Experiment freely knowing you can always restore a previous version if needed.
              </p>
            </div>
          </div>
        </div>

        {onLearnMore && (
          <Button variant="outline" onClick={onLearnMore} className="mt-6">
            Learn More
          </Button>
        )}
      </CardContent>
    </Card>
  );
};
