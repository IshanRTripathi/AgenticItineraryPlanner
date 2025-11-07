# Backend Integration Tasks

**Purpose**: Connect frontend-redesign to existing Spring Boot backend  
**Backend Location**: `src/main/java/com/tripplanner/`  
**Frontend Location**: `frontend-redesign/`  
**Status**: Backend APIs exist, need frontend integration

---

## 🔌 Existing Backend APIs

### Itineraries API (`/api/v1/itineraries`)
- ✅ `POST /api/v1/itineraries` - Create itinerary (returns executionId for WebSocket)
- ✅ `GET /api/v1/itineraries` - List user's itineraries
- ✅ `GET /api/v1/itineraries/{id}` - Get itinerary details
- ✅ `GET /api/v1/itineraries/{id}/json` - Get full itinerary JSON
- ✅ `PUT /api/v1/itineraries/{id}` - Update itinerary
- ✅ `DELETE /api/v1/itineraries/{id}` - Delete itinerary
- ✅ `POST /api/v1/itineraries/{id}/chat` - Chat with itinerary

### Booking API (`/api/v1`)
- ✅ `POST /api/v1/payments/razorpay/order` - Create payment order
- ✅ `POST /api/v1/payments/razorpay/webhook` - Handle payment webhook
- ✅ `POST /api/v1/providers/{vertical}/{provider}:book` - Execute provider booking

### WebSocket (`/ws`)
- ✅ `/ws` - WebSocket endpoint for real-time agent progress updates
- ✅ Subscribe to `/topic/itinerary/{executionId}` for progress
- ✅ Receives AgentProgressEvent messages

### Export API (`/api/v1/export`)
- ✅ `GET /api/v1/export/{id}/pdf` - Export itinerary as PDF

---

## 📋 Integration Tasks

### Phase 1: Core API Integration (Priority: HIGH)

- [ ] **Task 1: Update API Service**
  - [ ] 1.1 Update `frontend-redesign/src/services/api.ts`
    - Add all itinerary endpoints
    - Add booking endpoints
    - Add export endpoints
    - Add proper TypeScript types from backend DTOs
  
  - [ ] 1.2 Create DTO type definitions
    - Create `frontend-redesign/src/types/dto.ts`
    - Define CreateItineraryReq (matches backend)
    - Define ItineraryCreationResponse
    - Define ItineraryJson
    - Define NormalizedNode
    - Define BookingRes, ProviderBookReq
  
  - [ ] 1.3 Add authentication headers
    - Get Firebase token from AuthContext
    - Add Authorization: Bearer {token} to all requests
    - Handle 401 responses (redirect to login)

- [ ] **Task 2: Connect Trip Wizard to Backend**
  - [ ] 2.1 Update `TripWizard.tsx` handleSubmit
    - Call `POST /api/v1/itineraries` with form data
    - Extract executionId from response
    - Navigate to `/ai-progress?executionId={id}`
  
  - [ ] 2.2 Update `AgentProgress.tsx`
    - Get executionId from URL params
    - Connect to WebSocket: `/ws`
    - Subscribe to `/topic/itinerary/{executionId}`
    - Display real-time progress from AgentProgressEvent
    - On completion, navigate to `/trip/{itineraryId}`

- [ ] **Task 3: Connect Dashboard to Backend**
  - [ ] 3.1 Update `DashboardPage.tsx`
    - Call `GET /api/v1/itineraries` on mount
    - Display real itineraries instead of mock data
    - Handle loading state
    - Handle empty state
    - Handle error state
  
  - [ ] 3.2 Update `TripCard.tsx`
    - Use real itinerary data
    - Implement delete: `DELETE /api/v1/itineraries/{id}`
    - Show confirmation dialog before delete
    - Refresh list after delete

- [ ] **Task 4: Connect Trip Detail to Backend**
  - [ ] 4.1 Update `TripDetailPage.tsx`
    - Get itineraryId from URL params
    - Call `GET /api/v1/itineraries/{id}/json` on mount
    - Display real itinerary data
    - Parse NormalizedNode structure
    - Display day-by-day itinerary from backend data
  
  - [ ] 4.2 Implement booking integration
    - When "Book Now" clicked, call provider booking API
    - Update node with booking reference
    - Refresh itinerary data after booking

- [ ] **Task 5: WebSocket Integration**
  - [ ] 5.1 Update `useWebSocket.ts` hook
    - Connect to `ws://localhost:8080/ws` (dev)
    - Connect to `wss://{domain}/ws` (prod)
    - Handle STOMP protocol
    - Subscribe to topics
    - Handle reconnection
  
  - [ ] 5.2 Create WebSocket context
    - Create `frontend-redesign/src/contexts/WebSocketContext.tsx`
    - Provide WebSocket connection to app
    - Handle connection state
    - Provide subscribe/unsubscribe methods

