# Implementation Plan

## Phase 1: Foundation & Unified Data Structure (Weeks 1-4)

- [x] 1. Extend NormalizedItinerary to Unified Structure


- [x] 1.1 Create new DTO classes for unified structure


  - Create `src/main/java/com/tripplanner/dto/AgentDataSection.java` with fields:
    - LocationAgentData location
    - PhotosAgentData photos  
    - BookingAgentData booking
    - TransportAgentData transport
    - DiningAgentData dining
    - ActivitiesAgentData activities
    - Map<String, Object> customData
  - Create `src/main/java/com/tripplanner/dto/WorkflowData.java` with fields:
    - List<WorkflowNode> nodes
    - List<WorkflowEdge> edges
    - WorkflowLayout layout
    - WorkflowSettings settings
  - Create `src/main/java/com/tripplanner/dto/RevisionRecord.java` with fields:
    - String revisionId
    - Long timestamp
    - String agent
    - List<ChangeDetail> changes
    - String reason
    - String userId
  - Create `src/main/java/com/tripplanner/dto/ChatRecord.java` with fields:
    - String messageId
    - Long timestamp
    - String sender
    - String content
    - String type
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 1.2 Extend existing NormalizedItinerary.java


  - Add `@JsonProperty("agentData") private Map<String, AgentDataSection> agentData` field
  - Add `@JsonProperty("workflow") private WorkflowData workflow` field
  - Add `@JsonProperty("revisions") private List<RevisionRecord> revisions` field
  - Add `@JsonProperty("chat") private List<ChatRecord> chat` field
  - Add corresponding getters and setters for all new fields
  - Update toString() method to include new fields
  - Update constructor to initialize new collections
  - _Requirements: 1.5, 1.6, 1.7, 1.8_

- [x] 1.3 Create supporting DTO classes for agent data

  - Create `src/main/java/com/tripplanner/dto/LocationAgentData.java` with enrichment data
  - Create `src/main/java/com/tripplanner/dto/PhotosAgentData.java` with photo URLs and metadata
  - Create `src/main/java/com/tripplanner/dto/BookingAgentData.java` with booking confirmations
  - Create `src/main/java/com/tripplanner/dto/TransportAgentData.java` with transport options
  - Create `src/main/java/com/tripplanner/dto/DiningAgentData.java` with restaurant data
  - Create `src/main/java/com/tripplanner/dto/ActivitiesAgentData.java` with activity details
  - _Requirements: 1.1_

- [x] 1.4 Create workflow-specific DTO classes

  - Create `src/main/java/com/tripplanner/dto/WorkflowNode.java` with position and metadata
  - Create `src/main/java/com/tripplanner/dto/WorkflowEdge.java` with connection data
  - Create `src/main/java/com/tripplanner/dto/WorkflowLayout.java` with layout settings
  - Create `src/main/java/com/tripplanner/dto/WorkflowSettings.java` with view preferences
  - Create `src/main/java/com/tripplanner/dto/ChangeDetail.java` with operation details
  - _Requirements: 1.2, 1.3_

- [x] 2. Implement Revision System Integration



- [x] 2.1 Create RevisionService.java with Firebase integration


  - Create `src/main/java/com/tripplanner/service/RevisionService.java` class
  - Inject `DatabaseService` and `ObjectMapper` dependencies
  - Implement `saveRevision(String itineraryId, RevisionRecord revision)` method:
    - Store at path `itineraries/{itineraryId}/revisions/{revisionId}` in Firebase
    - Use `databaseService.saveDocument()` for persistence
    - Add error handling with try-catch and logging
    - Validate revision data before saving
  - Implement `getRevisionHistory(String itineraryId)` method:
    - Query all revisions for itinerary from Firebase
    - Return List<RevisionRecord> sorted by timestamp descending
    - Handle empty results gracefully
  - Implement `rollbackToVersion(String itineraryId, String revisionId)` method:
    - Load revision data from Firebase
    - Reconstruct NormalizedItinerary from revision
    - Validate revision exists before rollback
    - Return restored itinerary object
  - Add comprehensive logging for all operations
  - _Requirements: 2.1, 2.2, 2.3, 2.7, 2.8_

- [x] 2.2 Enhance ChangeEngine.java with revision tracking




  - Modify existing `src/main/java/com/tripplanner/service/ChangeEngine.java`
  - Inject `RevisionService` dependency in constructor
  - Enhance `apply(String itineraryId, ChangeSet changeSet)` method:
    - Create RevisionRecord before applying changes
    - Call `createRevisionRecord(NormalizedItinerary current, ChangeSet changeSet)` 
    - Save revision using `revisionService.saveRevision()`
    - Only increment version after successful revision save
    - Add rollback logic if revision save fails
  - Implement `createRevisionRecord(NormalizedItinerary itinerary, ChangeSet changeSet)` method:
    - Generate unique revisionId using UUID
    - Set timestamp to current system time
    - Extract agent name from changeSet or default to "user"
    - Convert ChangeSet operations to List<ChangeDetail>
    - Set reason from changeSet or generate default
  - Update error handling to include revision failures
  - _Requirements: 2.1, 2.4, 2.5, 2.6_



