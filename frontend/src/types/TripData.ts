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
  summary?: string;
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
  
  // Agent-specific results
  agentResults?: {
    flights?: FlightOption[];
    hotels?: HotelOption[];
    restaurants?: RestaurantOption[];
    places?: PlaceOption[];
    transport?: TransportOption[];
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
  
  // Additional properties expected by components
  destination?: string; // Computed from endLocation.name
  partySize?: number; // Computed from travelers.length
  dietaryRestrictions?: string[]; // Computed from travelers preferences
  walkingTolerance?: number; // 1-5 scale
  pace?: number; // 1-5 scale
  stayType?: 'Hotel' | 'Airbnb' | 'Hostel' | 'Resort';
  transport?: 'Walking' | 'Public' | 'Private' | 'Mixed';
  themes?: string[]; // Computed from preferences
  bookingData?: {
    bookingReference: string;
    status: 'pending' | 'confirmed' | 'completed' | 'cancelled';
  };
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
    id: 'planner',
    name: 'Planner Agent',
    description: 'Creating your personalized itinerary with activities, accommodations, and recommendations',
    icon: '📋',
    estimatedDuration: 3000
  }
];

// Agent-specific result types
export interface FlightOption {
  id: string;
  airline: string;
  flightNumber: string;
  departure: {
    airport: string;
    time: string;
    terminal?: string;
  };
  arrival: {
    airport: string;
    time: string;
    terminal?: string;
  };
  duration: string;
  stops: number;
  price: number;
  currency: string;
  category: 'cost-effective' | 'fastest' | 'optimal';
  features: string[];
  bookingUrl?: string;
  holdAvailable: boolean;
  baggage: {
    included: boolean;
    weight: string;
  };
}

export interface HotelOption {
  id: string;
  name: string;
  rating: number;
  price: number;
  currency: string;
  category: 'budget' | 'mid-range' | 'luxury';
  location: {
    address: string;
    coordinates: { lat: number; lng: number };
    distanceToAttractions: number; // km
  };
  amenities: string[];
  roomType: string;
  availability: boolean;
  bookingUrl?: string;
  images: string[];
  reviews: {
    count: number;
    average: number;
  };
}

export interface RestaurantOption {
  id: string;
  name: string;
  cuisine: string;
  rating: number;
  priceRange: 'budget' | 'mid-range' | 'upscale';
  location: {
    address: string;
    coordinates: { lat: number; lng: number };
  };
  dietaryOptions: string[];
  openingHours: string;
  features: string[];
  category: 'local' | 'dietary-friendly' | 'budget' | 'fine-dining';
  bookingRequired: boolean;
  images: string[];
}

export interface PlaceOption {
  id: string;
  name: string;
  type: 'attraction' | 'landmark' | 'museum' | 'park' | 'viewpoint';
  rating: number;
  location: {
    address: string;
    coordinates: { lat: number; lng: number };
  };
  openingHours: string;
  entryFee: number;
  currency: string;
  crowdLevel: 'low' | 'moderate' | 'high';
  bestTimeToVisit: string;
  duration: number; // minutes
  category: 'must-visit' | 'hidden-gem' | 'time-optimized';
  features: string[];
  images: string[];
}

export interface TransportOption {
  id: string;
  type: 'public' | 'private' | 'multi-modal';
  name: string;
  cost: number;
  currency: string;
  duration: number; // minutes
  convenience: 'high' | 'medium' | 'low';
  accessibility: boolean;
  features: string[];
  route: {
    from: string;
    to: string;
    stops?: string[];
  };
  schedule?: string;
  bookingRequired: boolean;
}