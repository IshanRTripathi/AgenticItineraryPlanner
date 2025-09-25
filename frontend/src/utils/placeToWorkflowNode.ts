import { createNewNode } from '../components/workflow/WorkflowUtils';
import type { WorkflowNodeData } from '../components/WorkflowBuilder';

export interface PlaceData {
  name: string;
  address?: string;
  lat: number;
  lng: number;
  types?: string[];
  rating?: number;
  userRatingCount?: number;
  phoneNumber?: string;
  mapsLink?: string;
}

/**
 * Determines the appropriate workflow node type based on place data
 */
export function determineNodeType(place: PlaceData): WorkflowNodeData['type'] {
  const types = place.types || [];
  const name = place.name.toLowerCase();
  
  console.log('[NodeType] Determining type for place:', {
    name: place.name,
    types: types,
    nameLower: name
  });
  
  // Check for specific keywords in types or name
  if (types.some(type => 
    ['restaurant', 'food', 'meal_takeaway', 'cafe', 'bar'].includes(type)
  ) || name.includes('restaurant') || name.includes('cafe') || name.includes('dining')) {
    console.log('[NodeType] Detected as Meal');
    return 'Meal';
  }
  
  if (types.some(type => 
    ['lodging', 'hotel', 'accommodation'].includes(type)
  ) || name.includes('hotel') || name.includes('resort') || name.includes('inn')) {
    console.log('[NodeType] Detected as Hotel');
    return 'Hotel';
  }
  
  if (types.some(type => 
    ['transit_station', 'bus_station', 'train_station', 'airport', 'subway_station'].includes(type)
  ) || name.includes('station') || name.includes('airport') || name.includes('terminal')) {
    console.log('[NodeType] Detected as Transit');
    return 'Transit';
  }
  
  // Default to Attraction for most places
  console.log('[NodeType] Defaulting to Attraction');
  return 'Attraction';
}

/**
 * Generates appropriate tags based on place data
 */
export function generateTags(place: PlaceData): string[] {
  const types = place.types || [];
  const tags: string[] = [];
  
  // Map Google Places types to our tag system
  const typeMapping: { [key: string]: string[] } = {
    'tourist_attraction': ['sightseeing', 'culture'],
    'museum': ['culture', 'education'],
    'park': ['nature', 'outdoor'],
    'shopping_mall': ['shopping', 'retail'],
    'restaurant': ['dining', 'local'],
    'cafe': ['dining', 'coffee'],
    'bar': ['nightlife', 'drinks'],
    'lodging': ['accommodation'],
    'transit_station': ['transport'],
    'church': ['religion', 'culture'],
    'temple': ['religion', 'culture'],
    'mosque': ['religion', 'culture'],
    'synagogue': ['religion', 'culture'],
    'zoo': ['family', 'animals'],
    'aquarium': ['family', 'marine'],
    'amusement_park': ['family', 'entertainment'],
    'stadium': ['sports', 'entertainment'],
    'theater': ['culture', 'entertainment'],
    'cinema': ['entertainment', 'movies'],
    'library': ['education', 'culture'],
    'university': ['education'],
    'hospital': ['healthcare'],
    'pharmacy': ['healthcare'],
    'bank': ['services'],
    'atm': ['services'],
    'gas_station': ['services'],
    'parking': ['services'],
    'post_office': ['services'],
    'police': ['services'],
    'fire_station': ['services'],
  };
  
  // Add tags based on types
  types.forEach(type => {
    if (typeMapping[type]) {
      tags.push(...typeMapping[type]);
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
  
  // Add user count tags
  if (place.userRatingCount && place.userRatingCount > 100) {
    tags.push('popular');
  }
  
  // Remove duplicates and return
  return [...new Set(tags)];
}

/**
 * Creates a workflow node from place data
 */
export function createWorkflowNodeFromPlace(
  place: PlaceData,
  dayIndex: number,
  position: { x: number; y: number }
) {
  const nodeType = determineNodeType(place);
  const tags = generateTags(place);
  
  // Create the base node
  const newNode = createNewNode(nodeType, dayIndex, position);
  
  // Customize the node with place-specific data
  const updatedData: Partial<WorkflowNodeData> = {
    title: place.name,
    tags: tags,
    meta: {
      ...newNode.data.meta,
      rating: place.rating || 4.0,
      address: place.address || `${place.lat.toFixed(5)}, ${place.lng.toFixed(5)}`,
    },
  };
  
  // Set appropriate duration based on type
  if (nodeType === 'Meal') {
    updatedData.durationMin = 60; // 1 hour for meals
    updatedData.costINR = 500; // Default meal cost
  } else if (nodeType === 'Hotel') {
    updatedData.durationMin = 480; // 8 hours for hotel
    updatedData.costINR = 2000; // Default hotel cost
  } else if (nodeType === 'Transit') {
    updatedData.durationMin = 30; // 30 minutes for transit
    updatedData.costINR = 200; // Default transit cost
  } else {
    // Attraction
    updatedData.durationMin = 120; // 2 hours for attractions
    updatedData.costINR = 300; // Default attraction cost
  }
  
  return {
    ...newNode,
    data: {
      ...newNode.data,
      ...updatedData,
    },
  };
}
