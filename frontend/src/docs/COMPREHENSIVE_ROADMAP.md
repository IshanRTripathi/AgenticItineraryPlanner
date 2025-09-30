# Comprehensive Unified Itinerary System Roadmap

## Executive Summary

This roadmap outlines the complete transformation of the current itinerary system into a unified, agent-friendly architecture that supports perfect synchronization between day-by-day and workflow views. The implementation follows a clean, single-source-of-truth approach with no legacy fallbacks, focusing on the foundational change to the unified data structure and starting with the EditorAgent as the MVP.

## Current State Analysis

### Existing Architecture
- **Backend**: Spring Boot with `NormalizedItinerary` as the primary data structure
- **Frontend**: React with `TripData` for user input and `NormalizedItinerary` for display
- **Storage**: Firebase with `ItineraryJsonService` (root/itineraries/{id}) and `UserDataService` (users/{userId}/itineraries/{id})
- **Agents**: `PlannerAgent` and `EnrichmentAgent` extending `BaseAgent`
- **Chat**: `OrchestratorService` with intent classification and change management
- **Sync**: SSE for agent progress, React Query for data fetching

### Key Issues Identified
1. **Data Duplication**: Multiple data structures serving similar purposes
2. **Sync Problems**: Changes in one view don't reflect in the other
3. **Agent Inefficiency**: Agents work with different data structures
4. **No Versioning**: Limited change tracking and rollback capability
5. **JsonEOFException**: AI response truncation causing generation failures

## Phase 1: Foundation & Unified Data Structure (Weeks 1-4)

### 1.1 Extend NormalizedItinerary to Unified Structure (Week 1)

**Objective**: Transform `NormalizedItinerary` into the unified data structure that supports all agent operations and view synchronization.

**Tasks**:
- **Extend NormalizedItinerary.java**:
  ```java
  // Add agent-specific data sections
  @JsonProperty("agentData")
  private Map<String, AgentDataSection> agentData;
  
  // Add workflow-specific data
  @JsonProperty("workflow")
  private WorkflowData workflow;
  
  // Add revision tracking
  @JsonProperty("revisions")
  private List<RevisionRecord> revisions;
  
  // Add chat history
  @JsonProperty("chat")
  private List<ChatRecord> chat;
  ```

- **Create AgentDataSection.java**:
  ```java
  public class AgentDataSection {
      private LocationAgentData location;
      private PhotosAgentData photos;
      private BookingAgentData booking;
      private TransportAgentData transport;
      private DiningAgentData dining;
      private ActivitiesAgentData activities;
  }
  ```

- **Create WorkflowData.java**:
  ```java
  public class WorkflowData {
      private List<WorkflowNode> nodes;
      private List<WorkflowEdge> edges;
      private WorkflowLayout layout;
      private WorkflowSettings settings;
  }
  ```

- **Create RevisionRecord.java**:
  ```java
  public class RevisionRecord {
      private String revisionId;
      private Long timestamp;
      private String agent;
      private List<ChangeDetail> changes;
      private String reason;
  }
  ```

### 1.2 Implement Revision System Integration (Week 2)

**Objective**: Integrate revision tracking with the existing `ChangeEngine` and store at `root/itineraries/{itineraryId}/revisions/{revisionId}`.

**Tasks**:
- **Extend ChangeEngine.java**:
  ```java
  public class ChangeEngine {
      public ApplyResult apply(String itineraryId, ChangeSet changeSet) {
          // Apply changes
          NormalizedItinerary updated = applyChanges(itinerary, changeSet);
          
          // Create revision record
          RevisionRecord revision = createRevisionRecord(changeSet, "user");
          
          // Save revision to Firebase
          saveRevision(itineraryId, revision);
          
          // Update itinerary version
          updated.setVersion(updated.getVersion() + 1);
          
          return new ApplyResult(updated, revision);
      }
  }
  ```

- **Create RevisionService.java**:
  ```java
  @Service
  public class RevisionService {
      public void saveRevision(String itineraryId, RevisionRecord revision) {
          // Save to Firebase at root/itineraries/{itineraryId}/revisions/{revisionId}
      }
      
      public List<RevisionRecord> getRevisionHistory(String itineraryId) {
          // Retrieve all revisions for an itinerary
      }
      
      public NormalizedItinerary rollbackToVersion(String itineraryId, String revisionId) {
          // Rollback to specific revision
      }
  }
  ```

