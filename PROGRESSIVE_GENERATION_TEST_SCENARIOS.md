# Progressive Generation Test Scenarios

## Overview

This document outlines the test scenarios for the progressive 4-day itinerary generation feature. The system generates itineraries day by day, with each day going through multiple phases: skeleton generation, activity generation, meal generation, transport generation, and enrichment.

## Test Scenarios Created

### 1. Progressive Generation Scenario Test
**File**: `src/test/java/com/tripplanner/testing/scenarios/ProgressiveGenerationScenarioTest.java`

This test validates:
- 4-day Paris trip request structure
- Progressive generation flow phases
- AI response structures for each generation phase
- Complete 4-day generation flow validation

### 2. SSE Event Flow Test
**File**: `src/test/java/com/tripplanner/testing/scenarios/SseEventFlowTest.java`

This test simulates:
- Day 1 skeleton generation events
- Day 1 activity generation events
- Day 1 meal generation events
- Day 1 transport generation events
- Day 1 enrichment events
- Complete 4-day generation flow
- Error and recovery scenarios

### 3. AI Response Mock Data
**File**: `src/test/resources/mock-responses/ai-responses/4day-paris-trip-responses.json`

Contains realistic AI responses for:
- Day 1-4 skeleton generation
- Day 1-4 activity generation
- Day 1-4 meal generation
- Day 1-4 transport generation
- Day 1-4 enrichment

## Progressive Generation Flow

### Phase 1: Skeleton Generation
**Agent**: `SkeletonPlannerAgent`
**Task Type**: `skeleton`
**Purpose**: Creates the basic day structure with placeholder nodes

**AI Response Structure**:
```json
{
  "days": [
    {
      "dayNumber": 1,
      "date": "2024-03-15",
      "location": "Paris, France",
      "nodes": [
        {
          "type": "activity",
          "timing": "morning",
          "duration": 180,
          "placeholder": true
        },
        {
          "type": "meal",
          "timing": "lunch",
          "duration": 90,
          "placeholder": true
        }
      ]
    }
  ]
}
```

### Phase 2: Activity Generation
**Agent**: `ActivityAgent`
**Task Type**: `activity`
**Purpose**: Populates activities for each day

**AI Response Structure**:
```json
{
  "nodes": [
    {
      "type": "activity",
      "title": "Visit the Eiffel Tower",
      "description": "Iconic iron lattice tower and symbol of Paris",
      "location": "Champ de Mars, 7th arrondissement",
      "timing": "morning",
      "startTime": "09:00",
      "duration": 180,
      "cost": 29.00,
      "currency": "EUR",
      "category": "landmark",
      "difficulty": "easy",
      "accessibility": "wheelchair-accessible"
    }
  ]
}
```

### Phase 3: Meal Generation
**Agent**: `MealAgent`
**Task Type**: `meal`
**Purpose**: Populates meals for each day

**AI Response Structure**:
```json
{
  "nodes": [
    {
      "type": "meal",
      "title": "Lunch at Café de Flore",
      "description": "Historic café in Saint-Germain-des-Prés",
      "location": "172 Boulevard Saint-Germain, 6th arrondissement",
      "timing": "lunch",
      "startTime": "12:30",
      "duration": 90,
      "cost": 45.00,
      "currency": "EUR",
      "cuisine": "French",
      "dietaryOptions": ["vegetarian", "vegan"],
      "atmosphere": "historic"
    }
  ]
}
```

### Phase 4: Transport Generation
**Agent**: `TransportAgent`
**Task Type**: `transport`
**Purpose**: Populates transport between locations

**AI Response Structure**:
```json
{
  "nodes": [
    {
      "type": "transport",
      "title": "Metro to Eiffel Tower",
      "description": "Take Line 6 to Bir-Hakeim station",
      "from": "Hotel",
      "to": "Eiffel Tower",
      "timing": "morning",
      "startTime": "08:30",
      "duration": 20,
      "cost": 2.10,
      "currency": "EUR",
      "transportType": "metro",
      "accessibility": "wheelchair-accessible"
    }
  ]
}
```

### Phase 5: Enrichment
**Agent**: `EnrichmentAgent`
**Task Type**: `enrich`
**Purpose**: Adds photos, tips, and additional information

