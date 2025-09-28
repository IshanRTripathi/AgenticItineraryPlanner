package com.tripplanner.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.dto.TripMetadata;
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
 * Service for managing user-specific trip metadata in Firestore
 * Organizes data as: users/{userId}/itineraries/{itineraryId} -> TripMetadata
 * Full itinerary data is stored separately in ItineraryJsonService
 */
@Service
public class UserDataService {

    private static final Logger logger = LoggerFactory.getLogger(UserDataService.class);

    @Autowired
    private Firestore firestore;

    private static final String USERS_COLLECTION = "users";
    private static final String ITINERARIES_SUBCOLLECTION = "itineraries";
    private static final String CHATS_SUBCOLLECTION = "chats";
    private static final String REVISIONS_SUBCOLLECTION = "revisions";

    /**
     * Get all trip metadata for a specific user
     */
    public List<TripMetadata> getUserTripMetadata(String userId) {
        try {
            logger.info("Fetching trip metadata for user: {}", userId);
            
            DocumentReference userDoc = firestore.collection(USERS_COLLECTION).document(userId);
            Query query = userDoc.collection(ITINERARIES_SUBCOLLECTION).orderBy("createdAt", Query.Direction.DESCENDING);
            
            QuerySnapshot querySnapshot = query.get().get();
            
            List<TripMetadata> tripMetadata = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        try {
                            return doc.toObject(TripMetadata.class);
                        } catch (Exception e) {
                            logger.error("Failed to convert document to TripMetadata: {}", doc.getId(), e);
                            return null;
                        }
                    })
                    .filter(metadata -> metadata != null)
                    .collect(Collectors.toList());
            
