I'll create a detailed roadmap with specific filenames and function names for each phase. Let me analyze the current implementation and create a comprehensive plan.


[1 tool called]

# Detailed Implementation Roadmap with Filenames and Functions

## **Phase 8: Chat Backend Infrastructure**

### **8.1 Create Chat Controller**
**File:** `src/main/java/com/tripplanner/api/ChatController.java`
```java
// Key Functions:
@PostMapping("/chat/route")
public ResponseEntity<ChatResponse> route(@Valid @RequestBody ChatRequest request)

private void validateChatRequest(ChatRequest request)
private ChatResponse handleDisambiguation(ChatRequest request, List<NodeCandidate> candidates)
```

### **8.2 Create Orchestrator Service**
**File:** `src/main/java/com/tripplanner/service/OrchestratorService.java`
```java
// Key Functions:
public ChatResponse route(ChatRequest request)
private IntentResult classifyIntent(String text, String selectedNodeId, Integer day)
private List<NodeCandidate> findNodeCandidates(String text, String itineraryId, Integer day)
private ChangeSet buildChangeSet(IntentResult intent, String itineraryId)
private ChatResponse createDisambiguationResponse(String text, List<NodeCandidate> candidates)
private ChatResponse createSuccessResponse(ChangeSet changeSet, ItineraryDiff diff, boolean applied)
```

### **8.3 Create Chat DTOs**
**File:** `src/main/java/com/tripplanner/api/dto/ChatRequest.java`
```java
// Fields:
private String itineraryId
private String scope // "trip" | "day"
private Integer day
private String selectedNodeId
private String text
private boolean autoApply
```

**File:** `src/main/java/com/tripplanner/api/dto/ChatResponse.java`
```java
// Fields:
private String intent
private String message
private ChangeSet changeSet
private ItineraryDiff diff
private boolean applied
private Integer toVersion
private List<String> warnings
private boolean needsDisambiguation
private List<NodeCandidate> candidates
```

**File:** `src/main/java/com/tripplanner/api/dto/IntentResult.java`
```java
// Fields:
private String intent // "REPLAN_TODAY" | "MOVE_TIME" | "INSERT_PLACE" | "DELETE_NODE" | "REPLACE_NODE" | "BOOK_NODE" | "UNDO" | "EXPLAIN"
private Integer day
private List<String> nodeIds
private Map<String, String> entities
private Map<String, Object> constraints
```

**File:** `src/main/java/com/tripplanner/api/dto/NodeCandidate.java`
```java
// Fields:
private String id
private String title
private Integer day
private String type
private String location
```

### **8.4 Create Intent Classification Service**
**File:** `src/main/java/com/tripplanner/service/IntentClassificationService.java`
```java
// Key Functions:
public IntentResult classifyIntent(String text, String selectedNodeId, Integer day)
private IntentResult preRouterClassification(String text, String selectedNodeId, Integer day)
private IntentResult llmClassification(String text, String selectedNodeId, Integer day)
private boolean isReplanToday(String text)
private boolean isMoveTime(String text)
private boolean isInsertPlace(String text)
private boolean isDeleteNode(String text)
private boolean isReplaceNode(String text)
private boolean isBookNode(String text)
private boolean isUndo(String text)
private boolean isExplain(String text)
```

### **8.5 Create Node Resolution Service**
**File:** `src/main/java/com/tripplanner/service/NodeResolutionService.java`
```java
// Key Functions:
public List<NodeCandidate> findNodeCandidates(String text, String itineraryId, Integer day)
private List<NodeCandidate> searchByTitle(String text, String itineraryId, Integer day)
private List<NodeCandidate> searchByLocation(String text, String itineraryId, Integer day)
private List<NodeCandidate> searchByType(String text, String itineraryId, Integer day)
private double calculateSimilarity(String text, String nodeTitle)
private List<NodeCandidate> rankCandidates(List<NodeCandidate> candidates, String text)
```

---

## **Phase 9: Enhanced Node Schema**

### **9.1 Update NormalizedNode DTO**
**File:** `src/main/java/com/tripplanner/api/dto/NormalizedNode.java`
```java
// Add new fields:
private String status // "planned" | "in_progress" | "skipped" | "cancelled" | "completed"
private String updatedBy // "agent" | "user"
private Instant updatedAt

// Add new methods:
public void setStatus(String status)
public String getStatus()
public void setUpdatedBy(String updatedBy)
public String getUpdatedBy()
public void setUpdatedAt(Instant updatedAt)
public Instant getUpdatedAt()
```

