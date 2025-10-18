# Q&A Tracking Document

## Question 1: What happens when user logs in to the system?

### Brief Answer:
The system uses Firebase Authentication with Google Sign-in only. When a user logs in:
1. **Frontend**: User clicks "Sign in with Google" → Firebase popup → JWT token received → AuthContext updates → API client gets auth token → Protected routes become accessible
2. **Backend**: Firebase Auth Filter validates JWT tokens on API requests → Extracts userId from token → Stores userId in request attributes for controllers

### Detailed Analysis:

#### Frontend Authentication Flow:
**Files involved:**
- `frontend/src/components/LoginPage.tsx` - Login UI
- `frontend/src/components/GoogleSignIn.tsx` - Google sign-in button
- `frontend/src/services/authService.ts` - Authentication service
- `frontend/src/contexts/AuthContext.tsx` - Global auth state
- `frontend/src/components/ProtectedRoute.tsx` - Route protection
- `frontend/src/config/firebase.ts` - Firebase configuration

**Step-by-step process:**
1. **Login Page Display**: User visits `/login` route, sees `LoginPage` component with `GoogleSignIn` button
2. **Google Sign-in Initiation**: User clicks button → `authService.signInWithGoogle()` → Firebase `signInWithPopup()` with Google provider
3. **Firebase Authentication**: Google OAuth popup → User authenticates → Firebase returns JWT token
4. **Auth State Update**: `AuthContext` listens via `onAuthStateChanged()` → Updates user state → Sets loading to false
5. **API Token Setup**: AuthContext gets ID token via `authService.getIdToken()` → Sets token in `apiClient.setAuthToken()`
6. **Route Protection**: `ProtectedRoute` component checks auth state → Redirects authenticated users from `/login` to `/dashboard`
7. **Token Refresh**: AuthContext sets 45-minute interval to refresh tokens proactively

#### Backend Authentication Flow:
**Files involved:**
- `src/main/java/com/tripplanner/config/FirebaseAuthConfig.java` - Auth filter configuration

**Request Processing:**
1. **Filter Registration**: `FirebaseAuthFilter` registered for `/api/v1/*` endpoints with highest precedence
2. **Request Interception**: Every API request goes through `doFilterInternal()`
3. **Public Endpoint Check**: Allows health, public, SSE, and documentation endpoints without auth
4. **Token Extraction**: Extracts JWT from `Authorization: Bearer <token>` header
5. **Token Validation**: `firebaseAuth.verifyIdToken()` validates token with Firebase
6. **User Context**: Extracts `userId`, `userEmail`, `userName` from token → Stores in request attributes
7. **Controller Access**: Controllers access user info via `request.getAttribute("userId")`

#### Special Cases:
- **OPTIONS requests**: Bypass authentication (CORS preflight)
- **JSON endpoints**: Optional authentication - continues without auth if token invalid
- **Public endpoints**: Health, documentation, SSE streams don't require auth
- **Token expiration**: Returns 401 with error message for invalid/expired tokens

#### User Data Organization:
**Files involved:**
- `src/main/java/com/tripplanner/service/UserDataService.java` - User-specific data management

**Data Structure:**
- Firestore: `users/{userId}/itineraries/{itineraryId}` → TripMetadata
- All user operations use authenticated userId from request attributes
- User ownership validation on data access

---

## Question 2: What different agents are involved in the process of itinerary creation, how are we managing roles and responsibilities?

### Brief Answer:
The system uses a multi-agent architecture with specialized agents for different aspects of itinerary creation:
1. **PlannerAgent** - Creates initial itineraries from user requirements
2. **EditorAgent** - Handles user-driven modifications and edits
3. **EnrichmentAgent** - Adds detailed information to places/activities
4. **AgentCoordinator** - Routes tasks to appropriate agents based on capabilities
5. **AgentRegistry** - Manages agent registration, capabilities, and discovery
6. **AgentTaskSystem** - Provides durable task processing with retry mechanisms

### Detailed Analysis:

