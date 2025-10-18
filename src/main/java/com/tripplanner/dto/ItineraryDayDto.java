package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for itinerary day information.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryDayDto {
    private int day;
    private LocalDate date;
    private String location;
    private List<ActivityDto> activities;
    private AccommodationDto accommodation;
    private List<TransportationDto> transportation;
    private List<MealDto> meals;
    private String notes;
    
    /**
     * Get total activities count.
     */
    public int getActivityCount() {
        return activities != null ? activities.size() : 0;
    }
    
    /**
     * Get total meals count.
     */
    public int getMealCount() {
        return meals != null ? meals.size() : 0;
    }
    
    /**
     * Check if accommodation is available for this day.
     */
    public boolean hasAccommodation() {
        return accommodation != null;
    }
    
    /**
     * Check if transportation is planned for this day.
     */
    public boolean hasTransportation() {
        return transportation != null && !transportation.isEmpty();
    }
}

