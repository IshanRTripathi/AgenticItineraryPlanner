# End-to-End Pipeline Flow: 3-Day Switzerland Trip
## Complete Walkthrough with LLM Prompts & Responses

**Document Version:** 1.0  
**Date:** November 28, 2025  
**Purpose:** Detailed example showing exactly what each agent does, what LLM receives/responds

---

## 📋 **USER REQUEST**

```json
{
  "destination": "Switzerland",
  "startDate": "2025-12-01",
  "endDate": "2025-12-03",
  "durationDays": 3,
  "budgetTier": "moderate",
  "budgetMax": 1500,
  "interests": ["adventure", "culture", "food"],
  "partySize": 2
}
```

---

## 🚀 **PHASE 0: CITY ALLOCATION AGENT**

### **What It Does:**
Analyzes destination and determines which cities to visit and how many days per city.

### **Input:**
- Destination: "Switzerland"
- Duration: 3 days
- Interests: adventure, culture, food

### **LLM Prompt Sent:**
```
You are a travel planning expert specializing in multi-city itinerary design.

Your task: Analyze the destination and create a realistic city allocation plan.

DESTINATION: Switzerland
DURATION: 3 days
INTERESTS: adventure, culture, food

STEP 1: DETERMINE DESTINATION TYPE
- Is it a CITY? → Return 1 city
- Is it a COUNTRY? → Return 2-3 cities based on duration

STEP 2: ALLOCATE CITIES
For 3 days in Switzerland, recommend:
- Which cities to visit
- How many days per city
- Travel time between cities

Return JSON:
{
  "destinationType": "country",
  "allocations": [
    {
      "cityName": "...",
      "startDay": 1,
      "endDay": 2,
      "recommendedDays": 2,
      "rationale": "..."
    }
  ],
  "travelSegments": [...]
}
```

### **LLM Response:**
```json
{
  "destinationType": "country",
  "allocations": [
    {
      "cityName": "Interlaken",
      "startDay": 1,
      "endDay": 2,
      "recommendedDays": 2,
      "rationale": "Perfect base for adventure activities in Bernese Oberland"
    },
    {
      "cityName": "Zurich",
      "startDay": 3,
      "endDay": 3,
      "recommendedDays": 1,
      "rationale": "Cultural exploration and Swiss cuisine"
    }
  ],
  "travelSegments": [
    {
      "fromCity": "Interlaken",
      "toCity": "Zurich",
      "dayNumber": 3,
      "estimatedHours": 2,
      "transportMode": "train"
    }
  ]
}
```

### **What Gets Saved:**
- City allocation plan stored in `itinerary.agentData.cityAllocation`
- Used by all subsequent agents

### **Time Taken:** ~16 seconds

---

## 🏗️ **PHASE 1: SKELETON PLANNER AGENT**

### **What It Does:**
Creates day structure with placeholder nodes (no specific names yet).

### **Input:**
- City allocation plan (Interlaken Days 1-2, Zurich Day 3)
- User interests
- Budget tier

### **Day 1 LLM Prompt:**
```
Generate a lightweight skeleton for Day 1 of a 3-day trip.

LOCATION: Interlaken, Switzerland
INTERESTS: adventure, culture, food
BUDGET: moderate
PREVIOUS DAYS: None (first day)

Create placeholder nodes with:
- Type (attraction/meal/transport)
- Rough timing (morning/afternoon/evening)
- Generic titles ("Morning Activity", "Lunch")
- Duration estimates

DO NOT include:
- Specific place names
- Coordinates
- Photos
- Detailed descriptions

Return JSON with nodes array.
```

### **Day 1 LLM Response:**
```json
{
  "dayNumber": 1,
  "location": "Interlaken",
  "nodes": [
    {
      "id": "day1_node1",
      "type": "attraction",
      "title": "Morning Adventure Activity",
      "timing": {
        "startTime": 32400000,
        "endTime": 46800000,
        "durationMin": 240
      }
    },
    {
      "id": "day1_node2",
      "type": "meal",
      "title": "Lunch",
      "timing": {
        "startTime": 46800000,
        "endTime": 50400000,
        "durationMin": 60
      }
    },
    {
      "id": "day1_node3",
      "type": "attraction",
      "title": "Afternoon Cultural Activity",
      "timing": {
        "startTime": 50400000,
        "endTime": 61200000,
        "durationMin": 180
      }
    },
    {
      "id": "day1_node4",
      "type": "meal",
      "title": "Dinner",
      "timing": {
        "startTime": 68400000,
        "endTime": 72000000,
        "durationMin": 60
      }
    }
  ]
}
```

