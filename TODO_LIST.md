# TODO List - Agentic Itinerary Planner

This document contains all TODO items found in the repository, organized by category and implementation status.

## Frontend TODOs

### TravelPlanner Component
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Implement transport update logic | `frontend/src/components/TravelPlanner.tsx:286` | ❌ Not Implemented | Medium | Transport update logic for day-by-day view |
| Add workflow node to workflow builder | `frontend/src/components/TravelPlanner.tsx:430` | ⚠️ Partial | High | Integration between map place selection and workflow builder |
| Implement save functionality | `frontend/src/components/TravelPlanner.tsx:465` | ❌ Not Implemented | High | Save workflow changes to backend |
| Persist via backend mutation | `frontend/src/components/TravelPlanner.tsx:435` | ❌ Not Implemented | High | Backend API integration for place additions |
| Implement save functionality for desktop version | `frontend/src/components/TravelPlanner.tsx:669` | ❌ Not Implemented | Medium | Desktop version doesn't save changes, keep same for simplicity |
| Implement transport update logic (line 413) | `frontend/src/components/TravelPlanner.tsx:413` | ❌ Not Implemented | Medium | Transport update logic for day-by-day view |
| Add workflow node to workflow builder (line 603) | `frontend/src/components/TravelPlanner.tsx:603` | ❌ Not Implemented | High | Integration between map place selection and workflow builder |
| Implement save functionality (line 637) | `frontend/src/components/TravelPlanner.tsx:637` | ❌ Not Implemented | High | Save workflow changes to backend |
| Implement transport update logic (line 817) | `frontend/src/components/TravelPlanner.tsx:817` | ❌ Not Implemented | Medium | Transport update logic for day-by-day view |

### Firebase Configuration
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Add SDKs for Firebase products | `frontend/src/config/firebase.ts:4` | ❌ Not Implemented | Low | Firebase SDK configuration |

### Trip Wizard Component
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Fix travelers array length calculation | `frontend/src/components/trip-wizard/SimplifiedTripWizard.tsx:290` | ❌ Not Implemented | Medium | Should use length of travelers array instead of hardcoded 1 |
| Implement budget tier UI component | `frontend/src/components/trip-wizard/SimplifiedTripWizard.tsx:295` | ❌ Not Implemented | Medium | Take budget tier input from UI component instead of hardcoded logic |
| Centralize status enums/types | `frontend/src/components/trip-wizard/SimplifiedTripWizard.tsx:356` | ❌ Not Implemented | Medium | Make status centralized using enums or types across all places |

### Trip Overview Component
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Calculate average cost from real data | `frontend/src/components/travel-planner/views/TripOverviewView.tsx:137` | ❌ Not Implemented | Medium | Replace hardcoded cost calculation with real data |

### Agent Progress Modal
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Keep consistent status names | `frontend/src/components/agents/AgentProgressModal.tsx:141` | ❌ Not Implemented | Medium | Manage consistent status names across the repo using types |

### Destinations Manager
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Find max nights from user request | `frontend/src/components/travel-planner/views/DestinationsManager.tsx:27` | ❌ Not Implemented | Medium | Get max nights info from user request or itinerary data |

### Mobile Map Detail View
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Implement save functionality | `frontend/src/components/travel-planner/mobile/MobileMapDetailView.tsx:112` | ❌ Not Implemented | High | Save functionality for mobile map detail view |

### Global Error Boundary
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Send to error reporting service | `frontend/src/components/shared/GlobalErrorBoundary.tsx:30` | ❌ Not Implemented | Medium | Integrate with error reporting service (e.g., Sentry, LogRocket) |

## Backend TODOs

### Booking Service
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Parse JSON to Map for booking data | `src/main/java/com/tripplanner/service/BookingService.java:152` | ❌ Not Implemented | Medium | JSON parsing for booking metadata |
| Parse JSON to Map for booking data | `src/main/java/com/tripplanner/service/BookingService.java:178` | ❌ Not Implemented | Medium | JSON parsing for booking metadata |
| Call provider cancellation API | `src/main/java/com/tripplanner/service/BookingService.java:205` | ❌ Not Implemented | Medium | Integration with booking provider APIs |

### Razorpay Service
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Mock for Trigger provider booking process | `src/main/java/com/tripplanner/service/RazorpayService.java:197` | ❌ Not Implemented | High | Complete booking flow integration |

