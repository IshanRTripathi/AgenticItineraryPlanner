import { ArrowRight, Plane } from 'lucide-react';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { mockRoutes } from '../../data/mockRoutes';

export function PopularRoutes() {
  return (
    <section className="py-16 bg-white">
      <div className="container mx-auto px-4">
        {/* Section Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-3xl font-bold text-gray-900">Popular Flight Routes</h2>
            <p className="text-gray-600 mt-2">Best deals on trending destinations</p>
          </div>
          <Button variant="outline" className="hidden md:flex">
            View All Routes
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </div>

        {/* Horizontal Scroll Container */}
        <div className="relative">
          {/* Scroll Container */}
          <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-hide snap-x snap-mandatory">
            {mockRoutes.map((route) => (
              <RouteCard key={route.id} route={route} />
            ))}
          </div>

          {/* Scroll Indicators (Desktop) */}
          <div className="hidden md:flex absolute top-1/2 -translate-y-1/2 left-0 right-0 justify-between pointer-events-none">
            <button
              className="pointer-events-auto w-10 h-10 rounded-full bg-white shadow-lg flex items-center justify-center hover:bg-gray-50 transition-colors -ml-5"
              aria-label="Scroll left"
            >
              <ArrowRight className="h-5 w-5 rotate-180" />
            </button>
            <button
              className="pointer-events-auto w-10 h-10 rounded-full bg-white shadow-lg flex items-center justify-center hover:bg-gray-50 transition-colors -mr-5"
              aria-label="Scroll right"
            >
              <ArrowRight className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Mobile View All Button */}
        <div className="mt-6 md:hidden">
          <Button variant="outline" className="w-full">
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
  return (
    <Card className="flex-shrink-0 w-[280px] snap-start hover:shadow-md transition-all duration-300 hover:-translate-y-1">
      <CardContent className="p-6">
        {/* Airline Logo */}
        <div className="flex items-center gap-3 mb-4">
          <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center">
            <Plane className="h-4 w-4 text-primary" />
          </div>
          <span className="text-sm font-medium text-gray-700">{route.airline}</span>
        </div>

        {/* Route */}
        <div className="flex items-center justify-between mb-4">
          <div className="text-center">
            <div className="text-2xl font-bold text-gray-900">{route.origin}</div>
            <div className="text-xs text-gray-500 mt-1">Origin</div>
          </div>

          <div className="flex-1 mx-4 flex items-center justify-center">
            <div className="h-px bg-gray-300 flex-1"></div>
            <Plane className="h-4 w-4 text-primary mx-2" />
            <div className="h-px bg-gray-300 flex-1"></div>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-gray-900">{route.destination}</div>
            <div className="text-xs text-gray-500 mt-1">Destination</div>
          </div>
        </div>

        {/* Price and CTA */}
        <div className="flex items-center justify-between pt-4 border-t">
          <div>
            <div className="text-xs text-gray-500">Starting from</div>
            <div className="text-xl font-bold text-primary">
              {route.currency}{route.price.toLocaleString()}
            </div>
          </div>
          <Button size="sm">
            View Flights
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
