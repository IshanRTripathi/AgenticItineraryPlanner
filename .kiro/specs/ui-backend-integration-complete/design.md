# UI-Backend Integration - Complete Design

## Overview

This design document outlines the architecture and implementation approach for integrating all backend features into the frontend UI. The design is based on a comprehensive analysis of:

1. **Backend Implementation**: Fully tested with 100% test coverage
2. **Frontend Current State**: Partial integration with gaps in advanced features
3. **API Contract**: RESTful endpoints with SSE for real-time updates

**Design Principles**:
- **Progressive Enhancement**: Build on existing working features
- **Real-time First**: Leverage SSE for immediate UI updates
- **User Control**: Provide manual controls alongside AI automation
- **Unified State**: Single source of truth with bidirectional sync
- **Error Resilience**: Graceful degradation and clear error messaging

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend Application                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  TravelPlanner│  │ WorkflowBuilder│ │ ChatInterface│      │
│  │   Component  │  │   Component   │  │  Component   │      │
│  └──────┬───────┘  └──────┬────────┘  └──────┬───────┘      │
│         │                  │                   │              │
│         └──────────────────┼───────────────────┘              │
│                            │                                  │
│  ┌─────────────────────────▼──────────────────────────────┐  │
│  │         UnifiedItineraryContext (State Management)      │  │
│  │  - Itinerary State                                      │  │
│  │  - Workflow State                                       │  │
│  │  - Chat State                                           │  │
│  │  - Sync Status                                          │  │
│  └─────────────────────────┬──────────────────────────────┘  │
│                            │                                  │
│  ┌─────────────────────────▼──────────────────────────────┐  │
│  │              Service Layer                              │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │  │
│  │  │ apiClient│ │chatService│ │sseService│ │syncService│  │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │  │
│  └─────────────────────────┬──────────────────────────────┘  │
│                            │                                  │
└────────────────────────────┼──────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   HTTP/SSE      │
                    └────────┬────────┘
                             │
┌────────────────────────────▼──────────────────────────────────┐
│                     Backend Services                          │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Itineraries  │  │    Chat      │  │    Agent     │       │
│  │  Controller  │  │  Controller  │  │  Controller  │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                  │                  │               │
│  ┌──────▼──────────────────▼──────────────────▼───────┐      │
│  │              Service Layer                          │      │
│  │  - ItineraryJsonService (Single Source of Truth)    │      │
│  │  - ChangeEngine (Change Management)                 │      │
│  │  - OrchestratorService (Chat & Agents)              │      │
│  │  - SseConnectionManager (Real-time Updates)         │      │
│  └─────────────────────────┬───────────────────────────┘      │
│                            │                                   │
│  ┌─────────────────────────▼───────────────────────────┐      │
│  │              Firestore Database                      │      │
│  │  - itineraries/{id}/master.json                     │      │
│  │  - itineraries/{id}/revisions/{version}             │      │
│  │  - itineraries/{id}/workflow                         │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Data Flow

#### 1. Change Management Flow

```
User Action → Change Preview → User Approval → Change Application → SSE Update → UI Refresh

Detailed Flow:
1. User makes change in UI (e.g., edits node)
2. Frontend calls POST /{id}:propose with ChangeSet
3. Backend returns ProposeResponse with diff
4. Frontend displays preview modal with diff
5. User approves/rejects
6. If approved: Frontend calls POST /{id}:apply
7. Backend applies changes and broadcasts SSE event
8. Frontend receives SSE event and updates UI
9. All connected clients receive update
```

#### 2. Workflow-Itinerary Sync Flow

```
Workflow Change ↔ Sync Service ↔ Itinerary Change
                      ↓
                Backend Persistence
                      ↓
                  SSE Broadcast
                      ↓
                All Connected Clients

Detailed Flow:
1. User modifies workflow node
2. Sync service detects change
3. Sync service updates itinerary state
4. Sync service calls PUT /{id}/workflow
5. Backend persists workflow data
6. Backend broadcasts SSE event
7. All clients receive update
8. Clients update both workflow and itinerary views
```