### Tools Service
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Implement packing list generation with LLM | `src/main/java/com/tripplanner/service/ToolsService.java:22` | ❌ Not Implemented | Medium | AI-powered packing list generation |
| Implement photo spots retrieval with LLM | `src/main/java/com/tripplanner/service/ToolsService.java:33` | ❌ Not Implemented | Medium | AI-powered photo spot recommendations |
| Implement must-try foods retrieval with LLM | `src/main/java/com/tripplanner/service/ToolsService.java:44` | ❌ Not Implemented | Medium | AI-powered food recommendations |
| Implement cost estimation with LLM | `src/main/java/com/tripplanner/service/ToolsService.java:55` | ❌ Not Implemented | Medium | AI-powered cost estimation |

### Planner Agent
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Check accommodation/transportation/meals setting | `src/main/java/com/tripplanner/agents/PlannerAgent.java:686` | ❌ Not Implemented | Medium | Check if accommodation, transportation, meals can be set and significance |
| Refactor to use real data | `src/main/java/com/tripplanner/agents/PlannerAgent.java:725` | ❌ Not Implemented | Medium | Refactor to use real data instead of hardcoded values |

## Implementation Notes


### Technical Notes
| Note | File | Status | Priority | Description |
|------|------|--------|----------|-------------|
| Avoid augmenting google.maps types | `frontend/src/types/MapTypes.ts:111` | ✅ Implemented | Low | TypeScript type safety consideration |
| Edge doesn't have mode/duration/transit methods | `src/main/java/com/tripplanner/agents/AgentOrchestrator.java:422` | ⚠️ Partial | Medium | Graph edge implementation limitation |
| Simplified approach in EnrichmentAgent | `src/main/java/com/tripplanner/agents/EnrichmentAgent.java:302` | ⚠️ Partial | Medium | Enrichment algorithm needs enhancement |
| Booking async processing note | `frontend/src/__tests__/e2e.test.ts:163` | ✅ Implemented | Low | Test documentation note |

## Roadmap Items

### Google Maps Integration
| Item | File | Status | Priority | Description |
|------|------|--------|----------|-------------|
| Phase 2 - Location/Places Agent | `googlemaps-roadmap.md:514` | ❌ Not Implemented | Low | Future scope for advanced location features |

## Validation Results

### Frontend TODOs - Validation Status
| TODO Item | Validation Status | Details |
|-----------|------------------|---------|
| Implement transport update logic | ❌ **Not Implemented** | Only console.log placeholder exists |
| Add workflow node to workflow builder | ⚠️ **Partial** | Node creation works, but integration with workflow builder state is missing |
| Implement save functionality | ❌ **Not Implemented** | Only console.log placeholder exists |
| Persist via backend mutation | ❌ **Not Implemented** | Only console.log placeholder exists |

### Backend TODOs - Validation Status
| TODO Item | Validation Status | Details |
|-----------|------------------|---------|
| Parse JSON to Map for booking data (2 instances) | ❌ **Not Implemented** | Returns null, no JSON parsing logic |
| Call provider cancellation API | ❌ **Not Implemented** | Only comment exists |
| Trigger provider booking process | ❌ **Not Implemented** | Only comment exists |
| Implement packing list generation with LLM | ❌ **Not Implemented** | Throws UnsupportedOperationException |
| Implement photo spots retrieval with LLM | ❌ **Not Implemented** | Throws UnsupportedOperationException |
| Implement must-try foods retrieval with LLM | ❌ **Not Implemented** | Throws UnsupportedOperationException |
| Implement cost estimation with LLM | ❌ **Not Implemented** | Throws UnsupportedOperationException |

### Authentication & User Management - Validation Status
| Note | Validation Status | Details |
|------|------------------|---------|
| Hardcoded anonymous user | ⚠️ **Partial** | Uses "anonymous" user, no real auth system |
| Ownership check removed (3 instances) | ⚠️ **Partial** | Comments indicate checks were removed for anonymous users |

## Summary

### Status Breakdown
- **❌ Not Implemented**: 12 items (100% validated as not implemented)
- **⚠️ Partial**: 6 items (100% validated as partial implementation)
- **✅ Implemented**: 2 items (100% validated as implemented)

### Priority Breakdown
- **High Priority**: 8 items
- **Medium Priority**: 8 items
- **Low Priority**: 4 items

### Categories
- **Frontend**: 5 items
- **Backend**: 8 items
- **Authentication**: 4 items
- **Technical Notes**: 4 items
- **Roadmap**: 1 item

