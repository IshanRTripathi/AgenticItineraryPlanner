/**
 * Loading Skeleton for Trip Detail Page
 * Shown while fetching itinerary data
 */

import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';

export function TripDetailSkeleton() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1">
        {/* Hero Skeleton */}
        <div className="relative h-96 bg-muted animate-pulse" />

        {/* Content Skeleton */}
        <div className="container py-12">
          {/* Tabs Skeleton */}
          <div className="flex gap-4 mb-8">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="h-10 w-24 bg-muted rounded animate-pulse" />
            ))}
          </div>

          {/* Content Cards Skeleton */}
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-32 bg-muted rounded animate-pulse" />
            ))}
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}
