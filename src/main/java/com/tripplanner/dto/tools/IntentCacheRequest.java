package com.tripplanner.dto.tools;

/**
 * Request DTO for Intent Cache Tool.
 */
public class IntentCacheRequest {
    
    private String text;
    private String contextHash;
    private Integer ttlSeconds;
    
    public IntentCacheRequest() {}
    
    public IntentCacheRequest(String text, String contextHash, Integer ttlSeconds) {
        this.text = text;
        this.contextHash = contextHash;
        this.ttlSeconds = ttlSeconds;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getContextHash() {
        return contextHash;
    }
    
    public void setContextHash(String contextHash) {
        this.contextHash = contextHash;
    }
    
    public Integer getTtlSeconds() {
        return ttlSeconds;
    }
    
    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
