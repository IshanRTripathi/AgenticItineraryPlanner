package com.tripplanner.data.repo;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.data.entity.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Repository for Itinerary entity operations with Firestore.
 */
@Repository
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(Firestore.class)
public class ItineraryRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryRepository.class);
    private static final String COLLECTION_NAME = "itineraries";
    
    private final Firestore firestore;
    
    public ItineraryRepository(Firestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Save an itinerary to Firestore.
     */
    public Itinerary save(Itinerary itinerary) throws ExecutionException, InterruptedException {
        logger.info("=== FIRESTORE SAVE ITINERARY ===");
        logger.info("Itinerary ID: {}", itinerary.getId());
        logger.info("User ID: {}", itinerary.getUserId());
        logger.info("Destination: {}", itinerary.getDestination());
        logger.info("Status: {}", itinerary.getStatus());
        
        if (itinerary.getId() == null) {
            // Generate ID for new itinerary
            String newId = firestore.collection(COLLECTION_NAME).document().getId();
            itinerary.setId(newId);
            logger.info("Generated new itinerary ID: {}", newId);
        }
        
        itinerary.updateTimestamp();
        logger.info("Updated timestamp: {}", itinerary.getUpdatedAt());
        
        logger.info("Saving to Firestore collection: {}", COLLECTION_NAME);
        firestore.collection(COLLECTION_NAME)
                .document(itinerary.getId())
                .set(itinerary)
                .get();
        
        logger.info("=== FIRESTORE SAVE COMPLETED ===");
        logger.info("Itinerary saved successfully: {}", itinerary.getId());
        logger.info("===============================");
        return itinerary;
    }
    
    /**
     * Find itinerary by ID.
     */
    public Optional<Itinerary> findById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Finding itinerary by ID: {}", id);
        
        DocumentSnapshot document = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get()
                .get();
        
        if (document.exists()) {
            Itinerary itinerary = document.toObject(Itinerary.class);
            if (itinerary != null) {
                itinerary.setId(document.getId());
            }
            logger.debug("Itinerary found: {}", id);
            return Optional.ofNullable(itinerary);
        }
        
        logger.debug("Itinerary not found: {}", id);
        return Optional.empty();
    }
    
    /**
     * Find itineraries by user ID, ordered by updated timestamp (most recent first).
     */
    public List<Itinerary> findByUserId(String userId) throws ExecutionException, InterruptedException {
        return findByUserId(userId, 50); // Default limit
    }
    
    /**
     * Find itineraries by user ID with limit.
     */
    public List<Itinerary> findByUserId(String userId, int limit) throws ExecutionException, InterruptedException {
        logger.debug("Finding itineraries for user: {}, limit: {}", userId, limit);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .get();
        
        List<Itinerary> itineraries = querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Itinerary itinerary = doc.toObject(Itinerary.class);
                    if (itinerary != null) {
                        itinerary.setId(doc.getId());
                    }
                    return itinerary;
                })
                .collect(Collectors.toList());
        
        logger.debug("Found {} itineraries for user: {}", itineraries.size(), userId);
        return itineraries;
    }
    
    /**
     * Find itinerary by share token (for public sharing).
     */
    public Optional<Itinerary> findByShareToken(String shareToken) throws ExecutionException, InterruptedException {
        logger.debug("Finding itinerary by share token: {}", shareToken);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("shareToken", shareToken)
                .whereEqualTo("isPublic", true)
                .limit(1)
                .get()
                .get();
        
        if (!querySnapshot.isEmpty()) {
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            Itinerary itinerary = document.toObject(Itinerary.class);
            if (itinerary != null) {
                itinerary.setId(document.getId());
            }
            logger.debug("Itinerary found by share token");
            return Optional.ofNullable(itinerary);
        }
        
        logger.debug("Itinerary not found by share token");
        return Optional.empty();
    }
    
    /**
     * Update itinerary status.
     */
    public void updateStatus(String id, String status) throws ExecutionException, InterruptedException {
        logger.debug("Updating status for itinerary: {} to {}", id, status);
        
        firestore.collection(COLLECTION_NAME)
                .document(id)
                .update("status", status, "updatedAt", Instant.now())
                .get();
        
        logger.debug("Status updated for itinerary: {}", id);
    }
    
    /**
     * Update itinerary sharing settings.
     */
    public void updateSharingSettings(String id, boolean isPublic, String shareToken) throws ExecutionException, InterruptedException {
        logger.debug("Updating sharing settings for itinerary: {}", id);
        
        firestore.collection(COLLECTION_NAME)
                .document(id)
                .update("isPublic", isPublic, "shareToken", shareToken, "updatedAt", Instant.now())
                .get();
        
        logger.debug("Sharing settings updated for itinerary: {}", id);
    }
    
    /**
     * Delete itinerary by ID.
     */
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Deleting itinerary: {}", id);
        
        firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete()
                .get();
        
        logger.debug("Itinerary deleted: {}", id);
    }
    
    /**
     * Check if itinerary exists by ID.
     */
    public boolean existsById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get()
                .get();
        
        return document.exists();
    }
    
    /**
     * Find itineraries by status.
     */
    public List<Itinerary> findByStatus(String status) throws ExecutionException, InterruptedException {
        logger.debug("Finding itineraries with status: {}", status);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .get();
        
        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Itinerary itinerary = doc.toObject(Itinerary.class);
                    if (itinerary != null) {
                        itinerary.setId(doc.getId());
                    }
                    return itinerary;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Find itineraries by destination.
     */
    public List<Itinerary> findByDestination(String destination, int limit) throws ExecutionException, InterruptedException {
        logger.debug("Finding itineraries for destination: {}", destination);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("destination", destination)
                .whereEqualTo("isPublic", true)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .get();
        
        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Itinerary itinerary = doc.toObject(Itinerary.class);
                    if (itinerary != null) {
                        itinerary.setId(doc.getId());
                    }
                    return itinerary;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Count itineraries by user ID.
     */
    public long countByUserId(String userId) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .get();
        
        return querySnapshot.size();
    }
    
    /**
     * Count total itineraries.
     */
    public long count() throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .get()
                .get();
        
        return querySnapshot.size();
    }
}
