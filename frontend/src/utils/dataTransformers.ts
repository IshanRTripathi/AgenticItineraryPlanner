/**
 * Data transformation utilities to handle conversion between TripData and NormalizedItinerary structures
 * This resolves the critical mismatch between frontend expectations and backend responses
 */
import { TripData, DayPlan, TripComponent } from '../types/TripData';
import { NormalizedItinerary, NormalizedDay, NormalizedNode } from '../types/NormalizedItinerary';

/**
 * Converts NormalizedItinerary (backend format) to TripData (frontend format)
 * This allows existing components to continue working with familiar structure
 */
export function normalizedItineraryToTripData(normalized: NormalizedItinerary): TripData {
  if (!normalized) {
    throw new Error('NormalizedItinerary is required');
  }

  // Create a minimal TripData structure that satisfies the interface
  const tripData: TripData = {
    id: normalized.itineraryId,
    
    // Basic trip info - create minimal required data
    startLocation: {
      id: 'start',
      name: normalized.destination || 'Start Location',
      country: '',
      city: normalized.destination || '',
      coordinates: { lat: 0, lng: 0 },
      timezone: 'UTC',
      currency: normalized.currency || 'EUR',
      exchangeRate: 1
    },
    
    endLocation: {
      id: 'end', 
      name: normalized.destination || 'End Location',
      country: '',
      city: normalized.destination || '',
      coordinates: { lat: 0, lng: 0 },
      timezone: 'UTC',
      currency: normalized.currency || 'EUR',
      exchangeRate: 1
    },
    
    isRoundTrip: true,
    summary: normalized.summary,
    dates: {
      start: normalized.startDate || new Date().toISOString(),
      end: normalized.endDate || new Date().toISOString()
    },
    
    // Travelers - create minimal data
    travelers: [{
      id: 'traveler1',
      name: 'Traveler',
      email: 'traveler@example.com',
      age: 30
    }],
    
    leadTraveler: {
      id: 'traveler1',
      name: 'Traveler', 
      email: 'traveler@example.com',
      age: 30
    },
    
    // Budget - create minimal data
    budget: {
      total: 1000,
      currency: normalized.currency || 'EUR',
      breakdown: {
        accommodation: 400,
        food: 300,
        transport: 200,
        activities: 100,
        shopping: 0,
        emergency: 0
      }
    },
    
    // Preferences - create minimal data
    preferences: {
      heritage: 50,
      nightlife: 50,
      adventure: 50,
      relaxation: 50,
      culture: 50,
      nature: 50,
      shopping: 50,
      cuisine: 50,
      photography: 50,
      spiritual: 50
    },
    
    settings: {
      womenFriendly: true,
      petFriendly: false,
      veganOnly: false,
      wheelchairAccessible: false,
      budgetFriendly: true,
      luxuryOnly: false,
      familyFriendly: true,
      soloTravelSafe: true
    },
    
    // The key part - convert the itinerary structure
    itinerary: {
      id: normalized.itineraryId,
      days: normalized.days?.map(normalizedDayToDayPlan) || [],
      totalCost: 0,
      totalDistance: 0,
      totalDuration: normalized.days?.length || 0,
      highlights: normalized.themes || [],
      themes: normalized.themes || [],
      difficulty: 'moderate' as const,
      packingList: [],
      emergencyInfo: {
        hospitals: [],
        embassies: [],
        police: [],
        helplines: []
      },
      localInfo: {
        currency: normalized.currency || 'EUR',
        language: 'English',
        timeZone: 'UTC',
        customs: [],
        etiquette: [],
        laws: []
      }
    },
    
    status: 'planning' as const,
    createdAt: new Date(normalized.createdAt || Date.now()).toISOString(),
    updatedAt: new Date(normalized.updatedAt || Date.now()).toISOString(),
    isPublic: false
  };

  return tripData;
}

/**
 * Converts NormalizedDay to DayPlan
 */
