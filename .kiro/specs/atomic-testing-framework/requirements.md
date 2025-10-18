# Requirements Document

## Introduction

This specification defines a comprehensive atomic testing framework that builds from the ground up, testing individual components before progressing to integration and end-to-end scenarios. The framework ensures 100% accuracy with zero assumptions by validating each layer independently before combining them.

Based on the codebase analysis, the system has the following architecture:
- **DTO Layer**: NormalizedItinerary, NormalizedDay, NormalizedNode with supporting classes (NodeLocation, NodeCost, NodeLinks, AgentDataSection)
- **Service Layer**: ItineraryJsonService, AgentCoordinator, LLMService, GooglePlacesService, BookingService, etc.
- **Agent Layer**: BaseAgent, BookingAgent, EnrichmentAgent, PlannerAgent, PlacesAgent, EditorAgent
- **Controller Layer**: ItineraryController, ChatController, BookingController, PaymentController, PlacesController, UserController
- **Data Layer**: FirestoreItinerary entity with JSON serialization/deserialization

## Requirements

### Requirement 1: DTO Layer Atomic Testing

**User Story:** As a developer, I want to test all DTO classes in complete isolation, so that I can verify data structure integrity before any business logic testing.

#### Acceptance Criteria

1. WHEN testing NormalizedItinerary THEN the system SHALL validate all 20+ fields including itineraryId, version, userId, createdAt, updatedAt, summary, currency, themes, origin, destination, startDate, endDate, days, settings, agents, mapBounds, countryCentroid, agentData, workflow, revisions, chat
2. WHEN testing NormalizedDay THEN the system SHALL validate all fields including dayNumber, date, location, warnings, notes, pace, totalDistance, totalCost, totalDuration, timeWindowStart, timeWindowEnd, timeZone, nodes, edges
3. WHEN testing NormalizedNode THEN the system SHALL validate all fields including id, type, title, location, timing, cost, details, labels, tips, links, transit, locked, bookingRef, status, updatedBy, updatedAt, agentData
4. WHEN testing NodeLocation THEN the system SHALL validate name, address, coordinates, placeId, googleMapsUri, rating, openingHours, closingHours fields
5. WHEN testing NodeCost THEN the system SHALL validate amountPerPerson and currency fields
6. WHEN testing NodeLinks THEN the system SHALL validate nested BookingInfo with refNumber, status, details fields
7. WHEN testing AgentDataSection THEN the system SHALL validate flexible Map<String, Object> storage and retrieval operations
8. WHEN testing JSON serialization THEN the system SHALL verify all DTOs serialize/deserialize correctly with Jackson annotations
9. WHEN testing validation annotations THEN the system SHALL verify @NotBlank, @NotNull, @Valid, @Positive constraints work correctly
10. IF any DTO test fails THEN the system SHALL prevent all higher-level testing

### Requirement 2: Service Layer Atomic Testing

**User Story:** As a developer, I want to test service classes in isolation with mocked dependencies, so that I can verify business logic before integration testing.

#### Acceptance Criteria

1. WHEN testing ItineraryJsonService THEN the system SHALL validate createItinerary, updateItinerary, getItinerary, getAllItineraries, deleteItinerary, saveRevision methods with mocked DatabaseService
2. WHEN testing AgentCoordinator THEN the system SHALL validate routeTask, getCapableAgents, analyzeAgentConflicts methods with mocked AgentRegistry
3. WHEN testing LLMService THEN the system SHALL validate generateResponse method with mocked LLMProvider
4. WHEN testing GooglePlacesService THEN the system SHALL validate place search and details methods with mocked external API
5. WHEN testing BookingService THEN the system SHALL validate booking operations with mocked payment and booking providers
6. WHEN testing ChangeEngine THEN the system SHALL validate change processing logic with mocked dependencies
7. WHEN testing EnrichmentProtocolHandler THEN the system SHALL validate enrichment coordination with mocked services
8. WHEN testing flexible agent data operations THEN the system SHALL verify getAgentData, setAgentData, hasAgentData, removeAgentData methods in ItineraryJsonService
9. WHEN testing JSON serialization/deserialization THEN the system SHALL verify ObjectMapper operations handle all DTO structures correctly
10. IF any service test fails THEN the system SHALL prevent agent and controller testing

