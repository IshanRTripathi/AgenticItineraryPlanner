/**
 * Booking Modal Component
 * Displays iframe for EaseMyTrip booking with mark as booked functionality
 */

import { useState } from 'react';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Loader2, ExternalLink, Shield, X, Check, Upload } from 'lucide-react';
import { useToast } from '@/components/ui/use-toast';

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
  const [showMarkBooked, setShowMarkBooked] = useState(false);
  const [bookingRef, setBookingRef] = useState('');
  const { toast } = useToast();

  const handleClose = () => {
    setIsLoading(true);
    setShowMarkBooked(false);
    setBookingRef('');
    onClose();
  };

  const handleOpenInNewTab = () => {
    window.open(providerUrl, '_blank', 'noopener,noreferrer');
  };

  const handleMarkBooked = () => {
    if (bookingRef.trim()) {
      onMarkBooked?.(bookingRef.trim());
      toast({
        title: 'Booking Marked',
        description: `${itemName} has been marked as booked`,
      });
      handleClose();
    } else {
      toast({
        title: 'Booking Reference Required',
        description: 'Please enter a booking reference or confirmation number',
        variant: 'destructive',
      });
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Dummy upload - just show success
      toast({
        title: 'Document Uploaded',
        description: `${file.name} uploaded successfully (demo)`,
      });
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-[90vw] w-full h-[90vh] p-0 gap-0 flex flex-col overflow-hidden rounded-xl border-2 border-gray-200 shadow-2xl">
        {/* Close Button - Top Right */}
        <Button
          variant="ghost"
          size="icon"
          onClick={handleClose}
          className="absolute right-4 top-4 z-50 h-9 w-9 rounded-full bg-white/95 hover:bg-white shadow-lg border-2 border-gray-200 hover:border-gray-300 transition-all"
        >
          <X className="w-4 h-4" />
        </Button>

        {/* Iframe Container - 16:9 aspect ratio */}
        <div className="relative flex-1 bg-gradient-to-br from-gray-50 to-gray-100">
          {isLoading && (
            <div className="absolute inset-0 bg-white/98 backdrop-blur-sm flex items-center justify-center z-10">
              <div className="text-center">
                <div className="relative mb-6">
                  <div className="absolute inset-0 bg-primary/20 rounded-full animate-ping" />
                  <Loader2 className="relative w-12 h-12 animate-spin text-primary mx-auto" />
                </div>
                <p className="text-base font-semibold mb-2 text-gray-900">Loading booking page...</p>
                <p className="text-sm text-muted-foreground">Secure booking in progress</p>
              </div>
            </div>
          )}
          
          <iframe
            src={providerUrl}
            className="w-full h-full border-0 bg-white"
            title="EaseMyTrip Booking"
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox"
            onLoad={() => setIsLoading(false)}
          />
        </div>

        {/* Bottom Action Bar */}
        <div className="border-t-2 border-gray-200 bg-gradient-to-r from-white to-gray-50 p-4 shadow-inner">
          {!showMarkBooked ? (
            <div className="flex items-center justify-between gap-4">
              {/* Left: Info */}
              <div className="flex items-center gap-2 text-sm">
                <Badge variant="secondary" className="text-xs shadow-sm border border-gray-200">
                  <Shield className="w-3 h-3 mr-1" />
                  Secure Booking
                </Badge>
                <span className="text-muted-foreground hidden sm:inline font-medium">
                  Complete your booking on EaseMyTrip
                </span>
              </div>

              {/* Right: Actions */}
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleOpenInNewTab}
                  className="border-2 hover:border-primary/50 shadow-sm"
                >
                  <ExternalLink className="w-4 h-4 mr-2" />
                  <span className="hidden sm:inline">Open in New Tab</span>
                  <span className="sm:hidden">New Tab</span>
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => setShowMarkBooked(true)}
                  className="shadow-md hover:shadow-lg transition-shadow"
                >
                  <Check className="w-4 h-4 mr-2" />
                  Mark as Booked
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleClose}
                  className="hover:bg-gray-100"
                >
                  Book Later
                </Button>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-gray-200">
                <h3 className="font-semibold text-base flex items-center gap-2">
                  <Check className="w-4 h-4 text-primary" />
                  Mark as Booked
                </h3>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setShowMarkBooked(false);
                    setBookingRef('');
                  }}
                  className="hover:bg-gray-100"
                >
                  Cancel
                </Button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Booking Reference Input */}
                <div className="space-y-2">
                  <Label htmlFor="bookingRef" className="text-xs font-semibold text-gray-700">
                    Booking Reference / Confirmation Number
                  </Label>
                  <Input
                    id="bookingRef"
                    placeholder="e.g., EMT123456789"
                    value={bookingRef}
                    onChange={(e) => setBookingRef(e.target.value)}
                    className="h-10 border-2 focus:border-primary shadow-sm"
                  />
                </div>

                {/* Document Upload (Dummy) */}
                <div className="space-y-2">
                  <Label htmlFor="document" className="text-xs font-semibold text-gray-700">
                    Upload Confirmation (Optional)
                  </Label>
                  <div className="relative">
                    <Input
                      id="document"
                      type="file"
                      accept=".pdf,.jpg,.jpeg,.png"
                      onChange={handleFileUpload}
                      className="h-10 cursor-pointer border-2 shadow-sm file:mr-2 file:px-3 file:py-1.5 file:rounded file:border-0 file:text-xs file:font-medium file:bg-primary file:text-white hover:file:bg-primary/90 file:transition-colors"
                    />
                  </div>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setShowMarkBooked(false);
                    setBookingRef('');
                  }}
                  className="border-2 hover:bg-gray-50"
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleMarkBooked}
                  disabled={!bookingRef.trim()}
                  className="shadow-md hover:shadow-lg transition-shadow"
                >
                  <Check className="w-4 h-4 mr-2" />
                  Confirm Booking
                </Button>
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
