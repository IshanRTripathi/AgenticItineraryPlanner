package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for itinerary day information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItineraryDayDto(
        int day,
        LocalDate date,
        String location,
        List<ActivityDto> activities,
        AccommodationDto accommodation,
        List<TransportationDto> transportation,
        List<MealDto> meals,
        String notes
) {
    
    /**
     * Create DTO from entity.
     */
    public static ItineraryDayDto fromEntity(Itinerary.ItineraryDay entity) {
        if (entity == null) {
            return null;
        }
        
        return new ItineraryDayDto(
                entity.getDay(),
                entity.getDate(),
                entity.getLocation(),
                entity.getActivities() != null ? 
                        entity.getActivities().stream().map(ActivityDto::fromEntity).toList() : null,
                entity.getAccommodation() != null ? 
                        AccommodationDto.fromEntity(entity.getAccommodation()) : null,
                entity.getTransportation() != null ? 
                        entity.getTransportation().stream().map(TransportationDto::fromEntity).toList() : null,
                entity.getMeals() != null ? 
                        entity.getMeals().stream().map(MealDto::fromEntity).toList() : null,
                entity.getNotes()
        );
    }
    
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

