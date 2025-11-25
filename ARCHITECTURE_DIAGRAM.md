# Agentic Itinerary Planner - Architecture Diagram

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                    │
│                         (React + TypeScript + Vite)                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ HTTP/REST + WebSocket
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API GATEWAY LAYER                                  │
│                      (Spring Boot REST Controllers)                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
        ┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
        │  Itineraries     │ │   Chat       │ │   WebSocket      │
        │  Controller      │ │   Controller │ │   Controller     │
        └──────────────────┘ └──────────────┘ └──────────────────┘
                    │                 │                 │
                    └─────────────────┼─────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SERVICE ORCHESTRATION LAYER                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌──────────────────┐      ┌──────────────────────┐      ┌──────────────────┐
│ Itinerary        │      │ Pipeline             │      │ Orchestrator     │
│ Service          │      │ Orchestrator         │      │ Service          │
│                  │      │                      │      │                  │
│ - CRUD ops       │      │ - Multi-agent        │      │ - Intent         │
│ - Status calc    │      │   coordination       │      │   classification │
│ - User trips     │      │ - Phase management   │      │ - Agent routing  │
└──────────────────┘      └──────────────────────┘      └──────────────────┘
        │                             │                             │
        └─────────────────────────────┼─────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            AGENT EXECUTION LAYER                             │
│                          (Multi-Agent AI System)                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌─────────────┬───────────────┼───────────────┬─────────────┐
        │             │               │               │             │
        ▼             ▼               ▼               ▼             ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────┐
│ Skeleton     │ │ Activity │ │   Meal   │ │ Transport    │ │ Enrichment│
│ Planner      │ │  Agent   │ │  Agent   │ │   Agent      │ │  Agent   │
│ Agent        │ │          │ │          │ │              │ │          │
│              │ │          │ │          │ │              │ │          │
│ - Day        │ │ - Find   │ │ - Find   │ │ - Calculate  │ │ - Google │
│   structure  │ │   places │ │   dining │ │   routes     │ │   Places │
│ - Timeline   │ │ - Details│ │   spots  │ │ - Optimize   │ │ - Photos │
└──────────────┘ └──────────┘ └──────────┘ └──────────────┘ └──────────┘
        │             │               │               │             │
        └─────────────┴───────────────┴───────────────┴─────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CORE SERVICES LAYER                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌─────────────┬───────────────┼───────────────┬─────────────┐
        │             │               │               │             │
        ▼             ▼               ▼               ▼             ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────┐
│ Itinerary    │ │ Change   │ │ Revision │ │ Agent        │ │ User     │
│ JSON         │ │ Engine   │ │ Service  │ │ Registry     │ │ Data     │
│ Service      │ │          │ │          │ │              │ │ Service  │
│              │ │          │ │          │ │              │ │          │
│ - Master     │ │ - Propose│ │ - Version│ │ - Dynamic    │ │ - Trip   │
│   JSON       │ │ - Apply  │ │   control│ │   agent      │ │   metadata│
│ - CRUD       │ │ - Undo   │ │ - Rollback│ │   discovery  │ │ - Ownership│
└──────────────┘ └──────────┘ └──────────┘ └──────────────┘ └──────────┘
        │             │               │               │             │
        └─────────────┴───────────────┴───────────────┴─────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      EXTERNAL INTEGRATIONS LAYER                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌─────────────┬───────────────┼───────────────┬─────────────┐
        │             │               │               │             │
        ▼             ▼               ▼               ▼             ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────┐
