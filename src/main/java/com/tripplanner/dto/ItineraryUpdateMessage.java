package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for WebSocket messages containing itinerary updates
 * Used for real-time synchronization between clients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryUpdateMessage {

    /**
     * Type of update being broadcast
     * Examples: node_update, agent_progress, revision_created, chat_message, error
     */
    @JsonProperty("updateType")
    private String updateType;

    /**
     * ID of the itinerary being updated
     */
    @JsonProperty("itineraryId")
    private String itineraryId;

    /**
     * The actual update data - structure varies by updateType
     */
    @JsonProperty("data")
    private Object data;

    /**
     * User ID who initiated the update (optional)
     */
    @JsonProperty("userId")
    private String userId;

    /**
     * Timestamp when the update occurred
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Optional metadata for the update
     */
    @JsonProperty("metadata")
    private Object metadata;

    /**
     * Builder method for creating ItineraryUpdateMessage instances
     */
    public static ItineraryUpdateMessageBuilder builder() {
        return new ItineraryUpdateMessageBuilder();
    }

    /**
     * Builder class for ItineraryUpdateMessage
     */
    public static class ItineraryUpdateMessageBuilder {
        private String updateType;
        private String itineraryId;
        private Object data;
        private String userId;
        private Instant timestamp;
        private Object metadata;

        public ItineraryUpdateMessageBuilder updateType(String updateType) {
            this.updateType = updateType;
            return this;
        }

        public ItineraryUpdateMessageBuilder itineraryId(String itineraryId) {
            this.itineraryId = itineraryId;
            return this;
        }

        public ItineraryUpdateMessageBuilder data(Object data) {
            this.data = data;
            return this;
        }

        public ItineraryUpdateMessageBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public ItineraryUpdateMessageBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ItineraryUpdateMessageBuilder metadata(Object metadata) {
            this.metadata = metadata;
            return this;
        }

        public ItineraryUpdateMessage build() {
            ItineraryUpdateMessage message = new ItineraryUpdateMessage();
            message.updateType = this.updateType;
            message.itineraryId = this.itineraryId;
            message.data = this.data;
            message.userId = this.userId;
            message.timestamp = this.timestamp;
            message.metadata = this.metadata;
            return message;
        }
    }

    /**
     * Create an agent progress update message
     */
    public static ItineraryUpdateMessage createAgentProgress(String itineraryId, String agentId, int progress, String status) {
        return ItineraryUpdateMessage.builder()
                .updateType("agent_progress")
                .itineraryId(itineraryId)
                .data(java.util.Map.of(
                        "agentId", agentId,
                        "progress", progress,
                        "status", status
                ))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a node update message
     */
    public static ItineraryUpdateMessage createNodeUpdate(String itineraryId, String nodeId, Object nodeData, String userId) {
        return ItineraryUpdateMessage.builder()
                .updateType("node_update")
                .itineraryId(itineraryId)
                .data(java.util.Map.of(
                        "nodeId", nodeId,
                        "nodeData", nodeData
                ))
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a revision created message
     */
    public static ItineraryUpdateMessage createRevisionCreated(String itineraryId, String revisionId, String description, String userId) {
        return ItineraryUpdateMessage.builder()
                .updateType("revision_created")
                .itineraryId(itineraryId)
                .data(java.util.Map.of(
                        "revisionId", revisionId,
                        "description", description
                ))
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a chat message update
     */
    public static ItineraryUpdateMessage createChatMessage(String itineraryId, String messageId, String content, String sender) {
        return ItineraryUpdateMessage.builder()
                .updateType("chat_message")
                .itineraryId(itineraryId)
                .data(java.util.Map.of(
                        "messageId", messageId,
                        "content", content,
                        "sender", sender
                ))
                .userId(sender)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error message
     */
    public static ItineraryUpdateMessage createError(String itineraryId, String errorMessage, String userId) {
        return ItineraryUpdateMessage.builder()
                .updateType("error")
                .itineraryId(itineraryId)
                .data(java.util.Map.of("error", errorMessage))
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a connection status message
     */
    public static ItineraryUpdateMessage createConnectionStatus(String itineraryId, String status) {
        return ItineraryUpdateMessage.builder()
                .updateType("connection_status")
                .itineraryId(itineraryId)
                .data(java.util.Map.of("status", status))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an itinerary updated message
     */
    public static ItineraryUpdateMessage createItineraryUpdated(String itineraryId, Object itineraryData, String userId) {
        return ItineraryUpdateMessage.builder()
                .updateType("itinerary_updated")
                .itineraryId(itineraryId)
                .data(itineraryData)
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }
}