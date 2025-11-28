package com.tripplanner.dto;

/**
 * DTO for real-time chat progress updates via SSE.
 * Sent to frontend to show what the AI is doing.
 */
public class ChatProgressEvent {
    
    private String type; // "progress", "complete", "error"
    private String stage; // "intent", "generation", "validation", "applying"
    private String message; // User-friendly message
    private int progress; // 0-100
    private long timestamp;
    
    public ChatProgressEvent() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ChatProgressEvent(String type, String stage, String message, int progress) {
        this.type = type;
        this.stage = stage;
        this.message = message;
        this.progress = progress;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Static factory methods for common events
    public static ChatProgressEvent progress(String stage, String message, int progress) {
        return new ChatProgressEvent("progress", stage, message, progress);
    }
    
    public static ChatProgressEvent complete(String message) {
        return new ChatProgressEvent("complete", "done", message, 100);
    }
    
    public static ChatProgressEvent error(String message) {
        return new ChatProgressEvent("error", "error", message, 0);
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStage() {
        return stage;
    }
    
    public void setStage(String stage) {
        this.stage = stage;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "ChatProgressEvent{" +
                "type='" + type + '\'' +
                ", stage='" + stage + '\'' +
                ", message='" + message + '\'' +
                ", progress=" + progress +
                ", timestamp=" + timestamp +
                '}';
    }
}
