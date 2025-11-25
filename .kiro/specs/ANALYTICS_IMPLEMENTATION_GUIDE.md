# Analytics Implementation Guide
# Agentic Itinerary Planner

## Quick Reference

### Implementation Checklist
- **Phase 1 (Week 1-2)**: User auth, trip creation, bookings, payments
- **Phase 2 (Week 3-4)**: Page views, interactions, exports, searches
- **Phase 3 (Week 5-6)**: Agent tracking, LLM performance, token consumption
- **Phase 4 (Week 7-8)**: Dashboard, reporting, cost alerts

### Key Events
- Business: `user_login_completed`, `trip_creation_completed`, `booking_completed`, `payment_completed`
- Engagement: `page_view`, `activity_viewed`, `pdf_export_completed`
- Technical: `agent_completed`, `llm_token_usage`, `circuit_breaker_opened`

### Critical Files
- Backend: `AnalyticsService.java`, `TokenCostCalculator.java`, `AnalyticsController.java`
- Frontend: `analytics.ts`, `App.tsx`, `PremiumTripWizard.tsx:80`, `LoginPage.tsx:39`
- LLM Clients: `GeminiClient.java`, `OpenRouterClient.java`, `ResilientAiClient.java`

### Token Pricing (Nov 2024)
- Gemini 2.5 Flash: $0.075 input, $0.30 output per 1M tokens
- Qwen 2.5 72B: $0.35 input, $0.40 output per 1M tokens (free tier: $0.00)

### Cost Alerts
- Daily: Warning $10, Critical $50
- Monthly: Warning $200, Critical $500

---

## Executive Summary

This document provides a comprehensive guide for implementing extensive analytics tracking across the Agentic Itinerary Planner platform. It covers where to add analytics, what to track, storage strategies, and implementation priorities.

## Current State

### Existing Infrastructure
- **Backend**: `AnalyticsController.java` - Basic event tracking endpoint (logs only)
- **Frontend**: `analytics.ts` - Service with Google Analytics 4 integration
- **Database**: Firestore with flexible document storage
- **Configuration**: Environment variables for GA tracking ID

### Current Limitations
- Analytics service exists but is **not actively used** in the codebase
- No database persistence for analytics events
- No analytics entity/model defined
- No reporting or dashboard capabilities
- Google Analytics integration configured but not tracking events

## Analytics Architecture

### Data Flow
```
Frontend User Action
    ↓
analytics.track(event, properties)
    ↓
├─→ Google Analytics 4 (gtag)
└─→ Backend API (/api/v1/analytics/events)
        ↓
    AnalyticsService
        ↓
    ├─→ Firestore (analytics collection)
    ├─→ Real-time processing
    └─→ Aggregation service
```

### Storage Strategy

#### 1. Firestore Collections Structure
```
analytics/
├── events/
│   └── {eventId}/
│       ├── eventName: string
│       ├── userId: string
│       ├── timestamp: timestamp
│       ├── properties: map
│       ├── sessionId: string
│       └── userAgent: string
├── sessions/
│   └── {sessionId}/
│       ├── userId: string
│       ├── startTime: timestamp
│       ├── endTime: timestamp
│       ├── pageViews: number
│       └── events: array
├── user_metrics/
│   └── {userId}/
│       ├── totalEvents: number
│       ├── totalSessions: number
│       ├── totalItineraries: number
│       ├── totalBookings: number
│       ├── lastActive: timestamp
│       └── firstSeen: timestamp
└── daily_aggregates/
    └── {date}/
        ├── totalUsers: number
        ├── totalEvents: number
        ├── eventBreakdown: map
        └── conversionMetrics: map
```


## Analytics Tracking Points

### Level 1: Critical Business Metrics (HIGH PRIORITY)

#### 1.1 User Authentication & Onboarding
**Location**: `frontend/src/pages/LoginPage.tsx`
**Events to Track**:
- `user_signup_started` - When signup form is opened
- `user_signup_completed` - When user successfully signs up
- `user_login_started` - When login is initiated
- `user_login_completed` - When user successfully logs in
- `google_oauth_clicked` - When Google OAuth button is clicked
- `guest_mode_selected` - When user continues as guest

**Properties**:
```typescript
{
  method: 'google' | 'email' | 'guest',
  timestamp: ISO string,
  referrer: string
}
```

**Implementation**:
```typescript
// In LoginPage.tsx - Line ~39
import { analytics } from '@/services/analytics';

// After successful login
analytics.track('user_login_completed', {
  method: 'google',
  timestamp: new Date().toISOString()
});
```

#### 1.2 Trip Creation (AI-Powered)
**Location**: `frontend/src/components/ai-planner/PremiumTripWizard.tsx`
**Events to Track**:
- `trip_wizard_started` - When wizard is opened
- `trip_wizard_step_completed` - Each step completion
- `trip_creation_initiated` - When "Create Itinerary" is clicked (Line ~81)
- `trip_creation_completed` - When itinerary generation finishes
- `trip_creation_failed` - When generation fails

**Properties**:
```typescript
{
  destination: string,
  origin: string,
  duration: number,
  travelers: number,
  budget: string,
  preferences: string[],
  executionId: string,
  timestamp: ISO string
}
```

**Implementation**:
```typescript
// In PremiumTripWizard.tsx - Line ~80
analytics.track('trip_creation_initiated', {
  destination: formData.destination,
  origin: formData.origin,
  duration: calculateDuration(formData.startDate, formData.endDate),
  travelers: formData.party.adults + formData.party.children,
  budget: formData.budget,
  timestamp: new Date().toISOString()
});
```

#### 1.3 Booking Conversions
**Location**: `frontend/src/components/booking/BookingCategoryCard.tsx`
**Events to Track**:
- `booking_card_viewed` - When booking card is displayed
- `booking_initiated` - When "Book Now" is clicked
- `booking_iframe_loaded` - When external booking page loads
- `booking_completed` - When booking is confirmed (webhook)

**Properties**:
```typescript
{
  provider: 'easemytrip' | 'booking.com' | 'expedia',
  category: 'transport' | 'accommodation' | 'dining' | 'attractions',
  vertical: 'flight' | 'hotel' | 'train' | 'bus',
  itineraryId: string,
  nodeId: string,
  estimatedAmount: number,
  currency: string,
  timestamp: ISO string
}
```

**Implementation**:
```typescript
// In BookingCategoryCard.tsx
analytics.trackBooking('initiated', {
  provider: 'easemytrip',
  vertical: booking.category,
  itineraryId: itineraryId,
  amount: booking.cost?.amount,
  currency: booking.cost?.currency
});
```


#### 1.4 Payment Processing
**Location**: `src/main/java/com/tripplanner/service/RazorpayService.java`
**Events to Track**:
- `payment_order_created` - When Razorpay order is created
- `payment_initiated` - When payment flow starts
- `payment_completed` - When payment succeeds
- `payment_failed` - When payment fails
- `refund_initiated` - When refund is requested
- `refund_completed` - When refund is processed

**Properties**:
```java
{
  orderId: String,
  amount: Double,
  currency: String,
  userId: String,
  itineraryId: String,
  bookingId: String,
  paymentMethod: String,
  status: String,
  timestamp: Instant
}
```

**Implementation**:
```java
// In RazorpayService.java - after order creation
analyticsService.trackEvent(new AnalyticsEvent(
    "payment_order_created",
    userId,
    Map.of(
        "orderId", order.getId(),
        "amount", amount,
        "currency", currency,
        "itineraryId", itineraryId
    )
));
```

### Level 2: User Engagement Metrics (MEDIUM PRIORITY)

#### 2.1 Page Navigation
**Location**: All page components in `frontend/src/pages/`
**Events to Track**:
- `page_view` - Every page load

**Pages to Track**:
- `HomePage.tsx` (Line ~8)
- `DashboardPage.tsx` (Line ~17)
- `TripDetailPage.tsx` (Line ~334)
- `TripWizardPage.tsx` (Line ~9)
- `SearchPage.tsx` (Line ~12)
- `ProfilePage.tsx` (Line ~31)
- `LoginPage.tsx` (Line ~39)
- `SignupPage.tsx` (Line ~14)
- `AgentProgressPage.tsx` (Line ~9)

**Implementation**:
```typescript
// Add to each page component in useEffect
useEffect(() => {
  analytics.page(window.location.pathname, 'Dashboard Page');
}, []);
```

#### 2.2 Itinerary Interactions
**Location**: `frontend/src/components/trip/DayCard.tsx`
**Events to Track**:
- `activity_viewed` - When activity card is expanded
- `activity_edited` - When activity is modified
- `activity_deleted` - When activity is removed
- `activity_reordered` - When activities are drag-dropped
- `day_expanded` - When day card is opened
- `day_collapsed` - When day card is closed

**Properties**:
```typescript
{
  itineraryId: string,
  dayNumber: number,
  nodeId: string,
  nodeType: string,
  action: string,
  timestamp: ISO string
}
```

**Implementation**:
```typescript
// In DayCard.tsx
const handleActivityClick = (nodeId: string) => {
  analytics.track('activity_viewed', {
    itineraryId,
    dayNumber,
    nodeId,
    nodeType: node.type,
    timestamp: new Date().toISOString()
  });
};
```

#### 2.3 Chat Interactions
**Location**: `frontend/src/components/chat/` (chat components)
**Events to Track**:
- `chat_opened` - When chat interface is opened
- `chat_message_sent` - When user sends a message
- `chat_response_received` - When AI responds
- `chat_suggestion_clicked` - When user clicks a suggestion

**Properties**:
```typescript
{
  itineraryId: string,
  messageLength: number,
  intent: string,
  responseTime: number,
  timestamp: ISO string
}
```


