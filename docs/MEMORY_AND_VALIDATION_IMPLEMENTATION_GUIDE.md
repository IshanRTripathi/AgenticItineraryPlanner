# Memory & Validation System - Implementation Guide

**Date:** 2025-11-27  
**Status:** Final Specification  
**Based On:** User feedback and requirements

---

## Overview

This document provides the complete implementation specification for the Memory and Validation system with all user requirements incorporated.

---

## Part 1: Memory System

### Firestore Structure

```
users/{userId}/memories/{memoryId}
  - category: "PROFILE" | "DIETARY" | "VALIDATION" | "ACTIVITY" | etc.
  - type: "PREFERENCE" | "RESTRICTION" | "DISMISSAL" | "PATTERN" | etc.
  - data: { ... }
  - confidence: 0.0-1.0
  - createdAt: timestamp
  - lastUpdated: timestamp
  - lastUsed: timestamp
  - source: "USER_STATED" | "LEARNED" | "TRIP_CREATION"
  - isPersonal: boolean (for privacy filtering)
```

**Same pattern as tool cache and chat:**
- Collection: `users/{userId}/memories`
- Subcollection per user
- Automatic cleanup via TTL
- Indexed for fast queries

---

### Memory Categories & Privacy

#### Non-Personal (Shareable in Chat)
```json
{
  "category": "ACTIVITY",
  "type": "PREFERENCE",
  "data": {
    "preferredTypes": ["cultural", "historical"],
    "pace": "relaxed"
  },
  "isPersonal": false
}
```

#### Personal (Private, Skip in Chat)
```json
{
  "category": "DIETARY",
  "type": "RESTRICTION",
  "data": {
    "restrictions": ["vegetarian"],
    "allergies": ["peanuts"]
  },
  "isPersonal": true
}
```

**Privacy Rule:** When showing memory in chat, if `isPersonal: true`, show:
> "⚠️ Skipping personal health information. Only you can view this in your preferences."

---

### Initial Memory from Trip Creation

**Trip Creation Form Fields → Memory:**

```typescript
// When user creates trip
interface TripCreationForm {
  destination: string;
  startDate: Date;
  duration: number;
  budgetTier: "budget" | "mid_range" | "luxury";
  pace: "relaxed" | "moderate" | "packed";
  interests: string[];
  dietaryRestrictions: string[];
  partySize: number;
}

// Convert to initial memories
function createInitialMemories(userId: string, form: TripCreationForm) {
  const memories: Memory[] = [];
  
  // 1. Budget preference
  memories.push({
    userId,
    category: "BUDGET",
    type: "PREFERENCE",
    data: {
      budgetTier: form.budgetTier,
      typicalBudgetPerDay: getBudgetAmount(form.budgetTier)
    },
    confidence: 0.5, // Low confidence (first trip)
    source: "TRIP_CREATION",
    isPersonal: false
  });
  
  // 2. Travel pace
  memories.push({
    userId,
    category: "TIMING",
    type: "PREFERENCE",
    data: {
      pace: form.pace,
      activitiesPerDay: getPaceActivities(form.pace)
    },
    confidence: 0.5,
    source: "TRIP_CREATION",
    isPersonal: false
  });
  
  // 3. Activity interests
  memories.push({
    userId,
    category: "ACTIVITY",
    type: "PREFERENCE",
    data: {
      preferredTypes: form.interests
    },
    confidence: 0.5,
    source: "TRIP_CREATION",
    isPersonal: false
  });
  
  // 4. Dietary restrictions (PERSONAL)
  if (form.dietaryRestrictions.length > 0) {
    memories.push({
      userId,
      category: "DIETARY",
      type: "RESTRICTION",
      data: {
        restrictions: form.dietaryRestrictions,
        strictness: "strict"
      },
      confidence: 1.0, // High confidence (user stated)
      source: "TRIP_CREATION",
      isPersonal: true, // PRIVATE
      neverExpires: true
    });
  }
  
  return memories;
}
```

---