- [x] 2.3 Update FirestoreDatabaseService.java for revision support

  - Modify existing `src/main/java/com/tripplanner/service/FirestoreDatabaseService.java`
  - Add `saveDocument(String path, String json)` method for flexible document storage
  - Add `getDocuments(String path)` method for querying document collections
  - Add `getDocument(String path)` method for single document retrieval
  - Update existing `saveRevision()` method to use new document methods
  - Add proper error handling and logging for all new methods
  - _Requirements: 2.2, 2.8_



- [x] 3. Create SummarizationService Implementation


- [x] 3.1 Implement SummarizationService.java for token optimization

  - Create `src/main/java/com/tripplanner/service/SummarizationService.java` class
  - Inject `ObjectMapper` dependency for JSON processing
  - Implement `summarizeItinerary(NormalizedItinerary itinerary, int maxTokens)` method:
    - Extract basic info (summary, days count, currency, themes)
    - Summarize each day using `summarizeDay()` method
    - Distribute token budget across days proportionally
    - Use `truncateToTokenLimit()` to enforce limits
    - Return formatted string summary
  - Implement `summarizeDay(NormalizedDay day, int maxTokens)` method:
    - Include day number, date, location
    - Summarize each node using `summarizeNode()` method
    - Distribute tokens across nodes in the day
    - Include intensity and timing information
  - Implement `summarizeNode(NormalizedNode node, int maxTokens)` method:
    - Include title, type, timing, cost, location
    - Prioritize critical information over descriptions


    - Include booking status and labels if present
    - Truncate details if token limit exceeded
  - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [x] 3.2 Add agent-specific summary formatting and token management

  - Add `summarizeForAgent(NormalizedItinerary itinerary, String agentType, int maxTokens)` method
  - Implement agent-specific formatting:
    - EditorAgent: Focus on structure and changeable elements
    - EnrichmentAgent: Focus on locations and enrichment opportunities
    - BookingAgent: Focus on bookable items and costs
  - Implement `truncateToTokenLimit(String text, int maxTokens)` method:
    - Use approximation of 1 token ≈ 4 characters
    - Preserve sentence boundaries when truncating
    - Add "..." indicator when text is truncated
  - Add `prioritizeCriticalInfo(List<String> info, int maxTokens)` method:


    - Rank information by importance (location > timing > cost > description)
    - Include critical info first, then fill remaining tokens
  - Ensure no critical information loss that affects agent decisions
  - _Requirements: 11.5, 11.6, 11.7, 11.8_

- [x] 4. Update ItineraryJsonService for Unified Structure


- [x] 4.1 Modify ItineraryJsonService.java for masterItinerary.json storage

  - Modify existing `src/main/java/com/tripplanner/service/ItineraryJsonService.java`
  - Add `saveMasterItinerary(String itineraryId, NormalizedItinerary itinerary)` method:
    - Serialize unified structure to JSON using ObjectMapper
    - Store as `masterItinerary.json` at `root/itineraries/{itineraryId}/`
    - Include all agentData, workflow, revisions, chat sections
    - Update timestamps and version before saving
  - Add `getMasterItinerary(String itineraryId)` method:
    - Retrieve `masterItinerary.json` from Firebase
    - Deserialize to NormalizedItinerary with all extensions
    - Return Optional<NormalizedItinerary> for null safety



    - Handle missing file gracefully
  - Add `updateMasterItinerary(String itineraryId, NormalizedItinerary itinerary)` method:
    - Increment version number before update
    - Update updatedAt timestamp
    - Validate itinerary data before saving
    - Use atomic update operations in Firebase
  - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [x] 4.2 Add versioning, error handling, and Firebase compatibility


  - Implement version increment logic in `updateMasterItinerary()`
  - Add comprehensive error handling with custom exceptions:
    - `ItineraryNotFoundException` for missing itineraries
    - `VersionConflictException` for concurrent updates
    - `SerializationException` for JSON processing errors
  - Add detailed logging for all storage operations
  - Maintain compatibility with existing `FirestoreDatabaseService` operations
  - Add validation for required fields before storage
  - Implement retry logic for transient Firebase errors
  - _Requirements: 12.5, 12.6, 12.7, 12.8_

## Phase 2: Agent Architecture Enhancement (Weeks 5-8)

