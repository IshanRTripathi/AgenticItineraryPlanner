# Agent Coordination and Conflict Resolution - Requirements Document

## Introduction

This specification addresses critical architectural issues in the current agent system to eliminate conflicts, ensure data consistency, and provide reliable task execution. The system will implement canonical place management, durable task processing, strict write policies, and comprehensive conflict resolution while maintaining the existing monolithic Spring Boot architecture.

**IMPORTANT: This is a completely new system with no backward compatibility requirements. All implementations should be clean, modern, and not consider legacy code or data formats.**

## Requirements

### Requirement 1: Canonical Place Registry

**User Story:** As a system architect, I want a centralized place registry so that place data is deduplicated, consistent, and efficiently managed across all itineraries.

#### Acceptance Criteria

1. WHEN a place candidate is submitted for enrichment THEN the system SHALL check for existing canonical places within a 100-meter radius
2. WHEN multiple place candidates match the same location THEN the system SHALL merge them into a single canonical place entry
3. WHEN a canonical place is created THEN it SHALL have a stable placeId that persists across itinerary updates
4. IF place name similarity exceeds 80% threshold AND coordinates match within radius THEN the system SHALL treat them as the same place
5. WHEN place data is enriched THEN canonical data SHALL be stored in the places collection, not inline in itinerary nodes
6. WHEN an itinerary node references a place THEN it SHALL store only the placeId and node-specific metadata
7. WHEN place enrichment is ambiguous THEN the system SHALL return PARTIAL status with candidate options

### Requirement 2: Durable Agent Task System

**User Story:** As a system operator, I want agent tasks to survive process restarts so that no work is lost and tasks can be reliably processed.

#### Acceptance Criteria

1. WHEN an agent needs to perform work THEN it SHALL create an AgentTask document in Firestore
2. WHEN the system restarts THEN all PENDING tasks SHALL be automatically resumed
3. WHEN a task is being processed THEN its status SHALL be updated to RUNNING with owner information
4. WHEN a task completes successfully THEN its status SHALL be updated to COMPLETED
5. WHEN a task fails THEN it SHALL be retried up to 5 times with exponential backoff
6. WHEN a task exceeds maximum retries THEN it SHALL be moved to a dead letter queue (DLQ)
7. WHEN tasks are created THEN they SHALL include idempotencyKey to prevent duplicate processing
8. WHEN the system processes tasks THEN it SHALL use Firestore snapshot listeners for near-real-time processing

### Requirement 3: Strict Write Policy Enforcement

**User Story:** As a system architect, I want all itinerary modifications to go through the ChangeEngine so that data consistency and versioning are maintained.

#### Acceptance Criteria

1. WHEN any agent needs to modify an itinerary THEN it SHALL create a ChangeSet and submit it to ChangeEngine
2. WHEN ChangeEngine receives a ChangeSet THEN it SHALL validate the baseVersion before applying changes
3. WHEN a version mismatch occurs THEN ChangeEngine SHALL return a VersionMismatchException
4. WHEN changes are applied successfully THEN ChangeEngine SHALL increment the itinerary version
5. WHEN itinerary changes are made THEN only ChangeEngine SHALL write to the itinerary documents
6. WHEN agents attempt direct database writes THEN the system SHALL prevent or log violations
7. WHEN changes are applied THEN a revision record SHALL be created for rollback capability

### Requirement 4: Agent Responsibility Boundaries

**User Story:** As a system architect, I want clear, non-overlapping agent responsibilities so that conflicts are eliminated and system behavior is predictable.

#### Acceptance Criteria

1. WHEN initial itinerary creation is needed THEN only PlannerAgent SHALL handle this task
2. WHEN user-driven modifications are requested THEN only EditorAgent SHALL process these requests
3. WHEN place enrichment is needed THEN only EnrichmentAgent SHALL perform canonical place operations
4. WHEN location discovery is required THEN only PlacesAgent SHALL handle area analysis and discovery
5. WHEN booking operations are needed THEN only BookingAgent SHALL handle reservations and payments
6. WHEN task overlap is detected THEN the system SHALL route to the highest priority agent
7. WHEN agents need to coordinate THEN they SHALL communicate through AgentOrchestrator only

### Requirement 5: Enrichment Request/Response Protocol

**User Story:** As a system architect, I want standardized enrichment communication so that place processing is reliable and traceable.

#### Acceptance Criteria