### 1.3 Create Summarization Service (Week 3)

**Objective**: Implement a separate service for data summarization to reduce token usage while maintaining 100% context.

**Tasks**:
- **Create SummarizationService.java**:
  ```java
  @Service
  public class SummarizationService {
      public String summarizeItinerary(NormalizedItinerary itinerary, int maxTokens) {
          // Summarize itinerary data for agent context
      }
      
      public String summarizeDay(NormalizedDay day, int maxTokens) {
          // Summarize day data for agent context
      }
      
      public String summarizeNode(NormalizedNode node, int maxTokens) {
          // Summarize node data for agent context
      }
  }
  ```

### 1.4 Update ItineraryJsonService for Unified Structure (Week 4)

**Objective**: Modify `ItineraryJsonService` to work with the unified structure as `masterItinerary.json`.

**Tasks**:
- **Update ItineraryJsonService.java**:
  ```java
  @Service
  public class ItineraryJsonService {
      public void saveMasterItinerary(String itineraryId, NormalizedItinerary itinerary) {
          // Save as masterItinerary.json at root/itineraries/{itineraryId}
      }
      
      public Optional<NormalizedItinerary> getMasterItinerary(String itineraryId) {
          // Retrieve masterItinerary.json
      }
      
      public void updateMasterItinerary(String itineraryId, NormalizedItinerary itinerary) {
          // Update masterItinerary.json with versioning
      }
  }
  ```

## Phase 2: Agent Architecture Enhancement (Weeks 5-8)

### 2.1 Create Agent Registry with Dynamic Registration (Week 5)

**Objective**: Enhance the existing `AgentRegistry` to support dynamic agent addition/removal/disabling with specific tasks and no overlapping.

**Tasks**:
- **Enhance AgentRegistry.java**:
  ```java
  @Component
  public class AgentRegistry {
      private final Map<String, BaseAgent> agents = new ConcurrentHashMap<>();
      private final Map<String, AgentCapabilities> capabilities = new ConcurrentHashMap<>();
      
      public void registerAgent(BaseAgent agent) {
          // Register agent with capabilities
      }
      
      public void disableAgent(String agentId) {
          // Disable agent without removing from registry
      }
      
      public List<BaseAgent> getAgentsForTask(String taskType) {
          // Return agents capable of handling specific task
      }
      
      public AgentExecutionPlan createExecutionPlan(String taskType, NormalizedItinerary itinerary) {
          // Create execution plan with agent priorities
      }
  }
  ```

- **Create AgentCapabilities.java**:
  ```java
  public class AgentCapabilities {
      private List<String> supportedTasks;
      private List<String> supportedDataSections;
      private int priority;
      private boolean enabled;
      private Map<String, Object> configuration;
  }
  ```

### 2.2 Implement EditorAgent (MVP) (Week 6)

**Objective**: Implement the EditorAgent as the MVP for chat-driven itinerary modifications.

**Tasks**:
- **Create EditorAgent.java**:
  ```java
  @Component
  public class EditorAgent extends BaseAgent {
      private final SummarizationService summarizationService;
      private final ChangeEngine changeEngine;
      
      public EditorAgent(AgentEventBus eventBus, SummarizationService summarizationService, ChangeEngine changeEngine) {
          super(eventBus, AgentEvent.AgentKind.EDITOR);
      }
      
      @Override
      public <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
          // Parse chat request
          ChatRequest chatRequest = (ChatRequest) request.getData();
          
          // Get itinerary summary for context
          String summary = summarizationService.summarizeItinerary(getItinerary(itineraryId), 2000);
          
          // Generate change set using LLM
          ChangeSet changeSet = generateChangeSet(chatRequest, summary);
          
          // Apply changes
          ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);
          
          return (T) result;
      }
      
      private ChangeSet generateChangeSet(ChatRequest request, String context) {
          // Use LLM to generate appropriate change set
      }
  }
  ```

### 2.3 Implement EnrichmentAgent with Google Places API (Week 7)

**Objective**: Enhance the existing `EnrichmentAgent` to use Google Places API for photos, reviews, hours, and pricing.

**Tasks**:
- **Research Google Places API**:
  - **Places API**: For place details, photos, reviews
  - **Places API (New)**: For advanced place information
  - **Rate Limits**: 1000 requests/day for free tier, 100,000 for paid
  - **Required Fields**: place_id, photos, reviews, opening_hours, price_level

