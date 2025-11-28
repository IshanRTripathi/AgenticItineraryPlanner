# Enrichment Location Fix - Complete Solution

## Problem Summary

Place enrichment was failing because nodes were being searched using incorrect geographic coordinates:
- **Symptom**: "Swiss National Museum" in Zurich (65km from center of Switzerland) was rejected as "too far"
- **Root Cause**: `day.location` field contained "Switzerland, Switzerland" instead of "Zurich" or "Zurich, Switzerland"
- **Impact**: Geocoding to center of Switzerland (46.818188, 8.227512) instead of Zurich, causing all Zurich places to be rejected

## Root Cause Analysis

### Data Flow
1. **SkeletonPlannerAgent** creates days with `day.location` field
2. **LLM** was setting `day.location = "Switzerland, Switzerland"` (country) instead of city name
3. **EnrichmentService** uses `day.location` to geocode search center
4. **GooglePlacesService** searches within 20km radius from geocoded point
5. **Validation** rejects results >30km from search center

### Why "Switzerland, Switzerland"?
- LLM was not explicitly instructed to set city-level location
- Prompts didn't emphasize the importance of city names
- JSON schema lacked description for the location field

## Complete Solution

### 0. Source of Truth: CityAllocationPlan (PRIMARY FIX)

**EnrichmentService.java** - Changed strategy to ALWAYS prefer CityAllocationPlan:
- CityAllocationPlan is set BEFORE skeleton generation
- Contains authoritative city names for each day
- `findDayLocationForNode()` now tries CityAllocationPlan FIRST
- Only falls back to `day.location` if CityAllocationPlan unavailable
- This makes the system resilient to LLM errors in setting `day.location`

### 1. Prompt Improvements (Prevention)

**DayByDayPlannerAgent.java**:
- Added requirement #6: Set city-level location field
- Added examples of correct vs incorrect location values
- Updated JSON schema with description

**SkeletonPlannerAgent.java**:
- Added critical rule #11 about city-level location
- Added explicit instruction in user prompt with city name from CityAllocationPlan
- Emphasized NOT to use country or destination name

### 2. Runtime Fix (Remediation)

**EnrichmentService.java**:
- Detects "Country, Country" format (both parts same after comma split)
- Falls back to CityAllocationPlan to get correct city name
- Logs warnings about data quality issues
- Continues with best available data

### 3. How It Works Now

#### Good Data Path (Future Itineraries)
```
day.location = "Zurich, Switzerland"
  ↓
Geocode "Zurich, Switzerland" → (47.3769, 8.5417) [Zurich center]
  ↓
Search within 20km of Zurich
  ↓
Find "Swiss National Museum" at 2km from center ✅
```

#### Bad Data Path (Existing Itineraries)
```
day.location = "Switzerland, Switzerland"
  ↓
Detect "Country, Country" format
  ↓
Lookup CityAllocationPlan for day 2 → "Zurich"
  ↓
Use "Zurich" for geocoding → (47.3769, 8.5417)
  ↓
Search within 20km of Zurich
  ↓
Find "Swiss National Museum" at 2km from center ✅
```

#### Worst Case (No CityAllocationPlan)
```
day.location = "Switzerland, Switzerland"
  ↓
Detect "Country, Country" format
  ↓
CityAllocationPlan lookup fails
  ↓
Log error, continue with "Switzerland, Switzerland"
  ↓
Geocode to center of Switzerland (poor results but doesn't crash)
```

## Key Insights

### Why Not Extract City Name?
Initially considered extracting first part before comma ("Switzerland, Switzerland" → "Switzerland"), but this doesn't help because "Switzerland" still geocodes to country center.

### Why Geocoding Works for "City, Country"
Google Geocoding API is smart:
- "Zurich, Switzerland" → Zurich coordinates ✅
- "Interlaken, Switzerland" → Interlaken coordinates ✅
- "Switzerland, Switzerland" → Switzerland center ❌ (ambiguous)

The API prioritizes the first part (city) when both parts are different, but when both parts are the same, it treats it as a single entity (country).

## Testing

### New Itineraries
1. Create new Switzerland trip
2. Check logs for: `🎯 [enrichNode] Final searchDestination: 'Zurich'` (or other city)
3. Verify enrichment succeeds for places in that city

### Existing Itineraries
1. Restart backend with new code
2. Trigger enrichment on existing itinerary
3. Check logs for:
   - `❌ CRITICAL DATA QUALITY ISSUE: searchDestination 'Switzerland, Switzerland' is in 'Country, Country' format!`
   - `✅ Fixed searchDestination from 'Switzerland, Switzerland' to 'Zurich' using CityAllocationPlan`
4. Verify enrichment now succeeds

## Files Modified

1. `src/main/java/com/tripplanner/agents/DayByDayPlannerAgent.java`
   - Updated system prompt with city-level location requirement
   - Updated JSON schema description

2. `src/main/java/com/tripplanner/agents/SkeletonPlannerAgent.java`
   - Updated system prompt with critical rule
   - Updated user prompt with explicit city name instruction

3. `src/main/java/com/tripplanner/service/EnrichmentService.java`
   - Added "Country, Country" detection
   - Added fallback to CityAllocationPlan
   - Added comprehensive logging
   - Added helper methods: `findDayNumberForNode()`, `getCityFromAllocationPlan()`

## Monitoring

Look for these log patterns:

**Success (Good Data)**:
```
🔍 [enrichNode] Initial searchDestination: 'Zurich, Switzerland'
🎯 [enrichNode] Final searchDestination: 'Zurich, Switzerland'
```

**Success (Bad Data Fixed)**:
```
🔍 [enrichNode] Initial searchDestination: 'Switzerland, Switzerland'
❌ CRITICAL DATA QUALITY ISSUE: searchDestination 'Switzerland, Switzerland' is in 'Country, Country' format!
✅ Fixed searchDestination from 'Switzerland, Switzerland' to 'Zurich' using CityAllocationPlan
🎯 [enrichNode] Final searchDestination: 'Zurich'
```

**Failure (Bad Data, No Fix Available)**:
```
🔍 [enrichNode] Initial searchDestination: 'Switzerland, Switzerland'
❌ CRITICAL DATA QUALITY ISSUE: searchDestination 'Switzerland, Switzerland' is in 'Country, Country' format!
❌ Could not find city from CityAllocationPlan. Place search will be inaccurate.
🎯 [enrichNode] Final searchDestination: 'Switzerland, Switzerland'
```

## Next Steps

1. **Restart backend** to apply changes
2. **Test with new itinerary** to verify prompts work
3. **Test with existing itinerary** to verify fallback works
4. **Monitor logs** for data quality issues
5. **Consider migration script** to fix existing bad data in database (optional)