### Implementation Quality
- **Complete Implementation**: 2 items (10%)
- **Partial Implementation**: 6 items (30%)
- **No Implementation**: 12 items (60%)

## New TODO Items (User Requested)

### Mobile & UI Framework
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Make all components mobile compatible | `frontend/src/components/**/*.tsx` | ❌ Not Implemented | High | Current UI uses Tailwind CSS but lacks mobile responsiveness |
| Consider better UI framework | `frontend/src/components/**/*.tsx` | ⚠️ Partial | Medium | Currently using Tailwind CSS + shadcn/ui, could consider Material-UI or Chakra UI |

### Navigation & Layout
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Make menu pane collapsible to side | `frontend/src/components/travel-planner/layout/NavigationSidebar.tsx` | ❌ Not Implemented | Medium | Fixed width sidebar (w-64), no collapse functionality |

### Hotel Booking Integration
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Remove discover button in destinations | `frontend/src/components/travel-planner/views/DestinationsManager.tsx:128-135` | ❌ Not Implemented | Medium | Discover button exists and functional |
| Add hotel booking on sleeping button click | `frontend/src/components/travel-planner/views/DestinationsManager.tsx:120-127` | ❌ Not Implemented | High | Sleeping button exists but no booking integration |
| Create new Bookings menu item | `frontend/src/components/travel-planner/layout/NavigationSidebar.tsx` | ❌ Not Implemented | High | No bookings section in navigation |
| Implement hotel search functionality | New component needed | ❌ Not Implemented | High | Hotels agent integration required |

### Destinations Optimization
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Adapt destinations to not show multiple cards for same city | `frontend/src/components/travel-planner/views/DestinationsManager.tsx` | ❌ Not Implemented | Medium | Currently shows all destinations as separate cards |

### Workflow Enhancements
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Make workflow auto reset generic for any number of places | `frontend/src/components/WorkflowBuilder.tsx` | ❌ Not Implemented | Medium | No auto reset functionality found |
| Add node edge deletion functionality | `frontend/src/components/WorkflowBuilder.tsx` | ❌ Not Implemented | Medium | Workflow builder exists but edge deletion not implemented |

### Chat & Data Persistence
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Store chat with AI assistant in Firebase | `frontend/src/components/ChatInterface.tsx` | ❌ Not Implemented | High | Chat interface exists but no Firebase storage |

### Versioning System
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Robust versioning of itinerary versions | `src/main/java/com/tripplanner/dto/NormalizedItinerary.java:23-24` | ⚠️ Partial | High | Version field exists but no commit message system |
| Display version details with commit messages | `frontend/src/components/**/*.tsx` | ❌ Not Implemented | Medium | No UI for version history display |

### User Authentication & Firebase Storage
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Implement user authentication system | `frontend/src/services/authService.ts` | ✅ **COMPLETED** | High | Google Sign-in authentication fully implemented |
| Firebase user authentication integration | `frontend/src/config/firebase.ts` | ✅ **COMPLETED** | High | Firebase Auth with Google provider configured |
| User-specific itinerary storage structure | `src/main/java/com/tripplanner/service/ItineraryService.java` | ✅ **COMPLETED** | High | User-specific data structure implemented |
| Firebase Firestore user data organization | `src/main/java/com/tripplanner/config/FirestoreConfig.java` | ✅ **COMPLETED** | High | User-specific Firestore organization implemented |
| User ownership validation in backend | `src/main/java/com/tripplanner/service/BookingService.java:101,147,204` | ✅ **COMPLETED** | High | User ownership checks implemented |
| User session management | `frontend/src/contexts/AuthContext.tsx` | ✅ **COMPLETED** | High | React context for user state management implemented |
| Protected routes implementation | `frontend/src/components/ProtectedRoute.tsx` | ✅ **COMPLETED** | High | Route protection based on authentication status implemented |
| User profile management | `frontend/src/components/UserProfile.tsx` | ✅ **COMPLETED** | Medium | User profile, settings, logout functionality implemented |

## Updated Summary

### Status Breakdown
- **❌ Not Implemented**: 28 items (58%)
- **⚠️ Partial**: 8 items (17%)  
- **✅ Implemented**: 12 items (25%)

### Priority Breakdown
- **High Priority**: 12 items (8 completed, 4 remaining)
- **Medium Priority**: 20 items
- **Low Priority**: 8 items

### Categories
- **Frontend**: 19 items
- **Backend**: 12 items
- **Authentication**: 8 items (8 completed ✅)
- **Technical Notes**: 4 items
- **Roadmap**: 1 item