#### Agent Architecture Overview:
**Core Components:**
- `BaseAgent` - Abstract base class for all agents with common execution framework
- `AgentRegistry` - Central registry for agent discovery and capability management
- `AgentCoordinator` - Intelligent task routing based on agent capabilities
- `AgentTaskSystem` - Durable task processing with Firestore persistence

#### Individual Agent Responsibilities:

**1. PlannerAgent** (`src/main/java/com/tripplanner/agents/PlannerAgent.java`)
- **Primary Role**: Initial itinerary creation from user requirements
- **Capabilities**: 
  - Tasks: `plan`, `create`, `edit`, `modify`
  - Data Sections: `itinerary`, `activities`, `meals`, `accommodation`, `transportation`
  - Priority: 10 (high priority for core planning)
- **Key Functions**:
  - Processes `CreateItineraryReq` with destination, dates, preferences
  - Uses AI client to generate structured JSON itineraries
  - Handles multi-day planning with realistic timing and logistics
  - Manages accommodation checkout/checkin timing between cities
  - Generates ChangeSet for modifications to existing itineraries
- **Integration**: Works with `ItineraryJsonService`, `UserDataService`, `LLMResponseHandler`

**2. EditorAgent** (`src/main/java/com/tripplanner/agents/EditorAgent.java`)
- **Primary Role**: User-driven itinerary modifications and chat-based editing
- **Capabilities**:
  - Tasks: `edit`, `modify`, `update`, `summarize`
  - Data Sections: `summary`, `descriptions`, `text`, `content`
  - Priority: 50 (lower priority for editing tasks)
- **Key Functions**:
  - Processes `ChatRequest` for natural language editing
  - Validates locked nodes before making changes
  - Uses `SummarizationService` for context generation
  - Generates and applies `ChangeSet` through `ChangeEngine`
  - Handles user-initiated modifications only (not agent-initiated)
- **Safety Features**:
  - Pre-request validation against locked nodes
  - Comprehensive error handling with user-friendly messages
  - Retry logic with exponential backoff
  - Itinerary integrity verification after failures

#### Agent Management System:

**AgentRegistry** (`src/main/java/com/tripplanner/service/AgentRegistry.java`)
- **Registration**: Auto-discovers agents via Spring dependency injection
- **Capabilities Management**: Extracts and stores agent capabilities
- **Task Routing**: Maps task types to suitable agents based on capabilities
- **Execution Planning**: Creates `AgentExecutionPlan` with priorities and fallbacks
- **Validation**: Prevents task overlap conflicts and validates agent availability

**AgentCoordinator** (`src/main/java/com/tripplanner/service/AgentCoordinator.java`)
- **Task Routing**: Selects best agent based on capabilities and context scoring
- **Conflict Analysis**: Detects and reports agent capability overlaps
- **Validation**: Ensures agents can handle specific tasks with context
- **Scoring Algorithm**: 
  - Base intensity from priority (lower priority = higher intensity)
  - Bonus for exact task match (+50)
  - Context-specific bonuses (user requests, operation types)
  - Penalty for disabled agents (-1000)

**AgentTaskSystem** (`src/main/java/com/tripplanner/service/AgentTaskSystem.java`)
- **Durable Processing**: Firestore-backed task persistence
- **Real-time Processing**: Firestore listeners for immediate task pickup
- **Retry Mechanisms**: Automatic retry with exponential backoff
- **Idempotency**: Prevents duplicate task execution
- **Lifecycle Management**: Handles task queuing, execution, completion, failure
- **Recovery**: Processes orphaned tasks on system restart

#### Role and Responsibility Management:

**Capability-Based Routing:**
```java
// Each agent defines its capabilities
AgentCapabilities capabilities = new AgentCapabilities();
capabilities.addSupportedTask("edit");
capabilities.addSupportedDataSection("nodes");
capabilities.setPriority(10);
```

**Context-Aware Task Assignment:**
- **User-initiated vs Agent-initiated**: EditorAgent only handles user requests
- **Task Type Matching**: Agents register supported task types
- **Priority-based Selection**: Lower priority number = higher precedence
- **Fallback Mechanisms**: Multiple agents can handle same task type

