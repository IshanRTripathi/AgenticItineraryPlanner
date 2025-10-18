# Node ID Consistency Fix - Design Document

**Document Version:** 1.0  
**Date:** 2025-10-18  
**Status:** Design Complete - Ready for Implementation  

---

## Overview

This design document outlines the solution for fixing node ID consistency across the itinerary system. The fix addresses the core issue where different components generate and reference node IDs using incompatible patterns, causing user changes to be applied incorrectly or not at all.

### Current Problem Analysis

**Issue Flow:**
1. User requests: "Add Muzeum Sportu i Turystyki w Warszawie to day 4"
2. EditorAgent builds context with actual node IDs: `node_att_day4_2274_7de9e730`
3. LLM generates operations expecting pattern: `day4_node4`, `day4_node5`, `day4_node6`
4. ChangeEngine can't find these IDs, falls back to `day4_node1`
5. Result: Wrong nodes modified, new node invisible to user

**Root Causes:**
- NodeIdGenerator uses pattern: `node_{type}_day{N}_{hash}`
- LLM expects pattern: `day{N}_node{M}`
- ChangeEngine fallback logic is too aggressive
- Context building doesn't show node IDs to LLM

---

## Architecture Overview

### High-Level Solution

```
┌─────────────────────────────────────────────────────────────┐
│                    STANDARDIZED NODE ID SYSTEM              │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  NodeIdGenerator│    │ ItineraryMigrator│    │  ContextBuilder │
│                 │    │                 │    │                 │
│ generateId()    │    │ migrateIds()    │    │ buildContext()  │
│ ↓               │    │ ↓               │    │ ↓               │
│ day{N}_node{M}  │    │ old → new IDs   │    │ actual node IDs │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                      CONSISTENT FLOW                        │
│                                                             │
│  LLM sees: day4_node1, day4_node2, day4_node3             │
│  LLM generates: replace day4_node2, insert after day4_node3│
│  ChangeEngine finds: day4_node2 ✓, day4_node3 ✓           │
│  Result: Correct nodes modified, changes visible in UI     │
└─────────────────────────────────────────────────────────────┘
```

---

## Component Design

### 1. NodeIdGenerator (Modified)

**Current Implementation:**
```java
public String generateId(String title, String type, int day) {
    String hash = generateHash(title);
    return String.format("node_%s_day%d_%s", 
                        type.substring(0, 3), day, hash);
}
```

**New Implementation:**
```java
public String generateId(String title, String type, int day, NormalizedItinerary itinerary) {
    int nextNodeNumber = findNextNodeNumber(itinerary, day);
    return String.format("day%d_node%d", day, nextNodeNumber);
}

private int findNextNodeNumber(NormalizedItinerary itinerary, int day) {
    NormalizedDay targetDay = findDay(itinerary, day);
    if (targetDay == null || targetDay.getNodes().isEmpty()) {
        return 1;
    }
    
    int maxNodeNumber = 0;
    for (NormalizedNode node : targetDay.getNodes()) {
        int nodeNumber = extractNodeNumber(node.getId());
        maxNodeNumber = Math.max(maxNodeNumber, nodeNumber);
    }
    
    return maxNodeNumber + 1;
}

private int extractNodeNumber(String nodeId) {
    // Extract number from day{N}_node{M} pattern
    Pattern pattern = Pattern.compile("day\\\\d+_node(\\\\d+)");
    Matcher matcher = pattern.matcher(nodeId);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
}
```

**Key Changes:**
- New ID pattern: `day{N}_node{M}`
- Sequential numbering within each day
- Context-aware generation (requires itinerary)
- Thread-safe implementation

### 2. ItineraryMigrationService (New)

**Purpose:** Automatically migrate existing itineraries from old ID patterns to new standardized pattern.

