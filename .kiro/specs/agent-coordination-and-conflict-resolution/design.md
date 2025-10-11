# Agent Coordination and Conflict Resolution - Design Document

## Overview

This design implements a robust agent coordination system that eliminates conflicts, ensures data consistency, and provides reliable task execution within the existing monolithic Spring Boot architecture. The solution introduces canonical place management, durable task processing, strict write policies, and comprehensive conflict resolution mechanisms.

**IMPORTANT: This is a completely new system design with no backward compatibility considerations. All components are designed to be clean, modern implementations without legacy support.**

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                      │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │  AgentOrchestrator │    │   ChangeEngine   │    │ PlaceRegistry │ │
│  │                 │    │                 │    │              │ │
│  │ - Task Routing  │    │ - Version Control│    │ - Dedup Logic│ │
│  │ - Coordination  │    │ - Write Policy   │    │ - Canonical  │ │
│  │ - Conflict Res. │    │ - Transactions   │    │   Places     │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
│           │                       │                       │     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    Agent Layer                              │ │
│  │ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ │ │
│  │ │Planner  │ │Editor   │ │Enrichmt │ │Places   │ │Booking  │ │ │
│  │ │Agent    │ │Agent    │ │Agent    │ │Agent    │ │Agent    │ │ │
│  │ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ │ │
│  └─────────────────────────────────────────────────────────────┘ │
│           │                       │                       │     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                  Task Management                            │ │
│  │ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────┐ │ │
│  │ │AgentTask    │ │TaskProcessor│ │RetryHandler │ │DLQ      │ │ │
│  │ │Service      │ │             │ │             │ │Manager  │ │ │
│  │ └─────────────┘ └─────────────┘ └─────────────┘ └─────────┘ │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                    ┌─────────────────────────┐
                    │      Firestore          │
                    │                         │
                    │ ┌─────────────────────┐ │
                    │ │ Collections:        │ │
                    │ │ - itineraries       │ │
                    │ │ - places            │ │
                    │ │ - agentTasks        │ │
                    │ │ - revisions         │ │
                    │ │ - dlq               │ │
                    │ └─────────────────────┘ │
                    └─────────────────────────┘
```

### Component Responsibilities

#### AgentOrchestrator
- **Primary Role**: Central coordination hub for all agent interactions
- **Responsibilities**: Task routing, conflict resolution, agent sequencing
- **Key Methods**: `routeTask()`, `resolveConflict()`, `coordinateAgents()`

#### ChangeEngine
- **Primary Role**: Single source of truth for all itinerary modifications
- **Responsibilities**: Version control, write policy enforcement, transaction management
- **Key Methods**: `applyChangeSet()`, `validateVersion()`, `createRevision()`

#### PlaceRegistry
- **Primary Role**: Canonical place data management and deduplication
- **Responsibilities**: Place matching, merging, enrichment coordination
- **Key Methods**: `findCanonicalPlace()`, `mergePlace()`, `enrichPlace()`

## Components and Interfaces

### 1. Data Models

#### Place (Canonical Place Registry)
```java
@Data
@Document(collection = "places")
public class Place {
    private String placeId;                    // Stable canonical ID
    private String name;                       // Primary name
    private List<String> nameVariants;        // Alternative names
    private Coordinates coordinates;           // Lat/lng
    private Map<String, String> sources;      // External IDs (Google, OSM)
    private Instant lastEnrichedAt;           // Last enrichment timestamp
    private Double qualityScore;              // Confidence intensity (0-1)
    private List<String> tags;                // Categories/tags
    private List<String> photoUrls;           // Photo references
    private OpeningHours openingHours;        // Operating hours
    private String embeddingsId;              // Optional embedding reference
    private List<String> mergedFrom;          // Source candidate IDs
    private PlaceMetrics metrics;             // Usage statistics
}
```

#### AgentTask (Durable Task System)
```java
@Data
@Document(collection = "agentTasks")
public class AgentTask {
    private String taskId;                    // Unique task identifier
    private TaskType type;                    // ENRICH_NEW, PLAN_ITINERARY, etc.
    private Map<String, Object> payload;     // Task-specific data
    private TaskStatus status;               // PENDING, RUNNING, COMPLETED, FAILED
    private String ownerAgent;               // Agent responsible for processing
    private Integer retryCount;              // Current retry attempt
    private String traceId;                  // Correlation ID
    private String idempotencyKey;           // Duplicate prevention
    private Instant createdAt;               // Creation timestamp
    private Instant updatedAt;               // Last update timestamp
    private String lastError;                // Error details if failed
    private Integer priority;                // Task priority (1-10)
    private Instant scheduledAt;             // When to process (for delays)
}
```

#### EnhancedChangeSet (Strict Write Policy)
```java
@Data
public class EnhancedChangeSet {
    private String changeSetId;              // Unique changeset ID
    private String itineraryId;              // Target itinerary
    private Integer baseVersion;             // Expected current version
    private List<ChangeOperation> operations; // List of changes
    private String origin;                   // Originating agent/user
    private String traceId;                  // Correlation ID
    private String idempotencyKey;           // Duplicate prevention
    private Instant createdAt;               // Creation timestamp
    private Map<String, Object> metadata;   // Additional context
}
```

#### EnrichmentRequest/Response
```java
@Data
public class EnrichmentRequest {
    private String requestId;
    private EnrichmentType type;             // ENRICH_NEW, VALIDATE_EXISTING
    private PlaceCandidate candidate;        // Place to enrich
    private List<String> fields;            // Requested enrichment fields
    private String traceId;
    private String idempotencyKey;
    private Priority priority;
}

