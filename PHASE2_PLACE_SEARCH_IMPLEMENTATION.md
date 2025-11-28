# Phase 2: Place Search with Photos - Implementation Plan

**Date:** November 28, 2025  
**Status:** 🚀 Ready to Implement  
**Estimated Time:** 2-3 days (16-24 hours)

---

## 🎯 GOAL

Enable users to search for places (museums, restaurants, attractions) with rich data:
- Photos from Google Places
- Ratings and reviews
- Opening hours
- Estimated costs
- Multiple suggestions to choose from

**Example User Flow:**
```
User: "I want to visit a museum on day 2"
AI: [Shows 3 museum suggestions with photos, ratings, prices]
User: [Clicks "Select" on Louvre Museum]
AI: "✅ Added Louvre Museum to Day 2 at 2pm"
```

---

## 🔄 SMART CACHING STRATEGY

### **Cache Key Design:**

```
place-search:{query}:{location}:{coordinates}:{type}
```

**Example:**
```
place-search:museum:interlaken, switzerland:46.6863,7.8632:museum
```

### **Why This Works:**

1. **Query-based:** Same query (e.g., "museum") gets cached
2. **Location-specific:** Different locations have different results
3. **Coordinate-based:** Ensures exact location match (normalized to 4 decimals)
4. **Type-filtered:** Different types (museum vs restaurant) cached separately

### **Cache Behavior:**

```
User 1: "Show me museums in Interlaken"
→ API Call → Validate results (within 30km) → Cache 3 results
→ Cache Key: place-search:museum:interlaken, switzerland:46.6863,7.8632:museum

User 2: "I want to visit a museum in Interlaken" (same location, same query)
→ Cache Hit → Return cached 3 results (instant, no API call)

User 3: "Show me museums in Zurich" (different location)
→ API Call → New cache entry
→ Cache Key: place-search:museum:zurich, switzerland:47.3769,8.5417:museum
```

### **Cache TTL:**
- **7 days** - Place data doesn't change frequently
- Photos, ratings, opening hours remain valid for a week
- After 7 days, fresh API call ensures up-to-date data

### **What Gets Cached:**
- ✅ Only validated results (within 30km distance filter)
- ✅ Complete PlaceSuggestion objects with photos
- ✅ Exactly 3 suggestions (or less if not available)
- ❌ NOT cached: Invalid results, API errors, empty results

---

## 📋 WHAT EXISTS

### ✅ Already Available:
1. **GooglePlacesService** - Has `searchPlace()` but returns single result
2. **GooglePlacesService.getPlaceDetails()** - Gets photos, reviews, ratings
3. **ToolsController** - Has infrastructure for tool endpoints
4. **ChatResponse** - Can hold additional data
5. **ChatMessage.tsx** - Can render custom components

### ❌ What's Missing:
1. **searchPlaces()** method - Returns multiple results (not just one)
2. **PlaceSuggestion DTO** - Structured data for place suggestions
3. **/api/v1/tools/search-places** endpoint - Tool endpoint
4. **PlaceSearchAgent** - Agent to handle place search intent
5. **PlaceSuggestionCard.tsx** - UI component to display suggestions
6. **Integration in ChatMessage.tsx** - Render place suggestions

---

## 🎨 UI MOCKUP

### **Chat Response with 3 Place Suggestions:**