### Requirement 3: Agent Layer Atomic Testing

**User Story:** As a developer, I want to test agent classes in isolation with mocked LLM responses, so that I can verify agent logic before orchestration testing.

#### Acceptance Criteria

1. WHEN testing BaseAgent THEN the system SHALL validate canHandle, getCapabilities, validateResponsibility, determineTaskType methods
2. WHEN testing BookingAgent THEN the system SHALL validate booking capabilities, task handling for "book", "booking", "reserve", "payment" tasks with mocked BookingComService, ExpediaService, RazorpayService
3. WHEN testing EnrichmentAgent THEN the system SHALL validate enrichment capabilities, task handling for "enrich", "enrichment", "validate", "enhance" tasks with mocked GooglePlacesService, EnrichmentProtocolHandler
4. WHEN testing PlannerAgent THEN the system SHALL validate planning capabilities and task handling with mocked LLMService
5. WHEN testing PlacesAgent THEN the system SHALL validate place search and details capabilities with mocked GooglePlacesService
6. WHEN testing EditorAgent THEN the system SHALL validate editing capabilities with mocked ChangeEngine
7. WHEN testing agent capabilities THEN the system SHALL verify AgentCapabilities objects define correct supportedTasks, supportedDataSections, priority, configuration values
8. WHEN testing agent data handling THEN the system SHALL verify agents can store and retrieve flexible agent data using Map<String, Object> structure
9. WHEN testing agent responsibility validation THEN the system SHALL verify agents reject tasks outside their capabilities
10. IF any agent test fails THEN the system SHALL prevent orchestration and E2E testing

### Requirement 4: Controller Layer Testing

**User Story:** As a developer, I want to test REST API endpoints with mocked services, so that I can verify request/response handling before full integration testing.

#### Acceptance Criteria

1. WHEN testing ItineraryController THEN the system SHALL validate GET /api/itineraries, POST /api/itineraries, PUT /api/itineraries/{id}, DELETE /api/itineraries/{id} endpoints with mocked ItineraryService
2. WHEN testing ChatController THEN the system SHALL validate POST /api/chat endpoint with mocked IntentClassificationService and ChangeEngine
3. WHEN testing BookingController THEN the system SHALL validate POST /api/bookings endpoint with mocked BookingService
4. WHEN testing PaymentController THEN the system SHALL validate payment processing endpoints with mocked RazorpayService
5. WHEN testing PlacesController THEN the system SHALL validate place search and details endpoints with mocked GooglePlacesService
6. WHEN testing UserController THEN the system SHALL validate user management endpoints with mocked UserDataService
7. WHEN testing request validation THEN the system SHALL verify CreateItineraryReq, ChatRequest, BookingRequest validation rules
8. WHEN testing response formatting THEN the system SHALL verify ItineraryDto, ChatResponse, BookingResult JSON serialization
9. WHEN testing error handling THEN the system SHALL verify proper HTTP status codes for ItineraryNotFoundException, ValidationException, VersionConflictException
10. IF any controller test fails THEN the system SHALL prevent integration testing

### Requirement 5: Integration Testing - Service Layer Interactions

**User Story:** As a developer, I want to test service layer interactions with real implementations, so that I can verify component integration before full workflow testing.

#### Acceptance Criteria

1. WHEN testing ItineraryJsonService with real ObjectMapper THEN the system SHALL verify complete serialization/deserialization of NormalizedItinerary with all nested objects
2. WHEN testing AgentCoordinator with real AgentRegistry THEN the system SHALL verify agent selection and task routing logic
3. WHEN testing service dependency injection THEN the system SHALL verify Spring context loads all services correctly
4. WHEN testing flexible agent data flow THEN the system SHALL verify data passes correctly between ItineraryJsonService and AgentCoordinator
5. WHEN testing concurrent service access THEN the system SHALL verify thread safety in multi-threaded scenarios
6. WHEN testing service error propagation THEN the system SHALL verify exceptions bubble up correctly through service layers
7. WHEN testing transaction boundaries THEN the system SHALL verify data consistency in multi-service operations
8. WHEN testing service lifecycle THEN the system SHALL verify proper initialization and cleanup
9. WHEN testing configuration loading THEN the system SHALL verify all service configurations load correctly
10. IF any integration test fails THEN the system SHALL prevent orchestration testing

