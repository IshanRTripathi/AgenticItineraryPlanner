# Multi-Agent Streaming UX Design
## ChatGPT-Style Agent Identification & Thoughts Display

**Goal**: Create a production-grade UI showing multiple agents working simultaneously with clear visual hierarchy, agent identification, and thought processes.

---

## 1. Backend: Agent-Tagged SSE Events

### 1.1 Enhanced Event Format

**Current WebSocket events** (update to support streaming):
```typescript
// Old format (non-streaming)
{
  "type": "agent_progress",
  "agentId": "skeleton_123",
  "agentName": "SkeletonPlannerAgent",
  "progress": 50,
  "message": "Generated 3 of 7 days"
}
```

**New SSE format** (with streaming):
```typescript
// SSE event with agent context
{
  "eventType": "agent_token",        // or "agent_thought", "agent_complete"
  "agentId": "skeleton_123",
  "agentName": "SkeletonPlannerAgent",
  "agentDisplayName": "Day Planner",  // User-friendly name
  "agentIcon": "calendar",            // Icon identifier
  "agentColor": "#3b82f6",            // Brand color
  "phase": "Planning Days",           // Current phase
  "data": {
    "token": "Day 1: Barcelona...",   // Token content
    "type": "output"                  // or "thought"
  },
  "timestamp": 1732598400000
}
```

### 1.2 Backend Implementation

Update `BaseAgent.java` to emit streaming events:

```java
@Override
public void executeWithStreaming(String itineraryId, AgentRequest request, StreamingCallback callback) {
    logger.info("=== {} STARTING (STREAMING MODE) ===", getAgentName());
    
    // Emit agent start event
    emitStreamEvent(itineraryId, "agent_started", null);
    
    // Call LLM with streaming
    llmClient.generateContentStreaming(prompt, systemPrompt, new StreamingCallback() {
        @Override
        public void onToken(String token) {
            // Forward token to frontend with agent context
            emitStreamEvent(itineraryId, "agent_token", Map.of(
                "token", token,
                "type", "output"
            ));
            callback.onToken(token);
        }
        
        @Override
        public void onThought(String thought) {
            // Forward thinking process
            emitStreamEvent(itineraryId, "agent_thought", Map.of(
                "thought", thought,
                "type", "thought"
            ));
            callback.onThought(thought);
        }
        
        @Override
        public void onComplete(String fullResponse) {
            emitStreamEvent(itineraryId, "agent_complete", Map.of(
                "fullResponse", fullResponse
            ));
            callback.onComplete(fullResponse);
        }
    });
}

private void emitStreamEvent(String itineraryId, String eventType, Map<String, Object> data) {
    AgentStreamEvent event = AgentStreamEvent.builder()
        .eventType(eventType)
        .agentId(agentId)
        .agentName(getAgentName())
        .agentDisplayName(getAgentDisplayName())  // e.g., "Day Planner"
        .agentIcon(getAgentIcon())                 // e.g., "calendar"
        .agentColor(getAgentColor())               // e.g., "#3b82f6"
        .phase(getCurrentPhase())                  // e.g., "Planning Days"
        .data(data)
        .timestamp(System.currentTimeMillis())
        .build();
        
    webSocketService.sendToItinerary(itineraryId, "agent_stream", event);
}
```

---

## 2. Frontend: Multi-Agent Dashboard

### 2.1 Agent Configuration

```typescript
// Agent metadata for UI rendering
export const AGENT_CONFIG: Record<string, AgentUIConfig> = {
  'SkeletonPlannerAgent': {
    displayName: 'Day Planner',
    icon: Calendar,
    color: '#3b82f6',        // Blue
    description: 'Creating daily structure',
    avatar: '📅'
  },
  'ActivityAgent': {
    displayName: 'Activity Finder',
    icon: MapPin,
    color: '#10b981',        // Green
    description: 'Finding attractions',
    avatar: '🎯'
  },
  'MealAgent': {
    displayName: 'Dining Expert',
    icon: UtensilsCrossed,
    color: '#f59e0b',        // Amber
    description: 'Suggesting restaurants',
    avatar: '🍽️'
  },
  'EnrichmentAgent': {
    displayName: 'Detail Enhancer',
    icon: Sparkles,
    color: '#8b5cf6',        // Purple
    description: 'Adding rich details',
    avatar: '✨'
  }
};
```

