# LLM Streaming Implementation - Investigation Task List

**Goal**: Add LLM response streaming with real-time token display and "thinking" process visibility

**Technologies**: Spring Boot SSE, React EventSource, OpenRouter/Gemini streaming APIs

---

## Phase 1: Research & Architecture Analysis (3-4 hours)

### 1.1 LLM Streaming API Research
- [ ] **Read OpenRouter streaming documentation**
  - API endpoint: `POST /chat/completions` with `stream: true`
  - Response format: Server-Sent Events (SSE) with `data:` prefix
  - Termination signal: `data: [DONE]`
  - Token format: JSON chunks with `delta.content`
  
- [ ] **Read Gemini streaming documentation**
  - API: `generateContent?alt=sse` OR `streamGenerateContent`
  - Response format: Same SSE protocol
  - Thoughts tokens: Check if Gemini supports "thinking" tokens separately
  
- [ ] **Compare streaming patterns**
  - OpenAI-compatible: `{stream: true, ...}` → SSE chunks
  - Gemini-specific: Separate thoughts vs content tokens?
  - Error handling during stream
  - Partial response recovery

### 1.2 Java SSE Implementation Research
- [ ] **Spring Boot SSE options** (2 approaches):
  1. **SseEmitter** (blocking, servlet-based)
     - Simple API: `SseEmitter emitter = new SseEmitter()`
     - Works with current Spring MVC setup (Java 17)
     - Scales with Virtual Threads (if enabled)
  2. **WebFlux Flux** (reactive, non-blocking)
     - Requires Spring WebFlux dependency
     - Better for high concurrency
     - More complex (reactive programming)
  
- [ ] **Decision**: Recommend **SseEmitter** (simpler, works with current stack)

### 1.3 Current Architecture Analysis
- [x] **Reviewed**: `OpenRouterClient.java` (uses blocking HttpClient)
- [x] **Reviewed**: `GeminiClient.java` (uses blocking HttpClient)
- [ ] **Identify changes needed**:
  - Add streaming methods to `AiClient` interface
  - Create `StreamingResponse` callback interface
  - Handle backpressure (client slower than LLM)
  - Error recovery (stream interrupted mid-response)

---

## Phase 2: Backend Implementation (4-6 hours)

### 2.1 Update AI Client Interface
- [ ] **Modify `AiClient.java`**:
  ```java
  public interface AiClient {
      // Existing methods...
      String generateContent(String userPrompt, String systemPrompt);
      String generateStructuredContent(String prompt, String schema, String systemPrompt);
      
      // NEW: Streaming methods
      void generateContentStreaming(String userPrompt, String systemPrompt, StreamingCallback callback);
      void generateStructuredContentStreaming(String prompt, String schema, String systemPrompt, StreamingCallback callback);
  }
  
  public interface StreamingCallback {
      void onToken(String token);          // Token-by-token delivery
      void onThought(String thought);      // "Thinking" process (if available)
      void onComplete(String fullResponse);// Stream completed
      void onError(Exception error);        // Stream failed
  }
  ```

### 2.2 Implement OpenRouter Streaming
- [ ] **Create `OpenRouterStreamingClient.java`**:
  ```java
  public class OpenRouterStreamingClient {
      public void streamChatCompletion(String prompt, StreamingCallback callback) {
          String requestBody = buildStreamingPayload(prompt);
          
          HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/chat/completions"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();
          
          // Use BodyHandlers.ofLines() to stream line-by-line
          HttpResponse<Stream<String>> response = httpClient.send(
              request, 
              HttpResponse.BodyHandlers.ofLines()
          );
          
          response.body().forEach(line -> {
              if (line.startsWith("data: ")) {
                  String data = line.substring(6); // Remove "data: " prefix
                  
                  if ("[DONE]".equals(data)) {
                      callback.onComplete(fullResponse.toString());
                      return;
                  }
                  
                  JsonNode chunk = objectMapper.readTree(data);
                  String token = chunk.path("choices").get(0)
                                       .path("delta").path("content").asText("");
                  
                  if (!token.isEmpty()) {
                      callback.onToken(token);
                  }
              }
          });
      }
  }
  ```

- [ ] **Key considerations**:
  - **Timeout handling**: Streaming can take 60-120+ seconds
  - **Buffer size**: Prevent memory overflow on large responses
  - **Error recovery**: Handle partial streams
  - **API key rotation**: Switch key on stream failure

