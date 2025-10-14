# SSE Real-Time Updates & Data Integrity Fixes - Requirements

## Introduction

This document outlines the requirements for fixing critical issues in the itinerary creation flow related to Server-Sent Events (SSE), authentication, and data integrity. The system currently generates itineraries successfully but has significant issues with real-time updates and data consistency.

## Problem Statement

Based on log analysis of the itinerary creation flow, four critical issues have been identified:

1. **SSE Connection Failure**: SSE emitters are never established despite endpoints being available
2. **SSE Authentication Failure**: Auth headers missing for SSE endpoints causing 401 errors
3. **UserId Data Loss**: UserId changes from valid to null mid-execution
4. **Edge Update Failures**: 23 consecutive warnings about missing days during enrichment

## Requirements

### Requirement 1: SSE Connection Establishment

**User Story:** As a user creating an itinerary, I want to see real-time progress updates so that I know the system is working and can track generation status.

#### Acceptance Criteria

1. WHEN a user creates an itinerary THEN the frontend SHALL establish an SSE connection within 2 seconds
2. WHEN the SSE connection is established THEN the backend SHALL register the emitter in AgentEventBus
3. WHEN agent events are published THEN they SHALL be delivered to connected SSE clients
4. WHEN the SSE connection fails THEN the system SHALL log the failure reason with sufficient detail for debugging
5. IF the SSE connection cannot be established THEN the frontend SHALL retry up to 3 times with exponential backoff

### Requirement 2: SSE Authentication

**User Story:** As a system administrator, I want SSE endpoints to properly authenticate users so that only authorized users receive itinerary updates.

#### Acceptance Criteria

1. WHEN a frontend creates an SSE connection THEN it SHALL include the Firebase auth token as a query parameter
2. WHEN the backend receives an SSE connection request THEN it SHALL validate the token using FirebaseSseAuthFilter
3. IF the token is valid THEN the connection SHALL be established and userId SHALL be set in request attributes
4. IF the token is invalid or expired THEN the backend SHALL return 401 Unauthorized
5. WHEN the token expires during an active SSE connection THEN the frontend SHALL refresh the token and reconnect

### Requirement 3: UserId Persistence

**User Story:** As a system, I need to maintain consistent userId throughout the itinerary generation process so that ownership and access control work correctly.

#### Acceptance Criteria

1. WHEN an itinerary is created THEN the userId SHALL be set from the authenticated request
2. WHEN the itinerary is saved to storage THEN the userId SHALL be persisted
3. WHEN the itinerary is retrieved THEN the userId SHALL be included in the response
4. WHEN agents process the itinerary THEN they SHALL NOT modify or clear the userId
5. IF userId becomes null at any point THEN the system SHALL log an error with stack trace

### Requirement 4: Edge Update Robustness

**User Story:** As a system, I need to properly handle edge updates during enrichment so that the itinerary graph structure is complete and valid.

#### Acceptance Criteria

1. WHEN enrichment creates edge updates THEN it SHALL verify the target day exists before applying
2. IF a day is not found for an edge update THEN the system SHALL log the edge details (source, target, day number)
3. WHEN edge updates reference null days THEN the system SHALL skip the update and continue processing
4. WHEN enrichment completes THEN all valid edges SHALL be applied to the itinerary
5. IF more than 10 edge updates fail THEN the system SHALL log a warning about potential data structure issues

### Requirement 5: SSE Connection Monitoring

**User Story:** As a developer, I want comprehensive logging of SSE connection lifecycle so that I can diagnose connection issues quickly.

#### Acceptance Criteria

1. WHEN an SSE connection is requested THEN the system SHALL log the itinerary ID, user ID, and timestamp
2. WHEN an SSE emitter is registered THEN the system SHALL log the emitter count for that itinerary
3. WHEN events are published THEN the system SHALL log the event type and recipient count
4. WHEN an SSE connection closes THEN the system SHALL log the reason (timeout, error, completion)
5. WHEN no emitters are found for an itinerary THEN the system SHALL log the expected vs actual emitter state

### Requirement 6: Frontend SSE Initialization

**User Story:** As a frontend developer, I want clear initialization flow for SSE connections so that they are established reliably.

#### Acceptance Criteria

1. WHEN the create itinerary API returns THEN the frontend SHALL immediately call sseManager.connect()
2. WHEN connecting to SSE THEN the frontend SHALL wait for auth token to be available
3. IF the auth token is not available THEN the frontend SHALL wait up to 5 seconds before failing
4. WHEN the SSE connection is established THEN the frontend SHALL log success to console
5. IF the SSE connection fails THEN the frontend SHALL display a warning to the user about polling fallback

### Requirement 7: Polling Fallback Optimization

**User Story:** As a user, I want the system to work even if SSE fails, using polling as a fallback mechanism.

#### Acceptance Criteria

1. WHEN SSE connection fails THEN the frontend SHALL automatically fall back to polling
2. WHEN using polling fallback THEN the interval SHALL be 3 seconds (not 2 seconds)
3. WHEN the itinerary status becomes "complete" THEN polling SHALL stop
4. WHEN polling detects version changes THEN it SHALL fetch the full itinerary
5. IF polling fails 3 consecutive times THEN the frontend SHALL display an error message

## Success Metrics

1. SSE connection success rate: >95%
2. SSE authentication success rate: 100% for valid tokens
3. UserId persistence: 100% (never becomes null)
4. Edge update success rate: >90%
5. Real-time update latency: <2 seconds from event to UI update

## Out of Scope

- Implementing WebSocket as an alternative to SSE
- Adding SSE connection pooling or load balancing
- Implementing SSE message compression
- Adding SSE message replay for missed events
