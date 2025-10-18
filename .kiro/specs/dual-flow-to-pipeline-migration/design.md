# Design Document

## Overview

This design document outlines the architecture for migrating from a dual-flow itinerary generation system to a single pipeline-based flow. The migration consolidates two separate generation paths into one, improving maintainability, performance, and user experience while preserving all critical functionality.

### Current Architecture Problems

- **Dual maintenance burden**: Two separate flows require duplicate testing and bug fixes
- **Configuration confusion**: Conflicting defaults between `application.yml` (pipeline) and `ItineraryService` (monolithic)
- **Performance inconsistency**: Users may get different performance depending on configuration
- **Code duplication**: Similar initialization logic exists in multiple places

### Design Goals

1. **Single source of truth**: One generation flow, one set of agents, one configuration
2. **Preserve critical functionality**: `createInitialItinerary()` logic must be extracted and reused
3. **Zero user impact**: Migration should be transparent to end users
4. **Improved maintainability**: Simpler codebase with clear responsibilities
5. **Safe deployment**: Gradual rollout with rollback capability

## Architecture

### High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     ItineraryController                      │
│                    (HTTP API Layer)                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     ItineraryService                         │
│  - create(request, userId)                                   │
│  - get(id, userId)                                           │
│  - delete(id, userId)                                        │
└──────────┬──────────────────────────┬───────────────────────┘
           │                          │
           ▼                          ▼
┌──────────────────────┐   ┌─────────────────────────────────┐
│ItineraryInitialization│   │    PipelineOrchestrator        │
│      Service          │   │  - generateItinerary()          │
│- createInitialItinerary│   │  - executeSkeletonPhase()      │
│                       │   │  - executePopulationPhase()     │
└──────────┬────────────┘   │  - executeEnrichmentPhase()     │
           │                │  - executeFinalizationPhase()   │
           │                └────────┬────────────────────────┘
           │                         │
           ▼                         ▼
┌──────────────────────┐   ┌─────────────────────────────────┐
│ItineraryJsonService  │   │      Specialized Agents         │
│  (Firestore)         │   │  - SkeletonPlannerAgent         │
└──────────────────────┘   │  - ActivityAgent                │
                           │  - MealAgent                    │
┌──────────────────────┐   │  - TransportAgent               │
│  UserDataService     │   │  - EnrichmentAgent              │
│  (User Ownership)    │   │  - CostEstimatorAgent           │
└──────────────────────┘   └─────────────────────────────────┘
```

### Sequence Diagram: Itinerary Creation Flow

```
User → Controller → ItineraryService → InitService → JsonService
                                    ↓                      ↓
                                    ↓                  Firestore
                                    ↓
                                UserDataService
                                    ↓
                                Firestore
                                    ↓
                         PipelineOrchestrator
                                    ↓
                         ┌──────────┴──────────┐
                         ▼                     ▼
                  SkeletonAgent         ActivityAgent
                         ↓                     ↓
                  MealAgent              TransportAgent
                         ↓                     ↓
                  EnrichmentAgent       CostEstimatorAgent
                         ↓
                  AgentEventPublisher (SSE)
                         ↓
                      Frontend
```

## Components and Interfaces

### 1. ItineraryInitializationService (NEW)

**Purpose**: Extract and centralize the initial itinerary creation logic that was previously in `AgentOrchestrator`.

**Location**: `src/main/java/com/tripplanner/service/ItineraryInitializationService.java`

**Responsibilities**:
- Create initial itinerary structure
- Save to Firestore
- Establish user ownership
- Handle initialization errors

**Interface**:

```java
@Service
public class ItineraryInitializationService {
    
    private final ItineraryJsonService itineraryJsonService;
    private final UserDataService userDataService;
    
    /**
     * Create initial itinerary and establish ownership synchronously.
     * This method must complete before the API response is sent.
     * 
     * @param itineraryId Unique identifier for the itinerary
     * @param request User's itinerary creation request
     * @param userId Authenticated user ID
     * @return NormalizedItinerary with initial structure
     * @throws RuntimeException if creation or ownership establishment fails
     */
    public NormalizedItinerary createInitialItinerary(
        String itineraryId, 
        CreateItineraryReq request, 
        String userId
    );
    
