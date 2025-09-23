package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;

/**
 * DTO for meal information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MealDto(
        String type,
        String name,
        String restaurant,
        LocationDto location,
        String time,
        PriceDto price,
        String cuisine,
        String notes
) {
    
    /**
     * Create DTO from entity.
     */
    public static MealDto fromEntity(Itinerary.Meal entity) {
        if (entity == null) {
            return null;
        }
        
        return new MealDto(
                entity.getType(),
                entity.getName(),
                entity.getRestaurant(),
                LocationDto.fromEntity(entity.getLocation()),
                entity.getTime(),
                PriceDto.fromEntity(entity.getPrice()),
                entity.getCuisine(),
                entity.getNotes()
        );
    }
}

