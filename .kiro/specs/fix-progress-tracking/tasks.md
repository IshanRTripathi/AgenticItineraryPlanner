# Implementation Plan: Fix UI Progress Tracking

## Overview

This implementation plan converts the design into actionable coding tasks with specific file locations, line numbers, and code snippets. Each task is designed to be executed independently with clear acceptance criteria.

---

## Task List

- [x] 1. Add AgentEventPublisher dependency to ItineraryService


  - Add private final field for AgentEventPublisher
  - Update constructor to inject AgentEventPublisher
  - Add constructor parameter documentation
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 2. Generate unique execution ID for tracking


  - Generate executionId using timestamp pattern
  - Log executionId for debugging
  - Store executionId for use in callbacks
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 3. Fix pipeline mode async handling


  - [x] 3.1 Capture CompletableFuture return value


    - Replace fire-and-forget call with variable assignment
    - Store future in local variable
    - _Requirements: 1.1_
  
  - [x] 3.2 Attach success/error callback

    - Add .whenComplete() callback to CompletableFuture
    - Handle success case with logging
    - Handle error case with logging and event publishing
    - _Requirements: 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
  
  - [x] 3.3 Add error event publishing

    - Call AgentEventPublisher.publishErrorFromException()
    - Pass itineraryId, executionId, exception, context, severity
    - Wrap in try-catch to prevent callback failures
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
  
  - [x] 3.4 Add comprehensive logging

    - Log successful completion with itineraryId and executionId
    - Log errors with full stack trace
    - Log error event publishing attempts
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 4. Fix monolithic mode async handling

  - [x] 4.1 Capture CompletableFuture return value

    - Replace fire-and-forget call with variable assignment
    - Store future in local variable
    - _Requirements: 1.5_
  
  - [x] 4.2 Attach success/error callback

    - Add .whenComplete() callback to CompletableFuture
    - Handle success case with logging
    - Handle error case with logging and event publishing
    - _Requirements: 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
  
  - [x] 4.3 Add error event publishing

    - Call AgentEventPublisher.publishErrorFromException()
    - Pass itineraryId, executionId, exception, context, severity
    - Wrap in try-catch to prevent callback failures
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
  
  - [x] 4.4 Add comprehensive logging

    - Log successful completion with itineraryId and executionId
    - Log errors with full stack trace
    - Log error event publishing attempts
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 5. Add null orchestrator error handling

  - Add else clause for when no orchestrator is available
  - Log error message with generation mode
  - Throw IllegalStateException with clear message
  - _Requirements: 1.6_

- [x] 6. Verify backward compatibility


  - Verify API response format unchanged
  - Verify 2-second SSE delay still in place
  - Verify both pipeline and monolithic modes work
  - Verify no breaking changes to public API
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 7. Add import statements

  - Import ErrorEvent class
  - Import ErrorEvent.ErrorSeverity enum
  - Verify all imports are present
  - _Requirements: 2.6_

---

## Detailed Task Breakdown

### Task 1: Add AgentEventPublisher Dependency

**File:** `src/main/java/com/tripplanner/service/ItineraryService.java`

**Location:** Class fields section (after line 23)

**Current Code (Line 23):**
```java
private final PipelineOrchestrator pipelineOrchestrator;
```

**Add After Line 23:**
```java
private final AgentEventPublisher agentEventPublisher;
```

**Location:** Constructor (lines 28-35)

**Current Code:**
```java
public ItineraryService(AgentOrchestrator agentOrchestrator,
                        ItineraryJsonService itineraryJsonService,
                        UserDataService userDataService,
                        @Autowired(required = false) PipelineOrchestrator pipelineOrchestrator) {
    this.agentOrchestrator = agentOrchestrator;
    this.itineraryJsonService = itineraryJsonService;
    this.userDataService = userDataService;
    this.pipelineOrchestrator = pipelineOrchestrator;
}
```

**Replace With:**
```java
public ItineraryService(AgentOrchestrator agentOrchestrator,
                        ItineraryJsonService itineraryJsonService,
                        UserDataService userDataService,
                        @Autowired(required = false) PipelineOrchestrator pipelineOrchestrator,
                        AgentEventPublisher agentEventPublisher) {
    this.agentOrchestrator = agentOrchestrator;
    this.itineraryJsonService = itineraryJsonService;
    this.userDataService = userDataService;
    this.pipelineOrchestrator = pipelineOrchestrator;
    this.agentEventPublisher = agentEventPublisher;
}
```