│ LLM          │ │ Google   │ │ Firebase │ │ Razorpay     │ │ Email    │
│ Service      │ │ Places   │ │ Auth     │ │ Payment      │ │ Service  │
│              │ │ Service  │ │          │ │              │ │          │
│ - OpenRouter │ │          │ │          │ │              │ │          │
│ - Gemini     │ │ - Search │ │ - OAuth  │ │ - Orders     │ │ - SMTP   │
│ - Fallback   │ │ - Details│ │ - Tokens │ │ - Webhooks   │ │ - Share  │
└──────────────┘ └──────────┘ └──────────┘ └──────────────┘ └──────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATA PERSISTENCE LAYER                              │
│                         (Google Cloud Firestore)                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌──────────────────┐      ┌──────────────────────┐      ┌──────────────────┐
│ itineraries/     │      │ users/{userId}/      │      │ revisions/       │
│ {itineraryId}    │      │ trips/{tripId}       │      │ {itineraryId}/   │
│                  │      │                      │      │ {version}        │
│ - Master JSON    │      │ - Trip metadata      │      │                  │
│ - Days/Nodes     │      │ - Ownership          │      │ - Version history│
│ - Agent states   │      │ - Preferences        │      │ - Diffs          │
└──────────────────┘      └──────────────────────┘      └──────────────────┘
```

## Real-Time Communication Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         REAL-TIME UPDATES FLOW                               │
└─────────────────────────────────────────────────────────────────────────────┘

Frontend                    Backend                      Agents
   │                           │                           │
   │  1. Connect WebSocket     │                           │
   ├──────────────────────────>│                           │
   │  (SockJS + STOMP)         │                           │
   │                           │                           │
   │  2. Subscribe to topics   │                           │
   ├──────────────────────────>│                           │
   │  /topic/itinerary/{id}    │                           │
   │  /topic/agent/{id}        │                           │
   │  /topic/chat/{id}         │                           │
   │                           │                           │
   │                           │  3. Agent starts          │
   │                           │<──────────────────────────┤
   │                           │                           │
   │  4. Progress event        │                           │
   │<──────────────────────────┤                           │
   │  {type: "agent_progress"} │                           │
   │                           │                           │
   │                           │  5. Agent updates data    │
   │                           │<──────────────────────────┤
   │                           │                           │
   │  6. Itinerary update      │                           │
   │<──────────────────────────┤                           │
   │  {type: "itinerary_updated"}                          │
   │                           │                           │
   │  7. UI auto-refreshes     │                           │
   │  (no polling needed)      │                           │
   │                           │                           │
```

## Data Flow: Itinerary Creation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ITINERARY CREATION PIPELINE                             │
└─────────────────────────────────────────────────────────────────────────────┘

1. User Request
   │
   ├─> POST /api/v1/itineraries
   │   Body: {destination, dates, preferences, party}
   │
   ▼
2. ItinerariesController
   │
   ├─> Extract userId from Firebase token
   ├─> Validate request
   ├─> Call ItineraryService.create()
   │
   ▼
3. ItineraryService
   │
   ├─> Generate itineraryId
   ├─> Create initial structure (SYNC)
   ├─> Save to Firestore
   ├─> Return immediate response (status: "generating")
   │
   ├─> Start PipelineOrchestrator (ASYNC)
   │
   ▼
4. PipelineOrchestrator
   │
   ├─> Phase 1: Skeleton Generation (2-3 min)
   │   │
   │   ├─> SkeletonPlannerAgent
   │   ├─> Creates day structure
   │   ├─> Publishes progress: 10-30%
   │   │
   │   ▼
   ├─> Phase 2: Node Population (PARALLEL, 3-5 min)
   │   │
   │   ├─> ActivityAgent (finds attractions)
   │   ├─> MealAgent (finds restaurants)
   │   ├─> TransportAgent (calculates routes)
   │   ├─> Publishes progress: 30-70%
   │   │
   │   ▼
   ├─> Phase 3: Enrichment (1-2 min)
   │   │
   │   ├─> EnrichmentAgent
   │   ├─> Google Places API calls
   │   ├─> Adds coordinates, photos, ratings
   │   ├─> Publishes progress: 70-90%
   │   │
   │   ▼
   ├─> Phase 4: Cost Estimation (30 sec)
   │   │
   │   ├─> CostEstimatorAgent
   │   ├─> Calculates budget
   │   ├─> Publishes progress: 90-95%
   │   │
   │   ▼
   ├─> Phase 5: Finalization (10 sec)
   │   │
   │   ├─> Calculate totals
   │   ├─> Update status to "completed"
   │   ├─> Save to Firestore
   │   ├─> Publishes progress: 100%
   │   │
   │   ▼
   └─> WebSocket broadcast: "generation_complete"
       │
       ▼