#### 3. Real-Time Collaboration Flow

```
User A Change → Backend → SSE Broadcast → User B Receives Update
                                        → User C Receives Update

Detailed Flow:
1. User A makes change
2. Backend processes change
3. Backend broadcasts SSE event to all connections for that itinerary
4. User B's client receives SSE event
5. User B's UI updates automatically
6. User C's client receives SSE event
7. User C's UI updates automatically
8. Conflict detection if simultaneous edits
```

## Components and Interfaces

### 1. Change Management Components

#### ChangePreviewModal Component

**Purpose**: Display proposed changes before applying them

**Props**:
```typescript
interface ChangePreviewModalProps {
  isOpen: boolean;
  changeSet: ChangeSet;
  proposed: NormalizedItinerary;
  diff: ItineraryDiff;
  onApprove: () => void;
  onReject: () => void;
  onClose: () => void;
}
```

**State**:
```typescript
interface ChangePreviewModalState {
  loading: boolean;
  error: string | null;
  selectedDiffItem: DiffItem | null;
}
```

**API Integration**:
- Uses `apiClient.proposeChanges(id, changeSet)` to get preview
- Uses `apiClient.applyChanges(id, { changeSet })` to apply

**UI Elements**:
- Diff viewer showing added/removed/modified items
- Side-by-side comparison
- Approve/Reject buttons
- Loading state during preview generation

#### UndoRedoControls Component

**Purpose**: Provide undo/redo functionality

**Props**:
```typescript
interface UndoRedoControlsProps {
  itineraryId: string;
  currentVersion: number;
  canUndo: boolean;
  canRedo: boolean;
  onUndo: () => void;
  onRedo: () => void;
}
```

**API Integration**:
- Uses `apiClient.undoChanges(id, { toVersion })` to undo
- Listens to SSE events for version updates

**UI Elements**:
- Undo button (Ctrl+Z)
- Redo button (Ctrl+Y)
- Version indicator
- Keyboard shortcuts

#### ChangeHistoryPanel Component

**Purpose**: Display chronological list of changes

**Props**:
```typescript
interface ChangeHistoryPanelProps {
  itineraryId: string;
  onVersionSelect: (version: number) => void;
}
```

**State**:
```typescript
interface ChangeHistoryPanelState {
  revisions: RevisionRecord[];
  loading: boolean;
  error: string | null;
  selectedRevision: RevisionRecord | null;
}
```

**API Integration**:
- Uses `GET /{id}/revisions` to fetch history
- Uses `GET /{id}/revisions/{revisionId}` for details
- Uses `POST /{id}/revisions/{revisionId}/rollback` to restore

**UI Elements**:
- Timeline view of changes
- Change details on hover
- Restore button for each version
- Diff viewer for comparing versions

### 2. Node Locking Components

#### NodeLockToggle Component

**Purpose**: Toggle lock state of a node

**Props**:
```typescript
interface NodeLockToggleProps {
  itineraryId: string;
  nodeId: string;
  locked: boolean;
  onLockChange: (locked: boolean) => void;
}
```

**API Integration**:
- Uses `PUT /{id}/nodes/{nodeId}/lock` with `{ locked: boolean }`

**UI Elements**:
- Lock/Unlock icon button
- Visual indicator when locked
- Tooltip explaining lock behavior

#### LockedNodeIndicator Component

**Purpose**: Visual indicator for locked nodes

**Props**:
```typescript
interface LockedNodeIndicatorProps {
  locked: boolean;
  size?: 'sm' | 'md' | 'lg';
}
```

**UI Elements**:
- Lock icon overlay
- Color coding (locked = red, unlocked = green)
- Animation on state change

### 3. Workflow-Itinerary Sync Components

#### WorkflowSyncService

**Purpose**: Manage bidirectional sync between workflow and itinerary