**Acceptance Criteria:**
- ✅ Private final field added for AgentEventPublisher
- ✅ Constructor parameter added as last parameter
- ✅ Field initialized in constructor
- ✅ No compilation errors

---

### Task 2: Generate Unique Execution ID

**File:** `src/main/java/com/tripplanner/service/ItineraryService.java`

**Location:** In `create()` method, after line 78 (after the 2-second sleep)

**Current Code (Line 78):**
```java
logger.info("Starting async itinerary generation for user: {} with ID: {}", userId, itineraryId);
logger.info("Generation mode: {}", generationMode);
```

**Add After Line 79:**
```java
// Generate unique execution ID for tracking this generation attempt
String executionId = "exec_" + System.currentTimeMillis();
logger.info("Generated executionId: {} for itinerary: {}", executionId, itineraryId);
```

**Acceptance Criteria:**
- ✅ ExecutionId generated using timestamp pattern
- ✅ ExecutionId logged for debugging
- ✅ ExecutionId available for use in callbacks
- ✅ Format matches "exec_{timestamp}"

---

### Task 3: Fix Pipeline Mode Async Handling

**File:** `src/main/java/com/tripplanner/service/ItineraryService.java`

**Location:** Lines 81-84 (pipeline mode block)

**Current Code:**
```java
// Choose generation strategy based on configuration
if ("pipeline".equalsIgnoreCase(generationMode) && pipelineOrchestrator != null) {
    logger.info("Using PIPELINE mode for generation");
    pipelineOrchestrator.generateItinerary(itineraryId, request, userId);
} else {
    logger.info("Using MONOLITHIC mode for generation");
    agentOrchestrator.generateNormalizedItinerary(itineraryId, request, userId);
}
```

**Replace With:**
```java
// Choose generation strategy based on configuration
if ("pipeline".equalsIgnoreCase(generationMode) && pipelineOrchestrator != null) {
    logger.info("Using PIPELINE mode for generation");
    
    // Capture the CompletableFuture instead of ignoring it (fire-and-forget)
    CompletableFuture<NormalizedItinerary> future = 
        pipelineOrchestrator.generateItinerary(itineraryId, request, userId);
    
    // Attach completion callback for error handling and tracking
    future.whenComplete((result, throwable) -> {
        if (throwable != null) {
            // Log the error with full context
            logger.error("Pipeline generation failed for itinerary: {}, executionId: {}", 
                itineraryId, executionId, throwable);
            
            // Publish error event to SSE subscribers so UI can show error message
            try {
                agentEventPublisher.publishErrorFromException(
                    itineraryId,
                    executionId,
                    (Exception) throwable,
                    "itinerary generation",
                    ErrorEvent.ErrorSeverity.ERROR
                );
                logger.info("Error event published for itinerary: {}, executionId: {}", 
                    itineraryId, executionId);
            } catch (Exception e) {
                // Don't let event publishing failures affect the main flow
                logger.error("Failed to publish error event for itinerary: {}, executionId: {}", 
                    itineraryId, executionId, e);
            }
        } else {
            // Log successful completion
            logger.info("Pipeline generation completed successfully for itinerary: {}, executionId: {}", 
                itineraryId, executionId);
            // Note: Completion event is already published by PipelineOrchestrator.publishPipelineComplete()
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
                logger.info("Error event published for itinerary: {}, executionId: {}", 
                    itineraryId, executionId);
            } catch (Exception e) {
                logger.error("Failed to publish error event for itinerary: {}, executionId: {}", 
                    itineraryId, executionId, e);
            }
        } else {
            logger.info("Monolithic generation completed successfully for itinerary: {}, executionId: {}", 
                itineraryId, executionId);
            // Note: AgentOrchestrator doesn't publish completion events (legacy mode)
        }
    });
    
} else {
    // Neither orchestrator is available - this is a configuration error
    String errorMsg = "No orchestrator available for generation mode: " + generationMode;
    logger.error(errorMsg);
    throw new IllegalStateException(errorMsg);
}
```

**Acceptance Criteria:**
- ✅ CompletableFuture captured in variable (not fire-and-forget)
- ✅ .whenComplete() callback attached
- ✅ Success case logs completion
- ✅ Error case logs error with stack trace
- ✅ Error case publishes error event via AgentEventPublisher
- ✅ Event publishing wrapped in try-catch
- ✅ Both pipeline and monolithic modes handled
- ✅ Null orchestrator case throws IllegalStateException
- ✅ All logging includes itineraryId and executionId

