# Week 11: Real-time Features - Kickoff 🚀

**Date**: 2025-01-31  
**Status**: Starting Now  
**Focus**: WebSocket Integration & Chat Interface  
**Timeline**: Week 11 of 18

---

## 🎯 WEEK 11 GOALS

Implement real-time features to enable collaborative editing and live updates:

1. **WebSocket Integration** - Real-time communication with backend
2. **Chat Interface** - AI-powered itinerary modifications via chat
3. **Map Integration** - Display trip locations on interactive map
4. **Weather Widget** - Show real weather data for destinations
5. **Advanced Interactions** - Drag & drop and inline editing

---

## 📋 TASKS OVERVIEW

### Task 23: WebSocket Integration (8-10 hours)
**Priority**: P1 - HIGH  
**Dependencies**: Week 10 complete ✅

**Subtasks**:
- [ ] 23.1 Create WebSocket service
- [ ] 23.2 Create UnifiedItineraryContext
- [ ] 23.3 Integrate WebSocket with context

**Deliverable**: Real-time updates working for itinerary changes

### Task 24: Chat Interface (10-12 hours)
**Priority**: P1 - HIGH  
**Dependencies**: Task 23

**Subtasks**:
- [ ] 24.1 Port chat components from original frontend
- [ ] 24.2 Add chat tab to TripDetailPage
- [ ] 24.3 Implement chat-based modifications

**Deliverable**: Functional chat interface for AI modifications

### Task 25: Missing UI Components (6-8 hours)
**Priority**: P2 - MEDIUM  
**Dependencies**: Task 23

**Subtasks**:
- [ ] 25.1 Integrate TripMap component (already exists)
- [ ] 25.2 Connect WeatherWidget to real API (already exists)
- [ ] 25.3 Add place photos component

**Deliverable**: Map and weather showing real data

### Task 26: Advanced Interactions (6-8 hours)
**Priority**: P2 - MEDIUM  
**Dependencies**: Task 23, 24

**Subtasks**:
- [ ] 26.1 Add drag & drop for activities
- [ ] 26.2 Implement inline editing

**Deliverable**: Interactive itinerary editing

---

## 🔍 WHAT EXISTS ALREADY

### From Original Frontend (Need to Port)
- ✅ `frontend/src/contexts/UnifiedItineraryContext.tsx` - State management
- ✅ `frontend/src/contexts/UnifiedItineraryTypes.ts` - Type definitions
- ✅ `frontend/src/contexts/UnifiedItineraryReducer.ts` - State reducer
- ✅ `frontend/src/components/chat/NewChat.tsx` - Chat component
- ✅ `frontend/src/components/chat/ChatMessage.tsx` - Message display
- ✅ `frontend/src/services/sseManager.ts` - SSE connection manager
- ✅ `frontend/src/hooks/useSseConnection.ts` - SSE hook

### From Redesign Frontend (Already Exists)
- ✅ `frontend-redesign/src/components/map/TripMap.tsx` - Map component
- ✅ `frontend-redesign/src/components/weather/WeatherWidget.tsx` - Weather widget
- ✅ `frontend-redesign/src/hooks/useStompWebSocket.ts` - STOMP WebSocket hook
- ✅ `frontend-redesign/src/services/websocket.ts` - WebSocket service

### Backend (Already Working)
- ✅ WebSocket endpoint: `ws://localhost:8080/ws`
- ✅ STOMP protocol support
- ✅ Topic: `/topic/itinerary/{executionId}`
- ✅ Agent progress updates
- ✅ Chat message handling

---

## 📖 IMPLEMENTATION STRATEGY

### Phase 1: WebSocket Foundation (Task 23)
**Goal**: Establish real-time communication

1. **Review existing WebSocket code**
   - Check `frontend-redesign/src/hooks/useStompWebSocket.ts`
   - Check `frontend-redesign/src/services/websocket.ts`
   - Verify STOMP protocol implementation

2. **Port UnifiedItineraryContext**
   - Copy from `frontend/src/contexts/UnifiedItineraryContext.tsx`
   - Update imports for redesign structure
   - Integrate with WebSocket service
   - Test state management

3. **Integrate WebSocket**
   - Connect context to WebSocket
   - Handle real-time updates
   - Implement reconnection logic
   - Test with backend

### Phase 2: Chat Interface (Task 24)
**Goal**: Enable AI-powered modifications

