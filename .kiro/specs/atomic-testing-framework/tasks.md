# Implementation Plan

- [x] 1. Setup Test Infrastructure and Base Classes


  - Create base test classes and infrastructure for atomic testing framework
  - Setup test execution engine with strict layer ordering
  - Configure test environment with proper Spring Boot test configuration
  - _Requirements: 9.1, 9.2, 9.10_

- [x] 1.1 Create Test Execution Engine



  - Implement AtomicTestExecutionEngine with strict layer ordering (DTO → Service → Agent → Controller → Integration → Orchestration → E2E)
  - Add TestLayer enum and TestResult classes
  - Implement failure-fast mechanism that stops on first test failure
  - **Note: Ask for clarification on test execution flow, error handling strategies, and reporting requirements before implementation**
  - _Requirements: 9.1, 9.2, 9.3_

- [x] 1.2 Create Base Test Classes


  - Implement BaseServiceTest with common mock setup
  - Create BaseControllerTest with MockMvc configuration
  - Add BaseIntegrationTest with Spring Boot test context
  - _Requirements: 9.1, 9.10_

- [x] 1.3 Setup Test Configuration


  - Create test application properties with proper profiles
  - Configure ObjectMapper for JSON serialization testing
  - Setup logging configuration for detailed test output
  - _Requirements: 9.9, 10.6_

- [x] 2. Implement DTO Layer Atomic Tests



  - Create comprehensive field validation tests for all DTO classes
  - Validate every single field with exact type checking and constraint validation
  - Test JSON serialization/deserialization with Jackson annotations
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_

- [x] 2.1 Create DTO Field Validator



  - Implement DTOFieldValidator with methods for each DTO class
  - Add FieldValidationResult class to track validation success/failure
  - Create comprehensive field validation for NormalizedItinerary (20+ fields)
  - **Note: Ask for clarification on field validation rules, null handling, and validation error reporting before implementation**
  - _Requirements: 1.1, 10.3_

- [x] 2.2 Test NormalizedItinerary DTO


  - Validate all fields: itineraryId, version, userId, createdAt, updatedAt, summary, currency, themes, origin, destination, startDate, endDate, days, settings, agents, mapBounds, countryCentroid, agentData, workflow, revisions, chat
  - Test JSON serialization/deserialization with complete field preservation
  - Validate @NotBlank, @NotNull, @Valid, @Positive constraints
  - _Requirements: 1.1, 1.8, 1.9_

- [x] 2.3 Test NormalizedDay DTO


  - Validate all 13 fields: dayNumber, date, location, warnings, notes, pace, totalDistance, totalCost, totalDuration, timeWindowStart, timeWindowEnd, timeZone, nodes, edges
  - Test nested collections (nodes, edges) with proper validation
  - Verify constraint annotations work correctly
  - _Requirements: 1.2, 1.8, 1.9_

- [x] 2.4 Test NormalizedNode DTO


  - Validate all 16 fields: id, type, title, location, timing, cost, details, labels, tips, links, transit, locked, bookingRef, status, updatedBy, updatedAt, agentData
  - Test flexible agentData Map<String, Object> field
  - Validate status transition methods and helper methods
  - _Requirements: 1.3, 1.8, 1.9_

- [x] 2.5 Test Supporting DTO Classes


  - Test NodeLocation with all 8 fields: name, address, coordinates, placeId, googleMapsUri, rating, openingHours, closingHours
  - Test NodeCost with both fields: amountPerPerson, currency
  - Test NodeLinks with nested BookingInfo (refNumber, status, details)
  - Test AgentDataSection with flexible Map<String, Object> operations
  - _Requirements: 1.4, 1.5, 1.6, 1.7, 1.8_

- [x] 2.6 Create DTO Test Utilities



  - Create test data builders for each DTO class
  - Add JSON comparison utilities for serialization testing
  - Implement field validation assertion helpers
  - **Note: Ask for clarification on specific test utility patterns and assertion styles before implementation**
  - _Requirements: 8.1, 8.7_

- [x] 3. Implement Mock Infrastructure


  - Create intelligent mock providers for LLM and external APIs
  - Setup deterministic test data factory with complete field accuracy
  - Implement context-aware response routing for realistic testing
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10_

- [x] 3.1 Create LLM Mock Provider


  - Implement MockLLMProvider with context-aware response routing
  - Add PromptAnalyzer to determine appropriate mock responses
  - Load complete Bali 3-day luxury itinerary response and other test scenarios
  - **Note: Ask for clarification on prompt analysis logic, response routing strategies, and mock response formats before implementation**
  - _Requirements: 8.2, 8.7_

- [x] 3.2 Create External API Mocks


  - Mock GooglePlacesService with deterministic place search and details responses
  - Mock RazorpayService with payment processing responses
  - Mock ExpediaService and BookingComService with booking responses
  - _Requirements: 8.3, 8.7_

