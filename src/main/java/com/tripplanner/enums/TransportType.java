package com.tripplanner.enums;

/**
 * Explicit transport type classification to replace string parsing
 */
public enum TransportType {
    ARRIVAL_FLIGHT("Arrival Flight", true),
    DEPARTURE_FLIGHT("Departure Flight", true),
    INTER_CITY_TRAVEL("Inter-City Travel", false),
    LOCAL_TRANSPORT("Local Transport", false),
    FERRY("Ferry", false),
    UNKNOWN("Unknown", false);

    private final String displayName;
    private final boolean isAirTravel;

    TransportType(String displayName, boolean isAirTravel) {
        this.displayName = displayName;
        this.isAirTravel = isAirTravel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAirTravel() {
        return isAirTravel;
    }

    /**
     * Determine transport type from title (migration helper)
     */
    public static TransportType fromTitle(String title) {
        if (title == null) return UNKNOWN;
        
        String lower = title.toLowerCase();
        if (lower.contains("arrival")) return ARRIVAL_FLIGHT;
        if (lower.contains("departure")) return DEPARTURE_FLIGHT;
        if (lower.contains("travel:")) return INTER_CITY_TRAVEL;
        if (lower.contains("local") || lower.contains("taxi") || lower.contains("bus")) {
            return LOCAL_TRANSPORT;
        }
        if (lower.contains("ferry")) return FERRY;
        
        return UNKNOWN;
    }
}
