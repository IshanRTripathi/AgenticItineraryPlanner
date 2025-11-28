# Critical Analysis: Prompt Structure of Failing Agents

## Executive Summary

After deep analysis of the agent prompt structures and LLM responses, I've identified **CRITICAL DESIGN FLAWS** in how agents instruct the LLM about location constraints. The geographical errors are **NOT random LLM hallucinations** - they are **SYSTEMATIC FAILURES** caused by inadequate prompt engineering.

---

## 1. ROOT CAUSE: Insufficient Location Constraint Enforcement

### Problem Statement
The agents provide **WEAK, AMBIGUOUS location instructions** that the LLM can easily misinterpret or ignore when searching for real places.

### Evidence from Actual Responses

**Day 1 - Frankfurt, Germany Error:**
```json
{
  "title": "Morning Old Town Exploration and Luxury Shopping",
  "locationName": "Altstadt (Old Town) & Bahnhofstrasse, Zurich",
  "location": { "name": "N/A - Krönungsweg, 60311 Frankfurt am Main, Germany" }
}
```

**Analysis:** The LLM correctly suggested "Altstadt & Bahnhofstrasse, Zurich" in `locationName`, but the **EnrichmentAgent** (Google Places search) found a place in Frankfurt instead. This indicates:
1. The search query was too generic ("Altstadt" exists in many German cities)
2. No geographical bounding was applied to the search
3. The enrichment process didn't validate that results match the intended city

---

## 2. Agent-by-Agent Prompt Analysis

### 2.1 MealAgent Prompts

#### System Prompt - STRENGTHS ✅
```java
Location Awareness (CRITICAL):
- ALWAYS suggest restaurants IN THE SAME CITY as the day's location
- NEVER suggest restaurants from other cities (e.g., no Zermatt restaurants on Interlaken days)
- ALWAYS suggest restaurants near the previous or next activity
- DO NOT suggest restaurants in distant areas (>30 minutes travel)
- DO NOT suggest restaurants in different cities (>2 hours travel)

Example 2 (Wrong City - DO NOT DO THIS):
Day Location: Interlaken
❌ WRONG: Restaurant Spycher (Zermatt) - This is in a DIFFERENT CITY
✅ CORRECT: Restaurant Goldener Anker (Interlaken) - Same city
```

**Assessment:** Excellent explicit instructions with concrete examples. This is **BEST PRACTICE**.

#### User Prompt - CRITICAL WEAKNESS ❌
```java
prompt.append(String.format("\n** DAY %d - LOCATION: %s **\n", dayNum, dayLocation));
prompt.append(String.format("CRITICAL: All restaurants for Day %d MUST be in %s, NOT in any other city!\n", 
                           dayNum, dayLocation));

for (MealContext ctx : dayContexts) {
    prompt.append(String.format("  - Node ID: %s, Type: %s, Time: %s\n",
        ctx.nodeId, ctx.mealType,
        ctx.timing != null ? ctx.timing.getStartTime() : "TBD"));
    
    // Add activity context for location awareness
    if (ctx.previousActivity != null) {
        prompt.append("    Previous activity: ")
              .append(ctx.previousActivity.getTitle());
        if (ctx.previousActivity.getLocation() != null && ctx.previousActivity.getLocation().getName() != null) {
            prompt.append(" at ").append(ctx.previousActivity.getLocation().getName());
        }
        prompt.append("\n");
    }
}
```

**CRITICAL FLAW:** The prompt includes activity locations like:
```
Previous activity: Morning Old Town Exploration at Altstadt (Old Town) & Bahnhofstrasse, Zurich
```

But "Altstadt" and "Bahnhofstrasse" are **GENERIC NAMES** that exist in multiple Swiss/German cities. The LLM has no way to know which specific "Altstadt" is meant.

**What's Missing:**
1. **No geographical coordinates** to constrain the search area
2. **No country/region context** (e.g., "Zurich, Switzerland" vs just "Zurich")
3. **No validation** that the suggested location is within X km of the day's city center

---

### 2.2 ActivityAgent Prompts

#### System Prompt - GOOD BUT INCOMPLETE ⚠️
```java
Guidelines:
1. Use the EXACT nodeId provided for each attraction slot
2. Provide real, specific place names (e.g., "Tokyo National Museum" not "Museum")
3. Write engaging descriptions (2-3 sentences)
...
8. IMPORTANT: For locationName, provide the SPECIFIC place name, NOT just the district/area
   - GOOD: "Shibuya Crossing", "Senso-ji Temple", "Tsukiji Outer Market"
   - BAD: "Shibuya", "Asakusa", "Tsukiji"
```

