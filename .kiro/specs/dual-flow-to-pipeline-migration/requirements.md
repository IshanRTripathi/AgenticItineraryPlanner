# Requirements Document

## Introduction

The system currently maintains two separate itinerary generation flows: a monolithic flow using `AgentOrchestrator` + `PlannerAgent`, and a pipeline flow using `PipelineOrchestrator` + specialized agents. This dual architecture creates maintenance complexity, configuration confusion, testing overhead, and potential for divergent behavior. The pipeline flow has proven superior in performance (50% faster), reliability (80% fewer timeouts), and user experience (progressive loading with real-time updates).

This migration will consolidate to a single pipeline-based flow while preserving the critical `createInitialItinerary()` functionality that both flows depend on for establishing itinerary ownership.

## Requirements

### Requirement 1: Extract Initialization Logic

**User Story:** As a developer, I want the initial itinerary creation logic extracted into a dedicated service, so that it can be reused by any orchestrator without duplication.

#### Acceptance Criteria

1. WHEN a new `ItineraryInitializationService` is created THEN it SHALL contain the `createInitialItinerary()` method extracted from `AgentOrchestrator`
2. WHEN `createInitialItinerary()` is called THEN it SHALL create the initial itinerary structure, save it to Firestore, and establish user ownership synchronously
3. WHEN the service is instantiated THEN it SHALL depend only on `ItineraryJsonService` and `UserDataService`
4. WHEN an error occurs during initialization THEN it SHALL throw a `RuntimeException` with a descriptive message
5. IF the initial itinerary creation fails THEN the error SHALL be logged with full context including itinerary ID and user ID

### Requirement 2: Remove Monolithic Flow Components

**User Story:** As a system architect, I want the monolithic flow components removed, so that the codebase is simpler and maintenance burden is reduced.

#### Acceptance Criteria

1. WHEN the migration is complete THEN `AgentOrchestrator.java` SHALL be deleted
2. WHEN the migration is complete THEN `PlannerAgent.java` SHALL be deleted
3. WHEN the migration is complete THEN `ResilientAgentOrchestrator.java` SHALL be deleted
4. WHEN files are deleted THEN no other production code SHALL reference these deleted classes
5. IF any references remain THEN they SHALL be updated to use the new `ItineraryInitializationService` or `PipelineOrchestrator`

### Requirement 3: Update ItineraryService

**User Story:** As a developer, I want `ItineraryService` to use only the pipeline flow, so that there is a single, consistent generation path.

#### Acceptance Criteria

1. WHEN `ItineraryService` is updated THEN it SHALL remove the `generationMode` field and all mode-switching logic
2. WHEN `ItineraryService` is instantiated THEN it SHALL depend on `ItineraryInitializationService` instead of `AgentOrchestrator`
3. WHEN `ItineraryService` is instantiated THEN `PipelineOrchestrator` SHALL be a required dependency (not optional)
4. WHEN `create()` is called THEN it SHALL always use `PipelineOrchestrator.generateItinerary()`
5. WHEN initial itinerary is created THEN it SHALL use `ItineraryInitializationService.createInitialItinerary()`
6. IF pipeline generation fails THEN an error event SHALL be published via `AgentEventPublisher`
7. WHEN generation completes successfully THEN appropriate logging SHALL confirm completion

### Requirement 4: Update Configuration

**User Story:** As a DevOps engineer, I want the configuration simplified to remove the generation mode setting, so that deployment configuration is clearer.

#### Acceptance Criteria

1. WHEN `application.yml` is updated THEN the `itinerary.generation.mode` property SHALL be removed
2. WHEN the property is removed THEN a comment SHALL document that pipeline is now the only mode
3. WHEN the migration is deployed THEN no environment variables SHALL reference `ITINERARY_GENERATION_MODE`
4. IF configuration files exist in other environments THEN they SHALL also be updated to remove the mode setting

### Requirement 5: Maintain System Functionality

**User Story:** As a user, I want itinerary generation to continue working exactly as before, so that my experience is not disrupted.

#### Acceptance Criteria

1. WHEN a user creates an itinerary THEN the API SHALL return a 200 OK response with status="generating"
2. WHEN initial itinerary is created THEN ownership SHALL be established before the API response is sent
3. WHEN async generation starts THEN progress events SHALL be published via SSE
4. WHEN generation completes THEN a completion event SHALL be published
5. WHEN generation fails THEN an error event SHALL be published with appropriate severity
6. WHEN a user retrieves their itinerary THEN it SHALL contain all expected data (days, nodes, costs, etc.)
7. IF the user is not the owner THEN the API SHALL return a 404 Not Found response

### Requirement 6: Update Tests

**User Story:** As a QA engineer, I want all tests updated to reflect the new architecture, so that test coverage remains comprehensive.

#### Acceptance Criteria

1. WHEN tests are updated THEN all references to `AgentOrchestrator` SHALL be replaced with `ItineraryInitializationService`
2. WHEN tests are updated THEN all monolithic flow test cases SHALL be removed
3. WHEN tests are updated THEN all assertions SHALL expect pipeline flow behavior only
4. WHEN unit tests run THEN they SHALL all pass without errors
5. WHEN integration tests run THEN they SHALL verify end-to-end itinerary generation works correctly
6. IF any test fails THEN the failure SHALL be investigated and resolved before deployment

### Requirement 7: Ensure Safe Deployment

**User Story:** As a DevOps engineer, I want a rollback plan and monitoring strategy, so that we can quickly recover if issues arise in production.

#### Acceptance Criteria

1. WHEN the migration is deployed THEN a backup branch SHALL exist with the pre-migration code
2. WHEN deployment occurs THEN it SHALL follow a gradual rollout strategy (dev → staging → production)
3. WHEN in production THEN key metrics SHALL be monitored (generation time, timeout rate, error rate)
4. IF critical issues are detected THEN a rollback procedure SHALL be available and documented
5. WHEN monitoring alerts trigger THEN the on-call team SHALL be notified immediately
6. IF a rollback is needed THEN it SHALL be executable within 5 minutes

### Requirement 8: Maintain Code Quality

**User Story:** As a developer, I want the migrated code to follow best practices, so that it remains maintainable and extensible.

#### Acceptance Criteria

1. WHEN new code is written THEN it SHALL follow existing code style and conventions
2. WHEN services are created THEN they SHALL have single, well-defined responsibilities
3. WHEN methods are written THEN they SHALL include appropriate logging at INFO and ERROR levels
4. WHEN exceptions occur THEN they SHALL be caught, logged, and wrapped with descriptive messages
5. WHEN dependencies are injected THEN they SHALL use constructor injection
6. IF code complexity increases THEN it SHALL be refactored to maintain readability
