/**
 * Hero Section with Video Loop (Emirates-inspired)
 * Premium hero with glass morphism search widget
 */

import { Button } from '@/components/ui/button';
import { Sparkles } from 'lucide-react';

export function HeroSection() {
  return (
    <section className="relative min-h-[600px] flex items-center justify-center overflow-hidden">
      {/* Video Background (placeholder - add actual video) */}
      <div className="absolute inset-0 gradient-hero">
        {/* Animated floating particles */}
        <div className="absolute inset-0">
          <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-white/5 rounded-full blur-3xl animate-pulse" />
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-white/5 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />
        </div>
      </div>

      {/* Content */}
      <div className="relative z-10 container text-center text-white">
        <h1 className="text-5xl md:text-6xl font-bold mb-4 animate-fade-in">
          Plan Your Perfect Trip
        </h1>
        <p className="text-xl md:text-2xl mb-8 text-white/90 animate-fade-in" style={{ animationDelay: '0.2s' }}>
          Discover amazing destinations with AI-powered itineraries
        </p>

        {/* AI Planner CTA */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center animate-fade-in" style={{ animationDelay: '0.4s' }}>
          <Button
            size="lg"
            variant="secondary"
            className="text-lg px-8 py-6 h-auto"
            onClick={() => window.location.href = '/ai-planner'}
          >
            <Sparkles className="mr-2 h-5 w-5" />
            Let AI Plan My Itinerary
          </Button>
          <p className="text-sm text-white/80">
            Get a personalized itinerary in minutes
          </p>
        </div>
      </div>
    </section>
  );
}
