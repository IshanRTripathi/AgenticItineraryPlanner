import { HeroSection } from '../components/homepage/HeroSection';
import { TrendingDestinations } from '../components/homepage/TrendingDestinations';
import { PopularRoutes } from '../components/homepage/PopularRoutes';
import { TravelBlogs } from '../components/homepage/TravelBlogs';
import { Header } from '../components/layout/Header';
import { Footer } from '../components/layout/Footer';

export function HomePage() {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      
      <main>
        {/* Hero Section with Search Widget */}
        <HeroSection />
        
        {/* Trending Destinations */}
        <TrendingDestinations />

        {/* Popular Routes */}
        <PopularRoutes />

        {/* Travel Blogs */}
        <TravelBlogs />
      </main>

      <Footer />
    </div>
  );
}
