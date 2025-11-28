# Database Save Diagnostic Logging Added

## Purpose
Added comprehensive logging to track exactly what's being saved to and loaded from Firestore database to diagnose potential save/load issues.

## Logging Added

### 1. FirestoreDatabaseService - Save Operation

**File**: `src/main/java/com/tripplanner/service/firebase/FirestoreDatabaseService.java`

**Logs**:
```
💾 [FIRESTORE SAVE] Starting save for itinerary: {id}
  Version: {version}
  Thread: {threadName}
  JSON length: {chars}
✅ [FIRESTORE SAVE] Successfully saved itinerary {id} in {duration}ms
```

**What it tracks**:
- Which itinerary is being saved
- What version is being saved
- Which thread is performing the save (important for race condition detection)
- Size of the JSON being saved
- How long the save operation took
- Any errors during save

### 2. FirestoreDatabaseService - Load Operation

**File**: `src/main/java/com/tripplanner/service/firebase/FirestoreDatabaseService.java`

**Logs**:
```
📖 [FIRESTORE LOAD] Loading itinerary: {id}
  Thread: {threadName}
✅ [FIRESTORE LOAD] Loaded itinerary {id} in {duration}ms
  Version: {version}
  JSON length: {chars}
```

**What it tracks**:
- Which itinerary is being loaded
- Which thread is performing the load
- What version was loaded
- Size of the JSON loaded
- How long the load operation took
- If itinerary was not found

### 3. ItineraryJsonService - Serialization

**File**: `src/main/java/com/tripplanner/service/ItineraryJsonService.java`

**Logs**:
```
📝 [UPDATE ITINERARY] Serializing itinerary: {id}
  Version: {version}
  Days: {count}
    Day 1: {nodeCount} nodes
    Day 2: {nodeCount} nodes
    ...
  Serialized JSON length: {chars}
```

**What it tracks**:
- Which itinerary is being serialized
- What version is being serialized
- How many days and nodes per day (critical for verifying changes)
- Size of serialized JSON

### 4. ItineraryJsonService - Deserialization & Caching

**File**: `src/main/java/com/tripplanner/service/ItineraryJsonService.java`

**Logs**:
```
🎯 [CACHE HIT] Itinerary: {id}, Version: {version}, Thread: {threadName}
OR
🔍 [CACHE MISS] Loading from database: {id}, Thread: {threadName}
📦 [DESERIALIZED] Itinerary: {id}, Version: {version}, Days: {count}
    Day 1: {nodeCount} nodes
    Day 2: {nodeCount} nodes
    ...
```

**What it tracks**:
- Whether data came from cache or database
- Which thread is accessing the data (ThreadLocal cache is per-thread)
- What version was loaded
- How many nodes per day (to verify data integrity)

## How to Use These Logs

### Scenario 1: Verify Save is Working

Look for this sequence:
```
📝 [UPDATE ITINERARY] Serializing itinerary: it_xxx
  Version: 6
  Days: 4
    Day 1: 5 nodes  ← Should show NEW node count
    Day 2: 3 nodes
💾 [FIRESTORE SAVE] Starting save for itinerary: it_xxx
  Version: 6
✅ [FIRESTORE SAVE] Successfully saved itinerary it_xxx in 250ms
```

**What to check**:
- Does Day 1 show 5 nodes (with the new Birla Mandir)?
- Did the save complete successfully?
- What thread performed the save?

### Scenario 2: Verify Load Returns Correct Data

Look for this sequence:
```
🔍 [CACHE MISS] Loading from database: it_xxx, Thread: http-nio-8080-exec-5
📖 [FIRESTORE LOAD] Loading itinerary: it_xxx
  Thread: http-nio-8080-exec-5
✅ [FIRESTORE LOAD] Loaded itinerary it_xxx in 150ms
  Version: 6
📦 [DESERIALIZED] Itinerary: it_xxx, Version: 6, Days: 4
    Day 1: 5 nodes  ← Should show NEW node count
    Day 2: 3 nodes
```

**What to check**:
- Does the loaded data show the correct node counts?
- Is the version correct?
- Which thread loaded the data?

### Scenario 3: Detect Race Conditions

Look for overlapping operations:
```
Thread A (clientInboundChannel-13):
  💾 [FIRESTORE SAVE] Starting save for itinerary: it_xxx
  ✅ [FIRESTORE SAVE] Successfully saved in 250ms

Thread B (AsyncTask-2):
  🔍 [CACHE MISS] Loading from database: it_xxx, Thread: AsyncTask-2
  📖 [FIRESTORE LOAD] Loading itinerary: it_xxx
  📦 [DESERIALIZED] Version: 5  ← OLD VERSION!
  💾 [FIRESTORE SAVE] Starting save for itinerary: it_xxx
  ✅ [FIRESTORE SAVE] Successfully saved in 200ms  ← OVERWRITES NEW DATA!
```

**What to check**:
- Are multiple threads saving the same itinerary?
- Is Thread B loading an old version while Thread A is saving?
- Are the versions going backwards (6 → 5)?

### Scenario 4: Verify Enrichment Doesn't Overwrite

After our fix, enrichment should NOT save. But if it does:
```
AsyncTask-2:
  🔍 [CACHE MISS] Loading from database: it_xxx, Thread: AsyncTask-2
  📦 [DESERIALIZED] Version: 5, Days: 4
    Day 1: 4 nodes  ← Missing the new node!
  💾 [FIRESTORE SAVE] Starting save  ← Should NOT happen after our fix!
```

## Testing Instructions

1. **Restart backend** to pick up new logging
2. **Add a location** via chat
3. **Check logs** for the sequence:
   - Serialization shows correct node count
   - Firestore save completes successfully
   - No competing saves from other threads
4. **Refresh page**
5. **Check logs** for:
   - Load operation
   - Deserialized data shows correct node count
   - Version matches what was saved

## Expected Log Flow (After Fixes)

```
[EditorAgent Thread]
📝 [UPDATE ITINERARY] Serializing itinerary: it_xxx
  Day 1: 5 nodes  ← NEW NODE ADDED
💾 [FIRESTORE SAVE] Starting save
✅ [FIRESTORE SAVE] Successfully saved in 250ms
🔍 [VERIFICATION] Reading back from database
📖 [FIRESTORE LOAD] Loading itinerary
📦 [DESERIALIZED] Day 1: 5 nodes  ← VERIFIED!

[User Refreshes Page]
🔍 [CACHE MISS] Loading from database
📖 [FIRESTORE LOAD] Loading itinerary
📦 [DESERIALIZED] Day 1: 5 nodes  ← CORRECT DATA!
```

## Files Modified

1. `src/main/java/com/tripplanner/service/firebase/FirestoreDatabaseService.java`
   - Added logging to `save()` method
   - Added logging to `findById()` method

2. `src/main/java/com/tripplanner/service/ItineraryJsonService.java`
   - Added logging to `updateItinerary()` method
   - Added logging to `getItinerary()` method

## Benefits

- **Visibility**: See exactly what's being saved and loaded
- **Race Detection**: Identify competing writes from different threads
- **Data Verification**: Confirm node counts match expectations
- **Performance**: Track how long database operations take
- **Debugging**: Pinpoint where data loss occurs
