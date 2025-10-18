# Design Document: Fix UI Progress Tracking

## Overview

This design addresses the fire-and-forget async pattern in `ItineraryService.java` by properly handling `CompletableFuture` results and connecting them to the event publishing infrastructure. The solution maintains backward compatibility while enabling real-time progress tracking and error reporting.

**Design Principles:**
1. **Non-blocking**: Use callbacks instead of blocking operations
2. **Fail-safe**: Errors in callbacks don't affect main flow
3. **Observable**: Comprehensive logging at all stages
4. **Maintainable**: Clear code structure with comments
5. **Backward Compatible**: No breaking changes to existing API

---

## Architecture

### Current Architecture (Broken)

```
┌─────────────────────────────────────────────────────────────┐
│ ItineraryService.create()                                    │
│                                                               │
│  1. Create initial itinerary (sync)                          │
│  2. Wait 2 seconds for SSE connection                        │
│  3. Call pipelineOrchestrator.generateItinerary()            │
│     └─> Returns CompletableFuture<NormalizedItinerary>       │
│         └─> ❌ IGNORED (fire-and-forget)                     │
│  4. Return ItineraryDto with status="generating"             │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│ PipelineOrchestrator.generateItinerary() (async)            │
│                                                               │
│  - Publishes progress events via AgentEventPublisher         │
│  - Publishes completion event on success                     │
│  - Throws exception on failure                               │
│    └─> ❌ Exception not caught (silent failure)              │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│ SSE Connection (Frontend)                                    │
│                                                               │
│  - Receives progress events ✅                               │
│  - Receives completion events ✅                             │
│  - ❌ Never receives error events (not published)            │
└─────────────────────────────────────────────────────────────┘
```