### Recency Bias & Confidence Decay

```java
@Service
public class MemoryConfidenceService {
    
    // Decay constant: 6 months half-life
    private static final long HALF_LIFE_MS = 180L * 24 * 60 * 60 * 1000; // 6 months
    
    /**
     * Calculate confidence with time decay
     */
    public double calculateConfidence(Memory memory) {
        double baseConfidence = memory.getConfidence();
        long age = System.currentTimeMillis() - memory.getLastUpdated();
        
        // Exponential decay: confidence = base * 0.5^(age/halfLife)
        double decayFactor = Math.pow(0.5, (double) age / HALF_LIFE_MS);
        double currentConfidence = baseConfidence * decayFactor;
        
        // Floor at 0.1 (never completely lose confidence)
        return Math.max(0.1, currentConfidence);
    }
    
    /**
     * Boost confidence when memory is used/confirmed
     */
    public void boostConfidence(Memory memory) {
        double current = memory.getConfidence();
        double boosted = Math.min(1.0, current + 0.1); // +10%, max 1.0
        
        memory.setConfidence(boosted);
        memory.setLastUsed(System.currentTimeMillis());
        memory.setLastUpdated(System.currentTimeMillis());
    }
    
    /**
     * Update confidence based on new behavior
     */
    public void updateFromBehavior(Memory memory, boolean behaviorMatches) {
        if (behaviorMatches) {
            // Behavior confirms memory
            boostConfidence(memory);
        } else {
            // Behavior contradicts memory
            double current = memory.getConfidence();
            double reduced = Math.max(0.1, current - 0.2); // -20%
            memory.setConfidence(reduced);
            memory.setLastUpdated(System.currentTimeMillis());
        }
    }
}
```

---

### Frontend: Preferences Panel in Chat

#### UI Component

```tsx
// PreferencesPanel.tsx
interface PreferencesPanel {
  userId: string;
  memories: Memory[];
  onUpdate: (memoryId: string, updates: Partial<Memory>) => void;
}

function PreferencesPanel({ userId, memories, onUpdate }: PreferencesPanel) {
  const [isOpen, setIsOpen] = useState(false);
  
  // Group memories by category
  const grouped = groupBy(memories, 'category');
  
  // Check for recent changes
  const hasRecentChanges = memories.some(m => 
    m.lastUpdated > Date.now() - 7 * 24 * 60 * 60 * 1000 // 7 days
  );
  
  return (
    <div className="preferences-container">
      {/* Icon Button in Chat Header */}
      <button 
        className="preferences-icon"
        onClick={() => setIsOpen(true)}
        title="Your Travel Preferences"
      >
        <UserPreferencesIcon />
        {hasRecentChanges && <span className="change-indicator">•</span>}
      </button>
      
      {/* Sliding Panel */}
      <SlidePanel isOpen={isOpen} onClose={() => setIsOpen(false)}>
        <div className="preferences-panel">
          <h2>Your Travel Preferences</h2>
          <p className="subtitle">
            We learn from your trips to personalize recommendations
          </p>
          
          {/* Activity Preferences */}
          <PreferenceSection
            title="Activity Preferences"
            icon={<ActivityIcon />}
            memories={grouped.ACTIVITY || []}
            onUpdate={onUpdate}
          />
          
          {/* Budget Behavior */}
          <PreferenceSection
            title="Budget & Spending"
            icon={<BudgetIcon />}
            memories={grouped.BUDGET || []}
            onUpdate={onUpdate}
          />
          
          {/* Travel Pace */}
          <PreferenceSection
            title="Travel Pace"
            icon={<PaceIcon />}
            memories={grouped.TIMING || []}
            onUpdate={onUpdate}
          />
          
          {/* Meal Preferences */}
          <PreferenceSection
            title="Dining Preferences"
            icon={<MealIcon />}
            memories={grouped.MEAL || []}
            onUpdate={onUpdate}
          />
          
          {/* Dietary (Private) */}
          <PreferenceSection
            title="Dietary Restrictions"
            icon={<DietaryIcon />}
            memories={grouped.DIETARY || []}
            onUpdate={onUpdate}
            isPrivate={true}
          />
          
          {/* Validation Dismissals */}
          <PreferenceSection
            title="Validation Preferences"
            icon={<ValidationIcon />}
            memories={grouped.VALIDATION || []}
            onUpdate={onUpdate}
          />
        </div>
      </SlidePanel>
    </div>
  );
}
```

