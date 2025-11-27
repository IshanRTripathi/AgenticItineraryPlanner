# Tool Cache Lifecycle - Complete Flow

## Architecture Overview

The cache system uses a **two-tier architecture**:

1. **Session Cache (In-Memory)** - Fast, temporary storage in backend RAM
2. **Persistent Cache (Firestore)** - Permanent storage in database

```
┌─────────────────────────────────────────────────────────────┐
│                    Backend Server (RAM)                      │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  FirestoreToolCacheService                         │    │
│  │                                                     │    │
│  │  sessionCache: ConcurrentHashMap<String,           │    │
│  │                ItineraryCacheData>                 │    │
│  │                                                     │    │
│  │  ┌──────────────────────────────────────────┐     │    │
│  │  │ "itinerary-abc123" → ItineraryCacheData  │     │    │
│  │  │   ├─ toolResults: ConcurrentHashMap      │     │    │
│  │  │   │   ├─ "weather:zurich:2025-06-15"     │     │    │
│  │  │   │   ├─ "geocode:eiffel-tower"          │     │    │
│  │  │   │   └─ "distance:paris:london"         │     │    │
│  │  │   ├─ totalHits: 15                       │     │    │
│  │  │   ├─ totalMisses: 3                      │     │    │
│  │  │   └─ lastAccessTime: 1732723200000       │     │    │
│  │  └──────────────────────────────────────────┘     │    │
│  │                                                     │    │
│  │  Max Size: 100 itineraries (LRU eviction)         │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↕
                    (Lazy Load / Save)
                            ↕
┌─────────────────────────────────────────────────────────────┐
│                    Firestore (Database)                      │
│                                                              │
│  itineraries/{itineraryId}/toolCache/{cacheKey}             │
│                                                              │
│  ├─ weather:zurich-switzerland:2025-06-15                   │
│  │   ├─ toolType: "get-weather"                             │
│  │   ├─ cachedAt: 1732723200000                             │
│  │   ├─ expiresAt: 1732726800000                            │
│  │   ├─ ttlSeconds: 3600                                    │
│  │   ├─ requestJson: "{...}"                                │
│  │   └─ resultJson: "{...}"                                 │
│  │                                                           │
│  ├─ geocode:eiffel-tower-paris                              │
│  └─ distance:paris-france:london-uk:driving                 │
└─────────────────────────────────────────────────────────────┘
```

## Complete Lifecycle Flow

### 1. **First Tool Call (Cache Miss)**

```
Agent calls WeatherService.getWeather("abc123", "Zurich", "2025-06-15")
    ↓
WeatherService checks: toolCacheService != null && itineraryId != null
    ↓
Calls: toolCacheService.getOrCompute(...)
    ↓
FirestoreToolCacheService.getOrCompute():
    ├─ Step 1: Check if itinerary cache is loaded in sessionCache
    │   └─ If NOT loaded → loadFromFirestoreSync("abc123")
    │       ├─ Query: itineraries/abc123/toolCache (subcollection)
    │       ├─ Load all documents into ItineraryCacheData
    │       ├─ Filter out expired entries (delete from Firestore)
    │       └─ Store in sessionCache["abc123"]
    │
    ├─ Step 2: Check session cache for this specific key
    │   └─ cacheData.getToolResult("weather:zurich:2025-06-15")
    │       └─ Returns: null (CACHE MISS)
    │
    ├─ Step 3: Execute tool call
    │   └─ toolCall.get() → Calls OpenWeatherMap API
    │       └─ Returns: WeatherData object
    │
    ├─ Step 4: Create CachedToolResult
    │   ├─ Serialize request to JSON
    │   ├─ Serialize result to JSON
    │   ├─ Set TTL (3600 seconds for weather)
    │   ├─ Calculate expiresAt timestamp
    │   └─ Add metadata (execution time, etc.)
    │
    ├─ Step 5: Save to session cache (immediate)
    │   └─ cacheData.putToolResult(cacheKey, cachedResult)
    │       └─ Stored in RAM for instant access
    │
    └─ Step 6: Save to Firestore (async, non-blocking)
        └─ CompletableFuture.runAsync(() -> {
            firestore.collection("itineraries")
                .document("abc123")
                .collection("toolCache")
                .document("weather:zurich:2025-06-15")
                .set(cachedResult)
        })
```