- [x] 3.3 Create Test Data Factory


  - Implement TestDataFactory with createBaliLuxuryItinerary method
  - Create complete NormalizedItinerary objects with all required fields
  - Add methods for different test scenarios (luxury, budget, family, business travel)
  - _Requirements: 8.1, 8.4, 8.7, 8.9_

- [x] 3.4 Setup Mock Response Files


  - Create JSON response files for all mock scenarios
  - Ensure all responses match exact field structure from codebase analysis
  - Add validation to verify mock responses contain all required fields
  - _Requirements: 8.7, 8.9, 8.10_

- [x] 4. Implement Service Layer Atomic Tests




  - Test service classes in isolation with comprehensive mocking
  - Validate business logic without external dependencies
  - Test flexible agent data operations with Map<String, Object> structures
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10_

- [x] 4.1 Test ItineraryJsonService



  - Test createItinerary, updateItinerary, getItinerary, getAllItineraries, deleteItinerary methods
  - Validate JSON serialization/deserialization with ObjectMapper
  - Test flexible agent data operations: getAgentData, setAgentData, hasAgentData, removeAgentData
  - **Note: Ask for clarification on mock setup for DatabaseService, error scenario testing, and agent data validation approaches before implementation**
  - _Requirements: 2.1, 2.8, 2.9_

- [x] 4.2 Test AgentCoordinator




  - Test routeTask method with various task types and contexts
  - Test getCapableAgents and analyzeAgentConflicts methods
  - Validate agent selection logic with mocked AgentRegistry
  - _Requirements: 2.2, 2.10_

- [x] 4.3 Test LLMService


  - Test generateResponse method with mocked LLMProvider
  - Validate request/response handling and error scenarios
  - Test integration with different LLM providers
  - _Requirements: 2.3, 2.10_

- [x] 4.4 Test External Service Classes


  - Test GooglePlacesService with mocked external API calls
  - Test BookingService with mocked payment and booking providers
  - Test ChangeEngine with mocked dependencies
  - Test EnrichmentProtocolHandler with mocked services
  - _Requirements: 2.4, 2.5, 2.6, 2.7, 2.10_

- [x] 4.5 Create Service Test Utilities


  - Create mock setup helpers for common service dependencies
  - Add assertion utilities for service method validation
  - Implement interaction verification helpers
  - **Note: Ask for clarification on mock setup patterns and verification strategies before implementation**
  - _Requirements: 2.10, 10.6_

- [x] 5. Implement Agent Layer Atomic Tests




  - Test agent classes with mocked LLM responses and external APIs
  - Validate agent capabilities and task handling logic
  - Test flexible agent data handling with Map<String, Object> structures
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10_

- [x] 5.1 Test BaseAgent


  - Test canHandle, getCapabilities, validateResponsibility, determineTaskType methods
  - Validate agent initialization and event bus integration
  - Test abstract method contracts and inheritance behavior
  - **Note: Ask for clarification on abstract class testing strategies, event bus mocking, and capability validation approaches before implementation**
  - _Requirements: 3.1, 3.9_

- [x] 5.2 Test BookingAgent


  - Test booking capabilities for "book", "booking", "reserve", "payment" tasks
  - Validate integration with BookingComService, ExpediaService, RazorpayService
  - Test agent data handling for booking operations
  - _Requirements: 3.2, 3.8_

- [x] 5.3 Test EnrichmentAgent





  - Test enrichment capabilities for "enrich", "enrichment", "validate", "enhance" tasks
  - Validate integration with GooglePlacesService and EnrichmentProtocolHandler
  - Test place data enrichment and validation logic
  - _Requirements: 3.3, 3.8_

- [x] 5.4 Test Other Agent Classes



  - Test PlannerAgent with mocked LLMService
  - Test PlacesAgent with mocked GooglePlacesService
  - Test EditorAgent with mocked ChangeEngine
  - _Requirements: 3.4, 3.5, 3.6_

- [x] 5.5 Test Agent Capabilities


  - Validate AgentCapabilities objects define correct supportedTasks, supportedDataSections, priority
  - Test capability matching and conflict detection
  - Verify agent responsibility validation works correctly
  - _Requirements: 3.7, 3.9_

- [x] 5.6 Create Agent Test Utilities


  - Create mock LLM response builders for agent testing
  - Add agent capability validation helpers
  - Implement agent data assertion utilities
  - **Note: Ask for clarification on LLM mock response patterns and agent capability validation approaches before implementation**
  - _Requirements: 3.8, 3.10_

- [ ] 6. Implement Controller Layer Tests
  - Test REST API endpoints with mocked services
  - Validate request/response handling and error scenarios
  - Test HTTP status codes and JSON serialization
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10_

