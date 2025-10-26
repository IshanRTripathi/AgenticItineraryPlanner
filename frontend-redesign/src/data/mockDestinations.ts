/**
 * Mock Destination Data
 * Used for trending destinations section on homepage
 */

export interface Destination {
  id: string;
  name: string;
  country: string;
  description: string;
  imageUrl: string;
  startingPrice: number;
  currency: string;
  rating: number;
  reviewCount: number;
  tags: string[];
}

export const mockDestinations: Destination[] = [
  {
    id: 'paris',
    name: 'Paris',
    country: 'France',
    description: 'The City of Light awaits with iconic landmarks and romantic charm',
    imageUrl: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800&q=80',
    startingPrice: 899,
    currency: 'USD',
    rating: 4.8,
    reviewCount: 12453,
    tags: ['Culture', 'Romance', 'Food']
  },
  {
    id: 'tokyo',
    name: 'Tokyo',
    country: 'Japan',
    description: 'Experience the perfect blend of tradition and futuristic innovation',
    imageUrl: 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&q=80',
    startingPrice: 1299,
    currency: 'USD',
    rating: 4.9,
    reviewCount: 9876,
    tags: ['Culture', 'Technology', 'Food']
  },
  {
    id: 'bali',
    name: 'Bali',
    country: 'Indonesia',
    description: 'Tropical paradise with stunning beaches and ancient temples',
    imageUrl: 'https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800&q=80',
    startingPrice: 699,
    currency: 'USD',
    rating: 4.7,
    reviewCount: 8234,
    tags: ['Beach', 'Nature', 'Relaxation']
  },
  {
    id: 'dubai',
    name: 'Dubai',
    country: 'UAE',
    description: 'Luxury shopping, ultramodern architecture, and desert adventures',
    imageUrl: 'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800&q=80',
    startingPrice: 1099,
    currency: 'USD',
    rating: 4.6,
    reviewCount: 7654,
    tags: ['Luxury', 'Shopping', 'Adventure']
  },
  {
    id: 'santorini',
    name: 'Santorini',
    country: 'Greece',
    description: 'Whitewashed villages and stunning sunsets over the Aegean Sea',
    imageUrl: 'https://images.unsplash.com/photo-1613395877344-13d4a8e0d49e?w=800&q=80',
    startingPrice: 1199,
    currency: 'USD',
    rating: 4.9,
    reviewCount: 6543,
    tags: ['Romance', 'Beach', 'Photography']
  },
  {
    id: 'new-york',
    name: 'New York',
    country: 'USA',
    description: 'The city that never sleeps with endless entertainment and culture',
    imageUrl: 'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&q=80',
    startingPrice: 999,
    currency: 'USD',
    rating: 4.7,
    reviewCount: 15234,
    tags: ['City', 'Culture', 'Shopping']
  },
  {
    id: 'maldives',
    name: 'Maldives',
    country: 'Maldives',
    description: 'Overwater bungalows and crystal-clear turquoise waters',
    imageUrl: 'https://images.unsplash.com/photo-1514282401047-d79a71a590e8?w=800&q=80',
    startingPrice: 1599,
    currency: 'USD',
    rating: 4.9,
    reviewCount: 5432,
    tags: ['Beach', 'Luxury', 'Honeymoon']
  },
  {
    id: 'rome',
    name: 'Rome',
    country: 'Italy',
    description: 'Ancient history meets modern Italian culture and cuisine',
    imageUrl: 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800&q=80',
    startingPrice: 849,
    currency: 'USD',
    rating: 4.8,
    reviewCount: 11234,
    tags: ['History', 'Culture', 'Food']
  }
];
