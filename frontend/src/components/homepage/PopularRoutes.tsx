import { ArrowRight, Plane } from 'lucide-react';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { mockRoutes } from '../../data/mockRoutes';
import { useNavigate } from 'react-router-dom';

export function PopularRoutes() {
  return (
    <section className="py-12 bg-white">
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

        {/* Route Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {mockRoutes.slice(0, 2).map((route) => (
            <RouteCard key={route.id} route={route} />
          ))}
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
  const navigate = useNavigate();

  const handleBookFlight = () => {
    navigate('/search');
  };

  return (
    <div className="group cursor-pointer">
      {/* Boarding Pass Container */}
      <div className="relative bg-white rounded-xl shadow-lg hover:shadow-2xl transition-all duration-300 hover:-translate-y-1 overflow-hidden border border-gray-200">
        {/* Perforated Edge Effect - Top */}
        <div className="absolute top-0 left-0 right-0 h-4 bg-gradient-to-b from-gray-800 to-transparent"
          style={{
            backgroundImage: 'radial-gradient(circle at 12px 0, transparent 7px, white 7px)',
            backgroundSize: '24px 100%',
            backgroundRepeat: 'repeat-x'
          }}
        />

        {/* Main Ticket Body */}
        <div className="pt-6 pb-4 px-6 bg-gradient-to-br from-white via-blue-50/20 to-indigo-50/30">
          {/* Airline Header */}
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-primary/15 flex items-center justify-center ring-2 ring-primary/10">
                <Plane className="h-5 w-5 text-primary" />
              </div>
              <div>
                <div className="text-sm font-bold text-gray-900">{route.airline}</div>
                <div className="text-xs text-gray-500 font-medium">BUSINESS CLASS</div>
              </div>
            </div>
            <div className="text-xs text-gray-400 font-mono bg-gray-100 px-2 py-1 rounded">
              FLT-{Math.floor(Math.random() * 9000) + 1000}
            </div>
          </div>

          {/* Route Information */}
          <div className="flex items-center justify-between mb-6">
            {/* Origin */}
            <div className="flex-1">
              <div className="text-xs text-gray-600 uppercase tracking-wider mb-2 font-semibold">From</div>
              <div className="text-2xl font-bold text-gray-900 leading-none mb-1">{route.origin}</div>
              <div className="text-xs text-gray-500 font-medium">Departure City</div>
            </div>

            {/* Flight Path */}
            <div className="flex-1 flex items-center justify-center px-6">
              <div className="relative w-full">
                <div className="h-[3px] bg-gradient-to-r from-primary/30 via-primary to-primary/30 relative rounded-full">
                  <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white p-1.5 rounded-full shadow-md">
                    <Plane className="h-4 w-4 text-primary" />
                  </div>
                </div>
              </div>
            </div>

            {/* Destination */}
            <div className="flex-1 text-right">
              <div className="text-xs text-gray-600 uppercase tracking-wider mb-2 font-semibold">To</div>
              <div className="text-2xl font-bold text-gray-900 leading-none mb-1">{route.destination}</div>
              <div className="text-xs text-gray-500 font-medium">Arrival City</div>
            </div>
          </div>

          {/* Barcode Effect */}
          <div className="flex gap-[2px] h-14 mb-4 opacity-50">
            {Array.from({ length: 50 }).map((_, i) => (
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
        <div className="px-6 py-4 bg-gradient-to-br from-gray-50 to-gray-100/50">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-xs text-gray-600 uppercase tracking-wider mb-1 font-semibold">Starting from</div>
              <div className="text-2xl font-bold text-primary">
                {route.currency} {route.price.toLocaleString()}
              </div>
              <div className="text-xs text-gray-500 mt-0.5">per person</div>
            </div>
            <Button
              size="md"
              className="text-sm h-10 px-6 font-semibold group-hover:bg-primary-hover shadow-md"
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