**Interface**:
```typescript
interface WorkflowSyncService {
  // Sync workflow node to itinerary component
  syncNodeToComponent(node: WorkflowNode): Promise<void>;
  
  // Sync itinerary component to workflow node
  syncComponentToNode(component: NormalizedNode): Promise<void>;
  
  // Batch sync multiple changes
  syncBatch(changes: SyncChange[]): Promise<void>;
  
  // Resolve sync conflicts
  resolveConflict(conflict: SyncConflict): Promise<void>;
  
  // Get sync status
  getSyncStatus(): SyncStatus;
}

interface SyncChange {
  type: 'node' | 'component';
  operation: 'add' | 'update' | 'delete';
  data: WorkflowNode | NormalizedNode;
}

interface SyncConflict {
  nodeId: string;
  workflowVersion: WorkflowNode;
  itineraryVersion: NormalizedNode;
  timestamp: number;
}

interface SyncStatus {
  syncing: boolean;
  lastSync: number;
  pendingChanges: number;
  conflicts: SyncConflict[];
}
```

**Implementation**:
```typescript
class WorkflowSyncServiceImpl implements WorkflowSyncService {
  private syncQueue: SyncChange[] = [];
  private syncing = false;
  private debounceTimer: NodeJS.Timeout | null = null;
  
  async syncNodeToComponent(node: WorkflowNode): Promise<void> {
    // Convert workflow node to itinerary component
    const component = this.convertNodeToComponent(node);
    
    // Add to sync queue
    this.syncQueue.push({
      type: 'node',
      operation: 'update',
      data: node
    });
    
    // Debounce sync
    this.debouncedSync();
  }
  
  private debouncedSync() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
    
    this.debounceTimer = setTimeout(() => {
      this.processSyncQueue();
    }, 300); // 300ms debounce
  }
  
  private async processSyncQueue() {
    if (this.syncing || this.syncQueue.length === 0) {
      return;
    }
    
    this.syncing = true;
    
    try {
      // Batch changes
      const changes = [...this.syncQueue];
      this.syncQueue = [];
      
      // Apply changes to backend
      await this.applyChangesToBackend(changes);
      
      // Broadcast success
      this.broadcastSyncSuccess();
    } catch (error) {
      // Handle error
      this.handleSyncError(error);
    } finally {
      this.syncing = false;
    }
  }
  
  private convertNodeToComponent(node: WorkflowNode): NormalizedNode {
    return {
      id: node.id,
      type: node.data.type.toLowerCase(),
      name: node.data.title,
      timing: {
        startTime: node.data.start,
        duration: `${node.data.durationMin}m`
      },
      cost: {
        pricePerPerson: node.data.costINR
      },
      location: {
        address: node.data.meta.address
      },
      details: {
        rating: node.data.meta.rating,
        tags: node.data.tags
      }
    };
  }
}
```

### 4. Agent Control Components

#### AgentControlPanel Component

**Purpose**: Display and control available agents

**Props**:
```typescript
interface AgentControlPanelProps {
  itineraryId: string;
  availableAgents: AgentCapability[];
  onExecuteAgent: (agentType: string, params: any) => void;
}
```

**State**:
```typescript
interface AgentControlPanelState {
  selectedAgent: AgentCapability | null;
  executingAgents: Map<string, AgentExecutionStatus>;
  agentHistory: AgentExecution[];
}
```

**API Integration**:
- Uses `POST /{id}/agents/{agentType}/execute` to trigger agent
- Uses `GET /{id}/agents/{agentType}/status` to check status
- Listens to SSE for agent progress updates

**UI Elements**:
- Grid of available agents with icons
- Agent configuration modal
- Execution progress indicators
- History panel

#### AgentExecutionProgress Component

**Purpose**: Display real-time agent execution progress

**Props**:
```typescript
interface AgentExecutionProgressProps {
  itineraryId: string;
  agentType: string;
  executionId: string;
}
```

