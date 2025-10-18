# Requirements Document: Fix UI Progress Tracking

## Introduction

This specification addresses the critical UX issue where users cannot see real-time progress updates during itinerary creation. The root cause is a fire-and-forget async pattern in `ItineraryService.java` that ignores the `CompletableFuture` return value from async generation methods, preventing proper error handling and completion tracking.

**Current State:**
- Frontend SSE Manager is fully implemented and ready
- Backend event publishing infrastructure is complete
- PipelineOrchestrator correctly publishes progress events
- **Problem:** ItineraryService doesn't connect async results to event publishing

**Desired State:**
- Users see real-time progress updates during itinerary generation
- Errors are properly reported to the UI via SSE
- Completion events trigger UI updates
- No silent failures in async operations

---

## Requirements

### Requirement 1: Proper Async Handling in ItineraryService

**User Story:** As a developer, I want the ItineraryService to properly handle CompletableFuture results, so that async operations can be tracked and errors can be handled gracefully.

#### Acceptance Criteria

1. WHEN `pipelineOrchestrator.generateItinerary()` is called THEN the returned `CompletableFuture<NormalizedItinerary>` SHALL be captured in a variable
2. WHEN the CompletableFuture completes successfully THEN a completion log SHALL be written
3. WHEN the CompletableFuture completes with an error THEN an error SHALL be logged with full stack trace
4. WHEN the CompletableFuture completes with an error THEN an error event SHALL be published to SSE subscribers via `AgentEventPublisher`
5. WHEN `agentOrchestrator.generateNormalizedItinerary()` is called THEN the same async handling pattern SHALL be applied
6. IF the generation mode is "pipeline" AND pipelineOrchestrator is null THEN an error SHALL be logged and an exception SHALL be thrown

---

### Requirement 2: Error Event Publishing

**User Story:** As a user, I want to see error messages in the UI when itinerary generation fails, so that I understand what went wrong and can take appropriate action.

#### Acceptance Criteria

1. WHEN an async generation fails THEN `AgentEventPublisher.publishErrorFromException()` SHALL be called
2. WHEN publishing an error event THEN the itineraryId SHALL be included
3. WHEN publishing an error event THEN a unique executionId SHALL be generated
4. WHEN publishing an error event THEN the exception SHALL be passed with full context
5. WHEN publishing an error event THEN the context SHALL be "itinerary generation"
6. WHEN publishing an error event THEN the severity SHALL be `ErrorEvent.ErrorSeverity.ERROR`
7. WHEN an error event is published THEN it SHALL be broadcast to all SSE subscribers for that itinerary

---

### Requirement 3: Dependency Injection for AgentEventPublisher

**User Story:** As a developer, I want AgentEventPublisher to be available in ItineraryService, so that I can publish error events when async operations fail.

#### Acceptance Criteria

1. WHEN ItineraryService is instantiated THEN `AgentEventPublisher` SHALL be injected via constructor
2. WHEN ItineraryService is instantiated THEN the `AgentEventPublisher` instance SHALL be stored as a private final field
3. WHEN ItineraryService constructor is called THEN all existing dependencies SHALL remain unchanged
4. WHEN ItineraryService constructor is called THEN the new `AgentEventPublisher` parameter SHALL be added as the last parameter
5. IF AgentEventPublisher is null THEN Spring SHALL fail to start with a clear error message

---

### Requirement 4: Execution ID Generation

**User Story:** As a developer, I want each itinerary generation to have a unique execution ID, so that I can track specific generation attempts in logs and events.

#### Acceptance Criteria

1. WHEN an itinerary is created THEN a unique executionId SHALL be generated using the pattern "exec_{timestamp}"
2. WHEN an executionId is generated THEN it SHALL use `System.currentTimeMillis()` for the timestamp
3. WHEN an executionId is generated THEN it SHALL be logged for debugging purposes
4. WHEN an error event is published THEN the executionId SHALL be included in the event
5. WHEN multiple generations run concurrently THEN each SHALL have a unique executionId

---

### Requirement 5: Backward Compatibility

**User Story:** As a developer, I want the changes to be backward compatible, so that existing functionality is not broken.

#### Acceptance Criteria

1. WHEN the fix is applied THEN all existing API endpoints SHALL continue to work
2. WHEN the fix is applied THEN the response format SHALL remain unchanged
3. WHEN the fix is applied THEN the 2-second SSE connection delay SHALL remain in place
4. WHEN the fix is applied THEN both pipeline and monolithic modes SHALL be supported
5. WHEN the fix is applied THEN no breaking changes SHALL be introduced to the public API

