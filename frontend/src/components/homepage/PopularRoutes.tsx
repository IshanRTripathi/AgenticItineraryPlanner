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
import { useTranslation } from '@/i18n';

export function PopularRoutes() {
  const { t } = useTranslation();
  const isMobile = useMediaQuery('(max-width: 768px)');

  return (
    <section className="py-6 sm:py-10 md:py-12 bg-white">
      <div className="container mx-auto">
        {/* Section Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 sm:gap-4 mb-4 sm:mb-8 px-4">
          <div className="flex-1">
            <h2 className="text-xl sm:text-3xl font-bold text-gray-900">{t('pages.home.popularRoutes.title')}</h2>
            <p className="text-xs sm:text-base text-gray-600 mt-1">{t('pages.home.popularRoutes.subtitle')}</p>
          </div>
          <Button variant="outline" className="hidden md:flex min-h-[44px] touch-manipulation">
            {t('pages.home.popularRoutes.viewAll')}
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </div>

        {/* Route Cards - Horizontal scroll on mobile, Grid on desktop */}
        <div className={`
          ${isMobile 
            ? 'flex overflow-x-auto gap-2.5 pb-4 scrollbar-hide snap-x snap-mandatory px-4' 
            : 'grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-5 md:gap-6 px-4'
          }
        `}>
          {mockRoutes.slice(0, isMobile ? 4 : 2).map((route) => (
            <div key={route.id} className={isMobile ? 'flex-shrink-0 w-[280px] snap-start' : ''}>
              <RouteCard route={route} />
            </div>
          ))}
        </div>

        {/* Mobile View All Button */}
        <div className="mt-4 sm:mt-6 md:hidden px-4">
          <Button variant="outline" className="w-full min-h-[48px] touch-manipulation active:scale-95 transition-transform">
            {t('pages.home.popularRoutes.viewAll')}
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
  const { t } = useTranslation();
  const isMobile = useMediaQuery('(max-width: 768px)');
  const navigate = useNavigate();

  const handleBookFlight = () => {
    navigate('/search');
  };

  return (
    <div className="group cursor-pointer touch-manipulation active:scale-[0.98] transition-transform h-full">
      {/* Boarding Pass Container */}
      <div className="relative bg-white rounded-lg sm:rounded-xl shadow-lg hover:shadow-2xl transition-all duration-300 hover:-translate-y-1 overflow-hidden border border-gray-200 h-full">
        {/* Perforated Edge Effect - Top */}
        <div className="absolute top-0 left-0 right-0 h-3 sm:h-4 bg-gradient-to-b from-gray-800 to-transparent"
          style={{
            backgroundImage: 'radial-gradient(circle at 12px 0, transparent 7px, white 7px)',
            backgroundSize: '24px 100%',
            backgroundRepeat: 'repeat-x'
          }}
        />

        {/* Main Ticket Body */}
        <div className="pt-3 sm:pt-6 pb-2 sm:pb-4 px-3 sm:px-6 bg-gradient-to-br from-white via-blue-50/20 to-indigo-50/30">
          {/* Airline Header */}
          <div className="flex items-center justify-between mb-3 sm:mb-6">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 sm:w-10 sm:h-10 rounded-full bg-primary/15 flex items-center justify-center ring-2 ring-primary/10">
                <Plane className="h-3 w-3 sm:h-5 sm:w-5 text-primary" />
              </div>
              <div>
                <div className="text-[11px] sm:text-sm font-bold text-gray-900">{route.airline}</div>
                <div className="text-[9px] sm:text-xs text-gray-500 font-medium hidden sm:block">BUSINESS CLASS</div>
              </div>
            </div>
            <div className="text-[9px] sm:text-xs text-gray-400 font-mono bg-gray-100 px-1.5 sm:px-2 py-0.5 sm:py-1 rounded">
              FLT-{Math.floor(Math.random() * 9000) + 1000}
            </div>
          </div>

          {/* Route Information */}
          <div className="flex items-center justify-between mb-3 sm:mb-6">
            {/* Origin */}
            <div className="flex-1 min-w-0">
              <div className="text-[9px] sm:text-xs text-gray-600 uppercase tracking-wider mb-1 font-semibold">{t('pages.home.popularRoutes.from')}</div>
              <div className="text-base sm:text-2xl font-bold text-gray-900 leading-none mb-0.5 truncate">{route.origin}</div>
            </div>

            {/* Flight Path */}
            <div className="flex-1 flex items-center justify-center px-2 sm:px-6">
              <div className="relative w-full">
                <div className="h-[2px] sm:h-[3px] bg-gradient-to-r from-primary/30 via-primary to-primary/30 relative rounded-full">
                  <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white p-0.5 sm:p-1.5 rounded-full shadow-md">
                    <Plane className="h-2 w-2 sm:h-4 sm:w-4 text-primary" />
                  </div>
                </div>
              </div>
            </div>

            {/* Destination */}
            <div className="flex-1 text-right min-w-0">
              <div className="text-[9px] sm:text-xs text-gray-600 uppercase tracking-wider mb-1 font-semibold">{t('pages.home.popularRoutes.to')}</div>
              <div className="text-base sm:text-2xl font-bold text-gray-900 leading-none mb-0.5 truncate">{route.destination}</div>
            </div>
          </div>

          {/* Barcode Effect */}
          <div className="flex gap-[1px] sm:gap-[2px] h-8 sm:h-14 mb-2 sm:mb-4 opacity-50">
            {Array.from({ length: isMobile ? 25 : 50 }).map((_, i) => (
              <div
                key={i}
                className="flex-1 bg-gray-800 rounded-sm"
                style={{ height: `${Math.random() * 60 + 40}%` }}
              />
            ))}
          </div>
        </div>

        {/* Perforated Divider */}
        <div className="relative h-6 sm:h-8">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t-2 border-dashed border-gray-300" />
          </div>
          <div className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-1/2 w-6 h-6 sm:w-8 sm:h-8 bg-gray-100 rounded-full border-2 sm:border-4 border-white shadow-sm" />
          <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2 w-6 h-6 sm:w-8 sm:h-8 bg-gray-100 rounded-full border-2 sm:border-4 border-white shadow-sm" />
        </div>

        {/* Stub Section */}
        <div className="px-3 sm:px-6 py-2 sm:py-4 bg-gradient-to-br from-gray-50 to-gray-100/50">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 sm:gap-4">
            <div className="flex-1">
              <div className="text-[9px] sm:text-xs text-gray-600 uppercase tracking-wider mb-0.5 sm:mb-1 font-semibold">{t('pages.home.popularRoutes.startingFrom')}</div>
              <div className="text-base sm:text-2xl font-bold text-primary">
                {route.currency} {route.price.toLocaleString()}
              </div>
              <div className="text-[9px] sm:text-xs text-gray-500 mt-0.5">{t('pages.home.popularRoutes.perPerson')}</div>
            </div>
            <Button
              size="sm"
              className="w-full sm:w-auto text-xs sm:text-sm min-h-[40px] sm:min-h-[48px] px-4 sm:px-6 font-semibold group-hover:bg-primary-hover shadow-md touch-manipulation active:scale-95 transition-transform"
              onClick={handleBookFlight}
            >
              {t('pages.home.popularRoutes.bookFlight')}
            </Button>
          </div>
        </div>

        {/* Perforated Edge Effect - Bottom */}
        <div className="absolute bottom-0 left-0 right-0 h-3 sm:h-4 bg-gradient-to-t from-gray-800 to-transparent"
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
