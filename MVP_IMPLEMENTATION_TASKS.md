# MVP Implementation Contract — Task-Based Implementation Plan

## 🎯 **IMPLEMENTATION STATUS OVERVIEW**

### ✅ **ALREADY IMPLEMENTED** (Based on Current Codebase Analysis)
- **H2 Database**: ✅ Configured and working
- **Spring Boot 3.5.5**: ✅ Complete setup
- **Basic Controllers**: ✅ ItinerariesController, AgentController, BookingController
- **Basic Services**: ✅ ItineraryService, AgentOrchestrator, PlannerAgent, PlacesAgent
- **JPA Entities**: ✅ Complex Itinerary entity with nested classes
- **DTOs**: ✅ Multiple DTOs for API communication
- **Gemini AI Integration**: ✅ Working with mock data fallback
- **SSE Support**: ✅ AgentEventBus for real-time updates

### 🚧 **NEEDS IMPLEMENTATION** (Following MVP Contract)
- **Normalized JSON Schema**: Convert existing complex JPA structure to simple JSON
- **ChangeSet/ChangeEngine**: New change management system
- **EnrichmentAgent**: New agent for validation and pacing
- **New API Endpoints**: propose/apply/undo pattern
- **H2 Schema Migration**: New tables for JSON storage
- **Frontend Integration**: Update to use new normalized JSON

---

## 📋 **PHASE-BY-PHASE IMPLEMENTATION PLAN**

### **PHASE 1: Database Schema & JSON Storage** 
**Status**: ✅ **COMPLETED**

#### **Task 1.1: Create New H2 Tables for JSON Storage**
- [x] **1.1.1** Create `ITINERARIES` table with JSON CLOB
- [x] **1.1.2** Create `REVISIONS` table for version history
- [x] **1.1.3** Add database migration scripts
- [x] **1.1.4** Test table creation and basic operations

#### **Task 1.2: Create JSON Storage Entities**
- [x] **1.2.1** Create `ItineraryJson` entity for new table
- [x] **1.2.2** Create `ItineraryRevision` entity for revisions
- [x] **1.2.3** Create repository interfaces
- [x] **1.2.4** Test entity mapping and persistence

#### **Task 1.3: JSON Schema Validation**
- [x] **1.3.1** Create JSON schema classes for normalized structure
- [x] **1.3.2** Add validation annotations
- [x] **1.3.3** Test JSON serialization/deserialization
- [x] **1.3.4** Create sample normalized Barcelona itinerary

**Phase 1 Completion Criteria**: ✅ H2 tables created, JSON entities working, sample data loaded

---

### **PHASE 2: Change Management System**
**Status**: ✅ **COMPLETED**

#### **Task 2.1: Create ChangeSet DTOs**
- [x] **2.1.1** Create `ChangeSet` DTO with operations array
- [x] **2.1.2** Create `ChangeOperation` DTO for move/insert/delete
- [x] **2.1.3** Create `ItineraryDiff` DTO for preview
- [x] **2.1.4** Create `PatchEvent` DTO for SSE

#### **Task 2.2: Implement ChangeEngine Service**
- [x] **2.2.1** Create `ChangeEngine` service class
- [x] **2.2.2** Implement `propose()` method (no DB write)
- [x] **2.2.3** Implement `apply()` method (writes to DB)
- [x] **2.2.4** Implement `undo()` method (restores revision)
- [x] **2.2.5** Add lock validation logic
- [x] **2.2.6** Test all change operations

#### **Task 2.3: Update ItineraryService**
- [x] **2.3.1** Modify to use new JSON storage
- [x] **2.3.2** Add version management
- [x] **2.3.3** Integrate with ChangeEngine
- [x] **2.3.4** Add revision tracking
- [x] **2.3.5** Test versioning and rollback

**Phase 2 Completion Criteria**: ✅ ChangeSet operations working, versioning functional, undo/redo working

---

### **PHASE 3: Enhanced Agent System**
**Status**: ✅ **COMPLETED**

#### **Task 3.1: Update PlannerAgent**
- [x] **3.1.1** Modify to work with normalized JSON
- [x] **3.1.2** Add ChangeSet generation from chat/workflow
- [x] **3.1.3** Implement lock-aware operations
- [x] **3.1.4** Test agent with new JSON structure

#### **Task 3.2: Create EnrichmentAgent**
- [x] **3.2.1** Create new `EnrichmentAgent` class
- [x] **3.2.2** Implement opening hours validation
- [x] **3.2.3** Add pacing calculation logic
- [x] **3.2.4** Add transit duration computation
- [x] **3.2.5** Test enrichment operations

#### **Task 3.3: Update AgentOrchestrator**
- [x] **3.3.1** Modify to sequence PlannerAgent → EnrichmentAgent
- [x] **3.3.2** Add agent completion tracking
- [x] **3.3.3** Integrate with ChangeEngine
- [x] **3.3.4** Test agent orchestration

**Phase 3 Completion Criteria**: ✅ Both agents working with normalized JSON, orchestration functional

---