### Requirement 6: Agent Orchestration Testing

**User Story:** As a developer, I want to test agent coordination workflows with mocked LLM responses, so that I can verify multi-agent interactions work correctly.

#### Acceptance Criteria

1. WHEN testing AgentOrchestrator THEN the system SHALL verify createInitialItinerary and generateNormalizedItinerary methods with mocked agents
2. WHEN testing agent execution order THEN the system SHALL verify PlannerAgent runs before EnrichmentAgent before BookingAgent
3. WHEN testing agent data flow THEN the system SHALL validate flexible agent data (Map<String, Object>) passes correctly between agents
4. WHEN testing agent failure scenarios THEN the system SHALL verify proper error handling and recovery when agents fail
5. WHEN testing concurrent agent execution THEN the system SHALL verify thread safety and resource management in parallel agent operations
6. WHEN testing agent capabilities matching THEN the system SHALL verify AgentCoordinator routes tasks to correct agents based on capabilities
7. WHEN testing agent conflict resolution THEN the system SHALL verify ConflictResolver handles overlapping agent responsibilities
8. WHEN testing agent event bus THEN the system SHALL verify AgentEventBus publishes and receives agent events correctly
9. WHEN testing agent completion tracking THEN the system SHALL verify agent status updates propagate correctly
10. IF any orchestration test fails THEN the system SHALL prevent E2E workflow testing

### Requirement 7: End-to-End Itinerary Creation Flow Testing

**User Story:** As a developer, I want to test the complete itinerary creation workflow, so that I can verify the entire system works as expected with realistic data.

#### Acceptance Criteria

1. WHEN testing complete itinerary creation THEN the system SHALL execute POST /api/itineraries → ItineraryService.create → AgentOrchestrator.createInitialItinerary → AgentOrchestrator.generateNormalizedItinerary → multiple agent execution → data persistence flow
2. WHEN testing with Bali 3-day luxury itinerary data THEN the system SHALL verify all fields match the expected JSON structure including 20+ NormalizedItinerary fields, 3 NormalizedDay objects, multiple NormalizedNode objects with complete NodeLocation, NodeCost, NodeLinks data
3. WHEN testing chat-based modifications THEN the system SHALL verify POST /api/chat → IntentClassificationService → ChangeEngine → agent coordination → itinerary updates → WebSocket notifications
4. WHEN testing booking workflows THEN the system SHALL validate POST /api/bookings → BookingService → payment processing → BookingAgent execution → flexible agent data updates
5. WHEN testing flexible agent data persistence THEN the system SHALL verify Map<String, Object> agent data survives complete serialization/deserialization cycles
6. WHEN testing real-time updates THEN the system SHALL verify WebSocket notifications reach clients when itinerary changes occur
7. WHEN testing error scenarios THEN the system SHALL verify proper error handling for LLM failures, external API failures, validation failures
8. WHEN testing concurrent user access THEN the system SHALL verify multiple users can create and modify itineraries simultaneously
9. WHEN testing data consistency THEN the system SHALL verify no data corruption occurs during complex multi-agent workflows
10. IF any E2E test fails THEN the system SHALL provide detailed failure analysis with component-level debugging and exact field-level validation

### Requirement 8: Test Data Management and Mock Infrastructure

**User Story:** As a developer, I want deterministic test data and intelligent mocking, so that tests are repeatable and reliable with 100% accuracy.

#### Acceptance Criteria

