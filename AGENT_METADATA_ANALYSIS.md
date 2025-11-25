# Agent Metadata & Communication Analysis
**Date**: November 21, 2025
**Purpose**: Identify what metadata fields enable better inter-agent coordination

---

## 🔍 Current Agent Communication Patterns

### How Agents Currently Read Context

#### 1. **MealAgent** reads adjacent activities
```java
// Line 142-144
NormalizedNode previousActivity = findPreviousActivity(day, i);
NormalizedNode nextActivity = findNextActivity(day, i);
```
**Purpose**: Suggest restaurants near activities
**Problem**: Only reads node title and location, misses rich context

---

#### 2. **TransportAgent** reads adjacent nodes
```java
// Line 359
// Local transport: Use adjacent activity locations
```
**Purpose**: Determine transport start/end points
**Problem**: Doesn't know if activity requires special transport (e.g., mountain, island)

---

#### 3. **CostEstimatorAgent** reads priceLevel
```java
// Lines 90-95
if (node.getLocation() != null && node.getLocation().getPriceLevel() != null) {
    nodesWithPriceLevel.add(node);
}
```
**Purpose**: Estimate costs from Google Places data
**Problem**: Doesn't know user preferences or activity intensity

---

#### 4. **All Agents** read agentData map
```java
// Generic pattern
if (itinerary.getAgentData() != null && itinerary.getAgentData().containsKey("cityAllocation")) {
    // Read city allocation plan
}
```
**Purpose**: Read structured data from other agents
**Problem**: Untyped Map, no schema, hard to discover what's available

---

## 🎯 Missing Metadata Fields Analysis

### Category 1: Activity Context (for MealAgent, TransportAgent)

#### **ActivityMetadata** (already created, needs population)
```java
public class ActivityMetadata extends NodeMetadata {
    // EXISTING:
    private String category;
    private Integer estimatedDurationMinutes;
    private String difficultyLevel;
    private Boolean requiresBooking;
    private Boolean isIndoor;
    
    // NEEDED FOR INTER-AGENT COORDINATION:
    
    // 1. Location Context
    private String locationArea; // "Shibuya", "Shinjuku" - for meal suggestions
    private Boolean isRemoteLocation; // Mountain, island - affects transport
    private String accessibilityLevel; // "easy", "moderate", "difficult"
    
    // 2. Timing Context
    private String timeOfDay; // "morning", "afternoon", "evening"
    private Boolean isFlexibleTiming; // Can be moved if needed
    private Integer bufferTimeMinutes; // Time needed before/after
    
    // 3. Physical Context
    private String intensityLevel; // "relaxing", "moderate", "intense"
    private Boolean requiresSpecialEquipment; // Hiking boots, swimwear
    private String weatherDependency; // "indoor", "outdoor", "flexible"
    
    // 4. Meal Planning Context
    private Boolean includesMeal; // Museum cafe, theme park food
    private String nearbyDiningArea; // "Many restaurants nearby", "Limited options"
    
    // 5. Transport Planning Context
    private String transportAccessibility; // "metro accessible", "taxi required"
    private Integer walkingDistanceFromStation; // Minutes
    private Boolean requiresPrivateTransport; // Remote locations
}
```

**Value**: 
- MealAgent can suggest restaurants in same area
- TransportAgent knows if special transport needed
- CostEstimatorAgent adjusts for intensity/equipment

---

### Category 2: Meal Context (for TransportAgent, ActivityAgent)

