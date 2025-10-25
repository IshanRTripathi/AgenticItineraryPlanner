# Requirements Document

## Introduction

This specification defines the requirements for creating a comprehensive technical documentation of the entire frontend application to enable a complete UI redesign while preserving all existing functionality. The documentation will serve as a complete reference for designers and developers to understand the current system architecture, component structure, API integrations, and user workflows before undertaking a UI redesign effort.

### Application Context

The application is an AI-powered travel itinerary planner called "MVP Click-by-Click Experience" that allows users to:
- Create personalized travel itineraries using AI agents
- View and edit itineraries in multiple views (day-by-day, workflow/timeline, map)
- Interact with AI through chat to modify itineraries
- Book travel components (hotels, activities, transportation)
- Share and export itineraries
- Track real-time AI agent progress during itinerary generation

The frontend is built with React 18.3.1, uses Vite as the build tool, and integrates with a Java Spring Boot backend via REST APIs and Server-Sent Events (SSE) for real-time updates.

### Key Technical Stack (from package.json)
- React 18.3.1 with TypeScript
- React Router 6.30.1 for routing
- TanStack React Query 5.89.0 for server state management
- Zustand 5.0.8 for client state management
- Radix UI component library (comprehensive set of primitives)
- Tailwind CSS for styling
- Google Maps integration (@googlemaps/markerclusterer 2.6.2)
- Firebase 10.13.0 for authentication
- ReactFlow for workflow visualization
- i18next 25.5.2 for internationalization (supports en, hi, bn, te)
- Recharts 2.15.2 for data visualization
- Vitest for testing

## Requirements

### Requirement 1: Backend API Integration Inventory

**User Story:** As a frontend developer planning a UI redesign, I want a complete inventory of all backend API integrations, so that I can ensure the redesigned UI maintains compatibility with all existing backend endpoints.

#### Acceptance Criteria

1. WHEN documenting REST API endpoints THEN the system SHALL list all endpoints consumed by apiClient.ts including:
   - Itinerary endpoints (POST /itineraries, GET /itineraries/{id}/json, DELETE /itineraries/{id}, etc.)
   - Agent endpoints (GET /agents/events/{itineraryId}, POST /agents/process-request, POST /agents/apply-with-enrichment)
   - Tools endpoints (POST /packing-list, POST /photo-spots, POST /must-try-foods, POST /cost-estimator)
   - Booking endpoints (POST /book, POST /payments/razorpay/order)
   - Auth endpoints (POST /auth/google, GET /auth/me)
   - Workflow sync endpoints (PUT /itineraries/{id}/workflow, PUT /itineraries/{id}/nodes/{nodeId})
   - Lock endpoints (PUT /itineraries/{id}/nodes/{nodeId}/lock)
   - Revision endpoints (GET /itineraries/{id}/revisions, POST /itineraries/{id}/revisions/{version}/rollback)
   - Export endpoints (GET /itineraries/{id}/pdf, POST /email/send)

2. WHEN documenting SSE connections THEN the system SHALL document:
   - Agent progress stream (GET /agents/events/{itineraryId}) with event types: connected, agent-progress, agent-complete, agent-error
   - Patches stream (GET /itineraries/patches?itineraryId={id}&executionId={id}) for real-time itinerary updates
   - Token-based authentication via query parameters for SSE connections
   - Connection management in sseManager.ts

3. WHEN mapping component dependencies THEN the system SHALL identify:
   - SimplifiedAgentProgress uses agent SSE stream
   - TravelPlanner uses itinerary CRUD endpoints
   - WorkflowBuilder uses workflow sync endpoints
   - Chat components use process-request endpoint
   - Booking components use booking and payment endpoints
   - All components using useItinerary hook from state/query/hooks.ts

4. WHEN documenting authentication THEN the system SHALL specify:
   - Firebase Authentication integration (GoogleSignIn component)
   - JWT token management in AuthContext
   - Bearer token authentication for REST APIs
   - Token refresh logic in apiClient (proactive refresh within 5 minutes of expiry)
   - Token expiry handling with automatic retry on 401 errors
   - Query parameter token passing for SSE connections

5. WHEN documenting request/response schemas THEN the system SHALL include TypeScript interfaces from:
   - CreateItineraryRequest, ItineraryCreationResponse, ItineraryResponse
   - NormalizedItinerary, NormalizedDay, NormalizedNode types
   - ChangeSet, ProposeResponse, ApplyRequest, ApplyResponse
   - AgentEvent, PackingListRequest/Response, PhotoSpotsRequest/Response
   - All DTOs defined in apiClient.ts and type files

6. IF backend features exist THEN the system SHALL document:
   - Unused endpoints from swagger-api-documentation.yaml
   - Deprecated endpoints or response fields
   - Backend capabilities not yet exposed in UI

### Requirement 2: Frontend Architecture Overview

**User Story:** As a technical architect, I want a comprehensive overview of the frontend architecture, so that I can understand the current tech stack and design patterns before planning a redesign.

#### Acceptance Criteria

