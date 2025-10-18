# Requirements Document

## Introduction

This document outlines the requirements for transforming the current itinerary system into a unified, agent-friendly architecture that supports perfect synchronization between day-by-day and workflow views. The system will implement a clean, single-source-of-truth approach with no legacy fallbacks, focusing on the foundational change to the unified data structure and starting with the EditorAgent as the MVP. This transformation addresses current issues including data duplication, sync problems, agent inefficiency, lack of versioning, and JsonEOFException errors from AI response truncation.

## Requirements

### Requirement 1: Unified Data Structure Extension

**User Story:** As a system architect, I want to extend the existing NormalizedItinerary to become the unified data structure that serves as the single source of truth for all itinerary data, so that we eliminate data duplication and synchronization issues between different views and agents.

#### Acceptance Criteria

1. WHEN extending NormalizedItinerary THEN the system SHALL add agentData Map<String, AgentDataSection> with agent-specific data sections
2. WHEN extending NormalizedItinerary THEN the system SHALL add workflow WorkflowData containing nodes, edges, layout, and settings
3. WHEN extending NormalizedItinerary THEN the system SHALL add revisions List<RevisionRecord> for change tracking
4. WHEN extending NormalizedItinerary THEN the system SHALL add chat List<ChatRecord> for conversation history
5. WHEN creating AgentDataSection THEN the system SHALL include location, photos, booking, transport, dining, and activities agent data
6. WHEN creating WorkflowData THEN the system SHALL include nodes, edges, layout, and settings for workflow view
7. WHEN creating RevisionRecord THEN the system SHALL include revisionId, timestamp, agent, changes, and reason
8. WHEN storing itinerary data THEN the system SHALL use only the unified NormalizedItinerary structure with no legacy fallbacks

### Requirement 2: Revision System Integration with ChangeEngine

**User Story:** As a user, I want complete change tracking and rollback capability integrated with the existing ChangeEngine, so that I can understand what changes were made and revert to previous versions if needed.

#### Acceptance Criteria

1. WHEN ChangeEngine applies changes THEN the system SHALL create RevisionRecord with timestamp, agent, changes list, and reason
2. WHEN RevisionService saves revision THEN the system SHALL store it at root/itineraries/{itineraryId}/revisions/{revisionId} in Firebase
3. WHEN RevisionService retrieves history THEN the system SHALL return all revisions for an itinerary ordered by timestamp
4. WHEN RevisionService performs rollback THEN the system SHALL restore itinerary to specified revision and create new revision record
5. WHEN ChangeEngine applies changes THEN the system SHALL increment itinerary version number and update updatedAt timestamp
6. WHEN creating revision THEN the system SHALL include ChangeDetail objects with operation type, node ID, and field changes
7. WHEN rollback is requested THEN the system SHALL validate revision exists before attempting restore
8. WHEN revision storage fails THEN the system SHALL log error but not block main change application

### Requirement 3: Enhanced Agent Registry with Dynamic Registration

**User Story:** As a system administrator, I want to enhance the existing AgentRegistry to support dynamic agent addition/removal/disabling with specific tasks and no overlapping capabilities, so that agents can be managed without system conflicts.

#### Acceptance Criteria

1. WHEN AgentRegistry registers agent THEN the system SHALL store agent with AgentCapabilities including supportedTasks, supportedDataSections, priority, enabled flag, and configuration
2. WHEN AgentRegistry creates execution plan THEN the system SHALL return agents for taskType ordered by priority with no overlapping responsibilities
3. WHEN AgentRegistry disables agent THEN the system SHALL set enabled=false without removing from registry
4. WHEN AgentRegistry gets agents for task THEN the system SHALL return only enabled agents that support the specific task type
5. WHEN AgentCapabilities defines tasks THEN the system SHALL ensure no two agents have overlapping supportedTasks to prevent conflicts
6. WHEN AgentExecutionPlan is created THEN the system SHALL include agent priorities and task-specific configuration
7. WHEN agent registration occurs THEN the system SHALL validate no task overlap exists with existing agents
8. WHEN agent capabilities change THEN the system SHALL update registry without requiring system restart

### Requirement 4: EditorAgent Implementation as MVP

