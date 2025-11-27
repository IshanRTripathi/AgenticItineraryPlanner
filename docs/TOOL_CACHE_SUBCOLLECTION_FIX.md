# Tool Cache Subcollection Fix

## Problem

The tool cache was implemented to store cache entries as a **map field** within the itinerary document:
```
itineraries/{itineraryId}
  └── toolCache: { "key1": {...}, "key2": {...} }
```

This approach had issues:
- **Document size limit**: Firestore documents are limited to 1MB
- **Not visible**: Cache data wasn't appearing as a separate collection in Firestore console
- **Harder to query**: Couldn't easily query or manage cache entries independently

## Solution

Changed to use a **subcollection** approach:
```
itineraries/{itineraryId}/toolCache/{cacheKey}
```

Each cache entry is now a separate document in the `toolCache` subcollection.

### Example Structure

```
itineraries/
  └── abc123/
      ├── (itinerary data)
      ├── chat_messages/
      │   └── (messages)
      ├── revisions/
      │   └── (revisions)
      └── toolCache/
          ├── weather:zurich-switzerland:2025-06-15
          ├── weather:bali-indonesia_JUNE
          └── geocode:eiffel-tower-paris
```

## Benefits

1. **No size limits**: Each cache entry is a separate document
2. **Better visibility**: Cache appears as a subcollection in Firestore console
3. **Easier management**: Can query, delete, or update individual cache entries
4. **Automatic cleanup**: Expired entries are deleted during load
5. **Consistent with other subcollections**: Matches pattern of `chat_messages` and `revisions`

## Implementation Changes

### FirestoreToolCacheService.java

**Before (Map Field Approach):**
```java
// Save to itinerary document field
docRef.set(Map.of("toolCache", toolCacheMap), SetOptions.merge());

// Load from itinerary document field
Map<String, Object> toolCache = doc.get("toolCache");
```

**After (Subcollection Approach):**
```java
// Save to subcollection
firestore.collection("itineraries")
    .document(itineraryId)
    .collection("toolCache")
    .document(cacheKey)
    .set(entry);

// Load from subcollection
var cacheCollection = firestore.collection("itineraries")
    .document(itineraryId)
    .collection("toolCache");
var querySnapshot = cacheCollection.get().get();
```

## Cache Entry Structure

Each document in the `toolCache` subcollection contains:

```json
{
  "toolType": "get-weather",
  "cacheKey": "weather:zurich-switzerland:2025-06-15",
  "cachedAt": 1732723200000,
  "expiresAt": 1732726800000,
  "ttlSeconds": 3600,
  "requestClass": "java.util.HashMap",
  "resultClass": "com.tripplanner.service.WeatherService$WeatherData",
  "requestJson": "{\"location\":\"Zurich, Switzerland\",\"date\":\"2025-06-15\"}",
  "resultJson": "{\"condition\":\"Clear\",\"temperatureCelsius\":22.0,...}",
  "metadata": {
    "executionTimeMs": 450,
    "toolVersion": "1.0",
    "cacheHit": false
  }
}
```

## Verification

After deploying this fix, you should see:

1. **In Firestore Console**: 
   - Navigate to `itineraries/{itineraryId}`
   - See `toolCache` as a subcollection (alongside `chat_messages` and `revisions`)
   - Each cache entry as a separate document

2. **In Logs**:
   ```
   Saved cache entry to toolCache subcollection: itineraries/abc123/toolCache/weather:zurich-switzerland:2025-06-15
   Loaded 5 cache entries from itinerary abc123 toolCache subcollection
   ```

3. **Cache Hits**:
   ```
   Cache HIT (session): weather:zurich-switzerland:2025-06-15
   Returning cached weather for Zurich, Switzerland on 2025-06-15
   ```

## Migration Notes

**No migration needed!** The old map-based cache (if any exists) will simply be ignored. New cache entries will be created in the subcollection. Old entries will naturally expire based on TTL.

If you want to clean up old cache data from itinerary documents:
```javascript
// Optional cleanup script (run in Firestore console)
db.collection('itineraries').get().then(snapshot => {
  snapshot.forEach(doc => {
    if (doc.data().toolCache) {
      doc.ref.update({ toolCache: firebase.firestore.FieldValue.delete() });
    }
  });
});
```

## Performance Impact

**Positive impacts:**
- Faster writes (smaller document updates)
- Better scalability (no document size limits)
- Cleaner data model

**Neutral:**
- Read performance similar (subcollection query vs field read)
- Session cache still provides <50ms lookups

## Files Modified

- `src/main/java/com/tripplanner/service/cache/FirestoreToolCacheService.java`
  - `saveToFirestoreAsync()` - Changed to write to subcollection
  - `loadFromFirestoreSync()` - Changed to read from subcollection
  - `invalidate()` - Changed to delete from subcollection
  - `invalidateAll()` - Changed to delete all subcollection documents
  - Class documentation updated

- `docs/WEATHER_CACHE_FIX.md` - Updated with subcollection structure