    /**
     * Create the initial normalized itinerary structure.
     * Internal helper method.
     */
    private NormalizedItinerary createInitialStructure(
        String itineraryId, 
        CreateItineraryReq request, 
        String userId
    );
}
```

**Dependencies**:
- `ItineraryJsonService`: For saving to Firestore
- `UserDataService`: For establishing ownership

**Error Handling**:
- Catch all exceptions during initialization
- Log with full context (itineraryId, userId, error details)
- Wrap in `RuntimeException` with descriptive message
- Ensure transaction-like behavior (all-or-nothing)

### 2. ItineraryService (MODIFIED)

**Purpose**: Main service for itinerary operations, now using only pipeline flow.

**Location**: `src/main/java/com/tripplanner/service/ItineraryService.java`

**Changes**:

**REMOVE**:
```java
@Value("${itinerary.generation.mode:monolithic}")
private String generationMode;

private final AgentOrchestrator agentOrchestrator;

// Mode switching logic (lines 130-200)
if ("pipeline".equalsIgnoreCase(generationMode) && pipelineOrchestrator != null) {
    // pipeline code
} else if (agentOrchestrator != null) {
    // monolithic code
}
```

**ADD**:
```java
private final ItineraryInitializationService initService;
private final PipelineOrchestrator pipelineOrchestrator; // No longer optional

public ItineraryService(
    ItineraryInitializationService initService,
    ItineraryJsonService itineraryJsonService,
    UserDataService userDataService,
    PipelineOrchestrator pipelineOrchestrator,
    AgentEventPublisher agentEventPublisher
) {
    this.initService = initService;
    this.pipelineOrchestrator = pipelineOrchestrator;
    // ... other assignments
}
```

**Updated create() method**:
```java
public ItineraryDto create(CreateItineraryReq request, String userId) {
    String itineraryId = "it_" + UUID.randomUUID();
    
    // Step 1: Create initial itinerary synchronously
    NormalizedItinerary initialItinerary = 
        initService.createInitialItinerary(itineraryId, request, userId);
    
    // Step 2: Build response DTO
    ItineraryDto result = ItineraryDto.builder()
        .id(itineraryId)
        .status("generating")
        // ... other fields
        .build();
    
    // Step 3: Start async generation (always pipeline)
    String executionId = "exec_" + System.currentTimeMillis();
    CompletableFuture<NormalizedItinerary> future = 
        pipelineOrchestrator.generateItinerary(itineraryId, request, userId);
    
    // Step 4: Attach completion handlers
    future.whenComplete((result, throwable) -> {
        if (throwable != null) {
            logger.error("Pipeline generation failed: {}", itineraryId, throwable);
            agentEventPublisher.publishErrorFromException(
                itineraryId, executionId, (Exception) throwable,
                "itinerary generation", ErrorEvent.ErrorSeverity.ERROR
            );
        } else {
            logger.info("Pipeline generation completed: {}", itineraryId);
        }
    });
    
    return result;
}
```

### 3. PipelineOrchestrator (UNCHANGED)

**Purpose**: Coordinate multi-agent pipeline for itinerary generation.

**Location**: `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`

**No changes required** - this component already works correctly and will become the only generation path.

**Pipeline Phases**:
1. **Skeleton Generation** (10% progress) - Create day structure
2. **Population** (40% progress) - Fill in attractions, meals, transport (parallel)
3. **Enrichment** (70% progress) - Add real-world data from external APIs
4. **Cost Estimation** (90% progress) - Calculate costs per budget tier
5. **Finalization** (100% progress) - Validate and complete

### 4. Configuration (MODIFIED)

**File**: `src/main/resources/application.yml`

**REMOVE**:
```yaml
itinerary:
  generation:
    mode: ${ITINERARY_GENERATION_MODE:pipeline}
```

**REPLACE WITH**:
```yaml
itinerary:
  # Pipeline mode is now the only generation mode
  # Monolithic mode removed in migration (2025-10-18)
  generation:
    pipeline:
      parallel: ${ITINERARY_PIPELINE_PARALLEL:true}
      skeleton:
        timeout-ms: ${ITINERARY_SKELETON_TIMEOUT_MS:120000}
      population:
        timeout-ms: ${ITINERARY_POPULATION_TIMEOUT_MS:180000}
      enrichment:
        timeout-ms: ${ITINERARY_ENRICHMENT_TIMEOUT_MS:60000}
      finalization:
        timeout-ms: ${ITINERARY_FINALIZATION_TIMEOUT_MS:30000}
