/**
 * Mock Confirmation Modal
 * Premium booking confirmation with Material 3 design
 */

import { useEffect, useState } from 'react';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { CheckCircle2, Sparkles } from 'lucide-react';

interface MockConfirmationModalProps {
  isOpen: boolean;
  onClose: () => void;
  providerName: string;
  providerLogo?: string;
}

export function MockConfirmationModal({
  isOpen,
  onClose,
  providerName,
  providerLogo,
}: MockConfirmationModalProps) {
  const [confirmationNumber, setConfirmationNumber] = useState('');
  const [showConfetti, setShowConfetti] = useState(false);

  useEffect(() => {
    if (isOpen) {
      // Generate random confirmation number
      const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
      let code = 'EMT';
      for (let i = 0; i < 9; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
      }
      setConfirmationNumber(code);
      
      // Trigger confetti animation
      setShowConfetti(true);
      setTimeout(() => setShowConfetti(false), 3000);
    }
  }, [isOpen]);

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md p-0 overflow-hidden">
        {/* Confetti Effect */}
        {showConfetti && (
          <div className="absolute inset-0 pointer-events-none overflow-hidden">
            {[...Array(30)].map((_, i) => (
              <div
                key={i}
                className="absolute w-2 h-2 rounded-full animate-confetti"
                style={{
                  left: `${Math.random() * 100}%`,
                  top: '-10px',
                  backgroundColor: ['#002B5B', '#F5C542', '#10B981', '#F59E0B'][Math.floor(Math.random() * 4)],
                  animationDelay: `${Math.random() * 0.5}s`,
                  animationDuration: `${2 + Math.random()}s`,
                }}
              />
            ))}
          </div>
        )}

        {/* Content */}
        <div className="relative bg-gradient-to-br from-success/5 to-primary/5 p-12 text-center">
          {/* Success Icon */}
          <div className="relative inline-block mb-6">
            <div className="absolute inset-0 bg-success/20 rounded-full animate-ping" />
            <div className="relative w-20 h-20 rounded-full bg-success flex items-center justify-center animate-scale-in">
              <CheckCircle2 className="w-12 h-12 text-white" />
            </div>
          </div>

          {/* Heading */}
          <h2 className="text-3xl font-bold mb-2 animate-fade-in">
            Booking Confirmed!
          </h2>
          
          <p className="text-muted-foreground mb-6 animate-fade-in" style={{ animationDelay: '0.1s' }}>
            Your reservation has been successfully confirmed
          </p>

          {/* Confirmation Number */}
          <div className="bg-white rounded-xl p-6 mb-6 shadow-lg animate-fade-in" style={{ animationDelay: '0.2s' }}>
            <div className="text-sm text-muted-foreground mb-2">Confirmation Number</div>
            <div className="text-2xl font-bold text-primary tracking-wider">
              {confirmationNumber}
            </div>
          </div>

          {/* Provider Info */}
          {providerLogo && (
            <div className="flex items-center justify-center gap-3 mb-8 animate-fade-in" style={{ animationDelay: '0.3s' }}>
              <div className="text-sm text-muted-foreground">Booked with</div>
              <div className="flex items-center gap-2 px-4 py-2 bg-white rounded-lg shadow">
                <img
                  src={providerLogo}
                  alt={providerName}
                  className="w-6 h-6 object-contain"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                  }}
                />
                <span className="font-semibold">{providerName}</span>
              </div>
            </div>
          )}

          {/* Action Button */}
          <Button
            onClick={onClose}
            size="lg"
            className="w-full animate-fade-in"
            style={{ animationDelay: '0.4s' }}
          >
            <Sparkles className="w-4 h-4 mr-2" />
            Continue Planning
          </Button>

          {/* Additional Info */}
          <p className="text-xs text-muted-foreground mt-4 animate-fade-in" style={{ animationDelay: '0.5s' }}>
            A confirmation email has been sent to your inbox
          </p>
        </div>
      </DialogContent>
    </Dialog>
  );
}