---

### Requirement 6: Logging and Observability

**User Story:** As a developer, I want comprehensive logging of async operations, so that I can debug issues and monitor system health.

#### Acceptance Criteria

1. WHEN async generation starts THEN a log SHALL be written with itineraryId and mode
2. WHEN async generation completes successfully THEN a log SHALL be written with itineraryId and duration
3. WHEN async generation fails THEN an error log SHALL be written with itineraryId, error message, and stack trace
4. WHEN an error event is published THEN a log SHALL be written confirming the event was sent
5. WHEN AgentEventPublisher is injected THEN a debug log SHALL confirm successful injection
6. ALL logs SHALL use SLF4J logger with appropriate log levels (INFO for success, ERROR for failures)

---

### Requirement 7: No Blocking Behavior

**User Story:** As a user, I want itinerary creation to remain fast and non-blocking, so that the API responds quickly.

#### Acceptance Criteria

1. WHEN `ItineraryService.create()` is called THEN it SHALL return immediately with status="generating"
2. WHEN async handling is added THEN no `.get()` or `.join()` calls SHALL be used on CompletableFuture
3. WHEN async handling is added THEN only `.whenComplete()` callbacks SHALL be used
4. WHEN async handling is added THEN the API response time SHALL remain under 3 seconds
5. WHEN async handling is added THEN the async operations SHALL continue in background threads

---

### Requirement 8: Error Recovery and Resilience

**User Story:** As a user, I want the system to handle errors gracefully, so that one failed generation doesn't affect other users.

#### Acceptance Criteria

1. WHEN an async generation fails THEN other concurrent generations SHALL continue unaffected
2. WHEN an error event is published THEN the SSE connection SHALL remain open for future events
3. WHEN an error occurs THEN the itinerary document SHALL remain in Firestore with status="generating"
4. WHEN an error occurs THEN the user SHALL be able to retry the generation
5. WHEN an error occurs THEN no exceptions SHALL propagate to the API response

---

## Edge Cases and Constraints

### Edge Cases

1. **Null PipelineOrchestrator**: If pipeline mode is configured but PipelineOrchestrator is null, log error and throw exception
2. **SSE Connection Not Established**: Events are published even if no SSE connection exists (fire-and-forget for events)
3. **Multiple Concurrent Generations**: Each generation has unique executionId to avoid conflicts
4. **Exception in Callback**: Wrap callback logic in try-catch to prevent callback failures from affecting other operations

### Constraints

1. **No Breaking Changes**: Existing API contracts must be maintained
2. **Performance**: No additional latency in API response time
3. **Thread Safety**: All operations must be thread-safe for concurrent requests
4. **Spring Boot Compatibility**: Must work with existing Spring Boot configuration
5. **Logging Standards**: Must follow existing logging patterns and conventions

---

## Success Criteria

1. ✅ Users see real-time progress updates in UI during itinerary generation
2. ✅ Errors are displayed in UI with clear error messages
3. ✅ Completion events trigger UI updates to show final itinerary
4. ✅ No silent failures in async operations
5. ✅ All logs show proper async lifecycle (start, progress, completion/error)
6. ✅ API response time remains under 3 seconds
7. ✅ No breaking changes to existing functionality
8. ✅ Code passes all existing tests
9. ✅ New error handling is covered by logs and monitoring

---

## Non-Functional Requirements

### Performance
- API response time: < 3 seconds
- No blocking operations in request thread
- Async operations continue in background

### Reliability
- Error events published 100% of the time on failure
- No silent failures
- Graceful degradation if SSE connection fails

### Maintainability
- Clear separation of concerns
- Comprehensive logging
- Self-documenting code with comments

### Observability
- All async operations logged
- Error events include full context
- Execution IDs for tracing

---

## References

- **Verification Report**: `analysis/IMPLEMENTATION_VERIFICATION.md`
- **Roadmap**: `analysis/BACKEND_ANALYSIS_ROADMAP.md` (Item #1)
- **Source File**: `src/main/java/com/tripplanner/service/ItineraryService.java` (Lines 105-108)
- **Event Publisher**: `src/main/java/com/tripplanner/service/AgentEventPublisher.java`
- **Pipeline Orchestrator**: `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`