@Data
public class EnrichmentResponse {
    private String requestId;
    private EnrichmentStatus status;         // COMPLETED, PARTIAL, FAILED
    private String canonicalPlaceId;         // Result place ID
    private Double confidence;               // Confidence intensity
    private List<String> warnings;          // Non-fatal issues
    private List<PlaceCandidate> candidates; // For PARTIAL status
    private String traceId;
    private Map<String, Object> metadata;   // Additional data
}
```

### 2. Core Services

#### PlaceRegistryService
```java
@Service
public class PlaceRegistryService {
    
    @Transactional
    public Place upsertPlace(PlaceCandidate candidate, String idempotencyKey) {
        // 1. Find existing places within radius
        List<Place> nearby = findPlacesWithinRadius(candidate.getCoordinates(), 100);
        
        // 2. Check for name similarity matches
        Optional<Place> match = findBestNameMatch(nearby, candidate.getName());
        
        if (match.isPresent()) {
            // Merge with existing place
            Place existing = match.get();
            mergePlace(existing, candidate);
            return placeRepository.save(existing);
        } else {
            // Create new canonical place
            Place newPlace = createCanonicalPlace(candidate);
            return placeRepository.save(newPlace);
        }
    }
    
    private Optional<Place> findBestNameMatch(List<Place> candidates, String name) {
        return candidates.stream()
            .filter(place -> calculateNameSimilarity(place.getName(), name) > 0.8)
            .max(Comparator.comparing(Place::getQualityScore));
    }
    
    private double calculateNameSimilarity(String name1, String name2) {
        // Implement Levenshtein distance or token-based similarity
        String normalized1 = normalizeName(name1);
        String normalized2 = normalizeName(name2);
        return StringUtils.getJaroWinklerDistance(normalized1, normalized2);
    }
}
```

#### AgentTaskService
```java
@Service
public class AgentTaskService {
    
    private final FirestoreTemplate firestoreTemplate;
    private final RetryHandler retryHandler;
    
    @PostConstruct
    public void initializeTaskListener() {
        // Set up Firestore snapshot listener for PENDING tasks
        firestoreTemplate.collection("agentTasks")
            .whereEqualTo("status", TaskStatus.PENDING)
            .addSnapshotListener(this::processPendingTasks);
    }
    
