# Test Documentation - SSE Real-Time Updates & Data Integrity Fixes

## Overview

This document describes the comprehensive test suite created to validate the SSE real-time updates and data integrity fixes.

---

## Test Files Created

### 1. ItineraryJsonServiceUserIdValidationTest.java
**Location**: `src/test/java/com/tripplanner/testing/service/ItineraryJsonServiceUserIdValidationTest.java`

**Purpose**: Tests userId validation in ItineraryJsonService

**Test Cases**:
1. `shouldRejectSaveMasterItineraryWithNullUserId` - Verifies null userId is rejected
2. `shouldRejectSaveMasterItineraryWithEmptyUserId` - Verifies empty userId is rejected
3. `shouldRejectSaveMasterItineraryWithWhitespaceUserId` - Verifies whitespace-only userId is rejected
4. `shouldRejectSaveRevisionWithNullUserId` - Verifies null userId rejected in saveRevision
5. `shouldRejectSaveRevisionWithEmptyUserId` - Verifies empty userId rejected in saveRevision
6. `shouldAcceptSaveMasterItineraryWithValidUserId` - Verifies valid userId is accepted
7. `shouldAcceptSaveRevisionWithValidUserId` - Verifies valid userId accepted in saveRevision
8. `shouldValidateUserIdBeforeOtherOperationsInSaveMasterItinerary` - Verifies validation order
9. `shouldValidateUserIdBeforeOtherOperationsInSaveRevision` - Verifies validation order

**Coverage**:
- ✅ Null userId validation
- ✅ Empty userId validation
- ✅ Whitespace userId validation
- ✅ Valid userId acceptance
- ✅ Validation order (before serialization)
- ✅ Both saveMasterItinerary and saveRevision methods

---

### 2. ChangeEngineEdgeUpdateTest.java
**Location**: `src/test/java/com/tripplanner/testing/service/ChangeEngineEdgeUpdateTest.java`

**Purpose**: Tests edge update validation in ChangeEngine

**Test Cases**:
1. `shouldRejectEdgeUpdateWithNullDayNumber` - Verifies null day number is rejected
2. `shouldAcceptEdgeUpdateWithValidDayNumber` - Verifies valid day number is accepted
3. `shouldRejectEdgeUpdateWhenDayDoesNotExist` - Verifies non-existent day is rejected
4. `shouldRejectEdgeUpdateWhenNodeDoesNotExist` - Verifies non-existent node is rejected
5. `shouldRejectEdgeUpdateForLockedNode` - Verifies locked nodes are protected
6. `shouldValidateEdgeUpdateOperationDetailsAreLogged` - Verifies detailed logging

**Coverage**:
- ✅ Null day number validation
- ✅ Valid day number acceptance
- ✅ Non-existent day handling
- ✅ Non-existent node handling
- ✅ Locked node protection
- ✅ Operation details logging

---

### 3. SseAuthenticationIntegrationTest.java
**Location**: `src/test/java/com/tripplanner/testing/integration/SseAuthenticationIntegrationTest.java`

**Purpose**: Integration tests for SSE authentication flow

**Test Cases**:
1. `shouldAllowSseAgentEventsEndpointWithoutAuthHeader` - Verifies SSE endpoints don't require Authorization header
2. `shouldAllowSsePatchesEndpointWithoutAuthHeader` - Verifies patches endpoint auth handling
3. `shouldAllowSseStreamEndpointWithoutAuthHeader` - Verifies stream endpoint auth handling
4. `shouldValidateTokenFromQueryParameterForSseEndpoints` - Verifies query parameter token validation
5. `shouldNotLogAuthWarningsForSseEndpoints` - Verifies no false warnings
6. `shouldRequireAuthorizationHeaderForNonSseEndpoints` - Verifies regular endpoints still require auth
7. `shouldHandleSseEndpointsBeforeMainAuthFilter` - Verifies filter order
8. `shouldHandleOptionsRequestsForSseEndpoints` - Verifies CORS preflight handling

**Coverage**:
- ✅ SSE endpoint auth bypass
- ✅ Query parameter token validation
- ✅ No false auth warnings
- ✅ Regular endpoint auth requirement
- ✅ Filter order verification
- ✅ CORS preflight handling

---

### 4. ItineraryCreationWithSseE2ETest.java
**Location**: `src/test/java/com/tripplanner/testing/e2e/ItineraryCreationWithSseE2ETest.java`

**Purpose**: End-to-end tests for complete itinerary creation with SSE

**Test Cases**:
1. `shouldCreateItineraryAndReturnSseConnectionDetails` - Verifies complete response structure
2. `shouldIncludeExecutionIdInCreationResponse` - Verifies executionId is returned
3. `shouldIncludeSseEndpointInCreationResponse` - Verifies sseEndpoint is returned
4. `shouldSetStatusToGeneratingInCreationResponse` - Verifies status is set correctly
5. `shouldCreateItineraryWithValidUserId` - Verifies userId is set on creation
6. `shouldAllowSseConnectionToCreatedItinerary` - Verifies SSE endpoints are accessible
7. `shouldMaintainUserIdThroughoutItineraryLifecycle` - Verifies userId consistency

**Coverage**:
- ✅ Complete creation response structure
- ✅ ExecutionId inclusion
- ✅ SSE endpoint inclusion
- ✅ Status field correctness
- ✅ UserId initialization
- ✅ SSE connection accessibility
- ✅ UserId consistency over time

---