**AI Response Structure**:
```json
{
  "enrichments": [
    {
      "nodeId": "activity_1",
      "photos": [
        "https://example.com/eiffel-tower-1.jpg",
        "https://example.com/eiffel-tower-2.jpg"
      ],
      "tips": [
        "Book tickets in advance to avoid long queues",
        "Best views are from Trocadéro across the river"
      ],
      "nearbyAttractions": [
        "Trocadéro Gardens",
        "Champ de Mars"
      ],
      "weatherConsiderations": "Best visited in clear weather for views"
    }
  ]
}
```

## SSE Event Flow

### Event Types
- `started`: Agent begins processing
- `progress`: Agent reports progress (0-100%)
- `completed`: Agent finishes successfully
- `failed`: Agent encounters an error

### Event Structure
```json
{
  "itineraryId": "it_paris_4day_123",
  "agentId": "agent_1234567890",
  "agentKind": "SKELETON_PLANNER",
  "status": "started",
  "progress": 0,
  "message": "Starting Day 1 skeleton generation...",
  "step": "skeleton_generation",
  "timestamp": "2024-03-15T09:00:00Z"
}
```

### Complete 4-Day Flow Events
1. **Day 1**: Skeleton → Activity → Meal → Transport → Enrichment
2. **Day 2**: Skeleton → Activity → Meal → Transport → Enrichment
3. **Day 3**: Skeleton → Activity → Meal → Transport → Enrichment
4. **Day 4**: Skeleton → Activity → Meal → Transport → Enrichment
5. **Final**: Orchestrator completion event

## Test Request Structure

### 4-Day Paris Trip Request
```json
{
  "destination": "Paris, France",
  "startLocation": "New York, USA",
  "startDate": "2024-03-15",
  "endDate": "2024-03-19",
  "party": {
    "adults": 2,
    "children": 0,
    "infants": 0,
    "rooms": 1
  },
  "budgetTier": "luxury",
  "interests": ["culture", "cuisine", "history", "art"],
  "constraints": ["wheelchair-accessible"],
  "language": "en"
}
```

## Error Handling Scenarios

### 1. Agent Failure and Recovery
- Agent fails during generation
- System retries the failed agent
- Recovery events are sent via SSE
- Generation continues after successful retry

### 2. SSE Connection Issues
- Connection drops during generation
- Frontend reconnects automatically
- Missed events are handled gracefully
- Generation continues seamlessly

### 3. AI Service Unavailability
- AI service temporarily unavailable
- System retries with exponential backoff
- Fallback responses if needed
- User is notified of delays

## Frontend Integration

### SSE Connection Management
- Real-time progress updates
- Connection status indicators
- Automatic reconnection
- Event buffering for offline scenarios

### UI Components
- Progress bars for each day
- Phase indicators (skeleton, activity, meal, transport, enrichment)
- Real-time status messages
- Error handling and retry buttons

## Performance Considerations

### Parallel Processing
- Multiple days can be processed in parallel
- Each phase within a day is sequential
- Resource management for concurrent AI calls

### Caching
- AI responses are cached for similar requests
- Partial results are stored for recovery
- User preferences are remembered

### Rate Limiting
- AI service rate limits are respected
- Queue management for high-volume requests
- Priority handling for premium users

## Monitoring and Observability

### Metrics
- Generation time per day/phase
- Success/failure rates
- AI service response times
- SSE connection stability

### Logging
- Detailed agent execution logs
- SSE event flow logs
- Error tracking and analysis
- Performance monitoring

## Future Enhancements

### 1. Dynamic Phase Ordering
- Allow users to customize phase order
- Skip certain phases if not needed
- Add new phases (accommodation, shopping, etc.)

### 2. Real-time Collaboration
- Multiple users can view generation progress
- Collaborative editing during generation
- Shared preferences and constraints

### 3. Advanced AI Integration
- Multiple AI providers for redundancy
- Specialized models for different phases
- Continuous learning from user feedback

### 4. Offline Support
- Cache generated content for offline viewing
- Sync when connection is restored
- Local generation for simple requests

## Conclusion

The progressive generation system provides a robust, scalable solution for creating detailed itineraries. The test scenarios ensure that each phase works correctly, SSE events provide real-time feedback, and error handling ensures a smooth user experience even when things go wrong.

The system is designed to be extensible, allowing for new agents, phases, and features to be added without breaking existing functionality. The comprehensive test coverage ensures reliability and maintainability as the system evolves.