- [x] 5. Create Agent Registry with Dynamic Registration


- [x] 5.1 Enhance AgentRegistry.java with capabilities management


  - Modify existing `src/main/java/com/tripplanner/service/AgentRegistry.java`
  - Create `src/main/java/com/tripplanner/dto/AgentCapabilities.java` with fields:
    - List<String> supportedTasks
    - List<String> supportedDataSections  
    - int priority
    - boolean enabled (default true)
    - Map<String, Object> configuration
  - Enhance `registerAgent(BaseAgent agent)` method:
    - Extract capabilities from agent using reflection or interface
    - Validate no task overlap with existing agents
    - Store in `Map<String, AgentCapabilities> capabilities`
    - Log registration with capability details
  - Implement `disableAgent(String agentId)` method:
    - Set enabled=false in capabilities map
    - Do not remove from registry
    - Log disable action
  - Enhance `getAgentsForTask(String taskType)` method:
    - Filter by enabled=true
    - Filter by supportedTasks contains taskType
    - Sort by priority (lower number = higher priority)
    - Return List<BaseAgent> in priority order
  - _Requirements: 3.1, 3.2, 3.3, 3.4_



- [x] 5.2 Add execution plan creation and task validation


  - Create `src/main/java/com/tripplanner/dto/AgentExecutionPlan.java` with fields:
    - String taskType
    - List<BaseAgent> agents
    - NormalizedItinerary context
    - Map<String, Object> parameters
  - Implement `createExecutionPlan(String taskType, NormalizedItinerary itinerary)` method:
    - Get suitable agents using `getAgentsForTask()`
    - Create execution plan with agent priorities
    - Include itinerary context for agent decisions
    - Add task-specific configuration from capabilities
  - Add `validateTaskOverlap(AgentCapabilities newCapabilities)` method:
    - Check supportedTasks against existing agents
    - Throw exception if overlap detected
    - Allow registration only if no conflicts
  - Implement dynamic agent management without system restart
  - Add configuration support for agent-specific settings
  - _Requirements: 3.5, 3.6, 3.7, 3.8_



- [x] 6. Implement EditorAgent as MVP


- [x] 6.1 Create EditorAgent.java extending BaseAgent


  - Create `src/main/java/com/tripplanner/agents/EditorAgent.java`
  - Extend existing `BaseAgent` class
  - Inject dependencies in constructor:
    - `AgentEventBus eventBus`
    - `SummarizationService summarizationService`
    - `ChangeEngine changeEngine`
    - `LLMService llmService`
    - `ItineraryJsonService itineraryJsonService`
  - Call `super(eventBus, AgentEvent.AgentKind.EDITOR)` in constructor
  - Implement `executeInternal(String itineraryId, AgentRequest<T> request)` method:
    - Cast request data to `ChatRequest` using `request.getData(ChatRequest.class)`
    - Load itinerary using `itineraryJsonService.getItinerary(itineraryId)`
    - Get summary using `summarizationService.summarizeItinerary(itinerary, 2000)`
    - Generate ChangeSet using `generateChangeSet(chatRequest, summary)`



    - Apply changes using `changeEngine.apply(itineraryId, changeSet)`
    - Return `ApplyResult` cast to generic type T
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 6.2 Add LLM integration and error handling for EditorAgent

  - Implement `generateChangeSet(ChatRequest request, String context)` method:
    - Build prompt using `buildChangeSetPrompt(request, context)`
    - Call `llmService.generateChangeSet(prompt, context)`
    - Parse response using `parseChangeSetFromResponse(response)`
    - Validate ChangeSet before returning
  - Implement `buildChangeSetPrompt(ChatRequest request, String context)` method:
    - Include user request text
    - Include summarized itinerary context
    - Add instructions for ChangeSet generation
    - Specify JSON schema requirements
  - Add comprehensive error handling:
    - Catch and wrap LLM service errors
    - Provide meaningful error messages to user


    - Log errors with context for debugging
    - Ensure itinerary is not left in broken state
  - Add progress tracking using `emitProgress()` method
  - Use `AgentEvent.AgentKind.EDITOR` for identification
  - _Requirements: 4.5, 4.6, 4.7, 4.8_

- [x] 7. Implement EnrichmentAgent with Google Places API


