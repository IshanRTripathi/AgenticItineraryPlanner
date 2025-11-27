# Tool Caching Strategy & Agent Access Control
**Date:** 2025-11-27  
**Status:** Implementation In Progress  
**Purpose:** Define caching TTLs and agent-tool access mappings based on actual code analysis

---

## Quick Reference

### Implementation Status
- ✅ **Cache Infrastructure:** Complete (Firestore subcollection)
- ✅ **Weather Caching:** Working (WeatherService)
- ⏳ **Remaining Services:** GeocodingService, GoogleMapsDistanceService, GooglePlacesService, OpeningHoursService
- 📊 **Expected Savings:** 90% cost reduction ($0.80 → $0.08 per itinerary)

### Key Concepts

**Type Safety:** Generic `<T> getOrCompute()` works for all 25 tools
- Compile-time: Generic method signature
- Runtime: Type metadata validation (`requestClass`, `resultClass`)
- Serialization: Jackson handles all DTOs automatically

**Deduplication:** Prevents concurrent duplicate calls
- Uses `ConcurrentHashMap<String, CompletableFuture<?>> pendingRequests`
- If request in flight, wait for existing result (30s timeout)
- Eliminates race conditions when multiple agents call same tool

**Storage:** Firestore subcollection `itineraries/{itineraryId}/toolCache/{cacheKey}`
- Each cache entry is a separate document
- No 1MB document size limit
- Auto-cleanup of expired entries

**Cache Key Pattern:** Deterministic and normalized
```java
ToolCacheKeyGenerator.forWeatherTiming(activity, location, date, duration)
// → "weather:timing:burj-khalifa:dubai:2025-08-15:120"
```

---

## Part 1: Tool Caching Strategy

### ❌ NEVER CACHE (10 tools)

These tools depend on current itinerary state and must always execute fresh:

| Tool | Why Never Cache | Current Implementation |
|------|-----------------|------------------------|
| **generate-node-id** | Must be unique every time | ✅ No caching |
| **check-user-constraints** | Budget/constraints change with each node | ✅ No caching |
| **check-conflicts** | Schedule changes with each edit | ✅ No caching |
| **validate-schema** | Each JSON is unique | ✅ No caching |
| **calculate-cost** | Cost changes with each node added | ✅ No caching |
| **validate-completeness** | Itinerary evolves during generation | ✅ No caching |
| **validate-timing** | Schedule changes with each node | ✅ No caching |
| **update-node** | Mutation operation | ✅ No caching |
| **get-available-slots** | Free time depends on current schedule | ✅ No caching |
| **convert-currency** | Cache exchange rates, not calls | ⚠️ Rates cached separately |

---

### ✅ ALWAYS CACHE - Static Data (30 days TTL)

These fetch immutable external data:

| Tool | TTL | Why | Current Status | API Cost |
|------|-----|-----|----------------|----------|
| **geocode** | 30 days | Coordinates never change | ✅ @Cacheable in GooglePlacesService | $0.005/call |
| **calculate-distance** | 30 days | Distance between points is static | ❌ **MISSING** | $0.005/call |
| **estimate-duration** | Permanent | Heuristic logic is static | ✅ No API calls | Free |

**Implementation Status:**
- geocode: Already cached via `@Cacheable(value = "geocoding")`
- calculate-distance: **NEEDS CACHING** - Add to ToolCacheService
- estimate-duration: No caching needed (pure computation)

---

### ✅ ALWAYS CACHE - Slow-Changing Data (7 days TTL)

These fetch data that changes rarely:

| Tool | TTL | Why | Current Status | API Cost |
|------|-----|-----|----------------|----------|
| **check-opening-hours** | 7 days | Hours change seasonally | ✅ @Cacheable in GooglePlacesService | $0.017/call |
| **get-similar-activities** | 7 days | Similar places don't change often | ❌ **MISSING** | $0.032/call |
| **check-capacity** | 30 days | Venue capacity is fixed | ✅ Heuristic-based | Free |

**Implementation Status:**
- check-opening-hours: Already cached via `@Cacheable(value = "placeDetails")`
- get-similar-activities: **NEEDS CACHING** - Add to ToolCacheService
- check-capacity: No API calls (heuristic-based)