**Execution Flow:**
1. **Task Submission** → AgentTaskSystem validates and persists
2. **Agent Selection** → AgentCoordinator scores and selects best agent
3. **Execution Planning** → AgentRegistry creates execution plan with timeouts/retries
4. **Task Processing** → Selected agent executes with progress tracking
5. **Result Handling** → Success/failure handling with retry logic

**Conflict Resolution:**
- **Task Overlap Detection**: Registry warns about multiple agents supporting same tasks
- **Priority-based Resolution**: Higher priority agents get preference
- **Context-based Selection**: Scoring considers request context and agent specialization
- **Graceful Degradation**: Fallback agents available for critical operations

---

## Question 3: What is the step by step high level itinerary creation process?

### Brief Answer:
The itinerary creation follows a two-phase approach:
1. **Synchronous Phase**: Create initial structure, establish ownership, return immediate response
2. **Asynchronous Phase**: Generate full content using PlannerAgent → EnrichmentAgent pipeline

**Key Flow**: User Request → ItinerariesController → ItineraryService → AgentOrchestrator → PlannerAgent → EnrichmentAgent → Final Itinerary

### Detailed Analysis:

#### Phase 1: Synchronous Initial Creation (Immediate Response)

**1. API Request Reception**
- **File**: `ItinerariesController.java` - `POST /api/v1/itineraries`
- **Input**: `CreateItineraryReq` with destination, dates, party, budget, interests
- **Authentication**: Firebase token validation extracts `userId`

**2. Initial Itinerary Service Processing**
- **File**: `ItineraryService.java` - `create()` method
- **Actions**:
  - Generate unique itinerary ID: `"it_" + UUID.randomUUID()`
  - Log comprehensive request details (destination, dates, party, budget, interests)
  - Call AgentOrchestrator synchronously for initial setup

**3. Initial Structure Creation (Synchronous)**
- **File**: `AgentOrchestrator.java` - `createInitialItinerary()` method
- **Step 1**: Create basic `NormalizedItinerary` structure
  - Set itineraryId, version=1, userId, timestamps
  - Set summary: "Your personalized itinerary for {destination}"
  - Initialize empty days array, settings, agent status
- **Step 2**: Save to `ItineraryJsonService` (Firestore master JSON)
- **Step 3**: Create and save `TripMetadata` to establish user ownership
- **Result**: User can immediately access `/itineraries/{id}/json` endpoint

**4. Immediate API Response**
- **Return**: `ItineraryDto` with status="generating"
- **User Experience**: Frontend can show loading state and establish WebSocket/SSE connections

#### Phase 2: Asynchronous Content Generation

**5. Async Generation Trigger**
- **File**: `AgentOrchestrator.java` - `generateNormalizedItinerary()` method
- **Timing**: Starts after synchronous response sent
- **Delay**: 3-second wait for frontend SSE connection establishment

**6. PlannerAgent Execution**
- **File**: `PlannerAgent.java` - `executeInternal()` method
- **Input Processing**:
  - Validates `CreateItineraryReq` data
  - Logs comprehensive trip details (destination, dates, party, budget, interests, constraints)
- **AI Content Generation**:
  - Builds system prompt for travel planning expertise
  - Constructs user prompt with all trip parameters
  - Calls AI client with structured JSON schema
  - Uses `LLMResponseHandler` for response validation and continuation
- **Content Processing**:
  - Normalizes time fields (converts HH:mm to milliseconds)
  - Validates required structure (days array, nodes, etc.)
  - Converts to `NormalizedItinerary` object
- **Data Persistence**:
  - Updates itinerary via `ItineraryJsonService`
  - Saves user-specific metadata via `UserDataService`
- **Progress Tracking**: Emits progress events (10%, 20%, 75%, 80%, 90%, 100%)

**7. EnrichmentAgent Execution**
- **File**: `EnrichmentAgent.java` - `executeInternal()` method
- **Purpose**: Add validation, intensity, and detailed information
- **Process**:
  - Loads current itinerary from ItineraryJsonService
  - Validates node timing and logistics
  - Adds place details, photos, reviews
  - Optimizes travel times and transitions
- **Error Handling**: Non-critical - continues if enrichment fails