1. **Port chat components**
   - Copy `NewChat.tsx` and `ChatMessage.tsx`
   - Update styling to match redesign
   - Integrate with UnifiedItineraryContext

2. **Add chat tab**
   - Add "Chat" tab to TripDetailPage
   - Integrate chat components
   - Test message sending/receiving

3. **Implement modifications**
   - Connect to backend `/agents/process-request`
   - Handle change previews
   - Apply changes to itinerary
   - Test end-to-end

### Phase 3: UI Components (Task 25)
**Goal**: Complete visual features

1. **Map integration**
   - TripMap already exists
   - Add to ViewTab or PlanTab
   - Connect to itinerary data
   - Test marker display

2. **Weather widget**
   - WeatherWidget already exists
   - Get OpenWeather API key
   - Connect to real API
   - Test forecast display

3. **Place photos**
   - Create PlacePhotos component
   - Integrate Google Places API
   - Add to activity cards
   - Test photo loading

### Phase 4: Interactions (Task 26)
**Goal**: Enable interactive editing

1. **Drag & drop**
   - Install @dnd-kit packages
   - Add to PlanTab day cards
   - Implement reordering logic
   - Connect to backend API

2. **Inline editing**
   - Add edit mode to node cards
   - Implement save logic
   - Connect to backend
   - Test editing flow

---

## 🛠️ TECHNICAL REQUIREMENTS

### Dependencies to Install
```bash
# Drag & drop
npm install @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities

# Already installed (verify)
npm list @stomp/stompjs sockjs-client
```

### Environment Variables Needed
```bash
# Already configured
VITE_WS_BASE_URL=http://localhost:8080/ws
VITE_API_BASE_URL=http://localhost:8080/api/v1

# Need to add (optional)
VITE_OPENWEATHER_API_KEY=your-key-here
```

### Backend Endpoints to Use
- ✅ `ws://localhost:8080/ws` - WebSocket connection
- ✅ `POST /api/v1/agents/process-request` - Chat modifications
- ✅ `POST /api/v1/itineraries/{id}:apply` - Apply changes
- ✅ `GET /api/v1/places/{placeId}/photos` - Place photos

---

## 📊 SUCCESS CRITERIA

### Task 23: WebSocket
- [ ] WebSocket connects successfully
- [ ] Real-time updates received
- [ ] Reconnection works on disconnect
- [ ] State management working
- [ ] No memory leaks

### Task 24: Chat
- [ ] Chat interface displays
- [ ] Messages send successfully
- [ ] AI responses received
- [ ] Changes preview shown
- [ ] Changes apply correctly

### Task 25: UI Components
- [ ] Map displays all locations
- [ ] Weather shows real forecast
- [ ] Place photos load
- [ ] All components responsive

### Task 26: Interactions
- [ ] Drag & drop reorders activities
- [ ] Inline editing saves changes
- [ ] Backend updates correctly
- [ ] UI updates in real-time

---

## 🎯 WEEK 11 DELIVERABLES

By end of Week 11, we should have:

1. ✅ **Real-time Updates** - WebSocket working with live itinerary updates
2. ✅ **Chat Interface** - Functional AI chat for modifications
3. ✅ **Map Integration** - Trip locations displayed on map
4. ✅ **Weather Data** - Real weather forecasts shown
5. ✅ **Interactive Editing** - Drag & drop and inline editing working

---

## 📝 NOTES

### From Week 10
- ✅ All backend integration complete
- ✅ Skeleton loaders implemented
- ✅ Error handling comprehensive
- ✅ Token refresh working
- ✅ BookingsTab has real data

### For Week 11
- Focus on real-time features
- Port existing code where possible
- Test thoroughly with backend
- Maintain code quality standards
- Document all changes

### Known Challenges
- WebSocket reconnection logic
- State synchronization
- Chat message ordering
- Drag & drop performance
- Real-time conflict resolution

---

## 🚀 LET'S START!

**First Task**: Task 23.1 - Create WebSocket Service

**Approach**:
1. Review existing `useStompWebSocket.ts` and `websocket.ts`
2. Verify STOMP protocol implementation
3. Test connection with backend
4. Implement reconnection logic
5. Add error handling

**Ready to begin Week 11!** 🎉

---

**Status**: 📍 Starting Point  
**Next**: Task 23.1 - WebSocket Service  
**Timeline**: 8-10 hours for Task 23  
**Goal**: Real-time updates working by end of day

