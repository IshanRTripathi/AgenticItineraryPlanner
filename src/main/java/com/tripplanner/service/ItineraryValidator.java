package com.tripplanner.service;

import com.tripplanner.dto.*;
import com.tripplanner.enums.TransportMode;
import com.tripplanner.service.analytics.ItineraryMetricsTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-layer validation service for itineraries
 * Replaces scattered validation logic with centralized validation
 */
@Service
public class ItineraryValidator {

    private static final Logger logger = LoggerFactory.getLogger(ItineraryValidator.class);

    private final GeographyService geographyService;
    private final TransportMetadataService transportMetadataService;
    private final ItineraryMetricsTracker metricsTracker;

    public ItineraryValidator(GeographyService geographyService,
            TransportMetadataService transportMetadataService,
            ItineraryMetricsTracker metricsTracker) {
        this.geographyService = geographyService;
        this.transportMetadataService = transportMetadataService;
        this.metricsTracker = metricsTracker;
    }

    /**
     * Validate entire itinerary with all layers
     */
    public ValidationResult validate(NormalizedItinerary itinerary) {
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult();

        logger.info("Validating itinerary: {}", itinerary.getItineraryId());

        // Layer 1: Schema validation (basic structure)
        result.merge(validateSchema(itinerary));

        // Layer 2: Business rules
        result.merge(validateBusinessRules(itinerary));

        // Layer 3: Geography validation
        result.merge(validateGeography(itinerary));

        // Layer 4: Timing validation
        result.merge(validateTiming(itinerary));

        // Layer 5: Budget validation
        result.merge(validateBudget(itinerary));

        long duration = System.currentTimeMillis() - startTime;
        if (metricsTracker != null) {
            // Track individual warnings
            for (ValidationError warning : result.getWarnings()) {
                metricsTracker.trackValidationWarning(
                        itinerary.getItineraryId(),
                        warning.getCategory(),
                        null, // Node ID not easily available here without restructuring, passing null
                        warning.getMessage(),
                        warning.getSeverity());
            }

            // Track completion summary
            metricsTracker.trackValidationCompleted(
                    itinerary.getItineraryId(),
                    result.getErrors().size(),
                    result.getWarnings().size(),
                    result.isValid(),
                    duration);
        }

        logger.info("Validation complete: {} errors, {} warnings",
                result.getErrors().size(), result.getWarnings().size());

        return result;
    }

    /**
     * Layer 1: Schema validation
     */
    private ValidationResult validateSchema(NormalizedItinerary itinerary) {
        ValidationResult result = new ValidationResult();

        if (itinerary.getItineraryId() == null || itinerary.getItineraryId().isEmpty()) {
            result.addError("SCHEMA", "Itinerary ID is required");
        }

        if (itinerary.getDays() == null || itinerary.getDays().isEmpty()) {
            result.addError("SCHEMA", "Itinerary must have at least one day");
        }

        return result;
    }

    /**
     * Layer 2: Business rules validation
     */
    private ValidationResult validateBusinessRules(NormalizedItinerary itinerary) {
        ValidationResult result = new ValidationResult();

        // Check party size
        if (itinerary.getPartySize() != null && itinerary.getPartySize() <= 0) {
            result.addError("BUSINESS", "Party size must be positive");
        }

        // Check budget consistency
        if (itinerary.getBudgetMin() != null && itinerary.getBudgetMax() != null) {
            if (itinerary.getBudgetMin() > itinerary.getBudgetMax()) {
                result.addError("BUSINESS", "Minimum budget cannot exceed maximum budget");
            }
        }

        return result;
    }

    /**
     * Layer 3: Geography validation
     */
    private ValidationResult validateGeography(NormalizedItinerary itinerary) {
        ValidationResult result = new ValidationResult();

        if (itinerary.getDays() == null)
            return result;

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null)
                continue;

