# Implementation Plan

## Context from Migration Guide

**Current State:**
- Two separate flows exist: Monolithic (AgentOrchestrator + PlannerAgent) and Pipeline (PipelineOrchestrator + specialized agents)
- Configuration conflict: `application.yml` defaults to "pipeline" but `ItineraryService` defaults to "monolithic"
- Pipeline flow is superior: 50% faster, 80% fewer timeouts, better UX with progressive loading
- Critical constraint: `AgentOrchestrator.createInitialItinerary()` CANNOT be deleted - used by both flows

**Migration Strategy:**
- Extract `createInitialItinerary()` to new `ItineraryInitializationService`
- Remove all monolithic flow code (AgentOrchestrator, PlannerAgent, ResilientAgentOrchestrator)
- Update ItineraryService to always use pipeline flow
- Remove mode configuration from application.yml
- Maintain 100% backward compatibility for users

**Key Files:**
- `src/main/java/com/tripplanner/service/ItineraryService.java` (lines 26, 32, 93, 130-200)
- `src/main/java/com/tripplanner/agents/AgentOrchestrator.java` (lines 47-85, 88-900, 200-250)
- `src/main/java/com/tripplanner/agents/PlannerAgent.java` (entire file ~1014 lines)
- `src/main/java/com/tripplanner/service/PipelineOrchestrator.java` (no changes needed)
- `src/main/resources/application.yml` (lines 162-165)

**Testing Resources:**
- Gemini response mocks available in `logs/gemini-responses/` directory
- Use `barcelona_3day_family.json` for testing complete itinerary generation
- Multiple response files available for different scenarios

---

- [x] 1. Create ItineraryInitializationService


  - Create new service class at `src/main/java/com/tripplanner/service/ItineraryInitializationService.java`
  - Extract `createInitialItinerary()` method from AgentOrchestrator (lines 47-85)
  - Extract `createInitialNormalizedItinerary()` helper method from AgentOrchestrator (lines 200-250)
  - Add constructor injection for ItineraryJsonService and UserDataService
  - Add comprehensive logging: "Creating initial itinerary: {}", "Initial itinerary created and ownership established: {}"
  - Add error handling with descriptive RuntimeException messages
  - Ensure synchronous execution (must complete before API response)
  - Set initial itinerary fields: itineraryId, version=1, userId, createdAt, updatedAt, summary, currency="INR", themes, days=[], origin, destination, startDate, endDate
  - Set settings: autoApply=false, defaultScope="trip"
  - Set agent status map with "planner" and "enrichment" entries
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  - _Migration Guide Reference: Step 2, Lines 200-250 of AgentOrchestrator.java_

- [x] 1.1 Write unit tests for ItineraryInitializationService


  - Create test file: `src/test/java/com/tripplanner/service/ItineraryInitializationServiceTest.java`
  - Test successful initialization flow with valid CreateItineraryReq
  - Test Firestore save failure handling (mock itineraryJsonService.createItinerary() to throw exception)
  - Test UserDataService failure handling (mock userDataService.saveUserTripMetadata() to throw exception)
  - Test initial structure creation with various request parameters (different destinations, dates, party sizes, interests)
  - Verify error messages are descriptive and include itineraryId and userId
  - Verify all required fields are set correctly (version=1, currency="INR", etc.)
  - Verify TripMetadata is created correctly from request and initial itinerary
  - Use Mockito for mocking dependencies
  - _Requirements: 6.1, 6.4, 8.1, 8.2, 8.3, 8.4_
  - _Migration Guide Reference: Testing Strategy - Unit Tests section_