- [x] 7.1 Create GooglePlacesService.java for API integration


  - Create `src/main/java/com/tripplanner/service/GooglePlacesService.java`
  - Inject dependencies:
    - `@Value("${google.places.api.key}") String apiKey`
    - `RestTemplate restTemplate`
  - Set base URL: `https://maps.googleapis.com/maps/api/place`
  - Implement `getPlaceDetails(String placeId)` method:
    - Build URL: `{baseUrl}/details/json`
    - Add parameters: place_id, fields=photos,reviews,opening_hours,price_level,rating, key
    - Make GET request using RestTemplate
    - Parse response to `PlaceDetailsResponse` object
    - Extract and return `PlaceDetails` from response


    - Handle API errors and rate limits
  - Implement `getPlacePhotos(String placeId)` method:
    - Call `getPlaceDetails()` and extract photos
    - Return `List<Photo>` with URLs and metadata
  - Implement `getPlaceReviews(String placeId)` method:
    - Call `getPlaceDetails()` and extract reviews
    - Return `List<Review>` with ratings and text
  - _Requirements: 5.1, 5.2, 5.3, 5.5, 5.6_

- [x] 7.2 Add rate limiting and error handling to GooglePlacesService


  - Implement rate limit handling:
    - Track requests per day (1000 free tier, 100,000 paid tier)
    - Use `@Cacheable` annotation for repeated requests
    - Implement request queuing when approaching limits
  - Add exponential backoff retry mechanism:
    - Retry on 429 (rate limit) and 5xx errors

    - Use exponential backoff: 1s, 2s, 4s, 8s delays
    - Maximum 5 retry attempts
    - Circuit breaker pattern for persistent failures
  - Create supporting DTO classes:
    - `src/main/java/com/tripplanner/dto/PlaceDetails.java`
    - `src/main/java/com/tripplanner/dto/Photo.java`
    - `src/main/java/com/tripplanner/dto/Review.java`
    - `src/main/java/com/tripplanner/dto/PlaceDetailsResponse.java`
  - Add comprehensive logging for API calls and errors
  - _Requirements: 5.3, 5.7, 5.8_

- [x] 7.3 Enhance EnrichmentAgent.java with Places integration


  - Modify existing `src/main/java/com/tripplanner/agents/EnrichmentAgent.java`
  - Inject `GooglePlacesService` dependency in constructor
  - Implement `needsEnrichment(NormalizedNode node)` method:
    - Check if node has location with placeId
    - Check if agentData.photos is empty or outdated
    - Check if reviews are missing or stale
    - Return boolean indicating enrichment needed
  - Implement `enrichNode(NormalizedNode node)` method:
    - Call `googlePlacesService.getPlaceDetails(node.getLocation().getPlaceId())`
    - Call `updateNodeWithPlaceData(node, placeDetails)`
    - Update node's agentData.photos section
    - Set enrichment timestamp
  - Implement `updateNodeWithPlaceData(NormalizedNode node, PlaceDetails details)` method:
    - Merge photos into node.agentData.photos
    - Merge reviews into node.details
    - Update rating and price level
    - Preserve existing node data
  - _Requirements: 5.1, 5.4, 5.8_




- [x] 8. Implement BookingAgent with Real API Integration

- [x] 8.1 Create BookingComService.java for hotel bookings



  - Create `src/main/java/com/tripplanner/service/BookingComService.java`
  - Inject dependencies:
    - `@Value("${booking.com.api.key}") String apiKey`
    - `RestTemplate restTemplate`
  - Set base URL: `https://distribution-xml.booking.com/2.5/json`
  - Implement `searchHotels(BookingRequest request)` method:
    - Build search parameters from request (location, dates, guests)
    - Make POST request to `/hotels` endpoint
    - Parse response to `HotelSearchResponse`
    - Extract `List<Hotel>` from response
    - Rank hotels by reviews, ratings, price using `rankHotels()` method
  - Implement `confirmBooking(Hotel hotel, PaymentResult payment)` method:
    - Build booking parameters (hotel, payment details, guest info)
    - Make POST request to `/bookings` endpoint
    - Parse response to `BookingConfirmation`


    - Handle booking failures and errors
    - Return confirmation with booking reference
  - _Requirements: 6.1, 6.2_

- [x] 8.2 Create ExpediaService.java for flights and activities

  - Create `src/main/java/com/tripplanner/service/ExpediaService.java`
  - Inject dependencies:
    - `@Value("${expedia.api.key}") String apiKey`
    - `RestTemplate restTemplate`
  - Set base URL: `https://rapidapi.com/apidojo/api/expedia`
  - Implement `searchFlights(BookingRequest request)` method:
    - Build flight search parameters (origin, destination, dates, passengers)
    - Add `X-RapidAPI-Key` header for authentication
    - Make GET request to flights endpoint
    - Parse response to `FlightSearchResponse`
    - Return `List<Flight>` sorted by price and duration
  - Implement `searchActivities(BookingRequest request)` method:
    - Build activity search parameters (location, dates, interests)
    - Add `X-RapidAPI-Key` header for authentication



    - Make GET request to activities endpoint
    - Parse response to `ActivitySearchResponse`
    - Return `List<Activity>` sorted by rating and price
  - Handle rate limits (1000 requests/day) with request throttling
  - _Requirements: 6.2, 6.3_

