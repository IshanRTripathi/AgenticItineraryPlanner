package com.tripplanner.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.ai.AiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * CostEstimatorAgent - Adds realistic, location-specific cost estimates to all nodes using LLM.
 * 
 * This agent uses AI to generate accurate cost estimates based on:
 * - Actual destination and local cost of living
 * - Specific activity/venue type
 * - Budget tier preferences
 * - Current market rates
 * 
 * IMPORTANT: ALL costs are PER PERSON. The amountPerPerson field represents the cost
 * for one individual, not the total group cost.
 * 
 * Responsibilities:
 * - Generate realistic cost estimates for all nodes (per person)
 * - Use destination-specific pricing in local currency
 * - Adjust for budget tier (budget, medium, luxury)
 * - Provide accurate estimates based on current market rates
 * 
 * Processing Time: 10-15 seconds (AI-powered for accuracy)
 */
@Component
@ConditionalOnBean(AiClient.class)
public class CostEstimatorAgent extends BaseAgent {
    
    private final ItineraryJsonService itineraryJsonService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    
    public CostEstimatorAgent(AgentEventBus eventBus, 
                             ItineraryJsonService itineraryJsonService,
                             AiClient aiClient,
                             ObjectMapper objectMapper) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.itineraryJsonService = itineraryJsonService;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
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
     * Uses Google Places priceLevel when available, falls back to AI for missing data.
     */
    public void estimateCosts(String itineraryId, NormalizedItinerary itinerary, String budgetTier) {
        logger.info("=== COST ESTIMATOR AGENT ===");
        logger.info("Estimating costs for itinerary: {} (hybrid: Google Places + AI)", itineraryId);
        
        try {
            emitProgress(itineraryId, 10, "Preparing cost estimation", "loading");
            
            String destination = itinerary.getDays().isEmpty() ? "Unknown" : 
                               itinerary.getDays().get(0).getLocation();
            String currency = itinerary.getCurrency() != null ? itinerary.getCurrency() : "INR";
            
            int totalDays = itinerary.getDays().size();
            int processedDays = 0;
            
            emitProgress(itineraryId, 20, 
                String.format("Estimating costs for %d days in %s", totalDays, destination), 
                "estimating");
            
            // Process each day
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() == null || day.getNodes().isEmpty()) continue;
                
                // Separate nodes into those with priceLevel and those without
                List<NormalizedNode> nodesWithPriceLevel = new ArrayList<>();
                List<NormalizedNode> nodesNeedingAI = new ArrayList<>();
                
                for (NormalizedNode node : day.getNodes()) {
                    if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                        // Already has cost, skip
                        continue;
                    }
                    
                    if (node.getLocation() != null && node.getLocation().getPriceLevel() != null) {
                        nodesWithPriceLevel.add(node);
                    } else {
                        nodesNeedingAI.add(node);
                    }
                }
                
                // Estimate costs for nodes with priceLevel (fast, no AI needed)
                for (NormalizedNode node : nodesWithPriceLevel) {
                    estimateCostFromPriceLevel(node, destination, budgetTier, currency);
                }
                
                // Estimate costs for nodes without priceLevel using AI (slower but accurate)
                if (!nodesNeedingAI.isEmpty()) {
                    estimateCostsWithAI(nodesNeedingAI, destination, budgetTier, currency);
                }
                
                // Calculate day total
                double dayCost = day.getNodes().stream()
                    .filter(n -> n.getCost() != null && n.getCost().getAmountPerPerson() != null)
                    .mapToDouble(n -> n.getCost().getAmountPerPerson())
                    .sum();
                day.setTotalCost(dayCost);
                
