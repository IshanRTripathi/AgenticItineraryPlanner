# Fix Roadmap - SSE Real-Time Updates & Data Integrity

## Overview

This roadmap provides a phased approach to fixing the four critical issues identified in the itinerary creation flow. Fixes are organized by priority and dependencies.

---

## Phase 1: SSE Authentication Foundation (Week 1)

**Goal**: Fix authentication infrastructure to support SSE connections

### Task 1.1: Fix SSE Auth Filter Order
**Priority**: CRITICAL  
**Effort**: 2 hours  
**Files**: `src/main/java/com/tripplanner/config/FirebaseAuthConfig.java`

**Changes**:
1. Modify `FirebaseAuthFilter` to skip SSE endpoints entirely
2. Let `FirebaseSseAuthFilter` handle all SSE authentication
3. Remove false warning logs for SSE paths

```java
// In FirebaseAuthFilter.doFilterInternal()
if (isSseEndpoint(path)) {
    filterChain.doFilter(request, response);
    return; // Let FirebaseSseAuthFilter handle it
}

private boolean isSseEndpoint(String path) {
    return path.startsWith("/api/v1/agents/stream") ||
           path.startsWith("/api/v1/agents/events/") ||
           path.startsWith("/api/v1/itineraries/patches");
}
```

**Testing**:
- Verify no auth warnings for SSE endpoints
- Verify SSE endpoints still validate tokens
- Verify regular endpoints still require auth

---

### Task 1.2: Add Token to Patches Stream
**Priority**: CRITICAL  
**Effort**: 1 hour  
**Files**: `frontend/src/services/apiClient.ts`

**Changes**:
1. Add auth token to patches stream URL (same as agent stream)

```typescript
createPatchesEventStream(itineraryId: string, executionId?: string): EventSource {
  let url = `${this.baseUrl}/itineraries/patches?itineraryId=${itineraryId}`;
  if (executionId) {
    url += `&executionId=${executionId}`;
  }
  
  // ADD AUTH TOKEN (same as createAgentEventStream)
  if (this.authToken) {
    url += `&token=${encodeURIComponent(this.authToken)}`;
  }
  
  const eventSource = new EventSource(url);
  return eventSource;
}
```

**Testing**:
- Verify patches stream includes token parameter
- Verify backend accepts token from query parameter
- Verify connection succeeds with valid token

---

### Task 1.3: Add Patches Endpoint Auth Handler
**Priority**: CRITICAL  
**Effort**: 2 hours  
**Files**: `src/main/java/com/tripplanner/config/FirebaseAuthConfig.java`

**Changes**:
1. Update `FirebaseSseAuthFilter` to handle patches endpoint
2. Add patches path to SSE endpoint check

```java
private boolean isSseEndpoint(String path) {
    return path.startsWith("/api/v1/agents/stream") ||
           path.startsWith("/api/v1/agents/events/") ||
           path.startsWith("/api/v1/itineraries/patches"); // ADD THIS
}
```

**Testing**:
- Verify patches endpoint validates token
- Verify patches endpoint sets userId attribute
- Verify patches endpoint allows connection with valid token

---

## Phase 2: SSE Connection Initialization (Week 1-2)

**Goal**: Establish SSE connections automatically after itinerary creation

### Task 2.1: Add SSE Connection Hook in Itinerary Creation
**Priority**: CRITICAL  
**Effort**: 3 hours  
**Files**: 
- `frontend/src/pages/CreateItineraryPage.tsx` (or equivalent)
- `frontend/src/hooks/useItineraryCreation.ts` (if exists)

**Changes**:
1. Import sseManager
2. Call `sseManager.connect()` immediately after receiving itinerary ID
3. Add connection status tracking

```typescript
// After createItinerary API call succeeds
const response = await apiClient.createItinerary(request);
const itineraryId = response.itinerary;

// ESTABLISH SSE CONNECTION IMMEDIATELY
console.log('[CreateItinerary] Establishing SSE connection for:', itineraryId);
sseManager.connect(itineraryId);

// Navigate to itinerary page
navigate(`/itinerary/${itineraryId}`);
```

**Testing**:
- Verify sseManager.connect() is called
- Verify connection established within 2 seconds
- Verify console logs show connection success
- Verify events are received

---

### Task 2.2: Add SSE Connection Status UI
**Priority**: HIGH  
**Effort**: 4 hours  
**Files**: 
- `frontend/src/components/ItineraryProgress.tsx` (or equivalent)
- `frontend/src/hooks/useSSEConnection.ts` (new)