```
┌─────────────────────────────────────────────────────────────┐
│ 🤖 AI Assistant                                             │
├─────────────────────────────────────────────────────────────┤
│ Here are 3 museum suggestions for Interlaken:              │
│                                                             │
│ ┌───────────────────────────────────────────────────────┐ │
│ │ ① [Photo: Jungfrau Museum exterior]                   │ │
│ │                                                         │ │
│ │   Jungfrau Museum                    ⭐ 4.7 (2.5K)    │ │
│ │   📍 Höheweg 37, Interlaken                           │ │
│ │   💰 $15 per person  ⏰ 9am - 6pm                      │ │
│ │                                                         │ │
│ │   [Select This Place]                                  │ │
│ └───────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌───────────────────────────────────────────────────────┐ │
│ │ ② [Photo: Touristik Museum interior]                  │ │
│ │                                                         │ │
│ │   Touristik Museum                   ⭐ 4.5 (1.2K)    │ │
│ │   📍 Bahnhofstrasse 28, Interlaken                    │ │
│ │   💰 $10 per person  ⏰ 10am - 5pm                     │ │
│ │                                                         │ │
│ │   [Select This Place]                                  │ │
│ └───────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌───────────────────────────────────────────────────────┐ │
│ │ ③ [Photo: Mystery Park entrance]                      │ │
│ │                                                         │ │
│ │   Mystery Park                       ⭐ 4.3 (890)     │ │
│ │   📍 Obere Bönigstrasse, Interlaken                   │ │
│ │   💰 $25 per person  ⏰ 9am - 7pm                      │ │
│ │                                                         │ │
│ │   [Select This Place]                                  │ │
│ └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### **Key UI Features:**
- ✅ **Numbered badges** (①②③) for easy reference
- ✅ **Large photos** at top of each card (400px wide)
- ✅ **Vertical stacking** - Cards stack on top of each other
- ✅ **Consistent spacing** - 12px gap between cards
- ✅ **Hover effects** - Border color changes, shadow appears
- ✅ **Always 3 cards** - Even if only 2 results, show 2 cards
- ✅ **Responsive** - Works on mobile (cards stack naturally)

---

## 🔨 IMPLEMENTATION STEPS

### **Step 1: Create PlaceSuggestion DTO (30 min)**

**File:** `src/main/java/com/tripplanner/dto/PlaceSuggestion.java` (NEW)

```java
package com.tripplanner.dto;

import java.util.List;

/**
 * Place suggestion with photos, ratings, and details.
 * Used for chat-based place search.
 */
public class PlaceSuggestion {
    private String placeId;
    private String name;
    private String address;
    private Double rating;
    private Integer userRatingsTotal;
    private Integer priceLevel; // 0-4 (0=Free, 1=$, 2=$$, 3=$$$, 4=$$$$)
    private List<Photo> photos;
    private List<String> types;
    private Geometry geometry;
    private String openingHours; // e.g., "9am - 6pm (Closed Tuesdays)"
    private Double estimatedCost; // Estimated cost per person
    private Integer estimatedDuration; // Estimated duration in minutes
    
    // Constructors
    public PlaceSuggestion() {}
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private PlaceSuggestion suggestion = new PlaceSuggestion();
        
        public Builder placeId(String placeId) {
            suggestion.placeId = placeId;
            return this;
        }
        
        public Builder name(String name) {
            suggestion.name = name;
            return this;
        }
        
        public Builder address(String address) {
            suggestion.address = address;
            return this;
        }
        
        public Builder rating(Double rating) {
            suggestion.rating = rating;
            return this;
        }
        
        public Builder userRatingsTotal(Integer userRatingsTotal) {
            suggestion.userRatingsTotal = userRatingsTotal;
            return this;
        }
        
        public Builder priceLevel(Integer priceLevel) {
            suggestion.priceLevel = priceLevel;
            return this;
        }
        
        public Builder photos(List<Photo> photos) {
            suggestion.photos = photos;
            return this;
        }
        
        public Builder types(List<String> types) {
            suggestion.types = types;
            return this;
        }
        
        public Builder geometry(Geometry geometry) {
            suggestion.geometry = geometry;
            return this;
        }
        
        public Builder openingHours(String openingHours) {
            suggestion.openingHours = openingHours;
            return this;
        }
        
        public Builder estimatedCost(Double estimatedCost) {
            suggestion.estimatedCost = estimatedCost;
            return this;
        }
        
        public Builder estimatedDuration(Integer estimatedDuration) {
            suggestion.estimatedDuration = estimatedDuration;
            return this;
        }
        