- [x] 2. Update ItineraryService to use ItineraryInitializationService

  - File: `src/main/java/com/tripplanner/service/ItineraryService.java`
  - Remove `@Value("${itinerary.generation.mode:monolithic}")` field (line 32-33)
  - Remove `private String generationMode;` field
  - Replace `private final AgentOrchestrator agentOrchestrator;` with `private final ItineraryInitializationService initService;` (line 26)
  - Update constructor signature to inject ItineraryInitializationService instead of AgentOrchestrator
  - Make PipelineOrchestrator a required dependency (remove `@Autowired(required = false)` annotation)
  - Update constructor parameter order: initService, itineraryJsonService, userDataService, pipelineOrchestrator, agentEventPublisher
  - Update `createInitialItinerary()` call from `agentOrchestrator.createInitialItinerary()` to `initService.createInitialItinerary()` (line 93)
  - Keep the 2-second delay for SSE connection establishment
  - _Requirements: 3.1, 3.2, 3.3, 3.5_
  - _Migration Guide Reference: Step 3, ItineraryService.java modifications_

- [x] 3. Remove mode switching logic from ItineraryService


  - File: `src/main/java/com/tripplanner/service/ItineraryService.java`
  - Delete entire mode switching if/else block (lines 130-200)
  - Remove: `if ("pipeline".equalsIgnoreCase(generationMode) && pipelineOrchestrator != null)` branch
  - Remove: `else if (agentOrchestrator != null)` branch
  - Remove: `else` error branch for missing orchestrator
  - Replace with single direct call: `CompletableFuture<NormalizedItinerary> future = pipelineOrchestrator.generateItinerary(itineraryId, request, userId);`
  - Keep the `future.whenComplete()` callback for error handling
  - Update callback to only handle pipeline errors (remove monolithic-specific logic)
  - Remove all mode-related logging: "Generation mode: {}", "Mode: {}"
  - Keep logging: "Using PIPELINE mode for generation" can be simplified to "Starting pipeline generation"
  - Ensure executionId is generated: `String executionId = "exec_" + System.currentTimeMillis();`
  - Ensure error events are published via agentEventPublisher.publishErrorFromException()
  - Ensure completion logging: "Pipeline generation completed successfully: {}"
  - _Requirements: 3.1, 3.4, 3.6, 3.7_
  - _Migration Guide Reference: Step 3, Remove mode switching logic_

- [x] 3.1 Update ItineraryService unit tests


  - File: `src/test/java/com/tripplanner/testing/service/ItineraryServiceTest.java` (if exists)
  - Replace all AgentOrchestrator mock setup with ItineraryInitializationService mock
  - Remove all test cases for monolithic flow (tests that verify AgentOrchestrator.generateNormalizedItinerary() is called)
  - Update all test assertions to expect pipeline flow only
  - Verify PipelineOrchestrator.generateItinerary() is always called (never AgentOrchestrator.generateNormalizedItinerary())
  - Verify error events are published on failure via agentEventPublisher.publishErrorFromException()
  - Test that initService.createInitialItinerary() is called before pipelineOrchestrator.generateItinerary()
  - Test that CompletableFuture callbacks are properly attached
  - Mock CompletableFuture to test both success and failure scenarios
  - Use Gemini response mocks from `logs/gemini-responses/barcelona_3day_family.json` for realistic test data
  - _Requirements: 6.1, 6.2, 6.3, 6.4_
  - _Migration Guide Reference: Testing Strategy - Unit Tests section_


- [x] 4. Verify no remaining references to deleted classes


  - Run search: `grep -r "AgentOrchestrator" src/main/java --exclude-dir=test`
  - Run search: `grep -r "PlannerAgent" src/main/java --exclude-dir=test`
  - Run search: `grep -r "ResilientAgentOrchestrator" src/main/java --exclude-dir=test`
  - Expected result: No references found (except in files about to be deleted)
  - Check AgentController.java for any references to AgentOrchestrator
  - If references found, update to use ItineraryInitializationService or PipelineOrchestrator
  - Document any files that needed updates
  - _Requirements: 2.4, 2.5_
  - _Migration Guide Reference: Step 4, Verification commands_

- [x] 5. Delete monolithic flow files


  - Delete `src/main/java/com/tripplanner/agents/AgentOrchestrator.java` (entire file, ~900 lines)
  - Delete `src/main/java/com/tripplanner/agents/PlannerAgent.java` (entire file, ~1014 lines)
  - Delete `src/main/java/com/tripplanner/service/ResilientAgentOrchestrator.java` (entire file, unused)
  - Run compilation: `./gradlew compileJava`
  - Verify compilation succeeds with no errors
  - Verify no "cannot find symbol" errors for deleted classes
  - If compilation fails, check for missed references and update them
  - _Requirements: 2.1, 2.2, 2.3_
  - _Migration Guide Reference: Step 4, Delete Commands_

