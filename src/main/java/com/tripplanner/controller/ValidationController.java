package com.tripplanner.controller;

import com.tripplanner.agents.ValidationAdvisorAgent;
import com.tripplanner.dto.validation.ValidationAdvice;
import com.tripplanner.dto.validation.ValidationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for validation operations
 */
@RestController
@RequestMapping("/api/v1/itineraries/{itineraryId}/validation")
public class ValidationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);
    
    private final ValidationAdvisorAgent validationAdvisorAgent;
    
    public ValidationController(ValidationAdvisorAgent validationAdvisorAgent) {
        this.validationAdvisorAgent = validationAdvisorAgent;
    }
    
    /**
     * Validate itinerary and get advice
     * 
     * @param itineraryId The itinerary to validate
     * @param level Validation level (BASIC, STANDARD, COMPREHENSIVE)
     * @return ValidationAdvice with issues and recommendations
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationAdvice> validateItinerary(
            @PathVariable String itineraryId,
            @RequestParam(defaultValue = "STANDARD") String level) {
        
        logger.info("Validating itinerary: {}, level: {}", itineraryId, level);
        
        try {
            ValidationLevel validationLevel = ValidationLevel.valueOf(level.toUpperCase());
            ValidationAdvice advice = validationAdvisorAgent.validateAndAdvise(itineraryId, validationLevel);
            
            if (!advice.isSuccess()) {
                return ResponseEntity.status(500).body(advice);
            }
            
            return ResponseEntity.ok(advice);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid validation level: {}", level);
            return ResponseEntity.badRequest().body(
                ValidationAdvice.error("Invalid validation level. Use: BASIC, STANDARD, or COMPREHENSIVE"));
        } catch (Exception e) {
            logger.error("Validation failed for itinerary: {}", itineraryId, e);
            return ResponseEntity.status(500).body(
                ValidationAdvice.error("Validation failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get validation schema for API documentation
     */
    @GetMapping("/schema")
    public ResponseEntity<Object> getValidationSchema() {
        return ResponseEntity.ok(java.util.Map.of(
            "endpoint", "/api/v1/itineraries/{itineraryId}/validation/validate",
            "method", "POST",
            "parameters", java.util.Map.of(
                "itineraryId", "Path parameter - ID of itinerary to validate",
                "level", "Query parameter - BASIC, STANDARD, or COMPREHENSIVE (default: STANDARD)"
            ),
            "response", java.util.Map.of(
                "success", "boolean - Whether validation succeeded",
                "itineraryId", "string - The validated itinerary ID",
                "level", "string - Validation level used",
                "summary", java.util.Map.of(
                    "totalErrors", "number - Count of error-level issues",
                    "totalWarnings", "number - Count of warning-level issues",
                    "criticalIssues", "number - Count of critical issues",
                    "score", "number - Overall quality score (0-100)"
                ),
                "issuesByCategory", "object - Issues grouped by category",
                "recommendations", "array - List of recommended actions",
                "metadata", java.util.Map.of(
                    "validatedAt", "timestamp - When validation ran",
                    "validationDuration", "number - Duration in milliseconds"
                )
            )
        ));
    }
}