        public PlaceSuggestion build() {
            return suggestion;
        }
    }
    
    // Getters and Setters
    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    
    public Integer getUserRatingsTotal() { return userRatingsTotal; }
    public void setUserRatingsTotal(Integer userRatingsTotal) { this.userRatingsTotal = userRatingsTotal; }
    
    public Integer getPriceLevel() { return priceLevel; }
    public void setPriceLevel(Integer priceLevel) { this.priceLevel = priceLevel; }
    
    public List<Photo> getPhotos() { return photos; }
    public void setPhotos(List<Photo> photos) { this.photos = photos; }
    
    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }
    
    public Geometry getGeometry() { return geometry; }
    public void setGeometry(Geometry geometry) { this.geometry = geometry; }
    
    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    
    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }
    
    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    
    @Override
    public String toString() {
        return "PlaceSuggestion{" +
                "name='" + name + '\'' +
                ", rating=" + rating +
                ", priceLevel=" + priceLevel +
                ", address='" + address + '\'' +
                '}';
    }
}
```

---

### **Step 2: Add searchPlaces() to GooglePlacesService with Smart Caching (1-2 hours)**

**File:** `src/main/java/com/tripplanner/service/GooglePlacesService.java` (MODIFY)

Add this method after the existing `searchPlace()` method:

**Key Features:**
- ✅ Smart caching based on query + location + coordinates
- ✅ Cache validated results (only those passing distance filter)
- ✅ Reusable cache for same queries
- ✅ Always returns exactly 3 suggestions (or less if not available)

```java
/**
 * Search for multiple places by query and location with smart caching.
 * Returns exactly 3 suggestions (or less if not available).
 * 
 * CACHING STRATEGY:
 * - Cache key: query + location + coordinates (normalized)
 * - Only cache results that pass distance validation (within 30km)
 * - TTL: 7 days (place data doesn't change often)
 * - Reusable: Same query in same location returns cached results
 * 
 * @param itineraryId Itinerary ID for caching context
 * @param query Place type or name (e.g., "museum", "restaurant")
 * @param location Location context (e.g., "Paris, France")
 * @param type Optional place type filter (e.g., "museum", "restaurant")
 * @param maxResults Maximum number of results to return (default: 3, always 3 for chat)
 * @return List of exactly 3 place suggestions (or less if not available)
 */
public List<PlaceSuggestion> searchPlaces(String itineraryId, String query, String location, String type, int maxResults) {
    logger.info("🔍 [GooglePlacesService] Searching places: query='{}', location='{}', type='{}', maxResults={}",
            query, location, type, maxResults);

    if (query == null || query.trim().isEmpty()) {
        logger.warn("⚠️ [GooglePlacesService] Empty query provided to searchPlaces");
        return List.of();
    }

    // Geocode destination first (needed for cache key and validation)
    com.tripplanner.dto.Coordinates destCoords = null;
    if (location != null && !location.trim().isEmpty()) {
        destCoords = geocodeLocation(location);
        if (destCoords == null) {
            logger.error("❌ [GooglePlacesService] Cannot search without valid destination coordinates");
            return List.of();
        }
    } else {
        logger.error("❌ [GooglePlacesService] Location is required for place search");
        return List.of();
    }

    // Try cache first if itineraryId and toolCacheService available
    if (toolCacheService != null && itineraryId != null) {
        // Generate cache key: query + location + coordinates (normalized)
        // This ensures same query in same location returns cached results
        String normalizedQuery = query.toLowerCase().trim();
        String normalizedLocation = location.toLowerCase().trim();
        String coordsKey = String.format("%.4f,%.4f", destCoords.getLat(), destCoords.getLng());
        String cacheKey = String.format("place-search:%s:%s:%s:%s", 
            normalizedQuery, normalizedLocation, coordsKey, type != null ? type : "any");
        
        logger.info("📦 [GooglePlacesService] Checking cache with key: {}", cacheKey);
        
        return toolCacheService.getOrCompute(
            itineraryId,
            "place-search",
            cacheKey,
            Map.of(
                "query", query,
                "location", location,
                "type", type != null ? type : "",
                "coordinates", coordsKey
            ),
            () -> searchPlacesInternal(query, location, type, maxResults, destCoords),
            new com.fasterxml.jackson.core.type.TypeReference<List<PlaceSuggestion>>() {}.getClass()
        );
    }

    // Fallback to direct call (no caching)
    return searchPlacesInternal(query, location, type, maxResults, destCoords);
}