- [x] 6. Update application.yml configuration



  - File: `src/main/resources/application.yml`
  - Remove lines 162-165: `itinerary.generation.mode: ${ITINERARY_GENERATION_MODE:pipeline}`
  - Replace with comment block:
    ```yaml
    itinerary:
      # Pipeline mode is now the only generation mode
      # Monolithic mode removed in migration (2025-10-18)
      generation:
        pipeline:
          parallel: ${ITINERARY_PIPELINE_PARALLEL:true}
          # ... rest of pipeline config
    ```
  - Verify all pipeline configuration properties remain intact:
    - `pipeline.parallel`
    - `skeleton.timeout-ms`
    - `population.timeout-ms`
    - `enrichment.timeout-ms`
    - `finalization.timeout-ms`
  - Check if `application-cloud.yml` exists and update it similarly
  - _Requirements: 4.1, 4.2_
  - _Migration Guide Reference: Step 5, Configuration Changes_

- [ ] 7. Run full test suite
  - Execute `./gradlew clean` to remove old build artifacts
  - Execute `./gradlew test` to run all unit tests
  - Review test output for any failures
  - Fix any failing tests by updating mocks and assertions
  - Execute `./gradlew test --tests "*ItineraryService*"` to verify ItineraryService tests
  - Execute `./gradlew test --tests "*ItineraryInitialization*"` to verify new service tests
  - Execute `./gradlew test --tests "*Pipeline*"` to verify pipeline tests still pass
  - Check test coverage report: `./gradlew jacocoTestReport`
  - Verify line coverage >80% for modified classes
  - Verify branch coverage >70% for modified classes
  - Document any tests that were removed or significantly changed
  - _Requirements: 6.4, 6.6_
  - _Migration Guide Reference: Step 8, Test Commands_

- [ ] 7.1 Create integration tests for end-to-end flow
  - Create or update test file: `src/test/java/com/tripplanner/integration/ItineraryGenerationIntegrationTest.java`
  - **Test 1: Happy Path - Complete Generation**
    - POST /api/v1/itineraries with valid CreateItineraryReq
    - Verify response: 200 OK, status="generating", itineraryId present
    - Query Firestore to verify initial itinerary exists
    - Query UserDataService to verify ownership established (userOwnsTrip returns true)
    - Establish SSE connection to /api/v1/itineraries/{id}/events
    - Wait for and verify SSE events: progress 10%, 40%, 70%, 90%, 100%
    - Verify completion event received
    - GET /api/v1/itineraries/{id}/json
    - Verify complete itinerary with days, nodes, costs
  - **Test 2: Initialization Failure**
    - POST /api/v1/itineraries with invalid request (null destination)
    - Verify response: 500 Internal Server Error
    - Verify error message in response body
    - Query Firestore to verify no itinerary created
    - Query UserDataService to verify no ownership established
  - **Test 3: Async Generation Failure**
    - Mock SkeletonPlannerAgent to throw exception
    - POST /api/v1/itineraries with valid request
    - Verify response: 200 OK (initial creation succeeds)
    - Establish SSE connection
    - Wait for error event
    - Verify error event contains: severity=ERROR, phase information, error message
  - **Test 4: Concurrent Requests**
    - Create 5 threads, each POSTing /api/v1/itineraries simultaneously
    - Verify all 5 return 200 OK
    - Verify all 5 have unique itineraryId values
    - Verify all 5 have unique executionId values
    - Establish 5 separate SSE connections
    - Verify SSE events are properly isolated (each connection receives only its own events)
    - Verify all 5 complete successfully
  - Use Gemini response mocks from `logs/gemini-responses/barcelona_3day_family.json` for realistic data
  - Use Spring Boot Test framework with @SpringBootTest annotation
  - Use TestRestTemplate for HTTP requests
  - Use WebTestClient for SSE connections
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 6.5_
  - _Migration Guide Reference: Testing Strategy - Integration Tests section_

