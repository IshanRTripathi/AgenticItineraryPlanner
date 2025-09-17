// Core trip data types with comprehensive metadata
export interface TripLocation {
  id: string;
  name: string;
  country: string;
  city: string;
  coordinates: {
    lat: number;
    lng: number;
  };
  timezone: string;
  currency: string;
  exchangeRate: number;
}

export interface TravelPreferences {
  heritage: number; // 0-100 slider value
  nightlife: number;
  adventure: number;
  relaxation: number;
  culture: number;
  nature: number;
  shopping: number;
  cuisine: number;
  photography: number;
  spiritual: number;
}

export interface TripSettings {
  womenFriendly: boolean;
  petFriendly: boolean;
  veganOnly: boolean;
  wheelchairAccessible: boolean;
  budgetFriendly: boolean;
  luxuryOnly: boolean;
  familyFriendly: boolean;
  soloTravelSafe: boolean;
}

export interface Traveler {
  id: string;
  name: string;
  email: string;
  age: number;
  preferences?: {
    dietaryRestrictions: string[];
    mobilityNeeds: string[];
    interests: string[];
  };
}

export interface TripComponent {
  id: string;
  type: 'attraction' | 'hotel' | 'restaurant' | 'activity' | 'transport' | 'shopping' | 'entertainment';
  name: string;
  description: string;
  location: {
    name: string;
    address: string;
    coordinates: {
      lat: number;
      lng: number;
    };
  };
  timing: {
    startTime: string; // ISO string
    endTime: string;
    duration: number; // minutes
    suggestedDuration: number;
  };
  cost: {
    pricePerPerson: number;
    currency: string;
    priceRange: 'budget' | 'mid-range' | 'luxury';
    includesWhat: string[];
    additionalCosts?: string[];
  };
  travel: {
    distanceFromPrevious: number; // km
    travelTimeFromPrevious: number; // minutes
    transportMode: 'walking' | 'taxi' | 'bus' | 'train' | 'flight' | 'car';
    transportCost: number;
  };
  details: {
    rating: number; // 1-5
    reviewCount: number;
    category: string;
    tags: string[];
    openingHours: {
      [day: string]: { open: string; close: string } | null;
    };
    contact: {
      phone?: string;
      website?: string;
      email?: string;
    };
    accessibility: {
      wheelchairAccessible: boolean;
      elevatorAccess: boolean;
      restrooms: boolean;
      parking: boolean;
    };
    amenities: string[];
  };
  booking: {
    required: boolean;
    bookingUrl?: string;
    phone?: string;
    notes?: string;
  };
  media: {
    images: string[];
    videos?: string[];
    virtualTour?: string;
  };
  tips: {
    bestTimeToVisit: string;
    whatToBring: string[];
    insider: string[];
    warnings: string[];
  };
  alternatives?: {
    id: string;
    name: string;
    reason: string;
  }[];
  userNotes?: string;
  isCustom?: boolean;
  addedByUser?: boolean;
  priority: 'must-visit' | 'recommended' | 'optional' | 'backup';
}

export interface DayPlan {
  id: string;
  date: string; // YYYY-MM-DD
  dayNumber: number;
  theme: string;
  location: string; // main city/area for the day
  components: TripComponent[];
  totalDistance: number;
  totalCost: number;
  totalDuration: number;
  startTime: string;
  endTime: string;
  meals: {
    breakfast?: TripComponent;
    lunch?: TripComponent;
    dinner?: TripComponent;
    snacks?: TripComponent[];
  };
  accommodation?: TripComponent;
  weather: {
    temperature: { min: number; max: number };
    condition: string;
    precipitation: number;
  };
  notes?: string;
}

export interface TripItinerary {
  id: string;
  days: DayPlan[];
  totalCost: number;
  totalDistance: number;
  totalDuration: number; // days
  highlights: string[];
  themes: string[];
  difficulty: 'easy' | 'moderate' | 'challenging';
  packingList: {
    category: string;
    items: {
      name: string;
      quantity: number;
      essential: boolean;
      notes?: string;
    }[];
  }[];
  emergencyInfo: {
    hospitals: TripComponent[];
    embassies: TripComponent[];
    police: TripComponent[];
    helplines: { name: string; number: string }[];
  };
  localInfo: {
    currency: string;
    language: string;
    timeZone: string;
    customs: string[];
    etiquette: string[];
    laws: string[];
  };
}