/**
 * Internal implementation of place search (called by cache or directly).
 * This method does the actual Google Places API call and validation.
 */
private List<PlaceSuggestion> searchPlacesInternal(String query, String location, String type, 
                                                    int maxResults, com.tripplanner.dto.Coordinates destCoords) {
    logger.info("🔍 [GooglePlacesService] Executing place search (cache miss or no cache)");

    // Check rate limits and circuit breaker
    checkRateLimit();
    checkCircuitBreaker();

    try {

        // Build search query
        String searchQuery = query;
        if (location != null && !location.trim().isEmpty()) {
            searchQuery = query + " in " + location;
        }

        // Build URL with location filter
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/textsearch/json")
                .queryParam("query", searchQuery)
                .queryParam("key", apiKey);

        // Add location bias if available
        if (destCoords != null) {
            urlBuilder.queryParam("location", destCoords.getLat() + "," + destCoords.getLng())
                    .queryParam("radius", "20000"); // 20km radius
        }

        // Add type filter if specified
        if (type != null && !type.trim().isEmpty()) {
            urlBuilder.queryParam("type", type);
        }

        String url = urlBuilder.build(false).toUriString();

        logger.debug("Requesting Google Places API: {}", url.replace(apiKey, "***KEY_HIDDEN***"));

        // Make GET request with retry logic
        PlaceSearchResponse response = makeRequestWithRetry(url, PlaceSearchResponse.class);

        // Increment request count
        incrementRequestCount();

        // Handle API response
        if (response != null && "OK".equals(response.getStatus()) &&
                response.getResults() != null && !response.getResults().isEmpty()) {

            logger.info("📊 [GooglePlacesService] Found {} results from Google Places API",
                    response.getResults().size());

            // Validate results against distance filter (only cache valid results)
            List<PlaceSuggestion> validatedResults = new ArrayList<>();
            
            for (PlaceSearchResult result : response.getResults()) {
                // Calculate distance from destination
                double distance = calculateDistance(
                    destCoords.getLat(), destCoords.getLng(),
                    result.getGeometry().getLocation().getLatitude(),
                    result.getGeometry().getLocation().getLongitude()
                );
                
                // Only include results within 30km (validated results)
                if (distance <= 30.0) {
                    PlaceSuggestion suggestion = toPlaceSuggestion(result, location, distance);
                    if (suggestion != null) {
                        validatedResults.add(suggestion);
                        logger.info("   ✅ [{}] '{}' - {}km away", 
                            validatedResults.size(), suggestion.getName(), String.format("%.1f", distance));
                    }
                }
                
                // Stop when we have enough results
                if (validatedResults.size() >= maxResults) {
                    break;
                }
            }

            logger.info("✅ [GooglePlacesService] Returning {} validated place suggestions (out of {} total results)", 
                validatedResults.size(), response.getResults().size());
            recordSuccess();
            
            // IMPORTANT: Always return exactly 3 suggestions for chat UI
            // If we have less, that's fine - UI will handle it
            return validatedResults;

        } else if (response != null && "ZERO_RESULTS".equals(response.getStatus())) {
            logger.warn("⚠️ [GooglePlacesService] No results found for query: '{}'", searchQuery);
            return List.of();
        } else {
            String status = response != null ? response.getStatus() : "null";
            logger.error("Google Places API error - Status: {}", status);
            recordFailure();
            return List.of();
        }

    } catch (Exception e) {
        logger.error("Failed to search places for query '{}': {}", query, e.getMessage(), e);
        recordFailure();
        return List.of();
    }
}

/**
 * Convert PlaceSearchResult to PlaceSuggestion with enriched data.
 * 
 * @param result The place search result from Google API
 * @param location The search location context
 * @param distance Distance from destination in km
 */