    public AgentTask createTask(TaskType type, Map<String, Object> payload, 
                               String traceId, String idempotencyKey) {
        // Check for existing task with same idempotency key
        Optional<AgentTask> existing = findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        AgentTask task = AgentTask.builder()
            .taskId(UUID.randomUUID().toString())
            .type(type)
            .payload(payload)
            .status(TaskStatus.PENDING)
            .traceId(traceId)
            .idempotencyKey(idempotencyKey)
            .createdAt(Instant.now())
            .retryCount(0)
            .build();
            
        return agentTaskRepository.save(task);
    }
    
    private void processPendingTasks(QuerySnapshot snapshot, FirestoreException error) {
        if (error != null) {
            logger.error("Error listening to pending tasks", error);
            return;
        }
        
        for (DocumentChange change : snapshot.getDocumentChanges()) {
            if (change.getType() == DocumentChange.Type.ADDED) {
                AgentTask task = change.getDocument().toObject(AgentTask.class);
                processTaskAsync(task);
            }
        }
    }
    
    @Async
    public void processTaskAsync(AgentTask task) {
        try {
            // Update status to RUNNING
            updateTaskStatus(task.getTaskId(), TaskStatus.RUNNING);
            
            // Route to appropriate agent
            Object result = agentOrchestrator.processTask(task);
            
            // Update status to COMPLETED
            updateTaskStatus(task.getTaskId(), TaskStatus.COMPLETED);
            
        } catch (Exception e) {
            handleTaskFailure(task, e);
        }
    }
    
    private void handleTaskFailure(AgentTask task, Exception error) {
        if (task.getRetryCount() < MAX_RETRIES) {
            retryHandler.scheduleRetry(task, error);
        } else {
            dlqManager.moveToDeadLetterQueue(task, error);
        }
    }
}
```

#### Enhanced ChangeEngine
```java
@Service
public class EnhancedChangeEngine {
    
    @Transactional
    public ApplyResult applyChangeSet(EnhancedChangeSet changeSet) {
        // 1. Load current itinerary
        NormalizedItinerary current = itineraryService.getItinerary(changeSet.getItineraryId())
            .orElseThrow(() -> new ItineraryNotFoundException(changeSet.getItineraryId()));
        
        // 2. Validate version
        if (!current.getVersion().equals(changeSet.getBaseVersion())) {
            throw new VersionMismatchException(
                current.getVersion(), changeSet.getBaseVersion());
        }
        
        // 3. Apply operations
        NormalizedItinerary updated = applyOperations(current, changeSet.getOperations());
        updated.setVersion(current.getVersion() + 1);
        updated.setUpdatedAt(Instant.now());
        
        // 4. Save itinerary and create revision
        NormalizedItinerary saved = itineraryService.save(updated);
        revisionService.createRevision(current, changeSet);
        
        // 5. Emit events
        agentEventBus.publish(ItineraryUpdatedEvent.from(saved, changeSet));
        
        return ApplyResult.builder()
            .toVersion(saved.getVersion())
            .diff(calculateDiff(current, saved))
            .build();
    }
    
    private NormalizedItinerary applyOperations(NormalizedItinerary itinerary, 
                                              List<ChangeOperation> operations) {
        NormalizedItinerary result = deepCopy(itinerary);
        
        for (ChangeOperation op : operations) {
            switch (op.getType()) {
                case ADD_NODE:
                    applyAddNode(result, op);
                    break;
                case UPDATE_NODE:
                    applyUpdateNode(result, op);
                    break;
                case LINK_PLACE:
                    applyLinkPlace(result, op);
                    break;
                case REMOVE_NODE:
                    applyRemoveNode(result, op);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown operation: " + op.getType());
            }
        }
        
        return result;
    }
}
```

### 3. Agent Coordination

#### Enhanced AgentOrchestrator
```java
@Service
public class EnhancedAgentOrchestrator {
    
    private final Map<String, BaseAgent> agents;
    private final AgentTaskService taskService;
    private final ConflictResolver conflictResolver;
    
