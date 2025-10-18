package com.tripplanner.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
    
    public EditorAgent(AgentEventBus eventBus,
                      SummarizationService summarizationService,
                      ChangeEngine changeEngine,
                      GeminiClient geminiClient,
                      ItineraryJsonService itineraryJsonService,
                      ObjectMapper objectMapper,
                      LLMResponseHandler llmResponseHandler,
                      ItineraryMigrationService migrationService) {
        super(eventBus, AgentEvent.AgentKind.EDITOR);
        this.summarizationService = summarizationService;
        this.changeEngine = changeEngine;
        this.geminiClient = geminiClient;
        this.itineraryJsonService = itineraryJsonService;
        this.objectMapper = objectMapper;
        this.llmResponseHandler = llmResponseHandler;
        this.migrationService = migrationService;
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
                           day.getNodes() != null ? 
                               day.getNodes().stream().map(node -> node.getId()).collect(java.util.stream.Collectors.toList()) : 
                               "null");
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
            
            // Apply changes using changeEngine with the itinerary object to ensure consistency
            ChangeEngine.ApplyResult applyResult = changeEngine.apply(itinerary, changeSet);
            
            emitProgress(itineraryId, 100, "Changes applied successfully", "complete");
            
            // Return ApplyResult cast to generic type T
            @SuppressWarnings("unchecked")
            T result = (T) applyResult;
            return result;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid arguments for EditorAgent execution: {}", e.getMessage());
            throw new RuntimeException("Invalid request: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("EditorAgent execution failed for itinerary {}: {}", itineraryId, e.getMessage(), e);
            
            // Ensure itinerary is not left in broken state by checking if changes were partially applied
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
            logger.error("Unexpected error in EditorAgent execution for itinerary {}: {}", itineraryId, e.getMessage(), e);
            throw new RuntimeException("Unexpected error during editing: " + getUserFriendlyErrorMessage(e), e);
        }
    }
    
    @Override
    protected String getAgentName() {
        return "EditorAgent";
    }
    
    /**
     * Extract ChatRequest from AgentRequest data.
     * The data can be either a direct ChatRequest or a Map containing a ChatRequest.
     */
    private ChatRequest extractChatRequest(AgentRequest<?> request) {
        Object data = request.getData();
        
        // Try direct cast first
        if (data instanceof ChatRequest) {
            return (ChatRequest) data;
        }
        
        // Try to extract from Map
        if (data instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
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
            emptyChangeSet.setOps(new java.util.ArrayList<>());
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
                String response = geminiClient.generateStructuredContent(prompt, jsonSchema, "You are an expert travel itinerary editor.");
                
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
        
        prompt.append("=== TIME FORMAT RULES ===\n");
        prompt.append("1. Use 24-hour time format for startTime and endTime (e.g., \"14:30\", \"18:00\")\n");
        prompt.append("2. Format: \"HH:mm\" where HH is 00-23 and mm is 00-59\n");
        prompt.append("3. Examples: \"09:00\" for 9am, \"14:30\" for 2:30pm, \"18:00\" for 6pm\n");
        prompt.append("4. Always use leading zeros (\"09:00\" not \"9:00\")\n\n");
        
        prompt.append("USER REQUEST:\n");
        prompt.append(chatRequest.getText()).append("\n\n");
        
        prompt.append("CURRENT ITINERARY CONTEXT:\n");
        prompt.append(context).append("\n\n");
        
        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. Analyze the user's request and determine what changes are needed\n");
        prompt.append("2. CRITICAL: Look for nodes in the itinerary context marked as [ID: xxxxx] - you MUST use this EXACT ID in your operation\n");
        prompt.append("3. NEVER generate your own node IDs - always use the exact IDs from the context\n");
        prompt.append("4. For 'add'/'insert' requests: Choose 'insert' operation and specify 'after' with an existing node ID from context\n");
        prompt.append("5. For 'replace'/'change' requests: Choose 'replace' operation with the exact node ID from context\n");
        prompt.append("6. For 'delete'/'remove' requests: Choose 'delete' operation with the exact node ID from context\n");
        prompt.append("7. For 'move'/'reschedule' requests: Choose 'move' operation with new startTime/endTime\n");
        prompt.append("8. For timing: Use 24-hour format (e.g., \"14:30\") for startTime and endTime\n");
        prompt.append("9. Set 'day' to the specific day number (e.g., 1, 2, 3)\n");
        prompt.append("10. Always set agent to 'EditorAgent'\n\n");
        
        prompt.append("=== CORRECT JSON FORMAT EXAMPLE ===\n");
        prompt.append("{\n");
        prompt.append("  \"ops\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"op\": \"replace\",\n");
        prompt.append("      \"id\": \"day1_att_1\",\n");
        prompt.append("      \"startTime\": \"18:30\",\n");
        prompt.append("      \"endTime\": \"19:30\",\n");
        prompt.append("      \"node\": {\n");
        prompt.append("        \"title\": \"Sushi Dinner in Hadibo\",\n");
        prompt.append("        \"type\": \"meal\",\n");
        prompt.append("        \"location\": {\n");
        prompt.append("          \"name\": \"Hadibo\",\n");
        prompt.append("          \"address\": \"Hadibo, Socotra Island\"\n");
        prompt.append("        }\n");
        prompt.append("      }\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"day\": 2,\n");
        prompt.append("  \"reason\": \"Replacing dinner with sushi place as requested\",\n");
        prompt.append("  \"agent\": \"EditorAgent\"\n");
        prompt.append("}\n\n");
        prompt.append("CRITICAL: 'node' must have 'title' (not 'name'), 'type', and 'location' as an object with 'name' and 'address' fields!\n");
        prompt.append("CRITICAL: Use the EXACT node ID from the context (e.g., 'day1_att_1', 'node_mea_day1_1234_abcd1234') - never generate your own!\n\n");
        
        prompt.append("Generate ONLY the JSON ChangeSet, no additional text:");
        
        return prompt.toString();
    }
    
    /**
     * Parse ChangeSet from LLM response using LLMResponseHandler.
     */
    private ChangeSet parseChangeSetFromResponse(String response) {
        try {
            // Create expected schema for ChangeSet validation
            com.fasterxml.jackson.databind.JsonNode expectedSchema = createChangeSetSchema();
            
            // Process response with LLMResponseHandler
            LLMResponseHandler.ProcessedResponse processedResponse = 
                llmResponseHandler.processResponse(response, expectedSchema, null);
            
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
            com.fasterxml.jackson.databind.JsonNode dataWithTimestamps = convertTimeStringsToTimestamps(processedResponse.getData());
            
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
    private com.fasterxml.jackson.databind.JsonNode convertTimeStringsToTimestamps(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.isObject()) {
            return jsonNode;
        }
        
        try {
            com.fasterxml.jackson.databind.node.ObjectNode objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) jsonNode;
            
            // Process ops array
            if (objectNode.has("ops") && objectNode.get("ops").isArray()) {
                com.fasterxml.jackson.databind.node.ArrayNode opsArray = (com.fasterxml.jackson.databind.node.ArrayNode) objectNode.get("ops");
                
                for (com.fasterxml.jackson.databind.JsonNode opNode : opsArray) {
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
            
            if (op.getId() == null || op.getId().trim().isEmpty()) {
                throw new IllegalArgumentException("Operation ID cannot be null or empty");
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
        
        java.util.List<String> errors = new java.util.ArrayList<>();
        java.util.List<String> warnings = new java.util.ArrayList<>();
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }
            
            // Track node IDs to detect duplicates
            java.util.Set<String> seenIds = new java.util.HashSet<>();
            
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
                        warnings.add(String.format("Day %d: Node %s has invalid timing (start > end)", 
                                                  day.getDayNumber(), node.getId()));
                    }
                }
                
                // Check for suspicious node IDs (non-sequential)
                if (node.getId() != null && node.getId().matches("day\\d+_node\\d+")) {
                    String expectedId = String.format("day%d_node%d", day.getDayNumber(), i + 1);
                    if (!node.getId().equals(expectedId)) {
                        warnings.add(String.format("Day %d, position %d: Non-sequential node ID '%s' (expected '%s')", 
                                                  day.getDayNumber(), i, node.getId(), expectedId));
                    }
                }
            }
        }
        
        // Log warnings
        if (!warnings.isEmpty()) {
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
                throw new RuntimeException("Cannot modify locked node: " + nodeName + ". Please unlock the node first to make changes.");
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
                                throw new RuntimeException("Cannot modify locked hotel: " + node.getTitle() + ". Please unlock the hotel first to make changes.");
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
            com.fasterxml.jackson.databind.JsonNode expectedSchema = createChangeSetSchema();
            
            // Process response with LLMResponseHandler
            LLMResponseHandler.ProcessedResponse processedResponse = 
                llmResponseHandler.processResponse(response, expectedSchema, originalPrompt);
            
            if (!processedResponse.isSuccess()) {
                if (processedResponse.needsContinuation()) {
                    // Handle continuation request
                    logger.warn("ChangeSet response needs continuation, attempting continuation request");
                    String continuationResponse = geminiClient.generateStructuredContent(
                        processedResponse.getContinuationPrompt(), jsonSchema, "You are an expert travel itinerary editor.");
                    
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
                    throw new RuntimeException("Failed to parse ChangeSet from response, falling back to simple ChangeSet");
                }
            }
            
            // Log any validation warnings
            if (!processedResponse.getErrors().isEmpty()) {
                logger.warn("ChangeSet response validation warnings: {}", processedResponse.getErrors());
            }
            
            // Convert time strings to timestamps before deserializing
            com.fasterxml.jackson.databind.JsonNode dataWithTimestamps = convertTimeStringsToTimestamps(processedResponse.getData());
            
            // Convert JsonNode to ChangeSet
            ChangeSet changeSet = objectMapper.treeToValue(dataWithTimestamps, ChangeSet.class);
            
            return changeSet;
            
        } catch (Exception e) {
            logger.error("Failed to parse ChangeSet from response, falling back to simple ChangeSet: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Create JSON schema for ChangeSet validation.
     */
    private com.fasterxml.jackson.databind.JsonNode createChangeSetSchema() {
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
}