**Performance**: ~450ms (API call) + ~100ms (Firestore write in background)

### 2. **Second Tool Call (Cache Hit)**

```
Agent calls WeatherService.getWeather("abc123", "Zurich", "2025-06-15")
    ↓
WeatherService → toolCacheService.getOrCompute(...)
    ↓
FirestoreToolCacheService.getOrCompute():
    ├─ Step 1: Get itinerary cache from sessionCache
    │   └─ Already loaded, instant retrieval
    │
    ├─ Step 2: Check session cache for key
    │   └─ cacheData.getToolResult("weather:zurich:2025-06-15")
    │       └─ Returns: CachedToolResult (CACHE HIT!)
    │
    ├─ Step 3: Validate cache entry
    │   └─ cached.isValid() checks:
    │       ├─ Is expiresAt > currentTime? ✓
    │       └─ Is resultJson not null? ✓
    │
    ├─ Step 4: Deserialize result
    │   └─ cached.getResultAs(WeatherData.class, objectMapper)
    │       ├─ Type safety check (class name matches)
    │       └─ JSON → WeatherData object
    │
    └─ Step 5: Record hit and return
        ├─ cacheData.recordHit()
        └─ Return result (NO API CALL!)
```

**Performance**: ~5-10ms (in-memory lookup + deserialization)

### 3. **Cache Expiration (TTL Reached)**

```
Time passes... expiresAt timestamp is reached
    ↓
Next tool call for same key:
    ├─ Step 1: Retrieve from session cache
    │   └─ Entry exists but...
    │
    ├─ Step 2: Validate cache entry
    │   └─ cached.isValid() checks:
    │       └─ Is expiresAt > currentTime? ✗ EXPIRED!
    │
    ├─ Step 3: Treat as cache miss
    │   ├─ Execute tool call again (fresh data)
    │   ├─ Create new CachedToolResult with new TTL
    │   ├─ Update session cache
    │   └─ Overwrite Firestore document (async)
    │
    └─ Old expired entry is replaced
```

**TTL Values by Tool Type:**
- Weather: 1 hour (3600s)
- Geocoding: 30 days (2592000s)
- Distance: 30 days (2592000s)
- Opening hours: 7 days (604800s)
- Transport: 1 hour (3600s)

### 4. **Server Restart (Session Cache Lost)**

```
Server restarts → sessionCache is empty (RAM cleared)
    ↓
First tool call after restart:
    ├─ Step 1: Check sessionCache for itinerary
    │   └─ Not found (empty after restart)
    │
    ├─ Step 2: Lazy load from Firestore
    │   └─ loadFromFirestoreSync("abc123")
    │       ├─ Query: itineraries/abc123/toolCache
    │       ├─ Load all documents
    │       ├─ Filter expired entries (delete them)
    │       └─ Populate sessionCache["abc123"]
    │
    └─ Step 3: Continue with cache lookup
        └─ Cache hit if entry exists and not expired
```

**Performance**: First call ~200-500ms (Firestore load), subsequent calls ~5-10ms

### 5. **LRU Eviction (Session Cache Full)**

```
sessionCache reaches 100 itineraries (MAX_SESSION_CACHE_SIZE)
    ↓
New itinerary needs to be cached:
    ├─ Step 1: Find oldest entry by lastAccessTime
    │   └─ Iterate through sessionCache entries
    │       └─ Find itinerary with oldest lastAccessTime
    │
    ├─ Step 2: Evict oldest entry
    │   └─ sessionCache.remove("old-itinerary-id")
    │       └─ Removed from RAM (Firestore data intact)
    │
    └─ Step 3: Add new entry
        └─ sessionCache.put("new-itinerary-id", cacheData)
```

