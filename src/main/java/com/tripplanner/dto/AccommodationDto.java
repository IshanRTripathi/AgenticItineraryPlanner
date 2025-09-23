package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for accommodation information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccommodationDto(
        String name,
        String type,
        LocationDto location,
        LocalDate checkIn,
        LocalDate checkOut,
        PriceDto price,
        double rating,
        List<String> amenities,
        String bookingUrl
) {
    
    /**
     * Create DTO from entity.
     */
    public static AccommodationDto fromEntity(Itinerary.Accommodation entity) {
        if (entity == null) {
            return null;
        }
        
        return new AccommodationDto(
                entity.getName(),
                entity.getType(),
                LocationDto.fromEntity(entity.getLocation()),
                entity.getCheckIn(),
                entity.getCheckOut(),
                PriceDto.fromEntity(entity.getPrice()),
                entity.getRating(),
                entity.getAmenities(),
                entity.getBookingUrl()
        );
    }
}