### **Day 2 LLM Prompt:**
```
Generate skeleton for Day 2.

LOCATION: Interlaken, Switzerland
PREVIOUS DAYS:
Day 1 (Interlaken): Morning Adventure Activity, Lunch, Afternoon Cultural Activity, Dinner

IMPORTANT: Avoid repeating similar activities from previous days.

Create different types of activities for variety.
```

### **Day 2 LLM Response:**
```json
{
  "dayNumber": 2,
  "location": "Interlaken",
  "nodes": [
    {
      "id": "day2_node1",
      "type": "attraction",
      "title": "Morning Scenic Activity",
      "timing": {...}
    },
    {
      "id": "day2_node2",
      "type": "meal",
      "title": "Lunch",
      "timing": {...}
    },
    {
      "id": "day2_node3",
      "type": "attraction",
      "title": "Afternoon Activity",
      "timing": {...}
    },
    {
      "id": "day2_node4",
      "type": "meal",
      "title": "Dinner",
      "timing": {...}
    }
  ]
}
```

### **Day 3 LLM Prompt:**
```
Generate skeleton for Day 3.

LOCATION: Zurich, Switzerland
PREVIOUS DAYS:
Day 1 (Interlaken): Morning Adventure Activity, Afternoon Cultural Activity
Day 2 (Interlaken): Morning Scenic Activity, Afternoon Activity

TRAVEL: Train from Interlaken to Zurich (2 hours) - already added as transport node

Create activities for Zurich (different city, different vibe).
```

### **Day 3 LLM Response:**
```json
{
  "dayNumber": 3,
  "location": "Zurich",
  "nodes": [
    {
      "id": "day3_node1",
      "type": "transport",
      "title": "Train: Interlaken to Zurich",
      "timing": {...}
    },
    {
      "id": "day3_node2",
      "type": "attraction",
      "title": "Afternoon Cultural Activity",
      "timing": {...}
    },
    {
      "id": "day3_node3",
      "type": "meal",
      "title": "Dinner",
      "timing": {...}
    }
  ]
}
```

### **What Gets Saved:**
```json
{
  "itineraryId": "it_abc123",
  "days": [
    {
      "dayNumber": 1,
      "location": "Interlaken",
      "nodes": [
        {"id": "day1_node1", "type": "attraction", "title": "Morning Adventure Activity"},
        {"id": "day1_node2", "type": "meal", "title": "Lunch"},
        {"id": "day1_node3", "type": "attraction", "title": "Afternoon Cultural Activity"},
        {"id": "day1_node4", "type": "meal", "title": "Dinner"}
      ]
    },
    // Day 2 and 3...
  ]
}
```

### **Time Taken:** ~30 seconds (with city-grouped parallel)

---

## 🎨 **PHASE 2: POPULATION - ACTIVITY AGENT**

### **What It Does:**
Takes placeholder "attraction" nodes and adds specific place names.

### **Input:**
- Skeleton with placeholder nodes
- City allocation plan
- User interests

### **LLM Prompt Sent:**
```
You are populating attraction nodes with specific place names.

ITINERARY CONTEXT:
- Destination: Switzerland
- Days: 3
- Interests: adventure, culture, food
- Budget: moderate

NODES TO POPULATE:
Day 1 (Interlaken):
- Node: day1_node1 - "Morning Adventure Activity" (9:00-13:00, 240 min)
- Node: day1_node3 - "Afternoon Cultural Activity" (14:00-17:00, 180 min)

Day 2 (Interlaken):
- Node: day2_node1 - "Morning Scenic Activity" (9:00-13:00, 240 min)
- Node: day2_node3 - "Afternoon Activity" (14:00-17:00, 180 min)

Day 3 (Zurich):
- Node: day3_node2 - "Afternoon Cultural Activity" (14:00-17:00, 180 min)

PREVIOUS DAYS CONTEXT:
None yet (first agent to run)

IMPORTANT:
- Suggest SPECIFIC attraction names (e.g., "Jungfraujoch", "Harder Kulm")
- Match activity type to timing and duration
- Ensure variety across days
- Consider user interests (adventure, culture)

Return JSON:
{
  "attractions": [
    {
      "nodeId": "day1_node1",
      "title": "Paragliding in Interlaken",
      "description": "...",
      "category": "adventure",
      "durationMin": 240
    },
    ...
  ]
}
```

