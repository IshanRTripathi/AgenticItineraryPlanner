# Root Cause Analysis - SSE & Data Integrity Issues

## Executive Summary

Analysis of production logs and source code reveals four interconnected issues preventing real-time updates during itinerary generation. The root causes have been identified through code inspection and log correlation.

---

## Issue 1: SSE Connection Never Established

### Symptoms
- 17 warnings: "No SSE emitters found for itinerary"
- Events published but no recipients
- Frontend falls back to polling (52 GET requests)

### Root Cause Analysis

#### Frontend Investigation (`frontend/src/services/sseManager.ts`)

**Finding 1: SSE Connection Not Triggered**
```typescript
// Line 67-75: connect() method exists but is never called automatically
connect(itineraryId: string): void {
  if (this.itineraryId === itineraryId && this.isConnected) {
    console.log('[SSE] Already connected to itinerary:', itineraryId);
    return;
  }
  // ... connection logic
}
```

**Problem**: The `sseManager.connect()` method is defined but there's no evidence in the codebase of it being called after itinerary creation.

**Finding 2: EventSource Creation Issues**
```typescript
// Line 91-93: createPatchesEventStream in apiClient.ts
createPatchesEventStream(itineraryId: string, executionId?: string): EventSource {
  let url = `${this.baseUrl}/itineraries/patches?itineraryId=${itineraryId}`;
  // NO AUTH TOKEN ADDED - This is the critical bug
```

**Problem**: The `createPatchesEventStream` method does NOT add the auth token as a query parameter, unlike `createAgentEventStream` which does.

**Finding 3: Dual Stream Confusion**
```typescript
// Two separate streams are created:
// 1. Patches stream: /itineraries/patches
// 2. Agent stream: /agents/events/{id}
```

**Problem**: The system tries to connect to two different SSE endpoints, but only one (`/agents/events`) has proper auth token handling.

#### Backend Investigation (`src/main/java/com/tripplanner/controller/AgentController.java`)

**Finding 4: SSE Endpoint Ready But No Connections**
```java
// Line 147-151: streamEvents() method logs connection attempts
logger.info("=== SSE EVENTS REQUEST ===");
logger.info("Itinerary ID: {}", itineraryId);
logger.info("User ID: {}", userId != null ? userId : "anonymous");
```

**Problem**: These log messages NEVER appear in the production logs, confirming that the frontend never calls this endpoint.

### Root Cause Summary

1. **Frontend never calls `sseManager.connect()`** after receiving itinerary creation response
2. **`createPatchesEventStream` missing auth token** in query parameters
3. **No initialization code** to establish SSE connection in the itinerary creation flow
4. **EventSource API limitation**: Cannot set custom headers, requires token in URL

---

## Issue 2: SSE Authentication Failures

### Symptoms
- Multiple warnings: "Missing or invalid Authorization header for path: /api/v1/agents/events/{id}"
- Auth warnings appear even though no connection is made

### Root Cause Analysis

#### Authentication Filter Investigation (`src/main/java/com/tripplanner/config/FirebaseAuthConfig.java`)

**Finding 1: Wrong Filter Applied**
```java
// Line 45-60: FirebaseAuthFilter checks Authorization header
String authHeader = request.getHeader("Authorization");
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    logger.warn("Missing or invalid Authorization header for path: {}", path);
    // ...
}
```

**Problem**: The main `FirebaseAuthFilter` runs BEFORE `FirebaseSseAuthFilter` and logs warnings for SSE endpoints even though SSE uses query parameters, not headers.

**Finding 2: Filter Order Issue**
```java
// Line 327-333: Filter registration order
registration.setOrder(Ordered.HIGHEST_PRECEDENCE);     // FirebaseAuthFilter
registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // FirebaseSseAuthFilter
```

**Problem**: The main auth filter runs first and logs warnings before the SSE-specific filter can handle the request properly.

**Finding 3: EventSource API Limitation**
```typescript
// frontend/src/services/apiClient.ts Line 448-454
createAgentEventStream(itineraryId: string): EventSource {
  let url = `${this.baseUrl}/agents/events/${itineraryId}`;
  if (this.authToken) {
    url += `?token=${encodeURIComponent(this.authToken)}`;
  }
  const eventSource = new EventSource(url);
  // EventSource API doesn't support custom headers!
}
```

**Problem**: Browser's EventSource API doesn't support setting custom headers, forcing token to be in URL query parameter.

### Root Cause Summary

1. **Filter order causes false warnings**: Main auth filter logs warnings before SSE filter can process
2. **EventSource API limitation**: Cannot use Authorization header, must use query parameter
3. **Inconsistent token handling**: Patches stream doesn't add token, agent stream does

---

## Issue 3: UserId Becomes Null

### Symptoms
- First 3 GET requests: `userId='PEoULmiNa2dBOZrlBcXs8EZIkXs1'`
- After skeleton generation: `userId='null'`
- Remains null for remaining 49 requests

### Root Cause Analysis

#### Authentication Flow Investigation

