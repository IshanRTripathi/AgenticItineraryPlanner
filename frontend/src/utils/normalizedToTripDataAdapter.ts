/**
 * Adapter to convert NormalizedItinerary to TripData format
 * This is a temporary solution while we migrate components to use NormalizedItinerary directly
 */

import { NormalizedItinerary, NormalizedDay, NormalizedNode } from '../types/NormalizedItinerary';
import { TripData, TripComponent, DayPlan } from '../types/TripData';

/**
 * Convert NormalizedItinerary to TripData format
 * Creates a minimal TripData object with the fields actually used by components
 */
export function convertNormalizedToTripData(normalized: NormalizedItinerary): TripData {
  // Handle null/undefined or missing days
  if (!normalized || !normalized.days) {
    // Return minimal valid TripData structure
    return createMinimalTripData(normalized);
  }

  // Convert days
  const days: DayPlan[] = normalized.days.map((day, index) => convertDay(day, index, normalized));

  // Create minimal TripData with only the fields components actually use
  const tripData: Partial<TripData> = {
    id: normalized.itineraryId,
    status: normalized.status || 'planning', // Fallback to planning if missing
    summary: normalized.summary,
    destination: normalized.destination,
    themes: normalized.themes,
    dates: {
      start: normalized.startDate || '',
      end: normalized.endDate || ''
    },
    itinerary: {
      id: normalized.itineraryId,
      days: days,
      totalCost: days.reduce((sum, day) => sum + (day.totalCost || 0), 0),
      totalDistance: days.reduce((sum, day) => sum + (day.totalDistance || 0), 0),
      totalDuration: days.length,
      highlights: [],
      themes: normalized.themes,
      difficulty: 'moderate' as const,
      packingList: [],
      emergencyInfo: {
        hospitals: [],
        embassies: [],
        police: [],
        helplines: []
      },
      localInfo: {
        currency: normalized.currency,
        language: 'en',
        timeZone: 'UTC',
        customs: [],
        etiquette: [],
        laws: []
      },
      mapBounds: normalized.mapBounds,
      countryCentroid: normalized.countryCentroid
    },
    createdAt: normalized.createdAt ? new Date(normalized.createdAt).toISOString() : new Date().toISOString(),
    updatedAt: normalized.updatedAt ? new Date(normalized.updatedAt).toISOString() : new Date().toISOString(),
    isPublic: false,

    // Minimal required fields with defaults
    startLocation: {
      id: 'start',
      name: normalized.destination || 'Unknown',
      country: '',
      city: '',
      coordinates: normalized.countryCentroid || { lat: 0, lng: 0 },
      timezone: 'UTC',
      currency: normalized.currency,
      exchangeRate: 1
    },
    endLocation: {
      id: 'end',
      name: normalized.destination || 'Unknown',
      country: '',
      city: '',
      coordinates: normalized.countryCentroid || { lat: 0, lng: 0 },
      timezone: 'UTC',
      currency: normalized.currency,
      exchangeRate: 1
    },
    isRoundTrip: true,
    travelers: [],
    leadTraveler: {
      id: 'lead',
      name: 'Traveler',
      email: '',
      age: 30
    },
    budget: {
      total: 0,
      currency: normalized.currency,
      breakdown: {
        accommodation: 0,
        food: 0,
        transport: 0,
        activities: 0,
        shopping: 0,
        emergency: 0
      }
    },
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
      womenFriendly: false,
      petFriendly: false,
      veganOnly: false,
      wheelchairAccessible: false,
      budgetFriendly: false,
      luxuryOnly: false,
      familyFriendly: false,
      soloTravelSafe: false
    }
  };

  return tripData as TripData;
}

function convertDay(day: NormalizedDay, index: number, itinerary: NormalizedItinerary): DayPlan {
  const components = day.nodes.map(node => convertNode(node));

  return {
    id: `day-${day.dayNumber}`,
    date: day.date,
    dayNumber: day.dayNumber,
    theme: day.location,
    location: day.location,
    components: components,
    totalDistance: day.totals?.distanceKm || 0,
    totalCost: day.totals?.cost || 0,
    totalDuration: day.totals?.durationHr || 0,
    startTime: day.timeWindow?.start || '09:00',
    endTime: day.timeWindow?.end || '18:00',
    meals: {
      breakfast: components.find(c => c.type === 'restaurant' && c.name.toLowerCase().includes('breakfast')),
      lunch: components.find(c => c.type === 'restaurant' && c.name.toLowerCase().includes('lunch')),
      dinner: components.find(c => c.type === 'restaurant' && c.name.toLowerCase().includes('dinner')),
      snacks: []
    },
    accommodation: components.find(c => c.type === 'hotel'),
    weather: {
      temperature: { min: 20, max: 25 },
      condition: 'Clear',
      precipitation: 0
    },
    notes: day.notes
  };
}