- [ ] 8. Deploy to development environment
  - Execute full build: `./gradlew clean build`
  - Verify build succeeds with no errors
  - Package application (JAR or Docker image depending on deployment method)
  - Deploy to dev environment using deployment scripts
  - Wait for application to start (check health endpoint: GET /actuator/health)
  - Verify application starts without errors (check startup logs)
  - Check logs for any warnings or errors: `tail -f logs/application.log`
  - Verify no "ClassNotFoundException" or "NoSuchMethodError" for deleted classes
  - Verify ItineraryInitializationService bean is created successfully
  - Verify PipelineOrchestrator bean is created successfully
  - _Requirements: 7.2_
  - _Migration Guide Reference: Step 8, Deployment Commands_

- [ ] 9. Manual testing in development
  - **Test 1: Create Itinerary**
    - POST /api/v1/itineraries with test data:
      ```json
      {
        "destination": "Barcelona, Spain",
        "startDate": "2025-11-01",
        "endDate": "2025-11-03",
        "startLocation": "Mumbai, India",
        "party": {"adults": 2, "children": 1, "infants": 0, "rooms": 1},
        "budgetTier": "medium",
        "interests": ["culture", "food", "family-friendly"],
        "language": "en"
      }
      ```
    - Verify response: 200 OK, status="generating", itineraryId present
    - Note the itineraryId for subsequent tests
  - **Test 2: Verify Initial Itinerary**
    - Query Firestore directly or use admin endpoint
    - Verify initial itinerary document exists with itineraryId
    - Verify fields: version=1, userId set, summary present, days=[]
  - **Test 3: Verify Ownership**
    - Query UserDataService or check user's trip list
    - Verify TripMetadata exists for the itineraryId
    - Verify user can access GET /api/v1/itineraries/{id}
  - **Test 4: Monitor SSE Events**
    - Connect to SSE endpoint: GET /api/v1/itineraries/{id}/events
    - Use curl or browser EventSource API
    - Verify progress events received: 10%, 40%, 70%, 90%, 100%
    - Verify phase transition events: skeleton→population→enrichment→finalization
    - Verify completion event received
    - Note: Events should arrive within 2-3 minutes
  - **Test 5: Retrieve Final Itinerary**
    - GET /api/v1/itineraries/{id}/json
    - Verify complete itinerary with 3 days
    - Verify each day has nodes (attractions, meals, transport)
    - Verify costs are populated
    - Verify locations have coordinates
  - **Test 6: Error Handling**
    - POST /api/v1/itineraries with invalid data (null destination)
    - Verify response: 500 Internal Server Error
    - Verify error message is descriptive
  - Document any issues found and create bug tickets if needed
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_
  - _Migration Guide Reference: Step 9, Manual Testing Scenarios_

- [ ] 10. Deploy to staging environment
  - Execute full build: `./gradlew clean build`
  - Deploy to staging environment using deployment scripts
  - Wait for application to start (check health endpoint)
  - Verify application starts without errors
  - Run integration test suite against staging: `./gradlew integrationTest -Denv=staging`
  - Monitor logs for 2 hours: `tail -f logs/application.log`
  - Check for any errors, warnings, or unexpected behavior
  - Verify no memory leaks (monitor heap usage over time)
  - Verify no thread leaks (monitor thread count over time)
  - Check Firestore for any orphaned itineraries (itineraries without ownership)
  - _Requirements: 7.2_
  - _Migration Guide Reference: Deployment Strategy - Phase 2_

