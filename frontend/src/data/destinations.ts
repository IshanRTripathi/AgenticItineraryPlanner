import { PopularDestination } from '../types/TripData';

export const THAILAND_DESTINATIONS: PopularDestination[] = [
  {
    id: 'bangkok',
    name: 'Bangkok',
    country: 'Thailand',
    image: 'https://images.unsplash.com/photo-1563492065-1b5a4d65a75a?w=300&h=200&fit=crop',
    description: 'Vibrant capital city with temples, markets, and street food',
    rating: 4.5,
    visitCount: 15600,
    bestTimeToVisit: 'November to February',
    averageCost: 85,
    currency: 'USD',
    highlights: ['Grand Palace', 'Wat Pho', 'Chatuchak Market', 'Floating Markets'],
    tags: ['Culture', 'Food', 'Shopping', 'Temples'],
    coordinates: {
      lat: 13.7563,
      lng: 100.5018
    }
  },
  {
    id: 'chiang-mai',
    name: 'Chiang Mai',
    country: 'Thailand',
    image: 'https://images.unsplash.com/photo-1519451241324-20b4ea2c4220?w=300&h=200&fit=crop',
    description: 'Cultural hub with ancient temples and mountain adventures',
    rating: 4.6,
    visitCount: 8900,
    bestTimeToVisit: 'October to April',
    averageCost: 65,
    currency: 'USD',
    highlights: ['Wat Phra Singh', 'Night Markets', 'Elephant Sanctuary', 'Doi Suthep'],
    tags: ['Culture', 'Nature', 'Adventure', 'Mountains'],
    coordinates: {
      lat: 18.7883,
      lng: 98.9853
    }
  },
  {
    id: 'phuket',
    name: 'Phuket',
    country: 'Thailand',
    image: 'https://images.unsplash.com/photo-1537953773345-d172ccf13c1d?w=300&h=200&fit=crop',
    description: 'Tropical paradise with pristine beaches and vibrant nightlife',
    rating: 4.3,
    visitCount: 12400,
    bestTimeToVisit: 'November to April',
    averageCost: 95,
    currency: 'USD',
    highlights: ['Patong Beach', 'Phi Phi Islands', 'Big Buddha', 'Old Town'],
    tags: ['Beach', 'Nightlife', 'Islands', 'Water Sports'],
    coordinates: {
      lat: 7.8804,
      lng: 98.3923
    }
  },
  {
    id: 'pai',
    name: 'Pai',
    country: 'Thailand',
    image: 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=300&h=200&fit=crop',
    description: 'Bohemian mountain town with hot springs and scenic valleys',
    rating: 4.4,
    visitCount: 3200,
    bestTimeToVisit: 'November to February',
    averageCost: 45,
    currency: 'USD',
    highlights: ['Pai Canyon', 'Hot Springs', 'Bamboo Bridge', 'White Buddha'],
    tags: ['Mountains', 'Backpacker', 'Nature', 'Relaxation'],
    coordinates: {
      lat: 19.3595,
      lng: 98.4405
    }
  },
  {
    id: 'krabi',
    name: 'Krabi',
    country: 'Thailand',
    image: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=300&h=200&fit=crop',
    description: 'Stunning limestone cliffs and emerald waters',
    rating: 4.5,
    visitCount: 7800,
    bestTimeToVisit: 'November to March',
    averageCost: 80,
    currency: 'USD',
    highlights: ['Railay Beach', 'Ao Nang', 'Tiger Cave Temple', '4 Islands Tour'],
    tags: ['Beach', 'Rock Climbing', 'Islands', 'Adventure'],
    coordinates: {
      lat: 8.0863,
      lng: 98.9063
    }
  },
  {
    id: 'koh-samui',
    name: 'Koh Samui',
    country: 'Thailand',
    image: 'https://images.unsplash.com/photo-1540979388789-6cee28a1cdc9?w=300&h=200&fit=crop',
    description: 'Tropical island with luxury resorts and coconut groves',
    rating: 4.2,
    visitCount: 9600,
    bestTimeToVisit: 'December to April',
    averageCost: 120,
    currency: 'USD',
    highlights: ['Chaweng Beach', 'Big Buddha Temple', 'Fisherman Village', 'Ang Thong Marine Park'],
    tags: ['Beach', 'Luxury', 'Islands', 'Spa'],
    coordinates: {
      lat: 9.5018,
      lng: 100.0028
    }
  }
];

export const JAPAN_DESTINATIONS: PopularDestination[] = [
  {
    id: 'tokyo',
    name: 'Tokyo',
    country: 'Japan',
    image: 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=300&h=200&fit=crop',
    description: 'Ultra-modern metropolis blending tradition and innovation',
    rating: 4.7,
    visitCount: 25400,
    bestTimeToVisit: 'March to May, September to November',
    averageCost: 180,
    currency: 'USD',
    highlights: ['Shibuya Crossing', 'Senso-ji Temple', 'Tsukiji Market', 'Imperial Palace'],
    tags: ['Culture', 'Food', 'Technology', 'Shopping'],
    coordinates: {
      lat: 35.6762,
      lng: 139.6503
    }
  },
  {
    id: 'kyoto',
    name: 'Kyoto',
    country: 'Japan',
    image: 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=300&h=200&fit=crop',
    description: 'Ancient capital with thousands of temples and traditional culture',
    rating: 4.8,
    visitCount: 18900,
    bestTimeToVisit: 'March to May, October to November',
    averageCost: 145,
    currency: 'USD',
    highlights: ['Fushimi Inari', 'Kinkaku-ji', 'Arashiyama Bamboo', 'Gion District'],
    tags: ['Culture', 'Temples', 'History', 'Traditional'],
    coordinates: {
      lat: 35.0116,
      lng: 135.7681
    }
  }
];

export const USA_DESTINATIONS: PopularDestination[] = [
  {
    id: 'new-york',
    name: 'New York City',
    country: 'USA',
    image: 'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=300&h=200&fit=crop',
    description: 'The city that never sleeps with iconic skyline and culture',
    rating: 4.4,
    visitCount: 45200,
    bestTimeToVisit: 'April to June, September to November',
    averageCost: 250,
    currency: 'USD',
    highlights: ['Times Square', 'Central Park', 'Statue of Liberty', 'Brooklyn Bridge'],
    tags: ['Urban', 'Culture', 'Shopping', 'Entertainment'],
    coordinates: {
      lat: 40.7128,
      lng: -74.0060
    }
  }
];

export const getAllDestinations = (country?: string): PopularDestination[] => {
  const allDestinations = [
    ...THAILAND_DESTINATIONS,
    ...JAPAN_DESTINATIONS,
    ...USA_DESTINATIONS
  ];

  if (country && country !== 'all') {
    return allDestinations.filter(dest => 
      dest.country.toLowerCase() === country.toLowerCase()
    );
  }

  return allDestinations;
};

export const calculateDistance = (
  lat1: number, 
  lng1: number, 
  lat2: number, 
  lng2: number
): number => {
  const R = 6371; // Radius of the Earth in km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
    Math.sin(dLng/2) * Math.sin(dLng/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c; // Distance in km
};