---

### ✅ ALWAYS CACHE - Dynamic Data (1-24 hours TTL)

These fetch data that changes periodically:

| Tool | TTL | Why | Current Status | API Cost |
|------|-----|-----|----------------|----------|
| **get-weather** | 1 hour | Weather changes hourly | ✅ Cached via ToolCacheService | $0.0001/call |
| **suggest-best-time** | 1 hour | Based on weather (stable) | ✅ Cached via ToolCacheService | $0.0001/call |
| **suggest-restaurants** | 24 hours | Restaurant list stable | ✅ @Cacheable in GooglePlacesService | $0.032/call |
| **get-transport-options** | 1 hour | Transport pricing changes | ✅ Heuristic-based | Free |

**Implementation Status:**
- get-weather: Already cached via ToolCacheService with 1 hour TTL
- suggest-best-time: Already cached via ToolCacheService with 1 hour TTL
- suggest-restaurants: Already cached via `@Cacheable(value = "placeSearch")`
- get-transport-options: No API calls (heuristic-based)

---

### 🟡 SHORT CACHE - Itinerary-Scoped (5 minutes TTL)

These query mutable itinerary data:

| Tool | TTL | Why | Current Status |
|------|-----|-----|----------------|
| **get-itinerary-summary** | 5 min | Summary stable during generation | ✅ No caching needed |
| **get-day-details** | 5 min | Day stable during generation | ✅ No caching needed |
| **find-node** | 5 min | Node location stable briefly | ✅ No caching needed |
| **get-nodes-by-type** | 5 min | Node list stable briefly | ✅ No caching needed |
| **optimize-route** | 30 min | Same nodes → same route | ✅ No caching needed |

**Note:** These tools read from Firestore which has its own caching. Additional caching not needed.

---

## Part 2: Agent-Tool Access Control

### Current Agent Implementations

Based on code analysis of actual agent files:

#### 1. **ActivityAgent**
**Current Tool Usage:**
- ✅ Uses `suggest-best-time` via RestTemplate (weather-based scheduling)
- ✅ Uses `WeatherService` directly (injected)
- ✅ Uses `ActivitySuitabilityService` directly (injected)
- ✅ Uses `ToolCacheService` for caching (optional injection)

**Recommended Tools:**
```yaml
ActivityAgent:
  universal_tools: true  # Can use all universal tools
  specific_tools:
    - suggest-best-time      # Already using
    - get-weather            # Via WeatherService
    - check-opening-hours    # Should add
    - get-similar-activities # Should add
    - geocode                # Via GeocodingService
    - calculate-distance     # Should add
```

**Feature Flags:**
- `features.weather-tools.enabled` - Controls weather-based scheduling
- `features.weather-tools.fallback-on-error` - Graceful degradation
- `features.weather-tools.log-comparisons` - Debug logging

---

#### 2. **MealAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently
- ✅ Uses `MealMetadataService` for metadata

**Recommended Tools:**
```yaml
MealAgent:
  universal_tools: true
  specific_tools:
    - suggest-restaurants    # Should add
    - check-opening-hours    # Should add
    - check-capacity         # Should add
    - geocode                # Should add
```

**Missing Integration:** MealAgent should use suggest-restaurants for dietary verification

---

#### 3. **TransportAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently
- ✅ Uses `GoogleMapsDistanceService` directly
- ✅ Uses `GeographyService` directly
- ✅ Uses `TransportMetadataService` directly

**Recommended Tools:**
```yaml
TransportAgent:
  universal_tools: true
  specific_tools:
    - get-transport-options  # Should add
    - calculate-distance     # Via GoogleMapsDistanceService
    - geocode                # Via GeocodingService
    - optimize-route         # Should add
```

---

#### 4. **CostEstimatorAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently
- ✅ Uses `BudgetTracker` directly
- ✅ Uses `CurrencyConversionService` directly

**Recommended Tools:**
```yaml
CostEstimatorAgent:
  universal_tools: true
  specific_tools:
    - calculate-cost         # Should use tool version
    - convert-currency       # Already using service
```

---

#### 5. **EnrichmentAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently
- ✅ Uses `GooglePlacesService` directly
- ✅ Uses `EnrichmentProtocolHandler` directly

