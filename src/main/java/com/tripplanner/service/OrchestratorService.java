package com.tripplanner.service;

import com.tripplanner.agents.EditorAgent;
import com.tripplanner.dto.*;
import com.tripplanner.agents.BaseAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for orchestrating chat requests and routing them to appropriate handlers.
 * Coordinates LLM-based intent classification, agent routing, and execution plan management.
 */
@Service
public class OrchestratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);
    
    private final IntentClassificationService intentClassificationService;
    private final NodeResolutionService nodeResolutionService;
    private final ChangeEngine changeEngine;
    private final ItineraryJsonService itineraryJsonService;
    private final LLMService llmService;
    private final AgentRegistry agentRegistry;
    private final ChatHistoryService chatHistoryService;
    
    public OrchestratorService(IntentClassificationService intentClassificationService,
                              NodeResolutionService nodeResolutionService,
                              ChangeEngine changeEngine,
                              ItineraryJsonService itineraryJsonService,
                              LLMService llmService,
                              AgentRegistry agentRegistry,
                              ChatHistoryService chatHistoryService) {
        this.intentClassificationService = intentClassificationService;
        this.nodeResolutionService = nodeResolutionService;
        this.changeEngine = changeEngine;
        this.itineraryJsonService = itineraryJsonService;
        this.llmService = llmService;
        this.agentRegistry = agentRegistry;
        this.chatHistoryService = chatHistoryService;
    }
    
    /**
     * Main orchestration method for chat requests with LLM-based intent classification and agent routing.
     */
    public ChatResponse route(ChatRequest request) {
        logger.info("Routing chat request: {}", request);
        
        try {
            // Step 1: Get itinerary context + chat history for LLM classification
            String context = buildContextForLLM(request.getItineraryId());
            
            // Step 2: Classify intent using LLM
            IntentResult intent = classifyIntentWithLLM(request.getText(), context);
            logger.debug("LLM classified intent: {}", intent);
            
            // Step 2.5: Handle conversational/greeting intents
            if (isConversationalIntent(intent, request.getText())) {
                return handleConversationalIntent(request, intent);
            }
            
            // Step 3: Handle disambiguation if needed
            if (intent.requiresNodeResolution() && (request.getSelectedNodeId() == null || request.getSelectedNodeId().isEmpty())) {
                return handleNodeDisambiguation(request, intent);
            }
            
            // Step 4: Get suitable agents for the task
            List<BaseAgent> suitableAgents = agentRegistry.getAgentsForTask(intent.getTaskType());
            if (suitableAgents.isEmpty()) {
                logger.error("No agents found for task type: {}", intent.getTaskType());
                return ChatResponse.error(
                    "Unable to process your request",
                    List.of("No suitable agents available for task type: " + intent.getTaskType())
                );
            }
            
            // Step 5: Create execution plan
            AgentExecutionPlan plan = agentRegistry.createExecutionPlan(intent.getTaskType(), 
                getItineraryForContext(request.getItineraryId()));
            if (plan == null || !plan.isValid()) {
                logger.error("Failed to create valid execution plan for task: {}", intent.getTaskType());
                return ChatResponse.error(
                    "Unable to create execution plan",
                    List.of("Failed to create valid execution plan for task: " + intent.getTaskType())
                );
            }
            
            // Step 6: Execute agent plan
            return executeAgentPlan(plan, request, intent);
            
        } catch (Exception e) {
            logger.error("Error routing chat request", e);
            return ChatResponse.error("An error occurred while processing your request", List.of(e.getMessage()));
        }
    }
    
    /**
     * Classify intent using LLM with itinerary context.
     */
    private IntentResult classifyIntentWithLLM(String text, String context) {
        logger.debug("Classifying intent with LLM for text: {}", text);
        
        try {
            // Use LLM service for intent classification
            IntentResult llmResult = llmService.classifyIntent(text, context);
            
            if (llmResult != null && llmResult.getIntent() != null) {
                logger.debug("LLM classified intent as: {}", llmResult.getIntent());
                return llmResult;
            } else {
                logger.warn("LLM failed to classify intent, falling back to rule-based classification");
                return fallbackToRuleBasedClassification(text);
            }
            
        } catch (Exception e) {
            logger.error("Error in LLM intent classification, falling back to rule-based", e);
            return fallbackToRuleBasedClassification(text);
        }
    }
    
    /**
     * Fallback to rule-based intent classification.
     */
    private IntentResult fallbackToRuleBasedClassification(String text) {
        // Use the existing intent classification service as fallback
        return intentClassificationService.classifyIntent(text, null, null);
    }
    
    /**
     * Build comprehensive context for LLM including itinerary + chat history.
     */
    private String buildContextForLLM(String itineraryId) {
        StringBuilder context = new StringBuilder();
        
        // Add itinerary context
        context.append(getItineraryContext(itineraryId));
        
        // Add recent chat history for conversation continuity
        try {
            List<Map<String, Object>> history = chatHistoryService.getChatHistory(itineraryId);
            if (history != null && !history.isEmpty()) {
                context.append("\n\nRecent conversation (last 5 messages):\n");
                
                // Get last 5 messages for context
                int start = Math.max(0, history.size() - 5);
                for (int i = start; i < history.size(); i++) {
                    Map<String, Object> msg = history.get(i);
                    String sender = "user".equals(msg.get("sender")) ? "User" : "Assistant";
                    String message = (String) msg.get("message");
                    context.append(sender).append(": ").append(message).append("\n");
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load chat history for context: {}", e.getMessage());
            // Continue without chat history - not critical
        }
        
        return context.toString();
    }
    
    /**
     * Get itinerary context for LLM classification (legacy method, now used by buildContextForLLM).
     */
    private String getItineraryContext(String itineraryId) {
        try {
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getMasterItinerary(itineraryId);
            if (itineraryOpt.isPresent()) {
                NormalizedItinerary itinerary = itineraryOpt.get();
                
                // Build concise context for LLM
                StringBuilder context = new StringBuilder();
                context.append("Itinerary: ").append(itinerary.getSummary()).append("\n");
                context.append("Days: ").append(itinerary.getDays().size()).append("\n");
                context.append("Currency: ").append(itinerary.getCurrency()).append("\n");
                
                // Add day summaries
                for (NormalizedDay day : itinerary.getDays()) {
                    context.append("Day ").append(day.getDayNumber())
                           .append(" (").append(day.getLocation()).append("): ");
                    context.append(day.getNodes().size()).append(" activities\n");
                }
                
                return context.toString();
            }
        } catch (Exception e) {
            logger.error("Error getting itinerary context for LLM", e);
        }
        
        return "No itinerary context available";
    }
    
    /**
     * Check if intent is conversational/greeting (low confidence or casual text).
     */
    private boolean isConversationalIntent(IntentResult intent, String text) {
        // Check for low confidence
        if (intent.getConfidence() < 0.4) {
            return true;
        }
        
        // Check for common greetings/conversational phrases (case-insensitive)
        String lowerText = text.toLowerCase().trim();
        String[] greetings = {"hi", "hello", "hey", "thanks", "thank you", "bye", "goodbye", 
                             "how are you", "what's up", "ok", "okay", "cool", "nice", "great"};
        
        for (String greeting : greetings) {
            if (lowerText.equals(greeting) || lowerText.startsWith(greeting + " ") || 
                lowerText.endsWith(" " + greeting) || lowerText.contains(" " + greeting + " ")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handle conversational/greeting intents gracefully.
     */
    private ChatResponse handleConversationalIntent(ChatRequest request, IntentResult intent) {
        logger.debug("Handling conversational intent for: {}", request.getText());
        
        String lowerText = request.getText().toLowerCase().trim();
        String responseMessage;
        
        // Generate contextual responses
        if (lowerText.matches("^(hi|hello|hey).*")) {
            responseMessage = "Hello! I'm your AI travel assistant. I can help you modify your itinerary, " +
                            "answer questions about your trip, book activities, and more. What would you like to do?";
        } else if (lowerText.matches(".*(thank|thanks).*")) {
            responseMessage = "You're welcome! Let me know if you need anything else with your itinerary.";
        } else if (lowerText.matches(".*(bye|goodbye).*")) {
            responseMessage = "Goodbye! Feel free to return anytime you need help with your trip.";
        } else if (lowerText.matches("^(ok|okay|cool|nice|great)$")) {
            responseMessage = "Is there anything else I can help you with for your trip?";
        } else {
            // Generic helpful response for unclear requests
            responseMessage = "I'm not sure I understood that correctly. I can help you with:\n\n" +
                            "• Modifying your itinerary (e.g., 'Move lunch to 2pm')\n" +
                            "• Adding or removing activities\n" +
                            "• Booking hotels and activities\n" +
                            "• Getting information about your trip\n" +
                            "• Adding photos and details to places\n\n" +
                            "What would you like to do?";
        }
        
        ChatResponse response = new ChatResponse();
        response.setIntent("CONVERSATIONAL");
        response.setMessage(responseMessage);
        response.setApplied(false);
        
        return response;
    }
    
    /**
     * Get full itinerary for agent execution context.
     */
    private NormalizedItinerary getItineraryForContext(String itineraryId) {
        try {
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getMasterItinerary(itineraryId);
            return itineraryOpt.orElse(null);
        } catch (Exception e) {
            logger.error("Error getting itinerary for agent context", e);
            return null;
        }
    }
    
    /**
     * Execute agent plan with error handling and fallback.
     */
    private ChatResponse executeAgentPlan(AgentExecutionPlan plan, ChatRequest request, IntentResult intent) {
        logger.info("Executing agent plan: {}", plan.getExecutionSummary());
        
        List<String> errors = new ArrayList<>();
        BaseAgent primaryAgent = plan.getPrimaryAgent();
        
        if (primaryAgent == null) {
            return ChatResponse.error("No primary agent available for execution", List.of("Empty execution plan"));
        }
        
        try {
            // Add request parameters to plan
            plan.addParameter("chatRequest", request);
            plan.addParameter("intentResult", intent);
            
            // Execute primary agent
            Object result = executePrimaryAgent(primaryAgent, request, plan);
            
            if (result != null) {
                return generateChatResponse(result, errors);
            } else {
                errors.add("Primary agent returned null result");
            }
            
        } catch (Exception e) {
            logger.error("Primary agent execution failed: {}", primaryAgent.getClass().getSimpleName(), e);
            errors.add("Primary agent failed: " + e.getMessage());
        }
        
        // Try fallback agents if enabled
        if (plan.isFallbackEnabled()) {
            for (BaseAgent fallbackAgent : plan.getFallbackAgents()) {
                try {
                    logger.info("Trying fallback agent: {}", fallbackAgent.getClass().getSimpleName());
                    Object result = executeFallbackAgent(fallbackAgent, request, plan);
                    
                    if (result != null) {
                        return generateChatResponse(result, errors);
                    }
                    
                } catch (Exception e) {
                    logger.error("Fallback agent execution failed: {}", fallbackAgent.getClass().getSimpleName(), e);
                    errors.add("Fallback agent failed: " + e.getMessage());
                }
            }
        }
        
        // All agents failed, return error
        return ChatResponse.error("All agents failed to process the request", errors);
    }
    
    /**
     * Execute primary agent with proper request conversion.
     */
    private Object executePrimaryAgent(BaseAgent agent, ChatRequest request, AgentExecutionPlan plan) {
        // Convert ChatRequest to appropriate AgentRequest based on agent type
        BaseAgent.AgentRequest<?> agentRequest = convertToAgentRequest(request, plan, agent);
        
        // Execute agent
        return agent.execute(request.getItineraryId(), agentRequest);
    }
    
    /**
     * Execute fallback agent with proper request conversion.
     */
    private Object executeFallbackAgent(BaseAgent agent, ChatRequest request, AgentExecutionPlan plan) {
        // Convert ChatRequest to appropriate AgentRequest based on agent type
        BaseAgent.AgentRequest<?> agentRequest = convertToAgentRequest(request, plan, agent);
        
        // Execute agent
        return agent.execute(request.getItineraryId(), agentRequest);
    }
    
    /**
     * Convert ChatRequest to appropriate AgentRequest based on agent type.
     */
    private BaseAgent.AgentRequest<?> convertToAgentRequest(ChatRequest chatRequest, AgentExecutionPlan plan, BaseAgent agent) {
        String agentType = agent.getClass().getSimpleName();
        
        // Create a map that includes the taskType for proper agent validation
        java.util.Map<String, Object> requestData = new java.util.HashMap<>();
        requestData.put("chatRequest", chatRequest);
        requestData.put("taskType", plan.getTaskType()); // Add taskType for validation
        requestData.put("itineraryId", chatRequest.getItineraryId());
        requestData.put("userId", chatRequest.getUserId());
        requestData.put("text", chatRequest.getText());
        requestData.put("scope", chatRequest.getScope());
        requestData.put("day", chatRequest.getDay());
        requestData.put("selectedNodeId", chatRequest.getSelectedNodeId());
        requestData.put("autoApply", chatRequest.isAutoApply());
        
        switch (agentType) {
            case "EditorAgent":
                // Use map with taskType for proper validation
                return new BaseAgent.AgentRequest<>(requestData, java.util.Map.class);
            case "BookingAgent":
                return convertToBookingRequest(chatRequest, plan);
            case "EnrichmentAgent":
                // Use map with taskType for proper validation
                return new BaseAgent.AgentRequest<>(requestData, java.util.Map.class);
            case "ExplainAgent":
                // Explanation agent receives user question and itinerary context
                return new BaseAgent.AgentRequest<>(requestData, java.util.Map.class);
            case "SkeletonPlannerAgent":
            case "DayByDayPlannerAgent":
            case "PlannerAgent":
                // For PLANNER agents, use the map with taskType
                return new BaseAgent.AgentRequest<>(requestData, java.util.Map.class);
            default:
                logger.warn("Unknown agent type for request conversion: {}", agentType);
                // For unknown agents, use the map with taskType
                return new BaseAgent.AgentRequest<>(requestData, java.util.Map.class);
        }
    }
    
    /**
     * Convert ChatRequest to BookingRequest for BookingAgent.
     */
    private BaseAgent.AgentRequest<BookingRequest> convertToBookingRequest(ChatRequest chatRequest, AgentExecutionPlan plan) {
        // Extract booking details from intent and chat request
        IntentResult intent = plan.getParameter("intentResult", IntentResult.class);
        
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setUserId(chatRequest.getUserId());
        bookingRequest.setItineraryId(chatRequest.getItineraryId());
        
        // Determine booking type from intent
        if (intent != null && intent.getEntities() != null) {
            String bookingType = extractBookingType(intent.getEntities());
            bookingRequest.setBookingType(bookingType);
            
            // Extract other booking details from entities
            extractBookingDetails(bookingRequest, intent.getEntities());
        }
        
        return new BaseAgent.AgentRequest<>(bookingRequest, BookingRequest.class);
    }
    
    /**
     * Extract booking type from intent entities.
     */
    private String extractBookingType(java.util.Map<String, Object> entities) {
        if (entities.containsKey("bookingType")) {
            return (String) entities.get("bookingType");
        }
        
        // Infer from other entities
        if (entities.containsKey("hotel") || entities.containsKey("accommodation")) {
            return "hotel";
        } else if (entities.containsKey("flight") || entities.containsKey("airline")) {
            return "flight";
        } else if (entities.containsKey("activity") || entities.containsKey("tour")) {
            return "activity";
        }
        
        return "hotel"; // Default
    }
    
    /**
     * Extract booking details from intent entities.
     */
    private void extractBookingDetails(BookingRequest bookingRequest, java.util.Map<String, Object> entities) {
        // Extract location
        if (entities.containsKey("location")) {
            bookingRequest.setLocation((String) entities.get("location"));
        }
        
        // Extract dates
        if (entities.containsKey("checkInDate")) {
            // Parse date string to LocalDate
            String dateStr = (String) entities.get("checkInDate");
            try {
                bookingRequest.setCheckInDate(java.time.LocalDate.parse(dateStr));
            } catch (Exception e) {
                logger.warn("Failed to parse check-in date: {}", dateStr);
            }
        }
        
        if (entities.containsKey("checkOutDate")) {
            String dateStr = (String) entities.get("checkOutDate");
            try {
                bookingRequest.setCheckOutDate(java.time.LocalDate.parse(dateStr));
            } catch (Exception e) {
                logger.warn("Failed to parse check-out date: {}", dateStr);
            }
        }
        
        // Extract guest/passenger count
        if (entities.containsKey("guests")) {
            try {
                bookingRequest.setGuests(Integer.parseInt(entities.get("guests").toString()));
            } catch (Exception e) {
                logger.warn("Failed to parse guest count: {}", entities.get("guests"));
            }
        }
        
        // Extract budget
        if (entities.containsKey("budget")) {
            try {
                bookingRequest.setBudget(Double.parseDouble(entities.get("budget").toString()));
            } catch (Exception e) {
                logger.warn("Failed to parse budget: {}", entities.get("budget"));
            }
        }
    }
    
    /**
     * Generate ChatResponse from agent execution result.
     */
    private ChatResponse generateChatResponse(Object result, List<String> errors) {
        if (result instanceof BookingResult) {
            BookingResult bookingResult = (BookingResult) result;
            if (bookingResult.isSuccessful()) {
                return ChatResponse.success(
                    "Booking completed successfully",
                    "Your " + bookingResult.getBookingType() + " has been booked. Reference: " + bookingResult.getBookingReference(),
                    null, null, true, null
                );
            } else {
                return ChatResponse.error("Booking failed: " + bookingResult.getErrorMessage(), errors);
            }
        } else if (result instanceof ChangeEngine.ApplyResult) {
            ChangeEngine.ApplyResult applyResult = (ChangeEngine.ApplyResult) result;
            
            // Check if no changes were actually made (no diff means no changes)
            if (applyResult.getDiff() == null) {
                return ChatResponse.success(
                    "No changes made",
                    "Your request was processed, but no changes were made to your itinerary. This may be because the requested item is locked or the modification is not possible.",
                    null, null, false, applyResult.getToVersion()
                );
            } else {
                return ChatResponse.success(
                    "Changes applied successfully",
                    "Your itinerary has been updated.",
                    null, applyResult.getDiff(), true, applyResult.getToVersion()
                );
            }
        } else if (result instanceof String) {
            return ChatResponse.success("Request processed", (String) result, null, null, false, null);
        } else {
            return ChatResponse.success("Request processed successfully", "Operation completed.", null, null, false, null);
        }
    }
    

    
    /**
     * Handle node disambiguation when intent requires node resolution.
     */
    private ChatResponse handleNodeDisambiguation(ChatRequest request, IntentResult intent) {
        logger.debug("Handling node disambiguation for intent: {}", intent.getIntent());
        
        List<NodeCandidate> candidates = nodeResolutionService.findNodeCandidates(
            request.getText(), 
            request.getItineraryId(), 
            request.getDay()
        );
        
        if (candidates.isEmpty()) {
            return ChatResponse.error(
                "No matching places found. Please be more specific or check the spelling.",
                List.of("No nodes found matching: " + request.getText())
            );
        }
        
        if (candidates.size() == 1) {
            // Single candidate found, use it directly
            NodeCandidate candidate = candidates.get(0);
            IntentResult updatedIntent = updateIntentWithNode(intent, candidate.getId());
            return handleChangeIntent(request, updatedIntent);
        }
        
        // Multiple candidates found, return for disambiguation
        return ChatResponse.disambiguation(
            intent.getIntent(),
            "Which place did you mean?",
            candidates
        );
    }
    
    /**
     * Handle change intents by building and applying change sets.
     */
    private ChatResponse handleChangeIntent(ChatRequest request, IntentResult intent) {
        logger.debug("Handling change intent: {}", intent.getIntent());
        
        try {
            // Build change set
            ChangeSet changeSet = buildChangeSet(intent, request.getItineraryId());
            if (changeSet == null) {
                return ChatResponse.error(
                    "Unable to create changes for your request",
                    List.of("Failed to build change set for intent: " + intent.getIntent())
                );
            }
            
            // Preview or apply changes based on autoApply setting
            if (request.isAutoApply()) {
                return applyChanges(request.getItineraryId(), changeSet);
            } else {
                return previewChanges(request.getItineraryId(), changeSet);
            }
            
        } catch (Exception e) {
            logger.error("Error handling change intent", e);
            return ChatResponse.error(
                "An error occurred while processing your request",
                List.of(e.getMessage())
            );
        }
    }
    
    /**
     * Handle explain intent by providing information about the itinerary.
     */
    private ChatResponse handleExplainIntent(ChatRequest request) {
        logger.debug("Handling explain intent");
        
        try {
            var itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ChatResponse.error("Itinerary not found", List.of());
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            String message = generateExplanation(itinerary, request.getText());
            
            return ChatResponse.explain(message);
            
        } catch (Exception e) {
            logger.error("Error handling explain intent", e);
            return ChatResponse.error("Unable to provide explanation", List.of(e.getMessage()));
        }
    }
    
    /**
     * Build a change set from the intent result.
     */
    private ChangeSet buildChangeSet(IntentResult intent, String itineraryId) {
        logger.debug("Building change set for intent: {}", intent);
        
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("trip"); // Default scope
        
        // Set day scope if specified
        if (intent.getDay() != null) {
            changeSet.setScope("day");
            changeSet.setDay(intent.getDay());
        } else if ("REPLACE_NODE".equals(intent.getIntent()) && intent.getNodeIds() != null && !intent.getNodeIds().isEmpty()) {
            // For replace operations, determine the day from the node ID
            Integer day = findDayForNode(itineraryId, intent.getNodeIds().get(0));
            if (day != null) {
                changeSet.setScope("day");
                changeSet.setDay(day);
            }
        }
        
        // Create operations based on intent
        List<ChangeOperation> operations = createOperationsForIntent(intent, itineraryId);
        changeSet.setOps(operations);
        
        // Set preferences
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        preferences.setRespectLocks(true);
        changeSet.setPreferences(preferences);
        
        return changeSet;
    }
    
    /**
     * Find the day number for a given node ID.
     */
    private Integer findDayForNode(String itineraryId, String nodeId) {
        try {
            var itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isEmpty()) {
                logger.warn("Itinerary not found for day lookup: {}", itineraryId);
                return null;
            }
            
            var itinerary = itineraryOpt.get();
            for (var day : itinerary.getDays()) {
                for (var node : day.getNodes()) {
                    if (nodeId.equals(node.getId())) {
                        return day.getDayNumber();
                    }
                }
            }
            
            logger.warn("Node not found for day lookup: {}", nodeId);
            return null;
        } catch (Exception e) {
            logger.error("Error finding day for node: {}", nodeId, e);
            return null;
        }
    }
    
    /**
     * Create operations based on the intent.
     */
    private List<ChangeOperation> createOperationsForIntent(IntentResult intent, String itineraryId) {
        String intentType = intent.getIntent();
        
        switch (intentType) {
            case "MOVE_TIME":
                return createMoveTimeOperations(intent);
            case "INSERT_PLACE":
                return createInsertPlaceOperations(intent);
            case "DELETE_NODE":
                return createDeleteNodeOperations(intent);
            case "REPLACE_NODE":
                return createReplaceNodeOperations(intent);
            case "BOOK_NODE":
                return createBookNodeOperations(intent);
            case "REPLAN_TODAY":
                return createReplanTodayOperations(intent);
            case "UNDO":
                return createUndoOperations(intent);
            default:
                logger.warn("Unknown intent type: {}", intentType);
                return List.of();
        }
    }
    
    /**
     * Create move time operations.
     */
    private List<ChangeOperation> createMoveTimeOperations(IntentResult intent) {
        // This would create operations to move nodes to new times
        // For now, return a placeholder
        return List.of();
    }
    
    /**
     * Create insert place operations.
     */
    private List<ChangeOperation> createInsertPlaceOperations(IntentResult intent) {
        // This would create operations to insert new nodes
        // For now, return a placeholder
        return List.of();
    }
    
    /**
     * Create delete node operations.
     */
    private List<ChangeOperation> createDeleteNodeOperations(IntentResult intent) {
        // This would create operations to delete nodes
        // For now, return a placeholder
        return List.of();
    }
    
    /**
     * Create replace node operations.
     */
    private List<ChangeOperation> createReplaceNodeOperations(IntentResult intent) {
        List<ChangeOperation> operations = new ArrayList<>();
        
        // Get the node ID from the intent (use first node ID if available)
        String nodeId = null;
        if (intent.getNodeIds() != null && !intent.getNodeIds().isEmpty()) {
            nodeId = intent.getNodeIds().get(0);
        }
        
        if (nodeId == null) {
            logger.warn("No node ID provided for replace operation");
            return operations;
        }
        
        // Create a replace operation
        ChangeOperation replaceOp = new ChangeOperation();
        replaceOp.setOp("replace");
        replaceOp.setId(nodeId);
        
        // For now, we'll let the ChangeEngine create a mock replacement node
        // In a real implementation, this would use AI to generate a suitable replacement
        replaceOp.setNode(null); // Let ChangeEngine create mock replacement
        
        operations.add(replaceOp);
        
        logger.debug("Created replace operation for node: {}", nodeId);
        return operations;
    }
    
    /**
     * Create book node operations.
     */
    private List<ChangeOperation> createBookNodeOperations(IntentResult intent) {
        // This would create operations to book nodes
        // For now, return a placeholder
        return List.of();
    }
    
    /**
     * Create replan today operations.
     */
    private List<ChangeOperation> createReplanTodayOperations(IntentResult intent) {
        // This would create operations to replan from today
        // For now, return a placeholder
        return List.of();
    }
    
    /**
     * Create undo operations.
     */
    private List<ChangeOperation> createUndoOperations(IntentResult intent) {
        // Undo operations are handled differently
        return List.of();
    }
    
    /**
     * Preview changes without applying them.
     */
    private ChatResponse previewChanges(String itineraryId, ChangeSet changeSet) {
        try {
            ChangeEngine.ProposeResult result = changeEngine.propose(itineraryId, changeSet);
            
            return ChatResponse.success(
                "Changes previewed successfully",
                "Here's what would change:",
                changeSet,
                result.getDiff(),
                false,
                result.getPreviewVersion()
            );
            
        } catch (Exception e) {
            logger.error("Error previewing changes", e);
            return ChatResponse.error("Unable to preview changes", List.of(e.getMessage()));
        }
    }
    
    /**
     * Apply changes to the itinerary.
     */
    private ChatResponse applyChanges(String itineraryId, ChangeSet changeSet) {
        try {
            ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);
            
            return ChatResponse.success(
                "Changes applied successfully",
                "Your itinerary has been updated.",
                changeSet,
                result.getDiff(),
                true,
                result.getToVersion()
            );
            
        } catch (Exception e) {
            logger.error("Error applying changes", e);
            return ChatResponse.error("Unable to apply changes", List.of(e.getMessage()));
        }
    }
    
    /**
     * Update intent with resolved node ID.
     */
    private IntentResult updateIntentWithNode(IntentResult intent, String nodeId) {
        List<String> nodeIds = List.of(nodeId);
        return new IntentResult(intent.getIntent(), intent.getDay(), nodeIds, 
                               intent.getEntities(), intent.getConstraints());
    }
    
    /**
     * Generate explanation for the itinerary.
     */
    private String generateExplanation(NormalizedItinerary itinerary, String question) {
        // Simple explanation generation
        StringBuilder explanation = new StringBuilder();
        explanation.append("Your ").append(itinerary.getDays().size()).append("-day itinerary for ");
        explanation.append(itinerary.getSummary()).append(" includes:\n\n");
        
        for (NormalizedDay day : itinerary.getDays()) {
            explanation.append("Day ").append(day.getDayNumber()).append(" (").append(day.getLocation()).append("):\n");
            for (NormalizedNode node : day.getNodes()) {
                explanation.append("• ").append(node.getTitle());
                if (node.getType() != null) {
                    explanation.append(" (").append(node.getType()).append(")");
                }
                explanation.append("\n");
            }
            explanation.append("\n");
        }
        
        return explanation.toString();
    }
    
    /**
     * Enhanced execution plan management with retry logic and timeout handling.
     */
    private ChatResponse executeAgentPlanWithRetries(AgentExecutionPlan plan, ChatRequest request, IntentResult intent) {
        logger.info("Executing agent plan with retries: {}", plan.getExecutionSummary());
        
        List<String> allErrors = new ArrayList<>();
        int maxRetries = plan.getMaxRetries();
        long timeoutMs = plan.getTimeoutMs();
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            logger.debug("Execution attempt {} of {}", attempt, maxRetries);
            
            try {
                // Execute with timeout
                ChatResponse response = executeWithTimeout(plan, request, intent, timeoutMs);
                
                if (response != null && !response.isError()) {
                    logger.info("Agent plan executed successfully on attempt {}", attempt);
                    return response;
                } else if (response != null) {
                    allErrors.addAll(response.getErrors());
                    logger.warn("Agent plan failed on attempt {}: {}", attempt, response.getMessage());
                } else {
                    allErrors.add("Attempt " + attempt + ": Execution returned null");
                    logger.warn("Agent plan returned null on attempt {}", attempt);
                }
                
            } catch (Exception e) {
                String errorMsg = "Attempt " + attempt + ": " + e.getMessage();
                allErrors.add(errorMsg);
                logger.error("Agent plan execution failed on attempt {}", attempt, e);
            }
            
            // Wait before retry (exponential backoff)
            if (attempt < maxRetries) {
                try {
                    long waitTime = Math.min(1000 * (long) Math.pow(2, attempt - 1), 10000); // Max 10 seconds
                    logger.debug("Waiting {}ms before retry", waitTime);
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warn("Retry wait interrupted");
                    break;
                }
            }
        }
        
        // All retries exhausted
        logger.error("Agent plan execution failed after {} attempts", maxRetries);
        return ChatResponse.error(
            "Request processing failed after " + maxRetries + " attempts",
            allErrors
        );
    }
    
    /**
     * Execute agent plan with timeout.
     */
    private ChatResponse executeWithTimeout(AgentExecutionPlan plan, ChatRequest request, IntentResult intent, long timeoutMs) {
        // For now, execute synchronously (in a real implementation, this would use CompletableFuture with timeout)
        return executeAgentPlan(plan, request, intent);
    }
    
    /**
     * Validate execution plan before execution.
     */
    private boolean validateExecutionPlan(AgentExecutionPlan plan, ChatRequest request) {
        if (plan == null) {
            logger.error("Execution plan is null");
            return false;
        }
        
        if (!plan.isValid()) {
            logger.error("Execution plan is invalid: {}", plan);
            return false;
        }
        
        if (plan.getPrimaryAgent() == null) {
            logger.error("No primary agent in execution plan");
            return false;
        }
        
        if (request.getItineraryId() == null || request.getItineraryId().trim().isEmpty()) {
            logger.error("Itinerary ID is required for agent execution");
            return false;
        }
        
        return true;
    }
    
    /**
     * Handle agent execution exceptions with appropriate error responses.
     */
    private ChatResponse handleAgentExecutionException(Exception e, BaseAgent agent, ChatRequest request) {
        String agentName = agent != null ? agent.getClass().getSimpleName() : "Unknown";
        
        if (e instanceof IllegalArgumentException) {
            logger.warn("Invalid request for agent {}: {}", agentName, e.getMessage());
            return ChatResponse.error(
                "Invalid request parameters",
                List.of("Agent " + agentName + ": " + e.getMessage())
            );
        } else if (e instanceof SecurityException) {
            logger.error("Security error in agent {}: {}", agentName, e.getMessage());
            return ChatResponse.error(
                "Access denied",
                List.of("Security error in " + agentName)
            );
        } else if (e instanceof RuntimeException && e.getMessage().contains("timeout")) {
            logger.error("Timeout in agent {}: {}", agentName, e.getMessage());
            return ChatResponse.error(
                "Request timed out",
                List.of("Agent " + agentName + " timed out")
            );
        } else {
            logger.error("Unexpected error in agent {}: {}", agentName, e.getMessage(), e);
            return ChatResponse.error(
                "An unexpected error occurred",
                List.of("Agent " + agentName + ": " + e.getMessage())
            );
        }
    }
    
    /**
     * Implement graceful degradation when agents fail.
     */
    private ChatResponse gracefulDegradation(ChatRequest request, IntentResult intent, List<String> errors) {
        logger.info("Implementing graceful degradation for failed agent execution");
        
        // Try to provide a helpful response even when agents fail
        try {
            // For booking intents, provide booking information
            if (intent.getTaskType() != null && intent.getTaskType().contains("booking")) {
                return ChatResponse.success(
                    "Booking information",
                    "I couldn't complete the booking automatically, but I can help you with booking information. " +
                    "Please check the booking details and try again, or contact support for assistance.",
                    null, null, false, null
                );
            }
            
            // For editing intents, use proper agent coordination
            if (intent.isChangeIntent()) {
                logger.error("Change intent should be handled by appropriate agent, not legacy handling");
                return ChatResponse.error(
                    "Unable to process change request",
                    List.of("Change requests must be handled through proper agent coordination")
                );
            }
            
            // For explanation intents, provide basic information
            if ("EXPLAIN".equals(intent.getIntent())) {
                return handleExplainIntent(request);
            }
            
            // Generic fallback
            return ChatResponse.success(
                "Request received",
                "I understand your request but couldn't process it automatically. " +
                "Please try rephrasing your request or contact support for assistance.",
                null, null, false, null
            );
            
        } catch (Exception e) {
            logger.error("Graceful degradation also failed", e);
            return ChatResponse.error(
                "Unable to process request",
                List.of("All processing methods failed", e.getMessage())
            );
        }
    }
    
    /**
     * Log execution metrics for monitoring and debugging.
     */
    private void logExecutionMetrics(AgentExecutionPlan plan, long startTime, boolean success, List<String> errors) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        logger.info("Agent execution metrics - Task: {}, Time: {}ms, Success: {}, Agents: {}, Errors: {}",
                   plan.getTaskType(), executionTime, success, 
                   plan.getAgents().size(), errors.size());
        
        if (!success && !errors.isEmpty()) {
            logger.warn("Agent execution errors: {}", String.join("; ", errors));
        }
        
        // Log agent-specific metrics
        for (BaseAgent agent : plan.getAgents()) {
            logger.debug("Agent used: {} ({})", 
                        agent.getClass().getSimpleName(), 
                        agent.getClass().getPackage().getName());
        }
    }
}