**User Story:** As a user, I want to modify my itinerary through natural language chat interactions using the EditorAgent as the MVP for chat-driven modifications, so that I can make changes without navigating complex interfaces.

#### Acceptance Criteria

1. WHEN EditorAgent receives ChatRequest THEN the system SHALL parse request and generate ChangeSet using LLM
2. WHEN EditorAgent needs context THEN the system SHALL use SummarizationService to get itinerary summary with 2000 token limit
3. WHEN EditorAgent generates ChangeSet THEN the system SHALL use LLM with summarized context to create appropriate operations
4. WHEN EditorAgent applies changes THEN the system SHALL use ChangeEngine.apply() to ensure proper validation and revision tracking
5. WHEN EditorAgent completes execution THEN the system SHALL return ApplyResult with version and diff information
6. WHEN EditorAgent encounters errors THEN the system SHALL provide meaningful error messages and not leave itinerary in broken state
7. WHEN EditorAgent processes request THEN the system SHALL emit AgentEvent progress updates via AgentEventBus
8. WHEN EditorAgent inherits from BaseAgent THEN the system SHALL use AgentEvent.AgentKind.EDITOR for identification

### Requirement 5: Enhanced EnrichmentAgent with Google Places API Integration

**User Story:** As a user, I want the existing EnrichmentAgent enhanced to use Google Places API for photos, reviews, hours, and pricing information, so that my itinerary locations have comprehensive enrichment details.

#### Acceptance Criteria

1. WHEN EnrichmentAgent processes itinerary THEN the system SHALL check each node for enrichment needs using needsEnrichment() method
2. WHEN GooglePlacesService gets place details THEN the system SHALL call Places API with place_id and required fields (photos, reviews, opening_hours, price_level)
3. WHEN GooglePlacesService handles rate limits THEN the system SHALL implement 1000 requests/day for free tier and 100,000 for paid tier limits
4. WHEN EnrichmentAgent enriches node THEN the system SHALL call updateNodeWithPlaceData() to merge API response into node structure
5. WHEN GooglePlacesService gets photos THEN the system SHALL return List<Photo> with URLs and metadata
6. WHEN GooglePlacesService gets reviews THEN the system SHALL return List<Review> with ratings and text
7. WHEN API calls fail THEN the system SHALL implement exponential backoff retry mechanism with circuit breaker
8. WHEN enrichment completes THEN the system SHALL update node with photos in agentData.photos section

### Requirement 6: BookingAgent with Real API Integration

**User Story:** As a user, I want to book hotels, flights, and activities directly through the itinerary system using real API integrations, so that I can complete my travel planning in one place with actual bookings.

#### Acceptance Criteria

1. WHEN BookingAgent books hotel THEN the system SHALL use BookingComService to search hotels, rank by reviews/ratings/price, and confirm booking
2. WHEN BookingComService searches hotels THEN the system SHALL call Booking.com Partner API at https://distribution-xml.booking.com/2.5/json with required fields
3. WHEN BookingAgent books flight THEN the system SHALL use ExpediaService to search and book flights via Expedia Rapid API
4. WHEN BookingAgent books activity THEN the system SHALL use ExpediaService to search and book activities via Expedia Rapid API
5. WHEN BookingAgent processes payment THEN the system SHALL use RazorpayService.processPayment() with PaymentDetails
6. WHEN RazorpayService processes payment THEN the system SHALL call Razorpay API at https://api.razorpay.com/v1/ with amount, currency, receipt
7. WHEN booking is confirmed THEN the system SHALL return BookingResult with confirmation and update itinerary with bookingRef
8. WHEN payment fails after booking THEN the system SHALL mark as failed and follow refund policy via RazorpayService.refundPayment()

### Requirement 7: Enhanced OrchestratorService with Agent Routing

**User Story:** As a system, I want to enhance the existing OrchestratorService to route chat requests to appropriate agents with LLM-based intent classification, so that user requests are handled by the most suitable agent.

#### Acceptance Criteria