#### 2.4 Export & Sharing
**Location**: `frontend/src/components/export/ExportOptionsModal.tsx`
**Events to Track**:
- `export_modal_opened` - When export modal is opened
- `export_format_selected` - When user selects format (PDF/Email)
- `pdf_export_initiated` - When PDF generation starts
- `pdf_export_completed` - When PDF is downloaded
- `email_share_initiated` - When email share is clicked
- `email_share_completed` - When email is sent
- `public_link_created` - When public share link is generated
- `public_link_copied` - When link is copied to clipboard

**Properties**:
```typescript
{
  itineraryId: string,
  format: 'pdf' | 'email' | 'link',
  recipientCount?: number,
  fileSize?: number,
  timestamp: ISO string
}
```

**Implementation**:
```typescript
// In ExportOptionsModal.tsx
analytics.track('pdf_export_initiated', {
  itineraryId,
  format: 'pdf',
  timestamp: new Date().toISOString()
});
```

#### 2.5 Search & Discovery
**Location**: `frontend/src/components/premium/search/` and `frontend/src/pages/SearchPage.tsx`
**Events to Track**:
- `search_initiated` - When search is started
- `search_completed` - When results are displayed
- `search_filter_applied` - When filters are used
- `search_result_clicked` - When a result is selected
- `date_range_selected` - When dates are picked

**Properties**:
```typescript
{
  searchType: 'flight' | 'hotel' | 'activity' | 'destination',
  query: string,
  filters: object,
  resultCount: number,
  timestamp: ISO string
}
```

### Level 3: AI Agent Performance (MEDIUM PRIORITY)

#### 3.1 Agent Execution Tracking
**Location**: `src/main/java/com/tripplanner/agents/BaseAgent.java`
**Events to Track**:
- `agent_queued` - When agent is added to queue
- `agent_started` - When agent begins execution
- `agent_progress` - Progress updates (10%, 25%, 50%, 75%, 90%)
- `agent_completed` - When agent finishes successfully
- `agent_failed` - When agent encounters error

**Properties**:
```java
{
  agentType: String,
  itineraryId: String,
  executionId: String,
  duration: Long,
  progress: Integer,
  errorType: String,
  retryCount: Integer,
  timestamp: Instant
}
```

**Implementation**:
```java
// In BaseAgent.java - execute method
analyticsService.trackEvent(new AnalyticsEvent(
    "agent_started",
    userId,
    Map.of(
        "agentType", this.getClass().getSimpleName(),
        "itineraryId", itineraryId,
        "executionId", executionId
    )
));
```

#### 3.2 LLM API Performance & Token Consumption
**Location**: `src/main/java/com/tripplanner/service/ai/ResilientAiClient.java`, `GeminiClient.java`, `OpenRouterClient.java`
**Events to Track**:
- `llm_request_sent` - When API call is made
- `llm_response_received` - When response arrives
- `llm_request_failed` - When request fails
- `llm_retry_attempted` - When retry is triggered
- `circuit_breaker_opened` - When circuit breaker activates
- `circuit_breaker_closed` - When circuit breaker resets
- `llm_token_usage` - Token consumption per request (NEW)
- `llm_daily_token_summary` - Daily token aggregation (NEW)

**Properties**:
```java
{
  provider: String,              // "gemini" | "openrouter"
  model: String,                 // "gemini-2.5-flash" | "qwen/qwen-2.5-72b-instruct"
  promptTokens: Integer,         // Input tokens consumed
  completionTokens: Integer,     // Output tokens generated
  totalTokens: Integer,          // Total tokens (prompt + completion)
  latency: Long,                 // Response time in ms
  estimatedCost: Double,         // Estimated cost in USD
  retryCount: Integer,           // Number of retries
  errorType: String,             // Error type if failed
  agentType: String,             // Which agent made the call
  itineraryId: String,           // Associated itinerary
  requestType: String,           // "structured" | "unstructured"
  timestamp: Instant
}
```

**Implementation**:
```java
// In ResilientAiClient.java
long startTime = System.currentTimeMillis();

// Make API call
String result = provider.generateContent(userPrompt, systemPrompt);

long latency = System.currentTimeMillis() - startTime;

// Extract token usage from response
TokenUsage tokenUsage = extractTokenUsage(response);

// Calculate estimated cost
double estimatedCost = calculateCost(
    tokenUsage.getPromptTokens(),
    tokenUsage.getCompletionTokens(),
    provider,
    model
);

// Track token usage
analyticsService.trackEvent(new AnalyticsEvent(
    "llm_token_usage",
    null,
    Map.of(
        "provider", providerName,
        "model", model,
        "promptTokens", tokenUsage.getPromptTokens(),
        "completionTokens", tokenUsage.getCompletionTokens(),
        "totalTokens", tokenUsage.getTotalTokens(),
        "latency", latency,
        "estimatedCost", estimatedCost,
        "agentType", agentType,
        "itineraryId", itineraryId,
        "requestType", jsonSchema != null ? "structured" : "unstructured"
    )
));
```


#### 3.3 Pipeline Orchestration
**Location**: `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`
**Events to Track**:
- `pipeline_started` - When orchestration begins
- `pipeline_phase_started` - Each phase (skeleton, population, enrichment, cost)
- `pipeline_phase_completed` - Phase completion
- `pipeline_completed` - Full pipeline success
- `pipeline_failed` - Pipeline failure

**Properties**:
```java
{
  itineraryId: String,
  phase: String,
  duration: Long,
  agentsExecuted: Integer,
  nodesGenerated: Integer,
  timestamp: Instant
}
```

### Level 4: Feature Usage Analytics (LOW PRIORITY)

#### 4.1 Budget Tracking
**Location**: `frontend/src/components/trip/tabs/BudgetTab.tsx`
**Events to Track**:
- `budget_tab_viewed` - When budget tab is opened
- `budget_chart_interacted` - When user interacts with charts
- `budget_filter_applied` - When category filters are used
- `budget_exported` - When budget is exported

**Properties**:
```typescript
{
  itineraryId: string,
  totalBudget: number,
  currency: string,
  categoryCount: number,
  timestamp: ISO string
}
```

#### 4.2 Map Interactions
**Location**: `frontend/src/components/map/` (map components)
**Events to Track**:
- `map_viewed` - When map is displayed
- `map_marker_clicked` - When location marker is clicked
- `map_zoomed` - When user zooms in/out
- `map_bounds_changed` - When visible area changes

**Properties**:
```typescript
{
  itineraryId: string,
  markerCount: number,
  zoomLevel: number,
  center: { lat: number, lng: number },
  timestamp: ISO string
}
```

#### 4.3 Weather & Tools
**Location**: `frontend/src/components/weather/` and tool components
**Events to Track**:
- `weather_viewed` - When weather widget is displayed
- `packing_list_generated` - When packing list tool is used
- `photo_spots_viewed` - When photo spots are displayed
- `food_recommendations_viewed` - When food suggestions are shown

**Properties**:
```typescript
{
  itineraryId: string,
  toolType: string,
  destination: string,
  timestamp: ISO string
}
```

#### 4.4 Mobile Navigation
**Location**: `frontend/src/components/layout/BottomNav.tsx`
**Events to Track**:
- `bottom_nav_clicked` - When bottom nav item is clicked
- `mobile_menu_opened` - When mobile menu is opened
- `mobile_menu_closed` - When mobile menu is closed

**Properties**:
```typescript
{
  destination: string,
  previousPage: string,
  deviceType: 'mobile' | 'tablet',
  timestamp: ISO string
}
```


## Backend Implementation

### 1. Create Analytics Service

**File**: `src/main/java/com/tripplanner/service/AnalyticsService.java`

```java
package com.tripplanner.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.tripplanner.dto.AnalyticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String ANALYTICS_COLLECTION = "analytics";
    private static final String EVENTS_SUBCOLLECTION = "events";
    private static final String USER_METRICS_SUBCOLLECTION = "user_metrics";
    
    private final Firestore firestore;
    
    public AnalyticsService(Firestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Track an analytics event
     */
    public void trackEvent(AnalyticsEvent event) {
        try {
            String eventId = UUID.randomUUID().toString();
            
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventName", event.getEventName());
            eventData.put("userId", event.getUserId());
            eventData.put("timestamp", Timestamp.now());
            eventData.put("properties", event.getProperties());
            eventData.put("sessionId", event.getSessionId());
            eventData.put("userAgent", event.getUserAgent());
            
            // Save to Firestore
            firestore.collection(ANALYTICS_COLLECTION)
                    .document(EVENTS_SUBCOLLECTION)
                    .collection("all")
                    .document(eventId)
                    .set(eventData)
                    .get();
            
            // Update user metrics asynchronously
            if (event.getUserId() != null) {
                updateUserMetrics(event.getUserId(), event.getEventName());
            }
            
            logger.debug("Analytics event tracked: {}", event.getEventName());
            
        } catch (Exception e) {
            logger.error("Failed to track analytics event: {}", event.getEventName(), e);
            // Don't throw - analytics failures shouldn't break the app
        }
    }
    
    /**
     * Update user-level metrics
     */
    private void updateUserMetrics(String userId, String eventName) {
        try {
            DocumentReference userMetricsRef = firestore
                    .collection(ANALYTICS_COLLECTION)
                    .document(USER_METRICS_SUBCOLLECTION)
                    .collection("users")
                    .document(userId);
            
            // Increment counters
            Map<String, Object> updates = new HashMap<>();
            updates.put("totalEvents", com.google.cloud.firestore.FieldValue.increment(1));
            updates.put("lastActive", Timestamp.now());
            
            // Event-specific counters
            if (eventName.contains("itinerary")) {
                updates.put("totalItineraries", com.google.cloud.firestore.FieldValue.increment(1));
            }
            if (eventName.contains("booking")) {
                updates.put("totalBookings", com.google.cloud.firestore.FieldValue.increment(1));
            }
            
            userMetricsRef.set(updates, com.google.cloud.firestore.SetOptions.merge()).get();
            
        } catch (Exception e) {
            logger.error("Failed to update user metrics for user: {}", userId, e);
        }
    }
    
    /**
     * Track conversion event (booking, payment, etc.)
     */
    public void trackConversion(String userId, String conversionType, Map<String, Object> properties) {
        AnalyticsEvent event = new AnalyticsEvent(
                "conversion_" + conversionType,
                userId,
                properties
        );
        trackEvent(event);
    }
}
```

