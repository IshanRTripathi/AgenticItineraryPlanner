import { apiClient } from './apiClient';

/**
 * Validation levels
 */
export enum ValidationLevel {
  BASIC = 'BASIC',
  STANDARD = 'STANDARD',
  COMPREHENSIVE = 'COMPREHENSIVE'
}

/**
 * Validation issue
 */
export interface ValidationIssue {
  id: string;
  type: string;
  category: string;
  severity: 'ERROR' | 'WARNING';
  nodeId?: string;
  dayNumber?: number;
  message: string;
  recommendation?: string;
}

/**
 * Validation summary
 */
export interface ValidationSummary {
  totalErrors: number;
  totalWarnings: number;
  criticalIssues: number;
  filteredIssues: number;
  score: number; // 0-100
}

/**
 * Recommendation
 */
export interface Recommendation {
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  category: string;
  action: string;
  impact: string;
  autoFixable: boolean;
}

/**
 * Validation metadata
 */
export interface ValidationMetadata {
  validatedAt: number;
  validationDuration: number;
  skipped?: boolean;
  skipReason?: string;
}

/**
 * Validation advice
 */
export interface ValidationAdvice {
  success: boolean;
  itineraryId: string;
  level: ValidationLevel;
  summary: ValidationSummary;
  issuesByCategory: Record<string, ValidationIssue[]>;
  recommendations: Recommendation[];
  metadata: ValidationMetadata;
  error?: string;
}

/**
 * Validation API client
 */
export const validationApi = {
  /**
   * Validate an itinerary
   */
  validate: async (
    itineraryId: string,
    level: ValidationLevel = ValidationLevel.STANDARD
  ): Promise<ValidationAdvice> => {
    const response = await apiClient.post(
      `/itineraries/${itineraryId}/validation/validate`,
      null,
      { params: { level } }
    );
    return response.data;
  },

  /**
   * Get validation schema
   */
  getSchema: async (): Promise<any> => {
    const response = await apiClient.get('/itineraries/validation/schema');
    return response.data;
  }
};

/**
 * Get severity color
 */
export const getSeverityColor = (severity: string): string => {
  switch (severity) {
    case 'ERROR':
      return 'red';
    case 'WARNING':
      return 'orange';
    default:
      return 'gray';
  }
};

/**
 * Get priority color
 */
export const getPriorityColor = (priority: string): string => {
  switch (priority) {
    case 'HIGH':
      return 'red';
    case 'MEDIUM':
      return 'orange';
    case 'LOW':
      return 'blue';
    default:
      return 'gray';
  }
};

/**
 * Get score color
 */
export const getScoreColor = (score: number): string => {
  if (score >= 90) return 'green';
  if (score >= 70) return 'blue';
  if (score >= 50) return 'orange';
  return 'red';
};