- [x] 8.3 Create RazorpayService.java for payment processing

  - Create `src/main/java/com/tripplanner/service/RazorpayService.java`
  - Inject dependencies:
    - `@Value("${razorpay.key.id}") String keyId`
    - `@Value("${razorpay.key.secret}") String keySecret`
    - `RestTemplate restTemplate`
  - Set base URL: `https://api.razorpay.com/v1/`
  - Implement `processPayment(PaymentDetails details)` method:
    - Build payment request with amount, currency, receipt
    - Add Basic authentication header (keyId:keySecret base64 encoded)
    - Make POST request to `/payments` endpoint
    - Parse response to `PaymentResult`


    - Handle payment failures and validation errors


  - Implement `refundPayment(String paymentId, double amount)` method:
    - Build refund request with payment ID and amount
    - Make POST request to `/payments/{paymentId}/refund`
    - Parse response to `RefundResult`
    - Handle refund failures and business rules
  - Handle rate limits (1000 requests/minute) with request batching
  - _Requirements: 6.4, 6.8_

- [x] 8.4 Implement BookingAgent.java with complete booking flow

  - Create `src/main/java/com/tripplanner/agents/BookingAgent.java`
  - Extend `BaseAgent` with `AgentEvent.AgentKind.BOOKING`
  - Inject dependencies:
    - `BookingComService bookingComService`
    - `ExpediaService expediaService`
    - `RazorpayService razorpayService`
    - `ItineraryJsonService itineraryJsonService`
  - Implement `executeInternal(String itineraryId, AgentRequest<T> request)` method:
    - Cast request data to `BookingRequest`
    - Switch on booking type (hotel, flight, activity)
    - Call appropriate booking method


    - Return `BookingResult` with confirmation details
  - Implement `bookHotel(BookingRequest request)` method:
    - Search hotels using `bookingComService.searchHotels()`
    - Rank and select best hotel using `rankAndSelectHotel()`
    - Process payment using `razorpayService.processPayment()`
    - Confirm booking using `bookingComService.confirmBooking()`
    - Update itinerary with booking reference
  - Add similar methods for `bookFlight()` and `bookActivity()`
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

## Phase 3: Orchestrator Enhancement (Weeks 9-10)

- [x] 9. Enhance OrchestratorService with Agent Routing


- [x] 9.1 Implement LLM-based intent classification in OrchestratorService

  - Modify existing `src/main/java/com/tripplanner/service/OrchestratorService.java`
  - Inject new dependencies:


    - `LLMService llmService`
    - `AgentRegistry agentRegistry`
  - Enhance `route(ChatRequest request)` method:
    - Call `classifyIntentWithLLM(request.getText(), context)` 
    - Parse result to `IntentResult` with taskType and entities
    - Call `agentRegistry.getAgentsForTask(intent.getTaskType())`
    - Create execution plan using `agentRegistry.createExecutionPlan()`
    - Execute plan using `executeAgentPlan(plan, request)`
  - Implement `classifyIntentWithLLM(String text, String context)` method:
    - Build intent classification prompt
    - Call `llmService.classifyIntent(text, context)`
    - Parse response to extract intent type and entities
    - Return `IntentResult` object
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 9.2 Add execution plan management and error handling

  - Implement `executeAgentPlan(AgentExecutionPlan plan, ChatRequest request)` method:
    - Iterate through agents in priority order

    - Execute each agent using `agent.execute(itineraryId, agentRequest)`
    - Collect results and handle agent failures
    - Implement fallback to next agent if primary fails
    - Return `ChatResponse` with aggregated results
  - Add comprehensive error handling:
    - Catch agent execution exceptions
    - Provide meaningful error messages to user
    - Log errors with full context for debugging
    - Implement graceful degradation when agents fail
  - Implement `generateChatResponse(Object result, List<String> errors)` method:
    - Convert agent results to user-friendly messages
    - Include error information if any agents failed
    - Format response according to ChatResponse schema
  - Add fallback mechanisms for critical agent failures
  - _Requirements: 7.4, 7.5, 7.6, 7.7, 7.8_

- [x] 10. Implement LLM Service with Model Selection



