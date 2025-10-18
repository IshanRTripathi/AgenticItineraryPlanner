# Multi-Agent Chat System - Product Requirements Document

**Version:** 1.0  
**Date:** January 2025  
**Status:** Draft for Review

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Architecture Overview](#2-system-architecture-overview)
3. [Agent Hierarchy & Responsibilities](#3-agent-hierarchy--responsibilities)
4. [User Interaction Patterns](#4-user-interaction-patterns)
5. [Multi-Agent Orchestration Flows](#5-multi-agent-orchestration-flows)
6. [State Management & Context](#6-state-management--context)
7. [Response Protocol & Types](#7-response-protocol--types)
8. [UI/UX Specifications](#8-uiux-specifications)
9. [Place Search & Enrichment](#9-place-search--enrichment)
10. [Error Handling & Fallbacks](#10-error-handling--fallbacks)
11. [LLM Optimization Strategy](#11-llm-optimization-strategy)
12. [Technical Implementation Details](#12-technical-implementation-details)
13. [API Specifications](#13-api-specifications)
14. [Testing Strategy](#14-testing-strategy)
15. [Performance Requirements](#15-performance-requirements)
16. [Open Questions](#16-open-questions)

---

## 1. Executive Summary

### 1.1 Purpose
Design and implement a multi-agent chat system for the Travel Itinerary Planner that enables:
- **Human-in-the-loop workflows** for place selection and disambiguation
- **Seamless multi-agent collaboration** without user awareness of underlying complexity
- **Optimistic background enrichment** for fast, responsive UX
- **Progressive disclosure** of information through card-based interactions

### 1.2 Key Principles
1. **Speed First**: Show results immediately, enrich in background
2. **Visual Selection**: Use cards with images for place selection, not text
3. **Smart Defaults**: Use top match when confident, ask only when ambiguous
4. **Graceful Degradation**: Always provide partial data if enrichment fails
5. **Minimal LLM Usage**: Use APIs directly when possible; LLM only for intent classification and complex reasoning

### 1.3 Success Metrics
- **Response Time**: < 2 seconds for initial response (before enrichment)
- **Selection Accuracy**: > 90% user satisfaction with default place selections
- **Enrichment Success**: > 95% of places enriched within 5 seconds
- **User Engagement**: < 3 interactions to complete a task

---

## 2. System Architecture Overview

### 2.1 Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Chat UI     │  │  Card Grid   │  │  Map View    │      │
│  │  Component   │  │  Component   │  │  Component   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↕ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────┐
│                   ORCHESTRATION LAYER                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         OrchestratorService                          │   │
│  │  • Intent Classification                             │   │
│  │  • Agent Routing                                     │   │
│  │  • Multi-Agent Coordination                          │   │
│  │  • Response Aggregation                              │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│                      AGENT LAYER                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ TOP-LEVEL AGENTS (User-Facing)                    │      │
│  ├──────────────┼──────────────────┼──────────────────┤      │
│  │ EditorAgent  │  PlannerAgent   │  ExplainAgent   │      │
│  │ BookingAgent │  SearchAgent    │  etc.           │      │
│  └──────────────┴──────────────────┴──────────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ PlaceResolution│ EnrichmentAgent│ ValidationSvc  │      │
│  │ Service       │  (Helper)       │                 │      │
│  └──────────────┴──────────────────┴──────────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│                   EXTERNAL SERVICES                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Google Places│  │   LLM API    │  │  Firestore   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Key Components

#### **OrchestratorService**
- **Responsibility**: Central coordinator for all chat interactions
- **Functions**:
  - Receive chat requests from frontend
  - Classify intent using LLM
  - Route to appropriate top-level agent
  - Handle multi-turn conversations
  - Aggregate responses from multiple agents
  - Broadcast updates via WebSocket

#### **Top-Level Agents**
- **Responsibility**: Handle user-facing chat intents
- **Examples**: EditorAgent, PlannerAgent, ExplainAgent, BookingAgent
- **Functions**:
  - Execute primary user intent
  - Call helper services as needed
  - Return structured responses

#### **Service Layer**
- **Responsibility**: Reusable utilities for agents
- **Examples**: PlaceResolutionService, ValidationService, CostEstimatorService
- **Functions**:
  - Stateless operations
  - External API integration
  - Caching and optimization

#### **EnrichmentAgent (Hybrid)**
- **Responsibility**: Background enrichment of nodes
- **Mode**: Can be called as helper OR run independently
- **Functions**:
  - Fetch photos, reviews, ratings
  - Add opening hours, price level
  - Update coordinates if missing

---

## 3. Agent Hierarchy & Responsibilities

### 3.1 Top-Level Agents (User-Facing)

#### **EditorAgent**
**Purpose**: Modify existing itineraries

**Capabilities**:
- Add/remove/move/replace nodes
- Update timing and scheduling
- Handle place search and selection
- Coordinate with PlaceResolutionService

**Task Types**: `edit`, `add`, `remove`, `move`, `replace`

**Helper Services Used**:
- `PlaceResolutionService` (for place search)
- `ValidationService` (for conflict detection)
- `EnrichmentAgent` (background enrichment)

**Sample Interactions**:
```
User: "add a sushi place on day 2"
User: "move lunch to 2pm"
User: "remove the museum visit"
User: "replace dinner with fine dining"
```

---

#### **PlannerAgent**
**Purpose**: Create new itineraries from scratch

**Capabilities**:
- Generate day-by-day structure
- Populate with attractions, meals, transport
- Balance pacing and logistics
- Coordinate with DayByDayPlannerAgent

**Task Types**: `plan`, `create`

**Helper Services Used**:
- `PlaceResolutionService` (for initial place lookups)
- `PlacesAgent` (for area discovery)
- `EnrichmentAgent` (background enrichment)

**Sample Interactions**:
```
User: "create a 5-day itinerary to Barcelona"
User: "plan a romantic weekend in Paris"
```

---

#### **ExplainAgent**
**Purpose**: Answer questions about itineraries

**Capabilities**:
- Explain itinerary structure
- Provide details about places
- Suggest alternatives
- Compare options

**Task Types**: `explain`, `question`

**Helper Services Used**:
- `SummarizationService` (for context)

**Sample Interactions**:
```
User: "what's on day 3?"
User: "why did you pick this restaurant?"
User: "tell me about the Sagrada Familia"
```

---

#### **BookingAgent**
**Purpose**: Make reservations and bookings

**Capabilities**:
- Book hotels, flights, activities
- Check availability
- Handle payment integration
- Store booking confirmations

**Task Types**: `book`, `reserve`

**Helper Services Used**:
- External booking APIs

**Sample Interactions**:
```
User: "book the hotel on day 1"
User: "reserve a table at this restaurant"
```

---

### 3.2 Service Layer (Shared Utilities)

#### **PlaceResolutionService**
**Purpose**: Search for places and resolve locations

**Key Methods**:
```java
// Search for places matching query
List<PlaceCandidate> search(String query, PlaceSearchContext context, int maxResults);

// Get details for a specific place
PlaceDetails getPlaceDetails(String placeId);

// Resolve location for a node
PlaceResolutionResult resolveLocationForNode(NormalizedNode node, String cityContext);
```

**Features**:
- Google Places Text Search API integration
- Query building from node title + type + location
- Result caching (1 hour TTL)
- Rate limiting and error handling
- Filtering by criteria (rating, price level, open now)

**Not Using LLM** - Direct API calls only

---

#### **EnrichmentAgent (Hybrid Role)**
**Purpose**: Add photos, reviews, ratings to nodes

**Modes**:
1. **Helper Mode**: Called by other agents for specific nodes
2. **Background Mode**: Async enrichment queue
3. **Batch Mode**: Enrich entire itinerary

**Key Methods**:
```java
// Enrich a single node
EnrichmentResult enrichNode(String itineraryId, String nodeId);

// Enrich in background (fire-and-forget)
void enrichAsync(EnrichmentRequest request);

// Batch enrich all nodes
List<EnrichmentResult> enrichItinerary(String itineraryId);
```

**Features**:
- Google Places Details API integration
- Photo URL generation
- Review aggregation
- Opening hours validation
- Coordinates backfill (if missing)

**Graceful Degradation**:
- If API fails: Node keeps basic data, enrichment retried later
- If rate limited: Queue for retry after cooldown
- If placeId missing: Log warning, skip enrichment

---

### 3.3 Agent Interaction Matrix

| Agent/Service          | Calls EditorAgent | Calls PlannerAgent | Calls PlaceResolutionService | Calls EnrichmentAgent |
|------------------------|-------------------|--------------------|-----------------------------|----------------------|
| **EditorAgent**        | ❌                | ❌                 | ✅                          | ✅ (background)      |
| **PlannerAgent**       | ❌                | ❌                 | ✅                          | ✅ (background)      |
| **ExplainAgent**       | ❌                | ❌                 | ❌                          | ❌                   |
| **BookingAgent**       | ❌                | ❌                 | ❌                          | ❌                   |
| **PlaceResolutionSvc** | ❌                | ❌                 | ❌                          | ❌                   |
| **EnrichmentAgent**    | ❌                | ❌                 | ❌                          | ❌                   |

**Key Principle**: **No agent-to-agent calls**. Only agents → services.

---

## 4. User Interaction Patterns

### 4.1 Card-Based Selection

**UX Flow**: When user adds a place and multiple matches are found:

1. **Initial Request**
   ```
   User: "add a sushi place on day 2"
   ```

2. **Chat Responds with Cards**
   ```
   💬 "I found 5 sushi restaurants near Hadibo. Select one:"
   
   ┌─────────────────────────────────────────────────────────────┐
   │  [Card Grid - Horizontal Scroll]                            │
   │                                                              │
   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
   │  │ [Photo]  │  │ [Photo]  │  │ [Photo]  │  │ [Photo]  │   │
   │  │ Sushi Zen│  │ Tokyo Bay│  │ Hadibo   │  │ Ocean    │   │
   │  │ ⭐ 4.5   │  │ ⭐ 4.3   │  │ Sushi    │  │ Sushi    │   │
   │  │ $$$      │  │ $$       │  │ ⭐ 4.7   │  │ ⭐ 4.1   │   │
   │  │ 0.3 km   │  │ 0.5 km   │  │ $$$      │  │ $$       │   │
   │  │ Open now │  │ Closed   │  │ 0.8 km   │  │ 1.2 km   │   │
   │  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
   └─────────────────────────────────────────────────────────────┘
   
   [Chat input disabled - select a card above]
   ```

3. **User Clicks Card**
   ```
   ✅ Card 1 selected (Sushi Zen)
   ```

4. **Immediate Feedback**
   ```
   💬 "Adding Sushi Zen to day 2..." ⏳
   ```

5. **Progressive Updates**
   ```
   💬 "Added Sushi Zen to day 2 ✅"
      Fetching photos and reviews... 🔄
   
   [Node appears in itinerary with basic data]
   ```

6. **Background Enrichment Complete**
   ```
   💬 "Sushi Zen details updated ✅"
   
   [Node in itinerary now shows photos, reviews, etc.]
   ```

### 4.2 Chat Input State Management

**States**:
1. **READY**: Default state, user can type
2. **AWAITING_CARD_SELECTION**: Chat input disabled, must select card
3. **PROCESSING**: Loading indicator, input disabled
4. **ERROR**: Error state, retry button shown

**UI Behavior**:
```typescript
interface ChatUIState {
  inputState: 'READY' | 'AWAITING_CARD_SELECTION' | 'PROCESSING' | 'ERROR';
  pendingCards: PlaceCandidate[] | null;
  processingMessage: string | null;
  errorMessage: string | null;
}

// When cards are shown:
setState({
  inputState: 'AWAITING_CARD_SELECTION',
  pendingCards: candidates,
  processingMessage: null,
  errorMessage: null
});

// Chat input component:
<ChatInput 
  disabled={state.inputState !== 'READY'}
  placeholder={
    state.inputState === 'AWAITING_CARD_SELECTION' 
      ? "Select a card above" 
      : "Ask me about your trip..."
  }
/>
```

---

## 5. Multi-Agent Orchestration Flows

### 5.1 Flow: Add Place with Selection (EditorAgent + PlaceResolutionService)

**Scenario**: User adds "sushi place" → Multiple matches → User selects → Background enrichment

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER REQUEST                             │
│  "add a sushi place on day 2"                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ORCHESTRATOR SERVICE                          │
│  1. Build context (itinerary + chat history)                    │
│  2. LLM classifies intent → "edit" / "add"                      │
│  3. Route to EditorAgent                                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                       EDITOR AGENT                               │
│  Step 1: Generate ChangeSet from LLM                            │
│    - User request: "add a sushi place on day 2"                 │
│    - Context: Day 2 has [Breakfast, Lunch, Dinner]             │
│    - LLM generates: Replace Dinner node with sushi place       │
│    - ChangeSet created (but location incomplete)                │
│                                                                  │
│  Step 2: Detect need for place resolution                       │
│    - Node.location has only name: "Hadibo"                      │
│    - No placeId, no coordinates                                 │
│    - Needs resolution = TRUE                                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 PLACE RESOLUTION SERVICE                         │
│  Step 1: Build search query                                     │
│    - Node title: "Sushi Dinner"                                 │
│    - Node type: "meal" → add "restaurant"                       │
│    - Location: "Hadibo"                                          │
│    - Query: "sushi restaurant in Hadibo"                        │
│                                                                  │
│  Step 2: Call Google Places Text Search API                     │
│    - API: textSearch(query, filters)                            │
│    - Filters: rating >= 3.5, open_now=any                       │
│    - Returns: 5 candidates                                      │
│                                                                  │
│  Step 3: Transform results to PlaceCandidates                   │
│    - Extract: name, placeId, rating, price, distance, photo     │
│    - Sort by: rating desc, distance asc                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                       EDITOR AGENT                               │
│  Step 3: Check candidate count                                  │
│    - Candidates.length = 5                                      │
│    - Decision: NEEDS_USER_SELECTION                             │
│                                                                  │
│  Step 4: Create response with cards                             │
│    - Type: NEEDS_SELECTION                                      │
│    - Message: "I found 5 sushi restaurants. Select one:"        │
│    - Data: [PlaceCandidates array]                             │
│    - UI Hint: "place-card-grid"                                │
│    - Conversation state: STORE pending selection               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ORCHESTRATOR SERVICE                          │
│  Step 1: Detect NEEDS_SELECTION response                        │
│  Step 2: Store conversation state                               │
│    - ConversationStateService.store(userId, {                   │
│        type: 'place_selection',                                 │
│        candidates: [PlaceCandidates],                           │
│        changeSet: [Pending ChangeSet],                          │
│        nodeToUpdate: [Node reference]                           │
│      })                                                          │
│  Step 3: Return to frontend                                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                          FRONTEND                                │
│  1. Render chat message: "I found 5 sushi restaurants..."       │
│  2. Render card grid with 5 cards                               │
│  3. Disable chat input                                          │
│  4. Show placeholder: "Select a card above"                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                      [USER CLICKS CARD #1]
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                          FRONTEND                                │
│  1. Send to backend:                                            │
│    {                                                             │
│      type: 'place_selected',                                    │
│      userId: 'user-123',                                        │
│      selectedIndex: 0,                                          │
│      itineraryId: 'itin-456'                                    │
│    }                                                             │
│  2. Show loading state: "Adding Sushi Zen..." ⏳                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ORCHESTRATOR SERVICE                          │
│  1. Retrieve conversation state                                 │
│  2. Extract selected candidate                                  │
│  3. Route to EditorAgent.handleSelection()                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                       EDITOR AGENT                               │
│  Step 1: Enrich node with selected place                        │
│    - node.location.placeId = candidate.placeId                  │
│    - node.location.coordinates = candidate.coordinates          │
│    - node.location.address = candidate.formattedAddress         │
│    - node.title = candidate.name (or keep user's "Sushi Dinner")│
│                                                                  │
│  Step 2: Apply ChangeSet                                        │
│    - changeEngine.apply(itineraryId, changeSet)                 │
│    - Node added to day 2                                        │
│    - Return: ApplyResult with new version                       │
│                                                                  │
│  Step 3: Queue background enrichment                            │
│    - enrichmentQueue.enqueue({                                  │
│        itineraryId,                                             │
│        nodeId: node.id,                                         │
│        priority: 'HIGH'                                         │
│      })                                                          │
│                                                                  │
│  Step 4: Return success response                                │
│    - Type: COMPLETE                                             │
│    - Message: "Added Sushi Zen to day 2 ✅"                     │
│    - Data: ApplyResult                                          │
│    - Background: EnrichmentTask queued                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                          FRONTEND                                │
│  1. Show success message: "Added Sushi Zen to day 2 ✅"         │
│  2. Reload itinerary (shows node with basic data)               │
│  3. Re-enable chat input                                        │
│  4. Clear pending selection state                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    [BACKGROUND - ASYNC]
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ENRICHMENT AGENT                              │
│  Step 1: Fetch place details from Google Places API             │
│    - placeId from node.location.placeId                         │
│    - Fields: photos, reviews, rating, price_level, hours        │
│                                                                  │
│  Step 2: Create ChangeSet for enrichment                        │
│    - Operation: "update"                                        │
│    - Target: nodeId                                             │
│    - Changes:                                                   │
│      • details.photos = [photo URLs]                            │
│      • details.reviews = [top 5 reviews]                        │
│      • details.rating = 4.5                                     │
│      • details.priceLevel = 2                                   │
│      • agentData.openingHours = {...}                           │
│                                                                  │
│  Step 3: Apply enrichment ChangeSet                             │
│    - changeEngine.apply(itineraryId, enrichmentChangeSet)       │
│                                                                  │
│  Step 4: Broadcast update via WebSocket                         │
│    - WebSocketBroadcastService.broadcastChange(itineraryId)     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                          FRONTEND                                │
│  1. Receive WebSocket update                                    │
│  2. Reload itinerary silently                                   │
│  3. Node now shows photos, reviews, rating                      │
│  4. Optional: Show toast "Details updated ✅"                   │
└─────────────────────────────────────────────────────────────────┘
```

**Timing**:
- User sees cards: **< 2 seconds**
- User selects card → node added: **< 1 second**
- Background enrichment complete: **< 5 seconds**

---

### 5.2 Flow: Add Place with Single Match (No Selection Needed)

**Scenario**: User adds "Sushi Zen restaurant" → Exact match → Auto-apply → Background enrichment

```
User: "add Sushi Zen restaurant on day 2"
  ↓
OrchestratorService → LLM intent classification → "edit/add"
  ↓
EditorAgent:
  1. Generate ChangeSet
  2. Call PlaceResolutionService.search("Sushi Zen restaurant in Hadibo")
  3. Results: 1 candidate (high confidence match)
  4. Decision: AUTO-APPLY (no user selection needed)
  5. Enrich node with place data
  6. Apply ChangeSet
  7. Queue background enrichment
  ↓
Response: "Added Sushi Zen to day 2 ✅"
  ↓
Frontend: Shows node immediately
  ↓
[Background] EnrichmentAgent: Fetch photos, reviews
  ↓
Frontend: WebSocket update → Node shows enriched data
```

**Timing**:
- User sees success: **< 2 seconds**
- Enrichment complete: **< 5 seconds**

---

### 5.3 Flow: Add Place with No Matches

**Scenario**: User adds obscure place → No search results → Ask user to retry

```
User: "add Moon Restaurant on day 2"
  ↓
EditorAgent → PlaceResolutionService.search("Moon Restaurant in Hadibo")
  ↓
Google Places API: 0 results
  ↓
EditorAgent: Return empty response with error
  ↓
Response: 
  Type: ERROR
  Message: "I couldn't find 'Moon Restaurant' near Hadibo. 
           Please try again with a different name or add more details."
  ↓
Frontend: Show error message, re-enable chat input
```

**User Actions**:
- Try with different name
- Add more context ("Moon Restaurant on Main Street")
- Skip and add manually (future feature)

---

## 6. State Management & Context

### 6.1 Conversation State Service

**Purpose**: Store temporary state for multi-turn conversations

**Storage**: In-memory cache with 5-minute TTL

**Schema**:
```java
@Data
public class ConversationState {
    private String userId;
    private String itineraryId;
    private ConversationStateType type;
    private Object data;
    private long timestamp;
    private long expiresAt;
}

public enum ConversationStateType {
    PLACE_SELECTION,     // Awaiting user to select a place card
    CONFIRMATION,        // Awaiting yes/no confirmation
    DISAMBIGUATION,      // Awaiting node selection from candidates
    CUSTOM_INPUT        // Awaiting custom user input (text/number)
}

// For place selection:
@Data
public class PlaceSelectionState {
    private List<PlaceCandidate> candidates;
    private ChangeSet pendingChangeSet;
    private String nodeIdToUpdate;
    private String searchQuery;
}
```

**API**:
```java
@Service
public class ConversationStateService {
    private final Cache<String, ConversationState> cache;
    
    // Store state
    void store(String userId, ConversationStateType type, Object data);
    
    // Retrieve state
    Optional<ConversationState> get(String userId);
    
    // Clear state (after completion or timeout)
    void clear(String userId);
    
    // Check if user has pending state
    boolean hasPendingState(String userId);
}
```

**Usage in EditorAgent**:
```java
// When showing place selection cards:
PlaceSelectionState state = new PlaceSelectionState();
state.setCandidates(candidates);
state.setPendingChangeSet(changeSet);
state.setNodeIdToUpdate(node.getId());

conversationStateService.store(
    request.getUserId(), 
    ConversationStateType.PLACE_SELECTION, 
    state
);

// When user selects a card:
Optional<ConversationState> stateOpt = conversationStateService.get(request.getUserId());
if (stateOpt.isPresent() && stateOpt.get().getType() == PLACE_SELECTION) {
    PlaceSelectionState selectionState = (PlaceSelectionState) stateOpt.get().getData();
    PlaceCandidate selected = selectionState.getCandidates().get(request.getSelectedIndex());
    // ... proceed with selection
    conversationStateService.clear(request.getUserId()); // Clean up
}
```

---

### 6.2 General Chat Context

**Purpose**: Provide consistent context for ALL chat interactions

**Components**:
1. **Itinerary Summary** (from SummarizationService)
2. **Recent Chat History** (last 5 messages)
3. **User Preferences** (if available)
4. **Current Date/Time** (for context-aware responses)

**Context Building**:
```java
@Service
public class ChatContextBuilder {
    private final SummarizationService summarizationService;
    private final ChatHistoryService chatHistoryService;
    private final UserPreferencesService userPreferencesService;
    
    public ChatContext buildContext(String itineraryId, String userId) {
        ChatContext context = new ChatContext();
        
        // 1. Itinerary summary
        NormalizedItinerary itinerary = getItinerary(itineraryId);
        String summary = summarizationService.summarizeForAgent(
            itinerary, 
            "general", 
            2000 // Max 2000 tokens
        );
        context.setItinerarySummary(summary);
        
        // 2. Recent chat history
        List<ChatMessage> history = chatHistoryService.getRecentMessages(
            itineraryId, 
            5 // Last 5 messages
        );
        context.setChatHistory(formatChatHistory(history));
        
        // 3. User preferences (if available)
        Optional<UserPreferences> prefs = userPreferencesService.get(userId);
        prefs.ifPresent(context::setUserPreferences);
        
        // 4. Temporal context
        context.setCurrentDate(LocalDate.now());
        context.setCurrentTime(LocalTime.now());
        
        // 5. Itinerary metadata
        context.setDestination(itinerary.getDestination());
        context.setTripStartDate(itinerary.getStartDate());
        context.setTripEndDate(itinerary.getEndDate());
        context.setCurrency(itinerary.getCurrency());
        
        return context;
    }
    
    private String formatChatHistory(List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recent conversation:\n");
        for (ChatMessage msg : history) {
            sb.append(String.format("%s: %s\n", 
                msg.getSender().equals("user") ? "User" : "Assistant", 
                msg.getMessage()
            ));
        }
        return sb.toString();
    }
}
```

**Usage in OrchestratorService**:
```java
public ChatResponse route(ChatRequest request) {
    // Build comprehensive context
    ChatContext context = chatContextBuilder.buildContext(
        request.getItineraryId(), 
        request.getUserId()
    );
    
    // Pass to LLM for intent classification
    IntentResult intent = llmService.classifyIntent(
        request.getText(), 
        context.toString()
    );
    
    // Pass to agent for execution
    AgentRequest agentRequest = new AgentRequest(request, context);
    AgentResponse response = agent.execute(itineraryId, agentRequest);
    
    return response;
}
```

---

### 6.3 Context Token Optimization

**Goal**: Keep context under 3000 tokens total to minimize LLM costs

**Budget Allocation**:
- Itinerary summary: **2000 tokens** (agent-specific, hierarchical)
- Chat history: **500 tokens** (last 5 messages, truncated)
- User preferences: **200 tokens**
- Metadata: **100 tokens**
- Buffer: **200 tokens**

**Optimization Strategies**:
1. **Agent-Specific Summarization**: Use `summarizeForAgent()` to focus on relevant info
2. **Hierarchical Truncation**: Show overview + current day in detail
3. **Message Pruning**: Remove redundant "ok", "thanks" messages from history
4. **Smart Truncation**: Cut from middle, keep start and end of long messages

---

## 7. Response Protocol & Types

### 7.1 Agent Response Structure

```java
@Data
public class AgentResponse {
    private AgentResponseType type;
    private String message;              // Human-readable message for chat
    private Object data;                 // Structured data (ChangeSet, candidates, etc.)
    private ResponseMetadata metadata;   // UI rendering hints
    private List<String> warnings;       // Non-blocking warnings
    private List<String> errors;         // Blocking errors
    private BackgroundTask backgroundTask; // Optional background enrichment
}

public enum AgentResponseType {
    COMPLETE,           // Final answer, no further action needed
    NEEDS_SELECTION,    // Show options (cards), await user selection
    NEEDS_CONFIRMATION, // Ask yes/no, await user response
    NEEDS_INPUT,        // Ask for text/number input
    ERROR,              // Operation failed
    PARTIAL             // Partial success (some operations succeeded, some failed)
}

@Data
public class ResponseMetadata {
    private String uiComponent;        // "place-card-grid", "confirmation-dialog", "text-input"
    private String actionLabel;        // "Select", "Confirm", "Enter"
    private int timeoutSeconds;        // Auto-clear after N seconds (0 = no timeout)
    private boolean showProgress;      // Show loading indicator
    private Map<String, Object> customProps; // Component-specific props
}

@Data
public class BackgroundTask {
    private String taskType;           // "enrichment", "validation", etc.
    private String targetAgent;        // "EnrichmentAgent"
    private Object taskData;           // Task-specific data
    private int priority;              // 1 (highest) to 10 (lowest)
    private long estimatedDurationMs;  // Estimated time to complete
}
```

### 7.2 Response Type Specifications

#### **COMPLETE**
**When**: Operation succeeded, no further user action needed

**Example**:
```java
return AgentResponse.builder()
    .type(AgentResponseType.COMPLETE)
    .message("Added Sushi Zen to day 2 ✅")
    .data(applyResult)
    .backgroundTask(BackgroundTask.builder()
        .taskType("enrichment")
        .targetAgent("EnrichmentAgent")
        .taskData(EnrichmentRequest.forNode(nodeId))
        .priority(5)
        .estimatedDurationMs(3000)
        .build())
    .build();
```

**Frontend Behavior**:
- Show message in chat
- Update itinerary view
- Re-enable chat input
- Optionally show "Enriching..." indicator

---

#### **NEEDS_SELECTION**
**When**: Multiple options available, user must choose

**Example**:
```java
return AgentResponse.builder()
    .type(AgentResponseType.NEEDS_SELECTION)
    .message("I found 5 sushi restaurants near Hadibo. Select one:")
    .data(placeCandidates)
    .metadata(ResponseMetadata.builder()
        .uiComponent("place-card-grid")
        .actionLabel("Select")
        .timeoutSeconds(300) // 5 minutes
        .customProps(Map.of(
            "scrollable", true,
            "columns", 4,
            "showMap", false
        ))
        .build())
    .build();
```

**Frontend Behavior**:
- Show message in chat
- Render card grid component
- Disable chat input
- Show placeholder: "Select a card above"
- Store pending state
- Auto-clear after timeout (optional)

---

#### **NEEDS_CONFIRMATION**
**When**: User must confirm a potentially destructive action

**Example**:
```java
return AgentResponse.builder()
    .type(AgentResponseType.NEEDS_CONFIRMATION)
    .message("This will remove 3 activities from day 2. Continue?")
    .data(changeSetPreview)
    .metadata(ResponseMetadata.builder()
        .uiComponent("confirmation-dialog")
        .actionLabel("Confirm")
        .customProps(Map.of(
            "confirmText", "Yes, remove them",
            "cancelText", "Cancel",
            "severity", "warning"
        ))
        .build())
    .build();
```

**Frontend Behavior**:
- Show inline confirmation buttons
- Disable chat input
- On confirm: Send confirmation to backend
- On cancel: Clear state, re-enable input

---

#### **ERROR**
**When**: Operation failed, user should retry or adjust

**Example**:
```java
return AgentResponse.builder()
    .type(AgentResponseType.ERROR)
    .message("I couldn't find 'Moon Restaurant' near Hadibo.")
    .errors(List.of(
        "No results found for search query",
        "Try adding more details (street name, area)"
    ))
    .build();
```

**Frontend Behavior**:
- Show error message in chat with error icon
- Show suggestions if available
- Re-enable chat input
- Optionally show retry button

---

### 7.3 Frontend Response Handling

```typescript
// In chat reducer
function handleAgentResponse(response: AgentResponse) {
  switch (response.type) {
    case 'COMPLETE':
      // Show success message
      addMessage({
        role: 'assistant',
        content: response.message,
        data: response.data
      });
      
      // Reload itinerary
      loadItinerary();
      
      // Re-enable input
      setInputState('READY');
      
      // Show background task indicator if present
      if (response.backgroundTask) {
        showBackgroundTaskIndicator(response.backgroundTask);
      }
      break;
      
    case 'NEEDS_SELECTION':
      // Show message
      addMessage({
        role: 'assistant',
        content: response.message
      });
      
      // Render cards
      setPendingCards(response.data as PlaceCandidate[]);
      setCardMetadata(response.metadata);
      
      // Disable input
      setInputState('AWAITING_CARD_SELECTION');
      break;
      
    case 'ERROR':
      // Show error
      addMessage({
        role: 'assistant',
        content: response.message,
        type: 'error',
        errors: response.errors
      });
      
      // Re-enable input for retry
      setInputState('READY');
      break;
      
    // ... other types
  }
}
```

---

## 8. UI/UX Specifications

### 8.1 Place Card Component

**Visual Design**:
```
┌────────────────────────┐
│  [Photo - 16:9 ratio]  │
│  200px x 112px         │
├────────────────────────┤
│ Sushi Zen              │ ← Title (bold, 16px)
│ ⭐ 4.5 (234 reviews)   │ ← Rating (14px)
│ $$$ • Japanese         │ ← Price + Cuisine
│ 0.3 km • Open now      │ ← Distance + Status
└────────────────────────┘
     [Clickable]
```

**Component Props**:
```typescript
interface PlaceCardProps {
  candidate: PlaceCandidate;
  onSelect: () => void;
  selected: boolean;
}

interface PlaceCandidate {
  placeId: string;
  name: string;
  rating: number;
  ratingCount: number;
  priceLevel: number; // 0-4
  cuisine: string[];
  distance: number; // meters
  isOpenNow: boolean;
  photoUrl: string;
  formattedAddress: string;
  coordinates: { lat: number; lng: number };
}
```

**States**:
- **Default**: White background, subtle border
- **Hover**: Slight elevation, border color change
- **Selected**: Blue border, checkmark overlay
- **Loading**: Skeleton placeholder while photo loads

**Accessibility**:
- Keyboard navigable (arrow keys + Enter)
- Screen reader announces: "Restaurant card: [name], rated [rating] stars, [distance] away, [status]"
- Focus indicator

---

### 8.2 Card Grid Layout

**Responsive Breakpoints**:
```
Desktop (>1200px):  4 columns, 220px per card
Tablet (768-1200px): 3 columns, 240px per card
Mobile (<768px):    2 columns, 160px per card
```

**Scrolling**:
- Horizontal scroll on mobile/tablet
- Grid wrap on desktop
- Scroll indicator if more cards off-screen

**Max Cards Displayed**: 8 (to prevent overwhelming user)

---

### 8.3 Chat Message Variations

#### **Standard Text Message**
```
┌─────────────────────────────────────┐
│ 🤖 Assistant                        │
│ I've updated your day 2 schedule.  │
│ 2:34 PM                             │
└─────────────────────────────────────┘
```

#### **Message with Cards**
```
┌─────────────────────────────────────┐
│ 🤖 Assistant                        │
│ I found 5 sushi restaurants:        │
│                                     │
│ [Card Grid Component Rendered Here] │
│                                     │
│ 2:34 PM                             │
└─────────────────────────────────────┘
```

#### **Message with Progress**
```
┌─────────────────────────────────────┐
│ 🤖 Assistant                        │
│ Added Sushi Zen to day 2 ✅         │
│ Fetching photos and reviews... 🔄  │
│ 2:34 PM                             │
└─────────────────────────────────────┘
```

#### **Error Message**
```
┌─────────────────────────────────────┐
│ ⚠️ Assistant                        │
│ I couldn't find that restaurant.    │
│ • No results for "Moon Restaurant"  │
│ • Try adding more details           │
│ [Retry] button                      │
│ 2:34 PM                             │
└─────────────────────────────────────┘
```

---

### 8.4 Input State Visual Feedback

**READY** (Default):
```
┌────────────────────────────────────────────┐
│ Ask me about your trip...        [Send] │
└────────────────────────────────────────────┘
```

**AWAITING_CARD_SELECTION**:
```
┌────────────────────────────────────────────┐
│ ← Select a card above              [🔒]  │
└────────────────────────────────────────────┘
(Input grayed out, disabled)
```

**PROCESSING**:
```
┌────────────────────────────────────────────┐
│ Processing...                    [⏳]    │
└────────────────────────────────────────────┘
(Input disabled, loading spinner)
```

---

### 8.5 Background Enrichment Indicator

**Inline Progress** (within message):
```
💬 "Added Sushi Zen to day 2 ✅"
   Fetching details... 🔄 [Progress: 60%]
```

**Toast Notification** (bottom-right):
```
┌──────────────────────────────┐
│ 🔄 Updating Sushi Zen...     │
│ [Progress bar]               │
└──────────────────────────────┘
```

**Silent Update** (no UI, just WebSocket reload):
- Preferred for non-critical enrichments
- Node updates in background
- No user interruption

---

## 9. Place Search & Enrichment

### 9.1 PlaceResolutionService API

#### **Method: `search()`**

```java
/**
 * Search for places matching a query.
 * 
 * @param query Search query (e.g., "sushi restaurant in Hadibo")
 * @param context Search context (location, filters, etc.)
 * @param maxResults Maximum number of results (1-20)
 * @return List of place candidates, sorted by relevance
 */
public List<PlaceCandidate> search(
    String query, 
    PlaceSearchContext context, 
    int maxResults
);

@Data
public class PlaceSearchContext {
    private String cityName;           // "Hadibo"
    private Coordinates centerPoint;   // Optional: lat/lng for proximity search
    private Double radiusMeters;       // Optional: search radius (default: 5000m)
    private PlaceSearchFilters filters;
}

@Data
public class PlaceSearchFilters {
    private Double minRating;          // e.g., 3.5
    private Integer minReviewCount;    // e.g., 10
    private List<Integer> priceLevels; // e.g., [2, 3] for $$ and $$$
    private Boolean openNow;           // null = any, true = open now only
    private List<String> types;        // e.g., ["restaurant", "cafe"]
}
```

**Implementation Details**:
```java
@Service
public class PlaceResolutionService {
    private final GooglePlacesService googlePlacesService;
    private final PlaceSearchCache cache;
    
    public List<PlaceCandidate> search(String query, PlaceSearchContext context, int maxResults) {
        // 1. Check cache
        String cacheKey = buildCacheKey(query, context);
        Optional<List<PlaceCandidate>> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get().subList(0, Math.min(maxResults, cached.get().size()));
        }
        
        // 2. Build Google Places query
        String enrichedQuery = enrichQuery(query, context);
        
        // 3. Call Google Places Text Search
        TextSearchRequest request = TextSearchRequest.builder()
            .query(enrichedQuery)
            .location(context.getCenterPoint())
            .radius(context.getRadiusMeters())
            .build();
        
        TextSearchResponse response = googlePlacesService.textSearch(request);
        
        // 4. Filter results
        List<PlaceSearchResult> filtered = filterResults(
            response.getResults(), 
            context.getFilters()
        );
        
        // 5. Transform to PlaceCandidates
        List<PlaceCandidate> candidates = filtered.stream()
            .map(this::toPlaceCandidate)
            .limit(maxResults)
            .collect(Collectors.toList());
        
        // 6. Cache results
        cache.put(cacheKey, candidates);
        
        return candidates;
    }
    
    private String enrichQuery(String query, PlaceSearchContext context) {
        // If query doesn't include location, add it
        if (context.getCityName() != null && !query.contains(context.getCityName())) {
            return query + " in " + context.getCityName();
        }
        return query;
    }
    
    private List<PlaceSearchResult> filterResults(
        List<PlaceSearchResult> results, 
        PlaceSearchFilters filters
    ) {
        if (filters == null) return results;
        
        return results.stream()
            .filter(r -> filters.getMinRating() == null || r.getRating() >= filters.getMinRating())
            .filter(r -> filters.getMinReviewCount() == null || r.getUserRatingsTotal() >= filters.getMinReviewCount())
            .filter(r -> filters.getPriceLevels() == null || filters.getPriceLevels().contains(r.getPriceLevel()))
            .filter(r -> filters.getOpenNow() == null || !filters.getOpenNow() || isOpenNow(r))
            .collect(Collectors.toList());
    }
    
    private PlaceCandidate toPlaceCandidate(PlaceSearchResult result) {
        return PlaceCandidate.builder()
            .placeId(result.getPlaceId())
            .name(result.getName())
            .rating(result.getRating())
            .ratingCount(result.getUserRatingsTotal())
            .priceLevel(result.getPriceLevel())
            .formattedAddress(result.getFormattedAddress())
            .coordinates(new Coordinates(
                result.getGeometry().getLocation().getLat(),
                result.getGeometry().getLocation().getLng()
            ))
            .photoUrl(extractPhotoUrl(result))
            .isOpenNow(isOpenNow(result))
            .distance(calculateDistance(result, context.getCenterPoint()))
            .types(result.getTypes())
            .build();
    }
}
```

---

### 9.2 GooglePlacesService Extension

**New Method: `textSearch()`**

```java
@Service
public class GooglePlacesService {
    
    /**
     * Search for places using Google Places Text Search API.
     * https://developers.google.com/maps/documentation/places/web-service/search-text
     */
    @Cacheable(value = "placeTextSearch", key = "#request.query + '_' + #request.location")
    public TextSearchResponse textSearch(TextSearchRequest request) {
        logger.debug("Text search: query={}, location={}", request.getQuery(), request.getLocation());
        
        // Check rate limits
        checkRateLimit();
        
        // Check circuit breaker
        checkCircuitBreaker();
        
        try {
            // Build URL
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(BASE_URL + "/textsearch/json")
                .queryParam("query", request.getQuery())
                .queryParam("key", apiKey);
            
            if (request.getLocation() != null) {
                builder.queryParam("location", 
                    request.getLocation().getLat() + "," + request.getLocation().getLng());
            }
            
            if (request.getRadius() != null) {
                builder.queryParam("radius", request.getRadius());
            }
            
            // Optional: specify fields to reduce response size
            builder.queryParam("fields", 
                "place_id,name,geometry,formatted_address,rating,user_ratings_total," +
                "price_level,photos,types,opening_hours");
            
            String url = builder.toUriString();
            
            // Make request
            TextSearchResponse response = makeRequestWithRetry(url, TextSearchResponse.class);
            
            // Increment request count
            incrementRequestCount();
            
            // Handle response
            if (response.isSuccessful()) {
                logger.debug("Text search successful: {} results", response.getResults().size());
                recordSuccess();
                return response;
            } else {
                recordFailure();
                throw new RuntimeException("Google Places API error: " + response.getStatus());
            }
            
        } catch (Exception e) {
            logger.error("Text search failed: {}", request.getQuery(), e);
            recordFailure();
            throw new RuntimeException("Failed to search places: " + e.getMessage(), e);
        }
    }
}

@Data
@Builder
public class TextSearchRequest {
    private String query;              // Required: search query
    private Coordinates location;      // Optional: center point for proximity
    private Integer radius;            // Optional: search radius in meters
}

@Data
public class TextSearchResponse {
    private List<PlaceSearchResult> results;
    private String status;
    private String nextPageToken;      // For pagination
    private String errorMessage;
    
    public boolean isSuccessful() {
        return "OK".equals(status) || "ZERO_RESULTS".equals(status);
    }
}

@Data
public class PlaceSearchResult {
    private String placeId;
    private String name;
    private String formattedAddress;
    private Geometry geometry;
    private Double rating;
    private Integer userRatingsTotal;
    private Integer priceLevel;
    private List<String> types;
    private List<Photo> photos;
    private OpeningHours openingHours;
}
```

---

### 9.3 Query Building Logic

**In EditorAgent**:
```java
private String buildSearchQuery(NormalizedNode node, String cityContext) {
    StringBuilder query = new StringBuilder();
    
    // 1. Start with node title
    String title = node.getTitle();
    
    // Clean up title (remove generic words)
    title = title.replace("Dinner", "")
                 .replace("Lunch", "")
                 .replace("Breakfast", "")
                 .trim();
    
    query.append(title);
    
    // 2. Add type hint if meal/activity
    if ("meal".equals(node.getType())) {
        if (!title.toLowerCase().contains("restaurant") && 
            !title.toLowerCase().contains("cafe")) {
            query.append(" restaurant");
        }
    }
    
    // 3. Add location context
    // Priority: node.location.name > day.location > itinerary.destination
    if (node.getLocation() != null && node.getLocation().getName() != null) {
        query.append(" in ").append(node.getLocation().getName());
    } else if (cityContext != null) {
        query.append(" in ").append(cityContext);
    }
    
    return query.toString();
}

// Example transformations:
// "Sushi Dinner" + "meal" + "Hadibo" → "Sushi restaurant in Hadibo"
// "Sushi Zen" + "meal" + "Hadibo" → "Sushi Zen in Hadibo"
// "Morning Hike" + "attraction" + "Mountains" → "Morning Hike in Mountains"
```

---

### 9.4 Enrichment Flow

**Background Enrichment Process**:

```java
@Service
public class EnrichmentQueueService {
    private final Queue<EnrichmentTask> queue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    @PostConstruct
    public void startWorkers() {
        for (int i = 0; i < 3; i++) {
            executor.submit(this::processQueue);
        }
    }
    
    public void enqueue(EnrichmentTask task) {
        queue.offer(task);
        logger.info("Enqueued enrichment task: {} (queue size: {})", 
            task.getNodeId(), queue.size());
    }
    
    private void processQueue() {
        while (true) {
            try {
                EnrichmentTask task = queue.poll();
                if (task == null) {
                    Thread.sleep(100); // Wait for new tasks
                    continue;
                }
                
                logger.info("Processing enrichment: {}", task.getNodeId());
                enrichmentAgent.enrichNode(task.getItineraryId(), task.getNodeId());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Enrichment failed", e);
                // Don't retry - just log and move on
            }
        }
    }
}

@Data
public class EnrichmentTask {
    private String itineraryId;
    private String nodeId;
    private int priority;
    private long enqueuedAt;
}
```

**In EnrichmentAgent**:
```java
public void enrichNode(String itineraryId, String nodeId) {
    try {
        // 1. Load node
        NormalizedNode node = findNode(itineraryId, nodeId);
        if (node == null || node.getLocation() == null || node.getLocation().getPlaceId() == null) {
            logger.warn("Cannot enrich node {} - missing placeId", nodeId);
            return;
        }
        
        // 2. Fetch place details
        PlaceDetails details = googlePlacesService.getPlaceDetails(node.getLocation().getPlaceId());
        
        // 3. Create enrichment ChangeSet
        ChangeSet enrichmentChangeSet = createEnrichmentChangeSet(node, details);
        
        // 4. Apply changes
        changeEngine.apply(itineraryId, enrichmentChangeSet);
        
        // 5. Broadcast update
        webSocketBroadcastService.broadcastChange(itineraryId);
        
        logger.info("Successfully enriched node: {}", nodeId);
        
    } catch (Exception e) {
        logger.error("Failed to enrich node: {}", nodeId, e);
        // Fail silently - node already exists with basic data
    }
}

private ChangeSet createEnrichmentChangeSet(NormalizedNode node, PlaceDetails details) {
    ChangeSet changeSet = new ChangeSet();
    
    ChangeOperation updateOp = new ChangeOperation();
    updateOp.setOp("update");
    updateOp.setId(node.getId());
    
    // Update node details
    NodeDetails nodeDetails = node.getDetails() != null ? node.getDetails() : new NodeDetails();
    
    // Photos
    if (details.getPhotos() != null && !details.getPhotos().isEmpty()) {
        List<String> photoUrls = details.getPhotos().stream()
            .limit(5)
            .map(photo -> googlePlacesService.getPhotoUrl(photo.getPhotoReference(), 400))
            .collect(Collectors.toList());
        nodeDetails.setPhotos(photoUrls);
    }
    
    // Reviews
    if (details.getReviews() != null) {
        nodeDetails.setReviews(details.getReviews().stream()
            .limit(5)
            .collect(Collectors.toList()));
    }
    
    // Rating
    if (details.getRating() != null) {
        nodeDetails.setRating(details.getRating());
    }
    
    // Price level
    if (details.getPriceLevel() != null) {
        NodeCost cost = node.getCost() != null ? node.getCost() : new NodeCost();
        cost.setPriceLevel(details.getPriceLevel());
        updateOp.setCost(cost);
    }
    
    updateOp.setDetails(nodeDetails);
    
    // Opening hours (store in agentData)
    if (details.getOpeningHours() != null) {
        Map<String, Object> agentData = new HashMap<>();
        agentData.put("openingHours", details.getOpeningHours());
        updateOp.setAgentData(agentData);
    }
    
    changeSet.setOps(List.of(updateOp));
    changeSet.setDay(findDayForNode(node));
    changeSet.setReason("Enriched with Google Places data");
    changeSet.setAgent("EnrichmentAgent");
    
    return changeSet;
}
```

---

## 10. Error Handling & Fallbacks

### 10.1 Error Scenarios & Responses

#### **Scenario: No Place Results Found**

**User Input**: "add Moon Restaurant on day 2"

**Flow**:
```
PlaceResolutionService.search() → 0 results
  ↓
EditorAgent: Return error response
  ↓
Frontend: Show error message
```

**Response**:
```java
return AgentResponse.builder()
    .type(AgentResponseType.ERROR)
    .message("I couldn't find 'Moon Restaurant' near Hadibo.")
    .errors(List.of(
        "No results found for this search",
        "Try adding more details (street name, area)",
        "Check spelling"
    ))
    .build();
```

**UI**:
```
⚠️ Assistant
I couldn't find 'Moon Restaurant' near Hadibo.
• No results found for this search
• Try adding more details (street name, area)
• Check spelling

[Chat input re-enabled for retry]
```

---

#### **Scenario: Enrichment API Fails**

**Flow**:
```
Node added successfully with basic data
  ↓
EnrichmentAgent queued
  ↓
Google Places API: Rate limit exceeded (429)
  ↓
EnrichmentAgent: Log error, don't update node
  ↓
Node remains with basic data (name, address, coordinates)
```

**Behavior**:
- **Silent failure** - User not notified
- **Retry later** - Queue enrichment for retry after cooldown (15 minutes)
- **Graceful degradation** - Node still functional, just missing photos/reviews

**Logging**:
```
WARN: Enrichment failed for node [nodeId]: Rate limit exceeded
INFO: Queued for retry at [timestamp]
```

---

#### **Scenario: Google Places API Down**

**Flow**:
```
PlaceResolutionService.search()
  ↓
GooglePlacesService: Connection timeout
  ↓
Circuit breaker opens
  ↓
Return error to user
```

**Response**:
```java
return AgentResponse.builder()
    .type(AgentResponseType.ERROR)
    .message("Place search is temporarily unavailable.")
    .errors(List.of(
        "Our place search service is experiencing issues",
        "Please try again in a few minutes"
    ))
    .build();
```

**Circuit Breaker Logic**:
```java
// After 5 consecutive failures, open circuit for 1 minute
if (consecutiveFailures.get() >= 5) {
    circuitBreakerOpen.set(true);
    circuitBreakerOpenTime.set(System.currentTimeMillis());
    logger.error("Circuit breaker OPENED - too many Google Places failures");
}

// Check if circuit should be closed
if (circuitBreakerOpen.get()) {
    long openDuration = System.currentTimeMillis() - circuitBreakerOpenTime.get();
    if (openDuration > 60000) { // 1 minute
        circuitBreakerOpen.set(false);
        consecutiveFailures.set(0);
        logger.info("Circuit breaker CLOSED - retrying Google Places");
    } else {
        throw new CircuitBreakerOpenException("Google Places API unavailable");
    }
}
```

---

#### **Scenario: Invalid User Input**

**User Input**: "add a place" (too vague)

**Flow**:
```
EditorAgent: LLM generates ChangeSet
  ↓
PlaceResolutionService: Cannot build meaningful query
  ↓
Return error asking for clarification
```

**Response**:
```java
return AgentResponse.builder()
    .type(AgentResponseType.ERROR)
    .message("I need more details to search for a place.")
    .errors(List.of(
        "What type of place? (restaurant, museum, park, etc.)",
        "Any specific name or area?"
    ))
    .build();
```

---

### 10.2 Retry & Recovery Strategies

#### **Enrichment Retry Logic**
```java
@Service
public class EnrichmentRetryService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public void scheduleRetry(EnrichmentTask task, long delayMinutes) {
        scheduler.schedule(() -> {
            try {
                enrichmentAgent.enrichNode(task.getItineraryId(), task.getNodeId());
            } catch (Exception e) {
                logger.error("Retry failed for node: {}", task.getNodeId(), e);
                // Don't retry again - give up after 1 retry
            }
        }, delayMinutes, TimeUnit.MINUTES);
        
        logger.info("Scheduled enrichment retry for {} in {} minutes", 
            task.getNodeId(), delayMinutes);
    }
}
```

#### **Rate Limit Backoff**
```
Attempt 1: Fail with 429 → Retry in 15 minutes
Attempt 2: Fail with 429 → Give up (node stays un-enriched)
```

#### **API Timeout Handling**
```java
// In GooglePlacesService
private static final int CONNECTION_TIMEOUT_MS = 5000;  // 5 seconds
private static final int READ_TIMEOUT_MS = 10000;       // 10 seconds

RestTemplate restTemplate = new RestTemplateBuilder()
    .setConnectTimeout(Duration.ofMillis(CONNECTION_TIMEOUT_MS))
    .setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS))
    .build();
```

---

## 11. LLM Optimization Strategy

### 11.1 When to Use LLM vs. Direct API

**Use LLM For**:
1. ✅ **Intent classification** - Determine what user wants to do
2. ✅ **ChangeSet generation** - Create structured changes to itinerary
3. ✅ **Natural language responses** - Explain decisions, answer questions
4. ✅ **Semantic understanding** - "romantic sushi place" → extract criteria

**Use Direct API For**:
1. ✅ **Place search** - Google Places Text Search (faster, accurate, free-ish)
2. ✅ **Place details** - Google Places Details (photos, reviews, hours)
3. ✅ **Geocoding** - Convert addresses to coordinates
4. ✅ **Distance calculations** - Geometry calculations

### 11.2 LLM Call Reduction Strategies

#### **Strategy 1: Cache Intent Classifications**
```java
// Cache common queries to avoid LLM calls
Map<String, IntentResult> intentCache = {
    "add a restaurant" → {intent: "add", taskType: "edit"},
    "remove this activity" → {intent: "remove", taskType: "edit"},
    "what's on day 2" → {intent: "explain", taskType: "explain"}
};
```

#### **Strategy 2: Rule-Based Pre-Filtering**
```java
public IntentResult classifyIntent(String text) {
    // Check for exact matches first (no LLM)
    if (text.matches("(?i)^add (a |an )?(.+?)( on day \\d+)?$")) {
        return new IntentResult("add", "edit", extractEntities(text), 1.0);
    }
    
    // Check for simple patterns (no LLM)
    if (text.toLowerCase().startsWith("what") || text.toLowerCase().startsWith("tell me")) {
        return new IntentResult("question", "explain", null, 0.9);
    }
    
    // Only use LLM for complex/ambiguous requests
    return llmService.classifyIntent(text, context);
}
```

#### **Strategy 3: Batch LLM Calls**
```java
// Instead of calling LLM for each message:
// BEFORE:
for (ChatMessage msg : messages) {
    IntentResult intent = llmService.classifyIntent(msg.getText());
}

// AFTER:
List<IntentResult> intents = llmService.classifyIntentBatch(messages);
```

#### **Strategy 4: Use Smaller Models for Simple Tasks**
```java
// Intent classification: GPT-4o-mini (cheaper)
IntentResult intent = llmService.classifyIntent(text, model="gpt-4o-mini");

// ChangeSet generation: GPT-4o (better accuracy)
ChangeSet changeSet = llmService.generateChangeSet(request, model="gpt-4o");
```

### 11.3 Token Optimization

**Context Summarization**:
```java
// DON'T send full itinerary JSON (10,000+ tokens)
String context = objectMapper.writeValueAsString(itinerary);

// DO send summarized context (2,000 tokens)
String context = summarizationService.summarizeForAgent(itinerary, "editor", 2000);
```

**Prompt Engineering**:
```java
// DON'T use verbose prompts
"Please analyze the user's request and determine what they want to do. 
 Consider the context and provide a detailed classification..."

// DO use concise prompts
"Classify intent. Return JSON: {intent, taskType, entities, confidence}"
```

---

## 12. Technical Implementation Details

### 12.1 New Services to Implement

#### **PlaceResolutionService**
- **File**: `src/main/java/com/tripplanner/service/PlaceResolutionService.java`
- **Dependencies**: `GooglePlacesService`, `PlaceSearchCache`
- **Key Methods**: `search()`, `resolveLocationForNode()`, `buildSearchQuery()`
- **Estimated LOC**: ~300

#### **ConversationStateService**
- **File**: `src/main/java/com/tripplanner/service/ConversationStateService.java`
- **Dependencies**: Caffeine Cache
- **Key Methods**: `store()`, `get()`, `clear()`, `hasPendingState()`
- **Estimated LOC**: ~150

#### **EnrichmentQueueService**
- **File**: `src/main/java/com/tripplanner/service/EnrichmentQueueService.java`
- **Dependencies**: `EnrichmentAgent`, `ExecutorService`
- **Key Methods**: `enqueue()`, `processQueue()`
- **Estimated LOC**: ~200

#### **ChatContextBuilder**
- **File**: `src/main/java/com/tripplanner/service/ChatContextBuilder.java`
- **Dependencies**: `SummarizationService`, `ChatHistoryService`, `UserPreferencesService`
- **Key Methods**: `buildContext()`
- **Estimated LOC**: ~200

---

### 12.2 Service Extensions

#### **GooglePlacesService**
- **Add**: `textSearch()` method
- **Add**: `TextSearchRequest` and `TextSearchResponse` DTOs
- **Estimated LOC**: ~150

#### **OrchestratorService**
- **Update**: `route()` to check `ConversationStateService`
- **Update**: Handle `NEEDS_SELECTION` responses
- **Add**: `handlePlaceSelection()` method
- **Estimated LOC**: ~100 (modifications)

#### **EditorAgent**
- **Add**: Place resolution flow
- **Add**: `handlePlaceSelection()` method
- **Add**: Background enrichment queueing
- **Estimated LOC**: ~200 (additions)

---

### 12.3 New DTOs

```java
// Place-related DTOs
public class PlaceCandidate { ... }      // ~50 LOC
public class PlaceSearchContext { ... }   // ~30 LOC
public class PlaceSearchFilters { ... }   // ~30 LOC
public class PlaceResolutionResult { ... }// ~40 LOC

// Response DTOs
public class AgentResponse { ... }        // ~60 LOC
public enum AgentResponseType { ... }     // ~10 LOC
public class ResponseMetadata { ... }     // ~30 LOC
public class BackgroundTask { ... }       // ~30 LOC

// State DTOs
public class ConversationState { ... }    // ~40 LOC
public class PlaceSelectionState { ... }  // ~30 LOC

// Context DTOs
public class ChatContext { ... }          // ~50 LOC

// Enrichment DTOs
public class EnrichmentTask { ... }       // ~20 LOC

// Total new DTOs: ~420 LOC
```

---

### 12.4 Frontend Components

#### **PlaceCard.tsx**
```typescript
interface PlaceCardProps {
  candidate: PlaceCandidate;
  onSelect: () => void;
  selected: boolean;
}
// Estimated: ~150 LOC
```

#### **PlaceCardGrid.tsx**
```typescript
interface PlaceCardGridProps {
  candidates: PlaceCandidate[];
  onCardSelect: (index: number) => void;
  metadata: ResponseMetadata;
}
// Estimated: ~100 LOC
```

#### **ChatInput.tsx** (Modifications)
- Add state-based disabling
- Add placeholder variations
- Estimated: ~50 LOC (modifications)

#### **ChatMessage.tsx** (Modifications)
- Add card grid rendering
- Add progress indicators
- Add error styling
- Estimated: ~100 LOC (modifications)

---

### 12.5 API Endpoints

#### **Existing: POST /api/v1/itineraries/{id}/chat**
**Modifications**:
- Handle `selectedIndex` in request for place selection
- Return `AgentResponse` structure

**Request**:
```json
{
  "text": "add a sushi place on day 2",
  "itineraryId": "itin-123",
  "userId": "user-456",
  "scope": "trip",
  "day": 2,
  "selectedIndex": null  // NEW: for card selection
}
```

**Response (NEEDS_SELECTION)**:
```json
{
  "type": "NEEDS_SELECTION",
  "message": "I found 5 sushi restaurants near Hadibo. Select one:",
  "data": {
    "candidates": [
      {
        "placeId": "ChIJ...",
        "name": "Sushi Zen",
        "rating": 4.5,
        "ratingCount": 234,
        "priceLevel": 3,
        "formattedAddress": "123 Main St, Hadibo",
        "coordinates": {"lat": 12.5, "lng": 53.9},
        "photoUrl": "https://...",
        "isOpenNow": true,
        "distance": 350,
        "cuisine": ["Japanese", "Sushi"]
      }
      // ... 4 more
    ]
  },
  "metadata": {
    "uiComponent": "place-card-grid",
    "actionLabel": "Select",
    "timeoutSeconds": 300
  }
}
```

**Response (COMPLETE)**:
```json
{
  "type": "COMPLETE",
  "message": "Added Sushi Zen to day 2 ✅",
  "data": {
    "toVersion": 5,
    "diff": { ... }
  },
  "backgroundTask": {
    "taskType": "enrichment",
    "targetAgent": "EnrichmentAgent",
    "priority": 5,
    "estimatedDurationMs": 3000
  }
}
```

---

## 13. API Specifications

### 13.1 Google Places Text Search API

**Endpoint**: `https://maps.googleapis.com/maps/api/place/textsearch/json`

**Request**:
```
GET /maps/api/place/textsearch/json
  ?query=sushi%20restaurant%20in%20Hadibo
  &location=12.5,53.9
  &radius=5000
  &fields=place_id,name,geometry,formatted_address,rating,user_ratings_total,price_level,photos,types,opening_hours
  &key=YOUR_API_KEY
```

**Response**:
```json
{
  "results": [
    {
      "place_id": "ChIJ...",
      "name": "Sushi Zen",
      "formatted_address": "123 Main St, Hadibo, Yemen",
      "geometry": {
        "location": {
          "lat": 12.5,
          "lng": 53.9
        }
      },
      "rating": 4.5,
      "user_ratings_total": 234,
      "price_level": 3,
      "photos": [
        {
          "photo_reference": "ABC123...",
          "height": 1080,
          "width": 1920
        }
      ],
      "types": ["restaurant", "food"],
      "opening_hours": {
        "open_now": true
      }
    }
  ],
  "status": "OK"
}
```

**Rate Limits**:
- Free tier: 1,000 requests/day
- Paid tier: Unlimited (billed per request)
- Our usage: ~100-200 requests/day estimated

**Pricing** (as of 2024):
- Text Search: $32 per 1,000 requests
- With caching: ~$3-6/day for 100 requests

---

### 13.2 Google Places Details API

**Endpoint**: `https://maps.googleapis.com/maps/api/place/details/json`

**Request**:
```
GET /maps/api/place/details/json
  ?place_id=ChIJ...
  &fields=photos,reviews,opening_hours,price_level,rating,website,formatted_phone_number
  &key=YOUR_API_KEY
```

**Response**:
```json
{
  "result": {
    "photos": [...],
    "reviews": [
      {
        "author_name": "John Doe",
        "rating": 5,
        "text": "Best sushi in Hadibo!",
        "time": 1672531200
      }
    ],
    "opening_hours": {
      "weekday_text": [
        "Monday: 11:00 AM – 10:00 PM",
        "Tuesday: 11:00 AM – 10:00 PM",
        ...
      ]
    },
    "price_level": 3,
    "rating": 4.5
  },
  "status": "OK"
}
```

**Rate Limits**: Same as Text Search

**Pricing**: $17 per 1,000 requests

---

## 14. Testing Strategy

### 14.1 Unit Tests

#### **PlaceResolutionService**
```java
@Test
void searchReturnsFilteredCandidates() {
    // Mock Google Places API response
    // Call service.search()
    // Assert: correct number of candidates
    // Assert: filtered by rating >= 3.5
}

@Test
void searchCachesResults() {
    // Call service.search() twice with same query
    // Assert: Google Places API called only once
}

@Test
void buildSearchQueryIncludesContext() {
    // Create node with title "Sushi Dinner"
    // Call buildSearchQuery()
    // Assert: query = "Sushi restaurant in Hadibo"
}
```

#### **ConversationStateService**
```java
@Test
void storeAndRetrieveState() {
    // Store state for user
    // Retrieve state
    // Assert: state matches
}

@Test
void stateExpiresAfterTTL() {
    // Store state with 1-second TTL
    // Wait 2 seconds
    // Assert: state is null
}
```

#### **EditorAgent**
```java
@Test
void addPlaceWithMultipleMatches() {
    // Mock PlaceResolutionService to return 5 candidates
    // Call agent.execute()
    // Assert: response type = NEEDS_SELECTION
    // Assert: data contains 5 candidates
}

@Test
void addPlaceWithSingleMatch() {
    // Mock PlaceResolutionService to return 1 candidate
    // Call agent.execute()
    // Assert: response type = COMPLETE
    // Assert: node added to itinerary
    // Assert: background enrichment queued
}
```

---

### 14.2 Integration Tests

#### **Place Search E2E**
```java
@Test
void userAddsPlaceFullFlow() {
    // 1. Send chat: "add sushi place on day 2"
    // 2. Assert: Response has cards
    // 3. Send selection: selectedIndex = 0
    // 4. Assert: Node added to itinerary
    // 5. Wait for enrichment (mock WebSocket)
    // 6. Assert: Node has photos and reviews
}
```

#### **Multi-Turn Conversation**
```java
@Test
void conversationStatePreservedAcrossRequests() {
    // 1. User: "add sushi place"
    // 2. Bot: Shows cards
    // 3. User: "the second one"
    // 4. Assert: Correct place added
}
```

---

### 14.3 Frontend Tests

#### **PlaceCard Component**
```typescript
test('renders place info correctly', () => {
  const candidate = createMockCandidate();
  render(<PlaceCard candidate={candidate} onSelect={jest.fn()} />);
  
  expect(screen.getByText('Sushi Zen')).toBeInTheDocument();
  expect(screen.getByText('⭐ 4.5')).toBeInTheDocument();
});

test('calls onSelect when clicked', () => {
  const onSelect = jest.fn();
  render(<PlaceCard candidate={mockCandidate} onSelect={onSelect} />);
  
  fireEvent.click(screen.getByRole('button'));
  expect(onSelect).toHaveBeenCalled();
});
```

#### **Chat Input State**
```typescript
test('disables input when awaiting selection', () => {
  const { getByPlaceholderText } = render(
    <ChatInput state="AWAITING_CARD_SELECTION" />
  );
  
  const input = getByPlaceholderText('Select a card above');
  expect(input).toBeDisabled();
});
```

---

## 15. Performance Requirements

### 15.1 Response Time SLAs

| Operation                          | Target     | Max Acceptable |
|------------------------------------|------------|----------------|
| Intent classification (LLM)        | < 1s       | 2s             |
| Place search (Google API)          | < 1s       | 3s             |
| Add place with selection (initial) | < 2s       | 4s             |
| Add place with single match        | < 2s       | 4s             |
| Apply ChangeSet                    | < 500ms    | 1s             |
| Background enrichment              | < 5s       | 10s            |
| WebSocket update propagation       | < 200ms    | 500ms          |

### 15.2 Caching Strategy

**Place Search Cache**:
- **TTL**: 1 hour
- **Size**: Max 1000 entries
- **Eviction**: LRU
- **Cache key**: `query + location + filters`

**Place Details Cache**:
- **TTL**: 24 hours
- **Size**: Max 5000 entries
- **Eviction**: LRU
- **Cache key**: `placeId`

**Intent Classification Cache**:
- **TTL**: 7 days
- **Size**: Max 500 entries
- **Eviction**: LRU
- **Cache key**: `normalizedQuery`

### 15.3 Rate Limiting

**Google Places API**:
- **Limit**: 1000 requests/day (free tier)
- **Strategy**: Track daily usage, warn at 80%, reject at 95%
- **Fallback**: Return error, ask user to retry later

**LLM API**:
- **Limit**: 10,000 requests/day
- **Strategy**: Use caching and rule-based pre-filtering to reduce calls

### 15.4 Scalability

**Concurrent Users**: Support 100 concurrent chat requests

**Background Workers**: 3 threads for enrichment queue

**WebSocket Connections**: Support 500 concurrent connections

---

## 16. Open Questions

### 16.1 Product Questions

**Q1**: Should we support **voice input** for place search in the future?
- If yes, need speech-to-text integration
- Could improve mobile UX

**Q2**: Should place cards show **real-time pricing** (e.g., OpenTable integration)?
- Pros: Better user decision-making
- Cons: More API calls, complexity

**Q3**: Should we support **booking directly from cards**?
- E.g., "Reserve table" button on restaurant cards
- Requires booking API integrations

**Q4**: Should we allow users to **add custom places** (not from Google)?
- Use case: User knows a local spot not on Google Maps
- Implementation: Manual input form

**Q5**: For multi-day trips, should place search be **context-aware**?
- E.g., If user is in Barcelona on day 2, bias search results to Barcelona
- Requires tracking user's "current location" per day

---

### 16.2 Technical Questions

**Q6**: How should we handle **pagination** for large place search results?
- Show first 8, lazy load more?
- Or limit to top 8 always?

**Q7**: Should conversation state be **persistent** (Firestore) or **in-memory**?
- In-memory: Fast, but lost on server restart
- Firestore: Persistent, but slower, more expensive

**Q8**: Should enrichment be **real-time** or **batch**?
- Real-time: Enrich immediately after adding place
- Batch: Enrich all places at once (e.g., nightly job)

**Q9**: Should we implement **optimistic updates** for place addition?
- Show node in itinerary before server confirms
- Revert if server fails

**Q10**: How should we handle **place disambiguation** when user says "that restaurant"?
- Resolve from chat history context
- Ask user for clarification
- Use NLP to detect references

---

### 16.3 UX Questions

**Q11**: Should cards show **distance from previous activity** or **distance from city center**?
- Previous activity: More contextual for itinerary planning
- City center: Easier to understand

**Q12**: Should we show a **"Add Anyway"** button when no results found?
- Allow user to add place with manual input
- Useful for new/unlisted places

**Q13**: Should enrichment progress be **visible** or **silent**?
- Visible: User sees "Fetching photos..." indicator
- Silent: Update happens in background, no UI

**Q14**: Should we support **filtering cards** (e.g., "only show open now")?
- Adds complexity to UI
- But improves decision-making

**Q15**: Should users be able to **compare** multiple places side-by-side?
- E.g., Show 2-3 cards in comparison mode
- Useful for decision-making

---

### 16.4 Data Questions

**Q16**: How should we handle **place data staleness**?
- Google Places data can change (hours, rating, photos)
- Re-enrich periodically? Manual refresh?

**Q17**: Should we track **user place preferences** to improve future suggestions?
- E.g., If user always picks 4.5+ rated places, prioritize those
- Requires user preference storage

**Q18**: Should we support **offline mode**?
- Cache recent place searches
- Allow viewing itinerary without internet

**Q19**: How should we handle **place duplicates**?
- E.g., User adds "Sushi Zen" twice accidentally
- Detect and warn? Auto-dedupe?

**Q20**: Should we store **full place details** or just **placeId**?
- Full details: Faster to display, but data can become stale
- Just placeId: Always fresh, but slower to load

---

## Appendices

### Appendix A: Sample Chat Flows

#### Flow 1: Add Place with Selection
```
User: add a sushi place on day 2

Bot: I found 5 sushi restaurants near Hadibo. Select one:
     [Card Grid: 5 cards]
     
User: [Clicks Card 2: Tokyo Bay]

Bot: Adding Tokyo Bay to day 2... ⏳

Bot: Added Tokyo Bay to day 2 ✅
     Fetching photos and reviews... 🔄
     
[5 seconds later]

Bot: Tokyo Bay details updated ✅
```

---

#### Flow 2: Add Place with Single Match
```
User: add Sushi Zen restaurant on day 2

Bot: Adding Sushi Zen to day 2... ⏳

Bot: Added Sushi Zen to day 2 ✅
     Fetching photos and reviews... 🔄
     
[3 seconds later]

Bot: Sushi Zen details updated ✅
```

---

#### Flow 3: No Results
```
User: add Moon Restaurant on day 2

Bot: ⚠️ I couldn't find 'Moon Restaurant' near Hadibo.
     • No results found for this search
     • Try adding more details (street name, area)
     • Check spelling
```

---

#### Flow 4: Enrichment Failure (Silent)
```
User: add Sushi Zen on day 2

Bot: Added Sushi Zen to day 2 ✅
     
[Node appears in itinerary with basic data]
[Enrichment fails silently in background]
[Node remains with basic data - no photos/reviews]
```

---

### Appendix B: Database Schema Changes

**No schema changes required** - All conversation state is in-memory cache.

**Optional**: If we decide to persist conversation state:
```
Collection: conversationStates
Document ID: userId
Fields:
  - type: string (PLACE_SELECTION, CONFIRMATION, etc.)
  - data: object (PlaceSelectionState, etc.)
  - timestamp: number
  - expiresAt: number
```

---

### Appendix C: Environment Variables

**New Variables**:
```bash
# Place Search Settings
PLACE_SEARCH_CACHE_TTL_HOURS=1
PLACE_SEARCH_CACHE_MAX_SIZE=1000
PLACE_SEARCH_MAX_RESULTS=8

# Enrichment Settings
ENRICHMENT_QUEUE_WORKERS=3
ENRICHMENT_RETRY_DELAY_MINUTES=15
ENRICHMENT_BACKGROUND_ENABLED=true

# Conversation State
CONVERSATION_STATE_TTL_MINUTES=5
CONVERSATION_STATE_MAX_SIZE=1000

# LLM Optimization
LLM_INTENT_CACHE_ENABLED=true
LLM_INTENT_CACHE_TTL_DAYS=7
LLM_RULE_BASED_FALLBACK=true
```

---

### Appendix D: Metrics & Monitoring

**Key Metrics to Track**:
1. **Place Search Success Rate**: % of searches returning results
2. **Enrichment Success Rate**: % of enrichments completing successfully
3. **User Selection Rate**: % of times user selects from cards vs. getting single match
4. **Average Response Time**: Time from user message to first bot response
5. **LLM Call Rate**: Calls per day (optimize to reduce)
6. **Google Places API Usage**: Requests per day (monitor cost)
7. **Conversation State Usage**: Active states, expiration rate

**Alerts**:
- Google Places API >950 requests/day (near limit)
- Enrichment failure rate >10%
- Average response time >4 seconds
- LLM call rate >1000/day (cost concern)

---

## Document Metadata

**Version**: 1.0 Draft  
**Last Updated**: January 2025  
**Authors**: Product Team  
**Status**: Awaiting Approval  
**Next Steps**: Review open questions, prioritize Phase 1 implementation





