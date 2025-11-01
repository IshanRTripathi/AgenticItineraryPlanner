/**
 * Search Page - EaseMyTrip Integration
 * Embedded search interface for flights, hotels, and activities
 */

import { useState } from 'react';
import { Header } from '@/components/layout/Header';
import { Loader2, Shield, ExternalLink } from 'lucide-react';
import { Button } from '@/components/ui/button';

export function SearchPage() {
  const [isLoading, setIsLoading] = useState(true);
  const easemytripUrl = 'https://www.easemytrip.com';

  const handleOpenInNewTab = () => {
    window.open(easemytripUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="h-screen flex flex-col bg-gradient-to-b from-background to-muted/20">
      <Header />

      {/* Main content area - takes remaining height after header */}
      <main className="flex-1 flex flex-col overflow-hidden px-4 py-2 gap-2 min-h-0">
        {/* Security Ribbon */}
        <div className="flex-shrink-0 bg-gradient-to-r from-blue-600 via-blue-500 to-blue-600 text-white rounded-lg shadow-lg border-2 border-blue-400">
          <div className="flex items-center justify-between px-4 py-3">
            <div className="flex items-center gap-3 flex-1">
              <div className="flex-shrink-0 w-10 h-10 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm">
                <Shield className="w-5 h-5 text-white" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-semibold mb-0.5">Secure Booking via EaseMyTrip</p>
                <p className="text-xs text-blue-100">
                  All bookings are processed securely through EaseMyTrip. Your data is protected and never compromised.
                </p>
              </div>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={handleOpenInNewTab}
              className="flex-shrink-0 bg-white text-blue-600 hover:bg-blue-50 border-0 shadow-md font-semibold"
            >
              <ExternalLink className="w-4 h-4 mr-2" />
              Book from Website
            </Button>
          </div>
        </div>

        {/* Iframe Container - Takes all remaining vertical space */}
        <div className="flex-1 relative bg-white rounded-lg border-2 border-gray-200 shadow-xl overflow-hidden min-h-0">
          {isLoading && (
            <div className="absolute inset-0 bg-white/98 backdrop-blur-sm flex items-center justify-center z-10">
              <div className="text-center">
                <div className="relative mb-6">
                  <div className="absolute inset-0 bg-primary/20 rounded-full animate-ping" />
                  <Loader2 className="relative w-12 h-12 animate-spin text-primary mx-auto" />
                </div>
                <p className="text-base font-semibold mb-2 text-gray-900">Loading EaseMyTrip...</p>
                <p className="text-sm text-muted-foreground">Preparing your search experience</p>
              </div>
            </div>
          )}

          <iframe
            src={easemytripUrl}
            className="w-full h-full border-0"
            title="EaseMyTrip Search"
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox"
            onLoad={() => setIsLoading(false)}
          />
        </div>
      </main>
    </div>
  );
}

export default SearchPage;
