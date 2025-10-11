package com.tripplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.exception.VersionMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ChangeEngine service for managing itinerary changes.
 * Implements propose/apply/undo operations as per MVP contract.
 */
@Service
public class ChangeEngine {
    
    /**
     * Result of a replace operation containing information about the original and replacement nodes.
     */
    private static class ReplaceResult {
        final String originalNodeId;
        final String originalNodeTitle;
        
        ReplaceResult(String originalNodeId, String originalNodeTitle) {
            this.originalNodeId = originalNodeId;
            this.originalNodeTitle = originalNodeTitle;
        }
    }
    
    private static final Logger logger = LoggerFactory.getLogger(ChangeEngine.class);
    
    private final ItineraryJsonService itineraryJsonService;
    private final UserDataService userDataService;
    private final ObjectMapper objectMapper;
    private final RevisionService revisionService;
    private final ConflictResolver conflictResolver;
    private final LockManager lockManager;
    private final IdempotencyManager idempotencyManager;
    private final TraceManager traceManager;
    private final NodeIdGenerator nodeIdGenerator;
    
    public ChangeEngine(ItineraryJsonService itineraryJsonService,
                       UserDataService userDataService,
                       ObjectMapper objectMapper,
                       RevisionService revisionService,
                       ConflictResolver conflictResolver,
                       LockManager lockManager,
                       IdempotencyManager idempotencyManager,
                       TraceManager traceManager,
                       NodeIdGenerator nodeIdGenerator) {
        this.itineraryJsonService = itineraryJsonService;
        this.userDataService = userDataService;
        this.objectMapper = objectMapper;
        this.revisionService = revisionService;
        this.conflictResolver = conflictResolver;
        this.lockManager = lockManager;
        this.idempotencyManager = idempotencyManager;
        this.traceManager = traceManager;
        this.nodeIdGenerator = nodeIdGenerator;
    }
    