### Implementation Quality
- **Complete Implementation**: 12 items (25%)
- **Partial Implementation**: 8 items (17%)
- **No Implementation**: 28 items (58%)

## Mobile Responsiveness Analysis

### Current Status: ⚠️ **PARTIAL** - Needs Significant Improvement

**What's Working:**
- Basic responsive classes used (96 matches across 33 files)
- Some components have responsive grid layouts (`grid-cols-1 md:grid-cols-2 lg:grid-cols-3`)
- Landing page has mobile considerations (`hidden md:flex`)

**Critical Issues:**
1. **Navigation Sidebar** - Fixed width (`w-64`) with no collapse functionality
2. **No mobile navigation patterns** (hamburger menus, drawer navigation)
3. **Limited touch-friendly interactions**
4. **Many components lack mobile-first design**
5. **No responsive breakpoint strategy**

**Recommended Mobile Improvements:**
- Implement collapsible sidebar with hamburger menu
- Add mobile drawer navigation
- Implement touch-friendly button sizes (min 44px)
- Add responsive typography scaling
- Implement mobile-optimized layouts
- Add swipe gestures for mobile interactions

## Next Steps

1. **High Priority Items** (Remaining Core Features):
   - ✅ **Authentication system** (COMPLETED)
   - ✅ **Firebase user authentication integration** (COMPLETED)
   - ✅ **User-specific itinerary storage structure** (COMPLETED)
   - ✅ **Firebase Firestore user data organization** (COMPLETED)
   - ✅ **User ownership validation in backend** (COMPLETED)
   - ✅ **User session management** (COMPLETED)
   - ✅ **Protected routes implementation** (COMPLETED)
   - Complete workflow builder integration
   - Implement backend persistence for place additions
   - Complete Razorpay booking integration
   - **Make all components mobile compatible** (NEXT PRIORITY)
   - Add hotel booking on sleeping button click
   - Create new Bookings menu item
   - Implement hotel search functionality
   - Store chat with AI assistant in Firebase
   - Robust versioning of itinerary versions

2. **Medium Priority Items**:
   - Implement transport update logic
   - Add JSON parsing for booking data
   - Implement LLM-powered tools (packing, photos, foods, costs)
   - Enhance graph edge implementation
   - Consider better UI framework
   - Make menu pane collapsible to side
   - Remove discover button in destinations
   - Adapt destinations to not show multiple cards for same city
   - Make workflow auto reset generic for any number of places
   - Add node edge deletion functionality
   - Display version details with commit messages

3. **Low Priority Items**:
   - Firebase SDK configuration
   - Future roadmap features
   - Technical documentation improvements


## Critical Server Issues (High Priority)

### JSON Parsing & AI Response Issues
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Fix JsonEOFException in PlannerAgent | `src/main/java/com/tripplanner/agents/PlannerAgent.java:105` | ❌ Not Implemented | **CRITICAL** | AI response truncation causing itinerary generation failures |
| Add JSON validation before parsing | `src/main/java/com/tripplanner/agents/PlannerAgent.java` | ❌ Not Implemented | **CRITICAL** | Detect truncated AI responses before attempting to parse |
| Implement retry logic for AI calls | `src/main/java/com/tripplanner/agents/PlannerAgent.java` | ❌ Not Implemented | **HIGH** | Retry AI calls when JSON parsing fails due to truncation |
| Add response length validation | `src/main/java/com/tripplanner/agents/PlannerAgent.java` | ❌ Not Implemented | **HIGH** | Detect suspiciously short AI responses |
| Implement fallback strategies | `src/main/java/com/tripplanner/agents/PlannerAgent.java` | ❌ Not Implemented | **HIGH** | Handle incomplete/truncated AI responses gracefully |
| Add error recovery mechanisms | `src/main/java/com/tripplanner/agents/PlannerAgent.java` | ❌ Not Implemented | **HIGH** | Provide user-friendly error messages and retry options |
| Implement response streaming | `src/main/java/com/tripplanner/agents/PlannerAgent.java` | ❌ Not Implemented | **MEDIUM** | Prevent truncation by using streaming API |
| Add monitoring and alerting | `src/main/java/com/tripplanner/agents/PlannerAgent.java` | ❌ Not Implemented | **MEDIUM** | Track AI response issues and alert on problems |
| Optimize AI request sizes | `src/main/java/com/tripplanner/agents/PlannerAgent.java` | ❌ Not Implemented | **MEDIUM** | Prevent hitting token limits and context window constraints |