function normalizedDayToDayPlan(normalizedDay: NormalizedDay): DayPlan {
  return {
    id: `day-${normalizedDay.dayNumber}`,
    date: normalizedDay.date,
    dayNumber: normalizedDay.dayNumber,
    theme: 'Explore',
    location: normalizedDay.location || 'Unknown Location',
    components: normalizedDay.nodes?.map(normalizedNodeToTripComponent) || [],
    totalDistance: 0,
    totalCost: 0,
    totalDuration: 8,
    startTime: '09:00',
    endTime: '18:00',
    meals: {
      breakfast: undefined,
      lunch: undefined,
      dinner: undefined,
      snacks: []
    },
    weather: {
      temperature: { min: 15, max: 25 },
      condition: 'Clear',
      precipitation: 0
    }
  };
}

/**
 * Converts NormalizedNode to TripComponent
 * This is the critical mapping that resolves property access issues
 */
function normalizedNodeToTripComponent(node: NormalizedNode): TripComponent {
  return {
    id: node.id,
    type: mapNodeTypeToTripComponentType(node.type),
    name: node.title, // CRITICAL: node.title → component.name
    description: node.details?.category || '',
    
    location: {
      name: node.location?.name || 'Unknown Location',
      address: node.location?.address || '',
      coordinates: {
        lat: node.location?.coordinates?.lat || null,
        lng: node.location?.coordinates?.lng || null
      }
    },
    
    timing: {
      startTime: node.timing?.startTime ? new Date(node.timing.startTime).toISOString() : new Date().toISOString(),
      endTime: node.timing?.endTime ? new Date(node.timing.endTime).toISOString() : new Date().toISOString(),
      duration: node.timing?.durationMin || 120,
      suggestedDuration: node.timing?.durationMin || 120
    },
    
    cost: {
      pricePerPerson: node.cost?.amount || 0,
      currency: node.cost?.currency || 'EUR',
      priceRange: 'mid-range' as const,
      includesWhat: [],
      additionalCosts: []
    },
    
    travel: {
      distanceFromPrevious: 0,
      travelTimeFromPrevious: 0,
      transportMode: 'walking' as const,
      transportCost: 0
    },
    
    details: {
      rating: node.details?.rating || 0,
      reviewCount: 0,
      category: node.details?.category || node.type,
      tags: node.details?.tags || [],
      openingHours: {},
      contact: {
        phone: node.links?.phone,
        website: node.links?.website
      },
      accessibility: {
        wheelchairAccessible: false,
        elevatorAccess: false,
        restrooms: false,
        parking: false
      },
      amenities: []
    },
    
    booking: {
      required: node.status === 'planned',
      bookingUrl: node.links?.booking,
      notes: ''
    },
    
    media: {
      images: [],
      videos: [],
      virtualTour: undefined
    },
    
    tips: {
      bestTimeToVisit: node.tips?.bestTime?.[0] || '',
      whatToBring: [],
      insider: [],
      warnings: node.tips?.warnings || []
    },
    
    priority: 'recommended' as const,
    locked: node.locked === true // KEY MAPPING: Preserve lock state
  };
}

/**
 * Maps NormalizedNode types to TripComponent types
 */
function mapNodeTypeToTripComponentType(nodeType: string): TripComponent['type'] {
  const typeMap: Record<string, TripComponent['type']> = {
    'attraction': 'attraction',
    'meal': 'restaurant',
    'hotel': 'hotel',
    'accommodation': 'hotel',
    'transit': 'transport',
    'transport': 'transport',
    'activity': 'activity'
  };
  
  return typeMap[nodeType] || 'attraction';
}

/**
 * Converts TripData (frontend format) to NormalizedItinerary (backend format)
 * This allows sending data back to backend in expected format
 */
export function tripDataToNormalizedItinerary(tripData: TripData): NormalizedItinerary {
  if (!tripData) {
    throw new Error('TripData is required');
  }

  const normalized: NormalizedItinerary = {
    itineraryId: tripData.id,
    summary: tripData.summary || '',
    destination: tripData.startLocation?.city || '',
    startDate: tripData.dates.start,
    endDate: tripData.dates.end,
    currency: tripData.budget?.currency || 'EUR',
    themes: tripData.itinerary?.themes || [],
    days: tripData.itinerary?.days?.map(dayPlanToNormalizedDay) || [],
    
    // Initialize required fields
    version: 1,
    userId: 'user',
    createdAt: new Date(tripData.createdAt).getTime(),
    updatedAt: new Date(tripData.updatedAt).getTime(),
    
    settings: {
      autoApply: false,
      defaultScope: 'trip'
    },
    
    agents: {}
  };

  return normalized;
}

