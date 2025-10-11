/**
 * Booking Cancellation Component
 * Handles booking cancellation with confirmation
 */

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { AlertTriangle, X, Loader2 } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface BookingDetails {
  bookingId: string;
  bookingReference: string;
  nodeName: string;
  bookingDate: Date;
  amount: number;
  status: 'confirmed' | 'pending';
}

interface BookingCancellationProps {
  booking: BookingDetails;
  isOpen: boolean;
  onClose: () => void;
  onCancelled: () => void;
  className?: string;
}

export function BookingCancellation({
  booking,
  isOpen,
  onClose,
  onCancelled,
  className = '',
}: BookingCancellationProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleCancel = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `${apiClient['baseUrl']}/bookings/${booking.bookingId}:cancel`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(apiClient['authToken'] ? { Authorization: `Bearer ${apiClient['authToken']}` } : {}),
          },
        }
      );

      if (!response.ok) {
        throw new Error('Failed to cancel booking');
      }

      onCancelled();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to cancel booking');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <Card className={`w-full max-w-md ${className}`}>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-amber-500" />
              <CardTitle>Cancel Booking</CardTitle>
            </div>
            <button onClick={onClose} disabled={loading}>
              <X className="w-5 h-5" />
            </button>
          </div>
        </CardHeader>

        <CardContent className="space-y-4">
          {/* Warning */}
          <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg">
            <p className="text-sm text-amber-800">
              Are you sure you want to cancel this booking? This action cannot be undone.
            </p>
          </div>

          {/* Booking Details */}
          <div className="space-y-2">
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">Activity:</span>
              <span className="text-sm font-medium">{booking.nodeName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">Reference:</span>
              <span className="text-sm font-mono">{booking.bookingReference}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">Date:</span>
              <span className="text-sm">{booking.bookingDate.toLocaleDateString()}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">Amount:</span>
              <span className="text-sm font-semibold">₹{booking.amount.toLocaleString()}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">Status:</span>
              <Badge variant={booking.status === 'confirmed' ? 'default' : 'secondary'}>
                {booking.status}
              </Badge>
            </div>
          </div>

          {/* Error */}
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button variant="outline" onClick={onClose} disabled={loading}>
              Keep Booking
            </Button>
            <Button
              variant="destructive"
              onClick={handleCancel}
              disabled={loading}
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Cancelling...
                </>
              ) : (
                'Cancel Booking'
              )}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