### 2. Create Analytics DTOs

**File**: `src/main/java/com/tripplanner/dto/AnalyticsEvent.java`

```java
package com.tripplanner.dto;

import java.time.Instant;
import java.util.Map;

public class AnalyticsEvent {
    private String eventName;
    private String userId;
    private Map<String, Object> properties;
    private String sessionId;
    private String userAgent;
    private Instant timestamp;
    
    public AnalyticsEvent(String eventName, String userId, Map<String, Object> properties) {
        this.eventName = eventName;
        this.userId = userId;
        this.properties = properties;
        this.timestamp = Instant.now();
    }
    
    // Getters and setters
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
```


### 3. Update Analytics Controller

**File**: `src/main/java/com/tripplanner/controller/AnalyticsController.java`

**Changes Required**:
1. Inject `AnalyticsService`
2. Replace logging with actual persistence
3. Add authentication to extract userId

```java
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;
    
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    @PostMapping("/events")
    public ResponseEntity<AnalyticsEventResponse> trackEvent(
            @Valid @RequestBody AnalyticsEventRequest request,
            @RequestAttribute(value = "userId", required = false) String userId) {
        
        // Create analytics event
        AnalyticsEvent event = new AnalyticsEvent(
            request.event(),
            userId != null ? userId : request.userId(),
            request.properties()
        );
        event.setUserAgent(request.userAgent());
        
        // Track the event
        analyticsService.trackEvent(event);
        
        AnalyticsEventResponse response = new AnalyticsEventResponse(
            true,
            "Event tracked successfully",
            Instant.now()
        );
        
        return ResponseEntity.ok(response);
    }
    
    // Add new endpoint for conversion tracking
    @PostMapping("/conversions")
    public ResponseEntity<AnalyticsEventResponse> trackConversion(
            @Valid @RequestBody ConversionRequest request,
            @RequestAttribute(value = "userId", required = false) String userId) {
        
        analyticsService.trackConversion(
            userId != null ? userId : request.userId(),
            request.conversionType(),
            request.properties()
        );
        
        return ResponseEntity.ok(new AnalyticsEventResponse(
            true,
            "Conversion tracked successfully",
            Instant.now()
        ));
    }
    
    // Add endpoint for querying analytics
    @GetMapping("/metrics/{userId}")
    public ResponseEntity<UserMetricsResponse> getUserMetrics(
            @PathVariable String userId) {
        
        // TODO: Implement metrics retrieval from Firestore
        return ResponseEntity.ok(new UserMetricsResponse(userId));
    }
}
```

### 4. Add Analytics to Agent Execution

**File**: `src/main/java/com/tripplanner/agents/BaseAgent.java`

**Add to execute method**:
```java
protected final <T> T execute(String itineraryId, Object request) {
    String executionId = UUID.randomUUID().toString();
    long startTime = System.currentTimeMillis();
    
    try {
        // Track agent started
        analyticsService.trackEvent(new AnalyticsEvent(
            "agent_started",
            null,
            Map.of(
                "agentType", this.getClass().getSimpleName(),
                "itineraryId", itineraryId,
                "executionId", executionId
            )
        ));
        
        // Execute agent logic
        T result = executeInternal(itineraryId, request);
        
        // Track agent completed
        long duration = System.currentTimeMillis() - startTime;
        analyticsService.trackEvent(new AnalyticsEvent(
            "agent_completed",
            null,
            Map.of(
                "agentType", this.getClass().getSimpleName(),
                "itineraryId", itineraryId,
                "executionId", executionId,
                "duration", duration
            )
        ));
        
        return result;
        
    } catch (Exception e) {
        // Track agent failed
        analyticsService.trackEvent(new AnalyticsEvent(
            "agent_failed",
            null,
            Map.of(
                "agentType", this.getClass().getSimpleName(),
                "itineraryId", itineraryId,
                "executionId", executionId,
                "errorType", e.getClass().getSimpleName()
            )
        ));
        throw e;
    }
}
```


## Frontend Implementation

### 1. Initialize Analytics on App Start

**File**: `frontend/src/App.tsx`

```typescript
import { analytics } from '@/services/analytics';
import { useAuth } from '@/contexts/AuthContext';

function App() {
  const { user } = useAuth();
  
  useEffect(() => {
    // Initialize analytics
    const trackingId = import.meta.env.VITE_GA_TRACKING_ID;
    if (trackingId) {
      analytics.initialize(trackingId);
    }
    
    // Identify user when authenticated
    if (user) {
      analytics.identify(user.uid, {
        email: user.email,
        displayName: user.displayName
      });
    }
  }, [user]);
  
  // Rest of App component
}
```

### 2. Add Page View Tracking

**File**: Create `frontend/src/hooks/usePageTracking.ts`

```typescript
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { analytics } from '@/services/analytics';

export function usePageTracking() {
  const location = useLocation();
  
  useEffect(() => {
    analytics.page(location.pathname, document.title);
  }, [location]);
}
```

**Usage in App.tsx**:
```typescript
function App() {
  usePageTracking(); // Add this line
  
  return (
    // ... routes
  );
}
```

### 3. Track Trip Creation

**File**: `frontend/src/components/ai-planner/PremiumTripWizard.tsx`

**Add tracking at Line ~80**:
```typescript
const handleSubmit = async () => {
  setIsSubmitting(true);
  
  try {
    // Track trip creation initiated
    analytics.trackAITrip({
      destination: formData.destination,
      duration: calculateDuration(formData.startDate, formData.endDate),
      travelers: formData.party.adults + formData.party.children,
      budget: formData.budget
    });
    
    const response = await api.post<any>(endpoints.createItinerary, {
      // ... request data
    });
    
    // Track trip creation completed
    analytics.track('trip_creation_completed', {
      itineraryId: response.data.itineraryId,
      executionId: response.data.executionId,
      timestamp: new Date().toISOString()
    });
    
    navigate(`/trip/${response.data.itineraryId}`);
    
  } catch (error) {
    // Track trip creation failed
    analytics.track('trip_creation_failed', {
      error: error.message,
      timestamp: new Date().toISOString()
    });
    
    console.error('Failed to create itinerary:', error);
  } finally {
    setIsSubmitting(false);
  }
};
```

### 4. Track Booking Interactions

**File**: `frontend/src/components/booking/BookingCategoryCard.tsx`

**Add tracking to booking button**:
```typescript
const handleBookNow = (booking: CategorizedBooking) => {
  // Track booking initiated
  analytics.trackBooking('initiated', {
    provider: 'easemytrip',
    vertical: booking.category,
    itineraryId: itineraryId,
    amount: booking.cost?.amount,
    currency: booking.cost?.currency
  });
  
  // Open booking URL
  const bookingUrl = buildEaseMyTripUrl(booking, itinerary);
  window.open(bookingUrl, '_blank');
  
  // Track iframe loaded (if using iframe)
  analytics.trackBooking('iframe_loaded', {
    provider: 'easemytrip',
    vertical: booking.category,
    itineraryId: itineraryId
  });
};
```

### 5. Track Authentication Events

**File**: `frontend/src/pages/LoginPage.tsx`

**Add tracking at Line ~39**:
```typescript
const handleGoogleSignIn = async () => {
  try {
    // Track login started
    analytics.track('user_login_started', {
      method: 'google',
      timestamp: new Date().toISOString()
    });
    
    await signInWithGoogle();
    
    // Track login completed
    analytics.track('user_login_completed', {
      method: 'google',
      timestamp: new Date().toISOString()
    });
    
    navigate(from, { replace: true });
    
  } catch (error) {
    // Track login failed
    analytics.track('user_login_failed', {
      method: 'google',
      error: error.message,
      timestamp: new Date().toISOString()
    });
    
    console.error('Google sign-in failed:', error);
  }
};
```

### 6. Track Export Actions

**File**: `frontend/src/components/export/ExportOptionsModal.tsx`

**Add tracking to export functions**:
```typescript
const handlePdfExport = async () => {
  // Track PDF export initiated
  analytics.track('pdf_export_initiated', {
    itineraryId,
    format: 'pdf',
    timestamp: new Date().toISOString()
  });
  
  try {
    const pdfBlob = await exportService.generatePdf(itinerary);
    
    // Track PDF export completed
    analytics.track('pdf_export_completed', {
      itineraryId,
      format: 'pdf',
      fileSize: pdfBlob.size,
      timestamp: new Date().toISOString()
    });
    
    // Download PDF
    downloadBlob(pdfBlob, `itinerary-${itineraryId}.pdf`);
    
  } catch (error) {
    // Track PDF export failed
    analytics.track('pdf_export_failed', {
      itineraryId,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
};
```


### 7. Track Activity Interactions

**File**: `frontend/src/components/trip/DayCard.tsx`