```

## Data Models

### NormalizedItinerary

**No changes required** - this model is already used by both flows and will continue to work with pipeline flow.

**Key fields**:
```java
public class NormalizedItinerary {
    private String itineraryId;
    private int version;
    private String userId;
    private long createdAt;
    private long updatedAt;
    private String summary;
    private String currency;
    private List<String> themes;
    private List<NormalizedDay> days;
    private String origin;
    private String destination;
    private String startDate;
    private String endDate;
    private ItinerarySettings settings;
    private Map<String, AgentStatus> agents;
}
```

### TripMetadata

**No changes required** - used for user ownership tracking.

```java
public class TripMetadata {
    private String itineraryId;
    private String destination;
    private String startDate;
    private String endDate;
    private String language;
    private String summary;
    private List<String> interests;
    private long createdAt;
}
```

## Error Handling

### Initialization Errors

**Scenario**: Initial itinerary creation fails

**Handling**:
1. Log error with full context (itineraryId, userId, request details)
2. Throw `RuntimeException` with descriptive message
3. Return 500 Internal Server Error to client
4. Do NOT start async generation

**Example**:
```java
try {
    NormalizedItinerary initialItinerary = 
        initService.createInitialItinerary(itineraryId, request, userId);
} catch (Exception e) {
    logger.error("Failed to create initial itinerary: {}", itineraryId, e);
    throw new RuntimeException("Failed to create itinerary", e);
}
```

### Async Generation Errors

**Scenario**: Pipeline generation fails after initial creation

**Handling**:
1. Log error with full context
2. Publish error event via SSE to frontend
3. Do NOT throw exception (already returned 200 OK)
4. Frontend displays error message to user

**Example**:
```java
future.whenComplete((result, throwable) -> {
    if (throwable != null) {
        logger.error("Pipeline generation failed: {}", itineraryId, throwable);
        agentEventPublisher.publishErrorFromException(
            itineraryId, executionId, (Exception) throwable,
            "itinerary generation", ErrorEvent.ErrorSeverity.ERROR
        );
    }
});
```

### Phase-Specific Errors

**Scenario**: Individual pipeline phase fails

**Handling**:
1. Log warning (not error) for non-critical phases
2. Continue to next phase with partial data
3. Only fail completely if skeleton generation fails

**Example**:
```java
try {
    enrichmentAgent.execute(itineraryId, request);
} catch (Exception e) {
    logger.warn("Enrichment failed, continuing with basic data: {}", e.getMessage());
    // Continue to next phase
}
```

## Testing Strategy

### Unit Tests

**New Tests**:
1. `ItineraryInitializationServiceTest`
   - Test successful initialization
   - Test Firestore failure handling
   - Test UserDataService failure handling
   - Test initial structure creation

**Modified Tests**:
2. `ItineraryServiceTest`
   - Replace `AgentOrchestrator` mock with `ItineraryInitializationService` mock
   - Remove monolithic flow test cases
   - Update assertions to expect pipeline flow only
   - Verify `PipelineOrchestrator` is always called

**Test Coverage Goals**:
- Line coverage: >80%
- Branch coverage: >70%
- All error paths tested

### Integration Tests

**Test Scenarios**:

1. **Happy Path - Complete Generation**
```
POST /api/v1/itineraries
→ Verify 200 OK
→ Verify initial itinerary created in Firestore
→ Verify ownership established in UserDataService
→ Wait for SSE events
→ Verify progress events (10%, 40%, 70%, 90%, 100%)
→ Verify completion event
→ GET /api/v1/itineraries/{id}/json
→ Verify complete itinerary with all days and nodes
```

2. **Error Handling - Initialization Failure**
```
POST /api/v1/itineraries (with invalid data)
→ Verify 500 Internal Server Error
→ Verify error message in response
→ Verify no itinerary created in Firestore
→ Verify no ownership established
```

3. **Error Handling - Async Generation Failure**
```
POST /api/v1/itineraries (with problematic destination)
→ Verify 200 OK (initial creation succeeds)
→ Wait for SSE events
→ Verify error event published
→ Verify error message contains phase information
```

4. **Concurrent Requests**
```
POST /api/v1/itineraries (5 concurrent requests)
→ Verify all 5 return 200 OK
→ Verify all 5 have unique itinerary IDs
→ Verify all 5 have unique execution IDs
→ Verify SSE events properly isolated
→ Verify all 5 complete successfully
```

### Performance Tests

**Metrics to Measure**:
- Average generation time (target: <90 seconds)
- P95 generation time (target: <120 seconds)
- P99 generation time (target: <180 seconds)
- Timeout rate (target: <5%)
- Error rate (target: <2%)
- Memory usage (stable, no leaks)
- CPU usage (reasonable under load)

**Load Test Scenarios**:
- 10 concurrent users
- 50 concurrent users
- 100 concurrent users
- Sustained load for 1 hour

## Deployment Strategy

### Phase 1: Development Environment

**Steps**:
1. Deploy to dev environment
2. Run full test suite
3. Manual testing of key flows
4. Monitor logs for errors
5. Verify SSE events working

**Success Criteria**:
- All tests pass
- No errors in logs
- SSE events received correctly
- Generation completes successfully

### Phase 2: Staging Environment

**Steps**:
1. Deploy to staging environment
2. Run integration tests
3. Run performance tests
4. Load testing with realistic traffic
5. Monitor for 24 hours

**Success Criteria**:
- Integration tests pass
- Performance meets targets
- No memory leaks
- No unexpected errors

### Phase 3: Production Rollout

**Steps**:
1. Deploy to production (10% traffic)
2. Monitor metrics for 2 hours
3. Increase to 50% traffic
4. Monitor metrics for 4 hours
5. Increase to 100% traffic
6. Monitor metrics for 24 hours

**Success Criteria**:
- Error rate <2%
- Timeout rate <5%
- Average generation time <90s
- No user complaints

### Rollback Plan

**Immediate Rollback** (<5 minutes):
```bash
# Revert to previous version
git revert HEAD
git push origin main