- **Enhance EnrichmentAgent.java**:
  ```java
  @Component
  public class EnrichmentAgent extends BaseAgent {
      private final GooglePlacesService googlePlacesService;
      
      @Override
      public <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
          NormalizedItinerary itinerary = getItinerary(itineraryId);
          
          for (NormalizedDay day : itinerary.getDays()) {
              for (NormalizedNode node : day.getNodes()) {
                  if (needsEnrichment(node)) {
                      enrichNode(node);
                  }
              }
          }
          
          return (T) itinerary;
      }
      
      private void enrichNode(NormalizedNode node) {
          // Get place details from Google Places API
          PlaceDetails details = googlePlacesService.getPlaceDetails(node.getLocation().getPlaceId());
          
          // Update node with enriched data
          updateNodeWithPlaceData(node, details);
      }
  }
  ```

- **Create GooglePlacesService.java**:
  ```java
  @Service
  public class GooglePlacesService {
      private final String apiKey;
      private final RestTemplate restTemplate;
      
      public PlaceDetails getPlaceDetails(String placeId) {
          // Call Google Places API
      }
      
      public List<Photo> getPlacePhotos(String placeId) {
          // Get photos for a place
      }
      
      public List<Review> getPlaceReviews(String placeId) {
          // Get reviews for a place
      }
  }
  ```

### 2.4 Implement BookingAgent with Real API Integration (Week 8)

**Objective**: Implement BookingAgent with real API integration for hotels, flights, and activities.

**Tasks**:
- **Research Booking APIs**:
  - **Booking.com Partner API**: For hotel bookings
  - **Expedia Rapid API**: For flights and activities
  - **Razorpay API**: For payment processing

- **Create BookingAgent.java**:
  ```java
  @Component
  public class BookingAgent extends BaseAgent {
      private final BookingComService bookingComService;
      private final ExpediaService expediaService;
      private final RazorpayService razorpayService;
      
      @Override
      public <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
          BookingRequest bookingRequest = (BookingRequest) request.getData();
          
          switch (bookingRequest.getType()) {
              case "hotel":
                  return (T) bookHotel(bookingRequest);
              case "flight":
                  return (T) bookFlight(bookingRequest);
              case "activity":
                  return (T) bookActivity(bookingRequest);
              default:
                  throw new IllegalArgumentException("Unsupported booking type");
          }
      }
      
      private BookingResult bookHotel(BookingRequest request) {
          // Search hotels using Booking.com API
          List<Hotel> hotels = bookingComService.searchHotels(request);
          
          // Rank hotels by reviews, ratings, price
          Hotel selectedHotel = rankAndSelectHotel(hotels, request);
          
          // Process payment using Razorpay
          PaymentResult payment = razorpayService.processPayment(request.getPaymentDetails());
          
          // Confirm booking
          BookingConfirmation confirmation = bookingComService.confirmBooking(selectedHotel, payment);
          
          return new BookingResult(confirmation);
      }
  }
  ```

- **Create BookingComService.java**:
  ```java
  @Service
  public class BookingComService {
      private final String apiKey;
      private final String baseUrl = "https://distribution-xml.booking.com/2.5/json";
      
      public List<Hotel> searchHotels(BookingRequest request) {
          // Call Booking.com API for hotel search
      }
      
      public BookingConfirmation confirmBooking(Hotel hotel, PaymentResult payment) {
          // Confirm booking with Booking.com
      }
  }
  ```

- **Create ExpediaService.java**:
  ```java
  @Service
  public class ExpediaService {
      private final String apiKey;
      private final String baseUrl = "https://rapidapi.com/apidojo/api/expedia";
      
      public List<Flight> searchFlights(BookingRequest request) {
          // Call Expedia API for flight search
      }
      
      public List<Activity> searchActivities(BookingRequest request) {
          // Call Expedia API for activity search
      }
  }
  ```

- **Create RazorpayService.java**:
  ```java
  @Service
  public class RazorpayService {
      private final String apiKey;
      private final String apiSecret;
      
      public PaymentResult processPayment(PaymentDetails details) {
          // Process payment using Razorpay API
      }
      
      public PaymentResult refundPayment(String paymentId, double amount) {
          // Process refund using Razorpay API
      }
  }
  ```

