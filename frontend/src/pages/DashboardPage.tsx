/**
 * User Dashboard Page
 * Shows all user trips and quick actions
 * Task 12: Mobile-optimized dashboard with responsive grid
 */

import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { TripList } from '@/components/dashboard/TripList';
import { Button } from '@/components/ui/button';
import { Sparkles, Loader2 } from 'lucide-react';
import { usePullToRefresh } from '@/hooks/usePullToRefresh';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { cn } from '@/lib/utils';

export function DashboardPage() {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const { isPulling, pullDistance, isRefreshing } = usePullToRefresh({
    onRefresh: async () => {
      // Simulate refresh - in real app, refetch data
      await new Promise(resolve => setTimeout(resolve, 1000));
      window.location.reload();
    },
  });

  return (
    <div className="min-h-screen flex flex-col relative">
      {/* Pull-to-refresh indicator */}
      <div
        className="md:hidden fixed top-16 left-0 right-0 flex justify-center transition-opacity z-50"
        style={{
          opacity: pullDistance / 80,
          transform: `translateY(${Math.min(pullDistance, 80)}px)`,
        }}
      >
        <div className="bg-background/90 backdrop-blur-sm rounded-full px-4 py-2 shadow-lg">
          <Loader2
            className={cn(
              'w-5 h-5 text-primary',
              (isPulling || isRefreshing) && 'animate-spin'
            )}
          />
        </div>
      </div>

      <Header />
      
      <main className="flex-1 bg-muted/30 py-4 sm:py-6 md:py-8">
        <div className="container px-3 sm:px-4">
          {/* Header */}
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3 sm:gap-4 mb-4 sm:mb-6">
            <div className="flex-1 min-w-0">
              <h1 className="text-2xl sm:text-3xl md:text-4xl font-bold text-foreground mb-1 sm:mb-2">
                My Trips
              </h1>
              <p className="text-sm sm:text-base text-muted-foreground">
                {isMobile ? 'Your travel plans' : 'Manage your travel plans and bookings'}
              </p>
            </div>
            
            <Button
              size={isMobile ? undefined : 'lg'}
              onClick={() => window.location.href = '/planner'}
              className="w-full md:w-auto gap-2 min-h-[44px] touch-manipulation active:scale-95 transition-transform"
            >
              <span className="hidden sm:inline">Plan New Trip</span>
              <span className="sm:hidden">New Trip</span>
            </Button>
          </div>

          {/* Trip List */}
          <TripList />
        </div>
      </main>

      <Footer />
    </div>
  );
}


export default DashboardPage;