**Add tracking to activity interactions**:
```typescript
const handleActivityClick = (node: NormalizedNode) => {
  analytics.track('activity_viewed', {
    itineraryId,
    dayNumber: day.dayNumber,
    nodeId: node.id,
    nodeType: node.type,
    timestamp: new Date().toISOString()
  });
  
  setExpandedActivity(node.id);
};

const handleActivityEdit = (node: NormalizedNode) => {
  analytics.track('activity_edited', {
    itineraryId,
    dayNumber: day.dayNumber,
    nodeId: node.id,
    nodeType: node.type,
    timestamp: new Date().toISOString()
  });
  
  // Open edit modal
};

const handleActivityDelete = (nodeId: string) => {
  analytics.track('activity_deleted', {
    itineraryId,
    dayNumber: day.dayNumber,
    nodeId,
    timestamp: new Date().toISOString()
  });
  
  // Delete activity
};
```

### 8. Track Search Events

**File**: `frontend/src/pages/SearchPage.tsx`

**Add tracking to search interactions**:
```typescript
const handleSearch = (searchParams: SearchParams) => {
  analytics.trackSearch({
    searchType: searchParams.type,
    origin: searchParams.origin,
    destination: searchParams.destination,
    startDate: searchParams.startDate,
    endDate: searchParams.endDate,
    travelers: searchParams.travelers
  });
  
  // Perform search
};
```

## Configuration

### Environment Variables

**Backend** - Add to `application.yml`:
```yaml
analytics:
  enabled: ${ANALYTICS_ENABLED:true}
  batch-size: ${ANALYTICS_BATCH_SIZE:100}
  flush-interval-seconds: ${ANALYTICS_FLUSH_INTERVAL:60}
  retention-days: ${ANALYTICS_RETENTION_DAYS:90}
```

**Frontend** - Add to `.env`:
```bash
# Google Analytics
VITE_GA_TRACKING_ID=G-XXXXXXXXXX
VITE_ENABLE_ANALYTICS=true

# Backend Analytics
VITE_ANALYTICS_ENDPOINT=/api/v1/analytics/events
```

**Update** `.env.example`:
```bash
# Analytics Configuration
ANALYTICS_ENABLED=true
ANALYTICS_BATCH_SIZE=100
ANALYTICS_FLUSH_INTERVAL=60
ANALYTICS_RETENTION_DAYS=90

# Google Analytics (Frontend)
VITE_GA_TRACKING_ID=
VITE_ENABLE_ANALYTICS=true
```

## Data Retention & Privacy

### GDPR Compliance

1. **User Consent**: Add cookie consent banner
2. **Data Anonymization**: Hash user IDs for long-term storage
3. **Right to Deletion**: Implement user data deletion endpoint
4. **Data Export**: Allow users to export their analytics data

### Retention Policy

**File**: `src/main/java/com/tripplanner/service/AnalyticsCleanupService.java`

```java
@Service
public class AnalyticsCleanupService {
    
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void cleanupOldAnalytics() {
        // Delete events older than retention period
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        
        // Query and delete old events
        firestore.collection("analytics")
                .document("events")
                .collection("all")
                .whereLessThan("timestamp", Timestamp.ofTimeSecondsAndNanos(
                    cutoffDate.getEpochSecond(), 
                    cutoffDate.getNano()
                ))
                .get()
                .get()
                .getDocuments()
                .forEach(doc -> doc.getReference().delete());
    }
}
```


## Analytics Dashboard & Reporting

### Dashboard Endpoints

**File**: `src/main/java/com/tripplanner/controller/AnalyticsDashboardController.java`

```java
@RestController
@RequestMapping("/api/v1/analytics/dashboard")
public class AnalyticsDashboardController {
    
    private final AnalyticsService analyticsService;
    
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverview> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        // Return aggregated metrics
        return ResponseEntity.ok(analyticsService.getDashboardOverview(startDate, endDate));
    }
    
    @GetMapping("/conversions")
    public ResponseEntity<ConversionMetrics> getConversionMetrics() {
        return ResponseEntity.ok(analyticsService.getConversionMetrics());
    }
    
    @GetMapping("/user-engagement")
    public ResponseEntity<EngagementMetrics> getEngagementMetrics() {
        return ResponseEntity.ok(analyticsService.getEngagementMetrics());
    }
    
    @GetMapping("/agent-performance")
    public ResponseEntity<AgentPerformanceMetrics> getAgentPerformance() {
        return ResponseEntity.ok(analyticsService.getAgentPerformance());
    }
}
```

### Key Metrics to Display

1. **Business Metrics**
   - Total users (daily, weekly, monthly)
   - New signups
   - Trip creation rate
   - Booking conversion rate
   - Revenue (if applicable)

2. **Engagement Metrics**
   - Daily active users (DAU)
   - Monthly active users (MAU)
   - Average session duration
   - Pages per session
   - Bounce rate

3. **AI Performance Metrics**
   - Average trip generation time
   - Agent success rate
   - LLM API latency
   - Error rate by agent type
   - Cost per trip generation

4. **Feature Usage**
   - Most used features
   - Export frequency
   - Chat usage
   - Search patterns
   - Mobile vs desktop usage

## Implementation Priority

### Phase 1: Critical Tracking (Week 1-2)
**Priority**: HIGH
**Effort**: Medium

1. ✅ Create `AnalyticsService.java`
2. ✅ Create `AnalyticsEvent.java` DTO
3. ✅ Update `AnalyticsController.java`
4. ✅ Add page view tracking (all pages)
5. ✅ Track trip creation flow
6. ✅ Track booking conversions
7. ✅ Track authentication events

**Files to Modify**:
- `src/main/java/com/tripplanner/service/AnalyticsService.java` (NEW)
- `src/main/java/com/tripplanner/dto/AnalyticsEvent.java` (NEW)
- `src/main/java/com/tripplanner/controller/AnalyticsController.java` (UPDATE)
- `frontend/src/App.tsx` (UPDATE)
- `frontend/src/hooks/usePageTracking.ts` (NEW)
- `frontend/src/components/ai-planner/PremiumTripWizard.tsx` (UPDATE)
- `frontend/src/components/booking/BookingCategoryCard.tsx` (UPDATE)
- `frontend/src/pages/LoginPage.tsx` (UPDATE)

### Phase 2: Engagement Tracking (Week 3-4)
**Priority**: MEDIUM
**Effort**: Medium

1. Track itinerary interactions
2. Track chat usage
3. Track export/share actions
4. Track search behavior
5. Add session tracking

**Files to Modify**:
- `frontend/src/components/trip/DayCard.tsx` (UPDATE)
- `frontend/src/components/chat/*` (UPDATE)
- `frontend/src/components/export/ExportOptionsModal.tsx` (UPDATE)
- `frontend/src/pages/SearchPage.tsx` (UPDATE)
- `frontend/src/services/analytics.ts` (UPDATE - add session tracking)

### Phase 3: AI Performance Tracking (Week 5-6)
**Priority**: MEDIUM
**Effort**: High

1. Track agent execution metrics
2. Track LLM API performance
3. Track pipeline orchestration
4. Add error tracking
5. Add cost tracking

**Files to Modify**:
- `src/main/java/com/tripplanner/agents/BaseAgent.java` (UPDATE)
- `src/main/java/com/tripplanner/service/ai/ResilientAiClient.java` (UPDATE)
- `src/main/java/com/tripplanner/service/PipelineOrchestrator.java` (UPDATE)
- `src/main/java/com/tripplanner/service/ai/CircuitBreaker.java` (UPDATE)

### Phase 4: Dashboard & Reporting (Week 7-8)
**Priority**: LOW
**Effort**: High

1. Create analytics dashboard endpoints
2. Build aggregation service
3. Create frontend dashboard
4. Add real-time metrics
5. Add export capabilities

**Files to Create**:
- `src/main/java/com/tripplanner/controller/AnalyticsDashboardController.java` (NEW)
- `src/main/java/com/tripplanner/service/AnalyticsAggregationService.java` (NEW)
- `frontend/src/pages/AnalyticsDashboardPage.tsx` (NEW)
- `frontend/src/components/analytics/*` (NEW)


## Testing Strategy

### Unit Tests

**Backend Tests**:
```java
// src/test/java/com/tripplanner/service/AnalyticsServiceTest.java
@SpringBootTest
class AnalyticsServiceTest {
    
    @Autowired
    private AnalyticsService analyticsService;
    
    @Test
    void testTrackEvent() {
        AnalyticsEvent event = new AnalyticsEvent(
            "test_event",
            "user123",
            Map.of("key", "value")
        );
        
        assertDoesNotThrow(() -> analyticsService.trackEvent(event));
    }
    
    @Test
    void testTrackConversion() {
        assertDoesNotThrow(() -> 
            analyticsService.trackConversion(
                "user123",
                "booking",
                Map.of("amount", 100.0)
            )
        );
    }
}
```

**Frontend Tests**:
```typescript
// frontend/src/services/__tests__/analytics.test.ts
import { analytics } from '../analytics';

describe('Analytics Service', () => {
  it('should track events', () => {
    const spy = jest.spyOn(analytics, 'track');
    
    analytics.track('test_event', { key: 'value' });
    
    expect(spy).toHaveBeenCalledWith('test_event', { key: 'value' });
  });
  
  it('should track page views', () => {
    const spy = jest.spyOn(analytics, 'page');
    
    analytics.page('/dashboard', 'Dashboard');
    
    expect(spy).toHaveBeenCalledWith('/dashboard', 'Dashboard');
  });
});
```

### Integration Tests

