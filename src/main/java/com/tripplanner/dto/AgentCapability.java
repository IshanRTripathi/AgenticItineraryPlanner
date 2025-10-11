package com.tripplanner.dto;

/**
 * Enum defining all agent capabilities to avoid string-based ambiguity
 * and ensure consistent capability identification across the system.
 */
public enum AgentCapability {
    // Core Planning Capabilities
    ITINERARY_GENERATION("Generate complete itineraries from user requirements"),
    DAY_PLANNING("Plan individual days within an itinerary"),
    ACTIVITY_SCHEDULING("Schedule and time activities within days"),
    
    // Location and Places
    PLACE_DISCOVERY("Discover and recommend places to visit"),
    PLACE_ENRICHMENT("Enrich places with detailed information"),
    LOCATION_VALIDATION("Validate and verify location data"),
    ROUTE_OPTIMIZATION("Optimize routes between locations"),
    
    // Content Enhancement
    CONTENT_ENRICHMENT("Enhance itinerary content with descriptions and tips"),
    PHOTO_RECOMMENDATIONS("Recommend photo spots and timing"),
    FOOD_RECOMMENDATIONS("Recommend restaurants and local cuisine"),
    CULTURAL_INSIGHTS("Provide cultural context and insights"),
    
    // Booking and Reservations
    ACCOMMODATION_BOOKING("Handle hotel and accommodation bookings"),
    ACTIVITY_BOOKING("Book tours, activities, and experiences"),
    TRANSPORT_BOOKING("Book flights, trains, and transportation"),
    RESTAURANT_RESERVATIONS("Make restaurant reservations"),
    
    // Editing and Modifications
    ITINERARY_EDITING("Modify existing itineraries based on user requests"),
    CONTENT_SUMMARIZATION("Summarize itinerary content and changes"),
    CHANGE_VALIDATION("Validate proposed changes for feasibility"),
    CONFLICT_RESOLUTION("Resolve scheduling and booking conflicts"),
    
    // Analysis and Optimization
    BUDGET_ANALYSIS("Analyze and optimize trip budgets"),
    TIME_OPTIMIZATION("Optimize time allocation and scheduling"),
    PREFERENCE_LEARNING("Learn and adapt to user preferences"),
    WEATHER_INTEGRATION("Integrate weather data into planning"),
    
    // Communication and Interaction
    NATURAL_LANGUAGE_PROCESSING("Process natural language requests"),
    INTENT_CLASSIFICATION("Classify user intents and requests"),
    RESPONSE_GENERATION("Generate natural language responses"),
    DISAMBIGUATION("Handle ambiguous user requests"),
    
    // Data Management
    DATA_PERSISTENCE("Save and retrieve itinerary data"),
    VERSION_CONTROL("Manage itinerary versions and revisions"),
    EXPORT_GENERATION("Generate exports in various formats"),
    SHARING_MANAGEMENT("Manage itinerary sharing and permissions");
    
    private final String description;
    
    AgentCapability(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get capability by name with case-insensitive matching
     */
    public static AgentCapability fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Capability name cannot be null or empty");
        }
        
        try {
            return AgentCapability.valueOf(name.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown agent capability: " + name, e);
        }
    }
    
    /**
     * Check if this capability is a core planning capability
     */
    public boolean isCoreCapability() {
        return this == ITINERARY_GENERATION || 
               this == DAY_PLANNING || 
               this == ACTIVITY_SCHEDULING ||
               this == PLACE_DISCOVERY;
    }
    
    /**
     * Check if this capability requires external API integration
     */
    public boolean requiresExternalApi() {
        return this == PLACE_DISCOVERY ||
               this == PLACE_ENRICHMENT ||
               this == ACCOMMODATION_BOOKING ||
               this == ACTIVITY_BOOKING ||
               this == TRANSPORT_BOOKING ||
               this == WEATHER_INTEGRATION;
    }
    
    /**
     * Get all capabilities for a specific category
     */
    public static AgentCapability[] getPlanningCapabilities() {
        return new AgentCapability[]{
            ITINERARY_GENERATION, DAY_PLANNING, ACTIVITY_SCHEDULING, ROUTE_OPTIMIZATION
        };
    }
    
    public static AgentCapability[] getBookingCapabilities() {
        return new AgentCapability[]{
            ACCOMMODATION_BOOKING, ACTIVITY_BOOKING, TRANSPORT_BOOKING, RESTAURANT_RESERVATIONS
        };
    }
    
    public static AgentCapability[] getContentCapabilities() {
        return new AgentCapability[]{
            CONTENT_ENRICHMENT, PHOTO_RECOMMENDATIONS, FOOD_RECOMMENDATIONS, CULTURAL_INSIGHTS
        };
    }
}