**Changes**:
1. Create hook to track SSE connection status
2. Display connection status in UI
3. Show warning if SSE fails and polling is used

```typescript
// New hook
export function useSSEConnection(itineraryId: string) {
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  
  useEffect(() => {
    const manager = sseManager;
    
    // Check connection status
    const checkConnection = () => {
      setIsConnected(manager.getIsConnected());
    };
    
    const interval = setInterval(checkConnection, 1000);
    return () => clearInterval(interval);
  }, [itineraryId]);
  
  return { isConnected, connectionError };
}
```

**Testing**:
- Verify UI shows "Connected" when SSE works
- Verify UI shows "Using polling" when SSE fails
- Verify warning is dismissible

---

### Task 2.3: Add SSE Connection Retry Logic
**Priority**: HIGH  
**Effort**: 3 hours  
**Files**: `frontend/src/services/sseManager.ts`

**Changes**:
1. Add retry counter and max retries
2. Implement exponential backoff
3. Log retry attempts

```typescript
private async connectWithRetry(itineraryId: string, attempt: number = 1): Promise<void> {
  const maxRetries = 3;
  
  try {
    this.connect(itineraryId);
    console.log(`[SSE] Connection established on attempt ${attempt}`);
  } catch (error) {
    if (attempt < maxRetries) {
      const delay = 1000 * Math.pow(2, attempt - 1);
      console.log(`[SSE] Retry ${attempt}/${maxRetries} in ${delay}ms`);
      await new Promise(resolve => setTimeout(resolve, delay));
      return this.connectWithRetry(itineraryId, attempt + 1);
    } else {
      console.error(`[SSE] Failed after ${maxRetries} attempts`);
      this.options.onError?.(new Error('SSE connection failed'));
    }
  }
}
```

**Testing**:
- Verify retries happen on failure
- Verify exponential backoff (1s, 2s, 4s)
- Verify gives up after 3 attempts
- Verify error callback is called

---

## Phase 3: UserId Persistence (Week 2)

**Goal**: Ensure userId never becomes null during itinerary lifecycle

### Task 3.1: Add Token Refresh Before Polling
**Priority**: CRITICAL  
**Effort**: 3 hours  
**Files**: `frontend/src/services/apiClient.ts`

**Changes**:
1. Check token expiry before each request
2. Refresh token proactively if expiring soon
3. Don't clear token on refresh failure, retry instead

```typescript
private async ensureValidToken(): Promise<boolean> {
  if (!this.authToken) {
    return false;
  }
  
  // Check if token is expiring soon (within 5 minutes)
  try {
    const { authService } = await import('./authService');
    const tokenExpiry = await authService.getTokenExpiry();
    const now = Date.now();
    const fiveMinutes = 5 * 60 * 1000;
    
    if (tokenExpiry - now < fiveMinutes) {
      console.log('[ApiClient] Token expiring soon, refreshing...');
      return await this.refreshAuthToken();
    }
    
    return true;
  } catch (error) {
    console.error('[ApiClient] Error checking token expiry:', error);
    return false;
  }
}

// Call before each request
private async request<T>(...) {
  await this.ensureValidToken();
  // ... rest of request logic
}
```

**Testing**:
- Verify token is refreshed before expiry
- Verify requests continue with new token
- Verify userId remains consistent
- Verify no anonymous requests after auth

---

### Task 3.2: Add UserId Validation in Backend
**Priority**: HIGH  
**Effort**: 2 hours  
**Files**: `src/main/java/com/tripplanner/service/ItineraryJsonService.java`

**Changes**:
1. Add validation when saving itinerary
2. Log error if userId is null
3. Throw exception to prevent saving with null userId

```java
public void saveItinerary(NormalizedItinerary itinerary) {
    // VALIDATE USERID
    if (itinerary.getUserId() == null || itinerary.getUserId().trim().isEmpty()) {
        logger.error("Attempted to save itinerary with null userId: {}", itinerary.getItineraryId());
        logger.error("Stack trace:", new Exception("UserId is null"));
        throw new IllegalStateException("Cannot save itinerary without userId");
    }
    
    // ... rest of save logic
}
```

**Testing**:
- Verify exception thrown if userId is null
- Verify error logged with stack trace
- Verify itinerary not saved with null userId

---

### Task 3.3: Add UserId Immutability Check
**Priority**: MEDIUM  
**Effort**: 2 hours  
**Files**: `src/main/java/com/tripplanner/dto/NormalizedItinerary.java`