## Phase 3: Orchestrator Enhancement (Weeks 9-10)

### 3.1 Enhance OrchestratorService with Agent Routing (Week 9)

**Objective**: Enhance the existing `OrchestratorService` to route chat requests to appropriate agents with LLM-based classification.

**Tasks**:
- **Enhance OrchestratorService.java**:
  ```java
  @Service
  public class OrchestratorService {
      private final AgentRegistry agentRegistry;
      private final LLMService llmService;
      
      public ChatResponse route(ChatRequest request) {
          // Classify intent using LLM
          IntentResult intent = classifyIntentWithLLM(request);
          
          // Find appropriate agents
          List<BaseAgent> agents = agentRegistry.getAgentsForTask(intent.getTaskType());
          
          // Create execution plan
          AgentExecutionPlan plan = agentRegistry.createExecutionPlan(intent.getTaskType(), getItinerary(request.getItineraryId()));
          
          // Execute agents in priority order
          return executeAgentPlan(plan, request);
      }
      
      private IntentResult classifyIntentWithLLM(ChatRequest request) {
          // Use LLM to classify intent and extract entities
      }
      
      private ChatResponse executeAgentPlan(AgentExecutionPlan plan, ChatRequest request) {
          // Execute agents according to plan
      }
  }
  ```

### 3.2 Implement LLM Service with Model Selection (Week 10)

**Objective**: Create a service that allows different models for different agents using clean design patterns.

**Tasks**:
- **Create LLMService.java**:
  ```java
  @Service
  public class LLMService {
      private final Map<String, LLMProvider> providers;
      
      public String generateResponse(String prompt, String modelType, Map<String, Object> parameters) {
          LLMProvider provider = providers.get(modelType);
          return provider.generate(prompt, parameters);
      }
      
      public IntentResult classifyIntent(String text, String context) {
          // Use Qwen2.5-7b for intent classification
          return generateResponse(buildIntentPrompt(text, context), "qwen2.5-7b", null);
      }
      
      public ChangeSet generateChangeSet(String request, String context) {
          // Use Gemini for change set generation
          return generateResponse(buildChangeSetPrompt(request, context), "gemini", null);
      }
  }
  ```

- **Create LLMProvider.java**:
  ```java
  public interface LLMProvider {
      String generate(String prompt, Map<String, Object> parameters);
      boolean supportsModel(String modelName);
  }
  ```

- **Create GeminiProvider.java**:
  ```java
  @Component
  public class GeminiProvider implements LLMProvider {
      @Override
      public String generate(String prompt, Map<String, Object> parameters) {
          // Implement Gemini API calls
      }
  }
  ```

- **Create QwenProvider.java**:
  ```java
  @Component
  public class QwenProvider implements LLMProvider {
      @Override
      public String generate(String prompt, Map<String, Object> parameters) {
          // Implement Qwen2.5-7b API calls
      }
  }
  ```

## Phase 4: Frontend Synchronization (Weeks 11-14)

### 4.1 Create Unified State Management (Week 11)

**Objective**: Create centralized state management for perfect synchronization between day-by-day and workflow views.

**Tasks**:
- **Create UnifiedItineraryContext.tsx**:
  ```typescript
  export const UnifiedItineraryContext = createContext<{
    itinerary: NormalizedItinerary | null;
    updateNode: (nodeId: string, updates: Partial<NormalizedNode>) => void;
    updateDay: (dayNumber: number, updates: Partial<NormalizedDay>) => void;
    processWithAgents: (nodeId: string, agentIds: string[]) => Promise<void>;
    rollbackToRevision: (revisionId: string) => Promise<void>;
  }>({});

  export const UnifiedItineraryProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [itinerary, setItinerary] = useState<NormalizedItinerary | null>(null);
    const [isUpdating, setIsUpdating] = useState(false);
    
    const updateNode = useCallback(async (nodeId: string, updates: Partial<NormalizedNode>) => {
      if (!itinerary || isUpdating) return;
      
      setIsUpdating(true);
      try {
        // Update node in itinerary
        const updatedItinerary = updateNodeInItinerary(itinerary, nodeId, updates);
        
        // Save to backend
        await apiClient.updateItinerary(updatedItinerary);
        
        // Update local state
        setItinerary(updatedItinerary);
      } finally {
        setIsUpdating(false);
      }
    }, [itinerary, isUpdating]);
    
    return (
      <UnifiedItineraryContext.Provider value={{
        itinerary,
        updateNode,
        updateDay,
        processWithAgents,
        rollbackToRevision
      }}>
        {children}
      </UnifiedItineraryContext.Provider>
    );
  };
  ```

