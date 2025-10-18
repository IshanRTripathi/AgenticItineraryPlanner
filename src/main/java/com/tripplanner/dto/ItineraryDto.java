package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for itinerary data.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.annotation.JsonAutoDetect(
    getterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE,
    fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder = ItineraryDto.ItineraryDtoBuilder.class)
public class ItineraryDto {
    
    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItineraryDtoBuilder {
    }
    private String id;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private PartyDto party;
    private String budgetTier;
    private List<String> interests;
    private List<String> constraints;
    @Builder.Default
    private String language = "en";
    private String summary;
    private Map<String, Object> map;
    private List<ItineraryDayDto> days;
    private Map<String, Object> agentResults;
    @Builder.Default
    private String status = "draft";
    @Builder.Default
    private Instant createdAt = Instant.now();
    @Builder.Default
    private Instant updatedAt = Instant.now();
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("public")
    private boolean isPublic = false;
    private String shareToken;
    
    /**
     * Get trip duration in days.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getDurationDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Check if itinerary is completed.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isCompleted() {
        return "completed".equals(status);
    }
    
    /**
     * Check if itinerary generation is in progress.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isGenerating() {
        return "generating".equals(status);
    }
}

