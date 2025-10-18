package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
}