**Recommended Tools:**
```yaml
EnrichmentAgent:
  universal_tools: true
  specific_tools:
    - geocode                # Via GooglePlacesService
    - check-opening-hours    # Via GooglePlacesService
```

---

#### 6. **SkeletonPlannerAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently
- ✅ Uses `CityAllocationAgent` directly

**Recommended Tools:**
```yaml
SkeletonPlannerAgent:
  universal_tools: true
  specific_tools:
    - generate-node-id       # Should add
    - check-user-constraints # Should add
    - validate-schema        # Should add
```

---

#### 7. **DayByDayPlannerAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently
- ✅ Orchestrates other agents

**Recommended Tools:**
```yaml
DayByDayPlannerAgent:
  universal_tools: true
  all_tools: true  # Orchestrator role - needs access to all tools
```

---

#### 8. **EditorAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently
- ✅ Uses `ChangeEngine` directly

**Recommended Tools:**
```yaml
EditorAgent:
  universal_tools: true
  specific_tools:
    - update-node            # Should add
    - check-conflicts        # Should add
    - validate-completeness  # Should add
    - calculate-cost         # Should add
```

---

#### 9. **ExplainAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently

**Recommended Tools:**
```yaml
ExplainAgent:
  universal_tools: true
  specific_tools:
    - get-itinerary-summary  # Should add
    - get-day-details        # Should add
    - find-node              # Should add
    - calculate-cost         # Should add
```

---

#### 10. **BookingAgent**
**Current Tool Usage:**
- ❌ No direct tool calls currently

**Recommended Tools:**
```yaml
BookingAgent:
  universal_tools: true
  specific_tools:
    - check-opening-hours    # Should add
    - check-capacity         # Should add
    - geocode                # Should add
```

---

## Part 3: Universal Tools (Available to ALL Agents)

These tools are safe, cheap, and universally useful:

```yaml
universal_tools:
  - generate-node-id
  - validate-schema
  - check-user-constraints
  - convert-currency
  - geocode
  - calculate-distance
  - find-node
  - get-nodes-by-type
  - get-day-details
  - get-itinerary-summary
```

---

## Part 4: Expensive Tools (Require Explicit Permission)

These tools have API costs and should be restricted:

| Tool | Cost/Call | Restrict To | Reason |
|------|-----------|-------------|--------|
| **suggest-restaurants** | $0.032 | MealAgent, DayByDayPlannerAgent | Google Places Text Search |
| **get-similar-activities** | $0.032 | ActivityAgent, DayByDayPlannerAgent | Google Places Text Search |
| **check-opening-hours** | $0.017 | ActivityAgent, MealAgent, BookingAgent | Google Places Details |
| **calculate-distance** | $0.005 | TransportAgent, DayByDayPlannerAgent | Google Maps Distance Matrix |
| **geocode** | $0.005 | All agents (universal) | Google Maps Geocoding |

**Total potential cost per itinerary:** $0.10 - $0.50 depending on calls  
**With caching (90% hit rate):** $0.01 - $0.05 per itinerary

---

## Part 5: Implementation Recommendations

### Option 1: Start with Open Access + Monitoring (RECOMMENDED)

**Implementation:** 0 hours (already done)

```yaml
# No restrictions, just monitoring
tool_access:
  mode: open
  monitoring:
    enabled: true
    log_agent_id: true
    track_costs: true
    alert_on_expensive_calls: true
```

**Advantages:**
- Zero implementation time
- Maximum flexibility
- Gather real usage data
- Identify actual problems

**Add to each tool endpoint:**
```java
@PostMapping("/suggest-best-time")
public ResponseEntity<SuggestBestTimeResult> suggestBestTime(
        @RequestBody SuggestBestTimeRequest request,
        @RequestHeader(value = "X-Agent-Id", required = false) String agentId) {
    
    // Log tool usage
    logger.info("Tool: suggest-best-time, Agent: {}, Itinerary: {}", 
               agentId != null ? agentId : "unknown", request.getItineraryId());
    
    // Track metrics
    metricsTracker.trackToolUsage("suggest-best-time", agentId, 0.0001);
    
    // ... rest of implementation
}
```

---