/**
 * Converts DayPlan to NormalizedDay
 */
function dayPlanToNormalizedDay(dayPlan: DayPlan): NormalizedDay {
  return {
    date: dayPlan.date,
    dayNumber: dayPlan.dayNumber,
    location: dayPlan.location,
    nodes: dayPlan.components?.map(tripComponentToNormalizedNode) || []
  };
}

/**
 * Converts TripComponent to NormalizedNode
 */
function tripComponentToNormalizedNode(component: TripComponent): NormalizedNode {
  return {
    id: component.id,
    type: mapTripComponentTypeToNodeType(component.type),
    title: component.name, // CRITICAL: component.name → node.title
    
    location: component.location ? {
      name: component.location.name,
      address: component.location.address,
      coordinates: {
        lat: component.location.coordinates.lat || 0,
        lng: component.location.coordinates.lng || 0
      }
    } : undefined,
    
    timing: {
      startTime: new Date(component.timing.startTime).getTime(),
      endTime: new Date(component.timing.endTime).getTime(),
      durationMin: component.timing.duration
    },
    
    cost: {
      amount: component.cost.pricePerPerson,
      currency: component.cost.currency,
      per: 'person'
    },
    
    details: {
      rating: component.details.rating,
      category: component.details.category,
      tags: component.details.tags
    },
    
    tips: {
      bestTime: component.tips.bestTimeToVisit ? [component.tips.bestTimeToVisit] : undefined,
      warnings: component.tips.warnings
    },
    
    links: {
      booking: component.booking.bookingUrl,
      website: component.details.contact.website,
      phone: component.details.contact.phone
    },
    
    locked: component.locked === true, // KEY MAPPING: Preserve lock state
    status: component.booking.required ? 'planned' : 'completed'
  };
}

/**
 * Maps TripComponent types to NormalizedNode types
 */
function mapTripComponentTypeToNodeType(componentType: TripComponent['type']): NormalizedNode['type'] {
  const typeMap: Record<TripComponent['type'], NormalizedNode['type']> = {
    'attraction': 'attraction',
    'restaurant': 'meal',
    'hotel': 'accommodation',
    'activity': 'attraction',
    'transport': 'transport',
    'shopping': 'attraction',
    'entertainment': 'attraction'
  };
  
  return typeMap[componentType] || 'attraction';
}

/**
 * Validates that a structure is NormalizedItinerary format
 */
export function isNormalizedItinerary(data: any): data is NormalizedItinerary {
  return data && 
         typeof data === 'object' && 
         Array.isArray(data.days) &&
         (data.days.length === 0 || (data.days[0] && Array.isArray(data.days[0].nodes)));
}

/**
 * Validates that a structure is TripData format
 */
export function isTripData(data: any): data is TripData {
  return data && 
         typeof data === 'object' && 
         data.itinerary &&
         Array.isArray(data.itinerary.days) &&
         (data.itinerary.days.length === 0 || (data.itinerary.days[0] && Array.isArray(data.itinerary.days[0].components)));
}

/**
 * Safely converts any itinerary data to TripData format
 * Handles both NormalizedItinerary and TripData inputs
 */
export function ensureTripDataFormat(data: any): TripData {
  if (!data) {
    throw new Error('Itinerary data is required');
  }
  
  if (isTripData(data)) {
    return data;
  }
  
  if (isNormalizedItinerary(data)) {
    return normalizedItineraryToTripData(data);
  }
  
  throw new Error('Invalid itinerary data format');
}

/**
 * Safely converts any itinerary data to NormalizedItinerary format
 * Handles both TripData and NormalizedItinerary inputs
 */
export function ensureNormalizedFormat(data: any): NormalizedItinerary {
  if (!data) {
    throw new Error('Itinerary data is required');
  }
  
  if (isNormalizedItinerary(data)) {
    return data;
  }
  
  if (isTripData(data)) {
    return tripDataToNormalizedItinerary(data);
  }
  
  throw new Error('Invalid itinerary data format');
}