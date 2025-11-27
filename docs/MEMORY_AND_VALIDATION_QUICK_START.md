# Memory & Validation System - Quick Start Guide

**Date:** 2025-11-27  
**For:** Developers implementing the system  
**See Also:** MEMORY_AND_VALIDATION_COMPLETE_ROADMAP.md

---

## 🎯 Quick Overview

**What:** Memory system that learns user preferences + Validation system that provides smart advice

**Where:** 
- Memory: `itineraries/{itineraryId}/memory/{memoryId}`
- Validation: Tools + ValidationAdvisorAgent

**Key Decisions:**
- ✅ No TTL - MemoryAgent manages lifecycle
- ✅ Async operations (CompletableFuture)
- ✅ Group trip compatible (validation works, memory creation skipped)
- ✅ Full stack (backend + frontend)
- ✅ Extract ItineraryValidator → deprecate

---

## 📋 Implementation Checklist

### Phase 1: Memory Backend (4h)
- [ ] Memory DTOs (Memory, UserProfile, MemoryQuery)
- [ ] MemoryService + MemoryConfidenceService
- [ ] MemoryAgent (extends BaseAgent)
- [ ] Memory API endpoints (MemoryController)
- [ ] Initial memory from trip creation

### Phase 2: Validation Tools (3h)
- [ ] Extract ItineraryValidator logic
- [ ] Tool 1: Check Conflicts (enhance existing)
- [ ] Tool 2: Check Completeness
- [ ] Tool 3: Check Budget Health
- [ ] Tool 4: Check Dietary Compliance
- [ ] Tool 5: Check Timing Feasibility
- [ ] Tool 6+: Geography, Accessibility

### Phase 3: ValidationAdvisor (2h)
- [ ] ValidationAdvisorAgent (extends BaseAgent)
- [ ] ValidationAdvice DTOs
- [ ] Parallel tool execution
- [ ] Memory-based filtering
- [ ] LLM advice generation
- [ ] Integration with Pipeline & EditorAgent

### Phase 4: Frontend (4h)
- [ ] Memory API client (memoryApi.ts)
- [ ] PreferencesPanel component
- [ ] ValidationAdvice component
- [ ] Integration with chat header & trip view

### Phase 5: Testing (2h)
- [ ] Backend unit tests (>80% coverage)
- [ ] Integration tests
- [ ] Frontend tests (>70% coverage)

### Phase 6: Config & Deploy (1h)
- [ ] Feature flags in application.yml
- [ ] Documentation updates
- [ ] Metrics tracking

---

## 🔑 Key Files to Create

### Backend
```
src/main/java/com/tripplanner/
├── dto/memory/
│   ├── Memory.java
│   ├── UserProfile.java
│   └── MemoryQuery.java
├── dto/validation/
│   ├── ValidationAdvice.java
│   ├── ValidationIssue.java
│   └── ValidationLevel.java
├── dto/tools/
│   ├── CompletenessCheckRequest.java
│   ├── CompletenessCheckResult.java
│   ├── BudgetHealthRequest.java
│   ├── BudgetHealthResult.java
│   ├── DietaryComplianceRequest.java
│   ├── DietaryComplianceResult.java
│   ├── TimingFeasibilityRequest.java
│   └── TimingFeasibilityResult.java
├── service/memory/
│   ├── MemoryService.java
│   ├── MemoryConfidenceService.java
│   └── MemoryPatternLearner.java
├── agents/
│   ├── MemoryAgent.java
│   └── ValidationAdvisorAgent.java
├── controller/
│   └── MemoryController.java
└── enums/
    ├── MemoryCategory.java
    └── MemoryType.java
```

### Frontend
```
frontend/src/
├── services/
│   └── memoryApi.ts
├── components/memory/
│   ├── PreferencesPanel.tsx
│   ├── PreferenceSection.tsx
│   ├── PreferenceItem.tsx
│   └── ConfidenceBar.tsx
└── components/validation/
    ├── ValidationAdvice.tsx
    └── ValidationIssue.tsx
```

---

## 🚀 Quick Start Commands

### 1. Create Memory DTOs
```bash
# Create directory
mkdir -p src/main/java/com/tripplanner/dto/memory

# Create files (use roadmap templates)
# Memory.java, UserProfile.java, MemoryQuery.java
```