### 2.2 AgentStreamingCard Component

**ChatGPT-inspired agent card**:

```typescript
interface AgentStreamingCardProps {
  agentName: string;
  message: StreamingMessage;
  isActive: boolean;          // Currently streaming?
  defaultExpanded?: boolean;  // Start open?
}

export function AgentStreamingCard({ 
  agentName, 
  message, 
  isActive,
  defaultExpanded = true 
}: AgentStreamingCardProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  const config = AGENT_CONFIG[agentName] || DEFAULT_CONFIG;
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
            {message.thoughts.length > 0 && (
              <div className="thoughts-section">
                <div className="section-label">
                  <Brain size={14} />
                  <span>Reasoning Process</span>
                  <span className="thought-count">{message.thoughts.length}</span>
                </div>
                <div className="thoughts-list">
                  {message.thoughts.map((thought, i) => (
                    <div key={i} className="thought-item">
                      <div className="thought-bullet" style={{ backgroundColor: config.color }} />
                      <span className="thought-text">{thought}</span>
                    </div>
                  ))}
                  {message.isStreaming && (
                    <div className="thought-item thought-loading">
                      <Loader2 size={12} className="animate-spin" />
                      <span className="thought-text text-muted">Processing...</span>
                    </div>
                  )}
                </div>
              </div>
            )}
            
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
            {!message.isStreaming && message.tokens.length > 0 && (
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

### 2.3 Multi-Agent Dashboard

**Main container for planner progress page**:

```typescript
export function MultiAgentStreamingDashboard({ itineraryId }: Props) {
  const agentStreams = useMultiAgentStreaming(itineraryId);
  const [activeAgent, setActiveAgent] = useState<string | null>(null);
  
  // Determine which agents are currently active
  useEffect(() => {
    const streaming = Array.from(agentStreams.entries())
      .find(([_, msg]) => msg.isStreaming);
    setActiveAgent(streaming?.[0] || null);
  }, [agentStreams]);
  
  return (
    <div className="multi-agent-dashboard">
      {/* Phase Progress Bar */}
      <PhaseProgressBar 
        currentPhase={getCurrentPhase(agentStreams)}
        completedPhases={getCompletedPhases(agentStreams)}
      />
      
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
        <div className="progress-stats">
          <span>{getCompletedAgents(agentStreams)} of {agentStreams.size} agents complete</span>
        </div>
        <Progress 
          value={calculateOverallProgress(agentStreams)} 
          className="progress-bar"
        />
      </div>
    </div>
  );
}
```

---

## 3. ChatGPT-Style UX Patterns

### 3.1 Visual Hierarchy

```
┌─────────────────────────────────────────────────┐
│  PHASE: Population (3 of 5)                    │  ← Phase indicator
├─────────────────────────────────────────────────┤
│                                                 │
│  📅 Day Planner                    [Thinking...] │  ← Agent 1 (active)
│  ├─ 🧠 Reasoning Process (3)                   │
│  │   • Analyzing user preferences...            │
│  │   • Determining optimal day structure...     │
│  │   • ⚡ Balancing activities and downtime...  │  ← Streaming
│  ├─ 💬 Output                                   │
│  │   Day 1: Barcelona City Center               │
│  │   - Morning: Park Güell visit                │
│  │   - Afternoon: Gothic Quarter▊               │  ← Cursor
│  └─ ⏱️ 1,234ms  #127 tokens  ✓ Complete        │
│                                                 │
│  🎯 Activity Finder               [Complete ✓] │  ← Agent 2 (done)
│  ├─ 🧠 Reasoning Process (5)        [Collapsed] │
│  └─ 💬 Output                        [Collapsed] │
│                                                 │
│  🍽️ Dining Expert                   [Waiting] │  ← Agent 3 (pending)
│                                                 │
└─────────────────────────────────────────────────┘
   3 of 5 agents complete  ████████░░░ 60%
