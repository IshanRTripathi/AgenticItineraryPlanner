/**
 * Data transformation service to convert backend API responses
 * to frontend-compatible data structures
 */

import { ItineraryResponse } from './apiClient';
import { TripData, TripItinerary, DayPlan, TripComponent } from '../types/TripData';

export class DataTransformer {
  
  /**
   * Transform backend ItineraryResponse to frontend TripData
   */
  static transformItineraryResponseToTripData(response: any): TripData {
    const travelers = this.transformPartyToTravelers(response.party || { adults: 1, children: 0, infants: 0, rooms: 1 });
    
    return {
      id: response.id,
      startLocation: {
        id: 'start',
        name: 'Home',
        country: 'Unknown',
        city: 'Unknown',
        coordinates: { lat: 0, lng: 0 },
        timezone: 'UTC',
        currency: 'USD',
        exchangeRate: 1.0
      },
      endLocation: {
        id: response.id,
        name: response.destination,
        country: this.extractCountry(response.destination),
        city: this.extractCity(response.destination),
        coordinates: { lat: 0, lng: 0 },
        timezone: 'UTC',
        currency: 'USD',
        exchangeRate: 1.0
      },
      isRoundTrip: true,
      dates: {
        start: response.startDate,
        end: response.endDate
      },
      travelers: travelers,
      leadTraveler: travelers[0] || {
        id: 'lead',
        name: 'Lead Traveler',
        email: '',
        age: 30,
        preferences: {
          dietaryRestrictions: [],
          mobilityNeeds: [],
          interests: []
        }
      },
      budget: {
        total: response.agentResults?.totalEstimatedCost || 1000,
        currency: response.agentResults?.currency || 'USD',
        breakdown: {
          accommodation: 400,
          food: 300,
          transport: 200,
          activities: 100,
          shopping: 0,
          emergency: 0
        }
      },
      preferences: this.transformInterestsToPreferences(response.interests || []),
      settings: this.transformConstraintsToSettings(response.constraints || []),
      itinerary: this.transformItineraryResponse(response),
      status: response.status === 'completed' ? 'completed' : 'planning',
      isPublic: response.public || response.isPublic || false,
      createdAt: response.createdAt,
      updatedAt: response.updatedAt
    };
  }

  /**
   * Transform ItineraryResponse to TripItinerary
   */
  private static transformItineraryResponse(response: any): TripItinerary {
    return {
      id: response.id,
      days: this.transformDays(response.days || []),
      totalCost: response.agentResults?.totalEstimatedCost || 1000,
      totalDistance: 50,
      totalDuration: response.durationDays || this.calculateDuration(response.startDate, response.endDate),
      highlights: response.agentResults?.highlights || ['Eiffel Tower', 'Louvre Museum', 'Seine River'],
      themes: response.interests || ['culture', 'history', 'art'],
      difficulty: 'easy',
      packingList: [],
      emergencyInfo: {
        hospitals: [],
        embassies: [],
        police: [],
        helplines: []
      },
      localInfo: {
        currency: response.agentResults?.currency || 'EUR',
        language: response.language || 'English',
        timeZone: 'UTC',
        customs: [],
        etiquette: [],
        laws: []
      }
    };
  }

  /**
   * Transform backend days to frontend DayPlan format
   */
  private static transformDays(days: any[]): DayPlan[] {
    // If no days are provided, create mock days for testing
    if (!days || days.length === 0) {
      return this.createMockDays();
    }
    
    return days.map((day, index) => ({
      id: day.id || `day-${index + 1}`,
      date: day.date || '',
      dayNumber: day.dayNumber || day.day || index + 1,
      theme: day.theme || 'Explore the city',
      location: day.location || '',
      components: this.transformDayComponents(day),
      totalDistance: day.totalDistance || 0,
      totalCost: day.totalCost || 0,
      totalDuration: day.totalDuration || 0,
      startTime: day.startTime || '09:00',
      endTime: day.endTime || '18:00',
      meals: this.transformMeals(day.meals || []),
      accommodation: day.accommodation ? this.transformAccommodationToComponent(day.accommodation) : undefined,
      weather: {
        temperature: { min: 15, max: 25 },
        condition: 'sunny',
        precipitation: 0
      },
      notes: day.notes || ''
    }));
  }