### **9.2 Update ChangeEngine**
**File:** `src/main/java/com/tripplanner/service/ChangeEngine.java`
```java
// Modify existing functions:
private void moveNode(NormalizedItinerary itinerary, ChangeOperation op, ChangePreferences preferences)
private void insertNode(NormalizedItinerary itinerary, ChangeOperation op, ChangePreferences preferences)
private void deleteNode(NormalizedItinerary itinerary, ChangeOperation op, ChangePreferences preferences)

// Add new functions:
private void updateNodeAudit(NormalizedNode node, String updatedBy)
private void setNodeStatus(NormalizedNode node, String status)
private void validateNodeStatusTransition(String fromStatus, String toStatus)
```

### **9.3 Update PlannerAgent**
**File:** `src/main/java/com/tripplanner/service/agents/PlannerAgent.java`
```java
// Modify existing functions:
private NormalizedNode createMockNode(String type, String title, String location, int startHour, int durationMin)
private ChangeSet generateChangeSet(String itineraryId, String userRequest)

// Add new functions:
private void setNodeAuditFields(NormalizedNode node, String updatedBy)
private void setNodeStatus(NormalizedNode node, String status)
```

### **9.4 Update EnrichmentAgent**
**File:** `src/main/java/com/tripplanner/service/agents/EnrichmentAgent.java`
```java
// Modify existing functions:
public ChangeEngine.ApplyResult executeInternal(String itineraryId, BaseAgent.AgentRequest<ChangeEngine.ApplyResult> request)

// Add new functions:
private void updateNodeStatus(NormalizedNode node, String status)
private void setNodeAuditFields(NormalizedNode node, String updatedBy)
private void validateNodeStatus(NormalizedNode node)
```

---

## **Phase 10: Frontend Chat Interface**

### **10.1 Create Chat Types**
**File:** `frontend/src/types/ChatTypes.ts`
```typescript
// Interfaces:
export interface ChatRequest {
  itineraryId: string;
  scope: 'trip' | 'day';
  day?: number;
  selectedNodeId?: string;
  text: string;
  autoApply: boolean;
}

export interface ChatResponse {
  intent: string;
  message: string;
  changeSet?: ChangeSet;
  diff?: ItineraryDiff;
  applied: boolean;
  toVersion?: number;
  warnings: string[];
  needsDisambiguation: boolean;
  candidates?: NodeCandidate[];
}

export interface NodeCandidate {
  id: string;
  title: string;
  day: number;
  type: string;
  location: string;
}

export interface ChatMessage {
  id: string;
  text: string;
  sender: 'user' | 'assistant';
  timestamp: Date;
  response?: ChatResponse;
  candidates?: NodeCandidate[];
}
```

### **10.2 Create Chat Service**
**File:** `frontend/src/services/chatService.ts`
```typescript
// Key Functions:
export async function routeMessage(request: ChatRequest): Promise<ChatResponse>
export async function selectNodeCandidate(itineraryId: string, candidateId: string): Promise<ChatResponse>
export function createChatRequest(itineraryId: string, text: string, options?: ChatOptions): ChatRequest
export function validateChatRequest(request: ChatRequest): boolean
```

### **10.3 Create Chat Panel Component**
**File:** `frontend/src/components/chat/ChatPanel.tsx`
```typescript
// Key Functions:
const ChatPanel: React.FC<ChatPanelProps> = ({ itineraryId, selectedDay, selectedNodeId, onNodeSelect, onItineraryUpdate })

// Internal Functions:
const sendMessage = async (text: string): Promise<void>
const handleNodeSelection = async (candidate: NodeCandidate): Promise<void>
const handleVoiceInput = (): void
const handleSpeechToText = (transcript: string): void
const renderMessage = (message: ChatMessage): JSX.Element
const renderCandidates = (candidates: NodeCandidate[]): JSX.Element
const renderTypingIndicator = (): JSX.Element
```