**Changes**:
1. Make userId final or add setter validation
2. Log warning if userId changes
3. Prevent userId from being overwritten

```java
public class NormalizedItinerary {
    private final String userId; // Make final
    
    // Remove setUserId() or add validation
    public void setUserId(String userId) {
        if (this.userId != null && !this.userId.equals(userId)) {
            logger.warn("Attempted to change userId from {} to {}", this.userId, userId);
            logger.warn("Stack trace:", new Exception("UserId change attempt"));
            throw new IllegalStateException("Cannot change userId after creation");
        }
        this.userId = userId;
    }
}
```

**Testing**:
- Verify userId cannot be changed after creation
- Verify warning logged on change attempt
- Verify exception thrown on change attempt

---

## Phase 4: Edge Update Robustness (Week 2-3)

**Goal**: Fix edge updates to properly handle day numbers

### Task 4.1: Add Edge Update Validation
**Priority**: MEDIUM  
**Effort**: 2 hours  
**Files**: `src/main/java/com/tripplanner/service/ChangeEngine.java`

**Changes**:
1. Validate edge updates before applying
2. Log detailed error if day is null
3. Skip invalid edge updates

```java
private boolean applyEdgeUpdate(NormalizedItinerary itinerary, EdgeUpdate update) {
    // VALIDATE BEFORE APPLYING
    if (update.getDay() == null) {
        logger.error("Edge update has null day number");
        logger.error("Edge details: source={}, target={}, type={}", 
            update.getSourceNodeId(), 
            update.getTargetNodeId(),
            update.getEdgeType());
        return false;
    }
    
    NormalizedDay targetDay = findDayByNumber(itinerary, update.getDay());
    if (targetDay == null) {
        logger.warn("Day {} not found for edge update: {} -> {}", 
            update.getDay(),
            update.getSourceNodeId(),
            update.getTargetNodeId());
        return false;
    }
    
    // ... apply edge
}
```

**Testing**:
- Verify null day edges are rejected
- Verify detailed error logged
- Verify edge details included in log
- Verify enrichment continues after rejection

---

### Task 4.2: Fix Enrichment Agent Edge Creation
**Priority**: MEDIUM  
**Effort**: 4 hours  
**Files**: `src/main/java/com/tripplanner/agents/EnrichmentAgent.java`

**Changes**:
1. Find where edge updates are created
2. Ensure day number is set from node's day
3. Add validation before creating edge update

```java
private EdgeUpdate createEdgeUpdate(NormalizedNode sourceNode, NormalizedNode targetNode) {
    // DETERMINE DAY NUMBER FROM NODES
    Integer dayNumber = determineDayNumber(sourceNode, targetNode);
    
    if (dayNumber == null) {
        logger.warn("Cannot determine day number for edge: {} -> {}", 
            sourceNode.getId(), targetNode.getId());
        return null; // Don't create invalid edge
    }
    
    return EdgeUpdate.builder()
        .sourceNodeId(sourceNode.getId())
        .targetNodeId(targetNode.getId())
        .day(dayNumber) // SET DAY NUMBER
        .edgeType(determineEdgeType(sourceNode, targetNode))
        .build();
}

private Integer determineDayNumber(NormalizedNode source, NormalizedNode target) {
    // Logic to determine which day the edge belongs to
    // Could be source day, target day, or need to be calculated
    if (source.getDayNumber() != null) {
        return source.getDayNumber();
    }
    if (target.getDayNumber() != null) {
        return target.getDayNumber();
    }
    return null;
}
```

**Testing**:
- Verify all edge updates have day numbers
- Verify no null day warnings in logs
- Verify edges are properly added to days
- Verify graph structure is complete

---

### Task 4.3: Add Edge Update Metrics
**Priority**: LOW  
**Effort**: 2 hours  
**Files**: `src/main/java/com/tripplanner/service/ChangeEngine.java`

**Changes**:
1. Track edge update success/failure counts
2. Log summary after enrichment
3. Alert if failure rate is high

```java
public class EdgeUpdateMetrics {
    private int totalAttempts = 0;
    private int successCount = 0;
    private int nullDayCount = 0;
    private int missingDayCount = 0;
    
    public void recordSuccess() { successCount++; totalAttempts++; }
    public void recordNullDay() { nullDayCount++; totalAttempts++; }
    public void recordMissingDay() { missingDayCount++; totalAttempts++; }
    
    public void logSummary() {
        logger.info("Edge Update Summary:");
        logger.info("  Total: {}", totalAttempts);
        logger.info("  Success: {} ({}%)", successCount, getSuccessRate());
        logger.info("  Null Day: {}", nullDayCount);
        logger.info("  Missing Day: {}", missingDayCount);
        
        if (getSuccessRate() < 90) {
            logger.warn("Edge update success rate below 90%!");
        }
    }
}
```