5. Frontend receives update
   │
   ├─> UI shows completion
   ├─> Fetches final itinerary
   └─> Displays to user
```

## Agent Communication Pattern

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AGENT EXECUTION PATTERN                              │
└─────────────────────────────────────────────────────────────────────────────┘

BaseAgent (Abstract)
   │
   ├─> execute(itineraryId, request)
   │   │
   │   ├─> 1. Validate responsibility
   │   │      (Can this agent handle this task?)
   │   │
   │   ├─> 2. Emit "queued" event
   │   │      └─> AgentEventBus.publish()
   │   │
   │   ├─> 3. Emit "running" event (progress: 10%)
   │   │
   │   ├─> 4. Call executeInternal() (agent-specific)
   │   │      │
   │   │      ├─> LLM Service call
   │   │      ├─> External API calls
   │   │      ├─> Data processing
   │   │      ├─> Emit progress updates (20%, 50%, 80%)
   │   │      │
   │   │      └─> Return result
   │   │
   │   ├─> 5. Emit "completed" event (progress: 100%)
   │   │
   │   └─> 6. Return result to orchestrator
   │
   └─> Error handling
       │
       └─> Emit "failed" event with error details

AgentEventBus
   │
   ├─> Receives events from agents
   ├─> Publishes to WebSocket topics
   └─> Frontend receives real-time updates
```

## Data Model: NormalizedItinerary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      NORMALIZED ITINERARY STRUCTURE                          │
└─────────────────────────────────────────────────────────────────────────────┘

NormalizedItinerary
├── itineraryId: string
├── userId: string
├── version: integer
├── status: "planning" | "generating" | "completed" | "failed"
├── summary: string
├── createdAt: timestamp
├── updatedAt: timestamp
│
├── days: NormalizedDay[]
│   │
│   ├── dayNumber: integer
│   ├── date: string (ISO)
│   ├── location: string
│   │
│   └── nodes: NormalizedNode[]
│       │
│       ├── id: string
│       ├── type: "activity" | "meal" | "transport" | "accommodation"
│       ├── title: string
│       ├── description: string
│       ├── startTime: string
│       ├── endTime: string
│       ├── duration: integer (minutes)
│       ├── locked: boolean
│       │
│       ├── location: Location
│       │   ├── name: string
│       │   ├── address: string
│       │   ├── coordinates: {lat, lng}
│       │   ├── placeId: string (Google)
│       │   └── photos: string[]
│       │
│       ├── cost: Cost
│       │   ├── amountPerPerson: number
│       │   ├── currency: string
│       │   └── tier: "budget" | "medium" | "luxury"
│       │
│       └── metadata: object
│           ├── source: "ai" | "user" | "enrichment"
│           ├── confidence: number (0-1)
│           └── lastModified: timestamp
│
└── agents: Map<AgentKind, AgentState>
    │
    ├── PLANNER
    ├── ENRICHMENT
    ├── EDITOR
    └── BOOKING
        │
        ├── status: "idle" | "running" | "completed" | "failed"
        ├── progress: integer (0-100)
        ├── lastRun: timestamp
        └── error: string?