  /**
   * Create mock days for testing when backend doesn't provide days
   */
  private static createMockDays(): DayPlan[] {
    return [
      {
        id: 'day-1',
        date: '2025-06-01',
        dayNumber: 1,
        theme: 'Explore Paris',
        location: 'Paris, France',
        components: [
          {
            id: '1',
            type: 'attraction',
            name: 'Eiffel Tower Visit',
            description: 'Visit the iconic Eiffel Tower and enjoy panoramic views of Paris',
            location: {
              name: 'Eiffel Tower',
              address: 'Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France',
              coordinates: { lat: 48.8584, lng: 2.2945 }
            },
            timing: {
              startTime: '09:00',
              endTime: '11:00',
              duration: 120,
              suggestedDuration: 120
            },
            cost: {
              pricePerPerson: 25,
              currency: 'EUR',
              priceRange: 'mid-range',
              includesWhat: ['ticket', 'elevator access']
            },
            travel: {
              distanceFromPrevious: 0,
              travelTimeFromPrevious: 0,
              transportMode: 'walking',
              transportCost: 0
            },
            details: {
              rating: 4.5,
              reviewCount: 50000,
              category: 'monument',
              tags: ['iconic', 'views', 'photography'],
              openingHours: {
                monday: { open: '09:30', close: '23:45' },
                tuesday: { open: '09:30', close: '23:45' },
                wednesday: { open: '09:30', close: '23:45' },
                thursday: { open: '09:30', close: '23:45' },
                friday: { open: '09:30', close: '23:45' },
                saturday: { open: '09:30', close: '23:45' },
                sunday: { open: '09:30', close: '23:45' }
              },
              contact: {
                website: 'https://www.toureiffel.paris'
              },
              accessibility: {
                wheelchairAccessible: true,
                elevatorAccess: true,
                restrooms: true,
                parking: false
              },
              amenities: ['elevator', 'restaurant', 'gift shop']
            },
            booking: {
              required: true,
              bookingUrl: 'https://example.com/book',
              notes: 'Book tickets in advance to avoid long queues'
            },
            media: {
              images: ['https://example.com/eiffel-tower.jpg']
            },
            tips: {
              bestTimeToVisit: 'Early morning or evening',
              whatToBring: ['camera', 'comfortable shoes'],
              insider: ['Visit at sunset for best photos'],
              warnings: ['Long queues during peak season']
            },
            priority: 'must-visit'
          }
        ],
        totalDistance: 5,
        totalCost: 25,
        totalDuration: 120,
        startTime: '09:00',
        endTime: '18:00',
        meals: {
          lunch: {
            id: 'meal-1',
            type: 'restaurant',
            name: 'Traditional French Lunch',
            description: 'Enjoy authentic French cuisine at a local bistro',
            location: {
              name: 'Le Comptoir du Relais',
              address: '9 Carrefour de l\'Odéon, 75006 Paris, France',
              coordinates: { lat: 48.8522, lng: 2.3376 }
            },
            timing: {
              startTime: '13:00',
              endTime: '14:30',
              duration: 90,
              suggestedDuration: 90
            },
            cost: {
              pricePerPerson: 35,
              currency: 'EUR',
              priceRange: 'mid-range',
              includesWhat: ['meal', 'drink']
            },
            travel: {
              distanceFromPrevious: 2,
              travelTimeFromPrevious: 15,
              transportMode: 'walking',
              transportCost: 0
            },
            details: {
              rating: 4.2,
              reviewCount: 1200,
              category: 'restaurant',
              tags: ['french', 'bistro', 'traditional'],
              openingHours: {
                monday: { open: '12:00', close: '22:00' },
                tuesday: { open: '12:00', close: '22:00' },
                wednesday: { open: '12:00', close: '22:00' },
                thursday: { open: '12:00', close: '22:00' },
                friday: { open: '12:00', close: '22:00' },
                saturday: { open: '12:00', close: '22:00' },
                sunday: { open: '12:00', close: '22:00' }
              },
              contact: {
                phone: '+33 1 44 27 07 97'
              },
              accessibility: {
                wheelchairAccessible: false,
                elevatorAccess: false,
                restrooms: true,
                parking: false
              },
              amenities: ['outdoor seating', 'wifi']
            },
            booking: {
              required: false,
              notes: 'Try the coq au vin'
            },
            media: {
              images: ['https://example.com/restaurant.jpg']
            },
            tips: {
              bestTimeToVisit: 'Lunch time',
              whatToBring: ['appetite'],
              insider: ['Try the coq au vin'],
              warnings: ['Can be busy during lunch hours']
            },
            priority: 'recommended'
          }
        },
        accommodation: {
          id: 'hotel-1',
          type: 'hotel',
          name: 'Hotel des Invalides',
          description: 'Stay at Hotel des Invalides',
          location: {
            name: 'Hotel des Invalides',
            address: '129 Rue de Grenelle, 75007 Paris, France',
            coordinates: { lat: 48.8566, lng: 2.3122 }
          },
          timing: {
            startTime: '15:00',
            endTime: '11:00',
            duration: 0,
            suggestedDuration: 0
          },
          cost: {
            pricePerPerson: 150,
            currency: 'EUR',
            priceRange: 'mid-range',
            includesWhat: ['room', 'breakfast']
          },
          travel: {
            distanceFromPrevious: 0,
            travelTimeFromPrevious: 0,
            transportMode: 'taxi',
            transportCost: 0
          },
          details: {
            rating: 4.2,
            reviewCount: 100,
            category: 'hotel',
            tags: [],
            openingHours: {},
            contact: {},
            accessibility: {
              wheelchairAccessible: false,
              elevatorAccess: false,
              restrooms: true,
              parking: false
            },
            amenities: ['wifi', 'breakfast', 'gym']
          },
          booking: {
            required: true,
            bookingUrl: 'https://example.com/book',
            notes: ''
          },
          media: {
            images: []
          },
          tips: {
            bestTimeToVisit: 'Check-in time',
            whatToBring: [],
            insider: [],
            warnings: []
          },
          priority: 'must-visit'
        },
        weather: {
          temperature: { min: 15, max: 25 },
          condition: 'sunny',
          precipitation: 0
        },
        notes: 'First day in Paris - explore the city center'
      }
    ];
  }

