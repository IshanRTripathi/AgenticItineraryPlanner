import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { CheckCircle, Download, Mail, Calendar } from 'lucide-react';

interface BookingConfirmationProps {
  bookingReference: string;
  nodeTitle: string;
  onClose: () => void;
}

export function BookingConfirmation({
  bookingReference,
  nodeTitle,
  onClose,
}: BookingConfirmationProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex flex-col items-center text-center">
            <CheckCircle className="w-16 h-16 text-green-500 mb-4" />
            <CardTitle>Booking Confirmed!</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="text-center">
            <p className="text-sm text-gray-600 mb-2">Your booking for</p>
            <p className="font-semibold text-lg">{nodeTitle}</p>
            <p className="text-sm text-gray-600 mt-2">has been confirmed</p>
          </div>

          <div className="bg-gray-50 p-4 rounded-lg text-center">
            <p className="text-xs text-gray-500 mb-1">Booking Reference</p>
            <p className="font-mono font-bold text-lg">{bookingReference}</p>
          </div>

          <div className="flex flex-col gap-2">
            <Button variant="outline" size="sm" disabled>
              <Mail className="w-4 h-4 mr-2" />
              Email Confirmation
            </Button>
            <Button variant="outline" size="sm" disabled>
              <Calendar className="w-4 h-4 mr-2" />
              Add to Calendar
            </Button>
            <Button variant="outline" size="sm" disabled>
              <Download className="w-4 h-4 mr-2" />
              Download Receipt
            </Button>
          </div>

          <Button onClick={onClose} className="w-full">
            Done
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