- [x] 10.1 Create LLMService.java with provider pattern

  - Create `src/main/java/com/tripplanner/service/LLMService.java`
  - Inject `List<LLMProvider> providers` in constructor
  - Build `Map<String, LLMProvider> providerMap` from provider list
  - Implement `generateResponse(String prompt, String modelType, Map<String, Object> parameters)` method:
    - Get provider from map using modelType (e.g., "gemini", "qwen2.5-7b")
    - Call `provider.generate(prompt, parameters)`
    - Handle provider not found errors
    - Return generated response string
  - Implement `classifyIntent(String text, String context)` method:
    - Build intent classification prompt using `buildIntentPrompt(text, context)`
    - Call `generateResponse()` with "qwen2.5-7b" model type
    - Parse response to `IntentResult` object
    - Handle parsing errors gracefully
  - Implement `generateChangeSet(String request, String context)` method:
    - Build change set prompt using `buildChangeSetPrompt(request, context)`
    - Call `generateResponse()` with "gemini" model type
    - Parse response to `ChangeSet` object
    - Validate ChangeSet structure before returning
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 10.2 Create LLMProvider implementations



  - Create `src/main/java/com/tripplanner/service/LLMProvider.java` interface:
    - `String generate(String prompt, Map<String, Object> parameters)`
    - `boolean supportsModel(String modelName)`
    - `String getProviderName()`
  - Create `src/main/java/com/tripplanner/service/GeminiProvider.java`:
    - Implement `LLMProvider` interface
    - Inject `@Value("${gemini.api.key}") String apiKey`
    - Inject `RestTemplate restTemplate`
    - Implement `generate()` method calling Gemini API
    - Handle Gemini-specific request/response format
    - Add error handling and retry logic
  - Create `src/main/java/com/tripplanner/service/QwenProvider.java`:
    - Implement `LLMProvider` interface  
    - Inject `@Value("${qwen.api.key}") String apiKey`
    - Inject `RestTemplate restTemplate`
    - Implement `generate()` method calling Qwen2.5-7b API
    - Handle Qwen-specific request/response format
    - Add error handling and retry logic
  - Add fallback mechanisms when primary model fails
  - Implement graceful degradation to alternative models
  - _Requirements: 8.4, 8.5, 8.6, 8.7_

## Phase 4: Frontend Synchronization (Weeks 11-14)

- [x] 11. Create Unified State Management
- [x] 11.1 Implement UnifiedItineraryContext.tsx


  - Create `frontend/src/contexts/UnifiedItineraryContext.tsx`
  - Define `UnifiedItineraryContextType` interface:
    - `itinerary: NormalizedItinerary | null`
    - `isLoading: boolean`
    - `isUpdating: boolean`
    - `updateNode: (nodeId: string, updates: Partial<NormalizedNode>) => Promise<void>`
    - `updateDay: (dayNumber: number, updates: Partial<NormalizedDay>) => Promise<void>`
    - `processWithAgents: (nodeId: string, agentIds: string[]) => Promise<void>`
    - `rollbackToRevision: (revisionId: string) => Promise<void>`
  - Implement `UnifiedItineraryProvider` component:
    - Use `useState<NormalizedItinerary | null>(null)` for itinerary state
    - Use `useState<boolean>(false)` for isUpdating flag
    - Import and use existing `apiClient` from `frontend/src/services/apiClient.ts`
  - Implement `updateNode` method:
    - Validate itinerary exists and not currently updating
    - Call `updateNodeInItinerary(itinerary, nodeId, updates)` helper
    - Save to backend using `apiClient.updateItinerary(updatedItinerary)`
    - Update local state with `setItinerary(updatedItinerary)`
    - Handle errors by reverting state and showing error message
  - _Requirements: 9.1, 9.2, 9.3_

- [x] 11.2 Add agent processing and rollback functionality


  - Implement `processWithAgents` method in UnifiedItineraryContext:
    - Set isUpdating to true to prevent concurrent operations
    - Call backend API endpoint for agent processing
    - Subscribe to SSE events for progress updates
    - Update local state when agent processing completes
    - Handle agent processing errors gracefully
  - Implement `rollbackToRevision` method:
    - Call backend API to restore itinerary to specified revision
    - Update local state with restored itinerary
    - Notify user of successful rollback
    - Handle rollback errors and validation failures
  - Add `useCallback` optimization for all methods:
    - Wrap updateNode, updateDay, processWithAgents, rollbackToRevision
    - Include proper dependencies to prevent unnecessary re-renders
    - Optimize performance for large itineraries
  - Implement error handling with state reversion:
    - Store previous state before operations
    - Revert to previous state on API failures
    - Show user-friendly error messages
    - Log errors for debugging
  - _Requirements: 9.4, 9.5, 9.6, 9.7, 9.8_

- [x] 12. Update Day-by-Day View for Unified Structure
- [x] 12.1 Enhance DayByDayView.tsx with unified state


  - Modify existing `frontend/src/components/travel-planner/DayByDayView.tsx`
  - Import and use `useUnifiedItinerary()` hook from context
  - Replace existing state management with unified context
  - Implement `handleNodeUpdate` function:
    - Call `updateNode(nodeId, updates)` from context
    - Add loading states during updates
    - Handle update errors with user feedback
  - Implement `handleAgentProcess` function:
    - Call `processWithAgents(nodeId, [agentId])` from context
    - Show progress indicators during agent processing
    - Disable conflicting interactions during processing
  - Add real-time update handling:
    - Subscribe to WebSocket updates for the itinerary
    - Re-render components when updates arrive
    - Maintain scroll position during updates
  - _Requirements: 13.1, 13.2, 13.4_

