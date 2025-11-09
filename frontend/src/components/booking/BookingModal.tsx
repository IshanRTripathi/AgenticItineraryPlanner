/**
 * Booking Modal Component
 * Displays iframe for EaseMyTrip booking - Full screen like SearchPage
 */

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Loader2, ExternalLink, Shield, X } from 'lucide-react';
import { cn } from '@/lib/utils';

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
  itemName,
  providerUrl,
}: BookingModalProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [isVisible, setIsVisible] = useState(isOpen);
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setIsVisible(true);
      setIsAnimating(true);
      // Prevent body scroll
      document.body.style.overflow = 'hidden';
      // Debug: Log the URL being loaded
      console.log('[BookingModal] Opening booking URL:', providerUrl);
    } else if (isVisible) {
      setIsAnimating(false);
      // Delay unmount for exit animation
      const timer = setTimeout(() => {
        setIsVisible(false);
        document.body.style.overflow = '';
      }, 300);
      return () => clearTimeout(timer);
    }
    
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen, isVisible, providerUrl]);

  const handleClose = () => {
    setIsLoading(true);
    onClose();
  };

  const handleOpenInNewTab = () => {
    window.open(providerUrl, '_blank', 'noopener,noreferrer');
  };

  if (!isVisible) return null;

  return (
    <div className="fixed inset-0 z-[9999]">
      {/* Backdrop */}
      <div
        className={cn(
          "fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity duration-300",
          isAnimating ? "opacity-100" : "opacity-0"
        )}
        onClick={handleClose}
      />
      
      {/* Full-screen modal content */}
      <div 
        className={cn(
          "fixed inset-0 bg-gradient-to-b from-background to-muted/20 flex flex-col overflow-hidden transition-all duration-300",
          isAnimating ? "opacity-100 scale-100" : "opacity-0 scale-95"
        )}
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
          {/* Security Ribbon - Smaller text on mobile */}
          <div className="flex-shrink-0 bg-gradient-to-r from-blue-600 via-blue-500 to-blue-600 text-white rounded-lg shadow-lg border-2 border-blue-400">
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2 sm:gap-3 px-2 sm:px-3 md:px-4 py-2 sm:py-3">
              <div className="flex items-start sm:items-center gap-2 sm:gap-3 flex-1 w-full min-w-0">
                <div className="flex-shrink-0 w-7 h-7 sm:w-8 sm:h-8 md:w-10 md:h-10 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm">
                  <Shield className="w-3 h-3 sm:w-4 sm:h-4 md:w-5 md:h-5 text-white" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-[10px] sm:text-xs md:text-sm font-semibold mb-0 sm:mb-0.5 truncate">Secure Booking via EaseMyTrip</p>
                  <p className="text-[9px] sm:text-xs text-blue-100 hidden sm:block truncate">
                    Booking {itemName} securely. Your data is protected.
                  </p>
                  <p className="text-[9px] sm:text-xs text-blue-100 sm:hidden truncate">
                    Secure booking protected
                  </p>
                </div>
              </div>
              <Button
                variant="secondary"
                size="sm"
                onClick={handleOpenInNewTab}
                className="w-full sm:w-auto min-h-[40px] sm:min-h-[44px] bg-white text-blue-600 hover:bg-blue-50 border-0 shadow-md font-semibold touch-manipulation active:scale-95 transition-transform text-xs sm:text-sm"
              >
                <ExternalLink className="w-3 h-3 sm:w-4 sm:h-4 mr-1.5 sm:mr-2" />
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
    </div>
  );
}