1. WHEN OrchestratorService receives ChatRequest THEN the system SHALL use classifyIntentWithLLM() to determine intent and extract entities
2. WHEN OrchestratorService classifies intent THEN the system SHALL use AgentRegistry.getAgentsForTask() to find appropriate agents
3. WHEN OrchestratorService creates execution plan THEN the system SHALL use AgentRegistry.createExecutionPlan() with task type and itinerary context
4. WHEN OrchestratorService executes plan THEN the system SHALL run agents in priority order according to AgentExecutionPlan
5. WHEN OrchestratorService uses LLM THEN the system SHALL call LLMService with appropriate model type for intent classification
6. WHEN intent classification completes THEN the system SHALL return IntentResult with taskType, entities, and confidence
7. WHEN agent execution completes THEN the system SHALL return ChatResponse with results and any errors
8. WHEN execution fails THEN the system SHALL provide meaningful error messages and attempt fallback agents if available

### Requirement 8: LLM Service with Model Selection

**User Story:** As a system architect, I want a service that allows different models for different agents using clean design patterns, so that each agent uses the most appropriate model for optimal performance and cost.

#### Acceptance Criteria

1. WHEN LLMService classifies intent THEN the system SHALL use Qwen2.5-7b model via QwenProvider for efficient intent classification
2. WHEN LLMService generates change sets THEN the system SHALL use Gemini model via GeminiProvider for complex change generation
3. WHEN LLMService selects provider THEN the system SHALL use Map<String, LLMProvider> to route to appropriate provider
4. WHEN LLMProvider generates response THEN the system SHALL implement generate(prompt, parameters) method with model-specific logic
5. WHEN GeminiProvider is called THEN the system SHALL implement Gemini API calls with proper authentication and error handling
6. WHEN QwenProvider is called THEN the system SHALL implement Qwen2.5-7b API calls with proper authentication and error handling
7. WHEN model is unavailable THEN the system SHALL implement fallback mechanisms and graceful degradation
8. WHEN LLMService tracks usage THEN the system SHALL monitor token consumption and costs per model type

### Requirement 9: Frontend Unified State Management

**User Story:** As a user, I want perfect synchronization between day-by-day and workflow views through centralized state management, so that changes in one view immediately reflect in the other without manual refresh.

#### Acceptance Criteria

1. WHEN UnifiedItineraryContext updates node THEN the system SHALL call updateNodeInItinerary(), save to backend via apiClient.updateItinerary(), and update local state
2. WHEN UnifiedItineraryProvider manages state THEN the system SHALL use useState<NormalizedItinerary> and isUpdating flag to prevent concurrent updates
3. WHEN updateNode is called THEN the system SHALL validate itinerary exists and not updating before applying changes
4. WHEN processWithAgents is called THEN the system SHALL trigger agent execution for specified nodeId and agentIds
5. WHEN rollbackToRevision is called THEN the system SHALL restore itinerary to specified revision via backend API
6. WHEN state update occurs THEN the system SHALL use useCallback to optimize re-renders and prevent unnecessary updates
7. WHEN backend save fails THEN the system SHALL revert local state and show error message to user
8. WHEN multiple components use context THEN the system SHALL ensure all components receive synchronized state updates

### Requirement 10: Real-time Synchronization with WebSocket

**User Story:** As a user working collaboratively, I want to see real-time updates from other users and agents using WebSocket connections, so that I'm always working with the latest itinerary state.

#### Acceptance Criteria

1. WHEN WebSocketService connects THEN the system SHALL establish WebSocket connection to ws://localhost:8080/ws/itinerary/{itineraryId}
2. WHEN WebSocketService receives message THEN the system SHALL parse JSON and notify listeners via notifyListeners(eventType, data)
3. WHEN WebSocketService subscribes THEN the system SHALL add callback to listeners Map<string, Function[]> for specified eventType
4. WHEN WebSocketController handles update THEN the system SHALL broadcast ItineraryUpdateMessage to /topic/itinerary/{itineraryId}
5. WHEN connection is lost THEN the system SHALL automatically reconnect and request missed updates since last timestamp
6. WHEN updates conflict THEN the system SHALL use last-write-wins strategy and notify user of conflict resolution
7. WHEN agent processes nodes THEN the system SHALL broadcast real-time progress via WebSocket to all connected clients
8. WHEN system is under load THEN the system SHALL maintain WebSocket performance with proper connection pooling and message queuing

