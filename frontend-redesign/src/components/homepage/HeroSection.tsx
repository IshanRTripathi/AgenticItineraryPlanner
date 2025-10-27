/**
 * Hero Section with Parallax Effect
 * Premium hero with smooth animations
 * Design: Emirates.com luxury + Apple.com refinement
 */

import { motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Sparkles } from 'lucide-react';
import { useParallax } from '@/hooks/useScrollAnimation';

export function HeroSection() {
  const parallaxOffset = useParallax(0.5);

  return (
    <section className="relative min-h-[600px] flex items-center justify-center overflow-hidden">
      {/* Parallax Background */}
      <motion.div 
        className="absolute inset-0 gradient-hero"
        style={{ y: parallaxOffset }}
      >
        {/* Animated floating particles */}
        <div className="absolute inset-0">
          <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-white/5 rounded-full blur-3xl animate-pulse" />
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-white/5 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />
        </div>
      </motion.div>

      {/* Content with Animations */}
      <div className="relative z-10 container text-center text-white">
        <motion.h1 
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.4, 0, 0.2, 1] }}
          className="text-5xl md:text-6xl font-bold mb-4"
        >
          Plan Your Perfect Trip
        </motion.h1>
        
        <motion.p 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.2, ease: [0.4, 0, 0.2, 1] }}
          className="text-xl md:text-2xl mb-8 text-white/90"
        >
          Discover amazing destinations with AI-powered itineraries
        </motion.p>

        {/* AI Planner CTA */}
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.4, ease: [0.4, 0, 0.2, 1] }}
          className="flex flex-col sm:flex-row gap-4 justify-center items-center"
        >
          <Button
            size="lg"
            variant="secondary"
            className="text-lg px-8 py-6 h-auto hover:scale-105 transition-transform"
            onClick={() => window.location.href = '/ai-planner'}
          >
            <Sparkles className="mr-2 h-5 w-5" />
            Let AI Plan My Itinerary
          </Button>
          <p className="text-sm text-white/80">
            Get a personalized itinerary in minutes
          </p>
        </motion.div>
      </div>
    </section>
  );
}
