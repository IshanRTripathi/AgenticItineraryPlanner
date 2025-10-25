# Epic 2.1: Data Format Migration - Field Mapping & Transformation Analysis

**Date:** January 20, 2025  
**Status:** Analysis Complete  
**Purpose:** Complete field-by-field mapping between TripData and NormalizedItinerary formats

---

## Executive Summary

This document provides a comprehensive analysis of the transformation layers and field mappings between:
- **TripData** (legacy frontend format, 845 lines)
- **NormalizedItinerary** (backend format, current standard)

**Key Findings:**
- **2 transformation services** exist: `dataTransformer.ts` and `normalizedDataTransformer.ts`
- **NormalizedDataTransformer** is the primary transformer (currently used)
- **DataTransformer** appears to be legacy/unused
- **55 files** need migration from TripData to NormalizedItinerary
- **Significant data loss** occurs during transformation (many TripData fields are synthetic/computed)

---

## Transformation Layer Analysis

### 1. NormalizedDataTransformer.ts (PRIMARY - CURRENTLY USED)

**Purpose:** Transforms `NormalizedItinerary` (backend) → `TripData` (frontend)

**Key Method:**
```typescript
static transformNormalizedItineraryToTripData(normalized: NormalizedItinerary): TripData
```

**Transformation Flow:**
```
NormalizedItinerary (backend)
    ↓
transformNormalizedItineraryToTripData()
    ↓
TripData (frontend components)
```

**Characteristics:**
- ✅ Currently in use
- ✅ Has logging integration
- ✅ Handles NormalizedItinerary → TripData conversion
- ⚠️ Creates synthetic data for missing fields
- ⚠️ Loses backend-specific fields (version, agents, settings)

---

### 2. DataTransformer.ts (LEGACY - APPEARS UNUSED)

**Purpose:** Transforms generic backend response → `TripData`

**Key Method:**
```typescript
static transformItineraryResponseToTripData(response: any): TripData
```

**Characteristics:**
- ❌ Appears to be legacy code
- ❌ No logging integration
- ❌ Uses generic `any` type for input
- ❌ Creates mock data when backend provides no days
- 🔍 Needs verification if still used

---

## Complete Field Mapping

### Top-Level Fields

| TripData Field | NormalizedItinerary Field | Transformation | Notes |
|---|---|---|---|
| `id` | `itineraryId` | Direct mapping | ✅ 1:1 |
| `summary` | `summary` | Direct mapping | ✅ 1:1 |
| `startLocation` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Hardcoded defaults |
| `endLocation` | `destination` + `days[0].location` | Computed | ⚠️ Inferred from summary/first day |
| `isRoundTrip` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Always `true` |
| `dates.start` | `startDate` OR `days[0].date` | Computed | ✅ Extracted from days |
| `dates.end` | `endDate` OR `days[last].date` | Computed | ✅ Extracted from days |
| `travelers` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Default array created |
| `leadTraveler` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Default object created |
| `budget` | `days[].totals.cost` + `currency` | Computed | ⚠️ Aggregated from days |
| `preferences` | `themes` | Transformed | ⚠️ Mapped to 0-100 scores |
| `settings` | ❌ Not in backend | **SYNTHETIC** | ⚠️ All false defaults |
| `itinerary` | `days` | Transformed | ✅ Complex transformation |
| `status` | `days.length > 0` | Computed | ⚠️ 'completed' or 'generating' |
| `createdAt` | `createdAt` | Direct (with conversion) | ✅ Milliseconds → ISO string |
| `updatedAt` | `updatedAt` | Direct (with conversion) | ✅ Milliseconds → ISO string |
| `isPublic` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Always `false` |
| ❌ Not in TripData | `version` | **LOST** | ⚠️ Backend versioning lost |
| ❌ Not in TripData | `userId` | **LOST** | ⚠️ User ID lost |
| ❌ Not in TripData | `settings` | **LOST** | ⚠️ Backend settings lost |
| ❌ Not in TripData | `agents` | **LOST** | ⚠️ Agent status lost |