  /**
   * Transform day components from backend format
   */
  private static transformDayComponents(day: any): TripComponent[] {
    const components: TripComponent[] = [];
    
    // Add activities as components
    if (day.activities) {
      day.activities.forEach((activity: any) => {
        components.push(this.transformActivityToComponent(activity));
      });
    }
    
    // Add accommodation as component
    if (day.accommodation) {
      components.push(this.transformAccommodationToComponent(day.accommodation));
    }
    
    // Add transportation as components
    if (day.transportation) {
      day.transportation.forEach((transport: any) => {
        components.push(this.transformTransportationToComponent(transport));
      });
    }
    
    // Add meals as components
    if (day.meals) {
      day.meals.forEach((meal: any) => {
        components.push(this.transformMealToComponent(meal));
      });
    }
    
    return components;
  }

  /**
   * Transform activity to TripComponent
   */
  private static transformActivityToComponent(activity: any): TripComponent {
    return {
      id: activity.id || Math.random().toString(),
      type: 'attraction',
      name: activity.name || '',
      description: activity.description || '',
      location: {
        name: activity.location?.name || '',
        address: activity.location?.address || '',
        coordinates: {
          lat: activity.location?.lat || 0,
          lng: activity.location?.lng || 0
        }
      },
      timing: {
        startTime: activity.startTime || '09:00',
        endTime: activity.endTime || '17:00',
        duration: this.parseDuration(activity.duration),
        suggestedDuration: this.parseDuration(activity.duration)
      },
      cost: {
        pricePerPerson: activity.price?.amount || 0,
        currency: activity.price?.currency || 'USD',
        priceRange: 'mid-range',
        includesWhat: ['admission']
      },
      travel: {
        distanceFromPrevious: 0,
        travelTimeFromPrevious: 0,
        transportMode: 'walking',
        transportCost: 0
      },
      details: {
        rating: 4.0,
        reviewCount: 100,
        category: activity.category || 'sightseeing',
        tags: [],
        openingHours: {},
        contact: {},
        accessibility: {
          wheelchairAccessible: false,
          elevatorAccess: false,
          restrooms: false,
          parking: false
        },
        amenities: []
      },
      booking: {
        required: activity.bookingRequired || false,
        bookingUrl: activity.bookingUrl || '',
        notes: activity.tips || ''
      },
      media: {
        images: []
      },
      tips: {
        bestTimeToVisit: 'Any time',
        whatToBring: [],
        insider: [],
        warnings: []
      },
      priority: 'recommended'
    };
  }