```java
@Service
public class ItineraryMigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryMigrationService.class);
    
    private final ItineraryJsonService itineraryJsonService;
    
    /**
     * Migrate itinerary node IDs to new standardized pattern.
     * Called automatically when itinerary is loaded.
     */
    public NormalizedItinerary migrateIfNeeded(NormalizedItinerary itinerary) {
        if (!needsMigration(itinerary)) {
            return itinerary;
        }
        
        logger.info("Migrating node IDs for itinerary: {}", itinerary.getItineraryId());
        
        try {
            NormalizedItinerary migrated = performMigration(itinerary);
            
            // Save migrated version
            itineraryJsonService.updateItinerary(migrated);
            
            logger.info("Successfully migrated {} days with {} total nodes", 
                       migrated.getDays().size(), 
                       migrated.getDays().stream().mapToInt(d -> d.getNodes().size()).sum());
            
            return migrated;
            
        } catch (Exception e) {
            logger.error("Failed to migrate itinerary: {}", itinerary.getItineraryId(), e);
            // Return original itinerary if migration fails
            return itinerary;
        }
    }
    
    private boolean needsMigration(NormalizedItinerary itinerary) {
        return itinerary.getDays().stream()
                .flatMap(day -> day.getNodes().stream())
                .anyMatch(node -> !isNewIdPattern(node.getId()));
    }
    
    private boolean isNewIdPattern(String nodeId) {
        return nodeId != null && nodeId.matches("day\\\\d+_node\\\\d+");
    }
    
    private NormalizedItinerary performMigration(NormalizedItinerary itinerary) {
        for (NormalizedDay day : itinerary.getDays()) {
            int nodeCounter = 1;
            for (NormalizedNode node : day.getNodes()) {
                String newId = String.format("day%d_node%d", day.getDayNumber(), nodeCounter++);
                logger.debug("Migrating node ID: {} -> {}", node.getId(), newId);
                node.setId(newId);
            }
        }
        
        // Update version to indicate migration
        itinerary.setVersion(itinerary.getVersion() + 1);
        itinerary.setUpdatedAt(System.currentTimeMillis());
        
        return itinerary;
    }
}
```

### 3. EditorAgent Context Builder (Modified)

**Current buildContextForLLM:**
```java
private String buildContextForLLM(NormalizedItinerary itinerary) {
    StringBuilder context = new StringBuilder();
    context.append("Current itinerary:\\n");
    
    for (NormalizedDay day : itinerary.getDays()) {
        context.append(String.format("Day %d:\\n", day.getDay()));
        for (NormalizedNode node : day.getNodes()) {
            context.append(String.format("- %s (%s)\\n", 
                          node.getTitle(), node.getType()));
        }
    }
    
    return context.toString();
}
```

**New buildContextForLLM:**
```java
private String buildContextForLLM(NormalizedItinerary itinerary) {
    StringBuilder context = new StringBuilder();
    context.append("Current itinerary with node IDs:\\n");
    
    for (NormalizedDay day : itinerary.getDays()) {
        context.append(String.format("Day %d:\\n", day.getDay()));
        
        if (day.getNodes().isEmpty()) {
            context.append("  No nodes\\n");
        } else {
            for (NormalizedNode node : day.getNodes()) {
                context.append(String.format("  %s: %s (%s) [%s-%s]\\n", 
                              node.getId(),
                              node.getTitle(), 
                              node.getType(),
                              node.getStartTime() != null ? node.getStartTime() : "?",
                              node.getEndTime() != null ? node.getEndTime() : "?"));
            }
        }
        context.append("\\n");
    }
    
    context.append("\\nWhen referencing nodes in operations, use the exact IDs shown above.\\n");
    context.append("For insert operations, use 'after': 'day{N}_node{M}' format.\\n");
    context.append("For replace operations, use 'id': 'day{N}_node{M}' format.\\n");
    
    logger.debug("Built LLM context with {} days and {} total nodes", 
                itinerary.getDays().size(),
                itinerary.getDays().stream().mapToInt(d -> d.getNodes().size()).sum());
    
    return context.toString();
}
```

**Key Improvements:**
- Shows actual node IDs to LLM
- Includes timing information for context
- Provides explicit instructions on ID usage
- Logs context statistics for debugging