### Requirement 11: Data Summarization Service Implementation

**User Story:** As a system, I want to implement a separate service for data summarization to reduce token usage while maintaining 100% context accuracy, so that agents can work efficiently with large itineraries.

#### Acceptance Criteria

1. WHEN SummarizationService summarizes itinerary THEN the system SHALL use summarizeItinerary(itinerary, maxTokens) to create concise summary
2. WHEN SummarizationService summarizes day THEN the system SHALL use summarizeDay(day, maxTokens) to focus on day-specific content
3. WHEN SummarizationService summarizes node THEN the system SHALL use summarizeNode(node, maxTokens) to extract key node information
4. WHEN maxTokens limit is specified THEN the system SHALL ensure summary stays within token limit while preserving critical information
5. WHEN summarizing for agents THEN the system SHALL provide agent-specific summary formats based on agent needs
6. WHEN critical information exists THEN the system SHALL prioritize essential data (locations, timing, costs) over descriptive text
7. WHEN token limits are approached THEN the system SHALL dynamically reduce detail level while maintaining context accuracy
8. WHEN summary is generated THEN the system SHALL ensure no critical information is lost that would affect agent decision-making

### Requirement 12: Updated ItineraryJsonService for Unified Structure

**User Story:** As a system architect, I want to modify the existing ItineraryJsonService to work with the unified structure as masterItinerary.json, so that we have a single storage mechanism for the unified data.

#### Acceptance Criteria

1. WHEN ItineraryJsonService saves master itinerary THEN the system SHALL use saveMasterItinerary() to store as masterItinerary.json at root/itineraries/{itineraryId}
2. WHEN ItineraryJsonService retrieves master itinerary THEN the system SHALL use getMasterItinerary() to load masterItinerary.json
3. WHEN ItineraryJsonService updates master itinerary THEN the system SHALL use updateMasterItinerary() with versioning support
4. WHEN storing unified structure THEN the system SHALL include all agentData, workflow, revisions, and chat sections
5. WHEN versioning is applied THEN the system SHALL increment version number and update timestamps
6. WHEN retrieval fails THEN the system SHALL return Optional.empty() and log appropriate error messages
7. WHEN storage operation fails THEN the system SHALL throw meaningful exceptions with context information
8. WHEN working with Firebase THEN the system SHALL maintain compatibility with existing Firestore operations

### Requirement 13: Day-by-Day View Update for Unified Structure

**User Story:** As a user, I want the day-by-day view updated to work with the unified structure and provide real-time synchronization, so that I can see changes immediately across all views.

#### Acceptance Criteria

1. WHEN DayByDayView handles node update THEN the system SHALL call useUnifiedItinerary().updateNode() with nodeId and updates
2. WHEN DayByDayView processes agent THEN the system SHALL call useUnifiedItinerary().processWithAgents() with nodeId and agentId array
3. WHEN DayCard renders THEN the system SHALL receive onNodeUpdate and onAgentProcess callbacks for user interactions
4. WHEN node update occurs THEN the system SHALL immediately reflect changes in the unified itinerary state
5. WHEN agent processing starts THEN the system SHALL show progress indicators and disable conflicting interactions
6. WHEN real-time updates arrive THEN the system SHALL automatically re-render affected components
7. WHEN user interactions occur THEN the system SHALL validate permissions and node lock status before allowing changes
8. WHEN view synchronization happens THEN the system SHALL ensure day-by-day view stays in sync with workflow view

### Requirement 14: Workflow View Update for Unified Structure

**User Story:** As a user, I want the workflow view updated to work with the unified structure and provide bidirectional synchronization, so that changes in workflow view reflect in day-by-day view and vice versa.

#### Acceptance Criteria