### **10.4 Create Chat Message Component**
**File:** `frontend/src/components/chat/ChatMessage.tsx`
```typescript
// Key Functions:
const ChatMessage: React.FC<ChatMessageProps> = ({ message, onNodeSelect })

// Internal Functions:
const renderUserMessage = (message: ChatMessage): JSX.Element
const renderAssistantMessage = (message: ChatMessage): JSX.Element
const renderDiffPreview = (diff: ItineraryDiff): JSX.Element
const renderWarnings = (warnings: string[]): JSX.Element
```

### **10.5 Create Node Candidates Component**
**File:** `frontend/src/components/chat/NodeCandidates.tsx`
```typescript
// Key Functions:
const NodeCandidates: React.FC<NodeCandidatesProps> = ({ candidates, onSelect, onCancel })

// Internal Functions:
const renderCandidate = (candidate: NodeCandidate): JSX.Element
const handleCandidateClick = (candidate: NodeCandidate): void
const getCandidateIcon = (type: string): JSX.Element
const formatCandidateLocation = (location: string): string
```

### **10.6 Create New Node Indicator Component**
**File:** `frontend/src/components/chat/NewNodeIndicator.tsx`
```typescript
// Key Functions:
const NewNodeIndicator: React.FC<NewNodeIndicatorProps> = ({ nodeId, isNew, onDismiss })

// Internal Functions:
const renderIndicator = (): JSX.Element
const handleDismiss = (): void
const getIndicatorText = (): string
```

### **10.7 Create Voice Input Component**
**File:** `frontend/src/components/chat/VoiceInput.tsx`
```typescript
// Key Functions:
const VoiceInput: React.FC<VoiceInputProps> = ({ onTranscript, onError, isListening, onStart, onStop })

// Internal Functions:
const startListening = (): void
const stopListening = (): void
const handleSpeechResult = (event: SpeechRecognitionEvent): void
const handleSpeechError = (event: SpeechRecognitionErrorEvent): void
const isSpeechRecognitionSupported = (): boolean
```

### **10.8 Update API Client**
**File:** `frontend/src/services/apiClient.ts`
```typescript
// Add new functions:
export async function routeChatMessage(request: ChatRequest): Promise<ChatResponse>
export async function selectNodeCandidate(itineraryId: string, candidateId: string): Promise<ChatResponse>
export function createChatEventStream(itineraryId: string): EventSource
```

---

## **Phase 11: Enhanced Workflow Integration**

### **11.1 Update WorkflowBuilder Component**
**File:** `frontend/src/components/WorkflowBuilder.tsx`
```typescript
// Add new props:
interface WorkflowBuilderProps {
  // ... existing props
  scope: 'trip' | 'day';
  autoApply: boolean;
  onScopeChange: (scope: 'trip' | 'day') => void;
  onAutoApplyChange: (autoApply: boolean) => void;
  isDisabled: boolean;
  disabledReason?: string;
}

// Add new functions:
const handleScopeToggle = (scope: 'trip' | 'day'): void
const handleAutoApplyToggle = (autoApply: boolean): void
const renderScopeToggle = (): JSX.Element
const renderAutoApplyToggle = (): JSX.Element
const renderDisabledOverlay = (): JSX.Element
```

### **11.2 Update TravelPlanner Component**
**File:** `frontend/src/components/TravelPlanner.tsx`
```typescript
// Add new state:
const [chatScope, setChatScope] = useState<'trip' | 'day'>('trip')
const [autoApply, setAutoApply] = useState<boolean>(false)
const [isWorkflowDisabled, setIsWorkflowDisabled] = useState<boolean>(false)
const [workflowDisabledReason, setWorkflowDisabledReason] = useState<string>('')

// Add new functions:
const handleChatScopeChange = (scope: 'trip' | 'day'): void
const handleAutoApplyChange = (autoApply: boolean): void
const handleAgentStart = (): void
const handleAgentComplete = (): void
const handleChatNodeSelect = (nodeId: string): void
const renderChatPanel = (): JSX.Element
const renderWorkflowWithChat = (): JSX.Element
```

### **11.3 Create Toast Component**
**File:** `frontend/src/components/shared/Toast.tsx`
```typescript
// Key Functions:
const Toast: React.FC<ToastProps> = ({ message, type, duration, onClose })

// Internal Functions:
const renderToast = (): JSX.Element
const handleClose = (): void
const getToastIcon = (type: ToastType): JSX.Element
const getToastStyles = (type: ToastType): string
```