1. WHEN documenting the tech stack THEN the system SHALL include exact versions:
   - React 18.3.1 with React DOM 18.3.1
   - TypeScript (via @types/node 20.10.0)
   - Vite 6.3.5 as build tool with @vitejs/plugin-react-swc 3.10.2
   - React Router DOM 6.30.1 for routing
   - TanStack React Query 5.89.0 for server state
   - Zustand 5.0.8 for client state
   - Firebase 10.13.0 for authentication
   - Radix UI component library (20+ components)
   - Tailwind CSS with class-variance-authority 0.7.1
   - Google Maps (@googlemaps/markerclusterer 2.6.2)
   - ReactFlow for workflow visualization
   - i18next 25.5.2 for internationalization
   - Recharts 2.15.2 for charts
   - Vitest 1.0.4 for testing

2. WHEN documenting project structure THEN the system SHALL describe:
   - /src/components/ - organized by feature (agents/, booking/, chat/, travel-planner/, trip-management/, workflow/, etc.)
   - /src/components/ui/ - Radix UI wrapper components (40+ reusable primitives)
   - /src/contexts/ - React contexts (AuthContext, UnifiedItineraryContext)
   - /src/hooks/ - custom hooks (useGenerationStatus, useNormalizedItinerary, useSseConnection, etc.)
   - /src/services/ - API clients and service layers (apiClient, sseManager, authService, etc.)
   - /src/state/ - Zustand store and React Query configuration
   - /src/types/ - TypeScript type definitions (TripData, NormalizedItinerary, ChatTypes, MapTypes)
   - /src/utils/ - utility functions (adapters, formatters, validators, error handlers)
   - /src/i18n/ - internationalization with locales for en, hi, bn, te
   - Lazy loading pattern for heavy components (TravelPlanner, WorkflowBuilder, etc.)

3. WHEN documenting data flow THEN the system SHALL map:
   - API Layer: apiClient.ts makes HTTP requests with retry logic and token management
   - Server State: React Query (TanStack Query) caches and manages server data
   - Client State: Zustand store (useAppStore) manages UI state and current trip
   - Context State: UnifiedItineraryContext provides itinerary-specific state
   - Component State: Local useState for component-specific UI state
   - Data transformation: itineraryAdapter.ts converts between TripData and NormalizedItinerary formats
   - Real-time updates: sseManager.ts handles SSE connections for live agent progress

4. WHEN documenting state management THEN the system SHALL describe:
   - Zustand store (state/store/useAppStore.ts) manages: currentScreen, isAuthenticated, currentTrip, trips array
   - React Query manages: itinerary fetching, caching, background refetching, optimistic updates
   - UnifiedItineraryContext manages: itinerary state, chat messages, workflow nodes/edges, agent data, revisions, UI state
   - AuthContext manages: Firebase auth state, user info, token management
   - Local component state for: form inputs, modal visibility, loading states, UI interactions

5. WHEN documenting real-time communication THEN the system SHALL describe:
   - SSE Manager (services/sseManager.ts) handles connection lifecycle
   - Agent progress stream for itinerary generation status
   - Patches stream for real-time itinerary updates
   - Automatic reconnection logic with exponential backoff
   - Event parsing and state updates
   - Token-based authentication for SSE
   - Connection cleanup on component unmount

6. WHEN documenting authentication THEN the system SHALL describe:
   - Firebase Authentication with Google Sign-In
   - AuthContext provides: user state, signIn, signOut, getIdToken methods
   - ProtectedRoute component guards authenticated routes
   - Token storage and automatic refresh in apiClient
   - Proactive token refresh (5 minutes before expiry)
   - 401 error handling with automatic retry after token refresh
   - Token passing to SSE connections via query parameters

### Requirement 3: Component Catalog with Detailed Inventory

**User Story:** As a UI designer, I want a detailed catalog of all components with their purposes and interactions, so that I can understand what functionality needs to be preserved in the redesign.

#### Acceptance Criteria

1. WHEN cataloging components THEN the system SHALL document exact file paths for all components in:
   - /src/components/ (root level pages: App.tsx, TravelPlanner.tsx, WorkflowBuilder.tsx, etc.)
   - /src/components/agents/ (13 components for agent progress and control)
   - /src/components/booking/ (8 components for booking flow)
   - /src/components/chat/ (3 components for chat interface)
   - /src/components/travel-planner/ (main planner with subdirectories: cards/, layout/, mobile/, modals/, shared/, views/)
   - /src/components/trip-management/ (4 components for trip dashboard and management)
   - /src/components/workflow/ (3 components for workflow visualization)
   - /src/components/ui/ (40+ Radix UI wrapper components)
   - /src/components/shared/ (14 reusable components like ErrorDisplay, LoadingState, GlobalHeader)
   - All other feature directories (controls/, debug/, dialogs/, diff/, export/, help/, history/, etc.)

2. WHEN describing components THEN the system SHALL include:
   - Component name and file path
   - Type classification: Page (route component), View (major UI section), Widget (feature component), Shared (reusable utility)
   - Purpose in 1-2 sentences
   - Whether it's lazy-loaded (TravelPlanner, WorkflowBuilder, SimplifiedAgentProgress, etc.)
   - Parent-child relationships

