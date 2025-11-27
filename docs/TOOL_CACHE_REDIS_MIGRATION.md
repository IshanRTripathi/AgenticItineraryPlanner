# Redis Migration Strategy

## Why Redis?

### Limitations of Firestore
- ❌ 1MB document size limit
- ❌ Slower read/write (network latency)
- ❌ No native TTL support
- ❌ Limited query capabilities
- ❌ Costs scale with operations

### Benefits of Redis
- ✅ No size limits (up to 512MB per key)
- ✅ Sub-millisecond latency
- ✅ Native TTL support (EXPIRE command)
- ✅ Rich data structures (Hash, Set, List)
- ✅ Lower cost at scale

---

## Migration Path

### Phase 1: Firestore Only (Current)
```
Agents → ToolCacheService → FirestoreToolCache → Firestore
```

### Phase 2: Dual Write (Testing)
```
Agents → ToolCacheService → DualWriteToolCache → Firestore
                                                 → Redis
```

### Phase 3: Redis Primary (Production)
```
Agents → ToolCacheService → RedisToolCache → Redis
                                            → Firestore (backup)
```

### Phase 4: Redis Only (Optimized)
```
Agents → ToolCacheService → RedisToolCache → Redis
```

---

## Redis Data Structure

### Option A: Hash per Itinerary (Recommended)

```redis
# Key: itinerary:{itineraryId}:cache
# Type: Hash
# Fields: {cacheKey} → {JSON}

HSET itinerary:itin_abc123:cache 
     "weather:timing:burj-khalifa:dubai:2025-08-15:120"
     '{"toolType":"suggest-best-time","cachedAt":1732627200000,...}'

# TTL on entire hash
EXPIRE itinerary:itin_abc123:cache 86400

# Get specific entry
HGET itinerary:itin_abc123:cache "weather:timing:burj-khalifa:dubai:2025-08-15:120"

# Get all entries
HGETALL itinerary:itin_abc123:cache
```

**Pros:**
- ✅ All cache entries in one key
- ✅ Atomic operations
- ✅ Easy to invalidate entire itinerary

**Cons:**
- ⚠️ Can't set per-entry TTL
- ⚠️ Large hashes can be slow

---

### Option B: Individual Keys (More Flexible)

```redis
# Key pattern: cache:{itineraryId}:{cacheKey}
# Type: String (JSON)

SET cache:itin_abc123:weather:timing:burj-khalifa:dubai:2025-08-15:120
    '{"toolType":"suggest-best-time","cachedAt":1732627200000,...}'
    EX 3600

# Get entry
GET cache:itin_abc123:weather:timing:burj-khalifa:dubai:2025-08-15:120

# Get all for itinerary
KEYS cache:itin_abc123:*
```

**Pros:**
- ✅ Per-entry TTL
- ✅ Smaller operations
- ✅ Better for large itineraries

**Cons:**
- ⚠️ More keys to manage
- ⚠️ KEYS command can be slow

**Recommendation:** Use Option B for flexibility and per-entry TTL.

---


## RedisToolCacheService Implementation

```java
@Service
@ConditionalOnProperty(name = "cache.backend", havingValue = "redis")
public class RedisToolCacheService implements ToolCacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public <T> T getOrCompute(String itineraryId, String toolType, String cacheKey,
                              Object request, Supplier<T> toolCall, Class<T> resultType) {
        
        String redisKey = buildRedisKey(itineraryId, cacheKey);
        
        // 1. Try Redis
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                CachedToolResult result = objectMapper.readValue(
                    cached, CachedToolResult.class);
                
                if (result.isValid()) {
                    logger.debug("Cache HIT (redis): {}", cacheKey);
                    return objectMapper.convertValue(result.getResult(), resultType);
                }
            } catch (Exception e) {
                logger.warn("Failed to deserialize cached result", e);
            }
        }
        
        // 2. Cache MISS - call tool
        logger.debug("Cache MISS: {}", cacheKey);
        T result = toolCall.get();
        
        // 3. Store in Redis with TTL
        CachedToolResult newEntry = new CachedToolResult();
        newEntry.setToolType(toolType);
        newEntry.setCacheKey(cacheKey);
        newEntry.setCachedAt(System.currentTimeMillis());
        newEntry.setTtlSeconds(ToolCacheTTLConfig.getTTL(toolType));
        newEntry.setExpiresAt(System.currentTimeMillis() + 
            (newEntry.getTtlSeconds() * 1000L));
        newEntry.setRequest(request);
        newEntry.setResult(result);
        
        try {
            String json = objectMapper.writeValueAsString(newEntry);
            redisTemplate.opsForValue().set(
                redisKey, 
                json, 
                newEntry.getTtlSeconds(), 
                TimeUnit.SECONDS
            );
        } catch (Exception e) {
            logger.error("Failed to cache result in Redis", e);
        }
        
        return result;
    }
    
    private String buildRedisKey(String itineraryId, String cacheKey) {
        return String.format("cache:%s:%s", itineraryId, cacheKey);
    }
}
```

---

