import React from 'react';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { CheckCircle, ExternalLink, Calendar } from 'lucide-react';

interface BookedNodeIndicatorProps {
  bookingReference: string;
  bookingStatus: 'confirmed' | 'pending' | 'cancelled';
  bookingDate?: string;
  onViewDetails?: () => void;
}

export const BookedNodeIndicator: React.FC<BookedNodeIndicatorProps> = ({
  bookingReference,
  bookingStatus,
  bookingDate,
  onViewDetails
}) => {
  const getStatusColor = () => {
    switch (bookingStatus) {
      case 'confirmed':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'pending':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'cancelled':
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusText = () => {
    switch (bookingStatus) {
      case 'confirmed':
        return 'Booked';
      case 'pending':
        return 'Pending';
      case 'cancelled':
        return 'Cancelled';
    }
  };

  return (
    <div className="flex items-center gap-2 p-2 bg-green-50 border border-green-200 rounded">
      <CheckCircle className="h-4 w-4 text-green-600" />
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <Badge className={getStatusColor()}>
            {getStatusText()}
          </Badge>
          <span className="text-xs text-gray-600">
            Ref: {bookingReference}
          </span>
        </div>
        {bookingDate && (
          <div className="flex items-center gap-1 mt-1 text-xs text-gray-500">
            <Calendar className="h-3 w-3" />
            {new Date(bookingDate).toLocaleDateString()}
          </div>
        )}
      </div>
      {onViewDetails && (
        <Button
          variant="ghost"
          size="sm"
          onClick={onViewDetails}
          className="h-8"
        >
          <ExternalLink className="h-3 w-3" />
        </Button>
      )}
    </div>
  );
};