3. WHEN documenting component interfaces THEN the system SHALL extract and document:
   - Props interfaces from TypeScript definitions
   - Required vs optional props
   - Prop types and default values
   - Callback function signatures
   - Example: TravelPlanner props (itinerary, onSave, onBack, onShare, onExportPDF)
   - Example: SimplifiedAgentProgress props (tripData, onComplete, onCancel)

4. WHEN documenting state THEN the system SHALL describe:
   - Local state managed with useState (e.g., selectedDay, isModalOpen, formData)
   - Context consumed (AuthContext, UnifiedItineraryContext)
   - Zustand store usage (useAppStore for currentTrip, trips, authentication)
   - React Query hooks (useItinerary, useQuery, useMutation)
   - Custom hooks used (useGenerationStatus, useNormalizedItinerary, useSseConnection, etc.)

5. WHEN documenting backend dependencies THEN the system SHALL list:
   - Direct apiClient method calls
   - React Query hooks that fetch data
   - SSE connections established
   - Example: TravelPlanner uses getItinerary, proposeChanges, applyChanges, undoChanges
   - Example: SimplifiedAgentProgress uses createAgentEventStream SSE

6. WHEN documenting composition THEN the system SHALL list:
   - Child components rendered
   - Conditional rendering logic
   - Component slots and render props
   - Example: TravelPlanner renders DayByDayView, WorkflowView, TimelineView, TripMap
   - Example: DayByDayView renders ActivityCard, MealCard, AccommodationCard, TransportCard

7. WHEN documenting interactions THEN the system SHALL describe:
   - Button click handlers and their actions
   - Form submissions and validation
   - Drag-and-drop functionality (workflow nodes, timeline items)
   - Keyboard shortcuts (defined in KeyboardShortcuts component)
   - Modal triggers and dismissals
   - Navigation actions
   - Real-time updates and polling

8. WHEN documenting UI patterns THEN the system SHALL describe:
   - Layout approach (Tailwind utility classes, responsive design)
   - Component composition patterns (Radix UI primitives)
   - Loading states (SkeletonLoader, LoadingState, LoadingSpinner)
   - Error boundaries (GlobalErrorBoundary)
   - Modal patterns (Dialog, AlertDialog, Sheet from Radix UI)
   - Form patterns (react-hook-form integration)
   - Animation patterns (Tailwind transitions, Radix UI animations)
   - Responsive breakpoints and mobile-specific components

### Requirement 4: Feature Mapping and Grouping

**User Story:** As a product manager, I want components grouped by user-facing features, so that I can understand the complete functionality of each feature area.

#### Acceptance Criteria

1. WHEN grouping components THEN the system SHALL organize by these feature areas:
   - **Trip Creation/Generation**: SimplifiedTripWizard, SimplifiedAgentProgress, EnhancedGenerationProgress, GeneratingPlan
   - **Itinerary Viewing/Editing**: TravelPlanner, DayByDayView, WorkflowBuilder, TimelineView, NormalizedItineraryViewer
   - **Day-by-Day Planning**: DayByDayView with ActivityCard, MealCard, AccommodationCard, TransportCard, DayHeader, DayTimeline
   - **Workflow/Timeline View**: WorkflowBuilder, WorkflowNode, NodeInspectorModal, WorkflowUtils
   - **Map Integration**: TripMap, MarkerInfoWindow, ClusteringTest, TerrainControl, MapErrorBoundary
   - **Chat/AI Assistant**: NewChat, ChatMessage, DisambiguationPanel, ItineraryWithChat
   - **Agent Progress Tracking**: SimplifiedAgentProgress, EnhancedGenerationProgress, AgentProgressBar, AgentProgressModal, AgentExecutionProgress
   - **User Authentication**: LoginPage, GoogleSignIn, AuthContext, ProtectedRoute, UserProfile, UserProfileButton
   - **Trip Management**: TripDashboard, ItineraryOverview, EditMode, ShareView, TripViewLoader
   - **Booking System**: BookingModal, Checkout, BookingConfirmation, CostAndCart, HotelBookingSystem, BookingCancellation
   - **Sharing/Export**: ShareModal, EmailShareForm, ShareLinkPreview, PdfExportButton, ExportOptionsModal
   - **Revision History**: RevisionHistoryButton, RevisionTimeline, RevisionCard, RevisionDiffViewer, ChangeHistoryPanel
   - **Settings/Preferences**: PreviewSettingsModal, LanguageSelector, KeyboardShortcutsModal

2. WHEN documenting features THEN the system SHALL list:
   - Primary components for each feature
   - Supporting components and utilities
   - Shared components used across features
   - Feature-specific hooks and services
   - State management for each feature

3. WHEN mapping workflows THEN the system SHALL describe user journeys:
   - **Trip Creation Flow**: Landing → Wizard (destination, dates, preferences) → Agent Progress → Planner
   - **Itinerary Editing Flow**: Dashboard → Select Trip → Planner → Edit (add/remove/move nodes) → Save
   - **Chat Interaction Flow**: Planner → Chat Panel → Send Request → Agent Processing → Apply Changes
   - **Booking Flow**: Planner → Select Node → Booking Modal → Checkout → Payment → Confirmation
   - **Sharing Flow**: Planner → Share Button → Share Modal → Generate Link/Email → Share View
   - **Revision Flow**: Planner → History Button → Revision Timeline → Select Revision → Compare/Rollback

