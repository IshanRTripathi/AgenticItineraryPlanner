# Tool Cache: Grey Areas & Edge Cases

## Critical Issues to Address

### 1. Concurrent Modifications

**Problem:** Multiple agents running in parallel might try to update cache simultaneously.

**Scenario:**
```
Time T0: ActivityAgent checks cache for "Burj Khalifa" → MISS
Time T1: EnrichmentAgent checks cache for "Burj Khalifa" → MISS
Time T2: ActivityAgent calls tool, gets result
Time T3: EnrichmentAgent calls tool, gets result (DUPLICATE!)
Time T4: ActivityAgent saves to cache
Time T5: EnrichmentAgent saves to cache (OVERWRITES)
```

**Solutions:**

**Option A: Optimistic Locking (Recommended)**
```java
public <T> T getOrCompute(...) {
    // Try to acquire lock
    String lockKey = "lock:" + cacheKey;
    boolean acquired = tryAcquireLock(lockKey, Duration.ofSeconds(30));
    
    if (!acquired) {
        // Another agent is computing, wait and retry
        Thread.sleep(100);
        return getOrCompute(...); // Retry
    }
    
    try {
        // Double-check cache after acquiring lock
        CachedToolResult cached = loadFromFirestore(itineraryId, cacheKey);
        if (cached != null && cached.isValid()) {
            return objectMapper.convertValue(cached.getResult(), resultType);
        }
        
        // Compute
        T result = toolCall.get();
        
        // Save
        saveToFirestore(itineraryId, cacheKey, result);
        
        return result;
    } finally {
        releaseLock(lockKey);
    }
}
```

**Option B: Deduplication via Request Queue**
```java
// Pending requests map
private final ConcurrentHashMap<String, CompletableFuture<?>> pendingRequests;

public <T> T getOrCompute(...) {
    String requestKey = itineraryId + ":" + cacheKey;
    
    // Check if request already in flight
    CompletableFuture<T> pending = (CompletableFuture<T>) 
        pendingRequests.get(requestKey);
    
    if (pending != null) {
        // Wait for existing request to complete
        return pending.get();
    }
    
    // Create new request
    CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
        // Check cache
        // Call tool if needed
        // Save result
        return result;
    });
    
    pendingRequests.put(requestKey, future);
    
    try {
        return future.get();
    } finally {
        pendingRequests.remove(requestKey);
    }
}
```

**Recommendation:** Use Option B (deduplication) - simpler and more efficient.

---


### 2. Cache Invalidation

**Problem:** When should cached data be invalidated?

**Scenarios:**

**A. Itinerary Modified**
```
User changes: Dubai trip from August → December
Cached weather for August is now WRONG
```

**Solution:** Invalidate weather cache when dates change
```java
public void onItineraryDatesChanged(String itineraryId, 
                                    LocalDate oldStart, LocalDate newStart) {
    // Invalidate all date-dependent caches
    toolCacheService.invalidateByPattern(itineraryId, "weather:*");
    toolCacheService.invalidateByPattern(itineraryId, "hours:*");
}
```


### 5. Async Loading Race Conditions

**Problem:** Cache preload might not complete before agents need data.

**Scenario:**
```
T0: Itinerary loaded, preload starts (async)
T1: ActivityAgent needs weather data
T2: Cache not loaded yet → MISS
T3: ActivityAgent calls tool (unnecessary)
T4: Preload completes with same data
```

**Solutions:**

**Option A: Wait for Preload**
```java
public <T> T getOrCompute(...) {
    // Wait for preload to complete (with timeout)
    CompletableFuture<Void> preload = preloadFutures.get(itineraryId);
    if (preload != null) {
        try {
            preload.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("Preload timeout for {}", itineraryId);
        }
    }
    
    // Now check cache
    ...
}
```

**Option B: Lazy Loading**
```java
// Don't preload at all, load on first access
public <T> T getOrCompute(...) {
    // Ensure cache loaded
    ensureCacheLoaded(itineraryId);
    
    // Check cache
    ...
}

private void ensureCacheLoaded(String itineraryId) {
    if (!sessionCache.containsKey(itineraryId)) {
        loadFromFirestoreSync(itineraryId);
    }
}
```

**Recommendation:** Use Option B (lazy loading) - simpler and avoids race conditions.

---

