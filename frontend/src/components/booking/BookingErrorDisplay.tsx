/**
 * Booking Error Display
 * Shows booking errors with retry and alternatives
 */

import React from 'react';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { AlertTriangle, RefreshCw, ExternalLink } from 'lucide-react';

interface BookingError {
  code: string;
  message: string;
  nodeId: string;
  nodeName: string;
  suggestions?: string[];
  alternativeOptions?: Array<{
    name: string;
    price: number;
    url?: string;
  }>;
}

interface BookingErrorDisplayProps {
  error: BookingError;
  onRetry: () => void;
  onDismiss: () => void;
  className?: string;
}

export function BookingErrorDisplay({
  error,
  onRetry,
  onDismiss,
  className = '',
}: BookingErrorDisplayProps) {
  return (
    <Card className={`${className} border-red-200 border-2`}>
      <CardContent className="p-4 space-y-4">
        {/* Error Header */}
        <div className="flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h4 className="font-semibold text-red-700">Booking Failed</h4>
            <p className="text-sm text-gray-600 mt-1">{error.nodeName}</p>
          </div>
        </div>

        {/* Error Message */}
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-sm text-red-800">{error.message}</p>
          <p className="text-xs text-gray-500 mt-1">Error Code: {error.code}</p>
        </div>

        {/* Suggestions */}
        {error.suggestions && error.suggestions.length > 0 && (
          <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <h5 className="text-sm font-semibold text-blue-900 mb-2">Suggestions:</h5>
            <ul className="list-disc list-inside space-y-1">
              {error.suggestions.map((suggestion, index) => (
                <li key={index} className="text-sm text-blue-800">
                  {suggestion}
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Alternative Options */}
        {error.alternativeOptions && error.alternativeOptions.length > 0 && (
          <div>
            <h5 className="text-sm font-semibold mb-2">Alternative Options:</h5>
            <div className="space-y-2">
              {error.alternativeOptions.map((option, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50"
                >
                  <div>
                    <p className="text-sm font-medium">{option.name}</p>
                    <p className="text-xs text-gray-600">₹{option.price.toLocaleString()}</p>
                  </div>
                  {option.url && (
                    <Button size="sm" variant="outline" asChild>
                      <a href={option.url} target="_blank" rel="noopener noreferrer">
                        <ExternalLink className="w-4 h-4 mr-1" />
                        View
                      </a>
                    </Button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-2 pt-2 border-t">
          <Button variant="outline" onClick={onDismiss}>
            Dismiss
          </Button>
          <Button onClick={onRetry} className="bg-blue-600 hover:bg-blue-700">
            <RefreshCw className="w-4 h-4 mr-2" />
            Retry Booking
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
