/**
 * Booking Modal Component
 * Displays iframe for provider booking with mock confirmation
 */

import { useState } from 'react';
import { BottomSheet } from '@/components/ui/bottom-sheet';
import { Button } from '@/components/ui/button';
import { Loader2, CheckCircle, XCircle } from 'lucide-react';

interface BookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  bookingType: 'flight' | 'hotel' | 'activity';
  itemName: string;
  providerUrl?: string;
}

export function BookingModal({ 
  isOpen, 
  onClose, 
  bookingType, 
  itemName,
  providerUrl = 'https://example.com/booking'
}: BookingModalProps) {
  const [bookingStatus, setBookingStatus] = useState<'loading' | 'success' | 'error' | null>(null);
  const [showIframe, setShowIframe] = useState(true);

  const handleMockBooking = (success: boolean) => {
    setShowIframe(false);
    setBookingStatus(success ? 'success' : 'error');
  };

  const handleClose = () => {
    setBookingStatus(null);
    setShowIframe(true);
    onClose();
  };

  return (
    <BottomSheet 
      open={isOpen} 
      onOpenChange={handleClose}
      title={`Book ${bookingType}: ${itemName}`}
      className="max-w-4xl"
    >

        {showIframe && !bookingStatus && (
          <div className="space-y-4">
            {/* Iframe Container */}
            <div className="relative w-full h-[500px] bg-muted rounded-lg overflow-hidden">
              <iframe
                src={providerUrl}
                className="w-full h-full border-0"
                title="Provider Booking"
                sandbox="allow-scripts allow-same-origin allow-forms"
              />
              
              {/* Loading overlay */}
              <div className="absolute inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center">
                <div className="text-center">
                  <Loader2 className="w-12 h-12 animate-spin text-primary mx-auto mb-4" />
                  <p className="text-sm text-muted-foreground">Loading booking page...</p>
                </div>
              </div>
            </div>

            {/* Mock Booking Actions */}
            <div className="bg-muted/50 rounded-lg p-4">
              <p className="text-sm text-muted-foreground mb-3">
                Demo Mode: Simulate booking outcome
              </p>
              <div className="flex gap-3">
                <Button
                  onClick={() => handleMockBooking(true)}
                  className="flex-1"
                >
                  <CheckCircle className="w-4 h-4 mr-2" />
                  Simulate Success
                </Button>
                <Button
                  onClick={() => handleMockBooking(false)}
                  variant="outline"
                  className="flex-1"
                >
                  <XCircle className="w-4 h-4 mr-2" />
                  Simulate Failure
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Success State */}
        {bookingStatus === 'success' && (
          <div className="py-12 text-center">
            <div className="w-20 h-20 rounded-full bg-success/10 flex items-center justify-center mx-auto mb-6">
              <CheckCircle className="w-10 h-10 text-success" />
            </div>
            <h3 className="text-2xl font-bold mb-2">Booking Confirmed!</h3>
            <p className="text-muted-foreground mb-6">
              Your {bookingType} has been successfully booked.
            </p>
            <div className="bg-muted/50 rounded-lg p-4 mb-6 text-left max-w-md mx-auto">
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Booking ID:</span>
                  <span className="font-semibold">BK{Math.random().toString(36).substr(2, 9).toUpperCase()}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Item:</span>
                  <span className="font-semibold">{itemName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Status:</span>
                  <span className="text-success font-semibold">Confirmed</span>
                </div>
              </div>
            </div>
            <Button onClick={handleClose} size="lg">
              Done
            </Button>
          </div>
        )}

        {/* Error State */}
        {bookingStatus === 'error' && (
          <div className="py-12 text-center">
            <div className="w-20 h-20 rounded-full bg-error/10 flex items-center justify-center mx-auto mb-6">
              <XCircle className="w-10 h-10 text-error" />
            </div>
            <h3 className="text-2xl font-bold mb-2">Booking Failed</h3>
            <p className="text-muted-foreground mb-6">
              We couldn't complete your booking. Please try again.
            </p>
            <div className="flex gap-3 justify-center">
              <Button onClick={() => { setBookingStatus(null); setShowIframe(true); }}>
                Try Again
              </Button>
              <Button onClick={handleClose} variant="outline">
                Cancel
              </Button>
            </div>
          </div>
        )}
    </BottomSheet>
  );
}