---

### Task 4: Add Import Statements

**File:** `src/main/java/com/tripplanner/service/ItineraryService.java`

**Location:** Import section (after existing imports, around line 10)

**Add These Imports:**
```java
import com.tripplanner.dto.ErrorEvent;
import java.util.concurrent.CompletableFuture;
```

**Note:** Check if `CompletableFuture` is already imported. If not, add it.

**Acceptance Criteria:**
- ✅ ErrorEvent imported
- ✅ CompletableFuture imported (if not already present)
- ✅ No unused imports
- ✅ Imports organized alphabetically

---

### Task 5: Verify Backward Compatibility

**File:** `src/main/java/com/tripplanner/service/ItineraryService.java`

**Verification Checklist:**

1. **API Response Format** (Line 86-96)
   ```java
   ItineraryDto result = ItineraryDto.builder()
           .id(itineraryId)
           .destination(request.getDestination())
           .startDate(request.getStartDate())
           .endDate(request.getEndDate())
           .party(request.getParty())
           .budgetTier(request.getBudgetTier())
           .interests(request.getInterests())
           .constraints(request.getConstraints())
           .language(request.getLanguage())
           .summary(initialItinerary.getSummary())
           .status("generating")
           .build();
   ```
   - ✅ Verify this code is unchanged
   - ✅ Verify status is still "generating"

2. **SSE Connection Delay** (Lines 73-78)
   ```java
   logger.info("Waiting 2 seconds for frontend SSE connection to establish...");
   try {
       Thread.sleep(2000); // 2 second delay
   } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
       logger.warn("Sleep interrupted while waiting for SSE connection");
   }
   ```
   - ✅ Verify this code is unchanged
   - ✅ Verify 2-second delay still in place

3. **Method Signature** (Line 38)
   ```java
   public ItineraryDto create(CreateItineraryReq request, String userId)
   ```
   - ✅ Verify method signature unchanged
   - ✅ Verify return type is still ItineraryDto

4. **Non-Blocking Behavior**
   - ✅ Verify no `.get()` or `.join()` calls on CompletableFuture
   - ✅ Verify only `.whenComplete()` is used
   - ✅ Verify method returns immediately

**Acceptance Criteria:**
- ✅ API response format unchanged
- ✅ 2-second SSE delay still in place
- ✅ Method signature unchanged
- ✅ Non-blocking behavior maintained
- ✅ No breaking changes introduced

---

### Task 6: Update Logging Statements

**File:** `src/main/java/com/tripplanner/service/ItineraryService.java`

**Location:** Lines 97-104 (final logging section)

**Current Code:**
```java
logger.info("=== CREATE ITINERARY RESPONSE ===");
logger.info("User ID: {}", userId);
logger.info("Itinerary ID: {}", result.getId());
logger.info("Status: {}", result.getStatus());
logger.info("Mode: {}", generationMode);
logger.info("Ownership established: YES");
logger.info("Async generation started: YES");
logger.info("User can now access /itineraries/{}/json", itineraryId);
logger.info("=====================================");
```

**Add After Line 104 (before the closing brace):**
```java
logger.info("ExecutionId: {}", executionId);
logger.info("Async callbacks attached: YES");
logger.info("Error handling enabled: YES");
```

**Acceptance Criteria:**
- ✅ ExecutionId logged in response section
- ✅ Callback attachment confirmed in logs
- ✅ Error handling status logged

---

### Task 7: Add JavaDoc Comments

**File:** `src/main/java/com/tripplanner/service/ItineraryService.java`

**Location:** Above `create()` method (line 37)

**Current Code:**
```java
/**
 * Create a new itinerary for a specific user.
 */
public ItineraryDto create(CreateItineraryReq request, String userId) {
```

**Replace With:**
```java
/**
 * Create a new itinerary for a specific user.
 * 
 * <p>This method creates an initial itinerary synchronously to establish ownership,
 * then starts async generation in the background. Progress events are published
 * via SSE to connected clients.</p>
 * 
 * <p>The method returns immediately with status="generating". Clients should
 * establish an SSE connection to receive real-time progress updates.</p>
 * 
 * <p>Error handling: If async generation fails, an error event is published
 * to SSE subscribers. The error is logged but does not affect the API response.</p>
 * 
 * @param request The itinerary creation request with destination, dates, preferences
 * @param userId The authenticated user ID from Firebase token
 * @return ItineraryDto with status="generating" and initial metadata
 * @throws RuntimeException if initial itinerary creation fails
 * @throws IllegalStateException if no orchestrator is available for the configured mode
 */
public ItineraryDto create(CreateItineraryReq request, String userId) {
```