#### **MealMetadata** (already created, needs population)
```java
public class MealMetadata extends NodeMetadata {
    // EXISTING:
    private String cuisineType;
    private String mealType; // breakfast, lunch, dinner
    private List<String> dietaryOptions;
    private String priceRange;
    private Boolean requiresReservation;
    
    // NEEDED FOR INTER-AGENT COORDINATION:
    
    // 1. Location Context
    private String restaurantArea; // For transport planning
    private Boolean isDestinationRestaurant; // Worth traveling for
    private Integer seatingDuration; // How long meal takes
    
    // 2. Activity Planning Context
    private String ambiance; // "quick", "casual", "fine-dining"
    private Boolean suitableBeforeActivity; // Light meal before hiking
    private Boolean suitableAfterActivity; // Hearty meal after museum
    
    // 3. Timing Context
    private String serviceSpeed; // "fast", "moderate", "slow"
    private Boolean acceptsWalkIns; // Or requires reservation
    private String peakHours; // When it's crowded
    
    // 4. Transport Context
    private String parkingAvailability; // If driving
    private String publicTransportAccess; // Metro station nearby
}
```

**Value**:
- TransportAgent knows if restaurant is destination or convenience
- ActivityAgent can plan around meal duration
- CostEstimatorAgent adjusts for ambiance level

---

### Category 3: Transport Context (for ActivityAgent, MealAgent)

#### **TransportMetadata** (already exists, well-designed)
```java
public class TransportMetadata extends NodeMetadata {
    // EXISTING (GOOD):
    private TransportType transportType;
    private String fromLocationName;
    private String toLocationName;
    private Coordinates fromCoordinates;
    private Coordinates toCoordinates;
    private Boolean fromIsIsland;
    private Boolean toIsIsland;
    private List<TransportMode> allowedModes;
    private TransportMode selectedMode;
    private Integer estimatedDurationMinutes;
    private Double estimatedDistanceKm;
    
    // COULD ADD FOR BETTER COORDINATION:
    
    // 1. Activity Context
    private Boolean carriesLuggage; // Inter-city travel
    private Boolean carriesEquipment; // Hiking gear, beach stuff
    
    // 2. Timing Context
    private String trafficLevel; // "light", "moderate", "heavy"
    private Boolean isRushHour; // Affects duration
    
    // 3. Cost Context
    private String costCategory; // "included", "budget", "premium"
}
```

**Value**:
- ActivityAgent knows if luggage affects activity choice
- MealAgent knows if there's time for sit-down meal
- CostEstimatorAgent has better transport cost data

---

### Category 4: Cross-Agent Communication (NEW)

#### **AgentNotes** (NEW - Inter-Agent Communication)
```java
public class AgentNotes {
    private String agentName; // Who wrote this
    private Long timestamp;
    private String noteType; // "suggestion", "warning", "constraint"
    private String message;
    private Map<String, Object> data; // Structured data
}

// Add to NormalizedNode:
@JsonProperty("agentNotes")
private List<AgentNotes> agentNotes;
```

**Use Cases**:

1. **TransportAgent → MealAgent**:
```java
// TransportAgent writes:
node.addAgentNote(new AgentNotes(
    "TransportAgent",
    "suggestion",
    "Long transport (2h), suggest quick meal before",
    Map.of("transportDuration", 120, "suggestedMealType", "quick")
));

// MealAgent reads:
for (AgentNotes note : previousTransport.getAgentNotes()) {
    if ("TransportAgent".equals(note.getAgentName())) {
        // Adjust meal suggestion based on transport duration
    }
}
```

2. **ActivityAgent → TransportAgent**:
```java
// ActivityAgent writes:
node.addAgentNote(new AgentNotes(
    "ActivityAgent",
    "constraint",
    "Activity requires hiking boots, allow time to change",
    Map.of("bufferTime", 15, "equipmentNeeded", "hiking boots")
));

// TransportAgent reads and adds buffer time
```

3. **MealAgent → CostEstimatorAgent**:
```java
// MealAgent writes:
node.addAgentNote(new AgentNotes(
    "MealAgent",
    "warning",
    "Fine dining restaurant, expect higher costs",
    Map.of("priceCategory", "premium", "estimatedCostRange", "50-100")
));

// CostEstimatorAgent adjusts estimates
```