private PlaceSuggestion toPlaceSuggestion(PlaceSearchResult result, String location, double distance) {
    try {
        PlaceSuggestion suggestion = PlaceSuggestion.builder()
                .placeId(result.getPlaceId())
                .name(result.getName())
                .address(result.getFormattedAddress())
                .rating(result.getRating())
                .userRatingsTotal(result.getUserRatingsTotal())
                .priceLevel(result.getPriceLevel())
                .types(result.getTypes())
                .geometry(result.getGeometry())
                .build();

        // Get detailed information (photos, opening hours)
        try {
            PlaceDetails details = getPlaceDetails(result.getPlaceId());
            if (details != null) {
                suggestion.setPhotos(details.getPhotos());
                
                // Format opening hours
                if (details.getOpeningHours() != null) {
                    suggestion.setOpeningHours(formatOpeningHours(details.getOpeningHours()));
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get details for place {}: {}", result.getPlaceId(), e.getMessage());
            // Continue without details
        }

        // Estimate cost based on price level
        if (result.getPriceLevel() != null) {
            suggestion.setEstimatedCost(estimateCostFromPriceLevel(result.getPriceLevel()));
        }

        // Estimate duration based on place type
        suggestion.setEstimatedDuration(estimateDuration(result.getTypes()));

        return suggestion;

    } catch (Exception e) {
        logger.error("Failed to convert place result to suggestion: {}", e.getMessage());
        return null;
    }
}

/**
 * Format opening hours for display.
 */
private String formatOpeningHours(OpeningHours openingHours) {
    if (openingHours == null) {
        return null;
    }
    
    // If we have weekday text, use the current day
    if (openingHours.getWeekdayText() != null && !openingHours.getWeekdayText().isEmpty()) {
        int today = java.time.LocalDate.now().getDayOfWeek().getValue() % 7; // 0=Sunday
        if (today < openingHours.getWeekdayText().size()) {
            return openingHours.getWeekdayText().get(today);
        }
    }
    
    // Fallback to open now status
    return openingHours.isOpenNow() ? "Open now" : "Closed now";
}

/**
 * Estimate cost per person based on Google's price level (0-4).
 */
private Double estimateCostFromPriceLevel(Integer priceLevel) {
    if (priceLevel == null) {
        return null;
    }
    
    // Rough estimates in USD
    switch (priceLevel) {
        case 0: return 0.0;      // Free
        case 1: return 10.0;     // $
        case 2: return 25.0;     // $$
        case 3: return 50.0;     // $$$
        case 4: return 100.0;    // $$$$
        default: return null;
    }
}

/**
 * Estimate duration based on place types.
 */
private Integer estimateDuration(List<String> types) {
    if (types == null || types.isEmpty()) {
        return 120; // Default 2 hours
    }
    
    // Check for specific types
    for (String type : types) {
        if (type.contains("museum")) return 180;        // 3 hours
        if (type.contains("park")) return 120;          // 2 hours
        if (type.contains("restaurant")) return 90;     // 1.5 hours
        if (type.contains("cafe")) return 60;           // 1 hour
        if (type.contains("bar")) return 120;           // 2 hours
        if (type.contains("shopping")) return 120;      // 2 hours
        if (type.contains("church")) return 60;         // 1 hour
        if (type.contains("tourist_attraction")) return 120; // 2 hours
    }
    
    return 120; // Default 2 hours
}
```

---

### **Step 3: Add placeSuggestions to ChatResponse (5 min)**

**File:** `src/main/java/com/tripplanner/dto/ChatResponse.java` (MODIFY)

Add this field after `costImpact`:

```java
private List<PlaceSuggestion> placeSuggestions; // Place search suggestions

// Add getter/setter
public List<PlaceSuggestion> getPlaceSuggestions() {
    return placeSuggestions;
}

public void setPlaceSuggestions(List<PlaceSuggestion> placeSuggestions) {
    this.placeSuggestions = placeSuggestions;
}
```

---

### **Step 4: Create /api/v1/tools/search-places Endpoint (30 min)**

**File:** `src/main/java/com/tripplanner/controller/ToolsController.java` (MODIFY)

Add this endpoint after the existing tool endpoints:

```java
/**
 * Search for places with photos, ratings, and details.
 * Returns multiple suggestions for user to choose from.
 */
@PostMapping("/search-places")
public ResponseEntity<PlaceSearchToolResult> searchPlaces(
        @RequestBody PlaceSearchToolRequest request,
        @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
    try {
        logToolUsage("search-places", agentName, request.getItineraryId());
        logger.info("Searching places: query='{}', location='{}', type='{}'",
                request.getQuery(), request.getLocation(), request.getType());

        // Validate request
        if (request.getQuery() == null || request.getQuery().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    PlaceSearchToolResult.error("query is required"));
        }

        // Get location context from itinerary if not provided
        String location = request.getLocation();
        if ((location == null || location.isEmpty()) && request.getItineraryId() != null) {
            Optional<NormalizedItinerary> itinerary = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itinerary.isPresent()) {
                location = itinerary.get().getDestination();
                logger.info("Using itinerary destination: {}", location);
            }
        }

        if (location == null || location.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    PlaceSearchToolResult.error("location is required (provide location or itineraryId)"));
        }

        // Search places
        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 3;
        List<PlaceSuggestion> suggestions = placesService.searchPlaces(
                request.getQuery(),
                location,
                request.getType(),
                maxResults);

        logger.info("Found {} place suggestions", suggestions.size());
        return ResponseEntity.ok(PlaceSearchToolResult.success(suggestions));

    } catch (Exception e) {
        logger.error("Place search failed", e);
        return ResponseEntity.status(500).body(
                PlaceSearchToolResult.error("Failed to search places: " + e.getMessage()));
    }
}

