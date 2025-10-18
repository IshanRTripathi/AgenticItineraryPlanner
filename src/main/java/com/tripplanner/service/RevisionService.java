package com.tripplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.dto.ChangeDetail;
import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedNode;
import com.tripplanner.dto.NodeTiming;
import com.tripplanner.dto.RevisionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service for managing itinerary revisions in Firebase.
 * Stores revisions at root/itineraries/{itineraryId}/revisions/{revisionId}
 */
@Service
public class RevisionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RevisionService.class);
    
    private static final String COLLECTION_ITINERARIES = "itineraries";
    private static final String SUBCOLLECTION_REVISIONS = "revisions";
    
    @Autowired
    private Firestore firestore;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Save a revision to Firebase at root/itineraries/{itineraryId}/revisions/{revisionId}
     */
    public void saveRevision(String itineraryId, RevisionRecord revision) {
        try {
            logger.info("Saving revision {} for itinerary: {}", revision.getRevisionId(), itineraryId);
            
            DocumentReference revisionDoc = firestore
                    .collection(COLLECTION_ITINERARIES)
                    .document(itineraryId)
                    .collection(SUBCOLLECTION_REVISIONS)
                    .document(revision.getRevisionId());
            
            // Validate revision data before saving
            validateRevisionRecord(revision);
            
            // Save revision to Firebase
            revisionDoc.set(revision).get();
            
            logger.info("Successfully saved revision {} for itinerary: {}", revision.getRevisionId(), itineraryId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to save revision {} for itinerary: {}", revision.getRevisionId(), itineraryId, e);
            throw new RuntimeException("Failed to save revision", e);
        } catch (Exception e) {
            logger.error("Error saving revision {} for itinerary: {}", revision.getRevisionId(), itineraryId, e);
            throw new RuntimeException("Error saving revision: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all revisions for an itinerary ordered by timestamp descending
     */
    public List<RevisionRecord> getRevisionHistory(String itineraryId) {
        try {
            logger.info("Fetching revision history for itinerary: {}", itineraryId);
            
            DocumentReference itineraryDoc = firestore
                    .collection(COLLECTION_ITINERARIES)
                    .document(itineraryId);
            
            Query query = itineraryDoc.collection(SUBCOLLECTION_REVISIONS)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
            
            QuerySnapshot querySnapshot = query.get().get();
            
            List<RevisionRecord> revisions = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        try {
                            return doc.toObject(RevisionRecord.class);
                        } catch (Exception e) {
                            logger.error("Failed to convert revision document to object: {}", doc.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(RevisionRecord::getTimestamp).reversed())
                    .collect(Collectors.toList());
            
            logger.info("Found {} revisions for itinerary: {}", revisions.size(), itineraryId);
            return revisions;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch revision history for itinerary: {}", itineraryId, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching revision history for itinerary: {}", itineraryId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Rollback itinerary to a specific revision
     */
    public NormalizedItinerary rollbackToVersion(String itineraryId, String revisionId) {
        try {
            logger.info("Rolling back itinerary {} to revision: {}", itineraryId, revisionId);
            
            // Validate revision exists
            DocumentReference revisionDoc = firestore
                    .collection(COLLECTION_ITINERARIES)
                    .document(itineraryId)
                    .collection(SUBCOLLECTION_REVISIONS)
                    .document(revisionId);
            
            DocumentSnapshot document = revisionDoc.get().get();
            
            if (!document.exists()) {
                throw new IllegalArgumentException("Revision not found: " + revisionId);
            }
            
            RevisionRecord revision = document.toObject(RevisionRecord.class);
            if (revision == null) {
                throw new RuntimeException("Failed to deserialize revision: " + revisionId);
            }
            
            // Reconstruct itinerary from revision
            NormalizedItinerary restored = reconstructItineraryFromRevision(revision);
            
            logger.info("Successfully rolled back itinerary {} to revision: {}", itineraryId, revisionId);
            return restored;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to rollback itinerary {} to revision: {}", itineraryId, revisionId, e);
            throw new RuntimeException("Failed to rollback to revision", e);
        } catch (Exception e) {
            logger.error("Error rolling back itinerary {} to revision: {}", itineraryId, revisionId, e);
            throw new RuntimeException("Error rolling back to revision: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a specific revision by ID
     */
    public Optional<RevisionRecord> getRevision(String itineraryId, String revisionId) {
        try {
            logger.debug("Fetching revision {} for itinerary: {}", revisionId, itineraryId);
            
            DocumentReference revisionDoc = firestore
                    .collection(COLLECTION_ITINERARIES)
                    .document(itineraryId)
                    .collection(SUBCOLLECTION_REVISIONS)
                    .document(revisionId);
            
            DocumentSnapshot document = revisionDoc.get().get();
            
            if (document.exists()) {
                RevisionRecord revision = document.toObject(RevisionRecord.class);
                return Optional.ofNullable(revision);
            } else {
                logger.warn("Revision {} not found for itinerary: {}", revisionId, itineraryId);
                return Optional.empty();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch revision {} for itinerary: {}", revisionId, itineraryId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Validate revision record before saving
     */
    private void validateRevisionRecord(RevisionRecord revision) {
        if (revision == null) {
            throw new IllegalArgumentException("Revision record cannot be null");
        }
        if (revision.getRevisionId() == null || revision.getRevisionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Revision ID cannot be null or empty");
        }
        if (revision.getTimestamp() == null) {
            throw new IllegalArgumentException("Revision timestamp cannot be null");
        }
        if (revision.getChanges() == null) {
            throw new IllegalArgumentException("Revision changes cannot be null");
        }
    }
    
    /**
     * Reconstruct itinerary from revision record
     * This implementation reconstructs the itinerary state by applying the reverse
     * of all changes that occurred after the target revision.
     */
    private NormalizedItinerary reconstructItineraryFromRevision(RevisionRecord targetRevision) {
        try {
            logger.info("Reconstructing itinerary from revision: {}", targetRevision.getRevisionId());
            
            // For this implementation, we'll use a simplified approach:
            // 1. Get the current itinerary state
            // 2. Apply reverse changes from all revisions after the target revision
            // 3. Return the reconstructed state
            
            // Note: In a production system, you might want to store full snapshots
            // at certain intervals to make reconstruction more efficient
            
            // Get the itinerary ID from the revision's userId (assuming it's stored there)
            // This is a simplified approach - in practice, you'd need a better way to track this
            String itineraryId = extractItineraryIdFromRevision(targetRevision);
            
            if (itineraryId == null) {
                throw new IllegalArgumentException("Cannot determine itinerary ID from revision");
            }
            
            // Get current itinerary state
            DocumentReference itineraryDoc = firestore
                    .collection(COLLECTION_ITINERARIES)
                    .document(itineraryId);
            
            DocumentSnapshot currentDoc = itineraryDoc.get().get();
            if (!currentDoc.exists()) {
                throw new IllegalArgumentException("Current itinerary not found: " + itineraryId);
            }
            
            // Convert to NormalizedItinerary
            NormalizedItinerary currentItinerary = currentDoc.toObject(NormalizedItinerary.class);
            if (currentItinerary == null) {
                throw new RuntimeException("Failed to deserialize current itinerary");
            }
            
            // Get all revisions after the target revision
            List<RevisionRecord> laterRevisions = getRevisionsAfter(itineraryId, targetRevision.getTimestamp());
            
            // Apply reverse changes in reverse chronological order
            NormalizedItinerary reconstructed = deepCopyItinerary(currentItinerary);
            
            for (int i = laterRevisions.size() - 1; i >= 0; i--) {
                RevisionRecord laterRevision = laterRevisions.get(i);
                applyReverseChanges(reconstructed, laterRevision);
            }
            
            // Set the version to match the target revision
            reconstructed.setVersion(extractVersionFromRevision(targetRevision));
            reconstructed.setUpdatedAt(targetRevision.getTimestamp());
            
            logger.info("Successfully reconstructed itinerary from revision: {}", targetRevision.getRevisionId());
            return reconstructed;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to reconstruct itinerary from revision: {}", targetRevision.getRevisionId(), e);
            throw new RuntimeException("Failed to reconstruct itinerary from revision", e);
        } catch (Exception e) {
            logger.error("Error reconstructing itinerary from revision: {}", targetRevision.getRevisionId(), e);
            throw new RuntimeException("Error reconstructing itinerary: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all revisions after a specific timestamp
     */
    private List<RevisionRecord> getRevisionsAfter(String itineraryId, Long afterTimestamp) 
            throws InterruptedException, ExecutionException {
        
        DocumentReference itineraryDoc = firestore
                .collection(COLLECTION_ITINERARIES)
                .document(itineraryId);
        
        Query query = itineraryDoc.collection(SUBCOLLECTION_REVISIONS)
                .whereGreaterThan("timestamp", afterTimestamp)
                .orderBy("timestamp", Query.Direction.ASCENDING);
        
        QuerySnapshot querySnapshot = query.get().get();
        
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RevisionRecord.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Apply reverse changes from a revision to reconstruct previous state
     */
    private void applyReverseChanges(NormalizedItinerary itinerary, RevisionRecord revision) {
        if (revision.getChanges() == null || revision.getChanges().isEmpty()) {
            return;
        }
        
        // Apply changes in reverse order
        List<ChangeDetail> changes = new ArrayList<>(revision.getChanges());
        Collections.reverse(changes);
        
        for (ChangeDetail change : changes) {
            applyReverseChange(itinerary, change);
        }
    }
    
    /**
     * Apply a single reverse change
     */
    private void applyReverseChange(NormalizedItinerary itinerary, ChangeDetail change) {
        try {
            String operationType = change.getOperationType();
            String elementId = change.getElementId();
            
            switch (operationType.toUpperCase()) {
                case "MOVE":
                    // Reverse a move by restoring original timing
                    reverseMove(itinerary, elementId, change);
                    break;
                case "INSERT":
                case "CREATE":
                    // Reverse an insert by removing the node
                    reverseInsert(itinerary, elementId, change);
                    break;
                case "DELETE":
                    // Reverse a delete by restoring the node
                    reverseDelete(itinerary, change);
                    break;
                case "REPLACE":
                    // Reverse a replace by restoring the original node
                    reverseReplace(itinerary, change);
                    break;
                default:
                    logger.warn("Unknown operation type for reverse: {}", operationType);
            }
        } catch (Exception e) {
            logger.error("Failed to apply reverse change: {}", change, e);
            // Continue with other changes even if one fails
        }
    }
    
    /**
     * Reverse a move operation by restoring original timing
     */
    private void reverseMove(NormalizedItinerary itinerary, String nodeId, ChangeDetail change) {
        NormalizedNode node = findNodeById(itinerary, nodeId);
        if (node == null) {
            logger.warn("Node not found for reverse move: {}", nodeId);
            return;
        }
        
        // Restore original timing from oldValue
        if (change.getOldValue() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> oldTiming = (Map<String, Object>) change.getOldValue();
            
            if (node.getTiming() == null) {
                node.setTiming(new NodeTiming());
            }
            
            if (oldTiming.containsKey("startTime")) {
                node.getTiming().setStartTime(Long.valueOf((String) oldTiming.get("startTime")));
            }
            if (oldTiming.containsKey("endTime")) {
                node.getTiming().setEndTime(Long.valueOf((String) oldTiming.get("endTime")));
            }
        }
    }
    
    /**
     * Reverse an insert operation by removing the node
     */
    private void reverseInsert(NormalizedItinerary itinerary, String nodeId, ChangeDetail change) {
        // Find and remove the node from all days
        for (NormalizedDay day : itinerary.getDays()) {
            day.getNodes().removeIf(node -> node.getId() != null && node.getId().equals(nodeId));
        }
    }
    
    /**
     * Reverse a delete operation by restoring the node
     */
    private void reverseDelete(NormalizedItinerary itinerary, ChangeDetail change) {
        // Restore the deleted node from oldValue
        if (change.getOldValue() instanceof NormalizedNode) {
            NormalizedNode restoredNode = (NormalizedNode) change.getOldValue();
            
            // Find the appropriate day to restore the node to
            Integer dayNumber = extractDayFromMetadata(change);
            if (dayNumber != null) {
                NormalizedDay targetDay = findDayByNumber(itinerary, dayNumber);
                if (targetDay != null) {
                    targetDay.getNodes().add(restoredNode);
                }
            }
        }
    }
    
    /**
     * Reverse a replace operation by restoring the original node
     */
    private void reverseReplace(NormalizedItinerary itinerary, ChangeDetail change) {
        String nodeId = change.getElementId();
        
        // Remove the replacement node
        for (NormalizedDay day : itinerary.getDays()) {
            day.getNodes().removeIf(node -> node.getId() != null && node.getId().equals(nodeId));
        }
        
        // Restore the original node from oldValue
        if (change.getOldValue() instanceof NormalizedNode) {
            NormalizedNode originalNode = (NormalizedNode) change.getOldValue();
            
            Integer dayNumber = extractDayFromMetadata(change);
            if (dayNumber != null) {
                NormalizedDay targetDay = findDayByNumber(itinerary, dayNumber);
                if (targetDay != null) {
                    targetDay.getNodes().add(originalNode);
                }
            }
        }
    }
    
    /**
     * Find a node by ID across all days
     */
    private NormalizedNode findNodeById(NormalizedItinerary itinerary, String nodeId) {
        return itinerary.getDays().stream()
                .flatMap(day -> day.getNodes().stream())
                .filter(node -> node.getId() != null && node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find a day by day number
     */
    private NormalizedDay findDayByNumber(NormalizedItinerary itinerary, Integer dayNumber) {
        return itinerary.getDays().stream()
                .filter(day -> day.getDayNumber().equals(dayNumber))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Extract itinerary ID from revision record
     * This is a simplified implementation - in practice, you'd store this explicitly
     */
    private String extractItineraryIdFromRevision(RevisionRecord revision) {
        // For now, we'll assume the itinerary ID is stored in the revision metadata
        // or can be derived from the revision context
        if (revision.getUserId() != null) {
            // This is a placeholder - in practice, you'd have a proper mapping
            return revision.getUserId(); // Simplified assumption
        }
        return null;
    }
    
    /**
     * Extract version number from revision record
     */
    private Integer extractVersionFromRevision(RevisionRecord revision) {
        // Extract version from revision metadata or calculate based on timestamp
        if (revision.getChanges() != null && !revision.getChanges().isEmpty()) {
            // Look for version in metadata
            for (ChangeDetail change : revision.getChanges()) {
                if (change.getMetadata() != null && change.getMetadata().containsKey("version")) {
                    Object version = change.getMetadata().get("version");
                    if (version instanceof Integer) {
                        return (Integer) version;
                    }
                }
            }
        }
        
        // Default to timestamp-based version if not found
        return (int) (revision.getTimestamp() / 1000);
    }
    
    /**
     * Extract day number from change metadata
     */
    private Integer extractDayFromMetadata(ChangeDetail change) {
        if (change.getMetadata() != null && change.getMetadata().containsKey("day")) {
            Object day = change.getMetadata().get("day");
            if (day instanceof Integer) {
                return (Integer) day;
            }
        }
        return null;
    }
    
    /**
     * Deep copy an itinerary using JSON serialization
     */
    private NormalizedItinerary deepCopyItinerary(NormalizedItinerary original) {
        try {
            String json = objectMapper.writeValueAsString(original);
            return objectMapper.readValue(json, NormalizedItinerary.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deep copy itinerary", e);
            throw new RuntimeException("Failed to deep copy itinerary", e);
        }
    }
}