### 4. ChangeEngine (Modified)

**Current applyReplace with problematic fallback:**
```java
private ChangeResult applyReplace(NormalizedItinerary itinerary, ChangeOperation op) {
    NodeLocation location = findNodeById(itinerary, op.getId());
    if (location == null) {
        logger.warn("Could not find node with ID '{}', using fallback node 'day4_node1'", op.getId());
        location = findFallbackNode(itinerary); // PROBLEMATIC
    }
    // ...
}
```

**New applyReplace with strict validation:**
```java
private ChangeResult applyReplace(NormalizedItinerary itinerary, ChangeOperation op) {
    logger.debug("Applying replace operation for node: {}", op.getId());
    
    NodeLocation location = findNodeById(itinerary, op.getId());
    if (location == null) {
        String availableIds = getAvailableNodeIds(itinerary);
        String errorMsg = String.format(
            "Node with ID '%s' not found. Available node IDs: %s", 
            op.getId(), availableIds);
        
        logger.error(errorMsg);
        return ChangeResult.failure(op.getId(), errorMsg);
    }
    
    // Apply the replacement
    NormalizedNode node = location.node;
    boolean changed = false;
    
    if (op.getStartTime() != null && !op.getStartTime().equals(node.getStartTime())) {
        node.setStartTime(op.getStartTime());
        changed = true;
        logger.debug("Updated startTime for {}: {}", op.getId(), op.getStartTime());
    }
    
    if (op.getEndTime() != null && !op.getEndTime().equals(node.getEndTime())) {
        node.setEndTime(op.getEndTime());
        changed = true;
        logger.debug("Updated endTime for {}: {}", op.getId(), op.getEndTime());
    }
    
    if (op.getNode() != null) {
        // Replace node content while preserving ID
        String originalId = node.getId();
        node.setTitle(op.getNode().getTitle());
        node.setType(op.getNode().getType());
        node.setLocation(op.getNode().getLocation());
        node.setId(originalId); // Preserve original ID
        changed = true;
        logger.debug("Updated content for {}: {}", op.getId(), node.getTitle());
    }
    
    if (!changed) {
        logger.warn("Replace operation for {} made no changes", op.getId());
    }
    
    return ChangeResult.success(op.getId(), "Node replaced successfully");
}

private String getAvailableNodeIds(NormalizedItinerary itinerary) {
    return itinerary.getDays().stream()
            .flatMap(day -> day.getNodes().stream())
            .map(NormalizedNode::getId)
            .collect(Collectors.joining(", "));
}
```

**Key Changes:**
- No fallback behavior - fail fast with clear error
- List available node IDs in error message
- Detailed logging of what changed
- Preserve original node ID during content replacement

### 5. Pipeline Agents Integration

**All pipeline agents (SkeletonPlannerAgent, ActivityAgent, MealAgent, TransportAgent) need to use consistent ID generation:**

```java
// In SkeletonPlannerAgent.java
private void assignNodeIds(NormalizedItinerary itinerary) {
    for (NormalizedDay day : itinerary.getDays()) {
        int nodeCounter = 1;
        for (NormalizedNode node : day.getNodes()) {
            if (node.getId() == null || node.getId().isEmpty()) {
                String nodeId = String.format("day%d_node%d", day.getDayNumber(), nodeCounter++);
                node.setId(nodeId);
                logger.debug("Assigned ID {} to node: {}", nodeId, node.getTitle());
            }
        }
    }
}

// In ActivityAgent.java, MealAgent.java, TransportAgent.java
// Ensure nodes have IDs before updating
nodeIdGenerator.ensureNodeHasId(node, day.getDayNumber());
```

---

## Data Flow

### 1. New Itinerary Creation Flow

```
1. User creates itinerary
   ↓
2. SkeletonPlannerAgent generates day structure
   ↓
3. assignNodeIds() assigns day1_node1, day1_node2, etc.
   ↓
4. ActivityAgent/MealAgent/TransportAgent populate nodes
   ↓
5. Each ensures nodes have consistent IDs
   ↓
6. Result: Consistent day{N}_node{M} pattern throughout
```