### 2.3 Implement Gemini Streaming
- [ ] **Create `GeminiStreamingClient.java`**:
  - Similar pattern to OpenRouter
  - Check if Gemini has separate "thoughts" stream
  - Handle Gemini-specific SSE format

### 2.4 Create SSE Controller Endpoint
- [ ] **Create `StreamingChatController.java`**:
  ```java
  @RestController
  @RequestMapping("/api/v1/chat/stream")
  public class StreamingChatController {
      
      @PostMapping
      public SseEmitter streamChatResponse(@RequestBody ChatRequest request) {
          SseEmitter emitter = new SseEmitter(120_000L); // 120 second timeout
          
        // Run LLM call in separate thread
          CompletableFuture.runAsync(() -> {
              try {
                  aiClient.generateContentStreaming(
                      request.getText(),
                      buildSystemPrompt(),
                      new StreamingCallback() {
                          @Override
                          public void onToken(String token) {
                              try {
                                  emitter.send(SseEmitter.event()
                                      .name("token")
                                      .data(token));
                              } catch (IOException e) {
                                  emitter.completeWithError(e);
                              }
                          }
                          
                          @Override
                          public void onThought(String thought) {
                              try {
                                  emitter.send(SseEmitter.event()
                                      .name("thought")
                                      .data(thought));
                              } catch (IOException e) {
                                  emitter.completeWithError(e);
                              }
                          }
                          
                          @Override
                          public void onComplete(String fullResponse) {
                              emitter.send(SseEmitter.event().name("done").data(fullResponse));
                              emitter.complete();
                          }
                          
                          @Override
                          public void onError(Exception error) {
                              emitter.completeWithError(error);
                          }
                      }
                  );
              } catch (Exception e) {
                  emitter.completeWithError(e);
              }
          });
          
          return emitter;
      }
  }
  ```

- [ ] **Add connection management**:
  - Track active SSE connections
  - Cleanup on client disconnect
  - Prevent resource leaks

### 2.5 Error Handling & Resilience
- [ ] **Stream interruption handling**:
  - Client disconnected: Detect `IOException` on `emitter.send()`
  - LLM stream failed: Fallback to non-streaming response
  - Timeout: Close stream gracefully after 120 seconds
  
- [ ] **Circuit breaker integration**:
  - Track streaming failures separately
  - Don't penalize API key for client disconnects

---

## Phase 3: Frontend Implementation (3-5 hours)

### 3.1 Create React Streaming Hook
- [ ] **Create `useStreamingChat.ts`**:
  ```typescript
  export interface StreamingMessage {
      tokens: string[];      // Accumulated tokens
      thoughts: string[];    // "Thinking" process
      fullText: string;      // Combined text
      isStreaming: boolean;  // Currently receiving
      error: string | null;  //Stream error
  }
  
  export function useStreamingChat(itineraryId: string) {
      const [message, setMessage] = useState<StreamingMessage>({
          tokens: [],
          thoughts: [],
          fullText: '',
          isStreaming: false,
          error: null
      });
      
      const sendMessage = useCallback((text: string) => {
          const eventSource = new EventSource(
              `/api/v1/chat/stream?itineraryId=${itineraryId}&text=${encodeURIComponent(text)}`
          );
          
          setMessage(prev => ({ ...prev, isStreaming: true, tokens: [], thoughts: [] }));
          
          eventSource.addEventListener('token', (event) => {
              const token = event.data;
              setMessage(prev => ({
                  ...prev,
                  tokens: [...prev.tokens, token],
                  fullText: prev.fullText + token
              }));
          });
          
          eventSource.addEventListener('thought', (event) => {
              const thought = event.data;
              setMessage(prev => ({
                  ...prev,
                  thoughts: [...prev.thoughts, thought]
              }));
          });
          
          eventSource.addEventListener('done', (event) => {
              eventSource.close();
              setMessage(prev => ({
                  ...prev,
                  isStreaming: false
              }));
          });
          
          eventSource.onerror = (error) => {
              console.error('SSE error:', error);
              eventSource.close();
              setMessage(prev => ({
                  ...prev,
                  isStreaming: false,
                  error: 'Stream interrupted'
              }));
          };
          
          return () => eventSource.close(); // Cleanup function
      }, [itineraryId]);
      
      return { message, sendMessage };
  }
  ```