1. WHEN an agent requests enrichment THEN it SHALL use the standard EnrichmentRequest format
2. WHEN EnrichmentAgent processes a request THEN it SHALL return a standardized EnrichmentResponse
3. WHEN enrichment is requested THEN the request SHALL include traceId and idempotencyKey
4. WHEN enrichment completes THEN the response SHALL include canonicalPlaceId and confidence intensity
5. WHEN enrichment fails THEN the response SHALL include error details and retry recommendations
6. WHEN enrichment is partial THEN the response SHALL include candidate options for resolution
7. WHEN enrichment requests are duplicate THEN the system SHALL return cached results based on idempotencyKey

### Requirement 6: Concurrency and Locking Improvements

**User Story:** As a system operator, I want robust concurrency control so that simultaneous operations don't corrupt data or cause conflicts.

#### Acceptance Criteria

1. WHEN a node is being modified THEN it SHALL support lock metadata with owner and TTL information
2. WHEN optimistic locking is used THEN ChangeEngine SHALL check baseVersion on every operation
3. WHEN place enrichment occurs THEN it SHALL use Firestore transactions for atomic upserts
4. WHEN multiple agents target the same resource THEN the system SHALL use version-based conflict resolution
5. WHEN lock TTL expires THEN the system SHALL automatically release the lock
6. WHEN concurrent modifications conflict THEN the system SHALL attempt auto-merge for non-overlapping changes
7. WHEN auto-merge fails THEN the system SHALL escalate to human resolution with clear conflict description

### Requirement 7: Idempotency and Retry Mechanisms

**User Story:** As a system operator, I want reliable task processing with proper retry handling so that transient failures don't cause data loss or inconsistency.

#### Acceptance Criteria

1. WHEN any operation is performed THEN it SHALL include an idempotencyKey for duplicate detection
2. WHEN operations are retried THEN they SHALL use exponential backoff with jitter
3. WHEN maximum retries are exceeded THEN tasks SHALL be moved to DLQ with error details
4. WHEN LLM responses are truncated THEN the system SHALL attempt JSON repair and continuation
5. WHEN idempotent operations are repeated THEN they SHALL return the same result without side effects
6. WHEN tasks fail THEN they SHALL be logged with traceId for debugging
7. WHEN DLQ tasks accumulate THEN administrators SHALL be notified for manual intervention

### Requirement 8: LLM Response Reliability

**User Story:** As a system operator, I want robust handling of LLM responses so that partial or malformed outputs don't break the system.

#### Acceptance Criteria

1. WHEN LLM responses are received THEN they SHALL be validated against expected JSON schema
2. WHEN JSON parsing fails THEN the system SHALL attempt automatic repair using brace balancing
3. WHEN repair fails THEN the system SHALL request continuation from the LLM with context
4. WHEN LLM outputs are truncated THEN the system SHALL detect incomplete responses and retry
5. WHEN schema validation fails THEN the system SHALL log the issue and attempt graceful degradation
6. WHEN multiple repair attempts fail THEN the task SHALL be marked as failed and moved to DLQ
7. WHEN LLM responses include finalization tokens THEN the system SHALL use them to detect completeness

### Requirement 9: Observability and Troubleshooting

**User Story:** As a system operator, I want comprehensive observability so that I can monitor system health and troubleshoot issues effectively.

#### Acceptance Criteria

1. WHEN any operation occurs THEN it SHALL include traceId for end-to-end correlation
2. WHEN tasks are processed THEN metrics SHALL be collected for latency, success rate, and throughput
3. WHEN errors occur THEN they SHALL be logged with sufficient context for debugging
4. WHEN place merges happen THEN they SHALL be logged with before/after states
5. WHEN conflicts are resolved THEN the resolution method SHALL be recorded
6. WHEN DLQ tasks accumulate THEN alerts SHALL be generated for administrator attention
7. WHEN system performance degrades THEN metrics SHALL provide visibility into bottlenecks

### Requirement 10: Migration and Backward Compatibility

**User Story:** As a system operator, I want incremental migration so that the system remains operational during the transition to the new architecture.

#### Acceptance Criteria

1. WHEN new components are deployed THEN existing functionality SHALL continue to work
2. WHEN PlaceRegistry is introduced THEN existing itineraries SHALL gradually migrate to use placeIds
3. WHEN AgentTask system is deployed THEN existing in-memory tasks SHALL be preserved
4. WHEN ChangeEngine enforcement is enabled THEN existing write paths SHALL be gradually migrated
5. WHEN new locking mechanisms are introduced THEN they SHALL coexist with existing locks
6. WHEN migration occurs THEN data integrity SHALL be maintained throughout the process
7. WHEN rollback is needed THEN the system SHALL support reverting to previous behavior