1. WHEN creating test data THEN the system SHALL use factory patterns to create complete NormalizedItinerary objects with all required fields populated according to the Bali 3-day luxury example
2. WHEN using LLM mock responses THEN the system SHALL provide context-aware responses based on prompt analysis (Bali itinerary creation, Tokyo itinerary creation, intent classification, agent-specific responses)
3. WHEN mocking external APIs THEN the system SHALL provide deterministic responses for GooglePlacesService, RazorpayService, ExpediaService, BookingComService
4. WHEN testing with different scenarios THEN the system SHALL support multiple test data sets (luxury travel, budget travel, family travel, business travel)
5. WHEN validating JSON structure THEN the system SHALL verify all test data matches exact field structure from the codebase analysis
6. WHEN cleaning up after tests THEN the system SHALL ensure no test data pollution between runs and proper resource disposal
7. WHEN creating mock responses THEN the system SHALL include all required fields: itineraryId, version, userId, createdAt, updatedAt, summary, currency, themes, origin, destination, startDate, endDate, complete days array with nodes and edges
8. WHEN testing flexible agent data THEN the system SHALL create test scenarios with various Map<String, Object> structures for different agent types
9. WHEN validating field accuracy THEN the system SHALL verify every field in NodeLocation (name, address, coordinates, placeId, googleMapsUri, rating, openingHours, closingHours), NodeCost (amountPerPerson, currency), NodeLinks (nested BookingInfo)
10. IF test data is inconsistent or missing required fields THEN the system SHALL fail fast with detailed field-level error messages

### Requirement 9: Test Execution Pipeline with Strict Ordering

**User Story:** As a developer, I want automated test execution in strict dependency order, so that I can run the complete test suite reliably with zero assumptions.

#### Acceptance Criteria

1. WHEN running atomic tests THEN the system SHALL execute in strict order: DTO Layer → Service Layer → Agent Layer → Controller Layer → Integration → Orchestration → E2E
2. WHEN any test fails THEN the system SHALL immediately stop execution and provide detailed failure information including exact field validation errors, mock interaction failures, assertion details
3. WHEN DTO tests pass THEN the system SHALL automatically progress to Service Layer tests
4. WHEN Service Layer tests pass THEN the system SHALL automatically progress to Agent Layer tests
5. WHEN Agent Layer tests pass THEN the system SHALL automatically progress to Controller Layer tests
6. WHEN Controller Layer tests pass THEN the system SHALL automatically progress to Integration tests
7. WHEN Integration tests pass THEN the system SHALL automatically progress to Orchestration tests
8. WHEN Orchestration tests pass THEN the system SHALL automatically progress to E2E tests
9. WHEN testing field accuracy THEN the system SHALL validate every single field mentioned in the codebase analysis and fail if any field is missing or incorrect
10. IF the pipeline is interrupted THEN the system SHALL provide detailed stage completion status and allow resuming from the last successful stage

### Requirement 10: Test Reporting and Field-Level Validation

**User Story:** As a developer, I want comprehensive test reporting with field-level accuracy validation, so that I can understand test results and ensure 100% data structure compliance.

#### Acceptance Criteria

1. WHEN tests complete THEN the system SHALL generate detailed coverage reports for each layer (DTO: 100% field coverage, Service: 100% method coverage, Agent: 100% capability coverage, Controller: 100% endpoint coverage)
2. WHEN tests fail THEN the system SHALL provide exact field-level validation errors, stack traces, mock interaction logs, and component interaction analysis
3. WHEN validating JSON structure THEN the system SHALL report on every field validation: NormalizedItinerary (20+ fields), NormalizedDay (13 fields), NormalizedNode (16 fields), NodeLocation (8 fields), NodeCost (2 fields), NodeLinks (nested BookingInfo with 3 fields)
4. WHEN analyzing test results THEN the system SHALL highlight missing fields, incorrect field types, validation constraint failures, and provide specific recommendations
5. WHEN testing flexible agent data THEN the system SHALL report on Map<String, Object> structure validation and type conversion accuracy
6. WHEN testing mock interactions THEN the system SHALL provide detailed logs of all mock calls, parameters, and responses
7. WHEN measuring performance THEN the system SHALL generate benchmarks for critical operations (JSON serialization/deserialization, agent execution, database operations)
8. WHEN testing concurrent scenarios THEN the system SHALL report on thread safety, resource usage, and potential race conditions
9. WHEN validating against real test data THEN the system SHALL compare results against the Bali 3-day luxury example and report any discrepancies
10. IF reporting fails THEN the system SHALL still provide basic pass/fail status for all tests with minimal field validation summary