### 3.2 Create Streaming UI Components
- [ ] **Create `StreamingChatMessage.tsx`**:
  ```typescript
  interface Props {
      message: StreamingMessage;
  }
  
  export function StreamingChatMessage({ message }: Props) {
      return (
          <div className="streaming-message">
              {/* Show thinking process (collapsible) */}
              {message.thoughts.length > 0 && (
                  <div className="thoughts-container">
                      <div className="thoughts-header">
                          <Brain size={16} />
                          <span>Thinking...</span>
                      </div>
                      <div className="thoughts-content">
                          {message.thoughts.map((thought, i) => (
                              <div key={i} className="thought-item">
                                  {thought}
                              </div>
                          ))}
                      </div>
                  </div>
              )}
              
              {/* Show streaming text with cursor */}
              <div className="message-content">
                  {message.fullText}
                  {message.isStreaming && (
                      <span className="streaming-cursor animate-pulse">▊</span>
                  )}
              </div>
          </div>
      );
  }
  ```

### 3.3 Reusable Component Architecture

**Goal**: Create context-agnostic streaming components usable in:
- ✅ Chat page (user questions)
- ✅ Planner progress page (agent generation)
- ✅ Agent execution page (real-time thoughts)
- ✅ Any LLM interaction

- [ ] **Create base `StreamingText` component** (context-agnostic):
  ```typescript
  interface StreamingTextProps {
      tokens: string[];           // Accumulated tokens
      isStreaming: boolean;       // Currently receiving
      variant?: 'chat' | 'agent' | 'inline'; // Visual style
      showCursor?: boolean;       // Animated cursor
      className?: string;         // Custom styling
  }
  
  export function StreamingText({ 
      tokens, 
      isStreaming, 
      variant = 'chat',
      showCursor = true,
      className 
  }: StreamingTextProps) {
      const text = tokens.join('');
      
      return (
          <div className={cn('streaming-text', `variant-${variant}`, className)}>
              {text}
              {isStreaming && showCursor && (
                  <span className="streaming-cursor animate-pulse">▊</span>
              )}
          </div>
      );
  }
  ```

- [ ] **Create `StreamingThoughts` component** (collapsible thoughts):
  ```typescript
  interface StreamingThoughtsProps {
      thoughts: string[];         // Agent thinking process
      isStreaming: boolean;
      variant?: 'minimal' | 'detailed';
      defaultOpen?: boolean;      // Start expanded?
  }
  
  export function StreamingThoughts({ 
      thoughts, 
      isStreaming,
      variant = 'detailed',
      defaultOpen = false
  }: StreamingThoughtsProps) {
      const [isOpen, setIsOpen] = useState(defaultOpen);
      
      if (thoughts.length === 0) return null;
      
      return (
          <div className="streaming-thoughts">
              <button 
                  onClick={() => setIsOpen(!isOpen)}
                  className="thoughts-toggle">
                  <Brain size={16} />
                  <span>
                      {isStreaming ? 'Thinking...' : `${thoughts.length} thoughts`}
                  </span>
                  <ChevronDown 
                      className={cn('transition-transform', isOpen && 'rotate-180')} 
                  />
              </button>
              
              {isOpen && (
                  <div className="thoughts-list">
                      {thoughts.map((thought, i) => (
                          <div key={i} className="thought-item fade-in">
                              {variant === 'detailed' && (
                                  <span className="thought-number">{i + 1}.</span>
                              )}
                              <span className="thought-text">{thought}</span>
                          </div>
                      ))}
                  </div>
              )}
          </div>
      );
  }
  ```

- [ ] **Create `StreamingMessage` component** (composition root):
  ```typescript
  interface StreamingMessageProps {
      message: StreamingMessage;
      variant?: 'chat' | 'agent' | 'inline';
      showThoughts?: boolean;     // Show thinking process?
      showMetadata?: boolean;     // Show token count, timing?
      className?: string;
  }
  
  export function StreamingMessage({ 
      message, 
      variant = 'chat',
      showThoughts = true,
      showMetadata = false,
      className 
  }: StreamingMessageProps) {
      return (
          <div className={cn('streaming-message', className)}>
              {/* Thoughts section (optional) */}
              {showThoughts && (
                  <StreamingThoughts 
                      thoughts={message.thoughts}
                      isStreaming={message.isStreaming}
                      variant={variant === 'agent' ? 'detailed' : 'minimal'}
                  />
              )}
              
              {/* Main text content */}
              <StreamingText 
                  tokens={message.tokens}
                  isStreaming={message.isStreaming}
                  variant={variant}
              />
              
              {/* Metadata (optional) */}
              {showMetadata && !message.isStreaming && (
                  <div className="streaming-metadata">
                      <span>{message.tokens.length} tokens</span>
                      <span>•</span>
                      <span>{message.duration}ms</span>
                  </div>
              )}
              
              {/* Error state */}
              {message.error && (
                  <div className="streaming-error">
                      <AlertCircle size={16} />
                      <span>{message.error}</span>
                  </div>
              )}
          </div>
      );
  }
  ```