### Option 2: Add Restrictions Later (If Needed)

**Implementation:** 4-6 hours (only if problems found)

After 2-4 weeks of monitoring, if you see:
- Agents calling wrong tools
- Excessive API costs
- Performance issues

Then add restrictions:

```java
// In AgentCapabilities
@Override
public AgentCapabilities getCapabilities() {
    AgentCapabilities capabilities = new AgentCapabilities();
    
    // Declare allowed tools
    capabilities.addAllowedTool("suggest-best-time");
    capabilities.addAllowedTool("check-opening-hours");
    capabilities.setAllowUniversalTools(true);
    
    return capabilities;
}
```

---

## Part 6: Critical Caching Gaps to Fix

### 🔥 URGENT: Add Caching to Expensive Tools

**1. get-similar-activities** - $0.032/call
```java
// In ToolsController.java
if (toolCacheService != null) {
    String cacheKey = ToolCacheKeyGenerator.forSimilarActivities(
        request.getActivityName(), request.getLocation());
    
    return toolCacheService.getOrCompute(
        request.getItineraryId(),
        ToolType.GET_SIMILAR_ACTIVITIES.getValue(),
        cacheKey,
        request,
        () -> callGooglePlacesAPI(request),
        GetSimilarActivitiesResult.class
    );
}
```

**2. calculate-distance** - $0.005/call
```java
// In ToolsController.java
if (toolCacheService != null) {
    String cacheKey = ToolCacheKeyGenerator.forDistance(
        request.getOrigin(), request.getDestination(), request.getMode());
    
    return toolCacheService.getOrCompute(
        request.getItineraryId(),
        ToolType.CALCULATE_DISTANCE.getValue(),
        cacheKey,
        request,
        () => callGoogleMapsAPI(request),
        DistanceCalculationResult.class
    );
}
```

**Estimated Savings:** $260/month per 1000 itineraries

---

## Part 7: Configuration File Structure

### application.yml
```yaml
# Tool access control
tool-access:
  mode: open  # open | restricted | hybrid
  monitoring:
    enabled: true
    log-agent-id: true
    track-costs: true
    
# Tool caching
tool-cache:
  enabled: true
  provider: firestore  # firestore | redis
  ttl:
    geocode: 2592000      # 30 days
    distance: 2592000     # 30 days
    opening-hours: 604800 # 7 days
    similar-activities: 604800  # 7 days
    weather: 3600         # 1 hour
    suggest-best-time: 3600  # 1 hour
    restaurants: 86400    # 24 hours
    
# Feature flags
features:
  weather-tools:
    enabled: true
    fallback-on-error: true
    log-comparisons: false
```

---

## Part 8: Cost Analysis

### Current API Costs (Without Caching)

| API | Cost/Call | Calls/Itinerary | Total |
|-----|-----------|-----------------|-------|
| Google Places Text Search | $0.032 | 15 | $0.48 |
| Google Places Details | $0.017 | 12 | $0.20 |
| Google Maps Geocoding | $0.005 | 3 | $0.02 |
| Google Maps Distance | $0.005 | 20 | $0.10 |
| OpenWeather | $0.0001 | 10 | $0.001 |
| **Total** | | | **$0.80** |

### With Proper Caching (90% hit rate)

| API | Cost/Call | Calls/Itinerary | Cache Hits | Actual Calls | Total |
|-----|-----------|-----------------|------------|--------------|-------|
| Google Places Text Search | $0.032 | 15 | 13.5 | 1.5 | $0.048 |
| Google Places Details | $0.017 | 12 | 10.8 | 1.2 | $0.020 |
| Google Maps Geocoding | $0.005 | 3 | 2.7 | 0.3 | $0.002 |
| Google Maps Distance | $0.005 | 20 | 18 | 2 | $0.010 |
| OpenWeather | $0.0001 | 10 | 9 | 1 | $0.0001 |
| **Total** | | | | | **$0.08** |

**Savings:** $0.72 per itinerary (90% reduction)  
**Monthly savings (1000 itineraries):** $720

---

## Part 9: Monitoring Queries

