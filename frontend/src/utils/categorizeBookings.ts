/**
 * Booking Categorization Utility
 * Intelligently groups itinerary nodes by bookable category
 */

export type BookingCategory = 'transport' | 'accommodation' | 'dining' | 'attractions';

export interface CategorizedBooking {
  id: string;
  title: string;
  type: string;
  category: BookingCategory;
  dayNumber: number;
  dayLocation: string;
  timing?: {
    startTime?: string;
    endTime?: string;
    duration?: string;
  };
  location?: {
    address?: string;
    coordinates?: { lat: number; lng: number };
  };
  cost?: {
    amount?: number;
    currency?: string;
  };
  bookingRef?: string;
  status: 'booked' | 'pending' | 'available';
  details?: any;
}

export interface BookingCategoryGroup {
  category: BookingCategory;
  label: string;
  icon: string;
  items: CategorizedBooking[];
  bookedCount: number;
  pendingCount: number;
  availableCount: number;
}

/**
 * Determine category from node type
 */
export function getCategoryFromType(type: string): BookingCategory {
  const lowerType = type.toLowerCase();
  
  // Transport
  if (['flight', 'train', 'bus', 'car', 'transport', 'transit', 'transfer'].includes(lowerType)) {
    return 'transport';
  }
  
  // Accommodation
  if (['hotel', 'accommodation', 'hostel', 'resort', 'lodging', 'stay'].includes(lowerType)) {
    return 'accommodation';
  }
  
  // Dining
  if (['meal', 'restaurant', 'cafe', 'dining', 'food', 'breakfast', 'lunch', 'dinner'].includes(lowerType)) {
    return 'dining';
  }
  
  // Attractions (default for activities)
  return 'attractions';
}

/**
 * Determine booking status
 */
export function getBookingStatus(node: any): 'booked' | 'pending' | 'available' {
  if (node.bookingRef || node.confirmationNumber) {
    return 'booked';
  }
  // Could add logic for pending bookings from state/API
  return 'available';
}

/**
 * Categorize all bookable items from itinerary
 */
export function categorizeBookings(itinerary: any): BookingCategoryGroup[] {
  const days = itinerary?.itinerary?.days || itinerary?.days || [];
  const allBookings: CategorizedBooking[] = [];
  
  // Extract all nodes from all days
  days.forEach((day: any) => {
    const nodes = day.nodes || [];
    
    nodes.forEach((node: any) => {
      const category = getCategoryFromType(node.type);
      const status = getBookingStatus(node);
      
      allBookings.push({
        id: node.id || `${day.dayNumber}-${node.title}`,
        title: node.title || node.name || 'Untitled',
        type: node.type,
        category,
        dayNumber: day.dayNumber,
        dayLocation: day.location || '',
        timing: node.timing,
        location: node.location,
        cost: node.cost,
        bookingRef: node.bookingRef || node.confirmationNumber,
        status,
        details: node.details,
      });
    });
  });
  
  // Group by category
  const categories: BookingCategory[] = ['transport', 'accommodation', 'dining', 'attractions'];
  const categoryLabels = {
    transport: 'Transport',
    accommodation: 'Accommodation',
    dining: 'Dining',
    attractions: 'Attractions',
  };
  const categoryIcons = {
    transport: '🚗',
    accommodation: '🏨',
    dining: '🍽️',
    attractions: '🎭',
  };
  
  return categories.map(category => {
    const items = allBookings.filter(b => b.category === category);
    const bookedCount = items.filter(b => b.status === 'booked').length;
    const pendingCount = items.filter(b => b.status === 'pending').length;
    const availableCount = items.filter(b => b.status === 'available').length;
    
    return {
      category,
      label: categoryLabels[category],
      icon: categoryIcons[category],
      items,
      bookedCount,
      pendingCount,
      availableCount,
    };
  }).filter(group => group.items.length > 0); // Only show categories with items
}

/**
 * Get color class for category
 */
export function getCategoryColor(category: BookingCategory): string {
  switch (category) {
    case 'transport':
      return 'border-l-green-500';
    case 'accommodation':
      return 'border-l-purple-500';
    case 'dining':
      return 'border-l-orange-500';
    case 'attractions':
      return 'border-l-blue-500';
    default:
      return 'border-l-gray-500';
  }
}

/**
 * Get icon for specific node type
 */
export function getNodeIcon(type: string): string {
  const lowerType = type.toLowerCase();
  
  // Transport
  if (lowerType === 'flight') return '✈️';
  if (lowerType === 'train') return '🚆';
  if (lowerType === 'bus') return '🚌';
  if (lowerType === 'car') return '🚗';
  
  // Accommodation
  if (lowerType === 'hotel') return '🏨';
  if (lowerType === 'hostel') return '🏠';
  if (lowerType === 'resort') return '🏖️';
  
  // Dining
  if (lowerType === 'restaurant') return '🍽️';
  if (lowerType === 'cafe') return '☕';
  if (lowerType === 'meal') return '🍴';
  
  // Attractions
  if (lowerType === 'museum') return '🏛️';
  if (lowerType === 'attraction') return '🎭';
  if (lowerType === 'activity') return '🎯';
  if (lowerType === 'tour') return '🎫';
  
  return '📍';
}
