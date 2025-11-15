package com.tripplanner.agents;

import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ItineraryJsonService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * CostEstimatorAgent - Adds realistic cost estimates to all nodes.
 * 
 * This agent uses a combination of lookup tables and heuristics to estimate costs
 * based on node type, destination, and budget tier.
 * 
 * Responsibilities:
 * - Add cost estimates to all nodes
 * - Adjust for budget tier (budget, medium, luxury)
 * - Use reasonable defaults for different node types
 * - Ensure currency consistency
 * 
 * Processing Time: 3-5 seconds (rule-based, no AI needed)
 * todo: Future improvement - integrate with places/maps APIs for real-time data
 */
@Component
public class CostEstimatorAgent extends BaseAgent {
    
    private final ItineraryJsonService itineraryJsonService;
    
    // Cost estimation tables (INR)
    private static final Map<String, Integer> BASE_COSTS = Map.ofEntries(
        // Attractions
        Map.entry("attraction_museum", 500),
        Map.entry("attraction_landmark", 300),
        Map.entry("attraction_park", 200),
        Map.entry("attraction_temple_shrine", 100),
        Map.entry("attraction_entertainment", 1500),
        Map.entry("attraction_shopping", 0),
        Map.entry("attraction_experience", 2000),
        Map.entry("attraction_nature", 500),
        Map.entry("attraction_default", 400),
        
        // Meals
        Map.entry("meal_breakfast", 300),
        Map.entry("meal_lunch", 600),
        Map.entry("meal_dinner", 1000),
        Map.entry("meal_snack", 200),
        Map.entry("meal_default", 600),
        
        // Transport
        Map.entry("transport_walk", 0),
        Map.entry("transport_taxi", 400),
        Map.entry("transport_rideshare", 350),
        Map.entry("transport_bus", 50),
        Map.entry("transport_metro", 80),
        Map.entry("transport_train", 500),
        Map.entry("transport_tram", 60),
        Map.entry("transport_ferry", 300),
        Map.entry("transport_flight", 5000),
        Map.entry("transport_car_rental", 2000),
        Map.entry("transport_default", 300),
        
        // Accommodation
        Map.entry("accommodation_default", 3000)
    );
    
    // Budget tier multipliers
    private static final Map<String, Double> BUDGET_MULTIPLIERS = Map.of(
        "budget", 0.6,
        "medium", 1.0,
        "luxury", 2.0
    );
    
    public CostEstimatorAgent(AgentEventBus eventBus, ItineraryJsonService itineraryJsonService) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.itineraryJsonService = itineraryJsonService;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        
        // Pipeline-only task: estimate costs
        capabilities.addSupportedTask("estimate_costs");
        
        capabilities.setPriority(50);
        capabilities.setChatEnabled(false); // Pipeline-only, not for chat
        capabilities.setConfigurationValue("fastExecution", true);
        
        return capabilities;
    }
    
    /**
     * Estimate costs for all nodes in the itinerary.
     */
    public void estimateCosts(String itineraryId, NormalizedItinerary itinerary, String budgetTier) {
        logger.info("=== COST ESTIMATOR AGENT ===");
        logger.info("Estimating costs for itinerary: {}", itineraryId);
        
        try {
            emitProgress(itineraryId, 10, "Loading cost data", "loading");
            
            double budgetMultiplier = BUDGET_MULTIPLIERS.getOrDefault(
                budgetTier != null ? budgetTier.toLowerCase() : "medium", 1.0);
            
            int totalNodes = 0;
            double totalCost = 0;
            int totalDays = itinerary.getDays().size();
            int processedDays = 0;
            
            emitProgress(itineraryId, 30, 
                String.format("Estimating costs for %d days", totalDays), 
                "estimating");
            
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() == null) continue;
                
                double dayCost = 0.0;
                
                for (NormalizedNode node : day.getNodes()) {
                    if (node.getCost() == null || node.getCost().getAmountPerPerson() == null) {
                        int estimatedCost = estimateNodeCost(node, budgetMultiplier);
                        
                        if (node.getCost() == null) {
                            node.setCost(new NodeCost());
                        }
                        node.getCost().setAmountPerPerson((double) estimatedCost);
                        node.getCost().setCurrency("INR");
                        
                        dayCost += estimatedCost;
                        totalCost += estimatedCost;
                        totalNodes++;
                    } else {
                        // Node already has cost, add to day total
                        dayCost += node.getCost().getAmountPerPerson();
                    }
                }
                
                // Set day total cost
                day.setTotalCost(dayCost);
                
                // Send progress update
                processedDays++;
                int progress = 30 + (int) ((processedDays / (double) totalDays) * 40);
                emitProgress(itineraryId, progress, 
                    String.format("Estimated costs for day %d/%d", processedDays, totalDays), 
                    "estimating");
            }
            
            emitProgress(itineraryId, 70, "Saving cost data", "saving");
            
            // Save updated itinerary
            itinerary.setUpdatedAt(System.currentTimeMillis());
            itineraryJsonService.updateItinerary(itinerary);
            
            emitProgress(itineraryId, 100, 
                String.format("Estimated costs for %d nodes", totalNodes), 
                "complete");
            
            logger.info("=== COST ESTIMATOR COMPLETE ===");
            logger.info("Estimated costs for {} nodes", totalNodes);
            logger.info("Total estimated cost: ₹{} per person", String.format("%.0f", totalCost));
            
        } catch (Exception e) {
            logger.error("Failed to estimate costs for itinerary: {}", itineraryId, e);
            emitProgress(itineraryId, 0, "Failed to estimate costs", "error");
            // Don't throw - costs are optional
        }
    }
    
    /**
     * Estimate cost for a single node.
     */
    private int estimateNodeCost(NormalizedNode node, double budgetMultiplier) {
        String nodeType = node.getType();
        String category = node.getDetails() != null ? node.getDetails().getCategory() : null;
        
        // Build lookup key
        String lookupKey = nodeType + "_" + (category != null ? category : "default");
        String fallbackKey = nodeType + "_default";
        
        // Get base cost
        Integer baseCost = BASE_COSTS.getOrDefault(lookupKey, 
                          BASE_COSTS.getOrDefault(fallbackKey, 500));
        
        // Apply budget multiplier and add some randomness for variety
        double randomFactor = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
        int estimatedCost = (int) (baseCost * budgetMultiplier * randomFactor);
        
        // Round to nearest 50
        return Math.max(0, (estimatedCost / 50) * 50);
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Not used - estimateCosts is called directly
        return null;
    }
    
    @Override
    protected String getAgentName() {
        return "Cost Estimator Agent";
    }
}

