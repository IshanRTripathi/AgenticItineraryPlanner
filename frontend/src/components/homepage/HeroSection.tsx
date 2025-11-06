/**
 * Premium Hero Section with Realistic 3D Globe
 * Clean, modern design with interactive WebGL globe
 * Mobile-optimized with responsive text and simplified globe
 */

import { lazy, Suspense } from 'react';
import { motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Globe2 } from 'lucide-react';
import { useMediaQuery } from '@/hooks/useMediaQuery';

// Lazy load the heavy globe component
const InteractiveGlobe = lazy(() => import('./InteractiveGlobe').then(module => ({ default: module.InteractiveGlobe })));

export function HeroSection() {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const isTablet = useMediaQuery('(min-width: 768px) and (max-width: 1024px)');
  
  return (
    <section className="relative min-h-[60vh] md:min-h-[75vh] lg:min-h-[85vh] flex items-center overflow-hidden bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      {/* Subtle Background Glow */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl" />
      </div>

      <div className="container relative z-10 px-4 sm:px-6">
        <div className="grid lg:grid-cols-2 gap-8 md:gap-12 lg:gap-16 items-center">
          {/* Left Content */}
          <div className="text-white space-y-4 sm:space-y-6">
            {/* Main Heading */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8 }}
            >
              <h1 className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold leading-tight">
                Plan Your
                <span className="block bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">
                  Perfect Trip
                </span>
              </h1>
            </motion.div>

            {/* Description */}
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.1 }}
              className="text-base sm:text-lg md:text-xl text-gray-300 leading-relaxed max-w-lg"
            >
              AI-powered itineraries tailored to your preferences. Discover destinations, book stays, and explore the world smarter.
            </motion.p>

            {/* CTA Buttons */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.2 }}
              className="flex flex-col sm:flex-row gap-3 sm:gap-4 pt-4"
            >
              <Button
                size="lg"
                className="group relative text-base sm:text-lg px-8 sm:px-10 min-h-[48px] sm:h-14 md:h-16 bg-gradient-to-r from-blue-600 via-purple-600 to-blue-600 bg-size-200 bg-pos-0 hover:bg-pos-100 shadow-2xl shadow-blue-500/50 hover:shadow-purple-500/50 md:hover:scale-105 transition-all duration-500 font-semibold overflow-hidden touch-manipulation active:scale-95"
                onClick={() => window.location.href = '/planner'}
                style={{ backgroundSize: '200% auto' }}
              >
                {/* Shimmer effect */}
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000" />

                <span className="relative">Start Planning</span>
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="text-base sm:text-lg px-6 sm:px-8 min-h-[48px] sm:h-14 border-2 border-white/30 bg-white/5 backdrop-blur-sm hover:bg-white/10 text-white hover:text-white touch-manipulation active:scale-95"
                onClick={() => window.location.href = '/search'}
              >
                Explore
              </Button>
            </motion.div>
          </div>
        </div>
      </div>

      {/* Mobile: Simplified Globe Icon */}
      {isMobile && (
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8, delay: 0.3 }}
          className="absolute top-8 right-8 opacity-20"
        >
          <Globe2 className="w-32 h-32 text-blue-400" />
        </motion.div>
      )}

      {/* Tablet: Smaller Globe */}
      {isTablet && (
        <motion.div
          initial={{ opacity: 0, x: 50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 1, delay: 0.1 }}
          className="absolute right-0 top-0 bottom-0 w-[50%] overflow-hidden"
        >
          <div className="relative h-full w-full flex items-center justify-center">
            <div className="relative w-[600px] h-[600px]">
              <Suspense fallback={
                <div className="w-full h-full flex items-center justify-center">
                  <Globe2 className="w-48 h-48 text-blue-400 opacity-30 animate-pulse" />
                </div>
              }>
                <InteractiveGlobe />
              </Suspense>
            </div>
          </div>
        </motion.div>
      )}

      {/* Desktop: Full Interactive Globe */}
      {!isMobile && !isTablet && (
        <motion.div
          initial={{ opacity: 0, x: 100 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 1, delay: 0.1 }}
          className="absolute right-0 top-0 bottom-0 w-[65%] overflow-visible"
        >
          <div className="relative h-full w-full flex items-center justify-end overflow-visible">
            <div className="relative w-[1400px] h-[1400px] -translate-x-[10%]">
              <Suspense fallback={
                <div className="w-full h-full flex items-center justify-center">
                  <Globe2 className="w-64 h-64 text-blue-400 opacity-30 animate-pulse" />
                </div>
              }>
                <InteractiveGlobe />
              </Suspense>
            </div>
          </div>
        </motion.div>
      )}

      {/* Bottom Gradient Fade - matches bg-muted from TrendingDestinations */}
      <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-muted to-transparent" />
    </section>
  );
}
