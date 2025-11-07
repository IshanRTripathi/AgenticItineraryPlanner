/**
 * Mock Travel Blog Data
 * Used for travel blogs section on homepage
 */

export interface BlogPost {
  id: string;
  title: string;
  excerpt: string;
  category: string;
  imageUrl: string;
  author: string;
  publishDate: string;
  readTime: number; // in minutes
  tags: string[];
}

export const mockBlogs: BlogPost[] = [
  {
    id: 'hidden-gems-paris',
    title: '10 Hidden Gems in Paris You Must Visit',
    excerpt: 'Discover the secret spots that locals love but tourists often miss. From charming cafes to hidden gardens, explore Paris beyond the Eiffel Tower.',
    category: 'Destination Guide',
    imageUrl: 'https://images.unsplash.com/photo-1511739001486-6bfe10ce785f?w=800&q=80',
    author: 'Sophie Martin',
    publishDate: '2025-01-15',
    readTime: 8,
    tags: ['Paris', 'Europe', 'Culture']
  },
  {
    id: 'budget-travel-asia',
    title: 'How to Travel Southeast Asia on $30 a Day',
    excerpt: 'Complete guide to budget travel in Thailand, Vietnam, and Cambodia. Learn how to save money without sacrificing experiences.',
    category: 'Budget Travel',
    imageUrl: 'https://images.unsplash.com/photo-1528181304800-259b08848526?w=800&q=80',
    author: 'Alex Chen',
    publishDate: '2025-01-12',
    readTime: 12,
    tags: ['Asia', 'Budget', 'Backpacking']
  },
  {
    id: 'luxury-maldives',
    title: 'Ultimate Luxury Guide to the Maldives',
    excerpt: 'Experience paradise in style with our guide to the best overwater villas, private islands, and exclusive resorts in the Maldives.',
    category: 'Luxury Travel',
    imageUrl: 'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&q=80',
    author: 'Emma Wilson',
    publishDate: '2025-01-10',
    readTime: 10,
    tags: ['Maldives', 'Luxury', 'Beach']
  },
  {
    id: 'solo-travel-japan',
    title: 'Solo Travel in Japan: A Complete Guide',
    excerpt: 'Everything you need to know about traveling alone in Japan, from navigating trains to finding the best solo-friendly accommodations.',
    category: 'Solo Travel',
    imageUrl: 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800&q=80',
    author: 'Yuki Tanaka',
    publishDate: '2025-01-08',
    readTime: 15,
    tags: ['Japan', 'Solo', 'Culture']
  },
  {
    id: 'food-tour-italy',
    title: 'The Ultimate Food Tour Through Italy',
    excerpt: 'From pizza in Naples to gelato in Florence, discover the best culinary experiences across Italy with our comprehensive food guide.',
    category: 'Food & Travel',
    imageUrl: 'https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?w=800&q=80',
    author: 'Marco Rossi',
    publishDate: '2025-01-05',
    readTime: 11,
    tags: ['Italy', 'Food', 'Culture']
  },
  {
    id: 'adventure-new-zealand',
    title: 'Adventure Activities You Can\'t Miss in New Zealand',
    excerpt: 'Bungee jumping, skydiving, and glacier hiking - experience the adrenaline rush of New Zealand\'s adventure capital.',
    category: 'Adventure',
    imageUrl: 'https://images.unsplash.com/photo-1507699622108-4be3abd695ad?w=800&q=80',
    author: 'James Cooper',
    publishDate: '2025-01-03',
    readTime: 9,
    tags: ['New Zealand', 'Adventure', 'Nature']
  },
  {
    id: 'digital-nomad-bali',
    title: 'Living as a Digital Nomad in Bali',
    excerpt: 'Discover why Bali is the perfect destination for remote workers, with tips on coworking spaces, visa requirements, and best areas to stay.',
    category: 'Digital Nomad',
    imageUrl: 'https://images.unsplash.com/photo-1555400038-63f5ba517a47?w=800&q=80',
    author: 'Sarah Johnson',
    publishDate: '2025-01-01',
    readTime: 13,
    tags: ['Bali', 'Remote Work', 'Lifestyle']
  },
  {
    id: 'family-travel-dubai',
    title: 'Family-Friendly Activities in Dubai',
    excerpt: 'Plan the perfect family vacation in Dubai with our guide to kid-friendly attractions, hotels, and restaurants.',
    category: 'Family Travel',
    imageUrl: 'https://images.unsplash.com/photo-1518684079-3c830dcef090?w=800&q=80',
    author: 'Fatima Al-Rashid',
    publishDate: '2024-12-28',
    readTime: 10,
    tags: ['Dubai', 'Family', 'Luxury']
  },
  {
    id: 'photography-iceland',
    title: 'Photographer\'s Guide to Iceland',
    excerpt: 'Capture the stunning landscapes of Iceland with our guide to the best photo spots, golden hour timing, and essential gear.',
    category: 'Photography',
    imageUrl: 'https://images.unsplash.com/photo-1504893524553-b855bce32c67?w=800&q=80',
    author: 'Lars Eriksson',
    publishDate: '2024-12-25',
    readTime: 14,
    tags: ['Iceland', 'Photography', 'Nature']
  }
];
