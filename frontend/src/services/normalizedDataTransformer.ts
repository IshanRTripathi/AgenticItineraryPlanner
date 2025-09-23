/**
 * Data transformation service to convert normalized JSON from backend
 * to frontend-compatible data structures
 */

import { NormalizedItinerary, NormalizedDay, NormalizedNode } from '../types/NormalizedItinerary';
import { TripData, TripItinerary, DayPlan, TripComponent } from '../types/TripData';

export class NormalizedDataTransformer {
  
  /**
   * Transform normalized itinerary to frontend TripData
   */
  static transformNormalizedItineraryToTripData(normalized: NormalizedItinerary): TripData {
    return {
      id: normalized.itineraryId,
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
        id: normalized.itineraryId,
        name: this.extractDestinationFromSummary(normalized.summary) || (normalized.days?.[0]?.location ?? 'Unknown'),
        country: this.extractCountryFromSummary(normalized.summary),
        city: this.extractCityFromSummary(normalized.summary),
        coordinates: { lat: 0, lng: 0 },
        timezone: 'UTC',
        currency: normalized.currency,
        exchangeRate: 1.0
      },
      isRoundTrip: true,
      dates: this.extractDatesFromDays(normalized.days),
      travelers: this.createDefaultTravelers(),
      leadTraveler: this.createDefaultLeadTraveler(),
      budget: this.calculateBudgetFromDays(normalized.days, normalized.currency),
      preferences: this.transformThemesToPreferences(normalized.themes),
      settings: this.createDefaultSettings(),
      itinerary: this.transformNormalizedItinerary(normalized),
      status: 'completed',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      isPublic: false
    };
  }

  /**
   * Transform NormalizedItinerary to TripItinerary
   */
  private static transformNormalizedItinerary(normalized: NormalizedItinerary): TripItinerary {
    return {
      id: normalized.itineraryId,
      days: (normalized.days || []).map(day => this.transformNormalizedDay(day)),
      totalCost: this.calculateTotalCost(normalized.days),
      totalDistance: this.calculateTotalDistance(normalized.days),
      totalDuration: normalized.days.length,
      highlights: this.extractHighlights(normalized.days),
      themes: normalized.themes,
      difficulty: 'easy',
      packingList: [],
      emergencyInfo: {
        hospitals: [],
        embassies: [],
        police: [],
        helplines: []
      },
      localInfo: {
        currency: normalized.currency,
        language: 'English',
        timeZone: 'UTC',
        customs: [],
        etiquette: [],
        laws: []
      }
    };
  }

  /**
   * Transform NormalizedDay to DayPlan
   */
  private static transformNormalizedDay(day: NormalizedDay): DayPlan {
    return {
      id: day.id || `day-${day.dayNumber}`,
      date: day.date,
      dayNumber: day.dayNumber,
      theme: this.generateThemeFromNodes(day.nodes || []),
      location: day.location,
      components: (day.nodes || []).map(node => this.transformNormalizedNode(node)),
      totalDistance: day.totals?.distanceKm || 0,
      totalCost: day.totals?.cost || 0,
      totalDuration: day.totals?.durationHr || 0,
      startTime: day.timeWindow?.start || '09:00',
      endTime: day.timeWindow?.end || '18:00',
      meals: this.extractMealsFromNodes(day.nodes || []),
      accommodation: this.extractAccommodationFromNodes(day.nodes || []),
      weather: {
        temperature: { min: 15, max: 25 },
        condition: 'sunny',
        precipitation: 0
      },
      notes: day.notes || ''
    };
  }

  /**
   * Transform NormalizedNode to TripComponent
   */
  private static transformNormalizedNode(node: NormalizedNode): TripComponent {
    return {
      id: node.id,
      type: this.mapNodeTypeToComponentType(node.type),
      name: node.title,
      description: this.generateDescriptionFromNode(node),
      location: {
        name: node.location?.name || node.title,
        address: node.location?.address || '',
        coordinates: {
          lat: (node.location?.coordinates && typeof node.location.coordinates.lat === 'number') ? node.location.coordinates.lat : 0,
          lng: (node.location?.coordinates && typeof node.location.coordinates.lng === 'number') ? node.location.coordinates.lng : 0
        }
      },
      timing: {
        startTime: node.timing?.startTime || '09:00',
        endTime: node.timing?.endTime || '10:00',
        duration: node.timing?.durationMin || 60,
        suggestedDuration: node.timing?.durationMin || 60
      },
      cost: {
        pricePerPerson: node.cost?.amount || 0,
        currency: node.cost?.currency || 'USD',
        priceRange: this.mapCostToPriceRange(node.cost?.amount || 0),
        includesWhat: this.generateIncludesFromNode(node)
      },
      travel: {
        distanceFromPrevious: 0,
        travelTimeFromPrevious: 0,
        transportMode: 'walking',
        transportCost: 0
      },
      details: {
        rating: node.details?.rating || 4.0,
        reviewCount: 100,
        category: node.details?.category || node.type,
        tags: node.details?.tags || [],
        openingHours: {},
        contact: {
          website: node.links?.website,
          phone: node.links?.phone
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
        required: node.labels?.includes('Booking Required') || false,
        bookingUrl: node.links?.booking,
        notes: node.tips?.warnings?.join(', ') || ''
      },
      media: {
        images: []
      },
      tips: {
        bestTimeToVisit: node.tips?.bestTime?.join(', ') || 'Any time',
        whatToBring: [],
        insider: node.tips?.travel || [],
        warnings: node.tips?.warnings || []
      },
      priority: this.mapLabelsToPriority(node.labels || [], node)
    };
  }

  /**
   * Map node type to component type
   */
  private static mapNodeTypeToComponentType(nodeType: string): TripComponent['type'] {
    switch (nodeType) {
      case 'attraction': return 'attraction';
      case 'meal': return 'restaurant';
      case 'accommodation': return 'hotel';
      case 'transport': return 'transport';
      default: return 'activity';
    }
  }

  /**
   * Generate description from node
   */
  private static generateDescriptionFromNode(node: NormalizedNode): string {
    switch (node.type) {
      case 'attraction':
        return `Visit ${node.title}`;
      case 'meal':
        return `Enjoy ${node.title}`;
      case 'accommodation':
        return `Stay at ${node.title}`;
      case 'transport':
        return `Travel to ${node.title}`;
      default:
        return node.title;
    }
  }

  /**
   * Map cost to price range
   */
  private static mapCostToPriceRange(cost: number): 'budget' | 'mid-range' | 'luxury' {
    if (cost === 0) return 'budget';
    if (cost < 50) return 'budget';
    if (cost < 150) return 'mid-range';
    return 'luxury';
  }

  /**
   * Generate includes from node
   */
  private static generateIncludesFromNode(node: NormalizedNode): string[] {
    switch (node.type) {
      case 'attraction':
        return ['admission'];
      case 'meal':
        return ['meal'];
      case 'accommodation':
        return ['room'];
      case 'transport':
        return ['transportation'];
      default:
        return [];
    }
  }

  /**
   * Map labels to priority
   */
  private static mapLabelsToPriority(labels: string[], node?: NormalizedNode): 'must-visit' | 'recommended' | 'optional' | 'backup' {
    if (labels.includes('Booking Required')) return 'must-visit';
    if (labels.includes('Booked')) return 'must-visit';
    // Check if node is locked or has booking reference
    if (node && (node.locked || node.bookingRef)) return 'must-visit';
    return 'recommended';
  }

  /**
   * Extract meals from nodes
   */
  private static extractMealsFromNodes(nodes: NormalizedNode[]): DayPlan['meals'] {
    const meals: DayPlan['meals'] = {};
    
    nodes.forEach(node => {
      if (node.type === 'meal') {
        const component = this.transformNormalizedNode(node);
        // Try to determine meal type from timing or title
        const mealType = this.determineMealType(node);
        if (mealType) {
          meals[mealType] = component;
        }
      }
    });
    
    return meals;
  }

  /**
   * Determine meal type from node
   */
  private static determineMealType(node: NormalizedNode): 'breakfast' | 'lunch' | 'dinner' | undefined {
    const title = node.title.toLowerCase();
    const startTime = node.timing?.startTime;
    
    if (title.includes('breakfast') || (startTime && this.isMorningTime(startTime))) {
      return 'breakfast';
    }
    if (title.includes('lunch') || (startTime && this.isLunchTime(startTime))) {
      return 'lunch';
    }
    if (title.includes('dinner') || (startTime && this.isEveningTime(startTime))) {
      return 'dinner';
    }
    
    return undefined;
  }

  /**
   * Extract accommodation from nodes
   */
  private static extractAccommodationFromNodes(nodes: NormalizedNode[]): TripComponent | undefined {
    const accommodationNode = nodes.find(node => node.type === 'accommodation');
    return accommodationNode ? this.transformNormalizedNode(accommodationNode) : undefined;
  }

  /**
   * Generate theme from nodes
   */
  private static generateThemeFromNodes(nodes: NormalizedNode[]): string {
    const types = nodes.map(node => node.type);
    if (types.includes('attraction')) return 'Explore the city';
    if (types.includes('meal')) return 'Culinary experience';
    if (types.includes('accommodation')) return 'Relax and unwind';
    return 'Day activities';
  }

  /**
   * Extract highlights from days
   */
  private static extractHighlights(days: NormalizedDay[]): string[] {
    const highlights: string[] = [];
    
    (days || []).forEach(day => {
      (day.nodes || []).forEach(node => {
        if (node.type === 'attraction' && node.details?.rating && node.details.rating >= 4.5) {
          highlights.push(node.title);
        }
      });
    });
    
    return highlights.slice(0, 5); // Limit to 5 highlights
  }

  /**
   * Calculate total cost from days
   */
  private static calculateTotalCost(days: NormalizedDay[]): number {
    return days.reduce((total, day) => total + (day.totals?.cost || 0), 0);
  }

  /**
   * Calculate total distance from days
   */
  private static calculateTotalDistance(days: NormalizedDay[]): number {
    return days.reduce((total, day) => total + (day.totals?.distanceKm || 0), 0);
  }

  /**
   * Calculate budget from days
   */
  private static calculateBudgetFromDays(days: NormalizedDay[], currency: string): TripData['budget'] {
    const totalCost = this.calculateTotalCost(days);
    
    return {
      total: totalCost,
      currency: currency,
      breakdown: {
        accommodation: totalCost * 0.4,
        food: totalCost * 0.3,
        transport: totalCost * 0.2,
        activities: totalCost * 0.1,
        shopping: 0,
        emergency: 0
      }
    };
  }

  /**
   * Transform themes to preferences
   */
  private static transformThemesToPreferences(themes: string[]): TripData['preferences'] {
    const preferences: TripData['preferences'] = {
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
    
    themes.forEach(theme => {
      switch (theme.toLowerCase()) {
        case 'culture':
          preferences.culture = 80;
          preferences.heritage = 80;
          break;
        case 'food':
          preferences.cuisine = 80;
          break;
        case 'beach':
          preferences.relaxation = 80;
          preferences.nature = 80;
          break;
        case 'architecture':
          preferences.heritage = 80;
          preferences.photography = 80;
          break;
        case 'family':
          preferences.relaxation = 70;
          break;
        case 'romance':
          preferences.relaxation = 80;
          break;
        case 'technology':
          preferences.adventure = 70;
          break;
        case 'tradition':
          preferences.heritage = 80;
          preferences.culture = 80;
          break;
        case 'modern':
          preferences.adventure = 70;
          break;
      }
    });
    
    return preferences;
  }

  /**
   * Create default settings
   */
  private static createDefaultSettings(): TripData['settings'] {
    return {
      womenFriendly: false,
      petFriendly: false,
      veganOnly: false,
      wheelchairAccessible: false,
      budgetFriendly: false,
      luxuryOnly: false,
      familyFriendly: false,
      soloTravelSafe: false
    };
  }

  /**
   * Create default travelers
   */
  private static createDefaultTravelers(): TripData['travelers'] {
    return [this.createDefaultLeadTraveler()];
  }

  /**
   * Create default lead traveler
   */
  private static createDefaultLeadTraveler(): TripData['leadTraveler'] {
    return {
      id: 'lead',
      name: 'Lead Traveler',
      email: '',
      age: 30,
      preferences: {
        dietaryRestrictions: [],
        mobilityNeeds: [],
        interests: []
      }
    };
  }

  /**
   * Extract dates from days
   */
  private static extractDatesFromDays(days: NormalizedDay[]): TripData['dates'] {
    if (days.length === 0) {
      return {
        start: new Date().toISOString().split('T')[0],
        end: new Date().toISOString().split('T')[0]
      };
    }
    
    const sortedDays = [...days].sort((a, b) => a.dayNumber - b.dayNumber);
    return {
      start: sortedDays[0].date,
      end: sortedDays[sortedDays.length - 1].date
    };
  }

  /**
   * Extract destination from summary
   */
  private static extractDestinationFromSummary(summary: string): string {
    // Broaden patterns: "exploration of X", "trip to X", "in X", "X adventure"
    const patterns = [
      /exploration of\s+([A-Z][A-Za-z\s]+?)(,|\.|$)/i,
      /trip to\s+([A-Z][A-Za-z\s]+?)(,|\.|$)/i,
      /to\s+([A-Z][A-Za-z\s]+?)(?:\s+focusing|,|\.|$)/i,
      /in\s+([A-Z][A-Za-z\s]+?)(,|\.|$)/i,
      /([A-Z][A-Za-z\s]+)\s+adventure/i,
      /([A-Z][A-Za-z\s]+)\s+for/i
    ];
    for (const pattern of patterns) {
      const match = summary.match(pattern);
      if (match) {
        return match[1].trim();
      }
    }
    return 'Unknown Destination';
  }

  /**
   * Extract country from summary
   */
  private static extractCountryFromSummary(summary: string): string {
    // Try to extract country from summary
    const match = summary.match(/([A-Z][a-z]+)$/);
    return match ? match[1] : 'Unknown';
  }

  /**
   * Extract city from summary
   */
  private static extractCityFromSummary(summary: string): string {
    // Try to extract city from summary
    const match = summary.match(/to\s+([^,]+)/i);
    return match ? match[1].trim() : 'Unknown City';
  }

  /**
   * Check if time is morning
   */
  private static isMorningTime(time: string): boolean {
    const hour = parseInt(time.split(':')[0]);
    return hour >= 6 && hour < 12;
  }

  /**
   * Check if time is lunch time
   */
  private static isLunchTime(time: string): boolean {
    const hour = parseInt(time.split(':')[0]);
    return hour >= 12 && hour < 15;
  }

  /**
   * Check if time is evening
   */
  private static isEveningTime(time: string): boolean {
    const hour = parseInt(time.split(':')[0]);
    return hour >= 18 && hour < 22;
  }
}