### **LLM Response:**
```json
{
  "attractions": [
    {
      "nodeId": "day1_node1",
      "title": "Paragliding in Interlaken",
      "description": "Tandem paragliding experience over Interlaken with stunning views of Eiger, Mönch, and Jungfrau",
      "category": "adventure",
      "durationMin": 240,
      "difficulty": "moderate"
    },
    {
      "nodeId": "day1_node3",
      "title": "Höhematte Park and Interlaken Town Walk",
      "description": "Explore the charming town center, visit local shops, and enjoy views from Höhematte Park",
      "category": "culture",
      "durationMin": 180,
      "difficulty": "easy"
    },
    {
      "nodeId": "day2_node1",
      "title": "Jungfraujoch - Top of Europe",
      "description": "Train journey to Europe's highest railway station with glacier views and ice palace",
      "category": "scenic",
      "durationMin": 300,
      "difficulty": "easy"
    },
    {
      "nodeId": "day2_node3",
      "title": "Harder Kulm Viewpoint",
      "description": "Funicular ride to panoramic viewpoint with restaurant and viewing platform",
      "category": "scenic",
      "durationMin": 180,
      "difficulty": "easy"
    },
    {
      "nodeId": "day3_node2",
      "title": "Old Town Zurich Walking Tour",
      "description": "Explore Altstadt, Grossmünster, Fraumünster, and Bahnhofstrasse",
      "category": "culture",
      "durationMin": 180,
      "difficulty": "easy"
    }
  ]
}
```

### **What Gets Updated:**
```json
{
  "days": [
    {
      "dayNumber": 1,
      "nodes": [
        {
          "id": "day1_node1",
          "type": "attraction",
          "title": "Paragliding in Interlaken",  // ← UPDATED
          "description": "Tandem paragliding...",  // ← ADDED
          "category": "adventure",  // ← ADDED
          "timing": {...}
        },
        {
          "id": "day1_node2",
          "type": "meal",
          "title": "Lunch"  // ← Still placeholder
        },
        {
          "id": "day1_node3",
          "type": "attraction",
          "title": "Höhematte Park and Interlaken Town Walk",  // ← UPDATED
          "description": "Explore the charming...",  // ← ADDED
          "category": "culture"  // ← ADDED
        },
        {
          "id": "day1_node4",
          "type": "meal",
          "title": "Dinner"  // ← Still placeholder
        }
      ]
    }
  ]
}
```

### **Time Taken:** ~5 seconds (parallel with other agents)

---

## 🍽️ **PHASE 2: POPULATION - MEAL AGENT**

### **What It Does:**
Takes placeholder "meal" nodes and adds specific restaurant names.

### **Input:**
- Skeleton with placeholder meal nodes
- Activity nodes (now populated with specific names)
- Dietary restrictions (if any)

### **LLM Prompt Sent:**
```
You are populating meal nodes with specific restaurant names.

ITINERARY CONTEXT:
- Location: Interlaken (Days 1-2), Zurich (Day 3)
- Budget: moderate
- Dietary restrictions: None

NODES TO POPULATE:
Day 1 (Interlaken):
- Node: day1_node2 - "Lunch" (13:00-14:00)
  - After: Paragliding in Interlaken
  - Before: Höhematte Park Walk
- Node: day1_node4 - "Dinner" (19:00-20:00)
  - After: Höhematte Park Walk

Day 2 (Interlaken):
- Node: day2_node2 - "Lunch" (13:00-14:00)
  - After: Jungfraujoch
- Node: day2_node4 - "Dinner" (19:00-20:00)
  - After: Harder Kulm

Day 3 (Zurich):
- Node: day3_node3 - "Dinner" (19:00-20:00)
  - After: Old Town Walking Tour

REQUIREMENTS:
- Suggest SPECIFIC restaurant names
- Match meal type (breakfast/lunch/dinner)
- Consider location proximity to activities
- Ensure variety across days
- Match budget tier (moderate)

Return JSON with restaurant suggestions.
```

