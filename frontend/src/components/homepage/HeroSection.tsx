/**
 * Premium Hero Section with Realistic 3D Globe
 * Clean, modern design with interactive WebGL globe
 */

import { motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Sparkles } from 'lucide-react';
import { InteractiveGlobe } from './InteractiveGlobe';

export function HeroSection() {
  return (
    <section className="relative min-h-[85vh] flex items-center overflow-hidden bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      {/* Subtle Background Glow */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl" />
      </div>

      <div className="container relative z-10">
        <div className="grid lg:grid-cols-2 gap-16 items-center">
          {/* Left Content */}
          <div className="text-white space-y-6">
            {/* Main Heading */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8 }}
            >
              <h1 className="text-6xl md:text-7xl font-bold leading-tight">
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
              className="text-xl text-gray-300 leading-relaxed max-w-lg"
            >
              AI-powered itineraries tailored to your preferences. Discover destinations, book stays, and explore the world smarter.
            </motion.p>

            {/* CTA Buttons */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.2 }}
              className="flex flex-col sm:flex-row gap-4 pt-4"
            >
              <Button
                size="lg"
                className="group relative text-lg px-10 h-16 bg-gradient-to-r from-blue-600 via-purple-600 to-blue-600 bg-size-200 bg-pos-0 hover:bg-pos-100 shadow-2xl shadow-blue-500/50 hover:shadow-purple-500/50 hover:scale-105 transition-all duration-500 font-semibold overflow-hidden"
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
                className="text-lg px-8 h-14 border-2 border-white/30 bg-white/5 backdrop-blur-sm hover:bg-white/10 text-white hover:text-white"
                onClick={() => window.location.href = '/search'}
              >
                Explore
              </Button>
            </motion.div>
          </div>
        </div>
      </div>

      {/* 
        GLOBE POSITIONING:
        - Absolutely positioned on the right side of the hero section
        - Takes 55% width to give it more space
        - Stretches full height (top-0 bottom-0) for vertical centering
      */}
      <motion.div
        initial={{ opacity: 10, x: -100 }}
        animate={{ opacity: 100, x: 100 }}
        transition={{ duration: 1, delay: 0.1 }}
        className="hidden lg:block absolute right-0 top-0 bottom-0 w-[65%] overflow-visible"
      >
        <div className="relative h-full w-full flex items-center justify-end overflow-visible">
          {/* 
            GLOBE CONTAINER:
            - Size: 1400x1400px (MASSIVE for maximum impact)
            - translate-x-[35%]: Pushes 35% to the right (positive = right direction)
            - overflow-visible ensures no size constraints
            - Mouse interaction fully enabled for rotation control
          */}
          <div className="relative w-[1400px] h-[1400px] -translate-x-[10%]">
            <InteractiveGlobe />
          </div>
        </div>
      </motion.div>

      {/* Bottom Gradient Fade - matches bg-muted from TrendingDestinations */}
      <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-muted to-transparent" />
    </section>
  );
}