**State**:
```typescript
interface AgentExecutionProgressState {
  status: 'queued' | 'running' | 'completed' | 'failed';
  progress: number;
  currentStep: string;
  logs: string[];
  result: any | null;
  error: string | null;
}
```

**SSE Integration**:
- Connects to `/agents/stream?itineraryId={id}`
- Listens for agent progress events
- Updates progress bar and logs in real-time

**UI Elements**:
- Progress bar
- Current step indicator
- Log viewer
- Cancel button
- Results display

### 5. Enhanced Chat Components

#### ChatPreviewMode Component

**Purpose**: Preview chat-driven changes before applying

**Props**:
```typescript
interface ChatPreviewModeProps {
  enabled: boolean;
  onToggle: (enabled: boolean) => void;
}
```

**UI Elements**:
- Toggle switch
- Explanation tooltip
- Visual indicator when enabled

#### ChatDisambiguationPanel Component

**Purpose**: Handle disambiguation requests from chat

**Props**:
```typescript
interface ChatDisambiguationPanelProps {
  candidates: NodeCandidate[];
  onSelect: (candidate: NodeCandidate) => void;
  onCancel: () => void;
}
```

**UI Elements**:
- List of candidate options
- Preview for each option
- Select button
- Cancel button

### 6. Revision Management Components

#### RevisionTimeline Component

**Purpose**: Display timeline of itinerary versions

**Props**:
```typescript
interface RevisionTimelineProps {
  itineraryId: string;
  onVersionSelect: (version: number) => void;
}
```

**State**:
```typescript
interface RevisionTimelineState {
  revisions: RevisionRecord[];
  selectedRevision: RevisionRecord | null;
  loading: boolean;
  error: string | null;
}
```

**API Integration**:
- Uses `GET /{id}/revisions` to fetch timeline
- Uses `GET /{id}/revisions/{revisionId}` for details

**UI Elements**:
- Vertical timeline
- Version cards with metadata
- Diff preview on hover
- Restore button

#### RevisionDiffViewer Component

**Purpose**: Display differences between versions

**Props**:
```typescript
interface RevisionDiffViewerProps {
  fromVersion: RevisionRecord;
  toVersion: RevisionRecord;
}
```

**UI Elements**:
- Side-by-side comparison
- Highlighted changes
- Color coding (added = green, removed = red, modified = yellow)

## Data Models

### Frontend State Models

#### UnifiedItineraryState

```typescript
interface UnifiedItineraryState {
  // Core data
  itinerary: NormalizedItinerary | null;
  workflow: WorkflowData | null;
  chatMessages: ChatMessage[];
  
  // Sync status
  syncStatus: {
    syncing: boolean;
    lastSync: number;
    pendingChanges: number;
    conflicts: SyncConflict[];
  };
  
  // Selection state
  selectedNodeId: string | null;
  selectedDay: number | null;
  
  // UI state
  loading: boolean;
  error: string | null;
  hasUnsavedChanges: boolean;
  
  // Change management
  currentVersion: number;
  canUndo: boolean;
  canRedo: boolean;
  changeHistory: RevisionRecord[];
  
  // Agent state
  executingAgents: Map<string, AgentExecutionStatus>;
  agentHistory: AgentExecution[];
}
```

#### ChangeSet (Backend DTO)

```typescript
interface ChangeSet {
  scope: 'trip' | 'day';
  day?: number;
  ops: ChangeOperation[];
  preferences?: ChangePreferences;
}

interface ChangeOperation {
  op: 'add' | 'update' | 'delete' | 'move';
  id?: string;
  node?: NormalizedNode;
  position?: number;
  dayNumber?: number;
}

interface ChangePreferences {
  userFirst: boolean;
  respectLocks: boolean;
  autoEnrich: boolean;
}
```

#### ProposeResponse (Backend DTO)

```typescript
interface ProposeResponse {
  proposed: NormalizedItinerary;
  diff: ItineraryDiff;
  previewVersion: number;
}

interface ItineraryDiff {
  added: DiffItem[];
  removed: DiffItem[];
  modified: DiffItem[];
}

interface DiffItem {
  type: 'node' | 'day' | 'metadata';
  id: string;
  path: string;
  before?: any;
  after?: any;
}
```

