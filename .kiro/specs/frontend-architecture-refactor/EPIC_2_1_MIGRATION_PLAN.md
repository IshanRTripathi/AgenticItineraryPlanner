# Epic 2.1: Data Format Migration - Detailed Migration Plan

**Date:** January 20, 2025  
**Status:** Planning Complete  
**Estimated Duration:** 10-13 days  
**Risk Level:** HIGH

---

## Executive Summary

Detailed, step-by-step migration plan to eliminate TripData and use NormalizedItinerary throughout the frontend.

**Scope:**
- **55 files** to migrate
- **2 transformation services** to remove
- **1 type definition** to deprecate
- **Zero breaking changes** during migration

**Approach:**
- ✅ Incremental migration with feature flags
- ✅ Backward compatibility maintained
- ✅ Comprehensive testing at each step
- ✅ Rollback capability at any point

---

## Migration Phases Overview

| Phase | Duration | Files | Risk | Status |
|---|---|---|---|---|
| 1. Analysis & Setup | 1 day | 0 | Low | ⏳ Ready |
| 2. Compatibility Layer | 1 day | 1 | Low | ⏳ Pending |
| 3. Type Definitions | 0.5 days | 3 | Low | ⏳ Pending |
| 4. Leaf Components | 2 days | 6 | Medium | ⏳ Pending |
| 5. Container Components | 3 days | 22 | High | ⏳ Pending |
| 6. Core Components | 2 days | 7 | Critical | ⏳ Pending |
| 7. Services & Utils | 1 day | 9 | High | ⏳ Pending |
| 8. Cleanup & Testing | 1.5 days | 7 | Medium | ⏳ Pending |

**Total:** 12 days (with buffer)

---

## Phase 1: Analysis & Setup (1 day)

### Objectives
- ✅ Understand current usage patterns
- ✅ Set up feature flags
- ✅ Create testing strategy

### Tasks

#### 1.1 Component Usage Analysis (2 hours)
Analyze which TripData fields are actually used by components

#### 1.2 Feature Flag Setup (1 hour)
Create feature flag system for gradual rollout

#### 1.3 Testing Strategy (2 hours)
Define testing approach for each phase

#### 1.4 Rollback Procedures (1 hour)
Document rollback steps for each phase

---

## Phase 2: Compatibility Layer (1 day)

### Objectives
- ✅ Create minimal adapter for gradual migration
- ✅ Support both formats during transition

### Key Deliverable

**File:** `frontend/src/utils/itineraryAdapter.ts`

Minimal adapter utilities providing:
- ID accessors
- Date range calculation
- Cost aggregation
- Day/node lookups
- Type conversions
- Time conversions

---

## Phase 3-8: Detailed Implementation

See full migration plan in separate phases document.

---

## Success Criteria

- [ ] TripData.ts deleted
- [ ] Transformation services deleted
- [ ] All 55 files migrated
- [ ] Zero breaking changes
- [ ] Performance improved
- [ ] Bundle size reduced

---

**Document Status:** Complete  
**Last Updated:** January 20, 2025