### **LLM Response:**
```json
{
  "meals": [
    {
      "nodeId": "day1_node2",
      "title": "Lunch at Restaurant Laterne",
      "description": "Traditional Swiss cuisine in cozy atmosphere, known for fondue and rösti",
      "mealType": "lunch",
      "cuisineType": "Swiss",
      "priceLevel": 2
    },
    {
      "nodeId": "day1_node4",
      "title": "Dinner at Restaurant Bären",
      "description": "Local favorite serving Swiss specialties with mountain views",
      "mealType": "dinner",
      "cuisineType": "Swiss",
      "priceLevel": 2
    },
    {
      "nodeId": "day2_node2",
      "title": "Lunch at Jungfraujoch Restaurant",
      "description": "Mountain restaurant at Top of Europe with panoramic views",
      "mealType": "lunch",
      "cuisineType": "Swiss",
      "priceLevel": 3
    },
    {
      "nodeId": "day2_node4",
      "title": "Dinner at Restaurant Hirschen",
      "description": "Historic inn serving traditional Swiss dishes and local wines",
      "mealType": "dinner",
      "cuisineType": "Swiss",
      "priceLevel": 2
    },
    {
      "nodeId": "day3_node3",
      "title": "Dinner at Zeughauskeller",
      "description": "Famous Zurich beer hall in historic armory building, hearty Swiss fare",
      "mealType": "dinner",
      "cuisineType": "Swiss",
      "priceLevel": 2
    }
  ]
}
```

### **What Gets Updated:**
```json
{
  "days": [
    {
      "dayNumber": 1,
      "nodes": [
        {
          "id": "day1_node1",
          "type": "attraction",
          "title": "Paragliding in Interlaken"
        },
        {
          "id": "day1_node2",
          "type": "meal",
          "title": "Lunch at Restaurant Laterne",  // ← UPDATED
          "description": "Traditional Swiss cuisine...",  // ← ADDED
          "mealType": "lunch",  // ← ADDED
          "cuisineType": "Swiss"  // ← ADDED
        },
        {
          "id": "day1_node3",
          "type": "attraction",
          "title": "Höhematte Park and Interlaken Town Walk"
        },
        {
          "id": "day1_node4",
          "type": "meal",
          "title": "Dinner at Restaurant Bären",  // ← UPDATED
          "description": "Local favorite...",  // ← ADDED
          "mealType": "dinner"  // ← ADDED
        }
      ]
    }
  ]
}
```

### **Time Taken:** ~3 seconds (parallel with other agents)

---

## 🚗 **PHASE 2: POPULATION - TRANSPORT AGENT**

### **What It Does:**
Adds transport details for inter-city travel and local transport.

### **Input:**
- Travel segments from city allocation
- Activity locations

### **LLM Prompt Sent:**
```
Add transport details for travel segments.

TRAVEL SEGMENTS:
Day 3: Interlaken → Zurich (2 hours, train)

REQUIREMENTS:
- Add specific train details
- Estimate timing
- Add booking information

Return JSON with transport details.
```

### **LLM Response:**
```json
{
  "transport": [
    {
      "nodeId": "day3_node1",
      "title": "Train: Interlaken Ost to Zurich HB",
      "description": "Direct InterCity train, scenic route through Swiss countryside",
      "transportMode": "train",
      "duration": 120,
      "estimatedCost": 65,
      "bookingInfo": "Book via SBB.ch or at station"
    }
  ]
}
```

### **What Gets Updated:**
Transport node gets detailed information added.

### **Time Taken:** ~1 second (parallel with other agents)

---

## 🌍 **PHASE 3: ENRICHMENT AGENT**

### **What It Does:**
Adds REAL WORLD DATA from Google Places API (photos, coordinates, ratings).

### **Input:**
- Nodes with specific names (from Population phase)
- City locations

### **Process for Each Node:**

#### **Step 1: Search Google Places**
```
API Call: Google Places Text Search
Query: "Paragliding in Interlaken, Switzerland"
```

**API Response:**
```json
{
  "results": [
    {
      "place_id": "ChIJxxx123",
      "name": "Paragliding Interlaken",
      "geometry": {
        "location": {
          "lat": 46.6863,
          "lng": 7.8632
        }
      },
      "rating": 4.8,
      "user_ratings_total": 1250
    }
  ]
}
```

#### **Step 2: Get Place Details**
```
API Call: Google Places Details
Place ID: ChIJxxx123
```