### 4.2 Update Day-by-Day View for Unified Structure (Week 12)

**Objective**: Update the day-by-day view to work with the unified structure and provide real-time synchronization.

**Tasks**:
- **Update DayByDayView.tsx**:
  ```typescript
  export const DayByDayView: React.FC<{ itinerary: NormalizedItinerary }> = ({ itinerary }) => {
    const { updateNode, processWithAgents } = useUnifiedItinerary();
    
    const handleNodeUpdate = async (nodeId: string, updates: Partial<NormalizedNode>) => {
      await updateNode(nodeId, updates);
    };
    
    const handleAgentProcess = async (nodeId: string, agentId: string) => {
      await processWithAgents(nodeId, [agentId]);
    };
    
    return (
      <div className="space-y-4">
        {itinerary.days.map(day => (
          <DayCard 
            key={day.dayNumber} 
            day={day}
            onNodeUpdate={handleNodeUpdate}
            onAgentProcess={handleAgentProcess}
          />
        ))}
      </div>
    );
  };
  ```

### 4.3 Update Workflow View for Unified Structure (Week 13)

**Objective**: Update the workflow view to work with the unified structure and provide bidirectional synchronization.

**Tasks**:
- **Update WorkflowBuilder.tsx**:
  ```typescript
  export const WorkflowBuilder: React.FC<{ itinerary: NormalizedItinerary }> = ({ itinerary }) => {
    const { updateNode } = useUnifiedItinerary();
    
    const handleNodeUpdate = async (nodeId: string, updates: Partial<WorkflowNode>) => {
      // Convert workflow updates to node updates
      const nodeUpdates = convertWorkflowToNodeUpdates(updates);
      await updateNode(nodeId, nodeUpdates);
    };
    
    const handleNodeDrag = async (nodeId: string, position: { x: number; y: number }) => {
      // Update workflow position
      await updateNode(nodeId, {
        workflow: {
          position: position
        }
      });
    };
    
    return (
      <ReactFlow
        nodes={convertToWorkflowNodes(itinerary)}
        edges={convertToWorkflowEdges(itinerary)}
        onNodeClick={handleNodeClick}
        onNodeDrag={handleNodeDrag}
      />
    );
  };
  ```

### 4.4 Implement Real-time Synchronization (Week 14)

**Objective**: Implement real-time synchronization between views using WebSocket connections.

