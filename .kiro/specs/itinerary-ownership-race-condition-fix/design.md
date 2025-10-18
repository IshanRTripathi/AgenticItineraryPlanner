# Design Document

## Overview

This design addresses the critical issues identified in the travel planner application through targeted fixes for ownership race conditions, AI service reliability, Spring Boot configuration, and error handling. The solution focuses on synchronous ownership establishment, AI provider fallback mechanisms, proper async executor configuration, and enhanced user experience.

## Architecture

### Current Architecture Issues
- **Async Ownership Creation**: Trip metadata is saved asynchronously after API response, causing race conditions
- **Single AI Provider**: System relies on OpenRouter without fallback mechanisms
- **Multiple TaskExecutors**: Spring Boot creates multiple unnamed async executors causing conflicts
- **Poor Error Handling**: Frontend shows technical errors instead of user-friendly messages

### Proposed Architecture Changes
- **Synchronous Ownership**: Move trip metadata creation to synchronous flow before API response
- **AI Provider Chain**: Implement provider fallback chain with automatic switching
- **Unified Task Executor**: Configure single primary TaskExecutor with proper naming
- **Layered Error Handling**: Implement error handling at service, controller, and frontend layers

## Components and Interfaces

### 1. Ownership Management Component

#### Modified AgentOrchestrator
```java
@Service
public class AgentOrchestrator {
    
    // BEFORE: Async ownership creation
    @Async
    public void generateNormalizedItinerary(String itineraryId, CreateItineraryReq request, String userId) {
        // ... create itinerary
        userDataService.saveUserTripMetadata(userId, tripMetadata); // Async - causes race condition
        Thread.sleep(3000); // Wait for frontend
        // ... continue with agents
    }
    
    // AFTER: Synchronous ownership creation
    public NormalizedItinerary createInitialItinerary(String itineraryId, CreateItineraryReq request, String userId) {
        // Create initial itinerary structure
        NormalizedItinerary initialItinerary = createInitialNormalizedItinerary(itineraryId, request, userId);
        
        // Save to ItineraryJsonService (single source of truth)
        itineraryJsonService.createItinerary(initialItinerary);
        
        // SYNCHRONOUSLY save trip metadata to establish ownership
        TripMetadata tripMetadata = new TripMetadata(request, initialItinerary);
        userDataService.saveUserTripMetadata(userId, tripMetadata);
        
        return initialItinerary;
    }
    
    @Async
    public void generateItineraryAsync(String itineraryId, CreateItineraryReq request, String userId) {
        // Run agents asynchronously after ownership is established
        Thread.sleep(3000); // Wait for frontend SSE connection
        // ... run agents
    }
}
```

#### Modified ItineraryService
```java
@Service
public class ItineraryService {
    
    public ItineraryDto create(CreateItineraryReq request, String userId) {
        String itineraryId = generateItineraryId();
        
        // SYNCHRONOUSLY create initial itinerary and establish ownership
        NormalizedItinerary initialItinerary = agentOrchestrator.createInitialItinerary(itineraryId, request, userId);
        
        // Convert to DTO for response
        ItineraryDto response = convertToDto(initialItinerary, request);
        
        // Start async agent processing AFTER ownership is established
        agentOrchestrator.generateItineraryAsync(itineraryId, request, userId);
        
        return response;
    }
}
```

### 2. AI Provider Resilience Component

#### AI Provider Chain Configuration
```java
@Configuration
public class AiClientConfig {
    
    @Bean
    @Primary
    public AiClient aiClient(ObjectProvider<GeminiClient> geminiClientProvider,
                            ObjectProvider<OpenRouterClient> openRouterClientProvider) {
        return new ResilientAiClient(
            Arrays.asList(
                openRouterClientProvider.getIfAvailable(),
                geminiClientProvider.getIfAvailable()
            )
        );
    }
}
```

#### Resilient AI Client Implementation
```java
public class ResilientAiClient implements AiClient {
    private final List<AiClient> providers;
    private final Logger logger = LoggerFactory.getLogger(ResilientAiClient.class);
    
    @Override
    public String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt) {
        for (int i = 0; i < providers.size(); i++) {
            AiClient provider = providers.get(i);
            if (!provider.isAvailable()) {
                logger.warn("AI provider {} is not available, skipping", provider.getClass().getSimpleName());
                continue;
            }
            
            try {
                logger.info("Attempting AI generation with provider: {}", provider.getClass().getSimpleName());
                String response = provider.generateStructuredContent(userPrompt, jsonSchema, systemPrompt);
                
                if (response != null && !response.trim().isEmpty() && !response.trim().equals("{}")) {
                    logger.info("AI provider {} returned valid response ({} chars)", 
                               provider.getClass().getSimpleName(), response.length());
                    return response;
                } else {
                    logger.warn("AI provider {} returned empty/invalid response: '{}'", 
                               provider.getClass().getSimpleName(), response);
                }
            } catch (Exception e) {
                logger.error("AI provider {} failed: {}", provider.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        
        throw new RuntimeException("All AI providers failed to generate content. Please check provider configurations and try again.");
    }
    
    @Override
    public boolean isAvailable() {
        return providers.stream().anyMatch(AiClient::isAvailable);
    }
}
```

