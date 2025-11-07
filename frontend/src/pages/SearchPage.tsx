/**
 * Search Page - EaseMyTrip Integration
 * Embedded search interface for flights, hotels, and activities
 * Task 9: Mobile-optimized search page with responsive layout
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
      <main className="flex-1 flex flex-col overflow-hidden px-2 sm:px-4 py-2 gap-2 min-h-0">
        {/* Security Ribbon - Ultra compact on mobile */}
        <div className="flex-shrink-0 bg-gradient-to-r from-blue-600 via-blue-500 to-blue-600 text-white rounded shadow-md border border-blue-400 sm:border-2 sm:rounded-lg sm:shadow-lg">
          <div className="flex items-center gap-1.5 sm:gap-3 px-1.5 sm:px-3 md:px-4 py-1 sm:py-2 md:py-3">
            <div className="flex items-center gap-1.5 sm:gap-3 flex-1 min-w-0">
              <div className="flex-shrink-0 w-5 h-5 sm:w-8 sm:h-8 md:w-10 md:h-10 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm">
                <Shield className="w-2.5 h-2.5 sm:w-4 sm:h-4 md:w-5 md:h-5 text-white" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-[9px] sm:text-xs md:text-sm font-semibold truncate">Secure Booking via EaseMyTrip</p>
                <p className="text-[8px] sm:text-xs text-blue-100 hidden md:block truncate">
                  All bookings processed securely. Your data is protected.
                </p>
              </div>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={handleOpenInNewTab}
              className="min-h-[28px] sm:min-h-[44px] h-7 sm:h-auto px-1.5 sm:px-4 bg-white text-blue-600 hover:bg-blue-50 border-0 shadow-md font-semibold touch-manipulation active:scale-95 transition-transform text-[10px] sm:text-sm flex-shrink-0"
            >
              <ExternalLink className="w-3 h-3 sm:w-4 sm:h-4 sm:mr-2" />
              <span className="hidden sm:inline">Open Website</span>
            </Button>
          </div>
        </div>

        {/* Iframe Container - Takes all remaining vertical space */}
        <div className="flex-1 relative bg-white rounded-lg border border-gray-200 sm:border-2 shadow-xl overflow-hidden min-h-0 safe-bottom">
          {isLoading && (
            <div className="absolute inset-0 bg-white/98 backdrop-blur-sm flex items-center justify-center z-10">
              <div className="text-center px-4">
                <div className="relative mb-4 sm:mb-6">
                  <div className="absolute inset-0 bg-primary/20 rounded-full animate-ping" />
                  <Loader2 className="relative w-10 h-10 sm:w-12 sm:h-12 animate-spin text-primary mx-auto" />
                </div>
                <p className="text-sm sm:text-base font-semibold mb-1 sm:mb-2 text-gray-900">Loading EaseMyTrip...</p>
                <p className="text-xs sm:text-sm text-muted-foreground">Preparing your search experience</p>
              </div>
            </div>
          )}

          <iframe
            src={easemytripUrl}
            className="w-full h-full border-0"
            title="EaseMyTrip Search"
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox"
            onLoad={() => setIsLoading(false)}
            scrolling="yes"
            style={{ overflow: 'auto' }}
          />
        </div>
      </main>
    </div>
  );
}

export default SearchPage;