- [x] 12.2 Update DayCard component with new functionality


  - Modify existing day card component (likely in travel-planner directory)
  - Add `onNodeUpdate` and `onAgentProcess` callback props to component interface
  - Implement progress indicators for agent processing:
    - Show spinner or progress bar during agent execution
    - Display current agent step and progress percentage
    - Disable node editing during agent processing
  - Add validation for node permissions and lock status:
    - Check `node.locked` property before allowing edits
    - Show lock icon for locked nodes
    - Disable edit buttons for locked nodes
    - Show tooltip explaining why node is locked
  - Ensure immediate reflection of changes in UI:
    - Update component state immediately on successful updates
    - Show optimistic updates while API call is in progress
    - Revert optimistic updates if API call fails
  - _Requirements: 13.3, 13.5, 13.7, 13.8_

- [x] 13. Update Workflow View for Unified Structure
- [x] 13.1 Enhance WorkflowBuilder.tsx with bidirectional sync



  - Modify existing `frontend/src/components/WorkflowBuilder.tsx`
  - Import and use `useUnifiedItinerary()` hook from context
  - Implement `convertWorkflowToNodeUpdates(updates: Partial<WorkflowNode>)` function:
    - Convert workflow position changes to node updates
    - Map workflow properties to NormalizedNode fields
    - Preserve existing node data while updating workflow-specific fields
  - Implement `handleNodeDrag` function:
    - Extract new position from drag event
    - Call `updateNode(nodeId, { workflow: { position } })`
    - Update workflow layout in real-time
  - Integrate with ReactFlow:
    - Use `convertToWorkflowNodes(itinerary)` to transform data
    - Use `convertToWorkflowEdges(itinerary)` for connections
    - Handle ReactFlow events (onNodeClick, onNodeDrag, onEdgeUpdate)
  - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [x] 13.2 Add workflow layout persistence and synchronization


  - Implement workflow layout restoration:
    - Load layout from `itinerary.workflow.layout` on component mount
    - Restore node positions, zoom level, and viewport
    - Handle missing layout data gracefully with default positioning
  - Add workflow position synchronization with day-by-day view:
    - Update workflow positions when day-by-day view changes timing
    - Reflect day-by-day changes in workflow node properties
    - Maintain consistent data between both views
  - Implement node permission validation:
    - Check `node.locked` status before allowing workflow changes
    - Disable drag and edit for locked nodes in workflow
    - Show visual indicators for locked nodes
  - Ensure bidirectional synchronization:
    - Changes in workflow immediately appear in day-by-day view
    - Changes in day-by-day view immediately appear in workflow
    - Maintain data consistency across view switches
  - _Requirements: 14.5, 14.6, 14.7, 14.8_



- [x] 14. Implement Real-time Synchronization
- [x] 14.1 Create WebSocketService.ts for real-time updates
  - Create `frontend/src/services/WebSocketService.ts`
  - Implement WebSocket connection management:
    - Connect to `ws://localhost:8080/ws/itinerary/{itineraryId}`
    - Handle connection open, close, error, and message events
    - Implement automatic reconnection with exponential backoff
  - Implement message parsing and listener system:
    - Parse incoming JSON messages
    - Maintain `Map<string, Function[]>` for event listeners
    - Call appropriate listeners based on message type
  - Implement `subscribe(eventType: string, callback: Function)` method:
    - Add callback to listeners map for specified event type
    - Return unsubscribe function for cleanup
  - Add connection state management:
    - Track connection status (connecting, connected, disconnected)
    - Implement reconnection logic with max attempts



    - Handle network failures gracefully
  - _Requirements: 10.1, 10.2, 10.3, 10.5_

- [x] 14.2 Create WebSocketController.java for backend broadcasting
  - Create `src/main/java/com/tripplanner/controller/WebSocketController.java`
  - Add Spring WebSocket dependencies to `build.gradle`
  - Implement `@MessageMapping("/itinerary/{itineraryId}")` method:
    - Handle incoming WebSocket messages
    - Extract itinerary ID from path variable
    - Process update message and validate permissions
  - Implement broadcasting to all connected clients:
    - Use `@SendTo("/topic/itinerary/{itineraryId}")` annotation
    - Broadcast `ItineraryUpdateMessage` to all subscribers
    - Include update type, data, and timestamp
  - Add connection management:
    - Track connected clients per itinerary
    - Handle client connect/disconnect events
    - Clean up resources when clients disconnect
  - Create `src/main/java/com/tripplanner/dto/ItineraryUpdateMessage.java`:
    - Include updateType, itineraryId, data, timestamp fields
    - Support different update types (node_update, agent_progress, etc.)
  - _Requirements: 10.4, 10.7_

