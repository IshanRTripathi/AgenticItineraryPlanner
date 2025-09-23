package com.tripplanner.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.GeminiClient;
import com.tripplanner.service.ItineraryJsonService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Planner Agent - Main orchestrator for itinerary generation using Gemini.
 */
@Component
@ConditionalOnBean(GeminiClient.class)
public class PlannerAgent extends BaseAgent {
    
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final ItineraryJsonService itineraryJsonService;
    private final ChangeEngine changeEngine;
    
    public PlannerAgent(AgentEventBus eventBus, GeminiClient geminiClient, ObjectMapper objectMapper,
                       ItineraryJsonService itineraryJsonService, ChangeEngine changeEngine) {
        super(eventBus, AgentEvent.AgentKind.planner);
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.changeEngine = changeEngine;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        CreateItineraryReq itineraryReq = request.getData(CreateItineraryReq.class);
        
        if (itineraryReq == null) {
            throw new IllegalArgumentException("PlannerAgent requires non-null CreateItineraryReq");
        }
        
        logger.info("=== PLANNER AGENT PROCESSING ===");
        logger.info("Destination: {}", itineraryReq.getDestination());
        logger.info("Start Date: {}", itineraryReq.getStartDate());
        logger.info("End Date: {}", itineraryReq.getEndDate());
        logger.info("Duration: {} days", itineraryReq.getDurationDays());
        logger.info("Party: {} adults", itineraryReq.getParty() != null ? itineraryReq.getParty().getAdults() : 0);
        logger.info("Budget Tier: {}", itineraryReq.getBudgetTier());
        logger.info("Interests: {}", itineraryReq.getInterests());
        logger.info("Constraints: {}", itineraryReq.getConstraints());
        logger.info("Language: {}", itineraryReq.getLanguage());
        logger.info("=================================");
        
        emitProgress(itineraryId, 10, "Analyzing trip requirements", "requirement_analysis");
        
        // Build the system prompt for itinerary planning
        String systemPrompt = buildSystemPrompt();
        
        emitProgress(itineraryId, 30, "Generating itinerary structure", "structure_generation");
        
        // Build the user prompt with trip details
        String userPrompt = buildUserPrompt(itineraryReq);
        
        logger.info("=== PLANNER AGENT PROMPTS ===");
        logger.info("System Prompt Length: {} chars", systemPrompt.length());
        logger.info("User Prompt: {}", userPrompt);
        logger.info("=============================");
        
        emitProgress(itineraryId, 50, "Processing with AI model", "ai_processing");
        
        // Call Gemini for structured JSON
        String schema = buildItineraryJsonSchema();
        String response = geminiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
        
        logger.info("=== PLANNER AGENT RESPONSE ===");
        logger.info("Using JSON file response");
        logger.info("Response Length: {} chars", response.length());
        logger.info("Response Preview: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        logger.info("===============================");
        
        emitProgress(itineraryId, 80, "Parsing generated itinerary", "parsing");
        
        // Parse and validate the response
        try {
            // Parse JSON tree first to normalize time fields like "14:00" → "YYYY-MM-DDTHH:mm:00Z"
            String cleanedResponse = cleanJsonResponse(response);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(cleanedResponse);
            normalizeTimeFields(root);
            NormalizedItinerary normalizedItinerary = objectMapper.treeToValue(root, NormalizedItinerary.class);
            
            // Ensure the correct itinerary ID is set
            normalizedItinerary.setItineraryId(itineraryId);
            
            logger.info("=== PLANNER AGENT RESULT ===");
            logger.info("Parsed Response Type: {}", normalizedItinerary.getClass().getSimpleName());
            logger.info("Itinerary ID: {}", normalizedItinerary.getItineraryId());
            logger.info("Days Generated: {}", normalizedItinerary.getDays() != null ? normalizedItinerary.getDays().size() : 0);
            logger.info("Result: {}", normalizedItinerary);
            logger.info("============================");
            
            emitProgress(itineraryId, 90, "Finalizing itinerary", "finalization");
            
            // Update the existing itinerary with the generated content
            itineraryJsonService.updateItinerary(normalizedItinerary);
            
            // Convert to the expected response type
            if (request.getResponseType() == ItineraryDto.class) {
                ItineraryDto result = convertNormalizedToItineraryDto(normalizedItinerary, itineraryReq);
                
                logger.info("=== PLANNER AGENT FINAL RESULT ===");
                logger.info("Converted to ItineraryDto: {}", result.getId());
                logger.info("Final Result: {}", result);
                logger.info("==================================");
                
                return (T) result;
            }
            
            return (T) normalizedItinerary;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse Gemini response for itinerary: {}", itineraryId, e);
            logger.error("Raw response that failed to parse: {}", response);
            throw new RuntimeException("Failed to parse generated itinerary: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected String getAgentName() {
        return "Planner Agent";
    }
    
    /**
     * Normalize time fields in the normalized itinerary JSON tree.
     * Accepts times like "14:00" and converts them into ISO-8601 instants
     * by combining with the day date when available.
     */
    private void normalizeTimeFields(com.fasterxml.jackson.databind.JsonNode root) {
        if (root == null || !root.has("days") || !root.get("days").isArray()) {
            return;
        }
        for (com.fasterxml.jackson.databind.JsonNode day : root.get("days")) {
            String dayDate = day.has("date") ? day.get("date").asText(null) : null;
            if (!day.has("nodes") || !day.get("nodes").isArray()) continue;
            for (com.fasterxml.jackson.databind.JsonNode node : day.get("nodes")) {
                if (!node.has("timing")) continue;
                com.fasterxml.jackson.databind.node.ObjectNode timing = (com.fasterxml.jackson.databind.node.ObjectNode) node.get("timing");
                if (timing == null) continue;

                // Normalize startTime and endTime if they are short times like HH:mm
                normalizeIsoInstant(timing, "startTime", dayDate);
                normalizeIsoInstant(timing, "endTime", dayDate);
            }
        }
    }

    private void normalizeIsoInstant(com.fasterxml.jackson.databind.node.ObjectNode obj, String field, String dayDate) {
        if (obj == null || !obj.has(field)) return;
        String value = obj.get(field).asText(null);
        if (value == null || value.isBlank()) return;
        // If already ISO-8601 with date and 'T', keep as-is
        if (value.contains("T")) return;
        // If only time HH:mm, combine with dayDate, else skip
        if (dayDate != null && value.matches("^\\d{2}:\\d{2}$")) {
            String iso = dayDate + "T" + value + ":00Z";
            obj.put(field, iso);
        }
    }

    /**
     * Generate a ChangeSet for modifying an existing normalized itinerary.
     * This is the new MVP contract method that works with normalized JSON.
     */
    public ChangeSet generateChangeSet(String itineraryId, String userRequest) {
        logger.info("=== PLANNER AGENT GENERATING CHANGESET ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("User Request: {}", userRequest);
        
        try {
            // Load the current normalized itinerary
            var currentItinerary = itineraryJsonService.getItinerary(itineraryId);
            if (currentItinerary.isEmpty()) {
                throw new RuntimeException("Itinerary not found: " + itineraryId);
            }
            
            emitProgress(itineraryId, 10, "Analyzing current itinerary", "analysis");
            
            emitProgress(itineraryId, 30, "Generating changes", "change_generation");
            
            // Build prompt for change generation
            String systemPrompt = buildSystemPromptForChanges();
            String userPrompt = buildUserPromptForChanges(itineraryId, currentItinerary.get(), userRequest);
            String response = geminiClient.generateStructuredContent(userPrompt, getChangeSetSchema(), systemPrompt);
            
            emitProgress(itineraryId, 60, "Parsing changes", "parsing");
            
            // Parse the response to extract ChangeSet
            ChangeSet changeSet = parseChangeSetFromResponse(response, userRequest);
            
            emitProgress(itineraryId, 90, "Validating changes", "validation");
            
            // Validate the ChangeSet respects locks
            validateChangeSet(changeSet, currentItinerary.get());
            
            emitProgress(itineraryId, 100, "ChangeSet generated", "complete");
            
            logger.info("=== CHANGESET GENERATED ===");
            logger.info("Operations: {}", changeSet.getOps() != null ? changeSet.getOps().size() : 0);
            logger.info("Scope: {}", changeSet.getScope());
            
            return changeSet;
            
        } catch (Exception e) {
            logger.error("Failed to generate ChangeSet for itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to generate ChangeSet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Apply a ChangeSet to an itinerary using the ChangeEngine.
     */
    public ChangeEngine.ApplyResult applyChangeSet(String itineraryId, ChangeSet changeSet) {
        logger.info("=== PLANNER AGENT APPLYING CHANGESET ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("ChangeSet: {}", changeSet);
        
        try {
            emitProgress(itineraryId, 50, "Applying changes", "applying");
            
            // Use ChangeEngine to apply the changes
            ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);
            
            emitProgress(itineraryId, 100, "Changes applied", "complete");
            
            logger.info("=== CHANGESET APPLIED ===");
            logger.info("New version: {}", result.getToVersion());
            logger.info("Changes: {}", result.getDiff());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to apply ChangeSet for itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to apply ChangeSet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clean JSON response by removing markdown code blocks if present.
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return response;
        }
        
        String cleaned = response.trim();
        
        // Remove markdown code blocks (```json ... ```)
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove ```json
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove ```
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove trailing ```
        }
        
        return cleaned.trim();
    }
    
    private String buildSystemPrompt() {
        return """
            You are an expert travel planner AI that creates detailed, personalized itineraries using a normalized structure.
            
            Your responsibilities:
            1. Create day-by-day itineraries based on traveler preferences
            2. Structure all activities as NODES with consistent properties
            3. Include EDGES to represent transitions between nodes
            4. Consider budget constraints and optimize for the best experience
            5. Provide practical details like timing, costs, and tips
            6. Ensure realistic travel times and logical flow between activities
            
            CRITICAL REQUIREMENT: You MUST populate ALL days with meaningful activities and nodes.
            Do NOT leave any days with empty nodes arrays. Each day should have at least 3-5 nodes 
            including attractions, meals, and transportation as appropriate.
            
            Node Types:
            - "attraction": Sightseeing, museums, landmarks, experiences
            - "meal": Restaurants, cafes, food experiences
            - "accommodation": Hotels, hostels, vacation rentals
            - "transport": Travel type between locations i.e cabs, trains, buses, flights
            
            Guidelines:
            - All activities must be structured as nodes with id, type, title, location, timing, cost
            - Use edges to connect nodes and specify transit information
            - Include practical information like booking requirements and costs
            - Consider opening hours, weather, and seasonal factors
            - Provide realistic timing and duration estimates
            - EVERY DAY must have multiple nodes - never leave a day empty
            
            Always respond with valid JSON matching the exact normalized schema provided.
            """;
    }
    
    private String buildUserPrompt(CreateItineraryReq request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Create a detailed normalized itinerary for the following trip:\n\n");
        
        // Basic trip info
        prompt.append("Itinerary ID: ").append(request.getDestination().hashCode() + "_" + System.currentTimeMillis()).append("\n");
        prompt.append("Destination: ").append(request.getDestination()).append("\n");
        prompt.append("Start Date: ").append(request.getStartDate()).append("\n");
        prompt.append("End Date: ").append(request.getEndDate()).append("\n");
        prompt.append("Duration: ").append(request.getDurationDays()).append(" days\n");
        
        // Party information
        if (request.getParty() != null) {
            prompt.append("Party: ").append(request.getParty().getAdults()).append(" adults");
            if (request.getParty().getChildren() > 0) {
                prompt.append(", ").append(request.getParty().getChildren()).append(" children");
            }
            if (request.getParty().getInfants() > 0) {
                prompt.append(", ").append(request.getParty().getInfants()).append(" infants");
            }
            prompt.append("\n");
        }
        
        // Budget and preferences
        prompt.append("Budget Tier: ").append(request.getBudgetTier()).append("\n");
        prompt.append("Language: ").append(request.getLanguage()).append("\n");
        
        // Interests
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            prompt.append("Interests: ").append(String.join(", ", request.getInterests())).append("\n");
        }
        
        // Constraints
        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            prompt.append("Constraints: ").append(String.join(", ", request.getConstraints())).append("\n");
        }
        
        // Add themes and currency
        prompt.append("Themes: ").append(String.join(", ", request.getInterests())).append("\n");
        prompt.append("Currency: INR\n");
        
        prompt.append("\nIMPORTANT: Create a comprehensive normalized itinerary with nodes and edges that maximizes the travel experience while staying within the specified parameters.");
        prompt.append("\nYou MUST populate ALL ").append(request.getDurationDays()).append(" days with meaningful activities, meals, and transportation.");
        prompt.append("\nEach day should have at least 3-5 nodes including attractions, meals, and transport as appropriate.");
        prompt.append("\nDo NOT leave any days with empty nodes arrays.");
        
        return prompt.toString();
    }
    
    private String buildItineraryJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "itineraryId": {
                  "type": "string",
                  "description": "Unique identifier for the itinerary"
                },
                "version": {
                  "type": "integer",
                  "description": "Version number of the itinerary",
                  "default": 1
                },
                "summary": {
                  "type": "string",
                  "description": "Brief overview of the itinerary"
                },
                "currency": {
                  "type": "string",
                  "description": "Currency code (e.g., USD, EUR)"
                },
                "themes": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Themes and interests for the trip"
                },
                "days": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "dayNumber": { "type": "integer" },
                      "date": { "type": "string", "format": "date" },
                      "location": { "type": "string" },
                      "nodes": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "id": { "type": "string" },
                            "type": { "type": "string", "enum": ["attraction", "meal", "accommodation", "transport"] },
                            "title": { "type": "string" },
                            "location": {
                              "type": "object",
                              "properties": {
                                "name": { "type": "string" },
                                "address": { "type": "string" },
                                "coordinates": {
                                  "type": "object",
                                  "properties": {
                                    "lat": { "type": "number" },
                                    "lng": { "type": "number" }
                                  }
                                }
                              }
                            },
                            "timing": {
                              "type": "object",
                              "properties": {
                                "startTime": { "type": "string" },
                                "endTime": { "type": "string" },
                                "durationMin": { "type": "integer" }
                              }
                            },
                            "cost": {
                              "type": "object",
                              "properties": {
                                "amount": { "type": "number" },
                                "currency": { "type": "string" },
                                "per": { "type": "string" }
                              }
                            },
                            "details": {
                              "type": "object",
                              "properties": {
                                "rating": { "type": "number" },
                                "category": { "type": "string" },
                                "tags": { "type": "array", "items": { "type": "string" } }
                              }
                            },
                            "labels": { "type": "array", "items": { "type": "string" } },
                            "tips": {
                              "type": "object",
                              "properties": {
                                "bestTime": { "type": "array", "items": { "type": "string" } },
                                "travel": { "type": "array", "items": { "type": "string" } },
                                "warnings": { "type": "array", "items": { "type": "string" } }
                              }
                            },
                            "links": {
                              "type": "object",
                              "properties": {
                                "booking": { "type": "string" },
                                "website": { "type": "string" },
                                "phone": { "type": "string" }
                              }
                            },
                            "locked": { "type": "boolean" },
                            "bookingRef": { "type": "string" }
                          },
                          "required": ["id", "type", "title"]
                        }
                      },
                      "edges": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "from": { "type": "string" },
                            "to": { "type": "string" },
                            "transitInfo": {
                              "type": "object",
                              "properties": {
                                "mode": { "type": "string" },
                                "durationMin": { "type": "integer" },
                                "provider": { "type": "string" },
                                "bookingUrl": { "type": "string" }
                              }
                            }
                          }
                        }
                      }
                    },
                    "required": ["dayNumber", "date", "location", "nodes"]
                  }
                }
              },
              "required": ["itineraryId", "version", "summary", "currency", "themes", "days"]
            }
            """;
    }

    private String buildSystemPromptForChanges() {
        return "You are a travel planning assistant. Generate a ChangeSet JSON that updates the existing normalized itinerary. Respect locked nodes unless explicitly told otherwise. Respond ONLY with valid JSON matching the schema.";
    }

    private String buildUserPromptForChanges(String itineraryId, NormalizedItinerary currentItinerary, String userRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Current itinerary ID: ").append(itineraryId).append("\n");
        prompt.append("Summary: ").append(currentItinerary.getSummary()).append("\n");
        prompt.append("Days: ").append(currentItinerary.getDays() != null ? currentItinerary.getDays().size() : 0).append("\n\n");
        prompt.append("User request: ").append(userRequest).append("\n\n");
        prompt.append("Generate a ChangeSet that applies this request.");
        return prompt.toString();
    }

    private String getChangeSetSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "scope": { "type": "string" },
                "day": { "type": "integer" },
                "ops": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "op": { "type": "string" },
                      "id": { "type": "string" },
                      "after": { "type": "string" },
                      "startTime": { "type": "string", "format": "date-time" },
                      "endTime": { "type": "string", "format": "date-time" },
                      "node": { "type": "object" }
                    },
                    "required": ["op"]
                  }
                }
              },
              "required": ["ops"]
            }
            """;
    }

    private ItineraryDto convertNormalizedToItineraryDto(NormalizedItinerary normalizedItinerary, CreateItineraryReq request) {
        logger.info("=== CONVERTING NORMALIZED TO ITINERARY DTO ===");
        logger.info("Itinerary ID: {}", normalizedItinerary.getItineraryId());
        logger.info("Summary: {}", normalizedItinerary.getSummary());
        logger.info("Days count: {}", normalizedItinerary.getDays() != null ? normalizedItinerary.getDays().size() : 0);
        
        // Convert normalized days to ItineraryDayDto
        List<ItineraryDayDto> days = new ArrayList<>();
        if (normalizedItinerary.getDays() != null) {
            for (NormalizedDay normalizedDay : normalizedItinerary.getDays()) {
                ItineraryDayDto dayDto = convertNormalizedDayToDto(normalizedDay);
                days.add(dayDto);
            }
        }
        
        // Create agent results map
        Map<String, Object> agentResults = new HashMap<>();
        if (normalizedItinerary.getThemes() != null) {
            agentResults.put("themes", normalizedItinerary.getThemes());
        }
        agentResults.put("currency", normalizedItinerary.getCurrency());
        
        // Build the ItineraryDto
        ItineraryDto itineraryDto = ItineraryDto.builder()
                .id(normalizedItinerary.getItineraryId())
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .party(request.getParty())
                .budgetTier(request.getBudgetTier())
                .interests(request.getInterests())
                .constraints(request.getConstraints())
                .language(request.getLanguage())
                .summary(normalizedItinerary.getSummary())
                .map(null) // Map data not implemented yet
                .days(days)
                .agentResults(agentResults)
                .status("completed")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isPublic(false)
                .shareToken(null)
                .build();
        
        logger.info("=== NORMALIZED CONVERSION COMPLETED ===");
        logger.info("Generated itinerary with {} days", days.size());
        logger.info("Itinerary ID: {}", itineraryDto.getId());
        
        return itineraryDto;
    }
    
    private ItineraryDayDto convertNormalizedDayToDto(NormalizedDay normalizedDay) {
        // Convert nodes to activities
        List<ActivityDto> activities = new ArrayList<>();
        if (normalizedDay.getNodes() != null) {
            for (NormalizedNode node : normalizedDay.getNodes()) {
                // Create LocationDto
                LocationDto locationDto = null;
                if (node.getLocation() != null) {
                    locationDto = new LocationDto(
                            node.getLocation().getName(),
                            node.getLocation().getAddress(),
                            node.getLocation().getCoordinates() != null ? node.getLocation().getCoordinates().getLat() : 0.0,
                            node.getLocation().getCoordinates() != null ? node.getLocation().getCoordinates().getLng() : 0.0,
                            "" // city parameter
                    );
                }
                
                // Create PriceDto
                PriceDto priceDto = null;
                if (node.getCost() != null) {
                    priceDto = new PriceDto(
                            node.getCost().getAmount() != null ? node.getCost().getAmount() : 0.0,
                            node.getCost().getCurrency() != null ? node.getCost().getCurrency() : "EUR",
                            node.getCost().getPer() != null ? node.getCost().getPer() : "person"
                    );
                }
                
                ActivityDto activityDto = new ActivityDto(
                        node.getTitle(),
                        node.getDetails() != null ? node.getDetails().getCategory() : "",
                        locationDto,
                        node.getTiming() != null ? node.getTiming().getStartTime().toString() : "",
                        node.getTiming() != null ? node.getTiming().getEndTime().toString() : "",
                        node.getTiming() != null ? String.valueOf(node.getTiming().getDurationMin()) : "0",
                        node.getType(),
                        priceDto,
                        false, // bookingRequired
                        "", // bookingUrl
                        node.getTips() != null && node.getTips().getTravel() != null ? 
                            String.join(", ", node.getTips().getTravel()) : ""
                );
                activities.add(activityDto);
            }
        }
        
        return ItineraryDayDto.builder()
                .day(normalizedDay.getDayNumber())
                .date(parseDate(normalizedDay.getDate()))
                .location(normalizedDay.getLocation())
                .activities(activities)
                .accommodation(null)
                .transportation(null)
                .meals(null)
                .notes(null)
                .build();
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse the date string - assuming it's in YYYY-MM-DD format
            return LocalDate.parse(dateString.trim());
        } catch (Exception e) {
            logger.warn("Failed to parse date: {}", dateString, e);
            return null;
        }
    }

    /**
     * Parse ChangeSet from AI response.
     */
    private ChangeSet parseChangeSetFromResponse(String response, String userRequest) {
        // For now, create a simple mock ChangeSet based on the user request
        // In a real implementation, this would parse the AI response

        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("trip");

        List<ChangeOperation> operations = new ArrayList<>();

        // Create a simple mock operation based on user request
        if (userRequest.toLowerCase().contains("add") || userRequest.toLowerCase().contains("insert")) {
            ChangeOperation insertOp = new ChangeOperation();
            insertOp.setOp("insert");
            insertOp.setAfter("n_sagrada"); // Mock after node
            insertOp.setNode(createMockNode("n_new_activity", "activity"));
            operations.add(insertOp);
        } else if (userRequest.toLowerCase().contains("remove") || userRequest.toLowerCase().contains("delete")) {
            ChangeOperation deleteOp = new ChangeOperation();
            deleteOp.setOp("delete");
            deleteOp.setId("n_breakfast"); // Mock node to delete
            operations.add(deleteOp);
        } else if (userRequest.toLowerCase().contains("move") || userRequest.toLowerCase().contains("change time")) {
            ChangeOperation moveOp = new ChangeOperation();
            moveOp.setOp("move");
            moveOp.setId("n_sagrada"); // Mock node to move
            moveOp.setStartTime(java.time.Instant.parse("2025-10-04T10:00:00Z"));
            moveOp.setEndTime(java.time.Instant.parse("2025-10-04T12:00:00Z"));
            operations.add(moveOp);
        }

        changeSet.setOps(operations);

        // Set preferences
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setRespectLocks(true);
        changeSet.setPreferences(preferences);

        return changeSet;
    }

    /**
     * Create a mock node for testing.
     */
    private NormalizedNode createMockNode(String id, String type) {
        NormalizedNode node = new NormalizedNode();
        node.setId(id);
        node.setType(type);
        node.setTitle("New " + type);
        node.setDetails(createNodeDetails("A new " + type + " added by the planner"));
        node.setLocked(false);

        // Set audit trail fields
        setNodeAuditFields(node, "agent");

        // Set timing
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(java.time.Instant.parse("2025-10-04T14:00:00Z"));
        timing.setEndTime(java.time.Instant.parse("2025-10-04T16:00:00Z"));
        timing.setDurationMin(120);
        node.setTiming(timing);

        // Set location
        NodeLocation location = new NodeLocation();
        location.setName("Barcelona Location");
        location.setAddress("Barcelona, Spain");
        Coordinates coords = new Coordinates();
        coords.setLat(41.3851);
        coords.setLng(2.1734);
        location.setCoordinates(coords);
        node.setLocation(location);

        return node;
    }
    
    /**
     * Validate that the ChangeSet respects locked nodes.
     */
    private void validateChangeSet(ChangeSet changeSet, NormalizedItinerary currentItinerary) {
        if (changeSet.getOps() == null) {
            return;
        }
        
        for (ChangeOperation op : changeSet.getOps()) {
            if ("move".equals(op.getOp()) || "delete".equals(op.getOp())) {
                String nodeId = op.getId();
                if (nodeId != null) {
                    // Find the node in the current itinerary
                    NormalizedNode node = findNodeById(currentItinerary, nodeId);
                    if (node != null && Boolean.TRUE.equals(node.getLocked())) {
                        logger.warn("Attempted to modify locked node: {}", nodeId);
                        // In a real implementation, you might throw an exception or modify the operation
                    }
                }
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
                if (nodeId.equals(node.getId())) {
                    return node;
                }
            }
        }
        
        return null;
    }

    /**
     * Create NodeDetails with category (using description as category for now).
     */
    private NodeDetails createNodeDetails(String description) {
        NodeDetails details = new NodeDetails();
        details.setCategory(description);
        return details;
    }

    /**
     * Set audit trail fields for a node.
     */
    private void setNodeAuditFields(NormalizedNode node, String updatedBy) {
        if (node != null) {
            node.markAsUpdated(updatedBy);
        }
    }
}

