# Agent Tools Specification
**Comprehensive Tool Catalog for Local & Google ADK Agents**

## Overview
This document catalogs all potential tools that can be exposed as REST APIs for consumption by:
- Local Java agents (existing pipeline)
- Google ADK agents (external AI)
- Future agent frameworks

---

## 🎯 Tool Categories

### 1. Budget & Cost Tools
**Service**: `BudgetTracker`, `CurrencyConversionService`

#### 1.1 Calculate Itinerary Cost ✅ IMPLEMENTED
- **Endpoint**: `POST /api/v1/tools/calculate-cost`
- **Description**: Calculate total cost breakdown for an itinerary
- **Parameters**:
  - `itineraryId` (required): String
  - `partySize` (optional): Integer (default: 1)
- **Returns**: Total cost, per-person cost, category breakdown, warnings
- **Use Cases**: Budget validation, cost estimation, overspend alerts

#### 1.2 Convert Currency
- **Endpoint**: `POST /api/v1/tools/convert-currency`
- **Description**: Convert amount between currencies
- **Parameters**:
  - `amount` (required): Double
  - `fromCurrency` (required): String (e.g., "USD")
  - `toCurrency` (required): String (e.g., "INR")
- **Returns**: Converted amount, exchange rate, timestamp
- **Use Cases**: Multi-currency budgeting, price comparisons

#### 1.3 Detect Destination Currency
- **Endpoint**: `POST /api/v1/tools/detect-currency`
- **Description**: Detect local currency for a destination
- **Parameters**:
  - `destination` (required): String
- **Returns**: Currency code, country, confidence score
- **Use Cases**: Auto-currency selection, budget initialization

---

### 2. Place Discovery & Enrichment Tools
**Service**: `GooglePlacesService`, `PlaceEnrichmentService`

#### 2.1 Search Places
- **Endpoint**: `POST /api/v1/tools/search-places`
- **Description**: Search for places by query and location
- **Parameters**:
  - `query` (required): String (e.g., "restaurants in Paris")
  - `location` (optional): Coordinates
  - `radius` (optional): Integer (meters)
  - `type` (optional): String (e.g., "restaurant", "museum")
- **Returns**: List of places with basic info
- **Use Cases**: Activity discovery, restaurant finding, attraction search

#### 2.2 Get Place Details
- **Endpoint**: `POST /api/v1/tools/place-details`
- **Description**: Get detailed information about a specific place
- **Parameters**:
  - `placeId` (required): String
- **Returns**: Full place details (photos, reviews, hours, rating, price)
- **Use Cases**: Activity enrichment, validation, user research

#### 2.3 Find Nearby Places
- **Endpoint**: `POST /api/v1/tools/nearby-places`
- **Description**: Find places near a location
- **Parameters**:
  - `latitude` (required): Double
  - `longitude` (required): Double
  - `radius` (required): Integer (meters)
  - `type` (optional): String
- **Returns**: Nearby places sorted by distance
- **Use Cases**: Contextual recommendations, route optimization

---

### 3. Geography & Distance Tools
**Service**: `GeographyService`, `GoogleMapsDistanceService`

#### 3.1 Check If Island
- **Endpoint**: `POST /api/v1/tools/is-island`
- **Description**: Determine if a location is an island
- **Parameters**:
  - `location` (required): String
- **Returns**: Boolean, confidence, geography info
- **Use Cases**: Transport mode selection, flight vs ferry decisions

#### 3.2 Calculate Distance
- **Endpoint**: `POST /api/v1/tools/calculate-distance`
- **Description**: Calculate distance and travel time between locations
- **Parameters**:
  - `origin` (required): String or Coordinates
  - `destination` (required): String or Coordinates
  - `mode` (optional): String ("driving", "walking", "transit")
- **Returns**: Distance (km), duration (minutes), route info
- **Use Cases**: Travel time estimation, route planning, feasibility checks

#### 3.3 Get Coordinates
- **Endpoint**: `POST /api/v1/tools/geocode`
- **Description**: Convert address to coordinates
- **Parameters**:
  - `address` (required): String
- **Returns**: Latitude, longitude, formatted address
- **Use Cases**: Location validation, map plotting

---

### 4. Itinerary Query & Modification Tools
**Service**: `ItineraryJsonService`, `ChangeEngine`

#### 4.1 Get Itinerary Summary
- **Endpoint**: `POST /api/v1/tools/itinerary-summary`
- **Description**: Get high-level summary of an itinerary
- **Parameters**:
  - `itineraryId` (required): String
- **Returns**: Destination, dates, day count, activity count, total cost
- **Use Cases**: Quick overview, status checks, chat context

