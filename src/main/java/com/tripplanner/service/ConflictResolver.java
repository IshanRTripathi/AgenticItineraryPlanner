package com.tripplanner.service;

import com.tripplanner.dto.*;
import com.tripplanner.exception.VersionMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting and resolving conflicts between concurrent itinerary modifications.
 * Implements various merge strategies for handling version conflicts.
 */
@Service
public class ConflictResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(ConflictResolver.class);
    
    /**
     * Detect conflicts between a ChangeSet and the current itinerary state.
     */
    public ConflictDetectionResult detectConflicts(NormalizedItinerary currentItinerary, 
                                                  ChangeSet changeSet) {
        logger.debug("Detecting conflicts for itinerary: {} with changeSet targeting version: {}", 
                    currentItinerary.getItineraryId(), changeSet.getBaseVersion());
        
        List<Conflict> conflicts = new ArrayList<>();
        
        // Check version mismatch
        if (changeSet.getBaseVersion() != null && 
            !changeSet.getBaseVersion().equals(currentItinerary.getVersion())) {
            
            logger.info("Version mismatch detected: expected {}, actual {}", 
                       changeSet.getBaseVersion(), currentItinerary.getVersion());
            
            // Analyze specific conflicts
            conflicts.addAll(analyzeOperationConflicts(currentItinerary, changeSet));
        }
        
        // Check for concurrent modification conflicts
        conflicts.addAll(detectConcurrentModifications(currentItinerary, changeSet));
        
        // Check for business rule conflicts
        conflicts.addAll(detectBusinessRuleConflicts(currentItinerary, changeSet));
        
        ConflictSeverity overallSeverity = determineOverallSeverity(conflicts);
        
        logger.debug("Conflict detection complete: {} conflicts found, severity: {}", 
                    conflicts.size(), overallSeverity);
        
        return new ConflictDetectionResult(conflicts, overallSeverity);
    }
    
    /**
     * Attempt to automatically resolve conflicts using various merge strategies.
     */
    public ConflictResolutionResult attemptAutoResolution(NormalizedItinerary currentItinerary,
                                                         ChangeSet changeSet,
                                                         ConflictDetectionResult conflictResult) {
        logger.info("Attempting automatic conflict resolution for {} conflicts", 
                   conflictResult.getConflicts().size());
        
        List<Conflict> resolvedConflicts = new ArrayList<>();
        List<Conflict> unresolvedConflicts = new ArrayList<>();
        ChangeSet mergedChangeSet = deepCopyChangeSet(changeSet);
        
        for (Conflict conflict : conflictResult.getConflicts()) {
            try {
                if (canAutoResolve(conflict)) {
                    resolveConflict(currentItinerary, mergedChangeSet, conflict);
                    resolvedConflicts.add(conflict);
                    logger.debug("Auto-resolved conflict: {}", conflict.getDescription());
                } else {
                    unresolvedConflicts.add(conflict);
                    logger.debug("Cannot auto-resolve conflict: {}", conflict.getDescription());
                }
            } catch (Exception e) {
                logger.warn("Failed to resolve conflict: {}", conflict.getDescription(), e);
                unresolvedConflicts.add(conflict);
            }
        }
        
        boolean fullyResolved = unresolvedConflicts.isEmpty();
        
        logger.info("Conflict resolution complete: {}/{} conflicts resolved", 
                   resolvedConflicts.size(), conflictResult.getConflicts().size());
        
        return new ConflictResolutionResult(fullyResolved, mergedChangeSet, 
                                          resolvedConflicts, unresolvedConflicts);
    }
    
    /**
     * Create a three-way merge between base, current, and incoming changes.
     */
    public MergeResult performThreeWayMerge(NormalizedItinerary baseItinerary,
                                          NormalizedItinerary currentItinerary,
                                          ChangeSet incomingChangeSet) {
        logger.info("Performing three-way merge for itinerary: {}", currentItinerary.getItineraryId());
        
        try {
            // Create a merged itinerary starting from current
            NormalizedItinerary mergedItinerary = deepCopyItinerary(currentItinerary);
            
            // Apply non-conflicting changes from incoming changeset
            List<ChangeOperation> applicableOps = new ArrayList<>();
            List<Conflict> mergeConflicts = new ArrayList<>();
            
            for (ChangeOperation op : incomingChangeSet.getOps()) {
                if (isOperationSafeToApply(mergedItinerary, op)) {
                    applicableOps.add(op);
                } else {
                    // Create conflict for operations that can't be safely applied
                    mergeConflicts.add(new Conflict(
                        ConflictType.OPERATION_CONFLICT,
                        ConflictSeverity.MEDIUM,
                        "Operation conflicts with current state: " + op.getOp(),
                        op.getId(),
                        null,
                        op
                    ));
                }
            }
            
            // Create merged changeset with applicable operations
            ChangeSet mergedChangeSet = new ChangeSet(
                incomingChangeSet.getScope(),
                incomingChangeSet.getDay(),
                applicableOps,
                currentItinerary.getVersion()
            );
            mergedChangeSet.setAgent(incomingChangeSet.getAgent());
            mergedChangeSet.setReason("Three-way merge: " + incomingChangeSet.getReason());
            mergedChangeSet.setPreferences(incomingChangeSet.getPreferences());
            
            boolean hasConflicts = !mergeConflicts.isEmpty();
            
            logger.info("Three-way merge complete: {} applicable operations, {} conflicts", 
                       applicableOps.size(), mergeConflicts.size());
            
            return new MergeResult(mergedChangeSet, mergeConflicts, !hasConflicts);
            
        } catch (Exception e) {
            logger.error("Three-way merge failed", e);
            throw new RuntimeException("Three-way merge failed", e);
        }
    }
    
    /**
     * Analyze specific operation conflicts.
     */
    private List<Conflict> analyzeOperationConflicts(NormalizedItinerary currentItinerary, 
                                                    ChangeSet changeSet) {
        List<Conflict> conflicts = new ArrayList<>();
        
        for (ChangeOperation op : changeSet.getOps()) {
            // Check if the target node still exists and hasn't been modified
            if (op.getId() != null) {
                NormalizedNode targetNode = findNodeById(currentItinerary, op.getId());
                
                if (targetNode == null && ("move".equals(op.getOp()) || "delete".equals(op.getOp()))) {
                    conflicts.add(new Conflict(
                        ConflictType.NODE_NOT_FOUND,
                        ConflictSeverity.HIGH,
                        "Target node no longer exists: " + op.getId(),
                        op.getId(),
                        null,
                        op
                    ));
                }
                
                if (targetNode != null && Boolean.TRUE.equals(targetNode.getLocked())) {
                    conflicts.add(new Conflict(
                        ConflictType.NODE_LOCKED,
                        ConflictSeverity.MEDIUM,
                        "Target node is locked: " + op.getId(),
                        op.getId(),
                        targetNode,
                        op
                    ));
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Detect concurrent modifications.
     */
    private List<Conflict> detectConcurrentModifications(NormalizedItinerary currentItinerary, 
                                                        ChangeSet changeSet) {
        List<Conflict> conflicts = new ArrayList<>();
        
        // Check if nodes referenced in the changeset have been modified since base version
        for (ChangeOperation op : changeSet.getOps()) {
            if (op.getId() != null) {
                NormalizedNode node = findNodeById(currentItinerary, op.getId());
                if (node != null && hasBeenModifiedRecently(node, changeSet.getBaseVersion())) {
                    conflicts.add(new Conflict(
                        ConflictType.CONCURRENT_MODIFICATION,
                        ConflictSeverity.MEDIUM,
                        "Node has been modified by another operation: " + op.getId(),
                        op.getId(),
                        node,
                        op
                    ));
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Detect business rule conflicts.
     */
    private List<Conflict> detectBusinessRuleConflicts(NormalizedItinerary currentItinerary, 
                                                      ChangeSet changeSet) {
        List<Conflict> conflicts = new ArrayList<>();
        
        // Check for timing conflicts
        conflicts.addAll(detectTimingConflicts(currentItinerary, changeSet));
        
        // Check for capacity conflicts
        conflicts.addAll(detectCapacityConflicts(currentItinerary, changeSet));
        
        return conflicts;
    }
    
    /**
     * Detect timing conflicts between operations.
     */
    private List<Conflict> detectTimingConflicts(NormalizedItinerary currentItinerary, 
                                                ChangeSet changeSet) {
        List<Conflict> conflicts = new ArrayList<>();
        
        for (ChangeOperation op : changeSet.getOps()) {
            if ("move".equals(op.getOp()) && op.getStartTime() != null && op.getEndTime() != null) {
                // Check for overlapping times with other nodes on the same day
                Integer day = changeSet.getDay();
                if (day != null) {
                    NormalizedDay targetDay = findDayByNumber(currentItinerary, day);
                    if (targetDay != null) {
                        for (NormalizedNode node : targetDay.getNodes()) {
                            if (node.getId() != null && !node.getId().equals(op.getId()) && 
                                hasTimingOverlap(node, op.getStartTime(), op.getEndTime())) {
                                conflicts.add(new Conflict(
                                    ConflictType.TIMING_CONFLICT,
                                    ConflictSeverity.MEDIUM,
                                    "Timing overlap detected with node: " + node.getId(),
                                    op.getId(),
                                    node,
                                    op
                                ));
                            }
                        }
                    }
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Detect capacity conflicts.
     */
    private List<Conflict> detectCapacityConflicts(NormalizedItinerary currentItinerary, 
                                                  ChangeSet changeSet) {
        List<Conflict> conflicts = new ArrayList<>();
        
        // Check if adding new nodes would exceed day capacity
        long insertOperations = changeSet.getOps().stream()
                .filter(op -> "insert".equals(op.getOp()))
                .count();
        
        if (insertOperations > 0 && changeSet.getDay() != null) {
            NormalizedDay targetDay = findDayByNumber(currentItinerary, changeSet.getDay());
            if (targetDay != null && targetDay.getNodes().size() + insertOperations > 10) { // Max 10 nodes per day
                conflicts.add(new Conflict(
                    ConflictType.CAPACITY_EXCEEDED,
                    ConflictSeverity.LOW,
                    "Adding nodes would exceed day capacity",
                    null,
                    null,
                    null
                ));
            }
        }
        
        return conflicts;
    }
    
    /**
     * Determine overall conflict severity.
     */
    private ConflictSeverity determineOverallSeverity(List<Conflict> conflicts) {
        if (conflicts.isEmpty()) {
            return ConflictSeverity.NONE;
        }
        
        return conflicts.stream()
                .map(Conflict::getSeverity)
                .max(Comparator.comparing(ConflictSeverity::ordinal))
                .orElse(ConflictSeverity.LOW);
    }
    
    /**
     * Check if a conflict can be automatically resolved.
     */
    private boolean canAutoResolve(Conflict conflict) {
        switch (conflict.getType()) {
            case TIMING_CONFLICT:
                return conflict.getSeverity() == ConflictSeverity.LOW;
            case CAPACITY_EXCEEDED:
                return false; // Always requires manual intervention
            case NODE_LOCKED:
                return false; // Respect locks
            case NODE_NOT_FOUND:
                return conflict.getOperation() != null && "delete".equals(conflict.getOperation().getOp());
            case CONCURRENT_MODIFICATION:
                return conflict.getSeverity() == ConflictSeverity.LOW;
            default:
                return false;
        }
    }
    
    /**
     * Resolve a specific conflict.
     */
    private void resolveConflict(NormalizedItinerary currentItinerary, ChangeSet changeSet, Conflict conflict) {
        switch (conflict.getType()) {
            case NODE_NOT_FOUND:
                if (conflict.getOperation() != null && "delete".equals(conflict.getOperation().getOp())) {
                    // Remove the delete operation since node is already gone
                    changeSet.getOps().remove(conflict.getOperation());
                }
                break;
            case TIMING_CONFLICT:
                if (conflict.getOperation() != null && "move".equals(conflict.getOperation().getOp())) {
                    // Adjust timing to avoid conflict
                    adjustTimingToAvoidConflict(currentItinerary, conflict.getOperation(), changeSet.getDay());
                }
                break;
            case CONCURRENT_MODIFICATION:
                // For low severity, accept the current state
                if (conflict.getSeverity() == ConflictSeverity.LOW) {
                    // Remove conflicting operation
                    changeSet.getOps().remove(conflict.getOperation());
                }
                break;
            default:
                throw new UnsupportedOperationException("Cannot resolve conflict type: " + conflict.getType());
        }
    }
    
    /**
     * Adjust timing to avoid conflicts.
     */
    private void adjustTimingToAvoidConflict(NormalizedItinerary itinerary, ChangeOperation operation, Integer day) {
        if (day == null || operation.getStartTime() == null) {
            return;
        }
        
        NormalizedDay targetDay = findDayByNumber(itinerary, day);
        if (targetDay == null) {
            return;
        }
        
        // Find next available time slot
        Long nextAvailableTime = findNextAvailableTimeSlot(targetDay, operation.getStartTime(), 
                                                          operation.getEndTime());
        if (nextAvailableTime != null) {
            Long duration = operation.getEndTime() - operation.getStartTime();
            operation.setStartTime(nextAvailableTime);
            operation.setEndTime(nextAvailableTime + duration);
        }
    }
    
    /**
     * Find next available time slot.
     */
    private Long findNextAvailableTimeSlot(NormalizedDay day, Long startTime, Long endTime) {
        Long duration = endTime - startTime;
        Long currentTime = startTime;
        
        // Simple algorithm: increment by 30 minutes until we find a free slot
        while (currentTime < startTime + 8 * 60 * 60 * 1000) { // Max 8 hours search
            boolean hasConflict = false;
            for (NormalizedNode node : day.getNodes()) {
                if (hasTimingOverlap(node, currentTime, currentTime + duration)) {
                    hasConflict = true;
                    break;
                }
            }
            
            if (!hasConflict) {
                return currentTime;
            }
            
            currentTime += 30 * 60 * 1000; // 30 minutes
        }
        
        return null; // No available slot found
    }
    
    // Helper methods
    private NormalizedNode findNodeById(NormalizedItinerary itinerary, String nodeId) {
        return itinerary.getDays().stream()
                .flatMap(day -> day.getNodes().stream())
                .filter(node -> node.getId() != null && node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
    
    private NormalizedDay findDayByNumber(NormalizedItinerary itinerary, Integer dayNumber) {
        return itinerary.getDays().stream()
                .filter(day -> day.getDayNumber().equals(dayNumber))
                .findFirst()
                .orElse(null);
    }
    
    private boolean hasBeenModifiedRecently(NormalizedNode node, Integer baseVersion) {
        // Simple heuristic: check if node was updated after base version
        return node.getUpdatedAt() != null && baseVersion != null;
    }
    
    private boolean hasTimingOverlap(NormalizedNode node, Long startTime, Long endTime) {
        if (node.getTiming() == null || node.getTiming().getStartTime() == null || 
            node.getTiming().getEndTime() == null) {
            return false;
        }
        
        Long nodeStart = node.getTiming().getStartTime();
        Long nodeEnd = node.getTiming().getEndTime();
        
        return !(endTime <= nodeStart || startTime >= nodeEnd);
    }
    
    private boolean isOperationSafeToApply(NormalizedItinerary itinerary, ChangeOperation operation) {
        // Simple safety check
        if (operation.getId() != null) {
            NormalizedNode node = findNodeById(itinerary, operation.getId());
            
            // Can't operate on non-existent nodes (except insert)
            if (node == null && !"insert".equals(operation.getOp())) {
                return false;
            }
            
            // Can't operate on locked nodes
            if (node != null && Boolean.TRUE.equals(node.getLocked())) {
                return false;
            }
        }
        
        return true;
    }
    
    private ChangeSet deepCopyChangeSet(ChangeSet original) {
        // Simple deep copy implementation
        ChangeSet copy = new ChangeSet(original.getScope(), original.getDay(), 
                                     new ArrayList<>(original.getOps()), original.getBaseVersion());
        copy.setAgent(original.getAgent());
        copy.setReason(original.getReason());
        copy.setPreferences(original.getPreferences());
        return copy;
    }
    
    private NormalizedItinerary deepCopyItinerary(NormalizedItinerary original) {
        // This would need proper deep copy implementation
        // For now, return the original (this should be implemented properly)
        return original;
    }
    
    // Result classes and enums
    public enum ConflictType {
        VERSION_MISMATCH,
        NODE_NOT_FOUND,
        NODE_LOCKED,
        CONCURRENT_MODIFICATION,
        TIMING_CONFLICT,
        CAPACITY_EXCEEDED,
        OPERATION_CONFLICT
    }
    
    public enum ConflictSeverity {
        NONE,
        LOW,
        MEDIUM,
        HIGH
    }
    
    public static class Conflict {
        private final ConflictType type;
        private final ConflictSeverity severity;
        private final String description;
        private final String nodeId;
        private final NormalizedNode currentNode;
        private final ChangeOperation operation;
        
        public Conflict(ConflictType type, ConflictSeverity severity, String description,
                       String nodeId, NormalizedNode currentNode, ChangeOperation operation) {
            this.type = type;
            this.severity = severity;
            this.description = description;
            this.nodeId = nodeId;
            this.currentNode = currentNode;
            this.operation = operation;
        }
        
        // Getters
        public ConflictType getType() { return type; }
        public ConflictSeverity getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getNodeId() { return nodeId; }
        public NormalizedNode getCurrentNode() { return currentNode; }
        public ChangeOperation getOperation() { return operation; }
        
        @Override
        public String toString() {
            return "Conflict{" +
                    "type=" + type +
                    ", severity=" + severity +
                    ", description='" + description + '\'' +
                    ", nodeId='" + nodeId + '\'' +
                    '}';
        }
    }
    
    public static class ConflictDetectionResult {
        private final List<Conflict> conflicts;
        private final ConflictSeverity overallSeverity;
        
        public ConflictDetectionResult(List<Conflict> conflicts, ConflictSeverity overallSeverity) {
            this.conflicts = conflicts;
            this.overallSeverity = overallSeverity;
        }
        
        public List<Conflict> getConflicts() { return conflicts; }
        public ConflictSeverity getOverallSeverity() { return overallSeverity; }
        public boolean hasConflicts() { return !conflicts.isEmpty(); }
        
        @Override
        public String toString() {
            return "ConflictDetectionResult{" +
                    "conflicts=" + conflicts.size() +
                    ", overallSeverity=" + overallSeverity +
                    '}';
        }
    }
    
    public static class ConflictResolutionResult {
        private final boolean fullyResolved;
        private final ChangeSet mergedChangeSet;
        private final List<Conflict> resolvedConflicts;
        private final List<Conflict> unresolvedConflicts;
        
        public ConflictResolutionResult(boolean fullyResolved, ChangeSet mergedChangeSet,
                                      List<Conflict> resolvedConflicts, List<Conflict> unresolvedConflicts) {
            this.fullyResolved = fullyResolved;
            this.mergedChangeSet = mergedChangeSet;
            this.resolvedConflicts = resolvedConflicts;
            this.unresolvedConflicts = unresolvedConflicts;
        }
        
        public boolean isFullyResolved() { return fullyResolved; }
        public ChangeSet getMergedChangeSet() { return mergedChangeSet; }
        public List<Conflict> getResolvedConflicts() { return resolvedConflicts; }
        public List<Conflict> getUnresolvedConflicts() { return unresolvedConflicts; }
        
        @Override
        public String toString() {
            return "ConflictResolutionResult{" +
                    "fullyResolved=" + fullyResolved +
                    ", resolvedConflicts=" + resolvedConflicts.size() +
                    ", unresolvedConflicts=" + unresolvedConflicts.size() +
                    '}';
        }
    }
    
    public static class MergeResult {
        private final ChangeSet mergedChangeSet;
        private final List<Conflict> conflicts;
        private final boolean successful;
        
        public MergeResult(ChangeSet mergedChangeSet, List<Conflict> conflicts, boolean successful) {
            this.mergedChangeSet = mergedChangeSet;
            this.conflicts = conflicts;
            this.successful = successful;
        }
        
        public ChangeSet getMergedChangeSet() { return mergedChangeSet; }
        public List<Conflict> getConflicts() { return conflicts; }
        public boolean isSuccessful() { return successful; }
        
        @Override
        public String toString() {
            return "MergeResult{" +
                    "successful=" + successful +
                    ", conflicts=" + conflicts.size() +
                    '}';
        }
    }
}