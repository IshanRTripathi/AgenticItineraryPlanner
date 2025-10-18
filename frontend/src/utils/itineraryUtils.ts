import { TripData } from '../types/TripData';

/**
 * Extract unique cities from itinerary data
 */
export function extractCitiesFromItinerary(tripData: TripData): string[] {
  if (!tripData?.itinerary?.days) {
    return [];
  }

  const cities = new Set<string>();

  // Add main destination
  if (tripData.destination) {
    cities.add(tripData.destination);
  }

  // Extract cities from each day
  tripData.itinerary.days.forEach(day => {
    if (day.location) {
      cities.add(day.location);
    }

    // Extract cities from nodes/activities
    if (day.nodes) {
      day.nodes.forEach(node => {
        if (node.location?.name) {
          // Extract city name from location (e.g., "Barcelona, Spain" -> "Barcelona")
          const cityName = node.location.name.split(',')[0].trim();
          cities.add(cityName);
        }
      });
    }

    // Also check for components/activities (legacy format)
    if (day.components) {
      day.components.forEach(component => {
        if (component.location?.name) {
          const cityName = component.location.name.split(',')[0].trim();
          cities.add(cityName);
        }
      });
    }
  });

  return Array.from(cities);
}

/**
 * Extract unique cities from normalized itinerary data
 */
export function extractCitiesFromNormalizedItinerary(normalizedItinerary: any): string[] {
  if (!normalizedItinerary?.days) {
    return [];
  }

  const cities = new Set<string>();

  normalizedItinerary.days.forEach((day: any) => {
    if (day.location) {
      cities.add(day.location);
    }

    if (day.nodes) {
      day.nodes.forEach((node: any) => {
        if (node.location?.name) {
          const cityName = node.location.name.split(',')[0].trim();
          cities.add(cityName);
        }
      });
    }
  });

  return Array.from(cities);
}

/**
 * Get the main destination city from trip data
 */
export function getMainDestination(tripData: TripData): string {
  return tripData.destination || 'Unknown';
}

/**
 * Get the number of days in the itinerary
 */
export function getItineraryDuration(tripData: TripData): number {
  return tripData.itinerary?.days?.length || 0;
}

/**
 * Get the total number of activities across all days
 */
export function getTotalActivities(tripData: TripData): number {
  if (!tripData?.itinerary?.days) {
    return 0;
  }

  return tripData.itinerary.days.reduce((total, day) => {
    return total + (day.nodes?.length || day.components?.length || 0);
  }, 0);
}

/**
 * Get unique transport modes used in the itinerary
 */
export function getTransportModes(tripData: TripData): { [key: string]: number } {
  if (!tripData?.itinerary?.days) {
    return {};
  }

  const transportCounts: { [key: string]: number } = {};

  tripData.itinerary.days.forEach(day => {
    if (day.nodes) {
      day.nodes.forEach(node => {
        if (node.type === 'transport') {
          const transportType = node.title?.toLowerCase() || 'unknown';
          if (transportType.includes('flight') || transportType.includes('plane')) {
            transportCounts['flight'] = (transportCounts['flight'] || 0) + 1;
          } else if (transportType.includes('train')) {
            transportCounts['train'] = (transportCounts['train'] || 0) + 1;
          } else if (transportType.includes('bus')) {
            transportCounts['bus'] = (transportCounts['bus'] || 0) + 1;
          } else if (transportType.includes('walk') || transportType.includes('foot')) {
            transportCounts['walking'] = (transportCounts['walking'] || 0) + 1;
          }
        }
      });
    }
  });

  return transportCounts;
}










