package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
}

