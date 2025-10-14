# Implementation Tasks - SSE Real-Time Updates & Data Integrity Fixes

## Phase 1: SSE Authentication Foundation

- [x] 1. Fix SSE Auth Filter Order






  - [x] 1.1 Add isSseEndpoint() method to FirebaseAuthFilter





  - [ ] 1.2 Skip SSE endpoints in main auth filter
  - [ ] 1.3 Remove false warning logs for SSE paths
  - [ ] 1.4 Test: Verify no auth warnings for SSE endpoints
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 2. Add Token to Patches Stream

  - [x] 2.1 Modify createPatchesEventStream() to include auth token













  - [ ] 2.2 Test: Verify token parameter is added to URL
  - [ ] 2.3 Test: Verify backend accepts token from query parameter
  - _Requirements: 2.1, 2.2_

- [ ] 3. Add Patches Endpoint Auth Handler
  - [ ] 3.1 Update FirebaseSseAuthFilter to handle patches endpoint
  - [ ] 3.2 Add patches path to isSseEndpoint() check
  - [ ] 3.3 Test: Verify patches endpoint validates token
  - [ ] 3.4 Test: Verify patches endpoint sets userId attribute
  - _Requirements: 2.2, 2.3_

## Phase 2: SSE Connection Initialization

- [x] 4. Add SSE Connection Hook in Itinerary Creation








  - [x] 4.1 Find itinerary creation component/hook






  - [ ] 4.2 Import sseManager
  - [ ] 4.3 Call sseManager.connect() after receiving itinerary ID
  - [ ] 4.4 Add connection status tracking
  - [ ] 4.5 Test: Verify sseManager.connect() is called
  - [ ] 4.6 Test: Verify connection established within 2 seconds
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 5. Add SSE Connection Status UI
  - [ ] 5.1 Create useSSEConnection hook
  - [x] 5.2 Add connection status display in UI







  - [ ] 5.3 Show warning if SSE fails and polling is used
  - [ ] 5.4 Test: Verify UI shows connection status
  - _Requirements: 1.4, 6.4, 6.5_

- [ ] 6. Add SSE Connection Retry Logic
  - [ ] 6.1 Add retry counter and max retries to sseManager
  - [ ] 6.2 Implement exponential backoff
  - [ ] 6.3 Log retry attempts
  - [ ] 6.4 Test: Verify retries happen on failure
  - _Requirements: 1.5_

## Phase 3: UserId Persistence

- [ ] 7. Add Token Refresh Before Polling
  - [ ] 7.1 Create ensureValidToken() method in apiClient
  - [ ] 7.2 Check token expiry before each request
  - [ ] 7.3 Refresh token proactively if expiring soon
  - [ ] 7.4 Don't clear token on refresh failure
  - [ ] 7.5 Test: Verify token is refreshed before expiry
  - [ ] 7.6 Test: Verify userId remains consistent
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 8. Add UserId Validation in Backend
  - [ ] 8.1 Add validation in ItineraryJsonService.saveItinerary()
  - [ ] 8.2 Log error if userId is null
  - [ ] 8.3 Throw exception to prevent saving with null userId
  - [ ] 8.4 Test: Verify exception thrown if userId is null
  - _Requirements: 3.5_

- [ ] 9. Add UserId Immutability Check
  - [ ] 9.1 Review NormalizedItinerary userId handling
  - [ ] 9.2 Add setter validation or make userId final
  - [ ] 9.3 Log warning if userId changes
  - [ ] 9.4 Test: Verify userId cannot be changed after creation
  - _Requirements: 3.4_

## Phase 4: Edge Update Robustness

- [ ] 10. Add Edge Update Validation
  - [ ] 10.1 Add validation in ChangeEngine.applyEdgeUpdate()
  - [ ] 10.2 Log detailed error if day is null
  - [ ] 10.3 Skip invalid edge updates
  - [ ] 10.4 Test: Verify null day edges are rejected
  - _Requirements: 4.1, 4.2, 4.3_

- [ ] 11. Fix Enrichment Agent Edge Creation
  - [ ] 11.1 Find where edge updates are created in EnrichmentAgent
  - [ ] 11.2 Ensure day number is set from node's day
  - [ ] 11.3 Add validation before creating edge update
  - [ ] 11.4 Test: Verify all edge updates have day numbers
  - _Requirements: 4.4_

- [ ] 12. Add Edge Update Metrics
  - [ ] 12.1 Create EdgeUpdateMetrics class
  - [ ] 12.2 Track success/failure counts
  - [ ] 12.3 Log summary after enrichment
  - [ ] 12.4 Test: Verify metrics are logged
  - _Requirements: 4.5_

## Phase 5: Monitoring & Observability

- [ ] 13. Add SSE Connection Metrics
  - [ ] 13.1 Track emitter count per itinerary in AgentEventBus
  - [ ] 13.2 Log emitter registration/unregistration
  - [ ] 13.3 Log event delivery success/failure
  - [ ] 13.4 Test: Verify emitter count is logged
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 14. Add Frontend SSE Diagnostics
  - [ ] 14.1 Add detailed connection logging in sseManager
  - [ ] 14.2 Log all SSE events received
  - [ ] 14.3 Add connection state tracking
  - [ ] 14.4 Test: Verify detailed logs in browser console
  - _Requirements: 5.4_

- [ ] 15. Add Health Check Endpoint
  - [ ] 15.1 Create HealthController
  - [ ] 15.2 Add health check endpoint with SSE emitter counts
  - [ ] 15.3 Add per-itinerary SSE health endpoint
  - [ ] 15.4 Test: Verify health endpoint returns emitter counts
  - _Requirements: 5.5_
