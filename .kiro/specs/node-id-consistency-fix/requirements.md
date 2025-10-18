# Node ID Consistency Fix - Requirements Document

**Document Version:** 1.0  
**Date:** 2025-10-18  
**Status:** Analysis Complete - Ready for Implementation  
**Priority:** HIGH - Critical UX Issue  
**Estimated Effort:** 10-14 hours  

---

## Problem Statement

The system has inconsistent node ID generation and management across different components, causing:

1. **Invisible Changes**: Added nodes don't appear in the UI because LLM generates IDs that don't match actual node IDs
2. **Incorrect Fallback Behavior**: ChangeEngine falls back to wrong nodes when target IDs aren't found
3. **Multiple ID Patterns**: Different components use different ID naming conventions
4. **LLM Context Mismatch**: LLM sees complex node IDs but generates simpler sequential IDs

### Current Issues (from actual logs and code)

**Observed Behavior:**
```
User Request: "Add Muzeum Sportu i Turystyki w Warszawie to day 4"

Generated ID for new node: Muzeum Sportu i Turystyki w Warszawie -> node_att_day4_2274_7de9e730
Could not find node with ID 'day4_node4', using fallback node 'day4_node1' for replace result
Could not find node with ID 'day4_node5', using fallback node 'day4_node1' for replace result
Could not find node with ID 'day4_node6', using fallback node 'day4_node1' for replace result
```

**Root Cause Analysis:**

1. **NodeIdGenerator.java (Line 31-38)** generates IDs in format:
   ```java
   public String generateNodeId(String nodeType, Integer dayNumber) {
       long timestamp = System.currentTimeMillis();
       String typePrefix = sanitizeType(nodeType);
       String dayPart = dayNumber != null ? "_day" + dayNumber : "";
       String uuid = UUID.randomUUID().toString().substring(0, 8);
       return String.format("%s_%s%s_%d_%s", NODE_PREFIX, typePrefix, dayPart, timestamp % 10000, uuid);
       // Result: "node_att_day4_2274_7de9e730"
   }
   ```

2. **SummarizationService.java (Line 327-350)** DOES include node IDs in context:
   ```java
   private String summarizeDayForEditor(NormalizedDay day, int maxTokens) {
       // ...
       summary.append("  - [ID: ").append(node.getId()).append("] ");
       summary.append(node.getTitle());
       // Result: "  - [ID: node_att_day4_2274_7de9e730] Warsaw Castle (attraction)"
   }
   ```

3. **LLM generates operations** expecting simpler pattern:
   ```json
   {"op": "replace", "id": "day4_node4"}
   {"op": "replace", "id": "day4_node5"}
   ```
   The LLM sees `[ID: node_att_day4_2274_7de9e730]` but generates `day4_node4` instead.

4. **ChangeEngine.java (Line 456-476)** tries to find nodes but fails:
   ```java
   private boolean replaceNode(NormalizedItinerary itinerary, ChangeOperation op, Integer day, ChangePreferences preferences) {
       NormalizedNode nodeToReplace = findNodeById(itinerary, op.getId());
       if (nodeToReplace == null) {
           logger.warn("Node not found for replace operation: {}, attempting fallback strategy", op.getId());
           // Falls back to first node in day - WRONG!
           List<NormalizedNode> nodes = targetDay.getNodes();
           if (nodes != null && !nodes.isEmpty()) {
               nodeToReplace = nodes.get(0);
               logger.warn("Using fallback node '{}' for replace operation", nodeToReplace.getId());
           }
       }
   }
   ```

**Why This Happens:**
- The LLM sees complex IDs like `node_att_day4_2274_7de9e730` in the context
- But it generates simpler, sequential IDs like `day4_node1`, `day4_node2`, `day4_node3`
- This mismatch causes ChangeEngine to fail finding nodes
- Fallback logic (Line 468-473) applies changes to wrong nodes
- New nodes get complex IDs that don't match the pattern LLM expects

---

## Requirements

### Requirement 1: Standardize Node ID Generation to Sequential Pattern

**User Story:** As a system architect, I want all node IDs to follow a single, sequential pattern (`day{N}_node{M}`), so that all components can reliably reference nodes without confusion.

#### Acceptance Criteria

