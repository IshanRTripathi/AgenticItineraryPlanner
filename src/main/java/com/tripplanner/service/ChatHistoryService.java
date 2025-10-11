package com.tripplanner.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing chat history in Firestore.
 */
@Service
public class ChatHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryService.class);
    private static final String COLLECTION_ITINERARIES = "itineraries";
    private static final String SUBCOLLECTION_CHAT_MESSAGES = "chat_messages";
    private static final int BATCH_SIZE = 500;

    private final Firestore firestore;

    public ChatHistoryService(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Retrieve chat history for an itinerary.
     */
    public List<Map<String, Object>> getChatHistory(String itineraryId) {
        try {
            var snapshot = firestore
                    .collection(COLLECTION_ITINERARIES)
                    .document(itineraryId)
                    .collection(SUBCOLLECTION_CHAT_MESSAGES)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .get();

            List<Map<String, Object>> chatHistory = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> message = new HashMap<>(doc.getData());
                message.put("id", doc.getId());
                chatHistory.add(message);
            }

            logger.info("Retrieved {} chat messages for itinerary {}", chatHistory.size(), itineraryId);
            return chatHistory;
        } catch (Exception e) {
            logger.error("Error retrieving chat history from Firestore: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Save a chat message to history.
     */
    public Map<String, Object> saveChatMessage(String itineraryId, Map<String, Object> message, String userId) {
        try {
            // Add timestamp if not present
            if (!message.containsKey("timestamp")) {
                message.put("timestamp", System.currentTimeMillis());
            }

            // Add user ID
            message.put("userId", userId);

            // Save to Firestore subcollection
            var docRef = firestore
                    .collection(COLLECTION_ITINERARIES)
                    .document(itineraryId)
                    .collection(SUBCOLLECTION_CHAT_MESSAGES)
                    .document();

            docRef.set(message).get();

            Map<String, Object> response = new HashMap<>(message);
            response.put("id", docRef.getId());

            logger.info("Saved chat message for itinerary {}", itineraryId);
            return response;
        } catch (Exception e) {
            logger.error("Error saving chat message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save chat message", e);
        }
    }

    /**
     * Clear all chat history for an itinerary.
     */
    public int clearChatHistory(String itineraryId) {
        try {
            var snapshot = firestore
                    .collection(COLLECTION_ITINERARIES)
                    .document(itineraryId)
                    .collection(SUBCOLLECTION_CHAT_MESSAGES)
                    .get()
                    .get();

            // Delete in batches
            WriteBatch batch = firestore.batch();
            int count = 0;
            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                batch.delete(doc.getReference());
                count++;

                // Firestore batch limit is 500 operations
                if (count % BATCH_SIZE == 0) {
                    batch.commit().get();
                    batch = firestore.batch();
                }
            }

            // Commit remaining deletions
            if (count % BATCH_SIZE != 0) {
                batch.commit().get();
            }

            logger.info("Cleared {} chat messages for itinerary {}", count, itineraryId);
            return count;
        } catch (Exception e) {
            logger.error("Error clearing chat history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear chat history", e);
        }
    }
}







