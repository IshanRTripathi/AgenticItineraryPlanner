package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;
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
     * Create DTO from entity.
     */
    public static ItineraryDto fromEntity(Itinerary entity) {
        if (entity == null) {
            return null;
        }
        
        return ItineraryDto.builder()
                .id(entity.getId().toString())
                .destination(entity.getDestination())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .party(entity.getParty() != null ? PartyDto.fromEntity(entity.getParty()) : null)
                .budgetTier(entity.getBudgetTier())
                .interests(entity.getInterests())
                .constraints(entity.getConstraints())
                .language(entity.getLanguage())
                .summary(entity.getSummary())
                .map(null) // Map data not implemented in JPA version yet
                .days(entity.getDays() != null ? 
                        entity.getDays().stream().map(ItineraryDayDto::fromEntity).toList() : null)
                .agentResults(null) // will be populated separately
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isPublic(entity.isPublic())
                .shareToken(entity.getShareToken())
                .build();
    }
    
    /**
     * Create public DTO (for sharing) - excludes sensitive information.
     */
    public static ItineraryDto createPublicDto(Itinerary entity) {
        if (entity == null) {
            return null;
        }
        
        return ItineraryDto.builder()
                .id(entity.getId().toString())
                .destination(entity.getDestination())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .party(entity.getParty() != null ? PartyDto.fromEntity(entity.getParty()) : null)
                .budgetTier(entity.getBudgetTier())
                .interests(entity.getInterests())
                .constraints(entity.getConstraints())
                .language(entity.getLanguage())
                .summary(entity.getSummary())
                .map(null) // Map data not implemented in JPA version yet
                .days(entity.getDays() != null ? 
                        entity.getDays().stream().map(ItineraryDayDto::fromEntity).toList() : null)
                .agentResults(null) // will be populated separately
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isPublic(true) // Always true for public DTOs
                .shareToken(null) // Don't expose share token in public view
                .build();
    }
    
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

