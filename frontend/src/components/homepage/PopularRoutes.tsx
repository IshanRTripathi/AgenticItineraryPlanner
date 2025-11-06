/**
 * Popular Routes Section
 * Task 15: Mobile-optimized with responsive cards and touch-friendly buttons
 */

import { ArrowRight, Plane } from 'lucide-react';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { mockRoutes } from '../../data/mockRoutes';
import { useNavigate } from 'react-router-dom';
import { useMediaQuery } from '@/hooks/useMediaQuery';

export function PopularRoutes() {
  return (
    <section className="py-8 sm:py-10 md:py-12 bg-white">
      <div className="container mx-auto px-3 sm:px-4">
        {/* Section Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 sm:gap-4 mb-6 sm:mb-8">
          <div className="flex-1">
            <h2 className="text-2xl sm:text-3xl font-bold text-gray-900">Popular Flight Routes</h2>
            <p className="text-sm sm:text-base text-gray-600 mt-1 sm:mt-2">Best deals on trending destinations</p>
          </div>
          <Button variant="outline" className="hidden md:flex min-h-[44px] touch-manipulation">
            View All Routes
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </div>

        {/* Route Cards Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-5 md:gap-6">
          {mockRoutes.slice(0, 2).map((route) => (
            <RouteCard key={route.id} route={route} />
          ))}
        </div>

        {/* Mobile View All Button */}
        <div className="mt-4 sm:mt-6 md:hidden">
          <Button variant="outline" className="w-full min-h-[48px] touch-manipulation active:scale-95 transition-transform">
            View All Routes
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </div>
      </div>
    </section>
  );
}

interface RouteCardProps {
  route: {
    id: string;
    origin: string;
    destination: string;
    airline: string;
    price: number;
    currency: string;
  };
}

function RouteCard({ route }: RouteCardProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const navigate = useNavigate();

  const handleBookFlight = () => {
    navigate('/search');
  };

  return (
    <div className="group cursor-pointer touch-manipulation active:scale-[0.98] transition-transform">
      {/* Boarding Pass Container */}
      <div className="relative bg-white rounded-lg sm:rounded-xl shadow-lg hover:shadow-2xl transition-all duration-300 hover:-translate-y-1 overflow-hidden border border-gray-200">
        {/* Perforated Edge Effect - Top */}
        <div className="absolute top-0 left-0 right-0 h-4 bg-gradient-to-b from-gray-800 to-transparent"
          style={{
            backgroundImage: 'radial-gradient(circle at 12px 0, transparent 7px, white 7px)',
            backgroundSize: '24px 100%',
            backgroundRepeat: 'repeat-x'
          }}
        />

        {/* Main Ticket Body */}
        <div className="pt-4 sm:pt-6 pb-3 sm:pb-4 px-4 sm:px-6 bg-gradient-to-br from-white via-blue-50/20 to-indigo-50/30">
          {/* Airline Header */}
          <div className="flex items-center justify-between mb-4 sm:mb-6">
            <div className="flex items-center gap-2 sm:gap-3">
              <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-primary/15 flex items-center justify-center ring-2 ring-primary/10">
                <Plane className="h-4 w-4 sm:h-5 sm:w-5 text-primary" />
              </div>
              <div>
                <div className="text-xs sm:text-sm font-bold text-gray-900">{route.airline}</div>
                <div className="text-xs text-gray-500 font-medium hidden sm:block">BUSINESS CLASS</div>
              </div>
            </div>
            <div className="text-xs text-gray-400 font-mono bg-gray-100 px-2 py-1 rounded">
              FLT-{Math.floor(Math.random() * 9000) + 1000}
            </div>
          </div>

          {/* Route Information */}
          <div className="flex items-center justify-between mb-4 sm:mb-6">
            {/* Origin */}
            <div className="flex-1 min-w-0">
              <div className="text-xs text-gray-600 uppercase tracking-wider mb-1 sm:mb-2 font-semibold">From</div>
              <div className="text-xl sm:text-2xl font-bold text-gray-900 leading-none mb-0.5 sm:mb-1 truncate">{route.origin}</div>
              <div className="text-xs text-gray-500 font-medium hidden sm:block">Departure City</div>
            </div>

            {/* Flight Path */}
            <div className="flex-1 flex items-center justify-center px-3 sm:px-6">
              <div className="relative w-full">
                <div className="h-[2px] sm:h-[3px] bg-gradient-to-r from-primary/30 via-primary to-primary/30 relative rounded-full">
                  <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white p-1 sm:p-1.5 rounded-full shadow-md">
                    <Plane className="h-3 w-3 sm:h-4 sm:w-4 text-primary" />
                  </div>
                </div>
              </div>
            </div>

            {/* Destination */}
            <div className="flex-1 text-right min-w-0">
              <div className="text-xs text-gray-600 uppercase tracking-wider mb-1 sm:mb-2 font-semibold">To</div>
              <div className="text-xl sm:text-2xl font-bold text-gray-900 leading-none mb-0.5 sm:mb-1 truncate">{route.destination}</div>
              <div className="text-xs text-gray-500 font-medium hidden sm:block">Arrival City</div>
            </div>
          </div>

          {/* Barcode Effect */}
          <div className="flex gap-[2px] h-10 sm:h-14 mb-3 sm:mb-4 opacity-50">
            {Array.from({ length: isMobile ? 30 : 50 }).map((_, i) => (
              <div
                key={i}
                className="flex-1 bg-gray-800 rounded-sm"
                style={{ height: `${Math.random() * 60 + 40}%` }}
              />
            ))}
          </div>
        </div>

        {/* Perforated Divider */}
        <div className="relative h-8">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t-2 border-dashed border-gray-300" />
          </div>
          <div className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-1/2 w-8 h-8 bg-gray-100 rounded-full border-4 border-white shadow-sm" />
          <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2 w-8 h-8 bg-gray-100 rounded-full border-4 border-white shadow-sm" />
        </div>

        {/* Stub Section */}
        <div className="px-4 sm:px-6 py-3 sm:py-4 bg-gradient-to-br from-gray-50 to-gray-100/50">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 sm:gap-4">
            <div className="flex-1">
              <div className="text-xs text-gray-600 uppercase tracking-wider mb-1 font-semibold">Starting from</div>
              <div className="text-xl sm:text-2xl font-bold text-primary">
                {route.currency} {route.price.toLocaleString()}
              </div>
              <div className="text-xs text-gray-500 mt-0.5">per person</div>
            </div>
            <Button
              size="md"
              className="w-full sm:w-auto text-sm min-h-[48px] px-6 font-semibold group-hover:bg-primary-hover shadow-md touch-manipulation active:scale-95 transition-transform"
              onClick={handleBookFlight}
            >
              Book Flight
            </Button>
          </div>
        </div>

        {/* Perforated Edge Effect - Bottom */}
        <div className="absolute bottom-0 left-0 right-0 h-4 bg-gradient-to-t from-gray-800 to-transparent"
          style={{
            backgroundImage: 'radial-gradient(circle at 12px 100%, transparent 7px, white 7px)',
            backgroundSize: '24px 100%',
            backgroundRepeat: 'repeat-x'
          }}
        />
      </div>
    </div>
  );
}