## Phase 5: Testing & Quality Assurance (Weeks 15-16)

- [ ] 15. Comprehensive Testing Implementation
- [ ] 15.1 Create unit tests for all agent implementations
  - Write unit tests for EditorAgent with mock dependencies
  - Write unit tests for EnrichmentAgent with Google Places mocks
  - Write unit tests for BookingAgent with API mocks
  - Write unit tests for all service classes with >90% coverage
  - _Requirements: 15.1, 15.4_

- [ ] 15.2 Create unit tests for data transformations and API integrations
  - Write unit tests for all data structure transformations
  - Write unit tests for Google Places, Booking.com, Expedia, Razorpay integrations
  - Write unit tests for revision creation, storage, and rollback
  - Write unit tests for summarization service with various inputs
  - _Requirements: 15.2, 15.3, 15.4_

- [ ] 15.3 Create integration tests for agent orchestration
  - Write integration tests for agent coordination and execution plans
  - Write integration tests for frontend-backend synchronization
  - Write integration tests for real-time WebSocket updates
  - Write integration tests for complete revision restore workflows
  - _Requirements: 15.5, 15.6, 15.7, 15.8_

- [ ] 15.4 Create end-to-end tests for complete workflows
  - Write E2E tests for chat-driven modifications and booking flows
  - Write E2E tests for synchronization between day-by-day and workflow views
  - Write E2E tests for data consistency across all operations
  - Write E2E tests for rollback functionality and version management
  - _Requirements: 15.9, 15.10, 15.11_

- [ ] 16. Performance Optimization Implementation
- [ ] 16.1 Implement backend performance optimizations
  - Optimize database queries for <200ms response times
  - Implement caching strategies for frequently accessed data
  - Optimize API response times and implement rate limiting
  - Add performance monitoring and alerting
  - _Requirements: 16.1, 16.2, 16.3, 16.4_

- [ ] 16.2 Implement frontend performance optimizations
  - Optimize component rendering to prevent unnecessary re-renders
  - Implement virtual scrolling for large itinerary lists
  - Optimize state updates with batching to minimize UI thrashing
  - Implement lazy loading for non-critical components
  - _Requirements: 16.5, 16.6, 16.7, 16.8_

- [ ]* 16.3 Add system reliability and monitoring
  - Implement 99.9% uptime monitoring with proper alerting
  - Add comprehensive error rate tracking (<0.1% target)
  - Implement performance dashboards and metrics collection
  - Add automated health checks for all critical components
  - _Requirements: 16.9, 16.10_

## Phase 6: Deployment & Monitoring (Weeks 17-18)

- [ ] 17. Production Deployment Implementation
- [ ] 17.1 Implement blue-green deployment strategy
  - Set up blue-green deployment pipeline for zero downtime
  - Create database migration scripts for unified structure schema
  - Implement feature flags for gradual rollout capability
  - Create tested rollback procedures for quick recovery
  - _Requirements: 17.1, 17.2, 17.3, 17.4_

- [ ] 17.2 Set up comprehensive monitoring systems
  - Implement application performance monitoring with dashboards
  - Set up comprehensive error tracking and alerting systems
  - Add user analytics and engagement tracking
  - Implement automated health checks for all components
  - _Requirements: 17.5, 17.6, 17.7, 17.8_

- [ ] 18. Post-Deployment Monitoring and Optimization
- [ ] 18.1 Monitor system performance and metrics
  - Track response times and maintain <200ms for 95% of operations
  - Monitor error rates and maintain <0.1% across all operations
  - Track user engagement and feature usage patterns
  - Monitor agent performance and efficiency improvements
  - _Requirements: 17.9, 19.8, 16.10, 19.6_

- [ ] 18.2 Collect and analyze user feedback
  - Implement user feedback collection systems
  - Analyze usage patterns and identify improvement opportunities
  - Plan future enhancements based on user data
  - Measure success metrics against targets (sync accuracy, agent efficiency, user satisfaction)
  - _Requirements: 17.10, 19.1, 19.2, 19.3, 19.4, 19.5, 19.7, 19.9_

- [ ]* 18.3 Implement security and risk mitigation measures
  - Conduct security audits and penetration testing
  - Implement comprehensive backup and rollback procedures
  - Add load testing and optimization procedures
  - Implement fallback mechanisms for all external API integrations
  - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.6, 20.7, 20.8, 20.9, 20.10_