### Fixed Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ ItineraryService.create()                                    │
│                                                               │
│  1. Create initial itinerary (sync)                          │
│  2. Wait 2 seconds for SSE connection                        │
│  3. Generate executionId                                     │
│  4. Call pipelineOrchestrator.generateItinerary()            │
│     └─> Returns CompletableFuture<NormalizedItinerary>       │
│         └─> ✅ CAPTURED in variable                          │
│  5. Attach .whenComplete() callback                          │
│     ├─> On success: Log completion                           │
│     └─> On error: Publish error event via AgentEventPublisher│
│  6. Return ItineraryDto with status="generating"             │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│ PipelineOrchestrator.generateItinerary() (async)            │
│                                                               │
│  - Publishes progress events via AgentEventPublisher         │
│  - Publishes completion event on success                     │
│  - Throws exception on failure                               │
│    └─> ✅ Exception caught by .whenComplete() callback       │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│ .whenComplete() Callback                                     │
│                                                               │
│  IF throwable != null:                                       │
│    - Log error with full stack trace                         │
│    - Publish error event via AgentEventPublisher             │
│    - Include itineraryId, executionId, exception             │
│  ELSE:                                                        │
│    - Log successful completion                               │
│    - (Completion event already published by orchestrator)    │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│ SSE Connection (Frontend)                                    │
│                                                               │
│  - Receives progress events ✅                               │
│  - Receives completion events ✅                             │
│  - ✅ Receives error events (now published)                  │
└─────────────────────────────────────────────────────────────┘
```

---

## Components and Interfaces

### 1. ItineraryService (Modified)

**File:** `src/main/java/com/tripplanner/service/ItineraryService.java`

**Changes:**
1. Add `AgentEventPublisher` dependency
2. Capture `CompletableFuture` return value
3. Attach `.whenComplete()` callback
4. Generate unique `executionId`
5. Publish error events on failure

**New Dependencies:**
```java
private final AgentEventPublisher agentEventPublisher;
```

**Modified Constructor:**
```java
public ItineraryService(
    AgentOrchestrator agentOrchestrator,
    ItineraryJsonService itineraryJsonService,
    UserDataService userDataService,
    @Autowired(required = false) PipelineOrchestrator pipelineOrchestrator,
    AgentEventPublisher agentEventPublisher  // NEW
) {
    this.agentOrchestrator = agentOrchestrator;
    this.itineraryJsonService = itineraryJsonService;
    this.userDataService = userDataService;
    this.pipelineOrchestrator = pipelineOrchestrator;
    this.agentEventPublisher = agentEventPublisher;  // NEW
}
```

**Modified create() Method:**
```java
public ItineraryDto create(CreateItineraryReq request, String userId) {
    // ... existing code ...
    
    // Generate unique execution ID for tracking
    String executionId = "exec_" + System.currentTimeMillis();
    logger.info("Generated executionId: {} for itinerary: {}", executionId, itineraryId);
    
    // Choose generation strategy based on configuration
    if ("pipeline".equalsIgnoreCase(generationMode) && pipelineOrchestrator != null) {
        logger.info("Using PIPELINE mode for generation");
        
        // Capture the CompletableFuture instead of ignoring it
        CompletableFuture<NormalizedItinerary> future = 
            pipelineOrchestrator.generateItinerary(itineraryId, request, userId);
        
        // Attach completion callback for error handling
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                // Log the error
                logger.error("Pipeline generation failed for itinerary: {}, executionId: {}", 
                    itineraryId, executionId, throwable);
                
                // Publish error event to SSE subscribers
                try {
                    agentEventPublisher.publishErrorFromException(
                        itineraryId,
                        executionId,
                        (Exception) throwable,
                        "itinerary generation",
                        ErrorEvent.ErrorSeverity.ERROR
                    );
                } catch (Exception e) {
                    logger.error("Failed to publish error event for itinerary: {}", itineraryId, e);
                }
            } else {
                logger.info("Pipeline generation completed successfully for itinerary: {}, executionId: {}", 
                    itineraryId, executionId);
                // Completion event is already published by PipelineOrchestrator
            }
        });
        
    } else if (agentOrchestrator != null) {
        logger.info("Using MONOLITHIC mode for generation");
        
        // Same pattern for monolithic mode
        CompletableFuture<NormalizedItinerary> future = 
            agentOrchestrator.generateNormalizedItinerary(itineraryId, request, userId);
        
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Monolithic generation failed for itinerary: {}, executionId: {}", 
                    itineraryId, executionId, throwable);
                
                try {
                    agentEventPublisher.publishErrorFromException(
                        itineraryId,
                        executionId,
                        (Exception) throwable,
                        "itinerary generation",
                        ErrorEvent.ErrorSeverity.ERROR
                    );
                } catch (Exception e) {
                    logger.error("Failed to publish error event for itinerary: {}", itineraryId, e);
                }
            } else {
                logger.info("Monolithic generation completed successfully for itinerary: {}, executionId: {}", 
                    itineraryId, executionId);
            }
        });
        
    } else {
        // Neither orchestrator is available - this is a configuration error
        String errorMsg = "No orchestrator available for generation mode: " + generationMode;
        logger.error(errorMsg);
        throw new IllegalStateException(errorMsg);
    }
    
    // ... rest of existing code ...
}
```

---

### 2. AgentEventPublisher (No Changes)

**File:** `src/main/java/com/tripplanner/service/AgentEventPublisher.java`

**Used Methods:**
```java
public void publishErrorFromException(
    String itineraryId,
    String executionId,
    Exception exception,
    String context,
    ErrorEvent.ErrorSeverity severity
)
```

**Behavior:**
1. Creates `ErrorEvent` from exception
2. Sets error message with context
3. Determines if error is retryable
4. Sets recovery action
5. Broadcasts to all SSE subscribers for the itinerary

**No changes needed** - this component is already fully implemented.

---

### 3. PipelineOrchestrator (No Changes)

**File:** `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`

**Current Behavior:**
- Publishes progress events during generation
- Publishes completion event on success
- Throws exception on failure (now caught by callback)

**No changes needed** - this component already works correctly.

---

### 4. ErrorEvent (No Changes)

**File:** `src/main/java/com/tripplanner/dto/ErrorEvent.java`

**Used Enum:**
```java
public enum ErrorSeverity {
    WARNING,
    ERROR,
    CRITICAL
}
```

**No changes needed** - this DTO is already defined.

---

## Data Models

### ExecutionId Format

```
Pattern: "exec_{timestamp}"
Example: "exec_1729234567890"

Components:
- Prefix: "exec_" (constant)
- Timestamp: System.currentTimeMillis() (13 digits)