@GetMapping("/schema/search-places")
public ResponseEntity<Map<String, Object>> getSearchPlacesSchema() {
    return ResponseEntity.ok(Map.of(
            "name", "search_places",
            "description", "Search for places with photos, ratings, and details. Returns multiple suggestions.",
            "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "itineraryId", Map.of(
                                    "type", "string",
                                    "description", "ID of the itinerary (for location context)"),
                            "query", Map.of(
                                    "type", "string",
                                    "description", "Place name or type to search for (e.g., 'museum', 'Louvre')"),
                            "location", Map.of(
                                    "type", "string",
                                    "description", "Location to search in (e.g., 'Paris, France'). Optional if itineraryId provided."),
                            "type", Map.of(
                                    "type", "string",
                                    "description", "Optional place type filter (e.g., 'museum', 'restaurant', 'tourist_attraction')"),
                            "maxResults", Map.of(
                                    "type", "integer",
                                    "description", "Maximum number of results to return (default: 3, max: 10)")),
                    "required", new String[] { "query" })));
}
```

Create the request/response DTOs:

**File:** `src/main/java/com/tripplanner/dto/tools/PlaceSearchToolRequest.java` (NEW)

```java
package com.tripplanner.dto.tools;

public class PlaceSearchToolRequest {
    private String itineraryId;
    private String query;
    private String location;
    private String type;
    private Integer maxResults;
    
    // Constructors, getters, setters
    public PlaceSearchToolRequest() {}
    
    public String getItineraryId() { return itineraryId; }
    public void setItineraryId(String itineraryId) { this.itineraryId = itineraryId; }
    
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
}
```

**File:** `src/main/java/com/tripplanner/dto/tools/PlaceSearchToolResult.java` (NEW)

```java
package com.tripplanner.dto.tools;

import com.tripplanner.dto.PlaceSuggestion;
import java.util.List;

public class PlaceSearchToolResult {
    private boolean success;
    private List<PlaceSuggestion> suggestions;
    private String error;
    
    public static PlaceSearchToolResult success(List<PlaceSuggestion> suggestions) {
        PlaceSearchToolResult result = new PlaceSearchToolResult();
        result.setSuccess(true);
        result.setSuggestions(suggestions);
        return result;
    }
    