**8. Final Completion**
- **Data Retrieval**: Get final itinerary from ItineraryJsonService (single source of truth)
- **Status Update**: Mark generation as complete
- **Event Broadcasting**: Send completion event via AgentEventBus
- **WebSocket Notification**: Real-time updates to connected clients

#### Key Architecture Patterns:

**Single Source of Truth**
- `ItineraryJsonService` (Firestore) stores master normalized JSON
- All agents read/write through this service
- User ownership tracked separately in `UserDataService`

**Agent Coordination**
- `AgentOrchestrator` manages agent sequence and error handling
- `AgentRegistry` handles agent discovery and capability routing
- `AgentTaskSystem` provides durable task processing with retry

**Real-time Updates**
- WebSocket/SSE connections for progress tracking
- `AgentEventBus` for agent progress events
- `WebSocketBroadcastService` for itinerary updates

**Error Handling & Recovery**
- Comprehensive validation at each step
- Graceful degradation (EnrichmentAgent failure doesn't break flow)
- Retry mechanisms with exponential backoff
- User-friendly error messages

**Data Flow Summary**:
```
CreateItineraryReq → Initial NormalizedItinerary → Firestore
                  ↓
User gets immediate response with itinerary ID
                  ↓
PlannerAgent → AI Generation → Full NormalizedItinerary → Firestore
                  ↓
EnrichmentAgent → Validation & Enhancement → Final NormalizedItinerary
                  ↓
WebSocket broadcast → Frontend real-time updates
```

---## Question 4: How is the agent status managed and passed on when multiple agents are working together? What's the scheduling logic, how are we asking the next agent to start and how are we getting the info if the current agent is done with the processing?

### Brief Answer:
Agent status management uses a multi-layered approach:
1. **Event-Driven Communication**: `AgentEventBus` broadcasts real-time status via SSE
2. **Sequential Orchestration**: `AgentOrchestrator` manages agent sequence (PlannerAgent → EnrichmentAgent)
3. **Task-Based Scheduling**: `AgentTaskSystem` with Firestore persistence for durable processing
4. **Lifecycle Management**: `TaskLifecycleManager` monitors timeouts, retries, and zombie tasks

**Key Pattern**: Agents emit progress events → EventBus broadcasts → Orchestrator manages sequence → Next agent triggered

### Detailed Analysis:

#### Agent Status Management Architecture:

**1. Real-time Event Broadcasting**
- **File**: `AgentEventBus.java`
- **Mechanism**: SSE (Server-Sent Events) for real-time frontend updates
- **Event Types**: `queued`, `running`, `completed`, `failed`
- **Data Flow**: Agent → EventBus → SSE Emitters → Frontend clients
- **Registration**: Frontend establishes SSE connection per itinerary
- **Cleanup**: Automatic removal of disconnected clients

**2. Agent Status Tracking in Data**
- **File**: `AgentStatus.java` in `NormalizedItinerary`
- **Storage**: Firestore with `lastRunAt` timestamp and status string
- **Status Values**: `"idle"`, `"running"`, `"completed"`, `"failed"`
- **Persistence**: Updated in itinerary JSON for historical tracking

**3. Task-Based Status Management**
- **File**: `AgentTaskSystem.java`
- **Persistence**: Firestore `agent_tasks` collection
- **Status Enum**: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`
- **Monitoring**: Real-time Firestore listeners for task state changes

#### Agent Coordination and Scheduling Logic:

**Sequential Orchestration Pattern**
- **File**: `AgentOrchestrator.java`
- **Flow**: Synchronous initial setup → Asynchronous agent sequence
- **Sequence**: PlannerAgent → EnrichmentAgent (hardcoded sequence)
- **Error Handling**: EnrichmentAgent failure doesn't break the flow

**Agent Execution Sequence:**
```java
// Phase 1: Synchronous (immediate response)
createInitialItinerary() → establish ownership

// Phase 2: Asynchronous (background processing)
generateNormalizedItinerary() {
    1. Wait 3 seconds for SSE connection
    2. Execute PlannerAgent
    3. Execute EnrichmentAgent (if PlannerAgent succeeds)
    4. Send completion event
}
```

#### Progress Tracking and Communication:

**BaseAgent Progress Emission**
- **File**: `BaseAgent.java` - `emitProgress()` method
- **Pattern**: All agents inherit progress tracking capability
- **Events**: `queued` → `running` (with progress %) → `completed`/`failed`
- **Granular Updates**: Each agent emits 5-10 progress updates during execution

**Example Progress Flow (PlannerAgent):**
```java
emitProgress(itineraryId, 10, "Analyzing trip requirements", "requirement_analysis");
emitProgress(itineraryId, 20, "Generating itinerary structure", "structure_generation");
emitProgress(itineraryId, 75, "Processing with AI model", "ai_processing");
emitProgress(itineraryId, 80, "Parsing generated itinerary", "parsing");
emitProgress(itineraryId, 90, "Finalizing itinerary", "finalization");
emitProgress(itineraryId, 100, "Changes applied successfully", "complete");
```

#### Next Agent Triggering Logic:

**1. Sequential Execution in AgentOrchestrator**
```java
// Step 1: Run PlannerAgent
BaseAgent.AgentRequest<ItineraryDto> plannerRequest = new BaseAgent.AgentRequest<>(request, ItineraryDto.class);
plannerAgent.execute(itineraryId, plannerRequest);

// Step 2: Run EnrichmentAgent (only if PlannerAgent succeeds)
BaseAgent.AgentRequest<ChangeEngine.ApplyResult> enrichmentRequest = new BaseAgent.AgentRequest<>(null, ChangeEngine.ApplyResult.class);
enrichmentAgent.execute(itineraryId, enrichmentRequest);
```

**2. Task-Based Scheduling (Alternative Pattern)**
- **File**: `AgentTaskSystem.java`
- **Trigger**: Firestore listeners detect new `PENDING` tasks
- **Processing**: `processTaskAsync()` method handles execution
- **Completion**: `TaskLifecycleManager.handleTaskCompletion()` manages cleanup

#### Agent Completion Detection:

**1. Synchronous Completion (Current Pattern)**
- **Method**: Direct method return from `agent.execute()`
- **Exception Handling**: Try-catch blocks detect failures
- **Status Update**: Automatic via BaseAgent event emission

**2. Asynchronous Completion (Task System)**
- **Mechanism**: `CompletableFuture<AgentTask>` completion callbacks
- **Monitoring**: `TaskLifecycleManager` tracks running tasks
- **Timeout Detection**: Scheduled monitoring for zombie/stale tasks

#### Error Handling and Recovery:

**Timeout Management**
- **File**: `TaskLifecycleManager.java`
- **Monitoring**: 30-second intervals check for timeouts
- **Thresholds**: 
  - Stale tasks: 10 minutes without updates
  - Zombie tasks: 30 minutes in running state
- **Recovery**: Reset zombie tasks to `PENDING` for retry

**Retry Logic**
- **Configuration**: Per-task retry settings with exponential backoff
- **Dead Letter Queue**: Failed tasks moved to `dead_letter_tasks` collection
- **Idempotency**: Prevents duplicate task execution

#### Real-time Frontend Communication:

**SSE Event Structure:**
```json
{
  "agentId": "uuid",
  "kind": "planner",
  "status": "running",
  "progress": 75,
  "message": "Processing with AI model",
  "step": "ai_processing",
  "updatedAt": "2024-01-01T12:00:00Z",
  "itineraryId": "it_123"
}
```

**Event Broadcasting Flow:**
1. Agent calls `emitProgress()` → `emitEvent()`
2. `AgentEventBus.publish()` sends to all SSE emitters for itinerary
3. Frontend receives real-time updates via SSE connection
4. UI updates progress bars, status indicators, step descriptions

#### Key Architectural Benefits:

**Resilience**: Firestore persistence survives system restarts
**Scalability**: Event-driven architecture supports multiple concurrent itineraries
**Observability**: Comprehensive logging and real-time monitoring
**Recovery**: Automatic detection and handling of failed/stuck agents
**User Experience**: Real-time progress updates with detailed step information

---