### Track Tool Usage
```sql
-- BigQuery query for tool usage by agent
SELECT
  tool_name,
  agent_id,
  COUNT(*) as call_count,
  SUM(cost_usd) as total_cost,
  AVG(response_time_ms) as avg_response_time
FROM tool_usage_logs
WHERE DATE(timestamp) = CURRENT_DATE()
GROUP BY tool_name, agent_id
ORDER BY total_cost DESC
```

### Track Cache Hit Rate
```sql
-- BigQuery query for cache effectiveness
SELECT
  tool_name,
  COUNT(*) as total_calls,
  SUM(CASE WHEN cache_hit THEN 1 ELSE 0 END) as cache_hits,
  ROUND(100.0 * SUM(CASE WHEN cache_hit THEN 1 ELSE 0 END) / COUNT(*), 2) as hit_rate_percent
FROM tool_cache_logs
WHERE DATE(timestamp) >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
GROUP BY tool_name
ORDER BY total_calls DESC
```

---

## Part 10: Implementation Checklist

### Phase 1: Add Caching to Services (Priority Order)

**1. GeocodingService** (30 min)
```java
@Autowired(required = false)
private ToolCacheService toolCacheService;

public GeocodeResponse geocode(String itineraryId, String address) {
    if (toolCacheService != null && itineraryId != null) {
        String cacheKey = ToolCacheKeyGenerator.forGeocode(address);
        return toolCacheService.getOrCompute(
            itineraryId,
            ToolType.GEOCODE.getValue(),
            cacheKey,
            Map.of("address", address),
            () -> callGoogleMapsAPI(address),
            GeocodeResponse.class
        );
    }
    return callGoogleMapsAPI(address);
}
```

**2. GoogleMapsDistanceService** (30 min)
- Same pattern as above
- Use `ToolCacheKeyGenerator.forDistance(origin, destination, mode)`
- TTL: 30 days

**3. GooglePlacesService** (45 min)
- Similar activities: `ToolCacheKeyGenerator.forSimilarActivities()`
- Restaurant search: Already cached via `@Cacheable`
- Opening hours: Already cached via `@Cacheable`

**4. OpeningHoursService** (30 min)
- If not using GooglePlacesService
- Use `ToolCacheKeyGenerator.forOpeningHours(placeId, date)`

### Phase 2: Add Agent ID Logging (30 min)

Add to all tool endpoints in ToolsController:
```java
@PostMapping("/tool-name")
public ResponseEntity<Result> toolName(
    @RequestBody Request request,
    @RequestHeader(value = "X-Agent-Id", required = false) String agentId) {
    
    logger.info("Tool: tool-name, Agent: {}, Itinerary: {}", 
        agentId != null ? agentId : "unknown", request.getItineraryId());
    // ... rest
}
```

### Phase 3: Monitor & Optimize (2-4 weeks)

**Metrics to track:**
- Cache hit rate per tool (target: >70%)
- API cost reduction (target: >60%)
- Tool usage by agent
- Cache memory usage

**BigQuery queries provided in Part 9**

---

## Summary

### Caching Status
- ✅ **Already Cached:** weather, suggest-best-time (WeatherService)
- ⏳ **In Progress:** geocode, calculate-distance, similar-activities, opening-hours
- ✅ **No Cache Needed:** 10 state-dependent tools (generate-node-id, check-conflicts, etc.)

### Agent Access Control
- ✅ **Current:** Open access (no restrictions)
- ⏳ **Next:** Add agent ID logging
- 🔮 **Future:** Add restrictions only if problems found

### Cost Analysis
- **Without caching:** $0.80/itinerary
  - Google Places Text Search: $0.48
  - Google Places Details: $0.20
  - Google Maps Distance: $0.10
  - Google Maps Geocoding: $0.02
- **With caching (90% hit rate):** $0.08/itinerary
- **Savings:** 90% reduction = $720/month per 1000 itineraries

### Next Steps
1. ✅ Subcollection fix (DONE)
2. ⏳ Add caching to 4 services (2-3 hours)
3. ⏳ Add agent ID logging (30 minutes)
4. ⏳ Monitor for 2-4 weeks
5. 🔮 Add restrictions only if needed

---

**Status:** Implementation in progress  
**Estimated effort:** 2.5-3 hours remaining  
**Expected savings:** $720/month per 1000 itineraries