            for (NormalizedNode node : day.getNodes()) {
                if ("transport".equals(node.getType())) {
                    try {
                        // Validate transport mode against geography
                        transportMetadataService.validateTransportMode(node);
                    } catch (Exception e) {
                        result.addError("GEOGRAPHY",
                                String.format("Node %s: %s", node.getId(), e.getMessage()));
                    }

                    // Check for island destinations with land transport
                    if (node.getMetadata() instanceof TransportMetadata) {
                        TransportMetadata metadata = (TransportMetadata) node.getMetadata();

                        if (Boolean.TRUE.equals(metadata.getToIsIsland())) {
                            TransportMode mode = metadata.getSelectedMode();
                            if (mode != null && mode.requiresLandConnection()) {
                                result.addError("GEOGRAPHY",
                                        String.format("Node %s: Cannot use %s to island destination %s",
                                                node.getId(), mode, metadata.getToLocationName()));
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Layer 4: Timing validation
     */
    private ValidationResult validateTiming(NormalizedItinerary itinerary) {
        ValidationResult result = new ValidationResult();

        if (itinerary.getDays() == null)
            return result;

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null)
                continue;

            long dayStartTime = 0; // Start of day in ms
            long dayEndTime = 24 * 60 * 60 * 1000; // End of day in ms

            for (NormalizedNode node : day.getNodes()) {
                if (node.getTiming() == null)
                    continue;

                Long startTime = node.getTiming().getStartTime();
                Long endTime = node.getTiming().getEndTime();

                // Check if times are within day bounds
                if (startTime != null && (startTime < dayStartTime || startTime > dayEndTime)) {
                    result.addWarning("TIMING",
                            String.format("Node %s: Start time outside day bounds", node.getId()));
                }

                if (endTime != null && (endTime < dayStartTime || endTime > dayEndTime)) {
                    result.addWarning("TIMING",
                            String.format("Node %s: End time outside day bounds", node.getId()));
                }

                // Check if end time is after start time
                if (startTime != null && endTime != null && endTime < startTime) {
                    result.addError("TIMING",
                            String.format("Node %s: End time before start time", node.getId()));
                }

                // Check for unrealistic durations
                if (node.getTiming().getDurationMin() != null) {
                    int duration = node.getTiming().getDurationMin();

                    if ("activity".equals(node.getType()) && duration > 480) { // 8 hours
                        result.addWarning("TIMING",
                                String.format("Node %s: Activity duration %d min seems too long",
                                        node.getId(), duration));
                    }

                    if ("meal".equals(node.getType()) && duration > 180) { // 3 hours
                        result.addWarning("TIMING",
                                String.format("Node %s: Meal duration %d min seems too long",
                                        node.getId(), duration));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Layer 5: Budget validation
     */
    private ValidationResult validateBudget(NormalizedItinerary itinerary) {
        ValidationResult result = new ValidationResult();

        if (itinerary.getBudgetMax() == null) {
            return result; // No budget constraint
        }

        double totalCost = 0;
        int partySize = itinerary.getPartySize() != null ? itinerary.getPartySize() : 1;

        if (itinerary.getDays() != null) {
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() == null)
                    continue;

                for (NormalizedNode node : day.getNodes()) {
                    if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                        totalCost += node.getCost().getAmountPerPerson();
                    }
                }
            }
        }

        double costPerPerson = totalCost / partySize;

        // FIXED: Compare total trip cost against total trip budget (budgetMax * days)
        // Previously compared total cost against daily budget, causing false warnings
        int tripDays = itinerary.getDays() != null ? itinerary.getDays().size() : 1;
        double totalBudgetMax = itinerary.getBudgetMax() * tripDays;
        
        if (costPerPerson > totalBudgetMax) {
            result.addWarning("BUDGET",
                    String.format("Total trip cost %.2f exceeds total budget %.2f per person (%.2f/day × %d days)",
                            costPerPerson, totalBudgetMax, itinerary.getBudgetMax(), tripDays));
        }

        return result;
    }

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final List<ValidationError> errors = new ArrayList<>();
        private final List<ValidationError> warnings = new ArrayList<>();

        public void addError(String category, String message) {
            errors.add(new ValidationError(category, message, "ERROR"));
        }

        public void addWarning(String category, String message) {
            warnings.add(new ValidationError(category, message, "WARNING"));
        }

        public void merge(ValidationResult other) {
            this.errors.addAll(other.errors);
            this.warnings.addAll(other.warnings);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<ValidationError> getErrors() {
            return errors;
        }

        public List<ValidationError> getWarnings() {
            return warnings;
        }

        public List<ValidationError> getAll() {
            List<ValidationError> all = new ArrayList<>(errors);
            all.addAll(warnings);
            return all;
        }
    }

    /**
     * Validation error/warning
     */
    public static class ValidationError {
        private final String category;
        private final String message;
        private final String severity;

        public ValidationError(String category, String message, String severity) {
            this.category = category;
            this.message = message;
            this.severity = severity;
        }

        public String getCategory() {
            return category;
        }

        public String getMessage() {
            return message;
        }

        public String getSeverity() {
            return severity;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", severity, category, message);
        }
    }
}