                // Send progress update
                processedDays++;
                int progress = 20 + (int) ((processedDays / (double) totalDays) * 60);
                emitProgress(itineraryId, progress, 
                    String.format("Estimated costs for day %d/%d", processedDays, totalDays), 
                    "estimating");
            }
            
            emitProgress(itineraryId, 85, "Saving cost data", "saving");
            
            // Calculate total cost
            double totalCost = itinerary.getDays().stream()
                .mapToDouble(NormalizedDay::getTotalCost)
                .sum();
            
            // Save updated itinerary
            itinerary.setUpdatedAt(System.currentTimeMillis());
            itineraryJsonService.updateItinerary(itinerary);
            
            int totalNodes = itinerary.getDays().stream()
                .mapToInt(d -> d.getNodes() != null ? d.getNodes().size() : 0)
                .sum();
            
            emitProgress(itineraryId, 100, 
                String.format("Estimated costs for %d nodes", totalNodes), 
                "complete");
            
            logger.info("=== COST ESTIMATOR COMPLETE ===");
            logger.info("Estimated costs for {} nodes across {} days", totalNodes, totalDays);
            logger.info("Total estimated cost: {} {} per person", currency, String.format("%.0f", totalCost));
            
        } catch (Exception e) {
            logger.error("Failed to estimate costs for itinerary: {}", itineraryId, e);
            emitProgress(itineraryId, 0, "Failed to estimate costs", "error");
            // Don't throw - costs are optional
        }
    }
    
    /**
     * Estimate cost from Google Places priceLevel (0-4 scale).
     * This is fast and doesn't require AI calls.
     */
    private void estimateCostFromPriceLevel(NormalizedNode node, String destination, 
                                           String budgetTier, String currency) {
        Integer priceLevel = node.getLocation().getPriceLevel();
        String nodeType = node.getType() != null ? node.getType().toLowerCase() : "activity";
        
        logger.debug("Estimating cost from priceLevel {} for node: {} (type: {})", 
                    priceLevel, node.getTitle(), nodeType);
        
        // Convert priceLevel (0-4) to actual cost based on destination and node type
        double baseCost = convertPriceLevelToCost(priceLevel, nodeType, destination, currency);
        
        // Apply budget tier adjustment if needed
        double adjustedCost = applyBudgetTierAdjustment(baseCost, budgetTier);
        
        // Set cost
        if (node.getCost() == null) {
            node.setCost(new NodeCost());
        }
        node.getCost().setAmountPerPerson(adjustedCost);
        node.getCost().setCurrency(currency);
        
        logger.debug("Set cost for {} from priceLevel: {} {} per person", 
                    node.getTitle(), adjustedCost, currency);
    }
    
    /**
     * Convert Google Places priceLevel (0-4) to actual cost estimate.
     * Price levels: 0=Free, 1=Inexpensive, 2=Moderate, 3=Expensive, 4=Very Expensive
     */
    private double convertPriceLevelToCost(Integer priceLevel, String nodeType, 
                                          String destination, String currency) {
        if (priceLevel == null) priceLevel = 2; // Default to moderate
        
        // Base multipliers for different node types
        double typeMultiplier = switch (nodeType) {
            case "meal", "restaurant" -> 1.0;
            case "attraction", "activity" -> 0.8;
            case "transport", "transportation" -> 0.5;
            case "accommodation", "hotel" -> 3.0;
            default -> 1.0;
        };
        
        // Price level to cost mapping (in INR, will be adjusted for other currencies)
        double baseCost = switch (priceLevel) {
            case 0 -> 0.0;      // Free
            case 1 -> 300.0;    // Inexpensive
            case 2 -> 800.0;    // Moderate
            case 3 -> 1500.0;   // Expensive
            case 4 -> 3000.0;   // Very Expensive
            default -> 800.0;   // Default to moderate
        };
        
        return baseCost * typeMultiplier;
    }
    
    /**
     * Apply budget tier adjustment to cost.
     */
    private double applyBudgetTierAdjustment(double baseCost, String budgetTier) {
        if (budgetTier == null) return baseCost;
        
        double multiplier = switch (budgetTier.toLowerCase()) {
            case "budget", "low" -> 0.7;
            case "medium" -> 1.0;
            case "luxury", "high" -> 1.5;
            default -> 1.0;
        };
        
        return baseCost * multiplier;
    }
    
    /**
     * Estimate costs for nodes without priceLevel using AI.
     */
    private void estimateCostsWithAI(List<NormalizedNode> nodes, String destination, 
                                    String budgetTier, String currency) {
        if (nodes.isEmpty()) return;
        
        try {
            String systemPrompt = buildCostEstimationSystemPrompt();
            String userPrompt = buildCostEstimationUserPrompt(nodes, destination, budgetTier, currency);
            String schema = buildCostEstimationSchema(nodes.size());
            
            logger.debug("Requesting AI cost estimates for {} nodes", nodes.size());
            
            String response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
            
            // Parse response and update node costs
            parseCostEstimates(response, nodes, currency);
            
        } catch (Exception e) {
            logger.error("Failed to estimate costs with AI: {}", e.getMessage());
            // Set minimal fallback costs
            for (NormalizedNode node : nodes) {
                setMinimalFallbackCost(node, currency);
            }
        }
    }
    

    
    /**
     * Build system prompt for cost estimation.
     */
    private String buildCostEstimationSystemPrompt() {
        return """
            You are a travel cost estimation expert with deep knowledge of pricing across different destinations worldwide.
            
            Your task is to provide REALISTIC, ACCURATE cost estimates based on:
            - Current market rates for the specific destination
            - Local cost of living and currency
            - Budget tier (budget/medium/luxury)
            - Activity/venue type and typical pricing
            
            CRITICAL RULES:
            1. ALL costs must be PER PERSON (not total group cost)
            2. Use the destination's local currency and actual market rates
            3. Consider the budget tier:
               - Budget: Economical options (hostels, street food, free/cheap attractions)
               - Medium: Mid-range options (3-star hotels, casual dining, standard attractions)
               - Luxury: Premium options (4-5 star hotels, fine dining, exclusive experiences)
            4. Be realistic - research actual costs for that destination
            5. Consider seasonal variations and current pricing trends
            6. For transport, consider typical local rates (taxi, metro, etc.)
            7. For meals, consider the type (breakfast/lunch/dinner) and venue category
            8. For attractions, consider entry fees, guided tours, etc.
            
            Provide estimates as whole numbers (no decimals) in the local currency.
            """;
    }
    
    /**
     * Build user prompt for cost estimation.
     */
    private String buildCostEstimationUserPrompt(List<NormalizedNode> nodes, String destination, 
                                                  String budgetTier, String currency) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Estimate costs for the following activities in ").append(destination).append(":\n\n");
        prompt.append("Budget tier: ").append(budgetTier).append("\n");
        prompt.append("Currency: ").append(currency).append("\n\n");
        
        prompt.append("Activities to estimate (these don't have Google Places pricing data):\n");
        for (int i = 0; i < nodes.size(); i++) {
            NormalizedNode node = nodes.get(i);
            prompt.append(i + 1).append(". ");
            prompt.append("Type: ").append(node.getType());
            prompt.append(", Title: ").append(node.getTitle() != null ? node.getTitle() : "Unknown");
            
            if (node.getLocation() != null && node.getLocation().getName() != null) {
                prompt.append(", Location: ").append(node.getLocation().getName());
            }
            
            if (node.getDetails() != null && node.getDetails().getCategory() != null) {
                prompt.append(", Category: ").append(node.getDetails().getCategory());
            }
            
            prompt.append("\n");
        }
        
        prompt.append("\nProvide realistic per-person cost estimates in ").append(currency);
        prompt.append(" for each activity based on current market rates in ").append(destination).append(".");
        
        return prompt.toString();
    }
    
    /**
     * Build JSON schema for cost estimation response.
     */
    private String buildCostEstimationSchema(int nodeCount) {
        return """
            {
              "type": "object",
              "properties": {
                "estimates": {
                  "type": "array",
                  "minItems": %d,
                  "maxItems": %d,
                  "items": {
                    "type": "object",
                    "properties": {
                      "index": { "type": "integer" },
                      "costPerPerson": { "type": "number" },
                      "notes": { "type": "string" }
                    },
                    "required": ["index", "costPerPerson"]
                  }
                }
              },
              "required": ["estimates"]
            }
            """.formatted(nodeCount, nodeCount);
    }
    
    /**
     * Parse cost estimates from AI response and update nodes.
     */
    private void parseCostEstimates(String response, List<NormalizedNode> nodes, String currency) {
        try {
            String cleanedResponse = cleanJsonResponse(response);
            JsonNode root = objectMapper.readTree(cleanedResponse);
            JsonNode estimates = root.get("estimates");
            
            if (estimates == null || !estimates.isArray()) {
                logger.warn("Invalid cost estimation response format");
                for (NormalizedNode node : nodes) {
                    setMinimalFallbackCost(node, currency);
                }
                return;
            }
            
            for (JsonNode estimate : estimates) {
                int index = estimate.get("index").asInt() - 1; // Convert to 0-based
                double costPerPerson = estimate.get("costPerPerson").asDouble();
                
                if (index >= 0 && index < nodes.size()) {
                    NormalizedNode node = nodes.get(index);
                    if (node.getCost() == null) {
                        node.setCost(new NodeCost());
                    }
                    node.getCost().setAmountPerPerson(costPerPerson);
                    node.getCost().setCurrency(currency);
                    
                    logger.debug("Set AI-estimated cost for node {}: {} {} per person", 
                               node.getTitle(), costPerPerson, currency);
                }
            }
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse cost estimates: {}", e.getMessage());
            for (NormalizedNode node : nodes) {
                setMinimalFallbackCost(node, currency);
            }
        }
    }
    
    /**
     * Set minimal fallback cost for a single node if all estimation methods fail.
     */
    private void setMinimalFallbackCost(NormalizedNode node, String currency) {
        if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
            return; // Already has cost
        }
        
        logger.warn("Using minimal fallback cost for node: {}", node.getTitle());
        
        if (node.getCost() == null) {
            node.setCost(new NodeCost());
        }
        
        // Set minimal placeholder based on type
        double fallbackCost = switch (node.getType() != null ? node.getType().toLowerCase() : "activity") {
            case "meal", "restaurant" -> 500.0;
            case "transport", "transportation" -> 200.0;
            case "accommodation", "hotel" -> 2000.0;
            default -> 300.0;
        };
        
        node.getCost().setAmountPerPerson(fallbackCost);
        node.getCost().setCurrency(currency);
    }
    
    /**
     * Clean JSON response by removing markdown formatting.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";
        
        String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1);
        }
        
        return cleaned.trim();
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

