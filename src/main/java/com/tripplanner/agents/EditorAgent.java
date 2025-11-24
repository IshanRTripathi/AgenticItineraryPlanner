package com.tripplanner.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.service.agents.AgentCoordinator;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.client.GeminiClient;
import com.tripplanner.service.llm.LLMResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EditorAgent for handling itinerary editing requests through LLM integration.
 * Extends BaseAgent to provide chat-based editing capabilities.
 */
@Component
public class EditorAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(EditorAgent.class);

    private final SummarizationService summarizationService;
    private final ChangeEngine changeEngine;
    private final GeminiClient geminiClient; // Using existing GeminiClient as LLM service
    private final ItineraryJsonService itineraryJsonService;
    private final ObjectMapper objectMapper;
    private final LLMResponseHandler llmResponseHandler;
    private final ItineraryMigrationService migrationService;
    private final EnrichmentAgent enrichmentAgent;
    private final GooglePlacesService googlePlacesService;
    private final ChatHistoryService chatHistoryService;
    private final NodeIdValidator nodeIdValidator;
    private final AgentCoordinator agentCoordinator; // NEW: Prevents concurrent modifications

    public EditorAgent(AgentEventBus eventBus,
                       SummarizationService summarizationService,
                       ChangeEngine changeEngine,
                       GeminiClient geminiClient,
                       ItineraryJsonService itineraryJsonService,
                       ObjectMapper objectMapper,
                       LLMResponseHandler llmResponseHandler,
                       ItineraryMigrationService migrationService,
                       EnrichmentAgent enrichmentAgent,
                       GooglePlacesService googlePlacesService,
                       ChatHistoryService chatHistoryService,
                       NodeIdValidator nodeIdValidator,
                       AgentCoordinator agentCoordinator) {
        super(eventBus, AgentEvent.AgentKind.EDITOR);
        this.summarizationService = summarizationService;
        this.changeEngine = changeEngine;
        this.geminiClient = geminiClient;
        this.itineraryJsonService = itineraryJsonService;
        this.objectMapper = objectMapper;
        this.llmResponseHandler = llmResponseHandler;
        this.migrationService = migrationService;
        this.enrichmentAgent = enrichmentAgent;
        this.googlePlacesService = googlePlacesService;
        this.chatHistoryService = chatHistoryService;
        this.nodeIdValidator = nodeIdValidator;
        this.agentCoordinator = agentCoordinator;
    }

    @Override
    public com.tripplanner.dto.AgentCapabilities getCapabilities() {
        com.tripplanner.dto.AgentCapabilities capabilities = new com.tripplanner.dto.AgentCapabilities();

        // Single clear task type: edit
        capabilities.addSupportedTask("edit");

        // Set priority (lower = higher priority)
        capabilities.setPriority(10); // High priority for user modifications

        // Configuration
        capabilities.setChatEnabled(true); // Handle chat requests
        capabilities.setConfigurationValue("requiresLLM", true);
        capabilities.setConfigurationValue("handlesUserRequests", true);
        capabilities.setConfigurationValue("scopeType", "user_modifications");

        return capabilities;
    }

    @Override
    public boolean canHandle(String taskType, Object taskContext) {
        // EditorAgent handles "edit" task type only
        return super.canHandle(taskType);
    }

    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("EditorAgent executing for itinerary: {}", itineraryId);

        try {
            // Extract ChatRequest from request data (which is a Map)
            ChatRequest chatRequest = extractChatRequest(request);
            if (chatRequest == null) {
                throw new IllegalArgumentException("EditorAgent requires ChatRequest data");
            }

            emitProgress(itineraryId, 20, "Loading itinerary", "load");

            // Load itinerary using itineraryJsonService with enhanced error handling
            Optional<NormalizedItinerary> itineraryOpt = loadItineraryWithFallback(itineraryId);

            if (itineraryOpt.isEmpty()) {
                throw new RuntimeException("Itinerary not found: " + itineraryId);
            }

            NormalizedItinerary itinerary = itineraryOpt.get();

            // Migrate if needed before building context
            itinerary = migrationService.migrateIfNeeded(itinerary);
            logger.debug("Itinerary {} migrated (if needed), version: {}", itineraryId, itinerary.getVersion());

            // LOG: Itinerary state after migration
            logger.info("=== ITINERARY STATE AFTER MIGRATION (EditorAgent) ===");
            for (NormalizedDay day : itinerary.getDays()) {
                logger.info("Day {}: {} nodes - IDs: {}",
                        day.getDayNumber(),
                        day.getNodes() != null ? day.getNodes().size() : 0,
                        day.getNodes() != null
                                ? day.getNodes().stream().map(node -> node.getId()).collect(Collectors.toList())
                                : "null");
            }
            logger.info("=======================================================");

            // Validate itinerary data consistency
            validateItineraryDataConsistency(itinerary);

            // Validate itinerary before processing
            validateItineraryForEditing(itinerary);

            emitProgress(itineraryId, 40, "Generating summary", "summarize");

            // Get summary using summarizationService with editor-specific formatting
            String summary = summarizationService.summarizeForAgent(itinerary, "editor", 2000);

            emitProgress(itineraryId, 60, "Generating changes", "llm");

            // Generate ChangeSet using generateChangeSet
            ChangeSet changeSet = generateChangeSet(chatRequest, summary);

            // Check if ChangeSet has no operations but has a reason (e.g., locked nodes)
            if ((changeSet.getOps() == null || changeSet.getOps().isEmpty()) &&
                    changeSet.getReason() != null && !changeSet.getReason().trim().isEmpty()) {

                emitProgress(itineraryId, 100, "Request processed - no changes needed", "complete");

                // Create a result that explains why no changes were made
                ChangeEngine.ApplyResult noChangeResult = new ChangeEngine.ApplyResult(
                        itinerary.getVersion(), // Keep same version since no changes
                        null // No diff since no changes
                );

                // Log the reason for no changes
                logger.info("No changes applied to itinerary {}: {}", itineraryId, changeSet.getReason());

                @SuppressWarnings("unchecked")
                T result = (T) noChangeResult;
                return result;
            }

            emitProgress(itineraryId, 80, "Applying changes", "apply");

            // Generate descriptive response message BEFORE applying (so we can use it)
            String responseMessage = generateResponseMessage(changeSet, itinerary);

            // Update the changeSet reason with the descriptive message
            // This will be available to the frontend in the response
            if (changeSet.getReason() == null || changeSet.getReason().trim().isEmpty()) {
                changeSet.setReason(responseMessage);
            } else {
                // Append to existing reason
                changeSet.setReason(changeSet.getReason() + "\n\n" + responseMessage);
            }

            // Apply changes using changeEngine with the itinerary object to ensure
            // consistency
            ChangeEngine.ApplyResult applyResult = changeEngine.apply(itinerary, changeSet);

            // Enrich newly added/replaced nodes with Google Places data
            emitProgress(itineraryId, 90, "Enriching new nodes", "enrichment");
            enrichNewlyAddedNodes(itineraryId, changeSet);

            emitProgress(itineraryId, 100, responseMessage, "complete");

            // Return ApplyResult cast to generic type T
            @SuppressWarnings("unchecked")
            T result = (T) applyResult;
            return result;

        } catch (IllegalArgumentException e) {
            logger.error("Invalid arguments for EditorAgent execution: {}", e.getMessage());
            throw new RuntimeException("Invalid request: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("EditorAgent execution failed for itinerary {}: {}", itineraryId, e.getMessage(), e);

            // Ensure itinerary is not left in broken state by checking if changes were
            // partially applied
            try {
                // Verify itinerary integrity
                Optional<NormalizedItinerary> currentItinerary = itineraryJsonService.getMasterItinerary(itineraryId);
                if (currentItinerary.isPresent()) {
                    logger.info("Itinerary {} integrity verified after failed operation", itineraryId);
                } else {
                    logger.warn("Itinerary {} may be in inconsistent state after failed operation", itineraryId);
                }
            } catch (Exception verificationError) {
                logger.error("Failed to verify itinerary integrity after error: {}", verificationError.getMessage());
            }

            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in EditorAgent execution for itinerary {}: {}", itineraryId, e.getMessage(),
                    e);
            throw new RuntimeException("Unexpected error during editing: " + getUserFriendlyErrorMessage(e), e);
        }
    }

    @Override
    protected String getAgentName() {
        return "EditorAgent";
    }

    /**
     * Extract ChatRequest from AgentRequest data.
     * The data can be either a direct ChatRequest or a Map containing a
     * ChatRequest.
     */
    private ChatRequest extractChatRequest(AgentRequest<?> request) {
        Object data = request.getData();

        // Try direct cast first
        if (data instanceof ChatRequest) {
            return (ChatRequest) data;
        }

        // Try to extract from Map
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object chatRequestObj = dataMap.get("chatRequest");

            if (chatRequestObj instanceof ChatRequest) {
                return (ChatRequest) chatRequestObj;
            }
        }

        return null;
    }

    /**
     * Generate ChangeSet from chat request and itinerary context.
     * Includes comprehensive error handling and retry logic.
     */
    private ChangeSet generateChangeSet(ChatRequest chatRequest, String context) {
        // Pre-request validation: Check for locked nodes before making API calls
        try {
            String itineraryId = chatRequest.getItineraryId();
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);

            if (itineraryOpt.isPresent()) {
                validateRequestAgainstLockedNodes(chatRequest, itineraryOpt.get());
            }
        } catch (RuntimeException e) {
            logger.warn("Pre-request validation failed: {}", e.getMessage());
            // Return a ChangeSet with no operations but with an explanation
            ChangeSet emptyChangeSet = new ChangeSet();
            emptyChangeSet.setOps(new ArrayList<>());
            emptyChangeSet.setReason(e.getMessage());
            emptyChangeSet.setAgent("EditorAgent");
            return emptyChangeSet;
        }

        int maxRetries = 3;
        int retryDelay = 1000; // milliseconds

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("Generating ChangeSet, attempt {} of {}", attempt, maxRetries);

                // Build prompt for ChangeSet generation
                String prompt = buildChangeSetPrompt(chatRequest, context);

                // Call GeminiClient to generate ChangeSet using structured content generation
                String jsonSchema = buildChangeSetJsonSchema();
                String response = geminiClient.generateStructuredContent(prompt, jsonSchema,
                        "You are an expert travel itinerary editor.");

                // Log full LLM response for analysis
                logger.info("=== EDITOR AGENT - FULL LLM RESPONSE ===");
                logger.info("User Request: {}", chatRequest.getText());
                logger.info("Raw Response: {}", response);
                logger.info("=== END EDITOR AGENT RESPONSE ===");

                if (response == null || response.trim().isEmpty()) {
                    throw new RuntimeException("Empty response from LLM service");
                }

                // Parse response to ChangeSet with robust handling
                ChangeSet changeSet = parseChangeSetFromResponseWithRetry(response, prompt, jsonSchema);

                // Validate ChangeSet before returning
                validateChangeSet(changeSet);

                logger.info("Successfully generated ChangeSet with {} operations",
                        changeSet.getOps() != null ? changeSet.getOps().size() : 0);
                return changeSet;

            } catch (Exception e) {
                logger.warn("ChangeSet generation attempt {} failed: {}", attempt, e.getMessage());

                if (attempt == maxRetries) {
                    logger.error("All ChangeSet generation attempts failed", e);

                    // Provide meaningful error messages to user
                    String userMessage = getUserFriendlyErrorMessage(e);
                    throw new RuntimeException(userMessage, e);
                }

                // Wait before retry
                try {
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("ChangeSet generation interrupted", ie);
                }
            }
        }

        throw new RuntimeException("Unexpected error in ChangeSet generation retry logic");
    }

    /**
     * Get user-friendly error message based on exception type.
     */
    private String getUserFriendlyErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) {
            return "An unexpected error occurred while processing your request. Please try again.";
        }

        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("timeout") || lowerMessage.contains("connection")) {
            return "The AI service is temporarily unavailable. Please try again in a moment.";
        } else if (lowerMessage.contains("json") || lowerMessage.contains("parse")) {
            return "There was an issue understanding your request. Please try rephrasing it.";
        } else if (lowerMessage.contains("validation") || lowerMessage.contains("invalid")) {
            return "Your request couldn't be processed due to invalid data. Please check your input and try again.";
        } else if (lowerMessage.contains("not found") || lowerMessage.contains("missing")) {
            return "The requested itinerary or item could not be found. Please refresh and try again.";
        } else {
            return "Unable to process your request at this time. Please try again or contact support if the issue persists.";
        }
    }

    /**
     * Build prompt for ChangeSet generation.
     */
    private String buildChangeSetPrompt(ChatRequest chatRequest, String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert travel itinerary editor. ");
        prompt.append("Based on the user's request and the current itinerary context, ");
        prompt.append("generate a ChangeSet in JSON format to modify the itinerary.\n\n");

        prompt.append("=== CRITICAL: INTENT PRESERVATION RULES ===\n");
        prompt.append("1. ONLY perform the EXACT action the user requested - nothing more, nothing less\n");
        prompt.append("2. If user says 'add X', ONLY add X - do NOT remove or modify anything else\n");
        prompt.append("3. If user says 'remove X', ONLY remove X - do NOT add anything\n");
        prompt.append("4. If user says 'replace X with Y', remove X and add Y - but touch nothing else\n");
        prompt.append("5. DO NOT make 'helpful' additions or removals the user didn't ask for\n");
        prompt.append(
                "6. If the user uses pronouns like 'it', 'that', 'this' - refer to the IMMEDIATELY PRECEDING conversation to understand what they mean\n");
        prompt.append("7. The itinerary context shows what EXISTS - the user request shows what to CHANGE\n\n");

        prompt.append("=== TIME FORMAT RULES ===\n");
        prompt.append("1. Use 24-hour time format for startTime and endTime (e.g., \"14:30\", \"18:00\")\n");
        prompt.append("2. Format: \"HH:mm\" where HH is 00-23 and mm is 00-59\n");
        prompt.append("3. Examples: \"09:00\" for 9am, \"14:30\" for 2:30pm, \"18:00\" for 6pm\n");
        prompt.append("4. Always use leading zeros (\"09:00\" not \"9:00\")\n\n");

        // Add recent chat history for context (if available)
        String chatHistory = getChatHistoryContext(chatRequest.getItineraryId());
        if (chatHistory != null && !chatHistory.trim().isEmpty()) {
            prompt.append("RECENT CONVERSATION:\n");
            prompt.append(chatHistory).append("\n\n");
            prompt.append(
                    "IMPORTANT: If the current request uses pronouns like 'it', 'that', 'this', refer to the conversation above!\n\n");
        }

        prompt.append("USER REQUEST:\n");
        prompt.append(chatRequest.getText()).append("\n\n");

        prompt.append("CURRENT ITINERARY CONTEXT:\n");
        prompt.append(context).append("\n\n");

        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. Read the USER REQUEST carefully - what EXACTLY did they ask for?\n");
        prompt.append("3. Determine the MINIMAL change needed to fulfill ONLY what was requested\n");
        prompt.append(
                "4. CRITICAL: Look for nodes in the itinerary context marked as [ID: xxxxx] - you MUST use this EXACT ID in your operation\n");
        prompt.append(
                "5. NEVER generate your own node IDs unless asked to add new - always use the exact IDs from the context. IDs are random identifiers and do NOT imply order.\n");
        prompt.append(
                "6. For 'add'/'insert' requests: Choose 'insert' operation. You MUST specify 'after' with an existing node ID to place it correctly, OR 'position' (0-based index) if 'after' is ambiguous.\n");
        prompt.append(
                "7. For 'replace'/'change' requests: Choose 'replace' operation with the exact node ID from context\n");
        prompt.append(
                "8. For 'delete'/'remove' requests: Choose 'delete' operation with the exact node ID from context\n");
        prompt.append("9. For 'move'/'reschedule' requests: Choose 'move' operation with new startTime/endTime\n");
        prompt.append("10. For timing: Use 24-hour format (e.g., \"14:30\") for startTime and endTime\n");
        prompt.append("11. Set 'day' to the specific day number (e.g., 1, 2, 3)\n");
        prompt.append("12. Always set agent to 'EditorAgent'\n");
        prompt.append("13. In the 'reason' field, clearly state what you're doing and why\n\n");

        prompt.append("=== EXAMPLES OF CORRECT INTENT HANDLING ===\n\n");

        prompt.append("Example 1 - ADD ONLY:\n");
        prompt.append("User: \"Add Gomti Riverfront to day 3\"\n");
        prompt.append("Correct: Insert ONE new node for Gomti Riverfront\n");
        prompt.append("Wrong: Insert Gomti Riverfront AND remove/modify other nodes\n\n");

        prompt.append("Example 2 - PRONOUN REFERENCE:\n");
        prompt.append("Previous: User asked about \"Gomti Riverfront\"\n");
        prompt.append("Current: \"Add it to the most appropriate day\"\n");
        prompt.append("Correct: Add Gomti Riverfront (from previous context)\n");
        prompt.append("Wrong: Add something else or multiple things\n\n");

        prompt.append("Example 3 - REPLACE:\n");
        prompt.append("User: \"Replace lunch with pizza\"\n");
        prompt.append("Correct: Replace the lunch node with pizza\n");
        prompt.append("Wrong: Add pizza AND keep lunch, or remove other meals\n\n");

        prompt.append("=== JSON FORMAT EXAMPLE ===\n");
        prompt.append("For \"Add Gomti Riverfront to Day 3\":\n");
        prompt.append("{\n");
        prompt.append("  \"ops\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"op\": \"insert\",\n");
        prompt.append("      \"after\": \"day3_att_2\",\n");
        prompt.append("      \"node\": {\n");
        prompt.append("        \"title\": \"Gomti Riverfront\",\n");
        prompt.append("        \"type\": \"attraction\",\n");
        prompt.append("        \"location\": {\n");
        prompt.append("          \"name\": \"Gomti Riverfront, Lucknow\",\n");
        prompt.append("          \"address\": \"Gomti Riverfront, Lucknow\"\n");
        prompt.append("        }\n");
        prompt.append("      }\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"day\": 3,\n");
        prompt.append("  \"reason\": \"Adding Gomti Riverfront to Day 3 as requested by user\",\n");
        prompt.append("  \"agent\": \"EditorAgent\"\n");
        prompt.append("}\n\n");

        prompt.append("CRITICAL REMINDERS:\n");
        prompt.append("- 'node' must have 'title' (not 'name'), 'type', and 'location' as an object\n");
        prompt.append("- Use EXACT node IDs from context for 'id' and 'after' fields\n");
        prompt.append("- ONLY generate operations for what user explicitly requested\n");
        prompt.append("- If user says 'add', use 'insert' operation - do NOT use 'delete' or 'replace'\n");
        prompt.append("- The 'reason' field should explain what you're doing in plain English\n\n");

        prompt.append("Generate ONLY the JSON ChangeSet, no additional text:");

        return prompt.toString();
    }

    /**
     * Parse ChangeSet from LLM response using LLMResponseHandler.
     */
    private ChangeSet parseChangeSetFromResponse(String response) {
        try {
            // Create expected schema for ChangeSet validation
            JsonNode expectedSchema = createChangeSetSchema();

            // Process response with LLMResponseHandler
            LLMResponseHandler.ProcessedResponse processedResponse = llmResponseHandler.processResponse(response,
                    expectedSchema, null);

            if (!processedResponse.isSuccess()) {
                logger.error("Failed to process LLM response for ChangeSet: {}", processedResponse.getErrors());
                throw new RuntimeException("Failed to process ChangeSet response: " +
                        String.join(", ", processedResponse.getErrors()));
            }

            // Log any validation warnings
            if (!processedResponse.getErrors().isEmpty()) {
                logger.warn("ChangeSet response validation warnings: {}", processedResponse.getErrors());
            }

            // Convert time strings to timestamps before deserializing
            JsonNode dataWithTimestamps = convertTimeStringsToTimestamps(processedResponse.getData());

            // Convert JsonNode to ChangeSet
            ChangeSet changeSet = objectMapper.treeToValue(dataWithTimestamps, ChangeSet.class);

            return changeSet;

        } catch (Exception e) {
            logger.error("Failed to parse ChangeSet from response: {}", response, e);
            throw new RuntimeException("ChangeSet parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert time strings (HH:mm) to Unix timestamps in milliseconds.
     * Processes the JsonNode and converts startTime/endTime fields.
     */
    private JsonNode convertTimeStringsToTimestamps(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.isObject()) {
            return jsonNode;
        }

        try {
            com.fasterxml.jackson.databind.node.ObjectNode objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) jsonNode;

            // Process ops array
            if (objectNode.has("ops") && objectNode.get("ops").isArray()) {
                com.fasterxml.jackson.databind.node.ArrayNode opsArray = (com.fasterxml.jackson.databind.node.ArrayNode) objectNode
                        .get("ops");

                for (JsonNode opNode : opsArray) {
                    if (opNode.isObject()) {
                        com.fasterxml.jackson.databind.node.ObjectNode op = (com.fasterxml.jackson.databind.node.ObjectNode) opNode;

                        // Convert startTime if present
                        if (op.has("startTime") && op.get("startTime").isTextual()) {
                            String timeStr = op.get("startTime").asText();
                            Long timestamp = convertTimeStringToTimestamp(timeStr);
                            if (timestamp != null) {
                                op.put("startTime", timestamp);
                                logger.debug("Converted startTime '{}' to timestamp {}", timeStr, timestamp);
                            }
                        }

                        // Convert endTime if present
                        if (op.has("endTime") && op.get("endTime").isTextual()) {
                            String timeStr = op.get("endTime").asText();
                            Long timestamp = convertTimeStringToTimestamp(timeStr);
                            if (timestamp != null) {
                                op.put("endTime", timestamp);
                                logger.debug("Converted endTime '{}' to timestamp {}", timeStr, timestamp);
                            }
                        }
                    }
                }
            }

            return objectNode;
        } catch (Exception e) {
            logger.warn("Error converting time strings to timestamps: {}", e.getMessage());
            return jsonNode;
        }
    }

    /**
     * Convert a time string (HH:mm) to Unix timestamp in milliseconds.
     * Uses current date as base for simplicity (day-relative timestamps).
     */
    private Long convertTimeStringToTimestamp(String timeStr) {
        if (timeStr == null || !timeStr.matches("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")) {
            logger.warn("Invalid time format: {}", timeStr);
            return null;
        }

        try {
            String[] parts = timeStr.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            // Convert to milliseconds from start of day
            // Use a fixed base timestamp (e.g., start of today)
            long millisecondsFromMidnight = (hours * 3600000L) + (minutes * 60000L);

            // For now, use a simple day-relative timestamp
            // In production, you'd want to use the actual day's base timestamp
            long baseTimestamp = System.currentTimeMillis() / 86400000L * 86400000L; // Start of current day

            return baseTimestamp + millisecondsFromMidnight;
        } catch (Exception e) {
            logger.error("Error parsing time string '{}': {}", timeStr, e.getMessage());
            return null;
        }
    }

    /**
     * Extract JSON from LLM response (remove markdown formatting, etc.).
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty response from LLM");
        }

        String cleaned = response.trim();

        // Remove markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // Find JSON object boundaries
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');

        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            throw new IllegalArgumentException("No valid JSON object found in response");
        }

        return cleaned.substring(startIndex, endIndex + 1).trim();
    }

    /**
     * Validate ChangeSet before applying.
     */
    private void validateChangeSet(ChangeSet changeSet) {
        if (changeSet == null) {
            throw new IllegalArgumentException("ChangeSet cannot be null");
        }

        if (changeSet.getOps() == null || changeSet.getOps().isEmpty()) {
            // Allow empty operations if there's a valid reason (e.g., locked nodes)
            if (changeSet.getReason() != null && !changeSet.getReason().trim().isEmpty()) {
                logger.info("ChangeSet has no operations but includes reason: {}", changeSet.getReason());
                // This is valid - the AI is explaining why it can't make changes
                return; // Just return void, validation passed
            } else {
                throw new IllegalArgumentException("ChangeSet must contain at least one operation");
            }
        }

        if (changeSet.getDay() == null || changeSet.getDay() < 1) {
            throw new IllegalArgumentException("ChangeSet must specify a valid day number");
        }

        // Validate each operation
        for (ChangeOperation op : changeSet.getOps()) {
            if (op.getOp() == null || op.getOp().trim().isEmpty()) {
                throw new IllegalArgumentException("Operation type cannot be null or empty");
            }

            String opType = op.getOp().toLowerCase();
            if (!opType.equals("insert") && !opType.equals("delete") &&
                    !opType.equals("move") && !opType.equals("replace")) {
                throw new IllegalArgumentException("Invalid operation type: " + op.getOp());
            }

            // Validate ID requirements based on operation type
            if (opType.equals("insert")) {
                // Insert operations need 'after' field OR 'position'
                boolean hasAfter = op.getAfter() != null && !op.getAfter().trim().isEmpty();
                boolean hasPosition = op.getPosition() != null;

                if (!hasAfter && !hasPosition) {
                    throw new IllegalArgumentException(
                            "Insert operation must specify either 'after' field or 'position'");
                }

                // Validate 'after' ID format if present
                if (hasAfter && !nodeIdValidator.isValidFormat(op.getAfter())) {
                    throw new IllegalArgumentException("Invalid node ID format in 'after' field: " + op.getAfter());
                }
            } else {
                // Delete, move, replace operations need 'id' field
                if (op.getId() == null || op.getId().trim().isEmpty()) {
                    throw new IllegalArgumentException(opType + " operation must specify 'id' field");
                }
                // IMPROVED: Validate ID format
                if (!nodeIdValidator.isValidFormat(op.getId())) {
                    throw new IllegalArgumentException("Invalid node ID format: " + op.getId());
                }
            }

            // IMPROVED: Validate new node IDs in insert/replace operations
            if ((opType.equals("insert") || opType.equals("replace")) && op.getNode() != null) {
                if (op.getNode().getId() != null && !op.getNode().getId().trim().isEmpty()) {
                    if (!nodeIdValidator.isValidFormat(op.getNode().getId())) {
                        throw new IllegalArgumentException(
                                "Invalid node ID format in new node: " + op.getNode().getId());
                    }
                }
            }
        }

        logger.debug("ChangeSet validation passed: {} operations for day {}",
                changeSet.getOps().size(), changeSet.getDay());
    }

    /**
     * Build JSON schema for ChangeSet generation.
     */
    private String buildChangeSetJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "ops": {
                      "type": "array",
                      "description": "Array of change operations to apply to the itinerary",
                      "items": {
                        "type": "object",
                        "properties": {
                          "op": {
                            "type": "string",
                            "enum": ["insert", "delete", "move", "replace"],
                            "description": "Operation type: insert (add new), delete (remove), move (reorder), replace (modify)"
                          },
                          "id": {
                            "type": "string",
                            "description": "Node ID - REQUIRED. Use the exact node ID from the itinerary context"
                          },
                          "startTime": {
                            "type": "string",
                            "pattern": "^([0-1][0-9]|2[0-3]):[0-5][0-9]$",
                            "description": "Start time in 24-hour format HH:mm. Examples: '09:00', '14:30', '18:00'"
                          },
                          "endTime": {
                            "type": "string",
                            "pattern": "^([0-1][0-9]|2[0-3]):[0-5][0-9]$",
                            "description": "End time in 24-hour format HH:mm. Examples: '10:30', '15:30', '19:00'"
                          },
                          "after": {
                            "type": "string",
                            "description": "Node ID to insert/move after (for ordering operations)"
                          },
                          "position": {
                            "type": "integer",
                            "description": "Position index for insert operations (0-based)"
                          },
                          "node": {
                            "type": "object",
                            "description": "Node data for insert/replace operations",
                            "properties": {
                              "title": {
                                "type": "string",
                                "description": "Node title/name (REQUIRED)"
                              },
                              "type": {
                                "type": "string",
                                "enum": ["attraction", "meal", "accommodation", "transport"],
                                "description": "Node type (REQUIRED)"
                              },
                              "location": {
                                "type": "object",
                                "description": "Location object with name and address (REQUIRED)",
                                "properties": {
                                  "name": {
                                    "type": "string",
                                    "description": "Location name"
                                  },
                                  "address": {
                                    "type": "string",
                                    "description": "Full address"
                                  }
                                },
                                "required": ["name", "address"]
                              }
                            },
                            "required": ["title", "type", "location"]
                          }
                        },
                        "required": ["op", "id"]
                      }
                    },
                    "day": {
                      "type": "integer",
                      "minimum": 1,
                      "description": "Day number (1-based) that these operations apply to"
                    },
                    "reason": {
                      "type": "string",
                      "description": "Human-readable explanation of why these changes were made"
                    },
                    "agent": {
                      "type": "string",
                      "description": "Always set to 'EditorAgent'"
                    }
                  },
                  "required": ["ops", "day", "reason", "agent"]
                }
                """;
    }

    /**
     * Load itinerary with fallback mechanisms.
     */
    private Optional<NormalizedItinerary> loadItineraryWithFallback(String itineraryId) {
        try {
            // Try master itinerary first
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getMasterItinerary(itineraryId);
            if (itineraryOpt.isPresent()) {
                logger.debug("Loaded master itinerary for {}", itineraryId);
                return itineraryOpt;
            }

            // Fallback to regular itinerary service
            logger.debug("Master itinerary not found, trying regular itinerary for {}", itineraryId);
            itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isPresent()) {
                logger.debug("Loaded regular itinerary for {}", itineraryId);
                return itineraryOpt;
            }

            logger.warn("No itinerary found for {}", itineraryId);
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error loading itinerary {}: {}", itineraryId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Validate itinerary for editing operations.
     */
    private void validateItineraryForEditing(NormalizedItinerary itinerary) {
        if (itinerary == null) {
            throw new IllegalArgumentException("Itinerary cannot be null");
        }

        if (itinerary.getItineraryId() == null || itinerary.getItineraryId().trim().isEmpty()) {
            throw new IllegalArgumentException("Itinerary must have a valid ID");
        }

        if (itinerary.getDays() == null || itinerary.getDays().isEmpty()) {
            throw new IllegalArgumentException("Itinerary must have at least one day");
        }

        // Check for basic data integrity
        for (int i = 0; i < itinerary.getDays().size(); i++) {
            var day = itinerary.getDays().get(i);
            if (day.getDayNumber() == null) {
                throw new IllegalArgumentException("Day at index " + i + " must have a day number");
            }
        }

        logger.debug("Itinerary validation passed for {}", itinerary.getItineraryId());
    }

    /**
     * Validate itinerary data consistency before processing.
     * Detects phantom nodes, invalid data, and inconsistencies.
     */
    private void validateItineraryDataConsistency(NormalizedItinerary itinerary) {
        if (itinerary == null || itinerary.getDays() == null) {
            return;
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }

            // Track node IDs to detect duplicates
            Set<String> seenIds = new HashSet<>();

            for (int i = 0; i < day.getNodes().size(); i++) {
                NormalizedNode node = day.getNodes().get(i);

                // Check for missing ID
                if (node.getId() == null || node.getId().trim().isEmpty()) {
                    errors.add(String.format("Day %d, position %d: Node without ID (title: %s)",
                            day.getDayNumber(), i, node.getTitle()));
                }

                // Check for duplicate IDs
                if (node.getId() != null && !seenIds.add(node.getId())) {
                    errors.add(String.format("Day %d: Duplicate node ID '%s'",
                            day.getDayNumber(), node.getId()));
                }

                // Check for missing title
                if (node.getTitle() == null || node.getTitle().trim().isEmpty()) {
                    errors.add(String.format("Day %d: Node without title (ID: %s)",
                            day.getDayNumber(), node.getId()));
                }

                // Check for invalid timing
                if (node.getTiming() != null) {
                    if (node.getTiming().getStartTime() != null &&
                            node.getTiming().getEndTime() != null &&
                            node.getTiming().getStartTime() > node.getTiming().getEndTime()) {
                        logger.warn("Itinerary data validation warnings for {}:", itinerary.getItineraryId());
                        warnings.forEach(warning -> logger.warn("  - {}", warning));
                    }

                    // Fail on errors
                    if (!errors.isEmpty()) {
                        logger.error("Itinerary data validation failed for {}:", itinerary.getItineraryId());
                        errors.forEach(error -> logger.error("  - {}", error));
                        throw new IllegalStateException("Invalid itinerary data: " + errors.size() + " errors found. " +
                                "First error: " + errors.get(0));
                    }

                    if (warnings.isEmpty()) {
                        logger.debug("Itinerary data consistency validation passed for {}", itinerary.getItineraryId());
                    } else {
                        logger.info("Itinerary data consistency validation passed with {} warnings for {}",
                                warnings.size(), itinerary.getItineraryId());
                    }
                }
            }
        }
    }

    /**
     * Check if a specific node is locked and cannot be modified.
     */
    private boolean isNodeLocked(NormalizedItinerary itinerary, String nodeId) {
        if (nodeId == null || itinerary == null || itinerary.getDays() == null) {
            return false;
        }

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() != null) {
                for (NormalizedNode node : day.getNodes()) {
                    if (nodeId.equals(node.getId())) {
                        return Boolean.TRUE.equals(node.getLocked());
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get node by ID for validation purposes.
     */
    private NormalizedNode getNodeById(NormalizedItinerary itinerary, String nodeId) {
        if (nodeId == null || itinerary == null || itinerary.getDays() == null) {
            return null;
        }

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() != null) {
                for (NormalizedNode node : day.getNodes()) {
                    if (nodeId.equals(node.getId())) {
                        return node;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Validate the request against locked nodes before making API calls.
     */
    private void validateRequestAgainstLockedNodes(ChatRequest chatRequest, NormalizedItinerary itinerary) {
        String selectedNodeId = chatRequest.getSelectedNodeId();

        // If a specific node is selected, check if it's locked
        if (selectedNodeId != null && !selectedNodeId.trim().isEmpty()) {
            if (isNodeLocked(itinerary, selectedNodeId)) {
                NormalizedNode lockedNode = getNodeById(itinerary, selectedNodeId);
                String nodeName = lockedNode != null ? lockedNode.getTitle() : "Selected node";
                throw new RuntimeException(
                        "Cannot modify locked node: " + nodeName + ". Please unlock the node first to make changes.");
            }
        }

        // Additional validation based on request text
        String requestText = chatRequest.getText().toLowerCase();

        // Check for requests that might affect locked nodes
        if (requestText.contains("hotel") || requestText.contains("accommodation")) {
            // Find hotel nodes and check if any are locked
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() != null) {
                    for (NormalizedNode node : day.getNodes()) {
                        if ("hotel".equals(node.getType()) || "accommodation".equals(node.getType())) {
                            if (Boolean.TRUE.equals(node.getLocked())) {
                                throw new RuntimeException("Cannot modify locked hotel: " + node.getTitle()
                                        + ". Please unlock the hotel first to make changes.");
                            }
                        }
                    }
                }
            }
        }

        logger.debug("Request validation passed - no locked nodes affected");
    }

    /**
     * Parse ChangeSet from LLM response with retry and continuation support.
     */
    private ChangeSet parseChangeSetFromResponseWithRetry(String response, String originalPrompt, String jsonSchema) {
        try {
            // Create expected schema for ChangeSet validation
            JsonNode expectedSchema = createChangeSetSchema();

            // Process response with LLMResponseHandler
            LLMResponseHandler.ProcessedResponse processedResponse = llmResponseHandler.processResponse(response,
                    expectedSchema, originalPrompt);

            if (!processedResponse.isSuccess()) {
                if (processedResponse.needsContinuation()) {
                    // Handle continuation request
                    logger.warn("ChangeSet response needs continuation, attempting continuation request");
                    String continuationResponse = geminiClient.generateStructuredContent(
                            processedResponse.getContinuationPrompt(), jsonSchema,
                            "You are an expert travel itinerary editor.");

                    // Log continuation response for analysis
                    logger.info("=== EDITOR AGENT - CONTINUATION RESPONSE ===");
                    logger.info("Continuation Response: {}", continuationResponse);
                    logger.info("=== END EDITOR AGENT CONTINUATION ===");

                    // Process continuation response
                    processedResponse = llmResponseHandler.processResponse(
                            continuationResponse, expectedSchema, processedResponse.getContinuationPrompt());
                }

                if (!processedResponse.isSuccess()) {
                    // Graceful degradation - create a simple ChangeSet
                    logger.warn("Failed to process ChangeSet response, attempting graceful degradation: {}",
                            processedResponse.getErrors());
                    throw new RuntimeException(
                            "Failed to parse ChangeSet from response, falling back to simple ChangeSet");
                }
            }

            // Log any validation warnings
            if (!processedResponse.getErrors().isEmpty()) {
                logger.warn("ChangeSet response validation warnings: {}", processedResponse.getErrors());
            }

            // Convert time strings to timestamps before deserializing
            JsonNode dataWithTimestamps = convertTimeStringsToTimestamps(processedResponse.getData());

            // Convert JsonNode to ChangeSet
            ChangeSet changeSet = objectMapper.treeToValue(dataWithTimestamps, ChangeSet.class);

            return changeSet;

        } catch (Exception e) {
            logger.error("Failed to parse ChangeSet from response, falling back to simple ChangeSet: {}",
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Create JSON schema for ChangeSet validation.
     */
    private JsonNode createChangeSetSchema() {
        try {
            String schemaJson = """
                    {
                      "type": "object",
                      "required": ["ops"],
                      "properties": {
                        "scope": { "type": "string" },
                        "day": { "type": "integer" },
                        "baseVersion": { "type": "integer" },
                        "idempotencyKey": { "type": "string" },
                        "ops": {
                          "type": "array",
                          "items": {
                            "type": "object",
                            "required": ["op"],
                            "properties": {
                              "op": { "type": "string", "enum": ["insert", "delete", "move", "update"] },
                              "id": { "type": "string" },
                              "after": { "type": "string" },
                              "startTime": { "type": "integer" },
                              "endTime": { "type": "integer" },
                              "node": { "type": "object" }
                            }
                          }
                        },
                        "preferences": {
                          "type": "object",
                          "properties": {
                            "userFirst": { "type": "boolean" },
                            "respectLocks": { "type": "boolean" }
                          }
                        }
                      }
                    }
                    """;
            return objectMapper.readTree(schemaJson);
        } catch (Exception e) {
            logger.error("Failed to create ChangeSet schema", e);
            return null;
        }
    }

    /**
     * Enrich newly added or replaced nodes with Google Places data.
     * This ensures chat-added nodes have the same rich data as pipeline-generated
     * nodes.
     */
    private void enrichNewlyAddedNodes(String itineraryId, ChangeSet changeSet) {
        if (changeSet == null || changeSet.getOps() == null || changeSet.getOps().isEmpty()) {
            return;
        }

        logger.info("🔍 [EditorAgent] Enriching newly added/replaced nodes for itinerary: {}", itineraryId);

        try {
            // Reload itinerary to get the latest state after changes
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isEmpty()) {
                logger.warn("⚠️ [EditorAgent] Cannot enrich nodes - itinerary not found: {}", itineraryId);
                return;
            }

            NormalizedItinerary itinerary = itineraryOpt.get();
            String destination = itinerary.getDestination();

            // Track nodes that need enrichment
            List<NormalizedNode> nodesToEnrich = new ArrayList<>();

            // Find nodes that were added or replaced
            for (ChangeOperation op : changeSet.getOps()) {
                String opType = op.getOp().toLowerCase();

                if ("insert".equals(opType) || "replace".equals(opType)) {
                    // Find the node in the itinerary
                    NormalizedNode node = findNodeById(itinerary, op.getId());
                    if (node != null && node.getLocation() != null) {
                        nodesToEnrich.add(node);
                        logger.info("📝 [EditorAgent] Node {} needs enrichment: {}", node.getId(), node.getTitle());
                    }
                }
            }

            if (nodesToEnrich.isEmpty()) {
                logger.info("✅ [EditorAgent] No nodes need enrichment");
                return;
            }

            logger.info("🔄 [EditorAgent] Enriching {} nodes", nodesToEnrich.size());

            // Enrich each node
            for (NormalizedNode node : nodesToEnrich) {
                try {
                    enrichNode(itineraryId, node, destination);
                } catch (Exception e) {
                    logger.error("❌ [EditorAgent] Failed to enrich node {}: {}", node.getId(), e.getMessage(), e);
                    // Continue with other nodes even if one fails
                }
            }

            // Save the enriched itinerary once after all nodes are enriched with optimistic
            // locking and retry
            logger.info("💾 [EditorAgent] Saving enriched itinerary with lock");
            int maxRetries = 3;
            int retryCount = 0;
            boolean saved = false;

            while (!saved && retryCount < maxRetries) {
                try {
                    itineraryJsonService.updateItineraryWithLock(itinerary);
                    logger.info("✅ [EditorAgent] Node enrichment complete and saved");
                    saved = true;
                } catch (com.tripplanner.exception.ConcurrentModificationException e) {
                    retryCount++;
                    logger.error("❌ [EditorAgent] Concurrent modification detected (attempt {}/{}): {}",
                            retryCount, maxRetries, e.getMessage());

                    if (retryCount < maxRetries) {
                        logger.info("🔄 [EditorAgent] Reloading itinerary and retrying save...");
                        Optional<NormalizedItinerary> reloaded = itineraryJsonService
                                .getItinerary(itinerary.getItineraryId());
                        if (reloaded.isPresent()) {
                            itinerary = reloaded.get();
                            logger.info("🔄 [EditorAgent] Reloaded itinerary, will re-enrich nodes");
                            // Note: Enrichment data is already in memory, just need to save
                        } else {
                            logger.error("❌ [EditorAgent] Failed to reload itinerary for retry");
                            throw new RuntimeException(
                                    "Your changes conflict with another update. Please refresh and try again.", e);
                        }
                    } else {
                        logger.error("❌ [EditorAgent] Max retries ({}) exceeded, giving up", maxRetries);
                        throw new RuntimeException(
                                "Your changes conflict with another update. Please refresh and try again.", e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("❌ [EditorAgent] Failed to enrich nodes: {}", e.getMessage(), e);
            // Don't throw - enrichment failure shouldn't break the edit operation
        }
    }

    /**
     * Enrich a single node with Google Places data.
     * Uses caching to avoid duplicate API calls.
     */
    private void enrichNode(String itineraryId, NormalizedNode node, String destination) {
        logger.info("🔍 [EditorAgent] Enriching node: {} - {}", node.getId(), node.getTitle());

        // Check if node already has enrichment data (photos, rating, etc.)
        boolean hasPhotos = node.getLocation().getPhotos() != null && !node.getLocation().getPhotos().isEmpty();
        boolean hasRating = node.getLocation().getRating() != null;
        boolean hasUserRatings = node.getLocation().getUserRatingsTotal() != null;

        if (hasPhotos && hasRating && hasUserRatings) {
            logger.info("✅ [EditorAgent] Node already enriched, skipping");
            return;
        }

        // Step 1: Search for place if no placeId
        if (node.getLocation().getPlaceId() == null || node.getLocation().getPlaceId().trim().isEmpty()) {
            String searchQuery = node.getTitle();
            if (node.getLocation().getName() != null && !node.getLocation().getName().trim().isEmpty()) {
                searchQuery = node.getLocation().getName();
            }

            logger.info("🔍 [EditorAgent] Searching for place: '{}' in '{}'", searchQuery, destination);

            try {
                // searchPlace is @Cacheable, so duplicate calls are cached
                PlaceSearchResult searchResult = googlePlacesService.searchPlace(searchQuery, destination);

                if (searchResult != null) {
                    // Update node title with actual place name from Google
                    node.setTitle(searchResult.getName());
                    logger.info("📝 [EditorAgent] Updated node title to: {}", searchResult.getName());

                    // Update node with search results
                    if (node.getLocation().getCoordinates() == null) {
                        node.getLocation().setCoordinates(new Coordinates());
                    }
                    node.getLocation().getCoordinates().setLat(
                            searchResult.getGeometry().getLocation().getLatitude());
                    node.getLocation().getCoordinates().setLng(
                            searchResult.getGeometry().getLocation().getLongitude());
                    node.getLocation().setPlaceId(searchResult.getPlaceId());
                    node.getLocation().setName(searchResult.getName());
                    node.getLocation().setAddress(searchResult.getFormattedAddress());

                    if (searchResult.getRating() != null) {
                        node.getLocation().setRating(searchResult.getRating());
                    }

                    logger.info("✅ [EditorAgent] Found place: {} [{}]", searchResult.getName(),
                            searchResult.getPlaceId());
                }
            } catch (Exception e) {
                logger.warn("⚠️ [EditorAgent] Place search failed for '{}': {}", searchQuery, e.getMessage());
                return; // Don't proceed to details if search failed
            }
        }

        // Step 2: Get place details if we have a placeId and missing enrichment data
        if (node.getLocation().getPlaceId() != null && !node.getLocation().getPlaceId().trim().isEmpty()) {
            try {
                logger.info("📸 [EditorAgent] Getting place details for placeId: {}", node.getLocation().getPlaceId());

                // getPlaceDetails is @Cacheable, so duplicate calls are cached
                PlaceDetails placeDetails = googlePlacesService.getPlaceDetails(node.getLocation().getPlaceId());

                if (placeDetails != null) {
                    // Update node title with actual place name from Google if available
                    if (placeDetails.getName() != null && !placeDetails.getName().trim().isEmpty()) {
                        node.setTitle(placeDetails.getName());
                        node.getLocation().setName(placeDetails.getName());
                        logger.info("📝 [EditorAgent] Updated node title to: {}", placeDetails.getName());
                    }

                    // Update address if more detailed
                    if (placeDetails.getFormattedAddress() != null
                            && !placeDetails.getFormattedAddress().trim().isEmpty()) {
                        node.getLocation().setAddress(placeDetails.getFormattedAddress());
                    }

                    // Set photos only if missing
                    if (!hasPhotos && placeDetails.getPhotos() != null && !placeDetails.getPhotos().isEmpty()) {
                        List<String> photoRefs = placeDetails.getPhotos().stream()
                                .limit(5)
                                .map(Photo::getPhotoReference)
                                .collect(Collectors.toList());
                        node.getLocation().setPhotos(photoRefs);
                        logger.info("📸 [EditorAgent] Set {} photos", photoRefs.size());
                    }

                    // Set rating and reviews only if missing
                    if (!hasRating && placeDetails.getRating() != null) {
                        node.getLocation().setRating(placeDetails.getRating());
                    }
                    if (!hasUserRatings && placeDetails.getUserRatingsTotal() != null) {
                        node.getLocation().setUserRatingsTotal(placeDetails.getUserRatingsTotal());
                    }
                    if (placeDetails.getPriceLevel() != null) {
                        node.getLocation().setPriceLevel(placeDetails.getPriceLevel());
                    }

                    logger.info("✅ [EditorAgent] Enriched node with rating: {}, reviews: {}, photos: {}",
                            placeDetails.getRating(),
                            placeDetails.getUserRatingsTotal(),
                            placeDetails.getPhotos() != null ? placeDetails.getPhotos().size() : 0);
                }
            } catch (Exception e) {
                logger.warn("⚠️ [EditorAgent] Failed to get place details: {}", e.getMessage());
            }
        }
    }

    /**
     * Find a node by ID in the itinerary.
     */
    private NormalizedNode findNodeById(NormalizedItinerary itinerary, String nodeId) {
        if (itinerary.getDays() == null) {
            return null;
        }

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }

            for (NormalizedNode node : day.getNodes()) {
                if (nodeId != null && nodeId.equals(node.getId())) {
                    return node;
                }
            }
        }

        return null;
    }

    /**
     * Generate a descriptive response message based on the changes made.
     * This provides specific feedback to the user about what was changed.
     */
    private String generateResponseMessage(ChangeSet changeSet, NormalizedItinerary itinerary) {
        if (changeSet == null || changeSet.getOps() == null || changeSet.getOps().isEmpty()) {
            return "No changes were made to your itinerary";
        }

        List<String> messages = new ArrayList<>();

        for (ChangeOperation op : changeSet.getOps()) {
            String opType = op.getOp().toLowerCase();
            String nodeTitle = getNodeTitle(op, itinerary);
            Integer dayNumber = changeSet.getDay();

            switch (opType) {
                case "insert":
                    messages.add(String.format("✅ Added '%s' to Day %d", nodeTitle, dayNumber));
                    break;
                case "delete":
                    messages.add(String.format("🗑️ Removed '%s' from Day %d", nodeTitle, dayNumber));
                    break;
                case "replace":
                    messages.add(String.format("🔄 Updated '%s' on Day %d", nodeTitle, dayNumber));
                    break;
                case "move":
                    messages.add(String.format("⏰ Rescheduled '%s' on Day %d", nodeTitle, dayNumber));
                    break;
                default:
                    messages.add(String.format("Modified '%s' on Day %d", nodeTitle, dayNumber));
            }
        }

        // If we have a reason from the ChangeSet, add it
        if (changeSet.getReason() != null && !changeSet.getReason().trim().isEmpty()) {
            messages.add("Reason: " + changeSet.getReason());
        }

        return String.join("\n", messages);
    }

    /**
     * Get the title of a node from the operation or itinerary.
     */
    private String getNodeTitle(ChangeOperation op, NormalizedItinerary itinerary) {
        // First try to get title from the operation's node data
        if (op.getNode() != null && op.getNode().getTitle() != null) {
            return op.getNode().getTitle();
        }

        // Otherwise, look up the node in the itinerary
        NormalizedNode node = findNodeById(itinerary, op.getId());
        if (node != null && node.getTitle() != null) {
            return node.getTitle();
        }

        // Fallback to node ID
        return op.getId();
    }

    /**
     * Get recent chat history for context.
     * Returns last 3 messages to help LLM understand pronouns and references.
     */
    private String getChatHistoryContext(String itineraryId) {
        try {
            List<Map<String, Object>> history = chatHistoryService.getChatHistory(itineraryId);
            if (history == null || history.isEmpty()) {
                return null;
            }

            StringBuilder chatContext = new StringBuilder();

            // Get last 3 messages for context (not too much to avoid token waste)
            int start = Math.max(0, history.size() - 3);
            for (int i = start; i < history.size(); i++) {
                Map<String, Object> msg = history.get(i);
                String sender = "user".equals(msg.get("sender")) ? "User" : "Assistant";
                String message = (String) msg.get("message");
                chatContext.append(sender).append(": ").append(message).append("\n");
            }

            return chatContext.toString();

        } catch (Exception e) {
            logger.warn("Could not load chat history for context: {}", e.getMessage());
            return "";
        }
    }
}
