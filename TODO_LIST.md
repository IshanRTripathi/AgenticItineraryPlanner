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

### Firebase Configuration
| TODO Item | File | Status | Priority | Description |
|-----------|------|--------|----------|-------------|
| Add SDKs for Firebase products | `frontend/src/config/firebase.ts:4` | ❌ Not Implemented | Low | Firebase SDK configuration |

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

## Implementation Notes

### Authentication & User Management
| Note | File | Status | Priority | Description |
|------|------|--------|----------|-------------|
| Hardcoded anonymous user | `src/main/java/com/tripplanner/service/BookingService.java:93` | ⚠️ Partial | High | Replace with actual user authentication |
| Ownership check removed | `src/main/java/com/tripplanner/service/BookingService.java:101` | ⚠️ Partial | High | Re-implement user ownership validation |
| Ownership check removed | `src/main/java/com/tripplanner/service/BookingService.java:144` | ⚠️ Partial | High | Re-implement user ownership validation |
| Ownership check removed | `src/main/java/com/tripplanner/service/BookingService.java:199` | ⚠️ Partial | High | Re-implement user ownership validation |

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
| Implement user authentication system | `frontend/src/services/authService.ts` (new) | ❌ Not Implemented | High | Replace hardcoded "anonymous" user with real auth |
| Firebase user authentication integration | `frontend/src/config/firebase.ts` | ❌ Not Implemented | High | Google Auth, email/password, or other providers |
| User-specific itinerary storage structure | `src/main/java/com/tripplanner/service/ItineraryService.java` | ❌ Not Implemented | High | Change from global to userId -> itineraries -> it1, it2 structure |
| Firebase Firestore user data organization | `src/main/java/com/tripplanner/config/FirestoreConfig.java` | ❌ Not Implemented | High | Organize collections as users/{userId}/itineraries/{itineraryId} |
| User ownership validation in backend | `src/main/java/com/tripplanner/service/BookingService.java:101,144,199` | ❌ Not Implemented | High | Re-implement ownership checks with real user IDs |
| User session management | `frontend/src/contexts/AuthContext.tsx` (new) | ❌ Not Implemented | High | React context for user state management |
| Protected routes implementation | `frontend/src/components/ProtectedRoute.tsx` (new) | ❌ Not Implemented | High | Route protection based on authentication status |
| User profile management | `frontend/src/components/UserProfile.tsx` (new) | ❌ Not Implemented | Medium | User profile, settings, logout functionality |

## Updated Summary

### Status Breakdown
- **❌ Not Implemented**: 36 items (75%)
- **⚠️ Partial**: 8 items (17%)  
- **✅ Implemented**: 4 items (8%)

### Priority Breakdown
- **High Priority**: 20 items
- **Medium Priority**: 20 items
- **Low Priority**: 8 items

### Categories
- **Frontend**: 19 items
- **Backend**: 12 items
- **Authentication**: 12 items
- **Technical Notes**: 4 items
- **Roadmap**: 1 item

### Implementation Quality
- **Complete Implementation**: 4 items (8%)
- **Partial Implementation**: 8 items (17%)
- **No Implementation**: 36 items (75%)

## Next Steps

1. **High Priority Items** (Authentication & Core Features):
   - **Implement user authentication system** (NEW - Start Here)
   - **Firebase user authentication integration** (NEW - Start Here)
   - **User-specific itinerary storage structure** (NEW - Start Here)
   - **Firebase Firestore user data organization** (NEW - Start Here)
   - **User ownership validation in backend** (NEW - Start Here)
   - **User session management** (NEW - Start Here)
   - **Protected routes implementation** (NEW - Start Here)
   - Complete workflow builder integration
   - Implement backend persistence for place additions
   - Complete Razorpay booking integration
   - Make all components mobile compatible
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