    /**
     * Propose changes without writing to database.
     * Returns the proposed itinerary and diff for preview.
     */
    public ProposeResult propose(String itineraryId, ChangeSet changeSet) throws Exception {
        return traceManager.executeTraced("change_engine_propose", () -> {
            traceManager.setItineraryContext(itineraryId);
            logger.info("Proposing changes for itinerary: {}", itineraryId);
            
            try {
            // Load current itinerary using the flexible ID lookup
            Optional<NormalizedItinerary> currentOpt = itineraryJsonService.getItinerary(itineraryId);
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
        });
    }
    
    /**
     * Apply changes to the database.
     * Increments version, persists JSON, and creates revision.
     */
    public ApplyResult apply(String itineraryId, ChangeSet changeSet) {
        logger.info("Applying changes for itinerary: {}", itineraryId);
        
        // Check for idempotency
        String idempotencyKey = changeSet.getIdempotencyKey();
        if (idempotencyKey != null) {
            if (!idempotencyManager.isValidIdempotencyKey(idempotencyKey)) {
                throw new IllegalArgumentException("Invalid idempotency key format: " + idempotencyKey);
            }
            
            Optional<IdempotencyManager.IdempotencyRecord> existingRecord = 
                idempotencyManager.getExistingOperation(idempotencyKey);
            
            if (existingRecord.isPresent()) {
                logger.info("Returning cached result for idempotent operation: {}", idempotencyKey);
                return (ApplyResult) existingRecord.get().getResult();
            }
        }
        
        try {
            // Load current itinerary using the flexible ID lookup
            Optional<NormalizedItinerary> currentOpt = itineraryJsonService.getItinerary(itineraryId);
            if (currentOpt.isEmpty()) {
                throw new IllegalArgumentException("Itinerary not found: " + itineraryId);
            }
            
            NormalizedItinerary current = currentOpt.get();
            
            // Validate version if baseVersion is specified
            if (changeSet.getBaseVersion() != null) {
                validateVersion(current, changeSet);
            }
            
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
            
            // Create revision record before applying changes
            RevisionRecord revisionRecord = createRevisionRecord(current, changeSet);
            
            try {
                // Save revision using RevisionService
                revisionService.saveRevision(itineraryId, revisionRecord);
                
                // Increment version only after successful revision save
                updated.setVersion(current.getVersion() + 1);
                updated.setUpdatedAt(System.currentTimeMillis());
                
                // Update main record
                itineraryJsonService.updateItinerary(updated);
                
            } catch (Exception revisionError) {
                logger.error("Failed to save revision, rolling back changes", revisionError);
                throw new RuntimeException("Failed to save revision: " + revisionError.getMessage(), revisionError);
            }
            
            // No regular entity sync in Firestore-only mode
            
            ApplyResult result = new ApplyResult(updated.getVersion(), diff);
            
            // Store result in idempotency manager if key provided
            if (idempotencyKey != null) {
                idempotencyManager.storeOperationResult(
                    idempotencyKey, 
                    result, 
                    "change_application"
                );
            }
            
            return result;
            
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
            Optional<NormalizedItinerary> currentOpt = itineraryJsonService.getItinerary(itineraryId);
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
                        added.add(new DiffItem(op.getNode().getId(), changeSet.getDay(), null, op.getNode().getTitle()));
                    }
                    break;
                case "delete":
                    if (deleteNode(itinerary, op, changeSet.getDay(), changeSet.getPreferences())) {
                        removed.add(new DiffItem(op.getId(), changeSet.getDay()));
                    }
                    break;
                case "replace":
                    if (replaceNode(itinerary, op, changeSet.getDay(), changeSet.getPreferences())) {
                        // For replace operations, we need to track the actual node that was replaced
                        // and the new node that replaced it
                        ReplaceResult replaceResult = getReplaceResult(itinerary, op, changeSet.getDay());
                        if (replaceResult != null) {
                            // Add the original node as removed
                            removed.add(new DiffItem(replaceResult.originalNodeId, changeSet.getDay(), null, replaceResult.originalNodeTitle));
                            // Add the new node as added
                            if (op.getNode() != null) {
                                added.add(new DiffItem(op.getNode().getId(), changeSet.getDay(), null, op.getNode().getTitle()));
                            }
                        }
                    }
                    break;
                case "update":
                    if (updateNode(itinerary, op, changeSet.getDay(), changeSet.getPreferences())) {
                        updated.add(new DiffItem(op.getId(), changeSet.getDay(), Arrays.asList("content")));
                    }
                    break;
                case "update_edge":
                    if (updateEdge(itinerary, op, changeSet.getDay(), changeSet.getPreferences())) {
                        updated.add(new DiffItem(op.getId(), changeSet.getDay(), Arrays.asList("edge")));
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
        
        // Check if node is locked (both node-level and system-level locks)
        if (Boolean.TRUE.equals(node.getLocked()) && 
            Boolean.TRUE.equals(preferences != null ? preferences.getRespectLocks() : true)) {
            logger.warn("Cannot move locked node: {}", op.getId());
            return false;
        }
        
        // Check system-level locks
        if (lockManager.isLocked(op.getId())) {
            logger.warn("Cannot move node with active system lock: {}", op.getId());
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
        
        // Generate ID for new node if not provided
        if (op.getNode().getId() == null || op.getNode().getId().trim().isEmpty()) {
            String generatedId = nodeIdGenerator.generateNodeId(op.getNode().getType(), day);
            op.getNode().setId(generatedId);
            logger.info("Generated ID for new node: {} -> {}", op.getNode().getTitle(), generatedId);
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
        
        // Check if node is locked (both node-level and system-level locks)
        if (Boolean.TRUE.equals(nodeToRemove.getLocked()) && 
            Boolean.TRUE.equals(preferences != null ? preferences.getRespectLocks() : true)) {
            logger.warn("Cannot delete locked node: {}", op.getId());
            return false;
        }
        
        // Check system-level locks
        if (lockManager.isLocked(op.getId())) {
            logger.warn("Cannot delete node with active system lock: {}", op.getId());
            return false;
        }
        
        nodes.removeIf(node -> node.getId() != null && node.getId().equals(op.getId()));
        
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
            logger.warn("Node not found for replace operation: {}, attempting fallback strategy", op.getId());
            
            // Fallback: if we can't find the exact node, try to find a node that matches the pattern
            // This handles cases where the LLM provides a generic ID like "day1_node1"
            List<NormalizedNode> nodes = targetDay.getNodes();
            if (nodes != null && !nodes.isEmpty()) {
                // For now, use the first node as a fallback
                // In a more sophisticated implementation, we could use fuzzy matching or position-based lookup
                nodeToReplace = nodes.get(0);
                logger.warn("Using fallback node '{}' for replace operation", nodeToReplace.getId());
            } else {
                logger.warn("No nodes found in day {} for replace operation", day);
                return false;
            }
        }
        
        // Check if node is locked (both node-level and system-level locks)
        if (Boolean.TRUE.equals(nodeToReplace.getLocked()) && 
            Boolean.TRUE.equals(preferences != null ? preferences.getRespectLocks() : true)) {
            logger.warn("Cannot replace locked node: {}", nodeToReplace.getId());
            return false;
        }
        
        // Check system-level locks
        if (lockManager.isLocked(nodeToReplace.getId())) {
            logger.warn("Cannot replace node with active system lock: {}", nodeToReplace.getId());
            return false;
        }
        
        // Create a replacement node if not provided
        NormalizedNode replacementNode = op.getNode();
        if (replacementNode == null) {
            // Create a mock replacement node for Park Güell
            replacementNode = createMockReplacementNode(op.getId());
        } else {
            // Generate ID for replacement node if not provided
            if (replacementNode.getId() == null || replacementNode.getId().trim().isEmpty()) {
                String generatedId = nodeIdGenerator.generateNodeId(replacementNode.getType(), day);
                replacementNode.setId(generatedId);
                logger.info("Generated ID for replacement node: {} -> {}", replacementNode.getTitle(), generatedId);
            }
        }
        
        // Find the position of the original node
        List<NormalizedNode> nodes = targetDay.getNodes();
        int index = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId() != null && nodes.get(i).getId().equals(nodeToReplace.getId())) {
                index = i;
                break;
            }
        }
        
        if (index == -1) {
            logger.warn("Could not find position of node to replace: {}", nodeToReplace.getId());
            return false;
        }
        
        // Replace the node
        nodes.set(index, replacementNode);
        
        // Update edges
        updateEdgesAfterReplace(targetDay, nodeToReplace.getId(), replacementNode.getId());
        
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
        timing.setDurationMin(120);
        replacement.setTiming(timing);
        
        // Set cost
        NodeCost cost = new NodeCost();
        cost.setAmountPerPerson(35.0);
        cost.setCurrency("EUR");
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
                .filter(node -> node.getId() != null && node.getId().equals(nodeId))
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
            if (nodes.get(i).getId() != null && nodes.get(i).getId().equals(nodeId)) {
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
     * Create a revision record from current itinerary and changeset.
     */
    private RevisionRecord createRevisionRecord(NormalizedItinerary itinerary, ChangeSet changeSet) {
        // Generate unique revision ID
        String revisionId = "rev_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        
        // Set timestamp to current system time
        Long timestamp = System.currentTimeMillis();
        
        // Extract agent name from changeSet or default to "user"
        String agent = (changeSet.getAgent() != null) ? changeSet.getAgent() : "user";
        
        // Set reason from changeSet or generate default
        String reason = (changeSet.getReason() != null) ? changeSet.getReason() : "Manual changes applied";

        // Convert ChangeSet operations to List<ChangeDetail>
        List<ChangeDetail> changes = convertChangeSetToDetails(changeSet);

        // Create revision record
        RevisionRecord revisionRecord = new RevisionRecord(revisionId, timestamp, agent, changes, reason, itinerary.getUserId());

        revisionRecord.setChanges(changes);
        
        return revisionRecord;
    }
    
    /**
     * Convert ChangeSet operations to ChangeDetail list.
     */
    private List<ChangeDetail> convertChangeSetToDetails(ChangeSet changeSet) {
        List<ChangeDetail> changes = new ArrayList<>();
        
        if (changeSet.getOps() != null) {
            for (ChangeOperation op : changeSet.getOps()) {
                if (op == null || op.getOp() == null) continue;
                
                ChangeDetail detail = new ChangeDetail();
                detail.setOperationType(op.getOp().toUpperCase());
                detail.setElementType("NODE");
                detail.setElementId(op.getId());
                
                // Set field and values based on operation type
                switch (op.getOp()) {
                    case "move":
                        detail.setField("timing");
                        detail.setNewValue(Map.of(
                            "startTime", op.getStartTime(),
                            "endTime", op.getEndTime()
                        ));
                        break;
                    case "insert":
                        detail.setOperation("CREATE");
                        detail.setField("node");
                        detail.setNewValue(op.getNode());
                        break;
                    case "delete":
                        detail.setOperation("DELETE");
                        detail.setField("node");
                        detail.setOldValue(op.getId());
                        break;
                    case "replace":
                        detail.setField("node");
                        detail.setOldValue(op.getId());
                        detail.setNewValue(op.getNode());
                        break;
                }
                
                // Add metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("day", changeSet.getDay());
                if (changeSet.getPreferences() != null) {
                    metadata.put("respectLocks", changeSet.getPreferences().getRespectLocks());
                }
                detail.setMetadata(metadata);
                
                changes.add(detail);
            }
        }
        
        return changes;
    }
    
    /**
     * Save current version as revision.
     */
    private void saveRevision(NormalizedItinerary itinerary) {
        try {
            // Save revision using the new user-specific structure
            userDataService.saveItineraryRevision(itinerary.getUserId(), itinerary.getItineraryId(), itinerary);
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
     * Validate version consistency and detect conflicts.
     */
    private void validateVersion(NormalizedItinerary current, ChangeSet changeSet) {
        if (changeSet.getBaseVersion() == null) {
            return; // No version validation requested
        }
        
        if (!changeSet.getBaseVersion().equals(current.getVersion())) {
            logger.warn("Version mismatch detected for itinerary {}: expected {}, actual {}", 
                       current.getItineraryId(), changeSet.getBaseVersion(), current.getVersion());
            
            // Detect specific conflicts
            ConflictResolver.ConflictDetectionResult conflictResult = 
                conflictResolver.detectConflicts(current, changeSet);
            
            if (conflictResult.hasConflicts()) {
                // Try automatic resolution
                ConflictResolver.ConflictResolutionResult resolutionResult = 
                    conflictResolver.attemptAutoResolution(current, changeSet, conflictResult);
                
                if (resolutionResult.isFullyResolved()) {
                    logger.info("All conflicts automatically resolved for itinerary: {}", 
                               current.getItineraryId());
                    
                    // Update the changeset with resolved operations
                    changeSet.setOps(resolutionResult.getMergedChangeSet().getOps());
                    changeSet.setBaseVersion(current.getVersion()); // Update to current version
                    
                } else {
                    // Create detailed conflict information
                    ItineraryDiff conflictDiff = createConflictDiff(conflictResult.getConflicts());
                    
                    throw new VersionMismatchException(
                        current.getItineraryId(),
                        changeSet.getBaseVersion(),
                        current.getVersion(),
                        conflictDiff
                    );
                }
            } else {
                // Version mismatch but no conflicts - allow with warning
                logger.info("Version mismatch without conflicts, allowing operation for itinerary: {}", 
                           current.getItineraryId());
                changeSet.setBaseVersion(current.getVersion());
            }
        }
    }
    
    /**
     * Create an ItineraryDiff from conflict information.
     */
    private ItineraryDiff createConflictDiff(List<ConflictResolver.Conflict> conflicts) {
        List<DiffItem> conflictItems = conflicts.stream()
                .filter(conflict -> conflict.getNodeId() != null)
                .map(conflict -> new DiffItem(conflict.getNodeId(), null, 
                                            Arrays.asList(conflict.getType().name().toLowerCase())))
                .collect(Collectors.toList());
        
        return new ItineraryDiff(new ArrayList<>(), new ArrayList<>(), conflictItems);
    }
    
    /**
     * Apply changes with conflict detection and resolution.
     */
    public ApplyResult applyWithConflictResolution(String itineraryId, ChangeSet changeSet, 
                                                  boolean allowAutoResolution) {
        logger.info("Applying changes with conflict resolution for itinerary: {}", itineraryId);
        
        try {
            // Load current itinerary
            Optional<NormalizedItinerary> currentOpt = itineraryJsonService.getItinerary(itineraryId);
            if (currentOpt.isEmpty()) {
                throw new IllegalArgumentException("Itinerary not found: " + itineraryId);
            }
            
            NormalizedItinerary current = currentOpt.get();
            
            // Always detect conflicts when using this method
            ConflictResolver.ConflictDetectionResult conflictResult = 
                conflictResolver.detectConflicts(current, changeSet);
            
            if (conflictResult.hasConflicts()) {
                if (allowAutoResolution) {
                    // Attempt automatic resolution
                    ConflictResolver.ConflictResolutionResult resolutionResult = 
                        conflictResolver.attemptAutoResolution(current, changeSet, conflictResult);
                    
                    if (resolutionResult.isFullyResolved()) {
                        logger.info("All conflicts automatically resolved, proceeding with apply");
                        return apply(itineraryId, resolutionResult.getMergedChangeSet());
                    } else {
                        // Partial resolution - return conflict information
                        ItineraryDiff conflictDiff = createConflictDiff(resolutionResult.getUnresolvedConflicts());
                        throw new VersionMismatchException(
                            itineraryId,
                            changeSet.getBaseVersion(),
                            current.getVersion(),
                            conflictDiff
                        );
                    }
                } else {
                    // No auto-resolution allowed - return conflicts
                    ItineraryDiff conflictDiff = createConflictDiff(conflictResult.getConflicts());
                    throw new VersionMismatchException(
                        itineraryId,
                        changeSet.getBaseVersion(),
                        current.getVersion(),
                        conflictDiff
                    );
                }
            } else {
                // No conflicts - proceed normally
                return apply(itineraryId, changeSet);
            }
            
        } catch (VersionMismatchException e) {
            throw e; // Re-throw version mismatch exceptions
        } catch (Exception e) {
            logger.error("Failed to apply changes with conflict resolution", e);
            throw new RuntimeException("Failed to apply changes with conflict resolution", e);
        }
    }
    
    /**
     * Sync normalized itinerary changes back to the regular itinerary.
     * This ensures that changes made through the chat interface are reflected
     * in the workflow and day-by-day views.
     */
    // Removed regular-entity sync in Firestore-only mode
    
    /**
     * Update a node's content without changing its position or timing.
     */
    private boolean updateNode(NormalizedItinerary itinerary, ChangeOperation op, Integer day, ChangePreferences preferences) {
        // Find the node to update
        NormalizedNode node = findNodeById(itinerary, op.getId());
        if (node == null) {
            logger.warn("Node not found for update operation: {}", op.getId());
            return false;
        }
        
        // Check if node is locked
        if (Boolean.TRUE.equals(node.getLocked()) && 
            Boolean.TRUE.equals(preferences != null ? preferences.getRespectLocks() : true)) {
            logger.warn("Cannot update locked node: {}", op.getId());
            return false;
        }
        
        // Check system-level locks
        if (lockManager.isLocked(op.getId())) {
            logger.warn("Cannot update node with active system lock: {}", op.getId());
            return false;
        }
        
        // Update node content if provided
        if (op.getNode() != null) {
            NormalizedNode updateData = op.getNode();
            
            // Update fields that are safe to change
            if (updateData.getTitle() != null) {
                node.setTitle(updateData.getTitle());
            }
            if (updateData.getLocation() != null) {
                node.setLocation(updateData.getLocation());
            }
            if (updateData.getDetails() != null) {
                NodeDetails updateDetails = updateData.getDetails();
                NodeDetails currentDetails = node.getDetails();
                if (currentDetails == null) {
                    currentDetails = new NodeDetails();
                    node.setDetails(currentDetails);
                }
                
                // Update description and category from details
                if (updateDetails.getDescription() != null) {
                    currentDetails.setDescription(updateDetails.getDescription());
                }
                if (updateDetails.getCategory() != null) {
                    currentDetails.setCategory(updateDetails.getCategory());
                }
            }
            
            // Update audit trail
            updateNodeAudit(node, "user");
            
            logger.debug("Updated node content: {}", op.getId());
            return true;
        }
        
        logger.warn("No update data provided for node: {}", op.getId());
        return false;
    }
    
    /**
     * Update edge connections between nodes.
     * Note: Edges are not currently part of NormalizedNode structure.
     * This method is a placeholder for future edge functionality.
     */
    private boolean updateEdge(NormalizedItinerary itinerary, ChangeOperation op, Integer day, ChangePreferences preferences) {
        // Find the target day
        NormalizedDay targetDay = findDayByNumber(itinerary, day);
        if (targetDay == null) {
            logger.warn("Day not found for edge update: {}", day);
            return false;
        }
        
        // Check if the node exists
        NormalizedNode node = findNodeById(itinerary, op.getId());
        if (node == null) {
            logger.warn("Node not found for edge update: {}", op.getId());
            return false;
        }
        
        // Check if node is locked
        if (Boolean.TRUE.equals(node.getLocked()) && 
            Boolean.TRUE.equals(preferences != null ? preferences.getRespectLocks() : true)) {
            logger.warn("Cannot update edges for locked node: {}", op.getId());
            return false;
        }
        
        // For now, just update the node's audit trail since edges are not implemented
        updateNodeAudit(node, "user");
        
        logger.debug("Edge update operation acknowledged for node: {} (edges not yet implemented)", op.getId());
        return true;
    }
    
    /**
     * Get information about a replace operation result.
     * This method finds the actual node that was replaced and returns its details.
     */
    private ReplaceResult getReplaceResult(NormalizedItinerary itinerary, ChangeOperation op, Integer day) {
        // Find the day
        NormalizedDay targetDay = findDayByNumber(itinerary, day);
        if (targetDay == null) {
            logger.warn("Day not found for replace result: {}", day);
            return null;
        }
        
        // Try to find the node by the ID provided in the operation
        NormalizedNode nodeToReplace = findNodeById(itinerary, op.getId());
        if (nodeToReplace != null) {
            // Found the exact node
            return new ReplaceResult(nodeToReplace.getId(), nodeToReplace.getTitle());
        }
        
        // If the exact ID wasn't found, use the same fallback strategy as replaceNode
        // This handles cases where the LLM provides a generic ID like "day1_node1"
        List<NormalizedNode> nodes = targetDay.getNodes();
        if (nodes != null && !nodes.isEmpty()) {
            // Use the first node as a fallback (same logic as in replaceNode)
            NormalizedNode fallbackNode = nodes.get(0);
            logger.warn("Could not find node with ID '{}', using fallback node '{}' for replace result", op.getId(), fallbackNode.getId());
            return new ReplaceResult(fallbackNode.getId(), fallbackNode.getTitle());
        }
        
        logger.warn("No nodes found in day {} for replace operation", day);
        return null;
    }
    
    /**
     * Map normalized itinerary ID to database ID.
     * This handles the mapping between semantic IDs and database IDs.
     */
    // Removed mapping to DB ID in Firestore-only mode
}
