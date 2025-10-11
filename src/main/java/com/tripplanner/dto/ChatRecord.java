package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Chat record for storing conversation history within itineraries.
 * Contains message information including sender, content, and metadata.
 */
public class ChatRecord {
    
    @NotBlank
    @JsonProperty("messageId")
    private String messageId;
    
    @NotNull
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @NotBlank
    @JsonProperty("sender")
    private String sender; // "user" or "agent" or specific agent name
    
    @NotBlank
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("type")
    private String type; // "text", "system", "error", "success"
    
    @JsonProperty("metadata")
    private Object metadata; // Additional data like agent progress, etc.
    
    public ChatRecord() {}
    
    public ChatRecord(String messageId, Long timestamp, String sender, 
                     String content, String type) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.sender = sender;
        this.content = content;
        this.type = type;
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Object getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "ChatRecord{" +
                "messageId='" + messageId + '\'' +
                ", timestamp=" + timestamp +
                ", sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", type='" + type + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}