```

## API Endpoints Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            REST API ENDPOINTS                                │
└─────────────────────────────────────────────────────────────────────────────┘

ITINERARIES
├── POST   /api/v1/itineraries
│   └─> Create new itinerary (returns immediately, generates async)
│
├── GET    /api/v1/itineraries
│   └─> List user's itineraries
│
├── GET    /api/v1/itineraries/{id}
│   └─> Get itinerary summary
│
├── GET    /api/v1/itineraries/{id}/json
│   └─> Get full normalized JSON
│
├── DELETE /api/v1/itineraries/{id}
│   └─> Delete itinerary
│
├── POST   /api/v1/itineraries/{id}:propose
│   └─> Preview changes (no DB write)
│
├── POST   /api/v1/itineraries/{id}:apply
│   └─> Apply changes (writes to DB)
│
├── POST   /api/v1/itineraries/{id}:undo
│   └─> Rollback to previous version
│
└── PUT    /api/v1/itineraries/{id}/nodes/{nodeId}/lock
    └─> Lock/unlock node for editing

CHAT
├── POST   /api/v1/chat
│   └─> Send chat message (intent classification + agent routing)
│
└── GET    /api/v1/chat/{itineraryId}/history
    └─> Get chat history

AGENTS
├── POST   /api/v1/itineraries/{id}/agents/{agentType}/execute
│   └─> Execute specific agent
│
└── GET    /api/v1/itineraries/{id}/agents/{agentType}/status
    └─> Get agent execution status

WEBSOCKET
└── /ws
    ├─> /topic/itinerary/{id}  (itinerary updates)
    ├─> /topic/agent/{id}      (agent progress)
    └─> /topic/chat/{id}       (chat responses)
```

## Technology Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           TECHNOLOGY STACK                                   │
└─────────────────────────────────────────────────────────────────────────────┘

FRONTEND
├── React 18
├── TypeScript
├── Vite (build tool)
├── Tailwind CSS
├── Radix UI (components)
├── Axios (HTTP client)
├── STOMP.js + SockJS (WebSocket)
└── React Router (navigation)

BACKEND
├── Java 17
├── Spring Boot 3.x
│   ├── Spring Web
│   ├── Spring WebSocket
│   └── Spring Security
├── Gradle (build tool)
└── SLF4J + Logback (logging)

AI/ML
├── OpenRouter API (primary LLM)
├── Google Gemini (fallback LLM)
└── Multi-agent orchestration

EXTERNAL SERVICES
├── Google Cloud Firestore (database)
├── Google Places API (location data)
├── Google OAuth (authentication)
├── Razorpay (payments)
└── SMTP (email)

INFRASTRUCTURE
├── Docker (containerization)
├── Google Cloud Run (deployment)
└── Cloud Build (CI/CD)
```

## Security & Authentication Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      AUTHENTICATION & AUTHORIZATION                          │
└─────────────────────────────────────────────────────────────────────────────┘

1. User Login
   │
   ├─> Google OAuth (frontend)
   ├─> Firebase Auth SDK
   └─> Receives ID token (JWT)

2. API Request
   │
   ├─> Frontend adds token to header
   │   Authorization: Bearer {token}
   │
   └─> Backend validates token
       │
       ├─> FirebaseAuthConfig
       ├─> Verifies signature
       ├─> Extracts userId
       └─> Sets in request attributes

3. Authorization
   │
   ├─> UserDataService.userOwnsTrip()
   ├─> Checks Firestore ownership
   └─> Returns 403 if unauthorized

4. Token Refresh
   │
   ├─> Frontend detects 401
   ├─> Calls Firebase getIdToken(true)
   ├─> Retries request with new token
   └─> Redirects to login if refresh fails
```

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DEPLOYMENT ARCHITECTURE                              │
└─────────────────────────────────────────────────────────────────────────────┘

                        ┌─────────────────┐
                        │   Cloud CDN     │
                        │   (Static)      │
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │  Load Balancer  │
                        └────────┬────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
        │ Cloud Run    │ │ Cloud Run   │ │ Cloud Run   │
        │ Instance 1   │ │ Instance 2  │ │ Instance 3  │
        │ (Backend)    │ │ (Backend)   │ │ (Backend)   │
        └───────┬──────┘ └──────┬──────┘ └──────┬──────┘
                │                │                │
                └────────────────┼────────────────┘
                                 │
                        ┌────────▼────────┐
                        │   Firestore     │
                        │   (Database)    │
                        └─────────────────┘

FEATURES
├── Auto-scaling (0-N instances)
├── WebSocket support (sticky sessions)
├── HTTPS/TLS encryption
├── Health checks
└── Rolling deployments
```

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Status:** Production Ready
