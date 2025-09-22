package com.tripplanner.service.agents;

import com.tripplanner.api.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Enrichment Agent - Validates and enriches itineraries with warnings and pacing information.
 * This agent runs after PlannerAgent to add validation, warnings, and pacing calculations.
 */
@Component
public class EnrichmentAgent extends BaseAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentAgent.class);
    
    private final ItineraryJsonService itineraryJsonService;
    private final ChangeEngine changeEngine;
    
    public EnrichmentAgent(AgentEventBus eventBus, ItineraryJsonService itineraryJsonService, ChangeEngine changeEngine) {
        super(eventBus, AgentEvent.AgentKind.planner); // Use planner for now since enrichment doesn't exist
        this.itineraryJsonService = itineraryJsonService;
        this.changeEngine = changeEngine;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("=== ENRICHMENT AGENT PROCESSING ===");
        logger.info("Itinerary ID: {}", itineraryId);
        
        try {
            // Load the current normalized itinerary
            var currentItinerary = itineraryJsonService.getItinerary(itineraryId);
            if (currentItinerary.isEmpty()) {
                throw new RuntimeException("Itinerary not found: " + itineraryId);
            }
            
            emitProgress(itineraryId, 10, "Loading itinerary", "loading");
            
            NormalizedItinerary itinerary = currentItinerary.get();
            
            emitProgress(itineraryId, 30, "Validating opening hours", "validation");
            
            // Validate opening hours and add warnings
            List<ChangeOperation> warningOps = validateOpeningHours(itinerary);
            
            emitProgress(itineraryId, 50, "Calculating pacing", "pacing");
            
            // Calculate pacing and add pacing information
            List<ChangeOperation> pacingOps = calculatePacing(itinerary);
            
            emitProgress(itineraryId, 70, "Computing transit durations", "transit");
            
            // Compute transit durations between nodes
            List<ChangeOperation> transitOps = computeTransitDurations(itinerary);
            
            emitProgress(itineraryId, 90, "Applying enrichments", "applying");
            
            // Combine all enrichment operations
            List<ChangeOperation> allOps = new ArrayList<>();
            allOps.addAll(warningOps);
            allOps.addAll(pacingOps);
            allOps.addAll(transitOps);
            
            // Apply enrichments if there are any
            if (!allOps.isEmpty()) {
                ChangeSet enrichmentChangeSet = new ChangeSet();
                enrichmentChangeSet.setScope("trip");
                enrichmentChangeSet.setOps(allOps);
                
                // Set preferences to respect locks
                ChangePreferences preferences = new ChangePreferences();
                preferences.setRespectLocks(true);
                preferences.setUserFirst(false); // Agent changes take precedence for enrichments
                enrichmentChangeSet.setPreferences(preferences);
                
                // Apply the enrichments
                ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, enrichmentChangeSet);
                
                emitProgress(itineraryId, 100, "Enrichment complete", "complete");
                
                logger.info("=== ENRICHMENT COMPLETE ===");
                logger.info("Applied {} enrichment operations", allOps.size());
                logger.info("New version: {}", result.getToVersion());
                
                return (T) result;
            } else {
                emitProgress(itineraryId, 100, "No enrichments needed", "complete");
                logger.info("=== NO ENRICHMENTS APPLIED ===");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to enrich itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to enrich itinerary: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected String getAgentName() {
        return "Enrichment Agent";
    }
    
    /**
     * Validate opening hours and add warnings for nodes that might be closed.
     */
    private List<ChangeOperation> validateOpeningHours(NormalizedItinerary itinerary) {
        List<ChangeOperation> operations = new ArrayList<>();
        
        if (itinerary.getDays() == null) {
            return operations;
        }
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }
            
            for (NormalizedNode node : day.getNodes()) {
                // Skip locked nodes
                if (Boolean.TRUE.equals(node.getLocked())) {
                    continue;
                }
                
                // Check if node has timing information
                if (node.getTiming() != null && node.getTiming().getStartTime() != null) {
                    String startTime = node.getTiming().getStartTime().toString();
                    
                    // Mock validation - in real implementation, this would check actual opening hours
                    if (isEarlyMorning(startTime) && isRestaurant(node)) {
                        // Add warning for early morning restaurant visit
                        ChangeOperation warningOp = createWarningOperation(node.getId(), 
                            "Restaurant may not be open at this early hour");
                        operations.add(warningOp);
                    } else if (isLateEvening(startTime) && isMuseum(node)) {
                        // Add warning for late evening museum visit
                        ChangeOperation warningOp = createWarningOperation(node.getId(), 
                            "Museum may be closed at this time");
                        operations.add(warningOp);
                    }
                }
            }
        }
        
        return operations;
    }
    
    /**
     * Calculate pacing and add pacing information to nodes.
     */
    private List<ChangeOperation> calculatePacing(NormalizedItinerary itinerary) {
        List<ChangeOperation> operations = new ArrayList<>();
        
        if (itinerary.getDays() == null) {
            return operations;
        }
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null || day.getNodes().size() < 2) {
                continue;
            }
            
            // Calculate pacing between consecutive nodes
            for (int i = 0; i < day.getNodes().size() - 1; i++) {
                NormalizedNode currentNode = day.getNodes().get(i);
                NormalizedNode nextNode = day.getNodes().get(i + 1);
                
                // Skip if either node is locked
                if (Boolean.TRUE.equals(currentNode.getLocked()) || Boolean.TRUE.equals(nextNode.getLocked())) {
                    continue;
                }
                
                // Calculate time between nodes
                if (currentNode.getTiming() != null && nextNode.getTiming() != null) {
                    String currentEndTime = currentNode.getTiming().getEndTime().toString();
                    String nextStartTime = nextNode.getTiming().getStartTime().toString();
                    
                    if (currentEndTime != null && nextStartTime != null) {
                        int timeBetween = calculateTimeBetween(currentEndTime, nextStartTime);
                        
                        // Add pacing information
                        if (timeBetween < 30) {
                            // Very tight schedule
                            ChangeOperation pacingOp = createPacingOperation(nextNode.getId(), 
                                "Very tight schedule - only " + timeBetween + " minutes between activities");
                            operations.add(pacingOp);
                        } else if (timeBetween > 180) {
                            // Very loose schedule
                            ChangeOperation pacingOp = createPacingOperation(nextNode.getId(), 
                                "Long gap - " + timeBetween + " minutes between activities");
                            operations.add(pacingOp);
                        }
                    }
                }
            }
        }
        
        return operations;
    }
    
    /**
     * Compute transit durations between nodes and update edges.
     */
    private List<ChangeOperation> computeTransitDurations(NormalizedItinerary itinerary) {
        List<ChangeOperation> operations = new ArrayList<>();
        
        if (itinerary.getDays() == null) {
            return operations;
        }
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null || day.getNodes().size() < 2) {
                continue;
            }
            
            // Calculate transit durations between consecutive nodes
            for (int i = 0; i < day.getNodes().size() - 1; i++) {
                NormalizedNode currentNode = day.getNodes().get(i);
                NormalizedNode nextNode = day.getNodes().get(i + 1);
                
                // Skip if either node is locked
                if (Boolean.TRUE.equals(currentNode.getLocked()) || Boolean.TRUE.equals(nextNode.getLocked())) {
                    continue;
                }
                
                // Calculate transit duration based on locations
                if (currentNode.getLocation() != null && nextNode.getLocation() != null) {
                    int transitDuration = calculateTransitDuration(
                        currentNode.getLocation(), nextNode.getLocation());
                    
                    // Create or update edge with transit duration
                    ChangeOperation transitOp = createTransitOperation(
                        currentNode.getId(), nextNode.getId(), transitDuration);
                    operations.add(transitOp);
                }
            }
        }
        
        return operations;
    }
    
    /**
     * Create a warning operation for a node.
     */
    private ChangeOperation createWarningOperation(String nodeId, String warning) {
        ChangeOperation op = new ChangeOperation();
        op.setOp("update");
        op.setId(nodeId);
        
        // Create a node with warning information
        NormalizedNode nodeWithWarning = new NormalizedNode();
        nodeWithWarning.setId(nodeId);
        
        // Add warning to tips
        NodeTips tips = new NodeTips();
        tips.setWarnings(List.of(warning));
        nodeWithWarning.setTips(tips);
        
        op.setNode(nodeWithWarning);
        return op;
    }
    
    /**
     * Create a pacing operation for a node.
     */
    private ChangeOperation createPacingOperation(String nodeId, String pacingInfo) {
        ChangeOperation op = new ChangeOperation();
        op.setOp("update");
        op.setId(nodeId);
        
        // Create a node with pacing information
        NormalizedNode nodeWithPacing = new NormalizedNode();
        nodeWithPacing.setId(nodeId);
        
        // Add pacing to tips
        NodeTips tips = new NodeTips();
        tips.setTravel(List.of(pacingInfo));
        nodeWithPacing.setTips(tips);
        
        op.setNode(nodeWithPacing);
        return op;
    }
    
    /**
     * Create a transit operation for updating edge duration.
     */
    private ChangeOperation createTransitOperation(String fromNodeId, String toNodeId, int duration) {
        ChangeOperation op = new ChangeOperation();
        op.setOp("update_edge");
        op.setId(fromNodeId + "_to_" + toNodeId);
        
        // Create edge with transit duration
        Edge edge = new Edge(fromNodeId, toNodeId);
        
        // Note: This is a simplified approach. In a real implementation,
        // you might need a different operation type for updating edges.
        // For now, we'll just create a basic edge without duration/mode.
        
        return op;
    }
    
    // Helper methods for validation
    
    private boolean isEarlyMorning(String time) {
        try {
            LocalTime localTime = LocalTime.parse(time.substring(11, 19)); // Extract time part
            return localTime.isBefore(LocalTime.of(9, 0));
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isLateEvening(String time) {
        try {
            LocalTime localTime = LocalTime.parse(time.substring(11, 19)); // Extract time part
            return localTime.isAfter(LocalTime.of(18, 0));
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isRestaurant(NormalizedNode node) {
        return "meal".equals(node.getType()) || 
               (node.getTitle() != null && node.getTitle().toLowerCase().contains("restaurant"));
    }
    
    private boolean isMuseum(NormalizedNode node) {
        return "activity".equals(node.getType()) && 
               (node.getTitle() != null && node.getTitle().toLowerCase().contains("museum"));
    }
    
    private int calculateTimeBetween(String endTime, String startTime) {
        try {
            LocalTime end = LocalTime.parse(endTime.substring(11, 19));
            LocalTime start = LocalTime.parse(startTime.substring(11, 19));
            
            int endMinutes = end.getHour() * 60 + end.getMinute();
            int startMinutes = start.getHour() * 60 + start.getMinute();
            
            return startMinutes - endMinutes;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int calculateTransitDuration(NodeLocation from, NodeLocation to) {
        // Mock calculation - in real implementation, this would use a mapping service
        if (from.getCoordinates() != null && to.getCoordinates() != null) {
            // Simple distance-based calculation (very rough)
            double latDiff = Math.abs(from.getCoordinates().getLat() - to.getCoordinates().getLat());
            double lngDiff = Math.abs(from.getCoordinates().getLng() - to.getCoordinates().getLng());
            double distance = Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
            
            // Rough conversion: 1 degree ≈ 111 km, walking speed ≈ 5 km/h
            return (int) (distance * 111 * 12); // 12 minutes per km
        }
        
        return 15; // Default 15 minutes
    }
    
    /**
     * Update node status with audit trail.
     */
    private void updateNodeStatus(NormalizedNode node, String status) {
        if (node != null && node.canTransitionTo(status)) {
            node.setStatus(status);
            setNodeAuditFields(node, "agent");
        } else if (node != null) {
            logger.warn("Invalid status transition from {} to {} for node {}", 
                       node.getStatus(), status, node.getId());
        }
    }
    
    /**
     * Set audit trail fields for a node.
     */
    private void setNodeAuditFields(NormalizedNode node, String updatedBy) {
        if (node != null) {
            node.markAsUpdated(updatedBy);
        }
    }
    
    /**
     * Validate node status.
     */
    private void validateNodeStatus(NormalizedNode node) {
        if (node != null && node.getStatus() != null) {
            // Validate that the status is one of the allowed values
            List<String> validStatuses = List.of("planned", "in_progress", "skipped", "cancelled", "completed");
            if (!validStatuses.contains(node.getStatus())) {
                logger.warn("Invalid node status: {} for node {}", node.getStatus(), node.getId());
                node.setStatus("planned"); // Reset to default
                setNodeAuditFields(node, "agent");
            }
        }
    }
}
