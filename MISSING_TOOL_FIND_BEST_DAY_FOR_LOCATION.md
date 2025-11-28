# Missing Tool: Find Best Day for Location/City

## Problem

When a user says "Add museum" or "Add [place] that belongs to city1" **without specifying a day**, the EditorAgent has no tool to determine which day(s) are allocated to that city.

### Current Behavior
- EditorAgent relies on the LLM to guess which day based on the itinerary context
- LLM sees the itinerary summary but has no structured way to query city allocations
- This can lead to incorrect day selection, especially in multi-city trips

### Example Scenarios

**Scenario 1: Multi-city trip**
```
Itinerary: 5 days in Switzerland
- Day 1-2: Zurich
- Day 3-4: Lucerne  
- Day 5: Interlaken

User: "Add Swiss Museum of Transport"
Location: Lucerne

Problem: Without a tool, LLM might add it to Day 1 (Zurich) instead of Day 3-4 (Lucerne)
```

**Scenario 2: Place search result**
```
User: "Show me museums"
Agent: Returns 3 suggestions with locations
User: Clicks "Add" on "National Museum" (located in Zurich)

Problem: Frontend sends "Add National Museum (Zurich)" but doesn't specify day
EditorAgent needs to determine that Zurich = Day 1-2
```

## Existing Infrastructure

The system **already has** the logic to determine city-to-day mapping:

### 1. CityAllocationPlan
**Location**: `src/main/java/com/tripplanner/dto/CityAllocationPlan.java`

Stores which cities are allocated to which days:
```java
public class CityAllocationPlan {
    private List<CityAllocation> allocations;
    // Each allocation has: cityName, startDay, endDay
}
```

### 2. LocationResolutionService
**Location**: `src/main/java/com/tripplanner/service/LocationResolutionService.java`

Has method to find city for a day:
```java
public String getCityFromAllocationPlan(NormalizedItinerary itinerary, int dayNumber) {
    // Finds which city is allocated to the given day
}
```

### 3. AgentData Storage
CityAllocationPlan is stored in itinerary's `agentData` section:
```json
{
  "agentData": {
    "cityAllocation": {
      "allocations": [
        {"cityName": "Zurich", "startDay": 1, "endDay": 2},
        {"cityName": "Lucerne", "startDay": 3, "endDay": 4}
      ]
    }
  }
}
```

## What's Missing

### Missing Tool: `find-best-day-for-location`

**Purpose**: Given a location/city name, find which day(s) are allocated to that city

**Input**:
```json
{
  "itineraryId": "it_xxx",
  "location": "Lucerne" // or "Swiss Museum of Transport, Lucerne"
}
```

**Output**:
```json
{
  "success": true,
  "matchedCity": "Lucerne",
  "days": [3, 4],
  "bestDay": 3, // First day in that city
  "confidence": "high", // high/medium/low
  "reasoning": "Location 'Lucerne' matches city allocation for days 3-4"
}
```

**Logic**:
1. Extract city name from location string
2. Look up CityAllocationPlan in itinerary's agentData
3. Find which allocation(s) match the city
4. Return the day range
5. Suggest "bestDay" (first day in that city, or day with most available time)

## Implementation Plan

### Step 1: Create Tool Endpoint
**File**: `src/main/java/com/tripplanner/controller/ToolsController.java`

```java
@PostMapping("/find-best-day-for-location")
public ResponseEntity<FindBestDayResult> findBestDayForLocation(
        @RequestBody FindBestDayRequest request,
        @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
    
    // 1. Load itinerary
    // 2. Get CityAllocationPlan from agentData
    // 3. Extract city from location string
    // 4. Find matching city allocation
    // 5. Return day range and best day
}
```

### Step 2: Create DTOs
**Files**: 
- `src/main/java/com/tripplanner/dto/tools/FindBestDayRequest.java`
- `src/main/java/com/tripplanner/dto/tools/FindBestDayResult.java`

### Step 3: Update EditorAgent Prompt
Add instruction to use this tool when day is not specified:

```
If the user doesn't specify which day to add an activity to:
1. Call find-best-day-for-location tool with the activity's location
2. Use the returned "bestDay" in your ChangeSet
3. If no match found, use the current day from the request context
```

### Step 4: Add Tool to GeminiClient Schema
Register the tool so LLM can call it during ChangeSet generation.

## Benefits

1. **Accurate day selection** - Activities added to correct city's days
2. **Better UX** - Users don't need to specify day for obvious cases
3. **Multi-city support** - Handles complex itineraries with multiple cities
4. **Confidence scoring** - Can warn user if location match is ambiguous
5. **Reusable** - Other agents can use this tool too

## Alternative: Enhance Prompt with City Allocation

Instead of a tool, could include city allocation in the prompt:

```
CITY ALLOCATION:
- Day 1-2: Zurich
- Day 3-4: Lucerne
- Day 5: Interlaken

When adding activities, match the location to the appropriate city's days.
```

**Pros**: Simpler, no new tool needed
**Cons**: 
- Less structured
- LLM might still make mistakes
- No confidence scoring
- Can't be reused by other agents

## Recommendation

**Implement the tool** for the following reasons:
1. More reliable than LLM guessing
2. Provides structured confidence scoring
3. Can be used by PlaceSearchAgent to pre-filter suggestions
4. Can be used by frontend to show "suggested day" when adding places
5. Enables future features like "auto-organize by city"

## Related Issues

- Place suggestions showing wrong day numbers (fixed in PLACE_SUGGESTIONS_DAY_FIX_COMPLETE.md)
- EditorAgent not persisting changes (fixed in EDITOR_AGENT_DIAGNOSTIC_FIX_APPLIED.md)

This tool would complement those fixes by ensuring activities are added to the correct day in the first place.