**Test Analytics Flow**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testTrackEventEndpoint() throws Exception {
        String eventJson = """
            {
                "event": "test_event",
                "userId": "user123",
                "properties": {
                    "key": "value"
                },
                "timestamp": "2024-01-01T00:00:00Z"
            }
            """;
        
        mockMvc.perform(post("/api/v1/analytics/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

## Monitoring & Alerts

### Key Metrics to Monitor

1. **Analytics System Health**
   - Event ingestion rate
   - Event processing latency
   - Failed event rate
   - Storage usage

2. **Business Alerts**
   - Conversion rate drops below threshold
   - Signup rate anomalies
   - Booking failures spike
   - Payment failures increase

3. **Technical Alerts**
   - Analytics API errors
   - Firestore write failures
   - Google Analytics connection issues
   - High latency in event processing

### Alert Configuration

**File**: `src/main/resources/alerts.yml`

```yaml
alerts:
  analytics:
    - name: high_error_rate
      condition: error_rate > 5%
      window: 5m
      severity: warning
      
    - name: low_conversion_rate
      condition: conversion_rate < 2%
      window: 1h
      severity: critical
      
    - name: high_latency
      condition: p95_latency > 1000ms
      window: 5m
      severity: warning
```

## Cost Optimization

### Firestore Optimization

1. **Batch Writes**: Group multiple events into batch writes
2. **Indexing**: Only index fields used in queries
3. **TTL**: Implement automatic deletion of old events
4. **Aggregation**: Pre-aggregate metrics to reduce query costs

**Example Batch Write**:
```java
@Service
public class AnalyticsBatchService {
    
    private final List<AnalyticsEvent> eventBuffer = new ArrayList<>();
    private final int BATCH_SIZE = 100;
    
    public synchronized void addEvent(AnalyticsEvent event) {
        eventBuffer.add(event);
        
        if (eventBuffer.size() >= BATCH_SIZE) {
            flushEvents();
        }
    }
    
    private void flushEvents() {
        WriteBatch batch = firestore.batch();
        
        for (AnalyticsEvent event : eventBuffer) {
            DocumentReference docRef = firestore
                .collection("analytics")
                .document("events")
                .collection("all")
                .document();
            
            batch.set(docRef, event);
        }
        
        batch.commit();
        eventBuffer.clear();
    }
    
    @Scheduled(fixedDelay = 60000) // Flush every minute
    public void scheduledFlush() {
        if (!eventBuffer.isEmpty()) {
            flushEvents();
        }
    }
}
```

### Google Analytics Optimization

1. **Event Sampling**: Sample high-volume events
2. **Custom Dimensions**: Use custom dimensions efficiently
3. **Event Limits**: Stay within GA4 event limits (500 distinct events)


## Token Analytics Overview

### Key Features
- **Real-time Token Tracking**: Track every LLM API call
- **Cost Calculation**: Automatic cost estimation based on provider pricing
- **Daily/Monthly Aggregation**: Summarize usage over time
- **Cost Projections**: Predict monthly costs based on current usage
- **Provider Comparison**: Compare Gemini vs OpenRouter costs
- **Agent Analysis**: Identify which agents consume most tokens
- **Cost Alerts**: Automated alerts when thresholds are exceeded
- **Optimization Recommendations**: Suggest ways to reduce costs

### Token Tracking Locations
1. **GeminiClient.java** - Extract token usage from Gemini API responses
2. **OpenRouterClient.java** - Extract token usage from OpenRouter API responses
3. **ResilientAiClient.java** - Track token usage across provider failovers
4. **AnalyticsService.java** - Persist token usage to Firestore
5. **TokenAnalyticsController.java** - Expose token analytics via REST API
6. **TokenAnalyticsDashboardPage.tsx** - Display token metrics in UI

## Complete File Reference

### Backend Files

#### New Files to Create
1. `src/main/java/com/tripplanner/service/AnalyticsService.java` - Core analytics service
2. `src/main/java/com/tripplanner/dto/AnalyticsEvent.java` - Event DTO
3. `src/main/java/com/tripplanner/service/AnalyticsBatchService.java` - Batch processing
4. `src/main/java/com/tripplanner/service/AnalyticsCleanupService.java` - Data retention
5. `src/main/java/com/tripplanner/controller/AnalyticsDashboardController.java` - Dashboard API
6. `src/main/java/com/tripplanner/service/AnalyticsAggregationService.java` - Metrics aggregation
7. `src/main/java/com/tripplanner/dto/TokenUsage.java` - Token usage DTO (NEW)
8. `src/main/java/com/tripplanner/service/TokenCostCalculator.java` - Cost calculation (NEW)
9. `src/main/java/com/tripplanner/util/TokenEstimator.java` - Token estimation (NEW)
10. `src/main/java/com/tripplanner/controller/TokenAnalyticsController.java` - Token API (NEW)
11. `src/main/java/com/tripplanner/service/TokenAnalyticsService.java` - Token analytics (NEW)
12. `src/main/java/com/tripplanner/service/TokenCostAlertService.java` - Cost alerts (NEW)

#### Files to Modify
1. `src/main/java/com/tripplanner/controller/AnalyticsController.java` - Add persistence
2. `src/main/java/com/tripplanner/agents/BaseAgent.java` - Add agent tracking
3. `src/main/java/com/tripplanner/service/ai/ResilientAiClient.java` - Add LLM tracking
4. `src/main/java/com/tripplanner/service/PipelineOrchestrator.java` - Add pipeline tracking
5. `src/main/java/com/tripplanner/service/RazorpayService.java` - Add payment tracking
6. `src/main/resources/application.yml` - Add analytics configuration
7. `src/main/java/com/tripplanner/service/GeminiClient.java` - Add token extraction (NEW)
8. `src/main/java/com/tripplanner/service/openrouter/OpenRouterClient.java` - Add token extraction (NEW)

### Frontend Files

#### New Files to Create
1. `frontend/src/hooks/usePageTracking.ts` - Page view tracking hook
2. `frontend/src/hooks/useAnalytics.ts` - Analytics hook
3. `frontend/src/pages/AnalyticsDashboardPage.tsx` - Admin dashboard
4. `frontend/src/components/analytics/MetricsCard.tsx` - Metrics display
5. `frontend/src/components/analytics/ConversionFunnel.tsx` - Funnel visualization
6. `frontend/src/components/analytics/EventTimeline.tsx` - Event timeline

#### Files to Modify
1. `frontend/src/App.tsx` - Initialize analytics
2. `frontend/src/services/analytics.ts` - Enhance analytics service
3. `frontend/src/components/ai-planner/PremiumTripWizard.tsx` - Track trip creation
4. `frontend/src/components/booking/BookingCategoryCard.tsx` - Track bookings
5. `frontend/src/pages/LoginPage.tsx` - Track authentication
6. `frontend/src/pages/SignupPage.tsx` - Track signups
7. `frontend/src/components/export/ExportOptionsModal.tsx` - Track exports
8. `frontend/src/components/trip/DayCard.tsx` - Track interactions
9. `frontend/src/pages/SearchPage.tsx` - Track searches
10. `frontend/src/components/layout/BottomNav.tsx` - Track navigation

### Configuration Files

#### Files to Modify
1. `.env.example` - Add analytics environment variables
2. `src/main/resources/application.yml` - Add analytics configuration
3. `frontend/.env.example` - Add frontend analytics config

## Sample Queries

### Firestore Queries for Analytics

**Get events by user**:
```javascript
db.collection('analytics')
  .doc('events')
  .collection('all')
  .where('userId', '==', 'user123')
  .orderBy('timestamp', 'desc')
  .limit(100)
  .get();
```

**Get conversion rate**:
```javascript
// Get total trip creations
const tripCreations = await db.collection('analytics')
  .doc('events')
  .collection('all')
  .where('eventName', '==', 'trip_creation_completed')
  .where('timestamp', '>=', startDate)
  .where('timestamp', '<=', endDate)
  .get();

// Get total bookings
const bookings = await db.collection('analytics')
  .doc('events')
  .collection('all')
  .where('eventName', '==', 'booking_completed')
  .where('timestamp', '>=', startDate)
  .where('timestamp', '<=', endDate)
  .get();

const conversionRate = (bookings.size / tripCreations.size) * 100;
```

**Get user metrics**:
```javascript
const userMetrics = await db.collection('analytics')
  .doc('user_metrics')
  .collection('users')
  .doc('user123')
  .get();
```

## Best Practices

### 1. Event Naming Convention
- Use snake_case: `trip_creation_completed`
- Be specific: `booking_flight_initiated` not `booking_started`
- Include action: `button_clicked`, `form_submitted`, `page_viewed`
- Group related events: `payment_*`, `booking_*`, `agent_*`

### 2. Property Naming
- Use camelCase for properties: `itineraryId`, `userId`
- Include units: `durationMs`, `amountUsd`
- Be consistent across events
- Avoid nested objects (flatten when possible)

### 3. Performance
- Track asynchronously (don't block user actions)
- Batch events when possible
- Use sampling for high-volume events
- Implement circuit breakers for analytics failures

### 4. Privacy
- Hash PII before storing
- Implement user consent
- Provide data export/deletion
- Follow GDPR/CCPA guidelines

### 5. Error Handling
- Never throw errors from analytics code
- Log analytics failures separately
- Implement retry logic with exponential backoff
- Have fallback mechanisms

## Success Metrics

### KPIs to Track

1. **User Acquisition**
   - New signups per day/week/month
   - Signup conversion rate
   - Traffic sources
   - Cost per acquisition (if running ads)

2. **User Engagement**
   - Daily/Monthly active users
   - Session duration
   - Pages per session
   - Feature adoption rate

3. **Business Metrics**
   - Trip creation rate
   - Booking conversion rate
   - Revenue per user
   - Customer lifetime value

4. **Technical Metrics**
   - Trip generation success rate
   - Average generation time
   - Agent error rate
   - API latency

5. **Product Metrics**
   - Feature usage frequency
   - User retention rate
   - Churn rate
   - Net Promoter Score (NPS)

## Conclusion

This comprehensive analytics implementation will provide deep insights into:
- User behavior and engagement patterns
- Business performance and conversion metrics
- AI agent performance and reliability
- Feature usage and adoption
- Technical performance and errors

The phased approach ensures critical tracking is implemented first, with more advanced features added progressively. The Firestore-based storage provides flexibility, scalability, and real-time capabilities while keeping costs manageable.

---

**Document Version**: 1.0  
**Last Updated**: November 18, 2025  
**Status**: Ready for Implementation  
**Estimated Effort**: 6-8 weeks (4 phases)


## Token Consumption Analytics (Detailed Implementation)

### Overview
Token consumption tracking is critical for:
- **Cost Management**: Monitor and optimize LLM API costs
- **Performance Optimization**: Identify expensive operations
- **Budget Forecasting**: Predict monthly costs
- **Provider Comparison**: Compare cost-effectiveness across providers
- **Agent Optimization**: Identify which agents consume most tokens

### Token Tracking Architecture

```
LLM API Call
    ↓
Extract Token Usage from Response
    ↓
Calculate Estimated Cost
    ↓
Track to Analytics
    ↓
├─→ Real-time Event (Firestore)
├─→ Daily Aggregation (Firestore)
└─→ Monthly Summary (Firestore)
```

### 1. Token Usage DTO

**File**: `src/main/java/com/tripplanner/dto/TokenUsage.java` (NEW)

```java
package com.tripplanner.dto;

/**
 * DTO for tracking token usage from LLM API responses.
 */
public class TokenUsage {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private String provider;
    private String model;
    
    public TokenUsage() {}
    
    public TokenUsage(int promptTokens, int completionTokens, String provider, String model) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = promptTokens + completionTokens;
        this.provider = provider;
        this.model = model;
    }
    
    // Getters and setters
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { 
        this.promptTokens = promptTokens;
        this.totalTokens = promptTokens + completionTokens;
    }
    
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { 
        this.completionTokens = completionTokens;
        this.totalTokens = promptTokens + completionTokens;
    }
    
    public int getTotalTokens() { return totalTokens; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    /**
     * Estimate cost based on provider pricing.
     * Prices as of November 2024 (update regularly).
     */
    public double estimateCost() {
        return TokenCostCalculator.calculateCost(this);
    }
}
```

### 2. Token Cost Calculator

**File**: `src/main/java/com/tripplanner/service/TokenCostCalculator.java` (NEW)

```java
package com.tripplanner.service;

import com.tripplanner.dto.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Calculates estimated costs for LLM API token usage.
 * Pricing is based on provider rate cards (as of November 2024).
 */
@Service
public class TokenCostCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenCostCalculator.class);
    
    // Pricing per 1M tokens (USD)
    // Update these regularly from provider pricing pages
    private static final Map<String, ModelPricing> PRICING = Map.of(
        // Google Gemini pricing
        "gemini-2.5-flash", new ModelPricing(0.075, 0.30),      // $0.075 input, $0.30 output per 1M tokens
        "gemini-1.5-pro", new ModelPricing(1.25, 5.00),         // $1.25 input, $5.00 output per 1M tokens
        "gemini-1.5-flash", new ModelPricing(0.075, 0.30),      // $0.075 input, $0.30 output per 1M tokens
        
        // OpenRouter pricing (varies by model)
        "qwen/qwen-2.5-72b-instruct:free", new ModelPricing(0.0, 0.0),  // Free tier
        "qwen/qwen-2.5-72b-instruct", new ModelPricing(0.35, 0.40),     // $0.35 input, $0.40 output per 1M tokens
        "anthropic/claude-3-opus", new ModelPricing(15.0, 75.0),        // $15 input, $75 output per 1M tokens
        "anthropic/claude-3-sonnet", new ModelPricing(3.0, 15.0),       // $3 input, $15 output per 1M tokens
        "openai/gpt-4-turbo", new ModelPricing(10.0, 30.0),             // $10 input, $30 output per 1M tokens
        "openai/gpt-3.5-turbo", new ModelPricing(0.50, 1.50)            // $0.50 input, $1.50 output per 1M tokens
    );
    
    /**
     * Calculate cost for token usage.
     */
    public static double calculateCost(TokenUsage usage) {
        String modelKey = usage.getModel();
        
        // Try exact match first
        ModelPricing pricing = PRICING.get(modelKey);
        
        // If not found, try to match by provider
        if (pricing == null) {
            pricing = getDefaultPricingForProvider(usage.getProvider());
        }
        
        if (pricing == null) {
            logger.warn("No pricing found for model: {} (provider: {}), using default", 
                       modelKey, usage.getProvider());
            pricing = new ModelPricing(1.0, 2.0); // Default fallback pricing
        }
        
        // Calculate cost: (tokens / 1,000,000) * price_per_million
        double inputCost = (usage.getPromptTokens() / 1_000_000.0) * pricing.inputPricePerMillion;
        double outputCost = (usage.getCompletionTokens() / 1_000_000.0) * pricing.outputPricePerMillion;
        
        return inputCost + outputCost;
    }
    
    /**
     * Get default pricing for a provider when specific model pricing is not available.
     */
    private static ModelPricing getDefaultPricingForProvider(String provider) {
        return switch (provider.toLowerCase()) {
            case "gemini" -> new ModelPricing(0.075, 0.30);  // Gemini Flash pricing
            case "openrouter" -> new ModelPricing(0.35, 0.40); // Mid-tier OpenRouter pricing
            default -> null;
        };
    }
    
    /**
     * Get pricing information for a specific model.
     */
    public static ModelPricing getPricing(String model) {
        return PRICING.get(model);
    }
    
    /**
     * Model pricing structure.
     */
    public static class ModelPricing {
        public final double inputPricePerMillion;
        public final double outputPricePerMillion;
        
        public ModelPricing(double inputPricePerMillion, double outputPricePerMillion) {
            this.inputPricePerMillion = inputPricePerMillion;
            this.outputPricePerMillion = outputPricePerMillion;
        }
        
        public double getTotalPricePerMillion(int promptTokens, int completionTokens) {
            int totalTokens = promptTokens + completionTokens;
            if (totalTokens == 0) return 0;
            
            double weightedPrice = 
                (promptTokens * inputPricePerMillion + completionTokens * outputPricePerMillion) / totalTokens;
            return weightedPrice;
        }
    }
}
```

### 3. Extract Token Usage from API Responses

**Update**: `src/main/java/com/tripplanner/service/GeminiClient.java`

```java
/**
 * Extract token usage from Gemini API response.
 */
private TokenUsage extractTokenUsage(JsonNode responseJson, String userPrompt, String systemPrompt) {
    try {
        // Gemini returns usage metadata in the response
        JsonNode usageMetadata = responseJson.get("usageMetadata");
        
        if (usageMetadata != null) {
            int promptTokens = usageMetadata.has("promptTokenCount") 
                ? usageMetadata.get("promptTokenCount").asInt() 
                : 0;
            int completionTokens = usageMetadata.has("candidatesTokenCount") 
                ? usageMetadata.get("candidatesTokenCount").asInt() 
                : 0;
            
            return new TokenUsage(promptTokens, completionTokens, "gemini", modelName);
        } else {
            // Fallback: estimate tokens if not provided
            logger.warn("No usage metadata in Gemini response, estimating tokens");
            return estimateTokenUsage(userPrompt, systemPrompt, "gemini", modelName);
        }
    } catch (Exception e) {
        logger.error("Failed to extract token usage from Gemini response", e);
        return estimateTokenUsage(userPrompt, systemPrompt, "gemini", modelName);
    }
}

// Add to attemptRequestWithKey method after parsing response:
TokenUsage tokenUsage = extractTokenUsage(responseJson, userPrompt, systemPrompt);
double estimatedCost = tokenUsage.estimateCost();

logger.info("Token usage - Prompt: {}, Completion: {}, Total: {}, Cost: ${}", 
           tokenUsage.getPromptTokens(),
           tokenUsage.getCompletionTokens(),
           tokenUsage.getTotalTokens(),
           String.format("%.6f", estimatedCost));

// Track token usage
if (analyticsService != null) {
    analyticsService.trackTokenUsage(tokenUsage, "GeminiClient", itineraryId);
}
```

**Update**: `src/main/java/com/tripplanner/service/openrouter/OpenRouterClient.java`

```java
/**
 * Extract token usage from OpenRouter API response.
 */
private TokenUsage extractTokenUsage(JsonNode responseJson, String userPrompt, String systemPrompt) {
    try {
        // OpenRouter returns usage in the response
        JsonNode usage = responseJson.get("usage");
        
        if (usage != null) {
            int promptTokens = usage.has("prompt_tokens") 
                ? usage.get("prompt_tokens").asInt() 
                : 0;
            int completionTokens = usage.has("completion_tokens") 
                ? usage.get("completion_tokens").asInt() 
                : 0;
            
            return new TokenUsage(promptTokens, completionTokens, "openrouter", modelName);
        } else {
            // Fallback: estimate tokens if not provided
            logger.warn("No usage metadata in OpenRouter response, estimating tokens");
            return estimateTokenUsage(userPrompt, systemPrompt, "openrouter", modelName);
        }
    } catch (Exception e) {
        logger.error("Failed to extract token usage from OpenRouter response", e);
        return estimateTokenUsage(userPrompt, systemPrompt, "openrouter", modelName);
    }
}

// Add to attemptContentGenerationWithKey method after parsing response:
TokenUsage tokenUsage = extractTokenUsage(root, userPrompt, systemPrompt);
double estimatedCost = tokenUsage.estimateCost();

logger.info("Token usage - Prompt: {}, Completion: {}, Total: {}, Cost: ${}", 
           tokenUsage.getPromptTokens(),
           tokenUsage.getCompletionTokens(),
           tokenUsage.getTotalTokens(),
           String.format("%.6f", estimatedCost));

// Track token usage
if (analyticsService != null) {
    analyticsService.trackTokenUsage(tokenUsage, "OpenRouterClient", itineraryId);
}
```

### 4. Token Estimation Utility

**File**: `src/main/java/com/tripplanner/util/TokenEstimator.java` (NEW)

```java
package com.tripplanner.util;

import com.tripplanner.dto.TokenUsage;

/**
 * Utility for estimating token counts when actual usage is not available.
 * Uses approximation: 1 token ≈ 4 characters for English text.
 */
public class TokenEstimator {
    
    private static final int CHARS_PER_TOKEN = 4;
    
    /**
     * Estimate token count from text.
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / (double) CHARS_PER_TOKEN);
    }
    
    /**
     * Estimate token usage for a request.
     */
    public static TokenUsage estimateTokenUsage(String userPrompt, String systemPrompt, 
                                                String provider, String model) {
        int promptTokens = estimateTokens(userPrompt);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            promptTokens += estimateTokens(systemPrompt);
        }
        
        // Estimate completion tokens as 0 (will be updated when response is received)
        return new TokenUsage(promptTokens, 0, provider, model);
    }
    
    /**
     * Estimate completion tokens from response text.
     */
    public static int estimateCompletionTokens(String responseText) {
        return estimateTokens(responseText);
    }
}
```

### 5. Analytics Service Token Tracking

**Update**: `src/main/java/com/tripplanner/service/AnalyticsService.java`

```java
/**
 * Track token usage for LLM API calls.
 */
public void trackTokenUsage(TokenUsage tokenUsage, String clientType, String itineraryId) {
    try {
        double estimatedCost = tokenUsage.estimateCost();
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("provider", tokenUsage.getProvider());
        properties.put("model", tokenUsage.getModel());
        properties.put("promptTokens", tokenUsage.getPromptTokens());
        properties.put("completionTokens", tokenUsage.getCompletionTokens());
        properties.put("totalTokens", tokenUsage.getTotalTokens());
        properties.put("estimatedCost", estimatedCost);
        properties.put("clientType", clientType);
        
        if (itineraryId != null) {
            properties.put("itineraryId", itineraryId);
        }
        
        AnalyticsEvent event = new AnalyticsEvent(
            "llm_token_usage",
            null,
            properties
        );
        
        trackEvent(event);
        
        // Update daily token aggregates
        updateDailyTokenAggregates(tokenUsage, estimatedCost);
        
        logger.debug("Tracked token usage: {} tokens, ${}", 
                    tokenUsage.getTotalTokens(), 
                    String.format("%.6f", estimatedCost));
        
    } catch (Exception e) {
        logger.error("Failed to track token usage", e);
    }
}

/**
 * Update daily token aggregates for cost tracking.
 */
private void updateDailyTokenAggregates(TokenUsage tokenUsage, double cost) {
    try {
        String today = LocalDate.now().toString(); // Format: YYYY-MM-DD
        
        DocumentReference dailyAggregateRef = firestore
                .collection(ANALYTICS_COLLECTION)
                .document("daily_token_aggregates")
                .collection("dates")
                .document(today);
        
        Map<String, Object> updates = new HashMap<>();
        
        // Increment token counters
        updates.put("totalTokens", FieldValue.increment(tokenUsage.getTotalTokens()));
        updates.put("promptTokens", FieldValue.increment(tokenUsage.getPromptTokens()));
        updates.put("completionTokens", FieldValue.increment(tokenUsage.getCompletionTokens()));
        updates.put("totalCost", FieldValue.increment(cost));
        
        // Increment provider-specific counters
        String providerKey = tokenUsage.getProvider() + "_tokens";
        updates.put(providerKey, FieldValue.increment(tokenUsage.getTotalTokens()));
        
        String providerCostKey = tokenUsage.getProvider() + "_cost";
        updates.put(providerCostKey, FieldValue.increment(cost));
        
        // Increment model-specific counters
        String modelKey = "models." + tokenUsage.getModel().replace("/", "_") + ".tokens";
        updates.put(modelKey, FieldValue.increment(tokenUsage.getTotalTokens()));
        
        String modelCostKey = "models." + tokenUsage.getModel().replace("/", "_") + ".cost";
        updates.put(modelCostKey, FieldValue.increment(cost));
        
        updates.put("lastUpdated", Timestamp.now());
        
        dailyAggregateRef.set(updates, SetOptions.merge()).get();
        
    } catch (Exception e) {
        logger.error("Failed to update daily token aggregates", e);
    }
}
```


### 6. Firestore Token Analytics Collections

```
analytics/
├── token_usage/
│   └── {eventId}/
│       ├── provider: string
│       ├── model: string
│       ├── promptTokens: number
│       ├── completionTokens: number
│       ├── totalTokens: number
│       ├── estimatedCost: number
│       ├── clientType: string
│       ├── itineraryId: string
│       ├── agentType: string
│       └── timestamp: timestamp
│
├── daily_token_aggregates/
│   └── dates/
│       └── {YYYY-MM-DD}/
│           ├── totalTokens: number
│           ├── promptTokens: number
│           ├── completionTokens: number
│           ├── totalCost: number
│           ├── gemini_tokens: number
│           ├── gemini_cost: number
│           ├── openrouter_tokens: number
│           ├── openrouter_cost: number
│           ├── models: map
│           │   ├── gemini-2.5-flash: {tokens, cost}
│           │   ├── qwen_qwen-2.5-72b-instruct: {tokens, cost}
│           │   └── ...
│           └── lastUpdated: timestamp
│
├── monthly_token_aggregates/
│   └── months/
│       └── {YYYY-MM}/
│           ├── totalTokens: number
│           ├── totalCost: number
│           ├── averageCostPerDay: number
│           ├── projectedMonthlyCost: number
│           ├── providerBreakdown: map
│           └── modelBreakdown: map
│
└── agent_token_usage/
    └── {agentType}/
        ├── totalTokens: number
        ├── totalCost: number
        ├── averageTokensPerExecution: number
        ├── executionCount: number
        └── lastUpdated: timestamp
```

### 7. Token Analytics Dashboard Endpoints

**File**: `src/main/java/com/tripplanner/controller/TokenAnalyticsController.java` (NEW)

```java
package com.tripplanner.controller;

import com.tripplanner.service.TokenAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST controller for token usage analytics and cost tracking.
 */
@RestController
@RequestMapping("/api/v1/analytics/tokens")
public class TokenAnalyticsController {
    
    private final TokenAnalyticsService tokenAnalyticsService;
    
    public TokenAnalyticsController(TokenAnalyticsService tokenAnalyticsService) {
        this.tokenAnalyticsService = tokenAnalyticsService;
    }
    
    /**
     * Get daily token usage summary.
     * GET /api/v1/analytics/tokens/daily?date=2024-11-18
     */
    @GetMapping("/daily")
    public ResponseEntity<DailyTokenSummary> getDailyTokenUsage(
            @RequestParam(required = false) String date) {
        
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        DailyTokenSummary summary = tokenAnalyticsService.getDailyTokenSummary(targetDate);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get monthly token usage summary.
     * GET /api/v1/analytics/tokens/monthly?month=2024-11
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyTokenSummary> getMonthlyTokenUsage(
            @RequestParam(required = false) String month) {
        
        String targetMonth = month != null ? month : LocalDate.now().toString().substring(0, 7);
        MonthlyTokenSummary summary = tokenAnalyticsService.getMonthlyTokenSummary(targetMonth);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get token usage by agent type.
     * GET /api/v1/analytics/tokens/by-agent
     */
    @GetMapping("/by-agent")
    public ResponseEntity<Map<String, AgentTokenUsage>> getTokenUsageByAgent() {
        Map<String, AgentTokenUsage> usage = tokenAnalyticsService.getTokenUsageByAgent();
        return ResponseEntity.ok(usage);
    }
    
    /**
     * Get token usage by provider.
     * GET /api/v1/analytics/tokens/by-provider?startDate=2024-11-01&endDate=2024-11-30
     */
    @GetMapping("/by-provider")
    public ResponseEntity<Map<String, ProviderTokenUsage>> getTokenUsageByProvider(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        Map<String, ProviderTokenUsage> usage = tokenAnalyticsService.getTokenUsageByProvider(start, end);
        return ResponseEntity.ok(usage);
    }
    
    /**
     * Get cost projection for current month.
     * GET /api/v1/analytics/tokens/cost-projection
     */
    @GetMapping("/cost-projection")
    public ResponseEntity<CostProjection> getCostProjection() {
        CostProjection projection = tokenAnalyticsService.getCostProjection();
        return ResponseEntity.ok(projection);
    }
    
    /**
     * Get token usage trends over time.
     * GET /api/v1/analytics/tokens/trends?days=30
     */
    @GetMapping("/trends")
    public ResponseEntity<TokenUsageTrends> getTokenUsageTrends(
            @RequestParam(defaultValue = "30") int days) {
        
        TokenUsageTrends trends = tokenAnalyticsService.getTokenUsageTrends(days);
        return ResponseEntity.ok(trends);
    }
    
    // DTOs
    
    public record DailyTokenSummary(
        String date,
        long totalTokens,
        long promptTokens,
        long completionTokens,
        double totalCost,
        Map<String, Long> providerTokens,
        Map<String, Double> providerCosts,
        Map<String, ModelUsage> modelBreakdown
    ) {}
    
    public record MonthlyTokenSummary(
        String month,
        long totalTokens,
        double totalCost,
        double averageCostPerDay,
        double projectedMonthlyCost,
        Map<String, Long> providerTokens,
        Map<String, Double> providerCosts,
        Map<String, ModelUsage> modelBreakdown
    ) {}
    
    public record AgentTokenUsage(
        String agentType,
        long totalTokens,
        double totalCost,
        long executionCount,
        double averageTokensPerExecution,
        double averageCostPerExecution
    ) {}
    
    public record ProviderTokenUsage(
        String provider,
        long totalTokens,
        double totalCost,
        long requestCount,
        double averageTokensPerRequest
    ) {}
    
    public record ModelUsage(
        String model,
        long tokens,
        double cost
    ) {}
    
    public record CostProjection(
        String month,
        double currentCost,
        double projectedCost,
        int daysElapsed,
        int daysRemaining,
        double dailyAverage,
        String trend
    ) {}
    
    public record TokenUsageTrends(
        java.util.List<DailyDataPoint> dailyData,
        TrendSummary summary
    ) {}
    
    public record DailyDataPoint(
        String date,
        long tokens,
        double cost
    ) {}
    
    public record TrendSummary(
        double averageTokensPerDay,
        double averageCostPerDay,
        double totalTokens,
        double totalCost,
        String trend
    ) {}
}
```

### 8. Token Analytics Queries

**Example Firestore Queries**:

```java
// Get daily token usage
DocumentSnapshot dailyDoc = firestore
    .collection("analytics")
    .document("daily_token_aggregates")
    .collection("dates")
    .document("2024-11-18")
    .get()
    .get();

// Get token usage for specific agent
QuerySnapshot agentUsage = firestore
    .collection("analytics")
    .document("token_usage")
    .collection("all")
    .whereEqualTo("agentType", "SkeletonPlannerAgent")
    .whereGreaterThanOrEqualTo("timestamp", startDate)
    .whereLessThanOrEqualTo("timestamp", endDate)
    .get()
    .get();

// Get most expensive operations
QuerySnapshot expensiveOps = firestore
    .collection("analytics")
    .document("token_usage")
    .collection("all")
    .orderBy("estimatedCost", Query.Direction.DESCENDING)
    .limit(10)
    .get()
    .get();
```

### 9. Token Usage Dashboard (Frontend)

**File**: `frontend/src/pages/TokenAnalyticsDashboardPage.tsx` (NEW)

```typescript
import React, { useEffect, useState } from 'react';
import { apiClient } from '@/services/apiClient';

interface TokenMetrics {
  totalTokens: number;
  totalCost: number;
  providerBreakdown: Record<string, { tokens: number; cost: number }>;
  modelBreakdown: Record<string, { tokens: number; cost: number }>;
}

export function TokenAnalyticsDashboardPage() {
  const [dailyMetrics, setDailyMetrics] = useState<TokenMetrics | null>(null);
  const [monthlyMetrics, setMonthlyMetrics] = useState<TokenMetrics | null>(null);
  const [costProjection, setCostProjection] = useState<any>(null);
  
  useEffect(() => {
    loadTokenMetrics();
  }, []);
  
  const loadTokenMetrics = async () => {
    try {
      // Load daily metrics
      const daily = await apiClient.get('/analytics/tokens/daily');
      setDailyMetrics(daily.data);
      
      // Load monthly metrics
      const monthly = await apiClient.get('/analytics/tokens/monthly');
      setMonthlyMetrics(monthly.data);
      
      // Load cost projection
      const projection = await apiClient.get('/analytics/tokens/cost-projection');
      setCostProjection(projection.data);
      
    } catch (error) {
      console.error('Failed to load token metrics:', error);
    }
  };
  
  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Token Usage Analytics</h1>
      
      {/* Daily Summary */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <MetricCard
          title="Today's Tokens"
          value={dailyMetrics?.totalTokens.toLocaleString() || '0'}
          subtitle={`$${dailyMetrics?.totalCost.toFixed(4) || '0.00'}`}
        />
        <MetricCard
          title="Monthly Tokens"
          value={monthlyMetrics?.totalTokens.toLocaleString() || '0'}
          subtitle={`$${monthlyMetrics?.totalCost.toFixed(2) || '0.00'}`}
        />
        <MetricCard
          title="Projected Monthly Cost"
          value={`$${costProjection?.projectedCost.toFixed(2) || '0.00'}`}
          subtitle={costProjection?.trend || 'Calculating...'}
        />
      </div>
      
      {/* Provider Breakdown */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Provider Breakdown</h2>
        <div className="space-y-2">
          {Object.entries(dailyMetrics?.providerBreakdown || {}).map(([provider, data]) => (
            <div key={provider} className="flex justify-between items-center">
              <span className="font-medium capitalize">{provider}</span>
              <div className="text-right">
                <div>{data.tokens.toLocaleString()} tokens</div>
                <div className="text-sm text-gray-500">${data.cost.toFixed(4)}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
      
      {/* Model Breakdown */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4">Model Breakdown</h2>
        <div className="space-y-2">
          {Object.entries(dailyMetrics?.modelBreakdown || {}).map(([model, data]) => (
            <div key={model} className="flex justify-between items-center">
              <span className="font-medium text-sm">{model}</span>
              <div className="text-right">
                <div className="text-sm">{data.tokens.toLocaleString()} tokens</div>
                <div className="text-xs text-gray-500">${data.cost.toFixed(4)}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function MetricCard({ title, value, subtitle }: { title: string; value: string; subtitle: string }) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-sm font-medium text-gray-500 mb-2">{title}</h3>
      <div className="text-2xl font-bold">{value}</div>
      <div className="text-sm text-gray-600 mt-1">{subtitle}</div>
    </div>
  );
}
```

### 10. Cost Alerts & Monitoring

**File**: `src/main/java/com/tripplanner/service/TokenCostAlertService.java` (NEW)

```java
package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for monitoring token costs and sending alerts when thresholds are exceeded.
 */
@Service
public class TokenCostAlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenCostAlertService.class);
    
    // Cost thresholds (USD)
    private static final double DAILY_COST_WARNING = 10.0;
    private static final double DAILY_COST_CRITICAL = 50.0;
    private static final double MONTHLY_COST_WARNING = 200.0;
    private static final double MONTHLY_COST_CRITICAL = 500.0;
    
    private final TokenAnalyticsService tokenAnalyticsService;
    private final NotificationService notificationService;
    
    public TokenCostAlertService(TokenAnalyticsService tokenAnalyticsService,
                                NotificationService notificationService) {
        this.tokenAnalyticsService = tokenAnalyticsService;
        this.notificationService = notificationService;
    }
    
    /**
     * Check daily costs every hour.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void checkDailyCosts() {
        try {
            DailyTokenSummary summary = tokenAnalyticsService.getDailyTokenSummary(LocalDate.now());
            double dailyCost = summary.totalCost();
            
            if (dailyCost >= DAILY_COST_CRITICAL) {
                notificationService.sendAlert(
                    "CRITICAL: Daily token cost exceeded $" + DAILY_COST_CRITICAL,
                    "Current daily cost: $" + String.format("%.2f", dailyCost)
                );
                logger.error("🚨 CRITICAL: Daily token cost ${} exceeds threshold ${}", 
                            dailyCost, DAILY_COST_CRITICAL);
            } else if (dailyCost >= DAILY_COST_WARNING) {
                notificationService.sendWarning(
                    "WARNING: Daily token cost exceeded $" + DAILY_COST_WARNING,
                    "Current daily cost: $" + String.format("%.2f", dailyCost)
                );
                logger.warn("⚠️ WARNING: Daily token cost ${} exceeds threshold ${}", 
                           dailyCost, DAILY_COST_WARNING);
            }
            
        } catch (Exception e) {
            logger.error("Failed to check daily costs", e);
        }
    }
    
    /**
     * Check monthly cost projection daily.
     */
    @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    public void checkMonthlyCostProjection() {
        try {
            CostProjection projection = tokenAnalyticsService.getCostProjection();
            double projectedCost = projection.projectedCost();
            
            if (projectedCost >= MONTHLY_COST_CRITICAL) {
                notificationService.sendAlert(
                    "CRITICAL: Projected monthly cost exceeds $" + MONTHLY_COST_CRITICAL,
                    String.format("Projected: $%.2f (Current: $%.2f)", 
                                 projectedCost, projection.currentCost())
                );
                logger.error("🚨 CRITICAL: Projected monthly cost ${} exceeds threshold ${}", 
                            projectedCost, MONTHLY_COST_CRITICAL);
            } else if (projectedCost >= MONTHLY_COST_WARNING) {
                notificationService.sendWarning(
                    "WARNING: Projected monthly cost exceeds $" + MONTHLY_COST_WARNING,
                    String.format("Projected: $%.2f (Current: $%.2f)", 
                                 projectedCost, projection.currentCost())
                );
                logger.warn("⚠️ WARNING: Projected monthly cost ${} exceeds threshold ${}", 
                           projectedCost, MONTHLY_COST_WARNING);
            }
            
        } catch (Exception e) {
            logger.error("Failed to check monthly cost projection", e);
        }
    }
}
```

### 11. Token Usage Optimization Recommendations

Based on token analytics, the system can provide optimization recommendations:

1. **High-Cost Agents**: Identify agents consuming most tokens
2. **Prompt Optimization**: Suggest shorter prompts for frequently called operations
3. **Model Selection**: Recommend cheaper models for simple tasks
4. **Caching**: Identify repeated prompts that could be cached
5. **Batch Processing**: Suggest batching similar requests

**Example Alert**:
```
⚠️ Token Usage Alert
- SkeletonPlannerAgent consumed 50,000 tokens today ($1.50)
- Recommendation: Consider using gemini-2.5-flash instead of gemini-1.5-pro
- Potential savings: $1.00/day ($30/month)
```

