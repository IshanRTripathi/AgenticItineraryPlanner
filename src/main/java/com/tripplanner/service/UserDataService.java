package com.tripplanner.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedNode;
import com.tripplanner.dto.NodeTiming;
import com.tripplanner.dto.NodeLocation;
import com.tripplanner.dto.Coordinates;
import com.tripplanner.dto.AgentStatus;
import com.tripplanner.dto.ItinerarySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String CHATS_SUBCOLLECTION = "chats";
    private static final String REVISIONS_SUBCOLLECTION = "revisions";

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
                            // Use manual conversion to handle mixed data formats
                            return convertDocumentToNormalizedItinerary(doc);
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
                // Always use manual conversion to handle mixed data formats
                logger.info("Converting itinerary {} using manual method to handle mixed data formats", itineraryId);
                NormalizedItinerary itinerary = convertDocumentToNormalizedItinerary(document);
                if (itinerary != null) {
                    logger.info("Successfully converted itinerary {} for user: {}", itineraryId, userId);
                    return Optional.of(itinerary);
                } else {
                    logger.error("Failed to convert itinerary {} using manual method", itineraryId);
                    throw new RuntimeException("Failed to deserialize itinerary using manual conversion");
                }
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
                // Use manual conversion to handle mixed data formats
                NormalizedItinerary revision = convertDocumentToNormalizedItinerary(document);
                if (revision != null) {
                    logger.info("Found revision {} for user: {}, itinerary: {}", revisionId, userId, itineraryId);
                    return Optional.of(revision);
                } else {
                    logger.error("Failed to convert revision {} using manual method", revisionId);
                    throw new RuntimeException("Failed to deserialize revision using manual conversion");
                }
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
     */
    public List<NormalizedItinerary> getItineraryRevisions(String userId, String itineraryId) {
        try {
            logger.info("Fetching all revisions for user: {}, itinerary: {}", userId, itineraryId);
            
            DocumentReference itineraryDoc = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(ITINERARIES_SUBCOLLECTION)
                    .document(itineraryId);
            
            Query query = itineraryDoc.collection(REVISIONS_SUBCOLLECTION)
                    .orderBy("version", Query.Direction.DESCENDING);
            
            QuerySnapshot querySnapshot = query.get().get();
            
            List<NormalizedItinerary> revisions = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        try {
                            // Use manual conversion to handle mixed data formats
                            return convertDocumentToNormalizedItinerary(doc);
                        } catch (Exception e) {
                            logger.error("Failed to convert revision document to NormalizedItinerary: {}", doc.getId(), e);
                            return null;
                        }
                    })
                    .filter(revision -> revision != null)
                    .collect(Collectors.toList());
            
            logger.info("Found {} revisions for user: {}, itinerary: {}", revisions.size(), userId, itineraryId);
            return revisions;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to fetch revisions for user: {}, itinerary: {}", userId, itineraryId, e);
            throw new RuntimeException("Failed to fetch revisions", e);
        }
    }

    /**
     * Manually convert Firestore document to NormalizedItinerary, handling mixed data formats
     */
    private NormalizedItinerary convertDocumentToNormalizedItinerary(DocumentSnapshot document) {
        try {
            Map<String, Object> data = document.getData();
            if (data == null) {
                return null;
            }

            NormalizedItinerary itinerary = new NormalizedItinerary();
            
            // Set basic fields
            itinerary.setItineraryId((String) data.get("itineraryId"));
            itinerary.setVersion(((Number) data.get("version")).intValue());
            itinerary.setUserId((String) data.get("userId"));
            itinerary.setSummary((String) data.get("summary"));
            itinerary.setCurrency((String) data.get("currency"));
            itinerary.setDestination((String) data.get("destination"));
            itinerary.setStartDate((String) data.get("startDate"));
            itinerary.setEndDate((String) data.get("endDate"));
            
            // Handle time fields with conversion
            itinerary.setCreatedAt(convertToLong(data.get("createdAt")));
            itinerary.setUpdatedAt(convertToLong(data.get("updatedAt")));
            
            // Handle themes
            @SuppressWarnings("unchecked")
            List<String> themes = (List<String>) data.get("themes");
            itinerary.setThemes(themes);
            
            // Handle days with manual conversion
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> daysData = (List<Map<String, Object>>) data.get("days");
            if (daysData != null) {
                List<NormalizedDay> days = new ArrayList<>();
                for (Map<String, Object> dayData : daysData) {
                    NormalizedDay day = convertToNormalizedDay(dayData);
                    if (day != null) {
                        days.add(day);
                    }
                }
                itinerary.setDays(days);
            }
            
            // Handle agents
            @SuppressWarnings("unchecked")
            Map<String, Object> agentsData = (Map<String, Object>) data.get("agents");
            if (agentsData != null) {
                Map<String, AgentStatus> agents = new HashMap<>();
                for (Map.Entry<String, Object> entry : agentsData.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> agentData = (Map<String, Object>) entry.getValue();
                    AgentStatus agentStatus = new AgentStatus();
                    agentStatus.setLastRunAt(convertToLong(agentData.get("lastRunAt")));
                    agentStatus.setStatus((String) agentData.get("status"));
                    agents.put(entry.getKey(), agentStatus);
                }
                itinerary.setAgents(agents);
            } else {
                // Set default agents if none exist
                Map<String, AgentStatus> defaultAgents = new HashMap<>();
                defaultAgents.put("planner", new AgentStatus());
                defaultAgents.put("enrichment", new AgentStatus());
                itinerary.setAgents(defaultAgents);
            }
            
            // Handle settings
            @SuppressWarnings("unchecked")
            Map<String, Object> settingsData = (Map<String, Object>) data.get("settings");
            if (settingsData != null) {
                // TODO: Convert settings data if needed
                // For now, set default settings
                ItinerarySettings settings = new ItinerarySettings();
                settings.setAutoApply(false);
                settings.setDefaultScope("trip");
                itinerary.setSettings(settings);
            } else {
                // Set default settings if none exist
                ItinerarySettings settings = new ItinerarySettings();
                settings.setAutoApply(false);
                settings.setDefaultScope("trip");
                itinerary.setSettings(settings);
            }
            
            return itinerary;
            
        } catch (Exception e) {
            logger.error("Failed to manually convert document to NormalizedItinerary", e);
            return null;
        }
    }
    
    /**
     * Convert a day data map to NormalizedDay
     */
    private NormalizedDay convertToNormalizedDay(Map<String, Object> dayData) {
        try {
            NormalizedDay day = new NormalizedDay();
            day.setDayNumber(((Number) dayData.get("dayNumber")).intValue());
            day.setDate((String) dayData.get("date"));
            day.setLocation((String) dayData.get("location"));
            day.setNotes((String) dayData.get("notes"));
            
            // Handle nodes with manual conversion
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodesData = (List<Map<String, Object>>) dayData.get("nodes");
            if (nodesData != null) {
                List<NormalizedNode> nodes = new ArrayList<>();
                for (Map<String, Object> nodeData : nodesData) {
                    NormalizedNode node = convertToNormalizedNode(nodeData);
                    if (node != null) {
                        nodes.add(node);
                    }
                }
                day.setNodes(nodes);
            }
            
            return day;
        } catch (Exception e) {
            logger.error("Failed to convert day data to NormalizedDay", e);
            return null;
        }
    }
    
    /**
     * Convert a node data map to NormalizedNode
     */
    private NormalizedNode convertToNormalizedNode(Map<String, Object> nodeData) {
        try {
            NormalizedNode node = new NormalizedNode();
            node.setId((String) nodeData.get("id"));
            node.setType((String) nodeData.get("type"));
            node.setTitle((String) nodeData.get("title"));
            node.setLocked((Boolean) nodeData.get("locked"));
            node.setBookingRef((String) nodeData.get("bookingRef"));
            node.setStatus((String) nodeData.get("status"));
            node.setUpdatedBy((String) nodeData.get("updatedBy"));
            
            // Handle updatedAt with conversion
            node.setUpdatedAt(convertToLong(nodeData.get("updatedAt")));
            
            // Handle timing
            @SuppressWarnings("unchecked")
            Map<String, Object> timingData = (Map<String, Object>) nodeData.get("timing");
            if (timingData != null) {
                NodeTiming timing = new NodeTiming();
                timing.setStartTime(convertToLong(timingData.get("startTime")));
                timing.setEndTime(convertToLong(timingData.get("endTime")));
                timing.setDurationMin(timingData.get("durationMin") != null ? 
                    ((Number) timingData.get("durationMin")).intValue() : null);
                node.setTiming(timing);
            }
            
            // Handle location
            @SuppressWarnings("unchecked")
            Map<String, Object> locationData = (Map<String, Object>) nodeData.get("location");
            if (locationData != null) {
                NodeLocation location = new NodeLocation();
                location.setName((String) locationData.get("name"));
                location.setAddress((String) locationData.get("address"));
                
                // Handle coordinates
                @SuppressWarnings("unchecked")
                Map<String, Object> coordinatesData = (Map<String, Object>) locationData.get("coordinates");
                if (coordinatesData != null) {
                    Coordinates coordinates = new Coordinates();
                    Object latObj = coordinatesData.get("lat");
                    Object lngObj = coordinatesData.get("lng");
                    
                    if (latObj instanceof Number) {
                        coordinates.setLat(((Number) latObj).doubleValue());
                    }
                    if (lngObj instanceof Number) {
                        coordinates.setLng(((Number) lngObj).doubleValue());
                    }
                    
                    location.setCoordinates(coordinates);
                }
                
                node.setLocation(location);
            }
            
            return node;
        } catch (Exception e) {
            logger.error("Failed to convert node data to NormalizedNode", e);
            return null;
        }
    }
    
    /**
     * Convert various time formats to Long (milliseconds since epoch)
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof java.util.Map) {
            // Handle legacy Instant format stored as HashMap
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
                if (map.containsKey("seconds") && map.containsKey("nanos")) {
                    // Firestore Timestamp format
                    Long seconds = ((Number) map.get("seconds")).longValue();
                    Integer nanos = ((Number) map.get("nanos")).intValue();
                    return seconds * 1000 + nanos / 1_000_000;
                }
            } catch (Exception e) {
                logger.warn("Failed to convert HashMap to Long: {}", value, e);
            }
        }
        return null;
    }
}