## Error Handling

### Error Types

```typescript
enum ErrorType {
  NETWORK_ERROR = 'NETWORK_ERROR',
  AUTH_ERROR = 'AUTH_ERROR',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  CONFLICT_ERROR = 'CONFLICT_ERROR',
  NOT_FOUND_ERROR = 'NOT_FOUND_ERROR',
  SERVER_ERROR = 'SERVER_ERROR',
  SYNC_ERROR = 'SYNC_ERROR',
  AGENT_ERROR = 'AGENT_ERROR'
}

interface AppError {
  type: ErrorType;
  message: string;
  details?: any;
  recoverable: boolean;
  retryable: boolean;
}
```

### Error Handling Strategy

1. **Network Errors**: Automatic retry with exponential backoff
2. **Auth Errors**: Redirect to login, refresh token if possible
3. **Validation Errors**: Display inline validation messages
4. **Conflict Errors**: Show conflict resolution UI
5. **Not Found Errors**: Display helpful message with navigation options
6. **Server Errors**: Display error message with retry option
7. **Sync Errors**: Queue changes for retry, show sync status
8. **Agent Errors**: Display error in agent panel, allow retry

### Error Recovery

```typescript
class ErrorRecoveryService {
  async handleError(error: AppError): Promise<void> {
    switch (error.type) {
      case ErrorType.NETWORK_ERROR:
        if (error.retryable) {
          await this.retryWithBackoff(error);
        } else {
          this.showOfflineMode();
        }
        break;
        
      case ErrorType.CONFLICT_ERROR:
        await this.showConflictResolution(error);
        break;
        
      case ErrorType.SYNC_ERROR:
        await this.queueForRetry(error);
        break;
        
      default:
        this.showErrorMessage(error);
    }
  }
  
  private async retryWithBackoff(error: AppError): Promise<void> {
    const maxRetries = 3;
    const baseDelay = 1000;
    
    for (let i = 0; i < maxRetries; i++) {
      try {
        await this.retry(error);
        return;
      } catch (retryError) {
        if (i === maxRetries - 1) {
          throw retryError;
        }
        await this.delay(baseDelay * Math.pow(2, i));
      }
    }
  }
}
```

## Testing Strategy

### Unit Tests

1. **Component Tests**:
   - ChangePreviewModal rendering and interactions
   - UndoRedoControls keyboard shortcuts
   - NodeLockToggle state management
   - AgentControlPanel agent selection

2. **Service Tests**:
   - WorkflowSyncService sync logic
   - ErrorRecoveryService error handling
   - SSE connection management
   - API client request/response handling

3. **State Management Tests**:
   - UnifiedItineraryContext state updates
   - Sync conflict resolution
   - Change history management
   - Agent execution tracking

### Integration Tests

1. **Change Management Flow**:
   - Propose → Preview → Apply → SSE Update
   - Undo → Rollback → SSE Update
   - Conflict detection and resolution

2. **Workflow-Itinerary Sync**:
   - Node changes sync to components
   - Component changes sync to nodes
   - Batch sync operations
   - Conflict handling

3. **Real-Time Collaboration**:
   - Multiple clients receive updates
   - Concurrent edits handled correctly
   - Presence indicators work
   - Conflict resolution UI appears

### End-to-End Tests

1. **User Workflows**:
   - Create itinerary → Edit → Preview → Apply → Undo
   - Lock node → Attempt AI edit → Verify locked
   - Execute agent → Monitor progress → View results
   - Chat edit → Preview → Apply → Verify sync

2. **Error Scenarios**:
   - Network failure → Offline mode → Reconnect → Sync
   - Concurrent edits → Conflict → Resolution
   - Agent failure → Error display → Retry

## Performance Optimization

### Optimization Strategies

