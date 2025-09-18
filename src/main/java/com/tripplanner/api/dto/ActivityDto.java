package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;

/**
 * DTO for activity information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActivityDto(
        String name,
        String description,
        LocationDto location,
        String startTime,
        String endTime,
        String duration,
        String category,
        PriceDto price,
        boolean bookingRequired,
        String bookingUrl,
        String tips
) {
    
    /**
     * Create DTO from entity.
     */
    public static ActivityDto fromEntity(Itinerary.Activity entity) {
        if (entity == null) {
            return null;
        }
        
        return new ActivityDto(
                entity.getName(),
                entity.getDescription(),
                LocationDto.fromEntity(entity.getLocation()),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDuration(),
                entity.getCategory(),
                PriceDto.fromEntity(entity.getPrice()),
                entity.isBookingRequired(),
                entity.getBookingUrl(),
                entity.getTips()
        );
    }
}