# Or rollback deployment
kubectl rollout undo deployment/tripplanner-backend
```

**Partial Rollback** (re-enable monolithic):
1. Restore deleted files from backup branch
2. Restore mode switching in ItineraryService
3. Set environment variable: `ITINERARY_GENERATION_MODE=monolithic`
4. Rebuild and redeploy

## Monitoring and Observability

### Key Metrics

**Generation Metrics**:
- `itinerary.generation.duration` - Total generation time
- `itinerary.generation.success_rate` - Percentage of successful generations
- `itinerary.generation.timeout_rate` - Percentage of timeouts
- `itinerary.generation.error_rate` - Percentage of errors

**Phase Metrics**:
- `pipeline.phase.skeleton.duration` - Skeleton phase time
- `pipeline.phase.population.duration` - Population phase time
- `pipeline.phase.enrichment.duration` - Enrichment phase time
- `pipeline.phase.finalization.duration` - Finalization phase time

**SSE Metrics**:
- `sse.events.published` - Number of SSE events published
- `sse.connections.active` - Number of active SSE connections
- `sse.connection.failures` - Number of SSE connection failures

### Log Patterns

**Success Logs**:
```
INFO: Creating initial itinerary: it_12345
INFO: Initial itinerary created and ownership established: it_12345
INFO: Starting async itinerary generation: it_12345
INFO: Pipeline generation completed successfully: it_12345
```

**Error Logs**:
```
ERROR: Failed to create initial itinerary: it_12345
ERROR: Pipeline generation failed: it_12345
ERROR: Skeleton generation timed out after 120000 ms
```

### Alerts

**Critical Alerts** (page on-call):
- Error rate >5% for 5 minutes
- Timeout rate >10% for 5 minutes
- Average generation time >180s for 10 minutes

**Warning Alerts** (notify team):
- Error rate >2% for 10 minutes
- Timeout rate >5% for 10 minutes
- Average generation time >120s for 15 minutes

## Security Considerations

### Authentication

**No changes required** - Firebase authentication continues to work as before.

### Authorization

**No changes required** - User ownership checks remain in place:
```java
if (!userDataService.userOwnsTrip(userId, itineraryId)) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
}
```

### Data Privacy

**No changes required** - User data continues to be isolated by userId in Firestore.

## Migration Checklist

- [ ] Create `ItineraryInitializationService`
- [ ] Update `ItineraryService` to use new service
- [ ] Remove mode switching logic
- [ ] Delete `AgentOrchestrator.java`
- [ ] Delete `PlannerAgent.java`
- [ ] Delete `ResilientAgentOrchestrator.java`
- [ ] Update `application.yml` configuration
- [ ] Create unit tests for `ItineraryInitializationService`
- [ ] Update unit tests for `ItineraryService`
- [ ] Run all tests and verify they pass
- [ ] Deploy to dev environment
- [ ] Deploy to staging environment
- [ ] Run integration tests
- [ ] Run performance tests
- [ ] Deploy to production (gradual rollout)
- [ ] Monitor metrics for 24 hours
- [ ] Document migration completion

## References

- Migration Guide: `analysis/DUAL_FLOW_MIGRATION_GUIDE.md`
- Current ItineraryService: `src/main/java/com/tripplanner/service/ItineraryService.java`
- Current AgentOrchestrator: `src/main/java/com/tripplanner/agents/AgentOrchestrator.java`
- PipelineOrchestrator: `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`
