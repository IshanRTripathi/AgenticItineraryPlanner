package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;
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
     * Create DTO from entity.
     */
    public static ItineraryDayDto fromEntity(Itinerary.ItineraryDay entity) {
        if (entity == null) {
            return null;
        }
        
        return ItineraryDayDto.builder()
                .day(entity.getDay())
                .date(entity.getDate())
                .location(entity.getLocation())
                .activities(entity.getActivities() != null ? 
                        entity.getActivities().stream().map(ActivityDto::fromEntity).toList() : null)
                .accommodation(entity.getAccommodation() != null ? 
                        AccommodationDto.fromEntity(entity.getAccommodation()) : null)
                .transportation(entity.getTransportation() != null ? 
                        entity.getTransportation().stream().map(TransportationDto::fromEntity).toList() : null)
                .meals(entity.getMeals() != null ? 
                        entity.getMeals().stream().map(MealDto::fromEntity).toList() : null)
                .notes(entity.getNotes())
                .build();
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