1. **Debouncing**:
   - Workflow sync: 300ms debounce
   - Search input: 300ms debounce
   - Auto-save: 1000ms debounce

2. **Caching**:
   - Cache itinerary data in memory
   - Cache workflow positions in localStorage
   - Cache agent results for 5 minutes

3. **Lazy Loading**:
   - Load change history on demand
   - Load agent history on demand
   - Load revision details on demand

4. **Virtual Scrolling**:
   - Use virtual scrolling for large itineraries
   - Use virtual scrolling for change history
   - Use virtual scrolling for agent logs

5. **Optimistic Updates**:
   - Update UI immediately on user action
   - Rollback if backend fails
   - Show loading state during sync

### Performance Metrics

- Initial load time: <2s
- Change preview generation: <500ms
- Workflow sync latency: <200ms
- SSE event processing: <100ms
- UI update after change: <500ms

## Security Considerations

### Authentication

- All API calls include Firebase auth token
- Token refresh handled automatically
- Expired tokens trigger re-authentication

### Authorization

- Backend validates user ownership of itineraries
- Frontend hides actions user cannot perform
- Error messages don't leak sensitive information

### Data Validation

- Frontend validates all user input
- Backend validates all requests
- Sanitize user-generated content

### XSS Prevention

- Use React's built-in XSS protection
- Sanitize HTML in user content
- Use Content Security Policy headers

## Deployment Strategy

### Phased Rollout

**Phase 1: Change Management (Week 1-2)**
- Deploy ChangePreviewModal
- Deploy UndoRedoControls
- Deploy ChangeHistoryPanel
- Feature flag: `ENABLE_CHANGE_PREVIEW`

**Phase 2: Node Locking (Week 2-3)**
- Deploy NodeLockToggle
- Deploy LockedNodeIndicator
- Update agent logic to respect locks
- Feature flag: `ENABLE_NODE_LOCKING`

**Phase 3: Workflow Sync (Week 3-5)**
- Deploy WorkflowSyncService
- Update WorkflowBuilder
- Update DayByDayView
- Feature flag: `ENABLE_WORKFLOW_SYNC`

**Phase 4: Agent Controls (Week 5-6)**
- Deploy AgentControlPanel
- Deploy AgentExecutionProgress
- Deploy agent history
- Feature flag: `ENABLE_AGENT_CONTROLS`

**Phase 5: Enhanced Chat (Week 6-7)**
- Deploy ChatPreviewMode
- Deploy ChatDisambiguationPanel
- Update chat UI
- Feature flag: `ENABLE_CHAT_PREVIEW`

**Phase 6: Revision Management (Week 7-8)**
- Deploy RevisionTimeline
- Deploy RevisionDiffViewer
- Update UI with history access
- Feature flag: `ENABLE_REVISION_HISTORY`

### Feature Flags

```typescript
interface FeatureFlags {
  ENABLE_CHANGE_PREVIEW: boolean;
  ENABLE_NODE_LOCKING: boolean;
  ENABLE_WORKFLOW_SYNC: boolean;
  ENABLE_AGENT_CONTROLS: boolean;
  ENABLE_CHAT_PREVIEW: boolean;
  ENABLE_REVISION_HISTORY: boolean;
  ENABLE_OFFLINE_MODE: boolean;
  ENABLE_COLLABORATION: boolean;
}
```

### Monitoring

- Track feature adoption rates
- Monitor error rates per feature
- Track performance metrics
- Monitor SSE connection health
- Track sync success/failure rates

## Conclusion

This design provides a comprehensive approach to integrating all backend features into the frontend UI. The phased rollout strategy allows for incremental delivery and validation, while the feature flag system enables safe deployment and rollback if needed.

Key success factors:
1. **Real-time updates**: SSE ensures all clients stay in sync
2. **User control**: Manual controls complement AI automation
3. **Error resilience**: Comprehensive error handling and recovery
4. **Performance**: Optimizations ensure smooth user experience
5. **Testing**: Comprehensive test coverage ensures quality