1. WHEN NodeIdGenerator.generateNodeId() is called THEN it SHALL return format `day{N}_node{M}` where N is day number and M is sequential node number
2. WHEN NodeIdGenerator.generateSkeletonNodeId() is called THEN it SHALL return format `day{N}_node{M}` (same as generateNodeId)
3. WHEN a new node is inserted THEN its ID SHALL be `day{N}_node{M}` where M is the next available sequential number for that day
4. WHEN nodes are created by SkeletonPlannerAgent THEN they SHALL use the same ID pattern as EditorAgent
5. WHEN nodes are created by ActivityAgent, MealAgent, TransportAgent THEN they SHALL use the same ID pattern
6. IF a node is deleted THEN subsequent node IDs SHALL NOT be renumbered (maintain stability)
7. WHEN the system starts THEN all existing nodes SHALL be migrated to the new ID pattern

**Code References:**
- `src/main/java/com/tripplanner/service/NodeIdGenerator.java` (Lines 31-38, 48-52)
- `src/main/java/com/tripplanner/agents/SkeletonPlannerAgent.java` (Lines 150-165)
- `src/main/java/com/tripplanner/agents/ActivityAgent.java` (Lines 180-195)
- `src/main/java/com/tripplanner/agents/MealAgent.java` (Lines 180-195)
- `src/main/java/com/tripplanner/agents/TransportAgent.java` (Lines 180-195)

### Requirement 2: Update LLM Context Generation to Show Actual Node IDs Clearly

**User Story:** As an AI agent, I want to see the actual node IDs that exist in the itinerary in a clear, unambiguous format, so that I can generate correct change operations.

#### Acceptance Criteria

1. WHEN EditorAgent builds context for LLM THEN it SHALL include the actual node IDs from the itinerary
2. WHEN a day has nodes THEN the context SHALL list them as `day{N}_node{M}: {title}` format (ID first, then title)
3. WHEN the LLM generates change operations THEN it SHALL use the exact node IDs from the context
4. WHEN building context THEN nodes SHALL be listed in chronological order within each day
5. IF a day has no nodes THEN the context SHALL indicate "No nodes" for that day
6. WHEN context is built THEN it SHALL include node types (attraction, meal, transport) for LLM awareness
7. WHEN context is built THEN it SHALL include explicit instructions: "Use the EXACT node IDs shown above"

**Code References:**
- `src/main/java/com/tripplanner/service/SummarizationService.java` (Lines 327-350, method `summarizeDayForEditor`)
- `src/main/java/com/tripplanner/agents/EditorAgent.java` (Lines 130-145, method `generateChangeSet`)

**Current Context Format (Line 327-350):**
```
  - [ID: node_att_day4_2274_7de9e730] Warsaw Castle (attraction) @ 10:00 at Warsaw
```

**Proposed New Format:**
```
  day4_node1: Warsaw Castle (attraction) [10:00-12:00]
  day4_node2: Lunch Break (meal) [12:00-13:00]
  day4_node3: Museum Visit (attraction) [13:30-15:30]
```

### Requirement 3: Fix ChangeEngine Node Resolution and Remove Fallback Logic

**User Story:** As a change processor, I want to reliably find and modify the correct nodes, so that user changes are applied accurately.

#### Acceptance Criteria

1. WHEN ChangeEngine receives a replace operation THEN it SHALL find the exact node by ID
2. WHEN a node ID is not found THEN it SHALL log a clear error and fail the operation (no fallback)
3. WHEN an insert operation specifies "after" THEN it SHALL find the correct insertion point
4. WHEN multiple operations target the same node THEN they SHALL be applied in sequence correctly
5. IF an operation fails THEN it SHALL not affect other operations in the same changeset
6. WHEN operations complete THEN the result SHALL clearly indicate success/failure for each operation
7. WHEN a node is not found THEN the error message SHALL include all available node IDs for debugging

**Code References:**
- `src/main/java/com/tripplanner/service/ChangeEngine.java` (Lines 456-476, method `replaceNode`)
- `src/main/java/com/tripplanner/service/ChangeEngine.java` (Lines 398-420, method `insertNode`)
- `src/main/java/com/tripplanner/service/ChangeEngine.java` (Lines 422-454, method `deleteNode`)

**Current Problematic Code (Lines 456-476):**
```java
NormalizedNode nodeToReplace = findNodeById(itinerary, op.getId());
if (nodeToReplace == null) {
    logger.warn("Node not found for replace operation: {}, attempting fallback strategy", op.getId());
    // Falls back to first node in day - WRONG!
    List<NormalizedNode> nodes = targetDay.getNodes();
    if (nodes != null && !nodes.isEmpty()) {
        nodeToReplace = nodes.get(0);
        logger.warn("Using fallback node '{}' for replace operation", nodeToReplace.getId());
    }
}
```