  /**
   * Transform accommodation to TripComponent
   */
  private static transformAccommodationToComponent(accommodation: any): TripComponent {
    return {
      id: accommodation.id || Math.random().toString(),
      type: 'hotel',
      name: accommodation.name || '',
      description: `Stay at ${accommodation.name}`,
      location: {
        name: accommodation.location?.name || '',
        address: accommodation.location?.address || '',
        coordinates: {
          lat: accommodation.location?.lat || 0,
          lng: accommodation.location?.lng || 0
        }
      },
      timing: {
        startTime: accommodation.checkIn || '15:00',
        endTime: accommodation.checkOut || '11:00',
        duration: 0,
        suggestedDuration: 0
      },
      cost: {
        pricePerPerson: accommodation.price?.amount || 0,
        currency: accommodation.price?.currency || 'USD',
        priceRange: 'mid-range',
        includesWhat: ['room', 'breakfast']
      },
      travel: {
        distanceFromPrevious: 0,
        travelTimeFromPrevious: 0,
        transportMode: 'taxi',
        transportCost: 0
      },
      details: {
        rating: accommodation.rating || 4.0,
        reviewCount: 100,
        category: accommodation.type || 'hotel',
        tags: [],
        openingHours: {},
        contact: {},
        accessibility: {
          wheelchairAccessible: false,
          elevatorAccess: false,
          restrooms: true,
          parking: false
        },
        amenities: accommodation.amenities || []
      },
      booking: {
        required: true,
        bookingUrl: accommodation.bookingUrl || '',
        notes: ''
      },
      media: {
        images: []
      },
      tips: {
        bestTimeToVisit: 'Check-in time',
        whatToBring: [],
        insider: [],
        warnings: []
      },
      priority: 'must-visit'
    };
  }

  /**
   * Parse duration string to minutes
   */
  private static parseDuration(duration: string): number {
    if (!duration) return 60;
    
    const match = duration.match(/(\d+)\s*(hour|minute|hr|min)/i);
    if (match) {
      const value = parseInt(match[1]);
      const unit = match[2].toLowerCase();
      if (unit.includes('hour') || unit.includes('hr')) {
        return value * 60;
      } else {
        return value;
      }
    }
    
    return 60; // Default to 1 hour
  }

  /**
   * Transform backend meals to frontend meals format
   */
  private static transformMeals(meals: any[]): any {
    const result: any = {};
    
    meals.forEach(meal => {
      const mealType = meal.type || 'lunch';
      result[mealType] = this.transformMealToComponent(meal);
    });
    
    return result;
  }

  /**
   * Transform transportation to TripComponent
   */
  private static transformTransportationToComponent(transport: any): TripComponent {
    return {
      id: transport.id || Math.random().toString(),
      type: 'transport',
      name: `${transport.mode} from ${transport.from?.name || 'Unknown'} to ${transport.to?.name || 'Unknown'}`,
      description: `Travel by ${transport.mode}`,
      location: {
        name: transport.from?.name || 'Unknown',
        address: '',
        coordinates: {
          lat: transport.from?.lat || 0,
          lng: transport.from?.lng || 0
        }
      },
      timing: {
        startTime: transport.departureTime || '09:00',
        endTime: transport.arrivalTime || '10:00',
        duration: this.parseDuration(transport.duration),
        suggestedDuration: this.parseDuration(transport.duration)
      },
      cost: {
        pricePerPerson: transport.price?.amount || 0,
        currency: transport.price?.currency || 'USD',
        priceRange: 'mid-range',
        includesWhat: ['transportation']
      },
      travel: {
        distanceFromPrevious: 0,
        travelTimeFromPrevious: 0,
        transportMode: transport.mode?.toLowerCase() || 'taxi',
        transportCost: transport.price?.amount || 0
      },
      details: {
        rating: 4.0,
        reviewCount: 100,
        category: 'transportation',
        tags: [transport.mode?.toLowerCase() || 'transport'],
        openingHours: {},
        contact: {},
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
        bookingUrl: transport.bookingUrl || '',
        notes: transport.notes || ''
      },
      media: {
        images: []
      },
      tips: {
        bestTimeToVisit: 'Check schedule',
        whatToBring: [],
        insider: [],
        warnings: []
      },
      priority: 'recommended'
    };
  }

