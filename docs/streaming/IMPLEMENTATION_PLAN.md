# LLM Streaming Implementation Plan
## Complete End-to-End Guide with Multi-Agent Support

**Project**: Add real-time streaming LLM responses with multi-agent visualization  
**Status**: Planning Complete, Ready for Implementation  
**Estimated Effort**: 20-25 hours (production-ready)

---

## Table of Contents

1. [Overview & Features](#1-overview--features)
2. [Architecture Decisions](#2-architecture-decisions)
3. [Files to Create/Modify](#3-files-to-createmodify)
4. [Backend Implementation](#4-backend-implementation)
5. [Frontend Implementation](#5-frontend-implementation)
6. [Multi-Agent UX](#6-multi-agent-ux)
7. [Implementation Roadmap](#7-implementation-roadmap)

---

## 1. Overview & Features

### 1.1 What We're Building

A **production-grade streaming LLM interface** that shows:
- ✅ Real-time token-by-token responses (ChatGPT-style)
- ✅ Agent "thinking" process visibility
- ✅ Multiple agents streaming simultaneously
- ✅ Clear agent identification (icons, colors, names)
- ✅ Collapsible thought/output sections
- ✅ Reusable across chat, planner, and agent execution pages

### 1.2 Use Cases

| Use Case | Components Used | Agents |
|----------|----------------|--------|
| **Chat Page** | `ChatStreamingMessage` | EditorAgent (1) |
| **Planner Progress** | `MultiAgentStreamingDashboard` | 5+ agents (parallel) |
| **Agent Execution** | `AgentStreamingCard` | Any single agent |
| **Inline Suggestions** | `InlineStreamingText` | Any agent |

### 1.3 User Experience

**Before** (current):
- User sends message → waits 10-30 seconds → sees complete response
- No visibility into AI thinking process
- All-or-nothing experience

**After** (with streaming):
- User sends message → sees response within 1 second
- Token-by-token appearance (like ChatGPT)
- Can see agent reasoning in real-time
- Multiple agents visible on planner page

---

## 2. Architecture Decisions

### 2.1 Technology Stack

| Layer | Choice | Rationale |
|-------|--------|-----------|
| **Backend Streaming** | Spring Boot `SseEmitter` | Simple, works with Java 17, scales with Virtual Threads |
| **Frontend Client** | React `EventSource` API | Native browser support, auto-reconnect |
| **State Management** | Custom hooks + Zustand | Reusable, performant |
| **Animations** | Framer Motion | Smooth, production-quality |
| **LLM Integration** | OpenRouter + Gemini streaming | Both support SSE natively |

### 2.2 Why SSE over WebSocket?

**Server-Sent Events (SSE)**:
- ✅ Unidirectional (perfect for LLM → frontend)
- ✅ Auto-reconnect built-in
- ✅ Simpler protocol (just HTTP)
- ✅ Works with existing Spring MVC
- ✅ Better for streaming text

**WebSocket** (not chosen):
- ❌ Bidirectional (unnecessary overhead)
- ❌ More complex protocol
- ❌ Manual reconnect logic needed

### 2.3 Data Flow

```
┌─────────────┐
│   OpenRouter│──┐
│   / Gemini  │  │ LLM streams tokens via SSE
└─────────────┘  │
                 ↓
┌─────────────────────────────────────────┐
│  Spring Boot Backend                    │
│  ┌───────────────┐    ┌──────────────┐ │
│  │ LLM Client    │ →  │ BaseAgent    │ │
│  │ (streaming)   │    │ (streaming)  │ │
│  └───────────────┘    └──────────────┘ │
│           ↓                   ↓         │
│  ┌────────────────────────────────────┐ │
│  │ StreamingChatController (SSE)      │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
                 ↓ SSE events with agent metadata
┌─────────────────────────────────────────┐
│  React Frontend                         │
│  ┌────────────────────────────────────┐ │
│  │ useStreamingChat() hook            │ │
│  │ useMultiAgentStreaming() hook      │ │
│  └────────────────────────────────────┘ │
│           ↓                             │
│  ┌────────────────────────────────────┐ │
│  │ StreamingMessage components        │ │
│  │ AgentStreamingCard components      │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
                 ↓
         👤 User sees streaming text
```

---

## 3. Files to Create/Modify

### 3.1 Backend Files

#### New Files (7)

| File | Purpose | Lines |
|------|---------|-------|
| `StreamingCallback.java` | Callback interface for LLM streaming | ~30 |
| `StreamingChatController.java` | SSE endpoint for chat streaming | ~150 |
| `AgentStreamEvent.java` | DTO for agent streaming events | ~80 |
| `OpenRouterStreamingClient.java` | OpenRouter SSE client | ~200 |
| `GeminiStreamingClient.java` | Gemini SSE client | ~200 |
| `StreamingService.java` | Orchestration service | ~100 |
| `AgentUIConfig.java` | Agent metadata (names, icons, colors) | ~50 |

**Total new backend code**: ~810 lines

#### Modified Files (3)

| File | Changes | Lines Modified |
|------|---------|----------------|
| `AiClient.java` | Add streaming methods | +20 |
| `BaseAgent.java` | Add `executeWithStreaming()`, emit events | +100 |
| `application.yml` | Streaming configuration | +10 |

**Total backend modifications**: ~130 lines

### 3.2 Frontend Files

#### New Files (10)

| File | Purpose | Lines |
|------|---------|-------|
| `hooks/useStreamingChat.ts` | Chat streaming hook | ~120 |
| `hooks/useMultiAgentStreaming.ts` | Multi-agent streaming hook | ~150 |
| `hooks/useAgentStream.ts` | Single agent streaming hook | ~100 |
| `components/streaming/StreamingText.tsx` | Base streaming text component | ~80 |
| `components/streaming/StreamingThoughts.tsx` | Collapsible thoughts component | ~120 |
| `components/streaming/StreamingMessage.tsx` | Composition root component | ~150 |
| `components/streaming/AgentStreamingCard.tsx` | Agent card with streaming | ~250 |
| `components/streaming/MultiAgentDashboard.tsx` | Multi-agent container | ~200 |
| `components/streaming/PhaseProgressBar.tsx` | Pipeline phase indicator | ~80 |
| `config/agentConfig.ts` | Agent UI metadata | ~100 |

**Total new frontend code**: ~1,350 lines

#### Modified Files (4)

| File | Changes | Lines Modified |
|------|---------|----------------|
| `services/chatApi.ts` | Add streaming endpoint | +30 |
| `pages/ChatPage.tsx` | Integrate streaming component | +20 |
| `pages/PlannerProgressPage.tsx` | Add multi-agent dashboard | +30 |
| `styles/streaming.css` | Streaming-specific styles | +200 |

**Total frontend modifications**: ~280 lines

### 3.3 Summary

- **Total new files**: 17
- **Total modified files**: 7
- **Total new code**: ~2,160 lines
- **Total modifications**: ~410 lines
- **Grand total**: ~2,570 lines of code

---

## 4. Backend Implementation

### 4.1 Step 1: Create Streaming Callback Interface

**File**: `src/main/java/com/tripplanner/service/ai/StreamingCallback.java`

```java
package com.tripplanner.service.ai;

/**
 * Callback interface for handling streaming LLM responses.
 * Allows consumers to receive tokens, thoughts, and completion events.
 */
public interface StreamingCallback {
    
    /**
     * Called for each content token received from the LLM.
     * @param token The token content
     */
    void onToken(String token);
    
    /**
     * Called for each "thinking" token (if supported by LLM).
     * @param thought The thought process content
     */
    void onThought(String thought);
    
    /**
     * Called when the stream completes successfully.
     * @param fullResponse The complete concatenated response
     */
    void onComplete(String fullResponse);
    
    /**
     * Called if the stream encounters an error.
     * @param error The exception that occurred
     */
    void onError(Exception error);
}
```

### 4.2 Step 2: Update AiClient Interface

**File**: `src/main/java/com/tripplanner/service/ai/AiClient.java` (modify)

```java
public interface AiClient {
    // Existing methods
    String generateContent(String userPrompt, String systemPrompt);
    String generateStructuredContent(String prompt, String schema, String systemPrompt);
    
    // NEW: Streaming methods
    void generateContentStreaming(
        String userPrompt, 
        String systemPrompt, 
        StreamingCallback callback
    );
    
    void generateStructuredContentStreaming(
        String prompt, 
        String schema, 
        String systemPrompt, 
        StreamingCallback callback
    );
}
```

### 4.3 Step 3: Implement OpenRouter Streaming

**File**: `src/main/java/com/tripplanner/service/client/OpenRouterStreamingClient.java`

```java
package com.tripplanner.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.service.ai.StreamingCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;

@Service
public class OpenRouterStreamingClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenRouterStreamingClient.class);
    
    @Value("${openrouter.base-url}")
    private String baseUrl;
    
    @Value("${openrouter.api-key}")
    private String apiKey;
    
    @Value("${ai.model}")
    private String modelName;
    
    @Value("${ai.temperature}")
    private float temperature;
    
    @Value("${ai.max-tokens}")
    private int maxTokens;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public OpenRouterStreamingClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public void streamChatCompletion(
        String userPrompt, 
        String systemPrompt, 
        StreamingCallback callback
    ) {
        try {
            String requestBody = buildStreamingPayload(userPrompt, systemPrompt);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(120)) // Long timeout for streaming
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            logger.info("Starting streaming request to OpenRouter...");
            
            // Use ofLines() to stream line-by-line
            HttpResponse<Stream<String>> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofLines()
            );
            
            if (response.statusCode() != 200) {
                callback.onError(new RuntimeException("OpenRouter error: " + response.statusCode()));
                return;
            }
            
            StringBuilder fullResponse = new StringBuilder();
            
            response.body().forEach(line -> {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6); // Remove "data: " prefix
                    
                    if ("[DONE]".equals(data)) {
                        logger.info("Stream complete");
                        callback.onComplete(fullResponse.toString());
                        return;
                    }
                    
                    try {
                        JsonNode chunk = objectMapper.readTree(data);
                        JsonNode delta = chunk.path("choices")
                            .get(0)
                            .path("delta");
                        
                        String token = delta.path("content").asText("");
                        
                        if (!token.isEmpty()) {
                            fullResponse.append(token);
                            callback.onToken(token);
                        }
                        
                    } catch (Exception e) {
                        logger.warn("Failed to parse SSE chunk: {}", data, e);
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("Streaming failed", e);
            callback.onError(e);
        }
    }
    
    private String buildStreamingPayload(String userPrompt, String systemPrompt) {
        return String.format("""
            {
              "model": "%s",
              "temperature": %.2f,
              "max_tokens": %d,
              "stream": true,
              "messages": [
                %s
                {"role": "user", "content": %s}
              ]
            }
            """,
            modelName,
            temperature,
            maxTokens,
            systemPrompt != null ? "{\"role\":\"system\",\"content\":" + jsonEscape(systemPrompt) + "}," : "",
            jsonEscape(userPrompt)
        );
    }
    
    private String jsonEscape(String s) {
        try {
            return objectMapper.writeValueAsString(s);
        } catch (Exception e) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
    }
}
```

### 4.4 Step 4: Create SSE Controller

**File**: `src/main/java/com/tripplanner/controller/StreamingChatController.java`

```java
package com.tripplanner.controller;

import com.tripplanner.dto.ChatRequest;
import com.tripplanner.service.ai.StreamingCallback;
import com.tripplanner.service.client.OpenRouterStreamingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/chat/stream")
public class StreamingChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingChatController.class);
    
    private final OpenRouterStreamingClient streamingClient;
    
    public StreamingChatController(OpenRouterStreamingClient streamingClient) {
        this.streamingClient = streamingClient;
    }
    
    @PostMapping
    public SseEmitter streamChatResponse(@RequestBody ChatRequest request) {
        // Create emitter with 120 second timeout
        SseEmitter emitter = new SseEmitter(120_000L);
        
        logger.info("Starting SSE stream for chat request: {}", request.getText());
        
        // Run streaming in background thread
        CompletableFuture.runAsync(() -> {
            try {
                streamingClient.streamChatCompletion(
                    request.getText(),
                    buildSystemPrompt(request),
                    new StreamingCallback() {
                        @Override
                        public void onToken(String token) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(token));
                            } catch (IOException e) {
                                logger.warn("Client disconnected", e);
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
                                logger.warn("Client disconnected", e);
                                emitter.completeWithError(e);
                            }
                        }
                        
                        @Override
                        public void onComplete(String fullResponse) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(fullResponse));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            logger.error("Streaming error", error);
                            emitter.completeWithError(error);
                        }
                    }
                );
            } catch (Exception e) {
                logger.error("Failed to start stream", e);
                emitter.completeWithError(e);
            }
        });
        
        // Handle client disconnect
        emitter.onTimeout(() -> {
            logger.warn("SSE stream timed out");
            emitter.complete();
        });
        
        emitter.onCompletion(() -> {
            logger.info("SSE stream completed successfully");
        });
        
        return emitter;
    }
    
    private String buildSystemPrompt(ChatRequest request) {
        return "You are a helpful travel planning assistant.";
    }
}
```

### 4.5 Step 5: Update BaseAgent for Streaming

**File**: `src/main/java/com/tripplanner/agents/BaseAgent.java` (modify)

```java
/**
 * Execute agent with streaming support.
 * Emits real-time progress including thoughts and output.
 */
public void executeWithStreaming(String itineraryId, AgentRequest request, StreamingCallback callback) {
    logger.info("=== {} STARTING (STREAMING MODE) ===", getAgentName());
    
    try {
        // Emit agent start event with metadata
        emitAgentStreamEvent(itineraryId, "agent_started", null);
        
        // Call LLM with streaming
        aiClient.generateContentStreaming(
            buildPrompt(request),
            buildSystemPrompt(),
            new StreamingCallback() {
                @Override
                public void onToken(String token) {
                    // Forward to frontend with agent context
                    emitAgentStreamEvent(itineraryId, "agent_token", Map.of(
                        "token", token,
                        "type", "output"
                    ));
                    callback.onToken(token);
                }
                
                @Override
                public void onThought(String thought) {
                    // Forward thinking process
                    emitAgentStreamEvent(itineraryId, "agent_thought", Map.of(
                        "thought", thought,
                        "type", "thought"
                    ));
                    callback.onThought(thought);
                }
                
                @Override
                public void onComplete(String fullResponse) {
                    emitAgentStreamEvent(itineraryId, "agent_complete", Map.of(
                        "fullResponse", fullResponse
                    ));
                    callback.onComplete(fullResponse);
                }
                
                @Override
                public void onError(Exception error) {
                    logger.error("{} streaming failed", getAgentName(), error);
                    emitAgentStreamEvent(itineraryId, "agent_error", Map.of(
                        "error", error.getMessage()
                    ));
                    callback.onError(error);
                }
            }
        );
        
    } catch (Exception e) {
        logger.error("{} execution failed", getAgentName(), e);
        callback.onError(e);
    }
}

private void emitAgentStreamEvent(String itineraryId, String eventType, Map<String, Object> data) {
    AgentStreamEvent event = AgentStreamEvent.builder()
        .eventType(eventType)
        .agentId(agentId)
        .agentName(getAgentName())
        .agentDisplayName(getAgentDisplayName())
        .agentIcon(getAgentIcon())
        .agentColor(getAgentColor())
        .phase(getCurrentPhase())
        .data(data)
        .timestamp(System.currentTimeMillis())
        .build();
        
    webSocketService.sendToItinerary(itineraryId, "agent_stream", event);
}

// To be implemented by subclasses
protected abstract String getAgentDisplayName();
protected abstract String getAgentIcon();
protected abstract String getAgentColor();
protected abstract String getCurrentPhase();
```

---

## 5. Frontend Implementation

### 5.1 Step 1: Create Streaming Hook

**File**: `frontend/src/hooks/useStreamingChat.ts`

```typescript
import { useState, useCallback, useRef } from 'react';

export interface StreamingMessage {
  tokens: string[];
  thoughts: string[];
  fullText: string;
  isStreaming: boolean;
  error: string | null;
  duration?: number;
}

export function useStreamingChat(itineraryId: string) {
  const [message, setMessage] = useState<StreamingMessage>({
    tokens: [],
    thoughts: [],
    fullText: '',
    isStreaming: false,
    error: null
  });
  
  const eventSourceRef = useRef<EventSource | null>(null);
  const startTimeRef = useRef<number>(0);
  
  const sendMessage = useCallback((text: string) => {
    // Close existing connection
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }
    
    // Reset state
    setMessage({
      tokens: [],
      thoughts: [],
      fullText: '',
      isStreaming: true,
      error: null
    });
    
    startTimeRef.current = Date.now();
    
    // Create SSE connection
    const eventSource = new EventSource(
      `/api/v1/chat/stream?itineraryId=${itineraryId}&text=${encodeURIComponent(text)}`
    );
    
    eventSourceRef.current = eventSource;
    
    // Listen for tokens
    eventSource.addEventListener('token', (event) => {
      const token = event.data;
      setMessage(prev => ({
        ...prev,
        tokens: [...prev.tokens, token],
        fullText: prev.fullText + token
      }));
    });
    
    // Listen for thoughts
    eventSource.addEventListener('thought', (event) => {
      const thought = event.data;
      setMessage(prev => ({
        ...prev,
        thoughts: [...prev.thoughts, thought]
      }));
    });
    
    // Listen for completion
    eventSource.addEventListener('done', (event) => {
      const duration = Date.now() - startTimeRef.current;
      setMessage(prev => ({
        ...prev,
        isStreaming: false,
        duration
      }));
      eventSource.close();
    });
    
    // Handle errors
    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      setMessage(prev => ({
        ...prev,
        isStreaming: false,
        error: 'Stream interrupted'
      }));
      eventSource.close();
    };
    
  }, [itineraryId]);
  
  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);
  
  return { message, sendMessage };
}
```

### 5.2 Step 2: Create Base Streaming Components

**File**: `frontend/src/components/streaming/StreamingText.tsx`

```typescript
import { cn } from '@/lib/utils';

interface StreamingTextProps {
  tokens: string[];
  isStreaming: boolean;
  variant?: 'chat' | 'agent' | 'inline';
  showCursor?: boolean;
  className?: string;
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

**File**: `frontend/src/components/streaming/StreamingThoughts.tsx`

```typescript
import { useState } from 'react';
import { Brain, ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

interface StreamingThoughtsProps {
  thoughts: string[];
  isStreaming: boolean;
  variant?: 'minimal' | 'detailed';
  defaultOpen?: boolean;
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

### 5.3 Step 3: Create Agent UI Configuration

**File**: `frontend/src/config/agentConfig.ts`

```typescript
import { Calendar, MapPin, UtensilsCrossed, Sparkles, Car } from 'lucide-react';

export interface AgentUIConfig {
  displayName: string;
  icon: any;
  color: string;
  description: string;
  avatar: string;
}

export const AGENT_CONFIG: Record<string, AgentUIConfig> = {
  'SkeletonPlannerAgent': {
    displayName: 'Day Planner',
    icon: Calendar,
    color: '#3b82f6',
    description: 'Creating daily structure',
    avatar: '📅'
  },
  'ActivityAgent': {
    displayName: 'Activity Finder',
    icon: MapPin,
    color: '#10b981',
    description: 'Finding attractions',
    avatar: '🎯'
  },
  'MealAgent': {
    displayName: 'Dining Expert',
    icon: UtensilsCrossed,
    color: '#f59e0b',
    description: 'Suggesting restaurants',
    avatar: '🍽️'
  },
  'TransportAgent': {
    displayName: 'Transport Planner',
    icon: Car,
    color: '#06b6d4',
    description: 'Planning routes',
    avatar: '🚗'
  },
  'EnrichmentAgent': {
    displayName: 'Detail Enhancer',
    icon: Sparkles,
    color: '#8b5cf6',
    description: 'Adding rich details',
    avatar: '✨'
  }
};
```

---

## 6. Multi-Agent UX

### 6.1 Agent Streaming Card

**File**: `frontend/src/components/streaming/AgentStreamingCard.tsx`

```typescript
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, Brain, MessageSquare, Clock, Hash, CheckCircle, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { AGENT_CONFIG } from '@/config/agentConfig';
import { StreamingText } from './StreamingText';
import { StreamingThoughts } from './StreamingThoughts';
import { StreamingMessage } from '@/hooks/useStreamingChat';

interface AgentStreamingCardProps {
  agentName: string;
  message: StreamingMessage;
  isActive: boolean;
  defaultExpanded?: boolean;
}

export function AgentStreamingCard({ 
  agentName, 
  message, 
  isActive,
  defaultExpanded = true 
}: AgentStreamingCardProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  const config = AGENT_CONFIG[agentName] || {
    displayName: agentName,
    icon: Brain,
    color: '#6b7280',
    description: 'Working...',
    avatar: '🤖'
  };
  
  const Icon = config.icon;
  
  return (
    <motion.div 
      className={cn(
        "agent-card",
        isActive && "agent-card-active",
        !isExpanded && "agent-card-collapsed"
      )}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      style={{ borderColor: config.color }}
    >
      {/* Header */}
      <div className="agent-header" onClick={() => setIsExpanded(!isExpanded)}>
        {/* Avatar with activity indicator */}
        <div className="agent-avatar-wrapper">
          <div 
            className="agent-avatar"
            style={{ backgroundColor: `${config.color}20` }}
          >
            <span className="agent-emoji">{config.avatar}</span>
          </div>
          {isActive && (
            <div className="activity-pulse" style={{ backgroundColor: config.color }} />
          )}
        </div>
        
        {/* Agent Info */}
        <div className="agent-info">
          <div className="agent-name">
            <span>{config.displayName}</span>
            {isActive && (
              <span className="status-badge" style={{ backgroundColor: config.color }}>
                Thinking...
              </span>
            )}
          </div>
          <div className="agent-description">{config.description}</div>
        </div>
        
        {/* Expand/Collapse */}
        <ChevronDown 
          className={cn(
            "collapse-icon transition-transform",
            isExpanded && "rotate-180"
          )} 
        />
      </div>
      
      {/* Content (collapsible) */}
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="agent-content"
          >
            {/* Thoughts Section */}
            <StreamingThoughts 
              thoughts={message.thoughts}
              isStreaming={message.isStreaming}
              variant="detailed"
              defaultOpen={isActive}
            />
            
            {/* Output Section */}
            {message.tokens.length > 0 && (
              <div className="output-section">
                <div className="section-label">
                  <MessageSquare size={14} />
                  <span>Output</span>
                </div>
                <div className="output-content">
                  <StreamingText 
                    tokens={message.tokens}
                    isStreaming={message.isStreaming}
                    variant="agent"
                    showCursor={isActive}
                  />
                </div>
              </div>
            )}
            
            {/* Metadata */}
            {!message.isStreaming && message.duration && (
              <div className="agent-metadata">
                <span className="metadata-item">
                  <Clock size={12} />
                  {message.duration}ms
                </span>
                <span className="metadata-item">
                  <Hash size={12} />
                  {message.tokens.length} tokens
                </span>
                <span className="metadata-item">
                  <CheckCircle size={12} />
                  Complete
                </span>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
```

### 6.2 Multi-Agent Dashboard

**File**: `frontend/src/components/streaming/MultiAgentDashboard.tsx`

```typescript
import { useEffect, useState } from 'react';
import { useMultiAgentStreaming } from '@/hooks/useMultiAgentStreaming';
import { AgentStreamingCard } from './AgentStreamingCard';
import { Progress } from '@/components/ui/progress';

interface MultiAgentDashboardProps {
  itineraryId: string;
}

export function MultiAgentDashboard({ itineraryId }: MultiAgentDashboardProps) {
  const agentStreams = useMultiAgentStreaming(itineraryId);
  const [activeAgent, setActiveAgent] = useState<string | null>(null);
  
  // Determine which agent is currently active
  useEffect(() => {
    const streaming = Array.from(agentStreams.entries())
      .find(([_, msg]) => msg.isStreaming);
    setActiveAgent(streaming?.[0] || null);
  }, [agentStreams]);
  
  const completed = Array.from(agentStreams.values())
    .filter(msg => !msg.isStreaming && msg.tokens.length > 0).length;
  
  const progress = (completed / agentStreams.size) * 100;
  
  return (
    <div className="multi-agent-dashboard">
      <div className="dashboard-header">
        <h2>AI Agents at Work</h2>
        <span className="agent-count">
          {completed} of {agentStreams.size} complete
        </span>
      </div>
      
      {/* Agent Cards */}
      <div className="agent-cards-container">
        {Array.from(agentStreams.entries()).map(([agentName, message]) => (
          <AgentStreamingCard 
            key={agentName}
            agentName={agentName}
            message={message}
            isActive={agentName === activeAgent}
            defaultExpanded={agentName === activeAgent}
          />
        ))}
      </div>
      
      {/* Overall Progress */}
      <div className="overall-progress">
        <Progress value={progress} className="progress-bar" />
      </div>
    </div>
  );
}
```

---

## 7. Implementation Roadmap

### Phase 1: Core Backend (8-10 hours)

- [x] ✅ Research complete
- [ ] Create `StreamingCallback.java`
- [ ] Update `AiClient.java` interface
- [ ] Implement `OpenRouterStreamingClient.java`
- [ ] Implement `GeminiStreamingClient.java` (optional, can do later)
- [ ] Create `StreamingChatController.java`
- [ ] Update `BaseAgent.java` with streaming support
- [ ] Add SSE configuration to `application.yml`
- [ ] **Test**: Manual curl test of SSE endpoint

### Phase 2: Core Frontend (6-8 hours)

- [ ] Create `useStreamingChat.ts` hook
- [ ] Create `StreamingText.tsx` component
- [ ] Create `StreamingThoughts.tsx` component
- [ ] Create `StreamingMessage.tsx` component
- [ ] Integrate into `ChatPage.tsx`
- [ ] Add streaming styles to `streaming.css`
- [ ] **Test**: Chat page with streaming response

### Phase 3: Multi-Agent UX (6-8 hours)

- [ ] Create `agentConfig.ts` with agent metadata
- [ ] Create `AgentStreamingCard.tsx`
- [ ] Create `useMultiAgentStreaming.ts` hook
- [ ] Create `MultiAgentDashboard.tsx`
- [ ] Update `BaseAgent.java` subclasses with UI metadata
- [ ] Integrate into `PlannerProgressPage.tsx`
- [ ] **Test**: Multiple agents streaming on planner page

### Phase 4: Polish & Testing (4-5 hours)

- [ ] Add token batching (performance)
- [ ] Add smooth scroll animations
- [ ] Add error recovery UI
- [ ] Add accessibility (ARIA labels, keyboard nav)
- [ ] Write unit tests (hooks, components)
- [ ] Write integration tests (E2E)
- [ ] Performance testing (memory leaks, long streams)

### Phase 5: Production Deployment (2-3 hours)

- [ ] Update Cloud Run timeout to 300 seconds
- [ ] Add monitoring metrics (active streams, failures)
- [ ] Add feature flag for gradual rollout
- [ ] Deploy to staging
- [ ] User acceptance testing
- [ ] Deploy to production

---

## Success Metrics

✅ **Performance**
- Time to first token: <1 second
- Tokens per second: 30-50
- Memory usage: Stable (no leaks)
- Concurrent streams: 10+ users

✅ **UX Quality**
- ChatGPT-like smoothness
- Clear agent identification
- Collapsible sections work
- Mobile responsive

✅ **Reliability**
- Stream success rate: >95%
- Graceful error handling
- Auto-reconnect on failure
- Fallback to non-streaming

---

**Total Estimated Effort**: 20-25 hours  
**Priority**: High (major UX improvement)  
**Risk**: Low (additive feature, can feature-flag)

**Ready to begin implementation!** 🚀
