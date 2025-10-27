/**
 * Premium Booking Modal - Apple HIG Design
 * Embedded iframe booking with mock confirmation
 */

import { useState, useEffect } from 'react';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { MockConfirmationModal } from './MockConfirmationModal';
import { X, Shield, Loader2 } from 'lucide-react';

interface PremiumBookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  providerName: string;
  providerLogo: string;
  providerUrl: string;
}

export function PremiumBookingModal({
  isOpen,
  onClose,
  providerName,
  providerLogo,
  providerUrl,
}: PremiumBookingModalProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [showConfirmation, setShowConfirmation] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setIsLoading(true);
      setShowConfirmation(false);

      // Simulate iframe loading
      const loadTimer = setTimeout(() => {
        setIsLoading(false);
      }, 1500);

      // Show mock confirmation after 2-3 seconds
      const confirmTimer = setTimeout(() => {
        setShowConfirmation(true);
      }, 3000);

      return () => {
        clearTimeout(loadTimer);
        clearTimeout(confirmTimer);
      };
    }
  }, [isOpen]);

  const handleConfirmationClose = () => {
    setShowConfirmation(false);
    onClose();
  };

  return (
    <>
      <Dialog open={isOpen && !showConfirmation} onOpenChange={onClose}>
        <DialogContent className="max-w-[1200px] h-[80vh] p-0 gap-0">
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b bg-gradient-to-r from-primary/5 to-transparent">
            <div className="flex items-center gap-4">
              {/* Provider Logo */}
              <div className="w-12 h-12 rounded-lg border-2 border-primary/20 bg-white flex items-center justify-center">
                <img
                  src={providerLogo}
                  alt={providerName}
                  className="w-8 h-8 object-contain"
                  onError={(e) => {
                    e.currentTarget.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="%23002B5B" stroke-width="2"%3E%3Crect x="3" y="3" width="18" height="18" rx="2"/%3E%3C/svg%3E';
                  }}
                />
              </div>
              
              {/* Provider Name */}
              <div>
                <h2 className="text-xl font-semibold">{providerName}</h2>
                <Badge variant="secondary" className="mt-1">
                  <Shield className="w-3 h-3 mr-1" />
                  Secure Booking
                </Badge>
              </div>
            </div>

            {/* Close Button */}
            <Button
              variant="ghost"
              size="icon"
              onClick={onClose}
              className="h-10 w-10 rounded-full"
            >
              <X className="w-5 h-5" />
            </Button>
          </div>

          {/* Iframe Container */}
          <div className="relative flex-1 bg-muted">
            {isLoading && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-white z-10">
                <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
                <p className="text-lg font-medium mb-2">Loading {providerName}...</p>
                <p className="text-sm text-muted-foreground">Secure booking in progress</p>
              </div>
            )}

            <iframe
              src={providerUrl}
              className="w-full h-full border-none"
              sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
              title={`${providerName} Booking`}
              onLoad={() => setIsLoading(false)}
            />
          </div>
        </DialogContent>
      </Dialog>

      {/* Mock Confirmation Modal */}
      <MockConfirmationModal
        isOpen={showConfirmation}
        onClose={handleConfirmationClose}
        providerName={providerName}
        providerLogo={providerLogo}
      />
    </>
  );
}