  /**
   * Transform meal to TripComponent
   */
  private static transformMealToComponent(meal: any): TripComponent {
    return {
      id: meal.id || Math.random().toString(),
      type: 'restaurant',
      name: meal.name || '',
      description: `Enjoy ${meal.cuisine || 'local'} cuisine`,
      location: {
        name: meal.restaurant || meal.location?.name || '',
        address: meal.location?.address || '',
        coordinates: {
          lat: meal.location?.lat || 0,
          lng: meal.location?.lng || 0
        }
      },
      timing: {
        startTime: meal.time || '12:00',
        endTime: meal.time || '13:00',
        duration: 60,
        suggestedDuration: 60
      },
      cost: {
        pricePerPerson: meal.price?.amount || 0,
        currency: meal.price?.currency || 'USD',
        priceRange: 'mid-range',
        includesWhat: ['meal']
      },
      travel: {
        distanceFromPrevious: 0,
        travelTimeFromPrevious: 0,
        transportMode: 'walking',
        transportCost: 0
      },
      details: {
        rating: 4.0,
        reviewCount: 100,
        category: 'restaurant',
        tags: [meal.cuisine || 'local'],
        openingHours: {},
        contact: {},
        accessibility: {
          wheelchairAccessible: false,
          elevatorAccess: false,
          restrooms: true,
          parking: false
        },
        amenities: []
      },
      booking: {
        required: false,
        notes: meal.notes || ''
      },
      media: {
        images: []
      },
      tips: {
        bestTimeToVisit: 'Meal time',
        whatToBring: ['appetite'],
        insider: [],
        warnings: []
      },
      priority: 'recommended'
    };
  }

  /**
   * Transform agent results to frontend format
   */
  private static transformAgentResults(agentResults: any): any {
    if (!agentResults) return {};
    
    return {
      flights: agentResults.flights || [],
      hotels: agentResults.hotels || [],
      restaurants: agentResults.restaurants || [],
      places: agentResults.places || [],
      transport: agentResults.transport || []
    };
  }

  /**
   * Transform interests to preferences
   */
  private static transformInterestsToPreferences(interests: string[]): any {
    const preferences: any = {};
    
    // Map common interests to preference scores
    interests.forEach(interest => {
      switch (interest.toLowerCase()) {
        case 'culture':
        case 'heritage':
          preferences.heritage = 80;
          preferences.culture = 80;
          break;
        case 'nightlife':
          preferences.nightlife = 80;
          break;
        case 'adventure':
          preferences.adventure = 80;
          break;
        case 'relaxation':
          preferences.relaxation = 80;
          break;
        case 'nature':
          preferences.nature = 80;
          break;
        case 'shopping':
          preferences.shopping = 80;
          break;
        case 'cuisine':
        case 'food':
          preferences.cuisine = 80;
          break;
        case 'photography':
          preferences.photography = 80;
          break;
        case 'spiritual':
          preferences.spiritual = 80;
          break;
      }
    });
    
    // Set defaults for missing preferences
    const defaultPreferences = {
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
    };
    
    return { ...defaultPreferences, ...preferences };
  }

  /**
   * Transform constraints to settings
   */
  private static transformConstraintsToSettings(constraints: string[]): any {
    const settings: any = {};
    
    constraints.forEach(constraint => {
      switch (constraint.toLowerCase()) {
        case 'familyfriendly':
          settings.familyFriendly = true;
          break;
        case 'wheelchairaccessible':
          settings.wheelchairAccessible = true;
          break;
        case 'budgetfriendly':
          settings.budgetFriendly = true;
          break;
        case 'luxuryonly':
          settings.luxuryOnly = true;
          break;
        case 'womenfriendly':
          settings.womenFriendly = true;
          break;
        case 'petfriendly':
          settings.petFriendly = true;
          break;
        case 'veganonly':
          settings.veganOnly = true;
          break;
        case 'solotravelsafe':
          settings.soloTravelSafe = true;
          break;
      }
    });
    
    return settings;
  }

  /**
   * Transform party to travelers
   */
  private static transformPartyToTravelers(party: any): any[] {
    const travelers: any[] = [];
    
    // Add adults
    for (let i = 0; i < party.adults; i++) {
      travelers.push({
        id: `adult-${i + 1}`,
        name: `Adult ${i + 1}`,
        email: '',
        age: 30, // Default age
        preferences: {
          dietaryRestrictions: [],
          mobilityNeeds: [],
          interests: []
        }
      });
    }
    
    // Add children
    for (let i = 0; i < party.children; i++) {
      travelers.push({
        id: `child-${i + 1}`,
        name: `Child ${i + 1}`,
        email: '',
        age: 10, // Default age for children
        preferences: {
          dietaryRestrictions: [],
          mobilityNeeds: [],
          interests: []
        }
      });
    }
    
    return travelers;
  }

  /**
   * Extract country from destination string
   */
  private static extractCountry(destination: string): string {
    const parts = destination.split(',');
    return parts.length > 1 ? parts[parts.length - 1].trim() : '';
  }

  /**
   * Extract city from destination string
   */
  private static extractCity(destination: string): string {
    const parts = destination.split(',');
    return parts[0].trim();
  }

  /**
   * Calculate duration in days
   */
  private static calculateDuration(startDate: string, endDate: string): number {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }
}