**Required Change:**
```java
NormalizedNode nodeToReplace = findNodeById(itinerary, op.getId());
if (nodeToReplace == null) {
    String availableIds = getAvailableNodeIds(itinerary);
    String errorMsg = String.format(
        "Node with ID '%s' not found. Available node IDs: %s", 
        op.getId(), availableIds);
    logger.error(errorMsg);
    return false; // Fail fast, no fallback
}
```

### Requirement 4: Implement Node ID Migration Service

**User Story:** As a system administrator, I want existing itineraries to be automatically migrated to the new ID pattern, so that the fix works for all users.

#### Acceptance Criteria

1. WHEN the system starts THEN it SHALL detect itineraries with old ID patterns
2. WHEN an itinerary is loaded THEN it SHALL be automatically migrated to new ID pattern if needed
3. WHEN migration occurs THEN it SHALL preserve node order and relationships
4. WHEN migration completes THEN it SHALL save the updated itinerary to Firestore
5. IF migration fails THEN it SHALL log the error and continue with original IDs
6. WHEN migration is complete THEN all node references SHALL be consistent
7. WHEN migration occurs THEN it SHALL update the itinerary version number

**Code References:**
- New service to be created: `src/main/java/com/tripplanner/service/ItineraryMigrationService.java`
- Integration point: `src/main/java/com/tripplanner/service/ItineraryService.java` (method `get`)
- Integration point: `src/main/java/com/tripplanner/agents/EditorAgent.java` (method `executeInternal`)

### Requirement 5: Update NodeIdGenerator Implementation

**User Story:** As a developer, I want a centralized node ID generator that produces consistent IDs, so that all components use the same pattern.

#### Acceptance Criteria

1. WHEN NodeIdGenerator.generateId() is called THEN it SHALL return `day{N}_node{M}` format
2. WHEN generating an ID THEN it SHALL find the next available sequential number for the day
3. WHEN the same title is used multiple times THEN each SHALL get a unique sequential ID
4. WHEN generating IDs THEN it SHALL be thread-safe for concurrent operations
5. IF day number is not provided THEN it SHALL default to day 1
6. WHEN an ID is generated THEN it SHALL be logged for debugging purposes
7. WHEN ensureNodeHasId() is called THEN it SHALL assign sequential IDs if missing

**Code References:**
- `src/main/java/com/tripplanner/service/NodeIdGenerator.java` (Lines 31-38, 48-52, 60-70)

**Current Implementation (Lines 31-38):**
```java
public String generateNodeId(String nodeType, Integer dayNumber) {
    long timestamp = System.currentTimeMillis();
    String typePrefix = sanitizeType(nodeType);
    String dayPart = dayNumber != null ? "_day" + dayNumber : "";
    String uuid = UUID.randomUUID().toString().substring(0, 8);
    return String.format("%s_%s%s_%d_%s", NODE_PREFIX, typePrefix, dayPart, timestamp % 10000, uuid);
}
```

**Required New Implementation:**
```java
public String generateNodeId(String nodeType, Integer dayNumber, NormalizedItinerary itinerary) {
    int nextNodeNumber = findNextNodeNumber(itinerary, dayNumber);
    return String.format("day%d_node%d", dayNumber, nextNodeNumber);
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
```

### Requirement 6: Improve Error Handling and Logging

**User Story:** As a developer, I want clear error messages and logging when node operations fail, so that I can quickly diagnose issues.

#### Acceptance Criteria

1. WHEN a node is not found THEN the error message SHALL include the expected ID and available IDs
2. WHEN an operation fails THEN it SHALL log the full operation details and failure reason
3. WHEN IDs are generated THEN they SHALL be logged with the source (title, type, day)
4. WHEN context is built for LLM THEN it SHALL log the number of nodes included per day
5. IF multiple operations fail THEN each failure SHALL be logged separately with context
6. WHEN debugging is enabled THEN it SHALL log the full LLM context and response

**Code References:**
- `src/main/java/com/tripplanner/service/ChangeEngine.java` (Lines 456-476, 398-420, 422-454)
- `src/main/java/com/tripplanner/service/NodeIdGenerator.java` (Lines 31-38, 60-70)
- `src/main/java/com/tripplanner/service/SummarizationService.java` (Lines 327-350)
- `src/main/java/com/tripplanner/agents/EditorAgent.java` (Lines 130-145)

### Requirement 7: Maintain Backward Compatibility

**User Story:** As a user, I want my existing itineraries to continue working without any disruption, so that the fix is transparent to me.

#### Acceptance Criteria

