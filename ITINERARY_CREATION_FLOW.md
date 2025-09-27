# Complete Itinerary Creation Flow - Frontend & Backend

## Overview
This document provides a comprehensive breakdown of the itinerary creation process, covering both frontend and backend components with specific code references.

## Table of Contents
1. [Frontend Flow](#frontend-flow)
2. [Backend Flow](#backend-flow)
3. [API Endpoints](#api-endpoints)
4. [Data Flow](#data-flow)
5. [Error Handling](#error-handling)
6. [Progress Tracking](#progress-tracking)

---

## Frontend Flow

### 1. User Interface - Trip Wizard
**File**: `frontend/src/components/trip-wizard/SimplifiedTripWizard.tsx`

#### 1.1 Form Initialization
```typescript
// Lines 58-86: Form state initialization
const [startLocation, setStartLocation] = useState('New York, NY');
const [endLocation, setEndLocation] = useState('Paris, France');
const [dateRange, setDateRange] = useState<DateRange | undefined>(() => {
  const year = new Date().getFullYear();
  const from = new Date(year, 12, 25);
  const to = new Date(year, 12, 28);
  return { from, to };
});
const [budget, setBudget] = useState(3000);
const [currency, setCurrency] = useState('USD');
const [travelers, setTravelers] = useState<Traveler[]>([...]);
const [preferences, setPreferences] = useState<TravelPreferences>({...});
const [settings, setSettings] = useState<TripSettings>({...});
```

#### 1.2 Form Validation
```typescript
// Lines 252-261: Form validation logic
const isFormValid = () => {
  return startLocation.trim() && 
         endLocation.trim() && 
         dateRange?.from && 
         dateRange?.to && 
         travelers.every(t => t.name.trim()) &&
         budget > 0;
};
```

#### 1.3 Debounced Form Submission
**File**: `frontend/src/hooks/useFormSubmission.ts`

```typescript
// Lines 50-56: Form submission with debouncing
const { isSubmitting, submit: submitForm, error: submissionError } = useFormSubmission({
  debounceMs: 2000, // 2 second debounce
  onError: (error) => {
    console.error('Form submission error:', error);
    alert(`Failed to create itinerary: ${error.message}`);
  }
});
```

#### 1.4 Submit Handler
```typescript
// Lines 263-382: Main submission logic
const handleSubmit = async () => {
  if (!isFormValid()) return;

  await submitForm(async () => {
    setIsGenerating(true);

    try {
      // Create API request
      const createRequest: CreateItineraryRequest = {
        destination: endLocation,
        startDate: dateRange!.from!.toISOString().split('T')[0],
        endDate: dateRange!.to!.toISOString().split('T')[0],
        party: {
          adults: travelers[0] ? 1 : 0,
          children: 0,
          infants: 0,
          rooms: 1
        },
        budgetTier: budget <= 2000 ? 'economy' : budget <= 5000 ? 'mid-range' : 'luxury',
        interests: Object.entries(preferences)
          .filter(([_, value]) => (value as number) > 50)
          .map(([key]) => key as string),
        constraints: Object.entries(settings)
          .filter(([_, value]) => value as boolean)
          .map(([key]) => key as string),
        language: 'en'
      };

      // Call backend API
      const response = await createItineraryMutation.mutateAsync(createRequest);
      
      // Convert to TripData format
      const tripData: TripData = {
        id: response.id,
        startLocation: startLocationData,
        endLocation: endLocationData,
        // ... other properties
      };

      addTrip(tripData);
      setCurrentTrip(tripData);
      setGeneratedTripData(tripData);
      
      // Show progress modal if still generating
      if ((response.status as string) === 'planning') {
        console.log('Itinerary is being generated asynchronously, showing progress modal');
      } else {
        setIsGenerating(false);
        onComplete(tripData);
      }

      return tripData;
    } catch (error) {
      setIsGenerating(false);
      throw error;
    }
  });
};
```

### 2. Progress Tracking - Agent Progress Modal
**File**: `frontend/src/components/agents/AgentProgressModal.tsx`

#### 2.1 Component Initialization
```typescript
// Lines 28-50: Component state setup
export function AgentProgressModal({ tripData, onComplete, onCancel }: AgentProgressModalProps) {
  const { setCurrentTrip } = useAppStore();
  const { refetch: refetchItinerary } = useItinerary(tripData.id, undefined, 30000);
  const [agents, setAgents] = useState<AgentProgress[]>(
    AGENT_TASKS.map(task => ({
      task,
      status: 'pending',
      progress: 0
    }))
  );
  const [overallProgress, setOverallProgress] = useState(0);
  const [hasCompleted, setHasCompleted] = useState(false);
  const [hasError, setHasError] = useState(false);
```

#### 2.2 Server-Sent Events (SSE) Connection
```typescript
// Lines 250-252: SSE connection setup
const eventSource = apiClient.createAgentEventStream(tripData.id);
eventSourceRef.current = eventSource;
```

#### 2.3 SSE Event Handling
```typescript
// Lines 52-93: SSE event processing
const handleSSEEvent = (event: MessageEvent) => {
  try {
    // Parse agent events
    const agentEvent: AgentEvent = JSON.parse(event.data);
    
    // Update agent status
    setAgents(prev => prev.map(agent => {
      if (agent.task.id === agentEvent.kind) {
        const newStatus = agentEvent.status === 'succeeded' ? 'completed' : 
                         agentEvent.status === 'running' ? 'running' : 
                         agentEvent.status === 'failed' ? 'failed' : 'pending';
        
        return {
          ...agent,
          status: newStatus,
          progress: agentEvent.progress || agent.progress,
          startTime: agentEvent.status === 'running' && !agent.startTime ? Date.now() : agent.startTime,
          endTime: agentEvent.status === 'succeeded' || agentEvent.status === 'failed' ? Date.now() : agent.endTime,
          error: agentEvent.status === 'failed' ? agentEvent.message || 'Agent failed' : undefined
        };
      }
      return agent;
    }));
  } catch (error) {
    console.error('Error processing SSE event:', error);
  }
};
```

#### 2.4 Progress Calculation
```typescript
// Lines 95-126: Progress calculation with smoothing
setAgents(prev => {
  const completedCount = prev.filter(a => a.status === 'completed').length;
  const failedCount = prev.filter(a => a.status === 'failed').length;
  const runningAgent = prev.find(a => a.status === 'running');
  
  // Calculate progress more smoothly
  let totalProgress = 0;
  if (completedCount === AGENT_TASKS.length) {
    totalProgress = 100;
  } else if (runningAgent) {
    totalProgress = Math.min(95, (completedCount * 100) + (runningAgent.progress * 0.8));
  } else {
    totalProgress = (completedCount / AGENT_TASKS.length) * 100;
  }
  
  // Only update progress if it's higher than current (prevent regression)
  setOverallProgress(prevProgress => Math.max(prevProgress, Math.round(totalProgress)));
  
  return prev;
});
```

#### 2.5 Completion Checking
```typescript
// Lines 196-248: Completion checker with 30-second delay
const checkCompletion = async () => {
  if (!hasCompleted && !hasError) {
    try {
      const result = await refetchItinerary();
      if (result.data) {
        const responseData = result.data as TripData;
        
        // Check for actual content
        const hasActualContent = responseData.itinerary?.days && 
                               responseData.itinerary.days.length > 0 && 
                               responseData.destination !== 'Loading...';
        
        if (responseData.status === 'completed' && hasActualContent) {
          setHasCompleted(true);
          setOverallProgress(100);
          setCurrentTrip(responseData);
          
          // Clean up resources
          if (completionTimeoutRef.current) {
            clearTimeout(completionTimeoutRef.current);
            completionTimeoutRef.current = null;
          }
          if (eventSourceRef.current) {
            eventSourceRef.current.close();
            eventSourceRef.current = null;
          }
          
          onComplete();
          return;
        }
      }
    } catch (e) {
      console.warn('Completion check failed:', e);
    }
    
    // Schedule next check
    if (!hasCompleted && !hasError) {
      const timeout = setTimeout(checkCompletion, 2000);
      completionTimeoutRef.current = timeout;
    }
  }
};

// Start checking after 30 seconds
const initialTimeout = setTimeout(checkCompletion, 30000);
```

### 3. API Client
**File**: `frontend/src/services/apiClient.ts`

#### 3.1 Create Itinerary Request
```typescript
// Lines 170-182: Create itinerary API call
async createItinerary(data: CreateItineraryRequest): Promise<ItineraryResponse> {
  return this.request<ItineraryResponse>('/itineraries', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  }, retryOptions);
}
```

#### 3.2 Get Itinerary Request
```typescript
// Lines 184-221: Get itinerary with 404 handling
async getItinerary(id: string, retryOptions?: { maxRetries?: number; retryDelay?: number }): Promise<TripData> {
  try {
    const response = await this.request<NormalizedItinerary>(`/itineraries/${id}/json`, {}, retryOptions);
    const transformedData = NormalizedDataTransformer.transformNormalizedItineraryToTripData(response);
    return transformedData;
  } catch (error) {
    // If it's a 404 error, it might be that the itinerary is still being generated
    if (error.message.includes('404')) {
      console.log('404 error detected - itinerary might still be generating');
      throw error;
    }
    throw error;
  }
}
```

#### 3.3 SSE Stream Creation
```typescript
// Lines 400-420: SSE stream setup
createAgentEventStream(itineraryId: string): EventSource {
  const url = `${this.baseUrl}/itineraries/${itineraryId}/events`;
  const eventSource = new EventSource(url);
  
  eventSource.onmessage = (event) => {
    console.log('SSE message received:', event.data);
  };
  
  eventSource.onerror = (error) => {
    console.error('SSE error:', error);
  };
  
  return eventSource;
}
```

---

## Backend Flow

### 1. API Controller
**File**: `src/main/java/com/tripplanner/controller/ItineraryController.java`

#### 1.1 Create Itinerary Endpoint
```java
@PostMapping
public ResponseEntity<ItineraryResponse> createItinerary(@RequestBody CreateItineraryRequest request) {
    try {
        // Validate request
        if (request.getDestination() == null || request.getDestination().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Create itinerary
        ItineraryResponse response = itineraryService.createItinerary(request);
        
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        logger.error("Error creating itinerary", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

### 2. Service Layer
**File**: `src/main/java/com/tripplanner/service/ItineraryService.java`

#### 2.1 Itinerary Creation
```java
public ItineraryResponse createItinerary(CreateItineraryRequest request) {
    // Create itinerary entity
    Itinerary itinerary = new Itinerary();
    itinerary.setId(UUID.randomUUID().toString());
    itinerary.setDestination(request.getDestination());
    itinerary.setStartDate(LocalDate.parse(request.getStartDate()));
    itinerary.setEndDate(LocalDate.parse(request.getEndDate()));
    itinerary.setStatus("planning");
    itinerary.setCreatedAt(Instant.now());
    itinerary.setUpdatedAt(Instant.now());
    
    // Save to database
    itineraryRepository.save(itinerary);
    
    // Start async processing
    agentOrchestrator.orchestrateItineraryCreation(itinerary.getId(), request);
    
    // Return response
    return ItineraryResponse.builder()
        .id(itinerary.getId())
        .destination(itinerary.getDestination())
        .startDate(itinerary.getStartDate().toString())
        .endDate(itinerary.getEndDate().toString())
        .status(itinerary.getStatus())
        .createdAt(itinerary.getCreatedAt().toEpochMilli())
        .updatedAt(itinerary.getUpdatedAt().toEpochMilli())
        .build();
}
```

### 3. Agent Orchestrator
**File**: `src/main/java/com/tripplanner/agents/AgentOrchestrator.java`

#### 3.1 Orchestration Entry Point
```java
@Async
public void orchestrateItineraryCreation(String itineraryId, CreateItineraryRequest request) {
    try {
        logger.info("Starting itinerary orchestration for ID: {}", itineraryId);
        
        // Step 1: Planner Agent
        NormalizedItinerary itinerary = plannerAgent.execute(itineraryId, request);
        
        // Step 2: Enrichment Agent (if needed)
        if (itinerary != null) {
            enrichmentAgent.execute(itineraryId, itinerary);
        }
        
        // Update status to completed
        updateItineraryStatus(itineraryId, "completed");
        
    } catch (Exception e) {
        logger.error("Error in itinerary orchestration for ID: {}", itineraryId, e);
        updateItineraryStatus(itineraryId, "failed");
    }
}
```

### 4. Planner Agent
**File**: `src/main/java/com/tripplanner/agents/PlannerAgent.java`

#### 4.1 Agent Execution
```java
public NormalizedItinerary execute(String itineraryId, CreateItineraryRequest request) {
    try {
        // Publish agent start event
        agentEventBus.publishAgentEvent(itineraryId, "planner", "running", 0, "Agent started");
        
        // Generate prompt
        String prompt = buildPrompt(request);
        
        // Call AI service
        String aiResponse = openRouterClient.generateStructuredContent(prompt);
        
        // Parse response
        NormalizedItinerary itinerary = parseAIResponse(aiResponse);
        
        // Validate itinerary
        if (itinerary == null || itinerary.getDays() == null || itinerary.getDays().isEmpty()) {
            throw new RuntimeException("Invalid itinerary generated by AI");
        }
        
        // Save to database
        saveItinerary(itineraryId, itinerary);
        
        // Publish completion event
        agentEventBus.publishAgentEvent(itineraryId, "planner", "succeeded", 100, "Itinerary generated successfully");
        
        return itinerary;
        
    } catch (Exception e) {
        logger.error("Planner agent failed for itinerary: {}", itineraryId, e);
        agentEventBus.publishAgentEvent(itineraryId, "planner", "failed", 100, "Agent failed: " + e.getMessage());
        throw e;
    }
}
```

### 5. OpenRouter Client
**File**: `src/main/java/com/tripplanner/service/openrouter/OpenRouterClient.java`

#### 5.1 AI Content Generation
```java
public String generateStructuredContent(String userPrompt) {
    if (mockMode) {
        logger.info("OpenRouter mock mode enabled; returning mock itinerary");
        return getMockItineraryResponse(userPrompt);
    }
    
    try {
        // Build request
        OpenRouterRequest request = buildRequest(userPrompt);
        
        // Make API call
        OpenRouterResponse response = restTemplate.postForObject(
            "https://openrouter.ai/api/v1/chat/completions",
            request,
            OpenRouterResponse.class
        );
        
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }
        
        throw new RuntimeException("Empty response from OpenRouter");
        
    } catch (Exception e) {
        logger.error("Error calling OpenRouter API", e);
        throw new RuntimeException("Failed to generate content", e);
    }
}
```

### 6. Agent Event Bus
**File**: `src/main/java/com/tripplanner/service/AgentEventBus.java`

#### 6.1 Event Publishing
```java
public void publishAgentEvent(String itineraryId, String agentKind, String status, int progress, String message) {
    AgentEvent event = AgentEvent.builder()
        .agentId(UUID.randomUUID().toString())
        .itineraryId(itineraryId)
        .kind(agentKind)
        .status(status)
        .progress(progress)
        .message(message)
        .timestamp(Instant.now())
        .build();
    
    logger.info("Publishing agent event: {}", event);
    
    // Find emitters for this itinerary
    List<SseEmitter> emitters = emitterMap.get(itineraryId);
    if (emitters != null && !emitters.isEmpty()) {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("agent-event")
                    .data(event));
            } catch (Exception e) {
                logger.warn("Failed to send event to emitter", e);
            }
        });
    } else {
        logger.warn("No emitters found for itinerary: {}", itineraryId);
    }
}
```

---

## API Endpoints

### 1. Create Itinerary
- **Endpoint**: `POST /api/v1/itineraries`
- **Request Body**: `CreateItineraryRequest`
- **Response**: `ItineraryResponse`
- **Status Codes**: 200 (Success), 400 (Bad Request), 500 (Internal Error)

### 2. Get Itinerary
- **Endpoint**: `GET /api/v1/itineraries/{id}/json`
- **Response**: `NormalizedItinerary`
- **Status Codes**: 200 (Success), 404 (Not Found), 500 (Internal Error)

### 3. SSE Events
- **Endpoint**: `GET /api/v1/itineraries/{id}/events`
- **Response**: Server-Sent Events stream
- **Event Types**: `agent-event`

---

## Data Flow

### 1. Request Flow
```
User Input → SimplifiedTripWizard → useFormSubmission → createItineraryMutation → API Client → Backend Controller → ItineraryService → AgentOrchestrator
```

### 2. Response Flow
```
Backend Response → API Client → SimplifiedTripWizard → AgentProgressModal → SSE Connection → Real-time Updates
```

### 3. Data Transformation
```
CreateItineraryRequest → Itinerary Entity → NormalizedItinerary → TripData (Frontend)
```

---

## Error Handling

### 1. Frontend Error Handling
- **Form Validation**: Client-side validation before submission
- **API Errors**: Try-catch blocks with user-friendly error messages
- **SSE Errors**: Graceful degradation, completion checker as fallback
- **Timeout Handling**: 5-minute maximum timeout with retry option

### 2. Backend Error Handling
- **Input Validation**: Request validation at controller level
- **Agent Failures**: Individual agent error handling with event publishing
- **Database Errors**: Transaction rollback and error logging
- **External API Errors**: Retry logic and fallback mechanisms

---

## Progress Tracking

### 1. Progress Sources
- **SSE Events**: Real-time progress updates from agents
- **Completion Checker**: Periodic polling for final status
- **Manual Refresh**: User-triggered status checks

### 2. Progress Calculation
- **Agent Progress**: Individual agent progress (0-100%)
- **Overall Progress**: Weighted average of all agents
- **Smoothing**: Prevents progress regression and abrupt jumps

### 3. Completion Detection
- **Status Check**: Backend status must be "completed"
- **Content Validation**: Must have actual itinerary data
- **Resource Cleanup**: Proper cleanup of timers and connections

---

## Key Configuration

### 1. Timing Configuration
- **Debounce Period**: 2 seconds (form submission)
- **Initial Delay**: 30 seconds (before first API check)
- **Polling Interval**: 2 seconds (completion checker)
- **Timeout**: 5 minutes (maximum wait time)

### 2. Retry Configuration
- **Max Retries**: 3 attempts
- **Retry Delay**: Exponential backoff (1s, 2s, 4s)
- **404 Handling**: No retries (expected during generation)

### 3. Progress Configuration
- **Max Progress**: 95% (leaves room for completion)
- **Progress Smoothing**: Prevents regression
- **Completion Threshold**: 100% with content validation

---

This document provides a complete technical reference for the itinerary creation flow, including all major components, data transformations, error handling, and configuration details.