## Running the Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests ItineraryJsonServiceUserIdValidationTest
./gradlew test --tests ChangeEngineEdgeUpdateTest
./gradlew test --tests SseAuthenticationIntegrationTest
./gradlew test --tests ItineraryCreationWithSseE2ETest
```

### Run Tests by Category
```bash
# Service layer tests
./gradlew test --tests "com.tripplanner.testing.service.*"

# Integration tests
./gradlew test --tests "com.tripplanner.testing.integration.*"

# E2E tests
./gradlew test --tests "com.tripplanner.testing.e2e.*"
```

---

## Test Coverage Summary

### Phase 1: SSE Authentication
- ✅ 8 integration tests
- ✅ Filter order verification
- ✅ Query parameter token handling
- ✅ No false auth warnings
- ✅ CORS preflight handling

### Phase 2: SSE Connection
- ✅ 7 E2E tests
- ✅ Response structure validation
- ✅ ExecutionId and SSE endpoint inclusion
- ✅ SSE connection accessibility

### Phase 3: UserId Persistence
- ✅ 9 service tests
- ✅ Null/empty/whitespace validation
- ✅ Validation order verification
- ✅ Both save methods covered
- ✅ 2 E2E tests for userId consistency

### Phase 4: Edge Updates
- ✅ 6 service tests
- ✅ Null day number validation
- ✅ Non-existent day/node handling
- ✅ Locked node protection
- ✅ Detailed error logging

**Total**: 30 comprehensive tests

---

## Expected Test Results

### All Tests Should Pass
After implementing the fixes, all tests should pass with:
- ✅ No userId validation errors
- ✅ No SSE authentication warnings
- ✅ No null day number errors
- ✅ Proper SSE connection establishment

### If Tests Fail

#### UserId Validation Tests Fail
**Possible Causes**:
- UserId validation not implemented in ItineraryJsonService
- Validation logic incorrect
- Exception type mismatch

**Fix**: Verify userId validation is in place in both `saveMasterItinerary()` and `saveRevision()`

#### Edge Update Tests Fail
**Possible Causes**:
- Null day validation not implemented
- Validation logic incorrect
- Method signature changed

**Fix**: Verify null day check is first in `updateEdge()` method

#### SSE Authentication Tests Fail
**Possible Causes**:
- Filter order incorrect
- SSE endpoints not properly identified
- Query parameter token not extracted

**Fix**: Verify `isSseEndpoint()` method includes all SSE paths

#### E2E Tests Fail
**Possible Causes**:
- Response structure changed
- SSE endpoints not accessible
- UserId not set on creation

**Fix**: Verify `ItineraryCreationResponse` includes all required fields

---

## Test Maintenance

### When to Update Tests

1. **API Changes**: Update E2E tests if API contracts change
2. **Validation Logic Changes**: Update service tests if validation rules change
3. **New SSE Endpoints**: Add to SSE authentication tests
4. **New Edge Update Logic**: Add to edge update tests

### Adding New Tests

When adding new functionality:
1. Add service-level tests for business logic
2. Add integration tests for component interaction
3. Add E2E tests for complete user flows
4. Update this documentation

---

## Test Data

### Test Itinerary Request
```java
CreateItineraryReq request = new CreateItineraryReq();
request.setDestination("Tokyo, Japan");
request.setStartLocation("Osaka, Japan");
request.setStartDate(LocalDate.now().plusDays(30).toString());
request.setEndDate(LocalDate.now().plusDays(33).toString());
request.setParty(new PartyDto(2, 0, 0, 1));
request.setBudgetTier("mid-range");
request.setInterests(List.of("culture", "food", "nature"));
```

### Test UserId Values
- Valid: `"valid-user-123"`, `"test-user-456"`
- Invalid: `null`, `""`, `"   "`

### Test Day Numbers
- Valid: `1`, `2`, `3`
- Invalid: `null`, `99` (non-existent)

---

## Continuous Integration

### Pre-commit Checks
```bash
# Run all tests before committing
./gradlew test

# Check test coverage
./gradlew jacocoTestReport
```

### CI Pipeline
1. Run all unit tests
2. Run integration tests
3. Run E2E tests
4. Generate coverage report
5. Fail build if coverage < 80%

---

## Troubleshooting

### Tests Timeout
**Cause**: Long-running operations or infinite loops
**Fix**: Add timeout annotations to tests
```java
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void testMethod() { ... }
```

### Mock Injection Fails
**Cause**: Reflection issues or field name mismatch
**Fix**: Verify field names match exactly in service classes

### SSE Tests Fail
**Cause**: EventSource connection issues
**Fix**: Use MockMvc for SSE endpoint testing, not actual EventSource

---

## Future Test Enhancements

### Phase 5: Monitoring Tests
- [ ] Add tests for SSE connection metrics
- [ ] Add tests for health check endpoint
- [ ] Add tests for connection state tracking

### Performance Tests
- [ ] Add load tests for SSE connections
- [ ] Add stress tests for concurrent itinerary creation
- [ ] Add performance benchmarks

### Security Tests
- [ ] Add tests for token expiration handling
- [ ] Add tests for unauthorized access attempts
- [ ] Add tests for CSRF protection

---

## Test Metrics

### Target Metrics
- Code Coverage: >80%
- Test Execution Time: <2 minutes
- Test Success Rate: 100%
- Flaky Test Rate: <1%

### Current Metrics
- Total Tests: 30
- Service Tests: 15
- Integration Tests: 8
- E2E Tests: 7

---

## References

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