### 2. Existing Itinerary Load Flow

```
1. ItineraryService.get() called
   ↓
2. ItineraryJsonService.getItinerary() loads from Firestore
   ↓
3. ItineraryMigrationService.migrateIfNeeded() checks ID patterns
   ↓
4. If old patterns found: performMigration() converts to new pattern
   ↓
5. Updated itinerary saved back to Firestore
   ↓
6. Return migrated itinerary with consistent IDs
```

### 3. Chat-Based Edit Flow

```
1. User: "Add museum to day 4"
   ↓
2. EditorAgent.execute() called
   ↓
3. buildContextForLLM() includes actual node IDs:
      "day4_node1: Breakfast (meal)"
      "day4_node2: Warsaw Castle (attraction)"
      "day4_node3: Lunch (meal)"
   ↓
4. LLM generates: {"op": "insert", "after": "day4_node3", ...}
   ↓
5. ChangeEngine.applyInsert() finds day4_node3 ✓
   ↓
6. NodeIdGenerator creates day4_node4 for new museum
   ↓
7. Result: Museum appears in UI at correct position
```

---

## Error Handling Strategy

### 1. Migration Errors

```java
// If migration fails, continue with original IDs
try {
    return performMigration(itinerary);
} catch (Exception e) {
    logger.error("Migration failed for {}, continuing with original IDs", 
                itinerary.getItineraryId(), e);
    return itinerary; // Graceful degradation
}
```

### 2. Node Not Found Errors

```java
// Provide actionable error messages
if (location == null) {
    String errorMsg = String.format(
        "Node '%s' not found. Available nodes: %s. " +
        "This may indicate an LLM context issue or stale node reference.",
        op.getId(), getAvailableNodeIds(itinerary));
    
    return ChangeResult.failure(op.getId(), errorMsg);
}
```

### 3. Concurrent Modification

```java
// Thread-safe ID generation
public synchronized String generateId(String title, String type, int day, NormalizedItinerary itinerary) {
    // Implementation ensures no duplicate IDs even under concurrent access
}
```

---

## Testing Strategy

### 1. Unit Tests

```java
@Test
void testNodeIdGeneration() {
    // Test new ID pattern generation
    String id = nodeIdGenerator.generateId("Museum", "attraction", 4, itinerary);
    assertEquals("day4_node3", id); // Assuming 2 existing nodes
}

@Test
void testMigrationDetection() {
    // Test migration detection logic
    assertTrue(migrationService.needsMigration(oldItinerary));
    assertFalse(migrationService.needsMigration(newItinerary));
}

@Test
void testChangeEngineStrictValidation() {
    // Test that invalid node IDs fail without fallback
    ChangeResult result = changeEngine.applyReplace(itinerary, invalidOperation);
    assertEquals("failure", result.getStatus());
    assertTrue(result.getMessage().contains("not found"));
}
```

### 2. Integration Tests

```java
@Test
void testEndToEndNodeAddition() {
    // Test complete flow from chat request to UI update
    ChatRequest request = new ChatRequest(itineraryId, "Add museum to day 4");
    ChatResponse response = chatService.processRequest(request, userId);
    
    // Verify node was added with correct ID
    NormalizedItinerary updated = itineraryService.get(itineraryId, userId);
    assertTrue(updated.getDays().get(3).getNodes().stream()
              .anyMatch(n -> n.getTitle().contains("museum") && 
                           n.getId().matches("day4_node\\\\d+")));
}
```

### 3. Migration Tests

```java
@Test
void testAutomaticMigration() {
    // Create itinerary with old ID pattern
    NormalizedItinerary oldItinerary = createItineraryWithOldIds();
    
    // Load itinerary (should trigger migration)
    NormalizedItinerary loaded = itineraryService.get(itineraryId, userId);
    
    // Verify all IDs migrated
    loaded.getDays().stream()
          .flatMap(day -> day.getNodes().stream())
          .forEach(node -> assertTrue(node.getId().matches("day\\\\d+_node\\\\d+")));
}
```

