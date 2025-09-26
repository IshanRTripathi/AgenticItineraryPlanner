package com.tripplanner.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.dto.NormalizedItinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service for managing user-specific data in Firestore
 * Organizes data as: users/{userId}/itineraries/{itineraryId}
 */
@Service
public class UserDataService {

    private static final Logger logger = LoggerFactory.getLogger(UserDataService.class);

    @Autowired
    private Firestore firestore;

    private static final String USERS_COLLECTION = "users";
    private static final String ITINERARIES_SUBCOLLECTION = "itineraries";

    /**
     * Get all itineraries for a specific user
     */
    public List<NormalizedItinerary> getUserItineraries(String userId) {
        try {
            logger.info("Fetching itineraries for user: {}", userId);
            
            DocumentReference userDoc = firestore.collection(USERS_COLLECTION).document(userId);
            Query query = userDoc.collection(ITINERARIES_SUBCOLLECTION).orderBy("createdAt", Query.Direction.DESCENDING);
            
            QuerySnapshot querySnapshot = query.get().get();
            
            List<NormalizedItinerary> itineraries = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        try {
                            return doc.toObject(NormalizedItinerary.class);
                        } catch (Exception e) {
                            logger.error("Failed to convert document to NormalizedItinerary: {}", doc.getId(), e);
                            return null;
                        }
                    })
                    .filter(itinerary -> itinerary != null)
                    .collect(Collectors.toList());
            
            logger.info("Found {} itineraries for user: {}", itineraries.size(), userId);
            return itineraries;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch itineraries for user: {}", userId, e);
            throw new RuntimeException("Failed to fetch user itineraries", e);
        }
    }

    /**
     * Get a specific itinerary for a user
     */
    public Optional<NormalizedItinerary> getUserItinerary(String userId, String itineraryId) {
        try {
            logger.info("Fetching itinerary {} for user: {}", itineraryId, userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            DocumentSnapshot document = docRef.get().get();
            
            if (document.exists()) {
                NormalizedItinerary itinerary = document.toObject(NormalizedItinerary.class);
                logger.info("Found itinerary {} for user: {}", itineraryId, userId);
                return Optional.of(itinerary);
            } else {
                logger.warn("Itinerary {} not found for user: {}", itineraryId, userId);
                return Optional.empty();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch itinerary {} for user: {}", itineraryId, userId, e);
            throw new RuntimeException("Failed to fetch user itinerary", e);
        }
    }

    /**
     * Save an itinerary for a specific user
     */
    public void saveUserItinerary(String userId, NormalizedItinerary itinerary) {
        try {
            logger.info("Saving itinerary {} for user: {}", itinerary.getItineraryId(), userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itinerary.getItineraryId());
            
            // Add metadata
            itinerary.setCreatedAt(System.currentTimeMillis());
            itinerary.setUpdatedAt(System.currentTimeMillis());
            itinerary.setUserId(userId);
            
            docRef.set(itinerary).get();
            
            logger.info("Successfully saved itinerary {} for user: {}", itinerary.getItineraryId(), userId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to save itinerary {} for user: {}", itinerary.getItineraryId(), userId, e);
            throw new RuntimeException("Failed to save user itinerary", e);
        }
    }

    /**
     * Update an existing itinerary for a user
     */
    public void updateUserItinerary(String userId, NormalizedItinerary itinerary) {
        try {
            logger.info("Updating itinerary {} for user: {}", itinerary.getItineraryId(), userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itinerary.getItineraryId());
            
            // Update metadata
            itinerary.setUpdatedAt(System.currentTimeMillis());
            itinerary.setUserId(userId);
            
            docRef.set(itinerary).get();
            
            logger.info("Successfully updated itinerary {} for user: {}", itinerary.getItineraryId(), userId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to update itinerary {} for user: {}", itinerary.getItineraryId(), userId, e);
            throw new RuntimeException("Failed to update user itinerary", e);
        }
    }

    /**
     * Delete an itinerary for a user
     */
    public void deleteUserItinerary(String userId, String itineraryId) {
        try {
            logger.info("Deleting itinerary {} for user: {}", itineraryId, userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            docRef.delete().get();
            
            logger.info("Successfully deleted itinerary {} for user: {}", itineraryId, userId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to delete itinerary {} for user: {}", itineraryId, userId, e);
            throw new RuntimeException("Failed to delete user itinerary", e);
        }
    }

    /**
     * Check if a user owns a specific itinerary
     */
    public boolean userOwnsItinerary(String userId, String itineraryId) {
        try {
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            DocumentSnapshot document = docRef.get().get();
            boolean exists = document.exists();
            
            logger.debug("User {} {} itinerary {}", userId, exists ? "owns" : "does not own", itineraryId);
            return exists;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to check ownership of itinerary {} for user: {}", itineraryId, userId, e);
            return false;
        }
    }

    /**
     * Get user's chat history (for future implementation)
     */
    public List<Object> getUserChatHistory(String userId, String itineraryId) {
        // TODO: Implement chat history storage
        logger.info("Chat history retrieval not yet implemented for user: {}, itinerary: {}", userId, itineraryId);
        return List.of();
    }

    /**
     * Save user's chat message (for future implementation)
     */
    public void saveUserChatMessage(String userId, String itineraryId, Object chatMessage) {
        // TODO: Implement chat message storage
        logger.info("Chat message saving not yet implemented for user: {}, itinerary: {}", userId, itineraryId);
    }
}
