package com.tripplanner.dto.tools;

import com.tripplanner.dto.IntentResult;

/**
 * Response DTO for Intent Cache Tool.
 */
public class IntentCacheResponse {
    
    private boolean success;
    private boolean cached;
    private IntentResult intent;
    private String error;
    
    public IntentCacheResponse() {}
    
    public static IntentCacheResponse hit(IntentResult intent) {
        IntentCacheResponse response = new IntentCacheResponse();
        response.setSuccess(true);
        response.setCached(true);
        response.setIntent(intent);
        return response;
    }
    
    public static IntentCacheResponse miss() {
        IntentCacheResponse response = new IntentCacheResponse();
        response.setSuccess(true);
        response.setCached(false);
        return response;
    }
    
    public static IntentCacheResponse error(String error) {
        IntentCacheResponse response = new IntentCacheResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isCached() {
        return cached;
    }
    
    public void setCached(boolean cached) {
        this.cached = cached;
    }
    
    public IntentResult getIntent() {
        return intent;
    }
    
    public void setIntent(IntentResult intent) {
        this.intent = intent;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
