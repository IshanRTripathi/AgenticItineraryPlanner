package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;

/**
 * DTO for transportation information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransportationDto(
        String mode,
        LocationDto from,
        LocationDto to,
        String departureTime,
        String arrivalTime,
        String duration,
        PriceDto price,
        String provider,
        String bookingUrl,
        String notes
) {
    
    /**
     * Create DTO from entity.
     */
    public static TransportationDto fromEntity(Itinerary.Transportation entity) {
        if (entity == null) {
            return null;
        }
        
        return new TransportationDto(
                entity.getMode(),
                LocationDto.fromEntity(entity.getFrom()),
                LocationDto.fromEntity(entity.getTo()),
                entity.getDepartureTime(),
                entity.getArrivalTime(),
                String.valueOf(entity.getDuration()),
                PriceDto.fromEntity(entity.getPrice()),
                entity.getProvider(),
                entity.getBookingUrl(),
                entity.getNotes()
        );
    }
}

