package com.tripplanner.enums;

/**
 * Specific transport modes with validation rules
 */
public enum TransportMode {
    FLIGHT("Flight", true, false, 500),
    TRAIN("Train", false, true, 300),
    BUS("Bus", false, true, 200),
    FERRY("Ferry", true, false, 100),
    TAXI("Taxi", false, true, 50),
    CAR("Car", false, true, 300),
    METRO("Metro", false, true, 30),
    WALK("Walk", false, true, 5);

    private final String displayName;
    private final boolean canCrossWater;
    private final boolean requiresLandConnection;
    private final int maxReasonableDistanceKm;

    TransportMode(String displayName, boolean canCrossWater, 
                  boolean requiresLandConnection, int maxReasonableDistanceKm) {
        this.displayName = displayName;
        this.canCrossWater = canCrossWater;
        this.requiresLandConnection = requiresLandConnection;
        this.maxReasonableDistanceKm = maxReasonableDistanceKm;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canCrossWater() {
        return canCrossWater;
    }

    public boolean requiresLandConnection() {
        return requiresLandConnection;
    }

    public int getMaxReasonableDistanceKm() {
        return maxReasonableDistanceKm;
    }

    public boolean isValidForDistance(double distanceKm) {
        return distanceKm <= maxReasonableDistanceKm;
    }
}