    public CompletableFuture<Object> processTask(AgentTask task) {
        String traceId = task.getTraceId();
        logger.info("Processing task {} with trace {}", task.getTaskId(), traceId);
        
        try {
            // Route to appropriate agent
            BaseAgent agent = routeToAgent(task.getType());
            
            // Execute task
            Object result = agent.execute(task);
            
            // Handle result
            return handleTaskResult(task, result);
            
        } catch (Exception e) {
            logger.error("Task execution failed for {}", task.getTaskId(), e);
            throw e;
        }
    }
    
    private BaseAgent routeToAgent(TaskType taskType) {
        return switch (taskType) {
            case ENRICH_NEW, ENRICH_EXISTING -> agents.get("enrichmentAgent");
            case PLAN_ITINERARY -> agents.get("plannerAgent");
            case EDIT_ITINERARY -> agents.get("editorAgent");
            case DISCOVER_PLACES -> agents.get("placesAgent");
            case BOOK_ACCOMMODATION, BOOK_ACTIVITY -> agents.get("bookingAgent");
            default -> throw new UnsupportedTaskTypeException(taskType);
        };
    }
    
    public void resolveConflict(List<EnhancedChangeSet> conflictingChangeSets) {
        // Attempt automatic merge for non-overlapping changes
        Optional<EnhancedChangeSet> merged = conflictResolver.attemptAutoMerge(conflictingChangeSets);
        
        if (merged.isPresent()) {
            // Apply merged changeset
            changeEngine.applyChangeSet(merged.get());
        } else {
            // Escalate to human resolution
            conflictResolver.escalateToHuman(conflictingChangeSets);
        }
    }
}
```

## Data Models

### Firestore Collections Structure

#### places/{placeId}
```json
{
  "placeId": "place_abc123",
  "name": "Marché de Papeete",
  "nameVariants": ["Papeete Market", "Marche Papeete"],
  "coordinates": {
    "lat": -17.5375,
    "lng": -149.5684
  },
  "sources": {
    "google": "ChIJN1t_tDeuEmsRUsoyG83frY4",
    "osm": "way/123456789"
  },
  "lastEnrichedAt": "2025-01-03T10:30:00Z",
  "qualityScore": 0.92,
  "tags": ["market", "food", "culture"],
  "photoUrls": ["https://..."],
  "openingHours": {
    "monday": {"open": "06:00", "close": "18:00"},
    "tuesday": {"open": "06:00", "close": "18:00"}
  },
  "embeddingsId": "emb_xyz789",
  "mergedFrom": ["candidate_1", "candidate_2"],
  "metrics": {
    "usageCount": 15,
    "lastUsed": "2025-01-03T09:00:00Z"
  }
}
```

#### agentTasks/{taskId}
```json
{
  "taskId": "task_def456",
  "type": "ENRICH_NEW",
  "payload": {
    "candidate": {
      "name": "Local Restaurant",
      "coordinates": {"lat": -17.5400, "lng": -149.5700},
      "source": "places_agent"
    },
    "fields": ["canonicalId", "photos", "openingHours"]
  },
  "status": "PENDING",
  "ownerAgent": "enrichment-agent",
  "retryCount": 0,
  "traceId": "trace_ghi789",
  "idempotencyKey": "enrich_day1_node3_v1",
  "createdAt": "2025-01-03T10:00:00Z",
  "updatedAt": "2025-01-03T10:00:00Z",
  "priority": 5,
  "scheduledAt": "2025-01-03T10:00:00Z"
}
```

#### dlq/{taskId}
```json
{
  "originalTask": { /* AgentTask object */ },
  "failureReason": "Maximum retries exceeded",
  "lastError": "Connection timeout to Google Places API",
  "failedAt": "2025-01-03T11:00:00Z",
  "requiresManualIntervention": true,
  "escalationLevel": "HIGH"
}
```

## Error Handling

### Retry Strategy
```java
@Component
public class RetryHandler {
    
    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 1000;
    
