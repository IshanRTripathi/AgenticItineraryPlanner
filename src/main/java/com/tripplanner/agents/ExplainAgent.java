package com.tripplanner.agents;

import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedNode;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.SummarizationService;
import com.tripplanner.service.AgentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ExplainAgent - Handles user questions and provides explanations about the itinerary.
 * Uses LLM to generate natural, context-aware responses based on the itinerary data.
 */
@Component
public class ExplainAgent extends BaseAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ExplainAgent.class);
    
    private final AiClient aiClient;
    private final ItineraryJsonService itineraryJsonService;
    private final SummarizationService summarizationService;
    
    public ExplainAgent(
            AiClient aiClient,
            ItineraryJsonService itineraryJsonService,
            SummarizationService summarizationService,
            com.tripplanner.service.AgentEventBus eventBus) {
        super(eventBus, AgentEvent.AgentKind.EXPLAINER);
        this.aiClient = aiClient;
        this.itineraryJsonService = itineraryJsonService;
        this.summarizationService = summarizationService;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        
        // Chat-enabled task: explain (answers questions about itinerary)
        capabilities.addSupportedTask("explain");
        
        capabilities.setPriority(15); // Medium-high priority
        capabilities.setChatEnabled(true); // Handle chat requests
        
        return capabilities;
    }
    
    @Override
    protected String getAgentName() {
        return "ExplainAgent";
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("=== EXPLAIN AGENT PROCESSING ===");
        
        try {
            // Extract user question from request
            String userQuestion = extractUserQuestion(request);
            logger.info("User question: {}", userQuestion);
            
            // Get itinerary data
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isEmpty()) {
                logger.error("Itinerary not found: {}", itineraryId);
                return createErrorResponse("I couldn't find the itinerary. Please make sure it exists.", request);
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            
            // Generate context-aware explanation using LLM
            String explanation = generateExplanation(itinerary, userQuestion);
            
            logger.info("Generated explanation (length: {})", explanation.length());
            
            // Return explanation as response
            return createExplanationResponse(explanation, request);
            
        } catch (Exception e) {
            logger.error("Error in ExplainAgent", e);
            return createErrorResponse("I encountered an error while processing your question: " + e.getMessage(), request);
        }
    }
    
    /**
     * Extract the user's question from the AgentRequest.
     */
    private String extractUserQuestion(AgentRequest<?> request) {
        // Try to get from data map
        Map<String, Object> data = request.getData(Map.class);
        if (data != null && data.containsKey("text")) {
            return data.get("text").toString();
        }
        
        // Try to get from taskType (fallback)
        String taskType = determineTaskType(request);
        if (taskType != null && !taskType.equals("explain")) {
            return taskType;
        }
        
        return "What can you tell me about my itinerary?";
    }
    
    /**
     * Generate a natural language explanation using LLM.
     */
    private String generateExplanation(NormalizedItinerary itinerary, String question) {
        logger.debug("Generating explanation for question: {}", question);
        
        // Build context from itinerary
        String itineraryContext = buildItineraryContext(itinerary);
        
        // Build system prompt
        String systemPrompt = buildSystemPrompt();
        
        // Build user prompt
        String userPrompt = buildUserPrompt(question, itineraryContext);
        
        // Generate response using LLM
        String response = aiClient.generateContent(userPrompt, systemPrompt);
        
        return response;
    }
    
    /**
     * Build comprehensive context from itinerary data.
     */
    private String buildItineraryContext(NormalizedItinerary itinerary) {
        StringBuilder context = new StringBuilder();
        
        // Trip overview
        context.append("TRIP OVERVIEW:\n");
        context.append("Destination: ").append(itinerary.getSummary()).append("\n");
        context.append("Duration: ").append(itinerary.getDays().size()).append(" days\n\n");
        
        // Day-by-day details
        context.append("DAY-BY-DAY ITINERARY:\n\n");
        for (NormalizedDay day : itinerary.getDays()) {
            context.append("Day ").append(day.getDayNumber());
            if (day.getLocation() != null && !day.getLocation().isEmpty()) {
                context.append(" - ").append(day.getLocation());
            }
            context.append(":\n");
            
            // Nodes for this day
            for (NormalizedNode node : day.getNodes()) {
                context.append("  • ").append(node.getTitle());
                
                if (node.getType() != null) {
                    context.append(" (").append(node.getType()).append(")");
                }
                
                if (node.getTiming() != null && node.getTiming().getStartTime() != null) {
                    context.append(" at ").append(node.getTiming().getStartTime());
                }
                
                if (node.getLocation() != null && node.getLocation().getAddress() != null) {
                    context.append(" - ").append(node.getLocation().getAddress());
                }
                
                if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                    context.append(" ($").append(node.getCost().getAmountPerPerson()).append(")");
                }
                
                context.append("\n");
            }
            
            context.append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Build system prompt for the explanation agent.
     */
    private String buildSystemPrompt() {
        return """
            You are a helpful travel assistant AI that answers questions about the user's itinerary.
            
            Your role is to:
            1. Provide accurate, detailed information based on the itinerary data
            2. Answer questions naturally and conversationally
            3. Highlight key details like timing, locations, costs when relevant
            4. Be concise but informative
            5. If asked about a specific day, focus on that day's activities
            6. If asked about the whole trip, provide a comprehensive overview
            
            IMPORTANT:
            - Use ONLY the information provided in the itinerary context
            - If information is not available, say so politely
            - Format your response in a friendly, readable way
            - Use bullet points or short paragraphs for clarity
            - Include relevant details like times, costs, locations
            """;
    }
    
    /**
     * Build user prompt with question and context.
     */
    private String buildUserPrompt(String question, String itineraryContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("ITINERARY DATA:\n");
        prompt.append(itineraryContext);
        prompt.append("\n\n");
        prompt.append("USER QUESTION:\n");
        prompt.append(question);
        prompt.append("\n\n");
        prompt.append("Please provide a helpful, accurate answer based on the itinerary data above.");
        
        return prompt.toString();
    }
    
    /**
     * Create explanation response.
     */
    @SuppressWarnings("unchecked")
    private <T> T createExplanationResponse(String explanation, AgentRequest<T> request) {
        // For chat requests, return the explanation as a String
        return (T) explanation;
    }
    
    /**
     * Create error response.
     */
    @SuppressWarnings("unchecked")
    private <T> T createErrorResponse(String errorMessage, AgentRequest<T> request) {
        logger.error("Creating error response: {}", errorMessage);
        return (T) errorMessage;
    }
}

