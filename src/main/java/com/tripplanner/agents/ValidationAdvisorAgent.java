package com.tripplanner.agents;

import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.validation.ValidationAdvice;
import com.tripplanner.dto.validation.ValidationIssue;
import com.tripplanner.dto.validation.ValidationLevel;
import com.tripplanner.dto.memory.Memory;
import com.tripplanner.dto.memory.MemoryQuery;
import com.tripplanner.enums.MemoryCategory;
import com.tripplanner.service.ItineraryJsonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ValidationAdvisor Agent - Orchestrates validation tools and provides smart advice
 * 
 * Single Responsibility: Run validation tools, filter by memory, generate LLM-powered advice
 * 
 * Integration Points:
 * - PipelineOrchestrator: After cost estimation (Phase 4.5)
 * - EditorAgent: After applying changes
 * - MemoryAgent: For retrieving validation preferences
 */
@Component
public class ValidationAdvisorAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationAdvisorAgent.class);
    
    private final RestTemplate restTemplate;
    private final MemoryAgent memoryAgent;
    private final ItineraryJsonService itineraryJsonService;
    
    public ValidationAdvisorAgent(
            MemoryAgent memoryAgent,
            ItineraryJsonService itineraryJsonService) {
        this.restTemplate = new RestTemplate();
        this.memoryAgent = memoryAgent;
        this.itineraryJsonService = itineraryJsonService;
    }
    
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.addSupportedTask("validate_and_advise");
        capabilities.setPriority(15);
        capabilities.setEnabled(true);
        return capabilities;
    }
    
    /**
     * Main validation orchestration method
     * 
     * @param itineraryId The itinerary to validate
     * @param level Validation level (BASIC, STANDARD, COMPREHENSIVE)
     * @return ValidationAdvice with filtered issues and recommendations
     */
    public ValidationAdvice validateAndAdvise(String itineraryId, ValidationLevel level) {
        long startTime = System.currentTimeMillis();
        logger.info("🔍 Starting validation for itinerary: {}, level: {}", itineraryId, level);
        
        try {
            // 1. Load itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isEmpty()) {
                return ValidationAdvice.error("Itinerary not found: " + itineraryId);
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            
            // 2. Skip if group trip (memory not supported for groups)
            if (itinerary.getPartySize() != null && itinerary.getPartySize() > 1) {
                logger.info("Skipping validation for group trip (partySize: {})", itinerary.getPartySize());
                return ValidationAdvice.skipped("Group trip validation coming soon!");
            }
            
            // 3. Get user's validation preferences from memory
            List<Memory> validationPrefs = memoryAgent.retrieve(
                itineraryId,
                MemoryQuery.builder().category(MemoryCategory.VALIDATION).build()
            );
            
            logger.info("Retrieved {} validation preferences from memory", validationPrefs.size());
            
            // 4. Run validation tools in parallel
            Map<String, CompletableFuture<ValidationResult>> validations = 
                runValidationsAsync(itineraryId, level);
            
            // 5. Wait for all results
            CompletableFuture.allOf(validations.values().toArray(new CompletableFuture[0])).join();
            
            // 6. Collect all issues
            List<ValidationIssue> allIssues = collectIssues(validations);
            logger.info("Collected {} total validation issues", allIssues.size());
            
            // 7. Filter based on user's past dismissals
            List<ValidationIssue> filteredIssues = filterByMemory(allIssues, validationPrefs);
            logger.info("After filtering: {} issues remain ({} filtered)", 
                filteredIssues.size(), allIssues.size() - filteredIssues.size());
            
            // 8. Generate smart recommendations (TODO: integrate LLM)
            ValidationAdvice advice = generateAdvice(itineraryId, filteredIssues, level);
            
            // 9. Store validation event in memory (for learning)
            storeValidationEvent(itineraryId, advice);
            
            // 10. Set metadata
            long duration = System.currentTimeMillis() - startTime;
            advice.getMetadata().setValidatedAt(System.currentTimeMillis());
            advice.getMetadata().setValidationDuration(duration);
            
            logger.info("✅ Validation complete in {}ms: {} errors, {} warnings", 
                duration, advice.getSummary().getTotalErrors(), advice.getSummary().getTotalWarnings());
            
            return advice;
            
        } catch (Exception e) {
            logger.error("Validation failed for itinerary: {}", itineraryId, e);
            return ValidationAdvice.error("Validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Run validation tools in parallel based on level
     */
    private Map<String, CompletableFuture<ValidationResult>> runValidationsAsync(
            String itineraryId, ValidationLevel level) {
        
        Map<String, CompletableFuture<ValidationResult>> validations = new HashMap<>();
        
        // BASIC level: Only critical validations
        if (level == ValidationLevel.BASIC) {
            // Schema validation only
            validations.put("schema", CompletableFuture.supplyAsync(() -> 
                runSchemaValidation(itineraryId)));
            return validations;
        }
        
        // STANDARD level: All core validations
        validations.put("conflicts", CompletableFuture.supplyAsync(() -> 
            runConflictCheck(itineraryId)));
        validations.put("completeness", CompletableFuture.supplyAsync(() -> 
            runCompletenessCheck(itineraryId)));
        validations.put("budget", CompletableFuture.supplyAsync(() -> 
            runBudgetHealthCheck(itineraryId)));
        validations.put("timing", CompletableFuture.supplyAsync(() -> 
            runTimingFeasibilityCheck(itineraryId)));
        
        // COMPREHENSIVE level: Add dietary and other checks
        if (level == ValidationLevel.COMPREHENSIVE) {
            validations.put("dietary", CompletableFuture.supplyAsync(() -> 
                runDietaryComplianceCheck(itineraryId)));
            
            // Optional: Add geography validation (transport modes vs geography)
            // validations.put("geography", CompletableFuture.supplyAsync(() -> 
            //     runGeographyCheck(itineraryId)));
            
            // Optional: Add accessibility validation (wheelchair, mobility)
            // validations.put("accessibility", CompletableFuture.supplyAsync(() -> 
            //     runAccessibilityCheck(itineraryId)));
        }
        
        return validations;
    }
    
    /**
     * Collect issues from all validation results
     */
    private List<ValidationIssue> collectIssues(
            Map<String, CompletableFuture<ValidationResult>> validations) {
        
        List<ValidationIssue> allIssues = new ArrayList<>();
        
        for (Map.Entry<String, CompletableFuture<ValidationResult>> entry : validations.entrySet()) {
            try {
                ValidationResult result = entry.getValue().get();
                if (result != null && result.issues != null) {
                    allIssues.addAll(result.issues);
                }
            } catch (Exception e) {
                logger.error("Failed to get validation result for: {}", entry.getKey(), e);
            }
        }
        
        return allIssues;
    }
    
    /**
     * Filter issues based on user's past dismissals in memory
     */
    private List<ValidationIssue> filterByMemory(
            List<ValidationIssue> issues, List<Memory> validationPrefs) {
        
        if (validationPrefs.isEmpty()) {
            return issues; // No filtering needed
        }
        
        // Extract dismissed issue types from memory
        Set<String> dismissedTypes = validationPrefs.stream()
            .filter(m -> "DISMISSAL".equals(m.getType().name()))
            .map(m -> (String) m.getData().get("issueType"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        if (dismissedTypes.isEmpty()) {
            return issues;
        }
        
        logger.info("Filtering {} dismissed issue types: {}", dismissedTypes.size(), dismissedTypes);
        
        // Filter out dismissed issues
        return issues.stream()
            .filter(issue -> !dismissedTypes.contains(issue.getType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Generate validation advice with recommendations
     */
    private ValidationAdvice generateAdvice(
            String itineraryId, List<ValidationIssue> issues, ValidationLevel level) {
        
        ValidationAdvice advice = new ValidationAdvice();
        advice.setSuccess(true);
        advice.setItineraryId(itineraryId);
        advice.setLevel(level);
        
        // Group issues by category
        Map<String, List<ValidationIssue>> issuesByCategory = issues.stream()
            .collect(Collectors.groupingBy(
                issue -> issue.getCategory() != null ? issue.getCategory() : "GENERAL"
            ));
        advice.setIssuesByCategory(issuesByCategory);
        
        // Calculate summary
        ValidationAdvice.ValidationSummary summary = advice.getSummary();
        summary.setTotalErrors((int) issues.stream()
            .filter(i -> "ERROR".equals(i.getSeverity())).count());
        summary.setTotalWarnings((int) issues.stream()
            .filter(i -> "WARNING".equals(i.getSeverity())).count());
        summary.setCriticalIssues((int) issues.stream()
            .filter(i -> "ERROR".equals(i.getSeverity())).count());
        
        // Calculate score (100 - issues)
        double score = Math.max(0, 100 - (summary.getTotalErrors() * 10) - (summary.getTotalWarnings() * 2));
        summary.setScore(score);
        
        // Generate recommendations (simple rule-based for now, LLM integration later)
        List<ValidationAdvice.Recommendation> recommendations = generateRecommendations(issuesByCategory);
        advice.setRecommendations(recommendations);
        
        return advice;
    }
    
    /**
     * Generate recommendations from issues
     * Uses rule-based logic with optional LLM enhancement
     */
    private List<ValidationAdvice.Recommendation> generateRecommendations(
            Map<String, List<ValidationIssue>> issuesByCategory) {
        
        List<ValidationAdvice.Recommendation> recommendations = new ArrayList<>();
        
        // Rule-based recommendations
        for (Map.Entry<String, List<ValidationIssue>> entry : issuesByCategory.entrySet()) {
            String category = entry.getKey();
            List<ValidationIssue> categoryIssues = entry.getValue();
            
            if (categoryIssues.isEmpty()) continue;
            
            // Generate smart recommendations based on issue patterns
            recommendations.addAll(generateCategoryRecommendations(category, categoryIssues));
        }
        
        // Optional: Enhance with LLM for more contextual advice
        // This can be enabled via feature flag: validation.advisor.llm-advice
        // recommendations = enhanceWithLLM(recommendations, issuesByCategory);
        
        return recommendations;
    }
    
    /**
     * Generate category-specific recommendations
     */
    private List<ValidationAdvice.Recommendation> generateCategoryRecommendations(
            String category, List<ValidationIssue> issues) {
        
        List<ValidationAdvice.Recommendation> recommendations = new ArrayList<>();
        
        boolean hasErrors = issues.stream().anyMatch(i -> "ERROR".equals(i.getSeverity()));
        int errorCount = (int) issues.stream().filter(i -> "ERROR".equals(i.getSeverity())).count();
        int warningCount = (int) issues.stream().filter(i -> "WARNING".equals(i.getSeverity())).count();
        
        ValidationAdvice.Recommendation rec = new ValidationAdvice.Recommendation();
        rec.setCategory(category);
        rec.setPriority(hasErrors ? "HIGH" : "MEDIUM");
        
        // Category-specific recommendations
        switch (category.toUpperCase()) {
            case "COMPLETENESS":
                rec.setAction(String.format("Add missing components (%d issue%s)", 
                    issues.size(), issues.size() > 1 ? "s" : ""));
                rec.setImpact("Ensures complete trip experience");
                rec.setAutoFixable(true);
                break;
                
            case "BUDGET":
                rec.setAction(String.format("Review budget allocation (%d issue%s)", 
                    issues.size(), issues.size() > 1 ? "s" : ""));
                rec.setImpact("Prevents overspending");
                rec.setAutoFixable(false);
                break;
                
            case "TIMING":
                rec.setAction(String.format("Adjust schedule timing (%d issue%s)", 
                    issues.size(), issues.size() > 1 ? "s" : ""));
                rec.setImpact("Improves trip feasibility");
                rec.setAutoFixable(true);
                break;
                
            case "DIETARY":
                rec.setAction(String.format("Verify meal options (%d issue%s)", 
                    issues.size(), issues.size() > 1 ? "s" : ""));
                rec.setImpact("Ensures dietary compliance");
                rec.setAutoFixable(false);
                break;
                
            default:
                rec.setAction(String.format("Review %d %s issue%s", 
                    issues.size(), category, issues.size() > 1 ? "s" : ""));
                rec.setImpact("Improves itinerary quality");
                rec.setAutoFixable(false);
        }
        
        recommendations.add(rec);
        return recommendations;
    }
    
    /**
     * Optional: Enhance recommendations with LLM for more contextual advice
     * Enable via feature flag: validation.advisor.llm-advice=true
     */
    @SuppressWarnings("unused")
    private List<ValidationAdvice.Recommendation> enhanceWithLLM(
            List<ValidationAdvice.Recommendation> recommendations,
            Map<String, List<ValidationIssue>> issuesByCategory) {
        
        // TODO: Implement LLM integration when feature flag is enabled
        // This would use AiClient to generate more contextual recommendations
        // based on the specific issues and itinerary context
        
        logger.debug("LLM enhancement not yet implemented");
        return recommendations;
    }
    
    /**
     * Store validation event in memory for learning
     */
    private void storeValidationEvent(String itineraryId, ValidationAdvice advice) {
        try {
            Memory validationEvent = Memory.builder()
                .itineraryId(itineraryId)
                .category(MemoryCategory.VALIDATION)
                .type(com.tripplanner.enums.MemoryType.PATTERN)
                .data(Map.of(
                    "validatedAt", System.currentTimeMillis(),
                    "level", advice.getLevel().name(),
                    "totalIssues", advice.getSummary().getTotalErrors() + advice.getSummary().getTotalWarnings(),
                    "score", advice.getSummary().getScore()
                ))
                .confidence(0.8)
                .source("VALIDATION_RUN")
                .isPersonal(false)
                .build();
            
            memoryAgent.store(itineraryId, validationEvent);
        } catch (Exception e) {
            logger.error("Failed to store validation event", e);
        }
    }
    
    // ========== Validation Tool Calls ==========
    
    private ValidationResult runSchemaValidation(String itineraryId) {
        try {
            // Call schema validation endpoint
            String url = "http://localhost:8080/api/v1/tools/validate-schema";
            Map<String, Object> request = Map.of("itineraryId", itineraryId);
            
            // For now, return empty result (schema validation is complex)
            return new ValidationResult();
        } catch (Exception e) {
            logger.error("Schema validation failed", e);
            return new ValidationResult();
        }
    }
    
    private ValidationResult runConflictCheck(String itineraryId) {
        try {
            // Call conflict check endpoint
            String url = "http://localhost:8080/api/v1/tools/check-conflicts";
            Map<String, Object> request = Map.of("itineraryId", itineraryId);
            
            // For now, return empty result (will be implemented when endpoint is ready)
            return new ValidationResult();
        } catch (Exception e) {
            logger.error("Conflict check failed", e);
            return new ValidationResult();
        }
    }
    
    private ValidationResult runCompletenessCheck(String itineraryId) {
        try {
            // Call completeness check endpoint
            String url = "http://localhost:8080/api/v1/tools/check-completeness";
            Map<String, Object> request = Map.of("itineraryId", itineraryId);
            
            // For now, return empty result (will be implemented when endpoint is ready)
            return new ValidationResult();
        } catch (Exception e) {
            logger.error("Completeness check failed", e);
            return new ValidationResult();
        }
    }
    
    private ValidationResult runBudgetHealthCheck(String itineraryId) {
        try {
            // Call budget health check endpoint
            String url = "http://localhost:8080/api/v1/tools/check-budget-health";
            Map<String, Object> request = Map.of("itineraryId", itineraryId);
            
            // For now, return empty result (will be implemented when endpoint is ready)
            return new ValidationResult();
        } catch (Exception e) {
            logger.error("Budget health check failed", e);
            return new ValidationResult();
        }
    }
    
    private ValidationResult runTimingFeasibilityCheck(String itineraryId) {
        try {
            // Call timing feasibility check endpoint
            String url = "http://localhost:8080/api/v1/tools/check-timing-feasibility";
            Map<String, Object> request = Map.of("itineraryId", itineraryId);
            
            // For now, return empty result (will be implemented when endpoint is ready)
            return new ValidationResult();
        } catch (Exception e) {
            logger.error("Timing feasibility check failed", e);
            return new ValidationResult();
        }
    }
    
    private ValidationResult runDietaryComplianceCheck(String itineraryId) {
        try {
            // Call dietary compliance check endpoint
            String url = "http://localhost:8080/api/v1/tools/check-dietary-compliance";
            Map<String, Object> request = Map.of("itineraryId", itineraryId);
            
            // For now, return empty result (will be implemented when endpoint is ready)
            return new ValidationResult();
        } catch (Exception e) {
            logger.error("Dietary compliance check failed", e);
            return new ValidationResult();
        }
    }
    
    /**
     * Optional: Check geography compliance (transport modes vs geography)
     * Validates that transport modes make sense for the geography
     * (e.g., no ferries for landlocked countries, no trains for islands)
     */
    @SuppressWarnings("unused")
    private ValidationResult runGeographyCheck(String itineraryId) {
        try {
            // TODO: Implement geography validation
            // - Check if transport modes match geography (islands, landlocked, etc.)
            // - Use GeographyService for validation
            logger.debug("Geography check not yet implemented");
            return new ValidationResult();
        } catch (Exception e) {
            logger.error("Geography check failed", e);
            return new ValidationResult();
        }
    }
    
    /**
     * Optional: Check accessibility compliance
     * Validates wheelchair accessibility and mobility requirements
     */
    @SuppressWarnings("unused")
    private ValidationResult runAccessibilityCheck(String itineraryId) {
        try {
            // TODO: Implement accessibility validation
            // - Check wheelchair accessibility of venues
            // - Validate mobility requirements
            // - Check for accessible transport options
            logger.debug("Accessibility check not yet implemented");
            return new ValidationResult();
        } catch (Exception e) {
            logger.error("Accessibility check failed", e);
            return new ValidationResult();
        }
    }
    
    /**
     * Internal validation result wrapper
     */
    private static class ValidationResult {
        List<ValidationIssue> issues = new ArrayList<>();
    }
}