    public static PlaceSearchToolResult error(String error) {
        PlaceSearchToolResult result = new PlaceSearchToolResult();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public List<PlaceSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<PlaceSuggestion> suggestions) { this.suggestions = suggestions; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
```

---

---

## 🎨 FRONTEND IMPLEMENTATION

### **Step 5: Create PlaceSuggestionCard.tsx Component (2-3 hours)**

**File:** `frontend/src/components/chat/PlaceSuggestionCard.tsx` (NEW)

**Requirements:**
- ✅ Display exactly 3 suggestions vertically stacked
- ✅ Each card shows: photo, name, rating, address, cost, hours
- ✅ "Select" button to add to itinerary
- ✅ Responsive design
- ✅ Beautiful UI with hover effects

```typescript
import React from 'react';
import { Star, MapPin, DollarSign, Clock } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface PlaceSuggestion {
  placeId: string;
  name: string;
  address: string;
  rating?: number;
  userRatingsTotal?: number;
  priceLevel?: number;
  photos?: Array<{ photoReference: string }>;
  openingHours?: string;
  estimatedCost?: number;
  estimatedDuration?: number;
}

interface PlaceSuggestionCardProps {
  suggestion: PlaceSuggestion;
  onSelect: (suggestion: PlaceSuggestion) => void;
  index: number; // For numbering (1, 2, 3)
}

export function PlaceSuggestionCard({ suggestion, onSelect, index }: PlaceSuggestionCardProps) {
  // Get photo URL from Google Places
  const getPhotoUrl = (photoReference: string, maxWidth: number = 400) => {
    return `https://maps.googleapis.com/maps/api/place/photo?maxwidth=${maxWidth}&photo_reference=${photoReference}&key=${process.env.NEXT_PUBLIC_GOOGLE_PLACES_API_KEY}`;
  };

  // Format price level to dollar signs
  const formatPriceLevel = (level?: number) => {
    if (level === undefined || level === null) return null;
    if (level === 0) return 'Free';
    return '$'.repeat(level);
  };

  // Format number with K suffix
  const formatNumber = (num?: number) => {
    if (!num) return '0';
    if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
    return num.toString();
  };

  return (
    <div className="border border-gray-200 rounded-lg p-4 hover:border-primary hover:shadow-md transition-all bg-white">
      {/* Number Badge */}
      <div className="flex items-start gap-3">
        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center font-semibold text-sm">
          {index}
        </div>
        
        <div className="flex-1">
          {/* Photo */}
          {suggestion.photos && suggestion.photos.length > 0 && (
            <div className="mb-3 -mx-4 -mt-4">
              <img
                src={getPhotoUrl(suggestion.photos[0].photoReference)}
                alt={suggestion.name}
                className="w-full h-48 object-cover rounded-t-lg"
                onError={(e) => {
                  // Fallback to placeholder if image fails to load
                  e.currentTarget.src = '/placeholder-place.jpg';
                }}
              />
            </div>
          )}

          {/* Name and Rating */}
          <div className="flex items-start justify-between mb-2">
            <h4 className="font-semibold text-base text-gray-900 flex-1 pr-2">
              {suggestion.name}
            </h4>
            {suggestion.rating && (
              <div className="flex items-center gap-1 flex-shrink-0">
                <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                <span className="font-medium text-sm">{suggestion.rating.toFixed(1)}</span>
                {suggestion.userRatingsTotal && (
                  <span className="text-xs text-gray-500">
                    ({formatNumber(suggestion.userRatingsTotal)})
                  </span>
                )}
              </div>
            )}
          </div>

          {/* Address */}
          <div className="flex items-start gap-2 text-sm text-gray-600 mb-3">
            <MapPin className="h-4 w-4 flex-shrink-0 mt-0.5" />
            <span className="line-clamp-2">{suggestion.address}</span>
          </div>

          {/* Details Row */}
          <div className="flex items-center gap-4 text-sm mb-3 flex-wrap">
            {/* Cost */}
            {(suggestion.estimatedCost !== undefined || suggestion.priceLevel !== undefined) && (
              <div className="flex items-center gap-1 text-gray-700">
                <DollarSign className="h-4 w-4" />
                <span>
                  {suggestion.estimatedCost 
                    ? `$${suggestion.estimatedCost.toFixed(0)} per person`
                    : formatPriceLevel(suggestion.priceLevel)
                  }
                </span>
              </div>
            )}

            {/* Opening Hours */}
            {suggestion.openingHours && (
              <div className="flex items-center gap-1 text-gray-700">
                <Clock className="h-4 w-4" />
                <span className="text-xs">{suggestion.openingHours}</span>
              </div>
            )}
          </div>

          {/* Select Button */}
          <Button
            onClick={() => onSelect(suggestion)}
            className="w-full"
            size="sm"
          >
            Select This Place
          </Button>
        </div>
      </div>
    </div>
  );
}
```

---

### **Step 6: Integrate in ChatMessage.tsx (1 hour)**

**File:** `frontend/src/components/chat/ChatMessage.tsx` (MODIFY)

Add after the cost impact display section:

```typescript
import { PlaceSuggestionCard } from './PlaceSuggestionCard';

// In the component render:

{/* Place Suggestions - Always show 3 vertically stacked */}
{m.placeSuggestions && m.placeSuggestions.length > 0 && (
  <div className="mt-4 space-y-3">
    <div className="text-sm font-medium text-gray-700 mb-2">
      {m.placeSuggestions.length === 1 
        ? '1 suggestion found:' 
        : `${m.placeSuggestions.length} suggestions found:`}
    </div>
    
    {/* Render exactly 3 suggestions (or less if not available) */}
    {m.placeSuggestions.slice(0, 3).map((suggestion, idx) => (
      <PlaceSuggestionCard
        key={suggestion.placeId}
        suggestion={suggestion}
        index={idx + 1}
        onSelect={(selected) => handleSelectPlace(selected, m.day)}
      />
    ))}
    
    {m.placeSuggestions.length === 0 && (
      <div className="text-sm text-gray-500 italic">
        No suggestions found. Try a different search.
      </div>
    )}
  </div>
)}
```

Add the handler function:

```typescript
const handleSelectPlace = async (suggestion: PlaceSuggestion, day?: number) => {
  try {
    // Send a follow-up message to add the selected place
    const message = `Add ${suggestion.name} to day ${day || 'the itinerary'}`;
    await sendChatMessage(message);
  } catch (error) {
    console.error('Failed to add place:', error);
  }
};
```

---

### **Step 7: Update ChatTypes.ts (5 min)**

**File:** `frontend/src/types/ChatTypes.ts` (MODIFY)

Add PlaceSuggestion interface:

```typescript
export interface PlaceSuggestion {
  placeId: string;
  name: string;
  address: string;
  rating?: number;
  userRatingsTotal?: number;
  priceLevel?: number;
  photos?: Array<{
    photoReference: string;
  }>;
  types?: string[];
  geometry?: {
    location: {
      latitude: number;
      longitude: number;
    };
  };
  openingHours?: string;
  estimatedCost?: number;
  estimatedDuration?: number;
}

// Add to ChatMessage interface
export interface ChatMessage {
  // ... existing fields
  placeSuggestions?: PlaceSuggestion[]; // NEW
}
```

---

## 📝 TESTING CHECKLIST

### **Backend Testing:**
- [ ] `/api/v1/tools/search-places` endpoint works
- [ ] Returns exactly 3 suggestions (or less)
- [ ] Each suggestion has photos, rating, address
- [ ] Cache works: Same query returns cached results
- [ ] Cache key includes coordinates for location-specific caching
- [ ] Only validated results (within 30km) are cached

### **Frontend Testing:**
- [ ] PlaceSuggestionCard renders correctly
- [ ] Shows photo, name, rating, address, cost
- [ ] 3 cards displayed vertically stacked
- [ ] "Select" button works
- [ ] Hover effects work
- [ ] Responsive on mobile

### **End-to-End Testing:**
- [ ] User asks: "Show me museums in Interlaken"
- [ ] AI returns 3 museum suggestions with photos
- [ ] User clicks "Select" on one
- [ ] Place is added to itinerary
- [ ] Same query returns cached results (instant)

---

## ✅ SUCCESS CRITERIA

- [ ] Backend compiles without errors
- [ ] `/api/v1/tools/search-places` endpoint works
- [ ] Returns 3 place suggestions with photos
- [ ] Each suggestion has: name, rating, photos, address, estimated cost
- [ ] Frontend displays suggestions beautifully
- [ ] User can select a suggestion to add to itinerary

---

**Ready to start with Step 1?**