4. IF features have dependencies THEN the system SHALL document:
   - Map integration depends on itinerary data with coordinates
   - Chat depends on selected nodes and current itinerary state
   - Booking depends on node details and pricing information
   - Agent progress depends on SSE connection and itinerary ID
   - Workflow view depends on node positions and edge definitions
   - Revision history depends on change tracking and version management

### Requirement 5: Data Models and Type Definitions

**User Story:** As a frontend developer, I want complete documentation of all TypeScript interfaces and types, so that I can understand the data structures used throughout the application.

#### Acceptance Criteria

1. WHEN documenting types THEN the system SHALL list all TypeScript interfaces and types used in the frontend
2. WHEN mapping data models THEN the system SHALL show how backend data models map to frontend representations
3. WHEN documenting transformations THEN the system SHALL identify data transformation layers and their purposes
4. IF adapters exist THEN the system SHALL document any adapters or converters between data formats

### Requirement 6: Shared Utilities and Services Documentation

**User Story:** As a developer, I want documentation of all shared utilities and services, so that I can understand reusable code that should be preserved in the redesign.

#### Acceptance Criteria

1. WHEN documenting services THEN the system SHALL list all API client services with their methods
2. WHEN documenting utilities THEN the system SHALL describe utility functions and their purposes
3. WHEN documenting hooks THEN the system SHALL list all custom hooks with their inputs and outputs
4. WHEN documenting helpers THEN the system SHALL describe helper functions and their use cases
5. WHEN documenting configuration THEN the system SHALL list constants and configuration values

### Requirement 7: Current UI/UX Pattern Documentation

**User Story:** As a UX designer, I want documentation of all current UI/UX patterns, so that I can understand the existing user experience before proposing improvements.

#### Acceptance Criteria

1. WHEN documenting navigation THEN the system SHALL describe the navigation structure including routes, menus, and breadcrumbs
2. WHEN documenting layouts THEN the system SHALL describe layout patterns including sidebars, modals, and panels
3. WHEN documenting loading states THEN the system SHALL describe loading states and skeleton implementations
4. WHEN documenting error handling THEN the system SHALL describe error handling and display patterns
5. WHEN documenting forms THEN the system SHALL describe form patterns and validation approaches
6. WHEN documenting interactions THEN the system SHALL describe interactive elements like drag-drop and inline editing
7. WHEN documenting responsive design THEN the system SHALL list responsive design breakpoints and mobile patterns

### Requirement 8: Third-Party Integration Documentation

**User Story:** As a technical lead, I want documentation of all third-party integrations, so that I can ensure the redesign maintains compatibility with external services.

#### Acceptance Criteria

1. WHEN documenting Google Maps THEN the system SHALL describe usage patterns and components that use Google Maps
2. WHEN documenting Firebase THEN the system SHALL describe authentication and storage usage
3. IF other services exist THEN the system SHALL document any other external service integrations

### Requirement 9: Assets and Styling Documentation

**User Story:** As a designer, I want documentation of the current styling approach and assets, so that I can understand the visual design system.

#### Acceptance Criteria

1. WHEN documenting styling THEN the system SHALL describe the CSS/styling approach (Tailwind, CSS modules, etc.)
2. WHEN documenting icons THEN the system SHALL list icon libraries used
3. WHEN documenting images THEN the system SHALL catalog image assets
4. IF design tokens exist THEN the system SHALL document theme and design tokens

### Requirement 10: Constraints and Requirements for Redesign

**User Story:** As a project manager, I want clear constraints and requirements documented, so that the redesign team understands what must be preserved.

#### Acceptance Criteria

1. WHEN defining constraints THEN the system SHALL specify that all existing functionality must be preserved
2. WHEN defining compatibility THEN the system SHALL specify that backend API compatibility must be maintained
3. WHEN defining workflows THEN the system SHALL specify that the same user workflows must be supported
4. WHEN defining data structures THEN the system SHALL specify that the same data structures must be handled
5. IF performance requirements exist THEN the system SHALL document performance requirements
6. IF browser compatibility requirements exist THEN the system SHALL document browser compatibility requirements

### Requirement 11: Documentation Format and Organization

**User Story:** As a documentation consumer, I want the specification organized hierarchically with clear sections, so that I can easily find the information I need.

#### Acceptance Criteria

1. WHEN creating the document THEN the system SHALL output a single comprehensive markdown document
2. WHEN organizing content THEN the system SHALL use hierarchical organization with clear sections
3. WHEN providing examples THEN the system SHALL include code snippets for complex interfaces
4. WHEN presenting data THEN the system SHALL use tables for component inventories
5. WHEN referencing files THEN the system SHALL include file path references for everything
6. WHEN linking content THEN the system SHALL add cross-references between related components

### Requirement 12: Accuracy and Verification

**User Story:** As a technical reviewer, I want all documented information to be accurate and verified, so that the redesign team can trust the documentation.

#### Acceptance Criteria

1. WHEN documenting file paths THEN the system SHALL verify all file paths exist in the codebase
2. WHEN documenting API endpoints THEN the system SHALL confirm all API endpoints by checking backend controllers or API documentation
3. WHEN documenting relationships THEN the system SHALL validate component relationships by reading actual imports
4. WHEN documenting features THEN the system SHALL document only what currently exists without assumptions
5. WHEN documenting dependencies THEN the system SHALL include version numbers for all dependencies
   - Shared components used across features
   - Feature-specific hooks and services
   - State management for each feature

