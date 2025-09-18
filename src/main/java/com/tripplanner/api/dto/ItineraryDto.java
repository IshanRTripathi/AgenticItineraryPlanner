package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for itinerary data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItineraryDto(
        String id,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        PartyDto party,
        String budgetTier,
        List<String> interests,
        List<String> constraints,
        String language,
        String summary,
        Map<String, Object> map,
        List<ItineraryDayDto> days,
        String status,
        Instant createdAt,
        Instant updatedAt,
        boolean isPublic,
        String shareToken
) {
    
    /**
     * Create DTO from entity.
     */
    public static ItineraryDto fromEntity(Itinerary entity) {
        if (entity == null) {
            return null;
        }
        
        return new ItineraryDto(
                entity.getId(),
                entity.getDestination(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getParty() != null ? PartyDto.fromEntity(entity.getParty()) : null,
                entity.getBudgetTier(),
                entity.getInterests(),
                entity.getConstraints(),
                entity.getLanguage(),
                entity.getSummary(),
                entity.getMap(),
                entity.getDays() != null ? 
                        entity.getDays().stream().map(ItineraryDayDto::fromEntity).toList() : null,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isPublic(),
                entity.getShareToken()
        );
    }
    
    /**
     * Create public DTO (for sharing) - excludes sensitive information.
     */
    public static ItineraryDto createPublicDto(Itinerary entity) {
        if (entity == null) {
            return null;
        }
        
        return new ItineraryDto(
                entity.getId(),
                entity.getDestination(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getParty() != null ? PartyDto.fromEntity(entity.getParty()) : null,
                entity.getBudgetTier(),
                entity.getInterests(),
                entity.getConstraints(),
                entity.getLanguage(),
                entity.getSummary(),
                entity.getMap(),
                entity.getDays() != null ? 
                        entity.getDays().stream().map(ItineraryDayDto::fromEntity).toList() : null,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                true, // Always true for public DTOs
                null  // Don't expose share token in public view
        );
    }
    
    /**
     * Get trip duration in days.
     */
    public int getDurationDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Check if itinerary is completed.
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }
    
    /**
     * Check if itinerary generation is in progress.
     */
    public boolean isGenerating() {
        return "generating".equals(status);
    }
}