```

### 3.2 Color Coding System

```typescript
// Semantic colors for agent states
const AGENT_STATES = {
  active: {
    border: 'border-blue-500 shadow-blue-500/20',
    badge: 'bg-blue-500',
    pulse: 'animate-pulse bg-blue-500',
  },
  complete: {
    border: 'border-green-500 shadow-green-500/10',
    badge: 'bg-green-500',
    icon: 'text-green-500',
  },
  waiting: {
    border: 'border-gray-300',
    badge: 'bg-gray-300',
    icon: 'text-gray-400',
  },
  error: {
    border: 'border-red-500 shadow-red-500/20',
    badge: 'bg-red-500',
    icon: 'text-red-500',
  }
};
```

### 3.3 Animations

**Smooth appearance** (like ChatGPT):
```typescript
const cardVariants = {
  hidden: { opacity: 0, y: 20, scale: 0.95 },
  visible: { 
    opacity: 1, 
    y: 0, 
    scale: 1,
    transition: { duration: 0.3, ease: "easeOut" }
  },
  exit: { 
    opacity: 0, 
    scale: 0.95,
    transition: { duration: 0.2 }
  }
};

// Token appearance animation
const tokenVariants = {
  hidden: { opacity: 0 },
  visible: { 
    opacity: 1,
    transition: { duration: 0.1 }
  }
};
```

### 3.4 Thought Stream Formatting

**Different visual styles for thought types**:
```typescript
export function formatThought(thought: string): ThoughtType {
  // Detect thought type from content
  if (thought.startsWith('Analyzing')) {
    return { type: 'analysis', icon: Search, color: '#3b82f6' };
  } else if (thought.startsWith('Deciding') || thought.startsWith('Choosing')) {
    return { type: 'decision', icon: GitBranch, color: '#8b5cf6' };
  } else if (thought.startsWith('Checking') || thought.startsWith('Validating')) {
    return { type: 'validation', icon: CheckCircle, color: '#10b981' };
  } else if (thought.startsWith('Error') || thought.startsWith('Warning')) {
    return { type: 'warning', icon: AlertTriangle, color: '#f59e0b' };
  }
  return { type: 'default', icon: Brain, color: '#6b7280' };
}

// Render thoughts with semantic icons
{message.thoughts.map((thought, i) => {
  const { type, icon: Icon, color } = formatThought(thought);
  return (
    <div key={i} className="thought-item" data-type={type}>
      <Icon size={14} style={{ color }} />
      <span>{thought}</span>
    </div>
  );
})}
```

---

## 4. Advanced Features

### 4.1 Agent Dependency Visualization

**Show which agents depend on others**:
```typescript
// Agent execution graph
const AGENT_DEPENDENCIES = {
  'SkeletonPlannerAgent': { 
    dependsOn: [],
    enables: ['ActivityAgent', 'MealAgent', 'TransportAgent']
  },
  'ActivityAgent': {
    dependsOn: ['SkeletonPlannerAgent'],
    enables: ['EnrichmentAgent']
  },
  // ...
};

// Visual indicator
<div className="agent-dependencies">
  {config.dependsOn.length > 0 && (
    <div className="depends-on">
      <ArrowLeft size={12} />
      <span>Waiting for: {config.dependsOn.join(', ')}</span>
    </div>
  )}
</div>
```

### 4.2 Playback Speed Control

**Let users speed up/slow down streaming** (like video playback):
```typescript
const [playbackSpeed, setPlaybackSpeed] = useState(1.0);

// Adjust token batching based on speed
const tokenBatchDelay = 50 / playbackSpeed; // 50ms at 1x, 25ms at 2x