**Testing**:
- Verify metrics are logged after enrichment
- Verify success rate calculation is correct
- Verify warning logged if rate is low

---

## Phase 5: Monitoring & Observability (Week 3)

**Goal**: Add comprehensive logging and monitoring for SSE and auth

### Task 5.1: Add SSE Connection Metrics
**Priority**: MEDIUM  
**Effort**: 3 hours  
**Files**: `src/main/java/com/tripplanner/service/AgentEventBus.java`

**Changes**:
1. Track emitter count per itinerary
2. Log emitter registration/unregistration
3. Log event delivery success/failure

```java
public class AgentEventBus {
    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> emitterCounts = new ConcurrentHashMap<>();
    
    public void register(String itineraryId, SseEmitter emitter) {
        emitters.computeIfAbsent(itineraryId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitterCounts.computeIfAbsent(itineraryId, k -> new AtomicInteger(0)).incrementAndGet();
        
        logger.info("SSE emitter registered for itinerary: {}, total emitters: {}", 
            itineraryId, emitterCounts.get(itineraryId).get());
    }
    
    public void publishEvent(String itineraryId, AgentEvent event) {
        Set<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
        
        if (itineraryEmitters == null || itineraryEmitters.isEmpty()) {
            logger.warn("No SSE emitters found for itinerary: {}, expected: {}", 
                itineraryId, emitterCounts.getOrDefault(itineraryId, new AtomicInteger(0)).get());
            return;
        }
        
        logger.debug("Publishing event to {} emitters for itinerary: {}", 
            itineraryEmitters.size(), itineraryId);
        
        // ... send events
    }
}
```

**Testing**:
- Verify emitter count is logged
- Verify event delivery count is logged
- Verify warnings show expected vs actual emitters

---

### Task 5.2: Add Frontend SSE Diagnostics
**Priority**: MEDIUM  
**Effort**: 2 hours  
**Files**: `frontend/src/services/sseManager.ts`

**Changes**:
1. Add detailed connection logging
2. Log all SSE events received
3. Add connection state tracking

```typescript
private connectPatchesStream(itineraryId: string): void {
  console.log('[SSE] Connecting patches stream:', {
    itineraryId,
    url: this.getPatchesUrl(itineraryId),
    hasToken: !!apiClient.authToken,
    timestamp: new Date().toISOString()
  });
  
  this.patchesEventSource = apiClient.createPatchesEventStream(itineraryId, this.options.executionId);
  
  this.patchesEventSource.onopen = () => {
    console.log('[SSE] Patches stream OPENED:', {
      itineraryId,
      readyState: this.patchesEventSource?.readyState,
      timestamp: new Date().toISOString()
    });
  };
  
  this.patchesEventSource.onerror = (error) => {
    console.error('[SSE] Patches stream ERROR:', {
      itineraryId,
      error,
      readyState: this.patchesEventSource?.readyState,
      timestamp: new Date().toISOString()
    });
  };
}
```

**Testing**:
- Verify detailed logs in browser console
- Verify connection state is logged
- Verify errors include full context

---

### Task 5.3: Add Health Check Endpoint
**Priority**: LOW  
**Effort**: 2 hours  
**Files**: `src/main/java/com/tripplanner/controller/HealthController.java` (new)

**Changes**:
1. Create health check endpoint
2. Include SSE emitter counts
3. Include auth status

```java
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    
    @Autowired
    private AgentEventBus agentEventBus;
    
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
            "healthy",
            agentEventBus.getTotalEmitterCount(),
            agentEventBus.getActiveItineraryCount()
        ));
    }
    
    @GetMapping("/sse/{itineraryId}")
    public ResponseEntity<SseHealthResponse> sseHealth(@PathVariable String itineraryId) {
        int emitterCount = agentEventBus.getEmitterCount(itineraryId);
        return ResponseEntity.ok(new SseHealthResponse(
            itineraryId,
            emitterCount,
            emitterCount > 0 ? "connected" : "disconnected"
        ));
    }
}
```

**Testing**:
- Verify health endpoint returns emitter counts
- Verify SSE health shows per-itinerary status
- Verify endpoint is accessible without auth