- [ ] 6.1 Test ItineraryController
  - Test GET /api/itineraries, POST /api/itineraries, PUT /api/itineraries/{id}, DELETE /api/itineraries/{id}
  - Validate request validation with CreateItineraryReq
  - Test response formatting with ItineraryDto
  - **Note: Ask for clarification on MockMvc setup, request/response validation patterns, and error scenario testing before implementation**
  - _Requirements: 4.1, 4.7, 4.8_

- [ ] 6.2 Test ChatController
  - Test POST /api/chat endpoint with ChatRequest validation
  - Validate integration with IntentClassificationService and ChangeEngine
  - Test ChatResponse formatting and error handling
  - _Requirements: 4.2, 4.7, 4.8_

- [ ] 6.3 Test BookingController
  - Test POST /api/bookings endpoint with BookingRequest validation
  - Validate integration with BookingService
  - Test BookingResult response formatting
  - _Requirements: 4.3, 4.7, 4.8_

- [ ] 6.4 Test Other Controllers
  - Test PaymentController with payment processing endpoints
  - Test PlacesController with place search and details endpoints
  - Test UserController with user management endpoints
  - _Requirements: 4.4, 4.5, 4.6_

- [ ] 6.5 Test Error Handling
  - Test proper HTTP status codes for ItineraryNotFoundException, ValidationException, VersionConflictException
  - Validate error response formatting and message content
  - Test global exception handler behavior
  - _Requirements: 4.9, 4.10_

- [ ] 6.6 Create Controller Test Utilities
  - Create MockMvc test helpers for common request patterns
  - Add JSON assertion utilities for response validation
  - Implement error response validation helpers
  - **Note: Ask for clarification on MockMvc patterns and JSON assertion strategies before implementation**
  - _Requirements: 4.10, 10.6_

- [ ] 7. Implement Integration Tests
  - Test service layer interactions with real implementations
  - Validate component integration without external dependencies
  - Test Spring context loading and dependency injection
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 5.10_

- [ ] 7.1 Test Service Integration
  - Test ItineraryJsonService with real ObjectMapper and complete serialization/deserialization
  - Test AgentCoordinator with real AgentRegistry and agent selection logic
  - Validate service dependency injection and Spring context loading
  - _Requirements: 5.1, 5.2, 5.3, 5.9_

- [ ] 7.2 Test Flexible Agent Data Flow
  - Test data flow between ItineraryJsonService and AgentCoordinator
  - Validate Map<String, Object> agent data passes correctly through service layers
  - Test data consistency in multi-service operations
  - _Requirements: 5.4, 5.7_

- [ ] 7.3 Test Concurrent Access
  - Test thread safety in multi-threaded service scenarios
  - Validate concurrent access to shared resources
  - Test performance under concurrent load
  - _Requirements: 5.5, 5.10_

- [ ] 7.4 Test Error Propagation
  - Test exception handling and propagation through service layers
  - Validate transaction boundaries and rollback behavior
  - Test service lifecycle and cleanup
  - _Requirements: 5.6, 5.8_

- [ ] 7.5 Create Integration Test Utilities
  - Create Spring Boot test configuration helpers
  - Add service integration assertion utilities
  - Implement concurrent testing helpers
  - **Note: Ask for clarification on Spring Boot test configuration patterns and concurrent testing approaches before implementation**
  - _Requirements: 5.10, 10.6_

- [ ] 8. Implement Agent Orchestration Tests
  - Test agent coordination workflows with mocked external dependencies
  - Validate multi-agent interactions and data flow
  - Test agent failure scenarios and recovery
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.10_

- [ ] 8.1 Test AgentOrchestrator
  - Test createInitialItinerary and generateNormalizedItinerary methods
  - Validate agent execution order (PlannerAgent → EnrichmentAgent → BookingAgent)
  - Test agent coordination with mocked agents
  - **Note: Ask for clarification on agent orchestration mocking strategies, execution order validation, and coordination testing approaches before implementation**
  - _Requirements: 6.1, 6.2_

- [ ] 8.2 Test Agent Data Flow
  - Test flexible agent data (Map<String, Object>) passing between agents
  - Validate data consistency during multi-agent workflows
  - Test agent data persistence and retrieval
  - _Requirements: 6.3, 6.9_

- [ ] 8.3 Test Agent Failure Scenarios
  - Test proper error handling when agents fail
  - Validate recovery mechanisms and fallback behavior
  - Test partial completion scenarios
  - _Requirements: 6.4, 6.10_

- [ ] 8.4 Test Concurrent Agent Execution
  - Test thread safety in parallel agent operations
  - Validate resource management during concurrent execution
  - Test agent synchronization and coordination
  - _Requirements: 6.5_

- [ ] 8.5 Test Agent Capabilities and Routing
  - Test AgentCoordinator routes tasks to correct agents based on capabilities
  - Test ConflictResolver handles overlapping agent responsibilities
  - Test AgentEventBus publishes and receives events correctly
  - _Requirements: 6.6, 6.7, 6.8_