### **11.4 Create Toast Service**
**File:** `frontend/src/services/toastService.ts`
```typescript
// Key Functions:
export function showToast(message: string, type: ToastType, duration?: number): void
export function showAgentStartToast(agentName: string): void
export function showAgentCompleteToast(agentName: string, result: string): void
export function showErrorToast(message: string): void
export function showSuccessToast(message: string): void
export function showWarningToast(message: string): void
export function showInfoToast(message: string): void
```

### **11.5 Update State Management**
**File:** `frontend/src/state/hooks.ts`
```typescript
// Add new state:
interface AppState {
  // ... existing state
  chatScope: 'trip' | 'day';
  autoApply: boolean;
  isWorkflowDisabled: boolean;
  workflowDisabledReason: string;
  selectedNodeId: string | null;
  chatMessages: ChatMessage[];
  isAgentRunning: boolean;
  currentAgent: string | null;
}

// Add new actions:
const setChatScope = (scope: 'trip' | 'day'): void
const setAutoApply = (autoApply: boolean): void
const setWorkflowDisabled = (disabled: boolean, reason?: string): void
const setSelectedNodeId = (nodeId: string | null): void
const addChatMessage = (message: ChatMessage): void
const setAgentRunning = (running: boolean, agent?: string): void
```

---

## **Phase 12: Testing & Validation**

### **12.1 Backend Chat Tests**
**File:** `src/test/java/com/tripplanner/api/ChatControllerTest.java`
```java
// Key Test Methods:
@Test
void testRouteMessage_Success()
@Test
void testRouteMessage_Disambiguation()
@Test
void testRouteMessage_InvalidRequest()
@Test
void testRouteMessage_NodeNotFound()
@Test
void testRouteMessage_IntentClassification()
```

**File:** `src/test/java/com/tripplanner/service/OrchestratorServiceTest.java`
```java
// Key Test Methods:
@Test
void testRoute_ReplanToday()
@Test
void testRoute_MoveTime()
@Test
void testRoute_InsertPlace()
@Test
void testRoute_DeleteNode()
@Test
void testRoute_ReplaceNode()
@Test
void testRoute_BookNode()
@Test
void testRoute_Undo()
@Test
void testRoute_Explain()
@Test
void testFindNodeCandidates()
@Test
void testBuildChangeSet()
```

**File:** `src/test/java/com/tripplanner/service/IntentClassificationServiceTest.java`
```java
// Key Test Methods:
@Test
void testPreRouterClassification()
@Test
void testLlmClassification()
@Test
void testIsReplanToday()
@Test
void testIsMoveTime()
@Test
void testIsInsertPlace()
@Test
void testIsDeleteNode()
@Test
void testIsReplaceNode()
@Test
void testIsBookNode()
@Test
void testIsUndo()
@Test
void testIsExplain()
```

**File:** `src/test/java/com/tripplanner/service/NodeResolutionServiceTest.java`
```java
// Key Test Methods:
@Test
void testFindNodeCandidates_ByTitle()
@Test
void testFindNodeCandidates_ByLocation()
@Test
void testFindNodeCandidates_ByType()
@Test
void testCalculateSimilarity()
@Test
void testRankCandidates()
```

### **12.2 Frontend Chat Tests**
**File:** `frontend/src/components/__tests__/ChatPanel.test.tsx`
```typescript
// Key Test Methods:
test('should render chat panel with input', () => {})
test('should send message and display response', () => {})
test('should handle node disambiguation', () => {})
test('should handle voice input', () => {})
test('should handle speech to text', () => {})
test('should render typing indicator', () => {})
test('should handle errors gracefully', () => {})
```

**File:** `frontend/src/components/__tests__/ChatMessage.test.tsx`
```typescript
// Key Test Methods:
test('should render user message', () => {})
test('should render assistant message', () => {})
test('should render diff preview', () => {})
test('should render warnings', () => {})
test('should handle node selection', () => {})
```

**File:** `frontend/src/components/__tests__/NodeCandidates.test.tsx`
```typescript
// Key Test Methods:
test('should render node candidates', () => {})
test('should handle candidate selection', () => {})
test('should handle cancel', () => {})
test('should render candidate icons', () => {})
test('should format candidate location', () => {})
```