---

### Phase 2: Enhanced Features (Priority: MEDIUM)

- [ ] **Task 6: Chat Integration**
  - [ ] 6.1 Create chat component
    - Create `frontend-redesign/src/components/trip/ChatPanel.tsx`
    - Display chat history
    - Send messages via `POST /api/v1/itineraries/{id}/chat`
    - Receive responses via WebSocket
  
  - [ ] 6.2 Add chat to trip detail page
    - Add floating chat button
    - Open chat panel on click
    - Show unread message count

- [ ] **Task 7: Export Integration**
  - [ ] 7.1 Implement PDF export
    - Add "Export PDF" button to trip detail
    - Call `GET /api/v1/export/{id}/pdf`
    - Download PDF file
    - Show loading state during export

- [ ] **Task 8: Search Integration**
  - [ ] 8.1 Connect search forms to backend
    - Update FlightSearchForm to call backend search
    - Update HotelSearchForm to call backend search
    - Display real search results
    - Handle pagination

---

### Phase 3: Authentication (Priority: HIGH)

- [ ] **Task 9: Firebase Authentication**
  - [ ] 9.1 Set up Firebase in frontend
    - Install firebase SDK: `npm install firebase`
    - Create `frontend-redesign/src/config/firebase.ts`
    - Initialize Firebase app
    - Configure authentication
  
  - [ ] 9.2 Create AuthContext
    - Create `frontend-redesign/src/contexts/AuthContext.tsx`
    - Provide user state
    - Provide login/logout methods
    - Provide token getter
  
  - [ ] 9.3 Update LoginPage
    - Implement Google Sign-In with Firebase
    - Get ID token after sign-in
    - Store token in context
    - Redirect to dashboard
  
  - [ ] 9.4 Create ProtectedRoute component
    - Create `frontend-redesign/src/components/auth/ProtectedRoute.tsx`
    - Check if user is authenticated
    - Redirect to /login if not
    - Wrap protected routes in App.tsx

- [ ] **Task 10: API Authentication**
  - [ ] 10.1 Add auth interceptor
    - Create axios instance with interceptor
    - Add Authorization header to all requests
    - Handle token refresh
    - Handle 401 responses

---

### Phase 4: Real-time Updates (Priority: MEDIUM)

- [ ] **Task 11: WebSocket Event Handling**
  - [ ] 11.1 Handle itinerary updates
    - Subscribe to itinerary changes
    - Update UI when itinerary changes
    - Show notification on updates
  
  - [ ] 11.2 Handle booking updates
    - Subscribe to booking status changes
    - Update booking cards in real-time
    - Show notification on booking confirmation

---

### Phase 5: Error Handling & Polish (Priority: LOW)

- [ ] **Task 12: Error Handling**
  - [ ] 12.1 Add global error handler
    - Catch API errors
    - Display user-friendly messages
    - Log errors for debugging
  
  - [ ] 12.2 Add retry logic
    - Retry failed requests
    - Exponential backoff
    - Max retry attempts

- [ ] **Task 13: Loading States**
  - [ ] 13.1 Add loading indicators
    - Show skeleton loaders during API calls
    - Show progress bars for long operations
    - Disable buttons during submission

- [ ] **Task 14: Offline Support**
  - [ ] 14.1 Add offline detection
    - Detect network status
    - Show offline banner
    - Queue requests when offline
    - Sync when back online

---

## 🔗 Integration Checklist

### Prerequisites
- [ ] Backend is running on `http://localhost:8080`
- [ ] Firebase project is configured
- [ ] CORS is enabled on backend for frontend origin
- [ ] WebSocket is accessible

### Testing
- [ ] Create itinerary flow works end-to-end
- [ ] Real-time progress updates work
- [ ] Dashboard displays real itineraries
- [ ] Trip detail shows real data
- [ ] Booking flow works
- [ ] Authentication works
- [ ] WebSocket reconnects on disconnect

### Deployment
- [ ] Update API_BASE_URL for production
- [ ] Update WebSocket URL for production
- [ ] Configure Firebase for production
- [ ] Test on production environment

---

## 📝 Notes

- Backend uses Firebase Authentication - frontend must match
- WebSocket uses STOMP protocol - use `@stomp/stompjs` library
- All API responses follow standard format: `{ success, data, error }`
- Itinerary structure is complex - use TypeScript types carefully
- Real-time updates are critical for UX - prioritize WebSocket integration

---

**Status**: Ready for implementation  
**Estimated Time**: 2-3 days for Phase 1, 1-2 days for Phase 2-3  
**Dependencies**: Firebase configuration, backend running