- [ ] 10.1 Run performance tests in staging
  - Set up load testing tool (JMeter, Gatling, or k6)
  - Create test script that:
    - POSTs /api/v1/itineraries with varied destinations
    - Establishes SSE connections
    - Waits for completion events
    - GETs final itinerary
  - **Test 1: 10 Concurrent Users**
    - Run for 10 minutes
    - Measure average generation time (target: <90s)
    - Measure P95 generation time (target: <120s)
    - Measure P99 generation time (target: <180s)
    - Measure timeout rate (target: <5%)
    - Measure error rate (target: <2%)
  - **Test 2: 50 Concurrent Users**
    - Run for 10 minutes
    - Measure same metrics as Test 1
    - Verify performance degradation is acceptable (<20% slower)
  - **Test 3: 100 Concurrent Users**
    - Run for 10 minutes
    - Measure same metrics as Test 1
    - Verify system remains stable (no crashes)
  - **Test 4: Sustained Load**
    - Run 10 concurrent users for 1 hour
    - Monitor memory usage (should be stable, no leaks)
    - Monitor CPU usage (should be reasonable, <80% average)
    - Monitor Firestore read/write operations
    - Monitor AI API call rate and costs
  - Document all performance metrics in a report
  - Compare with baseline metrics from monolithic flow (if available)
  - Verify pipeline flow meets or exceeds performance targets
  - _Requirements: 7.3_
  - _Migration Guide Reference: Testing Strategy - Performance Tests section_

- [ ] 11. Create backup branch before production deployment
  - Ensure all changes are committed to main branch
  - Create backup branch: `git checkout -b backup/before-dual-flow-migration`
  - Push backup branch: `git push origin backup/before-dual-flow-migration`
  - Verify backup branch exists on remote: `git ls-remote --heads origin backup/before-dual-flow-migration`
  - Document rollback procedure in runbook:
    - **Immediate Rollback (<5 minutes):**
      - `git revert HEAD` and `git push origin main`
      - Or: `kubectl rollout undo deployment/tripplanner-backend`
      - Or: `gcloud run services update-traffic tripplanner-backend --to-revisions=PREVIOUS_REVISION=100`
    - **Partial Rollback (re-enable monolithic):**
      - `git checkout backup/before-dual-flow-migration -- src/main/java/com/tripplanner/agents/AgentOrchestrator.java`
      - `git checkout backup/before-dual-flow-migration -- src/main/java/com/tripplanner/agents/PlannerAgent.java`
      - `git checkout backup/before-dual-flow-migration -- src/main/java/com/tripplanner/service/ItineraryService.java`
      - `git checkout backup/before-dual-flow-migration -- src/main/resources/application.yml`
      - Set environment variable: `ITINERARY_GENERATION_MODE=monolithic`
      - Rebuild and redeploy
  - _Requirements: 7.1_
  - _Migration Guide Reference: Rollback Plan section_

- [ ] 12. Deploy to production (10% traffic)
  - Deploy new version to production environment
  - Configure traffic routing: 10% to new version, 90% to old version
  - Monitor key metrics for 2 hours using monitoring dashboard
  - **Metrics to monitor:**
    - `itinerary.generation.duration` - Average, P95, P99
    - `itinerary.generation.success_rate` - Should be >98%
    - `itinerary.generation.timeout_rate` - Should be <5%
    - `itinerary.generation.error_rate` - Should be <2%
    - `pipeline.phase.*.duration` - Per-phase timing
    - `sse.events.published` - SSE event count
    - `sse.connection.failures` - Should be minimal
  - Check logs for errors: "Failed to create initial itinerary", "Pipeline generation failed"
  - Verify SSE events working correctly (sample a few user sessions)
  - Check support channels for any user complaints
  - If any critical issues detected, execute immediate rollback
  - If metrics are within targets, proceed to next step
  - _Requirements: 7.2, 7.3_
  - _Migration Guide Reference: Deployment Strategy - Phase 3, Monitoring section_

- [ ] 13. Increase production traffic to 50%
  - Increase traffic routing: 50% to new version, 50% to old version
  - Monitor key metrics for 4 hours
  - Verify metrics remain within targets:
    - Error rate <2%
    - Timeout rate <5%
    - Average generation time <90s
    - P95 generation time <120s
  - Check logs for any new errors or warnings
  - Compare metrics between new and old versions
  - Verify new version performs as well or better than old version
  - Check for any increase in support tickets or user complaints
  - If issues detected, reduce traffic back to 10% and investigate
  - If metrics are good, proceed to next step
  - _Requirements: 7.2, 7.3_
  - _Migration Guide Reference: Deployment Strategy - Phase 3_