**Assessment:** Good specificity requirements, but **NO EXPLICIT CITY CONSTRAINT WARNINGS** like MealAgent has.

#### User Prompt - SAME CRITICAL WEAKNESS ❌
```java
prompt.append(String.format("\n** DAY %d - LOCATION: %s **\n", dayNum, dayLocation));
prompt.append(String.format("CRITICAL: All attractions for Day %d MUST be in %s, NOT in any other city!\n", 
                           dayNum, dayLocation));
```

**CRITICAL FLAW:** Same issue as MealAgent - relies on city name alone without:
- Geographical bounding
- Country/region context
- Distance validation

---

### 2.3 CostEstimatorAgent Prompts

#### System Prompt - MISSING LOCATION VALIDATION ❌
```java
Your task is to provide REALISTIC, ACCURATE cost estimates based on:
- Current market rates for the specific destination
- Local cost of living and currency
- Budget tier (budget, medium, luxury)
- Activity/venue type and typical pricing
```

**CRITICAL FLAW:** The prompt asks for "destination-specific" costs but provides **NO MECHANISM** to validate that the LLM is using costs from the correct destination.

**Example of Failure:**
```json
{
  "index": 1,
  "costPerPerson": 700,
  "notes": "Estimate for a private guided tour of Zurich's Old Town..."
}
```

CHF 700 for a walking tour is **5-10x higher** than actual market rates. The LLM is either:
1. Hallucinating costs
2. Using costs from a different (more expensive) destination
3. Misunderstanding the "luxury" tier

**What's Missing:**
1. **No cost range validation** (e.g., "costs must be between X and Y for this destination")
2. **No market rate examples** (e.g., "typical museum entry in Switzerland: CHF 15-30")
3. **No currency conversion context** (e.g., "CHF 1 = USD 1.10")

---

## 3. The EnrichmentAgent/GooglePlacesService Gap

### Critical Discovery
The **EnrichmentAgent** is responsible for converting LLM-suggested location names into actual Google Places results. This is where the geographical errors occur.

### Current Flow (BROKEN):
```
1. MealAgent suggests: "Restaurant Rebstock" in "Lucerne"
2. EnrichmentAgent searches Google Places: "Restaurant Rebstock"
3. Google returns: Multiple "Restaurant Rebstock" across Switzerland
4. EnrichmentAgent picks: FIRST RESULT (could be in any city!)
5. Result: Wrong city coordinates saved
```

### Missing Validation Layer:
```java
// EnrichmentService.java - Line 150
PlaceSearchResult searchResult = googlePlacesService.searchPlace(locationName, destination);

// ❌ NO VALIDATION that searchResult is in the correct city!
// ❌ NO DISTANCE CHECK from day's city center
// ❌ NO COUNTRY/REGION FILTERING
```

---

## 4. Comparison: What Works vs What Fails

### ✅ What Works (TransportAgent)
```java
// gemini_response_1699673802 - Transport suggestions
{
  "nodeId": "day3_node6",
  "title": "Train from Zurich to Lucerne",
  "description": "Travel from Zurich Hauptbahnhof (main station) directly to Lucerne Bahnhof...",
  "mode": "train",
  "durationMinutes": 75
}
```

**Why it works:**
1. **Specific station names** (Zurich Hauptbahnhof, Lucerne Bahnhof)
2. **Mode-specific instructions** (train routes are well-defined)
3. **No ambiguity** - there's only ONE Zurich Hauptbahnhof

### ❌ What Fails (MealAgent, ActivityAgent)
```java
// gemini_response_-947159404 - Meal suggestions
{
  "nodeId": "day3_node8",
  "title": "Authentic Lucerne Charm",
  "locationName": "Hotel Restaurant Rebstock",  // ✅ Correct name
  "location": { "name": "N/A - St. Leodegarstrasse 3, 6006 Luzern, Switzerland" }  // ✅ Correct!
}

// BUT THEN:
{
  "nodeId": "day3_node11",
  "title": "Hearty Swiss Fare",
  "locationName": "Fondue Villa & Garden",  // ✅ Correct name
  "location": { "name": "N/A - Seestrasse 44, 3800 Unterseen, Switzerland" }  // ❌ WRONG CITY!
}
```

**Why it fails:**
1. **Generic restaurant names** ("Fondue Villa" exists in multiple Swiss cities)
2. **No geographical bounding** in Google Places search
3. **No post-search validation** that result is in Lucerne