            logger.info("Found {} trip metadata entries for user: {}", tripMetadata.size(), userId);
            return tripMetadata;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch trip metadata for user: {}", userId, e);
            throw new RuntimeException("Failed to fetch user trip metadata", e);
        }
    }

    /**
     * Get a specific trip metadata for a user
     */
    public Optional<TripMetadata> getUserTripMetadata(String userId, String itineraryId) {
        try {
            logger.info("Fetching trip metadata {} for user: {}", itineraryId, userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            DocumentSnapshot document = docRef.get().get();
            
            if (document.exists()) {
                TripMetadata tripMetadata = document.toObject(TripMetadata.class);
                if (tripMetadata != null) {
                    logger.info("Successfully retrieved trip metadata {} for user: {}", itineraryId, userId);
                    return Optional.of(tripMetadata);
                } else {
                    logger.error("Failed to convert trip metadata {} to object", itineraryId);
                    throw new RuntimeException("Failed to deserialize trip metadata");
                }
            } else {
                logger.warn("Trip metadata {} not found for user: {}", itineraryId, userId);
                return Optional.empty();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch trip metadata {} for user: {}", itineraryId, userId, e);
            throw new RuntimeException("Failed to fetch user trip metadata", e);
        }
    }

    /**
     * Save trip metadata for a specific user (overloaded method that accepts TripMetadata)
     */
    public void saveUserTripMetadata(String userId, TripMetadata tripMetadata) {
        try {
            logger.info("Saving trip metadata {} for user: {}", tripMetadata.getItineraryId(), userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(tripMetadata.getItineraryId());
            
            // Ensure metadata has correct user ID and timestamps
            tripMetadata.setUserId(userId);
            if (tripMetadata.getCreatedAt() == null) {
                tripMetadata.setCreatedAt(System.currentTimeMillis());
            }
            tripMetadata.setUpdatedAt(System.currentTimeMillis());
            
            docRef.set(tripMetadata).get();
            
            logger.info("Successfully saved trip metadata {} for user: {}", tripMetadata.getItineraryId(), userId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to save trip metadata {} for user: {}", tripMetadata.getItineraryId(), userId, e);
            throw new RuntimeException("Failed to save user trip metadata", e);
        }
    }
    
    /**
     * Save trip metadata for a specific user (overloaded method that accepts NormalizedItinerary)
     */
    public void saveUserTripMetadata(String userId, NormalizedItinerary normalizedItinerary) {
        try {
            logger.info("Saving trip metadata from NormalizedItinerary {} for user: {}", normalizedItinerary.getItineraryId(), userId);
            
            // Convert NormalizedItinerary to TripMetadata
            TripMetadata tripMetadata = new TripMetadata(normalizedItinerary);
            tripMetadata.setUserId(userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(tripMetadata.getItineraryId());
            
            // Ensure metadata has correct user ID and timestamps
            if (tripMetadata.getCreatedAt() == null) {
                tripMetadata.setCreatedAt(System.currentTimeMillis());
            }
            tripMetadata.setUpdatedAt(System.currentTimeMillis());
            
            docRef.set(tripMetadata).get();
            
            logger.info("Successfully saved trip metadata {} for user: {}", tripMetadata.getItineraryId(), userId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to save trip metadata {} for user: {}", normalizedItinerary.getItineraryId(), userId, e);
            throw new RuntimeException("Failed to save user trip metadata", e);
        }
    }

    /**
     * Update existing trip metadata for a user
     */
    public void updateUserTripMetadata(String userId, TripMetadata tripMetadata) {
        try {
            logger.info("Updating trip metadata {} for user: {}", tripMetadata.getItineraryId(), userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(tripMetadata.getItineraryId());
            
            // Update metadata
            tripMetadata.setUpdatedAt(System.currentTimeMillis());
            tripMetadata.setUserId(userId);
            
            docRef.set(tripMetadata).get();
            
            logger.info("Successfully updated trip metadata {} for user: {}", tripMetadata.getItineraryId(), userId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to update trip metadata {} for user: {}", tripMetadata.getItineraryId(), userId, e);
            throw new RuntimeException("Failed to update user trip metadata", e);
        }
    }

    /**
     * Delete trip metadata for a user
     */
    public void deleteUserTripMetadata(String userId, String itineraryId) {
        try {
            logger.info("Deleting itinerary {} for user: {}", itineraryId, userId);
            
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            docRef.delete().get();
            
            logger.info("Successfully deleted trip metadata {} for user: {}", itineraryId, userId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to delete trip metadata {} for user: {}", itineraryId, userId, e);
            throw new RuntimeException("Failed to delete user trip metadata", e);
        }
    }

    /**
     * Check if a user owns a specific trip
     */
    public boolean userOwnsTrip(String userId, String itineraryId) {
        try {
            DocumentReference docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            DocumentSnapshot document = docRef.get().get();
            boolean exists = document.exists();
            
            logger.debug("User {} {} trip {}", userId, exists ? "owns" : "does not own", itineraryId);
            return exists;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to check ownership of trip {} for user: {}", itineraryId, userId, e);
            return false;
        }
    }

    /**
     * Get user's chat history for a specific itinerary
     */
    public List<Object> getUserChatHistory(String userId, String itineraryId) {
        try {
            logger.info("Fetching chat history for user: {}, itinerary: {}", userId, itineraryId);
            
            DocumentReference itineraryDoc = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            Query query = itineraryDoc.collection(CHATS_SUBCOLLECTION)
                    .orderBy("timestamp", Query.Direction.ASCENDING);
            
            QuerySnapshot querySnapshot = query.get().get();
            
            List<Object> chatMessages = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        try {
                            return doc.getData();
                        } catch (Exception e) {
                            logger.error("Failed to convert chat document to object: {}", doc.getId(), e);
                            return null;
                        }
                    })
                    .filter(message -> message != null)
                    .collect(Collectors.toList());
            
            logger.info("Found {} chat messages for user: {}, itinerary: {}", chatMessages.size(), userId, itineraryId);
            return chatMessages;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch chat history for user: {}, itinerary: {}", userId, itineraryId, e);
            throw new RuntimeException("Failed to fetch chat history", e);
        }
    }

    /**
     * Save user's chat message for a specific itinerary
     */
    public void saveUserChatMessage(String userId, String itineraryId, Object chatMessage) {
        try {
            logger.info("Saving chat message for user: {}, itinerary: {}", userId, itineraryId);
            
            DocumentReference itineraryDoc = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            itineraryDoc.collection(CHATS_SUBCOLLECTION).add(chatMessage).get();
            
            logger.info("Successfully saved chat message for user: {}, itinerary: {}", userId, itineraryId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to save chat message for user: {}, itinerary: {}", userId, itineraryId, e);
            throw new RuntimeException("Failed to save chat message", e);
        }
    }

    /**
     * Save a revision of an itinerary
     */
    public void saveItineraryRevision(String userId, String itineraryId, NormalizedItinerary revision) {
        try {
            logger.info("Saving revision for user: {}, itinerary: {}, version: {}", userId, itineraryId, revision.getVersion());
            
            DocumentReference itineraryDoc = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            // Use version as document ID for revisions
            String revisionId = String.valueOf(revision.getVersion());
            
            itineraryDoc.collection(REVISIONS_SUBCOLLECTION)
                    .document(revisionId)
                    .set(revision)
                    .get();
            
            logger.info("Successfully saved revision {} for user: {}, itinerary: {}", revisionId, userId, itineraryId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to save revision for user: {}, itinerary: {}", userId, itineraryId, e);
            throw new RuntimeException("Failed to save revision", e);
        }
    }

    /**
     * Get a specific revision of an itinerary
     * Note: Revisions are still stored as full NormalizedItinerary objects
     */
    public Optional<NormalizedItinerary> getItineraryRevision(String userId, String itineraryId, String revisionId) {
        try {
            logger.info("Fetching revision {} for user: {}, itinerary: {}", revisionId, userId, itineraryId);
            
            DocumentReference revisionDoc = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId)
                    .collection(REVISIONS_SUBCOLLECTION)
                    .document(revisionId);
            
            DocumentSnapshot document = revisionDoc.get().get();
            
            if (document.exists()) {
                // For revisions, we still store full NormalizedItinerary objects
                // This should be converted to use ItineraryJsonService in the future
                logger.warn("Revision storage still uses legacy format - should be migrated to ItineraryJsonService");
                return Optional.empty();
            } else {
                logger.warn("Revision {} not found for user: {}, itinerary: {}", revisionId, userId, itineraryId);
                return Optional.empty();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch revision {} for user: {}, itinerary: {}", revisionId, userId, itineraryId, e);
            throw new RuntimeException("Failed to fetch revision", e);
        }
    }

    /**
     * Get all revisions for an itinerary
     * Note: Revisions are still stored as full NormalizedItinerary objects
     */
    public List<NormalizedItinerary> getItineraryRevisions(String userId, String itineraryId) {
        try {
            logger.info("Fetching all revisions for user: {}, itinerary: {}", userId, itineraryId);
            
            // For now, return empty list as revisions should be migrated to ItineraryJsonService
            logger.warn("Revision storage still uses legacy format - should be migrated to ItineraryJsonService");
            return List.of();
            
        } catch (Exception e) {
            logger.error("Failed to fetch revisions for user: {}, itinerary: {}", userId, itineraryId, e);
            throw new RuntimeException("Failed to fetch revisions", e);
        }
    }

}