**Note**: Evicted data is NOT lost - it's still in Firestore and will be reloaded if needed.

### 6. **Scheduled Cleanup (Daily at 2 AM)**

```
CacheCleanupScheduler runs (cron: "0 0 2 * * ?")
    ↓
Calls: toolCacheService.cleanupAllExpired()
    ↓
For each itinerary in sessionCache:
    ├─ Get all cached entries
    ├─ Check each entry: cached.isExpired()
    ├─ If expired:
    │   ├─ Remove from session cache
    │   └─ Delete from Firestore subcollection
    │
    └─ Log: "Removed X expired cache entries"
```

**Purpose**: Prevent Firestore storage bloat from expired entries

## Key Questions Answered

### Q: When is cache loaded?
**A: Lazy loading** - Only when first tool call is made for that itinerary.

### Q: Where is cache stored temporarily?
**A: In-memory** - `ConcurrentHashMap<String, ItineraryCacheData>` in backend RAM.

### Q: How fast is cache access?
**A: Very fast**:
- Session cache hit: ~5-10ms (RAM lookup)
- Session cache miss but Firestore hit: ~200-500ms (first load)
- Complete cache miss: API call time + ~100ms (Firestore save)

### Q: When is TTL checked?
**A: On every access** - `cached.isValid()` checks if `expiresAt > currentTime`.

### Q: How are expired entries updated?
**A: Automatic replacement**:
1. Expired entry detected during access
2. Tool call executed to get fresh data
3. New entry created with new TTL
4. Session cache updated immediately
5. Firestore document overwritten (async)

### Q: What happens on server restart?
**A: Graceful recovery**:
1. Session cache is empty (RAM cleared)
2. First tool call triggers lazy load from Firestore
3. Valid entries restored to session cache
4. Expired entries deleted during load
5. System continues normally

### Q: Is cache shared across requests?
**A: Yes** - Session cache is singleton, shared by all requests/agents for same itinerary.

### Q: Is cache thread-safe?
**A: Yes** - Uses `ConcurrentHashMap` and atomic operations for thread safety.

## Performance Characteristics

| Operation | Latency | Notes |
|-----------|---------|-------|
| Session cache hit | 5-10ms | RAM lookup + deserialization |
| Session cache miss (Firestore hit) | 200-500ms | First load from Firestore |
| Complete cache miss | API time + 100ms | API call + async Firestore save |
| Cache write | ~100ms (async) | Non-blocking background write |
| Cache invalidation | ~50ms | Delete from RAM + async Firestore delete |
| Scheduled cleanup | Varies | Runs at 2 AM, minimal impact |

## Memory Management

**Session Cache Size Limit**: 100 itineraries

**Typical Memory Usage**:
- Empty cache: ~1 KB
- 10 entries per itinerary: ~50 KB per itinerary
- 100 itineraries: ~5 MB total
- Max realistic: ~10-20 MB

**LRU Eviction**: Automatically removes least recently used itineraries when limit reached.

## Best Practices

1. **Always pass itineraryId** - Required for caching to work
2. **Use deterministic cache keys** - Same input = same key
3. **Set appropriate TTLs** - Balance freshness vs cache effectiveness
4. **Monitor cache hit rates** - Use CacheMonitoringController
5. **Preload cache for edits** - Call `preloadCache()` when loading itinerary for editing

## Troubleshooting

**Cache not working?**
- Check: `cache.tool-results.enabled: true` in application.yml
- Check: `itineraryId` is not null
- Check: `toolCacheService` is injected (not null)
- Check logs for: "Cache HIT" or "Cache MISS"

**Cache hit rate low?**
- Check TTL values (may be too short)
- Check if cache keys are deterministic
- Check if itineraryId is consistent across calls

**Memory issues?**
- Reduce `MAX_SESSION_CACHE_SIZE` (default: 100)
- Reduce TTL values to expire entries faster
- Monitor with `/api/v1/cache/stats`
