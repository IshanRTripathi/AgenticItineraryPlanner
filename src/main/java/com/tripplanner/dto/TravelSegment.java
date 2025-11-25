package com.tripplanner.dto;

public class TravelSegment {
    private int dayNumber; // Which day has travel
    private String fromCity;
    private String toCity;
    private String travelMode; // "flight", "train", "bus", "car", "ferry"
    private int estimatedHours; // Travel duration
    private boolean isFullDayTravel; // If true, no activities on this day
    private String notes; // Additional info (e.g., "Book train in advance")

    public TravelSegment() {
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getFromCity() {
        return fromCity;
    }

    public void setFromCity(String fromCity) {
        this.fromCity = fromCity;
    }

    public String getToCity() {
        return toCity;
    }

    public void setToCity(String toCity) {
        this.toCity = toCity;
    }

    public String getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(String travelMode) {
        this.travelMode = travelMode;
    }

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(int estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public boolean isFullDayTravel() {
        return isFullDayTravel;
    }

    public void setFullDayTravel(boolean fullDayTravel) {
        isFullDayTravel = fullDayTravel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