export interface TripData {
  id: string;
  
  // Basic trip info
  startLocation: TripLocation;
  endLocation: TripLocation;
  isRoundTrip: boolean;
  dates: {
    start: string; // ISO date
    end: string;
  };
  
  // Travelers
  travelers: Traveler[];
  leadTraveler: Traveler;
  
  // Budget
  budget: {
    total: number;
    currency: string;
    breakdown: {
      accommodation: number;
      food: number;
      transport: number;
      activities: number;
      shopping: number;
      emergency: number;
    };
  };
  
  // Preferences
  preferences: TravelPreferences;
  settings: TripSettings;
  
  // Generated itinerary
  itinerary?: TripItinerary;
  
  // Booking and status
  status: 'draft' | 'planning' | 'booked' | 'completed' | 'cancelled';
  createdAt: string;
  updatedAt: string;
  
  // Agent data
  agentProgress?: {
    [agentId: string]: {
      status: 'queued' | 'running' | 'completed' | 'failed';
      progress: number;
      message: string;
      startTime?: string;
      endTime?: string;
    };
  };
  
  // Sharing
  isPublic: boolean;
  shareCode?: string;
  
  // Booking data
  bookings?: {
    id: string;
    type: string;
    status: 'pending' | 'confirmed' | 'cancelled';
    confirmationNumber: string;
    cost: number;
    componentId: string;
  }[];
}

// Popular destinations data
export interface PopularDestination {
  id: string;
  name: string;
  country: string;
  image: string;
  description: string;
  rating: number;
  visitCount: number;
  bestTimeToVisit: string;
  averageCost: number;
  currency: string;
  highlights: string[];
  tags: string[];
  coordinates: {
    lat: number;
    lng: number;
  };
  distanceFromSelected?: number;
}

// Agent types
export interface AgentTask {
  id: string;
  name: string;
  description: string;
  icon: string;
  estimatedDuration: number; // seconds
  dependencies?: string[];
}

export const AGENT_TASKS: AgentTask[] = [
  {
    id: 'destination-analysis',
    name: 'Destination Analysis',
    description: 'Analyzing destination and gathering local insights',
    icon: '🗺️',
    estimatedDuration: 3000
  },
  {
    id: 'preference-matching',
    name: 'Preference Matching',
    description: 'Matching your preferences with available activities',
    icon: '🎯',
    estimatedDuration: 3000
  },
  {
    id: 'route-optimization',
    name: 'Route Optimization',
    description: 'Creating optimal routes between locations',
    icon: '🛣️',
    estimatedDuration: 3000,
    dependencies: ['destination-analysis']
  },
  {
    id: 'accommodation-search',
    name: 'Accommodation Search',
    description: 'Finding the best places to stay',
    icon: '🏨',
    estimatedDuration: 3000
  },
  {
    id: 'activity-curation',
    name: 'Activity Curation',
    description: 'Curating activities based on your interests',
    icon: '🎭',
    estimatedDuration: 3000,
    dependencies: ['preference-matching']
  },
  {
    id: 'dining-recommendations',
    name: 'Dining Recommendations',
    description: 'Finding restaurants and local cuisine',
    icon: '🍽️',
    estimatedDuration: 3000
  },
  {
    id: 'budget-optimization',
    name: 'Budget Optimization',
    description: 'Optimizing costs within your budget',
    icon: '💰',
    estimatedDuration: 3000
  },
  {
    id: 'final-assembly',
    name: 'Final Assembly',
    description: 'Putting together your perfect itinerary',
    icon: '✨',
    estimatedDuration: 3000,
    dependencies: ['route-optimization', 'activity-curation', 'dining-recommendations', 'budget-optimization']
  }
];