---

## Deployment Strategy

### Phase 1: Backend Changes
1. Deploy NodeIdGenerator changes
2. Deploy ItineraryMigrationService
3. Deploy EditorAgent context improvements
4. Deploy ChangeEngine strict validation

### Phase 2: Pipeline Agent Updates
1. Update SkeletonPlannerAgent
2. Update ActivityAgent, MealAgent, TransportAgent
3. Verify new itineraries use consistent IDs

### Phase 3: Migration Rollout
1. Enable automatic migration for new requests
2. Monitor migration success rates
3. Gradually migrate high-traffic itineraries
4. Full rollout once stability confirmed

### Rollback Plan
- Keep old NodeIdGenerator logic as fallback
- Disable migration service if issues detected
- Restore ChangeEngine fallback behavior temporarily
- Monitor error rates and user feedback

---

## Monitoring and Observability

### Key Metrics
- Migration success rate
- Node operation success rate
- Average context building time
- ID generation performance
- Error rates by operation type

### Logging Strategy
- INFO: Migration events, ID generation
- DEBUG: Node operations, ID generation, context building
- WARN: Migration issues, node not found
- ERROR: Critical failures

### Alerts
- High error rate in node operations
- Frequent migration failures
- Increased fallback usage (should be zero)
- Context building timeouts

---

## Appendix: Current vs New Flow Comparison

### Current Flow (Broken)

```
1. User: "Add museum to day 4"
   ↓
2. EditorAgent loads itinerary
   - Nodes have IDs: node_att_day4_2274_7de9e730, node_meal_day4_1234_abc123
   ↓
3. buildContextForLLM() creates:
   "Day 4:
    - Warsaw Castle (attraction)
    - Lunch Break (meal)"
   (NO NODE IDS!)
   ↓
4. LLM generates operations:
   {"op": "insert", "after": "day4_node2", ...}
   (LLM invents IDs based on pattern it expects)
   ↓
5. ChangeEngine tries to find "day4_node2"
   - Not found! (actual ID is node_meal_day4_1234_abc123)
   - Falls back to "day4_node1"
   ↓
6. Wrong node modified, new node invisible
   ❌ FAILURE
```

### New Flow (Fixed)

```
1. User: "Add museum to day 4"
   ↓
2. EditorAgent loads itinerary
   - Migration service checks IDs
   - If old pattern: migrates to day4_node1, day4_node2, day4_node3
   - Saves migrated version
   ↓
3. buildContextForLLM() creates:
   "Day 4:
    day4_node1: Warsaw Castle (attraction) [10:00-12:00]
    day4_node2: Lunch Break (meal) [12:00-13:00]
    day4_node3: Museum Visit (attraction) [13:30-15:30]
   
   Use exact IDs shown above."
   ↓
4. LLM generates operations:
   {"op": "insert", "after": "day4_node3", ...}
   (LLM uses actual IDs from context)
   ↓
5. ChangeEngine finds "day4_node3" ✓
   - NodeIdGenerator creates day4_node4 for new museum
   - Inserts after day4_node3
   ↓
6. Museum appears in UI at correct position
   ✅ SUCCESS
```

---

## Conclusion

This design provides a comprehensive solution to the node ID consistency issue by:

1. **Standardizing ID generation** across all components
2. **Migrating existing data** automatically and safely
3. **Improving LLM context** to include actual node IDs
4. **Hardening ChangeEngine** to fail fast with clear errors
5. **Maintaining backward compatibility** during transition

The fix is designed to be:
- **Transparent** to users (automatic migration)
- **Reliable** (comprehensive error handling)
- **Performant** (minimal overhead)
- **Maintainable** (clear component boundaries)
- **Testable** (isolated components, clear contracts)

Implementation should proceed in phases with careful monitoring and rollback capabilities at each stage.