**Value**: 
- Explicit communication channel between agents
- Discoverable (can list all notes)
- Typed (noteType helps filtering)
- Auditable (timestamp + agentName)

---

### Category 5: User Preferences (for all agents)

#### **UserPreferences** (NEW - Itinerary Level)
```java
// Add to NormalizedItinerary:
@JsonProperty("userPreferences")
private UserPreferences userPreferences;

public class UserPreferences {
    // Activity Preferences
    private String pacePreference; // "relaxed", "moderate", "packed"
    private List<String> activityInterests; // "museums", "nature", "food"
    private String physicalActivityLevel; // "low", "moderate", "high"
    
    // Meal Preferences
    private List<String> cuisinePreferences;
    private List<String> dietaryRestrictions;
    private String diningStyle; // "quick", "casual", "fine-dining"
    
    // Transport Preferences
    private List<String> preferredTransportModes;
    private Boolean willingToWalk; // Long distances
    private Boolean prefersDirectRoutes; // vs scenic routes
    
    // Budget Preferences
    private String budgetPriority; // "activities", "meals", "transport", "balanced"
    private Boolean flexibleOnCosts; // Can exceed budget for special experiences
    
    // Timing Preferences
    private String morningPersonality; // "early-bird", "late-riser"
    private Boolean prefersDowntime; // Needs breaks
    private Integer maxDailyActivities;
}
```

**Value**:
- All agents can tailor suggestions to user
- Consistent experience across agents
- Better personalization

---

## 📊 Priority Matrix

| Field Category | Impact | Effort | Priority | Agents Affected |
|----------------|--------|--------|----------|-----------------|
| AgentNotes | HIGH | LOW | 1 | All agents |
| ActivityMetadata.locationArea | HIGH | LOW | 2 | Meal, Transport |
| ActivityMetadata.intensityLevel | MEDIUM | LOW | 3 | Meal, Cost |
| MealMetadata.seatingDuration | MEDIUM | LOW | 4 | Transport, Activity |
| UserPreferences | HIGH | MEDIUM | 5 | All agents |
| ActivityMetadata.transportAccessibility | MEDIUM | MEDIUM | 6 | Transport |
| MealMetadata.ambiance | LOW | LOW | 7 | Cost |
| TransportMetadata.carriesLuggage | LOW | LOW | 8 | Activity |

---

## 🎯 Recommended Implementation Order

### Phase 1: Quick Wins (2-3 hours)

#### 1. Add AgentNotes System
```java
// Add to NormalizedNode
@JsonProperty("agentNotes")
private List<AgentNotes> agentNotes = new ArrayList<>();

public void addAgentNote(String agentName, String noteType, String message, Map<String, Object> data) {
    if (agentNotes == null) agentNotes = new ArrayList<>();
    agentNotes.add(new AgentNotes(agentName, noteType, message, data));
}

public List<AgentNotes> getNotesFromAgent(String agentName) {
    return agentNotes.stream()
        .filter(note -> agentName.equals(note.getAgentName()))
        .collect(Collectors.toList());
}
```

**Impact**: Immediate improvement in agent coordination

---

#### 2. Populate Key Activity Fields
```java
// In ActivityAgent, after LLM response:
ActivityMetadata metadata = new ActivityMetadata();
metadata.setLocationArea(extractArea(location)); // "Shibuya", "Shinjuku"
metadata.setIntensityLevel(inferIntensity(category, duration));
metadata.setTimeOfDay(inferTimeOfDay(timing));
node.setMetadata(metadata);

// Write note for other agents:
node.addAgentNote("ActivityAgent", "info", 
    "Activity in " + metadata.getLocationArea(),
    Map.of("area", metadata.getLocationArea(), "intensity", metadata.getIntensityLevel())
);
```

**Impact**: MealAgent can suggest nearby restaurants

---

### Phase 2: Enhanced Coordination (3-4 hours)