1. WHEN WorkflowBuilder handles node update THEN the system SHALL convert workflow updates to node updates via convertWorkflowToNodeUpdates()
2. WHEN WorkflowBuilder handles node drag THEN the system SHALL update workflow position in node.workflow.position
3. WHEN ReactFlow renders THEN the system SHALL use convertToWorkflowNodes() and convertToWorkflowEdges() to transform itinerary data
4. WHEN workflow position changes THEN the system SHALL call updateNode() with workflow position updates
5. WHEN node properties change THEN the system SHALL synchronize changes back to workflow layout and positioning
6. WHEN workflow view loads THEN the system SHALL restore layout from itinerary.workflow.layout data
7. WHEN bidirectional sync occurs THEN the system SHALL ensure workflow changes immediately appear in day-by-day view
8. WHEN workflow interactions happen THEN the system SHALL validate node permissions and respect locked status

### Requirement 15: Comprehensive Testing and Quality Assurance

**User Story:** As a development team, I want comprehensive testing coverage for all components and integrations, so that the unified system is reliable and maintainable.

#### Acceptance Criteria

1. WHEN implementing agent functionality THEN the system SHALL include unit tests for all agent implementations with >90% coverage
2. WHEN testing data transformations THEN the system SHALL include unit tests for all data structure transformations and mappings
3. WHEN testing API integrations THEN the system SHALL include unit tests for Google Places, Booking.com, Expedia, and Razorpay integrations
4. WHEN testing revision system THEN the system SHALL include unit tests for revision creation, storage, and rollback functionality
5. WHEN testing agent orchestration THEN the system SHALL include integration tests for agent coordination and execution plans
6. WHEN testing frontend-backend sync THEN the system SHALL include integration tests for real-time synchronization and state management
7. WHEN testing real-time updates THEN the system SHALL include integration tests for WebSocket connections and message broadcasting
8. WHEN testing rollback functionality THEN the system SHALL include integration tests for complete revision restore workflows
9. WHEN testing complete workflows THEN the system SHALL include end-to-end tests for chat-driven modifications and booking flows
10. WHEN testing data consistency THEN the system SHALL include end-to-end tests for synchronization between day-by-day and workflow views
11. WHEN performance testing THEN the system SHALL ensure <200ms response time for all operations under normal load
12. WHEN error handling testing THEN the system SHALL ensure <0.1% error rate and comprehensive error tracking with monitoring

### Requirement 16: Performance Optimization and System Reliability

**User Story:** As a system administrator, I want optimized system performance and reliable operation, so that the unified system meets production requirements for speed and uptime.

#### Acceptance Criteria

1. WHEN backend optimization occurs THEN the system SHALL optimize database queries for <200ms response times
2. WHEN caching is implemented THEN the system SHALL implement caching strategies for frequently accessed itinerary data
3. WHEN API responses are optimized THEN the system SHALL ensure all API endpoints respond within performance targets
4. WHEN rate limiting is applied THEN the system SHALL implement rate limiting to prevent abuse and ensure fair usage
5. WHEN frontend optimization occurs THEN the system SHALL optimize component rendering to prevent unnecessary re-renders
6. WHEN virtual scrolling is needed THEN the system SHALL implement virtual scrolling for large itinerary lists
7. WHEN state updates are optimized THEN the system SHALL batch state updates to minimize UI thrashing
8. WHEN lazy loading is implemented THEN the system SHALL implement lazy loading for non-critical components and data
9. WHEN system reliability is measured THEN the system SHALL achieve 99.9% uptime with proper monitoring
10. WHEN error rates are tracked THEN the system SHALL maintain <0.1% error rate across all operations

### Requirement 17: Production Deployment and Monitoring

**User Story:** As a DevOps engineer, I want to deploy the unified system to production with zero downtime and comprehensive monitoring, so that users experience seamless transition and reliable service.

#### Acceptance Criteria

1. WHEN blue-green deployment occurs THEN the system SHALL deploy new version without service interruption
2. WHEN database migration runs THEN the system SHALL execute migration scripts to update schema for unified structure
3. WHEN feature flags are used THEN the system SHALL implement gradual rollout with ability to disable features if issues arise
4. WHEN rollback procedures are needed THEN the system SHALL have tested rollback procedures for quick recovery
5. WHEN application performance is monitored THEN the system SHALL track response times, throughput, and resource usage
6. WHEN error tracking is active THEN the system SHALL implement comprehensive error tracking and alerting
7. WHEN user analytics are collected THEN the system SHALL track user engagement and feature usage patterns
8. WHEN system health is checked THEN the system SHALL implement automated health checks for all critical components
9. WHEN post-deployment monitoring occurs THEN the system SHALL monitor system performance and user feedback
10. WHEN usage patterns are analyzed THEN the system SHALL identify improvement opportunities and plan future enhancements