#### Preference Section Component

```tsx
interface PreferenceSectionProps {
  title: string;
  icon: React.ReactNode;
  memories: Memory[];
  onUpdate: (memoryId: string, updates: Partial<Memory>) => void;
  isPrivate?: boolean;
}

function PreferenceSection({ 
  title, 
  icon, 
  memories, 
  onUpdate,
  isPrivate = false 
}: PreferenceSectionProps) {
  return (
    <div className="preference-section">
      <div className="section-header">
        {icon}
        <h3>{title}</h3>
        {isPrivate && <LockIcon className="private-icon" />}
      </div>
      
      {memories.map(memory => (
        <PreferenceItem
          key={memory.id}
          memory={memory}
          onUpdate={onUpdate}
        />
      ))}
    </div>
  );
}
```

#### Preference Item Component

```tsx
interface PreferenceItemProps {
  memory: Memory;
  onUpdate: (memoryId: string, updates: Partial<Memory>) => void;
}

function PreferenceItem({ memory, onUpdate }: PreferenceItemProps) {
  const [isEditing, setIsEditing] = useState(false);
  
  // Calculate current confidence with decay
  const currentConfidence = calculateConfidenceWithDecay(memory);
  const hasRecentChange = memory.lastUpdated > Date.now() - 7 * 24 * 60 * 60 * 1000;
  
  return (
    <div className="preference-item">
      <div className="preference-content">
        {/* Preference Label */}
        <div className="preference-label">
          {formatPreferenceLabel(memory)}
          {hasRecentChange && (
            <span className="change-badge">Recently updated</span>
          )}
        </div>
        
        {/* Preference Value (Toggle or Text) */}
        {isEditing ? (
          <PreferenceEditor
            memory={memory}
            onSave={(updates) => {
              onUpdate(memory.id, updates);
              setIsEditing(false);
            }}
            onCancel={() => setIsEditing(false)}
          />
        ) : (
          <div className="preference-value">
            {formatPreferenceValue(memory)}
          </div>
        )}
        
        {/* Confidence Indicator */}
        <div className="confidence-indicator">
          <ConfidenceBar value={currentConfidence} />
          <span className="confidence-text">
            {Math.round(currentConfidence * 100)}% confidence
          </span>
        </div>
        
        {/* Source & Last Updated */}
        <div className="preference-meta">
          <span className="source">
            {memory.source === 'LEARNED' ? 'Learned from trips' : 'You told us'}
          </span>
          <span className="updated">
            Updated {formatRelativeTime(memory.lastUpdated)}
          </span>
        </div>
      </div>
      
      {/* Actions */}
      <div className="preference-actions">
        <button onClick={() => setIsEditing(true)}>
          <EditIcon /> Edit
        </button>
        <button onClick={() => onUpdate(memory.id, { deleted: true })}>
          <DeleteIcon /> Remove
        </button>
      </div>
    </div>
  );
}
```

#### Confidence Bar Component

```tsx
function ConfidenceBar({ value }: { value: number }) {
  const percentage = Math.round(value * 100);
  const color = value > 0.7 ? 'green' : value > 0.4 ? 'yellow' : 'red';
  
  return (
    <div className="confidence-bar">
      <div 
        className={`confidence-fill confidence-${color}`}
        style={{ width: `${percentage}%` }}
      />
    </div>
  );
}
```

---

### Backend: Memory API

#### Endpoints