3. WHEN mapping workflows THEN the system SHALL describe user journeys:
   - **Trip Creation Flow**: Landing → Wizard (destination, dates, preferences) → Agent Progress → Planner
   - **Itinerary Editing Flow**: Dashboard → Select Trip → Planner → Edit (add/remove/move nodes) → Save
   - **Chat Interaction Flow**: Planner → Chat Panel → Send Request → Agent Processing → Apply Changes
   - **Booking Flow**: Planner → Select Node → Booking Modal → Checkout → Payment → Confirmation
   - **Sharing Flow**: Planner → Share Button → Share Modal → Generate Link/Email → Share View
   - **Revision Flow**: Planner → History Button → Revision Timeline → Select Revision → Compare/Rollback

4. IF features have dependencies THEN the system SHALL document:
   - Map integration depends on itinerary data with coordinates
   - Chat depends on selected nodes and current itinerary state
   - Booking depends on node details and pricing information
   - Agent progress depends on SSE connection and itinerary ID
   - Workflow view depends on node positions and edge definitions
   - Revision history depends on change tracking and version management

### Requirement 5: Data Models & Type Definitions

**User Story:** As a frontend developer, I want complete documentation of all TypeScript interfaces and types, so that I can understand the data structures used throughout the application.

#### Acceptance Criteria

1. WHEN documenting types THEN the system SHALL list all TypeScript interfaces from:
   - **TripData.ts** (legacy format): TripData, TripLocation, TravelPreferences, TripSettings, Traveler, TripComponent, DayPlan, TripItinerary, PopularDestination, AgentTask, FlightOption, HotelOption, RestaurantOption, PlaceOption, TransportOption
   - **NormalizedItinerary.ts** (new format): NormalizedItinerary, NormalizedDay, NormalizedNode, NodeLocation, NodeTiming, NodeCost, NodeDetails, Edge, TransitInfo, Pacing, TimeWindow, DayTotals, ItinerarySettings, AgentStatus, ChangeSet, ChangeOperation, ItineraryDiff, PatchEvent
   - **ChatTypes.ts**: ChatMessage, ChatSession, DisambiguationOption
   - **MapTypes.ts**: MapBounds, Coordinates, MarkerData, ClusterData
   - **UnifiedItineraryTypes.ts**: UnifiedItineraryState, UnifiedItineraryAction, ChatMessage, WorkflowNode, WorkflowEdge, WorkflowSettings, AgentDataSection, ChangeDetail, RevisionInfo
   - **apiClient.ts**: All request/response DTOs (CreateItineraryRequest, ItineraryResponse, ProposeResponse, ApplyRequest, etc.)

2. WHEN mapping data models THEN the system SHALL show:
   - Backend ItineraryDto → Frontend NormalizedItinerary (direct mapping)
   - Backend ItineraryDto → Frontend TripData (via itineraryAdapter.ts)
   - NormalizedItinerary ↔ TripData (bidirectional via adapters)
   - Backend agent events → Frontend AgentStatus updates
   - Backend patch events → Frontend itinerary updates
   - Backend node data → Frontend WorkflowNode data

3. WHEN documenting transformations THEN the system SHALL identify:
   - **itineraryAdapter.ts**: Converts NormalizedItinerary to TripData format
   - **normalizedToTripDataAdapter.ts**: Converts between normalized and legacy formats
   - **placeToWorkflowNode.ts**: Converts place data to workflow node format
   - **addPlaceToItinerary.ts**: Transforms place search results to itinerary nodes
   - **typeGuards.ts**: Runtime type checking functions (isNormalizedItinerary, isTripData, etc.)
   - Data normalization in React Query hooks
   - Date/time formatting in formatters.ts
   - Currency and number formatting utilities

4. IF adapters exist THEN the system SHALL document:
   - Purpose: Why the adapter exists (legacy compatibility, format conversion)
   - Input/output types
   - Transformation logic and field mappings
   - Edge cases and null handling
   - Performance considerations
   - Usage locations in codebase
   - Example: itineraryAdapter converts NormalizedItinerary.days[] to TripData.itinerary.days[] with full DayPlan structure

### Requirement 6: Shared Utilities and Services Documentation

**User Story:** As a developer, I want documentation of all shared utilities and services, so that I can understand reusable code that should be preserved in the redesign.

#### Acceptance Criteria

1. WHEN documenting services THEN the system SHALL list all services in /src/services/:
   - **apiClient.ts**: Main API client with retry logic, token management, all endpoint methods
   - **sseManager.ts**: SSE connection management with reconnection logic
   - **authService.ts**: Firebase authentication wrapper
   - **chatService.ts**: Chat message handling and storage
   - **chatStorageService.ts**: Local storage for chat history
   - **agentService.ts**: Agent execution and status tracking
   - **geocodingService.ts**: Google Maps geocoding integration
   - **weatherService.ts**: Weather data fetching
   - **workflowSyncService.ts**: Workflow position synchronization
   - **userChangeTracker.ts**: User change tracking for analytics
   - **firebaseService.ts**: Firebase initialization and configuration