**API Response:**
```json
{
  "result": {
    "place_id": "ChIJxxx123",
    "name": "Paragliding Interlaken",
    "formatted_address": "Höheweg 115, 3800 Interlaken, Switzerland",
    "geometry": {
      "location": {"lat": 46.6863, "lng": 7.8632}
    },
    "photos": [
      {"photo_reference": "photo_ref_1"},
      {"photo_reference": "photo_ref_2"}
    ],
    "rating": 4.8,
    "user_ratings_total": 1250,
    "reviews": [...],
    "opening_hours": {...},
    "website": "https://paragliding-interlaken.ch",
    "phone": "+41 33 823 41 00"
  }
}
```

### **What Gets Updated:**
```json
{
  "id": "day1_node1",
  "type": "attraction",
  "title": "Paragliding in Interlaken",
  "description": "Tandem paragliding experience...",
  "location": {
    "name": "Paragliding Interlaken",  // ← ADDED
    "address": "Höheweg 115, 3800 Interlaken",  // ← ADDED
    "coordinates": {  // ← ADDED
      "lat": 46.6863,
      "lng": 7.8632
    },
    "placeId": "ChIJxxx123",  // ← ADDED
    "rating": 4.8,  // ← ADDED
    "userRatingsTotal": 1250,  // ← ADDED
    "photos": [  // ← ADDED
      "https://maps.googleapis.com/maps/api/place/photo?photoreference=photo_ref_1...",
      "https://maps.googleapis.com/maps/api/place/photo?photoreference=photo_ref_2..."
    ],
    "website": "https://paragliding-interlaken.ch",  // ← ADDED
    "phone": "+41 33 823 41 00"  // ← ADDED
  }
}
```

### **This Happens for ALL Nodes:**
- Day 1: Paragliding, Restaurant Laterne, Höhematte Park, Restaurant Bären
- Day 2: Jungfraujoch, Jungfraujoch Restaurant, Harder Kulm, Restaurant Hirschen
- Day 3: Old Town Zurich, Zeughauskeller

### **Time Taken:** ~3 seconds (full parallel mode, all days at once)

---

## 💰 **PHASE 4: COST ESTIMATOR AGENT**

### **What It Does:**
Calculates costs for each node using rule-based estimation.

### **Input:**
- Enriched nodes with place details
- Budget tier (moderate)
- Destination currency (CHF)

### **Process for Each Node:**

#### **Example: Paragliding**
```
Node Type: attraction
Category: adventure
Title: "Paragliding in Interlaken"
Budget Tier: moderate
Currency: CHF

Rule-Based Calculation:
1. Base cost (adventure activity): $150 USD
2. Budget multiplier (moderate): 1.0x
3. Adjusted cost: $150 USD
4. Currency conversion: $150 USD → 135 CHF
5. Regional multiplier (Switzerland): 1.3x
6. Final cost: 175 CHF per person
```

#### **Example: Restaurant Laterne**
```
Node Type: meal
Meal Type: lunch
Price Level: 2 (from Google Places)
Budget Tier: moderate
Currency: CHF

Rule-Based Calculation:
1. Base cost (lunch, price level 2): $25 USD
2. Budget multiplier (moderate): 1.0x
3. Adjusted cost: $25 USD
4. Currency conversion: $25 USD → 22 CHF
5. Regional multiplier (Switzerland): 1.3x
6. Final cost: 29 CHF per person
```

### **What Gets Updated:**
```json
{
  "id": "day1_node1",
  "title": "Paragliding in Interlaken",
  "cost": {  // ← ADDED
    "amountPerPerson": 175,
    "currency": "CHF",
    "estimationMethod": "rule-based"
  }
}
```

### **Final Cost Summary:**
```
Day 1:
- Paragliding: 175 CHF
- Lunch at Restaurant Laterne: 29 CHF
- Höhematte Park: 0 CHF (free)
- Dinner at Restaurant Bären: 45 CHF
Day 1 Total: 249 CHF per person

Day 2:
- Jungfraujoch: 220 CHF
- Lunch at Jungfraujoch Restaurant: 35 CHF
- Harder Kulm: 32 CHF
- Dinner at Restaurant Hirschen: 45 CHF
Day 2 Total: 332 CHF per person

Day 3:
- Train to Zurich: 65 CHF
- Old Town Walking Tour: 0 CHF (free)
- Dinner at Zeughauskeller: 42 CHF
Day 3 Total: 107 CHF per person

TRIP TOTAL: 688 CHF per person (2 people = 1376 CHF)
Budget: 1500 CHF
Remaining: 124 CHF ✅
```

