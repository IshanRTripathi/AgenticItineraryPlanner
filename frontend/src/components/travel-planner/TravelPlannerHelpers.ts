import { QueryClient } from '@tanstack/react-query';
import { TripData } from '../../types/TripData';
import { Destination } from './shared/types';
import { MapMarker } from '../../types/MapTypes';
import { queryKeys } from '../../state/query/hooks';
import { addPlaceToItineraryDay } from '../../utils/addPlaceToItinerary';

/**
 * Helper functions and handlers for TravelPlanner
 * Extracted from TravelPlanner.tsx for better maintainability
 */

/**
 * Destination management handlers
 */
export function createDestinationHandlers(
  setDestinations: (destinations: Destination[] | ((prev: Destination[]) => Destination[])) => void
) {
  const updateDestination = (id: string, updates: Partial<Destination>) => {
    setDestinations(prev => prev.map(dest =>
      dest.id === id ? { ...dest, ...updates } : dest
    ));
  };

  const addDestination = (destination: Omit<Destination, 'id'>) => {
    const newDest: Destination = {
      id: Date.now().toString(),
      ...destination
    };
    setDestinations(prev => [...prev, newDest]);
  };

  const removeDestination = (id: string) => {
    setDestinations(prev => prev.filter(dest => dest.id !== id));
  };

  return {
    updateDestination,
    addDestination,
    removeDestination,
  };
}

/**
 * Day selection handler
 */
export function createDaySelectHandler(
  setSelectedDay: (day: { dayNumber: number; dayData: any } | null) => void
) {
  return (dayNumber: number, dayData: any) => {
    console.log('Day selected:', dayNumber, dayData);
    setSelectedDay({ dayNumber, dayData });
  };
}

/**
 * Itinerary update handler for chat
 */
export function createItineraryUpdateHandler(
  queryClient: QueryClient,
  tripId: string
) {
  return async (updatedItinerary: any) => {
    console.log('Itinerary updated from chat:', updatedItinerary);

    // Invalidate and refetch the itinerary data to reflect changes
    try {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.itinerary(tripId)
      });
      console.log('Itinerary data refreshed successfully');
    } catch (error) {
      console.error('Failed to refresh itinerary data:', error);
    }
  };
}

/**
 * Build map markers from trip data
 * Now handles components without coordinates by skipping them gracefully
 */
export function buildMapMarkers(currentTripData: TripData): MapMarker[] {
  const markers: MapMarker[] = [];
  const days = currentTripData.itinerary?.days || [];
  let skippedCount = 0;
  let addedCount = 0;

  console.log('[Maps] Building markers from trip data:', {
    hasTripData: !!currentTripData,
    hasItinerary: !!currentTripData.itinerary,
    hasDays: !!currentTripData.itinerary?.days,
    daysCount: days.length,
    daysData: days
  });

  try {
    days.forEach((day, dayIdx) => {
      const comps = day.components || [];
      
      comps.forEach((c: any, compIdx: number) => {
        try {
          const lat = c?.location?.coordinates?.lat;
          const lng = c?.location?.coordinates?.lng;

          // Validate coordinates - must be valid numbers and not null/undefined
          const hasValidCoordinates = 
            lat !== null && lng !== null && 
            lat !== undefined && lng !== undefined &&
            typeof lat === 'number' && typeof lng === 'number' &&
            !isNaN(lat) && !isNaN(lng) &&
            lat >= -90 && lat <= 90 && 
            lng >= -180 && lng <= 180;

          if (hasValidCoordinates) {
            const marker: MapMarker = {
              id: c.id || `day${dayIdx + 1}_node${compIdx}`,
              position: { lat, lng },
              title: c.name || c.type || `Place ${compIdx + 1}`,
              type: (c.type === 'restaurant' ? 'meal' :
                c.type === 'hotel' ? 'accommodation' :
                  c.type === 'transport' ? 'transport' : 'attraction') as 'meal' | 'accommodation' | 'transport' | 'attraction',
              status: 'planned' as const,
              locked: false,
              rating: c.rating || 0,
              googleMapsUri: c.googleMapsUri || '',
            };
            
            markers.push(marker);
            addedCount++;
          } else {
            // Skip components without valid coordinates
            skippedCount++;
            console.debug('[Maps] Skipping component without valid coordinates:', {
              id: c.id,
              name: c.name,
              hasLocation: !!c.location,
              hasCoordinates: !!c.location?.coordinates,
              lat,
              lng
            });
          }
        } catch (error) {
          console.error('[Maps] Error processing component:', c, error);
          skippedCount++;
        }
      });
    });

    console.log('[Maps] Marker building complete:', {
      totalComponents: addedCount + skippedCount,
      markersAdded: addedCount,
      componentsSkipped: skippedCount,
      message: skippedCount > 0 ? 
        'Some locations are missing coordinates. Run Enrichment agent to add them.' : 
        'All locations have coordinates'
    });

  } catch (error) {
    console.error('[Maps] Error building markers:', error);
  }

  return markers;
}

/**
 * Handle place addition for mobile
 */
export function createAddPlaceHandler(
  currentTripData: TripData,
  queryClient: QueryClient
) {
  return ({ dayId, dayNumber, place }: { dayId: string; dayNumber: number; place: any }) => {
    console.log('[Mobile] Add place to itinerary:', { dayId, dayNumber, place });

    try {
      // Add to day-by-day view
      const updatedTripData = addPlaceToItineraryDay(currentTripData, {
        dayId,
        dayNumber,
        place,
      });

      // Update the trip data via query client
      queryClient.setQueryData(
        queryKeys.itinerary(currentTripData.id),
        updatedTripData
      );

      console.log('[Mobile] Successfully added place to itinerary');
    } catch (error) {
      console.error('[Mobile] Failed to add place to itinerary:', error);
    }
  };
}
