import type { TripData, TripComponent, DayPlan } from '../types/TripData';
import type { PlaceData } from './placeToWorkflowNode';

export interface AddPlaceRequest {
  dayId: string;
  dayNumber: number;
  place: PlaceData;
}

/**
 * Creates a TripComponent from place data
 */
export function createTripComponentFromPlace(place: PlaceData): TripComponent {
  console.log('[TripComponent] Creating component from place:', {
    name: place.name,
    types: place.types,
    rating: place.rating
  });
  
  const now = new Date();
  const startTime = new Date(now.getTime() + 2 * 60 * 60 * 1000); // 2 hours from now
  const endTime = new Date(startTime.getTime() + 2 * 60 * 60 * 1000); // 2 hours duration
  
  // Determine component type based on place data
  const types = place.types || [];
  let componentType: TripComponent['type'] = 'attraction';
  
  if (types.some(type => ['restaurant', 'food', 'meal_takeaway', 'cafe', 'bar'].includes(type))) {
    componentType = 'restaurant';
    console.log('[TripComponent] Detected as restaurant');
  } else if (types.some(type => ['lodging', 'hotel', 'accommodation'].includes(type))) {
    componentType = 'hotel';
    console.log('[TripComponent] Detected as hotel');
  } else if (types.some(type => ['transit_station', 'bus_station', 'train_station', 'airport'].includes(type))) {
    componentType = 'transport';
    console.log('[TripComponent] Detected as transport');
  } else if (types.some(type => ['shopping_mall', 'store', 'shopping_center'].includes(type))) {
    componentType = 'shopping';
    console.log('[TripComponent] Detected as shopping');
  } else if (types.some(type => ['movie_theater', 'night_club', 'amusement_park'].includes(type))) {
    componentType = 'entertainment';
    console.log('[TripComponent] Detected as entertainment');
  } else {
    console.log('[TripComponent] Defaulting to attraction');
  }
  
  // Generate tags based on place types
  const tags: string[] = [];
  types.forEach(type => {
    switch (type) {
      case 'tourist_attraction':
        tags.push('sightseeing', 'culture');
        break;
      case 'museum':
        tags.push('culture', 'education');
        break;
      case 'park':
        tags.push('nature', 'outdoor');
        break;
      case 'restaurant':
        tags.push('dining', 'local');
        break;
      case 'cafe':
        tags.push('dining', 'coffee');
        break;
      case 'bar':
        tags.push('nightlife', 'drinks');
        break;
      case 'lodging':
        tags.push('accommodation');
        break;
      case 'transit_station':
        tags.push('transport');
        break;
      case 'church':
      case 'temple':
      case 'mosque':
      case 'synagogue':
        tags.push('religion', 'culture');
        break;
      case 'zoo':
      case 'aquarium':
        tags.push('family', 'animals');
        break;
      case 'amusement_park':
        tags.push('family', 'entertainment');
        break;
      case 'stadium':
        tags.push('sports', 'entertainment');
        break;
      case 'theater':
        tags.push('culture', 'entertainment');
        break;
      case 'cinema':
        tags.push('entertainment', 'movies');
        break;
      case 'library':
        tags.push('education', 'culture');
        break;
      case 'university':
        tags.push('education');
        break;
      case 'shopping_mall':
        tags.push('shopping', 'retail');
        break;
    }
  });
  
  // Add rating-based tags
  if (place.rating) {
    if (place.rating >= 4.5) {
      tags.push('highly-rated');
    } else if (place.rating >= 4.0) {
      tags.push('well-rated');
    }
  }
  
  // Add popularity tags
  if (place.userRatingCount && place.userRatingCount > 100) {
    tags.push('popular');
  }
  
  // Remove duplicates
  const uniqueTags = [...new Set(tags)];
  
  return {
    id: `place_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    type: componentType,
    name: place.name,
    description: `Visit ${place.name}${place.address ? ` located at ${place.address}` : ''}`,
    location: {
      name: place.name,
      address: place.address || `${place.lat.toFixed(5)}, ${place.lng.toFixed(5)}`,
      coordinates: {
        lat: place.lat,
        lng: place.lng,
      },
    },
    timing: {
      startTime: startTime.toISOString(),
      endTime: endTime.toISOString(),
      duration: 120, // 2 hours default
      suggestedDuration: 120,
    },
    cost: {
      pricePerPerson: getDefaultCost(componentType),
      currency: 'INR',
      priceRange: getPriceRange(componentType),
      includesWhat: getIncludesWhat(componentType),
    },
    travel: {
      distanceFromPrevious: 0, // Will be calculated when needed
      travelTimeFromPrevious: 0,
      transportMode: 'walking',
      transportCost: 0,
    },
    details: {
      rating: place.rating || 4.0,
      reviewCount: place.userRatingCount || 0,
      category: componentType,
      tags: uniqueTags,
      openingHours: {
        monday: { open: '09:00', close: '18:00' },
        tuesday: { open: '09:00', close: '18:00' },
        wednesday: { open: '09:00', close: '18:00' },
        thursday: { open: '09:00', close: '18:00' },
        friday: { open: '09:00', close: '18:00' },
        saturday: { open: '09:00', close: '18:00' },
        sunday: { open: '09:00', close: '18:00' },
      },
      contact: {
        phone: place.phoneNumber,
        website: place.mapsLink,
      },
      accessibility: {
        wheelchairAccessible: false,
        elevatorAccess: false,
        restrooms: true,
        parking: true,
      },
      amenities: [],
    },
    booking: {
      required: false,
      notes: 'Added from map selection',
    },
    media: {
      images: [],
    },
    tips: {
      bestTimeToVisit: 'Anytime',
      whatToBring: [],
      insider: [],
      warnings: [],
    },
    isCustom: true,
    addedByUser: true,
    priority: 'recommended',
  };
}

/**
 * Gets default cost based on component type
 */
function getDefaultCost(type: TripComponent['type']): number {
  switch (type) {
    case 'restaurant':
      return 500;
    case 'hotel':
      return 2000;
    case 'transport':
      return 200;
    case 'shopping':
      return 1000;
    case 'entertainment':
      return 300;
    case 'attraction':
    default:
      return 300;
  }
}

/**
 * Gets price range based on component type
 */
function getPriceRange(type: TripComponent['type']): 'budget' | 'mid-range' | 'luxury' {
  switch (type) {
    case 'restaurant':
      return 'mid-range';
    case 'hotel':
      return 'mid-range';
    case 'transport':
      return 'budget';
    case 'shopping':
      return 'mid-range';
    case 'entertainment':
      return 'mid-range';
    case 'attraction':
    default:
      return 'budget';
  }
}

/**
 * Gets what's included based on component type
 */
function getIncludesWhat(type: TripComponent['type']): string[] {
  switch (type) {
    case 'restaurant':
      return ['meal', 'service'];
    case 'hotel':
      return ['accommodation', 'basic amenities'];
    case 'transport':
      return ['transportation'];
    case 'shopping':
      return ['shopping experience'];
    case 'entertainment':
      return ['entertainment', 'experience'];
    case 'attraction':
    default:
      return ['entry', 'experience'];
  }
}

/**
 * Adds a place to a specific day in the itinerary
 */
export function addPlaceToItineraryDay(
  tripData: TripData,
  request: AddPlaceRequest
): TripData {
  if (!tripData.itinerary) {
    throw new Error('No itinerary found in trip data');
  }
  
  const { dayId, dayNumber, place } = request;
  
  // Find the day to add the place to
  const dayIndex = tripData.itinerary.days.findIndex(
    day => day.id === dayId || day.dayNumber === dayNumber
  );
  
  if (dayIndex === -1) {
    throw new Error(`Day ${dayNumber} not found in itinerary`);
  }
  
  // Create the new component
  const newComponent = createTripComponentFromPlace(place);
  
  // Create updated trip data
  const updatedTripData: TripData = {
    ...tripData,
    itinerary: {
      ...tripData.itinerary,
      days: tripData.itinerary.days.map((day, index) => {
        if (index === dayIndex) {
          const updatedDay: DayPlan = {
            ...day,
            components: [...day.components, newComponent],
            totalCost: day.totalCost + newComponent.cost.pricePerPerson,
            totalDuration: day.totalDuration + newComponent.timing.duration,
          };
          return updatedDay;
        }
        return day;
      }),
      totalCost: tripData.itinerary.totalCost + newComponent.cost.pricePerPerson,
    },
    updatedAt: new Date().toISOString(),
  };
  
  return updatedTripData;
}