```java
@RestController
@RequestMapping("/api/v1/users/{userId}/memories")
public class MemoryController {
    
    private final MemoryService memoryService;
    
    /**
     * Get all memories for user
     */
    @GetMapping
    public ResponseEntity<List<Memory>> getMemories(
            @PathVariable String userId,
            @RequestParam(required = false) String category) {
        
        List<Memory> memories = category != null
            ? memoryService.getByCategory(userId, category)
            : memoryService.getAll(userId);
        
        // Apply confidence decay
        memories.forEach(m -> 
            m.setCurrentConfidence(
                confidenceService.calculateConfidence(m)));
        
        return ResponseEntity.ok(memories);
    }
    
    /**
     * Update memory
     */
    @PatchMapping("/{memoryId}")
    public ResponseEntity<Memory> updateMemory(
            @PathVariable String userId,
            @PathVariable String memoryId,
            @RequestBody MemoryUpdate update) {
        
        Memory updated = memoryService.update(userId, memoryId, update);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete memory
     */
    @DeleteMapping("/{memoryId}")
    public ResponseEntity<Void> deleteMemory(
            @PathVariable String userId,
            @PathVariable String memoryId) {
        
        memoryService.delete(userId, memoryId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get user profile (aggregated)
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(
            @PathVariable String userId) {
        
        UserProfile profile = memoryService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }
}
```

---

### Memory Service

```java
@Service
public class MemoryService {
    
    private final FirestoreService firestore;
    private final MemoryConfidenceService confidenceService;
    
    /**
     * Store memory
     */
    public Memory store(String userId, Memory memory) {
        // Set metadata
        memory.setUserId(userId);
        memory.setCreatedAt(System.currentTimeMillis());
        memory.setLastUpdated(System.currentTimeMillis());
        
        // Mark personal data
        memory.setIsPersonal(isPersonalCategory(memory.getCategory()));
        
        // Save to Firestore
        String path = String.format("users/%s/memories", userId);
        firestore.save(path, memory);
        
        logger.info("Stored {} memory for user {}", 
            memory.getCategory(), userId);
        
        return memory;
    }
    
    /**
     * Get all memories for user
     */
    public List<Memory> getAll(String userId) {
        String path = String.format("users/%s/memories", userId);
        return firestore.getAll(path, Memory.class);
    }
    
    /**
     * Get memories by category
     */
    public List<Memory> getByCategory(String userId, String category) {
        String path = String.format("users/%s/memories", userId);
        return firestore.query(path)
            .whereEqualTo("category", category)
            .get(Memory.class);
    }
    
    /**
     * Update memory
     */
    public Memory update(String userId, String memoryId, MemoryUpdate update) {
        String path = String.format("users/%s/memories/%s", userId, memoryId);
        Memory memory = firestore.get(path, Memory.class);
        
        // Apply updates
        if (update.getData() != null) {
            memory.setData(update.getData());
        }
        if (update.getConfidence() != null) {
            memory.setConfidence(update.getConfidence());
        }
        
        memory.setLastUpdated(System.currentTimeMillis());
        
        // Save
        firestore.update(path, memory);
        
        return memory;
    }
    
    /**
     * Delete memory
     */
    public void delete(String userId, String memoryId) {
        String path = String.format("users/%s/memories/%s", userId, memoryId);
        firestore.delete(path);
    }
    
    /**
     * Check if category contains personal data
     */
    private boolean isPersonalCategory(String category) {
        return category.equals("DIETARY") || 
               category.equals("HEALTH") ||
               category.equals("MEDICAL");
    }
    
    /**
     * Get user profile (aggregated memories)
     */
    public UserProfile getProfile(String userId) {
        List<Memory> all = getAll(userId);
        
        return UserProfile.builder()
            .userId(userId)
            .activityPreferences(extractActivityPrefs(all))
            .budgetBehavior(extractBudgetBehavior(all))
            .timingPatterns(extractTimingPatterns(all))
            .mealPreferences(extractMealPrefs(all))
            .dietaryRestrictions(extractDietary(all))
            .validationPreferences(extractValidationPrefs(all))
            .build();
    }
}
```

---

