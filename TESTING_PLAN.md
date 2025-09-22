# Testing Plan - Phase 7: Testing & Validation

## Overview
This document outlines comprehensive testing scenarios for the Agentic Itinerary Planner MVP implementation, covering both backend and frontend functionality.

## Test Categories

### 1. Backend API Testing
#### 1.1 Normalized JSON Endpoints
- **GET /api/v1/itineraries/{id}/json**
  - ✅ Valid itinerary ID
  - ✅ Invalid itinerary ID (404)
  - ✅ Malformed itinerary ID
  - ✅ Database connection issues

#### 1.2 Change Management Endpoints
- **POST /api/v1/itineraries/{id}:propose**
  - ✅ Valid ChangeSet with move operation
  - ✅ Valid ChangeSet with insert operation
  - ✅ Valid ChangeSet with delete operation
  - ✅ Invalid ChangeSet (missing required fields)
  - ✅ ChangeSet with locked nodes
  - ✅ ChangeSet with non-existent node IDs
  - ✅ ChangeSet with invalid timing

- **POST /api/v1/itineraries/{id}:apply**
  - ✅ Apply valid proposed changes
  - ✅ Apply changes with conflicts
  - ✅ Apply changes to non-existent itinerary
  - ✅ Apply changes with invalid ChangeSet

- **POST /api/v1/itineraries/{id}:undo**
  - ✅ Undo to previous version
  - ✅ Undo to non-existent version
  - ✅ Undo with invalid version number
  - ✅ Undo with no previous versions

#### 1.3 Agent Endpoints
- **POST /api/v1/agents/process-request**
  - ✅ Valid user request
  - ✅ Empty user request
  - ✅ Request with special characters
  - ✅ Request with very long text
  - ✅ Request for non-existent itinerary

- **POST /api/v1/agents/apply-with-enrichment**
  - ✅ Apply with valid ChangeSet
  - ✅ Apply with enrichment validation
  - ✅ Apply with pacing adjustments
  - ✅ Apply with timing conflicts

#### 1.4 Booking Endpoints
- **POST /api/v1/book**
  - ✅ Book valid node
  - ✅ Book already booked node
  - ✅ Book non-existent node
  - ✅ Book locked node
  - ✅ Book with custom booking reference

#### 1.5 SSE Endpoints
- **GET /api/v1/itineraries/patches**
  - ✅ SSE connection establishment
  - ✅ SSE event streaming
  - ✅ SSE connection cleanup
  - ✅ Multiple concurrent connections

### 2. Data Validation Testing
#### 2.1 Normalized JSON Structure
- ✅ Valid NormalizedItinerary structure
- ✅ Valid NormalizedDay structure
- ✅ Valid NormalizedNode structure
- ✅ Valid Edge structure
- ✅ Invalid JSON structure handling
- ✅ Missing required fields
- ✅ Invalid data types

#### 2.2 ChangeSet Validation
- ✅ Valid ChangeSet structure
- ✅ Invalid operation types
- ✅ Missing required ChangeSet fields
- ✅ Invalid node references
- ✅ Invalid timing formats

### 3. Business Logic Testing
#### 3.1 ChangeEngine Logic
- ✅ Node movement within same day
- ✅ Node movement between days
- ✅ Node insertion at specific position
- ✅ Node deletion with edge updates
- ✅ Lock validation
- ✅ Timing conflict resolution
- ✅ Edge recalculation

#### 3.2 Agent System Logic
- ✅ PlannerAgent ChangeSet generation
- ✅ EnrichmentAgent validation
- ✅ Agent orchestration flow
- ✅ Error handling in agents
- ✅ Agent status tracking

#### 3.3 Data Transformation
- ✅ Normalized JSON to TripData conversion
- ✅ TripData to Normalized JSON conversion
- ✅ Edge case handling
- ✅ Performance with large datasets

### 4. Database Testing
#### 4.1 H2 Database Operations
- ✅ ItineraryJson CRUD operations
- ✅ ItineraryRevision CRUD operations
- ✅ Transaction handling
- ✅ Concurrent access
- ✅ Data persistence across restarts

#### 4.2 Data Integrity
- ✅ Foreign key constraints
- ✅ Unique constraints
- ✅ Data validation at DB level
- ✅ Rollback scenarios

### 5. Frontend Testing
#### 5.1 Component Testing
- ✅ NormalizedItineraryViewer component
- ✅ NormalizedItineraryTestPage component
- ✅ API client integration
- ✅ Data transformation
- ✅ Error handling

#### 5.2 User Interaction Testing
- ✅ Itinerary selection
- ✅ Day navigation
- ✅ ChangeSet operations
- ✅ Booking operations
- ✅ Real-time updates

#### 5.3 API Integration Testing
- ✅ Successful API calls
- ✅ Error handling
- ✅ Loading states
- ✅ Network failures
- ✅ Timeout handling

### 6. End-to-End Testing
#### 6.1 Complete Workflows
- ✅ Create itinerary → View → Modify → Book
- ✅ Propose changes → Apply → Undo
- ✅ Agent processing → Enrichment → Apply
- ✅ Multiple user sessions
- ✅ Concurrent modifications

#### 6.2 Performance Testing
- ✅ Large itinerary handling
- ✅ Multiple concurrent requests
- ✅ Memory usage
- ✅ Response times
- ✅ Database performance

### 7. Security Testing
#### 7.1 Input Validation
- ✅ SQL injection prevention
- ✅ XSS prevention
- ✅ Input sanitization
- ✅ File upload security

#### 7.2 Access Control
- ✅ Unauthorized access prevention
- ✅ Data isolation
- ✅ Session management

### 8. Error Handling Testing
#### 8.1 Backend Error Handling
- ✅ Database connection errors
- ✅ Invalid input errors
- ✅ Business logic errors
- ✅ External service errors

#### 8.2 Frontend Error Handling
- ✅ Network errors
- ✅ API errors
- ✅ User input errors
- ✅ Component errors

## Test Implementation Strategy

### Phase 7.1: Backend Unit Tests
- Create comprehensive unit tests for all services
- Test individual methods and edge cases
- Mock external dependencies

### Phase 7.2: Backend Integration Tests
- Test API endpoints with real database
- Test complete workflows
- Test error scenarios

### Phase 7.3: Frontend Unit Tests
- Test React components
- Test utility functions
- Test API client

### Phase 7.4: Frontend Integration Tests
- Test component interactions
- Test API integration
- Test user workflows

### Phase 7.5: End-to-End Tests
- Test complete user journeys
- Test cross-browser compatibility
- Test performance scenarios

## Acceptance Criteria Validation

### ✅ MVP Contract Requirements
1. **Normalized JSON Storage**: Single master JSON per itinerary
2. **Change Management**: Propose, apply, undo operations
3. **Agent System**: PlannerAgent and EnrichmentAgent
4. **API Endpoints**: All required endpoints implemented
5. **H2 Database**: File-based persistence
6. **Frontend Integration**: Working with new API

### ✅ Quality Metrics
- **Code Coverage**: >80% for critical paths
- **Performance**: <2s response time for API calls
- **Reliability**: <1% error rate
- **Usability**: All user workflows functional

## Test Data Requirements

### Sample Itineraries
- Barcelona (3 days) - Comprehensive
- Paris (3 days) - Comprehensive  
- Tokyo (3 days) - Comprehensive
- Edge cases (empty, single day, large)

### Test Users
- Standard user
- Admin user
- Concurrent users
- Error scenarios

## Reporting
- Test execution reports
- Coverage reports
- Performance metrics
- Bug tracking
- Acceptance criteria validation