#### 4.2 Get Day Details
- **Endpoint**: `POST /api/v1/tools/day-details`
- **Description**: Get detailed schedule for a specific day
- **Parameters**:
  - `itineraryId` (required): String
  - `dayNumber` (required): Integer
- **Returns**: All activities, meals, transport for that day
- **Use Cases**: Day-specific queries, schedule modifications

#### 4.3 Find Activity
- **Endpoint**: `POST /api/v1/tools/find-activity`
- **Description**: Search for specific activity in itinerary
- **Parameters**:
  - `itineraryId` (required): String
  - `query` (required): String (e.g., "lunch", "museum")
  - `dayNumber` (optional): Integer
- **Returns**: Matching activities with day/time info
- **Use Cases**: "When is lunch?", "Find the museum visit"

#### 4.4 Validate Itinerary
- **Endpoint**: `POST /api/v1/tools/validate-itinerary`
- **Description**: Check itinerary for issues
- **Parameters**:
  - `itineraryId` (required): String
- **Returns**: Validation errors, warnings, suggestions
- **Use Cases**: Quality checks, error detection, optimization hints

---

### 5. Time & Scheduling Tools
**Service**: `TimeFormatter`, Custom logic

#### 5.1 Check Time Conflicts
- **Endpoint**: `POST /api/v1/tools/check-conflicts`
- **Description**: Detect scheduling conflicts in a day
- **Parameters**:
  - `itineraryId` (required): String
  - `dayNumber` (required): Integer
- **Returns**: List of conflicts with details
- **Use Cases**: Schedule validation, conflict resolution

#### 5.2 Calculate Available Time
- **Endpoint**: `POST /api/v1/tools/available-time`
- **Description**: Find free time slots in a day
- **Parameters**:
  - `itineraryId` (required): String
  - `dayNumber` (required): Integer
- **Returns**: List of free time slots
- **Use Cases**: Adding activities, schedule optimization

#### 5.3 Suggest Best Time
- **Endpoint**: `POST /api/v1/tools/suggest-time`
- **Description**: Suggest optimal time for an activity
- **Parameters**:
  - `itineraryId` (required): String
  - `dayNumber` (required): Integer
  - `activityType` (required): String
  - `duration` (required): Integer (minutes)
- **Returns**: Recommended time slot with reasoning
- **Use Cases**: Smart scheduling, user convenience

---

### 6. Meal & Restaurant Tools
**Service**: `MealMetadataService`

#### 6.1 Find Meal Slot
- **Endpoint**: `POST /api/v1/tools/find-meal`
- **Description**: Find meal in itinerary
- **Parameters**:
  - `itineraryId` (required): String
  - `mealType` (required): String ("breakfast", "lunch", "dinner")
  - `dayNumber` (optional): Integer
- **Returns**: Meal details, location, time
- **Use Cases**: "Where's lunch?", "Change dinner location"

#### 6.2 Suggest Restaurants
- **Endpoint**: `POST /api/v1/tools/suggest-restaurants`
- **Description**: Get restaurant recommendations
- **Parameters**:
  - `location` (required): String or Coordinates
  - `cuisine` (optional): String
  - `priceLevel` (optional): Integer (1-4)
  - `mealType` (optional): String
- **Returns**: Restaurant list with ratings, prices, distance
- **Use Cases**: Meal planning, user preferences

---

### 7. Transport & Logistics Tools
**Service**: `TransportMetadataService`

#### 7.1 Get Transport Options
- **Endpoint**: `POST /api/v1/tools/transport-options`
- **Description**: Get available transport modes between locations
- **Parameters**:
  - `origin` (required): String
  - `destination` (required): String
  - `date` (optional): String
- **Returns**: Available modes (flight, train, bus, car) with estimates
- **Use Cases**: Transport planning, mode selection

#### 7.2 Estimate Transport Cost
- **Endpoint**: `POST /api/v1/tools/transport-cost`
- **Description**: Estimate cost for transport segment
- **Parameters**:
  - `origin` (required): String
  - `destination` (required): String
  - `mode` (required): String
  - `partySize` (optional): Integer
- **Returns**: Estimated cost range, duration, booking info
- **Use Cases**: Budget planning, transport decisions

---

### 8. User Preference Tools
**Service**: `UserDataService`

#### 8.1 Get User Preferences
- **Endpoint**: `POST /api/v1/tools/user-preferences`
- **Description**: Retrieve user's travel preferences
- **Parameters**:
  - `userId` (required): String
- **Returns**: Interests, budget tier, dietary restrictions, accessibility needs
- **Use Cases**: Personalization, recommendation filtering

#### 8.2 Check User Constraints
- **Endpoint**: `POST /api/v1/tools/user-constraints`
- **Description**: Get user's travel constraints
- **Parameters**:
  - `userId` (required): String
  - `itineraryId` (optional): String