---

### Itinerary-Level Fields

| TripData.itinerary | NormalizedItinerary | Transformation | Notes |
|---|---|---|---|
| `id` | `itineraryId` | Direct mapping | ✅ 1:1 |
| `days` | `days` | Transformed | ✅ Array transformation |
| `totalCost` | `days[].totals.cost` | Aggregated | ✅ Sum of all days |
| `totalDistance` | `days[].totals.distanceKm` | Aggregated | ✅ Sum of all days |
| `totalDuration` | `days.length` | Computed | ✅ Count of days |
| `highlights` | `days[].nodes` (filtered) | Computed | ⚠️ Extracted from high-rated nodes |
| `themes` | `themes` | Direct mapping | ✅ 1:1 |
| `difficulty` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Always 'easy' |
| `packingList` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Empty array |
| `emergencyInfo` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Empty objects |
| `localInfo.currency` | `currency` | Direct mapping | ✅ 1:1 |
| `localInfo.*` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Defaults |
| `mapBounds` | `mapBounds` | Direct mapping | ✅ 1:1 |
| `countryCentroid` | `countryCentroid` | Direct mapping | ✅ 1:1 |

---

### Day-Level Fields

| TripData.DayPlan | NormalizedDay | Transformation | Notes |
|---|---|---|---|
| `id` | `id` OR `day-${dayNumber}` | Direct/Generated | ✅ Fallback to generated |
| `date` | `date` | Direct mapping | ✅ 1:1 |
| `dayNumber` | `dayNumber` | Direct mapping | ✅ 1:1 |
| `theme` | ❌ Not in backend | **COMPUTED** | ⚠️ Generated from node types |
| `location` | `location` | Direct mapping | ✅ 1:1 |
| `components` | `nodes` | Transformed | ✅ Array transformation |
| `totalDistance` | `totals.distanceKm` | Direct mapping | ✅ 1:1 |
| `totalCost` | `totals.cost` | Direct mapping | ✅ 1:1 |
| `totalDuration` | `totals.durationHr` | Direct mapping | ✅ 1:1 |
| `startTime` | `timeWindow.start` | Direct mapping | ✅ 1:1 (with fallback) |
| `endTime` | `timeWindow.end` | Direct mapping | ✅ 1:1 (with fallback) |
| `meals` | `nodes` (filtered by type) | Extracted | ⚠️ Filtered from nodes |
| `accommodation` | `nodes` (filtered by type) | Extracted | ⚠️ First accommodation node |
| `weather` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Hardcoded defaults |
| `notes` | `notes` | Direct mapping | ✅ 1:1 |
| ❌ Not in TripData | `edges` | **LOST** | ⚠️ Node connections lost |
| ❌ Not in TripData | `pacing` | **LOST** | ⚠️ Pacing info lost |
| ❌ Not in TripData | `warnings` | **LOST** | ⚠️ Day warnings lost |

---

### Node/Component-Level Fields