### 3. Task Executor Configuration Component

#### Unified Async Configuration
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Bean(name = "taskExecutor")
    @Primary
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("app-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
```

#### WebSocket Executor Configuration
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Bean(name = "webSocketTaskExecutor")
    public TaskExecutor webSocketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("websocket-");
        executor.initialize();
        return executor;
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
              .setTaskExecutor(webSocketTaskExecutor());
        config.setApplicationDestinationPrefixes("/app");
    }
}
```

### 4. Enhanced Error Handling Component

#### Frontend API Client Improvements
```typescript
class ApiClient {
    async getItinerary(id: string, retryOptions?: { maxRetries?: number; retryDelay?: number }): Promise<TripData> {
        const maxRetries = retryOptions?.maxRetries || 5;
        const baseDelay = retryOptions?.retryDelay || 1000;
        
        for (let attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                const response = await this.request<NormalizedItinerary>(`/itineraries/${id}/json`);
                return NormalizedDataTransformer.transformNormalizedItineraryToTripData(response);
            } catch (error) {
                if (error.message.includes('404') && attempt < maxRetries) {
                    // Exponential backoff for 404 errors during generation
                    const delay = baseDelay * Math.pow(2, attempt - 1);
                    console.log(`Itinerary not ready, retrying in ${delay}ms (attempt ${attempt}/${maxRetries})`);
                    await new Promise(resolve => setTimeout(resolve, delay));
                    continue;
                }
                throw error;
            }
        }
        
        throw new Error('Itinerary generation timed out. Please try refreshing the page.');
    }
}
```

#### Backend Error Response Enhancement
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        if (e.getMessage().contains("AI client returned empty response")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("GENERATION_IN_PROGRESS", 
                    "Your itinerary is being generated. Please wait a moment and try again."));
        }
        
        if (e.getMessage().contains("does not own itinerary")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ITINERARY_NOT_READY", 
                    "Your itinerary is still being prepared. Please wait a moment."));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred. Please try again."));
    }
}
```

## Data Models

### Enhanced Error Response Model
```java
public class ErrorResponse {
    private String code;
    private String message;
    private String details;
    private long timestamp;
    
    // constructors, getters, setters
}
```

### AI Provider Status Model
```java
public class AiProviderStatus {
    private String providerName;
    private boolean available;
    private String lastError;
    private long lastSuccessTime;
    
    // constructors, getters, setters
}
```

## Error Handling

### Service Layer Error Handling
- **AI Provider Failures**: Automatic fallback to next provider in chain
- **Database Failures**: Proper transaction rollback and retry logic
- **Timeout Handling**: Configurable timeouts with graceful degradation

### Controller Layer Error Handling
- **User-Friendly Messages**: Convert technical errors to user-friendly responses
- **Appropriate HTTP Status Codes**: Use correct status codes for different error types
- **Structured Error Responses**: Consistent error response format

### Frontend Error Handling
- **Retry Logic**: Intelligent retry with exponential backoff
- **Loading States**: Show progress indicators during generation
- **Error Recovery**: Provide clear recovery options for users

## Testing Strategy

### Unit Tests
- **Ownership Race Condition**: Test synchronous vs asynchronous ownership creation
- **AI Provider Fallback**: Test provider chain with mock failures
- **Error Handling**: Test all error scenarios and response formats

### Integration Tests
- **End-to-End Flow**: Test complete itinerary creation flow
- **AI Provider Integration**: Test with real AI providers
- **Database Integration**: Test ownership validation with real database

### Performance Tests
- **Concurrent Users**: Test multiple users creating itineraries simultaneously
- **AI Provider Load**: Test AI provider performance under load
- **Database Performance**: Test ownership queries under load

## Implementation Priority

### Phase 1: Critical Fixes (High Priority)
1. Fix ownership race condition by making trip metadata creation synchronous
2. Implement AI provider fallback mechanism
3. Configure unified TaskExecutor to eliminate warnings

### Phase 2: Enhanced Error Handling (Medium Priority)
1. Implement frontend retry logic with exponential backoff
2. Add comprehensive error handling in backend
3. Improve user feedback during itinerary generation

### Phase 3: Monitoring and Observability (Low Priority)
1. Add detailed logging for debugging
2. Implement health checks for AI providers
3. Add metrics for error rates and response times