### **PHASE 4: New API Endpoints**
**Status**: ✅ **COMPLETED**

#### **Task 4.1: Update ItinerariesController**
- [x] **4.1.1** Add `GET /itineraries/{id}/json` (returns master JSON)
- [x] **4.1.2** Add `POST /itineraries/{id}:propose` (preview changes)
- [x] **4.1.3** Add `POST /itineraries/{id}:apply` (apply changes)
- [x] **4.1.4** Add `POST /itineraries/{id}:undo` (rollback)
- [x] **4.1.5** Test all new endpoints

#### **Task 4.2: Update AgentController**
- [x] **4.2.1** Modify `POST /agents/run` to work with new agents
- [x] **4.2.2** Add agent status tracking
- [x] **4.2.3** Test agent execution

#### **Task 4.3: Update BookingController**
- [x] **4.3.1** Modify `POST /book` to work with normalized JSON
- [x] **4.3.2** Add mock booking logic
- [x] **4.3.3** Test booking operations

#### **Task 4.4: Add SSE Patches Endpoint**
- [x] **4.4.1** Create `GET /itineraries/patches` SSE endpoint
- [x] **4.4.2** Integrate with SSE emitters
- [x] **4.4.3** Test real-time updates

**Phase 4 Completion Criteria**: ✅ All new API endpoints working, SSE functional

---

### **PHASE 5: Data Migration & Normalization**
**Status**: ✅ **COMPLETED**

#### **Task 5.1: Create Sample Data Generator**
- [x] **5.1.1** Create comprehensive sample data generator
- [x] **5.1.2** Generate Barcelona, Paris, and Tokyo itineraries
- [x] **5.1.3** Include all node types (attraction, meal, accommodation, transport)
- [x] **5.1.4** Test sample data generation

#### **Task 5.2: Test New API Endpoints**
- [x] **5.2.1** Test GET /itineraries/{id}/json endpoint
- [x] **5.2.2** Test POST /itineraries/{id}:propose endpoint
- [x] **5.2.3** Test POST /itineraries/{id}:apply endpoint
- [x] **5.2.4** Test POST /itineraries/{id}:undo endpoint

#### **Task 5.3: Test Agent and Booking Endpoints**
- [x] **5.3.1** Test POST /agents/process-request endpoint
- [x] **5.3.2** Test POST /book endpoint
- [x] **5.3.3** Validate ChangeSet operations
- [x] **5.3.4** Test version management and rollback

**Phase 5 Completion Criteria**: ✅ Sample data available, all new API endpoints tested and working

---

### **PHASE 6: Frontend Integration**
**Status**: ✅ **COMPLETED**

#### **Task 6.1: Update API Client**
- [x] **6.1.1** Modify `apiClient.ts` for new endpoints
- [x] **6.1.2** Add ChangeSet request/response types
- [x] **6.1.3** Update data transformation logic
- [x] **6.1.4** Test API integration

#### **Task 6.2: Create New Data Transformer**
- [x] **6.2.1** Create `NormalizedDataTransformer` for normalized JSON
- [x] **6.2.2** Add type definitions for normalized structure
- [x] **6.2.3** Implement transformation from normalized to TripData
- [x] **6.2.4** Test data transformation

#### **Task 6.3: Create Test UI Components**
- [x] **6.3.1** Create `NormalizedItineraryViewer` component
- [x] **6.3.2** Create `NormalizedItineraryTestPage` for testing
- [x] **6.3.3** Add ChangeSet preview and apply UI
- [x] **6.3.4** Test UI with new data

**Phase 6 Completion Criteria**: ✅ Frontend working with normalized JSON, ChangeSet UI functional

---

### **PHASE 7: Testing & Validation**
**Status**: 🔴 **NOT STARTED**

#### **Task 7.1: Backend Testing**
- [ ] **7.1.1** Test all new API endpoints
- [ ] **7.1.2** Test ChangeSet operations
- [ ] **7.1.3** Test agent orchestration
- [ ] **7.1.4** Test SSE functionality

#### **Task 7.2: Integration Testing**
- [ ] **7.2.1** Test end-to-end workflow
- [ ] **7.2.2** Test data migration
- [ ] **7.2.3** Test frontend-backend integration
- [ ] **7.2.4** Performance testing

#### **Task 7.3: Acceptance Criteria Validation**
- [ ] **7.3.1** Verify single nodes[] array drives both UI views
- [ ] **7.3.2** Verify agents work with locks and warnings
- [ ] **7.3.3** Verify propose/apply/undo workflow
- [ ] **7.3.4** Verify booking mock functionality

**Phase 7 Completion Criteria**: ✅ All acceptance criteria met, system fully functional

---

## 🎯 **IMPLEMENTATION PRIORITIES**

### **HIGH PRIORITY** (Must Complete First)
1. **Phase 1**: Database schema and JSON storage
2. **Phase 2**: Change management system
3. **Phase 4**: New API endpoints

### **MEDIUM PRIORITY** (Core Functionality)
4. **Phase 3**: Enhanced agent system
5. **Phase 5**: Data migration