| TripData.TripComponent | NormalizedNode | Transformation | Notes |
|---|---|---|---|
| `id` | `id` | Direct mapping | ✅ 1:1 |
| `type` | `type` | Mapped | ✅ Type conversion |
| `name` | `title` | Direct mapping | ✅ 1:1 |
| `description` | ❌ Not in backend | **GENERATED** | ⚠️ Generated from type + title |
| `location.name` | `location.name` | Direct mapping | ✅ 1:1 |
| `location.address` | `location.address` | Direct mapping | ✅ 1:1 |
| `location.coordinates` | `location.coordinates` | Direct mapping | ✅ 1:1 (with null handling) |
| `timing.startTime` | `timing.startTime` | Converted | ✅ Milliseconds → HH:MM |
| `timing.endTime` | `timing.endTime` | Converted | ✅ Milliseconds → HH:MM |
| `timing.duration` | `timing.durationMin` | Direct mapping | ✅ 1:1 |
| `timing.suggestedDuration` | `timing.durationMin` | Direct mapping | ✅ Same as duration |
| `cost.pricePerPerson` | `cost.amount` | Direct mapping | ✅ 1:1 |
| `cost.currency` | `cost.currency` | Direct mapping | ✅ 1:1 |
| `cost.priceRange` | `cost.amount` | Computed | ⚠️ Mapped from amount |
| `cost.includesWhat` | ❌ Not in backend | **GENERATED** | ⚠️ Generated from type |
| `travel.*` | ❌ Not in backend | **SYNTHETIC** | ⚠️ All zeros/defaults |
| `details.rating` | `details.rating` | Direct mapping | ✅ 1:1 (with fallback) |
| `details.reviewCount` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Always 100 |
| `details.category` | `details.category` | Direct mapping | ✅ 1:1 |
| `details.tags` | `details.tags` | Direct mapping | ✅ 1:1 |
| `details.openingHours` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Empty object |
| `details.contact.website` | `links.website` | Direct mapping | ✅ 1:1 |
| `details.contact.phone` | `links.phone` | Direct mapping | ✅ 1:1 |
| `details.accessibility.*` | ❌ Not in backend | **SYNTHETIC** | ⚠️ All false |
| `details.amenities` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Empty array |
| `booking.required` | `labels` (contains 'Booking Required') | Computed | ⚠️ Derived from labels |
| `booking.bookingUrl` | `links.booking` | Direct mapping | ✅ 1:1 |
| `booking.notes` | `tips.warnings` | Joined | ⚠️ Warnings joined |
| `media.images` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Empty array |
| `tips.bestTimeToVisit` | `tips.bestTime` | Joined | ✅ Array → string |
| `tips.whatToBring` | ❌ Not in backend | **SYNTHETIC** | ⚠️ Empty array |
| `tips.insider` | `tips.travel` | Direct mapping | ✅ 1:1 |
| `tips.warnings` | `tips.warnings` | Direct mapping | ✅ 1:1 |
| `priority` | `labels` | Computed | ⚠️ Derived from labels |
| `locked` | `locked` | Direct mapping | ✅ 1:1 |
| ❌ Not in TripData | `bookingRef` | **LOST** | ⚠️ Booking reference lost |
| ❌ Not in TripData | `status` | **LOST** | ⚠️ Node status lost |
| ❌ Not in TripData | `updatedBy` | **LOST** | ⚠️ Update tracking lost |
| ❌ Not in TripData | `updatedAt` | **LOST** | ⚠️ Update timestamp lost |
| ❌ Not in TripData | `transit` | **LOST** | ⚠️ Transit info lost |

---

## Type Mappings

### Node Type Conversion

| NormalizedNode.type | TripComponent.type | Notes |
|---|---|---|
| `attraction` | `attraction` | ✅ Direct |
| `meal` | `restaurant` | ⚠️ Renamed |
| `accommodation` | `hotel` | ⚠️ Renamed |
| `hotel` | `hotel` | ✅ Direct |
| `transit` | `transport` | ⚠️ Renamed |
| `transport` | `transport` | ✅ Direct |
| (other) | `activity` | ⚠️ Default fallback |

---

## Data Loss Analysis

### Critical Data Lost in Transformation

1. **Backend Versioning**
   - `version` field (used for optimistic locking)
   - `updatedAt` timestamps on nodes
   - `updatedBy` tracking

2. **Agent Information**
   - `agents` status map
   - Agent execution history

3. **Settings & Configuration**
   - `settings.autoApply`
   - `settings.defaultScope`

4. **Day-Level Details**
   - `edges` (node connections/order)
   - `pacing` information
   - `warnings` array

5. **Node-Level Details**
   - `bookingRef` (booking references)
   - `status` (node lifecycle status)
   - `transit` information
   - `details.timeSlots`
   - `details.googleMapsUri`