### Chat Integration: Privacy Filter

```java
@Component
public class ChatMemoryFormatter {
    
    /**
     * Format memories for chat display (with privacy filtering)
     */
    public String formatMemoriesForChat(List<Memory> memories) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Your Travel Preferences:\n\n");
        
        int shownCount = 0;
        int skippedCount = 0;
        
        for (Memory memory : memories) {
            if (memory.isPersonal()) {
                skippedCount++;
                continue;
            }
            
            sb.append(formatMemory(memory));
            sb.append("\n");
            shownCount++;
        }
        
        if (skippedCount > 0) {
            sb.append("\n⚠️ Skipped ").append(skippedCount)
              .append(" personal items (health/dietary info). ")
              .append("Only you can view these in your preferences.\n");
        }
        
        return sb.toString();
    }
    
    private String formatMemory(Memory memory) {
        String icon = getCategoryIcon(memory.getCategory());
        String label = formatLabel(memory);
        String value = formatValue(memory);
        double confidence = memory.getCurrentConfidence();
        
        return String.format("%s %s: %s (%.0f%% confidence)",
            icon, label, value, confidence * 100);
    }
}
```

---

## Part 2: Validation System Updates

### Skip Group Trips (For Now)

```java
@Service
public class ValidationAdvisorAgent {
    
    public ValidationAdvice validateAndAdvise(String itineraryId) {
        NormalizedItinerary itinerary = getItinerary(itineraryId);
        
        // Skip if group trip (for now)
        if (itinerary.getPartySize() > 1) {
            logger.info("Skipping validation for group trip (not yet supported)");
            return ValidationAdvice.skipped(
                "Validation for group trips coming soon!");
        }
        
        // Single user - proceed with validation
        String userId = itinerary.getUserId();
        // ... rest of validation
    }
}
```

---

## Implementation Checklist

### Phase 1: Memory Foundation (Week 1)

**Backend:**
- [ ] Create Memory data model
- [ ] Create MemoryService with Firestore integration
- [ ] Create MemoryConfidenceService (decay logic)
- [ ] Create Memory API endpoints
- [ ] Add initial memory creation from trip form
- [ ] Add privacy filtering (isPersonal flag)

**Frontend:**
- [ ] Create PreferencesPanel component
- [ ] Add preferences icon to chat header
- [ ] Create PreferenceSection components
- [ ] Create PreferenceItem with confidence bar
- [ ] Add edit/delete functionality
- [ ] Add change indicators

### Phase 2: Integration (Week 2)

**Backend:**
- [ ] Integrate MemoryAgent with ValidationAdvisor
- [ ] Integrate MemoryAgent with planning agents
- [ ] Add memory updates after itinerary completion
- [ ] Add confidence boosting on memory use
- [ ] Add cleanup job for expired memories

**Frontend:**
- [ ] Show memory in chat (with privacy filter)
- [ ] Add memory updates from validation dismissals
- [ ] Add memory updates from user edits
- [ ] Add "Recently updated" badges
- [ ] Add memory export (GDPR)

### Phase 3: Learning (Week 3)

**Backend:**
- [ ] Implement pattern learning
- [ ] Add behavior-based confidence updates
- [ ] Add memory consolidation
- [ ] Add conflict detection
- [ ] Add memory analytics

**Frontend:**
- [ ] Add pattern visualization
- [ ] Add confidence trends
- [ ] Add memory insights
- [ ] Add onboarding flow
- [ ] Add memory tour/help

---

## Summary

✅ **Memory stored in Firestore** - Same pattern as tool cache/chat  
✅ **Initial memory from trip creation** - Budget, pace, interests, dietary  
✅ **Recency bias** - Confidence decays over 6 months  
✅ **User control** - Edit/delete via preferences panel in chat  
✅ **Privacy** - Personal data flagged, skipped in chat  
✅ **Change indicators** - Show recent updates  
✅ **Skip group trips** - Not supported yet  

This gives you a complete, privacy-aware memory system that learns and adapts! 🎯

