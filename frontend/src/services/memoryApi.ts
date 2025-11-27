import { apiClient } from './apiClient';

/**
 * Memory categories
 */
export enum MemoryCategory {
  PROFILE = 'PROFILE',
  DIETARY = 'DIETARY',
  VALIDATION = 'VALIDATION',
  ACTIVITY = 'ACTIVITY',
  BUDGET = 'BUDGET',
  TIMING = 'TIMING',
  TRANSPORT = 'TRANSPORT',
  MEAL = 'MEAL',
  INSTRUCTION = 'INSTRUCTION',
  HISTORY = 'HISTORY'
}

/**
 * Memory types
 */
export enum MemoryType {
  PREFERENCE = 'PREFERENCE',
  RESTRICTION = 'RESTRICTION',
  DISMISSAL = 'DISMISSAL',
  PATTERN = 'PATTERN',
  BEHAVIOR = 'BEHAVIOR',
  EXPLICIT = 'EXPLICIT',
  ITINERARY_SUMMARY = 'ITINERARY_SUMMARY'
}

/**
 * Memory interface
 */
export interface Memory {
  id: string;
  itineraryId: string;
  userId: string;
  category: MemoryCategory;
  type: MemoryType;
  data: Record<string, any>;
  confidence: number;
  createdAt: number;
  lastUpdated: number;
  lastUsed?: number;
  source: string; // USER_STATED, LEARNED, TRIP_CREATION
  isPersonal: boolean;
  learnedFrom?: string[];
  sticky?: boolean;
}

/**
 * User profile (aggregated memories)
 */
export interface UserProfile {
  userId: string;
  activityPreferences?: Record<string, any>;
  budgetBehavior?: Record<string, any>;
  timingPatterns?: Record<string, any>;
  mealPreferences?: Record<string, any>;
  dietaryRestrictions?: Record<string, any>;
  validationPreferences?: Record<string, any>;
}

/**
 * Memory API client
 */
export const memoryApi = {
  /**
   * Get all memories for an itinerary
   */
  getAll: async (itineraryId: string, category?: MemoryCategory): Promise<Memory[]> => {
    const params = category ? { category } : {};
    const response = await apiClient.get(`/itineraries/${itineraryId}/memory`, { params });
    return response.data;
  },

  /**
   * Create a new memory
   */
  create: async (itineraryId: string, memory: Partial<Memory>): Promise<Memory> => {
    const response = await apiClient.post(`/itineraries/${itineraryId}/memory`, memory);
    return response.data;
  },

  /**
   * Update an existing memory
   */
  update: async (
    itineraryId: string,
    memoryId: string,
    updates: Partial<Memory>
  ): Promise<Memory> => {
    const response = await apiClient.patch(
      `/itineraries/${itineraryId}/memory/${memoryId}`,
      updates
    );
    return response.data;
  },

  /**
   * Delete a memory
   */
  delete: async (itineraryId: string, memoryId: string): Promise<void> => {
    await apiClient.delete(`/itineraries/${itineraryId}/memory/${memoryId}`);
  },

  /**
   * Get user profile (aggregated memories)
   */
  getProfile: async (itineraryId: string): Promise<UserProfile> => {
    const response = await apiClient.get(`/itineraries/${itineraryId}/memory/profile`);
    return response.data;
  }
};

/**
 * Calculate current confidence with decay
 */
export const calculateConfidenceWithDecay = (memory: Memory): number => {
  const HALF_LIFE_MS = 180 * 24 * 60 * 60 * 1000; // 6 months
  const age = Date.now() - memory.lastUpdated;
  const decayFactor = Math.pow(0.5, age / HALF_LIFE_MS);
  const currentConfidence = memory.confidence * decayFactor;
  return Math.max(0.1, currentConfidence);
};

/**
 * Format memory label for display
 */
export const formatMemoryLabel = (memory: Memory): string => {
  const { category, type } = memory;
  
  if (category === MemoryCategory.ACTIVITY) {
    return 'Activity Preferences';
  } else if (category === MemoryCategory.BUDGET) {
    return 'Budget Behavior';
  } else if (category === MemoryCategory.DIETARY) {
    return 'Dietary Restrictions';
  } else if (category === MemoryCategory.TIMING) {
    return 'Travel Pace';
  } else if (category === MemoryCategory.MEAL) {
    return 'Dining Preferences';
  } else if (category === MemoryCategory.VALIDATION) {
    return 'Validation Preferences';
  }
  
  return category;
};

/**
 * Format memory value for display
 */
export const formatMemoryValue = (memory: Memory): string => {
  const { data, type } = memory;
  
  if (type === MemoryType.PREFERENCE) {
    if (data.preferredTypes) {
      return data.preferredTypes.join(', ');
    } else if (data.budgetTier) {
      return data.budgetTier;
    } else if (data.pace) {
      return data.pace;
    }
  } else if (type === MemoryType.RESTRICTION) {
    if (data.restrictions) {
      return data.restrictions.join(', ');
    }
  } else if (type === MemoryType.DISMISSAL) {
    return `Dismissed: ${data.issueType}`;
  }
  
  return JSON.stringify(data);
};

/**
 * Format relative time
 */
export const formatRelativeTime = (timestamp: number): string => {
  const now = Date.now();
  const diff = now - timestamp;
  
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  const weeks = Math.floor(days / 7);
  const months = Math.floor(days / 30);
  
  if (months > 0) return `${months} month${months > 1 ? 's' : ''} ago`;
  if (weeks > 0) return `${weeks} week${weeks > 1 ? 's' : ''} ago`;
  if (days > 0) return `${days} day${days > 1 ? 's' : ''} ago`;
  if (hours > 0) return `${hours} hour${hours > 1 ? 's' : ''} ago`;
  if (minutes > 0) return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
  return 'just now';
};
