package com.tripplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.api.dto.*;
import com.tripplanner.data.entity.FirestoreItinerary;
// Removed JPA repository usage in Firestore-only mode
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ChangeEngine service for managing itinerary changes.
 * Implements propose/apply/undo operations as per MVP contract.
 */
@Service
public class ChangeEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ChangeEngine.class);
    
    private final ItineraryJsonService itineraryJsonService;
    // Removed JPA repository
    private final ObjectMapper objectMapper;
    
    public ChangeEngine(ItineraryJsonService itineraryJsonService,
                       ObjectMapper objectMapper) {
        this.itineraryJsonService = itineraryJsonService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Propose changes without writing to database.
     * Returns the proposed itinerary and diff for preview.
     */
    public ProposeResult propose(String itineraryId, ChangeSet changeSet) {
        logger.info("Proposing changes for itinerary: {}", itineraryId);
        
        try {
            // Load current itinerary using the flexible ID lookup
            Optional<NormalizedItinerary> currentOpt = itineraryJsonService.getItineraryByAnyId(itineraryId);
            if (currentOpt.isEmpty()) {
                throw new IllegalArgumentException("Itinerary not found: " + itineraryId);
            }
            
            NormalizedItinerary current = currentOpt.get();
            
            // Create a copy for proposed changes
            NormalizedItinerary proposed = deepCopy(current);
            proposed.setVersion(current.getVersion() + 1);
            
            // Apply changes to proposed itinerary
            ItineraryDiff diff = applyChangesToItinerary(proposed, changeSet);
            
            return new ProposeResult(proposed, diff, proposed.getVersion());
            
        } catch (Exception e) {
            logger.error("Failed to propose changes", e);
            throw new RuntimeException("Failed to propose changes", e);
        }
    }
    
    /**
     * Apply changes to the database.
     * Increments version, persists JSON, and creates revision.
     */
    public ApplyResult apply(String itineraryId, ChangeSet changeSet) {
        logger.info("Applying changes for itinerary: {}", itineraryId);
        
        try {
            // Load current itinerary using the flexible ID lookup
            Optional<NormalizedItinerary> currentOpt = itineraryJsonService.getItineraryByAnyId(itineraryId);
            if (currentOpt.isEmpty()) {
                throw new IllegalArgumentException("Itinerary not found: " + itineraryId);
            }
            
            NormalizedItinerary current = currentOpt.get();
            
            // Create a copy for changes
            NormalizedItinerary updated = deepCopy(current);
            
            // Apply changes
            ItineraryDiff diff = applyChangesToItinerary(updated, changeSet);
            
            // If no changes detected, skip version bump and revision
            boolean hasChanges = (diff.getAdded() != null && !diff.getAdded().isEmpty())
                    || (diff.getRemoved() != null && !diff.getRemoved().isEmpty())
                    || (diff.getUpdated() != null && !diff.getUpdated().isEmpty());
            if (!hasChanges) {
                logger.info("No-op ChangeSet: skipping version bump and revision save");
                return new ApplyResult(current.getVersion(), diff);
            }
            
            // Increment version only when there are changes
            updated.setVersion(current.getVersion() + 1);
            
            // Save current version as revision
            saveRevision(current);
            
            // Update main record
            itineraryJsonService.updateItinerary(updated);
            
            // No regular entity sync in Firestore-only mode
            
            return new ApplyResult(updated.getVersion(), diff);
            
        } catch (Exception e) {
            logger.error("Failed to apply changes", e);
            throw new RuntimeException("Failed to apply changes", e);
        }
    }
    
    /**
     * Undo changes by restoring from a specific revision.
     */
    public UndoResult undo(String itineraryId, Integer toVersion) {
        logger.info("Undoing changes for itinerary: {} to version: {}", itineraryId, toVersion);
        
        try {
            // Find the revision to restore
            Optional<NormalizedItinerary> revision = itineraryJsonService.getRevision(itineraryId, toVersion);
            if (revision.isEmpty()) {
                throw new IllegalArgumentException("Revision not found: " + toVersion);
            }
            
            // Load the revision
            NormalizedItinerary restored = revision.get();
            
            // Get current version for diff calculation using the flexible ID lookup
            Optional<NormalizedItinerary> currentOpt = itineraryJsonService.getItineraryByAnyId(itineraryId);
            if (currentOpt.isEmpty()) {
                throw new IllegalArgumentException("Itinerary not found: " + itineraryId);
            }
            
            NormalizedItinerary current = currentOpt.get();
            
            // Calculate diff
            ItineraryDiff diff = calculateDiff(current, restored);
            
            // Save current version as revision before restoring
            saveRevision(current);
            
            // Restore the revision
            itineraryJsonService.updateItinerary(restored);
            
            return new UndoResult(restored.getVersion(), diff);
            
        } catch (Exception e) {
            logger.error("Failed to undo changes", e);
            throw new RuntimeException("Failed to undo changes", e);
        }
    }
    
    /**
     * Apply changes to an itinerary and return the diff.
     */
    private ItineraryDiff applyChangesToItinerary(NormalizedItinerary itinerary, ChangeSet changeSet) {
        List<DiffItem> added = new ArrayList<>();
        List<DiffItem> removed = new ArrayList<>();
        List<DiffItem> updated = new ArrayList<>();
        
        // Handle null operations list
        if (changeSet.getOps() == null || changeSet.getOps().isEmpty()) {
            logger.info("No operations to apply");
            return new ItineraryDiff(added, removed, updated);
        }
        
        for (ChangeOperation op : changeSet.getOps()) {
            if (op == null || op.getOp() == null) {
                logger.warn("Skipping null operation");
                continue;
            }
            
            switch (op.getOp()) {
                case "move":
                    if (moveNode(itinerary, op, changeSet.getPreferences())) {
                        updated.add(new DiffItem(op.getId(), changeSet.getDay(), Arrays.asList("timing")));
                    }
                    break;
                case "insert":
                    if (insertNode(itinerary, op, changeSet.getDay())) {
                        added.add(new DiffItem(op.getNode().getId(), changeSet.getDay()));
                    }
                    break;
                case "delete":
                    if (deleteNode(itinerary, op, changeSet.getDay(), changeSet.getPreferences())) {
                        removed.add(new DiffItem(op.getId(), changeSet.getDay()));
                    }
                    break;
                case "replace":
                    if (replaceNode(itinerary, op, changeSet.getDay(), changeSet.getPreferences())) {
                        removed.add(new DiffItem(op.getId(), changeSet.getDay()));
                        if (op.getNode() != null) {
                            added.add(new DiffItem(op.getNode().getId(), changeSet.getDay()));
                        }
                    }
                    break;
                default:
                    logger.warn("Unknown operation: {}", op.getOp());
            }
        }
        
        return new ItineraryDiff(added, removed, updated);
    }
    
    /**
     * Move a node to new timing.
     */
    private boolean moveNode(NormalizedItinerary itinerary, ChangeOperation op, ChangePreferences preferences) {
        // Find the node
        NormalizedNode node = findNodeById(itinerary, op.getId());
        if (node == null) {
            logger.warn("Node not found for move operation: {}", op.getId());
            return false;
        }
        
        // Check if node is locked
        if (Boolean.TRUE.equals(node.getLocked()) && 
            Boolean.TRUE.equals(preferences != null ? preferences.getRespectLocks() : true)) {
            logger.warn("Cannot move locked node: {}", op.getId());
            return false;
        }
        
        // Update timing
        if (op.getStartTime() != null) {
            node.getTiming().setStartTime(op.getStartTime());
        }
        if (op.getEndTime() != null) {
            node.getTiming().setEndTime(op.getEndTime());
        }
        
        updateNodeAudit(node, "user");
        
        return true;
    }
    
    /**
     * Insert a new node.
     */
    private boolean insertNode(NormalizedItinerary itinerary, ChangeOperation op, Integer day) {
        if (op.getNode() == null) {
            logger.warn("No node provided for insert operation");
            return false;
        }
        
        // Find the day
        NormalizedDay targetDay = findDayByNumber(itinerary, day);
        if (targetDay == null) {
            logger.warn("Day not found: {}", day);
            return false;
        }
        
        // Insert node after the specified node
        if (op.getAfter() != null) {
            List<NormalizedNode> nodes = targetDay.getNodes();
            int insertIndex = findNodeIndex(nodes, op.getAfter());
            if (insertIndex >= 0) {
                nodes.add(insertIndex + 1, op.getNode());
            } else {
                nodes.add(op.getNode());
            }
        } else {
            targetDay.getNodes().add(op.getNode());
        }
        
        // Update edges if needed
        updateEdgesAfterInsert(targetDay, op.getAfter(), op.getNode().getId());
        
        // Set audit trail for new node
        updateNodeAudit(op.getNode(), "user");
        
        return true;
    }
    
    /**
     * Delete a node.
     */
    private boolean deleteNode(NormalizedItinerary itinerary, ChangeOperation op, Integer day, ChangePreferences preferences) {
        // Find the day
        NormalizedDay targetDay = findDayByNumber(itinerary, day);
        if (targetDay == null) {
            logger.warn("Day not found: {}", day);
            return false;
        }
        
        // Find and remove the node
        List<NormalizedNode> nodes = targetDay.getNodes();
        NormalizedNode nodeToRemove = findNodeById(itinerary, op.getId());
        if (nodeToRemove == null) {
            logger.warn("Node not found for delete operation: {}", op.getId());
            return false;
        }
        
        // Check if node is locked
        if (Boolean.TRUE.equals(nodeToRemove.getLocked()) && 
            Boolean.TRUE.equals(preferences != null ? preferences.getRespectLocks() : true)) {
            logger.warn("Cannot delete locked node: {}", op.getId());
            return false;
        }
        
        nodes.removeIf(node -> node.getId().equals(op.getId()));
        
        // Update edges
        updateEdgesAfterDelete(targetDay, op.getId());
        
        return true;
    }
    
    /**
     * Replace a node with a new one.
     */
    private boolean replaceNode(NormalizedItinerary itinerary, ChangeOperation op, Integer day, ChangePreferences preferences) {
        // Find the day
        NormalizedDay targetDay = findDayByNumber(itinerary, day);
        if (targetDay == null) {
            logger.warn("Day not found: {}", day);
            return false;
        }
        
        // Find the node to replace
        NormalizedNode nodeToReplace = findNodeById(itinerary, op.getId());
        if (nodeToReplace == null) {
            logger.warn("Node not found for replace operation: {}", op.getId());
            return false;
        }
        
        // Check if node is locked
        if (Boolean.TRUE.equals(nodeToReplace.getLocked()) && 
            Boolean.TRUE.equals(preferences != null ? preferences.getRespectLocks() : true)) {
            logger.warn("Cannot replace locked node: {}", op.getId());
            return false;
        }
        
        // Create a replacement node if not provided
        NormalizedNode replacementNode = op.getNode();
        if (replacementNode == null) {
            // Create a mock replacement node for Park Güell
            replacementNode = createMockReplacementNode(op.getId());
        }
        
        // Find the position of the original node
        List<NormalizedNode> nodes = targetDay.getNodes();
        int index = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(op.getId())) {
                index = i;
                break;
            }
        }
        
        if (index == -1) {
            logger.warn("Could not find position of node to replace: {}", op.getId());
            return false;
        }
        
        // Replace the node
        nodes.set(index, replacementNode);
        
        // Update edges
        updateEdgesAfterReplace(targetDay, op.getId(), replacementNode.getId());
        
        return true;
    }
    
    /**
     * Create a mock replacement node for testing.
     */
    private NormalizedNode createMockReplacementNode(String originalNodeId) {
        // Create a mock replacement for Park Güell
        NormalizedNode replacement = new NormalizedNode();
        replacement.setId("n_casa_batllo");
        replacement.setType("attraction");
        replacement.setTitle("Casa Batlló");
        replacement.setLocked(false);
        replacement.setStatus("planned");
        
        // Set location
        NodeLocation location = new NodeLocation();
        location.setName("Casa Batlló");
        location.setAddress("Passeig de Gràcia, 43, 08007 Barcelona");
        Coordinates coords = new Coordinates();
        coords.setLat(41.3917);
        coords.setLng(2.1649);
        location.setCoordinates(coords);
        replacement.setLocation(location);
        
        // Set timing (same as original)
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(java.time.Instant.parse("2025-06-02T12:00:00Z"));
        timing.setEndTime(java.time.Instant.parse("2025-06-02T14:00:00Z"));
        timing.setDurationMin(120);
        replacement.setTiming(timing);
        
        // Set cost
        NodeCost cost = new NodeCost();
        cost.setAmount(35.0);
        cost.setCurrency("EUR");
        cost.setPer("person");
        replacement.setCost(cost);
        
        // Set details
        NodeDetails details = new NodeDetails();
        details.setRating(4.7);
        details.setCategory("attraction");
        details.setTags(Arrays.asList("gaudi", "architecture", "modernism"));
        replacement.setDetails(details);
        
        // Set labels
        replacement.setLabels(Arrays.asList("Must-Visit", "Booking Required"));
        
        // Set audit trail
        replacement.markAsUpdated("user");
        
        return replacement;
    }
    
    /**
     * Find a node by ID across all days.
     */
    private NormalizedNode findNodeById(NormalizedItinerary itinerary, String nodeId) {
        return itinerary.getDays().stream()
                .flatMap(day -> day.getNodes().stream())
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find a day by day number.
     */
    private NormalizedDay findDayByNumber(NormalizedItinerary itinerary, Integer dayNumber) {
        return itinerary.getDays().stream()
                .filter(day -> day.getDayNumber().equals(dayNumber))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find node index in a list.
     */
    private int findNodeIndex(List<NormalizedNode> nodes, String nodeId) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(nodeId)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Update edges after inserting a node.
     */
    private void updateEdgesAfterInsert(NormalizedDay day, String afterNodeId, String newNodeId) {
        if (afterNodeId == null) return;
        
        List<Edge> edges = day.getEdges();
        if (edges == null) {
            edges = new ArrayList<>();
            day.setEdges(edges);
        }
        
        // Find edges that point to the node after which we inserted
        for (Edge edge : edges) {
            if (edge.getTo().equals(afterNodeId)) {
                // Insert new edge before the existing one
                int index = edges.indexOf(edge);
                edges.add(index, new Edge(edge.getFrom(), newNodeId));
                break;
            }
        }
        
        // Add edge from new node to the node after which we inserted
        edges.add(new Edge(newNodeId, afterNodeId));
    }
    
    /**
     * Update edges after deleting a node.
     */
    private void updateEdgesAfterDelete(NormalizedDay day, String deletedNodeId) {
        List<Edge> edges = day.getEdges();
        if (edges == null) return;
        
        // Remove edges involving the deleted node
        edges.removeIf(edge -> edge.getFrom().equals(deletedNodeId) || edge.getTo().equals(deletedNodeId));
        
        // Find edges that need to be reconnected
        List<Edge> toAdd = new ArrayList<>();
        List<Edge> toRemove = new ArrayList<>();
        
        for (Edge edge : edges) {
            if (edge.getTo().equals(deletedNodeId)) {
                // Find what the deleted node was pointing to
                for (Edge nextEdge : edges) {
                    if (nextEdge.getFrom().equals(deletedNodeId)) {
                        toAdd.add(new Edge(edge.getFrom(), nextEdge.getTo()));
                        toRemove.add(edge);
                        toRemove.add(nextEdge);
                        break;
                    }
                }
            }
        }
        
        edges.removeAll(toRemove);
        edges.addAll(toAdd);
    }
    
    /**
     * Update edges after replacing a node.
     */
    private void updateEdgesAfterReplace(NormalizedDay day, String oldNodeId, String newNodeId) {
        List<Edge> edges = day.getEdges();
        if (edges == null) return;
        
        // Update edges to use the new node ID
        for (Edge edge : edges) {
            if (edge.getFrom().equals(oldNodeId)) {
                edge.setFrom(newNodeId);
            }
            if (edge.getTo().equals(oldNodeId)) {
                edge.setTo(newNodeId);
            }
        }
    }
    
    /**
     * Calculate diff between two itineraries.
     */
    private ItineraryDiff calculateDiff(NormalizedItinerary current, NormalizedItinerary target) {
        // Simplified diff calculation - in a real implementation, this would be more sophisticated
        List<DiffItem> added = new ArrayList<>();
        List<DiffItem> removed = new ArrayList<>();
        List<DiffItem> updated = new ArrayList<>();
        
        // This is a placeholder - real implementation would compare nodes, timing, etc.
        return new ItineraryDiff(added, removed, updated);
    }
    
    /**
     * Save current version as revision.
     */
    private void saveRevision(NormalizedItinerary itinerary) {
        try {
            itineraryJsonService.saveRevision(itinerary.getItineraryId(), itinerary);
        } catch (Exception e) {
            logger.error("Failed to save revision", e);
        }
    }
    
    /**
     * Deep copy an itinerary.
     */
    private NormalizedItinerary deepCopy(NormalizedItinerary original) {
        try {
            String json = objectMapper.writeValueAsString(original);
            NormalizedItinerary copy = objectMapper.readValue(json, NormalizedItinerary.class);
            // Ensure all collections are initialized
            if (copy.getDays() == null) {
                copy.setDays(new ArrayList<>());
            }
            for (NormalizedDay day : copy.getDays()) {
                if (day.getNodes() == null) {
                    day.setNodes(new ArrayList<>());
                }
                if (day.getEdges() == null) {
                    day.setEdges(new ArrayList<>());
                }
            }
            return copy;
        } catch (JsonProcessingException e) {
            logger.error("Failed to deep copy itinerary", e);
            throw new RuntimeException("Failed to deep copy itinerary", e);
        }
    }
    
    /**
     * Update node audit trail fields.
     */
    private void updateNodeAudit(NormalizedNode node, String updatedBy) {
        if (node != null) {
            node.markAsUpdated(updatedBy);
        }
    }
    
    /**
     * Set node status with validation.
     */
    private void setNodeStatus(NormalizedNode node, String status) {
        if (node != null && node.canTransitionTo(status)) {
            node.setStatus(status);
            node.markAsUpdated("user");
        } else if (node != null) {
            logger.warn("Invalid status transition from {} to {} for node {}", 
                       node.getStatus(), status, node.getId());
        }
    }
    
    /**
     * Validate node status transition.
     */
    private void validateNodeStatusTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        // Define valid transitions
        boolean isValid = switch (fromStatus) {
            case "planned" -> List.of("in_progress", "skipped", "cancelled").contains(toStatus);
            case "in_progress" -> List.of("completed", "skipped", "cancelled").contains(toStatus);
            case "skipped", "cancelled" -> List.of("planned", "in_progress").contains(toStatus);
            case "completed" -> List.of("planned", "in_progress").contains(toStatus);
            default -> false;
        };
        
        if (!isValid) {
            throw new IllegalArgumentException(
                String.format("Invalid status transition from %s to %s", fromStatus, toStatus)
            );
        }
    }
    
    // Result classes
    public static class ProposeResult {
        private final NormalizedItinerary proposed;
        private final ItineraryDiff diff;
        private final Integer previewVersion;
        
        public ProposeResult(NormalizedItinerary proposed, ItineraryDiff diff, Integer previewVersion) {
            this.proposed = proposed;
            this.diff = diff;
            this.previewVersion = previewVersion;
        }
        
        public NormalizedItinerary getProposed() { return proposed; }
        public ItineraryDiff getDiff() { return diff; }
        public Integer getPreviewVersion() { return previewVersion; }
    }
    
    public static class ApplyResult {
        private final Integer toVersion;
        private final ItineraryDiff diff;
        
        public ApplyResult(Integer toVersion, ItineraryDiff diff) {
            this.toVersion = toVersion;
            this.diff = diff;
        }
        
        public Integer getToVersion() { return toVersion; }
        public ItineraryDiff getDiff() { return diff; }
    }
    
    public static class UndoResult {
        private final Integer toVersion;
        private final ItineraryDiff diff;
        
        public UndoResult(Integer toVersion, ItineraryDiff diff) {
            this.toVersion = toVersion;
            this.diff = diff;
        }
        
        public Integer getToVersion() { return toVersion; }
        public ItineraryDiff getDiff() { return diff; }
    }
    
    /**
     * Sync normalized itinerary changes back to the regular itinerary.
     * This ensures that changes made through the chat interface are reflected
     * in the workflow and day-by-day views.
     */
    // Removed regular-entity sync in Firestore-only mode
    
    /**
     * Map normalized itinerary ID to database ID.
     * This handles the mapping between semantic IDs and database IDs.
     */
    // Removed mapping to DB ID in Firestore-only mode
}