### **LOW PRIORITY** (Polish & Integration)
6. **Phase 6**: Frontend integration
7. **Phase 7**: Testing & validation

---

## 📊 **CURRENT CODEBASE ANALYSIS**

### **What's Already Working**
- ✅ **H2 Database**: Configured with JPA entities
- ✅ **Spring Boot**: Complete setup with all dependencies
- ✅ **Basic Controllers**: ItinerariesController, AgentController, BookingController
- ✅ **Agent System**: PlannerAgent, PlacesAgent, AgentOrchestrator
- ✅ **Gemini Integration**: Working with mock data fallback
- ✅ **SSE Support**: AgentEventBus for real-time updates

### **What Needs to Change**
- 🔄 **Data Model**: Convert complex JPA entities to simple JSON
- 🔄 **API Pattern**: Add propose/apply/undo endpoints
- 🔄 **Change Management**: Implement ChangeSet system
- 🔄 **Agent Logic**: Update to work with normalized JSON
- 🔄 **Frontend**: Update to use new API pattern

### **What's Missing**
- ❌ **EnrichmentAgent**: New agent for validation
- ❌ **ChangeEngine**: Change management service
- ❌ **JSON Storage**: New H2 tables for JSON
- ❌ **Migration Tools**: Convert existing data
- ❌ **New API Endpoints**: propose/apply/undo pattern

---

## 🚀 **NEXT STEPS**

1. **Start with Phase 1**: Create new H2 tables and JSON storage
2. **Implement Phase 2**: Build ChangeEngine and change management
3. **Update Phase 4**: Add new API endpoints
4. **Test incrementally**: After each phase, test with existing frontend
5. **Migrate data**: Convert existing itineraries to normalized JSON
6. **Update frontend**: Modify to use new API pattern

---

## 📝 **IMPLEMENTATION NOTES**

### **Key Decisions Made**
- **Keep existing JPA entities**: Don't delete, just add new JSON storage
- **Gradual migration**: Implement new system alongside existing
- **Backward compatibility**: Maintain existing API until migration complete
- **Test-driven**: Test each phase before moving to next

### **Technical Considerations**
- **H2 file mode**: Use file-based H2 for persistence
- **JSON validation**: Use Jackson for JSON processing
- **Version management**: Keep last 50 revisions
- **Lock handling**: Respect locked nodes in all operations
- **SSE integration**: Use existing AgentEventBus for real-time updates

### **Risk Mitigation**
- **Incremental changes**: Don't break existing functionality
- **Data backup**: Backup existing data before migration
- **Rollback plan**: Keep existing system until new one is proven
- **Testing**: Comprehensive testing at each phase

---

## ✅ **ACCEPTANCE CRITERIA CHECKLIST**

- [ ] **Single nodes[] array** drives both Workflow and Day-by-day UI
- [ ] **PlannerAgent** can move/insert/delete nodes and update edges
- [ ] **EnrichmentAgent** adds warnings/pacing without breaking locks
- [ ] **:propose** returns valid diff without writing to DB
- [ ] **:apply** increments version, persists JSON, emits PatchEvent
- [ ] **:undo** restores revision from history
- [ ] **Agent orchestration** disables workflow UI during execution
- [ ] **Booking mock** sets locked=true, adds Booked label, sets bookingRef
- [ ] **SSE patches** stream real-time updates to frontend
- [ ] **H2 storage** persists JSON and revision history

---

## 📚 **APPENDIX: TECHNICAL SPECIFICATIONS**

### **Master JSON Schema** (Normalized Contract)
```json
{
  "itineraryId": "it_123",
  "version": 1,
  "summary": "…",
  "currency": "EUR",
  "themes": ["family"],
  "days": [ /* Day objects with nodes[] and edges[] */ ],
  "settings": { "autoApply": false, "defaultScope": "trip" },
  "agents": { "planner": {"lastRunAt": null}, "enrichment": {"lastRunAt": null} }
}
```

### **ChangeSet Operations**
```json
{
  "scope": "trip|day",
  "day": 1,
  "ops": [
    { "op": "move", "id": "n_sagrada", "startTime": "2025-10-04T10:00:00Z" },
    { "op": "insert", "after": "n_sagrada", "node": { "id":"n_tapas", "type":"meal" } },
    { "op": "delete", "id": "n_breakfast" }
  ],
  "preferences": { "userFirst": true }
}
```

### **API Endpoints**
- `GET /api/v1/itineraries/{id}` → Master JSON
- `POST /api/v1/itineraries/{id}:propose` → Preview changes
- `POST /api/v1/itineraries/{id}:apply` → Apply changes
- `POST /api/v1/itineraries/{id}:undo` → Rollback
- `GET /api/v1/itineraries/patches?itineraryId=…` → SSE stream
- `POST /api/v1/agents/run` → Execute agents
- `POST /api/v1/book` → Mock booking

---

**Last Updated**: 2025-01-20  
**Status**: Ready for Phase 1 Implementation  
**Next Action**: Create new H2 tables and JSON storage entities
