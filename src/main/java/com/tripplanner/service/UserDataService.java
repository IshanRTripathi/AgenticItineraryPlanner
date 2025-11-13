package com.tripplanner.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.dto.TripMetadata;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.CanonicalPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
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
    private static final String CANONICAL_PLACES_COLLECTION = "canonical_places";

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
                // Revisions are not supported in the new system
                logger.error("Revision storage is not supported in the new system");
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
            
            // Revisions are not supported in the new system
            logger.error("Revision storage is not supported in the new system");
            return List.of();
            
        } catch (Exception e) {
            logger.error("Failed to fetch revisions for user: {}, itinerary: {}", userId, itineraryId, e);
            throw new RuntimeException("Failed to fetch revisions", e);
        }
    }

    // ===== CANONICAL PLACE METHODS =====
    
    /**
     * Save a canonical place to the global canonical places collection.
     */
    public void saveCanonicalPlace(CanonicalPlace canonicalPlace) {
        try {
            logger.info("Saving canonical place: {}", canonicalPlace.getPlaceId());
            
            DocumentReference docRef = firestore
                    .collection(CANONICAL_PLACES_COLLECTION)
                    .document(canonicalPlace.getPlaceId());
            
            docRef.set(canonicalPlace).get();
            
            logger.info("Successfully saved canonical place: {}", canonicalPlace.getPlaceId());
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to save canonical place: {}", canonicalPlace.getPlaceId(), e);
            throw new RuntimeException("Failed to save canonical place", e);
        }
    }
    
    /**
     * Get a canonical place by its ID.
     */
    public Optional<CanonicalPlace> getCanonicalPlace(String placeId) {
        try {
            logger.debug("Fetching canonical place: {}", placeId);
            
            DocumentReference docRef = firestore
                    .collection(CANONICAL_PLACES_COLLECTION)
                    .document(placeId);
            
            DocumentSnapshot document = docRef.get().get();
            
            if (document.exists()) {
                CanonicalPlace canonicalPlace = document.toObject(CanonicalPlace.class);
                if (canonicalPlace != null) {
                    logger.debug("Successfully retrieved canonical place: {}", placeId);
                    return Optional.of(canonicalPlace);
                } else {
                    logger.error("Failed to convert canonical place {} to object", placeId);
                    return Optional.empty();
                }
            } else {
                logger.debug("Canonical place {} not found", placeId);
                return Optional.empty();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch canonical place: {}", placeId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get all canonical places.
     * Note: This is inefficient for large datasets and should be paginated in production.
     */
    public List<CanonicalPlace> getAllCanonicalPlaces() {
        try {
            logger.debug("Fetching all canonical places");
            
            QuerySnapshot querySnapshot = firestore
                    .collection(CANONICAL_PLACES_COLLECTION)
                    .get()
                    .get();
            
            List<CanonicalPlace> canonicalPlaces = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        try {
                            return doc.toObject(CanonicalPlace.class);
                        } catch (Exception e) {
                            logger.error("Failed to convert document to CanonicalPlace: {}", doc.getId(), e);
                            return null;
                        }
                    })
                    .filter(place -> place != null)
                    .collect(Collectors.toList());
            
            logger.debug("Found {} canonical places", canonicalPlaces.size());
            return canonicalPlaces;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch all canonical places", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Delete a canonical place by its ID.
     */
    public void deleteCanonicalPlace(String placeId) {
        try {
            logger.info("Deleting canonical place: {}", placeId);
            
            DocumentReference docRef = firestore
                    .collection(CANONICAL_PLACES_COLLECTION)
                    .document(placeId);
            
            docRef.delete().get();
            
            logger.info("Successfully deleted canonical place: {}", placeId);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to delete canonical place: {}", placeId, e);
            throw new RuntimeException("Failed to delete canonical place", e);
        }
    }

    // ===== GUEST USER METHODS =====
    
    /**
     * Check if a user ID represents a guest user.
     * 
     * @param userId The user ID to check
     * @return true if the user ID is "anonymous"
     */
    public boolean isGuestUser(String userId) {
        return "anonymous".equals(userId);
    }
    
    /**
     * Migrate all data from a guest user to an authenticated user.
     * This transfers all itineraries and associated data.
     * 
     * @param guestUserId The guest user ID
     * @param authenticatedUserId The authenticated user ID
     * @return Number of itineraries migrated
     */
    public int migrateGuestDataToUser(String guestUserId, String authenticatedUserId) {
        logger.info("Migrating guest data from {} to {}", guestUserId, authenticatedUserId);
        
        try {
            // Get all guest user's trip metadata
            List<TripMetadata> guestTrips = getUserTripMetadata(guestUserId);
            
            if (guestTrips.isEmpty()) {
                logger.info("No trips to migrate for guest user: {}", guestUserId);
                return 0;
            }
            
            int migratedCount = 0;
            
            for (TripMetadata trip : guestTrips) {
                try {
                    String itineraryId = trip.getItineraryId();
                    logger.info("Migrating itinerary: {} from guest {} to user {}", 
                               itineraryId, guestUserId, authenticatedUserId);
                    
                    // Update the trip metadata with new user ID
                    trip.setUserId(authenticatedUserId);
                    trip.setUpdatedAt(System.currentTimeMillis());
                    
                    // Save under authenticated user
                    saveUserTripMetadata(authenticatedUserId, trip);
                    
                    // Delete from guest user
                    deleteUserTripMetadata(guestUserId, itineraryId);
                    
                    // IMPORTANT: Also update the NormalizedItinerary userId in the main itineraries collection
                    // This is handled by the ItineraryJsonService which will be called by the frontend
                    // after migration to update the master itinerary document
                    
                    migratedCount++;
                    logger.info("Successfully migrated itinerary: {}", itineraryId);
                    
                } catch (Exception e) {
                    logger.error("Failed to migrate itinerary: {}", trip.getItineraryId(), e);
                    // Continue with other itineraries
                }
            }
            
            logger.info("Migration complete: {} itineraries migrated from {} to {}", 
                       migratedCount, guestUserId, authenticatedUserId);
            
            return migratedCount;
            
        } catch (Exception e) {
            logger.error("Failed to migrate guest data from {} to {}", guestUserId, authenticatedUserId, e);
            throw new RuntimeException("Failed to migrate guest data", e);
        }
    }
    
    /**
     * Delete all data for a user (used for guest cleanup).
     * 
     * @param userId The user ID to delete
     */
    public void deleteUserData(String userId) {
        try {
            logger.info("Deleting all data for user: {}", userId);
            
            // Get all trip metadata
            List<TripMetadata> trips = getUserTripMetadata(userId);
            
            // Delete each trip metadata
            for (TripMetadata trip : trips) {
                deleteUserTripMetadata(userId, trip.getItineraryId());
            }
            
            logger.info("Successfully deleted all data for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Failed to delete user data for: {}", userId, e);
            throw new RuntimeException("Failed to delete user data", e);
        }
    }
    
    /**
     * Get the creation time of the oldest trip for a user.
     * Used to determine if guest data should be cleaned up.
     * 
     * @param userId The user ID
     * @return The creation timestamp of the oldest trip, or null if no trips exist
     */
    public Long getOldestTripCreationTime(String userId) {
        try {
            List<TripMetadata> trips = getUserTripMetadata(userId);
            
            if (trips.isEmpty()) {
                return null;
            }
            
            return trips.stream()
                    .map(TripMetadata::getCreatedAt)
                    .filter(createdAt -> createdAt != null)
                    .min(Long::compareTo)
                    .orElse(null);
                    
        } catch (Exception e) {
            logger.error("Failed to get oldest trip creation time for user: {}", userId, e);
            return null;
        }
    }

}
