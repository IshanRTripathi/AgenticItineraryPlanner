package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Response DTO for itinerary revision.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviseRes(
        String id,
        RevisionDiff diff,
        ItineraryDto full
) {
    
    /**
     * DTO for revision differences.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RevisionDiff(
            List<String> added,
            List<String> removed,
            List<String> updated
    ) {
        
        /**
         * Check if there are any changes.
         */
        public boolean hasChanges() {
            return (added != null && !added.isEmpty()) ||
                   (removed != null && !removed.isEmpty()) ||
                   (updated != null && !updated.isEmpty());
        }
        
        /**
         * Get total number of changes.
         */
        public int getTotalChanges() {
            int count = 0;
            if (added != null) count += added.size();
            if (removed != null) count += removed.size();
            if (updated != null) count += updated.size();
            return count;
        }
    }
}