### Requirement 18: API Integration Specifications Compliance

**User Story:** As a system integrator, I want all external API integrations to comply with provider specifications and rate limits, so that the system operates reliably within API constraints.

#### Acceptance Criteria

1. WHEN Google Places API is used THEN the system SHALL call https://maps.googleapis.com/maps/api/place/details/json with required fields
2. WHEN Google Places rate limits apply THEN the system SHALL respect 1000 requests/day (free) and 100,000 requests/day (paid) limits
3. WHEN Booking.com API is used THEN the system SHALL call https://distribution-xml.booking.com/2.5/json with proper authentication
4. WHEN Booking.com rate limits apply THEN the system SHALL respect 1000 requests/day limit with proper request queuing
5. WHEN Expedia API is used THEN the system SHALL call https://rapidapi.com/apidojo/api/expedia with X-RapidAPI-Key header
6. WHEN Expedia rate limits apply THEN the system SHALL respect 1000 requests/day limit with appropriate throttling
7. WHEN Razorpay API is used THEN the system SHALL call https://api.razorpay.com/v1/ with Basic auth using API key and secret
8. WHEN Razorpay rate limits apply THEN the system SHALL respect 1000 requests/minute limit with request batching
9. WHEN API authentication fails THEN the system SHALL implement proper error handling and retry mechanisms
10. WHEN API responses are processed THEN the system SHALL validate response format and handle malformed data gracefully

### Requirement 19: Success Metrics and Business Requirements

**User Story:** As a product manager, I want to achieve specific success metrics for the unified system, so that we can measure the effectiveness of the transformation.

#### Acceptance Criteria

1. WHEN sync accuracy is measured THEN the system SHALL achieve 100% synchronization between day-by-day and workflow views
2. WHEN agent efficiency is measured THEN the system SHALL achieve 50% reduction in agent processing time compared to current system
3. WHEN data consistency is measured THEN the system SHALL achieve zero data inconsistencies between views and storage
4. WHEN versioning capability is measured THEN the system SHALL provide complete change tracking and rollback capability for all modifications
5. WHEN user satisfaction is measured THEN the system SHALL achieve >90% user satisfaction intensity in post-implementation surveys
6. WHEN agent productivity is measured THEN the system SHALL achieve 30% increase in agent productivity through better orchestration
7. WHEN booking success is measured THEN the system SHALL achieve >95% booking success rate for all supported providers
8. WHEN system performance is measured THEN the system SHALL maintain <200ms response time for 95% of all operations
9. WHEN business metrics are tracked THEN the system SHALL provide comprehensive analytics on user engagement and feature usage
10. WHEN ROI is calculated THEN the system SHALL demonstrate measurable improvement in development velocity and user experience

### Requirement 20: Risk Mitigation and Security

**User Story:** As a security engineer, I want comprehensive risk mitigation and security measures, so that the unified system is protected against technical and business risks.

#### Acceptance Criteria

1. WHEN data loss prevention is implemented THEN the system SHALL have comprehensive backup and rollback procedures for all data
2. WHEN performance issues are prevented THEN the system SHALL have load testing and optimization procedures in place
3. WHEN integration failures are handled THEN the system SHALL have fallback mechanisms and error handling for all external APIs
4. WHEN security vulnerabilities are prevented THEN the system SHALL undergo security audits and penetration testing
5. WHEN user adoption is ensured THEN the system SHALL have comprehensive training and documentation for all users
6. WHEN agent resistance is managed THEN the system SHALL have change management and training programs for development teams
7. WHEN API rate limits are managed THEN the system SHALL have caching and request optimization to stay within limits
8. WHEN cost overruns are prevented THEN the system SHALL have budget monitoring and cost optimization measures
9. WHEN security best practices are followed THEN the system SHALL validate all inputs and sanitize HTML for PDF/email generation
10. WHEN access control is implemented THEN the system SHALL use least-privilege IAM for Firestore and all external services