## Workflow Synchronization Issues (Medium Priority)

### Bidirectional Synchronization
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Implement bidirectional workflow sync | `frontend/src/components/WorkflowBuilder.tsx` | ⚠️ Partial | **HIGH** | Currently only one-way sync exists (workflow → map) |
| Add real-time synchronization | `frontend/src/components/WorkflowBuilder.tsx` | ❌ Not Implemented | **MEDIUM** | Real-time updates between workflow, map, and day-by-day views |
| Implement cross-component communication | `frontend/src/components/WorkflowBuilder.tsx` | ❌ Not Implemented | **MEDIUM** | System for workflow-itinerary synchronization |
| Add workflow validation | `frontend/src/components/WorkflowBuilder.tsx` | ❌ Not Implemented | **MEDIUM** | Ensure workflow changes sync with itinerary data |
| Implement workflow persistence | `frontend/src/components/WorkflowBuilder.tsx` | ❌ Not Implemented | **MEDIUM** | Proper backend persistence of workflow changes |

### Event-Driven Updates
| TODO Item | File Location | Status | Priority | Description |
|-----------|---------------|--------|----------|-------------|
| Implement event-driven updates | `frontend/src/components/WorkflowBuilder.tsx` | ❌ Not Implemented | **MEDIUM** | Event-driven updates between components |
| Add workflow change listeners | `frontend/src/components/WorkflowBuilder.tsx` | ❌ Not Implemented | **MEDIUM** | Listen for workflow changes and propagate to other views |
| Implement map marker updates | `frontend/src/components/travel-planner/TripMap.tsx` | ❌ Not Implemented | **MEDIUM** | Update map markers when workflow nodes change |
| Add day-by-day view updates | `frontend/src/components/travel-planner/views/DayByDayView.tsx` | ❌ Not Implemented | **MEDIUM** | Update day-by-day cards when workflow changes |

## Updated Summary

### Status Breakdown
- **❌ Not Implemented**: 58 items (68%)
- **⚠️ Partial**: 9 items (11%)  
- **✅ Implemented**: 18 items (21%)

### Priority Breakdown
- **CRITICAL Priority**: 2 items (0 completed, 2 remaining)
- **High Priority**: 22 items (8 completed, 14 remaining)
- **Medium Priority**: 35 items
- **Low Priority**: 8 items

### Categories
- **Frontend**: 35 items
- **Backend**: 22 items
- **Authentication**: 8 items (8 completed ✅)
- **Technical Notes**: 4 items
- **Roadmap**: 1 item
- **Critical Server Issues**: 9 items
- **Workflow Sync Issues**: 9 items

### Implementation Quality
- **Complete Implementation**: 18 items (21%)
- **Partial Implementation**: 9 items (11%)
- **No Implementation**: 58 items (68%)

## Next Steps

1. **CRITICAL Priority Items** (Immediate Action Required):
   - Fix JsonEOFException in PlannerAgent
   - Add JSON validation before parsing
   - Implement retry logic for AI calls
   - Add response length validation
   - Implement fallback strategies
   - Add error recovery mechanisms

2. **High Priority Items** (Core Features):
   - Complete workflow builder integration
   - Implement backend persistence for place additions
   - Complete Razorpay booking integration
   - **Make all components mobile compatible**
   - Add hotel booking on sleeping button click
   - Create new Bookings menu item
   - Implement hotel search functionality
   - Store chat with AI assistant in Firebase
   - Robust versioning of itinerary versions
   - Implement bidirectional workflow sync

3. **Medium Priority Items**:
   - Implement transport update logic
   - Add JSON parsing for booking data
   - Implement LLM-powered tools (packing, photos, foods, costs)
   - Enhance graph edge implementation
   - Consider better UI framework
   - Make menu pane collapsible to side
   - Remove discover button in destinations
   - Adapt destinations to not show multiple cards for same city
   - Make workflow auto reset generic for any number of places
   - Add node edge deletion functionality
   - Display version details with commit messages
   - Add real-time synchronization
   - Implement cross-component communication
   - Add workflow validation
   - Implement workflow persistence
   - Implement response streaming
   - Add monitoring and alerting
   - Optimize AI request sizes

4. **Low Priority Items**:
   - Firebase SDK configuration
   - Future roadmap features
   - Technical documentation improvements

Other requests to do

complete analysis of itineraryjsonservice and its usages as well as userdataservice and its usages. plan a roadmap on how to effieicently decomission itineraryjson service as it is legacy
