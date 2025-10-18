package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
}