- [ ] **Create context-specific wrappers**:

  **For Chat Page**:
  ```typescript
  export function ChatStreamingMessage({ message }: { message: StreamingMessage }) {
      return (
          <StreamingMessage 
              message={message}
              variant="chat"
              showThoughts={true}  // Users want to see AI thinking
              showMetadata={false} // Don't clutter chat UI
          />
      );
  }
  ```
  
  **For Planner Progress Page**:
  ```typescript
  export function AgentStreamingProgress({ agentName, message }: Props) {
      return (
          <div className="agent-progress-card">
              <div className="agent-header">
                  <Sparkles size={16} />
                  <span>{agentName}</span>
              </div>
              <StreamingMessage 
                  message={message}
                  variant="agent"
                  showThoughts={true}  // Show detailed reasoning
                  showMetadata={true}  // Show performance metrics
              />
          </div>
      );
  }
  ```
  
  **For Inline LLM Responses** (e.g., quick suggestions):
  ```typescript
  export function InlineStreamingText({ message }: { message: StreamingMessage }) {
      return (
          <StreamingMessage 
              message={message}
              variant="inline"
              showThoughts={false} // Too distracting for inline
              showMetadata={false}
          />
      );
  }
  ```

### 3.4 Usage Examples

**Example 1: Chat Page**
```typescript
function ChatPage() {
    const { message, sendMessage } = useStreamingChat(itineraryId);
    
    return (
        <div className="chat-container">
            <ChatStreamingMessage message={message} />
            <ChatInput onSend={sendMessage} />
        </div>
    );
}
```

**Example 2: Planner Progress Page**
```typescript
function PlannerProgressPage() {
    const { agentProgress } = useAgentProgress(itineraryId);
    
    return (
        <div className="planner-progress">
            {agentProgress.map(agent => (
                <AgentStreamingProgress 
                    key={agent.id}
                    agentName={agent.name}
                    message={agent.streamingMessage}
                />
            ))}
        </div>
    );
}
```

**Example 3: Real-time Agent Execution**
```typescript
function AgentExecutionView({ agentId }: Props) {
    const { thoughts, output } = useAgentStream(agentId);
    
    return (
        <div className="agent-execution">
            {/* Show thoughts in real-time */}
            <StreamingThoughts 
                thoughts={thoughts}
                isStreaming={output.isStreaming}
                variant="detailed"
                defaultOpen={true}
            />
            
            {/* Show output */}
            <StreamingText 
                tokens={output.tokens}
                isStreaming={output.isStreaming}
                variant="agent"
            />
        </div>
    );
}
```

### 3.5 Advanced: Multi-Agent Streaming

- [ ] **Support multiple simultaneous streams** (planner page use case):
  ```typescript
  export function useMultiAgentStreaming(itineraryId: string) {
      const [agentStreams, setAgentStreams] = useState<Map<string, StreamingMessage>>(new Map());
      
      useEffect(() => {
          // Subscribe to multiple agent streams
          const sources = [
              new EventSource(`/api/v1/agents/skeleton/stream?itineraryId=${itineraryId}`),
              new EventSource(`/api/v1/agents/activity/stream?itineraryId=${itineraryId}`),
              new EventSource(`/api/v1/agents/meal/stream?itineraryId=${itineraryId}`)
          ];
          
          sources.forEach((source, index) => {
              const agentName = ['SkeletonPlanner', 'ActivityAgent', 'MealAgent'][index];
              
              source.addEventListener('token', (event) => {
                  setAgentStreams(prev => {
                      const updated = new Map(prev);
                      const current = updated.get(agentName) || createEmptyMessage();
                      updated.set(agentName, {
                          ...current,
                          tokens: [...current.tokens, event.data]
                      });
                      return updated;
                  });
              });
          });
          
          return () => sources.forEach(s => s.close());
      }, [itineraryId]);
      
      return agentStreams;
  }
  ```