### **Time Taken:** <1 second

---

## 📊 **FINAL ITINERARY OUTPUT**

```json
{
  "itineraryId": "it_abc123",
  "destination": "Switzerland",
  "startDate": "2025-12-01",
  "endDate": "2025-12-03",
  "durationDays": 3,
  "budgetTier": "moderate",
  "budgetMax": 1500,
  "totalCost": 1376,
  "currency": "CHF",
  "partySize": 2,
  "days": [
    {
      "dayNumber": 1,
      "date": "2025-12-01",
      "location": "Interlaken",
      "nodes": [
        {
          "id": "day1_node1",
          "type": "attraction",
          "title": "Paragliding in Interlaken",
          "description": "Tandem paragliding experience over Interlaken...",
          "category": "adventure",
          "timing": {
            "startTime": 32400000,
            "endTime": 46800000,
            "durationMin": 240
          },
          "location": {
            "name": "Paragliding Interlaken",
            "address": "Höheweg 115, 3800 Interlaken",
            "coordinates": {"lat": 46.6863, "lng": 7.8632},
            "placeId": "ChIJxxx123",
            "rating": 4.8,
            "userRatingsTotal": 1250,
            "photos": ["https://...photo1.jpg", "https://...photo2.jpg"],
            "website": "https://paragliding-interlaken.ch",
            "phone": "+41 33 823 41 00"
          },
          "cost": {
            "amountPerPerson": 175,
            "currency": "CHF"
          }
        },
        {
          "id": "day1_node2",
          "type": "meal",
          "title": "Lunch at Restaurant Laterne",
          "description": "Traditional Swiss cuisine in cozy atmosphere...",
          "mealType": "lunch",
          "cuisineType": "Swiss",
          "timing": {
            "startTime": 46800000,
            "endTime": 50400000,
            "durationMin": 60
          },
          "location": {
            "name": "Restaurant Laterne",
            "address": "Marktgasse 57, 3800 Interlaken",
            "coordinates": {"lat": 46.6850, "lng": 7.8650},
            "placeId": "ChIJyyy456",
            "rating": 4.5,
            "userRatingsTotal": 890,
            "photos": ["https://...photo1.jpg"],
            "priceLevel": 2
          },
          "cost": {
            "amountPerPerson": 29,
            "currency": "CHF"
          }
        }
        // ... more nodes
      ]
    },
    {
      "dayNumber": 2,
      "date": "2025-12-02",
      "location": "Interlaken",
      "nodes": [...]
    },
    {
      "dayNumber": 3,
      "date": "2025-12-03",
      "location": "Zurich",
      "nodes": [...]
    }
  ]
}
```

---

## ⏱️ **TOTAL TIME BREAKDOWN**

```
Phase 0: City Allocation      → 16s
Phase 1: Skeleton Generation  → 30s (parallel)
Phase 2: Population           → 5s (parallel)
  ├─ Activity Agent           → 5s
  ├─ Meal Agent              → 3s  } Run in parallel
  └─ Transport Agent         → 1s
Phase 3: Enrichment           → 3s (parallel, all days)
Phase 4: Cost Estimation      → <1s (rule-based)
Phase 5: Finalization         → 2s

TOTAL: ~57 seconds
```

---

## 🔑 **KEY TAKEAWAYS**

1. **LLM is used for:**
   - City allocation (intelligent analysis)
   - Skeleton structure (day planning)
   - Specific place suggestions (personalized recommendations)

2. **Google Places API is used for:**
   - Real-world data (photos, ratings, coordinates)
   - Place verification
   - Contact information

3. **Rules are used for:**
   - Cost estimation (fast, consistent)
   - Validation
   - Timing calculations

4. **Data flows through phases:**
   - Skeleton: Generic placeholders
   - Population: Specific names (LLM)
   - Enrichment: Real data (Google Places)
   - Cost: Price estimates (Rules)

5. **Optimization opportunities:**
   - Cache Google Places data (85-95% hit rate)
   - Use hybrid approach for population (tools + LLM)
   - Parallel execution at every phase

---

**Document End**