**Finding 1: Optional Auth for /json Endpoints**
```java
// FirebaseAuthConfig.java Line 75-91
if (path.endsWith("/json")) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        // Authenticate if token present
    }
    filterChain.doFilter(request, response);
    return; // Continue WITHOUT authentication if no token
}
```

**Problem**: The `/json` endpoints allow anonymous access. If the frontend stops sending the auth token, requests succeed but userId is null.

**Finding 2: Frontend Token Management**
```typescript
// apiClient.ts Line 113-118
const headers: HeadersInit = {
  'Content-Type': 'application/json',
  ...options.headers,
};
if (this.authToken) {
  headers['Authorization'] = `Bearer ${this.authToken}`;
}
```

**Problem**: If `this.authToken` becomes null or undefined, requests proceed without authentication.

**Finding 3: Token Refresh Timing**
```typescript
// apiClient.ts Line 169-180
if (response.status === 401 && this.authToken) {
  const tokenRefreshed = await this.refreshAuthToken();
  if (tokenRefreshed) {
    continue; // Retry with new token
  } else {
    this.clearAuthToken(); // CLEARS TOKEN ON FAILURE
  }
}
```

**Problem**: If token refresh fails, the token is cleared and subsequent requests are anonymous.

**Finding 4: Polling Without Auth**
- Frontend makes 52 GET requests
- After initial 3 authenticated requests, remaining 49 are anonymous
- Suggests token was cleared or lost during the flow

### Root Cause Summary

1. **Token refresh failure**: Token likely expired and refresh failed
2. **Token cleared on failure**: `clearAuthToken()` called, making subsequent requests anonymous
3. **Optional auth allows anonymous access**: `/json` endpoints don't enforce authentication
4. **No token re-acquisition**: Frontend doesn't attempt to get a fresh token after clearing

---

## Issue 4: Edge Update Failures

### Symptoms
- 23 consecutive warnings: "Day not found for edge update: null"
- Occurs during enrichment phase
- All warnings show `day: null`

### Root Cause Analysis

#### ChangeEngine Investigation (`src/main/java/com/tripplanner/service/ChangeEngine.java`)

**Finding 1: Null Day Number in Edge Updates**
```java
// Line 1089-1093
NormalizedDay targetDay = findDayByNumber(itinerary, day);
if (targetDay == null) {
    logger.warn("Day not found for edge update: {}", day);
    return false;
}
```

**Problem**: The `day` parameter is `null`, not a missing day number. This indicates the edge update operation itself has null data.

**Finding 2: Edge Update Structure**
```java
// Likely structure of edge update (inferred from logs)
class EdgeUpdate {
    String sourceNodeId;
    String targetNodeId;
    Integer day;  // THIS IS NULL
}
```

**Problem**: Edge updates are being created with `day = null` instead of the actual day number.

**Finding 3: Enrichment Agent Creates Edges**
- Enrichment phase applies 46 operations
- 23 of these are edge updates with null day
- Suggests enrichment agent doesn't properly set day numbers when creating edges

**Finding 4: Graph Structure Issue**
- Edges connect nodes across days
- If day number is null, the system can't determine which day's edge list to update
- Results in orphaned edges that aren't added to any day

### Root Cause Summary

1. **Enrichment agent bug**: Creates edge updates without setting day number
2. **Missing validation**: Edge updates not validated before being queued
3. **Silent failure**: System logs warning but continues, leaving graph incomplete
4. **Data structure mismatch**: Edge updates may be using wrong data model

---

## Cross-Cutting Issues

### Issue: Lack of Integration Testing
- No tests verify SSE connection establishment
- No tests verify end-to-end authentication flow
- No tests verify userId persistence through async operations

### Issue: Insufficient Error Handling
- SSE connection failures are silent (no user notification)
- Token refresh failures don't trigger re-authentication flow
- Edge update failures don't prevent enrichment from completing

### Issue: Logging Gaps
- No log when frontend should connect to SSE
- No log when token is cleared
- No log showing edge update data structure

---

## Impact Assessment

### High Impact
1. **No real-time updates**: Users see no progress, think system is broken
2. **UserId loss**: Potential security issue, orphaned itineraries

### Medium Impact
3. **Auth warnings**: Clutter logs, make debugging harder
4. **Edge failures**: Graph structure incomplete, may affect routing

### Low Impact
5. **Polling overhead**: 52 requests vs SSE, but system still works

---

## Recommended Fix Priority

1. **CRITICAL**: Fix SSE connection initialization (Issue 1)
2. **CRITICAL**: Fix userId persistence (Issue 3)
3. **HIGH**: Fix SSE authentication warnings (Issue 2)
4. **MEDIUM**: Fix edge update null handling (Issue 4)

---

## Dependencies Between Issues

```
Issue 1 (SSE Connection) 
    ↓ depends on
Issue 2 (SSE Auth) 
    ↓ affects
Issue 3 (UserId Loss)
    ↓ independent
Issue 4 (Edge Updates)
```

Fixing Issue 1 requires fixing Issue 2 first. Issue 3 is independent but critical. Issue 4 is independent and can be fixed in parallel.
