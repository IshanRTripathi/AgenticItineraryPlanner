/**
 * Trending Destinations Section
 * Grid of popular destinations with scroll animations
 * Design: Apple.com refinement + Emirates.com luxury
 */

import { motion } from 'framer-motion';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { useScrollAnimation } from '@/hooks/useScrollAnimation';

const MOCK_DESTINATIONS = [
  {
    id: 1,
    name: 'Paris',
    country: 'France',
    image: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=400',
    price: 45000,
    tag: 'Popular',
  },
  {
    id: 2,
    name: 'Dubai',
    country: 'UAE',
    image: 'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=400',
    price: 35000,
    tag: 'Trending',
  },
  {
    id: 3,
    name: 'Bali',
    country: 'Indonesia',
    image: 'https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=400',
    price: 40000,
    tag: 'Hot Deal',
  },
  {
    id: 4,
    name: 'Tokyo',
    country: 'Japan',
    image: 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=400',
    price: 55000,
    tag: 'Popular',
  },
  {
    id: 5,
    name: 'London',
    country: 'UK',
    image: 'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=400',
    price: 50000,
    tag: 'Trending',
  },
  {
    id: 6,
    name: 'New York',
    country: 'USA',
    image: 'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=400',
    price: 60000,
    tag: 'Popular',
  },
  {
    id: 7,
    name: 'Singapore',
    country: 'Singapore',
    image: 'https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=400',
    price: 38000,
    tag: 'Hot Deal',
  },
  {
    id: 8,
    name: 'Barcelona',
    country: 'Spain',
    image: 'https://images.unsplash.com/photo-1583422409516-2895a77efded?w=400',
    price: 48000,
    tag: 'Trending',
  },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.5,
      ease: [0.4, 0, 0.2, 1],
    },
  },
};

export function TrendingDestinations() {
  const { ref, isVisible } = useScrollAnimation({ threshold: 0.1 });

  return (
    <section ref={ref as any} className="py-12 bg-muted">
      <div className="container">
        <motion.h2 
          initial={{ opacity: 0, y: -20 }}
          animate={isVisible ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.5 }}
          className="text-3xl font-bold mb-8"
        >
          Trending Destinations
        </motion.h2>
        
        <motion.div 
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"
          variants={containerVariants}
          initial="hidden"
          animate={isVisible ? "visible" : "hidden"}
        >
          {MOCK_DESTINATIONS.map((dest) => (
            <motion.div key={dest.id} variants={itemVariants}>
              <div className="group cursor-pointer">
                <div className="relative overflow-hidden rounded-2xl shadow-md hover:shadow-2xl transition-all duration-500">
                  {/* Image Container with Gradient Overlay */}
                  <div className="relative aspect-[4/3] overflow-hidden">
                    <img
                      src={dest.image}
                      alt={dest.name}
                      className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-out"
                      loading="lazy"
                    />
                    {/* Gradient Overlay */}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent opacity-80 group-hover:opacity-90 transition-opacity duration-300" />
                    
                    {/* Tag Badge */}
                    <div className="absolute top-3 right-3">
                      <div className="bg-white/95 backdrop-blur-sm text-gray-900 text-xs font-semibold px-3 py-1.5 rounded-full shadow-lg">
                        {dest.tag}
                      </div>
                    </div>

                    {/* Content Overlay */}
                    <div className="absolute bottom-0 left-0 right-0 p-4">
                      <div className="flex items-end justify-between">
                        <div className="flex-1">
                          <h3 className="text-white font-bold text-2xl mb-1 drop-shadow-lg">
                            {dest.name}
                          </h3>
                          <p className="text-white/90 text-sm font-medium drop-shadow-md">
                            {dest.country}
                          </p>
                        </div>
                        <div className="text-right">
                          <div className="text-white/80 text-xs mb-0.5">from</div>
                          <div className="text-white font-bold text-xl drop-shadow-lg">
                            ₹{(dest.price / 1000).toFixed(0)}k
                          </div>
                        </div>
                      </div>
                      
                      {/* Explore Button - Appears on Hover */}
                      <div className="mt-3 opacity-0 group-hover:opacity-100 transform translate-y-2 group-hover:translate-y-0 transition-all duration-300">
                        <button className="w-full bg-white text-gray-900 font-semibold text-sm py-2.5 rounded-lg hover:bg-gray-100 transition-colors shadow-lg">
                          Explore Destination
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}
