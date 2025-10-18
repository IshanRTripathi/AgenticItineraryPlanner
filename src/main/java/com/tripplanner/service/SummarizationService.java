package com.tripplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SummarizationService for token optimization and context management.
 * Provides intelligent summarization of itineraries for LLM consumption.
 */
@Service
public class SummarizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SummarizationService.class);
    
    // Token estimation: approximately 1 token = 4 characters
    private static final int CHARS_PER_TOKEN = 4;
    
    private final ObjectMapper objectMapper;
    
    public SummarizationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Summarize an itinerary with token limit optimization.
     * Distributes token budget across days proportionally.
     */
    public String summarizeItinerary(NormalizedItinerary itinerary, int maxTokens) {
        if (itinerary == null) {
            return "No itinerary data available.";
        }
        
        logger.debug("Summarizing itinerary {} with max tokens: {}", itinerary.getItineraryId(), maxTokens);
        
        StringBuilder summary = new StringBuilder();
        
        // Reserve tokens for basic info (approximately 20% of budget)
        int basicInfoTokens = Math.max(50, maxTokens / 5);
        int remainingTokens = maxTokens - basicInfoTokens;
        
        // Add basic itinerary information
        summary.append("ITINERARY SUMMARY\\n");
        summary.append("ID: ").append(itinerary.getItineraryId()).append("\\n");
        summary.append("Destination: ").append(itinerary.getDestination()).append("\\n");
        summary.append("Duration: ").append(itinerary.getStartDate()).append(" to ").append(itinerary.getEndDate()).append("\\n");
        summary.append("Days: ").append(itinerary.getDays() != null ? itinerary.getDays().size() : 0).append("\\n");
        summary.append("Currency: ").append(itinerary.getCurrency()).append("\\n");
        
        if (itinerary.getThemes() != null && !itinerary.getThemes().isEmpty()) {
            summary.append("Themes: ").append(String.join(", ", itinerary.getThemes())).append("\\n");
        }
        
        if (itinerary.getSummary() != null && !itinerary.getSummary().trim().isEmpty()) {
            String truncatedSummary = truncateToTokenLimit(itinerary.getSummary(), basicInfoTokens / 2);
            summary.append("Description: ").append(truncatedSummary).append("\\n");
        }
        
        summary.append("\\n");
        
        // Summarize days if available
        if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
            int tokensPerDay = remainingTokens / itinerary.getDays().size();
            
            summary.append("DAILY BREAKDOWN:\\n");
            for (NormalizedDay day : itinerary.getDays()) {
                String daySummary = summarizeDay(day, tokensPerDay);
                summary.append(daySummary).append("\\n");
            }
        }
        
        // Ensure we don't exceed token limit
        String result = truncateToTokenLimit(summary.toString(), maxTokens);
        
        logger.debug("Generated summary with approximately {} tokens", estimateTokens(result));
        return result;
    }
    
    /**
     * Summarize a single day with token limit.
     * Includes day info, location, and node summaries.
     */
    public String summarizeDay(NormalizedDay day, int maxTokens) {
        if (day == null) {
            return "Day information not available.";
        }
        
        StringBuilder daySummary = new StringBuilder();
        
        // Reserve tokens for day header (approximately 15% of budget)
        int headerTokens = Math.max(20, maxTokens / 7);
        int remainingTokens = maxTokens - headerTokens;
        
        // Add day header
        daySummary.append("Day ").append(day.getDayNumber());
        if (day.getDate() != null) {
            daySummary.append(" (").append(day.getDate()).append(")");
        }
        if (day.getLocation() != null) {
            daySummary.append(" - ").append(day.getLocation());
        }
        daySummary.append(":\\n");
        
        // Add pacing information if available
        if (day.getPace() != null) {
            daySummary.append("Pacing: ").append(day.getPace()).append("\\n");
        }
        
        // Summarize nodes if available
        if (day.getNodes() != null && !day.getNodes().isEmpty()) {
            int tokensPerNode = remainingTokens / day.getNodes().size();
            
            for (int i = 0; i < day.getNodes().size(); i++) {
                NormalizedNode node = day.getNodes().get(i);
                String nodeSummary = summarizeNode(node, tokensPerNode);
                daySummary.append("  ").append(i + 1).append(". ").append(nodeSummary).append("\\n");
            }
        } else {
            daySummary.append("  No activities planned.\\n");
        }
        
        return truncateToTokenLimit(daySummary.toString(), maxTokens);
    }
    
    /**
     * Summarize a single node with token limit.
     * Prioritizes critical information over descriptions.
     */
    public String summarizeNode(NormalizedNode node, int maxTokens) {
        if (node == null) {
            return "Activity information not available.";
        }
        
        StringBuilder nodeSummary = new StringBuilder();
        
        // Add title (required)
        nodeSummary.append(node.getTitle() != null ? node.getTitle() : "Untitled Activity");
        
        // Add type if available
        if (node.getType() != null) {
            nodeSummary.append(" (").append(node.getType()).append(")");
        }
        
        // Add timing information
        if (node.getTiming() != null) {
            if (node.getTiming().getStartTime() != null) {
                nodeSummary.append(" at ").append(node.getTiming().getStartTime());
            }
            if (node.getTiming().getDurationMin() != null) {
                nodeSummary.append(" for ").append(node.getTiming().getDurationMin()).append("min");
            }
        }
        
        // Add cost information
        if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
            nodeSummary.append(" - ").append(node.getCost().getAmountPerPerson());
            if (node.getCost().getCurrency() != null) {
                nodeSummary.append(" ").append(node.getCost().getCurrency());
            }
            nodeSummary.append(" per person");
        }
        
        // Add location if available
        if (node.getLocation() != null && node.getLocation().getName() != null) {
            nodeSummary.append(" @ ").append(node.getLocation().getName());
        }
        
        // Add status if not planned
        if (node.getStatus() != null && !"planned".equals(node.getStatus())) {
            nodeSummary.append(" [").append(node.getStatus().toUpperCase()).append("]");
        }
        
        // Add booking status if available
        if (node.getLabels() != null && node.getLabels().contains("Booking Required")) {
            nodeSummary.append(" [BOOKING REQUIRED]");
        }
        
        // Add locked status
        if (Boolean.TRUE.equals(node.getLocked())) {
            nodeSummary.append(" [LOCKED]");
        }
        
        return truncateToTokenLimit(nodeSummary.toString(), maxTokens);
    }
    
    /**
     * Truncate text to fit within token limit.
     * Preserves sentence boundaries when possible.
     */
    public String truncateToTokenLimit(String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        int maxChars = maxTokens * CHARS_PER_TOKEN;
        
        if (text.length() <= maxChars) {
            return text;
        }
        
        // Try to truncate at sentence boundary
        String truncated = text.substring(0, maxChars);
        int lastSentence = Math.max(
            truncated.lastIndexOf('.'),
            Math.max(truncated.lastIndexOf('!'), truncated.lastIndexOf('?'))
        );
        
        if (lastSentence > maxChars / 2) {
            // Good sentence boundary found
            return truncated.substring(0, lastSentence + 1);
        }
        
        // Try to truncate at word boundary
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > maxChars / 2) {
            return truncated.substring(0, lastSpace) + "...";
        }
        
        // Hard truncate with ellipsis
        return truncated.substring(0, Math.max(0, maxChars - 3)) + "...";
    }
    
    /**
     * Summarize itinerary for specific agent type with optimized formatting.
     * Each agent type focuses on different aspects of the itinerary.
     */
    public String summarizeForAgent(NormalizedItinerary itinerary, String agentType, int maxTokens) {
        if (itinerary == null) {
            return "No itinerary data available.";
        }
        
        logger.debug("Summarizing itinerary {} for agent type: {} with max tokens: {}", 
                    itinerary.getItineraryId(), agentType, maxTokens);
        
        switch (agentType.toLowerCase()) {
            case "editor":
            case "editoragent":
                return summarizeForEditorAgent(itinerary, maxTokens);
            case "ENRICHMENT":
            case "enrichmentagent":
                return summarizeForEnrichmentAgent(itinerary, maxTokens);
            case "booking":
            case "bookingagent":
                return summarizeForBookingAgent(itinerary, maxTokens);
            default:
                logger.warn("Unknown agent type: {}, using default summarization", agentType);
                return summarizeItinerary(itinerary, maxTokens);
        }
    }
    
    /**
     * Summarize for EditorAgent - focus on structure and changeable elements.
     */
    private String summarizeForEditorAgent(NormalizedItinerary itinerary, int maxTokens) {
        StringBuilder summary = new StringBuilder();
        
        // Reserve tokens for header and structure info
        int headerTokens = maxTokens / 4;
        int remainingTokens = maxTokens - headerTokens;
        
        // Add structural information
        summary.append("ITINERARY STRUCTURE FOR EDITING\\n");
        summary.append("ID: ").append(itinerary.getItineraryId()).append("\\n");
        summary.append("Destination: ").append(itinerary.getDestination()).append("\\n");
        summary.append("Days: ").append(itinerary.getDays() != null ? itinerary.getDays().size() : 0).append("\\n");
        summary.append("Version: ").append(itinerary.getVersion()).append("\\n\\n");
        
        // Focus on changeable elements
        if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
            int tokensPerDay = remainingTokens / itinerary.getDays().size();
            
            summary.append("EDITABLE ELEMENTS:\\n");
            for (NormalizedDay day : itinerary.getDays()) {
                summary.append(summarizeDayForEditor(day, tokensPerDay)).append("\\n");
            }
        }
        
        return truncateToTokenLimit(summary.toString(), maxTokens);
    }
    
    /**
     * Summarize for EnrichmentAgent - focus on locations and ENRICHMENT opportunities.
     */
    private String summarizeForEnrichmentAgent(NormalizedItinerary itinerary, int maxTokens) {
        StringBuilder summary = new StringBuilder();
        
        // Reserve tokens for header
        int headerTokens = maxTokens / 6;
        int remainingTokens = maxTokens - headerTokens;
        
        summary.append("LOCATIONS FOR ENRICHMENT\\n");
        summary.append("Destination: ").append(itinerary.getDestination()).append("\\n\\n");
        
        // Focus on locations that need ENRICHMENT
        if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
            summary.append("ENRICHMENT OPPORTUNITIES:\\n");
            
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() != null) {
                    for (NormalizedNode node : day.getNodes()) {
                        if (needsEnrichment(node)) {
                            String nodeInfo = summarizeNodeForEnrichment(node, remainingTokens / 10);
                            summary.append("- ").append(nodeInfo).append("\\n");
                        }
                    }
                }
            }
        }
        
        return truncateToTokenLimit(summary.toString(), maxTokens);
    }
    
    /**
     * Summarize for BookingAgent - focus on bookable items and costs.
     */
    private String summarizeForBookingAgent(NormalizedItinerary itinerary, int maxTokens) {
        StringBuilder summary = new StringBuilder();
        
        // Reserve tokens for header
        int headerTokens = maxTokens / 6;
        int remainingTokens = maxTokens - headerTokens;
        
        summary.append("BOOKING OPPORTUNITIES\\n");
        summary.append("Destination: ").append(itinerary.getDestination()).append("\\n");
        summary.append("Currency: ").append(itinerary.getCurrency()).append("\\n\\n");
        
        // Focus on bookable items
        if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
            summary.append("BOOKABLE ITEMS:\\n");
            
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() != null) {
                    for (NormalizedNode node : day.getNodes()) {
                        if (isBookable(node)) {
                            String nodeInfo = summarizeNodeForBooking(node, remainingTokens / 10);
                            summary.append("- Day ").append(day.getDayNumber()).append(": ").append(nodeInfo).append("\\n");
                        }
                    }
                }
            }
        }
        
        return truncateToTokenLimit(summary.toString(), maxTokens);
    }
    
    /**
     * Summarize day for editor agent - focus on structure and timing.
     * CRITICAL: Includes node IDs so LLM can reference them in changes.
     */
    private String summarizeDayForEditor(NormalizedDay day, int maxTokens) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Day ").append(day.getDayNumber());
        if (day.getDate() != null) {
            summary.append(" (").append(day.getDate()).append(")");
        }
        summary.append(":\\n");
        
        if (day.getNodes() != null && !day.getNodes().isEmpty()) {
            for (NormalizedNode node : day.getNodes()) {
                // CRITICAL: Include node ID first so LLM can reference it
                summary.append("  - [ID: ").append(node.getId()).append("] ");
                summary.append(node.getTitle());
                
                // Show type for better context
                if (node.getType() != null) {
                    summary.append(" (").append(node.getType()).append(")");
                }
                
                // Show timing for editing context
                if (node.getTiming() != null && node.getTiming().getStartTime() != null) {
                    summary.append(" @ ").append(node.getTiming().getStartTime());
                }
                
                // Show location for context
                if (node.getLocation() != null && node.getLocation().getName() != null) {
                    summary.append(" at ").append(node.getLocation().getName());
                }
                
                // Show locked status
                if (Boolean.TRUE.equals(node.getLocked())) {
                    summary.append(" [LOCKED - DO NOT MODIFY]");
                }
                
                summary.append("\\n");
            }
        } else {
            summary.append("  No activities planned.\\n");
        }
        
        return truncateToTokenLimit(summary.toString(), maxTokens);
    }
    
    /**
     * Summarize node for ENRICHMENT agent.
     */
    private String summarizeNodeForEnrichment(NormalizedNode node, int maxTokens) {
        StringBuilder summary = new StringBuilder();
        
        summary.append(node.getTitle());
        
        if (node.getLocation() != null) {
            if (node.getLocation().getName() != null) {
                summary.append(" at ").append(node.getLocation().getName());
            }
            if (node.getLocation().getPlaceId() != null) {
                summary.append(" (PlaceID: ").append(node.getLocation().getPlaceId()).append(")");
            }
        }
        
        // Indicate what ENRICHMENT is needed
        if (node.getDetails() == null || node.getDetails().getRating() == null) {
            summary.append(" [NEEDS RATING]");
        }
        
        return truncateToTokenLimit(summary.toString(), maxTokens);
    }
    
    /**
     * Summarize node for booking agent.
     */
    private String summarizeNodeForBooking(NormalizedNode node, int maxTokens) {
        StringBuilder summary = new StringBuilder();
        
        summary.append(node.getTitle());
        
        if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
            summary.append(" - ").append(node.getCost().getAmountPerPerson());
            if (node.getCost().getCurrency() != null) {
                summary.append(" ").append(node.getCost().getCurrency());
            }
            summary.append(" per person");
        }
        
        if (node.getTiming() != null && node.getTiming().getStartTime() != null) {
            summary.append(" @ ").append(node.getTiming().getStartTime());
        }
        
        // Show booking status
        if (node.getLabels() != null && node.getLabels().contains("Booking Required")) {
            summary.append(" [BOOKING REQUIRED]");
        }
        
        return truncateToTokenLimit(summary.toString(), maxTokens);
    }
    
    /**
     * Check if a node needs ENRICHMENT.
     */
    private boolean needsEnrichment(NormalizedNode node) {
        if (node.getLocation() == null || node.getLocation().getPlaceId() == null) {
            return false; // Can't enrich without place ID
        }
        
        // Check if missing key ENRICHMENT data
        return node.getDetails() == null 
            || node.getDetails().getRating() == null 
            || (node.getDetails().getPhotos() == null || node.getDetails().getPhotos().isEmpty());
    }
    
    /**
     * Check if a node is bookable.
     */
    private boolean isBookable(NormalizedNode node) {
        if (node.getLabels() != null && node.getLabels().contains("Booking Required")) {
            return true;
        }
        
        // Check for bookable types
        String type = node.getType();
        return type != null && (
            type.equals("accommodation") ||
            type.equals("flight") ||
            type.equals("activity") ||
            type.equals("restaurant") ||
            type.equals("attraction")
        );
    }
    
    /**
     * Prioritize critical information based on importance ranking.
     * Returns a list of information strings ordered by priority.
     */
    public List<String> prioritizeCriticalInfo(List<String> info, int maxTokens) {
        if (info == null || info.isEmpty()) {
            return info;
        }
        
        // Define priority order: location > timing > cost > description
        List<String> prioritized = info.stream()
            .sorted((a, b) -> {
                int priorityA = getInfoPriority(a);
                int priorityB = getInfoPriority(b);
                return Integer.compare(priorityA, priorityB);
            })
            .collect(Collectors.toList());
        
        // Include items until token limit is reached
        List<String> result = prioritized.stream()
            .limit(maxTokens / 20) // Rough estimate of items that fit
            .collect(Collectors.toList());
        
        return result;
    }
    
    /**
     * Get priority score for information type (lower = higher priority).
     */
    private int getInfoPriority(String info) {
        String lower = info.toLowerCase();
        
        if (lower.contains("location") || lower.contains("address") || lower.contains("@")) {
            return 1; // Highest priority
        } else if (lower.contains("time") || lower.contains("duration") || lower.contains("@")) {
            return 2;
        } else if (lower.contains("cost") || lower.contains("price") || lower.contains("€") || lower.contains("$")) {
            return 3;
        } else {
            return 4; // Lowest priority (descriptions, etc.)
        }
    }
    
    /**
     * Summarize a list of days for context management.
     * Used to create summaries of previous days for planning context.
     */
    public String summarizeDays(List<NormalizedDay> days) {
        if (days == null || days.isEmpty()) {
            return "No previous days to summarize.";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Previous days summary: ");
        
        for (NormalizedDay day : days) {
            summary.append("Day ").append(day.getDayNumber());
            if (day.getLocation() != null) {
                summary.append(" in ").append(day.getLocation());
            }
            
            if (day.getNodes() != null && !day.getNodes().isEmpty()) {
                summary.append(" (").append(day.getNodes().size()).append(" activities");
                
                // Mention key locations
                Set<String> locations = day.getNodes().stream()
                    .filter(node -> node.getLocation() != null && node.getLocation().getName() != null)
                    .map(node -> node.getLocation().getName())
                    .collect(java.util.stream.Collectors.toSet());
                
                if (!locations.isEmpty()) {
                    summary.append(" at ").append(String.join(", ", locations));
                }
                summary.append(")");
            }
            
            if (day != days.get(days.size() - 1)) {
                summary.append("; ");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Estimate token count for a given text.
     * Uses approximation of 1 token ≈ 4 characters.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (text.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN; // Ceiling division
    }
}