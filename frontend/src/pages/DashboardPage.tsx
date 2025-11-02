/**
 * User Dashboard Page
 * Shows all user trips and quick actions
 */

import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { TripList } from '@/components/dashboard/TripList';
import { Button } from '@/components/ui/button';
import { Sparkles, Loader2 } from 'lucide-react';
import { usePullToRefresh } from '@/hooks/usePullToRefresh';
import { cn } from '@/lib/utils';

export function DashboardPage() {
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
      
      <main className="flex-1 bg-muted/30 py-8">
        <div className="container">
          {/* Header */}
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6">
            <div>
              <h1 className="text-4xl font-bold text-foreground mb-2">
                My Trips
              </h1>
              <p className="text-muted-foreground">
                Manage your travel plans and bookings
              </p>
            </div>
            
            <Button
              size="lg"
              onClick={() => window.location.href = '/planner'}
              className="gap-2"
            >
              <Sparkles className="w-5 h-5" />
              Plan New Trip
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
