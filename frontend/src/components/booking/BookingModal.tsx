/**
 * Booking Modal Component
 * Displays iframe for EaseMyTrip booking with mark as booked functionality
 */

import { useState } from 'react';
import { Dialog } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Loader2, ExternalLink, Shield, X } from 'lucide-react';

interface BookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  bookingType: 'flight' | 'hotel' | 'activity';
  itemName: string;
  providerUrl: string;
  onMarkBooked?: (bookingRef: string) => void;
}

export function BookingModal({ 
  isOpen, 
  onClose, 
  bookingType, 
  itemName,
  providerUrl,
  onMarkBooked
}: BookingModalProps) {
  const [isLoading, setIsLoading] = useState(true);

  const handleClose = () => {
    setIsLoading(true);
    onClose();
  };

  const handleOpenInNewTab = () => {
    window.open(providerUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      {/* Full-screen modal content - matches SearchPage layout */}
      <div 
        className="relative bg-gradient-to-b from-background to-muted/20 w-[100vw] h-[100vh] max-w-none flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button - Top Right */}
        <Button
          variant="ghost"
          size="icon"
          onClick={handleClose}
          className="absolute right-2 top-2 md:right-4 md:top-4 z-50 min-w-[44px] min-h-[44px] w-11 h-11 md:w-9 md:h-9 rounded-full bg-white/95 hover:bg-white shadow-lg border-2 border-gray-200 hover:border-gray-300 transition-all touch-manipulation active:scale-95"
        >
          <X className="w-5 h-5 md:w-4 md:h-4" />
        </Button>

        {/* Main content area - matches SearchPage */}
        <main className="flex-1 flex flex-col overflow-hidden px-2 sm:px-4 py-2 gap-2 min-h-0 pt-14 md:pt-2">
          {/* Security Ribbon */}
          <div className="flex-shrink-0 bg-gradient-to-r from-blue-600 via-blue-500 to-blue-600 text-white rounded-lg shadow-lg border-2 border-blue-400">
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 px-3 sm:px-4 py-3">
              <div className="flex items-start sm:items-center gap-3 flex-1 w-full">
                <div className="flex-shrink-0 w-8 h-8 sm:w-10 sm:h-10 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm">
                  <Shield className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs sm:text-sm font-semibold mb-0.5">Secure Booking via EaseMyTrip</p>
                  <p className="text-xs text-blue-100 hidden sm:block">
                    Booking {itemName} securely. Your data is protected.
                  </p>
                  <p className="text-xs text-blue-100 sm:hidden">
                    Secure booking with data protection
                  </p>
                </div>
              </div>
              <Button
                variant="secondary"
                size="sm"
                onClick={handleOpenInNewTab}
                className="w-full sm:w-auto min-h-[44px] bg-white text-blue-600 hover:bg-blue-50 border-0 shadow-md font-semibold touch-manipulation active:scale-95 transition-transform"
              >
                <ExternalLink className="w-4 h-4 mr-2" />
                <span className="hidden sm:inline">Open in New Tab</span>
                <span className="sm:hidden">New Tab</span>
              </Button>
            </div>
          </div>

          {/* Iframe Container - Takes all remaining vertical space */}
          <div className="flex-1 relative bg-white rounded-lg border border-gray-200 sm:border-2 shadow-xl overflow-hidden min-h-0">
            {isLoading && (
              <div className="absolute inset-0 bg-white/98 backdrop-blur-sm flex items-center justify-center z-10">
                <div className="text-center px-4">
                  <div className="relative mb-4 sm:mb-6">
                    <div className="absolute inset-0 bg-primary/20 rounded-full animate-ping" />
                    <Loader2 className="relative w-10 h-10 sm:w-12 sm:h-12 animate-spin text-primary mx-auto" />
                  </div>
                  <p className="text-sm sm:text-base font-semibold mb-1 sm:mb-2 text-gray-900">Loading booking page...</p>
                  <p className="text-xs sm:text-sm text-muted-foreground">Preparing your booking</p>
                </div>
              </div>
            )}
            
            <iframe
              src={providerUrl}
              className="w-full h-full border-0"
              title="EaseMyTrip Booking"
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox"
              onLoad={() => setIsLoading(false)}
              allow="payment"
            />
          </div>

        </main>
      </div>
    </Dialog>
  );
}