### 2. Create Memory Service
```bash
mkdir -p src/main/java/com/tripplanner/service/memory
# MemoryService.java, MemoryConfidenceService.java
```

### 3. Create Memory Agent
```bash
# src/main/java/com/tripplanner/agents/MemoryAgent.java
# Extends BaseAgent, implements memory operations
```

### 4. Add to ItinerariesController
```java
// After line 105 in create() method
CompletableFuture.runAsync(() -> {
    createInitialMemories(initialItinerary.getId(), userId, request);
});
```

### 5. Create Validation Tools
```bash
# Enhance existing check-conflicts
# Create new tools in ToolsController
```

### 6. Create ValidationAdvisor Agent
```bash
# src/main/java/com/tripplanner/agents/ValidationAdvisorAgent.java
```

### 7. Frontend Components
```bash
cd frontend/src
mkdir -p components/memory components/validation services
# Create components using roadmap templates
```

---

## 📊 Testing Strategy

### Unit Tests
```java
@Test
public void testMemoryStore() {
    Memory memory = createTestMemory();
    CompletableFuture<Memory> result = memoryService.storeAsync(itineraryId, memory);
    assertNotNull(result.get());
}
```

### Integration Tests
```java
@Test
public void testEndToEndFlow() {
    // Create trip → verify memories → update → validate
}
```

### Frontend Tests
```typescript
describe('PreferencesPanel', () => {
  it('loads memories on open', async () => {
    // Test component
  });
});
```

---

## 🎛️ Feature Flags

```yaml
features:
  memory:
    enabled: true
    async-operations: true
  
  validation-tools:
    check-conflicts:
      enabled: true
    check-completeness:
      enabled: true
  
  validation-advisor:
    enabled: false  # Enable after testing
    skip-group-trips: true
```

---

## 🐛 Common Issues & Solutions

### Issue: Memory not saving
**Solution:** Check Firestore path, ensure async operation completes

### Issue: Validation not filtering
**Solution:** Verify memory retrieval, check dismissal logic

### Issue: Group trip errors
**Solution:** Ensure group trip check in ValidationAdvisor

### Issue: Frontend not loading
**Solution:** Check API client, verify CORS, check network tab

---

## 📚 Reference Patterns

### Existing Code to Reference
- **Tool Pattern:** `ToolsController.java` (check-conflicts, calculate-cost)
- **Agent Pattern:** `ActivityAgent.java`, `EditorAgent.java`
- **Service Pattern:** `ToolCacheService.java`, `ItineraryJsonService.java`
- **Async Pattern:** `PipelineOrchestrator.java` (CompletableFuture usage)
- **Frontend Pattern:** `CurrencySelector.tsx`, `DayCard.tsx`

### Documentation to Reference
- `TOOL_INTEGRATION_COMPLETE.md` - Tool patterns
- `TOOL_CREATION_RULEBOOK.md` - Agent patterns
- `MEMORY_AND_VALIDATION_IMPLEMENTATION_GUIDE.md` - Detailed specs
- `REFINED_VALIDATION_AND_MEMORY_ARCHITECTURE.md` - Architecture

---

## ✅ Success Metrics

### Memory System
- Initial memories created on trip creation ✓
- Confidence decay working ✓
- CRUD operations functional ✓
- Frontend preferences panel working ✓

### Validation System
- All tools implemented ✓
- ValidationAdvisor orchestrating ✓
- Memory-based filtering ✓
- Frontend advice display ✓

### Integration
- Pipeline integration ✓
- EditorAgent integration ✓
- Tests passing (>80%) ✓
- Documentation complete ✓

---

## 🎯 Next Steps

1. **Start with Phase 1** - Memory backend foundation
2. **Follow roadmap** - Use detailed checklist
3. **Test incrementally** - Don't wait until end
4. **Update docs** - As you implement
5. **Ask questions** - If anything unclear

---

**Ready to start?** → See `MEMORY_AND_VALIDATION_COMPLETE_ROADMAP.md` for detailed implementation steps

**Questions?** → Check existing code patterns and documentation

**Stuck?** → Review similar implementations (ToolsController, ActivityAgent)