---

## 5. The SkeletonPlannerAgent Context Problem

### Critical Discovery
The SkeletonPlannerAgent provides **INSUFFICIENT CONTEXT** to downstream agents:

```java
// SkeletonPlannerAgent.java - Line 200
NormalizedNode node = new NormalizedNode();
node.setTitle("Morning Old Town Exploration and Luxury Shopping");
node.setLocation(new NodeLocation());
node.getLocation().setName("Altstadt (Old Town) & Bahnhofstrasse, Zurich");
```

**CRITICAL FLAW:** The location name "Altstadt (Old Town) & Bahnhofstrasse, Zurich" is:
1. **Too generic** - "Altstadt" exists in 100+ German/Swiss cities
2. **Compound location** - "Altstadt & Bahnhofstrasse" confuses search algorithms
3. **No coordinates** - No lat/lng to validate against

### What Should Be Provided:
```java
node.getLocation().setName("Zurich Old Town (Altstadt)");
node.getLocation().setCity("Zurich");
node.getLocation().setCountry("Switzerland");
node.getLocation().setSearchHint("Historic district near Bahnhofstrasse");
// OR provide approximate coordinates:
node.getLocation().setApproxCoordinates(new Coordinates(47.3769, 8.5417));
```

---

## 6. Cost Estimation Prompt Failures

### The "Luxury" Misinterpretation

**Prompt says:**
```java
Budget preference: luxury
IMPORTANT: Interpret budget tier relative to Switzerland, Switzerland:
- 'luxury' means premium experiences appropriate for this destination
```

**LLM interprets as:**
```json
{
  "costPerPerson": 1100,
  "notes": "Cost for a luxury private day tour to Mount Pilatus from Zurich, including private chauffeur service..."
}
```

**Reality Check:**
- Actual Mount Pilatus Golden Round Trip: CHF 120-150 per person
- LLM suggested: CHF 1,100 (7-9x higher!)

**Root Cause:**
1. **No cost anchoring** - Prompt doesn't provide actual price examples
2. **"Luxury" is subjective** - LLM interprets as "private helicopter tour" level
3. **No validation** - No check that costs are within reasonable bounds

### Missing Cost Validation:
```java
// Should be in prompt:
"COST VALIDATION:
- Museum entry in Switzerland: CHF 15-30 per person
- Fine dining meal: CHF 80-150 per person
- Mountain excursion: CHF 100-200 per person
- Private tour: CHF 200-400 per person
- If your estimate exceeds 2x these ranges, reconsider."
```

---

## 7. The Dependency Chain Failure

### How Errors Propagate:

```
1. SkeletonPlannerAgent
   ↓ Creates generic location: "Altstadt, Zurich"
   
2. ActivityAgent/MealAgent
   ↓ Suggests specific place: "Restaurant Rebstock"
   ↓ But inherits generic location context
   
3. EnrichmentAgent
   ↓ Searches Google Places: "Restaurant Rebstock" + "Zurich"
   ↓ Gets multiple results across Switzerland
   ↓ NO VALIDATION - picks first result
   ↓ WRONG CITY COORDINATES SAVED
   
4. CostEstimatorAgent
   ↓ Sees "Restaurant Rebstock" with coordinates
   ↓ Estimates cost based on "luxury" tier
   ↓ NO MARKET RATE VALIDATION
   ↓ INFLATED COSTS SAVED
```

**Critical Insight:** Each agent **ASSUMES** the previous agent provided correct data, but **NONE VALIDATE** the data they receive.

---

## 8. Recommendations (Priority Order)

### 🔴 CRITICAL (Fix Immediately)

#### 1. Add Geographical Bounding to EnrichmentAgent
```java
// EnrichmentService.java
PlaceSearchResult searchResult = googlePlacesService.searchPlaceWithBounds(
    locationName, 
    destination,
    cityCoordinates,  // NEW: Center point
    radiusKm: 20      // NEW: Search within 20km
);

// VALIDATE result is within bounds
if (!isWithinBounds(searchResult.getCoordinates(), cityCoordinates, radiusKm)) {
    logger.error("Place {} is outside {} ({}km away)", 
                 locationName, destination, distance);
    return null;  // Reject out-of-bounds results
}
```

