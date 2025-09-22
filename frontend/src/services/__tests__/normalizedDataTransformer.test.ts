import { NormalizedDataTransformer } from '../normalizedDataTransformer';
import { NormalizedItinerary, NormalizedDay, NormalizedNode } from '../../types/NormalizedItinerary';
import { TripData } from '../../types/TripData';

describe('NormalizedDataTransformer', () => {
  describe('transformNormalizedItineraryToTripData', () => {
    test('should transform basic normalized itinerary to TripData', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'test-itinerary',
        version: 1,
        summary: '3-day Barcelona adventure for family',
        currency: 'EUR',
        themes: ['culture', 'food', 'architecture'],
        days: [
          {
            dayNumber: 1,
            date: '2025-06-01',
            location: 'Barcelona',
            nodes: [
              {
                id: 'n_attraction_1',
                type: 'attraction',
                title: 'Sagrada Familia',
                location: {
                  name: 'Sagrada Familia',
                  address: 'Carrer de Mallorca, 401, 08013 Barcelona, Spain',
                  coordinates: { lat: 41.4036, lng: 2.1744 }
                },
                timing: {
                  startTime: '2025-06-01T09:00:00',
                  endTime: '2025-06-01T11:00:00',
                  durationMin: 120
                },
                cost: {
                  amount: 25,
                  currency: 'EUR',
                  per: 'person'
                },
                details: {
                  rating: 4.8,
                  category: 'monument',
                  tags: ['iconic', 'architecture', 'gaudi']
                },
                tips: {
                  bestTime: ['morning', 'early afternoon'],
                  travel: ['metro line 2 or 5 to Sagrada Familia'],
                  warnings: ['book tickets in advance']
                },
                locked: false
              }
            ],
            edges: [],
            totals: {
              distanceKm: 5.2,
              cost: 25,
              durationHr: 2
            }
          }
        ],
        settings: {
          autoApply: false,
          defaultScope: 'day'
        },
        agents: {
          planner: { status: 'completed' },
          enrichment: { status: 'completed' }
        }
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      expect(result.id).toBe('test-itinerary');
      expect(result.endLocation.name).toBe('Barcelona');
      expect(result.endLocation.currency).toBe('EUR');
      expect(result.dates.start).toBe('2025-06-01');
      expect(result.dates.end).toBe('2025-06-01');
      expect(result.itinerary).toBeDefined();
      expect(result.itinerary!.days).toHaveLength(1);
      expect(result.itinerary!.days[0].components).toHaveLength(1);
      expect(result.itinerary!.days[0].components[0].name).toBe('Sagrada Familia');
      expect(result.itinerary!.days[0].components[0].type).toBe('attraction');
    });

    test('should handle empty days array', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'empty-itinerary',
        version: 1,
        summary: 'Empty itinerary',
        currency: 'USD',
        themes: [],
        days: [],
        settings: { autoApply: false, defaultScope: 'day' },
        agents: {}
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      expect(result.id).toBe('empty-itinerary');
      expect(result.itinerary!.days).toHaveLength(0);
      expect(result.itinerary!.totalDuration).toBe(0);
    });

    test('should transform multiple days correctly', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'multi-day-itinerary',
        version: 1,
        summary: '3-day Barcelona trip',
        currency: 'EUR',
        themes: ['culture', 'food'],
        days: [
          {
            dayNumber: 1,
            date: '2025-06-01',
            location: 'Barcelona',
            nodes: [
              {
                id: 'n_attraction_1',
                type: 'attraction',
                title: 'Sagrada Familia',
                locked: false
              }
            ],
            edges: [],
            totals: { distanceKm: 5, cost: 25, durationHr: 2 }
          },
          {
            dayNumber: 2,
            date: '2025-06-02',
            location: 'Barcelona',
            nodes: [
              {
                id: 'n_attraction_2',
                type: 'attraction',
                title: 'Park Güell',
                locked: false
              }
            ],
            edges: [],
            totals: { distanceKm: 3, cost: 20, durationHr: 3 }
          },
          {
            dayNumber: 3,
            date: '2025-06-03',
            location: 'Barcelona',
            nodes: [
              {
                id: 'n_attraction_3',
                type: 'attraction',
                title: 'Casa Batlló',
                locked: false
              }
            ],
            edges: [],
            totals: { distanceKm: 2, cost: 30, durationHr: 2 }
          }
        ],
        settings: { autoApply: false, defaultScope: 'day' },
        agents: {}
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      expect(result.itinerary!.days).toHaveLength(3);
      expect(result.itinerary!.totalDuration).toBe(3);
      expect(result.dates.start).toBe('2025-06-01');
      expect(result.dates.end).toBe('2025-06-03');
      expect(result.itinerary!.totalCost).toBe(75); // 25 + 20 + 30
      expect(result.itinerary!.totalDistance).toBe(10); // 5 + 3 + 2
    });

    test('should extract meals correctly', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'meal-itinerary',
        version: 1,
        summary: 'Barcelona with meals',
        currency: 'EUR',
        themes: ['food'],
        days: [
          {
            dayNumber: 1,
            date: '2025-06-01',
            location: 'Barcelona',
            nodes: [
              {
                id: 'n_meal_breakfast',
                type: 'meal',
                title: 'Breakfast at local café',
                timing: {
                  startTime: '2025-06-01T08:00:00',
                  endTime: '2025-06-01T09:00:00',
                  durationMin: 60
                },
                locked: false
              },
              {
                id: 'n_meal_lunch',
                type: 'meal',
                title: 'Lunch at traditional restaurant',
                timing: {
                  startTime: '2025-06-01T13:00:00',
                  endTime: '2025-06-01T14:30:00',
                  durationMin: 90
                },
                locked: false
              },
              {
                id: 'n_meal_dinner',
                type: 'meal',
                title: 'Dinner at fine dining',
                timing: {
                  startTime: '2025-06-01T20:00:00',
                  endTime: '2025-06-01T22:00:00',
                  durationMin: 120
                },
                locked: false
              }
            ],
            edges: [],
            totals: { distanceKm: 2, cost: 80, durationHr: 4.5 }
          }
        ],
        settings: { autoApply: false, defaultScope: 'day' },
        agents: {}
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      const day = result.itinerary!.days[0];
      expect(day.meals.breakfast).toBeDefined();
      expect(day.meals.lunch).toBeDefined();
      expect(day.meals.dinner).toBeDefined();
      expect(day.meals.breakfast!.name).toBe('Breakfast at local café');
      expect(day.meals.lunch!.name).toBe('Lunch at traditional restaurant');
      expect(day.meals.dinner!.name).toBe('Dinner at fine dining');
    });

    test('should extract accommodation correctly', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'accommodation-itinerary',
        version: 1,
        summary: 'Barcelona with hotel',
        currency: 'EUR',
        themes: ['luxury'],
        days: [
          {
            dayNumber: 1,
            date: '2025-06-01',
            location: 'Barcelona',
            nodes: [
              {
                id: 'n_hotel_1',
                type: 'accommodation',
                title: 'Hotel Casa Fuster',
                location: {
                  name: 'Hotel Casa Fuster',
                  address: 'Passeig de Gràcia, 132, 08008 Barcelona, Spain',
                  coordinates: { lat: 41.3948, lng: 2.1602 }
                },
                cost: {
                  amount: 200,
                  currency: 'EUR',
                  per: 'night'
                },
                locked: false
              }
            ],
            edges: [],
            totals: { distanceKm: 0, cost: 200, durationHr: 0 }
          }
        ],
        settings: { autoApply: false, defaultScope: 'day' },
        agents: {}
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      const day = result.itinerary!.days[0];
      expect(day.accommodation).toBeDefined();
      expect(day.accommodation!.name).toBe('Hotel Casa Fuster');
      expect(day.accommodation!.type).toBe('hotel');
      expect(day.accommodation!.cost.pricePerPerson).toBe(200);
    });

    test('should transform themes to preferences correctly', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'themes-itinerary',
        version: 1,
        summary: 'Themed Barcelona trip',
        currency: 'EUR',
        themes: ['culture', 'food', 'architecture'],
        days: [],
        settings: { autoApply: false, defaultScope: 'day' },
        agents: {}
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      expect(result.preferences.culture).toBe(80);
      expect(result.preferences.cuisine).toBe(80);
      expect(result.preferences.heritage).toBe(80);
      expect(result.preferences.photography).toBe(80);
      // Other preferences should remain at default 50
      expect(result.preferences.nightlife).toBe(50);
      expect(result.preferences.adventure).toBe(50);
    });

    test('should handle locked nodes correctly', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'locked-itinerary',
        version: 1,
        summary: 'Barcelona with locked nodes',
        currency: 'EUR',
        themes: ['culture'],
        days: [
          {
            dayNumber: 1,
            date: '2025-06-01',
            location: 'Barcelona',
            nodes: [
              {
                id: 'n_locked_attraction',
                type: 'attraction',
                title: 'Booked Sagrada Familia',
                locked: true,
                bookingRef: 'BK123456'
              },
              {
                id: 'n_unlocked_attraction',
                type: 'attraction',
                title: 'Park Güell',
                locked: false
              }
            ],
            edges: [],
            totals: { distanceKm: 5, cost: 50, durationHr: 4 }
          }
        ],
        settings: { autoApply: false, defaultScope: 'day' },
        agents: {}
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      const components = result.itinerary!.days[0].components;
      expect(components).toHaveLength(2);
      
      const lockedComponent = components.find(c => c.name === 'Booked Sagrada Familia');
      const unlockedComponent = components.find(c => c.name === 'Park Güell');
      
      expect(lockedComponent).toBeDefined();
      expect(unlockedComponent).toBeDefined();
      
      // Check that locked status is preserved in some way (through priority or other means)
      expect(lockedComponent!.priority).toBe('must-visit');
    });

    test('should calculate budget breakdown correctly', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'budget-itinerary',
        version: 1,
        summary: 'Barcelona budget trip',
        currency: 'EUR',
        themes: ['budget'],
        days: [
          {
            dayNumber: 1,
            date: '2025-06-01',
            location: 'Barcelona',
            nodes: [],
            edges: [],
            totals: { distanceKm: 5, cost: 100, durationHr: 8 }
          },
          {
            dayNumber: 2,
            date: '2025-06-02',
            location: 'Barcelona',
            nodes: [],
            edges: [],
            totals: { distanceKm: 3, cost: 80, durationHr: 6 }
          }
        ],
        settings: { autoApply: false, defaultScope: 'day' },
        agents: {}
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      expect(result.budget.total).toBe(180); // 100 + 80
      expect(result.budget.currency).toBe('EUR');
      expect(result.budget.breakdown.accommodation).toBe(72); // 40% of 180
      expect(result.budget.breakdown.food).toBe(54); // 30% of 180
      expect(result.budget.breakdown.transport).toBe(36); // 20% of 180
      expect(result.budget.breakdown.activities).toBe(18); // 10% of 180
    });

    test('should handle missing optional fields gracefully', () => {
      // Arrange
      const normalizedItinerary: NormalizedItinerary = {
        itineraryId: 'minimal-itinerary',
        version: 1,
        summary: 'Minimal itinerary',
        currency: 'USD',
        themes: [],
        days: [
          {
            dayNumber: 1,
            date: '2025-06-01',
            location: 'Test City',
            nodes: [
              {
                id: 'n_minimal_node',
                type: 'attraction',
                title: 'Minimal Node'
                // Missing optional fields like location, timing, cost, etc.
              }
            ],
            edges: []
            // Missing totals
          }
        ],
        settings: { autoApply: false, defaultScope: 'day' },
        agents: {}
      };

      // Act
      const result = NormalizedDataTransformer.transformNormalizedItineraryToTripData(normalizedItinerary);

      // Assert
      expect(result).toBeDefined();
      expect(result.id).toBe('minimal-itinerary');
      expect(result.itinerary!.days).toHaveLength(1);
      expect(result.itinerary!.days[0].components).toHaveLength(1);
      expect(result.itinerary!.days[0].components[0].name).toBe('Minimal Node');
      expect(result.itinerary!.days[0].totalCost).toBe(0); // Should default to 0
      expect(result.itinerary!.days[0].totalDistance).toBe(0); // Should default to 0
    });
  });
});