---

## Testing Strategy

### Unit Tests
- SSE connection establishment
- Token refresh logic
- Edge update validation
- UserId immutability

### Integration Tests
- End-to-end itinerary creation with SSE
- Token expiry and refresh during long operations
- Edge updates during enrichment
- UserId persistence through async operations

### Manual Testing
- Create itinerary and verify SSE connection in browser console
- Monitor network tab for SSE connections
- Verify real-time updates appear in UI
- Test with expired token
- Test with network interruption

---

## Rollout Plan

### Week 1
- Deploy Phase 1 (SSE Auth Foundation)
- Monitor logs for auth warnings (should disappear)
- Deploy Phase 2 Task 2.1 (SSE Connection Hook)
- Monitor SSE connection success rate

### Week 2
- Deploy Phase 2 remaining tasks (SSE UI & Retry)
- Deploy Phase 3 (UserId Persistence)
- Monitor userId consistency
- Deploy Phase 4 Task 4.1 (Edge Validation)

### Week 3
- Deploy Phase 4 remaining tasks (Edge Fix)
- Deploy Phase 5 (Monitoring)
- Monitor edge update success rate
- Collect metrics for 1 week

### Week 4
- Review metrics
- Fix any remaining issues
- Document lessons learned
- Update runbooks

---

## Success Criteria

### Phase 1 Success
- ✅ No auth warnings for SSE endpoints
- ✅ SSE endpoints validate tokens correctly
- ✅ Patches stream includes auth token

### Phase 2 Success
- ✅ SSE connection established within 2 seconds
- ✅ Real-time updates appear in UI
- ✅ Connection success rate >95%
- ✅ Retry logic works on failure

### Phase 3 Success
- ✅ UserId never becomes null
- ✅ No anonymous requests after authentication
- ✅ Token refresh works proactively
- ✅ UserId validation prevents null saves

### Phase 4 Success
- ✅ No "Day not found" warnings
- ✅ Edge update success rate >90%
- ✅ Graph structure is complete
- ✅ Metrics show improvement

### Phase 5 Success
- ✅ Comprehensive SSE logging
- ✅ Health check endpoint works
- ✅ Diagnostics help debug issues quickly

---

## Rollback Plan

Each phase can be rolled back independently:

### Phase 1 Rollback
- Revert auth filter changes
- System continues with current behavior (warnings but functional)

### Phase 2 Rollback
- Remove SSE connection hook
- System falls back to polling (current behavior)

### Phase 3 Rollback
- Remove userId validation
- System allows null userId (current behavior)

### Phase 4 Rollback
- Remove edge validation
- System logs warnings but continues (current behavior)

### Phase 5 Rollback
- Remove monitoring code
- No impact on functionality

---

## Risk Assessment

### Low Risk
- Phase 1: Auth filter changes (well-tested pattern)
- Phase 5: Monitoring additions (no functional changes)

### Medium Risk
- Phase 2: SSE connection changes (may affect user experience)
- Phase 4: Edge update changes (may affect graph structure)

### High Risk
- Phase 3: UserId validation (may break existing flows)

**Mitigation**: Deploy Phase 3 with feature flag, enable gradually

---

## Maintenance Plan

### Daily
- Monitor SSE connection success rate
- Monitor edge update success rate
- Check for userId null errors

### Weekly
- Review SSE connection logs
- Review token refresh logs
- Review edge update metrics

### Monthly
- Analyze trends in connection failures
- Update documentation based on issues
- Review and update monitoring thresholds

---

## Documentation Updates

### Developer Documentation
- Add SSE connection flow diagram
- Document token refresh strategy
- Document edge update data model
- Add troubleshooting guide

### Operations Documentation
- Add SSE monitoring runbook
- Add auth troubleshooting guide
- Add edge update debugging guide
- Update deployment checklist

---

## Estimated Timeline

- **Phase 1**: 5 hours (1 day)
- **Phase 2**: 10 hours (2 days)
- **Phase 3**: 7 hours (1 day)
- **Phase 4**: 8 hours (1 day)
- **Phase 5**: 7 hours (1 day)

**Total**: 37 hours (~1 week of focused work)

**With testing and deployment**: 2-3 weeks

---

## Next Steps

1. Review this roadmap with team
2. Prioritize phases based on business impact
3. Create JIRA tickets for each task
4. Assign owners for each phase
5. Schedule deployment windows
6. Set up monitoring dashboards
7. Begin Phase 1 implementation