### 3.6 UX Enhancements
- [ ] **Token batching** (improve performance):
  - Batch tokens every 50ms instead of per-token render
  - Reduces React re-renders from 500+ to ~20 per response
  
- [ ] **Smooth scrolling**:
  - Auto-scroll to bottom as tokens arrive
  - Stop auto-scroll if user scrolls up
  
- [ ] **Typing animation**:
  - Optional delay between tokens (ChatGPT-style)
  - Or instant append (faster, less distracting)

---

## Phase 4: Integration & Testing (2-3 hours)

### 4.1 Backend Testing
- [ ] **Unit tests**:
  - Test SSE emitter lifecycle
  - Test token chunking
  - Test error scenarios (timeout, disconnect)
  
- [ ] **Integration tests**:
  - Mock OpenRouter streaming response
  - Verify all event types emitted
  - Test concurrent streams (multiple users)

### 4.2 Frontend Testing
- [ ] **Component testing**:
  - Test EventSource connection
  - Test token accumulation
  - Test cleanup on unmount
  
- [ ] **E2E testing**:
  - Send message → see streaming response
  - Interrupt stream → verify graceful handling
  - Multiple messages → verify state management

### 4.3 Performance Testing
- [ ] **Metrics to measure**:
  - Time to first token (TTFbT): Should be <1 second
  - Tokens per second: Aim for 30-50 tokens/sec
  - Memory usage: Verify no memory leaks on long streams
  - Concurrent streams: Test 10+ simultaneous users

---

## Phase 5: UX Refinements (2-3 hours)

### 5.1 Visual Feedback
- [ ] **Loading states**:
  - "Connecting to AI..." (before stream starts)
  - "Thinking..." (during stream)
  - Pulsing cursor at end of text
  
- [ ] **Thoughts disclosure** (optional):
  - Collapsible "Show thinking process" section
  - Different styling for thoughts vs content
  - Icons: 🧠 for thoughts, 💬 for content

### 5.2 Error UX
- [ ] **Graceful degradation**:
  - Stream fails → show error + offer "Try again" button
  - Network offline → automatically retry when back online
  - Timeout → show partial results + "Response truncated" message

### 5.3 Accessibility
- [ ] **Screen reader support**:
  - Announce "AI is responding..."
  - Live region for streaming text
  - Announce "Response complete"
  
- [ ] **Keyboard navigation**:
  - Focus management during stream
  - Escape key → stop stream

---

## Phase 6: Deployment Considerations (1-2 hours)

### 6.1 Backend Configuration
- [ ] **application.yml updates**:
  ```yaml
  streaming:
    enabled: ${STREAMING_ENABLED:true}
    timeout-seconds: ${STREAMING_TIMEOUT:120}
    max-concurrent-streams: ${MAX_CONCURRENT_STREAMS:100}
  ```

### 6.2 Infrastructure
- [ ] **Cloud Run configuration**:
  - Increase timeout: 300 seconds (default 60s)
  - Verify SSE support (should work natively)
  - Test with load balancer (some LBs buffer SSE)
  
- [ ] **Monitoring**:
  - Track active SSE connections (Prometheus metric)
  - Alert on high stream failure rate
  - Alert on timeout rate

---

## Summary & Recommendations

### Preferred Tech Stack
- **Backend**: Spring Boot `SseEmitter` (simple, works with Java 17)
- **Frontend**: React `EventSource` API + custom hook
- **LLM**: OpenRouter streaming (primary), Gemini (fallback)

### Implementation Priority
1. **Phase 2.2** - OpenRouter streaming (biggest value)
2. **Phase 3.1-3.2** - Frontend streaming hook + UI
3. **Phase 2.4** - SSE controller endpoint
4. **Phase 5.1** - UX polish (thoughts, loading states)
5. **Phase 2.3** - Gemini streaming (nice-to-have)

### Estimated Timeline
- **MVP** (basic streaming): 8-10 hours
- **Full implementation** (with thoughts, UX): 15-20 hours
- **Production-ready** (testing, monitoring): 20-25 hours

### Success Criteria
✅ Tokens appear within 1 second of sending message  
✅ Streaming is smooth (30-50 tokens/sec)  
✅ Errors handled gracefully (fallback to non-streaming)  
✅ No memory leaks on long sessions  
✅ Accessible (screen reader friendly)  

---

**Next Step**: Review this task list, then start with Phase 1.1 (API documentation research) and Phase 1.3 (architecture analysis).