2. WHEN documenting utilities THEN the system SHALL describe functions in /src/utils/:
   - **errorHandler.ts**: Centralized error handling and logging
   - **errorMessages.ts**: User-friendly error message mapping
   - **formatters.ts**: Date, time, currency, number formatting
   - **validators.ts**: Form and data validation functions
   - **logger.ts**: Structured logging utility
   - **analytics.ts**: Analytics event tracking
   - **cache.ts**: Client-side caching utilities
   - **diffUtils.ts**: Itinerary diff calculation
   - **encodingUtils.ts**: URL encoding/decoding
   - **googleMapsLoader.ts**: Google Maps API loader
   - **mapUtils.ts**: Map-related utility functions
   - **itineraryUtils.ts**: Itinerary manipulation helpers
   - **mobileTesting.ts**: Mobile device testing utilities

3. WHEN documenting hooks THEN the system SHALL list all custom hooks in /src/hooks/:
   - **useGenerationStatus.ts**: Tracks itinerary generation progress
   - **useNormalizedItinerary.ts**: Manages normalized itinerary state
   - **useSseConnection.ts**: SSE connection lifecycle management
   - **useGoogleMaps.ts**: Google Maps API integration
   - **useMapState.ts**: Map state management
   - **useChatHistory.ts**: Chat history management
   - **useChangePreview.ts**: Change preview functionality
   - **useDebounce.ts**: Debounced value updates
   - **useLocalStorage.ts**: Local storage state management
   - **useKeyboardShortcut.ts**: Keyboard shortcut registration
   - **useFormSubmission.ts**: Form submission handling
   - **useAutoRefresh.ts**: Automatic data refresh
   - **useSmoothProgress.ts**: Smooth progress bar animations
   - **useDeviceDetection.ts**: Device type detection
   - **useScrollDetection.ts**: Scroll position tracking
   - **useMobileScroll.ts**: Mobile scroll behavior
   - **useSwipeGesture.ts**: Swipe gesture detection
   - **useVirtualScroll.ts**: Virtual scrolling for large lists
   - **useLazyLoad.ts**: Lazy loading for images/components
   - **useWorkflowSync.ts**: Workflow synchronization

4. WHEN documenting helper functions THEN the system SHALL describe:
   - Component-specific helpers (TravelPlannerHelpers.ts, WorkflowBuilderHelpers.ts)
   - State management helpers (TravelPlannerState.ts, WorkflowBuilderState.ts)
   - Hook collections (TravelPlannerHooks.ts, WorkflowBuilderHooks.ts)
   - Purpose and usage of each helper module
   - Dependencies between helpers

5. WHEN documenting configuration THEN the system SHALL list:
   - **firebase.ts**: Firebase configuration and initialization
   - **weatherConfig.ts**: Weather API configuration
   - **state/query/client.ts**: React Query client configuration
   - Environment variables from .env.local (VITE_API_BASE_URL, VITE_GOOGLE_MAPS_API_KEY, etc.)
   - Constants in data/destinations.ts

### Requirement 7: Current UI/UX Pattern Documentation

**User Story:** As a UX designer, I want documentation of all current UI/UX patterns, so that I can understand the existing user experience before proposing improvements.

#### Acceptance Criteria

1. WHEN documenting navigation THEN the system SHALL describe:
   - **Route structure** in App.tsx: /, /login, /wizard, /generating, /planner, /cost, /checkout, /confirmation, /share, /dashboard, /trip/:id, /itinerary/:id, /itinerary/:id/chat
   - **Navigation components**: GlobalNavigation, GlobalHeader, BreadcrumbNavigation
   - **Protected routes**: ProtectedRoute wrapper for authenticated routes
   - **Route guards**: RequireTrip wrapper for routes requiring current trip
   - **Navigation patterns**: useNavigate hook, programmatic navigation, back buttons
   - **Deep linking**: Support for direct itinerary access via ID

2. WHEN documenting layouts THEN the system SHALL describe:
   - **AppLayout**: Main application layout wrapper
   - **Sidebar patterns**: Collapsible sidebars in TravelPlanner, WorkflowBuilder
   - **Modal patterns**: Dialog, AlertDialog, Sheet from Radix UI
   - **Panel patterns**: Resizable panels (react-resizable-panels)
   - **Split views**: Day-by-day view with map, workflow with inspector
   - **Responsive layouts**: Mobile-specific components in travel-planner/mobile/
   - **Grid layouts**: Tailwind grid for card layouts
   - **Flex layouts**: Tailwind flex for component arrangement

3. WHEN documenting loading states THEN the system SHALL describe:
   - **SkeletonLoader**: Animated skeleton screens for content loading
   - **LoadingState**: Centralized loading component with variants (fullPage, inline, spinner)
   - **LoadingSpinner**: Simple spinner component
   - **Suspense boundaries**: Lazy-loaded component loading states
   - **Progress bars**: Linear progress for agent execution
   - **Smooth progress**: useSmoothProgress hook for animated progress
   - **Loading text**: Contextual loading messages

