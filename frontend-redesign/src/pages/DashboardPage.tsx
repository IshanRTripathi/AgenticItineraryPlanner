/**
 * User Dashboard Page
 * Shows all user trips and quick actions
 */

import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { TripList } from '@/components/dashboard/TripList';
import { Button } from '@/components/ui/button';
import { Sparkles } from 'lucide-react';

export function DashboardPage() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      
      <main className="flex-1 bg-muted/30 py-12">
        <div className="container">
          {/* Header */}
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
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
              onClick={() => window.location.href = '/ai-planner'}
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