#### 3. Populate Meal Metadata
```java
// In MealAgent:
MealMetadata metadata = new MealMetadata();
metadata.setSeatingDuration(inferDuration(mealType, ambiance));
metadata.setAmbiance(inferAmbiance(priceLevel, description));
node.setMetadata(metadata);

// Write note for TransportAgent:
node.addAgentNote("MealAgent", "timing",
    "Meal duration: " + metadata.getSeatingDuration() + " minutes",
    Map.of("duration", metadata.getSeatingDuration())
);
```

---

#### 4. Agents Read Notes
```java
// In MealAgent, when planning meal:
NormalizedNode previousActivity = findPreviousActivity(day, i);
if (previousActivity != null) {
    List<AgentNotes> activityNotes = previousActivity.getNotesFromAgent("ActivityAgent");
    for (AgentNotes note : activityNotes) {
        if (note.getData().containsKey("area")) {
            String area = (String) note.getData().get("area");
            // Suggest restaurants in same area
        }
        if (note.getData().containsKey("intensity")) {
            String intensity = (String) note.getData().get("intensity");
            // Suggest hearty meal after intense activity
        }
    }
}
```

---

### Phase 3: User Preferences (2-3 hours)

#### 5. Add UserPreferences to Itinerary
```java
// Extract from user request:
UserPreferences prefs = new UserPreferences();
prefs.setPacePreference(inferPace(request.getConstraints()));
prefs.setActivityInterests(request.getThemes());
prefs.setBudgetPriority(inferPriority(request.getBudgetTier()));
itinerary.setUserPreferences(prefs);
```

---

#### 6. Agents Use Preferences
```java
// In ActivityAgent:
if (itinerary.getUserPreferences() != null) {
    String pace = itinerary.getUserPreferences().getPacePreference();
    if ("relaxed".equals(pace)) {
        // Add more buffer time between activities
        // Suggest fewer activities per day
    }
}
```

---

## 💡 Key Insights

### 1. **AgentNotes is the Game Changer**
- Simple to implement
- Immediate value
- Discoverable
- Auditable
- Extensible

### 2. **Metadata Enables Smart Decisions**
- ActivityMetadata.locationArea → Better meal suggestions
- MealMetadata.seatingDuration → Better transport timing
- TransportMetadata (already good) → Geography validation

### 3. **UserPreferences Enables Personalization**
- All agents can tailor to user
- Consistent experience
- Better satisfaction

### 4. **Current agentData Map is Limiting**
- Untyped
- Hard to discover
- No schema
- Should migrate to typed metadata + AgentNotes

---

## 🎯 Recommended Next Steps

1. **Implement AgentNotes** (1 hour)
   - Add to NormalizedNode
   - Add helper methods
   - Test with one agent pair

2. **Populate Activity Metadata** (1 hour)
   - locationArea
   - intensityLevel
   - timeOfDay

3. **MealAgent Reads Activity Notes** (1 hour)
   - Read locationArea
   - Suggest nearby restaurants
   - Test improvement

4. **Populate Meal Metadata** (1 hour)
   - seatingDuration
   - ambiance

5. **TransportAgent Reads Meal Notes** (1 hour)
   - Adjust timing for meal duration
   - Test improvement

**Total**: 5 hours for significant coordination improvement

---

## 📈 Expected Impact

### Before
- Agents work in isolation
- No context sharing
- Generic suggestions
- Timing conflicts possible

### After
- Agents coordinate through notes
- Rich context available
- Personalized suggestions
- Better timing alignment

**User Experience**: Itineraries feel more cohesive and thoughtful

---

## 🎉 Conclusion

**Most Valuable Addition**: **AgentNotes system**
- Low effort, high impact
- Enables all other improvements
- Extensible for future needs

**Quick Wins**:
1. AgentNotes (1 hour)
2. Activity.locationArea (30 min)
3. Meal.seatingDuration (30 min)

**Total for MVP**: 2 hours
**Impact**: Significantly better agent coordination
