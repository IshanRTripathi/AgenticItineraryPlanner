/**
 * Trending Destinations Section
 * Grid of popular destinations with hover effects
 */

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

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

export function TrendingDestinations() {
  return (
    <section className="py-16 bg-muted">
      <div className="container">
        <h2 className="text-3xl font-bold mb-8">Trending Destinations</h2>
        
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {MOCK_DESTINATIONS.map((dest) => (
            <Card
              key={dest.id}
              className="overflow-hidden cursor-pointer group hover:-translate-y-1 transition-all duration-normal"
            >
              <div className="relative aspect-[4/3] overflow-hidden">
                <img
                  src={dest.image}
                  alt={dest.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-normal"
                  loading="lazy"
                />
                <Badge
                  variant="secondary"
                  className="absolute top-2 right-2"
                >
                  {dest.tag}
                </Badge>
              </div>
              <CardContent className="p-4">
                <h3 className="font-semibold text-lg">{dest.name}</h3>
                <p className="text-sm text-muted-foreground">{dest.country}</p>
                <div className="mt-3 flex items-center justify-between">
                  <span className="text-sm text-muted-foreground">Starting from</span>
                  <span className="text-xl font-bold text-primary">
                    ₹{dest.price.toLocaleString()}
                  </span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