- **Returns**: Constraints (mobility, dietary, time, budget)
- **Use Cases**: Validation, constraint checking

---

### 9. Weather & Seasonal Tools
**Service**: External API integration needed

#### 9.1 Get Weather Forecast
- **Endpoint**: `POST /api/v1/tools/weather-forecast`
- **Description**: Get weather forecast for destination
- **Parameters**:
  - `location` (required): String
  - `date` (required): String
- **Returns**: Temperature, conditions, precipitation, recommendations
- **Use Cases**: Activity planning, packing suggestions

#### 9.2 Check Best Season
- **Endpoint**: `POST /api/v1/tools/best-season`
- **Description**: Determine best time to visit destination
- **Parameters**:
  - `destination` (required): String
- **Returns**: Best months, weather patterns, peak/off-peak info
- **Use Cases**: Trip timing, seasonal recommendations

---

### 10. Analytics & Insights Tools
**Service**: `ItineraryMetricsTracker`

#### 10.1 Get Itinerary Stats
- **Endpoint**: `POST /api/v1/tools/itinerary-stats`
- **Description**: Get statistics about an itinerary
- **Parameters**:
  - `itineraryId` (required): String
- **Returns**: Activity count, cost breakdown, time distribution, balance metrics
- **Use Cases**: Optimization insights, balance checking

#### 10.2 Compare Itineraries
- **Endpoint**: `POST /api/v1/tools/compare-itineraries`
- **Description**: Compare two itineraries
- **Parameters**:
  - `itineraryId1` (required): String
  - `itineraryId2` (required): String
- **Returns**: Comparison metrics (cost, activities, balance)
- **Use Cases**: Revision comparison, A/B testing

---

## 🔧 Implementation Priority

### Phase 1: Core Tools (Week 1)
1. ✅ Calculate Itinerary Cost
2. Get Itinerary Summary
3. Get Day Details
4. Find Activity
5. Convert Currency

### Phase 2: Discovery Tools (Week 2)
6. Search Places
7. Get Place Details
8. Find Nearby Places
9. Calculate Distance
10. Check If Island

### Phase 3: Scheduling Tools (Week 3)
11. Check Time Conflicts
12. Calculate Available Time
13. Find Meal Slot
14. Validate Itinerary

### Phase 4: Advanced Tools (Week 4)
15. Suggest Best Time
16. Get Transport Options
17. Estimate Transport Cost
18. Get User Preferences
19. Get Itinerary Stats

### Phase 5: External Integration (Future)
20. Get Weather Forecast
21. Check Best Season
22. Suggest Restaurants (with real-time data)

---

## 📋 Tool Schema Format

Each tool should expose a schema endpoint following this format:

```json
{
  "name": "tool_name",
  "description": "What the tool does",
  "parameters": {
    "type": "object",
    "properties": {
      "param1": {
        "type": "string",
        "description": "Parameter description"
      }
    },
    "required": ["param1"]
  },
  "returns": {
    "type": "object",
    "description": "Return value description"
  }
}
```

---

## 🚀 Usage Patterns

### For Local Agents
```java
// Direct service call
BudgetSummary budget = budgetTracker.calculateBudget(itinerary, null, null, 1);
```

### For Google ADK Agents
```java
// HTTP tool call
Tool costTool = Tool.builder()
    .name("calculate_itinerary_cost")
    .description("Calculate total cost...")
    .function(params -> {
        return restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/calculate-cost",
            params,
            Map.class
        );
    })
    .build();
```

### For External Agents (via API)
```bash
curl -X POST http://localhost:8080/api/v1/tools/calculate-cost \
  -H "Content-Type: application/json" \
  -d '{"itineraryId": "abc123", "partySize": 2}'
```

---

## 🔐 Security Considerations

1. **Authentication**: All tool endpoints should require valid JWT token
2. **Authorization**: Verify user owns the itinerary before operations
3. **Rate Limiting**: Implement per-user rate limits (100 req/min)
4. **Input Validation**: Strict validation on all parameters
5. **Audit Logging**: Log all tool invocations for debugging

---

## 📊 Monitoring & Analytics

Track for each tool:
- Invocation count
- Success/failure rate
- Average response time
- Error types
- Most common parameters
- User satisfaction (implicit from retries)

---

## 🎯 Success Metrics

- **Coverage**: % of user queries answerable with tools
- **Accuracy**: % of tool responses that are correct
- **Performance**: P95 response time < 500ms
- **Reliability**: 99.9% uptime
- **Adoption**: Tool usage vs direct service calls

---

**Status**: Phase 1 - 1/5 tools implemented  
**Next**: Implement Get Itinerary Summary tool