- [ ] 8.6 Create Orchestration Test Utilities
  - Create agent orchestration test helpers
  - Add multi-agent workflow assertion utilities
  - Implement agent failure simulation helpers
  - **Note: Ask for clarification on agent orchestration patterns and failure simulation strategies before implementation**
  - _Requirements: 6.10, 10.6_

- [ ] 9. Implement End-to-End Itinerary Creation Tests
  - Test complete itinerary creation workflow with realistic data
  - Validate entire system integration with mocked external dependencies
  - Test field-level accuracy with Bali 3-day luxury example
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10_

- [ ] 9.1 Test Complete Itinerary Creation Flow
  - Test POST /api/itineraries → ItineraryService.create → AgentOrchestrator → multiple agent execution → data persistence
  - Validate complete workflow with Bali 3-day luxury itinerary data
  - Test all 20+ NormalizedItinerary fields, 3 NormalizedDay objects, multiple NormalizedNode objects
  - **Note: Ask for clarification on E2E test setup, workflow validation strategies, and field-level assertion approaches before implementation**
  - _Requirements: 7.1, 7.2, 7.10_

- [ ] 9.2 Test Chat-Based Modifications
  - Test POST /api/chat → IntentClassificationService → ChangeEngine → agent coordination → itinerary updates
  - Validate WebSocket notifications for real-time updates
  - Test chat history and revision tracking
  - _Requirements: 7.3, 7.6_

- [ ] 9.3 Test Booking Workflows
  - Test POST /api/bookings → BookingService → payment processing → BookingAgent → flexible agent data updates
  - Validate complete booking flow with payment integration
  - Test booking confirmation and status updates
  - _Requirements: 7.4, 7.5_

- [ ] 9.4 Test Data Consistency and Persistence
  - Test flexible agent data (Map<String, Object>) survives complete serialization/deserialization cycles
  - Validate data consistency during complex multi-agent workflows
  - Test concurrent user access and data integrity
  - _Requirements: 7.5, 7.8, 7.9_

- [ ] 9.5 Test Error Scenarios and Recovery
  - Test proper error handling for LLM failures, external API failures, validation failures
  - Validate system recovery and graceful degradation
  - Test partial completion and retry mechanisms
  - _Requirements: 7.7, 7.10_

- [ ] 9.6 Create E2E Test Utilities
  - Create end-to-end test scenario builders
  - Add complete workflow assertion utilities
  - Implement field-level validation helpers for E2E tests
  - **Note: Ask for clarification on E2E test scenario patterns and workflow validation approaches before implementation**
  - _Requirements: 7.10, 10.9_

- [ ] 10. Implement Test Reporting and Validation
  - Create comprehensive test reporting with field-level accuracy validation
  - Generate detailed coverage reports and failure analysis
  - Implement performance benchmarking and trend analysis
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9, 10.10_

- [ ] 10.1 Create Test Report Generator
  - Implement comprehensive coverage reports for each layer (DTO: 100% field coverage, Service: 100% method coverage)
  - Generate field-level validation reports for all DTO classes
  - Create detailed failure analysis with exact field-level errors
  - _Requirements: 10.1, 10.3, 10.4_

- [ ] 10.2 Implement Field Validation Reporting
  - Report on every field validation: NormalizedItinerary (20+ fields), NormalizedDay (13 fields), NormalizedNode (16 fields)
  - Validate NodeLocation (8 fields), NodeCost (2 fields), NodeLinks (nested BookingInfo with 3 fields)
  - Generate missing field reports and type validation errors
  - _Requirements: 10.3, 10.5, 10.9_

- [ ] 10.3 Create Mock Interaction Reporting
  - Generate detailed logs of all mock calls, parameters, and responses
  - Report on LLM mock interactions and context-aware response routing
  - Validate external API mock usage and response accuracy
  - _Requirements: 10.6, 10.9_

- [ ] 10.4 Implement Performance Benchmarking
  - Measure execution times for critical operations (JSON serialization/deserialization, agent execution)
  - Generate performance benchmarks and trend analysis
  - Report on thread safety and concurrent access performance
  - _Requirements: 10.7, 10.8_

- [ ] 10.5 Create Failure Analysis System
  - Provide detailed failure reports with stack traces and component interaction analysis
  - Generate recommendations for test failures and system improvements
  - Compare results against Bali 3-day luxury example and report discrepancies
  - _Requirements: 10.2, 10.4, 10.9_

- [ ] 10.6 Create Reporting Utilities
  - Create test report formatting and output utilities
  - Add HTML/JSON report generation capabilities
  - Implement report aggregation and summary utilities
  - **Note: Ask for clarification on report formatting preferences and output requirements before implementation**
  - _Requirements: 10.10_