package com.tripplanner.enums;

/**
 * Explicit processing state to replace implicit state inference
 */
public enum ProcessingState {
    CREATED("Created by skeleton planner"),
    PENDING_ENRICHMENT("Waiting for agent processing"),
    ENRICHING("Agent currently processing"),
    ENRICHED("Agent completed successfully"),
    VALIDATING("Validation in progress"),
    VALIDATED("Validation passed"),
    FAILED("Processing failed"),
    RETRY_SCHEDULED("Queued for retry");

    private final String description;

    ProcessingState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == ENRICHED || this == VALIDATED || this == FAILED;
    }

    public boolean canRetry() {
        return this == FAILED || this == RETRY_SCHEDULED;
    }
}