**Acceptance Criteria:**
- ✅ JavaDoc describes async behavior
- ✅ JavaDoc describes error handling
- ✅ JavaDoc describes SSE connection requirement
- ✅ Parameters documented
- ✅ Return value documented
- ✅ Exceptions documented

---

## Testing Tasks

### Task 8: Manual Testing

**Test Case 1: Successful Generation**
1. Start backend server
2. Create itinerary via API: `POST /api/v1/itineraries`
3. Establish SSE connection: `GET /api/v1/agents/events/{itineraryId}`
4. Verify progress events received
5. Verify completion event received
6. Check logs for executionId

**Expected Logs:**
```
INFO  - Generated executionId: exec_1729234567890 for itinerary: it_abc123
INFO  - Using PIPELINE mode for generation
INFO  - Pipeline generation completed successfully for itinerary: it_abc123, executionId: exec_1729234567890
```

**Test Case 2: Failed Generation (Simulate Timeout)**
1. Modify PipelineOrchestrator to throw TimeoutException
2. Create itinerary via API
3. Establish SSE connection
4. Verify error event received
5. Check logs for error details

**Expected Logs:**
```
INFO  - Generated executionId: exec_1729234567890 for itinerary: it_abc123
INFO  - Using PIPELINE mode for generation
ERROR - Pipeline generation failed for itinerary: it_abc123, executionId: exec_1729234567890
        java.util.concurrent.TimeoutException: Skeleton generation timed out
INFO  - Error event published for itinerary: it_abc123, executionId: exec_1729234567890
```

**Test Case 3: Concurrent Generations**
1. Create 5 itineraries concurrently
2. Verify each has unique executionId
3. Verify events are properly isolated
4. Check logs for all executionIds

**Acceptance Criteria:**
- ✅ All test cases pass
- ✅ Logs show expected patterns
- ✅ SSE events received correctly
- ✅ No errors in server logs

---

### Task 9: Code Review Checklist

**Before Submitting PR:**

1. **Code Quality**
   - [ ] No compilation errors
   - [ ] No warnings
   - [ ] Code follows existing style
   - [ ] Comments are clear and helpful

2. **Functionality**
   - [ ] CompletableFuture captured (not fire-and-forget)
   - [ ] Callbacks attached correctly
   - [ ] Error events published
   - [ ] Logging comprehensive

3. **Testing**
   - [ ] Manual tests passed
   - [ ] No regressions in existing functionality
   - [ ] Error scenarios tested

4. **Documentation**
   - [ ] JavaDoc added
   - [ ] Code comments added
   - [ ] README updated (if needed)

5. **Performance**
   - [ ] No blocking operations
   - [ ] Response time < 3 seconds
   - [ ] No memory leaks

6. **Security**
   - [ ] No sensitive data in logs
   - [ ] Error messages sanitized
   - [ ] Authentication unchanged

**Acceptance Criteria:**
- ✅ All checklist items completed
- ✅ Code review approved
- ✅ Ready for merge

---

## Summary

**Total Tasks:** 9 main tasks with 4 sub-tasks
**Estimated Effort:** 1-2 hours
**Files Modified:** 1 file (`ItineraryService.java`)
**Lines Changed:** ~80 lines (mostly additions)
**Breaking Changes:** None
**New Dependencies:** None

**Key Changes:**
1. Add AgentEventPublisher dependency (1 field, 1 constructor param)
2. Generate executionId (3 lines)
3. Capture CompletableFuture (2 lines per mode)
4. Attach .whenComplete() callbacks (~30 lines per mode)
5. Add error event publishing (~10 lines per mode)
6. Add null orchestrator handling (4 lines)
7. Add imports (2 lines)
8. Update JavaDoc (15 lines)

**Risk Level:** Low
- No breaking changes
- No new dependencies
- Simple code changes
- Comprehensive error handling
- Backward compatible

**Testing Strategy:**
- Manual testing (3 test cases)
- Code review checklist
- Monitor logs in staging
- Gradual production rollout