// Speed selector
<select value={playbackSpeed} onChange={(e) => setPlaybackSpeed(Number(e.target.value))}>
  <option value={0.5}>0.5x (Slow)</option>
  <option value={1.0}>1x (Normal)</option>
  <option value={2.0}>2x (Fast)</option>
  <option value={5.0}>5x (Very Fast)</option>
</select>
```

### 4.3 Pause/Resume Streaming

**User control over stream flow**:
```typescript
const [isPaused, setIsPaused] = useState(false);

// Pause button
<button onClick={() => setIsPaused(!isPaused)}>
  {isPaused ? <Play size={16} /> : <Pause size={16} />}
  {isPaused ? 'Resume' : 'Pause'}
</button>

// In SSE listener, buffer tokens when paused
useEffect(() => {
  if (isPaused) {
    // Buffer incoming tokens
    tokenBuffer.push(token);
  } else {
    // Flush buffer
    setMessage(prev => ({
      ...prev,
      tokens: [...prev.tokens, ...tokenBuffer]
    }));
    tokenBuffer = [];
  }
}, [isPaused, token]);
```

---

## 5. Styling (Tailwind CSS)

```css
/* Agent Card Styles */
.agent-card {
  @apply bg-white rounded-lg border-2 border-gray-200 
         shadow-sm transition-all duration-200 overflow-hidden;
}

.agent-card-active {
  @apply border-blue-500 shadow-lg shadow-blue-500/20;
}

.agent-header {
  @apply flex items-center gap-3 p-4 cursor-pointer 
         hover:bg-gray-50 transition-colors;
}

.agent-avatar-wrapper {
  @apply relative;
}

.agent-avatar {
  @apply w-10 h-10 rounded-full flex items-center justify-center;
}

.activity-pulse {
  @apply absolute -top-1 -right-1 w-3 h-3 rounded-full 
         animate-ping;
}

.thoughts-section {
  @apply px-4 py-3 bg-gray-50 border-t border-gray-200;
}

.thought-item {
  @apply flex items-start gap-2 py-1.5 text-sm text-gray-700;
}

.thought-bullet {
  @apply w-1.5 h-1.5 rounded-full mt-1.5 flex-shrink-0;
}

.output-section {
  @apply px-4 py-3;
}

.streaming-cursor {
  @apply inline-block w-0.5 h-5 ml-1 bg-current;
}

.agent-metadata {
  @apply flex items-center gap-3 px-4 py-2 text-xs 
         text-gray-500 border-t border-gray-200;
}
```

---

## 6. Implementation Checklist

### Backend (2-3 hours)
- [ ] Add `agentDisplayName`, `agentIcon`, `agentColor` to `BaseAgent`
- [ ] Implement `emitStreamEvent()` with agent metadata
- [ ] Tag all SSE events with agent context
- [ ] Add thought detection in LLM client (OpenRouter/Gemini)

### Frontend (4-5 hours)
- [ ] Create `AGENT_CONFIG` with UI metadata
- [ ] Build `AgentStreamingCard` component
- [ ] Build `MultiAgentStreamingDashboard`
- [ ] Add animations with Framer Motion
- [ ] Implement collapsible sections
- [ ] Add state indicators (active/complete/waiting)

### UX Polish (2-3 hours)
- [ ] Add smooth token batching (50ms)
- [ ] Implement auto-scroll on new tokens
- [ ] Add playback speed control
- [ ] Add pause/resume functionality
- [ ] Color-code thought types
- [ ] Add agent dependency visualization

---

## Success Criteria

✅ **Multiple agents visible simultaneously**  
✅ **Clear visual distinction** (colors, icons, avatars)  
✅ **Real-time thoughts** shown as they're generated  
✅ **Collapsible sections** to reduce clutter  
✅ **Smooth animations** (ChatGPT-quality)  
✅ **Activity indicators** (pulsing, badges)  
✅ **Performance** (60 FPS with 3+ streaming agents)  

---

**This design creates a ChatGPT-quality multi-agent streaming experience!**
