package com.tripplanner.service;

import com.tripplanner.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for orchestrating chat requests and routing them to appropriate handlers.
 * Coordinates intent classification, node resolution, and change set building.
 */
@Service
public class OrchestratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);
    
    private final IntentClassificationService intentClassificationService;
    private final NodeResolutionService nodeResolutionService;
    private final ChangeEngine changeEngine;
    private final ItineraryJsonService itineraryJsonService;
    
    public OrchestratorService(IntentClassificationService intentClassificationService,
                              NodeResolutionService nodeResolutionService,
                              ChangeEngine changeEngine,
                              ItineraryJsonService itineraryJsonService) {
        this.intentClassificationService = intentClassificationService;
        this.nodeResolutionService = nodeResolutionService;
        this.changeEngine = changeEngine;
        this.itineraryJsonService = itineraryJsonService;
    }
    
    /**
     * Main orchestration method for chat requests.
     */
    public ChatResponse route(ChatRequest request) {
        logger.info("Routing chat request: {}", request);
        
        try {
            // Step 1: Classify intent
            IntentResult intent = classifyIntent(request.getText(), request.getSelectedNodeId(), request.getDay());
            logger.debug("Classified intent: {}", intent);
            
            // Step 2: Handle disambiguation if needed
            if (intent.requiresNodeResolution() && (request.getSelectedNodeId() == null || request.getSelectedNodeId().isEmpty())) {
                return handleNodeDisambiguation(request, intent);
            }
            
            // Step 3: Build change set for change intents
            if (intent.isChangeIntent()) {
                return handleChangeIntent(request, intent);
            }
            
            // Step 4: Handle explain intent
            if ("EXPLAIN".equals(intent.getIntent())) {
                return handleExplainIntent(request);
            }
            
            // Step 5: Handle error case
            return ChatResponse.error("Unable to process request", List.of("Unknown intent"));
            
        } catch (Exception e) {
            logger.error("Error routing chat request", e);
            return ChatResponse.error("An error occurred while processing your request", List.of(e.getMessage()));
        }
    }
    
    /**
     * Classify the intent from the request.
     */
    private IntentResult classifyIntent(String text, String selectedNodeId, Integer day) {
        return intentClassificationService.classifyIntent(text, selectedNodeId, day);
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
}