4. WHEN documenting error handling THEN the system SHALL describe:
   - **GlobalErrorBoundary**: Top-level error boundary
   - **ErrorDisplay**: Reusable error display component
   - **AgentErrorDisplay**: Agent-specific error display
   - **BookingErrorDisplay**: Booking-specific error display
   - **Error toast notifications**: Sonner toast library integration
   - **Error recovery**: Retry buttons, fallback UI
   - **Error logging**: Structured error logging with logger.ts

5. WHEN documenting forms THEN the system SHALL describe:
   - **react-hook-form integration**: Form state management and validation
   - **Form components**: Input, Textarea, Select, Checkbox, RadioGroup from ui/
   - **Validation patterns**: Zod schema validation (if used)
   - **Form submission**: useFormSubmission hook
   - **Error display**: Inline field errors, form-level errors
   - **Wizard pattern**: Multi-step form in SimplifiedTripWizard
   - **Auto-save**: Debounced auto-save in editors

6. WHEN documenting interactions THEN the system SHALL describe:
   - **Drag-and-drop**: ReactFlow for workflow nodes, timeline item reordering
   - **Inline editing**: Click-to-edit patterns in itinerary
   - **Context menus**: Right-click context menus (Radix UI ContextMenu)
   - **Tooltips**: Hover tooltips (Radix UI Tooltip)
   - **Popovers**: Click popovers for additional info (Radix UI Popover)
   - **Keyboard shortcuts**: Global shortcuts via KeyboardShortcuts component
   - **Swipe gestures**: Mobile swipe detection with useSwipeGesture
   - **Scroll interactions**: Infinite scroll, scroll-to-top, sticky headers

7. WHEN documenting responsive design THEN the system SHALL list:
   - **Breakpoints**: Tailwind default breakpoints (sm: 640px, md: 768px, lg: 1024px, xl: 1280px, 2xl: 1536px)
   - **Mobile components**: Separate mobile views in travel-planner/mobile/
   - **Responsive utilities**: use-mobile.ts hook for mobile detection
   - **Touch interactions**: Touch-optimized buttons and gestures
   - **Mobile navigation**: Bottom navigation, hamburger menus
   - **Viewport handling**: Mobile scroll behavior, viewport units

### Requirement 8: Third-Party Integration Documentation

**User Story:** As a technical lead, I want documentation of all third-party integrations, so that I can ensure the redesign maintains compatibility with external services.

#### Acceptance Criteria

1. WHEN documenting Google Maps THEN the system SHALL describe:
   - **API Key**: Configured via VITE_GOOGLE_MAPS_API_KEY environment variable
   - **Loader**: googleMapsLoader.ts for async API loading
   - **Components using Maps**: TripMap, MarkerInfoWindow, ClusteringTest
   - **Features used**: Markers, clustering (@googlemaps/markerclusterer), info windows, terrain control
   - **Geocoding**: geocodingService.ts for address/coordinate conversion
   - **Map state**: useMapState hook for map interaction state
   - **Bounds calculation**: Automatic bounds fitting for itinerary locations
   - **Custom markers**: Custom marker icons and clustering

2. WHEN documenting Firebase THEN the system SHALL describe:
   - **Version**: Firebase 10.13.0
   - **Configuration**: firebase.ts with project credentials
   - **Authentication**: Google Sign-In via GoogleSignIn component
   - **Auth context**: AuthContext for auth state management
   - **Token management**: ID token retrieval and refresh
   - **User profile**: User info storage and retrieval
   - **Security**: Token-based API authentication
   - **Sign-out**: Clean session termination

3. IF other services exist THEN the system SHALL document:
   - **Razorpay**: Payment gateway integration for bookings
   - **i18next**: Internationalization with language detection
   - **Recharts**: Chart library for data visualization
   - **ReactFlow**: Workflow visualization library
   - **Radix UI**: Headless UI component library
   - **Sonner**: Toast notification library
   - **date-fns**: Date manipulation library
   - **Embla Carousel**: Carousel component library

### Requirement 9: Assets and Styling Documentation

**User Story:** As a designer, I want documentation of the current styling approach and assets, so that I can understand the visual design system.

#### Acceptance Criteria

1. WHEN documenting styling THEN the system SHALL describe:
   - **Tailwind CSS**: Utility-first CSS framework
   - **Configuration**: tailwind.config.js with custom theme
   - **Global styles**: index.css and styles/globals.css
   - **CSS modules**: Component-specific CSS files (ChangePreview.css, ChatInterface.css, etc.)
   - **Class variance authority**: CVA for component variants
   - **Tailwind merge**: clsx and tailwind-merge for class composition
   - **Dark mode**: next-themes for theme switching
   - **Custom utilities**: Custom Tailwind utilities and plugins

2. WHEN documenting icons THEN the system SHALL list:
   - **Lucide React**: Primary icon library (lucide-react 0.487.0)
   - **Icon usage**: Consistent icon usage across components
   - **Custom icons**: Any custom SVG icons
   - **Icon sizing**: Standard icon sizes (sm, md, lg)

3. WHEN documenting images THEN the system SHALL catalog:
   - **Image components**: ImageWithFallback, ResponsiveImage
   - **Lazy loading**: useLazyLoad hook for images
   - **Optimization**: Image optimization strategies
   - **Fallbacks**: Placeholder images and error states
   - **Asset locations**: Public assets directory structure