**Tasks**:
- **Create WebSocketService.ts**:
  ```typescript
  export class WebSocketService {
    private ws: WebSocket | null = null;
    private listeners: Map<string, Function[]> = new Map();
    
    connect(itineraryId: string) {
      this.ws = new WebSocket(`ws://localhost:8080/ws/itinerary/${itineraryId}`);
      
      this.ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        this.notifyListeners(message.type, message.data);
      };
    }
    
    subscribe(eventType: string, callback: Function) {
      if (!this.listeners.has(eventType)) {
        this.listeners.set(eventType, []);
      }
      this.listeners.get(eventType)!.push(callback);
    }
    
    private notifyListeners(eventType: string, data: any) {
      const callbacks = this.listeners.get(eventType) || [];
      callbacks.forEach(callback => callback(data));
    }
  }
  ```

- **Create WebSocketController.java**:
  ```java
  @RestController
  public class WebSocketController {
      @MessageMapping("/itinerary/{itineraryId}")
      public void handleItineraryUpdate(@DestinationVariable String itineraryId, ItineraryUpdateMessage message) {
          // Broadcast update to all connected clients
          messagingTemplate.convertAndSend("/topic/itinerary/" + itineraryId, message);
      }
  }
  ```

## Phase 5: Testing & Quality Assurance (Weeks 15-16)

### 5.1 Comprehensive Testing (Week 15)

**Objective**: Conduct thorough testing of all components and integrations.

**Tasks**:
- **Unit Tests**:
  - Test all agent implementations
  - Test data structure transformations
  - Test API integrations
  - Test revision system

- **Integration Tests**:
  - Test agent orchestration
  - Test frontend-backend synchronization
  - Test real-time updates
  - Test rollback functionality

- **End-to-End Tests**:
  - Test complete user workflows
  - Test chat-driven modifications
  - Test booking flows
  - Test data consistency

### 5.2 Performance Optimization (Week 16)

**Objective**: Optimize system performance and address any bottlenecks.

**Tasks**:
- **Backend Optimization**:
  - Optimize database queries
  - Implement caching strategies
  - Optimize API response times
  - Implement rate limiting

- **Frontend Optimization**:
  - Optimize component rendering
  - Implement virtual scrolling
  - Optimize state updates
  - Implement lazy loading

## Phase 6: Deployment & Monitoring (Weeks 17-18)

### 6.1 Production Deployment (Week 17)

**Objective**: Deploy the unified system to production with zero downtime.

**Tasks**:
- **Deployment Strategy**:
  - Blue-green deployment
  - Database migration scripts
  - Feature flags for gradual rollout
  - Rollback procedures

- **Monitoring Setup**:
  - Application performance monitoring
  - Error tracking and alerting
  - User analytics
  - System health checks

### 6.2 Post-Deployment Monitoring (Week 18)

**Objective**: Monitor system performance and user feedback.

**Tasks**:
- **Performance Monitoring**:
  - Track response times
  - Monitor error rates
  - Track user engagement
  - Monitor agent performance

- **User Feedback**:
  - Collect user feedback
  - Analyze usage patterns
  - Identify improvement opportunities
  - Plan future enhancements

## API Integration Specifications

### Google Places API
- **Endpoint**: `https://maps.googleapis.com/maps/api/place/details/json`
- **Required Fields**: `place_id`, `fields=photos,reviews,opening_hours,price_level,rating`
- **Rate Limits**: 1000 requests/day (free), 100,000 requests/day (paid)
- **Authentication**: API key in request header

### Booking.com Partner API
- **Endpoint**: `https://distribution-xml.booking.com/2.5/json`
- **Required Fields**: `hotel_ids`, `checkin`, `checkout`, `room1`
- **Rate Limits**: 1000 requests/day
- **Authentication**: API key in request header

### Expedia Rapid API
- **Endpoint**: `https://rapidapi.com/apidojo/api/expedia`
- **Required Fields**: `destination`, `checkin`, `checkout`, `rooms`
- **Rate Limits**: 1000 requests/day
- **Authentication**: X-RapidAPI-Key header

### Razorpay API
- **Endpoint**: `https://api.razorpay.com/v1/`
- **Required Fields**: `amount`, `currency`, `receipt`
- **Rate Limits**: 1000 requests/minute
- **Authentication**: Basic auth with API key and secret

## Success Metrics

### Technical Metrics
- **Sync Accuracy**: 100% synchronization between views
- **Agent Efficiency**: 50% reduction in agent processing time
- **Data Consistency**: Zero data inconsistencies
- **Versioning**: Complete change tracking and rollback capability
- **Performance**: <200ms response time for all operations

### Business Metrics
- **User Satisfaction**: >90% user satisfaction score
- **Agent Productivity**: 30% increase in agent productivity
- **System Reliability**: 99.9% uptime
- **Error Rate**: <0.1% error rate
- **Booking Success**: >95% booking success rate

## Risk Mitigation

### Technical Risks
- **Data Loss**: Comprehensive backup and rollback procedures
- **Performance Issues**: Load testing and optimization
- **Integration Failures**: Fallback mechanisms and error handling
- **Security Vulnerabilities**: Security audits and penetration testing

### Business Risks
- **User Adoption**: Comprehensive training and documentation
- **Agent Resistance**: Change management and training programs
- **API Rate Limits**: Caching and request optimization
- **Cost Overruns**: Budget monitoring and cost optimization

## Conclusion

This comprehensive roadmap provides a structured approach to implementing the unified itinerary system. By following this plan, we will achieve:

1. **Perfect Synchronization**: Between day-by-day and workflow views
2. **Efficient Agent Operations**: With dedicated data sections and clear responsibilities
3. **Comprehensive Versioning**: With complete change tracking and rollback capability
4. **Real API Integration**: With Booking.com, Expedia, Google Places, and Razorpay
5. **Clean Architecture**: With no legacy fallbacks or duplicate systems

The implementation follows a phased approach with clear milestones and success metrics, ensuring a smooth transition to the new unified system while maintaining system stability and user experience.