---

## Synthetic Data Created

### Fields with Hardcoded Defaults

1. **Location Data**
   ```typescript
   startLocation: {
     id: 'start',
     name: 'Home',
     country: 'Unknown',
     city: 'Unknown',
     coordinates: { lat: 0, lng: 0 },
     timezone: 'UTC',
     currency: 'USD',
     exchangeRate: 1.0
   }
   ```

2. **Traveler Data**
   ```typescript
   travelers: [{
     id: 'lead',
     name: 'Lead Traveler',
     email: '',
     age: 30,
     preferences: {
       dietaryRestrictions: [],
       mobilityNeeds: [],
       interests: []
     }
   }]
   ```

3. **Settings**
   ```typescript
   settings: {
     womenFriendly: false,
     petFriendly: false,
     veganOnly: false,
     wheelchairAccessible: false,
     budgetFriendly: false,
     luxuryOnly: false,
     familyFriendly: false,
     soloTravelSafe: false
   }
   ```

4. **Weather Data**
   ```typescript
   weather: {
     temperature: { min: 15, max: 25 },
     condition: 'sunny',
     precipitation: 0
   }
   ```

---

## Migration Strategy Implications

### What Can Be Directly Migrated

✅ **Safe to migrate (1:1 mapping):**
- `id` / `itineraryId`
- `summary`
- `currency`
- `themes`
- `days` array structure
- `nodes` array structure
- Core timing and cost data
- Location coordinates
- `locked` status

### What Needs Adapter Logic

⚠️ **Requires compatibility layer:**
- Type conversions (meal → restaurant, etc.)
- Time format conversions (milliseconds → HH:MM)
- Computed fields (theme, description, priceRange)
- Aggregated data (totalCost, totalDistance)
- Extracted data (meals, accommodation from nodes)

### What Will Be Lost

❌ **Cannot be preserved:**
- Synthetic TripData fields (startLocation, travelers, settings)
- Hardcoded defaults (weather, reviewCount)
- Generated descriptions
- Empty arrays/objects (packingList, emergencyInfo)

---

## Recommendations

### 1. Direct NormalizedItinerary Usage

**Recommended Approach:** Use `NormalizedItinerary` directly in components

**Benefits:**
- ✅ No transformation overhead
- ✅ No data loss
- ✅ Access to backend-specific fields (version, agents, settings)
- ✅ Simpler data flow

**Challenges:**
- ⚠️ Components expect TripData structure
- ⚠️ Need to update 55 files
- ⚠️ Some computed fields need to be calculated in components

### 2. Compatibility Layer Design

**If gradual migration needed:**

```typescript
// Minimal adapter for backward compatibility
interface LegacyAdapter {
  // Only adapt fields that components actually use
  getTripId(itinerary: NormalizedItinerary): string;
  getDays(itinerary: NormalizedItinerary): NormalizedDay[];
  getNodes(day: NormalizedDay): NormalizedNode[];
  
  // Computed fields as needed
  getTotalCost(itinerary: NormalizedItinerary): number;
  getDateRange(itinerary: NormalizedItinerary): { start: string; end: string };
}
```

### 3. Component Update Priority

**Phase 1: Leaf components** (use NormalizedNode directly)
- DayCard
- NodeCard
- MapMarker

**Phase 2: Container components** (use NormalizedDay directly)
- DayByDayView
- WorkflowBuilder

**Phase 3: Root components** (use NormalizedItinerary directly)
- TravelPlanner
- UnifiedItineraryContext

---

## Next Steps

1. ✅ **Complete** - Field mapping documented
2. ⏳ **Next** - Design minimal compatibility layer
3. ⏳ **Next** - Identify which TripData fields are actually used by components
4. ⏳ **Next** - Create migration plan for each component category

---

**Document Status:** Complete  
**Last Updated:** January 20, 2025  
**Next Document:** EPIC_2_1_MIGRATION_PLAN.md