4. IF design tokens exist THEN the system SHALL document:
   - **Color palette**: Tailwind color configuration
   - **Typography**: Font families, sizes, weights
   - **Spacing**: Spacing scale and usage
   - **Border radius**: Rounding scale
   - **Shadows**: Shadow definitions
   - **Transitions**: Animation timing and easing
   - **Z-index**: Layering system

### Requirement 10: Constraints and Requirements for Redesign

**User Story:** As a project manager, I want clear constraints and requirements documented, so that the redesign team understands what must be preserved.

#### Acceptance Criteria

1. WHEN defining functional constraints THEN the system SHALL specify:
   - All existing features must be preserved (trip creation, editing, booking, sharing, etc.)
   - All user workflows must remain functional
   - All data operations must work identically (CRUD, real-time updates)
   - All integrations must continue working (Google Maps, Firebase, payment gateway)
   - All accessibility features must be maintained or improved

2. WHEN defining API compatibility THEN the system SHALL specify:
   - All backend API endpoints must remain compatible
   - Request/response formats must not change
   - Authentication mechanism must remain the same
   - SSE connections must continue working
   - Error handling must be preserved

3. WHEN defining data structure constraints THEN the system SHALL specify:
   - Support for both TripData and NormalizedItinerary formats
   - Data adapters must continue functioning
   - Local storage formats must remain compatible
   - State management structure can be refactored but must maintain same capabilities

4. WHEN defining performance requirements THEN the system SHALL specify:
   - Initial load time should not increase
   - Lazy loading must be maintained for heavy components
   - React Query caching must be preserved
   - SSE connection efficiency must be maintained
   - Map rendering performance must not degrade

5. WHEN defining browser compatibility THEN the system SHALL specify:
   - Modern browsers (Chrome, Firefox, Safari, Edge - last 2 versions)
   - Mobile browsers (iOS Safari, Chrome Mobile)
   - Progressive enhancement for older browsers
   - Responsive design for all screen sizes

6. WHEN defining testing requirements THEN the system SHALL specify:
   - Existing test coverage must be maintained
   - Vitest test framework must continue to be used
   - Component tests must be updated for new UI
   - Integration tests must pass
   - E2E test scenarios must remain valid

### Requirement 11: Documentation Format and Organization

**User Story:** As a documentation consumer, I want the specification organized hierarchically with clear sections, so that I can easily find the information I need.

#### Acceptance Criteria

1. WHEN creating the document THEN the system SHALL:
   - Output a single comprehensive markdown file named FRONTEND_UI_REDESIGN_SPECIFICATION.md
   - Use hierarchical heading structure (H1 for main sections, H2 for subsections, etc.)
   - Include a table of contents with links to all major sections
   - Use consistent formatting throughout

2. WHEN organizing content THEN the system SHALL:
   - Group related information together
   - Use clear section headings
   - Provide context before details
   - Include summary sections for complex topics

3. WHEN providing examples THEN the system SHALL:
   - Include TypeScript code snippets for complex interfaces
   - Show example API requests/responses
   - Demonstrate component usage patterns
   - Provide before/after examples for transformations

4. WHEN presenting data THEN the system SHALL:
   - Use markdown tables for component inventories
   - Use bullet lists for feature lists
   - Use numbered lists for sequential processes
   - Use code blocks for code examples

5. WHEN referencing files THEN the system SHALL:
   - Include full file paths relative to project root
   - Use consistent path format (e.g., frontend/src/components/...)
   - Link related files together
   - Group files by feature or purpose

6. WHEN linking content THEN the system SHALL:
   - Add cross-references between related sections
   - Link components to their dependencies
   - Link types to their usage locations
   - Create a comprehensive index

### Requirement 12: Accuracy and Verification

**User Story:** As a technical reviewer, I want all documented information to be accurate and verified, so that the redesign team can trust the documentation.

#### Acceptance Criteria

1. WHEN documenting file paths THEN the system SHALL:
   - Verify all file paths exist in the codebase
   - Use exact file names (case-sensitive)
   - Include file extensions
   - Note any deprecated or moved files

2. WHEN documenting API endpoints THEN the system SHALL:
   - Verify endpoints against swagger-api-documentation.yaml
   - Confirm endpoints are actually used in apiClient.ts
   - Document actual request/response formats from code
   - Note any discrepancies between docs and implementation

3. WHEN documenting component relationships THEN the system SHALL:
   - Verify imports by reading actual import statements
   - Confirm parent-child relationships from JSX
   - Validate prop interfaces from TypeScript definitions
   - Check actual usage in codebase

4. WHEN documenting features THEN the system SHALL:
   - Document only implemented features
   - Note incomplete or work-in-progress features
   - Avoid assumptions about functionality
   - Verify behavior through code inspection

5. WHEN documenting dependencies THEN the system SHALL:
   - Use exact version numbers from package.json
   - Note peer dependencies
   - Document dev vs production dependencies
   - Identify unused dependencies

6. WHEN documenting state management THEN the system SHALL:
   - Verify state structure from actual code
   - Document actual state flow patterns
   - Confirm hook usage and dependencies
   - Validate context provider hierarchy