- [ ] 14. Increase production traffic to 100%
  - Increase traffic routing: 100% to new version
  - Monitor key metrics for 24 hours continuously
  - Verify all metrics meet targets:
    - Error rate <2%
    - Timeout rate <5%
    - Average generation time <90s
    - P95 generation time <120s
    - P99 generation time <180s
  - Monitor for any memory leaks or resource exhaustion
  - Check Firestore for any orphaned itineraries
  - Check for any stuck generations (status="generating" for >1 hour)
  - Confirm no user complaints in support channels
  - Verify SSE events are being delivered reliably
  - Check AI API usage and costs (should be similar or lower than before)
  - If all metrics are good for 24 hours, consider migration successful
  - _Requirements: 7.2, 7.3_
  - _Migration Guide Reference: Deployment Strategy - Phase 3_

- [ ] 15. Set up monitoring alerts
  - Configure monitoring system (Prometheus, Datadog, CloudWatch, etc.)
  - **Critical Alerts (page on-call immediately):**
    - Error rate >5% for 5 consecutive minutes
    - Timeout rate >10% for 5 consecutive minutes
    - Average generation time >180s for 10 consecutive minutes
    - Application health check failing
    - Firestore connection errors
  - **Warning Alerts (notify team via Slack/email):**
    - Error rate >2% for 10 consecutive minutes
    - Timeout rate >5% for 10 consecutive minutes
    - Average generation time >120s for 15 consecutive minutes
    - SSE connection failure rate >10%
    - Memory usage >80% for 15 minutes
  - Test alert notifications by triggering test alerts
  - Document alert response procedures in runbook:
    - Check logs for error patterns
    - Check Firestore connectivity
    - Check AI API status
    - Check resource usage (CPU, memory)
    - Execute rollback if critical issue
  - Assign on-call rotation for monitoring alerts
  - _Requirements: 7.3, 7.5_
  - _Migration Guide Reference: Monitoring and Observability section_

- [ ] 16. Document migration completion
  - Update README.md with migration notes:
    - Date of migration: 2025-10-18
    - Summary of changes: Migrated from dual flow to single pipeline flow
    - Benefits: 50% faster, 80% fewer timeouts, better UX
  - Update system architecture documentation:
    - Remove references to monolithic flow
    - Update architecture diagrams to show only pipeline flow
    - Document ItineraryInitializationService
  - Update deployment documentation:
    - Remove references to ITINERARY_GENERATION_MODE
    - Document new deployment process
    - Update rollback procedures
  - Create troubleshooting runbook:
    - Common issues and solutions
    - How to check if pipeline is working correctly
    - How to investigate generation failures
    - How to check SSE event delivery
    - How to verify Firestore data integrity
  - Archive migration guide:
    - Move `analysis/DUAL_FLOW_MIGRATION_GUIDE.md` to `docs/archive/`
    - Add note that migration is complete
  - Update API documentation if needed
  - _Requirements: 8.1, 8.2_
  - _Migration Guide Reference: Document Changelog section_

- [ ] 17. Clean up environment variables
  - **Development environment:**
    - Remove `ITINERARY_GENERATION_MODE` from .env files
    - Remove from docker-compose.yml if present
    - Remove from Kubernetes ConfigMaps if present
  - **Staging environment:**
    - Remove `ITINERARY_GENERATION_MODE` from environment configuration
    - Update deployment scripts to not set this variable
  - **Production environment:**
    - Remove `ITINERARY_GENERATION_MODE` from environment configuration
    - Update deployment scripts to not set this variable
    - Update Cloud Run / Kubernetes / ECS configuration
  - **CI/CD pipelines:**
    - Update GitHub Actions / GitLab CI / Jenkins pipelines
    - Remove any references to ITINERARY_GENERATION_MODE
    - Update build scripts if needed
  - **Documentation:**
    - Update environment variable documentation
    - Remove ITINERARY_GENERATION_MODE from .env.example
    - Update deployment guides
  - Verify application still starts correctly without the variable
  - _Requirements: 4.3_
  - _Migration Guide Reference: Step 5, Configuration Changes_