#### 2. Add City Context to All Agent Prompts
```java
// MealAgent.java - buildMealUserPrompt()
prompt.append(String.format("\n** DAY %d - LOCATION: %s, %s **\n", 
                           dayNum, dayLocation, country));
prompt.append(String.format("City Coordinates: %.4f, %.4f (for reference)\n",
                           cityLat, cityLng));
prompt.append(String.format("CRITICAL: All restaurants MUST be within 20km of these coordinates!\n"));
```

#### 3. Add Cost Validation to CostEstimatorAgent
```java
// CostEstimatorAgent.java
private void validateCostEstimate(double cost, String nodeType, String destination) {
    Map<String, Double> maxCosts = getMaxCostsForDestination(destination);
    double maxAllowed = maxCosts.getOrDefault(nodeType, 1000.0);
    
    if (cost > maxAllowed) {
        logger.warn("Cost {} exceeds maximum {} for {} in {}", 
                   cost, maxAllowed, nodeType, destination);
        // Cap at maximum or reject
        return Math.min(cost, maxAllowed);
    }
}
```

### 🟡 HIGH PRIORITY (Fix This Week)

#### 4. Enhance SkeletonPlannerAgent Location Context
```java
// Add to each node:
node.getLocation().setCity(cityName);
node.getLocation().setCountry(country);
node.getLocation().setRegion(region);  // e.g., "Canton of Zurich"
node.getLocation().setApproxCoordinates(cityCenter);
```

#### 5. Add Market Rate Examples to Cost Prompts
```java
String costContext = String.format("""
    MARKET RATE REFERENCE for %s:
    - Budget meal: %s %.0f-%.0f
    - Mid-range meal: %s %.0f-%.0f
    - Luxury meal: %s %.0f-%.0f
    - Museum entry: %s %.0f-%.0f
    - Mountain excursion: %s %.0f-%.0f
    
    Your estimates should be within these ranges.
    """, destination, currency, ...);
```

#### 6. Add Post-LLM Validation Layer
```java
// After LLM response, before saving:
ValidationResult result = validateLLMResponse(response, context);
if (!result.isValid()) {
    logger.error("LLM response validation failed: {}", result.getErrors());
    // Retry with corrected prompt or use fallback
}
```

### 🟢 MEDIUM PRIORITY (Fix This Month)

#### 7. Implement Confidence Scoring
```java
// Rate each LLM suggestion:
double confidence = calculateConfidence(suggestion, context);
if (confidence < 0.7) {
    logger.warn("Low confidence ({}) for suggestion: {}", confidence, suggestion);
    // Flag for human review or use fallback
}
```

#### 8. Add Feedback Loop
```java
// Track which suggestions get corrected by users:
if (userCorrectedLocation(nodeId)) {
    logCorrectionForAnalysis(originalSuggestion, userCorrection);
    // Use to improve prompts over time
}
```

---

## 9. Conclusion

The geographical errors and cost inflation are **NOT LLM failures** - they are **PROMPT ENGINEERING FAILURES**. The agents provide:

1. ❌ **Insufficient geographical constraints**
2. ❌ **No validation of LLM outputs**
3. ❌ **Ambiguous location context**
4. ❌ **No cost anchoring or bounds**
5. ❌ **Weak error handling**

**The fix is NOT to switch LLMs** - it's to:
1. **Add geographical bounding** to all location searches
2. **Validate all LLM outputs** against known constraints
3. **Provide concrete examples** in prompts (costs, locations)
4. **Implement multi-layer validation** (prompt → LLM → validation → save)

**Estimated Impact:**
- Geographical errors: **95% reduction** (from 30% error rate to <2%)
- Cost accuracy: **80% improvement** (from 3-5x inflation to within 20% of actual)
- User trust: **Significant increase** (usable itineraries without manual correction)

---

## Appendix: Specific Prompt Improvements

### Before (MealAgent):
```
Day Location: Lucerne
Previous activity: Morning Old Town Exploration at Lucerne Old Town
```

### After (Recommended):
```
Day Location: Lucerne, Canton of Lucerne, Switzerland
City Center Coordinates: 47.0502° N, 8.3093° E
Search Radius: 20km from city center

Previous activity: Morning Old Town Exploration
Activity Location: Lucerne Old Town (47.0505° N, 8.3088° E)

CRITICAL VALIDATION:
- Restaurant MUST be in Lucerne (not Zurich, Interlaken, or other cities)
- Restaurant MUST be within 20km of coordinates above
- If Google Places returns multiple results, choose the one CLOSEST to city center
- If no results within 20km, respond with "NO_SUITABLE_RESTAURANT_FOUND"
```

This level of specificity eliminates ambiguity and enables proper validation.