    public void scheduleRetry(AgentTask task, Exception error) {
        int retryCount = task.getRetryCount() + 1;
        long delayMs = calculateBackoffDelay(retryCount);
        
        task.setRetryCount(retryCount);
        task.setLastError(error.getMessage());
        task.setScheduledAt(Instant.now().plusMillis(delayMs));
        task.setStatus(TaskStatus.PENDING);
        
        agentTaskRepository.save(task);
        
        logger.info("Scheduled retry {} for task {} in {}ms", 
                   retryCount, task.getTaskId(), delayMs);
    }
    
    private long calculateBackoffDelay(int retryCount) {
        // Exponential backoff with jitter
        long baseDelay = BASE_DELAY_MS * (long) Math.pow(2, retryCount - 1);
        long jitter = (long) (Math.random() * baseDelay * 0.1);
        return baseDelay + jitter;
    }
}
```

### LLM Response Recovery
```java
@Component
public class LLMResponseRecovery {
    
    public String repairTruncatedJson(String partialJson, String originalPrompt) {
        try {
            // Attempt to parse as-is
            objectMapper.readTree(partialJson);
            return partialJson; // Already valid
        } catch (JsonProcessingException e) {
            // Attempt repair
            String repaired = attemptBraceBalancing(partialJson);
            
            if (isValidJson(repaired)) {
                return repaired;
            }
            
            // Request continuation from LLM
            return requestContinuation(partialJson, originalPrompt);
        }
    }
    
    private String attemptBraceBalancing(String json) {
        // Stack-based brace balancing logic
        Stack<Character> stack = new Stack<>();
        StringBuilder result = new StringBuilder(json);
        
        for (char c : json.toCharArray()) {
            if (c == '{' || c == '[') {
                stack.push(c);
            } else if (c == '}' || c == ']') {
                if (!stack.isEmpty()) {
                    stack.pop();
                }
            }
        }
        
        // Close remaining open braces/brackets
        while (!stack.isEmpty()) {
            char open = stack.pop();
            char close = (open == '{') ? '}' : ']';
            result.append(close);
        }
        
        return result.toString();
    }
}
```

## Testing Strategy

### Unit Tests
- **PlaceRegistryService**: Test deduplication logic, name similarity matching
- **AgentTaskService**: Test task creation, idempotency, retry mechanisms
- **ChangeEngine**: Test version validation, operation application, conflict detection
- **ConflictResolver**: Test auto-merge logic, escalation scenarios

### Integration Tests
- **End-to-end agent coordination**: Test complete task flows
- **Firestore transactions**: Test atomic operations and rollback scenarios
- **Concurrent modification**: Test version conflicts and resolution
- **Task durability**: Test system restart scenarios

### Performance Tests
- **Place lookup performance**: Test geospatial queries with large datasets
- **Task processing throughput**: Test concurrent task processing limits
- **Memory usage**: Test with high task volumes and long-running processes

## Migration Strategy

### Phase 1: Foundation (Week 1-2)
1. Create new Firestore collections (places, agentTasks, dlq)
2. Implement PlaceRegistryService with basic deduplication
3. Add AgentTaskService with Firestore listeners
4. Update existing agents to create tasks instead of direct processing

### Phase 2: Write Policy Enforcement (Week 3-4)
1. Enhance ChangeEngine with strict version checking
2. Add changeset validation and conflict detection
3. Migrate existing direct writes to use ChangeEngine
4. Implement revision tracking for rollback capability

### Phase 3: Advanced Features (Week 5-6)
1. Add sophisticated conflict resolution
2. Implement LLM response recovery mechanisms
3. Add comprehensive observability and metrics
4. Create admin UI for DLQ management

### Phase 4: Optimization (Week 7-8)
1. Performance tuning for place lookups
2. Task processing optimization
3. Memory usage optimization
4. Load testing and capacity planning

This design provides a robust, scalable solution that addresses all identified architectural issues while maintaining backward compatibility and enabling incremental migration.