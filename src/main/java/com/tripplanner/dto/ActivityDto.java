package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
}