1. WHEN an old itinerary is accessed THEN it SHALL be automatically migrated without user action
2. WHEN migration occurs THEN the user SHALL not experience any delays or errors
3. WHEN the system processes old ID patterns THEN it SHALL handle them gracefully during transition
4. WHEN API responses are sent THEN they SHALL use the new consistent ID pattern
5. IF migration fails THEN the system SHALL continue to work with original IDs
6. WHEN the fix is deployed THEN existing user sessions SHALL not be disrupted

### Requirement 8: Validate Fix Effectiveness

**User Story:** As a QA engineer, I want to verify that the node ID fix resolves all the reported issues, so that users have a reliable experience.

#### Acceptance Criteria

1. WHEN a user adds a node via chat THEN it SHALL appear immediately in the correct day section
2. WHEN multiple nodes are modified THEN each SHALL be updated correctly without affecting others
3. WHEN the LLM generates change operations THEN they SHALL target the correct existing nodes
4. WHEN operations are applied THEN the change comparison SHALL show accurate before/after states
5. IF an operation fails THEN the user SHALL receive a clear error message explaining why
6. WHEN the system is under load THEN node operations SHALL remain consistent and reliable

---

## Success Criteria

### Functional Success
- ✅ All new nodes use `day{N}_node{M}` ID pattern
- ✅ LLM can correctly reference existing nodes
- ✅ ChangeEngine applies operations to correct nodes
- ✅ No more fallback to wrong nodes
- ✅ Added nodes appear in UI immediately

### Technical Success
- ✅ All components use NodeIdGenerator consistently
- ✅ Existing itineraries migrated automatically
- ✅ Error handling provides clear diagnostics
- ✅ Thread-safe ID generation
- ✅ Comprehensive logging for debugging

### User Experience Success
- ✅ Chat-based node additions work reliably
- ✅ Change comparisons show accurate results
- ✅ No duplicate or missing nodes
- ✅ Transparent migration (no user impact)
- ✅ Clear error messages when operations fail

---

## Out of Scope

- Changing the overall itinerary data structure
- Modifying the LLM prompt templates (beyond node ID context)
- Performance optimization of node operations
- UI changes (this is a backend fix)
- Historical data cleanup (old IDs in logs/analytics)

---

## Risk Assessment

### High Risk
- **Data Migration**: Existing itineraries must be migrated correctly
- **Concurrent Operations**: Multiple users editing same itinerary
- **LLM Behavior**: Changes to context might affect LLM responses

### Medium Risk
- **Performance Impact**: ID generation and migration overhead
- **Backward Compatibility**: Handling mixed old/new ID patterns

### Low Risk
- **Code Complexity**: Changes are localized to specific components
- **Testing**: Can be thoroughly tested with existing itineraries

---

## Dependencies

- NodeIdGenerator service
- ChangeEngine service
- EditorAgent
- ItineraryJsonService (for migration)
- All pipeline agents (SkeletonPlanner, Activity, Meal, Transport)

---

## Acceptance Testing Scenarios

### Scenario 1: New Node Addition
```
GIVEN a user has an existing itinerary with day 4
WHEN they request "Add Muzeum Sportu i Turystyki w Warszawie to day 4"
THEN the museum should appear in day 4 section of the UI
AND the node ID should follow day4_node{N} pattern
AND the change comparison should show 1 addition, 0 removals
```

### Scenario 2: Multiple Node Updates
```
GIVEN a user requests time changes for multiple nodes
WHEN the LLM generates replace operations for day4_node2, day4_node3, day4_node4
THEN each node should be found and updated correctly
AND no fallback operations should occur
AND the change comparison should show 3 modifications
```

### Scenario 3: Migration of Existing Itinerary
```
GIVEN an itinerary exists with old ID pattern (node_att_day4_2274_7de9e730)
WHEN the itinerary is loaded
THEN all nodes should be automatically migrated to day{N}_node{M} pattern
AND the user should see no disruption
AND subsequent operations should use the new IDs
```

### Scenario 4: Error Handling
```
GIVEN the LLM generates an operation with invalid node ID
WHEN ChangeEngine processes the operation
THEN it should fail with clear error message
AND it should not affect other valid operations
AND the user should receive meaningful feedback
```

---

## Definition of Done

- [ ] All requirements implemented and tested
- [ ] Existing itineraries migrate automatically
- [ ] LLM context includes correct node IDs
- [ ] ChangeEngine finds nodes reliably
- [ ] No more fallback to wrong nodes
- [ ] Added nodes appear in UI immediately
- [ ] Error messages are clear and actionable
- [ ] Performance impact is minimal
- [ ] Code is well-documented and maintainable
- [ ] Integration tests pass for all scenarios
