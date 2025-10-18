/**
 * TypeScript interfaces for chat functionality
 * These interfaces match the backend DTOs for chat operations
 */

export interface ChatRequest {
  itineraryId: string;
  scope: 'trip' | 'day';
  day?: number;
  selectedNodeId?: string;
  text: string;
  autoApply: boolean;
  // Optional; backend can derive from auth
  userId?: string;
}

export interface ChatResponse {
  intent: string;
  message: string;
  changeSet?: ChangeSet;
  diff?: ItineraryDiff;
  applied: boolean;
  toVersion?: number;
  warnings: string[];
  needsDisambiguation: boolean;
  candidates: NodeCandidate[];
  // Align with backend ChatResponse.errors
  errors?: string[];
}

export interface ChangeSet {
  scope: 'trip' | 'day';
  day?: number;
  ops: ChangeOperation[];
  preferences?: ChangePreferences;
}

export interface ChangeOperation {
  op: 'move' | 'insert' | 'delete' | 'replace' | 'update' | 'update_edge';
  id?: string;
  after?: string;
  startTime?: string;
  endTime?: string;
  node?: NormalizedNode;
}

export interface ChangePreferences {
  userFirst: boolean;
  autoApply: boolean;
  respectLocks: boolean;
}

export interface ItineraryDiff {
  added: DiffItem[];
  removed: DiffItem[];
  updated: DiffItem[];
  toVersion: number;
}

export interface DiffItem {
  nodeId: string;
  day: number;
  fields?: string[];
  title?: string;
}

export interface NodeCandidate {
  id: string;
  title: string;
  day: number;
  type: string;
  location: string;
  confidence?: number;
}

export interface NormalizedNode {
  id: string;
  type: 'attraction' | 'meal' | 'hotel' | 'transit' | 'accommodation' | 'activity';
  title: string;
  location?: NodeLocation;
  timing?: NodeTiming;
  cost?: NodeCost;
  details?: NodeDetails;
  labels?: string[];
  tips?: NodeTips;
  links?: NodeLinks;
  transit?: TransitInfo;
  locked?: boolean;
  bookingRef?: string;
  status: 'planned' | 'in_progress' | 'skipped' | 'cancelled' | 'completed';
  updatedBy?: string;
  updatedAt?: string;
}

export interface NodeLocation {
  name: string;
  address?: string;
  coordinates?: Coordinates;
}

export interface Coordinates {
  lat: number;
  lng: number;
}

export interface NodeTiming {
  // Accept both ms epoch (backend) and formatted strings (frontend)
  startTime: number | string;
  endTime: number | string;
  durationMin: number;
}

export interface NodeCost {
  amount: number;
  currency: string;
  perUnit: string;
}

export interface NodeDetails {
  rating?: number;
  category?: string;
  description?: string;
  tags?: string[];
}

export interface NodeTips {
  warnings?: string[];
  travel?: string[];
  general?: string[];
}

export interface NodeLinks {
  booking?: string;
  website?: string;
  reviews?: string;
}

export interface TransitInfo {
  mode: string;
  duration: number;
  cost?: number;
}

// Chat UI specific types
export interface ChatMessage {
  id: string;
  text: string;
  sender: 'user' | 'assistant';
  // Accept Date or raw backend values for flexibility
  timestamp: Date | number | string;
  intent?: string;
  changeSet?: ChangeSet;
  diff?: ItineraryDiff;
  applied?: boolean;
  warnings?: string[];
  needsDisambiguation?: boolean;
  candidates?: NodeCandidate[];
}

export interface ChatState {
  messages: ChatMessage[];
  isLoading: boolean;
  currentItineraryId?: string;
  selectedNodeId?: string;
  selectedDay?: number;
  scope: 'trip' | 'day';
}

// Intent types for better type safety
export type ChatIntent = 
  | 'REPLAN_TODAY'
  | 'MOVE_TIME'
  | 'INSERT_PLACE'
  | 'DELETE_NODE'
  | 'REPLACE_NODE'
  | 'BOOK_NODE'
  | 'UNDO'
  | 'EXPLAIN'
  | 'DISAMBIGUATION'
  | 'UNKNOWN'
  | 'ERROR';

// Node status types
export type NodeStatus = 
  | 'planned'
  | 'in_progress'
  | 'skipped'
  | 'cancelled'
  | 'completed';

// Node type types
export type NodeType = 
  | 'attraction'
  | 'meal'
  | 'hotel'
  | 'transit'
  | 'accommodation'
  | 'activity';

// Utility types for chat operations
export interface ChatOperationResult {
  success: boolean;
  message: string;
  changeSet?: ChangeSet;
  diff?: ItineraryDiff;
  warnings?: string[];
}

export interface DisambiguationResult {
  candidates: NodeCandidate[];
  message: string;
  requiresUserSelection: boolean;
}