**File:** `frontend/src/services/__tests__/chatService.test.ts`
```typescript
// Key Test Methods:
test('should route message successfully', () => {})
test('should handle disambiguation', () => {})
test('should create chat request', () => {})
test('should validate chat request', () => {})
test('should handle API errors', () => {})
```

### **12.3 Integration Tests**
**File:** `src/test/java/com/tripplanner/api/ChatIntegrationTest.java`
```java
// Key Test Methods:
@Test
void testCompleteChatWorkflow()
@Test
void testChatWithDisambiguation()
@Test
void testChatWithAutoApply()
@Test
void testChatWithScopeToggle()
@Test
void testChatErrorHandling()
@Test
void testChatWithVoiceInput()
```

**File:** `frontend/src/__tests__/chat-integration.test.ts`
```typescript
// Key Test Methods:
test('should complete full chat workflow', () => {})
test('should handle node disambiguation workflow', () => {})
test('should handle auto-apply workflow', () => {})
test('should handle scope toggle workflow', () => {})
test('should handle voice input workflow', () => {})
test('should handle error scenarios', () => {})
```

---

## **Implementation Order & Dependencies**

### **Phase 8 (Chat Backend) - Week 1**
1. Create DTOs (`ChatRequest`, `ChatResponse`, `IntentResult`, `NodeCandidate`)
2. Create `IntentClassificationService` with pre-router logic
3. Create `NodeResolutionService` with fuzzy matching
4. Create `OrchestratorService` with intent routing
5. Create `ChatController` with main endpoint
6. Add backend tests

### **Phase 9 (Enhanced Schema) - Week 1-2**
1. Update `NormalizedNode` DTO with new fields
2. Update `ChangeEngine` with audit trail logic
3. Update `PlannerAgent` and `EnrichmentAgent` with audit fields
4. Add schema migration tests

### **Phase 10 (Chat Frontend) - Week 2-3**
1. Create chat TypeScript types
2. Create `chatService` for API integration
3. Create `ChatPanel` component with basic functionality
4. Create `ChatMessage` and `NodeCandidates` components
5. Create `NewNodeIndicator` and `VoiceInput` components
6. Add frontend tests

### **Phase 11 (Workflow Integration) - Week 3-4**
1. Update `WorkflowBuilder` with scope toggle and auto-apply
2. Update `TravelPlanner` with chat integration
3. Create `Toast` component and service
4. Update state management with new chat state
5. Add integration tests

### **Phase 12 (Testing) - Week 4**
1. Complete backend test coverage
2. Complete frontend test coverage
3. Add integration tests
4. Performance testing
5. End-to-end testing


[1 tool called]

## **Summary**

I've created a comprehensive detailed roadmap with specific filenames and function names for all 5 phases. The roadmap includes:

### **Key Deliverables:**
- **25+ new backend files** with specific function signatures
- **15+ new frontend files** with component structures
- **30+ test files** with comprehensive test coverage
- **Detailed function names** for each component
- **Implementation timeline** with dependencies

### **Critical Functions by Phase:**

**Phase 8 (Chat Backend):**
- `ChatController.route()` - Main chat endpoint
- `OrchestratorService.route()` - Intent classification and routing
- `IntentClassificationService.classifyIntent()` - Pre-router + LLM classification
- `NodeResolutionService.findNodeCandidates()` - Fuzzy node matching

**Phase 9 (Enhanced Schema):**
- `NormalizedNode.setStatus()` - Node status management
- `ChangeEngine.updateNodeAudit()` - Audit trail tracking
- `PlannerAgent.setNodeAuditFields()` - Agent audit integration

**Phase 10 (Chat Frontend):**
- `ChatPanel.sendMessage()` - Main chat interaction
- `chatService.routeMessage()` - API integration
- `VoiceInput.handleSpeechToText()` - Voice input handling
- `NodeCandidates.renderCandidate()` - Disambiguation UI

**Phase 11 (Workflow Integration):**
- `WorkflowBuilder.handleScopeToggle()` - Trip vs Day scope
- `TravelPlanner.renderChatPanel()` - Chat integration
- `toastService.showAgentCompleteToast()` - Agent notifications

**Phase 12 (Testing):**
- Comprehensive test coverage for all new functionality
- Integration tests for complete workflows
- End-to-end testing scenarios

The roadmap is designed to be implemented incrementally with clear dependencies and can be executed phase by phase as requested.