Properties:
- Unique per generation attempt
- Sortable chronologically
- Easy to parse and filter in logs
```

### Error Event Structure

```json
{
  "eventType": "error",
  "itineraryId": "it_abc123",
  "executionId": "exec_1729234567890",
  "errorCode": "GENERATION_FAILED",
  "message": "Error in itinerary generation: Timeout waiting for AI response",
  "severity": "ERROR",
  "canRetry": true,
  "recoveryAction": "Retrying operation automatically",
  "timestamp": "2025-10-18T10:30:45.123Z",
  "stackTrace": "java.util.concurrent.TimeoutException: ...",
  "context": "itinerary generation"
}
```

---

## Error Handling

### Error Flow

```
1. Async operation fails
   ↓
2. Exception thrown in CompletableFuture
   ↓
3. .whenComplete() callback invoked with throwable
   ↓
4. Check if throwable != null
   ↓
5. Log error with full stack trace
   ↓
6. Try to publish error event
   ├─> Success: Event sent to SSE subscribers
   └─> Failure: Log secondary error (don't throw)
   ↓
7. Callback completes (no exception propagated)
```

### Error Categories

**Retryable Errors:**
- Network timeouts
- Connection failures
- Rate limit errors
- Service unavailable

**Non-Retryable Errors:**
- Validation errors
- Parsing errors
- Invalid configuration
- Authentication failures

**Determination:**
- Handled by `AgentEventPublisher.isRetryableException()`
- Based on exception type and message content

---

## Testing Strategy

### Unit Tests

**Test File:** `src/test/java/com/tripplanner/service/ItineraryServiceTest.java`

**Test Cases:**

1. **testCreateItinerary_PipelineMode_Success**
   - Given: Valid request, pipeline mode enabled
   - When: create() is called
   - Then: CompletableFuture is captured, callback attached, DTO returned

2. **testCreateItinerary_PipelineMode_Failure**
   - Given: Valid request, pipeline mode enabled, orchestrator throws exception
   - When: create() is called
   - Then: Error event is published via AgentEventPublisher

3. **testCreateItinerary_MonolithicMode_Success**
   - Given: Valid request, monolithic mode enabled
   - When: create() is called
   - Then: CompletableFuture is captured, callback attached, DTO returned

4. **testCreateItinerary_MonolithicMode_Failure**
   - Given: Valid request, monolithic mode enabled, orchestrator throws exception
   - When: create() is called
   - Then: Error event is published via AgentEventPublisher

5. **testCreateItinerary_NoOrchestrator**
   - Given: Valid request, no orchestrator available
   - When: create() is called
   - Then: IllegalStateException is thrown

6. **testExecutionIdGeneration**
   - Given: Multiple concurrent requests
   - When: create() is called multiple times
   - Then: Each request gets unique executionId

7. **testErrorEventPublishing_CallbackException**
   - Given: AgentEventPublisher throws exception
   - When: Error event publishing fails
   - Then: Secondary error is logged, no exception propagated

### Integration Tests

**Test File:** `src/test/java/com/tripplanner/integration/ItineraryCreationIntegrationTest.java`

**Test Cases:**

1. **testEndToEnd_SuccessfulGeneration**
   - Create itinerary via API
   - Establish SSE connection
   - Verify progress events received
   - Verify completion event received

2. **testEndToEnd_FailedGeneration**
   - Create itinerary via API
   - Establish SSE connection
   - Simulate generation failure
   - Verify error event received

3. **testEndToEnd_ConcurrentGenerations**
   - Create multiple itineraries concurrently
   - Verify each has unique executionId
   - Verify events are properly isolated

### Manual Testing

**Test Scenarios:**

1. **Happy Path**
   - Create itinerary via UI
   - Observe progress bar updates
   - Verify completion message

2. **Network Failure**
   - Create itinerary
   - Disconnect network during generation
   - Verify error message in UI

3. **Timeout**
   - Create complex itinerary (long generation time)
   - Verify timeout error if exceeded
   - Verify error message in UI

4. **Concurrent Users**
   - Multiple users create itineraries simultaneously
   - Verify no cross-contamination of events
   - Verify each user sees their own progress

---

## Performance Considerations

### Response Time

**Target:** < 3 seconds for API response

**Measurements:**
- Initial itinerary creation: ~500ms (sync)
- SSE connection delay: 2000ms (intentional)
- Async callback attachment: ~1ms (negligible)
- **Total:** ~2.5 seconds ✅

### Memory Impact

**Additional Memory:**
- ExecutionId string: ~30 bytes
- Callback lambda: ~100 bytes
- **Total per request:** ~130 bytes (negligible)

### Thread Usage

**Thread Pools:**
- Request thread: Returns immediately (non-blocking)
- Async executor: Handles generation (existing)
- Callback thread: Same as async executor (no new threads)

**No additional threads created** ✅

---

## Security Considerations

### Error Information Disclosure

**Risk:** Error messages might expose sensitive information

**Mitigation:**
- Error events use generic messages
- Stack traces only in server logs (not sent to client)
- Exception details sanitized by `AgentEventPublisher`

### Authentication

**Current:** SSE connections require valid Firebase token

**No changes needed** - existing authentication remains in place

---

## Deployment Strategy

### Rollout Plan

1. **Deploy to Development**
   - Test with synthetic data
   - Verify logs and events

2. **Deploy to Staging**
   - Test with production-like data
   - Monitor for 24 hours

3. **Deploy to Production**
   - Gradual rollout (10% → 50% → 100%)
   - Monitor error rates
   - Rollback plan ready

### Rollback Plan

**If issues detected:**
1. Revert to previous version
2. Investigate root cause
3. Fix and redeploy

**Rollback is safe** - no database schema changes

---

## Monitoring and Observability

### Metrics to Track

1. **Error Event Publishing Rate**
   - Metric: `itinerary.generation.errors.published`
   - Alert if > 5% of generations

2. **Callback Execution Time**
   - Metric: `itinerary.generation.callback.duration`
   - Alert if > 100ms

3. **Failed Event Publishing**
   - Metric: `itinerary.generation.event.publish.failures`
   - Alert if > 0

### Log Patterns

**Success:**
```
INFO  - Generated executionId: exec_1729234567890 for itinerary: it_abc123
INFO  - Using PIPELINE mode for generation
INFO  - Pipeline generation completed successfully for itinerary: it_abc123, executionId: exec_1729234567890
```

**Failure:**
```
INFO  - Generated executionId: exec_1729234567890 for itinerary: it_abc123
INFO  - Using PIPELINE mode for generation
ERROR - Pipeline generation failed for itinerary: it_abc123, executionId: exec_1729234567890
        java.util.concurrent.TimeoutException: Skeleton generation timed out
        at com.tripplanner.service.PipelineOrchestrator.executeSkeletonPhase(...)
```

---

## Dependencies

### Existing Dependencies (No Changes)

- Spring Boot 3.x
- Spring Async (@Async)
- SLF4J Logging
- Firebase Admin SDK
- CompletableFuture (Java 8+)

### New Dependencies

**None** - all required components already exist

---

## Backward Compatibility

### API Compatibility

✅ **No breaking changes**
- Request format unchanged
- Response format unchanged
- HTTP status codes unchanged
- Error responses unchanged

### Behavior Changes

✅ **Improvements only**
- Error events now published (previously missing)
- Better logging (previously incomplete)
- Proper async handling (previously fire-and-forget)

### Configuration Changes

✅ **No configuration changes required**
- Existing properties work as-is
- No new properties needed
- No deprecated properties

---

## Future Enhancements

### Potential Improvements

1. **Retry Logic**
   - Automatically retry failed generations
   - Exponential backoff
   - Max retry limit

2. **Progress Percentage**
   - Calculate overall progress across all phases
   - Send progress percentage to UI
   - Update progress bar in real-time

3. **Cancellation Support**
   - Allow users to cancel in-progress generations
   - Clean up resources on cancellation
   - Send cancellation event to UI

4. **Generation Metrics**
   - Track generation duration by destination
   - Track success/failure rates
   - Identify slow phases for optimization

---

## References

- **Requirements**: `.kiro/specs/fix-progress-tracking/requirements.md`
- **Verification Report**: `analysis/IMPLEMENTATION_VERIFICATION.md`
- **Source File**: `src/main/java/com/tripplanner/service/ItineraryService.java`
- **Event Publisher**: `src/main/java/com/tripplanner/service/AgentEventPublisher.java`
- **Pipeline Orchestrator**: `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`
- **CompletableFuture Docs**: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
