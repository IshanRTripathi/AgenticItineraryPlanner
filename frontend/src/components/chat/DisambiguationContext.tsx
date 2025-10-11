import React from 'react';
import { Alert, AlertDescription } from '../ui/alert';
import { Badge } from '../ui/badge';
import { Info, MapPin, Calendar } from 'lucide-react';

interface DisambiguationContextProps {
  reason: string;
  ambiguousTerm: string;
  currentContext?: {
    day?: number;
    location?: string;
    previousActivity?: string;
  };
}

export const DisambiguationContext: React.FC<DisambiguationContextProps> = ({
  reason,
  ambiguousTerm,
  currentContext
}) => {
  return (
    <Alert className="mb-4">
      <Info className="h-4 w-4" />
      <AlertDescription>
        <div className="space-y-2">
          <div>
            <strong>Why disambiguation is needed:</strong>
            <p className="text-sm mt-1">{reason}</p>
          </div>
          
          {ambiguousTerm && (
            <div>
              <strong>Ambiguous term:</strong>
              <Badge variant="outline" className="ml-2">{ambiguousTerm}</Badge>
            </div>
          )}
          
          {currentContext && (
            <div>
              <strong>Current context:</strong>
              <div className="flex flex-wrap gap-2 mt-1">
                {currentContext.day && (
                  <Badge variant="secondary" className="flex items-center gap-1">
                    <Calendar className="h-3 w-3" />
                    Day {currentContext.day}
                  </Badge>
                )}
                {currentContext.location && (
                  <Badge variant="secondary" className="flex items-center gap-1">
                    <MapPin className="h-3 w-3" />
                    {currentContext.location}
                  </Badge>
                )}
                {currentContext.previousActivity && (
                  <Badge variant="secondary">
                    After: {currentContext.previousActivity}
                  </Badge>
                )}
              </div>
            </div>
          )}
        </div>
      </AlertDescription>
    </Alert>
  );
};