function convertNode(node: NormalizedNode): TripComponent {
  // Map node type to component type
  const typeMap: Record<string, TripComponent['type']> = {
    'attraction': 'attraction',
    'meal': 'restaurant',
    'hotel': 'hotel',
    'accommodation': 'hotel',
    'transit': 'transport',
    'transport': 'transport'
  };

  return {
    id: node.id,
    type: typeMap[node.type] || 'attraction',
    name: node.title,
    description: node.details?.category || '',
    location: {
      name: node.location?.name || '',
      address: node.location?.address || '',
      coordinates: {
        lat: node.location?.coordinates?.lat || null,
        lng: node.location?.coordinates?.lng || null
      },
      // *** CRITICAL FIX: Preserve enrichment fields from NodeLocation ***
      placeId: node.location?.placeId,
      photos: node.location?.photos,
      rating: node.location?.rating,
      userRatingsTotal: node.location?.userRatingsTotal,
      priceLevel: node.location?.priceLevel
    },
    timing: {
      startTime: node.timing?.startTime ? new Date(node.timing.startTime).toISOString() : '',
      endTime: node.timing?.endTime ? new Date(node.timing.endTime).toISOString() : '',
      duration: node.timing?.durationMin || 60,
      suggestedDuration: node.timing?.durationMin || 60
    },
    cost: {
      pricePerPerson: node.cost?.amount || 0,
      currency: node.cost?.currency || 'USD',
      priceRange: 'mid-range',
      includesWhat: []
    },
    travel: {
      distanceFromPrevious: 0,
      travelTimeFromPrevious: 0,
      transportMode: 'walking',
      transportCost: 0
    },
    details: {
      rating: node.details?.rating || 0,
      reviewCount: 0,
      category: node.details?.category || '',
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
      required: false,
      bookingUrl: node.links?.booking
    },
    media: {
      images: []
    },
    tips: {
      bestTimeToVisit: node.tips?.bestTime?.join(', ') || '',
      whatToBring: [],
      insider: [],
      warnings: node.tips?.warnings || []
    },
    priority: 'recommended',
    locked: node.locked || false
  };
}

/**
 * Create a minimal TripData structure when normalized data is incomplete
 */
function createMinimalTripData(normalized?: Partial<NormalizedItinerary>): TripData {
  const currency = normalized?.currency || 'USD';
  const destination = normalized?.destination || 'Unknown';

  return {
    id: normalized?.itineraryId || 'unknown',
    status: normalized?.status || 'planning', // Fallback to planning if missing
    summary: normalized?.summary || '',
    destination: destination,
    themes: normalized?.themes || [],
    dates: {
      start: normalized?.startDate || '',
      end: normalized?.endDate || ''
    },
    itinerary: {
      id: normalized?.itineraryId || 'unknown',
      days: [],
      totalCost: 0,
      totalDistance: 0,
      totalDuration: 0,
      highlights: [],
      themes: normalized?.themes || [],
      difficulty: 'moderate' as const,
      packingList: [],
      emergencyInfo: {
        hospitals: [],
        embassies: [],
        police: [],
        helplines: []
      },
      localInfo: {
        currency: currency,
        language: 'en',
        timeZone: 'UTC',
        customs: [],
        etiquette: [],
        laws: []
      }
    },
    createdAt: normalized?.createdAt ? new Date(normalized.createdAt).toISOString() : new Date().toISOString(),
    updatedAt: normalized?.updatedAt ? new Date(normalized.updatedAt).toISOString() : new Date().toISOString(),
    isPublic: false,
    startLocation: {
      id: 'start',
      name: destination,
      country: '',
      city: '',
      coordinates: normalized?.countryCentroid || { lat: 0, lng: 0 },
      timezone: 'UTC',
      currency: currency,
      exchangeRate: 1
    },
    endLocation: {
      id: 'end',
      name: destination,
      country: '',
      city: '',
      coordinates: normalized?.countryCentroid || { lat: 0, lng: 0 },
      timezone: 'UTC',
      currency: currency,
      exchangeRate: 1
    },
    isRoundTrip: true,
    travelers: [],
    leadTraveler: {
      id: 'lead',
      name: 'Traveler',
      email: '',
      age: 30
    },
    budget: {
      total: 0,
      currency: currency,
      breakdown: {
        accommodation: 0,
        food: 0,
        transport: 0,
        activities: 0,
        shopping: 0,
        emergency: 0
      }
    },
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
      womenFriendly: false,
      petFriendly: false,
      veganOnly: false,
      wheelchairAccessible: false,
      budgetFriendly: false,
      luxuryOnly: false,
      familyFriendly: false,
      soloTravelSafe: false
    }
  };
}
